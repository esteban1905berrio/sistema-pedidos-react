"""Base service class for all ABAP services."""

import logging
from contextlib import contextmanager
from typing import Optional, Any, Tuple, Dict, List
from app.core.rfc_adapter import RfcAdapter
from app.core.error_handler import handle_service_error
from app.core.response_formatter import (
    CHARACTER_LIMIT,
    truncate_response,
    should_truncate,
    calculate_response_size
)

logger = logging.getLogger(__name__)


class BaseService:
    """
    Base class for all ABAP services.

    Provides connection pool management and adapter creation.
    """

    def __init__(self, connection_pool):
        """
        Initialize base service.

        Args:
            connection_pool: RfcConnectionPool instance for getting connections
        """
        self.pool = connection_pool

    @contextmanager
    def _get_adapter(self):
        """
        Get an RfcAdapter with a connection from the pool.

        This context manager acquires a connection from the pool,
        creates an adapter, yields it for use, and automatically
        releases the connection back to the pool when done.

        Yields:
            RfcAdapter: Adapter instance with active connection

        Example:
            with self._get_adapter() as adapter:
                response = adapter.request(uri="/path", method="GET")
        """
        with self.pool.get_connection() as conn:
            adapter = RfcAdapter(conn)
            yield adapter

    def _handle_error(
        self,
        exception: Exception,
        operation: str,
        object_name: str = "",
        status_code: Optional[int] = None,
        response_text: Optional[str] = None
    ) -> str:
        """
        Handle and format service errors.

        Args:
            exception: Exception that occurred
            operation: Operation being performed
            object_name: Name of object being operated on
            status_code: HTTP status code if available
            response_text: Response body if available

        Returns:
            str: Actionable error message
        """
        return handle_service_error(
            exception,
            operation,
            object_name,
            status_code,
            response_text
        )

    def _check_and_truncate(
        self,
        data: Any,
        suggestions: Optional[List[str]] = None
    ) -> Tuple[Any, Dict[str, Any]]:
        """
        Check response size and truncate if it exceeds CHARACTER_LIMIT.

        This method ensures responses don't overwhelm the LLM's token limit
        by truncating large responses and providing helpful guidance.

        Args:
            data: Response data to check and potentially truncate
            suggestions: Optional list of suggestions for the LLM/user
                        on how to get complete results (e.g., use pagination,
                        add filters, use different format)

        Returns:
            Tuple[data, truncation_metadata]:
                - data: Original or truncated data
                - truncation_metadata: Dict with truncation info including:
                    * truncated: bool
                    * original_size: int (if truncated)
                    * truncated_size: int (if truncated)
                    * message: str (if truncated)
                    * suggestions: List[str] (if provided)

        Example:
            >>> result = {"items": [...1000 items...]}
            >>> suggestions = [
            ...     "Use pagination: offset=50",
            ...     "Add filters: object_types=['CLAS']"
            ... ]
            >>> data, meta = self._check_and_truncate(result, suggestions)
            >>> if meta["truncated"]:
            ...     print(meta["message"])
        """
        response_size = calculate_response_size(data)

        # No truncation needed
        if response_size <= CHARACTER_LIMIT:
            logger.debug(f"Response size ({response_size} chars) within limit")
            return data, {"truncated": False, "size": response_size}

        # Truncate response
        logger.warning(
            f"Response size ({response_size} chars) exceeds CHARACTER_LIMIT "
            f"({CHARACTER_LIMIT} chars). Truncating..."
        )

        truncated_data, was_truncated, metadata = truncate_response(
            data,
            CHARACTER_LIMIT,
            suggestions
        )

        return truncated_data, metadata

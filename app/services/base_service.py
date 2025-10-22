"""Base service class for all ABAP services."""

import logging
from contextlib import contextmanager
from typing import Optional
from app.core.rfc_adapter import RfcAdapter
from app.core.error_handler import handle_service_error

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

"""Error handling utilities for ABAP MCP server."""

import logging
from typing import Optional

logger = logging.getLogger(__name__)


def format_actionable_error(
    exception: Exception,
    context: str = "",
    status_code: Optional[int] = None,
    response_text: Optional[str] = None
) -> str:
    """
    Format an exception into an actionable error message for LLMs.

    Args:
        exception: The exception that occurred
        context: Context about what operation was being performed
        status_code: HTTP/ADT status code if available
        response_text: Response body if available

    Returns:
        str: User-friendly, actionable error message
    """
    # Connection errors
    error_msg = str(exception).lower()

    if 'connection reset' in error_msg or 'connection broken' in error_msg:
        return (
            f"Error: SAP connection was lost during {context or 'operation'}. "
            f"This is usually temporary. Please try again. "
            f"If the problem persists, check your network connection or SAP router."
        )

    if 'timeout' in error_msg:
        return (
            f"Error: Request timed out during {context or 'operation'}. "
            f"The SAP system may be slow or overloaded. Please try again. "
            f"Consider requesting less data if the problem continues."
        )

    if 'authentication' in error_msg or 'login' in error_msg or 'user' in error_msg:
        return (
            f"Error: Authentication failed. Please check your SAP credentials "
            f"in the .env file (SAP_USER and SAP_PASSWD). "
            f"Details: {exception}"
        )

    # HTTP status code errors
    if status_code:
        if status_code == 404:
            return (
                f"Error: Object not found (404). Please verify the object name "
                f"or URI is correct. Check spelling and that the object exists in SAP."
            )

        if status_code == 403:
            return (
                f"Error: Permission denied (403). You don't have authorization to access "
                f"this object or perform this operation. Contact your SAP administrator "
                f"to grant the necessary permissions."
            )

        if status_code == 500:
            return (
                f"Error: SAP system error (500). There was an internal error in the SAP system. "
                f"Check the object is valid and not corrupted. Details: {response_text[:200] if response_text else 'No details'}"
            )

        if status_code == 409:
            return (
                f"Error: Conflict (409). The object may already exist or be locked by another user. "
                f"Try unlocking the object or choose a different name."
            )

    # RFC errors
    try:
        from pyrfc import RfcCommunicationException, ABAPApplicationError, LogonException

        if isinstance(exception, LogonException):
            return (
                f"Error: SAP logon failed. Check your credentials (user, password, client) "
                f"and that the SAP system is accessible. Details: {exception}"
            )

        if isinstance(exception, ABAPApplicationError):
            return (
                f"Error: ABAP application error. The SAP system returned an error: {exception}. "
                f"This usually means the operation is not allowed or the data is invalid."
            )

        if isinstance(exception, RfcCommunicationException):
            return (
                f"Error: RFC communication failed. The connection to SAP was interrupted. "
                f"Please try again. If this persists, check the SAP router and network connectivity."
            )

    except ImportError:
        pass

    # Generic error fallback
    if context:
        return f"Error during {context}: {exception}"

    return f"Error: {exception}"


def handle_service_error(
    exception: Exception,
    operation: str,
    object_name: str = "",
    status_code: Optional[int] = None,
    response_text: Optional[str] = None
) -> str:
    """
    Handle service-level errors and return actionable message.

    Args:
        exception: Exception that occurred
        operation: Operation being performed (e.g., "get class source", "lock object")
        object_name: Name of the object being operated on
        status_code: HTTP status code if available
        response_text: Response body if available

    Returns:
        str: Actionable error message
    """
    context = f"{operation}"
    if object_name:
        context += f" for '{object_name}'"

    error_msg = format_actionable_error(
        exception,
        context=context,
        status_code=status_code,
        response_text=response_text
    )

    # Log the full error for debugging
    logger.error(f"Service error: {error_msg}", exc_info=True)

    return error_msg

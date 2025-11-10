"""Generic wrapper for MCP tools with guaranteed response and error handling."""

import logging
from typing import Any, Callable, Dict
from functools import wraps

logger = logging.getLogger(__name__)


def mcp_tool_wrapper(tool_func: Callable) -> Callable:
    """
    Wrapper for MCP tools that guarantees a response is always returned.

    Following MCP Best Practices:
    - ALWAYS return a response, even on error
    - Provide actionable error messages
    - Guide LLMs toward correct usage
    - Never allow tools to hang or fail silently

    Args:
        tool_func: The tool function to wrap

    Returns:
        Wrapped function that always returns a dict response
    """
    @wraps(tool_func)
    def wrapper(*args, **kwargs) -> Dict[str, Any]:
        tool_name = tool_func.__name__

        try:
            logger.debug(f"MCP Tool '{tool_name}' called with args={args}, kwargs={kwargs}")
            result = tool_func(*args, **kwargs)
            logger.debug(f"MCP Tool '{tool_name}' completed successfully")
            return result

        except TimeoutError as e:
            # Timeout errors - operation took too long
            logger.error(f"MCP Tool '{tool_name}' timed out: {e}")
            return {
                "error": True,
                "error_type": "TimeoutError",
                "error_message": str(e),
                "tool_name": tool_name,
                "suggestion": (
                    f"The operation '{tool_name}' timed out. This usually means:\n"
                    "1. The SAP system is slow or overloaded - try again later\n"
                    "2. The requested data is very large - try filtering or limiting results\n"
                    "3. The endpoint may not exist or is not responding\n"
                    "\n"
                    "Next steps:\n"
                    "- Verify the object/resource exists using search tools\n"
                    "- Try with a simpler query or fewer parameters\n"
                    "- Contact your SAP administrator if the issue persists"
                )
            }

        except ConnectionError as e:
            # Connection issues
            logger.error(f"MCP Tool '{tool_name}' connection error: {e}")
            return {
                "error": True,
                "error_type": "ConnectionError",
                "error_message": str(e),
                "tool_name": tool_name,
                "suggestion": (
                    f"Failed to connect to SAP system. This usually means:\n"
                    "1. SAP system is down or unreachable\n"
                    "2. Network connectivity issues\n"
                    "3. RFC connection pool exhausted\n"
                    "\n"
                    "Next steps:\n"
                    "- Verify SAP system is running\n"
                    "- Check network connectivity\n"
                    "- Contact your SAP administrator"
                )
            }

        except PermissionError as e:
            # Authorization issues
            logger.error(f"MCP Tool '{tool_name}' permission error: {e}")
            return {
                "error": True,
                "error_type": "PermissionError",
                "error_message": str(e),
                "tool_name": tool_name,
                "suggestion": (
                    f"Permission denied for operation '{tool_name}'. This usually means:\n"
                    "1. User lacks authorization for this operation\n"
                    "2. Object is locked by another user\n"
                    "3. Transport/package restrictions apply\n"
                    "\n"
                    "Next steps:\n"
                    "- Verify user has required SAP authorizations\n"
                    "- Check if object is locked using lock status tools\n"
                    "- Contact your SAP administrator for authorization"
                )
            }

        except ValueError as e:
            # Invalid input parameters
            logger.error(f"MCP Tool '{tool_name}' value error: {e}")
            return {
                "error": True,
                "error_type": "ValueError",
                "error_message": str(e),
                "tool_name": tool_name,
                "suggestion": (
                    f"Invalid parameter value for '{tool_name}'. This usually means:\n"
                    "1. Parameter format is incorrect\n"
                    "2. Required parameter is missing or empty\n"
                    "3. Parameter value is out of valid range\n"
                    "\n"
                    "Next steps:\n"
                    "- Review tool documentation for correct parameter format\n"
                    "- Verify all required parameters are provided\n"
                    "- Check parameter values match expected patterns"
                )
            }

        except KeyError as e:
            # Missing expected data
            logger.error(f"MCP Tool '{tool_name}' key error: {e}")
            return {
                "error": True,
                "error_type": "KeyError",
                "error_message": f"Missing expected data: {e}",
                "tool_name": tool_name,
                "suggestion": (
                    f"The SAP system response was missing expected data. This usually means:\n"
                    "1. The object or resource doesn't exist\n"
                    "2. The SAP system version doesn't support this operation\n"
                    "3. Data structure has changed\n"
                    "\n"
                    "Next steps:\n"
                    "- Verify the object exists using search tools\n"
                    "- Try alternative tools or approaches\n"
                    "- Contact your SAP administrator"
                )
            }

        except Exception as e:
            # Catch-all for unexpected errors
            error_type = type(e).__name__
            error_msg = str(e)
            logger.error(f"MCP Tool '{tool_name}' unexpected error: {error_type}: {error_msg}")
            logger.exception(f"Full traceback for '{tool_name}':")

            return {
                "error": True,
                "error_type": error_type,
                "error_message": error_msg,
                "tool_name": tool_name,
                "suggestion": (
                    f"An unexpected error occurred in '{tool_name}':\n"
                    f"Error: {error_type}: {error_msg}\n"
                    "\n"
                    "Next steps:\n"
                    "- Review the error message for specific details\n"
                    "- Try simplifying the operation or using alternative tools\n"
                    "- If this persists, report the issue to the MCP server maintainer"
                )
            }

    return wrapper

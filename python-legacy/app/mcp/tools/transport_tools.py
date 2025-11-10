"""MCP tool registration for transport management operations."""

import logging
from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Callable, Optional

from app.services.transport_service import TransportService
from app.mcp.tool_wrapper import mcp_tool_wrapper

logger = logging.getLogger(__name__)


def register_transport_tools(mcp: FastMCP, service_getter: Callable[[], TransportService]):
    """Register transport-related tools with MCP server."""

    @mcp.tool(
        name="transport_info",
        description="Get transport version history for an ABAP object. "
                   "Returns all versions with associated transport requests. "
                   "Use the /versions endpoint which returns an Atom feed with complete history."
    )
    def transport_info(
        obj_source_url: str = Field(
            description="URI of the object (e.g., '/sap/bc/adt/programs/includes/zsdi1038c_1' or '/sap/bc/adt/oo/classes/ztest')"
        ),
        dev_class: Optional[str] = Field(
            default=None,
            description="Development class/package (optional, not used in current implementation)"
        ),
        operation: Optional[str] = Field(
            default=None,
            description="Operation type (optional, not used in current implementation)"
        )
    ) -> dict:
        """
        Get transport version history for an object.

        Returns dictionary with:
        - object_uri: The object URI
        - object_name: Object name
        - total_versions: Number of versions
        - versions: List of versions with transport info

        Example:
            >>> transport_info("/sap/bc/adt/programs/includes/zsdi1038c_1")
            {
                "object_name": "ZSDI1038C_1",
                "total_versions": 5,
                "versions": [
                    {
                        "version_id": "00004",
                        "author": "JMVALENC",
                        "transport_number": "S4DK931511",
                        "transport_title": "DV-SD-I1038...",
                        ...
                    }
                ]
            }
        """
        # MCP Best Practice: ALWAYS return a response, even on timeout/error
        logger.info(f"🔧 MCP Tool 'transport_info' called with obj_source_url={obj_source_url}")

        try:
            logger.info(f"🔧 Calling service.transport_info()...")
            result = transport_service.transport_info(obj_source_url, dev_class, operation)
            logger.info(f"🔧 Service call completed, returning result")
            return result
        except TimeoutError as e:
            return {
                "error": True,
                "error_type": "TimeoutError",
                "error_message": str(e),
                "object_uri": obj_source_url,
                "suggestion": (
                    "The operation timed out after 30 seconds. This usually means:\n"
                    "1. The SAP system is slow or overloaded - try again later\n"
                    "2. The endpoint may not exist or is not responding\n"
                    "3. The object may not have version history available\n"
                    "\n"
                    "Try:\n"
                    "- Use search_objects() to verify the object exists\n"
                    "- Check the object URI format is correct\n"
                    "- Try again later when SAP system load is lower"
                )
            }
        except Exception as e:
            return {
                "error": True,
                "error_type": type(e).__name__,
                "error_message": str(e),
                "object_uri": obj_source_url,
                "suggestion": (
                    f"An error occurred: {type(e).__name__}: {str(e)}\n\n"
                    "Common solutions:\n"
                    "- Verify object exists using search_objects()\n"
                    "- Check URI format:\n"
                    "  * Includes: /sap/bc/adt/programs/includes/<name>\n"
                    "  * Classes: /sap/bc/adt/oo/classes/<name>\n"
                    "  * Programs: /sap/bc/adt/programs/programs/<name>\n"
                    "- Some objects may not support version history"
                )
            }

    @mcp.tool(
        name="create_transport",
        description="Create a new transport request in the SAP system. "
                   "Returns the transport number (TRKORR) of the newly created transport."
    )
    def create_transport(
        description: str = Field(
            description="Description of the transport request"
        ),
        dev_class: str = Field(
            description="Development class/package for the transport"
        ),
        transport_type: str = Field(
            default="K",
            description="Type of transport: 'K' for Workbench (default), 'C' for Customizing"
        )
    ) -> str:
        """Create a new transport request."""
        return service_getter().create_transport(description, dev_class, transport_type)

    @mcp.tool(
        name="list_user_transports",
        description="List transport requests for a user. "
                   "Returns a list of transports with their numbers, descriptions, and status."
    )
    def list_user_transports(
        user: Optional[str] = Field(
            default=None,
            description="User ID (default: current user)"
        ),
        status: Optional[str] = Field(
            default=None,
            description="Filter by status: 'R' for released, 'D' for modifiable"
        )
    ) -> list:
        """List transport requests for a user."""
        return service_getter().list_user_transports(user, status)

    @mcp.tool(
        name="get_transport_tasks",
        description="Get all tasks associated with a transport request by querying E070 table directly. "
                   "Returns a list of tasks with their numbers, owners, descriptions, status, and object counts. "
                   "Uses direct E070 table queries instead of ADT API to avoid token limitations."
    )
    def get_transport_tasks(
        transport_number: str = Field(
            description="Transport request number (e.g., 'CADK911272')"
        )
    ) -> list:
        """Get tasks for a transport request from E070 table."""
        return service_getter().get_transport_tasks(transport_number)

    @mcp.tool(
        name="get_transport_objects",
        description="Get all ABAP objects from a transport request or task by querying E071 table directly. "
                   "Returns complete transport data with metadata, objects, and tasks. "
                   "Can filter by task number to get objects for a specific task. "
                   "Uses direct table queries (E070/E071) instead of ADT API to avoid token limitations."
    )
    def get_transport_objects(
        transport_number: str = Field(
            description="Transport request number (e.g., 'CADK911088' for main OT or 'CADK911222' for task)"
        ),
        task_number: Optional[str] = Field(
            default=None,
            description="Optional task number to filter objects by task (e.g., 'CADK911222')"
        )
    ) -> dict:
        """
        Get objects from a transport request with complete metadata.

        Returns dictionary with:
        - success: bool
        - transport_number: str
        - metadata: dict (type, status, owner, dates, etc.)
        - objects: list (all objects from E071)
        - total_objects: int
        - tasks: list (task information, only for main transports)

        Example:
            get_transport_objects("CADK911088") -> All objects from main transport + tasks
            get_transport_objects("CADK911222") -> Objects from specific task
            get_transport_objects("CADK911088", "CADK911222") -> Filter by task
        """
        return service_getter().get_transport_objects(transport_number, task_number)

    @mcp.tool(
        name="add_object_to_transport",
        description="Add/assign an ABAP object to a transport request. "
                   "This is required before modifying objects in development systems."
    )
    def add_object_to_transport(
        transport_number: str = Field(
            description="Transport number to add the object to"
        ),
        object_uri: str = Field(
            description="URI of the object to add (e.g., '/sap/bc/adt/oo/classes/ztest')"
        ),
        lock_handle: Optional[str] = Field(
            default=None,
            description="Lock handle if the object is locked (optional)"
        )
    ) -> bool:
        """Add an object to a transport request."""
        return service_getter().add_object_to_transport(transport_number, object_uri, lock_handle)

    @mcp.tool(
        name="release_transport",
        description="Release a transport request. "
                   "WARNING: This will release the transport and make it immutable. "
                   "Only use in development systems, never in production!"
    )
    def release_transport(
        transport_number: str = Field(
            description="Transport number to release"
        ),
        ignore_atc: bool = Field(
            default=False,
            description="Ignore ATC (ABAP Test Cockpit) errors (default: False)"
        )
    ) -> dict:
        """Release a transport request."""
        return service_getter().release_transport(transport_number, ignore_atc)

    @mcp.tool(
        name="get_transport_config",
        description="Get transport configuration for the SAP system. "
                   "Returns target system, domain, and other transport settings."
    )
    def get_transport_config() -> dict:
        """Get transport configuration."""
        return service_getter().get_transport_config()

    @mcp.tool(
        name="delete_transport",
        description="Delete a transport request. "
                   "WARNING: Only works for non-released transports. Use with caution!"
    )
    def delete_transport(
        transport_number: str = Field(
            description="Transport number to delete"
        )
    ) -> bool:
        """Delete a transport request."""
        return service_getter().delete_transport(transport_number)

    @mcp.tool(
        name="set_transport_owner",
        description="Change the owner of a transport request. "
                   "Transfers ownership to another user."
    )
    def set_transport_owner(
        transport_number: str = Field(
            description="Transport number"
        ),
        target_user: str = Field(
            description="New owner user ID"
        )
    ) -> bool:
        """Set transport owner."""
        return service_getter().set_transport_owner(transport_number, target_user)

    @mcp.tool(
        name="add_transport_user",
        description="Add a collaborator user to a transport request. "
                   "Allows multiple users to work on the same transport."
    )
    def add_transport_user(
        transport_number: str = Field(
            description="Transport number"
        ),
        user: str = Field(
            description="User ID to add as collaborator"
        )
    ) -> bool:
        """Add user to transport."""
        return service_getter().add_transport_user(transport_number, user)

    @mcp.tool(
        name="get_system_users",
        description="Get a list of all users in the SAP system. "
                   "Useful for finding user IDs for transport collaboration."
    )
    def get_system_users() -> list:
        """Get system users."""
        return service_getter().get_system_users()

    @mcp.tool(
        name="get_transport_reference",
        description="Get transport references for an ABAP object. "
                   "Shows which transports contain or reference this object."
    )
    def get_transport_reference(
        pgmid: str = Field(
            description="Program ID (e.g., 'R3TR' for Repository objects)"
        ),
        obj_wbtype: str = Field(
            description="Workbench object type (e.g., 'PROG' for program, 'CLAS' for class)"
        ),
        obj_name: str = Field(
            description="Object name (e.g., 'ZTEST_PROGRAM')"
        ),
        tr_number: Optional[str] = Field(
            default=None,
            description="Optional transport number to filter references"
        )
    ) -> dict:
        """Get transport references for an object."""
        return service_getter().get_transport_reference(pgmid, obj_wbtype, obj_name, tr_number)

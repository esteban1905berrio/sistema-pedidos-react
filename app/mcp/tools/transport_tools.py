"""MCP tool registration for transport management operations."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Optional

from app.services.transport_service import TransportService


def register_transport_tools(mcp: FastMCP, transport_service: TransportService):
    """Register transport-related tools with MCP server."""

    @mcp.tool(
        name="transport_info",
        description="Get transport information for an ABAP object. "
                   "Returns transport number, status, lock information, and other transport metadata."
    )
    def transport_info(
        obj_source_url: str = Field(
            description="URI of the object (e.g., '/sap/bc/adt/oo/classes/ztest')"
        ),
        dev_class: Optional[str] = Field(
            default=None,
            description="Development class/package (optional)"
        ),
        operation: Optional[str] = Field(
            default=None,
            description="Operation type (optional)"
        )
    ) -> dict:
        """Get transport information for an object."""
        return transport_service.transport_info(obj_source_url, dev_class, operation)

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
        return transport_service.create_transport(description, dev_class, transport_type)

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
        return transport_service.list_user_transports(user, status)

    @mcp.tool(
        name="get_transport_request",
        description="Get complete transport request data including tasks and objects. "
                   "Returns full transport metadata, all tasks, and all objects in the transport."
    )
    def get_transport_request(
        transport_number: str = Field(
            description="Transport request number (e.g., 'S4DK932806')"
        )
    ) -> dict:
        """Get full transport request data."""
        return transport_service.get_transport_request(transport_number)

    @mcp.tool(
        name="get_transport_tasks",
        description="Get all tasks associated with a transport request. "
                   "Returns a list of tasks with their numbers, owners, and descriptions."
    )
    def get_transport_tasks(
        transport_number: str = Field(
            description="Transport request number (e.g., 'DEVK900123')"
        )
    ) -> list:
        """Get tasks for a transport request."""
        return transport_service.get_transport_tasks(transport_number)

    @mcp.tool(
        name="get_transport_objects",
        description="Get all ABAP objects from a transport request or task. "
                   "Returns list of objects with their types, names, and metadata. "
                   "Can filter by task number to get objects for a specific task."
    )
    def get_transport_objects(
        transport_number: str = Field(
            description="Transport request number (e.g., 'S4DK932806')"
        ),
        task_number: Optional[str] = Field(
            default=None,
            description="Optional task number to filter objects by task (e.g., 'S4DK932807')"
        )
    ) -> list:
        """Get objects from a transport request."""
        return transport_service.get_transport_objects(transport_number, task_number)

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
        return transport_service.add_object_to_transport(transport_number, object_uri, lock_handle)

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
        return transport_service.release_transport(transport_number, ignore_atc)

    @mcp.tool(
        name="get_transport_config",
        description="Get transport configuration for the SAP system. "
                   "Returns target system, domain, and other transport settings."
    )
    def get_transport_config() -> dict:
        """Get transport configuration."""
        return transport_service.get_transport_config()

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
        return transport_service.delete_transport(transport_number)

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
        return transport_service.set_transport_owner(transport_number, target_user)

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
        return transport_service.add_transport_user(transport_number, user)

    @mcp.tool(
        name="get_system_users",
        description="Get a list of all users in the SAP system. "
                   "Useful for finding user IDs for transport collaboration."
    )
    def get_system_users() -> list:
        """Get system users."""
        return transport_service.get_system_users()

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
        return transport_service.get_transport_reference(pgmid, obj_wbtype, obj_name, tr_number)

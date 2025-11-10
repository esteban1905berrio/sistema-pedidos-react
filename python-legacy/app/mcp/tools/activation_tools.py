"""MCP tool registration for ABAP object activation operations."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Callable, List, Dict

from app.services.activation_service import ActivationService


def register_activation_tools(mcp: FastMCP, service_getter: Callable[[], ActivationService]):
    """Register activation-related tools with MCP server."""

    @mcp.tool(
        name="activate",
        description="Activate a single ABAP object. "
                   "This makes the object active in the SAP system. "
                   "IMPORTANT: Always activate after modifying object source code."
    )
    def activate(
        object_name: str = Field(
            description="Name of the object (e.g., 'ZTEST_CLASS')"
        ),
        object_uri: str = Field(
            description="URI of the object (e.g., '/sap/bc/adt/oo/classes/ztest')"
        ),
        preaudit: bool = Field(
            default=True,
            description="Request preaudit check before activation (default: True)"
        )
    ) -> dict:
        """Activate a single object."""
        return service_getter().activate(object_name, object_uri, preaudit)

    @mcp.tool(
        name="activate_objects",
        description="Activate multiple ABAP objects in batch. "
                   "Efficient way to activate multiple objects at once. "
                   "Returns results for all objects."
    )
    def activate_objects(
        objects: List[Dict[str, str]] = Field(
            description="List of objects to activate. Each object must have 'name' and 'uri' keys. "
                       "Example: [{'name': 'ZTEST1', 'uri': '/sap/bc/adt/oo/classes/ztest1'}, ...]"
        ),
        preaudit: bool = Field(
            default=True,
            description="Request preaudit check before activation (default: True)"
        )
    ) -> dict:
        """Activate multiple objects in batch."""
        return service_getter().activate_objects(objects, preaudit)

    @mcp.tool(
        name="get_inactive_objects",
        description="Get list of inactive objects for current user. "
                   "Returns all objects that have been modified but not yet activated."
    )
    def get_inactive_objects() -> list:
        """Get list of inactive objects."""
        return service_getter().get_inactive_objects()

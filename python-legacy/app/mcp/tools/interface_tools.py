"""MCP tools for ABAP interface operations."""

from typing import Literal, Dict, Any, Callable
from mcp.server.fastmcp import FastMCP
from mcp.types import ToolAnnotations
from pydantic import Field

from app.services.interface_service import InterfaceService


def register_interface_tools(
    mcp: FastMCP, service_getter: Callable[[], InterfaceService]
):
    """
    Register interface-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        service_getter: Callable that returns InterfaceService instance (lazy-loaded)
    """

    @mcp.tool(
        name="get_interface_structure",
        description="[USE ONLY WHEN EXPLICITLY REQUESTED] Get the structure and metadata of an ABAP interface. "
        "Returns interface methods, visibility, and other metadata without the full source code. "
        "IMPORTANT: Only use this tool when the user specifically asks for interface structure/metadata. "
        "For general interface information, use get_interface_source instead.",
        annotations=ToolAnnotations(
            title="Get Interface Structure",
            readOnlyHint=True,
            destructiveHint=False,
            idempotentHint=True,
            openWorldHint=True,
        ),
    )
    def get_interface_structure(
        interface_name: str = Field(
            description="Name of the ABAP interface (e.g., 'ZIFCXR1002_ALVGRID', 'IF_EXAMPLE')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'",
        ),
        with_short_descriptions: bool = Field(
            default=True, description="Include short descriptions in response"
        ),
    ) -> Dict[str, Any]:
        """
        Get the structure and metadata of an ABAP interface.
        Returns interface methods, visibility, and other metadata.
        """
        return service_getter().get_interface_structure(
            interface_name, version, with_short_descriptions
        )

    @mcp.tool(
        name="get_interface_source",
        description="Get the source code of an ABAP interface from SAP system. "
        "Returns the complete interface definition including method signatures.",
        annotations=ToolAnnotations(
            title="Get Interface Source Code",
            readOnlyHint=True,
            destructiveHint=False,
            idempotentHint=True,
            openWorldHint=True,
        ),
    )
    def get_interface_source(
        interface_name: str = Field(
            description="Name of the ABAP interface (e.g., 'ZIFCXR1002_ALVGRID', 'IF_EXAMPLE')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active",
            description="Version to retrieve: 'active' for activated code or 'inactive' for draft",
        ),
    ) -> str:
        """
        Get the source code of an ABAP interface.

        Example:
            source = get_interface_source("ZIFCXR1002_ALVGRID")
            # INTERFACE zifcxr1002_alvgrid
            #   PUBLIC .
            #   METHODS:
            #     modificar_catalogo ...
            # ENDINTERFACE.
        """
        return service_getter().get_interface_source(interface_name, version)

    @mcp.tool(
        name="get_interface_includes",
        description="Get all includes of an ABAP interface (if any exist). "
        "Returns list of include types with their URIs and metadata. "
        "Useful for understanding interface structure and accessing specific include types.",
        annotations=ToolAnnotations(
            title="Get Interface Includes",
            readOnlyHint=True,
            destructiveHint=False,
            idempotentHint=True,
            openWorldHint=True,
        ),
    )
    def get_interface_includes(
        interface_name: str = Field(
            description="Name of the ABAP interface (e.g., 'ZIFCXR1002_ALVGRID')"
        ),
    ) -> list:
        """
        Get all includes of an ABAP interface (if any exist).
        Returns list of include types with their URIs and metadata.
        """
        return service_getter().get_interface_includes(interface_name)

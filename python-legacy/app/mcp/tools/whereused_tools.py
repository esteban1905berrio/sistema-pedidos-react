"""MCP tool registration for where-used analysis operations."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Callable, Optional

from app.services.whereused_service import WhereUsedService


def register_whereused_tools(mcp: FastMCP, service_getter: Callable[[], WhereUsedService]):
    """Register where-used analysis tools with MCP server."""

    @mcp.tool(
        name="get_usage_references",
        description="Get list of objects that reference/use a given ABAP object (first step in where-used analysis). "
                   "For CDS views, use URI format: /sap/bc/adt/ddic/ddl/sources/{cds_name}/source/main?version=active. "
                   "Returns object identifiers needed for get_usage_snippets calls. "
                   "Example: /sap/bc/adt/ddic/ddl/sources/ztfi1008_2/source/main?version=active"
    )
    def get_usage_references(
        object_uri: str = Field(
            description="URI to the object (e.g., /sap/bc/adt/ddic/ddl/sources/ztfi1008_2/source/main?version=active)"
        ),
        object_type: str = Field(
            default="DDLS",
            description="Type of object (DDLS for CDS views, CLAS for classes, etc.)"
        )
    ) -> dict:
        """
        Get usage references for an ABAP object.

        Returns:
            Dictionary containing:
            - total_references: Number of objects that reference this object
            - referenced_object_id: Main object identifier
            - references: List of referencing objects with their object_identifiers
        """
        return service_getter().get_usage_references(object_uri, object_type)

    @mcp.tool(
        name="get_usage_snippets",
        description="Find where an ABAP object is used in the codebase. "
                   "Returns code snippets showing all locations where the object is referenced. "
                   "The object_identifier format is: ABAPFullName;package;program;\\TY:classname;version. "
                   "Example: ABAPFullName;ZMMI1229_0;ZMMI1229_0C_1;\\TY:ZCLMMI1229_SINCRONIZA_INV_MAWM;2"
    )
    def get_usage_snippets(
        object_identifier: str = Field(
            description="Full object identifier in format: ABAPFullName;package;program;\\TY:classname;version"
        ),
        max_results: Optional[int] = Field(
            default=None,
            description="Maximum number of usage snippets to return (optional)"
        )
    ) -> dict:
        """
        Get usage snippets for an ABAP object.

        Returns:
            Dictionary containing:
            - object_identifier: The object being searched
            - total_usages: Total number of usages found
            - code_snippets: List of usage locations with code context
        """
        return service_getter().get_usage_snippets(object_identifier, max_results)

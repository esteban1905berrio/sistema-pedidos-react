"""MCP tools for ABAP object search operations."""

from typing import Callable
from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.search_service import SearchService


def register_search_tools(mcp: FastMCP, service_getter: Callable[[], SearchService]):
    """
    Register search-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        search_service: Callable that returns SearchService instance (lazy-loaded) for ABAP object search
    """

    @mcp.tool(
        name="search_objects",
        description="Search for ABAP objects in the SAP repository by name pattern. "
        "Supports wildcards (*) and returns matching classes, programs, function modules, and other objects.",
    )
    def search_objects(
        query: str = Field(
            description="Search query with wildcards (e.g., 'Z*', 'CL_ABAP*', '*UTIL*'). "
            "Use * for any characters."
        ),
        max_results: int = Field(
            default=20,
            description="Maximum number of results to return (1-100). Default is 20.",
            ge=1,
            le=100,
        ),
    ) -> list:
        """
        Search for ABAP objects in the SAP repository.

        This tool performs a quick search across the SAP repository to find objects
        matching the query pattern. Results include object name, type, URI, package,
        and other metadata.

        Common use cases:
        - Find custom objects: search_objects("Z*")
        - Find classes: search_objects("CL_ABAP*")
        - Find specific patterns: search_objects("*CHAR*UTIL*")
        - Find by prefix: search_objects("ZMY_COMPANY*")

        The results include:
        - Object name
        - Object type (CLAS/Class, PROG/Program, FUGR/Function Group, etc.)
        - URI for accessing the object
        - Package assignment
        - Description (if available)

        Example usage:
        - search_objects("Z*", max_results=10) - Find first 10 custom objects
        - search_objects("CL_ABAP_CHAR*") - Find character utility classes
        """
        return service_getter().search_objects(query, max_results=max_results)

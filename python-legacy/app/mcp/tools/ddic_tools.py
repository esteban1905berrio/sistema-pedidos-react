"""MCP tools for ABAP Data Dictionary (DDIC) operations."""

from typing import Callable, List, Dict, Any
from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.ddic_service import DdicService


def register_ddic_tools(mcp: FastMCP, service_getter: Callable[[], DdicService]):
    """
    Register DDIC-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        ddic_service: Callable that returns DdicService instance (lazy-loaded) for DDIC operations
    """

    @mcp.tool(
        name="get_ddic_element",
        description="Get definition of a Data Dictionary (DDIC) element. "
        "Supports tables, structures, data elements, domains, and table types. "
        "Returns field definitions, properties, and metadata.",
    )
    def get_ddic_element(
        element_name: str = Field(
            description="Name of the DDIC element (e.g., 'USR02', 'T000', 'MANDT')"
        ),
        element_type: str = Field(
            description="Type of element: 'tables', 'structures', 'dataelements', 'domains', or 'tableTypes'"
        )
    ) -> Dict[str, Any]:
        """
        Retrieve DDIC element definition.

        Returns comprehensive information about dictionary objects:
        - Tables: All fields, keys, descriptions
        - Structures: Field definitions and types
        - Data elements: Type information and domain
        - Domains: Value ranges and validation
        - Table types: Type definitions

        Example usage:
        - get_ddic_element("USR02", "tables")
        - get_ddic_element("T000", "tables")
        - get_ddic_element("MANDT", "dataelements")
        - get_ddic_element("CHAR10", "domains")
        """
        return service_getter().get_ddic_element(element_name, element_type)

    @mcp.tool(
        name="ddic_repository_access",
        description="Access DDIC repository by path. "
        "Provides direct access to dictionary repository structure. "
        "Advanced tool for navigating DDIC hierarchy.",
    )
    def ddic_repository_access(
        path: str = Field(
            description="Repository path to access (e.g., '/tables/usr02', '/dataelements/mandt')"
        )
    ) -> Dict[str, Any]:
        """
        Access DDIC repository by path.

        Provides low-level access to DDIC repository structure.
        Useful for exploring dictionary object hierarchies.

        Example usage:
        - ddic_repository_access("/tables/usr02")
        - ddic_repository_access("/dataelements/mandt")
        """
        return service_getter().ddic_repository_access(path)

    @mcp.tool(
        name="get_annotation_definitions",
        description="Get available CDS annotation definitions. "
        "Returns list of all CDS annotations supported by the system. "
        "Useful for understanding CDS view capabilities.",
    )
    def get_annotation_definitions() -> List[Dict[str, Any]]:
        """
        Retrieve CDS annotation definitions.

        Returns all available annotations for CDS views, including:
        - Annotation names
        - Data types
        - Descriptions
        - Scope and usage

        Example usage:
        - get_annotation_definitions()
        """
        return service_getter().get_annotation_definitions()

    @mcp.tool(
        name="package_search_help",
        description="Search for packages with autocomplete/search help. "
        "Supports wildcards (*) for pattern matching. "
        "Returns list of package names matching the query.",
    )
    def package_search_help(
        query: str = Field(
            description="Search query with optional wildcards (e.g., 'Z*', '*TEST*', 'ZPACKAGE')"
        ),
        max_results: int = Field(
            default=50,
            description="Maximum number of results to return (default: 50, max: 200)"
        )
    ) -> List[str]:
        """
        Search for packages (autocomplete).

        Provides package name autocomplete functionality.
        Supports wildcard patterns for flexible searching.

        Example usage:
        - package_search_help("Z*")
        - package_search_help("*TEST*", max_results=20)
        - package_search_help("ZPACKAGE")
        """
        return service_getter().package_search_help(query, max_results)

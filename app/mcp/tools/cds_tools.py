"""MCP tool registration for CDS Views operations."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Optional

from app.services.cds_service import CDSService


def register_cds_tools(mcp: FastMCP, cds_service: CDSService):
    """Register CDS Views tools with MCP server."""

    @mcp.tool(
        name="get_cds_view_metadata",
        description="Get complete metadata of a CDS (Core Data Services) view. "
                   "Returns name, description, SQL view name, package, owner, timestamps, and source URI. "
                   "Use this to understand the structure and properties of a CDS view before reading its source."
    )
    def get_cds_view_metadata(
        cds_name: str = Field(
            description="Name of the CDS view (e.g., 'ZI_RAP_ZTCXR1003_1')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> dict:
        """
        Get CDS view metadata.

        Returns metadata including SQL view name, package, timestamps, and more.
        """
        return cds_service.get_cds_view_metadata(cds_name, version)

    @mcp.tool(
        name="get_cds_view_source",
        description="Get the DDL (Data Definition Language) source code of a CDS view. "
                   "Returns the complete ABAP CDS definition including annotations, associations, and field selections. "
                   "Useful for analyzing CDS view logic, annotations, and data model."
    )
    def get_cds_view_source(
        cds_name: str = Field(
            description="Name of the CDS view (e.g., 'ZI_RAP_ZTCXR1003_1')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> str:
        """
        Get CDS view DDL source code.

        Returns the complete CDS definition with annotations.
        """
        return cds_service.get_cds_view_source(cds_name, version)

    @mcp.tool(
        name="search_cds_views_by_sqlview",
        description="Search for CDS views by SQL view name pattern. "
                   "Supports wildcards (*) for flexible searching. "
                   "Returns list of matching CDS views with basic information. "
                   "Example: 'ZI_RAP*' finds all CDS views with SQL view names starting with ZI_RAP."
    )
    def search_cds_views_by_sqlview(
        sql_view_name: str = Field(
            description="SQL view name pattern with wildcards (e.g., 'ZI_RAP*', '*CUSTOMER*')"
        ),
        max_results: Optional[int] = Field(
            default=None,
            description="Maximum number of results to return (optional, default 100)"
        )
    ) -> list:
        """
        Search CDS views by SQL view name.

        Returns list of matching CDS views with basic info.
        """
        return cds_service.search_cds_views_by_sqlview(sql_view_name, max_results)

    @mcp.tool(
        name="get_cds_view_properties",
        description="Get properties of a CDS view object including package, owner, API state, "
                   "application component, and software component. "
                   "Useful for understanding the organizational context and API release status of a CDS view."
    )
    def get_cds_view_properties(
        cds_name: str = Field(
            description="Name of the CDS view (e.g., 'ZI_RAP_ZTCXR1003_1')"
        )
    ) -> dict:
        """
        Get CDS view properties.

        Returns package, owner, API state, and component information.
        """
        return cds_service.get_cds_view_properties(cds_name)

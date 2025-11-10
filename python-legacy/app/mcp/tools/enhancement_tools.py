"""MCP tool registration for Enhancement (Ampliaciones) operations."""

from typing import Callable

from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.enhancement_service import EnhancementService


def register_enhancement_tools(mcp: FastMCP, service_getter: Callable[[], EnhancementService]):
    """Register Enhancement tools with MCP server."""

    @mcp.tool(
        name="search_enhancements",
        description="Search for enhancements (ampliaciones) in a package. "
                   "Returns list of enhancements with their type (ENHO/XHH, ENHO/XH, ENHO/XHB), description, and URI. "
                   "Enhancement types: "
                   "- ENHO/XHH: Hook Implementation (Explicit Enhancement) "
                   "- ENHO/XH: Enhancement Implementation "
                   "- ENHO/XHB: Enhancement Implementation with BAdI. "
                   "Use this to find all modifications and extensions in a package."
    )
    def search_enhancements(
        package: str = Field(
            description="Package name to search in (e.g., 'ZI1008')"
        ),
        enhancement_type: str = Field(
            default="ENHO",
            description="Enhancement type to filter (default: 'ENHO' for all enhancements)"
        )
    ) -> list:
        """
        Search for enhancements in a package.

        Returns list of enhancements with name, type, description, and URI.
        """
        return service_getter().search_enhancements(package, enhancement_type)

    @mcp.tool(
        name="get_enhancement_metadata",
        description="Get detailed metadata of an enhancement including hook implementations, "
                   "enhanced objects, spot names, and program names. "
                   "Shows the complete structure of the enhancement: which object is being enhanced, "
                   "where the enhancement point is located, and what type of hook is used. "
                   "Useful for understanding the context and impact of an enhancement."
    )
    def get_enhancement_metadata(
        enhancement_name: str = Field(
            description="Name of the enhancement (e.g., 'ZFII1008_1')"
        ),
        enhancement_subtype: str = Field(
            default="enhoxhh",
            description="Enhancement subtype: 'enhoxhh' (Hook Impl), 'enhoxh' (Enhancement Impl), 'enhoxhb' (BAdI)"
        )
    ) -> dict:
        """
        Get enhancement metadata.

        Returns complete metadata including hook implementations, enhanced objects,
        and spot information.
        """
        return service_getter().get_enhancement_metadata(enhancement_name, enhancement_subtype)

    @mcp.tool(
        name="get_enhancement_source",
        description="Get the source code of an enhancement. "
                   "Returns the ABAP code that implements the enhancement logic. "
                   "Shows the actual implementation within the ENHANCEMENT...ENDENHANCEMENT block. "
                   "Use this to review or analyze enhancement implementations."
    )
    def get_enhancement_source(
        enhancement_name: str = Field(
            description="Name of the enhancement (e.g., 'ZFII1008_1')"
        ),
        enhancement_subtype: str = Field(
            default="enhoxhh",
            description="Enhancement subtype: 'enhoxhh' (Hook Impl), 'enhoxh' (Enhancement Impl), 'enhoxhb' (BAdI)"
        )
    ) -> str:
        """
        Get enhancement source code.

        Returns the ABAP code implementation of the enhancement.
        """
        return service_getter().get_enhancement_source(enhancement_name, enhancement_subtype)

"""MCP tool registration for code quality operations."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.code_quality_service import CodeQualityService


def register_code_quality_tools(mcp: FastMCP, code_quality_service: CodeQualityService):
    """Register code quality tools with MCP server."""

    @mcp.tool(
        name="syntax_check",
        description="Perform syntax check on ABAP source code. "
                   "Returns list of errors, warnings, and info messages with line numbers. "
                   "Use this before activating modified code to catch syntax errors."
    )
    def syntax_check(
        object_uri: str = Field(
            description="URI of the object (e.g., '/sap/bc/adt/oo/classes/ztest')"
        ),
        include_uri: str = Field(
            description="URI of the include (e.g., '/sap/bc/adt/oo/classes/ztest/source/main')"
        ),
        source: str = Field(
            description="Source code to check for syntax errors"
        ),
        version: str = Field(
            default="active",
            description="Version to check: 'active' or 'inactive' (default: 'active')"
        )
    ) -> list:
        """Run syntax check on ABAP code."""
        return code_quality_service.syntax_check(object_uri, include_uri, source, version)

    @mcp.tool(
        name="prettyprint",
        description="Format ABAP source code using SAP pretty printer. "
                   "Automatically formats code according to SAP standards (keyword casing, indentation, etc.). "
                   "Returns the formatted source code."
    )
    def prettyprint(
        source: str = Field(
            description="Unformatted ABAP source code to format"
        )
    ) -> str:
        """Format ABAP source code."""
        return code_quality_service.prettyprint(source)

    @mcp.tool(
        name="get_prettyprint_settings",
        description="Get current pretty printer settings for the user. "
                   "Returns settings like indentation, keyword casing style, etc."
    )
    def get_prettyprint_settings() -> dict:
        """Get pretty printer settings."""
        return code_quality_service.get_prettyprint_settings()

    @mcp.tool(
        name="set_prettyprint_settings",
        description="Set pretty printer settings for the user. "
                   "Configure indentation and keyword casing style (e.g., keywordUpper, keywordLower)."
    )
    def set_prettyprint_settings(
        indent: bool = Field(
            default=True,
            description="Enable indentation (default: True)"
        ),
        style: str = Field(
            default="keywordUpper",
            description="Keyword casing style: 'keywordUpper' (default) or 'keywordLower'"
        )
    ) -> bool:
        """Set pretty printer settings."""
        return code_quality_service.set_prettyprint_settings(indent, style)

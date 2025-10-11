"""MCP tools for ABAP program operations."""

from typing import Literal
from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.program_service import ProgramService


def register_program_tools(mcp: FastMCP, program_service: ProgramService):
    """
    Register program-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        program_service: ProgramService instance for ABAP program operations
    """

    @mcp.tool(
        name="get_program_source",
        description="Get the source code of an ABAP program (report). "
        "Returns the complete program code including declarations, logic, and forms.",
    )
    def get_program_source(
        program_name: str = Field(
            description="Name of the ABAP program/report (e.g., 'RSUSR002', 'ZTEST_REPORT')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active",
            description="Version to retrieve: 'active' for activated code or 'inactive' for draft",
        ),
    ) -> str:
        """
        Retrieve the source code of an ABAP program.

        This tool fetches the complete source code of an ABAP program (report)
        from the SAP system, including all declarations, logic, forms, and routines.

        Example usage:
        - get_program_source("RSUSR002") - Get standard SAP user report
        - get_program_source("ZTEST_REPORT", version="inactive") - Get draft version
        """
        return program_service.get_program_source(program_name, version=version)

    @mcp.tool(
        name="get_include_source",
        description="Get the source code of a program include. "
        "Includes are modular pieces of code that can be included in programs.",
    )
    def get_include_source(
        program_name: str = Field(
            description="Name of the main ABAP program that contains the include"
        ),
        include_name: str = Field(
            description="Name of the include to retrieve (e.g., 'ZTEST_INCLUDE_TOP')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active", description="Version to retrieve: 'active' or 'inactive'"
        ),
    ) -> str:
        """
        Retrieve the source code of a program include.

        Includes are separate source code units that can be included in ABAP programs
        for better code organization. Common include types:
        - TOP includes: Global declarations
        - Form includes: Form routines
        - Data includes: Data declarations

        Example usage:
        - get_include_source("ZTEST_MAIN", "ZTEST_INCLUDE_TOP")
        """
        return program_service.get_include_source(program_name, include_name, version=version)

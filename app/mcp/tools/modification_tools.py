"""MCP tool registration for high-level object modification workflows."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Optional

from app.services.modification_service import ModificationService


def register_modification_tools(mcp: FastMCP, modification_service: ModificationService):
    """Register high-level modification workflow tools with MCP server."""

    @mcp.tool(
        name="modify_function_module",
        description="Complete workflow to modify a function module source code. "
                   "Executes: LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE. "
                   "This is a high-level tool that handles the entire modification process automatically. "
                   "For advanced control, use low-level tools: lock, set_object_source, unlock, activate."
    )
    def modify_function_module(
        function_module_name: str = Field(
            description="Name of the function module to modify (e.g., 'ZFIAAC002_DMEE_NRO_TRASL_DAV')"
        ),
        function_group_name: str = Field(
            description="Parent function group name (e.g., 'ZFIAAC002_1')"
        ),
        new_source: str = Field(
            description="Complete new ABAP source code for the function module. "
                       "Must include FUNCTION...ENDFUNCTION statements."
        ),
        transport: Optional[str] = Field(
            default=None,
            description="Transport number (required for transportable packages, e.g., 'CADK911140')"
        ),
        auto_activate: bool = Field(
            default=True,
            description="Automatically activate after modification (default: True). "
                       "Set to False to modify without activating."
        ),
        validate_syntax: bool = Field(
            default=True,
            description="Validate ABAP syntax before saving changes (default: True). "
                       "Prevents saving code with syntax errors."
        )
    ) -> dict:
        """
        Modify a function module with complete workflow automation.

        This tool handles the entire ADT modification process:
        1. Locks the function module for editing
        2. Validates syntax (if enabled)
        3. Modifies the source code
        4. Unlocks the function module
        5. Activates the changes (if auto_activate=True)

        Returns a detailed result dictionary with status of each step.
        If any step fails, the workflow stops and returns error details.
        """
        return modification_service.modify_function_module(
            function_module_name=function_module_name,
            function_group_name=function_group_name,
            new_source=new_source,
            transport=transport,
            auto_activate=auto_activate,
            validate_syntax=validate_syntax
        )

    @mcp.tool(
        name="modify_class",
        description="Complete workflow to modify an ABAP class source code. "
                   "Executes: LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE. "
                   "Supports different include types (main, implementation, testclasses, macros). "
                   "For advanced control, use low-level tools: lock, set_object_source, unlock, activate."
    )
    def modify_class(
        class_name: str = Field(
            description="Name of the class to modify (e.g., 'ZCL_TEST', 'ZCLCXR1002_UTIL')"
        ),
        new_source: str = Field(
            description="Complete new ABAP source code. "
                       "For 'main': include CLASS...ENDCLASS definition. "
                       "For 'implementation': include CLASS...IMPLEMENTATION...ENDCLASS."
        ),
        include_type: str = Field(
            default="main",
            description="Include type to modify: 'main' (definition), 'implementation', 'testclasses', 'macros'. "
                       "Default: 'main'"
        ),
        transport: Optional[str] = Field(
            default=None,
            description="Transport number (required for transportable packages)"
        ),
        auto_activate: bool = Field(
            default=True,
            description="Automatically activate after modification (default: True)"
        ),
        validate_syntax: bool = Field(
            default=True,
            description="Validate ABAP syntax before saving changes (default: True)"
        )
    ) -> dict:
        """
        Modify an ABAP class with complete workflow automation.

        This tool handles:
        1. Lock the class include
        2. Validate syntax (optional)
        3. Modify source code
        4. Unlock the class
        5. Activate changes (optional)

        Returns detailed status for each step.
        """
        return modification_service.modify_class(
            class_name=class_name,
            new_source=new_source,
            include_type=include_type,
            transport=transport,
            auto_activate=auto_activate,
            validate_syntax=validate_syntax
        )

    @mcp.tool(
        name="modify_program",
        description="Complete workflow to modify an ABAP program/report source code. "
                   "Executes: LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE. "
                   "For advanced control, use low-level tools: lock, set_object_source, unlock, activate."
    )
    def modify_program(
        program_name: str = Field(
            description="Name of the program to modify (e.g., 'ZTEST_REPORT', 'ZSDI1038C_1')"
        ),
        new_source: str = Field(
            description="Complete new ABAP source code. "
                       "Must include REPORT statement and all program logic."
        ),
        transport: Optional[str] = Field(
            default=None,
            description="Transport number (required for transportable packages)"
        ),
        auto_activate: bool = Field(
            default=True,
            description="Automatically activate after modification (default: True)"
        ),
        validate_syntax: bool = Field(
            default=True,
            description="Validate ABAP syntax before saving changes (default: True)"
        )
    ) -> dict:
        """
        Modify an ABAP program with complete workflow automation.

        This tool handles:
        1. Lock the program
        2. Validate syntax (optional)
        3. Modify source code
        4. Unlock the program
        5. Activate changes (optional)

        Returns detailed status for each step.
        """
        return modification_service.modify_program(
            program_name=program_name,
            new_source=new_source,
            transport=transport,
            auto_activate=auto_activate,
            validate_syntax=validate_syntax
        )

    @mcp.tool(
        name="modify_include",
        description="Complete workflow to modify a program include source code. "
                   "Executes: LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE. "
                   "Includes are modular pieces of code included in programs. "
                   "For advanced control, use low-level tools: lock, set_object_source, unlock, activate."
    )
    def modify_include(
        include_name: str = Field(
            description="Name of the include to modify (e.g., 'ZTEST_INCLUDE_TOP', 'ZSDI1038C_1_F01')"
        ),
        program_name: str = Field(
            description="Parent program name that contains this include (e.g., 'ZTEST_PROGRAM')"
        ),
        new_source: str = Field(
            description="Complete new ABAP source code for the include. "
                       "Typically contains DATA declarations, FORMs, or other modular code."
        ),
        transport: Optional[str] = Field(
            default=None,
            description="Transport number (required for transportable packages)"
        ),
        auto_activate: bool = Field(
            default=True,
            description="Automatically activate after modification (default: True)"
        ),
        validate_syntax: bool = Field(
            default=True,
            description="Validate ABAP syntax before saving changes (default: True)"
        )
    ) -> dict:
        """
        Modify a program include with complete workflow automation.

        This tool handles:
        1. Lock the include
        2. Validate syntax (optional)
        3. Modify source code
        4. Unlock the include
        5. Activate changes (optional)

        Returns detailed status for each step.
        """
        return modification_service.modify_include(
            include_name=include_name,
            program_name=program_name,
            new_source=new_source,
            transport=transport,
            auto_activate=auto_activate,
            validate_syntax=validate_syntax
        )

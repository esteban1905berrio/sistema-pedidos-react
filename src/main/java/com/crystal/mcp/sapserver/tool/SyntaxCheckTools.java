package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.SyntaxCheckResult;
import com.crystal.mcp.sapserver.service.SyntaxCheckService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP syntax checking.
 *
 * Provides tools to validate ABAP object syntax using ADT checkruns API.
 * Works with any ABAP object type: classes, programs, function modules,
 * interfaces, etc.
 */
@Component
@RequiredArgsConstructor
public class SyntaxCheckTools {

    private final SyntaxCheckService syntaxCheckService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check ABAP object syntax and return errors, warnings, and info messages.
     *
     * This tool validates the syntax of any ABAP object (class, program, function module,
     * interface, etc.) by calling the ADT checkruns API. Returns detailed messages with
     * line/column positions for each issue found.
     *
     * Use Cases:
     * - Validate syntax before activation
     * - Check for syntax errors after modifications
     * - Identify warnings and potential issues
     * - Verify inactive code before committing to transport
     *
     * Workflow Pattern:
     * 1. Modify object source (modify_class, modify_function_module, etc.)
     * 2. Check syntax with this tool
     * 3. Fix any errors
     * 4. Activate object when syntax is clean
     *
     * Example Object URIs:
     * - Class: "/sap/bc/adt/oo/classes/zcl_test/source/main"
     * - Program: "/sap/bc/adt/programs/programs/zrep_test/source/main"
     * - Function Module: "/sap/bc/adt/functions/groups/zgfg_test/fmodules/z_fm_test"
     * - Interface: "/sap/bc/adt/oo/interfaces/zif_test/source/main"
     *
     * @param objectUri ADT URI of the object to check. Obtain from search_objects or construct
     *                  from object name. Must be a valid ADT resource URI.
     * @param version   Version to check: "active" for activated code, "inactive" for draft code.
     *                  Default: "inactive" (recommended for checking before activation).
     * @return JSON string containing syntax check results with messages, status, and summary
     */
    @McpTool(
        description = """
            Check ABAP object syntax and return errors, warnings, and info messages.

            Progressive Discovery Stage 4: Validate syntax after modifications.
            Use after modifying objects to check for syntax errors before activation.

            Works with any ABAP object type: classes, programs, function modules, interfaces, etc.
            Returns detailed messages with line/column positions for each issue.

            Token cost: ~500-1,000 tokens (depends on number of messages).

            Examples:
            - check_syntax('/sap/bc/adt/oo/classes/zcl_test/source/main', 'inactive')
            - check_syntax('/sap/bc/adt/programs/programs/zrep_test/source/main', 'active')
            - check_syntax('/sap/bc/adt/functions/groups/zfg/fmodules/z_fm', 'inactive')
            """
    )
    public String check_syntax(
        @McpToolParam(description = "ADT URI of the object to check. Obtain from search_objects or construct from object name. Examples: '/sap/bc/adt/oo/classes/zcl_test/source/main', '/sap/bc/adt/programs/programs/zrep_test/source/main'")
        String objectUri,

        @McpToolParam(description = "Version to check: 'active' for activated code, 'inactive' for draft code. Default: 'inactive' (recommended for checking before activation).")
        String version
    ) throws JsonProcessingException {
        SyntaxCheckResult result = syntaxCheckService.checkSyntax(objectUri, version);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
}

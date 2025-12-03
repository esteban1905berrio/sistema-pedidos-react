package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.IncludeSourceResult;
import com.crystal.mcp.sapserver.model.ProgramSourceResult;
import com.crystal.mcp.sapserver.model.ProgramModifyResult;
import com.crystal.mcp.sapserver.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP Program Operations.
 *
 * This component provides tools for working with ABAP programs (reports, module pools, etc.).
 * Part of Progressive Discovery architecture (Stage 3+).
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Progressive Discovery Workflow:
 * Stage 1: search_objects (SearchTools) → Find programs
 * Stage 2: get_object_structure (ObjectTools) → Get program metadata, includes list
 * Stage 3: get_program_source (ProgramTools) → Fetch full program source
 * Stage 3+: get_include_source (ProgramTools) → Fetch individual includes
 *
 * Phase 1 Tools:
 * - get_program_source: Get program source code
 * - get_include_source: Get include source code
 * - modify_program_source: Modify program/include with workflow (LOCK → MODIFY → UNLOCK)
 *
 * Future Tools:
 * - list_program_includes: List all includes in a program
 * - activate_program: Activate program after modification
 * - syntax_check_program: Check program syntax before saving
 */
@Component
@RequiredArgsConstructor
public class ProgramTools {

    private final ProgramService programService;

    /**
     * MCP Tool: Get source code for an ABAP program.
     *
     * This tool implements Progressive Discovery Stage 3 for programs.
     * Use after search_objects identified the program and you need the actual code.
     *
     * Token Optimization:
     * - Stage 1 (search_objects): ~500 tokens → Find programs
     * - Stage 2 (get_object_structure): ~800 tokens → Get program metadata
     * - Stage 3 (get_program_source): ~3,000+ tokens → Get full source (THIS)
     *
     * Use Case:
     * After identifying a program via search, use this tool to:
     * - Get complete program source code
     * - Analyze program logic and structure
     * - Identify which includes to fetch (if needed)
     *
     * Works with:
     * - Reports (e.g., ZREP_INVOICE_LIST)
     * - Module Pools (e.g., SAPMZTEST)
     * - Function Group Main Programs (e.g., SAPLZFG_UTILS)
     *
     * Workflow Example:
     * 1. User: "Find invoice-related programs"
     * 2. Claude: search_objects("*invoice*") → Gets program list
     * 3. User: "Show me ZREP_INVOICE_LIST"
     * 4. Claude: get_program_source("ZREP_INVOICE_LIST") → Gets full source
     *
     * @param programName name of the ABAP program (e.g., "ZREP_INVOICE_LIST")
     * @param version     version to retrieve: "active" (default) or "inactive"
     * @return ProgramSourceResult containing source code
     */
    @McpTool(
            description = "Get source code for an ABAP program (report, module pool, etc.). " +
                    "Progressive Discovery Stage 3 for programs. " +
                    "Use after search_objects identifies the program and you need the actual code. " +
                    "Token cost: ~3,000+ tokens (only use when source code is needed). " +
                    "Example programs: 'ZREP_INVOICE_LIST', 'SAPMZTEST', 'SAPLZFG_UTILS'"
    )
    public ProgramSourceResult get_program_source(
            @McpToolParam(
                    description = "Name of the ABAP program. " +
                            "Examples: 'ZREP_INVOICE_LIST' (report), " +
                            "'SAPMZTEST' (module pool), " +
                            "'SAPLZFG_UTILS' (function group main program)",
                    required = true
            )
            String programName,
            @McpToolParam(
                    description = "Version to retrieve: 'active' for activated code or 'inactive' for draft. " +
                            "Default: 'active'",
                    required = false
            )
            String version
    ) {
        return programService.getProgramSource(programName, version);
    }

    /**
     * MCP Tool: Get source code for a program include.
     *
     * This tool implements Progressive Discovery Stage 3+ for includes.
     * Use after get_object_structure or get_class_includes identified the include.
     *
     * Token Optimization:
     * - More efficient than loading entire program when only one include is needed
     * - Allows parallel fetching of multiple includes
     * - Typical include: 500-2,000 tokens (vs 3,000+ for full program)
     *
     * Use Case:
     * After identifying includes via structure query, use this tool to:
     * - Get specific include source code
     * - Analyze modular ABAP code
     * - Fetch only needed includes (not entire program)
     *
     * Works with:
     * - Top includes (e.g., ZREP_TOP)
     * - Form includes (e.g., ZREP_F01)
     * - Class includes (e.g., ZCL_TEST===============CCAU)
     * - Any program include
     *
     * Workflow Example:
     * 1. User: "What includes does ZREP_INVOICE_LIST have?"
     * 2. Claude: get_object_structure(uri) → Gets includes list
     * 3. User: "Show me the ZREP_INVOICE_TOP include"
     * 4. Claude: get_include_source("ZREP_INVOICE_LIST", "ZREP_INVOICE_TOP")
     *
     * @param programName name of the parent program (e.g., "ZREP_INVOICE_LIST")
     * @param includeName name of the include (e.g., "ZREP_INVOICE_TOP")
     * @param version     version to retrieve: "active" (default) or "inactive"
     * @return IncludeSourceResult containing source code
     */
    @McpTool(
            description = "Get source code for a program include. " +
                    "Progressive Discovery Stage 3+ for includes. " +
                    "Use after get_object_structure or get_class_includes identifies the include. " +
                    "More efficient than loading entire program when only one include is needed. " +
                    "Token cost: ~500-2,000 tokens (depends on include size). " +
                    "Example includes: 'ZREP_TOP', 'ZREP_F01', 'ZCL_TEST===============CCAU'"
    )
    public IncludeSourceResult get_include_source(
            @McpToolParam(
                    description = "Name of the parent ABAP program that contains the include. " +
                            "Examples: 'ZREP_INVOICE_LIST', 'SAPMZTEST', 'ZCL_TEST'",
                    required = true
            )
            String programName,
            @McpToolParam(
                    description = "Name of the include to retrieve. " +
                            "Examples: 'ZREP_TOP' (top include), " +
                            "'ZREP_F01' (form include), " +
                            "'ZCL_TEST===============CCAU' (class auxiliary include)",
                    required = true
            )
            String includeName,
            @McpToolParam(
                    description = "Version to retrieve: 'active' for activated code or 'inactive' for draft. " +
                            "Default: 'active'",
                    required = false
            )
            String version
    ) {
        return programService.getIncludeSource(programName, includeName, version);
    }

    /**
     * MCP Tool: Modify an ABAP program or include source code.
     *
     * This is a workflow-based tool that orchestrates the complete ADT modification flow:
     * LOCK → MODIFY → UNLOCK
     *
     * Workflow Steps:
     * 1. LOCK: Acquires exclusive lock on the object
     *    - Returns transport number (system-assigned or existing)
     *    - Fails if object is already locked by another user
     * 2. MODIFY: Updates source code with new content
     *    - Uses transport from LOCK response
     *    - Validates lock handle before modification
     * 3. UNLOCK: Releases lock (ALWAYS executed, even on failure)
     *    - Critical step to prevent orphaned locks
     *
     * Based on Python implementation: modification_service.py
     * Reference: docs/requirements/mcp/workflow_based/pr_update_program.md
     *
     * Supports both:
     * - Programs: Reports, module pools, function group main programs
     * - Includes: Top includes, form includes, class includes
     *
     * Use Cases:
     * - Update program source code
     * - Fix bugs in includes
     * - Refactor ABAP code
     * - Apply code changes from code reviews
     *
     * Error Handling:
     * - If object is locked by another user: Returns error with lock owner information
     * - If modification fails: Automatically unlocks object (no orphaned locks)
     * - If unlock fails: Returns warning but doesn't fail the overall operation
     *
     * Workflow Example:
     * 1. User: "Modify ZREP_INVOICE_LIST to add new field"
     * 2. Claude: get_program_source("ZREP_INVOICE_LIST") → Gets current source
     * 3. Claude: modify_program_source("ZREP_INVOICE_LIST", new_source, "program")
     * 4. System: LOCK → MODIFY → UNLOCK → Success
     *
     * @param objectName  name of the program or include (e.g., "ZREP_INVOICE_LIST")
     * @param newSource   new source code to set (complete replacement)
     * @param objectType  type of object: "program" or "include"
     * @param transport   optional transport number (if null, uses system-assigned transport from LOCK)
     * @return ProgramModifyResult with detailed workflow execution status
     */
    @McpTool(
            description = "Modify ABAP program or include source code with complete workflow (LOCK → MODIFY → UNLOCK). " +
                    "Workflow-based tool that handles locking, modification, and unlocking automatically. " +
                    "Supports both programs and includes. " +
                    "Returns transport number from lock operation if not provided. " +
                    "Fails with error if object is already locked by another user. " +
                    "Always unlocks object even on failure (prevents orphaned locks). " +
                    "Example: modify_program_source('ZREP_INVOICE', new_code, 'program', null)"
    )
    public ProgramModifyResult modify_program_source(
            @McpToolParam(
                    description = "Name of the program or include to modify. " +
                            "Examples: 'ZREP_INVOICE_LIST' (program), " +
                            "'ZFIAAC002' (program), " +
                            "'ZFIAAC002V_1' (include), " +
                            "'ZREP_TOP' (include)",
                    required = true
            )
            String objectName,
            @McpToolParam(
                    description = "New source code to set (complete replacement). " +
                            "Must be valid ABAP syntax. " +
                            "For programs: Include REPORT/PROGRAM statement. " +
                            "For includes: No REPORT statement needed.",
                    required = true
            )
            String newSource,
            @McpToolParam(
                    description = "Type of object to modify: 'program' or 'include'. " +
                            "'program': Reports, module pools, function group mains. " +
                            "'include': Top includes, form includes, class includes.",
                    required = true
            )
            String objectType,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "If not provided, uses system-assigned transport from LOCK operation. " +
                            "Example: 'CADK911122', 'DEVK900123'. " +
                            "Leave null to use automatic transport assignment.",
                    required = false
            )
            String transport
    ) {
        return programService.modifyProgramSource(objectName, newSource, objectType, transport);
    }

    /**
     * MCP Tool: Modify a function module source code.
     * <p>
     * This is a workflow-based tool that orchestrates the complete ADT modification flow:
     * LOCK → MODIFY → UNLOCK
     * <p>
     * Similar to modify_program_source but specifically for function modules.
     * Function modules have a different URI structure and require both module and group names.
     * <p>
     * Workflow Steps:
     * 1. LOCK: Acquires exclusive lock on the function module
     *    - Returns transport number (system-assigned or existing)
     *    - Fails if object is already locked by another user
     * 2. MODIFY: Updates source code with new content
     *    - Uses transport from LOCK response
     *    - Validates lock handle before modification
     * 3. UNLOCK: Releases lock (ALWAYS executed, even on failure)
     *    - Critical step to prevent orphaned locks
     * <p>
     * Based on Python implementation: modification_service.py::modify_function_module()
     *
     * @param functionModuleName name of the function module (e.g., "Z_TEST_FM")
     * @param functionGroupName  parent function group name (e.g., "ZTEST_FG")
     * @param newSource          new source code to set (complete replacement)
     * @param transport          optional transport number (if null, uses system-assigned transport from LOCK)
     * @return ProgramModifyResult with detailed workflow execution status
     */
    @McpTool(
            description = "Modify ABAP function module source code with complete workflow (LOCK → MODIFY → UNLOCK). " +
                    "Workflow-based tool that handles locking, modification, and unlocking automatically. " +
                    "Requires both function module name and parent function group name. " +
                    "Returns transport number from lock operation if not provided. " +
                    "Fails with error if object is already locked by another user. " +
                    "Always unlocks object even on failure (prevents orphaned locks). " +
                    "\n\n⚠️ CRITICAL:  Signatures must NEVER include comments (*\" blocks). " +
                    "See docs/development_rules/abap_function_module_rules.md for correct signature format. " +
                    "\n\nExample: modify_function_module('Z_TEST_FM', 'ZTEST_FG', new_code, null)"
    )
    public ProgramModifyResult modify_function_module(
            @McpToolParam(
                    description = "Name of the function module to modify. " +
                            "Examples: 'Z_TEST_FM', 'Z_GET_INVOICE', 'Z_PROCESS_ORDER'",
                    required = true
            )
            String functionModuleName,
            @McpToolParam(
                    description = "Parent function group name. " +
                            "Examples: 'ZTEST_FG', 'ZFI_UTILS', 'ZMMI_PROCESS'",
                    required = true
            )
            String functionGroupName,
            @McpToolParam(
                    description = "New source code to set (complete replacement). " +
                            "Must be valid ABAP syntax. " +
                            "Must include FUNCTION/ENDFUNCTION statements. " +
                            "⚠️ IMPORTANT: Do NOT include signature definition or comments (*\") in source. " +
                            "Signatures must be configured separately in SE37.",
                    required = true
            )
            String newSource,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "If not provided, uses system-assigned transport from LOCK operation. " +
                            "Example: 'CADK911122', 'DEVK900123'. " +
                            "Leave null to use automatic transport assignment.",
                    required = false
            )
            String transport
    ) {
        return programService.modifyFunctionModuleSource(functionModuleName, functionGroupName, newSource, transport);
    }
}

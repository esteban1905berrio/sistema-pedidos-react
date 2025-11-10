package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.IncludeSourceResult;
import com.crystal.mcp.sapserver.model.ProgramSourceResult;
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
 *
 * Future Tools:
 * - lock_program: Lock program for editing
 * - unlock_program: Release program lock
 * - set_program_source: Update program source code
 * - list_program_includes: List all includes in a program
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
}

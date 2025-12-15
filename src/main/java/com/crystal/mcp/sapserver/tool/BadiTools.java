package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.BadiImplementationResult;
import com.crystal.mcp.sapserver.service.BadiService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for BAdI Implementation (SXCI) Operations.
 *
 * This component defines MCP (Model Context Protocol) tools that enable
 * LLM agents like Claude to interact with classic BAdI implementations in SAP systems.
 *
 * Classic BAdI vs Enhancement Framework:
 * - Classic BAdI (SXCI): Transaction SE18/SE19, this tool
 * - Enhancement BAdI (ENHO): Use get_enhancement_source tool instead
 *
 * Progressive Discovery Workflow:
 * Stage 1: search_objects (SearchTools) → Find SXCI objects
 * Stage 2: get_badi_implementation (BadiTools) → Get implementation details
 * Stage 3: get_class_source (ClassService) → Get implementing class source code
 *
 * Tools:
 * - get_badi_implementation: Retrieve BAdI implementation metadata and classes
 */
@Component
@RequiredArgsConstructor
public class BadiTools {

    private final BadiService badiService;

    /**
     * MCP Tool: Get BAdI Implementation details.
     *
     * This tool enables Claude to retrieve complete information about a classic
     * BAdI implementation (SXCI object type) from the SAP system.
     *
     * Returns:
     * - Header: implementation name, description, active status, package, author
     * - BAdI Definitions: which BAdIs are implemented, filter values, interfaces
     * - Implementing Classes: classes that implement the BAdI interfaces
     *
     * Classic BAdI tables queried:
     * - SXC_ATTR: Implementation attributes
     * - SXC_EXIT: BAdI definition relationship
     * - SXC_CLASS: Implementing classes
     * - SXS_INTER: BAdI interfaces
     * - SXS_ATTRT: BAdI descriptions
     *
     * Example Claude prompts:
     * - "Get details for BAdI implementation ZTEST_BADI_IMPL"
     * - "What class implements the BAdI BADI_SD_SALES?"
     * - "Show me the filter values for implementation ZFIE1017_BADI"
     *
     * Token cost: ~500-1500 tokens (depends on number of definitions and classes)
     *
     * @param implementationName name of BAdI implementation (e.g., "ZTEST_BADI_IMPL")
     * @return BadiImplementationResult containing header, definitions, and implementing classes
     */
    @McpTool(
            description = "Get classic BAdI Implementation (SXCI) details from SAP system. " +
                    "Returns header metadata (name, description, active status, package), " +
                    "BAdI definitions covered (with filter values and interfaces), " +
                    "and implementing classes. Use this for classic BAdIs (SE18/SE19). " +
                    "For Enhancement Framework BAdIs (ENHO), use get_enhancement_source instead. " +
                    "Token cost: ~500-1500 tokens."
    )
    public BadiImplementationResult get_badi_implementation(
            @McpToolParam(
                    description = "Name of the BAdI implementation (e.g., 'ZTEST_BADI_IMPL', 'ZFIE1017_BADI'). " +
                            "This is the implementation name from SE19, not the BAdI definition name from SE18.",
                    required = true
            )
            String implementationName
    ) {
        return badiService.getBadiImplementation(implementationName);
    }
}

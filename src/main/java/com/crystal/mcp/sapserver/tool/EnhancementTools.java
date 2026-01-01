package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.EnhancementSearchResult;
import com.crystal.mcp.sapserver.model.EnhancementSourceResult;
import com.crystal.mcp.sapserver.service.EnhancementService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for Enhancement Implementation Operations.
 *
 * This component defines MCP (Model Context Protocol) tools that enable
 * LLM agents like Claude to interact with Enhancement Implementations in SAP systems.
 *
 * Enhancement Implementations (ENHO) contain:
 * - Hook Implementations: ABAP code injected at enhancement spots
 * - BAdI Implementations: Classes implementing Business Add-In interfaces
 *
 * Progressive Discovery Workflow:
 * Stage 1: search_enhancements (EnhancementTools) → Find enhancements by pattern
 * Stage 2: get_enhancement_source (EnhancementTools) → Get source code
 *
 * Tools:
 * - search_enhancements: Search enhancement implementations by wildcard pattern
 * - get_enhancement_source: Retrieve enhancement implementation source code
 */
@Component
@RequiredArgsConstructor
public class EnhancementTools {

    private final EnhancementService enhancementService;

    /**
     * MCP Tool: Get Enhancement Implementation source code.
     *
     * This tool enables Claude to retrieve the complete source code and metadata
     * of any Enhancement Implementation (ENHO) from the SAP system.
     *
     * Returns:
     * - Header: enhancement name, description, tool type, package, author
     * - Elements: list of hooks or BAdI implementations with their details
     * - Sources: actual ABAP source code for each element
     *
     * Tool Types:
     * - HOOK_IMPL: Source code enhancement at enhancement spots
     * - BADI_IMPL: BAdI implementation classes
     *
     * Example Claude prompts:
     * - "Get the source code for enhancement ZENH_INVOICE_BADI"
     * - "Show me the hook implementations in enhancement ZSTD_LIEFERBADI"
     *
     * Token cost: ~1000-5000 tokens (depends on number of implementations)
     *
     * @param enhancementName name of Enhancement Implementation (e.g., "ZENH_INVOICE_BADI")
     * @param version version number (default: "00000" for active version)
     * @return EnhancementSourceResult containing header, elements, and source code
     */
    @McpTool(
            description = "Get Enhancement Implementation source code from SAP system. " +
                    "Returns header metadata, implementation elements (hooks/BAdIs), " +
                    "and ABAP source code for each element. " +
                    "Enhancement types: HOOK_IMPL (source code injection) or BADI_IMPL (BAdI classes). " +
                    "Token cost: ~1000-5000 tokens."
    )
    public EnhancementSourceResult get_enhancement_source(
            @McpToolParam(
                    description = "Name of the Enhancement Implementation (e.g., 'ZENH_INVOICE_BADI', 'ZSTD_LIEFERBADI')",
                    required = true
            )
            String enhancementName,
            @McpToolParam(
                    description = "Version number: '00000' for active version (default). Use specific version for historical.",
                    required = false
            )
            String version
    ) {
        String actualVersion = (version != null && !version.isBlank()) ? version : "00000";
        return enhancementService.getEnhancementSource(enhancementName, actualVersion);
    }

    /**
     * MCP Tool: Search Enhancement Implementations by wildcard pattern.
     *
     * This tool enables Claude to discover Enhancement Implementations matching a pattern.
     * Returns enhancement name and the container object (class, function module, or program
     * where the enhancement is implemented).
     *
     * Result interpretation:
     * - enhancementName: The ENHO object name (use with get_enhancement_source)
     * - objectType: Container type - CLAS (class), FUGR (function group), PROG (program)
     * - objectName: The actual class/FM/program name containing the enhancement
     *
     * Example Claude prompts:
     * - "Find all custom enhancements starting with Z"
     * - "Search for enhancements related to invoice processing"
     *
     * Token cost: ~500-2000 tokens (depends on result count)
     *
     * @param pattern wildcard pattern (e.g., "Z*", "*INVOICE*", "*_BADI")
     * @param maxResults maximum results to return (default: 100)
     * @return EnhancementSearchResult containing matching enhancements with container info
     */
    @McpTool(
            description = "Search Enhancement Implementations by wildcard pattern. " +
                    "Returns enhancement name and container object info (class/FM/program that contains it). " +
                    "Supports wildcards: 'Z*' (prefix), '*INVOICE*' (contains), '*_BADI' (suffix). " +
                    "Use objectType/objectName to understand WHERE the enhancement is implemented. " +
                    "Token cost: ~500-2000 tokens."
    )
    public EnhancementSearchResult search_enhancements(
            @McpToolParam(
                    description = "Wildcard pattern to search. Examples: 'Z*' (all custom), '*INVOICE*' (contains), '*_BADI' (suffix)",
                    required = true
            )
            String pattern,
            @McpToolParam(
                    description = "Maximum results to return. Default: 100",
                    required = false
            )
            Integer maxResults
    ) {
        return enhancementService.searchEnhancements(pattern, maxResults);
    }
}

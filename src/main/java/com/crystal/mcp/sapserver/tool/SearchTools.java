package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.SearchResult;
import com.crystal.mcp.sapserver.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP Repository Search Operations.
 *
 * This component implements Progressive Discovery Stage 1: Quick Search.
 * Enables LLM agents to discover ABAP objects without fetching full source code.
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Progressive Discovery Workflow:
 * 1. search_objects → Find objects by keyword (lightweight, ~500 tokens)
 * 2. get_object_structure → Get object metadata without source (~800 tokens)
 * 3. get_object_source → Fetch full source code on demand (~3,000+ tokens)
 *
 * Phase 1 Tools:
 * - search_objects: Quick search by keyword
 *
 * Future Tools (Progressive Discovery):
 * - search_with_filters: Advanced search with type/package/author filters
 * - fuzzy_search: Fuzzy matching for approximate queries
 */
@Component
@RequiredArgsConstructor
public class SearchTools {

    private final SearchService searchService;

    /**
     * MCP Tool: Search for ABAP objects by keyword.
     *
     * This tool implements Progressive Discovery Stage 1: Quick Search.
     * Returns lightweight object references (name, type, URI) without source code.
     *
     * Use Cases:
     * - Initial exploration: "Find all classes starting with ZCL_FI"
     * - Discovery before detail: Find objects first, then fetch specific ones
     * - Token optimization: ~500 tokens vs ~15,000 tokens for full source
     *
     * Supports wildcards:
     * - Prefix: "ZCL_*" or "ZCL_"
     * - Contains: "*payment*"
     * - Suffix: "*_UTIL"
     *
     * Example Claude prompts:
     * - "Search for classes containing 'invoice'"
     * - "Find all Z programs in the system"
     * - "Look for objects with 'payment' in the name"
     *
     * @param query      search keyword (supports wildcards *, e.g., "ZCL_*", "*payment*")
     * @param maxResults maximum results to return (default: 10, no upper limit)
     * @return SearchResult with list of matching objects and their URIs
     */
    @McpTool(
            description = "Search for ABAP objects (classes, programs, function groups, etc.) " +
                    "by keyword. Returns lightweight references (name, type, URI) without source code. " +
                    "Supports wildcards (* for any characters). " +
                    "Part of Progressive Discovery: use this first, then fetch details for specific objects. " +
                    "Examples: 'ZCL_*' (prefix), '*invoice*' (contains), 'CL_ABAP_*' (standard classes)."
    )
    public SearchResult search_objects(
            @McpToolParam(
                    description = "Search keyword or pattern. " +
                            "Supports wildcards: 'ZCL_*' (prefix), '*payment*' (contains), '*_UTIL' (suffix). " +
                            "Examples: 'ZCL_FI', '*invoice*', 'CL_ABAP_CHAR*'",
                    required = true
            )
            String query,
            @McpToolParam(
                    description = "Maximum number of results to return (default: 10, no upper limit). " +
                            "Lower values = faster response and fewer tokens.",
                    required = false
            )
            Integer maxResults
    ) {
        return searchService.searchObjects(query, maxResults);
    }
}

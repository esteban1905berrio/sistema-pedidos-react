package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ExtractionDiscovery;
import com.crystal.mcp.sapserver.model.ExtractionScope;
import com.crystal.mcp.sapserver.service.AbapExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP Object Extraction Operations.
 *
 * <p>This component implements the ABAP Extraction Tool (Phase 6) providing
 * a unified tool for discovering ABAP objects across 4 different scopes:
 * <ul>
 *   <li><strong>user</strong>: Objects created/modified by a specific user (by AUTHOR in TADIR)</li>
 *   <li><strong>package</strong>: Objects from package hierarchy (recursive)</li>
 *   <li><strong>transport</strong>: Objects from specific transport request(s)</li>
 *   <li><strong>list</strong>: Specific objects by name</li>
 * </ul>
 *
 * <p><strong>Workflow Pattern:</strong>
 * <pre>
 * 1. Discovery: extract_abap_objects(scope, input) → Returns summary
 * 2. Review: User reviews discovered objects
 * 3. Extraction: (Future) Execute extraction to filesystem
 * </pre>
 *
 * <p><strong>Spring AI MCP Server</strong> automatically discovers and registers
 * @McpTool methods via component scanning.
 *
 * @see AbapExtractionService
 * @see ExtractionScope
 * @see ExtractionDiscovery
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbapExtractionTools {

    private final AbapExtractionService extractionService;

    /**
     * MCP Tool: Discover ABAP objects for extraction.
     *
     * <p>This tool discovers ABAP objects based on the specified scope and input,
     * returning a summary of discovered objects without performing extraction.
     * Use this to review what will be extracted before proceeding.
     *
     * <p><strong>Scope-specific behavior:</strong>
     *
     * <ul>
     *   <li><strong>user</strong>: Discovers objects by AUTHOR field in TADIR table.
     *       Input: username (optional, defaults to current user).
     *       Example: "DEVELOPER", "L_ABAPS_ITA"</li>
     *
     *   <li><strong>package</strong>: Discovers objects from package and subpackages (recursive).
     *       Input: package name(s), comma-separated.
     *       Example: "ZCX", "ZMMI1229_0,ZFIE1017"</li>
     *
     *   <li><strong>transport</strong>: Discovers objects from transport request(s).
     *       Input: transport number(s), comma-separated.
     *       Example: "CADK911088", "CADK911088,CADK911089"</li>
     *
     *   <li><strong>list</strong>: Discovers specific objects by exact name.
     *       Input: object names, comma-separated (any type).
     *       Example: "ZCL_TEST,ZREP_INVOICE,Z_FM_UTIL"</li>
     * </ul>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>"What objects did I create in SAP?"</li>
     *   <li>"Show me all objects in package ZCX"</li>
     *   <li>"What's in transport CADK911088?"</li>
     *   <li>"Find these specific objects: ZCL_TEST, ZREP_INVOICE"</li>
     * </ul>
     *
     * <p><strong>Token Efficiency:</strong>
     * This tool is optimized for discovery without fetching source code.
     * Typical response: ~1,000-5,000 tokens depending on object count.
     *
     * @param scope extraction scope: "user", "package", "transport", or "list"
     * @param input scope-specific input (see descriptions above)
     * @return ExtractionDiscovery containing discovered objects and summary
     */
    @McpTool(
            description = "Discover ABAP objects for extraction. " +
                    "Returns summary of discovered objects without extracting source code. " +
                    "Scopes: " +
                    "'user' (objects by AUTHOR in TADIR, input: username or empty for current), " +
                    "'package' (objects from package hierarchy, input: package names comma-separated), " +
                    "'transport' (objects from OT(s), input: transport numbers comma-separated), " +
                    "'list' (specific objects by name, input: object names comma-separated). " +
                    "Examples: scope='package' input='ZCX' | scope='transport' input='CADK911088' | " +
                    "scope='list' input='ZCL_TEST,ZREP_INVOICE'. " +
                    "Use for discovery before extraction - review results then proceed."
    )
    public ExtractionDiscovery discover_extraction_objects(
            @McpToolParam(
                    description = "Extraction scope. Values: " +
                            "'user' (objects by AUTHOR in TADIR), " +
                            "'package' (objects from package hierarchy, recursive), " +
                            "'transport' (objects from transport request(s)), " +
                            "'list' (specific objects by name). " +
                            "Examples: 'package', 'transport', 'list', 'user'",
                    required = true
            )
            String scope,
            @McpToolParam(
                    description = "Scope-specific input. " +
                            "For 'user': username or empty for current user (e.g., 'DEVELOPER'). " +
                            "For 'package': package name(s), comma-separated (e.g., 'ZCX', 'ZMMI1229_0,ZFIE1017'). " +
                            "For 'transport': transport number(s), comma-separated (e.g., 'CADK911088'). " +
                            "For 'list': object names, comma-separated (e.g., 'ZCL_TEST,ZREP_INVOICE').",
                    required = true
            )
            String input
    ) {
        log.info("Discovering ABAP objects: scope={}, input={}", scope, input);

        // Parse and validate scope
        ExtractionScope extractionScope = ExtractionScope.fromCode(scope);

        // Delegate to service
        ExtractionDiscovery discovery = extractionService.discover(extractionScope, input);

        log.info("Discovery complete: {} objects found in {} sources",
                discovery.totalObjects(), discovery.sources().size());

        return discovery;
    }
}

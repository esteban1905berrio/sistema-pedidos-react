package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.DmeeTreeResult;
import com.crystal.mcp.sapserver.service.DmeeService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for DMEE (Data Medium Exchange Engine) Tree Operations.
 *
 * This component defines MCP (Model Context Protocol) tools that enable
 * LLM agents like Claude to interact with DMEE payment format trees in SAP systems.
 *
 * DMEE Overview:
 * - DMEE is SAP's framework for defining payment file format structures
 * - Trees define hierarchical layouts for bank payment files
 * - Common formats: ISO 20022, domestic bank formats, SWIFT
 *
 * Use Cases:
 * - Extract DMEE trees for migration (S/4HANA DMEEX ↔ ECC DMEE)
 * - Analyze tree structure and field mappings
 * - Document payment format configurations
 * - Compare tree versions or similar formats
 *
 * Progressive Discovery Workflow:
 * Stage 1: search_objects (SearchTools) → Find DMEE objects by name pattern
 * Stage 2: get_dmee_tree (DmeeTools) → Get complete tree configuration
 *
 * Related Tools:
 * - get_class_source: For exit function modules referenced in nodes
 * - get_ddic_source: For parameter structure definitions
 *
 * Tools:
 * - get_dmee_tree: Retrieve complete DMEE tree configuration with all nodes
 */
@Component
@RequiredArgsConstructor
public class DmeeTools {

    private final DmeeService dmeeService;

    /**
     * MCP Tool: Get DMEE Tree configuration.
     *
     * This tool enables Claude to retrieve complete information about a DMEE
     * payment format tree from the SAP system.
     *
     * Returns:
     * - Header: tree type, ID, version, description, charset, param structure
     * - Nodes: complete hierarchical structure with mapping properties:
     *   - Node ID, technical name, parent relationship
     *   - Node type (ROOT, SEGM, ELEM, etc.)
     *   - Mapping: constant values, source table/field, exit functions
     *   - Conversion rules, data types, lengths
     *
     * SAP Tables queried:
     * - DMEE_TREE: Tree master data
     * - DMEE_TREE_T: Tree texts (language-dependent)
     * - DMEE_TREE_HEAD: Version-specific configuration
     * - DMEE_TREE_NODE: Tree nodes with mapping logic
     * - DMEE_TREE_NODE_T: Node texts (language-dependent)
     *
     * Example Claude prompts:
     * - "Get the DMEE tree PAYM/ZFIE1017_CITIBANAMEX"
     * - "Show me the structure of payment format tree PAYM/US_CGI_ACH"
     * - "Extract DMEE tree PAYM/SEPA_CT for migration"
     * - "What nodes use exit functions in tree PAYM/ZFIE1017?"
     *
     * Token cost: ~500-5000 tokens (depends on number of nodes, typically 50-200 nodes)
     *
     * @param treeType DMEE tree type (e.g., "PAYM" for payment formats)
     * @param treeId DMEE tree ID (e.g., "ZFIE1017_CITIBANAMEX", "SEPA_CT")
     * @param version optional version number (null = latest active version)
     * @param language optional language for texts (default: system language, e.g., "EN", "ES")
     * @return DmeeTreeResult containing header and all tree nodes
     */
    @McpTool(
            description = "Get DMEE (Data Medium Exchange Engine) payment format tree from SAP system. " +
                    "Returns complete tree configuration including header metadata (type, ID, version, charset) " +
                    "and all tree nodes with hierarchical structure and mapping properties " +
                    "(source tables/fields, exit functions, conversion rules). " +
                    "Use for extracting payment format definitions, analyzing tree structure, " +
                    "or migrating formats between S/4HANA and ECC systems. " +
                    "Token cost: ~500-5000 tokens (depends on tree size)."
    )
    public DmeeTreeResult get_dmee_tree(
            @McpToolParam(
                    description = "DMEE tree type. Common values: 'PAYM' (payment formats), " +
                            "'STMT' (bank statements). Usually 'PAYM' for payment file formats.",
                    required = true
            )
            String treeType,

            @McpToolParam(
                    description = "DMEE tree ID. Examples: 'ZFIE1017_CITIBANAMEX' (custom), " +
                            "'SEPA_CT' (SEPA Credit Transfer), 'US_CGI_ACH' (US ACH format). " +
                            "Custom trees usually start with Z.",
                    required = true
            )
            String treeId,

            @McpToolParam(
                    description = "Optional version number. If not provided, retrieves the latest version. " +
                            "Format: numeric string like '001', '002'. Leave empty for latest.",
                    required = false
            )
            String version,

            @McpToolParam(
                    description = "Optional language for texts. Examples: 'EN' (English), 'ES' (Spanish), " +
                            "'DE' (German). If not provided, uses system language (SY-LANGU).",
                    required = false
            )
            String language
    ) {
        return dmeeService.getDmeeTree(treeType, treeId, version, language);
    }
}

package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.CdsSourceResult;
import com.crystal.mcp.sapserver.service.CdsService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for CDS View Operations.
 *
 * This component defines MCP (Model Context Protocol) tools that enable
 * LLM agents like Claude to interact with CDS Views in SAP systems.
 *
 * CDS Views (Core Data Services) are the foundation of SAP's modern data modeling:
 * - Used extensively in S/4HANA for analytical and transactional scenarios
 * - Define data models with annotations for Fiori UI generation
 * - Support associations, aggregations, and complex expressions
 *
 * Progressive Discovery Workflow:
 * Stage 1: search_objects (SearchTools) → Find CDS Views (type DDLS)
 * Stage 2: get_object_structure (ObjectTools) → Get CDS metadata
 * Stage 3: get_cds_source (CdsTools) → Get source code
 *
 * Tools:
 * - get_cds_source: Retrieve CDS View DDL source code
 */
@Component
@RequiredArgsConstructor
public class CdsTools {

    private final CdsService cdsService;

    /**
     * MCP Tool: Get CDS View source code.
     *
     * This tool enables Claude to retrieve the complete DDL source definition
     * of any CDS View from the SAP system.
     *
     * The source includes:
     * - Annotations (@AbapCatalog, @UI, @Analytics, @ObjectModel, etc.)
     * - View definition with SELECT statement
     * - Associations and compositions
     * - Parameters (if parameterized view)
     *
     * Example Claude prompts:
     * - "Use get_cds_source to fetch the source for I_BUSINESSPARTNER"
     * - "Get the CDS View source for ZCDS_INVOICE"
     *
     * Token cost: ~500-3000 tokens (depends on view complexity)
     *
     * @param cdsName name of CDS View (e.g., "I_BUSINESSPARTNER", "ZCDS_INVOICE")
     * @param version version to retrieve: "active" (default) or "inactive"
     * @return CdsSourceResult containing DDL source code and metadata
     */
    @McpTool(
            description = "Get CDS View source code from SAP system. " +
                    "Returns the complete DDL definition including annotations, SELECT statement, " +
                    "associations, and parameters. CDS Views are the foundation of S/4HANA data modeling. " +
                    "Token cost: ~500-3000 tokens."
    )
    public CdsSourceResult get_cds_source(
            @McpToolParam(
                    description = "Name of the CDS View (e.g., 'I_BUSINESSPARTNER', 'ZCDS_INVOICE', 'I_PRODUCT')",
                    required = true
            )
            String cdsName,
            @McpToolParam(
                    description = "Version to retrieve: 'active' for activated code or 'inactive' for draft. Default: 'active'",
                    required = false
            )
            String version
    ) {
        // Apply default
        String actualVersion = (version != null && !version.isBlank()) ? version : "active";

        return cdsService.getCdsSource(cdsName, actualVersion);
    }
}

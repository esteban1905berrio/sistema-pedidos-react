package com.crystal.mcp.sapserver.model;

import java.util.Map;

/**
 * Data Transfer Object for CDS View source code results.
 *
 * This immutable record represents the result of fetching CDS View source code
 * from the SAP system via the get_cds_source MCP tool.
 *
 * CDS Views (Core Data Services) are the foundation of SAP's modern data modeling
 * approach, used extensively in S/4HANA for analytical and transactional scenarios.
 *
 * @param source      complete CDS View source code (DDL definition)
 * @param cdsName     name of the CDS View (e.g., "I_BUSINESSPARTNER", "ZCDS_INVOICE")
 * @param version     version retrieved (active or inactive)
 * @param objectType  always "DDLS" for CDS Views
 * @param metadata    additional metadata (annotations, associations, etc.)
 */
public record CdsSourceResult(
        String source,
        String cdsName,
        String version,
        String objectType,
        Map<String, Object> metadata
) {
    /**
     * Creates a CdsSourceResult with default objectType "DDLS".
     */
    public CdsSourceResult(String source, String cdsName, String version, Map<String, Object> metadata) {
        this(source, cdsName, version, "DDLS", metadata);
    }
}

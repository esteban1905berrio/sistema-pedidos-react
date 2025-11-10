package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Data Transfer Object for ABAP repository search results.
 *
 * This immutable record represents the result of searching ABAP objects
 * from the SAP system via the search_objects MCP tool.
 *
 * Implements Progressive Discovery Stage 1: Quick Search
 * - Returns lightweight object references (no source code)
 * - Ideal for initial discovery before fetching full details
 *
 * @param query       original search query
 * @param maxResults  maximum results requested
 * @param totalFound  total objects found (may be > results size)
 * @param results     list of found ABAP objects
 */
public record SearchResult(
        String query,
        int maxResults,
        int totalFound,
        List<ObjectReference> results
) {
    /**
     * Individual ABAP object reference from search.
     *
     * @param name        object name (e.g., "ZCL_TEST_CLASS")
     * @param type        object type (e.g., "CLAS/OC", "PROG/P")
     * @param uri         ADT URI for object access
     * @param description short description
     * @param packageName package containing the object
     */
    public record ObjectReference(
            String name,
            String type,
            String uri,
            String description,
            String packageName
    ) {
    }
}

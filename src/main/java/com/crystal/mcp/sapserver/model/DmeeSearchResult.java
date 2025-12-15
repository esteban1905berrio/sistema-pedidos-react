package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result model for DMEE Tree wildcard search.
 *
 * Returns lightweight list of matching DMEE trees when searching
 * with wildcard patterns (e.g., "Z*", "*SEPA*").
 *
 * Use Cases:
 * - Discover DMEE payment format trees by pattern before fetching full details
 * - Progressive Discovery Stage 1 for DMEE objects
 *
 * @param treeType     DMEE tree type filter (e.g., "PAYM")
 * @param pattern      original search pattern (with wildcards)
 * @param maxResults   maximum results requested
 * @param totalFound   total trees found
 * @param results      list of matching DMEE trees
 */
public record DmeeSearchResult(
        String treeType,
        String pattern,
        int maxResults,
        int totalFound,
        List<DmeeReference> results
) {
    /**
     * Individual DMEE tree reference from search.
     *
     * @param treeType    DMEE tree type (e.g., "PAYM")
     * @param treeId      DMEE tree ID
     * @param description description from DMEE_TREE_T table
     */
    public record DmeeReference(
            String treeType,
            String treeId,
            String description
    ) {
    }
}

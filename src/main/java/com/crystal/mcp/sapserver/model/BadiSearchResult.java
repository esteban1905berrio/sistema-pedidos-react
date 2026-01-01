package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result model for BAdI Implementation wildcard search.
 *
 * Returns lightweight list of matching BAdI implementation names when searching
 * with wildcard patterns (e.g., "Z*", "*BADI*").
 *
 * Use Cases:
 * - Discover BAdI implementations by pattern before fetching full details
 * - Progressive Discovery Stage 1 for SXCI objects
 *
 * @param pattern      original search pattern (with wildcards)
 * @param maxResults   maximum results requested
 * @param totalFound   total implementations found
 * @param results      list of matching BAdI implementations
 */
public record BadiSearchResult(
        String pattern,
        int maxResults,
        int totalFound,
        List<BadiReference> results
) {
    /**
     * Individual BAdI implementation reference from search.
     *
     * @param implementationName name of the BAdI implementation
     * @param description        description from SXC_ATTR table
     */
    public record BadiReference(
            String implementationName,
            String description
    ) {
    }
}

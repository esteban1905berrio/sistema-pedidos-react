package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result model for Enhancement Implementation wildcard search.
 *
 * Returns lightweight list of matching enhancement names when searching
 * with wildcard patterns (e.g., "Z*", "*INVOICE*").
 *
 * Use Cases:
 * - Discover enhancements by pattern before fetching full details
 * - Progressive Discovery Stage 1 for Enhancement objects
 * - Identify which ABAP object (class, FM, program) contains the enhancement
 *
 * @param pattern      original search pattern (with wildcards)
 * @param maxResults   maximum results requested
 * @param totalFound   total enhancements found
 * @param results      list of matching enhancements
 */
public record EnhancementSearchResult(
        String pattern,
        int maxResults,
        int totalFound,
        List<EnhancementReference> results
) {
    /**
     * Individual enhancement reference from search.
     *
     * Contains enhancement name plus the container object information,
     * which tells you WHERE the enhancement is implemented (class, FM, program, etc.)
     *
     * @param enhancementName name of the enhancement implementation (ENHO name)
     * @param objectType      type of container object (CLAS, FUGR, PROG, etc.)
     * @param objectName      name of container object (class name, FM name, program name)
     */
    public record EnhancementReference(
            String enhancementName,
            String objectType,
            String objectName
    ) {
    }
}

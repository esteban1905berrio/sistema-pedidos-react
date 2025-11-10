package com.crystal.mcp.sapserver.model;

import java.util.Map;

/**
 * Data Transfer Object for generic ABAP object source code.
 *
 * This immutable record represents source code retrieved for any ABAP object
 * via a generic URI (Progressive Discovery Stage 3).
 *
 * Unlike ClassSourceResult which is class-specific, this DTO handles
 * any ABAP object type: classes, programs, function groups, interfaces, etc.
 *
 * @param source   complete ABAP source code
 * @param uri      ADT URI used to fetch the source
 * @param version  version retrieved (active or inactive)
 * @param metadata additional metadata (extensible)
 */
public record ObjectSourceResult(
        String source,
        String uri,
        String version,
        Map<String, Object> metadata
) {
}

package com.crystal.mcp.sapserver.model;

import java.util.Map;

/**
 * Data Transfer Object for ABAP include source code.
 *
 * This immutable record represents source code retrieved for a program include.
 * Includes are modular ABAP code units that are included in main programs.
 *
 * Progressive Discovery Stage 3:
 * Use this after identifying includes via get_object_structure or get_class_includes
 * to retrieve the full include source code.
 *
 * @param source      complete ABAP include source code
 * @param programName name of the parent program
 * @param includeName name of the include (e.g., "ZTEST_INCLUDE_TOP")
 * @param version     version retrieved (active or inactive)
 * @param metadata    additional metadata (extensible)
 */
public record IncludeSourceResult(
        String source,
        String programName,
        String includeName,
        String version,
        Map<String, Object> metadata
) {
}

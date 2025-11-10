package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Data Transfer Object for ABAP class includes information.
 *
 * This record represents the list of includes that exist for a class.
 * Classes in ABAP are split into multiple include programs:
 * - definitions: Class definition (attributes, method declarations)
 * - implementations: Method implementations
 * - testclasses: Unit test classes
 * - macros: Macro definitions
 *
 * Progressive Discovery Stage 2.5:
 * Use this after get_object_structure to identify which includes exist,
 * then optionally fetch specific includes with get_include_source.
 *
 * Token Optimization:
 * This method checks existence without fetching source code (~200 tokens),
 * allowing selective fetching of only needed includes.
 *
 * @param className     name of the class (e.g., "ZCL_TEST")
 * @param totalIncludes total number of includes found
 * @param includes      list of include definitions
 */
public record ClassIncludeResult(
        String className,
        int totalIncludes,
        List<Include> includes
) {
    /**
     * Individual include definition for a class.
     *
     * @param includeType type of include (definitions, implementations, testclasses, macros)
     * @param uri         ADT URI to access the include
     * @param exists      whether this include exists in the system
     * @param sizeBytes   size of include source in bytes (0 if not checked)
     */
    public record Include(
            String includeType,
            String uri,
            boolean exists,
            long sizeBytes
    ) {
    }
}

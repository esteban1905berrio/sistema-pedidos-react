package com.crystal.mcp.sapserver.model;

import java.util.Map;

/**
 * Data Transfer Object for ABAP class source code results.
 *
 * This immutable record represents the result of fetching ABAP class source code
 * from the SAP system via the get_class_source MCP tool.
 *
 * Java records (Java 17+) provide:
 * - Automatic constructor, getters, equals(), hashCode(), toString()
 * - Immutability (all fields are final)
 * - Concise syntax perfect for DTOs
 *
 * @param source      complete ABAP source code
 * @param className   name of the ABAP class
 * @param version     version retrieved (active or inactive)
 * @param includeType include type (main, implementation, testclasses, macros)
 * @param metadata    additional metadata (extensible for future use)
 */
public record ClassSourceResult(
        String source,
        String className,
        String version,
        String includeType,
        Map<String, Object> metadata
) {
}

package com.crystal.mcp.sapserver.model;

import java.util.Map;

/**
 * Data Transfer Object for ABAP program source code.
 *
 * This immutable record represents source code retrieved for an ABAP program.
 * Programs are executable ABAP units that can contain includes, subroutines,
 * and other program logic.
 *
 * Progressive Discovery Stage 3:
 * Use this after search_objects (Stage 1) and get_object_structure (Stage 2)
 * to retrieve the full program source code.
 *
 * @param source      complete ABAP program source code
 * @param programName name of the program (e.g., "ZTEST_PROGRAM")
 * @param version     version retrieved (active or inactive)
 * @param metadata    additional metadata (extensible)
 */
public record ProgramSourceResult(
        String source,
        String programName,
        String version,
        Map<String, Object> metadata
) {
}

package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Data Transfer Object for ABAP object structure/metadata.
 *
 * Implements Progressive Discovery Stage 2: Get Structure Without Source.
 * Returns object metadata (components, methods, attributes) without source code.
 *
 * Token Optimization:
 * - Stage 1 (search_objects): ~500 tokens → Find objects
 * - Stage 2 (get_object_structure): ~800 tokens → Get metadata ← THIS
 * - Stage 3 (get_object_source): ~3,000+ tokens → Get full source
 *
 * @param name        object name
 * @param type        object type (CLAS/OC, PROG/P, FUGR/F, etc.)
 * @param uri         ADT URI for the object
 * @param description object description
 * @param components  list of object components (methods, attributes, etc.)
 * @param links       related links (documentation, includes, etc.)
 */
public record ObjectStructure(
        String name,
        String type,
        String uri,
        String description,
        List<Component> components,
        List<Link> links
) {
    /**
     * Individual component within an ABAP object.
     *
     * Examples:
     * - For classes: methods, attributes, types
     * - For programs: includes, subroutines
     * - For function groups: function modules
     *
     * @param name        component name (e.g., "EXECUTE", "PROCESS_DATA")
     * @param type        component type (METHOD, ATTRIBUTE, INCLUDE, etc.)
     * @param uri         ADT URI to access component details
     * @param description component description
     * @param links       component-specific links
     */
    public record Component(
            String name,
            String type,
            String uri,
            String description,
            List<Link> links
    ) {
    }

    /**
     * Related link for object or component.
     *
     * @param rel  relationship type (e.g., "http://www.sap.com/adt/relations/documentation")
     * @param href link URL
     */
    public record Link(
            String rel,
            String href
    ) {
    }
}

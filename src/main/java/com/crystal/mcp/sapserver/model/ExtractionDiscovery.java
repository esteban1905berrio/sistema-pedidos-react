package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.Map;

/**
 * Result of ABAP extraction discovery phase.
 *
 * <p>Contains information about objects discovered for extraction,
 * organized by source (packages, transports, etc.) and object type.
 *
 * <p>This is returned by discovery methods before actual extraction,
 * allowing the user to review and approve before proceeding.
 *
 * @param scope           extraction scope used for discovery
 * @param scopeInput      original input (package names, transport numbers, etc.)
 * @param sources         list of sources discovered (packages or transports)
 * @param objectsByType   objects grouped by type with counts
 * @param totalObjects    total count of objects to extract
 * @param objects         flat list of all discovered objects
 * @param estimatedSizeMb estimated extraction size in MB
 * @param warnings        any warnings during discovery (e.g., empty packages)
 */
public record ExtractionDiscovery(
        ExtractionScope scope,
        String scopeInput,
        List<DiscoverySource> sources,
        Map<String, ObjectTypeInfo> objectsByType,
        int totalObjects,
        List<DiscoveredObject> objects,
        double estimatedSizeMb,
        List<String> warnings
) {

    /**
     * A source of objects (package or transport).
     *
     * @param type       source type: "package" or "transport"
     * @param name       source name (package name or transport number)
     * @param objectCount number of objects from this source
     */
    public record DiscoverySource(
            String type,
            String name,
            int objectCount
    ) {}

    /**
     * Information about an object type category.
     *
     * @param type        object type code (CLAS, PROG, FUGR, etc.)
     * @param typeText    human-readable type name
     * @param count       number of objects of this type
     * @param objectNames list of object names (for summary display)
     */
    public record ObjectTypeInfo(
            String type,
            String typeText,
            int count,
            List<String> objectNames
    ) {}

    /**
     * A discovered object ready for extraction.
     *
     * @param pgmid       program ID (R3TR, LIMU, etc.)
     * @param objectType  object type (CLAS, PROG, FUGR, FUNC, etc.)
     * @param objectName  object name
     * @param devclass    development class/package
     * @param author      object author (from TADIR)
     * @param description object description (if available)
     * @param uri         ADT URI for source retrieval
     * @param source      source name (package or transport)
     */
    public record DiscoveredObject(
            String pgmid,
            String objectType,
            String objectName,
            String devclass,
            String author,
            String description,
            String uri,
            String source
    ) {}

    /**
     * Get human-readable type name for an object type code.
     *
     * @param typeCode object type code
     * @return human-readable name
     */
    public static String getTypeText(String typeCode) {
        return switch (typeCode) {
            case "CLAS" -> "Class";
            case "INTF" -> "Interface";
            case "PROG" -> "Program";
            case "FUGR" -> "Function Group";
            case "FUNC" -> "Function Module";
            case "TABL" -> "Table/Structure";
            case "DTEL" -> "Data Element";
            case "DOMA" -> "Domain";
            case "TTYP" -> "Table Type";
            case "SHLP" -> "Search Help";
            case "DDLS" -> "CDS View";
            case "DCLS" -> "Access Control";
            case "ENHO" -> "Enhancement Implementation";
            case "ENHS" -> "Enhancement Spot";
            case "SXCI" -> "BAdI Implementation";
            case "DMEE" -> "DMEE Tree";
            case "SSFO" -> "Smart Form";
            case "SFPF" -> "Adobe Form";
            default -> typeCode;
        };
    }

    /**
     * Check if discovery found any objects.
     *
     * @return true if objects were discovered
     */
    public boolean hasObjects() {
        return totalObjects > 0;
    }

    /**
     * Get summary text for display.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Scope: ").append(scope.getDisplayName()).append("\n");
        sb.append("Input: ").append(scopeInput).append("\n");
        sb.append("Sources: ").append(sources.size()).append("\n");
        sb.append("Total Objects: ").append(totalObjects).append("\n");
        sb.append("Estimated Size: ").append(String.format("%.2f MB", estimatedSizeMb)).append("\n");

        if (!objectsByType.isEmpty()) {
            sb.append("\nObjects by Type:\n");
            objectsByType.forEach((type, info) ->
                    sb.append("  - ").append(info.typeText())
                      .append(": ").append(info.count()).append("\n")
            );
        }

        if (!warnings.isEmpty()) {
            sb.append("\nWarnings:\n");
            warnings.forEach(w -> sb.append("  ! ").append(w).append("\n"));
        }

        return sb.toString();
    }
}

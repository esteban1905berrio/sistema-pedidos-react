package com.crystal.mcp.sapserver.model;

/**
 * Request model for creating a transport copy.
 *
 * <p>Represents the input parameters for creating a transport of copies from an existing
 * transport request. Transport copies are used to move the same objects to multiple systems
 * or create backups of transport contents.
 *
 * @param sourceTransport Source transport request number (e.g., "CADK911511", "DEVK900123").
 *                        The tool will automatically find all related tasks.
 * @param targetSystem Target system name. Must match source transport's target system.
 *                     Examples: "S4D", "S4Q", "S4P". If null, uses source transport's target.
 * @param descriptionPrefix Prefix for transport description (optional).
 *                          Final description format: "&lt;prefix&gt;: &lt;original_description&gt;".
 *                          Max 60 chars total. Default: "COPIA"
 * @param autoRelease Auto-release transport after creation (optional).
 *                    true: Release automatically, false: Keep modifiable. Default: true
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 */
public record TransportCopyRequest(
    String sourceTransport,
    String targetSystem,
    String descriptionPrefix,
    boolean autoRelease
) {
    /**
     * Creates a TransportCopyRequest with default values.
     *
     * @param sourceTransport Source transport request number
     * @return TransportCopyRequest with default target system (null), prefix ("COPIA"), and autoRelease (true)
     */
    public static TransportCopyRequest withDefaults(String sourceTransport) {
        return new TransportCopyRequest(sourceTransport, null, "COPIA", true);
    }

    /**
     * Creates a TransportCopyRequest without auto-release.
     *
     * @param sourceTransport Source transport request number
     * @param targetSystem Target system name
     * @param descriptionPrefix Description prefix
     * @return TransportCopyRequest with autoRelease set to false
     */
    public static TransportCopyRequest withoutRelease(
            String sourceTransport,
            String targetSystem,
            String descriptionPrefix) {
        return new TransportCopyRequest(sourceTransport, targetSystem, descriptionPrefix, false);
    }

    /**
     * Validates the request parameters.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (sourceTransport == null || sourceTransport.trim().isEmpty()) {
            throw new IllegalArgumentException("Source transport number is required");
        }

        if (descriptionPrefix != null && descriptionPrefix.length() > 50) {
            throw new IllegalArgumentException(
                "Description prefix too long (max 50 chars to allow space for original description)"
            );
        }
    }

    /**
     * Returns the source transport in uppercase (SAP convention).
     */
    public String getSourceTransportUpperCase() {
        return sourceTransport != null ? sourceTransport.toUpperCase() : null;
    }

    /**
     * Returns the target system in uppercase (SAP convention).
     */
    public String getTargetSystemUpperCase() {
        return targetSystem != null ? targetSystem.toUpperCase() : null;
    }

    /**
     * Returns the description prefix or default "COPIA" if null.
     */
    public String getDescriptionPrefixOrDefault() {
        return descriptionPrefix != null && !descriptionPrefix.trim().isEmpty()
            ? descriptionPrefix
            : "COPIA";
    }
}

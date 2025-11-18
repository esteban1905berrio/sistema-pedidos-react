package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Request model for creating a transport copy.
 *
 * <p>Represents the input parameters for creating a transport of copies from an existing
 * transport request or multiple transport requests. Transport copies are used to move the
 * same objects to multiple systems or create backups of transport contents.
 *
 * <p><b>Single Transport Mode:</b> Use {@code sourceTransport} parameter
 * <p><b>Multiple Transport Mode:</b> Use {@code sourceTransports} list parameter
 *
 * @param sourceTransport Single source transport request number (e.g., "CADK911511", "DEVK900123").
 *                        The tool will automatically find all related tasks.
 *                        Use this OR sourceTransports, not both.
 * @param sourceTransports List of source transport request numbers for batch processing.
 *                         Example: ["CADK911511", "CADK911512", "CADK911513"].
 *                         Use this OR sourceTransport, not both.
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
    List<String> sourceTransports,
    String targetSystem,
    String descriptionPrefix,
    boolean autoRelease
) {
    /**
     * Creates a TransportCopyRequest with default values (single transport).
     *
     * @param sourceTransport Source transport request number
     * @return TransportCopyRequest with default target system (null), prefix ("COPIA"), and autoRelease (true)
     */
    public static TransportCopyRequest withDefaults(String sourceTransport) {
        return new TransportCopyRequest(sourceTransport, null, null, "COPIA", true);
    }

    /**
     * Creates a TransportCopyRequest with default values (multiple transports).
     *
     * @param sourceTransports List of source transport request numbers
     * @return TransportCopyRequest with default target system (null), prefix ("COPIA"), and autoRelease (true)
     */
    public static TransportCopyRequest withDefaults(List<String> sourceTransports) {
        return new TransportCopyRequest(null, sourceTransports, null, "COPIA", true);
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
        return new TransportCopyRequest(sourceTransport, null, targetSystem, descriptionPrefix, false);
    }

    /**
     * Validates the request parameters.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        // Must have either sourceTransport OR sourceTransports, not both
        boolean hasSingle = sourceTransport != null && !sourceTransport.trim().isEmpty();
        boolean hasMultiple = sourceTransports != null && !sourceTransports.isEmpty();

        if (!hasSingle && !hasMultiple) {
            throw new IllegalArgumentException(
                "Either sourceTransport or sourceTransports is required"
            );
        }

        if (hasSingle && hasMultiple) {
            throw new IllegalArgumentException(
                "Cannot use both sourceTransport and sourceTransports. Use one or the other."
            );
        }

        // Validate multiple transports if provided
        if (hasMultiple) {
            for (String transport : sourceTransports) {
                if (transport == null || transport.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "All transport numbers in sourceTransports must be non-empty"
                    );
                }
            }
        }

        if (descriptionPrefix != null && descriptionPrefix.length() > 50) {
            throw new IllegalArgumentException(
                "Description prefix too long (max 50 chars to allow space for original description)"
            );
        }
    }

    /**
     * Returns the source transport(s) as a comma-separated string in uppercase (SAP convention).
     *
     * <p>This method handles both single and multiple transport modes:
     * <ul>
     *   <li>Single mode: Returns "CADK911511"</li>
     *   <li>Multiple mode: Returns "CADK911511,CADK911512,CADK911513"</li>
     * </ul>
     *
     * <p><b>IMPORTANT:</b> The comma-separated string is sent directly to the SAP Function Module
     * {@code ZCX_CREATE_TRANSPORT_COPY} in parameter {@code IV_TRANSPORT_REQUEST}.
     * The FM is responsible for parsing the comma-separated values.
     *
     * @return Comma-separated transport numbers in uppercase, or null if none provided
     */
    public String getSourceTransportUpperCase() {
        if (sourceTransport != null && !sourceTransport.trim().isEmpty()) {
            // Single transport mode
            return sourceTransport.toUpperCase();
        } else if (sourceTransports != null && !sourceTransports.isEmpty()) {
            // Multiple transports mode - join with comma
            return sourceTransports.stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining(","));
        }
        return null;
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

    /**
     * Checks if this request is for multiple transports.
     *
     * @return true if sourceTransports is used, false if sourceTransport is used
     */
    public boolean isMultipleTransports() {
        return sourceTransports != null && !sourceTransports.isEmpty();
    }

    /**
     * Returns the count of transports in this request.
     *
     * @return Number of transports (1 for single mode, N for multiple mode)
     */
    public int getTransportCount() {
        if (isMultipleTransports()) {
            return sourceTransports.size();
        } else if (sourceTransport != null && !sourceTransport.trim().isEmpty()) {
            return 1;
        }
        return 0;
    }
}

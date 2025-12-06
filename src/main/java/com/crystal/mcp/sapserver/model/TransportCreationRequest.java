package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Request model for creating a new transport request.
 *
 * <p>Represents the input parameters for creating a transport request
 * (Workbench, Customizing, or Transport of Copies).
 *
 * @param requestType Type of transport: 'K' (Workbench), 'W' (Customizing), 'T' (Copy)
 * @param description Transport description (max 60 chars). Optional if inheritDescription=true.
 * @param targetSystem Target system name (e.g., "S4D", "S4Q", "S4P"). Optional.
 * @param referenceTransport Reference transport to copy objects from. Optional.
 * @param autoRelease Auto-release transport after creation. Default: false.
 * @param objects List of objects to include in the transport. Optional.
 * @param inheritDescription If true, inherit description from reference transport. Default: false.
 *
 * @author Crystal Development Team
 * @since 2025-12-02
 */
public record TransportCreationRequest(
    String requestType,
    String description,
    String targetSystem,
    String referenceTransport,
    boolean autoRelease,
    List<TransportObject> objects,
    boolean inheritDescription
) {
    /**
     * Represents an object to include in the transport.
     *
     * <p>Only objName is required. If pgmid and/or object are not provided,
     * the SAP function module will look them up in the TADIR table.
     *
     * @param pgmid Program ID (e.g., "R3TR", "LIMU"). Optional - auto-detected from TADIR if not provided.
     * @param object Object type (e.g., "PROG", "CLAS", "FUNC"). Optional - auto-detected from TADIR if not provided.
     * @param objName Object name. Required.
     */
    public record TransportObject(String pgmid, String object, String objName) {
        /**
         * Creates a TransportObject with only the name (type auto-detected from TADIR).
         */
        public static TransportObject withName(String objName) {
            return new TransportObject(null, null, objName);
        }

        /**
         * Creates a TransportObject with full specification.
         */
        public static TransportObject withFullSpec(String pgmid, String object, String objName) {
            return new TransportObject(pgmid, object, objName);
        }

        /**
         * Validates the transport object.
         * Only objName is required - pgmid and object can be auto-detected.
         */
        public void validate() {
            if (objName == null || objName.trim().isEmpty()) {
                throw new IllegalArgumentException("OBJ_NAME is required for transport object");
            }
        }
    }
    /**
     * Creates a Workbench transport request.
     */
    public static TransportCreationRequest workbench(String description, String targetSystem) {
        return new TransportCreationRequest("K", description, targetSystem, null, false, null, false);
    }

    /**
     * Creates a Workbench transport request with objects.
     */
    public static TransportCreationRequest workbenchWithObjects(
            String description,
            String targetSystem,
            List<TransportObject> objects) {
        return new TransportCreationRequest("K", description, targetSystem, null, false, objects, false);
    }

    /**
     * Creates a Customizing transport request.
     */
    public static TransportCreationRequest customizing(String description, String targetSystem) {
        return new TransportCreationRequest("W", description, targetSystem, null, false, null, false);
    }

    /**
     * Creates a Transport of Copies with reference.
     */
    public static TransportCreationRequest copyWithReference(
            String description,
            String targetSystem,
            String referenceTransport,
            boolean autoRelease) {
        return new TransportCreationRequest("T", description, targetSystem, referenceTransport, autoRelease, null, false);
    }

    /**
     * Creates a Transport of Copies with reference, inheriting the description.
     */
    public static TransportCreationRequest copyWithReferenceInheritDescription(
            String targetSystem,
            String referenceTransport,
            boolean autoRelease) {
        return new TransportCreationRequest("T", null, targetSystem, referenceTransport, autoRelease, null, true);
    }

    /**
     * Validates the request parameters.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (requestType == null || requestType.trim().isEmpty()) {
            throw new IllegalArgumentException("Request type is required");
        }

        String type = requestType.toUpperCase();
        if (!type.equals("K") && !type.equals("W") && !type.equals("T")) {
            throw new IllegalArgumentException(
                "Invalid request type: " + requestType + ". Use K (Workbench), W (Customizing), or T (Copy)"
            );
        }

        // Description is required unless inheriting from reference
        if (!inheritDescription && (description == null || description.trim().isEmpty())) {
            throw new IllegalArgumentException("Description is required (or use inheritDescription with a reference transport)");
        }

        if (description != null && description.length() > 60) {
            throw new IllegalArgumentException("Description too long (max 60 chars)");
        }

        // inheritDescription requires a reference transport
        if (inheritDescription && (referenceTransport == null || referenceTransport.trim().isEmpty())) {
            throw new IllegalArgumentException("inheritDescription requires a referenceTransport");
        }

        // Validate objects if provided
        if (objects != null && !objects.isEmpty()) {
            for (TransportObject obj : objects) {
                obj.validate();
            }
        }
    }

    /**
     * Returns the request type in uppercase.
     */
    public String getRequestTypeUpperCase() {
        return requestType != null ? requestType.toUpperCase() : null;
    }

    /**
     * Returns the target system in uppercase.
     */
    public String getTargetSystemUpperCase() {
        return targetSystem != null ? targetSystem.toUpperCase() : null;
    }

    /**
     * Returns the reference transport in uppercase.
     */
    public String getReferenceTransportUpperCase() {
        return referenceTransport != null ? referenceTransport.toUpperCase() : null;
    }

    /**
     * Returns human-readable request type description.
     */
    public String getRequestTypeDescription() {
        if (requestType == null) return "Unknown";
        return switch (requestType.toUpperCase()) {
            case "K" -> "Workbench";
            case "W" -> "Customizing";
            case "T" -> "Transport of Copies";
            default -> requestType;
        };
    }
}

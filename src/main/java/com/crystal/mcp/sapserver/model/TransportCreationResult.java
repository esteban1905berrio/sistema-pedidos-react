package com.crystal.mcp.sapserver.model;

/**
 * Result model for transport request creation.
 *
 * @param success Whether the operation was successful
 * @param transportNumber The created transport number (if successful)
 * @param taskNumber The created task number (if successful)
 * @param status Status code: 'X' (success), 'W' (warning), '' (error)
 * @param message Result message
 * @param objectsCopied Number of objects copied (if using reference transport)
 * @param requestType Type of transport created
 * @param requestTypeDescription Human-readable type description
 *
 * @author Crystal Development Team
 * @since 2025-12-02
 */
public record TransportCreationResult(
    boolean success,
    String transportNumber,
    String taskNumber,
    String status,
    String message,
    int objectsCopied,
    String requestType,
    String requestTypeDescription
) {
    /**
     * Creates a successful result.
     */
    public static TransportCreationResult success(
            String transportNumber,
            String taskNumber,
            String message,
            int objectsCopied,
            String requestType) {
        return new TransportCreationResult(
            true,
            transportNumber,
            taskNumber,
            "X",
            message,
            objectsCopied,
            requestType,
            mapRequestType(requestType)
        );
    }

    /**
     * Creates a warning result (created but with issues).
     */
    public static TransportCreationResult warning(
            String transportNumber,
            String taskNumber,
            String message,
            int objectsCopied,
            String requestType) {
        return new TransportCreationResult(
            true,
            transportNumber,
            taskNumber,
            "W",
            message,
            objectsCopied,
            requestType,
            mapRequestType(requestType)
        );
    }

    /**
     * Creates an error result.
     */
    public static TransportCreationResult error(String message) {
        return new TransportCreationResult(
            false,
            null,
            null,
            "E",
            message,
            0,
            null,
            null
        );
    }

    /**
     * Returns human-readable status description.
     */
    public String getStatusDescription() {
        if (status == null) return "Unknown";
        return switch (status) {
            case "X" -> "Success";
            case "W" -> "Warning";
            case "E" -> "Error";
            default -> status;
        };
    }

    private static String mapRequestType(String type) {
        if (type == null) return null;
        return switch (type.toUpperCase()) {
            case "K" -> "Workbench";
            case "W" -> "Customizing";
            case "T" -> "Transport of Copies";
            default -> type;
        };
    }
}

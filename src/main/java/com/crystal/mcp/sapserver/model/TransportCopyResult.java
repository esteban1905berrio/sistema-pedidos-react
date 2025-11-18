package com.crystal.mcp.sapserver.model;

/**
 * Result model for transport copy creation operation.
 *
 * <p>Represents the response from the SAP system after attempting to create a transport copy.
 * Contains the new transport number, operation status, descriptive message, and release log.
 *
 * @param newTransportNumber The newly created transport copy number (e.g., "CADK911520").
 *                           Null if creation failed.
 * @param status SAP status code: "S" (Success), "E" (Error), "W" (Warning), "I" (Info)
 * @param message Descriptive message about the operation result
 * @param success Boolean flag indicating if operation succeeded (status == "S")
 * @param releaseLog Detailed release log from transport copy/release operation.
 *                   Contains information about exported objects, warnings, and errors.
 *                   Null if log not available or operation failed before release.
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 */
public record TransportCopyResult(
    String newTransportNumber,
    String status,
    String message,
    boolean success,
    String releaseLog
) {
    /**
     * Creates a successful result.
     *
     * @param newTransportNumber The created transport number
     * @param message Success message
     * @param releaseLog Release log (optional, can be null)
     * @return TransportCopyResult with success status
     */
    public static TransportCopyResult success(String newTransportNumber, String message, String releaseLog) {
        return new TransportCopyResult(newTransportNumber, "S", message, true, releaseLog);
    }

    /**
     * Creates a successful result without release log.
     *
     * @param newTransportNumber The created transport number
     * @param message Success message
     * @return TransportCopyResult with success status
     */
    public static TransportCopyResult success(String newTransportNumber, String message) {
        return success(newTransportNumber, message, null);
    }

    /**
     * Creates an error result.
     *
     * @param errorMessage Error description
     * @return TransportCopyResult with error status
     */
    public static TransportCopyResult error(String errorMessage) {
        return new TransportCopyResult(null, "E", errorMessage, false, null);
    }

    /**
     * Creates a warning result.
     *
     * @param newTransportNumber The created transport number (may be partial success)
     * @param warningMessage Warning description
     * @return TransportCopyResult with warning status
     */
    public static TransportCopyResult warning(String newTransportNumber, String warningMessage) {
        return new TransportCopyResult(newTransportNumber, "W", warningMessage, false, null);
    }

    /**
     * Checks if the operation was successful.
     *
     * @return true if status is "S" (Success)
     */
    public boolean isSuccess() {
        return success && "S".equals(status);
    }

    /**
     * Checks if the operation resulted in an error.
     *
     * @return true if status is "E" (Error)
     */
    public boolean isError() {
        return "E".equals(status);
    }

    /**
     * Checks if the operation resulted in a warning.
     *
     * @return true if status is "W" (Warning)
     */
    public boolean isWarning() {
        return "W".equals(status);
    }

    /**
     * Returns a user-friendly status description.
     *
     * @return Status description in English
     */
    public String getStatusDescription() {
        return switch (status) {
            case "S" -> "Success";
            case "E" -> "Error";
            case "W" -> "Warning";
            case "I" -> "Information";
            default -> "Unknown";
        };
    }

    /**
     * Returns a formatted string representation for logging.
     *
     * @return Formatted result string
     */
    @Override
    public String toString() {
        return String.format("TransportCopyResult[transport=%s, status=%s (%s), message=%s, hasLog=%s]",
            newTransportNumber != null ? newTransportNumber : "null",
            status,
            getStatusDescription(),
            message,
            releaseLog != null && !releaseLog.isEmpty()
        );
    }
}

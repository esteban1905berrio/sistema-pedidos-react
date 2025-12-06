package com.crystal.mcp.sapserver.model;

/**
 * Result model for transport copy creation operation.
 *
 * <p>Represents the response from the SAP system after attempting to create a transport copy.
 * Contains the new transport number, operation status, descriptive message, and detailed
 * step-by-step results for each phase of the workflow.
 *
 * <p><b>Workflow Steps:</b>
 * <ol>
 *   <li>Creation: Create the transport request</li>
 *   <li>Objects: Include objects from source transport(s)</li>
 *   <li>Release: Release the transport (if autoRelease=true)</li>
 * </ol>
 *
 * @param newTransportNumber The newly created transport copy number (e.g., "CADK911520").
 *                           Null if creation failed.
 * @param status SAP status code: "S" (Success), "E" (Error), "W" (Warning), "I" (Info)
 * @param message Descriptive message about the operation result
 * @param success Boolean flag indicating if operation succeeded (status == "S")
 * @param creationOk Whether the transport was created successfully
 * @param creationMsg Message from creation step
 * @param objectsOk Whether objects were included successfully
 * @param objectsMsg Message from objects inclusion step
 * @param releaseOk Whether the transport was released successfully
 * @param releaseMsg Message from release step
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 */
public record TransportCopyResult(
    String newTransportNumber,
    String status,
    String message,
    boolean success,
    // Detailed step results
    boolean creationOk,
    String creationMsg,
    boolean objectsOk,
    String objectsMsg,
    boolean releaseOk,
    String releaseMsg
) {
    /**
     * Creates a successful result with detailed step info.
     *
     * @param newTransportNumber The created transport number
     * @param message Success message
     * @param creationOk Creation step success
     * @param creationMsg Creation step message
     * @param objectsOk Objects step success
     * @param objectsMsg Objects step message
     * @param releaseOk Release step success
     * @param releaseMsg Release step message
     * @return TransportCopyResult with success status
     */
    public static TransportCopyResult success(String newTransportNumber, String message,
            boolean creationOk, String creationMsg,
            boolean objectsOk, String objectsMsg,
            boolean releaseOk, String releaseMsg) {
        return new TransportCopyResult(newTransportNumber, "S", message, true,
            creationOk, creationMsg, objectsOk, objectsMsg, releaseOk, releaseMsg);
    }

    /**
     * Creates a successful result with default step info.
     *
     * @param newTransportNumber The created transport number
     * @param message Success message
     * @return TransportCopyResult with success status
     */
    public static TransportCopyResult success(String newTransportNumber, String message) {
        return new TransportCopyResult(newTransportNumber, "S", message, true,
            true, null, true, null, true, null);
    }

    /**
     * Creates an error result.
     *
     * @param errorMessage Error description
     * @return TransportCopyResult with error status
     */
    public static TransportCopyResult error(String errorMessage) {
        return new TransportCopyResult(null, "E", errorMessage, false,
            false, null, false, null, false, null);
    }

    /**
     * Creates a warning result (e.g., transport created but release failed).
     *
     * @param newTransportNumber The created transport number (may be partial success)
     * @param warningMessage Warning description
     * @param creationOk Creation step success
     * @param creationMsg Creation step message
     * @param objectsOk Objects step success
     * @param objectsMsg Objects step message
     * @param releaseOk Release step success
     * @param releaseMsg Release step message
     * @return TransportCopyResult with warning status
     */
    public static TransportCopyResult warning(String newTransportNumber, String warningMessage,
            boolean creationOk, String creationMsg,
            boolean objectsOk, String objectsMsg,
            boolean releaseOk, String releaseMsg) {
        return new TransportCopyResult(newTransportNumber, "W", warningMessage, false,
            creationOk, creationMsg, objectsOk, objectsMsg, releaseOk, releaseMsg);
    }

    /**
     * Creates a warning result with default step info.
     *
     * @param newTransportNumber The created transport number (may be partial success)
     * @param warningMessage Warning description
     * @return TransportCopyResult with warning status
     */
    public static TransportCopyResult warning(String newTransportNumber, String warningMessage) {
        return new TransportCopyResult(newTransportNumber, "W", warningMessage, false,
            false, null, false, null, false, null);
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
        return String.format("TransportCopyResult[transport=%s, status=%s (%s), message=%s, " +
            "creation=%s, objects=%s, release=%s]",
            newTransportNumber != null ? newTransportNumber : "null",
            status,
            getStatusDescription(),
            message,
            creationOk ? "OK" : "FAILED",
            objectsOk ? "OK" : "FAILED",
            releaseOk ? "OK" : "FAILED"
        );
    }
}

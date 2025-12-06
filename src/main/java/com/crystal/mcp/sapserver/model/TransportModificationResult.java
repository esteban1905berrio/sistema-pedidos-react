package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result model for transport request modification operations.
 *
 * <p>Supports multiple modification operations:
 * <ul>
 *   <li>Add objects to transport tasks</li>
 *   <li>Modify transport description</li>
 *   <li>Release transport tasks</li>
 *   <li>Release transport request (with all tasks)</li>
 * </ul>
 *
 * @param success Whether the operation was successful
 * @param transportNumber The transport number affected
 * @param taskNumber The task number affected (if applicable)
 * @param operation The operation performed
 * @param status Status code: 'X' (success), 'W' (warning), 'E' (error)
 * @param message Result message
 * @param objectsAdded Number of objects added (for add_objects operation)
 * @param tasksReleased List of tasks released (for release_transport operation)
 * @param requiresConfirmation Whether user confirmation is required
 * @param confirmationMessage Message to show for confirmation
 *
 * @author Crystal Development Team
 * @since 2025-12-05
 */
public record TransportModificationResult(
    boolean success,
    String transportNumber,
    String taskNumber,
    String operation,
    String status,
    String message,
    int objectsAdded,
    List<String> tasksReleased,
    boolean requiresConfirmation,
    String confirmationMessage
) {
    /**
     * Creates a successful result for adding objects.
     */
    public static TransportModificationResult successAddObjects(
            String transportNumber,
            String taskNumber,
            int objectsAdded,
            String message) {
        return new TransportModificationResult(
            true,
            transportNumber,
            taskNumber,
            "ADD_OBJECTS",
            "X",
            message,
            objectsAdded,
            null,
            false,
            null
        );
    }

    /**
     * Creates a successful result for modifying description.
     */
    public static TransportModificationResult successModifyDescription(
            String transportNumber,
            String message) {
        return new TransportModificationResult(
            true,
            transportNumber,
            null,
            "MODIFY_DESCRIPTION",
            "X",
            message,
            0,
            null,
            false,
            null
        );
    }

    /**
     * Creates a successful result for releasing a task.
     */
    public static TransportModificationResult successReleaseTask(
            String transportNumber,
            String taskNumber,
            String message) {
        return new TransportModificationResult(
            true,
            transportNumber,
            taskNumber,
            "RELEASE_TASK",
            "X",
            message,
            0,
            List.of(taskNumber),
            false,
            null
        );
    }

    /**
     * Creates a successful result for releasing transport (with all tasks).
     */
    public static TransportModificationResult successReleaseTransport(
            String transportNumber,
            List<String> tasksReleased,
            String message) {
        return new TransportModificationResult(
            true,
            transportNumber,
            null,
            "RELEASE_TRANSPORT",
            "X",
            message,
            0,
            tasksReleased,
            false,
            null
        );
    }

    /**
     * Creates a result requiring user confirmation before releasing transport.
     */
    public static TransportModificationResult confirmationRequired(
            String transportNumber,
            List<String> tasksToRelease,
            String confirmationMessage) {
        return new TransportModificationResult(
            true,
            transportNumber,
            null,
            "RELEASE_TRANSPORT_PENDING",
            "W",
            "Confirmation required before releasing transport",
            0,
            tasksToRelease,
            true,
            confirmationMessage
        );
    }

    /**
     * Creates a warning result (operation completed with issues).
     */
    public static TransportModificationResult warning(
            String transportNumber,
            String operation,
            String message) {
        return new TransportModificationResult(
            true,
            transportNumber,
            null,
            operation,
            "W",
            message,
            0,
            null,
            false,
            null
        );
    }

    /**
     * Creates an error result.
     */
    public static TransportModificationResult error(String message) {
        return new TransportModificationResult(
            false,
            null,
            null,
            null,
            "E",
            message,
            0,
            null,
            false,
            null
        );
    }

    /**
     * Creates an error result with transport context.
     */
    public static TransportModificationResult error(
            String transportNumber,
            String operation,
            String message) {
        return new TransportModificationResult(
            false,
            transportNumber,
            null,
            operation,
            "E",
            message,
            0,
            null,
            false,
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

    /**
     * Returns human-readable operation description.
     */
    public String getOperationDescription() {
        if (operation == null) return "Unknown";
        return switch (operation) {
            case "ADD_OBJECTS" -> "Add Objects to Task";
            case "MODIFY_DESCRIPTION" -> "Modify Description";
            case "RELEASE_TASK" -> "Release Task";
            case "RELEASE_TRANSPORT" -> "Release Transport";
            case "RELEASE_TRANSPORT_PENDING" -> "Release Transport (Pending Confirmation)";
            default -> operation;
        };
    }
}

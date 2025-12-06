package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.TransportModificationResult;
import com.crystal.mcp.sapserver.service.TransportModificationService;
import com.crystal.mcp.sapserver.service.TransportModificationService.TransportObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Tools for transport request modification.
 *
 * <p>Provides tools for modifying SAP transport requests:
 * <ul>
 *   <li><b>add_objects_to_transport:</b> Add development objects to a transport (OT)</li>
 *   <li><b>modify_transport_description:</b> Change the description of a transport request</li>
 *   <li><b>release_task:</b> Release a single transport task</li>
 *   <li><b>release_transport:</b> Release transport request with all child tasks</li>
 * </ul>
 *
 * <p><b>ABAP Implementation:</b> Uses function module {@code ZCX_MODIFY_TRANSPORT_REQUEST}
 * which handles all modification operations.
 *
 * <p><b>Important Notes:</b>
 * <ul>
 *   <li>add_objects_to_transport requires the MAIN TRANSPORT NUMBER (OT), not a task number.
 *       SAP's TR_REQUEST_CHOICE internally assigns objects to the appropriate task.</li>
 *   <li>Releasing a transport requires user confirmation (returns tasks list first)</li>
 * </ul>
 *
 * @author Crystal Development Team
 * @since 2025-12-05
 */
@Component
public class TransportModificationTools {

    private static final Logger logger = LoggerFactory.getLogger(TransportModificationTools.class);

    private final TransportModificationService transportModificationService;
    private final ObjectMapper objectMapper;

    public TransportModificationTools(TransportModificationService transportModificationService,
                                      ObjectMapper objectMapper) {
        this.transportModificationService = transportModificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * MCP Tool: add_objects_to_transport
     *
     * <p>Adds development objects to a transport request (OT). <b>IMPORTANT:</b> This tool
     * requires the MAIN TRANSPORT NUMBER (OT), not a task number. SAP's TR_REQUEST_CHOICE
     * internally assigns objects to the appropriate task.
     *
     * <p><b>Object Format:</b> JSON array with at minimum objName. If pgmid and object
     * are not provided, they will be auto-detected from the TADIR table.
     *
     * <p><b>Examples:</b>
     * <pre>
     * // With auto-detection (recommended)
     * add_objects_to_transport('CADK911088', '[{"objName":"ZTEST_PROGRAM"}]')
     *
     * // With full specification
     * add_objects_to_transport('CADK911088', '[{"pgmid":"R3TR","object":"PROG","objName":"ZTEST_PROGRAM"}]')
     * </pre>
     *
     * @param transportNumber Main transport number (OT) to add objects to (e.g., "CADK911088")
     * @param objectsJson JSON array of objects to add
     * @return JSON response with operation status (includes taskNumber where objects were assigned)
     */
    @McpTool(
        description = "Add development objects to a transport request (OT). " +
                     "IMPORTANT: Pass the MAIN TRANSPORT NUMBER (OT), NOT a task number. " +
                     "SAP internally assigns objects to the appropriate task. " +
                     "Format: [{\"objName\":\"ZTEST\"}] (auto-detects type from TADIR) or " +
                     "[{\"pgmid\":\"R3TR\",\"object\":\"PROG\",\"objName\":\"ZTEST\"}] (explicit type). " +
                     "Common pgmid values: 'R3TR' (repository), 'LIMU' (sub-objects). " +
                     "Example: add_objects_to_transport('CADK911088', '[{\"objName\":\"ZTEST_PROGRAM\"}]')"
    )
    public String add_objects_to_transport(
        @McpToolParam(description = "Main transport number (OT) to add objects to. " +
                                "Pass the main transport, NOT a task number. " +
                                "Example: 'CADK911088'")
        String transportNumber,

        @McpToolParam(description = "JSON array of objects to add. " +
                                "Only objName is required - pgmid/object auto-detected from TADIR. " +
                                "Format: [{\"objName\":\"ZTEST\"}] or " +
                                "[{\"pgmid\":\"R3TR\",\"object\":\"PROG\",\"objName\":\"ZTEST\"}]")
        String objectsJson
    ) {
        try {
            logger.info("MCP Tool called: add_objects_to_transport(transport={}, objects={})",
                       transportNumber, objectsJson != null ? "provided" : "null");

            // Validate parameters
            if (transportNumber == null || transportNumber.trim().isEmpty()) {
                return formatError("Transport number (OT) is required. Pass the main transport, not a task.");
            }

            if (objectsJson == null || objectsJson.trim().isEmpty()) {
                return formatError("Objects JSON is required");
            }

            // Parse objects
            List<TransportObject> objects = parseObjectsJson(objectsJson);

            // Execute
            TransportModificationResult result = transportModificationService.addObjectsToTransport(
                transportNumber, objects);

            return formatResult(result);

        } catch (JCoException e) {
            logger.error("RFC error adding objects to transport", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error adding objects to transport", e);
            return formatError("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error adding objects to transport", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: force_add_objects_to_transport
     *
     * <p>Force-adds development objects to a transport request, bypassing lock validation.
     * Use this when add_objects_to_transport fails with "ob_locked_by_other" error.
     *
     * <p><b>WARNING:</b> Objects will be added even if locked in another transport.
     * The same object may exist in multiple transports after using this tool.
     *
     * @param transportNumber Main transport number (OT) or task number
     * @param objectsJson JSON array of objects to add
     * @return JSON response with operation status
     */
    @McpTool(
        description = "Force-add development objects to a transport, BYPASSING lock validation. " +
                     "WARNING: Objects will be added even if locked in another transport. " +
                     "Use when add_objects_to_transport fails with 'ob_locked_by_other'. " +
                     "Format: [{\"objName\":\"ZTEST\"}] (auto-detects type from TADIR). " +
                     "Example: force_add_objects_to_transport('CADK911088', '[{\"objName\":\"ZTEST\"}]')"
    )
    public String force_add_objects_to_transport(
        @McpToolParam(description = "Transport number (OT or Task) to add objects to. " +
                                "Example: 'CADK911088'")
        String transportNumber,

        @McpToolParam(description = "JSON array of objects to add. " +
                                "Only objName is required - pgmid/object auto-detected from TADIR. " +
                                "Format: [{\"objName\":\"ZTEST\"}]")
        String objectsJson
    ) {
        try {
            logger.info("MCP Tool called: force_add_objects_to_transport(transport={}, objects={})",
                       transportNumber, objectsJson != null ? "provided" : "null");

            // Validate parameters
            if (transportNumber == null || transportNumber.trim().isEmpty()) {
                return formatError("Transport number is required.");
            }

            if (objectsJson == null || objectsJson.trim().isEmpty()) {
                return formatError("Objects JSON is required");
            }

            // Parse objects
            List<TransportObject> objects = parseObjectsJson(objectsJson);

            // Execute FORCE add
            TransportModificationResult result = transportModificationService.forceAddObjectsToTransport(
                transportNumber, objects);

            return formatResult(result);

        } catch (JCoException e) {
            logger.error("RFC error force-adding objects to transport", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error force-adding objects to transport", e);
            return formatError("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error force-adding objects to transport", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: modify_transport_description
     *
     * <p>Modifies the description of a transport request.
     *
     * @param transportNumber Transport request number
     * @param newDescription New description (max 60 characters)
     * @return JSON response with operation status
     */
    @McpTool(
        description = "Modify the description of a transport request. " +
                     "Example: modify_transport_description('CADK911088', 'New description here')"
    )
    public String modify_transport_description(
        @McpToolParam(description = "Transport request number. Example: 'CADK911088'")
        String transportNumber,

        @McpToolParam(description = "New description for the transport (max 60 characters)")
        String newDescription
    ) {
        try {
            logger.info("MCP Tool called: modify_transport_description(transport={}, desc='{}')",
                       transportNumber, newDescription);

            // Validate parameters
            if (transportNumber == null || transportNumber.trim().isEmpty()) {
                return formatError("Transport number is required");
            }

            if (newDescription == null || newDescription.trim().isEmpty()) {
                return formatError("New description is required");
            }

            // Execute
            TransportModificationResult result = transportModificationService.modifyDescription(
                transportNumber, newDescription);

            return formatResult(result);

        } catch (JCoException e) {
            logger.error("RFC error modifying transport description", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error modifying transport description", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: release_task
     *
     * <p>Releases a single transport task.
     *
     * @param taskNumber Task number to release
     * @return JSON response with operation status
     */
    @McpTool(
        description = "Release a single transport task. " +
                     "Example: release_task('CADK911511')"
    )
    public String release_task(
        @McpToolParam(description = "Task number to release. Example: 'CADK911511'")
        String taskNumber
    ) {
        try {
            logger.info("MCP Tool called: release_task(task={})", taskNumber);

            // Validate parameters
            if (taskNumber == null || taskNumber.trim().isEmpty()) {
                return formatError("Task number is required");
            }

            // Execute
            TransportModificationResult result = transportModificationService.releaseTask(taskNumber);

            return formatResult(result);

        } catch (JCoException e) {
            logger.error("RFC error releasing task", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error releasing task", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: release_transport
     *
     * <p>Releases a transport request with all its child tasks.
     *
     * <p><b>IMPORTANT:</b> This operation requires confirmation. If confirmed=false (default),
     * the tool returns a list of tasks that will be released and asks for confirmation.
     * Call again with confirmed=true to proceed with the release.
     *
     * <p><b>Workflow:</b>
     * <ol>
     *   <li>Call release_transport('CADK911088', false) - returns tasks to be released</li>
     *   <li>Show confirmation to user with task list</li>
     *   <li>Call release_transport('CADK911088', true) - performs the release</li>
     * </ol>
     *
     * @param transportNumber Transport request number
     * @param confirmed Whether user has confirmed the release
     * @return JSON response with operation status or confirmation request
     */
    @McpTool(
        description = "Release a transport request with all its child tasks. " +
                     "IMPORTANT: Requires confirmation. First call with confirmed=false to get " +
                     "list of tasks that will be released. Then call with confirmed=true to proceed. " +
                     "Example: release_transport('CADK911088', false) - gets confirmation info. " +
                     "Example: release_transport('CADK911088', true) - releases all."
    )
    public String release_transport(
        @McpToolParam(description = "Transport request number. Example: 'CADK911088'")
        String transportNumber,

        @McpToolParam(description = "Whether user has confirmed the release. " +
                                "false: Returns tasks list for confirmation. " +
                                "true: Proceeds with releasing all tasks and transport.")
        Boolean confirmed
    ) {
        try {
            boolean isConfirmed = confirmed != null && confirmed;
            logger.info("MCP Tool called: release_transport(transport={}, confirmed={})",
                       transportNumber, isConfirmed);

            // Validate parameters
            if (transportNumber == null || transportNumber.trim().isEmpty()) {
                return formatError("Transport number is required");
            }

            // Execute
            TransportModificationResult result = transportModificationService.releaseTransport(
                transportNumber, isConfirmed);

            return formatResult(result);

        } catch (JCoException e) {
            logger.error("RFC error releasing transport", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error releasing transport", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * Formats a successful result as JSON.
     */
    private String formatResult(TransportModificationResult result) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.success());
            response.put("status", result.status());
            response.put("statusDescription", result.getStatusDescription());
            response.put("operation", result.operation());
            response.put("operationDescription", result.getOperationDescription());
            response.put("transportNumber", result.transportNumber());

            if (result.taskNumber() != null) {
                response.put("taskNumber", result.taskNumber());
            }

            if (result.objectsAdded() > 0) {
                response.put("objectsAdded", result.objectsAdded());
            }

            if (result.tasksReleased() != null && !result.tasksReleased().isEmpty()) {
                response.put("tasksReleased", result.tasksReleased());
                response.put("tasksReleasedCount", result.tasksReleased().size());
            }

            if (result.requiresConfirmation()) {
                response.put("requiresConfirmation", true);
                response.put("confirmationMessage", result.confirmationMessage());
            }

            response.put("message", result.message());

            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error formatting result response", e);
            return "{\"success\": " + result.success() + ", \"error\": \"JSON formatting error\"}";
        }
    }

    /**
     * Formats an error message as JSON.
     */
    private String formatError(String errorMessage) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("status", "E");
            response.put("statusDescription", "Error");
            response.put("error", errorMessage);

            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error formatting error response", e);
            return "{\"success\": false, \"error\": \"" + errorMessage + "\"}";
        }
    }

    /**
     * Parses a JSON array of objects into TransportObject list.
     *
     * @param objectsJson JSON array string
     * @return List of TransportObject
     * @throws IllegalArgumentException if JSON parsing fails
     */
    private List<TransportObject> parseObjectsJson(String objectsJson) {
        try {
            List<Map<String, String>> rawObjects = objectMapper.readValue(
                objectsJson,
                new TypeReference<List<Map<String, String>>>() {}
            );

            return rawObjects.stream()
                .map(obj -> new TransportObject(
                    obj.get("pgmid"),
                    obj.get("object"),
                    obj.get("objName")
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to parse objects JSON: {}", objectsJson, e);
            throw new IllegalArgumentException(
                "Invalid objects JSON format. Expected: [{\"objName\":\"ZTEST\"}] or " +
                "[{\"pgmid\":\"R3TR\",\"object\":\"PROG\",\"objName\":\"ZTEST\"}]. Error: " + e.getMessage()
            );
        }
    }
}

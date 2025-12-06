package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TransportModificationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for modifying SAP transport requests.
 *
 * <p>This service provides functionality to modify transport requests:
 * <ul>
 *   <li><b>Add Objects to Transport:</b> Add development objects to a transport request (OT)</li>
 *   <li><b>Modify Description:</b> Change the description of a transport request</li>
 *   <li><b>Release Task:</b> Release a single transport task</li>
 *   <li><b>Release Transport:</b> Release transport request with all child tasks</li>
 * </ul>
 *
 * <p>The service calls the ABAP function module {@code ZCX_MODIFY_TRANSPORT_REQUEST}, which:
 * <ol>
 *   <li>Validates the transport/task exists and is modifiable</li>
 *   <li>Performs the requested operation</li>
 *   <li>Returns operation status and details</li>
 * </ol>
 *
 * <p><b>Implementation Notes:</b>
 * <ul>
 *   <li><b>ADD_OBJECTS requires the MAIN TRANSPORT NUMBER (OT)</b>, not a task number.
 *       The underlying FM {@code TR_REQUEST_CHOICE} expects the main transport and
 *       SAP internally assigns objects to the appropriate task.</li>
 *   <li>When releasing a transport, ALL child tasks must be released first</li>
 *   <li>Release operations require user confirmation (handled by MCP tool layer)</li>
 * </ul>
 *
 * @author Crystal Development Team
 * @since 2025-12-05
 * @see TransportModificationResult
 */
@Service
public class TransportModificationService {

    private static final Logger logger = LoggerFactory.getLogger(TransportModificationService.class);
    private static final String FUNCTION_MODULE = "ZCX_MODIFY_TRANSPORT_REQUEST";

    private final com.sap.conn.jco.JCoDestination destination;
    private final ObjectMapper objectMapper;

    public TransportModificationService(com.sap.conn.jco.JCoDestination destination,
                                       ObjectMapper objectMapper) {
        this.destination = destination;
        this.objectMapper = objectMapper;
    }

    /**
     * Adds objects to a transport request (OT).
     *
     * <p><b>IMPORTANT:</b> This method requires the MAIN TRANSPORT NUMBER (OT), not a task number.
     * The underlying SAP function module {@code TR_REQUEST_CHOICE} expects the main transport
     * and SAP internally assigns objects to the appropriate task.
     *
     * <p><b>Object Format:</b> Each object requires at minimum objName. If pgmid and object
     * are not provided, they will be auto-detected from the TADIR table.
     *
     * <p><b>Lock Validation:</b> This method validates object locks. If an object is locked
     * in another transport, it will fail with error. Use {@link #forceAddObjectsToTransport}
     * to bypass lock validation.
     *
     * @param transportNumber Main transport number (OT) to add objects to (e.g., "CADK911088")
     * @param objects List of objects to add (objName required, pgmid/object optional)
     * @return TransportModificationResult with operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportModificationResult addObjectsToTransport(
            String transportNumber,
            List<TransportObject> objects) throws JCoException {

        return addObjectsToTransportInternal(transportNumber, objects, "ADD_OBJECTS");
    }

    /**
     * Force-adds objects to a transport request, bypassing lock validation.
     *
     * <p><b>WARNING:</b> This method bypasses SAP's lock validation. Objects will be added
     * even if they are locked in another transport. Use this method only when you explicitly
     * need to have the same object in multiple transports.
     *
     * <p><b>Use Case:</b> When {@link #addObjectsToTransport} fails with "ob_locked_by_other"
     * error and you need to add the object anyway (e.g., creating a backup copy transport).
     *
     * <p><b>Implementation:</b> Uses {@code TRINT_APPEND_COMM} directly instead of
     * {@code TR_APPEND_TO_COMM_OBJS_KEYS}, which does not perform lock validation.
     *
     * @param transportNumber Transport number (OT or Task) to add objects to
     * @param objects List of objects to add (objName required, pgmid/object optional)
     * @return TransportModificationResult with operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportModificationResult forceAddObjectsToTransport(
            String transportNumber,
            List<TransportObject> objects) throws JCoException {

        return addObjectsToTransportInternal(transportNumber, objects, "ADD_OBJECTS_FORCE");
    }

    /**
     * Internal method for adding objects with specified operation.
     */
    private TransportModificationResult addObjectsToTransportInternal(
            String transportNumber,
            List<TransportObject> objects,
            String operation) throws JCoException {

        if (transportNumber == null || transportNumber.trim().isEmpty()) {
            return TransportModificationResult.error("Transport number (OT) is required. Pass the main transport, not a task.");
        }

        if (objects == null || objects.isEmpty()) {
            return TransportModificationResult.error("At least one object is required");
        }

        boolean isForce = "ADD_OBJECTS_FORCE".equals(operation);
        logger.info("{} {} objects to transport: {}",
            isForce ? "Force-adding" : "Adding", objects.size(), transportNumber);

        try {
            JCoFunction function = getFunction();
            if (function == null) {
                return TransportModificationResult.error(
                    FUNCTION_MODULE + " not found in SAP system");
            }

            // Set import parameters
            // NOTE: Uses IV_TRKORR (simplified interface) - FM auto-detects if it's OT or Task
            function.getImportParameterList().setValue("IV_OPERATION", operation);
            function.getImportParameterList().setValue("IV_TRKORR", transportNumber.toUpperCase());
            function.getImportParameterList().setValue("IV_OBJECTS_JSON", convertObjectsToJson(objects));

            // Execute
            function.execute(destination);

            // Parse response
            boolean success = "X".equalsIgnoreCase(
                function.getExportParameterList().getString("EV_SUCCESS"));
            String returnedTransport = function.getExportParameterList().getString("EV_TRANSPORT_NUMBER");
            String taskAssigned = function.getExportParameterList().getString("EV_TASK_NUMBER");
            String message = function.getExportParameterList().getString("EV_MESSAGE");
            int objectsAdded = function.getExportParameterList().getInt("EV_OBJECTS_ADDED");

            if (success) {
                logger.info("Successfully {} {} objects to transport {} (task: {})",
                    isForce ? "force-added" : "added", objectsAdded, transportNumber, taskAssigned);
                return TransportModificationResult.successAddObjects(
                    returnedTransport, taskAssigned, objectsAdded, message);
            } else {
                logger.warn("Failed to {} objects to transport {}: {}",
                    isForce ? "force-add" : "add", transportNumber, message);
                return TransportModificationResult.error(returnedTransport, operation, message);
            }

        } catch (JCoException e) {
            logger.error("RFC error {} objects to transport {}",
                isForce ? "force-adding" : "adding", transportNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error {} objects to transport {}",
                isForce ? "force-adding" : "adding", transportNumber, e);
            return TransportModificationResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Modifies the description of a transport request.
     *
     * @param transportNumber Transport request number
     * @param newDescription New description (max 60 characters)
     * @return TransportModificationResult with operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportModificationResult modifyDescription(
            String transportNumber,
            String newDescription) throws JCoException {

        if (transportNumber == null || transportNumber.trim().isEmpty()) {
            return TransportModificationResult.error("Transport number is required");
        }

        if (newDescription == null || newDescription.trim().isEmpty()) {
            return TransportModificationResult.error("New description is required");
        }

        if (newDescription.length() > 60) {
            return TransportModificationResult.error("Description too long (max 60 characters)");
        }

        logger.info("Modifying description of transport: {} to '{}'", transportNumber, newDescription);

        try {
            JCoFunction function = getFunction();
            if (function == null) {
                return TransportModificationResult.error(
                    FUNCTION_MODULE + " not found in SAP system");
            }

            // Set import parameters - IV_TRKORR auto-detects OT vs Task
            function.getImportParameterList().setValue("IV_OPERATION", "MODIFY_DESCRIPTION");
            function.getImportParameterList().setValue("IV_TRKORR", transportNumber.toUpperCase());
            function.getImportParameterList().setValue("IV_DESCRIPTION", newDescription);

            // Execute
            function.execute(destination);

            // Parse response
            boolean success = "X".equalsIgnoreCase(
                function.getExportParameterList().getString("EV_SUCCESS"));
            String message = function.getExportParameterList().getString("EV_MESSAGE");

            if (success) {
                logger.info("Successfully modified description of transport {}", transportNumber);
                return TransportModificationResult.successModifyDescription(transportNumber, message);
            } else {
                logger.warn("Failed to modify description of transport {}: {}", transportNumber, message);
                return TransportModificationResult.error(transportNumber, "MODIFY_DESCRIPTION", message);
            }

        } catch (JCoException e) {
            logger.error("RFC error modifying description of transport {}", transportNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error modifying description of transport {}", transportNumber, e);
            return TransportModificationResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Releases a transport task.
     *
     * @param taskNumber Task number to release
     * @return TransportModificationResult with operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportModificationResult releaseTask(String taskNumber) throws JCoException {

        if (taskNumber == null || taskNumber.trim().isEmpty()) {
            return TransportModificationResult.error("Task number is required");
        }

        logger.info("Releasing task: {}", taskNumber);

        try {
            JCoFunction function = getFunction();
            if (function == null) {
                return TransportModificationResult.error(
                    FUNCTION_MODULE + " not found in SAP system");
            }

            // Set import parameters - IV_TRKORR auto-detects OT vs Task
            function.getImportParameterList().setValue("IV_OPERATION", "RELEASE_TASK");
            function.getImportParameterList().setValue("IV_TRKORR", taskNumber.toUpperCase());

            // Execute
            function.execute(destination);

            // Parse response
            boolean success = "X".equalsIgnoreCase(
                function.getExportParameterList().getString("EV_SUCCESS"));
            String transportNumber = function.getExportParameterList().getString("EV_TRANSPORT_NUMBER");
            String message = function.getExportParameterList().getString("EV_MESSAGE");

            if (success) {
                logger.info("Successfully released task {}", taskNumber);
                return TransportModificationResult.successReleaseTask(transportNumber, taskNumber, message);
            } else {
                logger.warn("Failed to release task {}: {}", taskNumber, message);
                return TransportModificationResult.error(transportNumber, "RELEASE_TASK", message);
            }

        } catch (JCoException e) {
            logger.error("RFC error releasing task {}", taskNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error releasing task {}", taskNumber, e);
            return TransportModificationResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Gets tasks of a transport that need to be released.
     *
     * <p>This method queries the transport to find all modifiable (non-released) tasks.
     * Use this before releasing a transport to get user confirmation.
     *
     * @param transportNumber Transport request number
     * @return TransportModificationResult with confirmation required and task list
     * @throws JCoException if RFC communication fails
     */
    public TransportModificationResult getTasksForRelease(String transportNumber) throws JCoException {

        if (transportNumber == null || transportNumber.trim().isEmpty()) {
            return TransportModificationResult.error("Transport number is required");
        }

        logger.info("Getting tasks for release of transport: {}", transportNumber);

        try {
            JCoFunction function = getFunction();
            if (function == null) {
                return TransportModificationResult.error(
                    FUNCTION_MODULE + " not found in SAP system");
            }

            // Set import parameters - IV_TRKORR auto-detects OT vs Task
            function.getImportParameterList().setValue("IV_OPERATION", "GET_TASKS_FOR_RELEASE");
            function.getImportParameterList().setValue("IV_TRKORR", transportNumber.toUpperCase());

            // Execute
            function.execute(destination);

            // Parse response
            boolean success = "X".equalsIgnoreCase(
                function.getExportParameterList().getString("EV_SUCCESS"));
            String message = function.getExportParameterList().getString("EV_MESSAGE");
            String tasksJson = function.getExportParameterList().getString("EV_TASKS_JSON");

            if (!success) {
                return TransportModificationResult.error(transportNumber, "GET_TASKS_FOR_RELEASE", message);
            }

            // Parse tasks list
            List<String> tasksToRelease = parseTasksJson(tasksJson);

            if (tasksToRelease.isEmpty()) {
                return TransportModificationResult.warning(
                    transportNumber,
                    "GET_TASKS_FOR_RELEASE",
                    "No modifiable tasks found. Transport may already be released."
                );
            }

            // Build confirmation message
            String confirmationMessage = String.format(
                "Release transport %s? This will release %d task(s): %s",
                transportNumber,
                tasksToRelease.size(),
                String.join(", ", tasksToRelease)
            );

            return TransportModificationResult.confirmationRequired(
                transportNumber, tasksToRelease, confirmationMessage);

        } catch (JCoException e) {
            logger.error("RFC error getting tasks for release of transport {}", transportNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error getting tasks for release of transport {}", transportNumber, e);
            return TransportModificationResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Releases a transport request with all its tasks.
     *
     * <p><b>IMPORTANT:</b> This method releases all child tasks before releasing the main transport.
     * Use {@link #getTasksForRelease(String)} first to get confirmation from the user.
     *
     * @param transportNumber Transport request number
     * @param confirmed Whether user has confirmed the release
     * @return TransportModificationResult with operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportModificationResult releaseTransport(
            String transportNumber,
            boolean confirmed) throws JCoException {

        if (transportNumber == null || transportNumber.trim().isEmpty()) {
            return TransportModificationResult.error("Transport number is required");
        }

        // If not confirmed, return tasks for confirmation
        if (!confirmed) {
            return getTasksForRelease(transportNumber);
        }

        logger.info("Releasing transport with all tasks: {}", transportNumber);

        try {
            JCoFunction function = getFunction();
            if (function == null) {
                return TransportModificationResult.error(
                    FUNCTION_MODULE + " not found in SAP system");
            }

            // Set import parameters - IV_TRKORR auto-detects OT vs Task
            function.getImportParameterList().setValue("IV_OPERATION", "RELEASE_TRANSPORT");
            function.getImportParameterList().setValue("IV_TRKORR", transportNumber.toUpperCase());
            function.getImportParameterList().setValue("IV_CONFIRMED", "X");

            // Execute
            function.execute(destination);

            // Parse response
            boolean success = "X".equalsIgnoreCase(
                function.getExportParameterList().getString("EV_SUCCESS"));
            String message = function.getExportParameterList().getString("EV_MESSAGE");
            String tasksJson = function.getExportParameterList().getString("EV_TASKS_JSON");

            List<String> tasksReleased = parseTasksJson(tasksJson);

            if (success) {
                logger.info("Successfully released transport {} with {} tasks",
                    transportNumber, tasksReleased.size());
                return TransportModificationResult.successReleaseTransport(
                    transportNumber, tasksReleased, message);
            } else {
                logger.warn("Failed to release transport {}: {}", transportNumber, message);
                return TransportModificationResult.error(transportNumber, "RELEASE_TRANSPORT", message);
            }

        } catch (JCoException e) {
            logger.error("RFC error releasing transport {}", transportNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error releasing transport {}", transportNumber, e);
            return TransportModificationResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Gets the function module from SAP repository.
     */
    private JCoFunction getFunction() throws JCoException {
        return destination.getRepository().getFunction(FUNCTION_MODULE);
    }

    /**
     * Validates if the function module exists in the SAP system.
     *
     * @return true if ZCX_MODIFY_TRANSPORT_REQUEST exists
     */
    public boolean isFunctionModuleAvailable() {
        try {
            JCoFunction function = destination.getRepository().getFunction(FUNCTION_MODULE);
            return function != null;
        } catch (JCoException e) {
            logger.warn("Function module {} not available: {}", FUNCTION_MODULE, e.getMessage());
            return false;
        }
    }

    /**
     * Converts a list of TransportObject to JSON format expected by ABAP FM.
     *
     * <p>The ABAP FM expects a JSON array with uppercase field names:
     * <pre>
     * [{"PGMID":"R3TR","OBJECT":"PROG","OBJ_NAME":"ZTEST"}]
     * </pre>
     *
     * @param objects List of transport objects
     * @return JSON string representation
     */
    private String convertObjectsToJson(List<TransportObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return "[]";
        }

        List<Map<String, String>> jsonObjects = objects.stream()
            .map(obj -> {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("PGMID", obj.pgmid() != null ? obj.pgmid().toUpperCase() : "");
                map.put("OBJECT", obj.object() != null ? obj.object().toUpperCase() : "");
                map.put("OBJ_NAME", obj.objName().toUpperCase());
                return map;
            })
            .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(jsonObjects);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert objects to JSON", e);
            throw new IllegalArgumentException("Invalid objects format: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON array of task numbers.
     *
     * @param tasksJson JSON array string (e.g., ["CADK911511", "CADK911512"])
     * @return List of task numbers
     */
    private List<String> parseTasksJson(String tasksJson) {
        if (tasksJson == null || tasksJson.trim().isEmpty() || "[]".equals(tasksJson.trim())) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(tasksJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse tasks JSON: {}", tasksJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * Represents an object to be added to a transport task.
     *
     * <p>Only objName is required. If pgmid and/or object are not provided,
     * the SAP function module will look them up in the TADIR table.
     *
     * @param pgmid Program ID (e.g., "R3TR", "LIMU"). Optional.
     * @param object Object type (e.g., "PROG", "CLAS", "FUNC"). Optional.
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
         */
        public void validate() {
            if (objName == null || objName.trim().isEmpty()) {
                throw new IllegalArgumentException("OBJ_NAME is required for transport object");
            }
        }
    }
}

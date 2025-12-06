package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TransportCreationRequest;
import com.crystal.mcp.sapserver.model.TransportCreationRequest.TransportObject;
import com.crystal.mcp.sapserver.model.TransportCreationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for creating SAP transport requests (Workbench, Customizing, or Transport of Copies).
 *
 * <p>This service provides functionality to create transport requests of different types:
 * <ul>
 *   <li><b>K - Workbench:</b> For development objects (programs, classes, etc.)</li>
 *   <li><b>W - Customizing:</b> For configuration changes</li>
 *   <li><b>T - Transport of Copies:</b> Copy from existing transport</li>
 * </ul>
 *
 * <p>The service calls the ABAP function module {@code ZCX_CREATE_TRANSPORT_REQUEST}, which:
 * <ol>
 *   <li>Validates the request type (K, W, or T)</li>
 *   <li>Creates a new transport via {@code TR_EXT_CREATE_REQUEST}</li>
 *   <li>If reference transport provided, copies objects via {@code TR_REQUEST_CHOICE}</li>
 *   <li>Optionally releases the transport via {@code TR_RELEASE_REQUEST}</li>
 * </ol>
 *
 * <p><b>Implementation Note:</b> This service uses stateless connections as transport creation
 * operations don't require locks. The ABAP function module handles rollback on failure.
 *
 * @author Crystal Development Team
 * @since 2025-12-02
 * @see TransportCreationRequest
 * @see TransportCreationResult
 */
@Service
public class TransportCreationService {

    private static final Logger logger = LoggerFactory.getLogger(TransportCreationService.class);
    private static final String FUNCTION_MODULE = "ZCX_CREATE_TRANSPORT_REQUEST";

    private final com.sap.conn.jco.JCoDestination destination;
    private final ObjectMapper objectMapper;

    public TransportCreationService(com.sap.conn.jco.JCoDestination destination,
                                    ObjectMapper objectMapper) {
        this.destination = destination;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new transport request.
     *
     * <p>This method calls the SAP function module {@code ZCX_CREATE_TRANSPORT_REQUEST} which
     * internally uses standard SAP FMs to:
     * <ol>
     *   <li>Validate the request type (K=Workbench, W=Customizing, T=Copy)</li>
     *   <li>Create transport via {@code TR_EXT_CREATE_REQUEST}</li>
     *   <li>Copy objects from reference transport if provided via {@code TR_REQUEST_CHOICE}</li>
     *   <li>Optionally release via {@code TR_RELEASE_REQUEST}</li>
     * </ol>
     *
     * <p><b>Error Handling:</b> If any step fails after transport creation, the transport is
     * automatically deleted (rollback) via {@code TRINT_DELETE_COMM}.
     *
     * @param request The transport creation request parameters
     * @return TransportCreationResult containing the new transport number and operation status
     * @throws JCoException if RFC communication fails
     * @throws IllegalArgumentException if request validation fails
     *
     * @see TransportCreationRequest#validate()
     */
    public TransportCreationResult createTransportRequest(TransportCreationRequest request)
            throws JCoException {

        // Validate request
        request.validate();

        logger.info("Creating transport request: type={}, description='{}', target={}, reference={}, autoRelease={}",
            request.getRequestTypeUpperCase(),
            request.description(),
            request.targetSystem() != null ? request.targetSystem() : "auto",
            request.referenceTransport() != null ? request.referenceTransport() : "none",
            request.autoRelease()
        );

        try {
            // Get function module from repository
            JCoFunction function = destination.getRepository().getFunction(FUNCTION_MODULE);

            if (function == null) {
                throw new RuntimeException(
                    FUNCTION_MODULE + " not found in SAP system. " +
                    "Ensure the function module exists in function group ZGFCX_1."
                );
            }

            // Set import parameters
            function.getImportParameterList().setValue("IV_REQUEST_TYPE",
                request.getRequestTypeUpperCase());
            function.getImportParameterList().setValue("IV_DESCRIPTION",
                request.description());

            if (request.targetSystem() != null && !request.targetSystem().trim().isEmpty()) {
                function.getImportParameterList().setValue("IV_TARGET_SYSTEM",
                    request.getTargetSystemUpperCase());
            }

            if (request.referenceTransport() != null && !request.referenceTransport().trim().isEmpty()) {
                function.getImportParameterList().setValue("IV_REFERENCE_TRANSPORT",
                    request.getReferenceTransportUpperCase());
            }

            function.getImportParameterList().setValue("IV_AUTO_RELEASE",
                request.autoRelease() ? "X" : "");

            // Set objects JSON if provided
            if (request.objects() != null && !request.objects().isEmpty()) {
                String objectsJson = convertObjectsToJson(request.objects());
                function.getImportParameterList().setValue("IV_OBJECTS_JSON", objectsJson);
                logger.debug("Objects JSON: {}", objectsJson);
            }

            // Set inherit description flag
            function.getImportParameterList().setValue("IV_INHERIT_DESCRIPTION",
                request.inheritDescription() ? "X" : "");

            // Execute function module
            logger.debug("Executing RFC: {} with parameters: type={}, desc='{}'",
                FUNCTION_MODULE, request.getRequestTypeUpperCase(), request.description());
            function.execute(destination);

            // Get export parameters
            String transportNumber = function.getExportParameterList().getString("EV_TRANSPORT_NUMBER");
            String taskNumber = function.getExportParameterList().getString("EV_TASK_NUMBER");
            String successFlag = function.getExportParameterList().getString("EV_SUCCESS");
            String message = function.getExportParameterList().getString("EV_MESSAGE");
            int objectsCopied = function.getExportParameterList().getInt("EV_OBJECTS_COPIED");

            boolean success = "X".equalsIgnoreCase(successFlag);

            // Log result
            if (success) {
                logger.info("Transport request created successfully: {} with task {} (type: {}, objects: {})",
                    transportNumber, taskNumber, request.getRequestTypeDescription(), objectsCopied);
            } else {
                logger.warn("Transport request creation failed: {}", message);
            }

            // Build result
            if (success) {
                if (objectsCopied > 0 || request.referenceTransport() != null) {
                    // Success with objects copied
                    return TransportCreationResult.success(
                        transportNumber,
                        taskNumber,
                        message,
                        objectsCopied,
                        request.getRequestTypeUpperCase()
                    );
                } else {
                    // Success without reference (empty transport)
                    return TransportCreationResult.success(
                        transportNumber,
                        taskNumber,
                        message != null && !message.isEmpty() ? message : "Transport created successfully",
                        0,
                        request.getRequestTypeUpperCase()
                    );
                }
            } else {
                return TransportCreationResult.error(
                    message != null && !message.isEmpty() ? message : "Transport creation failed"
                );
            }

        } catch (JCoException e) {
            logger.error("RFC error creating transport request: type={}, desc='{}'",
                request.getRequestTypeUpperCase(), request.description(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating transport request: type={}, desc='{}'",
                request.getRequestTypeUpperCase(), request.description(), e);
            return TransportCreationResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a Workbench transport request.
     *
     * <p>Workbench transports are used for development objects like programs, classes,
     * function modules, tables, etc.
     *
     * @param description Transport description (max 60 chars)
     * @param targetSystem Target system name (optional)
     * @return TransportCreationResult containing the new transport number
     * @throws JCoException if RFC communication fails
     */
    public TransportCreationResult createWorkbenchTransport(String description, String targetSystem)
            throws JCoException {
        return createTransportRequest(TransportCreationRequest.workbench(description, targetSystem));
    }

    /**
     * Creates a Customizing transport request.
     *
     * <p>Customizing transports are used for configuration changes in IMG.
     *
     * @param description Transport description (max 60 chars)
     * @param targetSystem Target system name (optional)
     * @return TransportCreationResult containing the new transport number
     * @throws JCoException if RFC communication fails
     */
    public TransportCreationResult createCustomizingTransport(String description, String targetSystem)
            throws JCoException {
        return createTransportRequest(TransportCreationRequest.customizing(description, targetSystem));
    }

    /**
     * Creates a Transport of Copies from a reference transport.
     *
     * <p>Copies all objects from the reference transport (including tasks) to a new transport.
     *
     * @param description Transport description (max 60 chars)
     * @param targetSystem Target system name (optional)
     * @param referenceTransport Reference transport to copy objects from
     * @param autoRelease Whether to automatically release after creation
     * @return TransportCreationResult containing the new transport number
     * @throws JCoException if RFC communication fails
     */
    public TransportCreationResult createTransportCopy(
            String description,
            String targetSystem,
            String referenceTransport,
            boolean autoRelease) throws JCoException {

        return createTransportRequest(TransportCreationRequest.copyWithReference(
            description, targetSystem, referenceTransport, autoRelease
        ));
    }

    /**
     * Validates if the function module exists in the SAP system.
     *
     * @return true if ZCX_CREATE_TRANSPORT_REQUEST exists
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
            return "";
        }

        // Build JSON array with uppercase field names for ABAP compatibility
        // pgmid and object can be null - ABAP FM will auto-detect from TADIR
        List<java.util.Map<String, String>> jsonObjects = objects.stream()
            .map(obj -> {
                java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
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
}

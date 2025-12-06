package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.TransportCreationRequest;
import com.crystal.mcp.sapserver.model.TransportCreationRequest.TransportObject;
import com.crystal.mcp.sapserver.model.TransportCreationResult;
import com.crystal.mcp.sapserver.service.TransportCreationService;
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
 * MCP Tools for transport request creation.
 *
 * <p>Provides tools for creating SAP transport requests of different types:
 * <ul>
 *   <li><b>K - Workbench:</b> For development objects (programs, classes, function modules)</li>
 *   <li><b>W - Customizing:</b> For configuration changes (IMG settings)</li>
 *   <li><b>T - Transport of Copies:</b> Copy from existing transport</li>
 * </ul>
 *
 * <p><b>Workflow:</b> The tool automatically handles the complete workflow:
 * <pre>
 * VALIDATE → CREATE_TRANSPORT → COPY_OBJECTS (if reference) → RELEASE (optional)
 * </pre>
 *
 * <p><b>ABAP Implementation:</b> Uses function module {@code ZCX_CREATE_TRANSPORT_REQUEST}
 * which calls standard SAP FMs (TR_EXT_CREATE_REQUEST, TR_REQUEST_CHOICE, TR_RELEASE_REQUEST).
 *
 * @author Crystal Development Team
 * @since 2025-12-02
 */
@Component
public class TransportCreationTools {

    private static final Logger logger = LoggerFactory.getLogger(TransportCreationTools.class);

    private final TransportCreationService transportCreationService;
    private final ObjectMapper objectMapper;

    public TransportCreationTools(TransportCreationService transportCreationService,
                                  ObjectMapper objectMapper) {
        this.transportCreationService = transportCreationService;
        this.objectMapper = objectMapper;
    }

    /**
     * MCP Tool: create_transport_request
     *
     * <p>Creates a new SAP transport request. Supports three transport types:
     * <ul>
     *   <li><b>K - Workbench:</b> For development objects</li>
     *   <li><b>W - Customizing:</b> For configuration changes</li>
     *   <li><b>T - Transport of Copies:</b> Copy from existing transport</li>
     * </ul>
     *
     * <p><b>Workflow:</b>
     * <ol>
     *   <li>Validate request type and description</li>
     *   <li>Create transport via {@code TR_EXT_CREATE_REQUEST}</li>
     *   <li>If reference provided, copy objects via {@code TR_REQUEST_CHOICE}</li>
     *   <li>If autoRelease=true, release via {@code TR_RELEASE_REQUEST}</li>
     * </ol>
     *
     * <p><b>Error Handling:</b> If any step fails after transport creation, the transport
     * is automatically deleted (rollback) to prevent orphaned transports.
     *
     * <p><b>Examples:</b>
     * <pre>
     * // Create empty Workbench transport
     * create_transport_request('K', 'My Development Task', 'S4D', null, false)
     *
     * // Create Customizing transport
     * create_transport_request('W', 'IMG Configuration', 'S4D', null, false)
     *
     * // Create Transport of Copies from reference
     * create_transport_request('T', 'COPY: Original Description', 'S4D', 'CADK911511', true)
     * </pre>
     *
     * @param requestType Type of transport to create: 'K' (Workbench), 'W' (Customizing), 'T' (Copy).
     *                    Required parameter.
     * @param description Transport description (max 60 chars). Required parameter.
     *                    Example: 'FI: Invoice Processing Enhancement'
     * @param targetSystem Target system name (optional). Examples: 'S4D', 'S4Q', 'S4P'.
     *                     If null, uses default target from transport layer.
     * @param referenceTransport Reference transport to copy objects from (optional).
     *                           Only used when requestType='T'. Example: 'CADK911511'
     * @param autoRelease Auto-release transport after creation (optional).
     *                    true: Release immediately, false: Keep modifiable. Default: false
     * @return JSON response with success status, transport number, and message
     */
    @McpTool(
        description = "Create a new SAP transport request (OT). " +
                     "Supports types: 'K' (Workbench for development objects), " +
                     "'W' (Customizing for configuration), 'T' (Transport of Copies). " +
                     "Workflow: VALIDATE → CREATE → COPY_OBJECTS (if reference) → RELEASE (optional). " +
                     "Examples: " +
                     "Workbench: create_transport_request('K', 'My Dev Task', 'S4D', null, false). " +
                     "Customizing: create_transport_request('W', 'IMG Config', 'S4D', null, false). " +
                     "Copy: create_transport_request('T', 'COPY: Description', 'S4D', 'CADK911511', true)"
    )
    public String create_transport_request(
        @McpToolParam(description = "Type of transport: 'K' (Workbench - development objects), " +
                                "'W' (Customizing - configuration), 'T' (Transport of Copies). Required.")
        String requestType,

        @McpToolParam(description = "Transport description (max 60 chars). Required unless inheritDescription=true. " +
                                "Example: 'FI: Invoice Processing Enhancement'")
        String description,

        @McpToolParam(description = "Target system name (optional). Examples: 'S4D', 'S4Q', 'S4P'. " +
                                "If null, uses default from transport layer.")
        String targetSystem,

        @McpToolParam(description = "Reference transport to copy objects from (optional). " +
                                "Only used when requestType='T'. Example: 'CADK911511', 'DEVK900123'")
        String referenceTransport,

        @McpToolParam(description = "Auto-release transport after creation (optional). " +
                                "true: Release immediately, false: Keep modifiable. Default: false")
        Boolean autoRelease,

        @McpToolParam(description = "JSON array of objects to include in the transport (optional). " +
                                "Format: [{\"objName\":\"ZTEST\"}] or [{\"pgmid\":\"R3TR\",\"object\":\"PROG\",\"objName\":\"ZTEST\"}]. " +
                                "Only objName is required - if pgmid/object are omitted, they are auto-detected from TADIR table. " +
                                "Common pgmid values: 'R3TR' (repository objects), 'LIMU' (main/sub objects).")
        String objectsJson,

        @McpToolParam(description = "Inherit description from reference transport (optional). " +
                                "If true, description parameter can be null. Requires referenceTransport. Default: false")
        Boolean inheritDescription
    ) {
        try {
            logger.info("MCP Tool called: create_transport_request(type={}, desc='{}', target={}, ref={}, release={}, objects={}, inherit={})",
                       requestType, description, targetSystem, referenceTransport, autoRelease,
                       objectsJson != null ? "provided" : "null", inheritDescription);

            // Validate required parameters
            if (requestType == null || requestType.trim().isEmpty()) {
                return formatError("Request type is required. Use 'K' (Workbench), 'W' (Customizing), or 'T' (Copy).");
            }

            // Parse objects JSON if provided
            List<TransportObject> objects = null;
            if (objectsJson != null && !objectsJson.trim().isEmpty()) {
                objects = parseObjectsJson(objectsJson);
            }

            boolean inherit = inheritDescription != null && inheritDescription;

            // Description is required unless inheriting from reference
            if (!inherit && (description == null || description.trim().isEmpty())) {
                return formatError("Description is required (max 60 chars), or use inheritDescription=true with a reference transport.");
            }

            // Build request with defaults
            TransportCreationRequest request = new TransportCreationRequest(
                requestType.toUpperCase(),
                description,
                targetSystem != null && !targetSystem.trim().isEmpty() ? targetSystem.toUpperCase() : null,
                referenceTransport != null && !referenceTransport.trim().isEmpty() ? referenceTransport.toUpperCase() : null,
                autoRelease != null ? autoRelease : false,
                objects,
                inherit
            );

            // Execute service
            TransportCreationResult result = transportCreationService.createTransportRequest(request);

            // Format response
            return formatSuccess(result);

        } catch (JCoException e) {
            logger.error("RFC error creating transport request", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating transport request", e);
            return formatError("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error creating transport request", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * Formats a successful result as JSON.
     *
     * @param result The transport creation result
     * @return JSON string
     */
    private String formatSuccess(TransportCreationResult result) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.success());
            response.put("status", result.status());
            response.put("statusDescription", result.getStatusDescription());
            response.put("transportNumber", result.transportNumber());
            response.put("taskNumber", result.taskNumber());
            response.put("requestType", result.requestType());
            response.put("requestTypeDescription", result.requestTypeDescription());
            response.put("objectsCopied", result.objectsCopied());
            response.put("message", result.message());

            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error formatting success response", e);
            return "{\"success\": true, \"error\": \"JSON formatting error\"}";
        }
    }

    /**
     * Formats an error message as JSON.
     *
     * @param errorMessage The error message
     * @return JSON string
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
     * <p>Expected format:
     * <pre>
     * [{"pgmid":"R3TR","object":"PROG","objName":"ZTEST"}]
     * </pre>
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
                "Invalid objects JSON format. Expected: [{\"pgmid\":\"R3TR\",\"object\":\"PROG\",\"objName\":\"ZTEST\"}]. Error: " + e.getMessage()
            );
        }
    }
}

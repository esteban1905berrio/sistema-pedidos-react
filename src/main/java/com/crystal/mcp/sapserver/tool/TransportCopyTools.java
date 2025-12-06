package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.TransportCopyRequest;
import com.crystal.mcp.sapserver.model.TransportCopyResult;
import com.crystal.mcp.sapserver.service.TransportCopyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP Tools for transport copy operations.
 *
 * <p>Provides tools for creating transport copies (Transport of Copies) from existing
 * transport requests. This is useful for:
 * <ul>
 *   <li>Moving the same objects to multiple systems</li>
 *   <li>Creating backups of transport contents</li>
 *   <li>Re-importing objects after system refreshes</li>
 * </ul>
 *
 * <p><b>Workflow:</b> The tool automatically handles the complete workflow:
 * <pre>
 * QUERY_TASKS → CREATE_TRANSPORT → COPY_OBJECTS → RELEASE (optional)
 * </pre>
 *
 * <p><b>ABAP Implementation:</b> Uses function module {@code ZCX_CREATE_TRANSPORT_COPY}
 * which delegates to class {@code ZCLCX_TRANSPORT_MANAGEMENT}.
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 */
@Component
public class TransportCopyTools {

    private static final Logger logger = LoggerFactory.getLogger(TransportCopyTools.class);

    private final TransportCopyService transportCopyService;
    private final ObjectMapper objectMapper;

    public TransportCopyTools(TransportCopyService transportCopyService,
                             ObjectMapper objectMapper) {
        this.transportCopyService = transportCopyService;
        this.objectMapper = objectMapper;
    }

    /**
     * MCP Tool: create_transport_copy
     *
     * <p>Creates a transport copy from one or multiple existing transport requests.
     * Automatically finds all related tasks (via E070 STRKORR) and copies all objects
     * to a new transport.
     *
     * <p><b>Single Transport Mode:</b> Use {@code sourceTransport} parameter
     * <p><b>Multiple Transport Mode:</b> Use {@code sourceTransports} list parameter.
     * The list will be concatenated with commas and sent to SAP FM as a single string
     * (e.g., "CADK911511,CADK911512,CADK911513").
     *
     * <p><b>Workflow:</b>
     * <ol>
     *   <li>Query E070 for main transport(s) and all tasks (WHERE trkorr = X OR strkorr = X)</li>
     *   <li>Extract objects from E071 and E071K</li>
     *   <li>Create new transport via TR_EXT_CREATE_REQUEST (type 'T')</li>
     *   <li>Copy objects via TR_REQUEST_CHOICE</li>
     *   <li>Optionally release via TR_RELEASE_REQUEST</li>
     * </ol>
     *
     * <p><b>Error Handling:</b> If any step fails, the newly created transport is
     * automatically deleted (rollback) to prevent orphaned transports.
     *
     * <p><b>Examples:</b>
     * <pre>
     * // Single transport with default settings (auto-release)
     * create_transport_copy("CADK911511", null, null, null, null)
     *
     * // Multiple transports
     * create_transport_copy(null, ["CADK911511", "CADK911512"], "S4D", "BACKUP", true)
     *
     * // Create transport copy without releasing (keep modifiable)
     * create_transport_copy("CADK911511", null, "S4D", "WIP", false)
     * </pre>
     *
     * @param sourceTransport Single source transport request number (e.g., "CADK911511", "DEVK900123").
     *                        Use this OR sourceTransports, not both.
     * @param sourceTransports List of source transport request numbers for batch processing.
     *                         Example: ["CADK911511", "CADK911512", "CADK911513"].
     *                         Use this OR sourceTransport, not both.
     * @param targetSystem Target system name (optional). Must match source transport's target system.
     *                     Examples: "S4D", "S4Q", "S4P". If null, uses source transport's target.
     * @param descriptionPrefix Prefix for transport description (optional).
     *                          Final format: "&lt;prefix&gt;: &lt;original_description&gt;".
     *                          Max 60 chars total. Default: "COPIA"
     * @param autoRelease Auto-release transport after creation (optional).
     *                    true: Release automatically, false: Keep modifiable. Default: true
     * @return JSON response with success status, new transport number(s), and message
     */
    @McpTool(
        description = "Create a transport copy from existing transport request(s). " +
                     "Supports single or multiple source transports. " +
                     "Copies all objects from source transport(s) (including tasks) to new transport(s). " +
                     "Workflow: QUERY_TASKS → CREATE_TRANSPORT → COPY_OBJECTS → RELEASE (auto by default). " +
                     "Examples: " +
                     "Single: create_transport_copy('CADK911511', null, 'S4D', 'COPIA'). " +
                     "Multiple: create_transport_copy(null, ['CADK911511','CADK911512'], 'S4D', 'BACKUP'). " +
                     "Without release: create_transport_copy('CADK911511', null, null, null, false)"
    )
    public String create_transport_copy(
        @McpToolParam(description = "Single source transport request number. Example: 'CADK911511', 'DEVK900123'. " +
                                "Use this OR sourceTransports, not both.")
        String sourceTransport,

        @McpToolParam(description = "List of source transport request numbers for batch processing. " +
                                "Example: ['CADK911511', 'CADK911512', 'CADK911513']. " +
                                "The list will be sent to SAP as comma-separated string. " +
                                "Use this OR sourceTransport, not both.")
        java.util.List<String> sourceTransports,

        @McpToolParam(description = "Target system name (optional). Must match source transport's target system. " +
                                "Examples: 'S4D', 'S4Q', 'S4P'. Default: Same as source transport.")
        String targetSystem,

        @McpToolParam(description = "Prefix for transport description (optional). " +
                                "Final description format: '<prefix>: <original_description>'. " +
                                "Max 60 chars total. Default: 'COPIA'")
        String descriptionPrefix,

        @McpToolParam(description = "Auto-release transport after creation. " +
                                "Default: true (released automatically). " +
                                "Only specify false if you want to keep the transport modifiable.")
        Boolean autoRelease
    ) {
        try {
            // Determine mode
            boolean hasSingle = sourceTransport != null && !sourceTransport.trim().isEmpty();
            boolean hasMultiple = sourceTransports != null && !sourceTransports.isEmpty();

            logger.info("MCP Tool called: create_transport_copy(sourceTransport={}, sourceTransports={}, " +
                       "targetSystem={}, descriptionPrefix={}, autoRelease={})",
                       sourceTransport, sourceTransports, targetSystem, descriptionPrefix, autoRelease);

            // Validate inputs
            if (!hasSingle && !hasMultiple) {
                return formatError("Either sourceTransport or sourceTransports is required");
            }

            if (hasSingle && hasMultiple) {
                return formatError("Cannot use both sourceTransport and sourceTransports. Use one or the other.");
            }

            // Build request with defaults
            TransportCopyRequest request;
            if (hasSingle) {
                // Single transport mode
                request = new TransportCopyRequest(
                    sourceTransport.toUpperCase(),
                    null,
                    targetSystem != null ? targetSystem.toUpperCase() : null,
                    descriptionPrefix != null && !descriptionPrefix.trim().isEmpty()
                        ? descriptionPrefix
                        : "COPIA",
                    autoRelease != null ? autoRelease : true
                );
            } else {
                // Multiple transports mode
                request = new TransportCopyRequest(
                    null,
                    sourceTransports,
                    targetSystem != null ? targetSystem.toUpperCase() : null,
                    descriptionPrefix != null && !descriptionPrefix.trim().isEmpty()
                        ? descriptionPrefix
                        : "COPIA",
                    autoRelease != null ? autoRelease : true
                );
            }

            // Execute service
            TransportCopyResult result = transportCopyService.createTransportCopy(request);

            // Format response
            return formatSuccess(result);

        } catch (JCoException e) {
            logger.error("RFC error creating transport copy", e);
            return formatError("RFC Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating transport copy", e);
            return formatError("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error creating transport copy", e);
            return formatError("Unexpected Error: " + e.getMessage());
        }
    }

    /**
     * Formats a successful result as JSON.
     *
     * <p>Includes release log if available to provide LLM with detailed context about
     * the transport copy/release operation. Also includes step-by-step results for
     * each phase of the workflow.
     *
     * @param result The transport copy result
     * @return JSON string
     */
    private String formatSuccess(TransportCopyResult result) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.success());
            response.put("status", result.status());
            response.put("statusDescription", result.getStatusDescription());
            response.put("newTransportNumber", result.newTransportNumber());
            response.put("message", result.message());

            // Include detailed step results for LLM to understand what happened
            Map<String, Object> steps = new LinkedHashMap<>();

            Map<String, Object> creationStep = new LinkedHashMap<>();
            creationStep.put("success", result.creationOk());
            creationStep.put("message", result.creationMsg());
            steps.put("creation", creationStep);

            Map<String, Object> objectsStep = new LinkedHashMap<>();
            objectsStep.put("success", result.objectsOk());
            objectsStep.put("message", result.objectsMsg());
            steps.put("objectsInclusion", objectsStep);

            Map<String, Object> releaseStep = new LinkedHashMap<>();
            releaseStep.put("success", result.releaseOk());
            releaseStep.put("message", result.releaseMsg());
            steps.put("release", releaseStep);

            response.put("workflowSteps", steps);

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
}

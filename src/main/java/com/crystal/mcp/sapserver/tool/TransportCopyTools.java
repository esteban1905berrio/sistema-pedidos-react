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
     * <p>Creates a transport copy from an existing transport request. Automatically finds
     * all related tasks (via E070 STRKORR) and copies all objects to a new transport.
     *
     * <p><b>Workflow:</b>
     * <ol>
     *   <li>Query E070 for main transport and all tasks (WHERE trkorr = X OR strkorr = X)</li>
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
     * // Create transport copy with default settings (auto-release)
     * create_transport_copy("CADK911511", null, null, null)
     *
     * // Create transport copy with custom prefix
     * create_transport_copy("CADK911511", "S4D", "BACKUP", true)
     *
     * // Create transport copy without releasing (keep modifiable)
     * create_transport_copy("CADK911511", "S4D", "WIP", false)
     * </pre>
     *
     * @param sourceTransport Source transport request number (e.g., "CADK911511", "DEVK900123").
     *                        Tool will automatically find all related tasks.
     * @param targetSystem Target system name (optional). Must match source transport's target system.
     *                     Examples: "S4D", "S4Q", "S4P". If null, uses source transport's target.
     * @param descriptionPrefix Prefix for transport description (optional).
     *                          Final format: "&lt;prefix&gt;: &lt;original_description&gt;".
     *                          Max 60 chars total. Default: "COPIA"
     * @param autoRelease Auto-release transport after creation (optional).
     *                    true: Release automatically, false: Keep modifiable. Default: true
     * @return JSON response with success status, new transport number, and message
     */
    @McpTool(
        description = "Create a transport copy from an existing transport request. " +
                     "Copies all objects from source transport (including tasks) to a new transport. " +
                     "Workflow: QUERY_TASKS → CREATE_TRANSPORT → COPY_OBJECTS → RELEASE (optional). " +
                     "Example: create_transport_copy('CADK911511', 'S4D', 'COPIA', true)"
    )
    public String create_transport_copy(
        @McpToolParam(description = "Source transport request number. Example: 'CADK911511', 'DEVK900123'. " +
                                "Tool will automatically find all related tasks.")
        String sourceTransport,

        @McpToolParam(description = "Target system name (optional). Must match source transport's target system. " +
                                "Examples: 'S4D', 'S4Q', 'S4P'. Default: Same as source transport.")
        String targetSystem,

        @McpToolParam(description = "Prefix for transport description (optional). " +
                                "Final description format: '<prefix>: <original_description>'. " +
                                "Max 60 chars total. Default: 'COPIA'")
        String descriptionPrefix,

        @McpToolParam(description = "Auto-release transport after creation (optional). " +
                                "true: Release automatically, false: Keep modifiable. Default: true")
        Boolean autoRelease
    ) {
        try {
            logger.info("MCP Tool called: create_transport_copy(sourceTransport={}, targetSystem={}, " +
                       "descriptionPrefix={}, autoRelease={})",
                       sourceTransport, targetSystem, descriptionPrefix, autoRelease);

            // Validate source transport
            if (sourceTransport == null || sourceTransport.trim().isEmpty()) {
                return formatError("Source transport number is required");
            }

            // Build request with defaults
            TransportCopyRequest request = new TransportCopyRequest(
                sourceTransport.toUpperCase(),
                targetSystem != null ? targetSystem.toUpperCase() : null,
                descriptionPrefix != null && !descriptionPrefix.trim().isEmpty()
                    ? descriptionPrefix
                    : "COPIA",
                autoRelease != null ? autoRelease : true
            );

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

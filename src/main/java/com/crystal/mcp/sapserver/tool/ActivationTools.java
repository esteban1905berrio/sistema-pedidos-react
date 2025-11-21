package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ActivationResult;
import com.crystal.mcp.sapserver.model.InactiveObject;
import com.crystal.mcp.sapserver.service.ActivationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Tools for ABAP Object Activation Operations.
 *
 * This component provides tools for:
 * - Checking inactive objects in the SAP system
 * - Activating objects after modification
 * - Validating syntax before activation
 *
 * Part of the modification workflow:
 * LOCK → MODIFY → UNLOCK → CHECK_SYNTAX → ACTIVATE
 *
 * Available Tools:
 * - get_inactive_objects: List all inactive objects
 * - activate_objects: Activate objects (with syntax validation)
 */
@Component
@RequiredArgsConstructor
public class ActivationTools {

    private final ActivationService activationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * MCP Tool: Get all inactive objects in the SAP system.
     *
     * Returns objects that have been modified but not yet activated, along with
     * their associated transport requests.
     *
     * Use cases:
     * - Check what objects need activation after modifications
     * - Review objects in development state
     * - Find objects in specific transports
     *
     * Token cost: ~1,000-3,000 tokens (depends on number of inactive objects).
     *
     * @return JSON with inactive objects list
     */
    @McpTool(
        description = """
            Get all inactive ABAP objects in the SAP system.

            Returns objects that have been modified but not yet activated, along with
            their associated transport requests. Useful for checking what objects need
            activation after modifications.

            Token cost: ~1,000-3,000 tokens (depends on number of inactive objects).
            """
    )
    public String getInactiveObjects() {
        try {
            List<InactiveObject> objects = activationService.getInactiveObjects();
            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "count", objects.size(),
                "inactiveObjects", objects
            ));
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            } catch (JsonProcessingException ex) {
                return "{\"success\":false,\"error\":\"Failed to serialize response\"}";
            }
        }
    }

    /**
     * MCP Tool: Activate ABAP objects.
     *
     * Activates objects that are in inactive state. This is the final step
     * in the modification workflow (LOCK → MODIFY → UNLOCK → ACTIVATE).
     *
     * Workflow:
     * 1. Checks syntax of all objects
     * 2. If syntax is correct: activates objects
     * 3. If syntax errors: returns detailed error messages
     *
     * Token cost: ~500-2,000 tokens (depends on errors).
     *
     * @param objectUris List of ADT URIs of objects to activate
     * @return JSON with activation result and errors (if any)
     */
    @McpTool(
        description = """
            Activate ABAP objects after modification.

            Activates objects that are in inactive state. This is the final step
            in the modification workflow (LOCK → MODIFY → UNLOCK → ACTIVATE).

            Checks syntax and returns detailed error messages if validation fails.

            Token cost: ~500-2,000 tokens (depends on errors).

            Note: Empty response from SAP = successful activation.
            """
    )
    public String activateObjects(
        @McpToolParam(
            description = """
                List of ADT URIs of objects to activate.

                Format examples:
                - Class: "/sap/bc/adt/oo/classes/zcl_test"
                - Program: "/sap/bc/adt/programs/programs/zrep_test"
                - Function Module: "/sap/bc/adt/functions/groups/zfg_test/fmodules/z_fm_test"

                URIs can be obtained from search_objects, get_inactive_objects, or modification responses.
                """,
            required = true
        )
        List<String> objectUris
    ) {
        try {
            ActivationResult result = activationService.activateObjects(objectUris);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "message", "Activation failed: " + e.getMessage(),
                    "errors", List.of()
                ));
            } catch (JsonProcessingException ex) {
                return "{\"success\":false,\"message\":\"Failed to serialize response\",\"errors\":[]}";
            }
        }
    }
}

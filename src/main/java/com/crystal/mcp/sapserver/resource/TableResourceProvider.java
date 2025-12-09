package com.crystal.mcp.sapserver.resource;

import com.crystal.mcp.sapserver.model.DdicSourceResult;
import com.crystal.mcp.sapserver.service.ClassService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Resource Provider for SAP DDIC Tables.
 *
 * Exposes SAP Data Dictionary table/structure information as read-only MCP Resources.
 * Resources are lightweight alternatives to Tools for data that:
 * - Is read-only (no side effects)
 * - Can be cached by clients
 * - Follows a predictable URI structure
 *
 * URI Template Pattern: sap://table/{name}/{aspect}
 *
 * Implemented Resources:
 * - sap://table/{name}/fields - Table field definitions from DD03L
 *
 * Token Optimization:
 * - fields: ~400-1500 tokens (depends on field count)
 *
 * Usage Example:
 * Client requests: resources/read { uri: "sap://table/MARA/fields" }
 * Response: JSON with field definitions (name, type, length, key flag)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TableResourceProvider {

    private final ClassService classService;
    private final ObjectMapper objectMapper;

    /**
     * Get table field definitions.
     *
     * Returns field metadata for a DDIC table/structure/view
     * from DD03L system table via FM ZCX_GETDDICSOURCE.
     *
     * Returned data:
     * - objectName: Table/structure name
     * - objectType: TABLE, STRUCTURE, VIEW, or APPEND
     * - fields: List of field definitions
     * - Each field: fieldname, datatype, length, keyFlag, mandatory, checkTable
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~400-1500 tokens (depends on field count)
     *
     * Example URI: sap://table/MARA/fields
     *
     * @param name table name (e.g., "MARA", "DD03L", "T001")
     * @return ReadResourceResult with field definitions
     */
    @McpResource(
            uri = "sap://table/{name}/fields",
            name = "Table Fields",
            description = "DDIC table/structure field definitions from DD03L. Token-optimized: ~400-1500 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getTableFields(String name) {
        log.info("Resource request: sap://table/{}/fields", name);

        DdicSourceResult result = classService.getDdicSource(name);

        // Check if object was found (no fields = not found)
        if (result.getFields() == null || result.getFields().isEmpty()) {
            String errorJson = "{\"error\": \"Table not found or has no fields\", \"tableName\": \"" + name + "\"}";
            return new McpSchema.ReadResourceResult(List.of(
                    new McpSchema.TextResourceContents(
                            "sap://table/" + name + "/fields",
                            "application/json",
                            errorJson
                    )
            ));
        }

        // Convert to lightweight JSON structure
        List<FieldInfo> fields = result.getFields().stream()
                .map(f -> new FieldInfo(
                        f.getFieldname(),
                        f.getPosition(),
                        f.getRollname(),
                        f.getDatatype(),
                        f.getIntlen(),
                        f.getInttype(),
                        "X".equals(f.getKeyflag()),
                        "X".equals(f.getMandatory()),
                        f.getChecktable(),
                        f.getReffield()
                ))
                .collect(Collectors.toList());

        TableFieldsData data = new TableFieldsData(
                result.getObjectName(),
                result.getObjectType(),
                result.getObjectStatus(),
                fields.size(),
                fields
        );

        String jsonContent = serializeToJson(data);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://table/" + name + "/fields",
                        "application/json",
                        jsonContent
                )
        ));
    }

    // ========================================================================
    // Helper DTOs
    // ========================================================================

    /**
     * Lightweight DTO for field information.
     */
    private record FieldInfo(
            String fieldname,
            int position,
            String rollname,
            String datatype,
            int length,
            String inttype,
            boolean isKey,
            boolean isMandatory,
            String checkTable,
            String refField
    ) {}

    /**
     * Container DTO for table fields response.
     */
    private record TableFieldsData(
            String objectName,
            String objectType,
            String status,
            int fieldCount,
            List<FieldInfo> fields
    ) {}

    /**
     * Serialize object to JSON string.
     */
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
}

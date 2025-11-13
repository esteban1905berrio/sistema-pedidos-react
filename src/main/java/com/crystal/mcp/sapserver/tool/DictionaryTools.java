package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.DictionaryObjectRequest;
import com.crystal.mcp.sapserver.model.DictionaryObjectResult;
import com.crystal.mcp.sapserver.model.TableField;
import com.crystal.mcp.sapserver.service.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP Tools for SAP Data Dictionary Object Creation.
 *
 * <p>This component provides tools for creating objects in SAP Data Dictionary:
 * <ul>
 *   <li>Transparent Tables (TABL/DT)</li>
 *   <li>Future: Data Elements (DTEL), Structures (TABL/ST), Table Types (TTYP), Domains (DOMA)</li>
 * </ul>
 *
 * <p><b>High-Level Interface:</b> Tools accept structured fields (JSON) instead of DDL raw.
 * DDL is generated automatically by DdlGenerator.
 *
 * <p>Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * @author Crystal Development Team
 * @since 1.0
 * @see TableService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DictionaryTools {

    private final TableService tableService;

    /**
     * MCP Tool: Create a new transparent table in SAP Data Dictionary.
     *
     * <p>Creates a table with structured fields (high-level interface).
     * DDL is generated automatically from the field list.
     *
     * <p><b>Workflow:</b>
     * <ol>
     *   <li>Validate input (name, package, fields, transport)</li>
     *   <li>Generate DDL from fields[] (automatic)</li>
     *   <li>POST /ddic/tables (create object)</li>
     *   <li>Stateful workflow: LOCK → MODIFY → UNLOCK</li>
     * </ol>
     *
     * <p><b>Field Structure:</b> Each field must have:
     * <ul>
     *   <li>name: Field name (max 16 chars, A-Z0-9_)</li>
     *   <li>type: ABAP type (e.g., "matnr", "abap.char(10)")</li>
     *   <li>isKey: true if field is part of primary key</li>
     * </ul>
     *
     * <p><b>Examples:</b>
     * <pre>{@code
     * // Local table in $TMP
     * create_table(
     *   "YTMP_1",
     *   "Temporary Test Table",
     *   [
     *     {name: "mat", type: "matnr", isKey: true},
     *     {name: "gjahr", type: "gjahr", isKey: false}
     *   ],
     *   "$TMP",
     *   null
     * )
     *
     * // Transportable table in ZTEST package
     * create_table(
     *   "ZTABLE_1",
     *   "Custom Table",
     *   [
     *     {name: "doc_number", type: "abap.char(10)", isKey: true},
     *     {name: "description", type: "abap.char(255)", isKey: false}
     *   ],
     *   "ZTEST",
     *   "CADK911122"
     * )
     * }</pre>
     *
     * @param name Table name (max 8 chars, A-Z0-9_)
     * @param description Table description
     * @param fields Array of field objects [{name, type, isKey}]
     * @param packageName Package name ($TMP for local, ZXXX for transportable)
     * @param transport Transport request (required if package != $TMP)
     * @return DictionaryObjectResult with URI, version, transport, etc.
     */
    @McpTool(
            description = "Create a new transparent table in SAP Data Dictionary. " +
                    "Accepts structured fields (JSON) and generates DDL automatically. " +
                    "Workflow: CREATE → LOCK → MODIFY → UNLOCK. " +
                    "Name must be max 8 chars, A-Z0-9_. " +
                    "Fields: [{name, type, isKey}]. " +
                    "Use $TMP for local tables (no transport). " +
                    "Example: create_table('YTMP_1', 'Test Table', [{name:'mat',type:'matnr',isKey:true}], '$TMP', null)"
    )
    public DictionaryObjectResult create_table(
            @McpToolParam(
                    description = "Table name (max 8 chars, A-Z0-9_). " +
                            "Convention: Start with Y or Z for custom tables. " +
                            "Examples: 'YTMP_1', 'ZTABLE_1', 'YCUSTOM'",
                    required = true
            )
            String name,

            @McpToolParam(
                    description = "Table description (used in @EndUserText.label annotation). " +
                            "Max 60 chars recommended. " +
                            "Example: 'Temporary Test Table'",
                    required = true
            )
            String description,

            @McpToolParam(
                    description = "Array of table fields. Each field must have: " +
                            "- name: Field name (max 16 chars, A-Z0-9_) " +
                            "- type: ABAP type (e.g., 'matnr', 'gjahr', 'abap.char(10)') " +
                            "- isKey: true if field is part of primary key " +
                            "Note: 'client' field is added automatically. " +
                            "Example: [{name:'mat',type:'matnr',isKey:true}, {name:'gjahr',type:'gjahr',isKey:false}]",
                    required = true
            )
            List<Map<String, Object>> fields,

            @McpToolParam(
                    description = "Package name. " +
                            "Use '$TMP' for local objects (no transport). " +
                            "Use 'Z*' or 'Y*' package for transportable objects. " +
                            "Examples: '$TMP', 'ZTEST', 'YCUSTOM'",
                    required = true
            )
            String packageName,

            @McpToolParam(
                    description = "Optional transport request number. " +
                            "Required if packageName != '$TMP'. " +
                            "Leave null for $TMP objects. " +
                            "Examples: 'CADK911122', 'DEVK900123', null",
                    required = false
            )
            String transport
    ) {
        log.info("MCP Tool: create_table(name={}, package={}, fields={})", name, packageName, fields.size());

        try {
            // Convert Map<String, Object> to List<TableField>
            List<TableField> tableFields = convertToTableFields(fields);

            // Build request DTO
            DictionaryObjectRequest request = new DictionaryObjectRequest(
                    name,
                    description,
                    tableFields,
                    packageName,
                    transport
            );

            // Call service
            DictionaryObjectResult result = tableService.createTable(request);

            log.info("Table {} created successfully: {}", name, result.getUri());
            return result;

        } catch (IllegalArgumentException e) {
            log.error("Validation error in create_table: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating table {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to create table: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte la lista de Map&lt;String, Object&gt; a List&lt;TableField&gt;.
     *
     * <p>Cada Map debe contener:
     * <ul>
     *   <li>name (String): Nombre del campo</li>
     *   <li>type (String): Tipo ABAP</li>
     *   <li>isKey (Boolean): true si es campo clave</li>
     * </ul>
     *
     * @param fieldMaps Lista de maps con datos de campos
     * @return Lista de TableField
     * @throws IllegalArgumentException si algún campo tiene formato inválido
     */
    private List<TableField> convertToTableFields(List<Map<String, Object>> fieldMaps) {
        if (fieldMaps == null || fieldMaps.isEmpty()) {
            throw new IllegalArgumentException("Fields list cannot be empty");
        }

        List<TableField> tableFields = new ArrayList<>();

        for (int i = 0; i < fieldMaps.size(); i++) {
            Map<String, Object> fieldMap = fieldMaps.get(i);

            // Validate required keys
            if (!fieldMap.containsKey("name")) {
                throw new IllegalArgumentException(
                        "Field at index " + i + " is missing 'name' property");
            }
            if (!fieldMap.containsKey("type")) {
                throw new IllegalArgumentException(
                        "Field at index " + i + " is missing 'type' property");
            }
            if (!fieldMap.containsKey("isKey")) {
                throw new IllegalArgumentException(
                        "Field at index " + i + " is missing 'isKey' property");
            }

            // Extract values
            String name = String.valueOf(fieldMap.get("name"));
            String type = String.valueOf(fieldMap.get("type"));
            Object isKeyObj = fieldMap.get("isKey");

            // Convert isKey to boolean
            boolean isKey;
            if (isKeyObj instanceof Boolean) {
                isKey = (Boolean) isKeyObj;
            } else if (isKeyObj instanceof String) {
                isKey = Boolean.parseBoolean((String) isKeyObj);
            } else {
                throw new IllegalArgumentException(
                        "Field '" + name + "' has invalid isKey value (must be boolean)");
            }

            // Optional: description
            String descriptionField = fieldMap.containsKey("description")
                    ? String.valueOf(fieldMap.get("description"))
                    : null;

            // Create TableField
            TableField tableField = new TableField(name, type, isKey, descriptionField);

            // Validate
            if (!tableField.isValidName()) {
                throw new IllegalArgumentException(
                        "Field '" + name + "' has invalid name (max 16 chars, A-Z0-9_)");
            }
            if (!tableField.isValidType()) {
                throw new IllegalArgumentException(
                        "Field '" + name + "' has invalid type");
            }

            tableFields.add(tableField);
        }

        return tableFields;
    }
}

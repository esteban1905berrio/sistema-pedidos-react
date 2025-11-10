package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TableContentsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for querying SAP table data.
 *
 * This service provides generic table query capabilities using the SAP ADT
 * data preview API. It's designed to be reusable across multiple use cases
 * that need direct table access.
 *
 * Architecture:
 * - Uses RfcAdapter to call ADT data preview endpoint
 * - Supports WHERE clauses for filtering
 * - Supports field selection
 * - Returns data in structured format (TableContentsResult)
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 *
 * Reference: python-legacy/app/services/query_service.py
 *
 * Use Cases:
 * - Transport system queries (E070, E071 tables)
 * - DDIC metadata queries
 * - Custom data retrieval
 *
 * Example:
 *   QueryService queryService = ...;
 *   TableContentsResult result = queryService.getTableContents(
 *       "E071",
 *       "OBJ_NAME LIKE '%TEST%' AND OBJECT = 'CLAS'",
 *       100,
 *       List.of("TRKORR", "OBJ_NAME", "LOCKFLAG")
 *   );
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final RfcAdapter rfcAdapter;

    /**
     * Get table contents with optional filtering and field selection.
     *
     * This method queries SAP tables using the ADT data preview API endpoint:
     * /sap/bc/adt/datapreview/ddic
     *
     * ADT API Details:
     * - Method: POST
     * - Body: SQL SELECT statement
     * - Response: XML with table data
     *
     * @param tableName    SAP table name (e.g., "E071", "E070", "TADIR")
     * @param whereClause  Optional WHERE clause without "WHERE" keyword
     *                     (e.g., "TRKORR = 'DEVK900123'")
     * @param maxRows      Maximum rows to retrieve (1-1000, default 100)
     * @param fields       Optional list of field names to retrieve (null = all fields)
     * @return TableContentsResult containing rows and metadata
     * @throws RuntimeException if query fails
     */
    public TableContentsResult getTableContents(
            String tableName,
            String whereClause,
            int maxRows,
            List<String> fields
    ) {
        // Validate inputs
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }

        // Validate and adjust maxRows
        if (maxRows < 1) {
            maxRows = 100;
        } else if (maxRows > 1000) {
            log.warn("maxRows {} exceeds limit, setting to 1000", maxRows);
            maxRows = 1000;
        }

        log.info("Querying table: {} (maxRows: {}, whereClause: {})",
                tableName, maxRows, whereClause != null ? whereClause : "none");

        try {
            // Build SQL SELECT statement
            String sqlStatement = buildSqlSelect(tableName, whereClause, fields);
            log.debug("SQL Statement: {}", sqlStatement);

            // Build request parameters
            Map<String, String> params = new HashMap<>();
            params.put("rowNumber", String.valueOf(maxRows));
            params.put("ddicEntityName", tableName);

            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    "/sap/bc/adt/datapreview/ddic",
                    "POST",
                    null,
                    params,
                    sqlStatement,
                    "application/vnd.sap.adt.datapreview.table.v1+xml"
            );

            // Check HTTP status
            if (response.statusCode() == 200) {
                log.debug("Successfully retrieved table data ({} bytes)",
                        response.text().length());

                // Parse XML response
                TableContentsResult result = parseTableData(response.text(), tableName);
                log.info("Retrieved {} rows from table {}", result.rowCount(), tableName);
                return result;

            } else {
                String errorMsg = String.format(
                        "Failed to query table '%s': HTTP %d - %s",
                        tableName,
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error querying table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to query table: " + tableName, e);
        }
    }

    /**
     * Build SQL SELECT statement for ADT data preview.
     *
     * Constructs SQL with optional WHERE clause and field selection.
     *
     * Examples:
     * - SELECT * FROM E071
     * - SELECT * FROM E071 WHERE TRKORR = 'DEVK900123'
     * - SELECT TRKORR, OBJ_NAME FROM E071 WHERE LOCKFLAG = 'X'
     *
     * @param tableName   Table name
     * @param whereClause Optional WHERE clause (without "WHERE" keyword)
     * @param fields      Optional field list (null = all fields)
     * @return SQL SELECT statement
     */
    private String buildSqlSelect(String tableName, String whereClause, List<String> fields) {
        StringBuilder sql = new StringBuilder("SELECT ");

        // Field list or *
        if (fields != null && !fields.isEmpty()) {
            sql.append(String.join(", ", fields));
        } else {
            sql.append("*");
        }

        // FROM clause
        sql.append(" FROM ").append(tableName);

        // WHERE clause (if provided)
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause.trim());
        }

        return sql.toString();
    }

    /**
     * Parse XML response from ADT data preview API.
     *
     * XML Structure (simplified):
     * <dataPreview>
     *   <metadata>
     *     <columns>
     *       <column name="FIELD1"/>
     *       <column name="FIELD2"/>
     *     </columns>
     *   </metadata>
     *   <data>
     *     <dataSet>
     *       <row>
     *         <column index="1">VALUE1</column>
     *         <column index="2">VALUE2</column>
     *       </row>
     *       ...
     *     </dataSet>
     *   </data>
     * </dataPreview>
     *
     * @param xml       XML response text
     * @param tableName Table name for result
     * @return TableContentsResult with parsed data
     * @throws Exception if XML parsing fails
     */
    private TableContentsResult parseTableData(String xml, String tableName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)
        ));

        // Parse column names from metadata
        List<String> columns = new ArrayList<>();

        // First pass: get column names from metadata section
        Element metadataElement = (Element) doc.getElementsByTagName("metadata").item(0);
        if (metadataElement != null) {
            NodeList metadataColumns = metadataElement.getElementsByTagName("column");
            for (int i = 0; i < metadataColumns.getLength(); i++) {
                Element columnElement = (Element) metadataColumns.item(i);
                String columnName = columnElement.getAttribute("name");
                if (columnName != null && !columnName.isEmpty()) {
                    columns.add(columnName);
                }
            }
        }

        // Parse rows from data section
        List<Map<String, String>> rows = new ArrayList<>();
        NodeList rowNodes = doc.getElementsByTagName("row");

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowElement = (Element) rowNodes.item(i);
            Map<String, String> rowData = new LinkedHashMap<>();

            // Get all column elements in this row
            NodeList rowColumns = rowElement.getElementsByTagName("column");
            for (int j = 0; j < rowColumns.getLength(); j++) {
                Element columnElement = (Element) rowColumns.item(j);
                String indexStr = columnElement.getAttribute("index");
                String value = columnElement.getTextContent().trim();

                // Map value to column name using index
                try {
                    int index = Integer.parseInt(indexStr) - 1; // 1-based to 0-based
                    if (index >= 0 && index < columns.size()) {
                        String columnName = columns.get(index);
                        rowData.put(columnName, value);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid column index: {}", indexStr);
                }
            }

            if (!rowData.isEmpty()) {
                rows.add(rowData);
            }
        }

        log.debug("Parsed {} columns and {} rows", columns.size(), rows.size());

        return new TableContentsResult(
                tableName,
                rows,
                columns,
                rows.size()
        );
    }
}

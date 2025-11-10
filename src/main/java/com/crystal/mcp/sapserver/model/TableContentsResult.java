package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.Map;

/**
 * Result model for SAP table contents query.
 *
 * This record encapsulates the results from querying SAP tables using
 * the ADT data preview API (/sap/bc/adt/datapreview/ddic).
 *
 * Used by QueryService for generic table access operations.
 *
 * Thread Safety: Immutable record, inherently thread-safe.
 *
 * @param tableName  Name of the queried table
 * @param rows       List of rows, each row is a Map of field→value
 * @param columns    List of column names in the table
 * @param rowCount   Number of rows returned
 * @param totalRows  Total rows available (if known, otherwise same as rowCount)
 */
public record TableContentsResult(
        String tableName,
        List<Map<String, String>> rows,
        List<String> columns,
        int rowCount,
        int totalRows
) {
    /**
     * Create result with same rowCount and totalRows.
     */
    public TableContentsResult(String tableName, List<Map<String, String>> rows, List<String> columns, int rowCount) {
        this(tableName, rows, columns, rowCount, rowCount);
    }
}

package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result of ST05 trace analysis for a transaction execution.
 *
 * Contains:
 * - Summary metrics (total statements, tables, duration)
 * - Detailed trace records (individual SQL statements with source location)
 * - Call stack entries (ABAP execution flow)
 * - Table access summaries (aggregated statistics per table)
 *
 * Designed for LLM consumption with markdown-formatted output methods.
 *
 * @param transaction        Transaction code executed
 * @param variant            Selection variant used (if any)
 * @param program            Main program executed
 * @param executionTimeMs    Total execution time in milliseconds
 * @param totalStatements    Total number of SQL statements traced
 * @param totalTables        Number of distinct tables accessed
 * @param traceStartDate     Trace start date (YYYYMMDD)
 * @param traceStartTime     Trace start time (HHMMSS)
 * @param traceEndDate       Trace end date (YYYYMMDD)
 * @param traceEndTime       Trace end time (HHMMSS)
 * @param detailedRecords    Individual SQL trace records
 * @param callStackItems     ABAP call stack entries
 * @param tableAccessRecords Aggregated table access statistics
 * @param message            Status message or error description
 */
public record TraceAnalysisResult(
        String transaction,
        String variant,
        String program,
        long executionTimeMs,
        int totalStatements,
        int totalTables,
        String traceStartDate,
        String traceStartTime,
        String traceEndDate,
        String traceEndTime,
        List<TraceDetailedRecord> detailedRecords,
        List<TraceCallStackItem> callStackItems,
        List<TraceTableAccessRecord> tableAccessRecords,
        String message
) {
    /**
     * Creates a successful result.
     */
    public static TraceAnalysisResult success(
            String transaction,
            String variant,
            String program,
            long executionTimeMs,
            String traceStartDate,
            String traceStartTime,
            String traceEndDate,
            String traceEndTime,
            List<TraceDetailedRecord> detailedRecords,
            List<TraceCallStackItem> callStackItems,
            List<TraceTableAccessRecord> tableAccessRecords) {

        int totalStatements = detailedRecords != null ? detailedRecords.size() : 0;
        int totalTables = tableAccessRecords != null ? tableAccessRecords.size() : 0;

        return new TraceAnalysisResult(
                transaction, variant, program, executionTimeMs,
                totalStatements, totalTables,
                traceStartDate, traceStartTime, traceEndDate, traceEndTime,
                detailedRecords, callStackItems, tableAccessRecords,
                "Trace analysis completed successfully"
        );
    }

    /**
     * Creates an error result.
     */
    public static TraceAnalysisResult error(String errorMessage) {
        return new TraceAnalysisResult(
                null, null, null, 0, 0, 0,
                null, null, null, null,
                List.of(), List.of(), List.of(),
                errorMessage
        );
    }

    /**
     * Generates markdown-formatted summary for LLM consumption.
     */
    public String toMarkdownSummary() {
        if (message != null && !message.contains("successfully")) {
            return "## Error\n" + message;
        }

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("## Trace Analysis: ").append(transaction);
        if (variant != null && !variant.isEmpty()) {
            sb.append(" (variant: ").append(variant).append(")");
        }
        sb.append("\n\n");

        // Summary metrics
        sb.append("### Summary\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Program | ").append(program != null ? program : "-").append(" |\n");
        sb.append("| Execution Time | ").append(executionTimeMs).append(" ms |\n");
        sb.append("| SQL Statements | ").append(totalStatements).append(" |\n");
        sb.append("| Tables Accessed | ").append(totalTables).append(" |\n");
        sb.append("| Trace Period | ").append(traceStartDate).append(" ")
                .append(traceStartTime).append(" - ")
                .append(traceEndTime).append(" |\n\n");

        // Table access summary (top 20)
        if (tableAccessRecords != null && !tableAccessRecords.isEmpty()) {
            sb.append("### Tables Accessed (Top 20)\n");
            sb.append("| Table | Operation | Executions | Rows | Duration (μs) | Description |\n");
            sb.append("|-------|-----------|------------|------|---------------|-------------|\n");

            tableAccessRecords.stream()
                    .sorted((a, b) -> Long.compare(b.duration(), a.duration()))
                    .limit(20)
                    .forEach(r -> {
                        sb.append("| ").append(r.object() != null ? r.object() : "-");
                        sb.append(" | ").append(r.statementType() != null ? r.statementType() : "-");
                        sb.append(" | ").append(r.numberOfExecutions());
                        sb.append(" | ").append(r.rows());
                        sb.append(" | ").append(r.duration());
                        sb.append(" | ").append(r.ddtext() != null ? r.ddtext() : "-");
                        sb.append(" |\n");
                    });
            sb.append("\n");
        }

        // Execution flow (top 30 by duration)
        if (detailedRecords != null && !detailedRecords.isEmpty()) {
            sb.append("### Execution Flow (Top 30 by Duration)\n");
            sb.append("| # | Table | Operation | Program:Line | Duration (μs) | Rows |\n");
            sb.append("|---|-------|-----------|--------------|---------------|------|\n");

            detailedRecords.stream()
                    .sorted((a, b) -> Long.compare(b.duration(), a.duration()))
                    .limit(30)
                    .forEach(r -> {
                        sb.append("| ").append(r.recordNumber());
                        sb.append(" | ").append(r.object() != null ? r.object() : "-");
                        sb.append(" | ").append(r.operation() != null ? r.operation() : r.statementType());
                        sb.append(" | ").append(r.program() != null ? r.program() : "-")
                                .append(":").append(r.offset());
                        sb.append(" | ").append(r.duration());
                        sb.append(" | ").append(r.rows());
                        sb.append(" |\n");
                    });
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Gets distinct tables accessed with their descriptions.
     */
    public List<String> getDistinctTables() {
        if (tableAccessRecords == null) return List.of();
        return tableAccessRecords.stream()
                .map(r -> r.object() + (r.ddtext() != null ? " (" + r.ddtext() + ")" : ""))
                .distinct()
                .toList();
    }
}

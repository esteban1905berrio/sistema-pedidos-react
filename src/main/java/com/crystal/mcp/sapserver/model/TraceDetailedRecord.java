package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a detailed trace record from ST05 SQL trace.
 *
 * Maps to SAP structure ST05_DETAILED_RECORD.
 * Contains information about a single SQL statement execution including:
 * - Table/object accessed
 * - SQL statement with values
 * - Source code location (program + offset)
 * - Performance metrics (duration)
 * - Call context (transaction, variables)
 *
 * Key fields for analysis:
 * - object: Table name accessed
 * - statementWithValues: Full SQL with actual values
 * - program + offset: Source code location
 * - duration: Execution time in microseconds
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraceDetailedRecord(
        @JsonProperty("date") String date,
        @JsonProperty("time") String time,
        @JsonProperty("duration") long duration,
        @JsonProperty("object") String object,
        @JsonProperty("statement_with_values") String statementWithValues,
        @JsonProperty("program") String program,
        @JsonProperty("offset") int offset,
        @JsonProperty("operation") String operation,
        @JsonProperty("trace_type") String traceType,
        @JsonProperty("transaction") String transaction,
        @JsonProperty("record_number") int recordNumber,
        @JsonProperty("rows") int rows,
        @JsonProperty("statement_type") String statementType,
        @JsonProperty("variables") String variables
) {
    /**
     * Creates a summary string for LLM-optimized output.
     */
    public String toSummary() {
        return String.format("%s | %s | %s:%d | %dμs | %d rows",
                object != null ? object : "-",
                operation != null ? operation : statementType,
                program != null ? program : "-",
                offset,
                duration,
                rows);
    }
}

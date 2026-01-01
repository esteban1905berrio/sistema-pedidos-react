package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents aggregated table access statistics from ST05 trace.
 *
 * Maps to SAP structure ST05_TABLE_ACCESS_RECORD.
 * Provides summary of operations per table including execution counts and duration.
 *
 * Key fields for analysis:
 * - object: Table name
 * - statementType: SELECT, INSERT, UPDATE, DELETE, etc.
 * - numberOfExecutions: How many times this operation was executed
 * - duration: Total time spent on this table/operation combination
 * - ddtext: Data dictionary text (table description)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraceTableAccessRecord(
        @JsonProperty("object") String object,
        @JsonProperty("statement_type") String statementType,
        @JsonProperty("duration") long duration,
        @JsonProperty("number_of_executions") int numberOfExecutions,
        @JsonProperty("rows") int rows,
        @JsonProperty("ddtext") String ddtext,
        @JsonProperty("buffer_state") String bufferState,
        @JsonProperty("identical_count") int identicalCount,
        @JsonProperty("time_per_record") long timePerRecord
) {
    /**
     * Creates a summary line for table access.
     */
    public String toSummary() {
        return String.format("%-30s | %-8s | %5d exec | %8d rows | %10dμs | %s",
                object != null ? (object.length() > 30 ? object.substring(0, 27) + "..." : object) : "-",
                statementType != null ? statementType : "-",
                numberOfExecutions,
                rows,
                duration,
                ddtext != null ? ddtext : "");
    }
}

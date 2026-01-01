package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a call stack entry from ST05 trace.
 *
 * Maps to SAP structure ST05_KERNEL_CALL_STACK_ITEM.
 * Links trace records to their ABAP call stack position.
 *
 * Key fields for analysis:
 * - recordNumber: Links to TraceDetailedRecord
 * - level: Stack depth (0 = top level)
 * - progInfo: Full program/include/line information
 * - eventType: FORM, METHOD, FUNCTION-MODULE, etc.
 * - eventName: Name of the form/method/function
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraceCallStackItem(
        @JsonProperty("record_number") int recordNumber,
        @JsonProperty("level") int level,
        @JsonProperty("prog_info") String progInfo,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("event_name") String eventName,
        @JsonProperty("line") int line,
        @JsonProperty("column") int column,
        @JsonProperty("include") String include,
        @JsonProperty("program") String program
) {
    /**
     * Creates a formatted call stack line.
     */
    public String toStackLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ".repeat(level)); // Indent by level

        if (eventType != null && !eventType.isEmpty()) {
            sb.append(eventType).append(" ");
        }
        if (eventName != null && !eventName.isEmpty()) {
            sb.append(eventName);
        }
        if (program != null && !program.isEmpty()) {
            sb.append(" in ").append(program);
            if (include != null && !include.isEmpty() && !include.equals(program)) {
                sb.append("/").append(include);
            }
            if (line > 0) {
                sb.append(":").append(line);
            }
        }

        return sb.toString();
    }
}

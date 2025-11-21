package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Result of ABAP syntax check operation.
 *
 * Represents the response from ADT checkruns API which validates
 * ABAP object syntax and returns errors, warnings, and informational messages.
 */
public record SyntaxCheckResult(
    @JsonProperty("object_uri") String objectUri,
    @JsonProperty("version") String version,
    @JsonProperty("status") String status,
    @JsonProperty("status_text") String statusText,
    @JsonProperty("messages") List<CheckMessage> messages,
    @JsonProperty("has_errors") boolean hasErrors,
    @JsonProperty("has_warnings") boolean hasWarnings,
    @JsonProperty("total_messages") int totalMessages
) {

    /**
     * Individual syntax check message (error, warning, or info).
     */
    public record CheckMessage(
        @JsonProperty("uri") String uri,
        @JsonProperty("type") String type,
        @JsonProperty("short_text") String shortText,
        @JsonProperty("line") Integer line,
        @JsonProperty("column") Integer column
    ) {
        /**
         * Check if message is an error.
         */
        public boolean isError() {
            return "E".equals(type);
        }

        /**
         * Check if message is a warning.
         */
        public boolean isWarning() {
            return "W".equals(type);
        }

        /**
         * Check if message is informational.
         */
        public boolean isInfo() {
            return "I".equals(type);
        }

        /**
         * Get formatted location string for display.
         */
        public String getLocation() {
            if (line != null && column != null) {
                return String.format("Line %d, Column %d", line, column);
            } else if (line != null) {
                return String.format("Line %d", line);
            }
            return "Unknown location";
        }
    }

    /**
     * Check if syntax check passed (no errors).
     */
    public boolean isPassed() {
        return !hasErrors;
    }

    /**
     * Get summary text for display.
     */
    public String getSummary() {
        if (!hasErrors && !hasWarnings) {
            return "Syntax check passed - no issues found";
        }

        StringBuilder sb = new StringBuilder();
        if (hasErrors) {
            long errorCount = messages.stream().filter(CheckMessage::isError).count();
            sb.append(errorCount).append(" error(s)");
        }
        if (hasWarnings) {
            if (sb.length() > 0) sb.append(", ");
            long warningCount = messages.stream().filter(CheckMessage::isWarning).count();
            sb.append(warningCount).append(" warning(s)");
        }
        return sb.toString();
    }
}

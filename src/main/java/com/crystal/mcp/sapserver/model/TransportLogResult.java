package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Result for transport log queries.
 *
 * This record contains transport log information filtered by errors and warnings.
 * Uses the FM ZCX_GET_TRANSPORT_LOGS which calls IF_CTS_REST_API->READ_GLOBAL_INFO.
 *
 * Token Optimization:
 * - Only returns transports with problems (errors/warnings)
 * - Transports without issues are summarized in counts
 *
 * @param success Whether the query succeeded
 * @param message Error message if failed
 * @param query Query parameters used
 * @param summary Summary statistics
 * @param transports List of transports with problems
 */
public record TransportLogResult(
    @JsonProperty("success") boolean success,
    @JsonProperty("message") String message,
    @JsonProperty("query") QueryInfo query,
    @JsonProperty("summary") Summary summary,
    @JsonProperty("transports") List<TransportLogEntry> transports
) {

    /**
     * Query information for traceability.
     */
    public record QueryInfo(
        @JsonProperty("transports_requested") List<String> transportsRequested,
        @JsonProperty("user_filter") String userFilter,
        @JsonProperty("timestamp") String timestamp
    ) {}

    /**
     * Summary statistics.
     */
    public record Summary(
        @JsonProperty("total_transports") int totalTransports,
        @JsonProperty("with_errors") int withErrors,
        @JsonProperty("with_warnings") int withWarnings,
        @JsonProperty("without_log") int withoutLog
    ) {}

    /**
     * Individual transport log entry.
     */
    public record TransportLogEntry(
        @JsonProperty("trkorr") String trkorr,
        @JsonProperty("owner") String owner,
        @JsonProperty("type") String type,
        @JsonProperty("type_text") String typeText,
        @JsonProperty("description") String description,
        @JsonProperty("has_log") boolean hasLog,
        @JsonProperty("has_problems") boolean hasProblems,
        @JsonProperty("error_count") int errorCount,
        @JsonProperty("warning_count") int warningCount,
        @JsonProperty("problems") List<Problem> problems,
        @JsonProperty("message") String message
    ) {
        /**
         * Check if transport has errors.
         */
        public boolean hasErrors() {
            return errorCount > 0;
        }

        /**
         * Check if transport has warnings.
         */
        public boolean hasWarnings() {
            return warningCount > 0;
        }
    }

    /**
     * Individual problem (error or warning) from the log.
     */
    public record Problem(
        @JsonProperty("severity") String severity,
        @JsonProperty("message") String message,
        @JsonProperty("system") String system,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("step") String step,
        @JsonProperty("step_text") String stepText
    ) {
        /**
         * Check if this is an error.
         */
        public boolean isError() {
            return "E".equals(severity);
        }

        /**
         * Check if this is a warning.
         */
        public boolean isWarning() {
            return "W".equals(severity);
        }
    }

    /**
     * Create a failure result.
     */
    public static TransportLogResult failure(String errorMessage) {
        return new TransportLogResult(
            false,
            errorMessage,
            null,
            null,
            List.of()
        );
    }

    /**
     * Create a success result.
     */
    public static TransportLogResult success(
            QueryInfo query,
            Summary summary,
            List<TransportLogEntry> transports
    ) {
        return new TransportLogResult(
            true,
            null,
            query,
            summary,
            transports
        );
    }

    /**
     * Check if any transport has problems.
     */
    public boolean hasProblems() {
        return summary != null && (summary.withErrors() > 0 || summary.withWarnings() > 0);
    }

    /**
     * Get total problem count.
     */
    public int totalProblems() {
        return transports.stream()
            .mapToInt(t -> t.errorCount() + t.warningCount())
            .sum();
    }
}

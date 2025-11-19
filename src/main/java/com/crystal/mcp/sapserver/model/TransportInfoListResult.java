package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Result for transport info queries that can return multiple transports.
 *
 * This record supports both single and multiple transport queries.
 * The FM Z_CX_GET_TRANSPORT_INFO now accepts comma-separated transport
 * numbers and returns a JSON array.
 *
 * @param success Whether the query succeeded
 * @param message Error message if failed
 * @param transports List of transport metadata
 * @param totalCount Total number of transports returned
 */
public record TransportInfoListResult(
    @JsonProperty("success") boolean success,
    @JsonProperty("message") String message,
    @JsonProperty("transports") List<TransportInfo> transports,
    @JsonProperty("total_count") int totalCount
) {

    /**
     * Individual transport metadata.
     */
    public record TransportInfo(
        @JsonProperty("transport_number") String transportNumber,
        @JsonProperty("transport_type") String transportType,
        @JsonProperty("transport_type_desc") String transportTypeDesc,
        @JsonProperty("status") String status,
        @JsonProperty("status_desc") String statusDesc,
        @JsonProperty("owner") String owner,
        @JsonProperty("description") String description,
        @JsonProperty("created_date") String createdDate,
        @JsonProperty("created_time") String createdTime,
        @JsonProperty("target_system") String targetSystem,
        @JsonProperty("category") String category,
        @JsonProperty("parent_transport") String parentTransport,
        @JsonProperty("object_count") int objectCount,
        @JsonProperty("task_count") int taskCount
    ) {
        /**
         * Check if transport has objects.
         */
        public boolean hasObjects() {
            return objectCount > 0;
        }

        /**
         * Check if transport has tasks.
         */
        public boolean hasTasks() {
            return taskCount > 0;
        }

        /**
         * Check if transport is a main transport (not a task).
         */
        public boolean isMainTransport() {
            return !"S".equals(transportType);
        }

        /**
         * Check if transport is released.
         */
        public boolean isReleased() {
            return "R".equals(status) || "O".equals(status);
        }

        /**
         * Check if transport is modifiable.
         */
        public boolean isModifiable() {
            return "D".equals(status) || "N".equals(status);
        }
    }

    /**
     * Create a failure result.
     */
    public static TransportInfoListResult failure(String errorMessage) {
        return new TransportInfoListResult(
            false,
            errorMessage,
            List.of(),
            0
        );
    }

    /**
     * Create a success result with single transport.
     */
    public static TransportInfoListResult success(TransportInfo transport) {
        return new TransportInfoListResult(
            true,
            null,
            List.of(transport),
            1
        );
    }

    /**
     * Create a success result with multiple transports.
     */
    public static TransportInfoListResult success(List<TransportInfo> transports) {
        return new TransportInfoListResult(
            true,
            null,
            transports,
            transports.size()
        );
    }

    /**
     * Get the first transport (for single transport queries).
     */
    public TransportInfo getFirst() {
        return transports.isEmpty() ? null : transports.get(0);
    }
}

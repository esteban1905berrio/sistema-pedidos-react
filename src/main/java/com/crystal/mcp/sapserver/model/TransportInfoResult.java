package com.crystal.mcp.sapserver.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object for transport request metadata.
 *
 * This record represents transport metadata retrieved from SAP table E070.
 * It provides complete information about a transport request without
 * including the full object list.
 *
 * Progressive Discovery Integration:
 * Use when you need transport metadata without loading all objects.
 * For full object details, use get_transport_objects instead.
 *
 * @param success          whether the query succeeded
 * @param transportNumber  transport request number
 * @param transportType    transport type code (K=Workbench, S=Task, etc.)
 * @param transportTypeDesc human-readable transport type
 * @param status           status code (D=Modifiable, R=Released, etc.)
 * @param statusDesc       human-readable status
 * @param owner            transport owner user ID
 * @param description      transport description text
 * @param createdDate      creation date (YYYY-MM-DD)
 * @param createdTime      creation time (HH:MM:SS)
 * @param targetSystem     target system for import
 * @param category         category (KORRDEV)
 * @param parentTransport  parent transport number (for tasks)
 * @param hasObjects       whether transport contains objects
 * @param hasTasks         whether transport has tasks (main transports)
 */
public record TransportInfoResult(
        boolean success,
        String transportNumber,
        String transportType,
        String transportTypeDesc,
        String status,
        String statusDesc,
        String owner,
        String description,
        String createdDate,
        String createdTime,
        String targetSystem,
        String category,
        String parentTransport,
        boolean hasObjects,
        boolean hasTasks
) {
    /**
     * Create a "not found" result when transport doesn't exist.
     *
     * @param transportNumber Transport number that was not found
     * @return TransportInfoResult indicating transport not found
     */
    public static TransportInfoResult notFound(String transportNumber) {
        return new TransportInfoResult(
                false,
                transportNumber,
                "",
                "Transport not found",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                null,
                false,
                false
        );
    }

    /**
     * Create a "failure" result when an error occurs.
     *
     * @param transportNumber Transport number
     * @param errorMessage    Error message
     * @return TransportInfoResult indicating failure
     */
    public static TransportInfoResult failure(String transportNumber, String errorMessage) {
        return new TransportInfoResult(
                false,
                transportNumber,
                "",
                errorMessage,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                null,
                false,
                false
        );
    }

    /**
     * Convert to Map for backward compatibility with existing code.
     *
     * @return Map representation of transport metadata
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("transport_number", transportNumber);
        map.put("transport_type", transportType);
        map.put("transport_type_desc", transportTypeDesc);
        map.put("status", status);
        map.put("status_desc", statusDesc);
        map.put("owner", owner);
        map.put("description", description);
        map.put("created_date", createdDate);
        map.put("created_time", createdTime);
        map.put("target_system", targetSystem);
        map.put("category", category);
        if (parentTransport != null) {
            map.put("parent_transport", parentTransport);
        }
        map.put("has_objects", hasObjects);
        map.put("has_tasks", hasTasks);
        return map;
    }
}

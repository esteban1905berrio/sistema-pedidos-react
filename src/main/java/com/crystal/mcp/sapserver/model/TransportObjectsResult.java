package com.crystal.mcp.sapserver.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for transport request objects.
 *
 * This record represents complete transport information retrieved from
 * SAP tables E070 (transport headers) and E071 (transport objects).
 *
 * Progressive Discovery Integration:
 * Use after list_user_transports identifies a transport,
 * to get detailed object list and metadata.
 *
 * @param success         whether the query succeeded
 * @param transportNumber transport request number
 * @param metadata        transport metadata (owner, status, dates, etc.)
 * @param objects         list of ABAP objects in the transport
 * @param totalObjects    total number of objects
 * @param tasks           list of tasks (for main transports only)
 */
public record TransportObjectsResult(
        boolean success,
        String transportNumber,
        Map<String, Object> metadata,
        List<TransportObject> objects,
        int totalObjects,
        List<Task> tasks
) {
    /**
     * Individual ABAP object in a transport.
     *
     * @param trkorr       transport/task number containing this object
     * @param pgmid        program ID (R3TR, LIMU, etc.)
     * @param objectType   object type (CLAS, PROG, FUGR, TABL, etc.)
     * @param objectName   object name
     * @param lockFlag     lock flag (X = locked)
     * @param gennum       generation number
     * @param tabKey       table key (for table entries)
     */
    public record TransportObject(
            String trkorr,
            String pgmid,
            String objectType,
            String objectName,
            String lockFlag,
            String gennum,
            String tabKey
    ) {
    }

    /**
     * Transport task (subtask of main transport).
     *
     * @param taskNumber   task transport number
     * @param owner        task owner
     * @param createdDate  creation date (YYYY-MM-DD)
     * @param createdTime  creation time (HH:MM:SS)
     * @param status       task status code (D, R, L, etc.)
     * @param statusDesc   task status description
     * @param description  task description text
     * @param objectCount  number of objects in task
     */
    public record Task(
            String taskNumber,
            String owner,
            String createdDate,
            String createdTime,
            String status,
            String statusDesc,
            String description,
            int objectCount
    ) {
    }

    /**
     * Create a "not found" result when transport doesn't exist.
     *
     * @param transportNumber Transport number that was not found
     * @return TransportObjectsResult indicating transport not found
     */
    public static TransportObjectsResult notFound(String transportNumber) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transport_number", transportNumber);
        metadata.put("error", "Transport not found in E070 table");

        return new TransportObjectsResult(
                false,
                transportNumber,
                metadata,
                new ArrayList<>(),
                0,
                new ArrayList<>()
        );
    }

    /**
     * Create a "failure" result when an error occurs.
     *
     * @param transportNumber Transport number
     * @param errorMessage    Error message
     * @return TransportObjectsResult indicating failure
     */
    public static TransportObjectsResult failure(String transportNumber, String errorMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transport_number", transportNumber);
        metadata.put("error", errorMessage);

        return new TransportObjectsResult(
                false,
                transportNumber,
                metadata,
                new ArrayList<>(),
                0,
                new ArrayList<>()
        );
    }
}

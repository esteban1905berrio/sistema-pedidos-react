package com.crystal.mcp.sapserver.model;

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
     * @param objectType   object type (CLAS, PROG, FUGR, TABL, etc.)
     * @param objectName   object name
     * @param lockType     lock type (M=Modifiable, X=Deleted, etc.)
     * @param function     function (K=New, D=Delete, etc.)
     * @param tabKey       table key (for table entries)
     */
    public record TransportObject(
            String objectType,
            String objectName,
            String lockType,
            String function,
            String tabKey
    ) {
    }

    /**
     * Transport task (subtask of main transport).
     *
     * @param taskNumber   task transport number
     * @param owner        task owner
     * @param createdDate  creation date (YYYY-MM-DD)
     * @param status       task status (D=Modifiable, R=Released)
     * @param objectCount  number of objects in task
     */
    public record Task(
            String taskNumber,
            String owner,
            String createdDate,
            String status,
            int objectCount
    ) {
    }
}

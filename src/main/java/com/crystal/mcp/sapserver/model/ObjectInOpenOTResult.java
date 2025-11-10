package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result model for checking if an ABAP object is in open (non-released) transport requests.
 *
 * This record encapsulates the results from querying E071 and E070 tables to determine:
 * - Which transport requests contain a specific object
 * - Whether the object is locked (LOCKFLAG = 'X')
 * - Whether the transport is open/modifiable (TRSTATUS = 'D' or 'L')
 *
 * Used by TransportService.getObjectInOpenOT() method.
 *
 * Thread Safety: Immutable record, inherently thread-safe.
 *
 * Workflow Context:
 * - Developer asks: "Can I modify object X?"
 * - This result shows if object is in open transports
 * - If locked, shows which user has it locked
 * - If in released transport, won't appear in results
 *
 * @param success        Whether the query executed successfully
 * @param objectName     Object name that was searched
 * @param searchPattern  Actual search pattern used (e.g., "%INVOICE%")
 * @param transports     List of open transports containing the object
 * @param totalTransports Total count of transports found
 */
public record ObjectInOpenOTResult(
        boolean success,
        String objectName,
        String searchPattern,
        List<TransportInfo> transports,
        int totalTransports
) {
    /**
     * Information about a transport request containing the object.
     *
     * @param transportNumber Transport number (TRKORR from E070/E071)
     * @param transportType   Transport type: 'K'=Workbench, 'S'=Task, 'W'=Workbench Request
     * @param transportTypeDesc Human-readable transport type
     * @param status          Transport status: 'D'=Modifiable, 'L'=Protected
     * @param statusDesc      Human-readable status
     * @param owner           Transport owner (AS4USER from E070)
     * @param createdDate     Creation date (YYYY-MM-DD format)
     * @param createdTime     Creation time (HH:MM:SS format)
     * @param isLocked        Whether the object is locked in this transport (LOCKFLAG = 'X')
     * @param objectInfo      Details about the object in this transport
     * @param parentTransport Parent transport info (only for tasks, null otherwise)
     */
    public record TransportInfo(
            String transportNumber,
            String transportType,
            String transportTypeDesc,
            String status,
            String statusDesc,
            String owner,
            String createdDate,
            String createdTime,
            boolean isLocked,
            ObjectInfo objectInfo,
            ParentTransportInfo parentTransport
    ) {}

    /**
     * Information about the parent transport (when object is in a task).
     *
     * @param transportNumber Parent transport number (STRKORR from E070)
     * @param transportType   Parent transport type
     * @param transportTypeDesc Human-readable parent transport type
     * @param status          Parent transport status
     * @param statusDesc      Human-readable parent status
     * @param owner           Parent transport owner
     * @param description     Parent transport description
     */
    public record ParentTransportInfo(
            String transportNumber,
            String transportType,
            String transportTypeDesc,
            String status,
            String statusDesc,
            String owner,
            String description
    ) {}

    /**
     * Information about the ABAP object in the transport.
     *
     * @param objName    Object name (OBJ_NAME from E071)
     * @param objectType Object type (OBJECT from E071): 'CLAS', 'PROG', 'FUGR', etc.
     * @param pgmid      Program ID (PGMID from E071): 'R3TR', 'LIMU', etc.
     */
    public record ObjectInfo(
            String objName,
            String objectType,
            String pgmid
    ) {}

    /**
     * Create a failed result with error information.
     *
     * @param objectName Object name that was searched
     * @param errorMessage Error message
     * @return Failed result
     */
    public static ObjectInOpenOTResult failure(String objectName, String errorMessage) {
        return new ObjectInOpenOTResult(
                false,
                objectName,
                errorMessage,
                List.of(),
                0
        );
    }

    /**
     * Create a successful result with no transports found.
     *
     * @param objectName    Object name that was searched
     * @param searchPattern Search pattern used
     * @return Successful result with empty transport list
     */
    public static ObjectInOpenOTResult notFound(String objectName, String searchPattern) {
        return new ObjectInOpenOTResult(
                true,
                objectName,
                searchPattern,
                List.of(),
                0
        );
    }
}

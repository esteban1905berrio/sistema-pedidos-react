package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Data Transfer Object for transport search results.
 *
 * Contains detailed transport information including object and task counts.
 * Used by search_transports tool for flexible criteria-based searches.
 *
 * @param success       whether the search was successful
 * @param message       status message or error description
 * @param totalFound    total number of transports matching criteria
 * @param transports    list of transport details
 */
public record TransportSearchResult(
        boolean success,
        String message,
        int totalFound,
        List<TransportDetail> transports
) {
    /**
     * Detailed transport information.
     *
     * @param transportNumber    transport request number (e.g., "CADK911197")
     * @param description        transport description text
     * @param transportType      type code (K=Workbench, W=Customizing, T=Copies)
     * @param transportTypeDesc  type description
     * @param status             status code (D=Modifiable, R=Released, L=Protected)
     * @param statusDesc         status description
     * @param owner              transport owner (SAP user)
     * @param createdDate        creation date (YYYY-MM-DD format)
     * @param createdTime        creation time (HH:MM:SS format)
     * @param targetSystem       target system name
     * @param category           transport category (SYST, etc.)
     * @param parentTransport    parent transport number (null if main transport)
     * @param objectCount        number of objects in transport
     * @param taskCount          number of tasks under this transport
     */
    public record TransportDetail(
            String transportNumber,
            String description,
            String transportType,
            String transportTypeDesc,
            String status,
            String statusDesc,
            String owner,
            String createdDate,
            String createdTime,
            String targetSystem,
            String category,
            String parentTransport,
            int objectCount,
            int taskCount
    ) {
    }
}

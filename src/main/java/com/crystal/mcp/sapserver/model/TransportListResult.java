package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Data Transfer Object for list of transport requests.
 *
 * This record represents transport requests (change requests / workbench requests)
 * from the SAP Change and Transport System (CTS).
 *
 * Progressive Discovery Integration:
 * Use this to list available transports before fetching detailed objects with
 * get_transport_objects.
 *
 * @param user           user ID (filter applied)
 * @param status         status filter applied (R=released, D=modifiable)
 * @param totalTransports total number of transports found
 * @param transports     list of transport references
 */
public record TransportListResult(
        String user,
        String status,
        int totalTransports,
        List<TransportReference> transports
) {
    /**
     * Individual transport reference.
     *
     * @param number      transport/task number (e.g., "DEVK900123")
     * @param description transport description
     * @param status      transport status (D=modifiable, R=released)
     * @param owner       transport owner
     * @param type        transport type (K=Workbench, W=Customizing, etc.)
     */
    public record TransportReference(
            String number,
            String description,
            String status,
            String owner,
            String type
    ) {
    }
}

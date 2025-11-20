package com.crystal.mcp.sapserver.model;

/**
 * Represents an inactive ABAP object with its associated transport.
 */
public record InactiveObject(
    String uri,
    String type,
    String name,
    String description,
    String packageName,
    String user,
    boolean deleted,
    TransportInfo transport
) {
    /**
     * Transport information for the inactive object.
     */
    public record TransportInfo(
        String uri,
        String name,
        String description,
        String user,
        boolean linked
    ) {}
}

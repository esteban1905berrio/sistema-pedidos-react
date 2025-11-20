package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result of an object activation operation.
 */
public record ActivationResult(
    boolean success,
    String message,
    List<ActivationError> errors
) {
    /**
     * Represents a syntax or activation error.
     */
    public record ActivationError(
        String objectUri,
        String objectDescription,
        String type,
        int line,
        String href,
        String shortText,
        boolean forceSupported
    ) {}
}

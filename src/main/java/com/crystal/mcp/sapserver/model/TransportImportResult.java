package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TransportImportResult(
        String targetSystem,
        String targetClient,
        int totalRequests,
        int successCount,
        int errorCount,
        List<TransportResult> results) {
    public record TransportResult(
            String transport,
            String status,
            String message,
            boolean success) {
    }

    public static TransportImportResult failure(String message) {
        return new TransportImportResult(
                "", "", 0, 0, 0,
                List.of(new TransportResult("", "error", message, false)));
    }
}

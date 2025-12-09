package com.crystal.mcp.sapserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of ABAP component validation (local vs SAP comparison).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean success;
    private String message;
    private String sourcePath;

    private int totalComponents;
    private int matchingComponents;
    private int mismatchedComponents;
    private int missingInSap;
    private int missingLocal;

    @Builder.Default
    private List<ComponentStatus> components = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentStatus {
        private String name;
        private String type;
        private String status;  // MATCH, MISMATCH, MISSING_SAP, MISSING_LOCAL
        private String localChecksum;
        private String sapChecksum;
    }
}

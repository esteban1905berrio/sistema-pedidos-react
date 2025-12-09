package com.crystal.mcp.sapserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of prerequisite check for ABAP component installation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrerequisiteCheckResult {

    private boolean success;
    private String message;
    private String sapSystem;
    private String sapRelease;

    @Builder.Default
    private List<CheckItem> checks = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItem {
        private String name;
        private String description;
        private boolean passed;
        private String details;
    }
}

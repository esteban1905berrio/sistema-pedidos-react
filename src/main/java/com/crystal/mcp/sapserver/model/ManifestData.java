package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure representing the ABAP component manifest.json file.
 *
 * <p>This class models the manifest that describes all ABAP components
 * required for the MCP Server installation in target SAP systems.
 *
 * <p>See: abap/manifest.json for the actual file format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestData {

    @JsonProperty("$schema")
    private String schema;

    private String version;
    private String name;
    private String description;

    @JsonProperty("source_system")
    private String sourceSystem;

    @JsonProperty("extracted_date")
    private String extractedDate;

    @JsonProperty("extracted_by")
    private String extractedBy;

    @Builder.Default
    private List<Package> packages = new ArrayList<>();

    @JsonProperty("function_groups")
    @Builder.Default
    private List<FunctionGroup> functionGroups = new ArrayList<>();

    @Builder.Default
    private List<ClassDef> classes = new ArrayList<>();

    @JsonProperty("installation_order")
    @Builder.Default
    private List<InstallationStep> installationOrder = new ArrayList<>();

    private Dependencies dependencies;
    private Statistics statistics;

    /**
     * Package definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Package {
        private String name;
        private String description;
        private String parent;
        @JsonProperty("transport_layer")
        private String transportLayer;
    }

    /**
     * Function group definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionGroup {
        private String name;
        private String description;
        @JsonProperty("package")
        private String packageName;
        private String path;
        private Includes includes;

        @JsonProperty("function_modules")
        @Builder.Default
        private List<FunctionModule> functionModules = new ArrayList<>();
    }

    /**
     * Function group includes (TOP, UXX).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Includes {
        private String top;
        private String uxx;
    }

    /**
     * Function module definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionModule {
        private String name;
        private String description;
        private String path;
        @JsonProperty("rfc_enabled")
        private boolean rfcEnabled;
        @JsonProperty("mcp_tool")
        private String mcpTool;
    }

    /**
     * Class definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassDef {
        private String name;
        private String description;
        @JsonProperty("package")
        private String packageName;
        private String path;
        private String superclass;
        @Builder.Default
        private List<String> interfaces = new ArrayList<>();
        @JsonProperty("used_by_fm")
        @Builder.Default
        private List<String> usedByFm = new ArrayList<>();
    }

    /**
     * Installation step in order.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstallationStep {
        private int step;
        private String type;
        private String name;
        @Builder.Default
        private List<String> names = new ArrayList<>();
        private String description;
    }

    /**
     * SAP dependencies.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dependencies {
        @JsonProperty("sap_standard")
        @Builder.Default
        private List<String> sapStandard = new ArrayList<>();
        @JsonProperty("function_modules")
        @Builder.Default
        private List<String> functionModules = new ArrayList<>();
        @Builder.Default
        private List<String> tables = new ArrayList<>();
    }

    /**
     * Component statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        @JsonProperty("total_components")
        private int totalComponents;
        @JsonProperty("function_groups")
        private int functionGroups;
        @JsonProperty("function_modules")
        private int functionModules;
        private int classes;
        private int interfaces;
        private int programs;
        private int includes;
    }
}

package com.crystal.mcp.sapserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of ABAP component installation operation.
 *
 * <p>Contains summary of what was installed, including:
 * - Function groups and modules created
 * - Classes created
 * - Activation status
 * - Any errors encountered
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallationResult {

    /**
     * Overall success status.
     */
    private boolean success;

    /**
     * Summary message.
     */
    private String message;

    /**
     * Source path from which components were read.
     */
    private String sourcePath;

    /**
     * Target SAP system.
     */
    private String targetSystem;

    /**
     * Target package where objects were installed.
     */
    private String targetPackage;

    /**
     * Transport request used.
     */
    private String transport;

    /**
     * Installation timestamp.
     */
    private LocalDateTime installedAt;

    /**
     * Whether this was a dry run (simulation only).
     */
    private boolean dryRun;

    /**
     * Total number of components processed.
     */
    private int totalComponents;

    /**
     * Number of function groups created.
     */
    private int functionGroupsCreated;

    /**
     * Number of function modules created.
     */
    private int functionModulesCreated;

    /**
     * Number of classes created.
     */
    private int classesCreated;

    /**
     * Number of components skipped (already exist).
     */
    private int componentsSkipped;

    /**
     * Number of components that failed.
     */
    private int componentsFailed;

    /**
     * Number of objects activated successfully.
     */
    private int objectsActivated;

    /**
     * List of installed components with details.
     */
    @Builder.Default
    private List<InstalledComponent> components = new ArrayList<>();

    /**
     * List of errors encountered.
     */
    @Builder.Default
    private List<InstallationError> errors = new ArrayList<>();

    /**
     * List of warnings.
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Details about a single installed component.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstalledComponent {
        private String name;
        private String type;  // FUNC, CLAS, FUGR
        private String status; // CREATED, MODIFIED, SKIPPED, FAILED
        private boolean activated;
        private String transport;
        private String message;
    }

    /**
     * Error encountered during installation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallationError {
        private String componentName;
        private String componentType;
        private String phase; // CREATE, MODIFY, ACTIVATE, SYNTAX_CHECK
        private String errorMessage;
        private String errorDetails;
    }

    /**
     * Add a successfully installed component.
     */
    public void addInstalledComponent(String name, String type, String status,
                                       boolean activated, String transport) {
        if (components == null) {
            components = new ArrayList<>();
        }
        components.add(InstalledComponent.builder()
                .name(name)
                .type(type)
                .status(status)
                .activated(activated)
                .transport(transport)
                .build());
    }

    /**
     * Add a component that failed installation.
     */
    public void addFailedComponent(String name, String type, String phase, String errorMessage) {
        if (components == null) {
            components = new ArrayList<>();
        }
        components.add(InstalledComponent.builder()
                .name(name)
                .type(type)
                .status("FAILED")
                .activated(false)
                .message(errorMessage)
                .build());

        addError(name, type, phase, errorMessage, null);
    }

    /**
     * Add an error.
     */
    public void addError(String componentName, String componentType, String phase,
                         String errorMessage, String errorDetails) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(InstallationError.builder()
                .componentName(componentName)
                .componentType(componentType)
                .phase(phase)
                .errorMessage(errorMessage)
                .errorDetails(errorDetails)
                .build());
    }

    /**
     * Add a warning.
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
}

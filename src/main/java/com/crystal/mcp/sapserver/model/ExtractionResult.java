package com.crystal.mcp.sapserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of ABAP component extraction operation.
 *
 * <p>Contains summary of what was extracted, including:
 * - Function modules extracted
 * - Classes extracted
 * - Files written to disk
 * - Any errors encountered
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    /**
     * Overall success status.
     */
    private boolean success;

    /**
     * Summary message.
     */
    private String message;

    /**
     * Target path where files were written.
     */
    private String targetPath;

    /**
     * Source SAP system.
     */
    private String sourceSystem;

    /**
     * Extraction timestamp.
     */
    private LocalDateTime extractedAt;

    /**
     * Total number of components extracted.
     */
    private int totalComponents;

    /**
     * Number of function modules extracted.
     */
    private int functionModulesExtracted;

    /**
     * Number of classes extracted.
     */
    private int classesExtracted;

    /**
     * Number of files written.
     */
    private int filesWritten;

    /**
     * List of extracted components with details.
     */
    @Builder.Default
    private List<ExtractedComponent> components = new ArrayList<>();

    /**
     * List of errors encountered.
     */
    @Builder.Default
    private List<ExtractionError> errors = new ArrayList<>();

    /**
     * Whether manifest.json was updated.
     */
    private boolean manifestUpdated;

    /**
     * Details about a single extracted component.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedComponent {
        private String name;
        private String type;  // FUNC, CLAS, FUGR
        private String filePath;
        private long sizeBytes;
        private boolean success;
        private String errorMessage;
    }

    /**
     * Error encountered during extraction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionError {
        private String componentName;
        private String componentType;
        private String errorMessage;
        private String errorDetails;
    }

    /**
     * Add a successfully extracted component.
     */
    public void addExtractedComponent(String name, String type, String filePath, long sizeBytes) {
        if (components == null) {
            components = new ArrayList<>();
        }
        components.add(ExtractedComponent.builder()
                .name(name)
                .type(type)
                .filePath(filePath)
                .sizeBytes(sizeBytes)
                .success(true)
                .build());
    }

    /**
     * Add a component that failed extraction.
     */
    public void addFailedComponent(String name, String type, String errorMessage) {
        if (components == null) {
            components = new ArrayList<>();
        }
        components.add(ExtractedComponent.builder()
                .name(name)
                .type(type)
                .success(false)
                .errorMessage(errorMessage)
                .build());

        addError(name, type, errorMessage, null);
    }

    /**
     * Add an error.
     */
    public void addError(String componentName, String componentType, String errorMessage, String errorDetails) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(ExtractionError.builder()
                .componentName(componentName)
                .componentType(componentType)
                .errorMessage(errorMessage)
                .errorDetails(errorDetails)
                .build());
    }
}

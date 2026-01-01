package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Service for extracting ABAP components from SAP system to local filesystem.
 *
 * <p>This service implements Phase 2 of the ABAP Component Distribution system.
 * It extracts source code from SAP systems and saves them in a standardized
 * directory structure with metadata for version control and deployment.
 *
 * <p>Directory structure:
 * <pre>
 * abap/
 * ├── manifest.json                           # Master catalog
 * ├── functions/groups/{name}/
 * │   ├── includes/
 * │   │   ├── l{name}top.abap                # TOP include
 * │   │   └── l{name}uxx.abap                # UXX include
 * │   └── fmodules/{fm_name}/
 * │       └── {fm_name}.asfunc               # Function module source
 * └── classlib/classes/{name}/
 *     └── {name}.aclass                       # Class source
 * </pre>
 *
 * <p>Thread Safety: Stateless service, thread-safe via underlying services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentExtractionService {

    private final ClassService classService;
    private final ProgramService programService;
    private final ObjectMapper objectMapper;

    private static final String MANIFEST_FILENAME = "manifest.json";

    /**
     * Extract ABAP components from SAP system to local filesystem.
     *
     * @param targetPath       base directory for extraction (default: ./abap)
     * @param componentNames   specific components to extract (null = all from manifest)
     * @param includeMetadata  whether to generate metadata JSONs
     * @param updateManifest   whether to update manifest.json with checksums
     * @return ExtractionResult with details of extraction operation
     */
    public ExtractionResult extractComponents(
            String targetPath,
            List<String> componentNames,
            boolean includeMetadata,
            boolean updateManifest) {

        ExtractionResult.ExtractionResultBuilder resultBuilder = ExtractionResult.builder()
                .success(false)
                .extractedAt(LocalDateTime.now())
                .functionModulesExtracted(0)
                .classesExtracted(0)
                .filesWritten(0)
                .components(new ArrayList<>())
                .errors(new ArrayList<>());

        try {
            // Resolve target path
            Path basePath = resolveTargetPath(targetPath);
            resultBuilder.targetPath(basePath.toString());

            log.info("Starting ABAP component extraction to: {}", basePath);

            // Read existing manifest
            ManifestData manifest = readManifest(basePath);
            if (manifest == null) {
                resultBuilder.message("No manifest.json found at " + basePath);
                return resultBuilder.build();
            }

            resultBuilder.sourceSystem(manifest.getSourceSystem());

            int fmCount = 0;
            int classCount = 0;
            int filesWritten = 0;
            List<ExtractionResult.ExtractedComponent> components = new ArrayList<>();
            List<ExtractionResult.ExtractionError> errors = new ArrayList<>();

            // Extract function modules from function groups
            if (manifest.getFunctionGroups() != null) {
                for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
                    log.info("Processing function group: {}", fg.getName());

                    // Extract function modules
                    if (fg.getFunctionModules() != null) {
                        for (ManifestData.FunctionModule fm : fg.getFunctionModules()) {
                            // Skip if specific components requested and this FM not in list
                            if (componentNames != null && !componentNames.isEmpty()
                                    && !componentNames.contains(fm.getName())) {
                                continue;
                            }

                            try {
                                int written = extractFunctionModule(basePath, fg, fm);
                                filesWritten += written;
                                fmCount++;

                                components.add(ExtractionResult.ExtractedComponent.builder()
                                        .name(fm.getName())
                                        .type("FUNC")
                                        .filePath(fm.getPath())
                                        .success(true)
                                        .build());

                                log.info("Extracted FM: {} ({} files)", fm.getName(), written);

                            } catch (Exception e) {
                                log.error("Failed to extract FM {}: {}", fm.getName(), e.getMessage());
                                errors.add(ExtractionResult.ExtractionError.builder()
                                        .componentName(fm.getName())
                                        .componentType("FUNC")
                                        .errorMessage(e.getMessage())
                                        .build());
                            }
                        }
                    }

                    // Extract function group includes (TOP, UXX)
                    try {
                        int written = extractFunctionGroupIncludes(basePath, fg);
                        filesWritten += written;
                    } catch (Exception e) {
                        log.warn("Failed to extract includes for FG {}: {}", fg.getName(), e.getMessage());
                    }
                }
            }

            // Extract classes
            if (manifest.getClasses() != null) {
                for (ManifestData.ClassDef classDef : manifest.getClasses()) {
                    // Skip if specific components requested and this class not in list
                    if (componentNames != null && !componentNames.isEmpty()
                            && !componentNames.contains(classDef.getName())) {
                        continue;
                    }

                    try {
                        int written = extractClass(basePath, classDef);
                        filesWritten += written;
                        classCount++;

                        components.add(ExtractionResult.ExtractedComponent.builder()
                                .name(classDef.getName())
                                .type("CLAS")
                                .filePath(classDef.getPath())
                                .success(true)
                                .build());

                        log.info("Extracted class: {} ({} files)", classDef.getName(), written);

                    } catch (Exception e) {
                        log.error("Failed to extract class {}: {}", classDef.getName(), e.getMessage());
                        errors.add(ExtractionResult.ExtractionError.builder()
                                .componentName(classDef.getName())
                                .componentType("CLAS")
                                .errorMessage(e.getMessage())
                                .build());
                    }
                }
            }

            // Update manifest with extraction date
            if (updateManifest) {
                manifest.setExtractedDate(LocalDate.now().toString());
                manifest.setExtractedBy("MCP Server");
                writeManifest(basePath, manifest);
                resultBuilder.manifestUpdated(true);
            }

            resultBuilder
                    .success(errors.isEmpty())
                    .functionModulesExtracted(fmCount)
                    .classesExtracted(classCount)
                    .filesWritten(filesWritten)
                    .totalComponents(fmCount + classCount)
                    .components(components)
                    .errors(errors)
                    .message(String.format("Extracted %d FMs, %d classes (%d files total)",
                            fmCount, classCount, filesWritten));

            return resultBuilder.build();

        } catch (Exception e) {
            log.error("Extraction failed: {}", e.getMessage(), e);
            resultBuilder
                    .success(false)
                    .message("Extraction failed: " + e.getMessage());
            return resultBuilder.build();
        }
    }

    /**
     * Extract a single function module source code.
     */
    private int extractFunctionModule(Path basePath, ManifestData.FunctionGroup fg,
                                       ManifestData.FunctionModule fm) throws IOException {
        // Get FM source from SAP
        String fmSource = getFunctionModuleSource(fg.getName(), fm.getName());

        // Build file path
        Path fmDir = basePath.resolve("functions/groups")
                .resolve(fg.getName().toLowerCase())
                .resolve("fmodules")
                .resolve(fm.getName().toLowerCase());
        Files.createDirectories(fmDir);

        // Write source file
        Path sourceFile = fmDir.resolve(fm.getName().toLowerCase() + ".asfunc");
        Files.writeString(sourceFile, fmSource);

        return 1;
    }

    /**
     * Extract function group includes (TOP and UXX).
     */
    private int extractFunctionGroupIncludes(Path basePath, ManifestData.FunctionGroup fg)
            throws IOException {
        int filesWritten = 0;
        String fgNameLower = fg.getName().toLowerCase();

        Path includesDir = basePath.resolve("functions/groups")
                .resolve(fgNameLower)
                .resolve("includes");
        Files.createDirectories(includesDir);

        // Extract TOP include
        if (fg.getIncludes() != null && fg.getIncludes().getTop() != null) {
            try {
                String topIncludeName = "L" + fg.getName() + "TOP";
                String mainProgram = "SAPL" + fg.getName();

                IncludeSourceResult topSource = programService.getIncludeSource(
                        mainProgram, topIncludeName, "active");

                if (topSource != null && topSource.source() != null) {
                    Path topFile = includesDir.resolve(fg.getIncludes().getTop());
                    Files.writeString(topFile, topSource.source());
                    filesWritten++;
                }
            } catch (Exception e) {
                log.warn("Could not extract TOP include for {}: {}", fg.getName(), e.getMessage());
            }
        }

        // Extract UXX include
        if (fg.getIncludes() != null && fg.getIncludes().getUxx() != null) {
            try {
                String uxxIncludeName = "L" + fg.getName() + "UXX";
                String mainProgram = "SAPL" + fg.getName();

                IncludeSourceResult uxxSource = programService.getIncludeSource(
                        mainProgram, uxxIncludeName, "active");

                if (uxxSource != null && uxxSource.source() != null) {
                    Path uxxFile = includesDir.resolve(fg.getIncludes().getUxx());
                    Files.writeString(uxxFile, uxxSource.source());
                    filesWritten++;
                }
            } catch (Exception e) {
                log.warn("Could not extract UXX include for {}: {}", fg.getName(), e.getMessage());
            }
        }

        return filesWritten;
    }

    /**
     * Extract a single class source code.
     */
    private int extractClass(Path basePath, ManifestData.ClassDef classDef) throws IOException {
        int filesWritten = 0;
        String classNameLower = classDef.getName().toLowerCase();

        Path classDir = basePath.resolve("classlib/classes").resolve(classNameLower);
        Files.createDirectories(classDir);

        // Get main (definition) source
        ClassSourceResult mainSource = classService.getClassSource(
                classDef.getName(), "active", "main");
        if (mainSource != null && mainSource.source() != null) {
            Path mainFile = classDir.resolve(classNameLower + ".clas.abap");
            Files.writeString(mainFile, mainSource.source());
            filesWritten++;
        }

        // Get implementation source
        try {
            ClassSourceResult implSource = classService.getClassSource(
                    classDef.getName(), "active", "implementations");
            if (implSource != null && implSource.source() != null) {
                Path implFile = classDir.resolve(classNameLower + ".clas.impl.abap");
                Files.writeString(implFile, implSource.source());
                filesWritten++;
            }
        } catch (Exception e) {
            log.debug("No implementation found for class {}", classDef.getName());
        }

        // Write combined .aclass file (definition + implementation)
        StringBuilder combined = new StringBuilder();
        if (mainSource != null && mainSource.source() != null) {
            combined.append(mainSource.source());
        }

        Path aclassFile = classDir.resolve(classNameLower + ".aclass");
        Files.writeString(aclassFile, combined.toString());
        filesWritten++;

        return filesWritten;
    }

    /**
     * Get function module source code from SAP.
     */
    private String getFunctionModuleSource(String functionGroup, String functionModule)
            throws IOException {
        // Build the include name for the function module
        // FM source is stored in include L<FUGR>U<NN> where NN is a number
        // But we can also get it via the ADT function module endpoint
        String mainProgram = "SAPL" + functionGroup;
        String includeName = functionModule;

        try {
            // Try getting via include source (function modules are includes)
            IncludeSourceResult result = programService.getIncludeSource(
                    mainProgram, includeName, "active");
            if (result != null && result.source() != null) {
                return result.source();
            }
        } catch (Exception e) {
            log.debug("Could not get FM {} via include, trying object source", functionModule);
        }

        // Fallback: construct the source from what we have
        throw new IOException("Could not retrieve source for FM: " + functionModule);
    }

    /**
     * Resolve target path, using default if not specified.
     */
    private Path resolveTargetPath(String targetPath) {
        if (targetPath == null || targetPath.trim().isEmpty()) {
            return Paths.get("./abap").toAbsolutePath().normalize();
        }
        return Paths.get(targetPath).toAbsolutePath().normalize();
    }

    /**
     * Read manifest.json from base path.
     */
    private ManifestData readManifest(Path basePath) throws IOException {
        Path manifestPath = basePath.resolve(MANIFEST_FILENAME);
        if (!Files.exists(manifestPath)) {
            log.warn("Manifest not found at: {}", manifestPath);
            return null;
        }

        return objectMapper.readValue(manifestPath.toFile(), ManifestData.class);
    }

    /**
     * Write manifest.json to base path.
     */
    private void writeManifest(Path basePath, ManifestData manifest) throws IOException {
        Path manifestPath = basePath.resolve(MANIFEST_FILENAME);

        ObjectMapper prettyMapper = objectMapper.copy();
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);

        prettyMapper.writeValue(manifestPath.toFile(), manifest);
        log.info("Updated manifest at: {}", manifestPath);
    }

    /**
     * Calculate SHA-256 checksum for a string.
     */
    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

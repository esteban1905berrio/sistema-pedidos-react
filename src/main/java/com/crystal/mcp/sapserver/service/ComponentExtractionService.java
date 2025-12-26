package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.*;
import com.crystal.mcp.sapserver.model.ExtractionDiscovery.DiscoveredObject;
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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Service for extracting ABAP components from SAP system to local filesystem.
 *
 * <p>
 * This service implements Phase 2 of the ABAP Component Distribution system.
 * It extracts source code from SAP systems and saves them in a standardized
 * directory structure with metadata for version control and deployment.
 *
 * <p>
 * Directory structure:
 * 
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
 * <p>
 * Thread Safety: Stateless service, thread-safe via underlying services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentExtractionService {

    private final ClassService classService;
    private final ProgramService programService;
    private final ObjectService objectService;
    private final CdsService cdsService;
    private final EnhancementService enhancementService;
    private final ObjectMapper objectMapper;
    private final FunctionModuleScanner functionModuleScanner;

    private static final String MANIFEST_FILENAME = "manifest.json";
    private static final String DEFAULT_FUNCTION_GROUP = "ZGFCX_1";

    /**
     * Extract ABAP components from SAP system to local filesystem.
     *
     * <p>
     * <b>Auto-Sync Feature:</b> Before extraction, this method automatically
     * synchronizes the manifest with FMs actually used in the Java code.
     * This ensures no custom FMs are missed during extraction.
     *
     * @param targetPath      base directory for extraction (default: ./abap)
     * @param componentNames  specific components to extract (null = all from
     *                        manifest)
     * @param includeMetadata whether to generate metadata JSONs
     * @param updateManifest  whether to update manifest.json with checksums
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

            // STEP 1: Auto-sync manifest with FMs used in Java code
            log.info("Step 1: Synchronizing manifest with FMs used in code...");
            ManifestSyncResult syncResult = syncManifestWithUsedFMs(targetPath);
            if (syncResult.isManifestUpdated()) {
                log.info("Manifest updated: added {} FMs: {}",
                        syncResult.getAddedFMs().size(), syncResult.getAddedFMs());
            } else {
                log.info("Manifest already synchronized");
            }

            // STEP 2: Read (potentially updated) manifest
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

                    // Use package from manifest if available, otherwise default
                    String packageName = fg.getPackageName() != null ? fg.getPackageName() : "unknown";

                    // Extract function modules
                    if (fg.getFunctionModules() != null) {
                        for (ManifestData.FunctionModule fm : fg.getFunctionModules()) {
                            // Skip if specific components requested and this FM not in list
                            if (componentNames != null && !componentNames.isEmpty()
                                    && !componentNames.contains(fm.getName())) {
                                continue;
                            }

                            try {
                                int written = extractFunctionModule(basePath, packageName, fg, fm);
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
                        int written = extractFunctionGroupIncludes(basePath, packageName, fg);
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

                    // Use package from manifest if available
                    String packageName = classDef.getPackageName() != null ? classDef.getPackageName() : "unknown";

                    try {
                        int written = extractClass(basePath, packageName, classDef);
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
    private int extractFunctionModule(Path basePath, String packageName, ManifestData.FunctionGroup fg,
            ManifestData.FunctionModule fm) throws IOException {
        // Get FM source from SAP
        String fmSource = getFunctionModuleSource(fg.getName(), fm.getName());

        // Build file path: {basePath}/{package}/functions/groups/{fg}/fmodules/{fm}
        Path fmDir = basePath.resolve(packageName.toLowerCase())
                .resolve("functions/groups")
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
     * Extract a single function module source code (overload for direct use).
     */
    /**
     * Extract a single function module source code (overload for direct use).
     */
    private int extractFunctionModule(Path basePath, String packageName, String fgName, String fmName)
            throws IOException {
        // Get FM source from SAP
        String fmSource = getFunctionModuleSource(fgName, fmName);

        String pkgDirName = packageName != null && !packageName.isEmpty() ? packageName.toLowerCase() : "local";

        // Build file path: {basePath}/{package}/functions/groups/{fg}/fmodules/{fm}
        Path fmDir = basePath.resolve(pkgDirName)
                .resolve("functions/groups")
                .resolve(fgName.toLowerCase())
                .resolve("fmodules")
                .resolve(fmName.toLowerCase());
        Files.createDirectories(fmDir);

        // Write source file
        Path sourceFile = fmDir.resolve(fmName.toLowerCase() + ".asfunc");
        Files.writeString(sourceFile, fmSource);

        return 1;
    }

    /**
     * Extract function group includes (TOP and UXX).
     */
    private int extractFunctionGroupIncludes(Path basePath, String packageName, ManifestData.FunctionGroup fg)
            throws IOException {
        int filesWritten = 0;
        String fgNameLower = fg.getName().toLowerCase();

        Path includesDir = basePath.resolve(packageName.toLowerCase())
                .resolve("functions/groups")
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
     * Generates only ONE file (.aclass) with complete class code (definition +
     * implementation).
     */
    private int extractClass(Path basePath, String packageName, ManifestData.ClassDef classDef) throws IOException {
        String classNameLower = classDef.getName().toLowerCase();

        // Build file path: {basePath}/{package}/classlib/classes/{class}
        Path classDir = basePath.resolve(packageName.toLowerCase())
                .resolve("classlib/classes")
                .resolve(classNameLower);
        Files.createDirectories(classDir);

        // Get main (definition) source
        ClassSourceResult mainSource = classService.getClassSource(
                classDef.getName(), "active", "main");

        // Get implementation source
        ClassSourceResult implSource = null;
        try {
            implSource = classService.getClassSource(
                    classDef.getName(), "active", "implementations");
        } catch (Exception e) {
            log.debug("No implementation found for class {}", classDef.getName());
        }

        // Combine definition + implementation in single .aclass file
        StringBuilder combined = new StringBuilder();
        if (mainSource != null && mainSource.source() != null) {
            combined.append(mainSource.source());
        }
        if (implSource != null && implSource.source() != null) {
            // Add separator if both exist
            if (combined.length() > 0) {
                combined.append("\n");
            }
            combined.append(implSource.source());
        }

        if (combined.length() == 0) {
            throw new IOException("No source code found for class: " + classDef.getName());
        }

        // Write single .aclass file
        Path aclassFile = classDir.resolve(classNameLower + ".aclass");
        Files.writeString(aclassFile, combined.toString());

        return 1; // Always 1 file per class
    }

    /**
     * Extract complete function group components individually.
     * Generates separate files for includes and function modules.
     */
    private int extractFunctionGroupComponents(Path basePath, String packageName, String fgName) throws IOException {
        String fgNameLower = fgName.toLowerCase();
        int filesWritten = 0;

        // 1. Extract Includes (TOP and UXX)
        Path includesDir = basePath.resolve(packageName.toLowerCase())
                .resolve("functions/groups")
                .resolve(fgNameLower)
                .resolve("includes");
        Files.createDirectories(includesDir);

        // TOP Include
        try {
            String topIncludeName = "L" + fgName + "TOP";
            IncludeSourceResult topSource = programService.getIncludeSource(
                    "SAPL" + fgName, topIncludeName, "active");

            if (topSource != null && topSource.source() != null) {
                Path topFile = includesDir.resolve(topIncludeName.toLowerCase() + ".abap");
                Files.writeString(topFile, topSource.source());
                filesWritten++;
            }
        } catch (Exception e) {
            log.debug("No TOP include found for FG {}", fgName);
        }

        // UXX Include
        try {
            String uxxIncludeName = "L" + fgName + "UXX";
            IncludeSourceResult uxxSource = programService.getIncludeSource(
                    "SAPL" + fgName, uxxIncludeName, "active");

            if (uxxSource != null && uxxSource.source() != null) {
                Path uxxFile = includesDir.resolve(uxxIncludeName.toLowerCase() + ".abap");
                Files.writeString(uxxFile, uxxSource.source());
                filesWritten++;
            }
        } catch (Exception e) {
            log.debug("No UXX include found for FG {}", fgName);
        }

        // 2. Extract Function Modules
        // Use ADT nodestructure endpoint via ObjectService to get FMs in the group
        // because standard ADT structure (ObjectStructure) often returns empty for FGs.
        try {
            List<String> functionModules = objectService.getFunctionGroupModules(fgName);
            log.info("Found {} FMs for FG {} via ADT nodestructure", functionModules.size(), fgName);

            for (String fmName : functionModules) {
                try {
                    // Extract each FM found in the group
                    int written = extractFunctionModule(basePath, packageName, fgName, fmName);
                    filesWritten += written;
                } catch (Exception e) {
                    log.warn("Failed to extract FM {} from FG {}: {}", fmName, fgName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve FMs for FG {}: {}", fgName, e.getMessage());
        }

        if (filesWritten == 0) {
            throw new IOException("No source code found for function group: " + fgName);
        }

        return filesWritten;
    }

    /**
     * Extract ABAP program source in ONE file.
     */
    private int extractProgram(Path basePath, String packageName, String progName, String uri) throws IOException {
        String progNameLower = progName.toLowerCase();

        // Build file path: {basePath}/{package}/programs/{program}
        Path progDir = basePath.resolve(packageName.toLowerCase())
                .resolve("programs")
                .resolve(progNameLower);
        Files.createDirectories(progDir);

        // Get program source
        ProgramSourceResult progSource = programService.getProgramSource(progName, "active", uri);

        if (progSource == null || progSource.source() == null) {
            String errorDetails = "";
            if (progSource != null && progSource.metadata() != null && progSource.metadata().containsKey("error")) {
                errorDetails = " - " + progSource.metadata().get("error");
            }
            throw new IOException("Failed to retrieve program source" + errorDetails);
        }

        // Write single .prog.abap file
        Path progFile = progDir.resolve(progNameLower + ".prog.abap");
        Files.writeString(progFile, progSource.source());

        return 1; // Always 1 file per program
    }

    /**
     * Extract CDS View source in ONE file.
     */
    private int extractCds(Path basePath, String packageName, String cdsName) throws IOException {
        String name = cdsName.toUpperCase();
        // Build file path: {basePath}/{package}/ddls/{cds}
        Path dir = basePath.resolve(packageName.toLowerCase())
                .resolve("ddls")
                .resolve(name.toLowerCase());
        Files.createDirectories(dir);

        CdsSourceResult result = cdsService.getCdsSource(name, "active");
        if (result == null || result.source() == null) {
            throw new IOException("No source found for CDS View: " + name);
        }

        Path file = dir.resolve(name.toLowerCase() + ".asddls");
        Files.writeString(file, result.source());
        return 1;
    }

    /**
     * Extract Enhancement Source in JSON format (due to complexity).
     */
    private int extractEnhancement(Path basePath, String packageName, String enhName) throws IOException {
        String name = enhName.toUpperCase();
        // Build file path: {basePath}/{package}/enhancements/{enh}
        Path dir = basePath.resolve(packageName.toLowerCase())
                .resolve("enhancements")
                .resolve(name.toLowerCase());
        Files.createDirectories(dir);

        EnhancementSourceResult result = enhancementService.getEnhancementSource(name, null);
        if (result == null) {
            throw new IOException("No source found for Enhancement: " + name);
        }

        // Save full result as JSON to preserve structure (headers, elements, source
        // lines)
        String json = objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(result);

        Path file = dir.resolve(name.toLowerCase() + ".enho.json");
        Files.writeString(file, json);
        return 1;
    }

    /**
     * 
     * Get function module source code from SAP using ADT REST API.
     *
     * <p>
     * Uses the correct ADT URI format for function modules:
     * /sap/bc/adt/functions/groups/{function_group}/fmodules/{fm_name}/source/main
     *
     * @param functionGroup  Function group name (e.g., "ZGFCX_1")
     * @param functionModule Function module name (e.g., "ZCX_GETDDICSOURCE")
     * @return Function module source code
     * @throws IOException if source cannot be retrieved
     */
    private String getFunctionModuleSource(String functionGroup, String functionModule)
            throws IOException {
        // Build ADT URI for function module source
        // Format: /sap/bc/adt/functions/groups/{fg}/fmodules/{fm}/source/main
        String fmUri = String.format("/sap/bc/adt/functions/groups/%s/fmodules/%s",
                functionGroup.toLowerCase(), functionModule.toLowerCase());

        log.debug("Retrieving FM source via URI: {}", fmUri);

        try {
            ObjectSourceResult result = objectService.getObjectSource(fmUri, "active");
            if (result != null && result.source() != null && !result.source().isEmpty()) {
                log.debug("Successfully retrieved FM {} ({} bytes)",
                        functionModule, result.source().length());
                return result.source();
            }
        } catch (Exception e) {
            log.error("Failed to retrieve FM {} via ADT: {}", functionModule, e.getMessage());
        }

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

    /**
     * Synchronize manifest with function modules actually used by the MCP server.
     *
     * <p>
     * This method scans the Java source code to detect custom FM references
     * (Z_CX_*, ZCX_*) and updates the manifest to include any missing FMs.
     *
     * <p>
     * The sync process:
     * <ol>
     * <li>Scan Java source for FM references using
     * {@link FunctionModuleScanner}</li>
     * <li>Compare detected FMs with manifest</li>
     * <li>Add missing FMs to manifest (in default function group)</li>
     * <li>Return sync result with changes made</li>
     * </ol>
     *
     * @param targetPath path to ABAP components directory containing manifest.json
     * @return ManifestSyncResult with details of synchronization
     */
    public ManifestSyncResult syncManifestWithUsedFMs(String targetPath) {
        ManifestSyncResult.ManifestSyncResultBuilder resultBuilder = ManifestSyncResult.builder()
                .success(false)
                .syncedAt(LocalDateTime.now())
                .addedFMs(new ArrayList<>())
                .errors(new ArrayList<>());

        try {
            // Resolve paths
            Path basePath = resolveTargetPath(targetPath);
            resultBuilder.manifestPath(basePath.resolve(MANIFEST_FILENAME).toString());

            // Read existing manifest
            ManifestData manifest = readManifest(basePath);
            if (manifest == null) {
                resultBuilder.message("No manifest.json found at " + basePath);
                return resultBuilder.build();
            }

            // Scan Java code for FM references
            Set<String> detectedFMs = functionModuleScanner.scanForUsedFunctionModules();
            resultBuilder.detectedFMCount(detectedFMs.size());

            // Get FMs currently in manifest
            Set<String> manifestFMs = getManifestFMNames(manifest);
            resultBuilder.manifestFMCount(manifestFMs.size());

            // Find differences
            FunctionModuleScanner.ScanDifference diff = functionModuleScanner.compareWithManifest(detectedFMs,
                    manifestFMs);

            if (diff.isSynchronized()) {
                resultBuilder
                        .success(true)
                        .manifestUpdated(false)
                        .message("Manifest is already synchronized with code. No changes needed.");
                return resultBuilder.build();
            }

            // Add missing FMs to manifest
            List<String> addedFMs = new ArrayList<>();
            if (!diff.missingInManifest().isEmpty()) {
                ManifestData.FunctionGroup targetFG = findOrCreateFunctionGroup(manifest, DEFAULT_FUNCTION_GROUP);

                for (String fmName : diff.missingInManifest()) {
                    ManifestData.FunctionModule newFM = createFunctionModuleEntry(fmName);
                    targetFG.getFunctionModules().add(newFM);
                    addedFMs.add(fmName);
                    log.info("Added missing FM to manifest: {}", fmName);
                }

                // Update statistics
                updateManifestStatistics(manifest);

                // Write updated manifest
                writeManifest(basePath, manifest);
                resultBuilder.manifestUpdated(true);
            }

            resultBuilder
                    .success(true)
                    .addedFMs(addedFMs)
                    .extraFMs(new ArrayList<>(diff.extraInManifest()))
                    .message(String.format(
                            "Sync complete. Added %d FMs. %d FMs in manifest not found in code (review recommended).",
                            addedFMs.size(), diff.extraInManifest().size()));

            return resultBuilder.build();

        } catch (Exception e) {
            log.error("Manifest sync failed: {}", e.getMessage(), e);
            resultBuilder
                    .success(false)
                    .message("Sync failed: " + e.getMessage());
            return resultBuilder.build();
        }
    }

    /**
     * Get set of FM names from manifest.
     */
    private Set<String> getManifestFMNames(ManifestData manifest) {
        Set<String> fmNames = new HashSet<>();
        if (manifest.getFunctionGroups() != null) {
            for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
                if (fg.getFunctionModules() != null) {
                    for (ManifestData.FunctionModule fm : fg.getFunctionModules()) {
                        fmNames.add(fm.getName().toUpperCase());
                    }
                }
            }
        }
        return fmNames;
    }

    /**
     * Find existing function group or create new one.
     */
    private ManifestData.FunctionGroup findOrCreateFunctionGroup(ManifestData manifest, String fgName) {
        if (manifest.getFunctionGroups() == null) {
            manifest.setFunctionGroups(new ArrayList<>());
        }

        for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
            if (fg.getName().equalsIgnoreCase(fgName)) {
                if (fg.getFunctionModules() == null) {
                    fg.setFunctionModules(new ArrayList<>());
                }
                return fg;
            }
        }

        // Create new function group
        ManifestData.FunctionGroup newFG = new ManifestData.FunctionGroup();
        newFG.setName(fgName);
        newFG.setDescription("Crystal MCP Server - Core Function Modules");
        newFG.setPath("functions/groups/" + fgName.toLowerCase());
        newFG.setPackageName("ZCX");
        newFG.setFunctionModules(new ArrayList<>());
        manifest.getFunctionGroups().add(newFG);

        return newFG;
    }

    /**
     * Create a new function module entry for manifest.
     */
    private ManifestData.FunctionModule createFunctionModuleEntry(String fmName) {
        ManifestData.FunctionModule fm = new ManifestData.FunctionModule();
        fm.setName(fmName);
        fm.setDescription("Auto-detected FM (needs description)");
        fm.setPath("fmodules/" + fmName.toLowerCase() + "/" + fmName.toLowerCase() + ".asfunc");
        fm.setRfcEnabled(false);
        fm.setMcpTool("auto-detected");
        return fm;
    }

    /**
     * Update manifest statistics after adding FMs.
     */
    private void updateManifestStatistics(ManifestData manifest) {
        int fmCount = 0;
        int fgCount = 0;

        if (manifest.getFunctionGroups() != null) {
            fgCount = manifest.getFunctionGroups().size();
            for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
                if (fg.getFunctionModules() != null) {
                    fmCount += fg.getFunctionModules().size();
                }
            }
        }

        if (manifest.getStatistics() == null) {
            manifest.setStatistics(new ManifestData.Statistics());
        }
        manifest.getStatistics().setFunctionModules(fmCount);
        manifest.getStatistics().setFunctionGroups(fgCount);
        manifest.getStatistics().setTotalComponents(
                fmCount +
                        (manifest.getClasses() != null ? manifest.getClasses().size() : 0) +
                        manifest.getStatistics().getIncludes());
    }

    /**
     * Result of manifest synchronization.
     */
    @lombok.Builder
    @lombok.Data
    public static class ManifestSyncResult {
        private boolean success;
        private String message;
        private String manifestPath;
        private LocalDateTime syncedAt;
        private boolean manifestUpdated;
        private int detectedFMCount;
        private int manifestFMCount;
        private List<String> addedFMs;
        private List<String> extraFMs;
        private List<String> errors;
    }

    /**
     * Extract a list of discovered objects to the target path.
     *
     * <p>
     * This method allows extracting specific objects found via
     * AbapExtractionService.
     *
     * @param objects    List of discovered objects to extract
     * @param targetPath Target directory
     * @return ExtractionResult with details
     */
    public ExtractionResult extractDiscoveredObjects(
            List<DiscoveredObject> objects,
            String targetPath) {

        ExtractionResult.ExtractionResultBuilder resultBuilder = ExtractionResult.builder()
                .success(false)
                .extractedAt(LocalDateTime.now())
                .functionModulesExtracted(0)
                .classesExtracted(0)
                .filesWritten(0)
                .components(new ArrayList<>())
                .errors(new ArrayList<>());

        try {
            Path basePath = resolveTargetPath(targetPath);
            resultBuilder.targetPath(basePath.toString());
            log.info("Starting extraction of {} discovered objects to: {}", objects.size(), basePath);

            int filesWritten = 0;
            int fmCount = 0;
            int classCount = 0;
            List<ExtractionResult.ExtractedComponent> components = new ArrayList<>();
            List<ExtractionResult.ExtractionError> errors = new ArrayList<>();

            for (DiscoveredObject obj : objects) {
                try {
                    int written = 0;
                    // Use package from discovery, or default to "unknown"
                    String packageName = obj.devclass() != null ? obj.devclass() : "unknown";

                    if ("CLAS".equals(obj.objectType())) {
                        ManifestData.ClassDef classDef = new ManifestData.ClassDef();
                        classDef.setName(obj.objectName());
                        classDef.setPath("classlib/classes/" + obj.objectName().toLowerCase());
                        written = extractClass(basePath, packageName, classDef);
                        if (written > 0)
                            classCount++;

                    } else if ("FUGR".equals(obj.objectType())) {
                        // Extract function group components (separate files)
                        written = extractFunctionGroupComponents(basePath, packageName, obj.objectName());

                    } else if ("FUNC".equals(obj.objectType())) {
                        // Extract individual FM
                        String fgName = extractFgFromUri(obj.uri());
                        if (fgName != null) {
                            written = extractFunctionModule(basePath, packageName, fgName, obj.objectName());
                            if (written > 0)
                                fmCount++;
                        } else {
                            log.warn("Could not determine Function Group for FM: {}", obj.objectName());
                        }

                    } else if ("PROG".equals(obj.objectType())) {
                        // Extract program source
                        written = extractProgram(basePath, packageName, obj.objectName(), obj.uri());

                    } else if ("VIEW".equals(obj.objectType())) {
                        // Extract CDS View
                        written = extractCds(basePath, packageName, obj.objectName());

                    } else if ("ENHO".equals(obj.objectType())) {
                        // Extract Enhancement
                        written = extractEnhancement(basePath, packageName, obj.objectName());

                    } else {
                        log.debug("Object type {} not yet supported for extraction: {}",
                                obj.objectType(), obj.objectName());
                        continue;
                    }

                    if (written > 0) {
                        filesWritten += written;
                        components.add(ExtractionResult.ExtractedComponent.builder()
                                .name(obj.objectName())
                                .type(obj.objectType())
                                .success(true)
                                .build());
                    }

                } catch (Exception e) {
                    // Check for deleted/missing objects (HTTP 404)
                    boolean isDeleted = e.getMessage() != null &&
                            (e.getMessage().contains("HTTP 404") || e.getMessage().toLowerCase().contains("not found"));

                    if (isDeleted) {
                        log.warn("Object {} ({}) appears to be deleted/missing (404). Skipping.",
                                obj.objectName(), obj.objectType());
                        // Do not add to errors list - treat as skipped
                    } else {
                        log.error("Failed to extract {}: {}", obj.objectName(), e.getMessage());
                        errors.add(ExtractionResult.ExtractionError.builder()
                                .componentName(obj.objectName())
                                .componentType(obj.objectType())
                                .errorMessage(e.getMessage())
                                .build());
                    }
                }
            }

            resultBuilder.success(errors.isEmpty()).filesWritten(filesWritten).functionModulesExtracted(fmCount)
                    .classesExtracted(classCount).totalComponents(components.size()).components(components)
                    .errors(errors).message(String.format("Extracted %d objects to %s", components.size(), basePath));

            return resultBuilder.build();

        } catch (

        Exception e) {
            log.error("Generic extraction failed", e);
            resultBuilder.message("Extraction failed: " + e.getMessage());
            return resultBuilder.build();
        }
    }

    private String extractFgFromUri(String uri) {
        if (uri == null)
            return null;
        // /sap/bc/adt/functions/groups/ZFG/fmodules/ZFM
        if (uri.contains("/groups/") && uri.contains("/fmodules/")) {
            int start = uri.indexOf("/groups/") + 8;
            int end = uri.indexOf("/fmodules/");
            if (start < end) {
                return uri.substring(start, end);
            }
        }
        return null; // Fallback or strict fail
    }
}

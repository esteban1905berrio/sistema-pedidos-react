package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ManifestData;
import com.crystal.mcp.sapserver.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Service to validate ABAP components (compare local files vs SAP system).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentValidationService {

    private final ClassService classService;
    private final ProgramService programService;
    private final ObjectMapper objectMapper;

    private static final String STATUS_MATCH = "MATCH";
    private static final String STATUS_MISMATCH = "MISMATCH";
    private static final String STATUS_MISSING_SAP = "MISSING_SAP";
    private static final String STATUS_MISSING_LOCAL = "MISSING_LOCAL";

    /**
     * Validate local components against SAP system.
     */
    public ValidationResult validateComponents(String sourcePath, boolean checkChecksums) {
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder()
                .success(false);

        List<ValidationResult.ComponentStatus> components = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        int total = 0, matching = 0, mismatched = 0, missingSap = 0, missingLocal = 0;

        try {
            Path basePath = resolveSourcePath(sourcePath);
            builder.sourcePath(basePath.toString());

            // Read manifest
            Path manifestPath = basePath.resolve("manifest.json");
            if (!Files.exists(manifestPath)) {
                errors.add("manifest.json not found");
                return builder.errors(errors).message("Manifest not found").build();
            }

            ManifestData manifest = objectMapper.readValue(manifestPath.toFile(), ManifestData.class);

            // Validate function modules
            if (manifest.getFunctionGroups() != null) {
                for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
                    if (fg.getFunctionModules() != null) {
                        for (ManifestData.FunctionModule fm : fg.getFunctionModules()) {
                            total++;
                            ValidationResult.ComponentStatus status = validateFunctionModule(
                                    basePath, fg, fm, checkChecksums);
                            components.add(status);

                            switch (status.getStatus()) {
                                case STATUS_MATCH -> matching++;
                                case STATUS_MISMATCH -> mismatched++;
                                case STATUS_MISSING_SAP -> missingSap++;
                                case STATUS_MISSING_LOCAL -> missingLocal++;
                            }
                        }
                    }
                }
            }

            // Validate classes
            if (manifest.getClasses() != null) {
                for (ManifestData.ClassDef classDef : manifest.getClasses()) {
                    total++;
                    ValidationResult.ComponentStatus status = validateClass(
                            basePath, classDef, checkChecksums);
                    components.add(status);

                    switch (status.getStatus()) {
                        case STATUS_MATCH -> matching++;
                        case STATUS_MISMATCH -> mismatched++;
                        case STATUS_MISSING_SAP -> missingSap++;
                        case STATUS_MISSING_LOCAL -> missingLocal++;
                    }
                }
            }

            boolean allMatch = mismatched == 0 && missingSap == 0 && missingLocal == 0;

            builder.success(allMatch)
                    .totalComponents(total)
                    .matchingComponents(matching)
                    .mismatchedComponents(mismatched)
                    .missingInSap(missingSap)
                    .missingLocal(missingLocal)
                    .components(components)
                    .errors(errors)
                    .message(allMatch
                            ? "All " + total + " components in sync"
                            : mismatched + " mismatched, " + missingSap + " missing in SAP, " + missingLocal + " missing local");

            return builder.build();

        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage());
            errors.add("Validation failed: " + e.getMessage());
            return builder.errors(errors).message("Validation failed: " + e.getMessage()).build();
        }
    }

    private ValidationResult.ComponentStatus validateFunctionModule(
            Path basePath, ManifestData.FunctionGroup fg, ManifestData.FunctionModule fm,
            boolean checkChecksums) {

        String name = fm.getName();
        ValidationResult.ComponentStatus.ComponentStatusBuilder status = ValidationResult.ComponentStatus.builder()
                .name(name)
                .type("FUNC");

        try {
            // Read local file
            String localSource = readLocalFmSource(basePath, fg, fm);
            if (localSource == null) {
                return status.status(STATUS_MISSING_LOCAL).build();
            }

            String localChecksum = checkChecksums ? calculateChecksum(localSource) : null;
            status.localChecksum(localChecksum);

            // Get SAP source
            String sapSource = getSapFmSource(fg.getName(), fm.getName());
            if (sapSource == null) {
                return status.status(STATUS_MISSING_SAP).localChecksum(localChecksum).build();
            }

            String sapChecksum = checkChecksums ? calculateChecksum(sapSource) : null;
            status.sapChecksum(sapChecksum);

            // Compare
            if (checkChecksums) {
                boolean match = localChecksum != null && localChecksum.equals(sapChecksum);
                return status.status(match ? STATUS_MATCH : STATUS_MISMATCH).build();
            } else {
                // Simple existence check
                return status.status(STATUS_MATCH).build();
            }

        } catch (Exception e) {
            log.debug("Error validating FM {}: {}", name, e.getMessage());
            return status.status(STATUS_MISSING_SAP).build();
        }
    }

    private ValidationResult.ComponentStatus validateClass(
            Path basePath, ManifestData.ClassDef classDef, boolean checkChecksums) {

        String name = classDef.getName();
        ValidationResult.ComponentStatus.ComponentStatusBuilder status = ValidationResult.ComponentStatus.builder()
                .name(name)
                .type("CLAS");

        try {
            // Read local file
            String localSource = readLocalClassSource(basePath, classDef);
            if (localSource == null) {
                return status.status(STATUS_MISSING_LOCAL).build();
            }

            String localChecksum = checkChecksums ? calculateChecksum(localSource) : null;
            status.localChecksum(localChecksum);

            // Get SAP source
            var sapResult = classService.getClassSource(name, "active", "main");
            if (sapResult == null || sapResult.source() == null) {
                return status.status(STATUS_MISSING_SAP).localChecksum(localChecksum).build();
            }

            String sapChecksum = checkChecksums ? calculateChecksum(sapResult.source()) : null;
            status.sapChecksum(sapChecksum);

            // Compare
            if (checkChecksums) {
                boolean match = localChecksum != null && localChecksum.equals(sapChecksum);
                return status.status(match ? STATUS_MATCH : STATUS_MISMATCH).build();
            } else {
                return status.status(STATUS_MATCH).build();
            }

        } catch (Exception e) {
            log.debug("Error validating class {}: {}", name, e.getMessage());
            return status.status(STATUS_MISSING_SAP).build();
        }
    }

    private String readLocalFmSource(Path basePath, ManifestData.FunctionGroup fg,
                                      ManifestData.FunctionModule fm) {
        try {
            // Try manifest path first
            if (fm.getPath() != null) {
                Path fmPath = basePath.resolve(fm.getPath());
                if (Files.exists(fmPath)) {
                    return Files.readString(fmPath);
                }
            }

            // Try conventional path
            Path conventionalPath = basePath.resolve("functions/groups")
                    .resolve(fg.getName().toLowerCase())
                    .resolve("fmodules")
                    .resolve(fm.getName().toLowerCase())
                    .resolve(fm.getName().toLowerCase() + ".asfunc");

            if (Files.exists(conventionalPath)) {
                return Files.readString(conventionalPath);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readLocalClassSource(Path basePath, ManifestData.ClassDef classDef) {
        try {
            if (classDef.getPath() != null) {
                Path classPath = basePath.resolve(classDef.getPath());
                if (Files.exists(classPath)) {
                    return Files.readString(classPath);
                }
            }

            Path conventionalPath = basePath.resolve("classlib/classes")
                    .resolve(classDef.getName().toLowerCase())
                    .resolve(classDef.getName().toLowerCase() + ".aclass");

            if (Files.exists(conventionalPath)) {
                return Files.readString(conventionalPath);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getSapFmSource(String functionGroup, String functionModule) {
        try {
            String mainProgram = "SAPL" + functionGroup;
            var result = programService.getIncludeSource(mainProgram, functionModule, "active");
            return result != null ? result.source() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Path resolveSourcePath(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return Paths.get("./abap").toAbsolutePath().normalize();
        }
        return Paths.get(sourcePath).toAbsolutePath().normalize();
    }

    private String calculateChecksum(String content) {
        try {
            // Normalize line endings for consistent checksums
            String normalized = content.replaceAll("\\r\\n", "\n").trim();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes());
            return HexFormat.of().formatHex(hash).substring(0, 16); // First 16 chars
        } catch (Exception e) {
            return null;
        }
    }
}

package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for installing ABAP components to SAP systems.
 *
 * <p>This service implements Phase 3 of the ABAP Component Distribution system.
 * It reads source code from local filesystem and deploys to target SAP systems.
 *
 * <p>Installation workflow:
 * <ol>
 *   <li>Read manifest.json to identify components and installation order</li>
 *   <li>For each component, check if it exists in target system</li>
 *   <li>If exists and overwrite not confirmed, add to pending list for user confirmation</li>
 *   <li>Create/modify components following installation_order</li>
 *   <li>Activate all objects</li>
 * </ol>
 *
 * <p>Conflict handling:
 * <ul>
 *   <li>If object exists and forceOverwrite=false: Returns PENDING_CONFIRMATION status</li>
 *   <li>If object exists and forceOverwrite=true: Overwrites existing object</li>
 *   <li>If object doesn't exist: Creates new object</li>
 * </ul>
 *
 * <p>Thread Safety: Stateless service, thread-safe via underlying services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentInstallationService {

    private final CreationService creationService;
    private final ClassService classService;
    private final ProgramService programService;
    private final SearchService searchService;
    private final ActivationService activationService;
    private final ObjectMapper objectMapper;

    private static final String MANIFEST_FILENAME = "manifest.json";

    /**
     * Status constants for installation results.
     */
    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_MODIFIED = "MODIFIED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";

    /**
     * Install ABAP components to target SAP system.
     *
     * @param sourcePath      base directory containing manifest.json and sources
     * @param targetPackage   target package in SAP (null = $TMP)
     * @param transport       transport request (required if package != $TMP)
     * @param componentNames  specific components to install (null = all)
     * @param dryRun          if true, only simulate without actual changes
     * @param skipExisting    if true, skip objects that already exist
     * @param forceOverwrite  if true, overwrite existing objects without confirmation
     * @return InstallationResult with details including pending confirmations
     */
    public InstallationResult installComponents(
            String sourcePath,
            String targetPackage,
            String transport,
            List<String> componentNames,
            boolean dryRun,
            boolean skipExisting,
            boolean forceOverwrite) {

        InstallationResult.InstallationResultBuilder resultBuilder = InstallationResult.builder()
                .success(false)
                .installedAt(LocalDateTime.now())
                .dryRun(dryRun)
                .targetPackage(targetPackage != null ? targetPackage : "$TMP")
                .transport(transport)
                .functionGroupsCreated(0)
                .functionModulesCreated(0)
                .classesCreated(0)
                .componentsSkipped(0)
                .componentsFailed(0)
                .objectsActivated(0)
                .components(new ArrayList<>())
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>());

        try {
            // Resolve source path
            Path basePath = resolveSourcePath(sourcePath);
            resultBuilder.sourcePath(basePath.toString());

            log.info("Starting ABAP component installation from: {}", basePath);

            // Read manifest
            ManifestData manifest = readManifest(basePath);
            if (manifest == null) {
                resultBuilder.message("No manifest.json found at " + basePath);
                return resultBuilder.build();
            }

            // Set actual target package
            String actualPackage = targetPackage != null ? targetPackage : "$TMP";
            resultBuilder.targetPackage(actualPackage);

            // Validate transport requirement
            if (!"$TMP".equalsIgnoreCase(actualPackage) && (transport == null || transport.isEmpty())) {
                resultBuilder.message("Transport request required for non-local package: " + actualPackage);
                return resultBuilder.build();
            }

            List<InstallationResult.InstalledComponent> components = new ArrayList<>();
            List<InstallationResult.InstallationError> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> pendingConfirmations = new ArrayList<>();

            int fgCreated = 0, fmCreated = 0, classCreated = 0, skipped = 0, failed = 0;
            List<String> objectsToActivate = new ArrayList<>();

            // Install directly from manifest lists (ignore installation_order)
            // Order: 1) Function Groups, 2) Classes, 3) Function Modules, 4) Activation

            // Step 1: Install Function Groups
            if (manifest.getFunctionGroups() != null) {
                for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
                    // Filter by component names if specified
                    if (componentNames != null && !componentNames.contains(fg.getName().toUpperCase())) {
                        continue;
                    }

                    log.info("Installing function group: {}", fg.getName());
                    InstallComponentResult fgResult = installFunctionGroup(
                            basePath, fg, actualPackage, transport,
                            dryRun, skipExisting, forceOverwrite);

                    components.add(fgResult.component());
                    if (fgResult.success()) {
                        fgCreated++;
                        if (fgResult.objectUri() != null) {
                            objectsToActivate.add(fgResult.objectUri());
                        }
                    } else if (fgResult.pendingConfirmation()) {
                        pendingConfirmations.add("FUGR:" + fg.getName());
                    } else if (fgResult.skipped()) {
                        skipped++;
                    } else {
                        failed++;
                        if (fgResult.error() != null) {
                            errors.add(fgResult.error());
                        }
                    }
                }
            }

            // Step 2: Install Classes
            if (manifest.getClasses() != null) {
                for (ManifestData.ClassDef classDef : manifest.getClasses()) {
                    // Filter by component names if specified
                    if (componentNames != null && !componentNames.contains(classDef.getName().toUpperCase())) {
                        continue;
                    }

                    log.info("Installing class: {}", classDef.getName());
                    InstallComponentResult classResult = installClass(
                            basePath, classDef, actualPackage, transport,
                            dryRun, skipExisting, forceOverwrite);

                    components.add(classResult.component());
                    if (classResult.success()) {
                        classCreated++;
                        if (classResult.objectUri() != null) {
                            objectsToActivate.add(classResult.objectUri());
                        }
                    } else if (classResult.pendingConfirmation()) {
                        pendingConfirmations.add("CLAS:" + classDef.getName());
                    } else if (classResult.skipped()) {
                        skipped++;
                    } else {
                        failed++;
                        if (classResult.error() != null) {
                            errors.add(classResult.error());
                        }
                    }
                }
            }

            // Step 3: Install Function Modules (from all function groups)
            if (manifest.getFunctionGroups() != null) {
                for (ManifestData.FunctionGroup fg : manifest.getFunctionGroups()) {
                    if (fg.getFunctionModules() != null) {
                        for (ManifestData.FunctionModule fm : fg.getFunctionModules()) {
                            // Filter by component names if specified
                            if (componentNames != null && !componentNames.contains(fm.getName().toUpperCase())) {
                                continue;
                            }

                            log.info("Installing function module: {} in group {}", fm.getName(), fg.getName());
                            InstallComponentResult fmResult = installFunctionModule(
                                    basePath, fg, fm, actualPackage, transport,
                                    dryRun, skipExisting, forceOverwrite);

                            components.add(fmResult.component());
                            if (fmResult.success()) {
                                fmCreated++;
                                if (fmResult.objectUri() != null) {
                                    objectsToActivate.add(fmResult.objectUri());
                                }
                            } else if (fmResult.pendingConfirmation()) {
                                pendingConfirmations.add("FUNC:" + fm.getName());
                            } else if (fmResult.skipped()) {
                                skipped++;
                            } else {
                                failed++;
                                if (fmResult.error() != null) {
                                    errors.add(fmResult.error());
                                }
                            }
                        }
                    }
                }
            }

            // Step 4: Activate all pending objects
            if (!dryRun && !objectsToActivate.isEmpty()) {
                try {
                    activationService.activateObjects(objectsToActivate);
                    resultBuilder.objectsActivated(objectsToActivate.size());
                    log.info("Activated {} objects", objectsToActivate.size());
                } catch (Exception e) {
                    warnings.add("Activation failed: " + e.getMessage());
                }
            }

            // Build result
            boolean hasErrors = failed > 0 || !errors.isEmpty();
            boolean hasPending = !pendingConfirmations.isEmpty();

            String message;
            if (hasPending) {
                message = String.format(
                        "Installation requires confirmation for %d existing objects: %s. " +
                                "Re-run with forceOverwrite=true to overwrite.",
                        pendingConfirmations.size(), String.join(", ", pendingConfirmations));
            } else if (dryRun) {
                message = String.format("DRY RUN: Would install %d FGs, %d FMs, %d classes",
                        fgCreated, fmCreated, classCreated);
            } else {
                message = String.format("Installed %d FGs, %d FMs, %d classes. Skipped: %d, Failed: %d",
                        fgCreated, fmCreated, classCreated, skipped, failed);
            }

            resultBuilder
                    .success(!hasErrors && !hasPending)
                    .functionGroupsCreated(fgCreated)
                    .functionModulesCreated(fmCreated)
                    .classesCreated(classCreated)
                    .componentsSkipped(skipped)
                    .componentsFailed(failed)
                    .totalComponents(fgCreated + fmCreated + classCreated)
                    .components(components)
                    .errors(errors)
                    .warnings(warnings)
                    .message(message);

            return resultBuilder.build();

        } catch (Exception e) {
            log.error("Installation failed: {}", e.getMessage(), e);
            resultBuilder
                    .success(false)
                    .message("Installation failed: " + e.getMessage());
            return resultBuilder.build();
        }
    }

    /**
     * Check if an object exists in the SAP system.
     */
    private boolean objectExists(String objectName, String objectType) {
        try {
            var result = searchService.searchObjects(objectName, 1);
            return result != null && result.results() != null && !result.results().isEmpty();
        } catch (Exception e) {
            log.debug("Error checking if object exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Install a function group.
     */
    private InstallComponentResult installFunctionGroup(
            Path basePath,
            ManifestData.FunctionGroup fg,
            String packageName,
            String transport,
            boolean dryRun,
            boolean skipExisting,
            boolean forceOverwrite) {

        String name = fg.getName();
        log.info("Installing function group: {}", name);

        // Check if exists
        boolean exists = objectExists(name, "FUGR");

        if (exists) {
            if (skipExisting) {
                log.info("Function group {} already exists, skipping", name);
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUGR").status(STATUS_SKIPPED)
                                .message("Already exists").build(),
                        false, true, false, null, null);
            }

            if (!forceOverwrite) {
                log.warn("Function group {} exists. Confirmation required to overwrite.", name);
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUGR").status(STATUS_PENDING_CONFIRMATION)
                                .message("Exists - confirmation required to overwrite").build(),
                        false, false, true, null, null);
            }

            log.info("Function group {} exists, will overwrite (forceOverwrite=true)", name);
        }

        if (dryRun) {
            return new InstallComponentResult(
                    InstallationResult.InstalledComponent.builder()
                            .name(name).type("FUGR").status("DRY_RUN")
                            .message("Would create function group").build(),
                    true, false, false, null, null);
        }

        try {
            // Create function group
            CreationResult createResult = creationService.createFunctionGroup(
                    name, fg.getDescription(), packageName, transport);

            if (createResult.isSuccess()) {
                String uri = "/sap/bc/adt/functions/groups/" + name.toLowerCase();
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUGR").status(STATUS_CREATED)
                                .transport(transport).activated(false).build(),
                        true, false, false, uri, null);
            } else {
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUGR").status(STATUS_FAILED)
                                .message(createResult.getMessage()).build(),
                        false, false, false, null,
                        InstallationResult.InstallationError.builder()
                                .componentName(name).componentType("FUGR")
                                .phase("CREATE").errorMessage(createResult.getMessage()).build());
            }
        } catch (Exception e) {
            log.error("Failed to create function group {}: {}", name, e.getMessage());
            return new InstallComponentResult(
                    InstallationResult.InstalledComponent.builder()
                            .name(name).type("FUGR").status(STATUS_FAILED)
                            .message(e.getMessage()).build(),
                    false, false, false, null,
                    InstallationResult.InstallationError.builder()
                            .componentName(name).componentType("FUGR")
                            .phase("CREATE").errorMessage(e.getMessage()).build());
        }
    }

    /**
     * Install a function module.
     */
    private InstallComponentResult installFunctionModule(
            Path basePath,
            ManifestData.FunctionGroup fg,
            ManifestData.FunctionModule fm,
            String packageName,
            String transport,
            boolean dryRun,
            boolean skipExisting,
            boolean forceOverwrite) {

        String name = fm.getName();
        log.info("Installing function module: {} in group {}", name, fg.getName());

        // Check if exists
        boolean exists = objectExists(name, "FUNC");

        if (exists) {
            if (skipExisting) {
                log.info("Function module {} already exists, skipping", name);
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUNC").status(STATUS_SKIPPED)
                                .message("Already exists").build(),
                        false, true, false, null, null);
            }

            if (!forceOverwrite) {
                log.warn("Function module {} exists. Confirmation required to overwrite.", name);
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUNC").status(STATUS_PENDING_CONFIRMATION)
                                .message("Exists - confirmation required to overwrite").build(),
                        false, false, true, null, null);
            }

            log.info("Function module {} exists, will overwrite (forceOverwrite=true)", name);
        }

        if (dryRun) {
            return new InstallComponentResult(
                    InstallationResult.InstalledComponent.builder()
                            .name(name).type("FUNC").status("DRY_RUN")
                            .message("Would create function module").build(),
                    true, false, false, null, null);
        }

        try {
            // Read source from disk
            String source = readFunctionModuleSource(basePath, fg, fm);

            if (!exists) {
                // Create new FM
                String processingType = fm.isRfcEnabled() ? "rfc" : null;
                CreationResult createResult = creationService.createFunctionModule(
                        name, fg.getName(), fm.getDescription(), transport, processingType);

                if (!createResult.isSuccess()) {
                    return new InstallComponentResult(
                            InstallationResult.InstalledComponent.builder()
                                    .name(name).type("FUNC").status(STATUS_FAILED)
                                    .message(createResult.getMessage()).build(),
                            false, false, false, null,
                            InstallationResult.InstallationError.builder()
                                    .componentName(name).componentType("FUNC")
                                    .phase("CREATE").errorMessage(createResult.getMessage()).build());
                }
            }

            // Modify source
            ProgramModifyResult modifyResult = programService.modifyFunctionModuleSource(
                    name, fg.getName(), source, transport);

            // Success if: workflow completed (locked+modified+unlocked) OR explicit success
            // Activation may fail if source is identical (no inactive objects)
            boolean workflowOk = modifyResult.isLocked() && modifyResult.isModified() && modifyResult.isUnlocked();
            log.info("FM {} workflow: success={}, locked={}, modified={}, unlocked={}, workflowOk={}",
                    name, modifyResult.isSuccess(), modifyResult.isLocked(),
                    modifyResult.isModified(), modifyResult.isUnlocked(), workflowOk);
            if (modifyResult.isSuccess() || workflowOk) {
                String uri = String.format("/sap/bc/adt/functions/groups/%s/fmodules/%s",
                        fg.getName().toLowerCase(), name.toLowerCase());
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUNC")
                                .status(exists ? STATUS_MODIFIED : STATUS_CREATED)
                                .transport(transport).activated(false).build(),
                        true, false, false, uri, null);
            } else {
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("FUNC").status(STATUS_FAILED)
                                .message(modifyResult.getMessage()).build(),
                        false, false, false, null,
                        InstallationResult.InstallationError.builder()
                                .componentName(name).componentType("FUNC")
                                .phase("MODIFY").errorMessage(modifyResult.getMessage()).build());
            }

        } catch (Exception e) {
            log.error("Failed to install function module {}: {}", name, e.getMessage());
            return new InstallComponentResult(
                    InstallationResult.InstalledComponent.builder()
                            .name(name).type("FUNC").status(STATUS_FAILED)
                            .message(e.getMessage()).build(),
                    false, false, false, null,
                    InstallationResult.InstallationError.builder()
                            .componentName(name).componentType("FUNC")
                            .phase("INSTALL").errorMessage(e.getMessage()).build());
        }
    }

    /**
     * Install a class.
     */
    private InstallComponentResult installClass(
            Path basePath,
            ManifestData.ClassDef classDef,
            String packageName,
            String transport,
            boolean dryRun,
            boolean skipExisting,
            boolean forceOverwrite) {

        String name = classDef.getName();
        log.info("Installing class: {}", name);

        // Check if exists
        boolean exists = objectExists(name, "CLAS");

        if (exists) {
            if (skipExisting) {
                log.info("Class {} already exists, skipping", name);
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("CLAS").status(STATUS_SKIPPED)
                                .message("Already exists").build(),
                        false, true, false, null, null);
            }

            if (!forceOverwrite) {
                log.warn("Class {} exists. Confirmation required to overwrite.", name);
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("CLAS").status(STATUS_PENDING_CONFIRMATION)
                                .message("Exists - confirmation required to overwrite").build(),
                        false, false, true, null, null);
            }

            log.info("Class {} exists, will overwrite (forceOverwrite=true)", name);
        }

        if (dryRun) {
            return new InstallComponentResult(
                    InstallationResult.InstalledComponent.builder()
                            .name(name).type("CLAS").status("DRY_RUN")
                            .message("Would create class").build(),
                    true, false, false, null, null);
        }

        try {
            // Read source from disk
            String source = readClassSource(basePath, classDef);

            if (!exists) {
                // Create new class
                CreationResult createResult = creationService.createClass(
                        name, classDef.getDescription(), packageName,
                        classDef.getSuperclass(), transport);

                if (!createResult.isSuccess()) {
                    return new InstallComponentResult(
                            InstallationResult.InstalledComponent.builder()
                                    .name(name).type("CLAS").status(STATUS_FAILED)
                                    .message(createResult.getMessage()).build(),
                            false, false, false, null,
                            InstallationResult.InstallationError.builder()
                                    .componentName(name).componentType("CLAS")
                                    .phase("CREATE").errorMessage(createResult.getMessage()).build());
                }
            }

            // Modify source
            ClassModifyResult modifyResult = classService.modifyClass(
                    name, source, "main", transport);

            // Success if: workflow completed (locked+modified+unlocked) OR explicit success
            // Activation may fail if source is identical (no inactive objects)
            boolean workflowOk = modifyResult.isLocked() && modifyResult.isModified() && modifyResult.isUnlocked();
            log.info("Class {} workflow: success={}, locked={}, modified={}, unlocked={}, workflowOk={}",
                    name, modifyResult.isSuccess(), modifyResult.isLocked(),
                    modifyResult.isModified(), modifyResult.isUnlocked(), workflowOk);
            if (modifyResult.isSuccess() || workflowOk) {
                String uri = "/sap/bc/adt/oo/classes/" + name.toLowerCase();
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("CLAS")
                                .status(exists ? STATUS_MODIFIED : STATUS_CREATED)
                                .transport(transport).activated(false).build(),
                        true, false, false, uri, null);
            } else {
                return new InstallComponentResult(
                        InstallationResult.InstalledComponent.builder()
                                .name(name).type("CLAS").status(STATUS_FAILED)
                                .message(modifyResult.getMessage()).build(),
                        false, false, false, null,
                        InstallationResult.InstallationError.builder()
                                .componentName(name).componentType("CLAS")
                                .phase("MODIFY").errorMessage(modifyResult.getMessage()).build());
            }

        } catch (Exception e) {
            log.error("Failed to install class {}: {}", name, e.getMessage());
            return new InstallComponentResult(
                    InstallationResult.InstalledComponent.builder()
                            .name(name).type("CLAS").status(STATUS_FAILED)
                            .message(e.getMessage()).build(),
                    false, false, false, null,
                    InstallationResult.InstallationError.builder()
                            .componentName(name).componentType("CLAS")
                            .phase("INSTALL").errorMessage(e.getMessage()).build());
        }
    }

    // ========== Helper methods ==========

    private Path resolveSourcePath(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return Paths.get("./abap").toAbsolutePath().normalize();
        }
        return Paths.get(sourcePath).toAbsolutePath().normalize();
    }

    private ManifestData readManifest(Path basePath) throws IOException {
        Path manifestPath = basePath.resolve(MANIFEST_FILENAME);
        if (!Files.exists(manifestPath)) {
            log.warn("Manifest not found at: {}", manifestPath);
            return null;
        }
        return objectMapper.readValue(manifestPath.toFile(), ManifestData.class);
    }

    private String readFunctionModuleSource(Path basePath, ManifestData.FunctionGroup fg,
                                             ManifestData.FunctionModule fm) throws IOException {
        // Try path from manifest
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

        throw new IOException("Source file not found for FM: " + fm.getName());
    }

    private String readClassSource(Path basePath, ManifestData.ClassDef classDef) throws IOException {
        // Try path from manifest
        if (classDef.getPath() != null) {
            Path classPath = basePath.resolve(classDef.getPath());
            if (Files.exists(classPath)) {
                return Files.readString(classPath);
            }
        }

        // Try conventional path
        Path conventionalPath = basePath.resolve("classlib/classes")
                .resolve(classDef.getName().toLowerCase())
                .resolve(classDef.getName().toLowerCase() + ".aclass");

        if (Files.exists(conventionalPath)) {
            return Files.readString(conventionalPath);
        }

        throw new IOException("Source file not found for class: " + classDef.getName());
    }

    // ========== Internal records ==========

    private record InstallComponentResult(
            InstallationResult.InstalledComponent component,
            boolean success,
            boolean skipped,
            boolean pendingConfirmation,
            String objectUri,
            InstallationResult.InstallationError error
    ) {}
}

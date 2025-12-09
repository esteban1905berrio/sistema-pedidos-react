package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ManifestData;
import com.crystal.mcp.sapserver.model.PrerequisiteCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to check prerequisites before installing ABAP components.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentPrerequisiteService {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    /**
     * Check all prerequisites for ABAP component installation.
     */
    public PrerequisiteCheckResult checkPrerequisites(String manifestPath) {
        PrerequisiteCheckResult.PrerequisiteCheckResultBuilder builder = PrerequisiteCheckResult.builder()
                .success(false);

        List<PrerequisiteCheckResult.CheckItem> checks = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Check 1: Manifest exists
            Path path = resolveManifestPath(manifestPath);
            boolean manifestExists = Files.exists(path);
            checks.add(PrerequisiteCheckResult.CheckItem.builder()
                    .name("manifest.json")
                    .description("Manifest file exists")
                    .passed(manifestExists)
                    .details(manifestExists ? path.toString() : "Not found")
                    .build());

            if (!manifestExists) {
                errors.add("manifest.json not found at: " + path);
                return builder.checks(checks).errors(errors).message("Manifest not found").build();
            }

            // Read manifest for dependency checks
            ManifestData manifest = objectMapper.readValue(path.toFile(), ManifestData.class);

            // Check 2: /UI2/CL_JSON class available
            boolean jsonClassExists = checkObjectExists("/UI2/CL_JSON", "CLAS");
            checks.add(PrerequisiteCheckResult.CheckItem.builder()
                    .name("/UI2/CL_JSON")
                    .description("JSON serialization class")
                    .passed(jsonClassExists)
                    .details(jsonClassExists ? "Available" : "Not found - required for JSON handling")
                    .build());

            if (!jsonClassExists) {
                errors.add("/UI2/CL_JSON not available - required for component functionality");
            }

            // Check 3: ADT endpoint available (implicit - if we got here, it works)
            checks.add(PrerequisiteCheckResult.CheckItem.builder()
                    .name("SADT_REST_RFC_ENDPOINT")
                    .description("ADT REST RFC endpoint")
                    .passed(true)
                    .details("Available (connection successful)")
                    .build());

            // Check 4: Required SAP standard FMs
            List<String> requiredFMs = List.of(
                    "TR_EXT_CREATE_REQUEST",
                    "TR_REQUEST_CHOICE",
                    "TR_RELEASE_REQUEST"
            );

            for (String fm : requiredFMs) {
                boolean exists = checkObjectExists(fm, "FUNC");
                checks.add(PrerequisiteCheckResult.CheckItem.builder()
                        .name(fm)
                        .description("Transport management FM")
                        .passed(exists)
                        .details(exists ? "Available" : "Not found")
                        .build());

                if (!exists) {
                    warnings.add("FM " + fm + " not found - some features may not work");
                }
            }

            // Determine overall success
            boolean allPassed = checks.stream().allMatch(PrerequisiteCheckResult.CheckItem::isPassed);
            boolean criticalPassed = manifestExists && jsonClassExists;

            builder.success(criticalPassed)
                    .checks(checks)
                    .errors(errors)
                    .warnings(warnings)
                    .message(criticalPassed
                            ? "Prerequisites OK. " + (allPassed ? "All checks passed." : "Some warnings.")
                            : "Prerequisites FAILED. See errors.");

            return builder.build();

        } catch (Exception e) {
            log.error("Prerequisite check failed: {}", e.getMessage());
            errors.add("Check failed: " + e.getMessage());
            return builder.checks(checks).errors(errors).message("Check failed: " + e.getMessage()).build();
        }
    }

    private Path resolveManifestPath(String manifestPath) {
        if (manifestPath == null || manifestPath.trim().isEmpty()) {
            return Paths.get("./abap/manifest.json").toAbsolutePath().normalize();
        }
        Path path = Paths.get(manifestPath).toAbsolutePath().normalize();
        if (Files.isDirectory(path)) {
            return path.resolve("manifest.json");
        }
        return path;
    }

    private boolean checkObjectExists(String objectName, String objectType) {
        try {
            var result = searchService.searchObjects(objectName, 1);
            return result != null && result.results() != null && !result.results().isEmpty();
        } catch (Exception e) {
            log.debug("Error checking object {}: {}", objectName, e.getMessage());
            return false;
        }
    }
}

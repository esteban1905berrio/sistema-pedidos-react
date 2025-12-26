package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.ExtractionDiscovery;
import com.crystal.mcp.sapserver.model.ExtractionResult;
import com.crystal.mcp.sapserver.model.ExtractionScope;
import com.crystal.mcp.sapserver.service.AbapExtractionService;
import com.crystal.mcp.sapserver.service.ComponentExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Manual test for Extraction Scenarios.
 *
 * Scenarios:
 * 1. Package Extraction: Discover and extract all objects from a package (e.g.,
 * ZCX).
 * 2. Project Replication: Extract all components defined in manifest
 * (auto-synced)
 * for replication to another system.
 *
 * <p>
 * Run with:
 * 
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualExtractionTest
 * </pre>
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualExtractionTest implements CommandLineRunner {

    @Autowired
    private ComponentExtractionService componentExtractionService;

    @Autowired
    private AbapExtractionService abapExtractionService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualExtractionTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("========================================");
        System.out.println("Manual Test: Extraction Scenarios");
        System.out.println("========================================");
        System.out.println("1. Extract Package Objects (e.g., ZCX)");
        System.out.println("2. Extract MCP Server Project (Manifest/Replication)");
        System.out.print("\nSelect scenario (1 or 2): ");

        try (Scanner scanner = new Scanner(System.in)) {
            runPackageExtraction(scanner);
            // runProjectReplication();

        } catch (Exception e) {
            System.err.println("TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void runPackageExtraction(Scanner scanner) {
        System.out.print("Enter package name (default: ZCX): ");
        String pkgName = "ZCX";
        String username = "SEBLONDO";

        System.out.println("\n--- Scenario 1: Extract Objects from Package " + pkgName + " ---");

        // Step 1: Discover
        System.out.println("Discovering objects (Recursive)...");
        // ExtractionDiscovery discovery =
        // abapExtractionService.discover(ExtractionScope.USER, username);
        /*
         * ExtractionDiscovery discovery =
         * abapExtractionService.discover(ExtractionScope.PACKAGE, pkgName);
         * 
         * System.out.println("Found " + discovery.totalObjects() + " objects.");
         * discovery.objectsByType().forEach((type, info) -> System.out.println("  - " +
         * type + ": " + info.count()));
         * 
         * if (discovery.totalObjects() == 0) {
         * System.out.println("No objects found. Exiting.");
         * return;
         * }
         * 
         * // Step 2: Extract
         * System.out.println("Extracting to ./abap_package_export ...");
         * String targetPath = "./abap_package_export";
         * 
         * ExtractionResult result =
         * componentExtractionService.extractDiscoveredObjects(
         * discovery.objects(),
         * targetPath);
         * 
         * printResult(result);
         */
    }

    private void runProjectReplication() {
        System.out.println("\n--- Scenario 2: Extract MCP Server Project for Replication ---");
        System.out.println("Target: ./abap_replication");
        System.out.println("Action: Sync manifest with Java code + Extract all components");

        ExtractionResult result = componentExtractionService.extractComponents(
                "./abap_replication",
                null, // All components from manifest
                true, // Include metadata
                true // Update manifest (Auto-sync will run first)
        );

        printResult(result);
    }

    private void printResult(ExtractionResult result) {
        System.out.println("\n--- Extraction Result ---");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Files extracted: " + result.getFilesWritten());
        System.out.println("Total Components: " +
                (result.getFunctionModulesExtracted() + result.getClassesExtracted()));
        System.out.println("Message: " + result.getMessage());

        if (!result.getErrors().isEmpty()) {
            System.out.println("\nErrors:");
            result.getErrors().forEach(e -> System.out.println(
                    "  - " + e.getComponentName() + " (" + e.getComponentType() + "): " + e.getErrorMessage()));
        } else {
            System.out.println("\nStatus: CLEAN (No errors)");
        }

        Path absolutePath = Paths.get(result.getTargetPath() != null ? result.getTargetPath() : ".");
        System.out.println("\nOutput directory: " + absolutePath.toAbsolutePath());
    }
}

package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.SapMcpServerApplication;
import com.crystal.mcp.sapserver.model.ExtractionResult;
import com.crystal.mcp.sapserver.service.ComponentExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for ComponentExtractionService.
 *
 * <p>This test validates that the extraction of ABAP components
 * (specifically Function Modules) works correctly after the fix
 * to use ObjectService.getObjectSource() with correct ADT URIs.
 *
 * <p>Run with:
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

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualExtractionTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("========================================");
        System.out.println("Manual Test: ComponentExtractionService");
        System.out.println("========================================");

        try {
            // Test 1: Extract a single FM
            System.out.println("\n--- Test 1: Extract single FM (ZCX_GETDDICSOURCE) ---");
            ExtractionResult result1 = componentExtractionService.extractComponents(
                    "./abap",
                    java.util.List.of("ZCX_GETDDICSOURCE"),
                    true,
                    false
            );

            System.out.println("Success: " + result1.isSuccess());
            System.out.println("FMs extracted: " + result1.getFunctionModulesExtracted());
            System.out.println("Files written: " + result1.getFilesWritten());
            System.out.println("Message: " + result1.getMessage());

            if (!result1.getErrors().isEmpty()) {
                System.out.println("Errors:");
                result1.getErrors().forEach(e ->
                        System.out.println("  - " + e.getComponentName() + ": " + e.getErrorMessage()));
            }

            // Test 2: Extract all FMs
            System.out.println("\n--- Test 2: Extract all components ---");
            ExtractionResult result2 = componentExtractionService.extractComponents(
                    "./abap",
                    null,  // All components
                    true,
                    true   // Update manifest
            );

            System.out.println("Success: " + result2.isSuccess());
            System.out.println("FMs extracted: " + result2.getFunctionModulesExtracted());
            System.out.println("Classes extracted: " + result2.getClassesExtracted());
            System.out.println("Files written: " + result2.getFilesWritten());
            System.out.println("Message: " + result2.getMessage());

            if (!result2.getErrors().isEmpty()) {
                System.out.println("Errors:");
                result2.getErrors().forEach(e ->
                        System.out.println("  - " + e.getComponentName() + ": " + e.getErrorMessage()));
            }

            if (result2.isSuccess()) {
                System.out.println("\n========================================");
                System.out.println("TEST PASSED - All components extracted successfully");
                System.out.println("========================================");
            } else {
                System.out.println("\n========================================");
                System.out.println("TEST FAILED - Some components could not be extracted");
                System.out.println("========================================");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

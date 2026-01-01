package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.CreationResult;
import com.crystal.mcp.sapserver.service.CreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for CreationService.createProgram() and createInclude()
 * WITH Spring Boot but WITHOUT MCP.
 *
 * This test validates:
 * 1. CreationService.createProgram() logic
 * 2. CreationService.createInclude() logic
 * 3. ADT XML generation and RFC calls
 *
 * Prerequisites:
 * 1. ManualJCoConnectionTest passed
 * 2. .env file configured with valid SAP credentials
 * 3. VPN connection active
 * 4. User has developer permissions in the target system
 *
 * How to run:
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualCreationTest
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualCreationTest implements CommandLineRunner {

    @Autowired
    private CreationService creationService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualCreationTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== Manual CreationService Test ===\n");

        /*
         * // UNCOMMENT TO RUN TESTS
         * // WARNING: This CREATEs objects in the SAP system.
         * // Use a temporary package ($TMP) to avoid transport prompts,
         * // or provide a valid transport request if using a development package.
         * 
         * try {
         * // Test Data
         * String package_ = "$TMP"; // Local object
         * String transport = null; // No transport for local object
         * // String package_ = "ZTEST";
         * // String transport = "CADK911xxx";
         * 
         * // Test 1: Create Program
         * String progName = "Z_ANTIGRAVITY_MANUAL_TEST";
         * testCreateProgram(progName, "Manual Test Program", package_, transport);
         * 
         * // Test 2: Create Include
         * String incName = "Z_ANTIGRAVITY_MANUAL_TOP";
         * testCreateInclude(incName, "Manual Test Include", package_, transport);
         * 
         * System.out.println("\n=== ALL TESTS PASSED ✓ ===");
         * 
         * } catch (Exception e) {
         * System.err.println("\n=== TEST FAILED ✗ ===");
         * System.err.println("Error: " + e.getMessage());
         * e.printStackTrace();
         * System.exit(1);
         * }
         */

        System.out.println("Tests are commented out for safety. Uncomment in ManualCreationTest.java to run.");
    }

    private void testCreateProgram(String name, String desc, String pkg, String transport) {
        System.out.println("Testing Create Program: " + name);
        System.out.println("---------------------------------------------------");

        CreationResult result = creationService.createProgram(name, desc, pkg, transport);

        if (result.isSuccess()) {
            System.out.println("  ✓ Verification matched: SUCCESS");
            System.out.println("  ✓ URI: " + result.getUri());
        } else {
            throw new RuntimeException("Creation failed: " + result.getMessage());
        }
        System.out.println("\n  ✓ Test passed for: " + name + "\n");
    }

    private void testCreateInclude(String name, String desc, String pkg, String transport) {
        System.out.println("Testing Create Include: " + name);
        System.out.println("---------------------------------------------------");

        CreationResult result = creationService.createInclude(name, desc, pkg, transport);

        if (result.isSuccess()) {
            System.out.println("  ✓ Verification matched: SUCCESS");
            System.out.println("  ✓ URI: " + result.getUri());
        } else {
            throw new RuntimeException("Creation failed: " + result.getMessage());
        }
        System.out.println("\n  ✓ Test passed for: " + name + "\n");
    }
}

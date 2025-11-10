package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.ClassSourceResult;
import com.crystal.mcp.sapserver.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for ClassService.get_class_source() WITH Spring Boot but WITHOUT MCP.
 *
 * This test validates:
 * 1. Spring Boot configuration works
 * 2. JCo connection bean is created
 * 3. RfcAdapter works
 * 4. ClassService.getClassSource() retrieves ABAP source code
 *
 * Prerequisites:
 * 1. ManualJCoConnectionTest passed
 * 2. .env file configured
 * 3. VPN connection active
 *
 * How to run:
 *   mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualClassServiceTest
 *
 * Or:
 *   mvn clean package
 *   java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar \
 *        --spring.main.sources=com.crystal.mcp.sapserver.manual.ManualClassServiceTest
 */
@Profile("!test")  // Exclude from test profile to prevent auto-execution during tests
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualClassServiceTest implements CommandLineRunner {

    @Autowired
    private ClassService classService;

    public static void main(String[] args) {
        // Disable banner for cleaner output
        SpringApplication app = new SpringApplication(ManualClassServiceTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== Manual ClassService Test ===\n");

        try {
            // Test 1: Standard SAP class (should exist in all systems)
            testGetClassSource("CL_ABAP_CHAR_UTILITIES", "active", "main");

            // Test 2: Implementation include
            testGetClassSource("CL_ABAP_CHAR_UTILITIES", "active", "implementation");

            // Test 3: Custom class (replace with real class from your system)
            // testGetClassSource("ZTEST_CLASS", "active", "main");

            System.out.println("\n=== ALL TESTS PASSED ✓ ===");
            System.out.println("\nClassService is working correctly!");
            System.out.println("You can now proceed to test MCP integration.");

        } catch (Exception e) {
            System.err.println("\n=== TEST FAILED ✗ ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test get_class_source method
     */
    private void testGetClassSource(String className, String version, String includeType) {
        System.out.println("Testing: " + className + " (" + version + ", " + includeType + ")");
        System.out.println("---------------------------------------------------");

        try {
            ClassSourceResult result = classService.getClassSource(className, version, includeType);

            // Validate result
            if (result == null) {
                throw new AssertionError("Result is null!");
            }

            if (result.source() == null || result.source().isEmpty()) {
                throw new AssertionError("Source code is empty!");
            }

            // Print summary
            System.out.println("  ✓ Class: " + result.className());
            System.out.println("  ✓ Version: " + result.version());
            System.out.println("  ✓ Include Type: " + result.includeType());
            System.out.println("  ✓ Source length: " + result.source().length() + " characters");

            // Print first 200 characters of source
            String preview = result.source().substring(0, Math.min(200, result.source().length()));
            System.out.println("\n  Source preview:");
            System.out.println("  " + preview.replace("\n", "\n  "));
            if (result.source().length() > 200) {
                System.out.println("  ...");
            }

            System.out.println("\n  ✓ Test passed for: " + className + "\n");

        } catch (Exception e) {
            System.err.println("  ✗ Test failed for: " + className);
            System.err.println("  Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

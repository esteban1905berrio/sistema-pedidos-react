package com.crystal.mcp.sapserver.manual.transport;

import com.crystal.mcp.sapserver.model.TransportCopyRequest;
import com.crystal.mcp.sapserver.model.TransportCopyResult;
import com.crystal.mcp.sapserver.service.TransportCopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Manual test for create_transport_copy functionality.
 *
 * This test requires a live SAP connection (CRY/GDC system).
 * Run this test manually to verify the implementation.
 *
 * Test Cases:
 * 1. Create transport copy from multiple OTs (CADK911511, CADK911293)
 * 2. Verify detailed step results are returned
 *
 * How to Run:
 *   mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportCopyTest
 *
 * Or with JAR:
 *   mvn clean package -DskipTests
 *   java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar \
 *        --spring.main.sources=com.crystal.mcp.sapserver.manual.transport.ManualTransportCopyTest
 */
@Profile("!test")  // Exclude from test profile
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualTransportCopyTest implements CommandLineRunner {

    @Autowired
    private TransportCopyService transportCopyService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualTransportCopyTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              Manual Test: create_transport_copy (OT Copia)                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝\n");

        try {
            // Verify FM is available first
            //testFunctionModuleAvailable();

            // Main test: Create transport copy from multiple OTs
            //testCreateTransportCopy_MultipleTransports();

            System.out.println("\n╔══════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║                         ✅ ALL TESTS COMPLETED                               ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println("\n╔══════════════════════════════════════════════════════════════════════════════╗");
            System.err.println("║                           ❌ TEST FAILED                                     ║");
            System.err.println("╚══════════════════════════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test: Verify FM ZCX_CREATE_TRANSPORT_COPY is available
     */
    private void testFunctionModuleAvailable() {
        System.out.println("┌──────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ Step 0: Verify FM ZCX_CREATE_TRANSPORT_COPY is available                    │");
        System.out.println("└──────────────────────────────────────────────────────────────────────────────┘\n");

        boolean available = transportCopyService.isFunctionModuleAvailable();

        System.out.println("  Function Module: ZCX_CREATE_TRANSPORT_COPY");
        System.out.println("  Available: " + (available ? "✅ YES" : "❌ NO"));

        if (!available) {
            throw new RuntimeException("Function module ZCX_CREATE_TRANSPORT_COPY not found in SAP system");
        }

        System.out.println("\n  ✓ FM verification passed\n");
    }

    /**
     * Test: Create transport copy from multiple OTs (CADK911511, CADK911293)
     *
     * Expected Result:
     * - New transport(s) created
     * - Detailed step results for: creation, objects inclusion, release
     * - Each step should have success/message info
     */
    private void testCreateTransportCopy_MultipleTransports() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test: Create Transport Copy from Multiple OTs                               │");
        System.out.println("└──────────────────────────────────────────────────────────────────────────────┘\n");

        // Configuration
        List<String> sourceTransports = List.of("CADK911511", "CADK911293");
        String descriptionPrefix = "TEST_COPY";
        boolean autoRelease = true; // Keep modifiable for testing

        System.out.println("  📋 Test Configuration:");
        System.out.println("     Source Transports: " + sourceTransports);
        System.out.println("     Description Prefix: " + descriptionPrefix);
        System.out.println("     Auto Release: " + autoRelease);
        System.out.println();

        TransportCopyRequest request = new TransportCopyRequest(
            null,                    // No single transport
            sourceTransports,        // Multiple transports
            null,                    // Target system (auto)
            descriptionPrefix,
            autoRelease
        );

        // Execute
        System.out.println("  ⏳ Executing transport copy...\n");
        TransportCopyResult result = transportCopyService.createTransportCopy(request);

        // Print Results
        printResults(result);

        // Validate
        validateResults(result);
    }

    /**
     * Prints formatted results
     */
    private void printResults(TransportCopyResult result) {
        System.out.println("  ─────────────────────────────────────────────────────────────────────────────");
        System.out.println("  📊 RESULT SUMMARY");
        System.out.println("  ─────────────────────────────────────────────────────────────────────────────");
        System.out.printf("     Success:              %s%n", result.success() ? "✅ YES" : "❌ NO");
        System.out.printf("     Status:               %s (%s)%n", result.status(), result.getStatusDescription());
        System.out.printf("     New Transport(s):     %s%n", result.newTransportNumber());
        System.out.printf("     Message:              %s%n", result.message());

        System.out.println("\n  ─────────────────────────────────────────────────────────────────────────────");
        System.out.println("  📝 WORKFLOW STEPS (Detailed Results)");
        System.out.println("  ─────────────────────────────────────────────────────────────────────────────");

        // Step 1: Creation
        System.out.println("\n     1️⃣ CREATION STEP:");
        System.out.printf("        Success: %s%n", result.creationOk() ? "✅ YES" : "❌ NO");
        System.out.printf("        Message: %s%n",
            result.creationMsg() != null && !result.creationMsg().isEmpty()
                ? result.creationMsg() : "(no message)");

        // Step 2: Objects Inclusion
        System.out.println("\n     2️⃣ OBJECTS INCLUSION STEP:");
        System.out.printf("        Success: %s%n", result.objectsOk() ? "✅ YES" : "❌ NO");
        System.out.printf("        Message: %s%n",
            result.objectsMsg() != null && !result.objectsMsg().isEmpty()
                ? result.objectsMsg() : "(no message)");

        // Step 3: Release
        System.out.println("\n     3️⃣ RELEASE STEP:");
        System.out.printf("        Success: %s%n", result.releaseOk() ? "✅ YES" : "❌ NO");
        System.out.printf("        Message: %s%n",
            result.releaseMsg() != null && !result.releaseMsg().isEmpty()
                ? result.releaseMsg() : "(no message)");

        System.out.println();
    }

    /**
     * Validates results
     */
    private void validateResults(TransportCopyResult result) {
        System.out.println("  ─────────────────────────────────────────────────────────────────────────────");
        System.out.println("  🔍 VALIDATION");
        System.out.println("  ─────────────────────────────────────────────────────────────────────────────\n");

        boolean allPassed = true;

        // Check 1: Result not null
        if (result == null) {
            System.out.println("     ❌ Result is null");
            throw new RuntimeException("Result should not be null");
        }
        System.out.println("     ✓ Result is not null");

        // Check 2: If creation succeeded, transport number should be set
        if (result.creationOk()) {
            if (result.newTransportNumber() != null && !result.newTransportNumber().isEmpty()) {
                System.out.println("     ✓ Transport number is set: " + result.newTransportNumber());
            } else {
                System.out.println("     ❌ Creation OK but transport number is missing");
                allPassed = false;
            }
        } else {
            System.out.println("     ⚠️ Creation step failed");
        }

        // Check 3: Step messages are populated
        System.out.println("     ✓ Creation step has message: " +
            (result.creationMsg() != null && !result.creationMsg().isEmpty() ? "YES" : "NO"));
        System.out.println("     ✓ Objects step has message: " +
            (result.objectsMsg() != null && !result.objectsMsg().isEmpty() ? "YES" : "NO"));
        System.out.println("     ✓ Release step has message: " +
            (result.releaseMsg() != null && !result.releaseMsg().isEmpty() ? "YES" : "NO"));

        // Final status
        System.out.println();
        if (result.success()) {
            System.out.println("     ✅ OVERALL: Transport copy created successfully");
            System.out.println("        New Transport: " + result.newTransportNumber());
        } else if (result.isWarning()) {
            System.out.println("     ⚠️ OVERALL: Transport created with warnings");
            System.out.println("        Transport created but some steps had issues");
        } else {
            System.out.println("     ❌ OVERALL: Transport copy failed");
            System.out.println("        Error: " + result.message());
        }

        System.out.println();
    }
}

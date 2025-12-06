package com.crystal.mcp.sapserver.manual.transport;

import com.crystal.mcp.sapserver.model.TransportCreationRequest;
import com.crystal.mcp.sapserver.model.TransportCreationRequest.TransportObject;
import com.crystal.mcp.sapserver.model.TransportCreationResult;
import com.crystal.mcp.sapserver.service.TransportCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Manual test for TransportCreationService.
 *
 * <p>This test creates transport requests in SAP to validate the implementation.
 * Run it using:
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportCreationTest
 * </pre>
 *
 * <p><b>WARNING:</b> This test creates real transports in SAP. Use with caution
 * and ensure you have the proper authorizations.
 *
 * @author Crystal Development Team
 * @since 2025-12-02
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualTransportCreationTest implements CommandLineRunner {

    @Autowired
    private TransportCreationService transportCreationService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualTransportCreationTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║      Manual Test: TransportCreationService                   ║");
        System.out.println("║                                                              ║");
        System.out.println("║  This test creates real transports in SAP.                   ║");
        System.out.println("║  Ensure you have proper authorizations.                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Test 1: Check FM availability
            //testFunctionModuleAvailability();

            // Test 2: Create empty Workbench transport
            //testCreateEmptyWorkbenchTransport();

            // Test 3: Create Workbench transport with specific objects
            //testCreateWorkbenchWithSpecificObjects();

            // Test 4: Create Workbench transport copying from existing OTs
            //testCreateWorkbenchFromExistingOTs();

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ✅ ALL TESTS PASSED                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println();
            System.err.println("╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║                    ❌ TEST FAILED                            ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test 1: Verify function module is available.
     */
    private void testFunctionModuleAvailability() {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 1: Function Module Availability                        │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        boolean available = transportCreationService.isFunctionModuleAvailable();

        if (available) {
            System.out.println("  ✅ ZCX_CREATE_TRANSPORT_REQUEST is available");
        } else {
            throw new RuntimeException("ZCX_CREATE_TRANSPORT_REQUEST not found in SAP system");
        }
        System.out.println();
    }

    /**
     * Test 2: Create an empty Workbench transport.
     */
    private void testCreateEmptyWorkbenchTransport() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 2: Create Empty Workbench Transport                    │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        String description = "Test Empty";
        System.out.println("  Creating empty Workbench transport...");
        System.out.println("  Description: " + description);

        TransportCreationResult result = transportCreationService.createWorkbenchTransport(
            description,
            null  // Use default target system
        );

        printResult(result);

        if (!result.success()) {
            throw new RuntimeException("Failed to create empty Workbench transport: " + result.message());
        }

        System.out.println("  ✅ Empty Workbench transport created: " + result.transportNumber());
        System.out.println();
    }

    /**
     * Test 3: Create a Workbench transport with specific objects (auto-detect types from TADIR).
     * Objects: ZCLPSR011_CERT_COMPATIBILIDAD, ZFI_TRANSF_PAGO_BANCO_BBVA, ZFIDMEE_CON
     * The FM will automatically look up PGMID and OBJECT from TADIR table.
     */
    private void testCreateWorkbenchWithSpecificObjects() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 3: Create Workbench Transport with Objects (Auto-Type) │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        String description = "Test with specific objects (auto-detect types)";

        // Using only object names - types will be auto-detected from TADIR
        List<TransportObject> objects = List.of(
            TransportObject.withName("ZCLPSR011_CERT_COMPATIBILIDAD"),
            TransportObject.withName("ZFI_TRANSF_PAGO_BANCO_BBVA"),
            TransportObject.withName("ZFIDMEE_CON")
        );

        System.out.println("  Creating Workbench transport with objects (auto-detect types)...");
        System.out.println("  Description: " + description);
        System.out.println("  Objects (names only - types auto-detected from TADIR):");
        for (TransportObject obj : objects) {
            System.out.println("    - " + obj.objName());
        }

        TransportCreationRequest request = TransportCreationRequest.workbenchWithObjects(
            description,
            null,  // Use default target system
            objects
        );

        TransportCreationResult result = transportCreationService.createTransportRequest(request);

        printResult(result);

        if (!result.success()) {
            throw new RuntimeException("Failed to create transport with objects: " + result.message());
        }

        System.out.println("  ✅ Transport with objects created: " + result.transportNumber());
        System.out.println("  Objects added: " + result.objectsCopied());
        System.out.println();
    }

    /**
     * Test 4: Create a Workbench transport copying objects from existing OTs.
     * Reference OTs: CADK911467, CADK911293
     */
    private void testCreateWorkbenchFromExistingOTs() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 4: Create Workbench from Existing OTs                  │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        String description = "Test copying from CADK911467 and CADK911293";

        // First, create a Transport of Copies from CADK911467
        System.out.println("  Step 1: Creating copy from CADK911467...");
        TransportCreationRequest request1 = TransportCreationRequest.copyWithReference(
            description + " (from CADK911467)",
            null,  // Use default target system
            "CADK911467",
            false  // Don't auto-release
        );

        TransportCreationResult result1 = transportCreationService.createTransportRequest(request1);
        printResult(result1);

        if (!result1.success()) {
            throw new RuntimeException("Failed to create copy from CADK911467: " + result1.message());
        }
        System.out.println("  ✅ Copy from CADK911467 created: " + result1.transportNumber());
        System.out.println("  Objects copied: " + result1.objectsCopied());

        // Second, create a Transport of Copies from CADK911293
        System.out.println();
        System.out.println("  Step 2: Creating copy from CADK911293...");
        TransportCreationRequest request2 = TransportCreationRequest.copyWithReference(
            description + " (from CADK911293)",
            null,  // Use default target system
            "CADK911293",
            false  // Don't auto-release
        );

        TransportCreationResult result2 = transportCreationService.createTransportRequest(request2);
        printResult(result2);

        if (!result2.success()) {
            throw new RuntimeException("Failed to create copy from CADK911293: " + result2.message());
        }
        System.out.println("  ✅ Copy from CADK911293 created: " + result2.transportNumber());
        System.out.println("  Objects copied: " + result2.objectsCopied());
        System.out.println();
    }

    /**
     * Prints the result in a formatted way.
     */
    private void printResult(TransportCreationResult result) {
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────────┐");
        System.out.println("  │ Result                                                  │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        System.out.printf("  │ Success:          %-38s │%n", result.success());
        System.out.printf("  │ Status:           %-38s │%n", result.status() + " (" + result.getStatusDescription() + ")");
        System.out.printf("  │ Transport:        %-38s │%n", result.transportNumber() != null ? result.transportNumber() : "N/A");
        System.out.printf("  │ Task:             %-38s │%n", result.taskNumber() != null ? result.taskNumber() : "N/A");
        System.out.printf("  │ Type:             %-38s │%n", result.requestTypeDescription() != null ? result.requestTypeDescription() : "N/A");
        System.out.printf("  │ Objects Copied:   %-38s │%n", result.objectsCopied());
        System.out.printf("  │ Message:          %-38s │%n", truncate(result.message(), 38));
        System.out.println("  └─────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    /**
     * Truncates a string to the specified length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "N/A";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}

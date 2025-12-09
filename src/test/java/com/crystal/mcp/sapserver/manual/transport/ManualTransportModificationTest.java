package com.crystal.mcp.sapserver.manual.transport;

import com.crystal.mcp.sapserver.model.TransportModificationResult;
import com.crystal.mcp.sapserver.service.TransportModificationService;
import com.crystal.mcp.sapserver.service.TransportModificationService.TransportObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Manual test for TransportModificationService.
 *
 * <p>This test performs modification operations on transport requests in SAP.
 * Run it using:
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportModificationTest
 * </pre>
 *
 * <p><b>WARNING:</b> This test modifies real transports in SAP. Use with caution
 * and ensure you have the proper authorizations.
 *
 * <p><b>Tests Available:</b>
 * <ul>
 *   <li>Test 1: Function Module Availability</li>
 *   <li>Test 2: Add Objects to Task</li>
 *   <li>Test 3: Modify Transport Description</li>
 *   <li>Test 4: Release Task</li>
 *   <li>Test 5: Get Tasks for Release (Confirmation)</li>
 *   <li>Test 6: Release Transport with All Tasks</li>
 *   <li>Test 7: Force Add Objects (bypass lock validation)</li>
 * </ul>
 *
 * @author Crystal Development Team
 * @since 2025-12-05
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualTransportModificationTest implements CommandLineRunner {

    @Autowired
    private TransportModificationService transportModificationService;

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CONFIGURATION - All tests use OT CADK911430
    // ═══════════════════════════════════════════════════════════════════════════

    // Main transport number (OT) - used for all operations
    // FM auto-detects if it's OT or Task via IV_TRKORR parameter
    private static final String TEST_OT = "CADK911430";

    // Task number to release (child of TEST_OT)
    private static final String TEST_TASK_TO_RELEASE = "CADK911148";

    // New description for modification test
    private static final String TEST_NEW_DESCRIPTION = "PSR013: Modification - " + System.currentTimeMillis();

    // Object to add (class - type auto-detected from TADIR)
    private static final List<TransportObject> TEST_OBJECTS = List.of(
        TransportObject.withName("zclpsr011_cert_compatibilidad")
    );

    // Object for force-add test (use an object that may be locked in another transport)
    private static final List<TransportObject> TEST_FORCE_OBJECTS = List.of(
        TransportObject.withName("zclpsr011_cert_compatibilidad")
    );

    // ═══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualTransportModificationTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║      Manual Test: TransportModificationService               ║");
        System.out.println("║                                                              ║");
        System.out.println("║  This test modifies real transports in SAP.                  ║");
        System.out.println("║  Ensure you have proper authorizations.                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  - Main OT:          " + TEST_OT);
        System.out.println("  - Task to release:  " + TEST_TASK_TO_RELEASE);
        System.out.println("  - Object to add:    ZCLPSR013_CERT_COMPATIBILIDAD");
        System.out.println();

        try {
            // Test 1: Check FM availability
            testFunctionModuleAvailability();

            // Test 2: Modify transport description
            //testModifyDescription();

            // Test 3: Add objects to transport (OT)
            //testAddObjectsToTransport();

            // Test 4: Release single task
            //testReleaseTask();

            // Test 5: Get tasks for release (confirmation flow)
            // UNCOMMENT TO RUN: testGetTasksForRelease();

            // Test 6: Release transport with all tasks
            // UNCOMMENT TO RUN: testReleaseTransport();

            // Test 7: Force add objects (bypass lock validation)
            //testForceAddObjectsToTransport();

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

        boolean available = transportModificationService.isFunctionModuleAvailable();

        if (available) {
            System.out.println("  ✅ ZCX_MODIFY_TRANSPORT_REQUEST is available");
        } else {
            System.out.println("  ⚠️  ZCX_MODIFY_TRANSPORT_REQUEST not found in SAP system");
            System.out.println("     The function module needs to be created in SAP.");
            System.out.println("     See docs/abap/ for the FM signature.");
        }
        System.out.println();
    }

    /**
     * Test 3: Add objects to a transport (OT).
     *
     * <p>Uses IV_TRKORR parameter - FM auto-detects if it's OT or Task.
     * If OT is passed, TR_REQUEST_CHOICE assigns objects to appropriate task.
     */
    private void testAddObjectsToTransport() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 3: Add Objects to Transport (OT)                       │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  OT: " + TEST_OT);
        System.out.println("  Objects: ");
        for (TransportObject obj : TEST_OBJECTS) {
            System.out.println("    - " + obj.objName());
        }

        TransportModificationResult result = transportModificationService.addObjectsToTransport(
            TEST_OT, TEST_OBJECTS);

        printResult(result);

        if (!result.success()) {
            throw new RuntimeException("Failed to add objects: " + result.message());
        }

        System.out.println("  ✅ Objects added successfully");
        System.out.println("  Objects added: " + result.objectsAdded());
        System.out.println("  Task assigned: " + result.taskNumber());
        System.out.println();
    }

    /**
     * Test 2: Modify transport description.
     */
    private void testModifyDescription() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 2: Modify Transport Description                        │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  OT:              " + TEST_OT);
        System.out.println("  New description: " + TEST_NEW_DESCRIPTION);

        TransportModificationResult result = transportModificationService.modifyDescription(
            TEST_OT, TEST_NEW_DESCRIPTION);

        printResult(result);

        if (!result.success()) {
            throw new RuntimeException("Failed to modify description: " + result.message());
        }

        System.out.println("  ✅ Description modified successfully");
        System.out.println();
    }

    /**
     * Test 4: Release a single task.
     */
    private void testReleaseTask() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 4: Release Task                                        │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  Task: " + TEST_TASK_TO_RELEASE);

        TransportModificationResult result = transportModificationService.releaseTask(
            TEST_TASK_TO_RELEASE);

        printResult(result);

        if (!result.success()) {
            throw new RuntimeException("Failed to release task: " + result.message());
        }

        System.out.println("  ✅ Task released successfully");
        System.out.println();
    }

    /**
     * Test 5: Get tasks for release (confirmation flow).
     */
    private void testGetTasksForRelease() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 5: Get Tasks for Release (Confirmation)                │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  OT: " + TEST_OT);

        TransportModificationResult result = transportModificationService.getTasksForRelease(TEST_OT);

        printResult(result);

        if (result.requiresConfirmation()) {
            System.out.println("  ✅ Confirmation required:");
            System.out.println("     " + result.confirmationMessage());
            System.out.println("     Tasks to release: " + result.tasksReleased());
        } else {
            System.out.println("  ⚠️  No confirmation needed (no modifiable tasks)");
        }
        System.out.println();
    }

    /**
     * Test 7: Force add objects to a transport, bypassing lock validation.
     *
     * <p>This test demonstrates the force-add functionality that uses TRINT_APPEND_COMM
     * directly instead of TR_APPEND_TO_COMM_OBJS_KEYS. This allows adding objects even
     * when they are locked in another transport.
     *
     * <p><b>Use Case:</b> When you need to add an object to a transport but it's already
     * locked in another transport (e.g., creating a backup copy or parallel development).
     *
     * <p><b>WARNING:</b> After using force-add, the same object may exist in multiple
     * transports. This should be used carefully and with full awareness of the implications.
     */
    private void testForceAddObjectsToTransport() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 7: Force Add Objects (Bypass Lock Validation)          │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  OT: " + TEST_OT);
        System.out.println("  Objects (force-add): ");
        for (TransportObject obj : TEST_FORCE_OBJECTS) {
            System.out.println("    - " + obj.objName() + " (will bypass lock check)");
        }
        System.out.println();
        System.out.println("  ⚠️  WARNING: This bypasses SAP's lock validation!");
        System.out.println("     Objects will be added even if locked in another transport.");
        System.out.println();

        TransportModificationResult result = transportModificationService.forceAddObjectsToTransport(
            TEST_OT, TEST_FORCE_OBJECTS);

        printResult(result);

        if (!result.success()) {
            throw new RuntimeException("Failed to force-add objects: " + result.message());
        }

        System.out.println("  ✅ Objects force-added successfully");
        System.out.println("  Objects added: " + result.objectsAdded());
        System.out.println("  Task assigned: " + result.taskNumber());
        System.out.println();
    }

    /**
     * Test 6: Release transport with all tasks.
     */
    private void testReleaseTransport() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 6: Release Transport with All Tasks                    │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  OT: " + TEST_OT);
        System.out.println();

        // Step 1: Get confirmation info
        System.out.println("  Step 1: Getting tasks to release...");
        TransportModificationResult confirmResult = transportModificationService.releaseTransport(
            TEST_OT, false);

        if (confirmResult.requiresConfirmation()) {
            System.out.println("  Confirmation: " + confirmResult.confirmationMessage());
            System.out.println();

            // Step 2: Proceed with release (confirmed)
            System.out.println("  Step 2: Releasing transport (confirmed)...");
            TransportModificationResult releaseResult = transportModificationService.releaseTransport(
                TEST_OT, true);

            printResult(releaseResult);

            if (!releaseResult.success()) {
                throw new RuntimeException("Failed to release transport: " + releaseResult.message());
            }

            System.out.println("  ✅ Transport released successfully");
            System.out.println("  Tasks released: " + releaseResult.tasksReleased());
        } else {
            System.out.println("  ⚠️  No tasks to release or transport already released");
            printResult(confirmResult);
        }
        System.out.println();
    }

    /**
     * Prints the result in a formatted way.
     */
    private void printResult(TransportModificationResult result) {
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────────┐");
        System.out.println("  │ Result                                                  │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        System.out.printf("  │ Success:          %-38s │%n", result.success());
        System.out.printf("  │ Status:           %-38s │%n", result.status() + " (" + result.getStatusDescription() + ")");
        System.out.printf("  │ Operation:        %-38s │%n", result.getOperationDescription());
        System.out.printf("  │ Transport:        %-38s │%n", result.transportNumber() != null ? result.transportNumber() : "N/A");
        System.out.printf("  │ Task:             %-38s │%n", result.taskNumber() != null ? result.taskNumber() : "N/A");
        System.out.printf("  │ Objects Added:    %-38s │%n", result.objectsAdded());
        System.out.printf("  │ Requires Confirm: %-38s │%n", result.requiresConfirmation());
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

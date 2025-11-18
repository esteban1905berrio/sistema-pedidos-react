package com.crystal.mcp.sapserver.manual.transport;

import com.crystal.mcp.sapserver.model.TransportObjectsResult;
import com.crystal.mcp.sapserver.service.TransportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual test for get_transport_objects functionality.
 *
 * This test requires a live SAP connection (GDC system).
 * Run this test manually to verify the implementation.
 *
 * Test Cases:
 * 1. Get objects from a main transport (with tasks)
 * 2. Get objects from a specific task
 * 3. Filter objects by task number
 * 4. Handle non-existent transport
 *
 * How to Run:
 * 1. Ensure SAP connection is configured (check application.yml or env vars)
 * 2. Run: mvn test -Dtest=ManualTransportObjectsTest
 * 3. Or run from IDE (right-click -> Run Test)
 */
@SpringBootTest(classes = com.crystal.mcp.sapserver.SapMcpServerApplication.class)
class ManualTransportObjectsTest {

    @Autowired
    private TransportService transportService;

    /**
     * Test Case 1: Get objects from a main transport with tasks.
     *
     * Expected Result:
     * - success = true
     * - transportNumber set correctly
     * - metadata contains all E070 fields
     * - objects list contains all objects from main transport + tasks
     * - tasks list contains all subtasks (TRFUNCTION = 'S')
     */
    @Test
    void testGetTransportObjects_MainTransport() {
        // ARRANGE
        String transportNumber = "CADK911293"; // DV-MM-I1229 (Workbench)

        System.out.println("\n=== TEST: Get Objects from Main Transport ===");
        System.out.println("Transport Number: " + transportNumber);
        System.out.println("Expected: Main transport with tasks and objects\n");

        // ACT
        TransportObjectsResult result = transportService.getTransportObjects(
                transportNumber,
                null  // No task filter
        );

        // ASSERT
        assertNotNull(result, "Result should not be null");

        // Print result for debugging
        System.out.println("Success: " + result.success());
        System.out.println("Transport Number: " + result.transportNumber());
        System.out.println("Metadata: " + result.metadata());

        // If failed, show error
        if (!result.success()) {
            System.err.println("❌ ERROR: Query failed!");
            System.err.println("Error message: " + result.metadata().get("error"));
            System.err.println("Full metadata: " + result.metadata());
        }

        assertTrue(result.success(), "Query should succeed");
        assertEquals(transportNumber, result.transportNumber(), "Transport number should match");

        // Verify metadata
        assertNotNull(result.metadata(), "Metadata should not be null");
        assertFalse(result.metadata().isEmpty(), "Metadata should contain values");
        System.out.println("Metadata: " + result.metadata());

        // Verify objects
        assertNotNull(result.objects(), "Objects list should not be null");
        System.out.println("Total Objects: " + result.totalObjects());
        System.out.println("Objects found: " + result.objects().size());

        // Verify tasks
        assertNotNull(result.tasks(), "Tasks list should not be null");
        System.out.println("Tasks found: " + result.tasks().size());

        // Print details
        if (!result.tasks().isEmpty()) {
            System.out.println("\n--- Tasks ---");
            for (TransportObjectsResult.Task task : result.tasks()) {
                System.out.printf("  Task: %s | Owner: %s | Objects: %d | Status: %s%n",
                        task.taskNumber(),
                        task.owner(),
                        task.objectCount(),
                        task.statusDesc()
                );
            }
        }

        if (!result.objects().isEmpty()) {
            System.out.println("\n--- Sample Objects (first 5) ---");
            result.objects().stream().limit(5).forEach(obj -> {
                System.out.printf("  %s | %s | %s | TRKORR: %s%n",
                        obj.objectType(),
                        obj.objectName(),
                        obj.pgmid(),
                        obj.trkorr()
                );
            });
        }

        System.out.println("\n✅ TEST PASSED: Main Transport\n");
    }

    /**
     * Test Case 2: Get objects from a specific task.
     *
     * Expected Result:
     * - success = true
     * - transportNumber is the task number
     * - metadata shows TRFUNCTION = 'S'
     * - objects list contains only objects from this task
     * - tasks list is empty (tasks don't have sub-tasks)
     */
    @Test
    void testGetTransportObjects_Task() {
        // ARRANGE
        String taskNumber = "S4DK932807"; // Task from S4DK932806

        System.out.println("\n=== TEST: Get Objects from Task ===");
        System.out.println("Task Number: " + taskNumber);
        System.out.println("Expected: Task objects only, no sub-tasks\n");

        // ACT
        TransportObjectsResult result = transportService.getTransportObjects(
                taskNumber,
                null  // No task filter
        );

        // ASSERT
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertEquals(taskNumber, result.transportNumber(), "Task number should match");

        // Verify metadata
        assertNotNull(result.metadata(), "Metadata should not be null");
        assertEquals("S", result.metadata().get("transport_type"), "Type should be 'S' (Task)");
        System.out.println("Metadata: " + result.metadata());

        // Verify objects
        assertNotNull(result.objects(), "Objects list should not be null");
        System.out.println("Total Objects: " + result.totalObjects());

        // Verify no sub-tasks
        assertEquals(0, result.tasks().size(), "Tasks should have no sub-tasks");

        System.out.println("\n✅ TEST PASSED: Task\n");
    }

    /**
     * Test Case 3: Filter objects by task number.
     *
     * Expected Result:
     * - success = true
     * - objects list contains only objects from specified task
     * - totalObjects reflects filtered count
     */
    @Test
    void testGetTransportObjects_FilterByTask() {
        // ARRANGE
        String mainTransport = "S4DK932806";
        String filterTask = "S4DK932807";

        System.out.println("\n=== TEST: Filter Objects by Task ===");
        System.out.println("Main Transport: " + mainTransport);
        System.out.println("Filter Task: " + filterTask);
        System.out.println("Expected: Only objects from specified task\n");

        // ACT
        TransportObjectsResult result = transportService.getTransportObjects(
                mainTransport,
                filterTask  // Filter by specific task
        );

        // ASSERT
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");

        // Verify all objects belong to the filter task
        assertTrue(result.objects().stream()
                        .allMatch(obj -> filterTask.equals(obj.trkorr())),
                "All objects should belong to filter task"
        );

        System.out.println("Filtered Objects: " + result.totalObjects());
        System.out.println("All objects belong to task: " + filterTask);

        System.out.println("\n✅ TEST PASSED: Filter by Task\n");
    }

    /**
     * Test Case 4: Handle non-existent transport.
     *
     * Expected Result:
     * - success = false
     * - metadata contains error message
     * - objects and tasks are empty
     */
    @Test
    void testGetTransportObjects_NotFound() {
        // ARRANGE
        String nonExistentTransport = "XXXXK999999";

        System.out.println("\n=== TEST: Non-Existent Transport ===");
        System.out.println("Transport Number: " + nonExistentTransport);
        System.out.println("Expected: Failure with error message\n");

        // ACT
        TransportObjectsResult result = transportService.getTransportObjects(
                nonExistentTransport,
                null
        );

        // ASSERT
        assertNotNull(result, "Result should not be null");
        assertFalse(result.success(), "Query should fail");
        assertTrue(result.metadata().containsKey("error"), "Metadata should contain error");
        assertEquals(0, result.totalObjects(), "Should have no objects");
        assertEquals(0, result.tasks().size(), "Should have no tasks");

        System.out.println("Error: " + result.metadata().get("error"));

        System.out.println("\n✅ TEST PASSED: Not Found\n");
    }

    /**
     * Test Case 5: Test with Transport of Copies (Type T).
     *
     * Expected Result:
     * - success = true
     * - transport_type = 'T'
     * - objects list may be empty or contain copied objects
     */
    @Test
    void testGetTransportObjects_TransportOfCopies() {
        // ARRANGE
        String transportOfCopies = "S4DK931802"; // Transport of Copies

        System.out.println("\n=== TEST: Transport of Copies (Type T) ===");
        System.out.println("Transport Number: " + transportOfCopies);
        System.out.println("Expected: Type 'T' with copied objects\n");

        // ACT
        TransportObjectsResult result = transportService.getTransportObjects(
                transportOfCopies,
                null
        );

        // ASSERT
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertEquals("T", result.metadata().get("transport_type"), "Type should be 'T'");
        assertEquals("Transport of Copies", result.metadata().get("transport_type_desc"));

        System.out.println("Metadata: " + result.metadata());
        System.out.println("Total Objects: " + result.totalObjects());

        System.out.println("\n✅ TEST PASSED: Transport of Copies\n");
    }

    /**
     * Test Case 6: Comprehensive test with detailed output.
     *
     * This test prints complete information for manual verification.
     */
    @Test
    void testGetTransportObjects_Comprehensive() {
        // ARRANGE
        String transportNumber = "S4DK932806";

        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE TEST: get_transport_objects");
        System.out.println("=".repeat(80));
        System.out.println("Transport Number: " + transportNumber + "\n");

        // ACT
        TransportObjectsResult result = transportService.getTransportObjects(
                transportNumber,
                null
        );

        // PRINT RESULTS
        System.out.println("--- RESULT SUMMARY ---");
        System.out.println("Success: " + result.success());
        System.out.println("Transport Number: " + result.transportNumber());
        System.out.println("Total Objects: " + result.totalObjects());
        System.out.println("Total Tasks: " + result.tasks().size());

        System.out.println("\n--- METADATA ---");
        result.metadata().forEach((key, value) ->
                System.out.printf("  %-20s: %s%n", key, value)
        );

        System.out.println("\n--- TASKS ---");
        if (result.tasks().isEmpty()) {
            System.out.println("  (No tasks found)");
        } else {
            for (int i = 0; i < result.tasks().size(); i++) {
                TransportObjectsResult.Task task = result.tasks().get(i);
                System.out.printf("\n  Task %d:%n", i + 1);
                System.out.printf("    Number: %s%n", task.taskNumber());
                System.out.printf("    Owner: %s%n", task.owner());
                System.out.printf("    Created: %s %s%n", task.createdDate(), task.createdTime());
                System.out.printf("    Status: %s (%s)%n", task.statusDesc(), task.status());
                System.out.printf("    Description: %s%n", task.description());
                System.out.printf("    Object Count: %d%n", task.objectCount());
            }
        }

        System.out.println("\n--- OBJECTS (sample) ---");
        if (result.objects().isEmpty()) {
            System.out.println("  (No objects found)");
        } else {
            System.out.println("  Showing first 10 objects:");
            result.objects().stream().limit(10).forEach(obj -> {
                System.out.printf("    %-10s | %-40s | %-5s | %-12s%n",
                        obj.objectType(),
                        obj.objectName(),
                        obj.pgmid(),
                        obj.trkorr()
                );
            });
            if (result.objects().size() > 10) {
                System.out.printf("  ... and %d more objects%n", result.objects().size() - 10);
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST COMPLETED");
        System.out.println("=".repeat(80) + "\n");

        // ASSERT
        assertTrue(result.success(), "Comprehensive test should succeed");
    }
}

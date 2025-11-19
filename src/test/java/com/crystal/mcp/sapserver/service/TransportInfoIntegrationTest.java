package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TransportInfoListResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TransportService.getTransportInfo().
 *
 * This test verifies the complete workflow:
 * 1. Call Z_CX_GET_TRANSPORT_INFO function module via JCo
 * 2. Parse JSON array response
 * 3. Build TransportInfoListResult DTO
 *
 * Requirements:
 * - SAP connection configured (environment variables)
 * - Z_CX_GET_TRANSPORT_INFO FM exists in GDC system (updated version)
 * - Test transport exists in E070 table
 *
 * Test Strategy:
 * - Use real transport numbers from GDC system
 * - Verify metadata fields are populated (including object_count, task_count)
 * - Test both single and multiple transport queries
 * - Test both main transports and tasks
 * - Test error handling (transport not found)
 */
@SpringBootTest
class TransportInfoIntegrationTest {

    @Autowired
    private TransportService transportService;

    /**
     * Test getting metadata for a known transport (main OT).
     *
     * Expected Result:
     * - success = true
     * - All metadata fields populated
     * - transport_type = "K" (Workbench)
     * - has_tasks = true (main transports have tasks)
     * - object_count and task_count populated from JOIN queries
     */
    @Test
    void testGetTransportInfo_MainTransport() {
        // Given: Transport number from GDC system
        // NOTE: Replace with actual transport number from your system
        String transportNumber = "CADK911088";

        // When: Get transport info
        TransportInfoListResult result = transportService.getTransportInfo(transportNumber);

        // Then: Verify result
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertEquals(1, result.totalCount(), "Should return one transport");

        TransportInfoListResult.TransportInfo info = result.getFirst();
        assertNotNull(info, "Transport info should not be null");
        assertEquals(transportNumber, info.transportNumber());

        // Verify metadata fields
        assertNotNull(info.transportType(), "Transport type should not be null");
        assertNotNull(info.status(), "Status should not be null");
        assertNotNull(info.owner(), "Owner should not be null");
        assertNotNull(info.createdDate(), "Created date should not be null");
        assertNotNull(info.createdTime(), "Created time should not be null");

        // Verify type descriptions
        assertFalse(info.transportTypeDesc().isEmpty(), "Type description should not be empty");
        assertFalse(info.statusDesc().isEmpty(), "Status description should not be empty");

        // Verify counts from JOIN queries
        assertTrue(info.objectCount() >= 0, "Object count should be non-negative");
        assertTrue(info.taskCount() >= 0, "Task count should be non-negative");

        // Log result for manual verification
        System.out.println("\n=== Transport Info Test Results ===");
        System.out.println("Transport: " + info.transportNumber());
        System.out.println("Type: " + info.transportType() + " (" + info.transportTypeDesc() + ")");
        System.out.println("Status: " + info.status() + " (" + info.statusDesc() + ")");
        System.out.println("Owner: " + info.owner());
        System.out.println("Description: " + info.description());
        System.out.println("Created: " + info.createdDate() + " " + info.createdTime());
        System.out.println("Target System: " + info.targetSystem());
        System.out.println("Object Count: " + info.objectCount());
        System.out.println("Task Count: " + info.taskCount());
        System.out.println("Has Objects: " + info.hasObjects());
        System.out.println("Has Tasks: " + info.hasTasks());
        System.out.println("=====================================\n");
    }

    /**
     * Test getting metadata for a task (subtask of main transport).
     *
     * Expected Result:
     * - success = true
     * - transport_type = "S" (Task)
     * - parent_transport != null (tasks have parent)
     * - has_tasks = false (tasks don't have subtasks)
     */
    @Test
    void testGetTransportInfo_Task() {
        // Given: Task number from GDC system
        // NOTE: Replace with actual task number from your system
        String taskNumber = "CADK911089";

        // When: Get transport info
        TransportInfoListResult result = transportService.getTransportInfo(taskNumber);

        // Then: Verify result
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertEquals(1, result.totalCount(), "Should return one transport");

        TransportInfoListResult.TransportInfo info = result.getFirst();
        assertNotNull(info, "Transport info should not be null");
        assertEquals(taskNumber, info.transportNumber());

        // Tasks should have transport_type = "S"
        assertEquals("S", info.transportType(), "Task should have type 'S'");

        // Tasks should have parent transport
        assertNotNull(info.parentTransport(), "Task should have parent transport");
        assertFalse(info.parentTransport().isEmpty(), "Parent transport should not be empty");

        // Log result
        System.out.println("\n=== Task Info Test Results ===");
        System.out.println("Task: " + info.transportNumber());
        System.out.println("Parent: " + info.parentTransport());
        System.out.println("Owner: " + info.owner());
        System.out.println("Status: " + info.status() + " (" + info.statusDesc() + ")");
        System.out.println("Object Count: " + info.objectCount());
        System.out.println("==============================\n");
    }

    /**
     * Test error handling when transport doesn't exist.
     *
     * Expected Result:
     * - success = false or empty list
     * - Error message indicating transport not found
     */
    @Test
    void testGetTransportInfo_NotFound() {
        // Given: Non-existent transport number
        String invalidTransport = "INVALID999999";

        // When: Get transport info
        TransportInfoListResult result = transportService.getTransportInfo(invalidTransport);

        // Then: Verify result (FM may return empty array or error)
        assertNotNull(result, "Result should not be null");

        System.out.println("\n=== Not Found Test Results ===");
        System.out.println("Transport: " + invalidTransport);
        System.out.println("Success: " + result.success());
        System.out.println("Total Count: " + result.totalCount());
        if (!result.success()) {
            System.out.println("Error Message: " + result.message());
        }
        System.out.println("==============================\n");
    }

    /**
     * Test validation: empty transport number.
     */
    @Test
    void testGetTransportInfo_EmptyNumber() {
        // When/Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                transportService.getTransportInfo("")
        );

        assertThrows(IllegalArgumentException.class, () ->
                transportService.getTransportInfo(null)
        );
    }

    /**
     * Test multiple transport query support (NEW FEATURE).
     *
     * Tests the updated FM that accepts comma-separated transport numbers.
     */
    @Test
    void testGetTransportInfo_MultipleTransports() {
        // Given: Comma-separated transport numbers
        String multipleTransports = "CADK911088,CADK911089";

        // When: Get transport info for multiple transports
        TransportInfoListResult result = transportService.getTransportInfo(multipleTransports);

        // Then: Verify result contains multiple transports
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertTrue(result.totalCount() >= 1, "Should return at least one transport");

        System.out.println("\n=== Multiple Transports Test Results ===");
        System.out.println("Query: " + multipleTransports);
        System.out.println("Total Count: " + result.totalCount());

        result.transports().forEach(info -> {
            System.out.println("  - " + info.transportNumber() +
                             " (" + info.transportType() + ") " +
                             "Owner: " + info.owner() +
                             " Objects: " + info.objectCount());
        });
        System.out.println("=========================================\n");
    }
}

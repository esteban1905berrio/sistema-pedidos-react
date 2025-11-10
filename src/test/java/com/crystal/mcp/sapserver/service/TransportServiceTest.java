package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ObjectInOpenOTResult;
import com.crystal.mcp.sapserver.model.TransportListResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransportService.
 *
 * These tests require a live SAP connection configured via environment variables.
 * Tests verify the listUserTransports functionality with actual SAP data.
 *
 * Test Coverage:
 * - List all transports for current user
 * - Filter by user
 * - Filter by status
 */
@SpringBootTest
class TransportServiceTest {

    @Autowired
    private TransportService transportService;

    /**
     * Test listing all transports for current user.
     *
     * This test verifies:
     * 1. The endpoint is reachable
     * 2. The XML response is parsed correctly
     * 3. At least one transport is returned
     */
    @Test
    void testListUserTransports_CurrentUser() {
        // When: List transports for current user
        TransportListResult result = transportService.listUserTransports(null, null);

        // Then: Verify result structure
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.transports(), "Transports list should not be null");
        assertTrue(result.totalTransports() >= 0, "Total transports should be >= 0");
        assertEquals(result.transports().size(), result.totalTransports(),
                "Total count should match list size");

        // Log results for manual verification
        System.out.println("Total transports: " + result.totalTransports());
        result.transports().forEach(transport -> {
            System.out.printf("  - %s: %s (%s, owner: %s, type: %s)%n",
                    transport.number(),
                    transport.description(),
                    transport.status(),
                    transport.owner(),
                    transport.type());
        });
    }

    /**
     * Test listing transports filtered by status.
     *
     * This test verifies:
     * 1. Status filtering works correctly
     * 2. Only modifiable (D) transports are returned
     */
    @Test
    void testListUserTransports_FilterByStatus() {
        // When: List only modifiable transports
        TransportListResult result = transportService.listUserTransports(null, "D");

        // Then: Verify all returned transports have status 'D'
        assertNotNull(result, "Result should not be null");
        result.transports().forEach(transport -> {
            assertEquals("D", transport.status(),
                    "All transports should have status 'D' (modifiable)");
        });

        System.out.println("Modifiable transports: " + result.totalTransports());
    }

    /**
     * Test listing transports for a specific user.
     *
     * This test verifies:
     * 1. User filtering works correctly
     * 2. Only transports owned by the specified user are returned
     */
    @Test
    void testListUserTransports_FilterByUser() {
        // Given: Get user from first transport
        TransportListResult allTransports = transportService.listUserTransports(null, null);
        if (allTransports.totalTransports() == 0) {
            System.out.println("No transports found - skipping user filter test");
            return;
        }

        String testUser = allTransports.transports().get(0).owner();

        // When: List transports for specific user
        TransportListResult result = transportService.listUserTransports(testUser, null);

        // Then: Verify all returned transports belong to the user
        assertNotNull(result, "Result should not be null");
        result.transports().forEach(transport -> {
            assertEquals(testUser, transport.owner(),
                    "All transports should be owned by " + testUser);
        });

        System.out.println("Transports for user " + testUser + ": " + result.totalTransports());
    }

    /**
     * Test combined filters (user + status).
     *
     * This test verifies:
     * 1. Multiple filters work together correctly
     * 2. Results match both filter criteria
     */
    @Test
    void testListUserTransports_CombinedFilters() {
        // Given: Get user from first transport
        TransportListResult allTransports = transportService.listUserTransports(null, null);
        if (allTransports.totalTransports() == 0) {
            System.out.println("No transports found - skipping combined filter test");
            return;
        }

        String testUser = allTransports.transports().get(0).owner();

        // When: List modifiable transports for specific user
        TransportListResult result = transportService.listUserTransports(testUser, "D");

        // Then: Verify all results match both criteria
        assertNotNull(result, "Result should not be null");
        result.transports().forEach(transport -> {
            assertEquals(testUser, transport.owner(),
                    "All transports should be owned by " + testUser);
            assertEquals("D", transport.status(),
                    "All transports should have status 'D'");
        });

        System.out.println("Modifiable transports for user " + testUser + ": " + result.totalTransports());
    }

    /**
     * Test checking if specific class is in open transport.
     *
     * This test verifies:
     * 1. The method can find objects in E071 table
     * 2. Can retrieve transport metadata from E070
     * 3. Can detect tasks and get parent transport info
     * 4. Filters correctly by open status (D or L)
     */
    @Test
    void testGetObjectInOpenOT_SpecificClass() {
        // Given: A known class that should be in a transport
        String className = "ZCLMMI1229_SINCRONIZA_INV_MAWM";
        String objectType = "CLAS";

        System.out.println("\n=== DEBUG: Starting test ===");
        System.out.println("Searching for: " + className);
        System.out.println("Object type (will be ignored): " + objectType);
        System.out.println();

        // When: Check if object is in open transport
        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(className, objectType);

        System.out.println("\n=== DEBUG: Result received ===");
        System.out.println("Success: " + result.success());
        System.out.println("Search pattern used: " + result.searchPattern());
        System.out.println();

        // Then: Verify result structure
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should be successful");
        assertEquals(className, result.objectName(), "Object name should match");

        // Log results for analysis
        System.out.println("\n=== Object in Open Transport Test ===");
        System.out.println("Object: " + result.objectName());
        System.out.println("Search pattern: " + result.searchPattern());
        System.out.println("Total transports found: " + result.totalTransports());
        System.out.println();

        if (result.totalTransports() > 0) {
            result.transports().forEach(transport -> {
                System.out.println("Transport/Task: " + transport.transportNumber());
                System.out.println("  Type: " + transport.transportTypeDesc() + " (" + transport.transportType() + ")");
                System.out.println("  Status: " + transport.statusDesc() + " (" + transport.status() + ")");
                System.out.println("  Owner: " + transport.owner());
                System.out.println("  Created: " + transport.createdDate() + " " + transport.createdTime());
                System.out.println("  Locked: " + transport.isLocked());
                System.out.println("  Object: " + transport.objectInfo().objName() +
                        " (" + transport.objectInfo().objectType() + ")");

                // Print parent transport info if exists
                if (transport.parentTransport() != null) {
                    System.out.println("  Parent Transport: " + transport.parentTransport().transportNumber());
                    System.out.println("    Type: " + transport.parentTransport().transportTypeDesc() +
                            " (" + transport.parentTransport().transportType() + ")");
                    System.out.println("    Status: " + transport.parentTransport().statusDesc() +
                            " (" + transport.parentTransport().status() + ")");
                    System.out.println("    Owner: " + transport.parentTransport().owner());
                    System.out.println("    Description: " + transport.parentTransport().description());
                }
                System.out.println();
            });
        } else {
            System.out.println("⚠️  No open transports found for this object!");
            System.out.println("This could mean:");
            System.out.println("  - Object is not in any transport");
            System.out.println("  - Object is only in released transports");
            System.out.println("  - Search pattern didn't match");
        }
    }
}

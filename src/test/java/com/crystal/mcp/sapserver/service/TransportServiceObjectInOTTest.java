package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ObjectInOpenOTResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransportService.getObjectInOpenOT() method.
 *
 * These tests require a live SAP connection configured via environment variables.
 * Tests query real SAP tables (E071, E070) to validate functionality.
 *
 * Prerequisites:
 * - SAP connection configured in .mcp.json or environment
 * - ADT installed on SAP system
 * - User has authorization to query transport tables
 * - Test data: Ideally some objects in open transports (TRSTATUS = 'D' or 'L')
 *
 * Test Strategy:
 * - Test finding objects in open transports
 * - Test object type filtering
 * - Test locked object detection
 * - Test no results scenario
 * - Test error handling
 */
@SpringBootTest
class TransportServiceObjectInOTTest {

    @Autowired
    private TransportService transportService;

    @Test
    void testGetObjectInOpenOT_found() {
        // Test: Search for objects containing "Z" in name
        // Most SAP systems have custom Z* objects in development

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "Z",  // Search for objects with "Z" in name
                null  // All types
        );

        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should be successful");
        assertEquals("Z", result.objectName());
        assertTrue(result.searchPattern().contains("Z"), "Search pattern should contain Z");

        System.out.println("✅ Object search successful:");
        System.out.println("   - Found " + result.totalTransports() + " open transports");

        if (result.totalTransports() > 0) {
            var firstTransport = result.transports().get(0);
            System.out.println("   - Example: " + firstTransport.transportNumber() +
                    " (" + firstTransport.statusDesc() + ", owner: " + firstTransport.owner() + ")");
            System.out.println("   - Object: " + firstTransport.objectInfo().objName() +
                    " (type: " + firstTransport.objectInfo().objectType() + ")");
            System.out.println("   - Locked: " + firstTransport.isLocked());

            // Verify structure
            assertNotNull(firstTransport.transportNumber());
            assertNotNull(firstTransport.transportType());
            assertNotNull(firstTransport.status());
            assertTrue(firstTransport.status().equals("D") || firstTransport.status().equals("L"),
                    "Status should be D (Modifiable) or L (Protected)");
            assertNotNull(firstTransport.objectInfo());
            assertNotNull(firstTransport.objectInfo().objName());
        }
    }

    @Test
    void testGetObjectInOpenOT_withTypeFilter_CLAS() {
        // Test: Search for classes only

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "CL",    // Common class prefix
                "CLAS"  // Filter by CLAS type
        );

        assertNotNull(result);
        assertTrue(result.success());

        System.out.println("✅ CLAS type filter test successful:");
        System.out.println("   - Found " + result.totalTransports() + " open transports with classes");

        // Verify all results are CLAS type
        for (var transport : result.transports()) {
            assertEquals("CLAS", transport.objectInfo().objectType(),
                    "All objects should be CLAS type");
        }
    }

    @Test
    void testGetObjectInOpenOT_withTypeFilter_PROG() {
        // Test: Search for programs only

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "Z",     // Custom objects
                "PROG"  // Filter by PROG type
        );

        assertNotNull(result);
        assertTrue(result.success());

        System.out.println("✅ PROG type filter test successful:");
        System.out.println("   - Found " + result.totalTransports() + " open transports with programs");

        // Verify all results are PROG type
        for (var transport : result.transports()) {
            assertEquals("PROG", transport.objectInfo().objectType(),
                    "All objects should be PROG type");
        }
    }

    @Test
    void testGetObjectInOpenOT_notFound() {
        // Test: Search for non-existent object

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "NONEXISTENT_OBJECT_XYZ123",
                null
        );

        assertNotNull(result);
        assertTrue(result.success(), "Query should be successful even if no results");
        assertEquals(0, result.totalTransports(), "Should find no transports");
        assertTrue(result.transports().isEmpty(), "Transport list should be empty");

        System.out.println("✅ Not found test successful: 0 transports as expected");
    }

    @Test
    void testGetObjectInOpenOT_checkLockedStatus() {
        // Test: Verify locked status detection
        // This test checks if LOCKFLAG = 'X' is properly detected

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "Z",
                null
        );

        assertNotNull(result);
        assertTrue(result.success());

        System.out.println("✅ Locked status check:");

        long lockedCount = result.transports().stream()
                .filter(ObjectInOpenOTResult.TransportInfo::isLocked)
                .count();

        System.out.println("   - Total transports: " + result.totalTransports());
        System.out.println("   - Locked objects: " + lockedCount);
        System.out.println("   - Unlocked objects: " + (result.totalTransports() - lockedCount));

        // Just verify the structure is correct, don't assert specific counts
        // (depends on SAP system state)
        for (var transport : result.transports()) {
            assertNotNull(transport.isLocked(), "isLocked should not be null");
        }
    }

    @Test
    void testGetObjectInOpenOT_verifyMetadata() {
        // Test: Verify transport metadata structure

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "Z",
                null
        );

        assertNotNull(result);

        if (result.totalTransports() > 0) {
            var transport = result.transports().get(0);

            System.out.println("✅ Metadata verification:");
            System.out.println("   - Transport: " + transport.transportNumber());
            System.out.println("   - Type: " + transport.transportTypeDesc() + " (" + transport.transportType() + ")");
            System.out.println("   - Status: " + transport.statusDesc() + " (" + transport.status() + ")");
            System.out.println("   - Owner: " + transport.owner());
            System.out.println("   - Created: " + transport.createdDate() + " " + transport.createdTime());

            // Verify all required fields are present
            assertNotNull(transport.transportNumber(), "Transport number should not be null");
            assertNotNull(transport.transportType(), "Transport type should not be null");
            assertNotNull(transport.transportTypeDesc(), "Transport type desc should not be null");
            assertNotNull(transport.status(), "Status should not be null");
            assertNotNull(transport.statusDesc(), "Status desc should not be null");
            assertNotNull(transport.owner(), "Owner should not be null");
            assertNotNull(transport.createdDate(), "Created date should not be null");
            assertNotNull(transport.createdTime(), "Created time should not be null");

            // Verify date format (YYYY-MM-DD)
            assertTrue(transport.createdDate().matches("\\d{4}-\\d{2}-\\d{2}"),
                    "Date should be in YYYY-MM-DD format");

            // Verify time format (HH:MM:SS)
            assertTrue(transport.createdTime().matches("\\d{2}:\\d{2}:\\d{2}"),
                    "Time should be in HH:MM:SS format");
        }
    }

    @Test
    void testGetObjectInOpenOT_emptyObjectName() {
        // Test: Error handling for empty object name

        assertThrows(IllegalArgumentException.class, () -> {
            transportService.getObjectInOpenOT("", null);
        }, "Should throw IllegalArgumentException for empty object name");

        assertThrows(IllegalArgumentException.class, () -> {
            transportService.getObjectInOpenOT("   ", null);
        }, "Should throw IllegalArgumentException for whitespace-only object name");

        assertThrows(IllegalArgumentException.class, () -> {
            transportService.getObjectInOpenOT(null, null);
        }, "Should throw IllegalArgumentException for null object name");

        System.out.println("✅ Empty/null object name error handling successful");
    }

    @Test
    void testGetObjectInOpenOT_multipleResults() {
        // Test: Verify handling of objects in multiple transports

        ObjectInOpenOTResult result = transportService.getObjectInOpenOT(
                "Z",  // Common prefix, likely multiple results
                null
        );

        assertNotNull(result);
        assertTrue(result.success());

        System.out.println("✅ Multiple results test:");
        System.out.println("   - Total transports found: " + result.totalTransports());

        if (result.totalTransports() > 1) {
            System.out.println("   - Verified: Multiple transports can be returned");

            // Verify each transport is unique
            long uniqueTransports = result.transports().stream()
                    .map(ObjectInOpenOTResult.TransportInfo::transportNumber)
                    .distinct()
                    .count();

            assertEquals(result.totalTransports(), uniqueTransports,
                    "All transports should be unique");
        }
    }
}

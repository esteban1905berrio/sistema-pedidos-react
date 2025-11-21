package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TransportInfoListResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test for Z_CX_GET_TRANSPORT_INFO FM.
 *
 * Purpose: Verify the FM is correctly filtering by transport numbers
 * instead of returning ALL transports from E070 table.
 *
 * Expected Behavior:
 * - FM should split comma-separated input: "CADK911088,CADK911089"
 * - FM should use WHERE clause: WHERE h~trkorr IN @lt_transport_numbers
 * - FM should return ONLY the requested transports (not all 4831 from E070)
 *
 * Observed Problem:
 * - Test expected 1 transport, received 4831
 * - Queries taking 50+ seconds (full table scan)
 * - Suggests WHERE clause not filtering or SPLIT not working
 */
@SpringBootTest
class TransportInfoDiagnosticTest {

    @Autowired
    private TransportService transportService;

    /**
     * Simple diagnostic: Query a single known transport.
     * Should return exactly 1 result in < 5 seconds.
     */
    @Test
    void testSingleTransport_ExpectOne() {
        System.out.println("\n=== DIAGNOSTIC TEST: Single Transport ===");
        System.out.println("Querying: CADK911088");
        System.out.println("Expected: 1 transport");
        System.out.println("Expected time: < 5 seconds");

        long startTime = System.currentTimeMillis();

        TransportInfoListResult result = transportService.getTransportInfo("CADK911088");

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + result.success());
        System.out.println("Total Count: " + result.totalCount());
        System.out.println("Duration: " + duration + " ms");

        if (result.success() && result.totalCount() > 0) {
            System.out.println("\nFirst transport:");
            TransportInfoListResult.TransportInfo info = result.getFirst();
            System.out.println("  Number: " + info.transportNumber());
            System.out.println("  Type: " + info.transportType());
            System.out.println("  Owner: " + info.owner());
            System.out.println("  Status: " + info.status());
        }

        System.out.println("\n=== ANALYSIS ===");
        if (result.totalCount() == 1) {
            System.out.println("✅ PASS: FM correctly filtered to 1 transport");
        } else if (result.totalCount() > 100) {
            System.out.println("❌ FAIL: FM returned " + result.totalCount() + " transports!");
            System.out.println("   This suggests WHERE clause is NOT filtering");
            System.out.println("   or SPLIT is not working correctly.");
        } else {
            System.out.println("⚠️  UNEXPECTED: Got " + result.totalCount() + " transports");
        }

        if (duration > 5000) {
            System.out.println("⚠️  SLOW: Query took " + (duration / 1000.0) + " seconds");
            System.out.println("   This suggests full table scan (no index usage)");
        } else {
            System.out.println("✅ FAST: Query completed in " + duration + " ms");
        }

        System.out.println("=====================================\n");

        // Assertions
        assertTrue(result.success(), "FM should succeed");
        assertEquals(1, result.totalCount(), "Should return exactly 1 transport");
        assertTrue(duration < 5000, "Query should complete in < 5 seconds");
    }

    /**
     * Diagnostic: Query two transports with comma separator.
     * Should return exactly 2 results.
     */
    @Test
    void testMultipleTransports_ExpectTwo() {
        System.out.println("\n=== DIAGNOSTIC TEST: Multiple Transports ===");
        System.out.println("Querying: CADK911088,CADK911089");
        System.out.println("Expected: 2 transports");

        long startTime = System.currentTimeMillis();

        TransportInfoListResult result = transportService.getTransportInfo("CADK911088,CADK911089");

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + result.success());
        System.out.println("Total Count: " + result.totalCount());
        System.out.println("Duration: " + duration + " ms");

        if (result.success() && result.totalCount() > 0) {
            System.out.println("\nTransports returned:");
            result.transports().forEach(info -> {
                System.out.println("  - " + info.transportNumber() +
                                 " (Type: " + info.transportType() +
                                 ", Owner: " + info.owner() + ")");
            });
        }

        System.out.println("\n=== ANALYSIS ===");
        if (result.totalCount() == 2) {
            System.out.println("✅ PASS: FM correctly filtered to 2 transports");
            System.out.println("   SPLIT logic is working");
            System.out.println("   WHERE clause is filtering correctly");
        } else if (result.totalCount() > 100) {
            System.out.println("❌ FAIL: FM returned " + result.totalCount() + " transports!");
            System.out.println("   SPLIT might not be working");
            System.out.println("   WHERE clause is NOT filtering");
        } else {
            System.out.println("⚠️  UNEXPECTED: Got " + result.totalCount() + " transports");
        }

        System.out.println("=====================================\n");

        // Assertions
        assertTrue(result.success(), "FM should succeed");
        assertTrue(result.totalCount() <= 2, "Should return at most 2 transports");
        assertTrue(duration < 10000, "Query should complete in < 10 seconds");
    }
}

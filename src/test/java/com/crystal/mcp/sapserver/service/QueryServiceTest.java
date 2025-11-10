package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TableContentsResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for QueryService.
 *
 * These tests require a live SAP connection configured via environment variables.
 * Tests query real SAP tables (E071, E070) to validate functionality.
 *
 * Prerequisites:
 * - SAP connection configured in .mcp.json or environment
 * - ADT installed on SAP system
 * - User has authorization to query tables
 *
 * Test Strategy:
 * - Test basic table query functionality
 * - Test WHERE clause filtering
 * - Test field selection
 * - Test error handling
 */
@SpringBootTest
class QueryServiceTest {

    @Autowired
    private QueryService queryService;

    @Test
    void testGetTableContents_E071_simple() {
        // Test: Query E071 table (objects in transports)
        // This table should exist in all SAP systems

        TableContentsResult result = queryService.getTableContents(
                "E071",
                null,  // No WHERE clause
                10,    // Max 10 rows
                null   // All fields
        );

        assertNotNull(result, "Result should not be null");
        assertEquals("E071", result.tableName());
        assertTrue(result.columns().size() > 0, "Should have columns");
        System.out.println("✅ E071 query successful: " + result.rowCount() + " rows, " +
                result.columns().size() + " columns");
    }

    @Test
    void testGetTableContents_withWhereClause() {
        // Test: Query E071 with WHERE clause filtering
        // Find objects of type CLAS (classes)

        TableContentsResult result = queryService.getTableContents(
                "E071",
                "OBJECT = 'CLAS'",
                50,
                null
        );

        assertNotNull(result);
        assertEquals("E071", result.tableName());

        System.out.println("✅ WHERE clause test successful: " + result.rowCount() + " CLAS objects found");

        // Verify all returned objects are CLAS type
        if (result.rowCount() > 0) {
            for (var row : result.rows()) {
                assertEquals("CLAS", row.get("OBJECT"),
                        "All objects should be CLAS type");
            }
        }
    }

    @Test
    void testGetTableContents_withFieldSelection() {
        // Test: Query with specific field selection

        TableContentsResult result = queryService.getTableContents(
                "E071",
                null,
                10,
                List.of("TRKORR", "OBJ_NAME", "OBJECT")  // Only these fields
        );

        assertNotNull(result);
        assertTrue(result.columns().size() >= 3, "Should have at least 3 columns");

        System.out.println("✅ Field selection test successful: " +
                result.columns().size() + " columns returned");

        // Verify only requested fields are present (plus possibly system fields)
        if (result.rowCount() > 0) {
            var row = result.rows().get(0);
            assertTrue(row.containsKey("TRKORR"), "Should have TRKORR field");
            assertTrue(row.containsKey("OBJ_NAME"), "Should have OBJ_NAME field");
            assertTrue(row.containsKey("OBJECT"), "Should have OBJECT field");
        }
    }

    @Test
    void testGetTableContents_maxRowsLimit() {
        // Test: Verify maxRows parameter is respected

        int requestedRows = 5;
        TableContentsResult result = queryService.getTableContents(
                "E071",
                null,
                requestedRows,
                null
        );

        assertNotNull(result);
        assertTrue(result.rowCount() <= requestedRows,
                "Result should not exceed requested max rows");

        System.out.println("✅ MaxRows limit test successful: requested=" + requestedRows +
                ", got=" + result.rowCount());
    }

    @Test
    void testGetTableContents_emptyResult() {
        // Test: Query that returns no results

        TableContentsResult result = queryService.getTableContents(
                "E071",
                "TRKORR = 'NONEXISTENT99999'",  // Non-existent transport
                10,
                null
        );

        assertNotNull(result);
        assertEquals(0, result.rowCount(), "Should return 0 rows for non-existent data");
        System.out.println("✅ Empty result test successful");
    }

    @Test
    void testGetTableContents_invalidTableName() {
        // Test: Error handling for invalid table name

        assertThrows(RuntimeException.class, () -> {
            queryService.getTableContents(
                    "INVALID_TABLE_XYZ123",
                    null,
                    10,
                    null
            );
        }, "Should throw exception for invalid table name");

        System.out.println("✅ Invalid table name error handling successful");
    }

    @Test
    void testGetTableContents_E070() {
        // Test: Query E070 table (transport metadata)
        // Verify integration with transport system

        TableContentsResult result = queryService.getTableContents(
                "E070",
                "TRFUNCTION = 'K'",  // Workbench transports
                10,
                List.of("TRKORR", "AS4USER", "TRSTATUS", "TRFUNCTION")
        );

        assertNotNull(result);
        assertEquals("E070", result.tableName());

        System.out.println("✅ E070 query successful: " + result.rowCount() + " workbench transports found");

        // Verify all are Workbench type
        if (result.rowCount() > 0) {
            for (var row : result.rows()) {
                assertEquals("K", row.get("TRFUNCTION"),
                        "All transports should be Workbench type (K)");
            }
        }
    }
}

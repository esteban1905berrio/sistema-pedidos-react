package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TableContentsResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Debug test to investigate E071 table access.
 */
@SpringBootTest
class DebugQueryE071Test {

    @Autowired
    private QueryService queryService;

    /**
     * Test 1: Query E071 without WHERE clause to see if we can access it at all
     */
    @Test
    void testQueryE071_NoFilter() {
        System.out.println("\n=== TEST 1: Query E071 without filters ===");

        TableContentsResult result = queryService.getTableContents(
                "E071",
                null,  // No WHERE clause
                10,    // Just first 10 rows
                List.of("TRKORR", "OBJ_NAME", "OBJECT", "PGMID", "LOCKFLAG")
        );

        System.out.println("Row count: " + result.rowCount());
        System.out.println("Rows:");
        result.rows().forEach(row -> {
            System.out.println("  TRKORR: " + row.get("TRKORR") +
                    ", OBJ_NAME: " + row.get("OBJ_NAME") +
                    ", OBJECT: " + row.get("OBJECT"));
        });
    }

    /**
     * Test 2: Query E071 with LIKE pattern for our specific class
     */
    @Test
    void testQueryE071_WithLikePattern() {
        System.out.println("\n=== TEST 2: Query E071 with LIKE pattern ===");

        String className = "ZCLMMI1229_SINCRONIZA_INV_MAWM";
        String whereClause = "OBJ_NAME LIKE '%" + className + "%'";

        System.out.println("WHERE clause: " + whereClause);

        TableContentsResult result = queryService.getTableContents(
                "E071",
                whereClause,
                10,
                List.of("TRKORR", "OBJ_NAME", "OBJECT", "PGMID", "LOCKFLAG")
        );

        System.out.println("Row count: " + result.rowCount());
        System.out.println("Rows:");
        result.rows().forEach(row -> {
            System.out.println("  TRKORR: " + row.get("TRKORR") +
                    ", OBJ_NAME: " + row.get("OBJ_NAME") +
                    ", OBJECT: " + row.get("OBJECT") +
                    ", PGMID: " + row.get("PGMID"));
        });
    }

    /**
     * Test 3: Query E071 for specific transport S4DK932807
     */
    @Test
    void testQueryE071_SpecificTransport() {
        System.out.println("\n=== TEST 3: Query E071 for transport S4DK932807 ===");

        String whereClause = "TRKORR = 'S4DK932807'";

        System.out.println("WHERE clause: " + whereClause);

        TableContentsResult result = queryService.getTableContents(
                "E071",
                whereClause,
                100,
                List.of("TRKORR", "OBJ_NAME", "OBJECT", "PGMID", "LOCKFLAG")
        );

        System.out.println("Row count: " + result.rowCount());
        System.out.println("Rows:");
        result.rows().forEach(row -> {
            System.out.println("  TRKORR: " + row.get("TRKORR") +
                    ", OBJ_NAME: " + row.get("OBJ_NAME") +
                    ", OBJECT: " + row.get("OBJECT") +
                    ", PGMID: " + row.get("PGMID") +
                    ", LOCKFLAG: " + row.get("LOCKFLAG"));
        });
    }
}

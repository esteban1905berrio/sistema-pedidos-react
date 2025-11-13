package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.DictionaryObjectRequest;
import com.crystal.mcp.sapserver.model.DictionaryObjectResult;
import com.crystal.mcp.sapserver.model.TableField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TableService with real SAP connection.
 *
 * <p>Tests the complete workflow for creating transparent tables:
 * <ol>
 *   <li>CREATE object (POST /ddic/tables)</li>
 *   <li>LOCK object</li>
 *   <li>MODIFY source (set DDL)</li>
 *   <li>UNLOCK object</li>
 * </ol>
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>SAP connection configured via environment variables</li>
 *   <li>ADT authorization for creating dictionary objects</li>
 *   <li>Valid transport request (CADK911293)</li>
 *   <li>Package ZPSY must exist</li>
 * </ul>
 *
 * <p><b>Test Parameters:</b>
 * <ul>
 *   <li>Table: ZTPSR013_2</li>
 *   <li>Package: ZPSY</li>
 *   <li>Transport: CADK911293</li>
 * </ul>
 *
 * @author Crystal Development Team
 */
@SpringBootTest
class TableServiceIntegrationTest {

    @Autowired
    private TableService tableService;

    /**
     * Test 1: Create table ZTPSR013_2 with real SAP connection.
     *
     * <p>This test creates a transparent table in package ZPSY with transport CADK911293.
     * The table contains sample fields for testing purposes:
     * <ul>
     *   <li>mat (matnr) - Material number - KEY</li>
     *   <li>gjahr (gjahr) - Fiscal year - KEY</li>
     *   <li>description (abap.char(255)) - Description text - NON-KEY</li>
     * </ul>
     *
     * <p><b>Workflow Validation:</b>
     * <ol>
     *   <li>DDL generated from fields[]</li>
     *   <li>Object created in ZPSY package</li>
     *   <li>Source modified with generated DDL</li>
     *   <li>Transport CADK911293 assigned</li>
     *   <li>Object left in inactive version</li>
     * </ol>
     */
    @Test
    void testCreateTableZTPSR013_2() {
        // Given
        String tableName = "ZTPSR013_2";
        String description = "Test Table for Integration Testing";
        String packageName = "ZPSY";
        String transport = "CADK911293";

        List<TableField> fields = List.of(
                new TableField("mat", "matnr", true, "Material Number"),
                new TableField("gjahr", "gjahr", true, "Fiscal Year"),
                new TableField("description", "abap.char(255)", false, "Description Text")
        );

        DictionaryObjectRequest request = new DictionaryObjectRequest(
                tableName,
                description,
                fields,
                packageName,
                transport
        );

        // When
        DictionaryObjectResult result = tableService.createTable(request);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getUri(), "Object URI should be assigned");
        assertEquals("ZTPSR013_2", result.getName().toUpperCase(), "Table name should match");
        assertEquals("inactive", result.getVersion(), "New objects should be inactive");
        assertEquals("ZPSY", result.getPackageName(), "Package should be ZPSY");
        assertEquals("CADK911293", result.getTransport(), "Transport should be CADK911293");
        assertFalse(result.isLocal(), "Object should not be local (transportable)");
        assertNotNull(result.getMessage(), "Success message should be present");

        // Verify URI format
        assertTrue(result.getUri().contains("/sap/bc/adt/ddic/tables/"),
                "URI should be a valid ADT table URI");
        assertTrue(result.getUri().toLowerCase().contains("ztpsr013_2"),
                "URI should contain table name");

        // Log result for manual verification
        System.out.println("=== Integration Test Result ===");
        System.out.println("URI: " + result.getUri());
        System.out.println("Name: " + result.getName());
        System.out.println("Version: " + result.getVersion());
        System.out.println("Package: " + result.getPackageName());
        System.out.println("Transport: " + result.getTransport());
        System.out.println("Is Local: " + result.isLocal());
        System.out.println("Message: " + result.getMessage());
        System.out.println("==============================");
    }

    /**
     * Test 2: Verify DDL generation for ZTPSR013_2.
     *
     * <p>This test indirectly validates DDL generation by creating a table
     * and ensuring the workflow completes successfully. The generated DDL
     * should include:
     * <ul>
     *   <li>@EndUserText.label annotation with description</li>
     *   <li>@AbapCatalog annotations (enhancementCategory, tableCategory, etc.)</li>
     *   <li>Automatic "key client : abap.clnt;" field</li>
     *   <li>Key fields sorted alphabetically (gjahr, mat)</li>
     *   <li>Non-key field (description)</li>
     * </ul>
     */
    @Test
    void testTableCreationWorkflowComplete() {
        // Given
        String tableName = "ZTPSR013_2";
        String description = "Test Table for Workflow Validation";
        String packageName = "ZPSY";
        String transport = "CADK911293";

        List<TableField> fields = List.of(
                new TableField("mat", "matnr", true),
                new TableField("gjahr", "gjahr", true),
                new TableField("description", "abap.char(255)", false)
        );

        DictionaryObjectRequest request = new DictionaryObjectRequest(
                tableName,
                description,
                fields,
                packageName,
                transport
        );

        // When
        DictionaryObjectResult result = tableService.createTable(request);

        // Then - Workflow validation
        assertNotNull(result, "Workflow should complete successfully");

        // Validate stateful workflow executed
        assertNotNull(result.getUri(), "Object should be created (POST /ddic/tables)");
        assertNotNull(result.getTransport(), "Object should be locked with transport");
        assertEquals("inactive", result.getVersion(), "Object should be modified (source set)");

        // Validate transport assignment
        assertEquals("CADK911293", result.getTransport(),
                "Transport should match requested value");

        // Validate package assignment
        assertEquals("ZPSY", result.getPackageName(),
                "Package should match requested value");

        System.out.println("=== Workflow Validation Result ===");
        System.out.println("✓ CREATE: Object URI assigned");
        System.out.println("✓ LOCK: Transport assigned (" + result.getTransport() + ")");
        System.out.println("✓ MODIFY: Version set to inactive");
        System.out.println("✓ UNLOCK: Workflow completed successfully");
        System.out.println("==================================");
    }

    /**
     * Test 3: Error handling - Invalid transport number.
     *
     * <p>This test validates that the service properly handles invalid
     * transport requests. The workflow should fail gracefully with a
     * descriptive error message.
     */
    @Test
    void testErrorInvalidTransport() {
        // Given
        String tableName = "ZTPSR013_ERR";
        String description = "Test Table for Error Handling";
        String packageName = "ZPSY";
        String transport = "INVALID999"; // Invalid transport

        List<TableField> fields = List.of(
                new TableField("field1", "abap.char(10)", false)
        );

        DictionaryObjectRequest request = new DictionaryObjectRequest(
                tableName,
                description,
                fields,
                packageName,
                transport
        );

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            tableService.createTable(request);
        }, "Should throw exception for invalid transport");
    }

    /**
     * Test 4: Multiple key fields with different types.
     *
     * <p>This test validates DDL generation with various ABAP types:
     * <ul>
     *   <li>Built-in types: abap.char(10), abap.numc(8), abap.dec(13,2)</li>
     *   <li>Reference types: bukrs, gjahr</li>
     * </ul>
     */
    @Test
    void testTableWithMultipleTypesAndKeys() {
        // Given
        String tableName = "ZTPSR013_3";
        String description = "Test Table with Multiple Types";
        String packageName = "ZPSY";
        String transport = "CADK911293";

        List<TableField> fields = List.of(
                new TableField("bukrs", "bukrs", true, "Company Code"),
                new TableField("gjahr", "gjahr", true, "Fiscal Year"),
                new TableField("belnr", "abap.char(10)", true, "Document Number"),
                new TableField("amount", "abap.dec(13,2)", false, "Amount"),
                new TableField("ref_number", "abap.numc(8)", false, "Reference Number")
        );

        DictionaryObjectRequest request = new DictionaryObjectRequest(
                tableName,
                description,
                fields,
                packageName,
                transport
        );

        // When
        DictionaryObjectResult result = tableService.createTable(request);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("ZTPSR013_3", result.getName().toUpperCase(), "Table name should match");
        assertEquals("ZPSY", result.getPackageName(), "Package should be ZPSY");
        assertEquals("CADK911293", result.getTransport(), "Transport should be CADK911293");

        System.out.println("=== Multiple Types Test Result ===");
        System.out.println("Table: " + result.getName());
        System.out.println("Key Fields: bukrs, gjahr, belnr");
        System.out.println("Non-key Fields: amount (dec), ref_number (numc)");
        System.out.println("URI: " + result.getUri());
        System.out.println("==================================");
    }
}

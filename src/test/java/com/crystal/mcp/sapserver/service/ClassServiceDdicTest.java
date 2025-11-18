package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.DdicSourceResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ClassService.getDdicSource() method.
 *
 * <p>Tests the FM ZCX_GETDDICSOURCE integration for retrieving
 * DDIC object structures (tables, structures, views).
 *
 * <p>Prerequisites:
 * - SAP connection configured via environment variables
 * - FM ZCX_GETDDICSOURCE activated in SAP system (Function Group ZGFCX_1)
 * - FM signature configured in SE37 (see docs/abap/FM_ZCX_GETDDICSOURCE_SIGNATURE.md)
 *
 * @author Crystal Development Team
 * @since 1.0
 */
@SpringBootTest
class ClassServiceDdicTest {

    @Autowired
    private ClassService classService;

    /**
     * Test getDdicSource with a standard SAP table (MARA).
     *
     * <p>MARA (Material Master) is a standard transparent table
     * that should exist in all SAP systems.
     */
    @Test
    void testGetDdicSource_Table_MARA() {
        // Given
        String tableName = "MARA";

        // When
        DdicSourceResult result = classService.getDdicSource(tableName);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("MARA", result.getObjectName(), "Object name should be MARA");
        assertEquals("TABLE", result.getObjectType(), "Object type should be TABLE");
        assertEquals("ACTIVE", result.getObjectStatus(), "Object status should be ACTIVE");

        assertNotNull(result.getFields(), "Fields list should not be null");
        assertTrue(result.getFieldCount() > 0, "MARA should have fields");
        assertTrue(result.getFieldCount() > 100, "MARA should have more than 100 fields");

        // Verify first field is MANDT (client field, always first in SAP tables)
        DdicSourceResult.DdicField firstField = result.getFields().get(0);
        assertEquals("MANDT", firstField.getFieldname(), "First field should be MANDT");
        assertEquals(1, firstField.getPosition(), "MANDT should be at position 1");
        assertEquals("X", firstField.getKeyflag(), "MANDT should be a key field");

        // Verify MATNR (material number) exists and is a key field
        DdicSourceResult.DdicField matnrField = result.getFields().stream()
                .filter(f -> "MATNR".equals(f.getFieldname()))
                .findFirst()
                .orElse(null);

        assertNotNull(matnrField, "MATNR field should exist");
        assertEquals("X", matnrField.getKeyflag(), "MATNR should be a key field");
        assertEquals("X", matnrField.getMandatory(), "MATNR should be mandatory");
    }

    /**
     * Test getDdicSource with DD03L (Table Field Definitions).
     *
     * <p>DD03L is a standard structure used to store table field metadata.
     */
    @Test
    void testGetDdicSource_Structure_DD03L() {
        // Given
        String structureName = "DD03L";

        // When
        DdicSourceResult result = classService.getDdicSource(structureName);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("DD03L", result.getObjectName(), "Object name should be DD03L");
        // DD03L can be TABLE or STRUCTURE depending on system version
        assertTrue(
                "TABLE".equals(result.getObjectType()) || "STRUCTURE".equals(result.getObjectType()),
                "Object type should be TABLE or STRUCTURE"
        );
        assertEquals("ACTIVE", result.getObjectStatus(), "Object status should be ACTIVE");

        assertNotNull(result.getFields(), "Fields list should not be null");
        assertTrue(result.getFieldCount() > 0, "DD03L should have fields");

        // Verify FIELDNAME field exists
        boolean fieldnameExists = result.getFields().stream()
                .anyMatch(f -> "FIELDNAME".equals(f.getFieldname()));
        assertTrue(fieldnameExists, "FIELDNAME field should exist in DD03L");
    }

    /**
     * Test getDdicSource with T001 (Company Codes).
     *
     * <p>T001 is a standard configuration table present in all SAP systems.
     */
    @Test
    void testGetDdicSource_Table_T001() {
        // Given
        String tableName = "T001";

        // When
        DdicSourceResult result = classService.getDdicSource(tableName);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals("T001", result.getObjectName(), "Object name should be T001");
        assertEquals("TABLE", result.getObjectType(), "Object type should be TABLE");
        assertEquals("ACTIVE", result.getObjectStatus(), "Object status should be ACTIVE");

        assertNotNull(result.getFields(), "Fields list should not be null");
        assertTrue(result.getFieldCount() > 0, "T001 should have fields");

        // Verify BUKRS (company code) exists and is a key field
        DdicSourceResult.DdicField bukrsField = result.getFields().stream()
                .filter(f -> "BUKRS".equals(f.getFieldname()))
                .findFirst()
                .orElse(null);

        assertNotNull(bukrsField, "BUKRS field should exist");
        assertEquals("X", bukrsField.getKeyflag(), "BUKRS should be a key field");
    }

    /**
     * Test getDdicSource with non-existent table.
     *
     * <p>Should throw RuntimeException with OBJECT_NOT_FOUND exception.
     */
    @Test
    void testGetDdicSource_NonExistentTable() {
        // Given
        String tableName = "ZZZZZ_NO_EXISTE";

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> classService.getDdicSource(tableName),
                "Should throw RuntimeException for non-existent table"
        );

        assertTrue(
                exception.getMessage().contains("not found") ||
                exception.getMessage().contains("OBJECT_NOT_FOUND"),
                "Exception message should indicate object not found"
        );
    }

    /**
     * Test getDdicSource with empty object name.
     *
     * <p>Should throw IllegalArgumentException.
     */
    @Test
    void testGetDdicSource_EmptyObjectName() {
        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> classService.getDdicSource(""),
                "Should throw IllegalArgumentException for empty object name"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> classService.getDdicSource(null),
                "Should throw IllegalArgumentException for null object name"
        );
    }

    /**
     * Test field metadata parsing.
     *
     * <p>Verifies that field metadata is correctly parsed from JSON.
     */
    @Test
    void testGetDdicSource_FieldMetadata() {
        // Given
        String tableName = "T001";

        // When
        DdicSourceResult result = classService.getDdicSource(tableName);

        // Then
        assertNotNull(result.getFields(), "Fields should not be null");
        assertFalse(result.getFields().isEmpty(), "Fields should not be empty");

        // Verify each field has required metadata
        for (DdicSourceResult.DdicField field : result.getFields()) {
            assertNotNull(field.getFieldname(), "Field name should not be null");
            assertTrue(field.getPosition() > 0, "Field position should be positive");
            assertNotNull(field.getRollname(), "Rollname should not be null");
            assertNotNull(field.getDatatype(), "Datatype should not be null");
            assertNotNull(field.getInttype(), "Inttype should not be null");
            assertTrue(field.getIntlen() > 0, "Intlen should be positive");
        }

        // Verify raw JSON is present
        assertNotNull(result.getRawJson(), "Raw JSON should not be null");
        assertTrue(result.getRawJson().contains("["), "Raw JSON should be an array");
    }
}

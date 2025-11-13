package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TableField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DdlGenerator.
 *
 * <p>Tests DDL generation for various field combinations:
 * <ul>
 *   <li>Basic table with key and non-key fields</li>
 *   <li>Multiple key fields</li>
 *   <li>Built-in types (abap.char, abap.dec, etc.)</li>
 *   <li>Reference types (matnr, gjahr, etc.)</li>
 *   <li>Validation errors (empty fields, invalid types, duplicates)</li>
 * </ul>
 *
 * @author Crystal Development Team
 */
class DdlGeneratorTest {

    private DdlGenerator ddlGenerator;

    @BeforeEach
    void setUp() {
        ddlGenerator = new DdlGenerator();
    }

    /**
     * Test 1: Tabla simple con 2 campos (1 key, 1 non-key).
     */
    @Test
    void testGenerateSimpleTable() {
        // Given
        String tableName = "ytmp_1";
        String description = "Temporary Test Table";
        List<TableField> fields = List.of(
                new TableField("mat", "matnr", true),
                new TableField("gjahr", "gjahr", false)
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        assertNotNull(ddl);
        assertTrue(ddl.contains("@EndUserText.label : 'Temporary Test Table'"));
        assertTrue(ddl.contains("define table ytmp_1 {"));
        assertTrue(ddl.contains("key client : abap.clnt;"));
        assertTrue(ddl.contains("key mat"));
        assertTrue(ddl.contains(": matnr;"));
        assertTrue(ddl.contains("gjahr"));
        assertTrue(ddl.contains(": gjahr;"));
        assertTrue(ddl.endsWith("\n}"));
    }

    /**
     * Test 2: Tabla con múltiples key fields.
     */
    @Test
    void testGenerateTableWithMultipleKeys() {
        // Given
        String tableName = "ztable_keys";
        String description = "Table with Multiple Keys";
        List<TableField> fields = List.of(
                new TableField("bukrs", "bukrs", true),
                new TableField("gjahr", "gjahr", true),
                new TableField("belnr", "abap.char(10)", true),
                new TableField("description", "abap.char(255)", false)
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        assertTrue(ddl.contains("key client"));
        assertTrue(ddl.contains("key bukrs"));
        assertTrue(ddl.contains("key gjahr"));
        assertTrue(ddl.contains("key belnr"));
        assertTrue(ddl.contains("  description")); // non-key (sin "key")
    }

    /**
     * Test 3: Tabla con tipos built-in.
     */
    @Test
    void testGenerateTableWithBuiltinTypes() {
        // Given
        String tableName = "ytable_builtin";
        String description = "Table with Built-in Types";
        List<TableField> fields = List.of(
                new TableField("text_field", "abap.char(50)", false),
                new TableField("number_field", "abap.numc(8)", false),
                new TableField("decimal_field", "abap.dec(13,2)", false),
                new TableField("date_field", "abap.dats", false)
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        assertTrue(ddl.contains(": abap.char(50)"));
        assertTrue(ddl.contains(": abap.numc(8)"));
        assertTrue(ddl.contains(": abap.dec(13,2)"));
        assertTrue(ddl.contains(": abap.dats"));
    }

    /**
     * Test 4: Tabla con campos ordenados alfabéticamente.
     */
    @Test
    void testFieldsSortedAlphabetically() {
        // Given
        String tableName = "ytable_sorted";
        String description = "Table with Sorted Fields";
        List<TableField> fields = List.of(
                new TableField("zebra", "abap.char(10)", false),
                new TableField("alpha", "abap.char(10)", false),
                new TableField("gamma", "abap.char(10)", false)
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        // Los campos deben estar ordenados: alpha, gamma, zebra
        int alphaPos = ddl.indexOf("alpha");
        int gammaPos = ddl.indexOf("gamma");
        int zebraPos = ddl.indexOf("zebra");

        assertTrue(alphaPos < gammaPos);
        assertTrue(gammaPos < zebraPos);
    }

    /**
     * Test 5: Annotations estándar presentes.
     */
    @Test
    void testStandardAnnotationsPresent() {
        // Given
        String tableName = "ytable_annotations";
        String description = "Table for Annotations Test";
        List<TableField> fields = List.of(
                new TableField("field1", "abap.char(10)", false)
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        assertTrue(ddl.contains("@EndUserText.label"));
        assertTrue(ddl.contains("@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE"));
        assertTrue(ddl.contains("@AbapCatalog.tableCategory : #TRANSPARENT"));
        assertTrue(ddl.contains("@AbapCatalog.deliveryClass : #A"));
        assertTrue(ddl.contains("@AbapCatalog.dataMaintenance : #RESTRICTED"));
    }

    /**
     * Test 6: Error - tabla vacía.
     */
    @Test
    void testErrorEmptyTableName() {
        // Given
        String tableName = "";
        String description = "Test";
        List<TableField> fields = List.of(
                new TableField("field1", "abap.char(10)", false)
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ddlGenerator.generateTableDdl(tableName, description, fields);
        });
    }

    /**
     * Test 7: Error - descripción vacía.
     */
    @Test
    void testErrorEmptyDescription() {
        // Given
        String tableName = "ytable_test";
        String description = "";
        List<TableField> fields = List.of(
                new TableField("field1", "abap.char(10)", false)
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ddlGenerator.generateTableDdl(tableName, description, fields);
        });
    }

    /**
     * Test 8: Error - lista de campos vacía.
     */
    @Test
    void testErrorEmptyFields() {
        // Given
        String tableName = "ytable_test";
        String description = "Test Table";
        List<TableField> fields = new ArrayList<>();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ddlGenerator.generateTableDdl(tableName, description, fields);
        });
    }

    /**
     * Test 9: Error - campo "client" en la lista (se agrega automáticamente).
     */
    @Test
    void testErrorClientFieldInList() {
        // Given
        String tableName = "ytable_test";
        String description = "Test Table";
        List<TableField> fields = List.of(
                new TableField("client", "abap.clnt", true),
                new TableField("mat", "matnr", true)
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ddlGenerator.generateTableDdl(tableName, description, fields);
        });

        assertTrue(exception.getMessage().contains("client"));
        assertTrue(exception.getMessage().contains("automatically"));
    }

    /**
     * Test 10: Error - tipo de dato inválido (built-in incorrecto).
     */
    @Test
    void testErrorInvalidBuiltinType() {
        // Given
        String tableName = "ytable_test";
        String description = "Test Table";
        List<TableField> fields = List.of(
                new TableField("field1", "abap.invalid(10)", false)
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ddlGenerator.generateTableDdl(tableName, description, fields);
        });

        assertTrue(exception.getMessage().contains("invalid built-in type"));
    }

    /**
     * Test 11: Error - nombres de campos duplicados.
     */
    @Test
    void testErrorDuplicateFieldNames() {
        // Given
        String tableName = "ytable_test";
        String description = "Test Table";
        List<TableField> fields = List.of(
                new TableField("field1", "abap.char(10)", false),
                new TableField("field1", "abap.char(20)", false) // Duplicado
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ddlGenerator.generateTableDdl(tableName, description, fields);
        });

        assertTrue(exception.getMessage().contains("Duplicate field name"));
    }

    /**
     * Test 12: Escapado de comillas simples en descripción.
     */
    @Test
    void testDescriptionWithSingleQuotes() {
        // Given
        String tableName = "ytable_test";
        String description = "Table with 'quotes' in description";
        List<TableField> fields = List.of(
                new TableField("field1", "abap.char(10)", false)
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        // Las comillas simples deben estar escapadas (duplicadas)
        assertTrue(ddl.contains("Table with ''quotes'' in description"));
    }

    /**
     * Test 13: Validación de tipo ABAP válido.
     */
    @Test
    void testIsValidAbapType() {
        // Built-in types
        assertTrue(ddlGenerator.isValidAbapType("abap.char(10)"));
        assertTrue(ddlGenerator.isValidAbapType("abap.dec(13,2)"));
        assertTrue(ddlGenerator.isValidAbapType("abap.numc(8)"));

        // Reference types
        assertTrue(ddlGenerator.isValidAbapType("matnr"));
        assertTrue(ddlGenerator.isValidAbapType("gjahr"));
        assertTrue(ddlGenerator.isValidAbapType("bukrs"));

        // Invalid types
        assertFalse(ddlGenerator.isValidAbapType(""));
        assertFalse(ddlGenerator.isValidAbapType(null));
        assertFalse(ddlGenerator.isValidAbapType("abap.invalid(10)"));
    }
}

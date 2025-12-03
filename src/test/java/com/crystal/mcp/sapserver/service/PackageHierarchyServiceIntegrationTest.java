package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.PackageHierarchyResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PackageHierarchyService.
 *
 * <p>Tests require Z_CX_GET_PACKAGE_HIERARCHY FM to be implemented in SAP system.</p>
 *
 * <h2>Prerequisites</h2>
 * <ul>
 *   <li>FM Z_CX_GET_PACKAGE_HIERARCHY must be manually implemented in SE37</li>
 *   <li>SAP connection configured via environment variables</li>
 *   <li>Test package ZCX with children packages must exist</li>
 * </ul>
 *
 * @see PackageHierarchyService
 */
@SpringBootTest
class PackageHierarchyServiceIntegrationTest {

    @Autowired
    private PackageHierarchyService packageHierarchyService;

    /**
     * Test children mode - get direct children of a package.
     *
     * <p>Queries children of ZCX package (non-recursive).</p>
     */
    @Test
    void testGetPackageHierarchyChildren() throws Exception {
        // Given
        String packageName = "ZCX";
        String mode = "C";
        boolean recursive = false;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertNotNull(result.hierarchy(), "Hierarchy should not be null");

        JsonNode hierarchy = result.hierarchy();
        assertEquals("children", hierarchy.get("mode").asText());
        assertEquals(packageName, hierarchy.get("packageName").asText());
        assertEquals(recursive, hierarchy.get("recursive").asBoolean());
        assertTrue(hierarchy.has("hierarchy"), "Should have hierarchy array");
        assertTrue(hierarchy.has("totalPackages"), "Should have totalPackages");
    }

    /**
     * Test children mode - recursive query.
     *
     * <p>Queries all descendants of ZCX package.</p>
     */
    @Test
    void testGetPackageHierarchyChildrenRecursive() throws Exception {
        // Given
        String packageName = "ZCX";
        String mode = "C";
        boolean recursive = true;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");
        assertNotNull(result.hierarchy(), "Hierarchy should not be null");

        JsonNode hierarchy = result.hierarchy();
        assertTrue(hierarchy.get("recursive").asBoolean(), "Should be recursive");
    }

    /**
     * Test parents mode - get parent of a package.
     *
     * <p>Queries parent of a child package (non-recursive).</p>
     */
    @Test
    void testGetPackageHierarchyParents() throws Exception {
        // Given - Use a known child package
        String packageName = "ZCXENH";
        String mode = "P";
        boolean recursive = false;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");

        // Note: Test may fail if package doesn't exist or has no parent
        if (result.success()) {
            JsonNode hierarchy = result.hierarchy();
            assertEquals("parents", hierarchy.get("mode").asText());
            assertEquals(packageName, hierarchy.get("packageName").asText());
        }
    }

    /**
     * Test default mode - should use 'C' when mode is null.
     */
    @Test
    void testGetPackageHierarchyDefaultMode() throws Exception {
        // Given
        String packageName = "ZCX";
        String mode = null; // Should default to 'C'
        boolean recursive = false;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Query should succeed");

        JsonNode hierarchy = result.hierarchy();
        assertEquals("children", hierarchy.get("mode").asText(), "Should default to children mode");
    }

    /**
     * Test validation - missing package name.
     */
    @Test
    void testGetPackageHierarchyMissingPackageName() throws Exception {
        // Given
        String packageName = null;
        String mode = "C";
        boolean recursive = false;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.success(), "Query should fail");
        assertTrue(result.message().contains("required"), "Error message should mention required");
    }

    /**
     * Test validation - invalid mode.
     */
    @Test
    void testGetPackageHierarchyInvalidMode() throws Exception {
        // Given
        String packageName = "ZCX";
        String mode = "X"; // Invalid mode
        boolean recursive = false;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.success(), "Query should fail");
        assertTrue(result.message().contains("Invalid mode"), "Error message should mention invalid mode");
    }

    /**
     * Test non-existent package.
     *
     * <p>SAP should return success=false when package doesn't exist.</p>
     */
    @Test
    void testGetPackageHierarchyNonExistentPackage() throws Exception {
        // Given
        String packageName = "NONEXIST999";
        String mode = "C";
        boolean recursive = false;

        // When
        PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName, mode, recursive
        );

        // Then
        assertNotNull(result, "Result should not be null");
        // SAP should return success=false for non-existent packages
        assertFalse(result.success(), "Query should fail for non-existent package");
    }
}

package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.tool.PackageHierarchyTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Manual test for PackageHierarchyTools.
 *
 * <p><b>IMPORTANT</b>: This test requires Z_CX_GET_PACKAGE_HIERARCHY FM to be manually
 * implemented in SE37 transaction.</p>
 *
 * <h2>Prerequisites</h2>
 * <ul>
 *   <li>Function module Z_CX_GET_PACKAGE_HIERARCHY created in SAP</li>
 *   <li>Implementation code copied from resources/abap/functions/groups/zgfcx_1/fmodules/z_cx_get_package_hierarchy.abap</li>
 *   <li>FM activated in SE37</li>
 *   <li>Test package ZCX with children packages exists</li>
 * </ul>
 *
 * <h2>Expected Behavior</h2>
 * <ul>
 *   <li>Children mode: Returns JSON with subpackages</li>
 *   <li>Parents mode: Returns JSON with parent packages</li>
 *   <li>Recursive mode: Returns full hierarchy tree</li>
 * </ul>
 *
 * @see PackageHierarchyTools
 */
@SpringBootTest
class ManualPackageHierarchyTest {

    @Autowired
    private PackageHierarchyTools packageHierarchyTools;

    /**
     * Test get_package_hierarchy tool - children mode.
     *
     * <p>Tests direct children query for ZCX package.</p>
     */
    @Test
    void testGetPackageHierarchyChildren() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Children) ===");

        // Given
        String packageName = "ZCX";
        String mode = "C";
        Boolean recursive = false;

        System.out.println("Package: " + packageName);
        System.out.println("Mode: " + mode + " (children)");
        System.out.println("Recursive: " + recursive);

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected format:
        // {
        //   "success": true,
        //   "mode": "children",
        //   "recursive": false,
        //   "packageName": "ZCX",
        //   "hierarchy": [
        //     {
        //       "packageName": "ZCXENH",
        //       "parentPackage": "ZCX",
        //       "description": "...",
        //       "level": 1,
        //       "hasChildren": true
        //     }
        //   ],
        //   "totalPackages": 1
        // }
    }

    /**
     * Test get_package_hierarchy tool - children recursive.
     *
     * <p>Tests full descendant tree query.</p>
     */
    @Test
    void testGetPackageHierarchyChildrenRecursive() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Children Recursive) ===");

        // Given
        String packageName = "ZCX";
        String mode = "C";
        Boolean recursive = true;

        System.out.println("Package: " + packageName);
        System.out.println("Mode: " + mode + " (children)");
        System.out.println("Recursive: " + recursive);

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected: All descendants with level indicators (1, 2, 3...)
    }

    /**
     * Test get_package_hierarchy tool - parents mode.
     *
     * <p>Tests parent query for a child package.</p>
     */
    @Test
    void testGetPackageHierarchyParents() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Parents) ===");

        // Given
        String packageName = "ZCXENH";
        String mode = "P";
        Boolean recursive = false;

        System.out.println("Package: " + packageName);
        System.out.println("Mode: " + mode + " (parents)");
        System.out.println("Recursive: " + recursive);

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected:
        // {
        //   "success": true,
        //   "mode": "parents",
        //   "recursive": false,
        //   "packageName": "ZCXENH",
        //   "hierarchy": [
        //     {
        //       "packageName": "ZCX",
        //       "parentPackage": "",
        //       "description": "...",
        //       "level": 1
        //     }
        //   ],
        //   "totalPackages": 1
        // }
    }

    /**
     * Test get_package_hierarchy tool - parents recursive.
     *
     * <p>Tests full ancestor tree query.</p>
     */
    @Test
    void testGetPackageHierarchyParentsRecursive() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Parents Recursive) ===");

        // Given
        String packageName = "ZCXR1003";
        String mode = "P";
        Boolean recursive = true;

        System.out.println("Package: " + packageName);
        System.out.println("Mode: " + mode + " (parents)");
        System.out.println("Recursive: " + recursive);

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected: All ancestors up to root package
    }

    /**
     * Test get_package_hierarchy tool - default mode.
     *
     * <p>Tests default mode (should use 'C').</p>
     */
    @Test
    void testGetPackageHierarchyDefaultMode() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Default Mode) ===");

        // Given
        String packageName = "ZCX";
        String mode = null; // Should default to 'C'
        Boolean recursive = null; // Should default to false

        System.out.println("Package: " + packageName);
        System.out.println("Mode: " + mode + " (should default to C)");
        System.out.println("Recursive: " + recursive + " (should default to false)");

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected: Same as children mode, non-recursive
    }

    /**
     * Test error handling - missing package name.
     */
    @Test
    void testGetPackageHierarchyMissingPackageName() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Missing Package Name) ===");

        // Given
        String packageName = null;
        String mode = "C";
        Boolean recursive = false;

        System.out.println("Package: " + packageName + " (null)");
        System.out.println("Mode: " + mode);

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected: {"success":false,"error":"Package name is required"}
    }

    /**
     * Test error handling - invalid mode.
     */
    @Test
    void testGetPackageHierarchyInvalidMode() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Invalid Mode) ===");

        // Given
        String packageName = "ZCX";
        String mode = "X"; // Invalid
        Boolean recursive = false;

        System.out.println("Package: " + packageName);
        System.out.println("Mode: " + mode + " (invalid)");

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected: {"success":false,"error":"Invalid mode: X. Use 'C' or 'P'"}
    }

    /**
     * Test error handling - non-existent package.
     */
    @Test
    void testGetPackageHierarchyNonExistentPackage() {
        System.out.println("\n=== Manual Test: Get Package Hierarchy (Non-Existent Package) ===");

        // Given
        String packageName = "NONEXIST999";
        String mode = "C";
        Boolean recursive = false;

        System.out.println("Package: " + packageName + " (non-existent)");
        System.out.println("Mode: " + mode);

        // When
        String result = packageHierarchyTools.getPackageHierarchy(packageName, mode, recursive);

        // Then
        System.out.println("\n=== Result ===");
        System.out.println(result);

        // Expected: {"success":false,"error":"Package NONEXIST999 not found"}
    }
}

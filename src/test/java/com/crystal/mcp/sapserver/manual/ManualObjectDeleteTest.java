package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.DeleteObjectResult;
import com.crystal.mcp.sapserver.tool.DeletionTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual integration test for DeletionTools.delete_object().
 *
 * This test requires:
 * - Active SAP connection (configured via environment variables)
 * - User permissions to delete ABAP objects
 * - Valid transport request for the user
 * - Test object to delete (created for testing purposes)
 *
 * IMPORTANT:
 * - This test is @Disabled by default to prevent accidental execution
 * - Remove @Disabled annotation when running manually
 * - Use a test object in development system only (e.g., ZTEST_DELETE_ME)
 * - DO NOT run against production systems
 * - DO NOT delete critical objects
 *
 * Workflow Tested:
 * 1. Transport Check: Verify object exists and get metadata
 * 2. LOCK: Acquire lock on object
 * 3. DELETE: Remove object from SAP system
 * 4. UNLOCK: Release lock
 *
 * Test Scenarios:
 * Scenario 1: Delete function module
 * Scenario 2: Delete class
 * Scenario 3: Delete program
 * Scenario 4: Error handling - Object locked by another user
 * Scenario 5: Error handling - Object not found
 *
 * Reference: docs/requirements/mcp/workflow_based/pr_delete_object.md
 */
@Slf4j
@SpringBootTest
//@Disabled("Manual test - requires SAP connection, user permissions, and test object")
class ManualObjectDeleteTest {

    @Autowired
    private DeletionTools deletionTools;

    /**
     * Test Scenario 1: Delete a Function Module.
     *
     * BEFORE RUNNING:
     * 1. Create a test function module in your SAP system:
     *    - Function Group: ZTEST_FG
     *    - Function Module: Z_TEST_DELETE_FM
     *    - Package: $TMP (local) or your development package
     * 2. Update FUNCTION_MODULE_NAME and FUNCTION_GROUP_NAME if needed
     * 3. Update TRANSPORT_NUMBER with your transport or leave null for auto-assignment
     * 4. Remove @Disabled annotation from class
     *
     * AFTER RUNNING:
     * 1. Check SE37 to verify function module was deleted
     * 2. Check transport request to verify deletion was recorded
     * 3. Verify no orphaned locks (SM12)
     *
     * Expected Result:
     * - success = true
     * - transportNumber is populated
     * - Object no longer exists in SE37
     */
    @Test
    void testDeleteFunctionModule() {
        // ========================================
        // Configuration (UPDATE BEFORE RUNNING)
        // ========================================
        final String FUNCTION_MODULE_NAME = "ZFI_DMEE_COLPATRIA_R6";
        final String FUNCTION_GROUP_NAME = "ZFIDMEE_1";
        final String OBJECT_TYPE = "FUNC";
        final String TRANSPORT_NUMBER = null;  // Set to "CADK911122" or leave null for auto-assignment

        log.info("=== Manual Test: Delete Function Module ===");
        log.info("Function Module: {}", FUNCTION_MODULE_NAME);
        log.info("Function Group: {}", FUNCTION_GROUP_NAME);
        log.info("Transport: {}", TRANSPORT_NUMBER == null ? "Auto-assign" : TRANSPORT_NUMBER);

        // ========================================
        // Execute deletion workflow
        // ========================================
        log.info("Executing deletion workflow (Transport Check → LOCK → DELETE → UNLOCK)...");

        DeleteObjectResult result = deletionTools.delete_object(
                FUNCTION_MODULE_NAME,
                OBJECT_TYPE,
                FUNCTION_GROUP_NAME,
                TRANSPORT_NUMBER
        );

        // ========================================
        // Verify results
        // ========================================
        log.info("=== Deletion Result ===");
        log.info("Success: {}", result.isSuccess());
        log.info("Object Name: {}", result.getObjectName());
        log.info("Object Type: {}", result.getObjectType());
        log.info("Package: {}", result.getDevclass());
        log.info("Transport: {}", result.getTransportNumber());
        log.info("Transport User: {}", result.getTransportUser());
        log.info("Transport Description: {}", result.getTransportDescription());
        log.info("Object URI: {}", result.getObjectUri());

        if (!result.isSuccess()) {
            log.error("Error Message: {}", result.getErrorMessage());
            log.error("Error Details: {}", result.getErrorDetails());
        }

        // Assertions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObjectName()).isEqualTo(FUNCTION_MODULE_NAME);
        assertThat(result.getObjectType()).isEqualTo(OBJECT_TYPE);
        assertThat(result.getTransportNumber()).isNotEmpty();
        assertThat(result.getObjectUri()).contains(FUNCTION_GROUP_NAME.toLowerCase());
        assertThat(result.getObjectUri()).contains(FUNCTION_MODULE_NAME.toLowerCase());

        log.info("✅ Function module deleted successfully!");
        log.info("⚠️ MANUAL VERIFICATION REQUIRED:");
        log.info("   1. Check SE37 - function module should be deleted");
        log.info("   2. Check transport {} for deletion record", result.getTransportNumber());
        log.info("   3. Check SM12 - no orphaned locks");
    }

    /**
     * Test Scenario 2: Delete a Class.
     *
     * BEFORE RUNNING:
     * 1. Create a test class in your SAP system:
     *    - Class Name: ZCL_TEST_DELETE
     *    - Package: $TMP (local) or your development package
     * 2. Update CLASS_NAME if needed
     * 3. Update TRANSPORT_NUMBER with your transport or leave null for auto-assignment
     * 4. Remove @Disabled annotation from class
     *
     * AFTER RUNNING:
     * 1. Check SE24 to verify class was deleted
     * 2. Check transport request to verify deletion was recorded
     * 3. Verify no orphaned locks (SM12)
     *
     * Expected Result:
     * - success = true
     * - transportNumber is populated
     * - Object no longer exists in SE24
     */
    @Test
    void testDeleteClass() {
        // ========================================
        // Configuration (UPDATE BEFORE RUNNING)
        // ========================================
        final String CLASS_NAME = "ZCL_TEST_DELETE";
        final String OBJECT_TYPE = "CLAS";
        final String TRANSPORT_NUMBER = null;  // Set to "CADK911122" or leave null for auto-assignment

        log.info("=== Manual Test: Delete Class ===");
        log.info("Class: {}", CLASS_NAME);
        log.info("Transport: {}", TRANSPORT_NUMBER == null ? "Auto-assign" : TRANSPORT_NUMBER);

        // ========================================
        // Execute deletion workflow
        // ========================================
        log.info("Executing deletion workflow (Transport Check → LOCK → DELETE → UNLOCK)...");

        DeleteObjectResult result = deletionTools.delete_object(
                CLASS_NAME,
                OBJECT_TYPE,
                null,  // No function group for classes
                TRANSPORT_NUMBER
        );

        // ========================================
        // Verify results
        // ========================================
        log.info("=== Deletion Result ===");
        log.info("Success: {}", result.isSuccess());
        log.info("Object Name: {}", result.getObjectName());
        log.info("Object Type: {}", result.getObjectType());
        log.info("Package: {}", result.getDevclass());
        log.info("Transport: {}", result.getTransportNumber());
        log.info("Transport User: {}", result.getTransportUser());
        log.info("Object URI: {}", result.getObjectUri());

        if (!result.isSuccess()) {
            log.error("Error Message: {}", result.getErrorMessage());
            log.error("Error Details: {}", result.getErrorDetails());
        }

        // Assertions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObjectName()).isEqualTo(CLASS_NAME);
        assertThat(result.getObjectType()).isEqualTo(OBJECT_TYPE);
        assertThat(result.getTransportNumber()).isNotEmpty();
        assertThat(result.getObjectUri()).contains("/sap/bc/adt/oo/classes/");
        assertThat(result.getObjectUri()).contains(CLASS_NAME.toLowerCase());

        log.info("✅ Class deleted successfully!");
        log.info("⚠️ MANUAL VERIFICATION REQUIRED:");
        log.info("   1. Check SE24 - class should be deleted");
        log.info("   2. Check transport {} for deletion record", result.getTransportNumber());
        log.info("   3. Check SM12 - no orphaned locks");
    }

    /**
     * Test Scenario 3: Delete a Program.
     *
     * BEFORE RUNNING:
     * 1. Create a test program in your SAP system:
     *    - Program Name: ZTEST_DELETE_PROG
     *    - Package: $TMP (local) or your development package
     * 2. Update PROGRAM_NAME if needed
     * 3. Update TRANSPORT_NUMBER with your transport or leave null for auto-assignment
     * 4. Remove @Disabled annotation from class
     *
     * AFTER RUNNING:
     * 1. Check SE38 to verify program was deleted
     * 2. Check transport request to verify deletion was recorded
     * 3. Verify no orphaned locks (SM12)
     *
     * Expected Result:
     * - success = true
     * - transportNumber is populated
     * - Object no longer exists in SE38
     */
    @Test
    void testDeleteProgram() {
        // ========================================
        // Configuration (UPDATE BEFORE RUNNING)
        // ========================================
        final String PROGRAM_NAME = "ZTEST_DELETE_PROG";
        final String OBJECT_TYPE = "PROG";
        final String TRANSPORT_NUMBER = null;  // Set to "CADK911122" or leave null for auto-assignment

        log.info("=== Manual Test: Delete Program ===");
        log.info("Program: {}", PROGRAM_NAME);
        log.info("Transport: {}", TRANSPORT_NUMBER == null ? "Auto-assign" : TRANSPORT_NUMBER);

        // ========================================
        // Execute deletion workflow
        // ========================================
        log.info("Executing deletion workflow (Transport Check → LOCK → DELETE → UNLOCK)...");

        DeleteObjectResult result = deletionTools.delete_object(
                PROGRAM_NAME,
                OBJECT_TYPE,
                null,  // No function group for programs
                TRANSPORT_NUMBER
        );

        // ========================================
        // Verify results
        // ========================================
        log.info("=== Deletion Result ===");
        log.info("Success: {}", result.isSuccess());
        log.info("Object Name: {}", result.getObjectName());
        log.info("Object Type: {}", result.getObjectType());
        log.info("Package: {}", result.getDevclass());
        log.info("Transport: {}", result.getTransportNumber());
        log.info("Transport User: {}", result.getTransportUser());
        log.info("Object URI: {}", result.getObjectUri());

        if (!result.isSuccess()) {
            log.error("Error Message: {}", result.getErrorMessage());
            log.error("Error Details: {}", result.getErrorDetails());
        }

        // Assertions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObjectName()).isEqualTo(PROGRAM_NAME);
        assertThat(result.getObjectType()).isEqualTo(OBJECT_TYPE);
        assertThat(result.getTransportNumber()).isNotEmpty();
        assertThat(result.getObjectUri()).contains("/sap/bc/adt/programs/programs/");
        assertThat(result.getObjectUri()).contains(PROGRAM_NAME.toLowerCase());

        log.info("✅ Program deleted successfully!");
        log.info("⚠️ MANUAL VERIFICATION REQUIRED:");
        log.info("   1. Check SE38 - program should be deleted");
        log.info("   2. Check transport {} for deletion record", result.getTransportNumber());
        log.info("   3. Check SM12 - no orphaned locks");
    }

    /**
     * Test Scenario 4: Error Handling - Invalid Object Type.
     *
     * Tests validation of object type parameter.
     *
     * Expected Result:
     * - success = false
     * - errorMessage contains "Unsupported object type"
     */
    @Test
    void testDeleteObject_InvalidObjectType() {
        log.info("=== Manual Test: Error Handling - Invalid Object Type ===");

        DeleteObjectResult result = deletionTools.delete_object(
                "ZTEST_DUMMY",
                "INVALID_TYPE",
                null,
                null
        );

        log.info("=== Deletion Result ===");
        log.info("Success: {}", result.isSuccess());
        log.info("Error Message: {}", result.getErrorMessage());

        // Assertions
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unsupported object type");

        log.info("✅ Validation error handled correctly!");
    }

    /**
     * Test Scenario 5: Error Handling - Missing Function Group for FUNC type.
     *
     * Tests validation that functionGroupName is required for FUNC type.
     *
     * Expected Result:
     * - success = false
     * - errorMessage contains "functionGroupName is required"
     */
    @Test
    void testDeleteObject_MissingFunctionGroup() {
        log.info("=== Manual Test: Error Handling - Missing Function Group ===");

        DeleteObjectResult result = deletionTools.delete_object(
                "Z_TEST_FM",
                "FUNC",
                null,  // Missing function group
                null
        );

        log.info("=== Deletion Result ===");
        log.info("Success: {}", result.isSuccess());
        log.info("Error Message: {}", result.getErrorMessage());

        // Assertions
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("functionGroupName is required");

        log.info("✅ Validation error handled correctly!");
    }
}

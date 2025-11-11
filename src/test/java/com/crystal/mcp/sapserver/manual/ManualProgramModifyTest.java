package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.ProgramModifyResult;
import com.crystal.mcp.sapserver.service.ProgramService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual integration test for ProgramService.modifyProgramSource().
 *
 * This test requires:
 * - Active SAP connection (configured via environment variables)
 * - User permissions to modify ABAP objects
 * - Valid transport request for the user
 *
 * IMPORTANT:
 * - This test is @Disabled by default to prevent accidental execution
 * - Remove @Disabled annotation when running manually
 * - Use a test object (e.g., ZTEST_PROGRAM in $TMP or development system)
 * - DO NOT run against production systems
 *
 * Workflow Tested:
 * 1. LOCK: Acquire lock on program/include
 * 2. MODIFY: Update source code
 * 3. UNLOCK: Release lock
 *
 * Test Scenario:
 * - Modify a test program to add a comment line
 * - Verify workflow completes successfully
 * - Verify transport number is returned
 *
 * Reference: docs/requirements/mcp/workflow_based/pr_update_program.md
 */
@Slf4j
@SpringBootTest
@Disabled("Manual test - requires SAP connection and user permissions")
class ManualProgramModifyTest {

    @Autowired
    private ProgramService programService;

    /**
     * Test modifying a program.
     *
     * BEFORE RUNNING:
     * 1. Create a test program in your SAP system (e.g., ZTEST_MODIFY_PROGRAM)
     * 2. Ensure you have permissions to modify it
     * 3. Update PROGRAM_NAME constant below with your test program name
     * 4. Remove @Disabled annotation from class
     *
     * AFTER RUNNING:
     * 1. Check SE38/SE80 to verify the program was modified
     * 2. Check transport request to verify object was added
     * 3. Verify no orphaned locks (SM12)
     */
    @Test
    void testModifyProgram_Success() {
        // ========================================
        // Configuration (UPDATE BEFORE RUNNING)
        // ========================================
        final String PROGRAM_NAME = "ZTEST_MODIFY_PROGRAM";  // Change to your test program
        final String OBJECT_TYPE = "program";

        log.info("=== Manual Test: Modify Program ===");
        log.info("Program: {}", PROGRAM_NAME);

        // ========================================
        // Step 1: Prepare new source code
        // ========================================
        String newSource = "REPORT " + PROGRAM_NAME.toLowerCase() + ".\n" +
                "\n" +
                "* Modified by manual test at " + java.time.LocalDateTime.now() + "\n" +
                "* This is a test modification\n" +
                "\n" +
                "WRITE: / 'Hello World - Modified Version'.\n" +
                "WRITE: / 'Test completed successfully'.\n";

        log.info("New source code prepared ({} bytes)", newSource.length());

        // ========================================
        // Step 2: Execute modification workflow
        // ========================================
        log.info("Executing modification workflow...");

        ProgramModifyResult result = programService.modifyProgramSource(
                PROGRAM_NAME,
                newSource,
                OBJECT_TYPE,
                null  // Let system assign transport
        );

        // ========================================
        // Step 3: Verify results
        // ========================================
        log.info("Workflow completed. Success: {}", result.isSuccess());
        log.info("Lock Handle: {}", result.getLockHandle());
        log.info("Transport: {}", result.getTransportNumber());
        log.info("Transport User: {}", result.getTransportUser());
        log.info("Transport Description: {}", result.getTransportDescription());
        log.info("Messages: {}", result.getMessages().size());

        result.getMessages().forEach(msg ->
                log.info("  [{}] {}: {}", msg.getStep(), msg.getType(), msg.getText())
        );

        // Assertions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLocked()).isTrue();
        assertThat(result.isModified()).isTrue();
        assertThat(result.isUnlocked()).isTrue();
        assertThat(result.getLockHandle()).isNotEmpty();
        assertThat(result.getTransportNumber()).isNotEmpty();
        assertThat(result.getObjectName()).isEqualTo(PROGRAM_NAME);
        assertThat(result.getObjectType()).isEqualTo(OBJECT_TYPE);

        log.info("=== Test Passed ===");
        log.info("NEXT STEPS:");
        log.info("1. Check SE38/SE80 to verify program source was updated");
        log.info("2. Check SE09/SE10 to verify transport: {}", result.getTransportNumber());
        log.info("3. Check SM12 to verify no orphaned locks");
    }

    /**
     * Test modifying an include.
     *
     * BEFORE RUNNING:
     * 1. Create a test program with a top include (e.g., ZTEST_MODIFY_PROGRAM with ZTEST_MODIFY_TOP)
     * 2. Ensure you have permissions to modify it
     * 3. Update INCLUDE_NAME constant below
     * 4. Remove @Disabled annotation from class
     */
    @Test
    void testModifyInclude_Success() {
        // ========================================
        // Configuration (UPDATE BEFORE RUNNING)
        // ========================================
        final String INCLUDE_NAME = "ZTEST_MODIFY_TOP";  // Change to your test include
        final String OBJECT_TYPE = "include";

        log.info("=== Manual Test: Modify Include ===");
        log.info("Include: {}", INCLUDE_NAME);

        // ========================================
        // Step 1: Prepare new source code
        // ========================================
        String newSource = "* Top include for test program\n" +
                "* Modified by manual test at " + java.time.LocalDateTime.now() + "\n" +
                "\n" +
                "DATA: gv_test TYPE string VALUE 'Modified version'.\n";

        log.info("New source code prepared ({} bytes)", newSource.length());

        // ========================================
        // Step 2: Execute modification workflow
        // ========================================
        log.info("Executing modification workflow...");

        ProgramModifyResult result = programService.modifyProgramSource(
                INCLUDE_NAME,
                newSource,
                OBJECT_TYPE,
                null  // Let system assign transport
        );

        // ========================================
        // Step 3: Verify results
        // ========================================
        log.info("Workflow completed. Success: {}", result.isSuccess());
        log.info("Transport: {}", result.getTransportNumber());

        // Assertions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLocked()).isTrue();
        assertThat(result.isModified()).isTrue();
        assertThat(result.isUnlocked()).isTrue();

        log.info("=== Test Passed ===");
    }

    /**
     * Test error handling when object is already locked.
     *
     * BEFORE RUNNING:
     * 1. Open a test program in SE38/SE80 (lock it manually)
     * 2. Update PROGRAM_NAME to the locked program
     * 3. Remove @Disabled annotation
     *
     * EXPECTED RESULT:
     * - Lock operation should fail with HTTP 409
     * - Error message should indicate object is already locked
     * - No orphaned locks should be created
     */
    @Test
    void testModifyProgram_AlreadyLocked() {
        final String PROGRAM_NAME = "ZTEST_LOCKED_PROGRAM";  // Lock this manually first
        final String OBJECT_TYPE = "program";

        log.info("=== Manual Test: Modify Locked Program ===");
        log.info("Program: {} (should be locked manually)", PROGRAM_NAME);

        String newSource = "REPORT " + PROGRAM_NAME.toLowerCase() + ".\nWRITE: / 'Test'.\n";

        try {
            ProgramModifyResult result = programService.modifyProgramSource(
                    PROGRAM_NAME,
                    newSource,
                    OBJECT_TYPE,
                    null
            );

            // Should not reach here if object is locked
            log.error("FAIL: Expected exception for locked object, but got success");
            assertThat(result.isSuccess()).isFalse();

        } catch (RuntimeException e) {
            log.info("Expected exception caught: {}", e.getMessage());
            assertThat(e.getMessage()).contains("locked");
            log.info("=== Test Passed ===");
        }
    }
}

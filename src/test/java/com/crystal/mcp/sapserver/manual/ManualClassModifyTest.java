package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.ClassModifyResult;
import com.crystal.mcp.sapserver.service.ClassService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual integration test for ClassService.modifyClass().
 *
 * This test requires:
 * - Active SAP connection (configured via environment variables)
 * - User permissions to modify ABAP objects
 * - Valid transport request for the user
 *
 * IMPORTANT:
 * - This test is @Disabled by default to prevent accidental execution
 * - Remove @Disabled annotation when running manually
 * - Use a test object (e.g., ZCLFIAAC002_CARGA_ACTIVOS_FIJ in development system)
 * - DO NOT run against production systems
 *
 * Workflow Tested:
 * 1. LOCK: Acquire lock on class
 * 2. MODIFY: Update source code (remove cumulatedvalues parameter from BAPI call)
 * 3. UNLOCK: Release lock
 *
 * Test Scenario:
 * - Modify ZCLFIAAC002_CARGA_ACTIVOS_FIJ class
 * - Remove cumulatedvalues parameter from ejecutar_bapi_crear method
 * - Verify workflow completes successfully
 * - Verify transport number is returned
 *
 * Reference: docs/requirements/mcp/workflow_based/pr_class_modify.md
 */
@Slf4j
@SpringBootTest
//@Disabled("Manual test - requires SAP connection and user permissions")
class ManualClassModifyTest {

    @Autowired
    private ClassService classService;

    /**
     * Test modifying ZCLFIAAC002_CARGA_ACTIVOS_FIJ class - implementation include.
     *
     * BEFORE RUNNING:
     * 1. Ensure class ZCLFIAAC002_CARGA_ACTIVOS_FIJ exists in your SAP system
     * 2. Ensure you have permissions to modify it
     * 3. Update CLASS_NAME constant if needed
     * 4. Update TRANSPORT_NUMBER with your transport or leave null for auto-assignment
     * 5. Remove @Disabled annotation from class
     *
     * AFTER RUNNING:
     * 1. Check SE24/SE80 to verify the class was modified
     * 2. Verify the cumulatedvalues line is commented in ejecutar_bapi_crear method
     * 3. Check transport request to verify object was added
     * 4. Verify no orphaned locks (SM12)
     */
    @Test
    void testModifyClass_RemoveCumulatedValues() {
        // ========================================
        // Configuration (UPDATE BEFORE RUNNING)
        // ========================================
        final String CLASS_NAME = "ZCLFIAAC002_CARGA_ACTIVOS_FIJ";
        final String INCLUDE_TYPE = "implementation";
        final String TRANSPORT_NUMBER = null;  // Set to "CADK911122" or leave null for auto-assignment

        log.info("=== Manual Test: Modify Class - Remove cumulatedvalues Parameter ===");
        log.info("Class: {}", CLASS_NAME);
        log.info("Include Type: {}", INCLUDE_TYPE);
        log.info("Transport: {}", TRANSPORT_NUMBER == null ? "Auto-assign" : TRANSPORT_NUMBER);

        // ========================================
        // Step 1: Prepare new source code
        // ========================================
        String newSource = getModifiedClassSource();

        log.info("New source code prepared ({} bytes)", newSource.length());
        log.info("Modification: Commented cumulatedvalues parameter in BAPI_FIXEDASSET_OVRTAKE_CREATE");

        // ========================================
        // Step 2: Execute modification workflow
        // ========================================
        log.info("Executing modification workflow (LOCK → MODIFY → UNLOCK)...");

        ClassModifyResult result = classService.modifyClass(
                CLASS_NAME,
                newSource,
                INCLUDE_TYPE,
                TRANSPORT_NUMBER  // null = auto-assign
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
        assertThat(result.getClassName()).isEqualTo(CLASS_NAME);
        assertThat(result.getIncludeType()).isEqualTo(INCLUDE_TYPE);

        log.info("=== Test Passed ===");
        log.info("NEXT STEPS:");
        log.info("1. Check SE24/SE80 to verify class source was updated");
        log.info("2. Search for 'ELIMINADO - No enviar' comment in ejecutar_bapi_crear method");
        log.info("3. Check SE09/SE10 to verify transport: {}", result.getTransportNumber());
        log.info("4. Check SM12 to verify no orphaned locks");
        log.info("5. Test the class functionality to ensure BAPI call works without cumulatedvalues");
    }

    /**
     * Test modifying class definition (main include).
     */
    @Test
    void testModifyClass_Definition() {
        final String CLASS_NAME = "ZCLFIAAC002_CARGA_ACTIVOS_FIJ";
        final String INCLUDE_TYPE = "main";

        log.info("=== Manual Test: Modify Class Definition ===");
        log.info("Class: {}", CLASS_NAME);

        // Simple modification: Add a comment to class definition
        String newSource = "CLASS zclfiaac002_carga_activos_fij DEFINITION\n" +
                "  PUBLIC\n" +
                "  FINAL\n" +
                "  CREATE PUBLIC .\n" +
                "\n" +
                "  PUBLIC SECTION.\n" +
                "    \"* Modified by manual test at " + java.time.LocalDateTime.now() + "\n" +
                "    CONSTANTS: gc_test TYPE string VALUE 'TEST'.\n" +
                "ENDCLASS.\n";

        ClassModifyResult result = classService.modifyClass(
                CLASS_NAME,
                newSource,
                INCLUDE_TYPE,
                null
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLocked()).isTrue();
        assertThat(result.isModified()).isTrue();
        assertThat(result.isUnlocked()).isTrue();

        log.info("=== Test Passed ===");
    }

    /**
     * Test error handling when class is already locked.
     *
     * BEFORE RUNNING:
     * 1. Open ZCLFIAAC002_CARGA_ACTIVOS_FIJ in SE24/SE80 (lock it manually)
     * 2. Remove @Disabled annotation
     *
     * EXPECTED RESULT:
     * - Lock operation should fail with HTTP 423
     * - Error message should indicate object is already locked
     * - No orphaned locks should be created
     */
    @Test
    void testModifyClass_AlreadyLocked() {
        final String CLASS_NAME = "ZCLFIAAC002_CARGA_ACTIVOS_FIJ";
        final String INCLUDE_TYPE = "implementation";

        log.info("=== Manual Test: Modify Locked Class ===");
        log.info("Class: {} (should be locked manually)", CLASS_NAME);

        String newSource = getModifiedClassSource();

        try {
            ClassModifyResult result = classService.modifyClass(
                    CLASS_NAME,
                    newSource,
                    INCLUDE_TYPE,
                    null
            );

            // Should not reach here if object is locked
            log.error("FAIL: Expected exception for locked object, but got success");
            assertThat(result.isSuccess()).isFalse();

        } catch (RuntimeException e) {
            log.info("Expected exception caught: {}", e.getMessage());
            assertThat(e.getMessage()).containsIgnoringCase("locked");
            log.info("=== Test Passed ===");
        }
    }

    /**
     * Helper method: Returns the modified class source code.
     *
     * This is the complete implementation with cumulatedvalues parameter commented out.
     */
    private String getModifiedClassSource() {
        return """
CLASS zclfiaac002_carga_activos_fij IMPLEMENTATION.

  METHOD ejecutar_bapi_crear.
    CALL FUNCTION 'BAPI_FIXEDASSET_OVRTAKE_CREATE'
      EXPORTING
        key                 = i_es_key
*       reference           =
        createsubnumber     = i_createsubnumber
        creategroupasset    = i_creategroupasset
        testrun             = i_testrun
        generaldata         = i_es_generaldata
        generaldatax        = i_es_generaldatax
        inventory           = i_es_inventory
        inventoryx          = i_es_inventoryx
        postinginformation  = i_es_postinginformation
        postinginformationx = i_es_postinginformationx
        timedependentdata   = i_es_timedependentdata
        timedependentdatax  = i_es_timedependentdatax
        allocations         = i_es_allocations
        allocationsx        = i_es_allocationsx
        origin              = i_es_origin
        originx             = i_es_originx
      IMPORTING
        companycode         = e_companycode
        asset               = e_asset
        subnumber           = e_subnumber
        assetcreated        = e_assetcreated
      TABLES
        depreciationareas   = i_ti_depreciationareas
        depreciationareasx  = i_ti_depreciationareasx
        "cumulatedvalues     = i_ti_cumulatedvalues "ELIMINADO - No enviar para cargas iniciales
        "postedvalues        = i_ti_postedvalues "NO APLICA PARA CARGAS INICIALES
        return              = e_ti_return.
  ENDMETHOD.

ENDCLASS.
                """.trim();
    }
}

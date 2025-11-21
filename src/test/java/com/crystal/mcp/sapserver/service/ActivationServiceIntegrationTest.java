package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ActivationResult;
import com.crystal.mcp.sapserver.model.InactiveObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ActivationService.
 * Requires live SAP connection via environment variables.
 */
@SpringBootTest
class ActivationServiceIntegrationTest {

    @Autowired
    private ActivationService activationService;

    @Test
    void testGetInactiveObjects_Success() {
        // When
        List<InactiveObject> objects = activationService.getInactiveObjects();

        // Then
        assertNotNull(objects);
        System.out.println("Found " + objects.size() + " inactive objects");

        // If there are inactive objects, validate structure
        if (!objects.isEmpty()) {
            InactiveObject first = objects.get(0);
            assertNotNull(first.uri(), "Object URI should not be null");
            assertNotNull(first.type(), "Object type should not be null");
            assertNotNull(first.name(), "Object name should not be null");

            System.out.println("Sample inactive object:");
            System.out.println("  URI: " + first.uri());
            System.out.println("  Type: " + first.type());
            System.out.println("  Name: " + first.name());
            System.out.println("  Description: " + first.description());
            System.out.println("  User: " + first.user());
            System.out.println("  Deleted: " + first.deleted());

            if (first.transport() != null) {
                System.out.println("  Transport: " + first.transport().name());
                System.out.println("  Transport User: " + first.transport().user());
            }
        }
    }

    @Test
    void testActivateObjects_WithInvalidObject_ExpectErrors() {
        // Given - URI that doesn't exist or has syntax errors
        String invalidUri = "/sap/bc/adt/oo/classes/zcl_nonexistent_test_class_12345";
        List<String> uris = List.of(invalidUri);

        // When
        ActivationResult result = activationService.activateObjects(uris);

        // Then
        assertNotNull(result);
        System.out.println("Activation result: " + result.success());
        System.out.println("Message: " + result.message());
        System.out.println("Errors: " + result.errors().size());

        if (!result.errors().isEmpty()) {
            result.errors().forEach(error -> {
                System.out.println("  Error:");
                System.out.println("    Object: " + error.objectDescription());
                System.out.println("    Type: " + error.type());
                System.out.println("    Line: " + error.line());
                System.out.println("    Message: " + error.shortText());
            });
        }
    }

    @Test
    void testCheckAndActivate_AlreadyActive() {
        // Given - URI of a standard active class
        String activeClassUri = "/sap/bc/adt/oo/classes/cl_abap_char_utilities";

        // When
        // Note: checkAndActivate now directly activates without checking inactive status
        // This is the correct Eclipse ADT workflow: LOCK → MODIFY → UNLOCK → ACTIVATE
        ActivationResult result = activationService.checkAndActivate(activeClassUri);

        // Then
        assertNotNull(result);
        // Result can be success (if object doesn't need activation) or failure (if no metadata)
        // Expected: No objects found to activate (empty metadata from preaudit)
        assertFalse(result.success());
        assertTrue(result.message().contains("No objects found to activate"));
        assertTrue(result.errors().isEmpty());

        System.out.println("Check and activate result: " + result.message());
    }

    @Test
    void testActivationWorkflow_Integration() {
        // This test demonstrates the full workflow:
        // 1. Get inactive objects
        // 2. If any exist, try to activate them
        // 3. Verify results

        // Step 1: Get inactive objects
        List<InactiveObject> inactiveObjects = activationService.getInactiveObjects();
        System.out.println("Step 1: Found " + inactiveObjects.size() + " inactive objects");

        if (inactiveObjects.isEmpty()) {
            System.out.println("No inactive objects to test with - test passed (no work needed)");
            return;
        }

        // Step 2: Take first inactive object and try to activate
        InactiveObject firstObject = inactiveObjects.get(0);
        System.out.println("Step 2: Attempting to activate: " + firstObject.name());

        // Step 3: Activate
        ActivationResult result = activationService.activateObjects(List.of(firstObject.uri()));

        // Step 4: Verify
        assertNotNull(result);
        System.out.println("Step 3: Activation result:");
        System.out.println("  Success: " + result.success());
        System.out.println("  Message: " + result.message());
        System.out.println("  Errors: " + result.errors().size());

        if (!result.success()) {
            System.out.println("Note: Activation failed - likely due to syntax errors or dependencies");
            result.errors().forEach(error ->
                System.out.println("    - " + error.shortText() + " (line " + error.line() + ")")
            );
        }
    }
}

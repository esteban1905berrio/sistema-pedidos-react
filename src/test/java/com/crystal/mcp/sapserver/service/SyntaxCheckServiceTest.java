package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.SyntaxCheckResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SyntaxCheckService.
 *
 * Tests syntax checking functionality against a real SAP system.
 * Requires SAP connection configured via environment variables.
 *
 * Test Strategy:
 * 1. Check syntax of known good object (should pass)
 * 2. Check syntax of object with errors (should fail)
 * 3. Verify message parsing (line, column, type)
 */
@SpringBootTest
class SyntaxCheckServiceTest {

    @Autowired
    private SyntaxCheckService syntaxCheckService;

    /**
     * Test syntax check of a standard SAP class (should pass).
     */
    @Test
    void testCheckSyntax_ValidClass() {
        // Given: Standard SAP class with valid syntax
        String objectUri = "/sap/bc/adt/oo/classes/cl_abap_char_utilities/source/main";
        String version = "active";

        // When
        SyntaxCheckResult result = syntaxCheckService.checkSyntax(objectUri, version);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(objectUri, result.objectUri(), "Object URI should match");
        assertEquals(version, result.version(), "Version should match");
        assertEquals("processed", result.status(), "Status should be 'processed'");
        assertNotNull(result.messages(), "Messages list should not be null");
        assertTrue(result.isPassed(), "Standard SAP class should pass syntax check");
        assertFalse(result.hasErrors(), "Should not have errors");

        // Log summary
        System.out.println("✅ Syntax check passed: " + result.getSummary());
    }

    /**
     * Test syntax check with inactive version.
     *
     * This test uses the function module from the transport info implementation
     * which should exist in the SAP system if previous tests ran successfully.
     */
    @Test
    void testCheckSyntax_InactiveVersion() {
        // Given: Function module that was created in previous tests
        String objectUri = "/sap/bc/adt/functions/groups/zgfcx_1/fmodules/z_cx_get_transport_info";
        String version = "inactive";

        // When
        SyntaxCheckResult result = syntaxCheckService.checkSyntax(objectUri, version);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(objectUri, result.objectUri(), "Object URI should match");
        assertEquals(version, result.version(), "Version should match");
        assertEquals("processed", result.status(), "Status should be 'processed'");
        assertNotNull(result.messages(), "Messages list should not be null");

        // Log results
        System.out.println("\n📋 Syntax Check Results:");
        System.out.println("Status: " + result.status());
        System.out.println("Summary: " + result.getSummary());
        System.out.println("Total messages: " + result.totalMessages());

        if (result.totalMessages() > 0) {
            System.out.println("\nMessages:");
            result.messages().forEach(msg -> {
                String icon = msg.isError() ? "❌" : msg.isWarning() ? "⚠️" : "ℹ️";
                System.out.printf("%s [%s] %s - %s%n",
                    icon, msg.type(), msg.getLocation(), msg.shortText());
            });
        }
    }

    /**
     * Test message parsing (line and column extraction).
     */
    @Test
    void testCheckMessage_LocationParsing() {
        // Given: Message with location
        SyntaxCheckResult.CheckMessage message = new SyntaxCheckResult.CheckMessage(
            "/sap/bc/adt/oo/classes/zcl_test/source/main#start=10,5",
            "E",
            "Syntax error: Unexpected token",
            10,
            5
        );

        // Then
        assertTrue(message.isError(), "Should be an error");
        assertFalse(message.isWarning(), "Should not be a warning");
        assertFalse(message.isInfo(), "Should not be info");
        assertEquals("Line 10, Column 5", message.getLocation(), "Location should be formatted");
    }

    /**
     * Test message types (Error, Warning, Info).
     */
    @Test
    void testCheckMessage_Types() {
        // Given: Different message types
        SyntaxCheckResult.CheckMessage error = new SyntaxCheckResult.CheckMessage(
            "/test", "E", "Error message", 1, 1
        );
        SyntaxCheckResult.CheckMessage warning = new SyntaxCheckResult.CheckMessage(
            "/test", "W", "Warning message", 2, 2
        );
        SyntaxCheckResult.CheckMessage info = new SyntaxCheckResult.CheckMessage(
            "/test", "I", "Info message", 3, 3
        );

        // Then
        assertTrue(error.isError());
        assertFalse(error.isWarning());
        assertFalse(error.isInfo());

        assertFalse(warning.isError());
        assertTrue(warning.isWarning());
        assertFalse(warning.isInfo());

        assertFalse(info.isError());
        assertFalse(info.isWarning());
        assertTrue(info.isInfo());
    }

    /**
     * Test result summary generation.
     */
    @Test
    void testSyntaxCheckResult_Summary() {
        // Given: Result with errors and warnings
        SyntaxCheckResult.CheckMessage error1 = new SyntaxCheckResult.CheckMessage(
            "/test", "E", "Error 1", 1, 1
        );
        SyntaxCheckResult.CheckMessage error2 = new SyntaxCheckResult.CheckMessage(
            "/test", "E", "Error 2", 2, 2
        );
        SyntaxCheckResult.CheckMessage warning = new SyntaxCheckResult.CheckMessage(
            "/test", "W", "Warning 1", 3, 3
        );

        SyntaxCheckResult result = new SyntaxCheckResult(
            "/test/uri",
            "inactive",
            "processed",
            "Test status",
            java.util.List.of(error1, error2, warning),
            true,  // hasErrors
            true,  // hasWarnings
            3      // totalMessages
        );

        // Then
        assertFalse(result.isPassed(), "Should not pass with errors");
        assertEquals("2 error(s), 1 warning(s)", result.getSummary());
    }

    /**
     * Test result with no issues.
     */
    @Test
    void testSyntaxCheckResult_NoIssues() {
        // Given: Result with no errors or warnings
        SyntaxCheckResult result = new SyntaxCheckResult(
            "/test/uri",
            "active",
            "processed",
            "Test status",
            java.util.List.of(),
            false,  // hasErrors
            false,  // hasWarnings
            0       // totalMessages
        );

        // Then
        assertTrue(result.isPassed(), "Should pass with no issues");
        assertEquals("Syntax check passed - no issues found", result.getSummary());
    }
}

package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.SyntaxCheckResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual test for SyntaxCheckService.
 * Tests syntax checking on a real ABAP class.
 */
@SpringBootTest
class SyntaxCheckManualTest {

    @Autowired
    private SyntaxCheckService syntaxCheckService;

    @Test
    void testCheckSyntax_ZclFiaae001AmpliacionAs() {
        // Given
        String classUri = "/sap/bc/adt/oo/classes/zclfiaae001_ampliacion_as/source/main";
        String version = null; // Use default: inactive (recommended)

        // When
        System.out.println("=".repeat(80));
        System.out.println("Testing syntax check for: " + classUri);
        System.out.println("Expected version: inactive (default)");
        System.out.println("=".repeat(80));

        SyntaxCheckResult result = syntaxCheckService.checkSyntax(classUri, version);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.status(), "Status should not be null");

        System.out.println("\nRESULT:");
        System.out.println("  Object URI: " + result.objectUri());
        System.out.println("  Version: " + result.version());
        System.out.println("  Status: " + result.status());
        System.out.println("  Status Text: " + result.statusText());
        System.out.println("  Total Messages: " + result.totalMessages());
        System.out.println("  Has Errors: " + result.hasErrors());
        System.out.println("  Has Warnings: " + result.hasWarnings());
        System.out.println("  Summary: " + result.getSummary());

        if (!result.messages().isEmpty()) {
            System.out.println("\nMESSAGES:");
            for (SyntaxCheckResult.CheckMessage msg : result.messages()) {
                System.out.println(String.format("  [%s] Line %s, Col %s: %s",
                    msg.type(),
                    msg.line() != null ? msg.line() : "?",
                    msg.column() != null ? msg.column() : "?",
                    msg.shortText()
                ));
            }
        } else {
            System.out.println("\n✓ No syntax errors or warnings found!");
        }

        System.out.println("=".repeat(80));
    }
}

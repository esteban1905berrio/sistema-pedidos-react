package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.BadiImplementationResult;
import com.crystal.mcp.sapserver.model.BadiSearchResult;
import com.crystal.mcp.sapserver.model.DmeeSearchResult;
import com.crystal.mcp.sapserver.model.DmeeTreeResult;
import com.crystal.mcp.sapserver.model.EnhancementSearchResult;
import com.crystal.mcp.sapserver.model.EnhancementSourceResult;
import com.crystal.mcp.sapserver.model.ExtractionDiscovery;
import com.crystal.mcp.sapserver.model.ExtractionScope;
import com.crystal.mcp.sapserver.service.AbapExtractionService;
import com.crystal.mcp.sapserver.service.BadiService;
import com.crystal.mcp.sapserver.service.DmeeService;
import com.crystal.mcp.sapserver.service.EnhancementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Consolidated manual test for ABAP Ripper Tool services.
 *
 * Tests the following services added in Phases 3-6:
 * - EnhancementService (Phase 3): get_enhancement_source
 * - BadiService (Phase 4): get_badi_implementation
 * - DmeeService (Phase 5): get_dmee_tree
 * - AbapExtractionService (Phase 6): discover_extraction_objects
 *
 * This test uses the CommandLineRunner pattern for isolated execution,
 * avoiding JUnit's tendency to run all methods when targeting specific ones.
 *
 * Usage (test all services):
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 *
 * Usage (test specific service):
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="enhancement ZENH_NAME"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="badi ZBADI_IMPL_NAME"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="dmee PAYM ZTREE_ID"
 *
 * Usage (test wildcard search):
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="search-enhancement Z*"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="search-badi Z*"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="search-dmee PAYM Z*"
 *
 * Usage (test extraction discovery - Phase 6):
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="extract-user L_ABAPS_ITA"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="extract-package ZCX"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="extract-transport CADK911088"
 * mvn spring-boot:run
 * -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualAbapRipperToolsTest
 * -Dspring-boot.run.arguments="extract-list ZCL_TEST,ZREP_INVOICE"
 *
 * Prerequisites:
 * - SAP connection configured via environment variables
 * - Objects must exist in the target SAP system
 * - Required FMs installed: ZCX_GET_ENHANCEMENT_SOURCE, ZCX_UTIL_GET_BADI_IMPL,
 * ZCX_UTIL_GET_DMEE_TREE
 * - For Phase 6: Z_CX_GET_PACKAGE_OBJECTS, Z_CX_GET_PACKAGE_HIERARCHY,
 * Z_CX_GET_TRANSPORT_OBJECTS
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualAbapRipperToolsTest implements CommandLineRunner {

    @Autowired
    private EnhancementService enhancementService;

    @Autowired
    private BadiService badiService;

    @Autowired
    private DmeeService dmeeService;

    @Autowired
    private AbapExtractionService extractionService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualAbapRipperToolsTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     Manual Test: ABAP Ripper Tools (Phases 3, 4, 5, 6)       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Services: Enhancement, BAdI, DMEE, Extraction               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        int testsPassed = 0;
        int testsFailed = 0;

        try {
            // Determine which tests to run based on arguments
            if (args.length == 0) {
                // Run all tests with default values
                System.out.println("📋 Running ALL tests with default/sample values...");
                System.out.println();

                // Test Enhancement Service
                // if (testEnhancementService("Z*")) {
                // testsPassed++;
                // } else {
                // testsFailed++;
                // }

                // Test BAdI Service
                // if (testBadiService("Z*")) {
                // testsPassed++;
                // } else {
                // testsFailed++;
                // }

                // Test DMEE Service
                // if (testDmeeService("PAYM", "ZFI_TRANSF_PAGO_BANCO_ITAU")) {
                // testsPassed++;
                // } else {
                // testsFailed++;
                // }

            } else {
                // Run specific test based on first argument
                String testType = args[0].toLowerCase();

                /*
                 * switch (testType) {
                 * case "enhancement":
                 * String enhName = args.length > 1 ? args[1] : null;
                 * if (testEnhancementService(enhName)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "badi":
                 * String badiName = args.length > 1 ? args[1] : null;
                 * if (testBadiService(badiName)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "dmee":
                 * String treeType = args.length > 1 ? args[1] : null;
                 * String treeId = args.length > 2 ? args[2] : null;
                 * if (testDmeeService(treeType, treeId)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "search-enhancement":
                 * String enhPattern = args.length > 1 ? args[1] : "Z*";
                 * if (testEnhancementSearch(enhPattern)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "search-badi":
                 * String badiPattern = args.length > 1 ? args[1] : "Z*";
                 * if (testBadiSearch(badiPattern)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "search-dmee":
                 * String dmeeType = args.length > 1 ? args[1] : "PAYM";
                 * String dmeePattern = args.length > 2 ? args[2] : "Z*";
                 * if (testDmeeSearch(dmeeType, dmeePattern)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * // Phase 6: Extraction Discovery
                 * case "extract-user":
                 * String userName = args.length > 1 ? args[1] : null;
                 * if (testExtractionUser(userName)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "extract-package":
                 * String packageNames = args.length > 1 ? args[1] : null;
                 * if (testExtractionPackage(packageNames)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "extract-transport":
                 * String transportNumbers = args.length > 1 ? args[1] : null;
                 * if (testExtractionTransport(transportNumbers)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * case "extract-list":
                 * String objectNames = args.length > 1 ? args[1] : null;
                 * if (testExtractionList(objectNames)) {
                 * testsPassed++;
                 * } else {
                 * testsFailed++;
                 * }
                 * break;
                 * 
                 * default:
                 * System.err.println("❌ Unknown test type: " + testType);
                 * System.err.
                 * println("   Valid options: enhancement, badi, dmee, search-enhancement, search-badi, search-dmee"
                 * );
                 * System.err.
                 * println("   Phase 6: extract-user, extract-package, extract-transport, extract-list"
                 * );
                 * System.exit(1);
                 * }
                 */
            }

            // Print summary
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            if (testsFailed == 0) {
                System.out.println("✅ ALL TESTS COMPLETED: " + testsPassed + " passed, " + testsFailed + " failed");
            } else {
                System.out.println("⚠️  TESTS COMPLETED: " + testsPassed + " passed, " + testsFailed + " failed");
            }
            System.out.println("═══════════════════════════════════════════════════════════════");

            if (testsFailed > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("═══════════════════════════════════════════════════════════════");
            System.err.println("❌ FATAL ERROR: " + e.getMessage());
            System.err.println("═══════════════════════════════════════════════════════════════");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test EnhancementService.getEnhancementSource()
     *
     * @param enhancementName optional enhancement name, uses search if null
     * @return true if test passed
     */
    private boolean testEnhancementService(String enhancementName) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: EnhancementService.getEnhancementSource()              │");
        System.out.println("│       Phase 3 - FM: ZCX_GET_ENHANCEMENT_SOURCE               │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            // Use provided name or a sample (user must provide valid enhancement name)
            if (enhancementName == null || enhancementName.isBlank()) {
                System.out.println("⚠️  No enhancement name provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"enhancement ZENH_YOUR_NAME\"");
                System.out.println("   Skipping test (requires valid enhancement name).");
                System.out.println();
                return true; // Skip gracefully
            }

            System.out.printf("   Enhancement: %s%n", enhancementName);
            System.out.println();

            long startTime = System.currentTimeMillis();

            EnhancementSourceResult result = enhancementService.getEnhancementSource(
                    enhancementName,
                    "00000");

            long duration = System.currentTimeMillis() - startTime;

            // Validate result
            assert result != null : "Result should not be null";
            assert result.enhancementName() != null : "Enhancement name should not be null";

            // Print results
            System.out.println("📋 RESULT:");
            System.out.println("   Enhancement:  " + result.enhancementName());
            System.out.println("   Duration:     " + duration + " ms");
            System.out.println("   Metadata:     " + result.metadata());

            if (result.header() != null) {
                System.out.println();
                System.out.println("📄 HEADER:");
                System.out.println("   Description:  " + result.header().description());
                System.out.println(
                        "   Tool Type:    " + result.header().toolType() + " (" + result.header().toolTypeText() + ")");
                System.out.println("   Package:      " + result.header().devclass());
                System.out.println("   Author:       " + result.header().author());
                System.out.println("   Created:      " + result.header().createdOn());
            }

            if (result.elements() != null && !result.elements().isEmpty()) {
                System.out.println();
                System.out.println("📦 ELEMENTS (" + result.elements().size() + "):");
                for (var elem : result.elements()) {
                    System.out.println("   - Type: " + elem.elementType() +
                            ", Spot: " + elem.spotName() +
                            ", Active: " + elem.active());
                    if (elem.sourceCode() != null && !elem.sourceCode().isBlank()) {
                        System.out.println("     Source Code Length: " + elem.sourceCode().length());
                    }
                }
            }

            System.out.println();
            System.out.println("✅ Test PASSED: EnhancementService.getEnhancementSource()");
            System.out.println();
            return true;

        } catch (

        Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            System.err.println();
            return false;
        }
    }

    /**
     * Test BadiService.getBadiImplementation()
     *
     * @param implementationName optional BAdI implementation name
     * @return true if test passed
     */
    private boolean testBadiService(String implementationName) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: BadiService.getBadiImplementation()                    │");
        System.out.println("│       Phase 4 - FM: ZCX_UTIL_GET_BADI_IMPL                   │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            // Use provided name or require user to provide one
            if (implementationName == null || implementationName.isBlank()) {
                System.out.println("⚠️  No BAdI implementation name provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"badi ZBADI_IMPL_NAME\"");
                System.out.println("   Skipping test (requires valid BAdI implementation name).");
                System.out.println();
                return true; // Skip gracefully
            }

            System.out.printf("   Implementation: %s%n", implementationName);
            System.out.println();

            long startTime = System.currentTimeMillis();

            BadiImplementationResult result = badiService.getBadiImplementation(implementationName);

            long duration = System.currentTimeMillis() - startTime;

            // Validate result
            assert result != null : "Result should not be null";
            assert result.implementationName() != null : "Implementation name should not be null";

            // Print results
            System.out.println("📋 RESULT:");
            System.out.println("   Implementation: " + result.implementationName());
            System.out.println("   Duration:       " + duration + " ms");
            System.out.println("   Metadata:       " + result.metadata());

            if (result.header() != null) {
                System.out.println();
                System.out.println("📄 HEADER:");
                System.out.println("   Description:  " + result.header().description());
                System.out.println("   Active:       " + result.header().active());
                System.out.println("   Package:      " + result.header().devclass());
                System.out.println("   Author:       " + result.header().author());
                System.out.println("   Created:      " + result.header().createdOn());
            }

            if (result.badiDefinitions() != null && !result.badiDefinitions().isEmpty()) {
                System.out.println();
                System.out.println("📦 BADI DEFINITIONS (" + result.badiDefinitions().size() + "):");
                for (var badi : result.badiDefinitions()) {
                    System.out.println("   - BAdI: " + badi.badiName() +
                            " (" + badi.description() + ")");
                    System.out.println("     Filter: " + badi.filterValue() +
                            ", Multiple: " + badi.isMultipleUse() +
                            ", FilterDep: " + badi.isFilterDependent());
                    if (badi.interfaces() != null) {
                        System.out.println("     Interfaces: " + badi.interfaces());
                    }
                }
            }

            if (result.implementingClasses() != null && !result.implementingClasses().isEmpty()) {
                System.out.println();
                System.out.println("📝 IMPLEMENTING CLASSES (" + result.implementingClasses().size() + "):");
                for (var cls : result.implementingClasses()) {
                    System.out.println("   - " + cls.interfaceName() + " → " + cls.className());
                }
            }

            System.out.println();
            System.out.println("✅ Test PASSED: BadiService.getBadiImplementation()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            System.err.println();
            return false;
        }
    }

    /**
     * Test DmeeService.getDmeeTree()
     *
     * @param treeType optional tree type (default: PAYM)
     * @param treeId   optional tree ID
     * @return true if test passed
     */
    private boolean testDmeeService(String treeType, String treeId) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: DmeeService.getDmeeTree()                              │");
        System.out.println("│       Phase 5 - FM: ZCX_UTIL_GET_DMEE_TREE                   │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            // Default tree type is PAYM (payment formats)
            if (treeType == null || treeType.isBlank()) {
                treeType = "PAYM";
            }

            // Require tree ID from user
            if (treeId == null || treeId.isBlank()) {
                System.out.println("⚠️  No DMEE tree ID provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"dmee PAYM ZTREE_ID\"");
                System.out.println("   Skipping test (requires valid DMEE tree ID).");
                System.out.println();
                return true; // Skip gracefully
            }

            System.out.printf("   Tree Type: %s%n", treeType);
            System.out.printf("   Tree ID:   %s%n", treeId);
            System.out.println();

            long startTime = System.currentTimeMillis();

            DmeeTreeResult result = dmeeService.getDmeeTree(
                    treeType,
                    treeId,
                    null, // version (latest)
                    null // language (system default)
            );

            long duration = System.currentTimeMillis() - startTime;

            // Validate result
            assert result != null : "Result should not be null";
            assert result.treeType() != null : "Tree type should not be null";
            assert result.treeId() != null : "Tree ID should not be null";

            // Print results
            System.out.println("📋 RESULT:");
            System.out.println("   Tree Type: " + result.treeType());
            System.out.println("   Tree ID:   " + result.treeId());
            System.out.println("   Duration:  " + duration + " ms");
            System.out.println("   Metadata:  " + result.metadata());

            if (result.header() != null) {
                System.out.println();
                System.out.println("📄 HEADER:");
                System.out.println("   Description:    " + result.header().description());
                System.out.println("   Version:        " + result.header().version());
                System.out.println("   Release Flag:   " + result.header().releaseFlag());
                System.out.println("   Charset:        " + result.header().charset());
                System.out.println("   Param Struct:   " + result.header().paramStructure());
                System.out.println("   Created By:     " + result.header().createdBy());
                System.out.println("   Created On:     " + result.header().createdOn());
            }

            if (result.nodes() != null && !result.nodes().isEmpty()) {
                System.out.println();
                System.out.println("📦 TREE NODES (" + result.nodes().size() + "):");

                // Print first 10 nodes as sample
                int maxNodes = Math.min(result.nodes().size(), 10);
                for (int i = 0; i < maxNodes; i++) {
                    var node = result.nodes().get(i);
                    String indent = "  ".repeat(node.level());
                    System.out.println("   " + indent + "├─ " + node.nodeId() +
                            " [" + node.nodeType() + "] " + node.techName() +
                            (node.text().isBlank() ? "" : " - " + node.text()));
                }

                if (result.nodes().size() > 10) {
                    System.out.println("   ... and " + (result.nodes().size() - 10) + " more nodes");
                }

                // Count nodes with exit functions
                long nodesWithExit = result.nodes().stream()
                        .filter(n -> n.mappingExitFunction() != null && !n.mappingExitFunction().isBlank())
                        .count();
                if (nodesWithExit > 0) {
                    System.out.println();
                    System.out.println("   📍 Nodes with exit functions: " + nodesWithExit);
                }
            }

            System.out.println();
            System.out.println("✅ Test PASSED: DmeeService.getDmeeTree()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            System.err.println();
            return false;
        }
    }

    /**
     * Test EnhancementService.searchEnhancements() - Wildcard search
     *
     * @param pattern wildcard pattern (e.g., "Z*", "*INVOICE*")
     * @return true if test passed
     */
    private boolean testEnhancementSearch(String pattern) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: EnhancementService.searchEnhancements()                │");
        System.out.println("│       Wildcard Search - FM: ZCX_GET_ENHANCEMENT_SOURCE       │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            System.out.printf("   Pattern: %s%n", pattern);
            System.out.println();

            long startTime = System.currentTimeMillis();

            EnhancementSearchResult result = enhancementService.searchEnhancements(pattern, 50);

            long duration = System.currentTimeMillis() - startTime;

            // Validate result
            assert result != null : "Result should not be null";
            assert result.pattern() != null : "Pattern should not be null";

            // Print results
            System.out.println("📋 SEARCH RESULT:");
            System.out.println("   Pattern:      " + result.pattern());
            System.out.println("   Max Results:  " + result.maxResults());
            System.out.println("   Total Found:  " + result.totalFound());
            System.out.println("   Duration:     " + duration + " ms");

            if (result.results() != null && !result.results().isEmpty()) {
                System.out.println();
                System.out.println("📦 MATCHING ENHANCEMENTS (" + result.results().size() + "):");
                int maxDisplay = Math.min(result.results().size(), 10);
                for (int i = 0; i < maxDisplay; i++) {
                    var ref = result.results().get(i);
                    System.out.println("   - " + ref.enhancementName() +
                            " [" + ref.objectType() + "] " + ref.objectName());
                }
                if (result.results().size() > 10) {
                    System.out.println("   ... and " + (result.results().size() - 10) + " more");
                }
            } else {
                System.out.println();
                System.out.println("⚠️  No enhancements found matching pattern: " + pattern);
            }

            System.out.println();
            System.out.println("✅ Test PASSED: EnhancementService.searchEnhancements()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            System.err.println();
            return false;
        }
    }

    /**
     * Test BadiService.searchBadiImplementations() - Wildcard search
     *
     * @param pattern wildcard pattern (e.g., "Z*", "*BADI*")
     * @return true if test passed
     */
    private boolean testBadiSearch(String pattern) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: BadiService.searchBadiImplementations()                │");
        System.out.println("│       Wildcard Search - FM: ZCX_UTIL_GET_BADI_IMPL           │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            System.out.printf("   Pattern: %s%n", pattern);
            System.out.println();

            long startTime = System.currentTimeMillis();

            BadiSearchResult result = badiService.searchBadiImplementations(pattern, 50);

            long duration = System.currentTimeMillis() - startTime;

            // Validate result
            assert result != null : "Result should not be null";
            assert result.pattern() != null : "Pattern should not be null";

            // Print results
            System.out.println("📋 SEARCH RESULT:");
            System.out.println("   Pattern:      " + result.pattern());
            System.out.println("   Max Results:  " + result.maxResults());
            System.out.println("   Total Found:  " + result.totalFound());
            System.out.println("   Duration:     " + duration + " ms");

            if (result.results() != null && !result.results().isEmpty()) {
                System.out.println();
                System.out.println("📦 MATCHING BADI IMPLEMENTATIONS (" + result.results().size() + "):");
                int maxDisplay = Math.min(result.results().size(), 10);
                for (int i = 0; i < maxDisplay; i++) {
                    var ref = result.results().get(i);
                    System.out.println("   - " + ref.implementationName() +
                            (ref.description().isBlank() ? "" : " - " + ref.description()));
                }
                if (result.results().size() > 10) {
                    System.out.println("   ... and " + (result.results().size() - 10) + " more");
                }
            } else {
                System.out.println();
                System.out.println("⚠️  No BAdI implementations found matching pattern: " + pattern);
            }

            System.out.println();
            System.out.println("✅ Test PASSED: BadiService.searchBadiImplementations()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            System.err.println();
            return false;
        }
    }

    /**
     * Test DmeeService.searchDmeeTrees() - Wildcard search
     *
     * @param treeType DMEE tree type (e.g., "PAYM")
     * @param pattern  wildcard pattern (e.g., "Z*", "*SEPA*")
     * @return true if test passed
     */
    private boolean testDmeeSearch(String treeType, String pattern) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: DmeeService.searchDmeeTrees()                          │");
        System.out.println("│       Wildcard Search - FM: ZCX_UTIL_GET_DMEE_TREE           │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            System.out.printf("   Tree Type: %s%n", treeType);
            System.out.printf("   Pattern:   %s%n", pattern);
            System.out.println();

            long startTime = System.currentTimeMillis();

            DmeeSearchResult result = dmeeService.searchDmeeTrees(treeType, pattern, 50);

            long duration = System.currentTimeMillis() - startTime;

            // Validate result
            assert result != null : "Result should not be null";
            assert result.pattern() != null : "Pattern should not be null";

            // Print results
            System.out.println("📋 SEARCH RESULT:");
            System.out.println("   Tree Type:    " + result.treeType());
            System.out.println("   Pattern:      " + result.pattern());
            System.out.println("   Max Results:  " + result.maxResults());
            System.out.println("   Total Found:  " + result.totalFound());
            System.out.println("   Duration:     " + duration + " ms");

            if (result.results() != null && !result.results().isEmpty()) {
                System.out.println();
                System.out.println("📦 MATCHING DMEE TREES (" + result.results().size() + "):");
                int maxDisplay = Math.min(result.results().size(), 10);
                for (int i = 0; i < maxDisplay; i++) {
                    var ref = result.results().get(i);
                    System.out.println("   - " + ref.treeType() + "/" + ref.treeId() +
                            (ref.description().isBlank() ? "" : " - " + ref.description()));
                }
                if (result.results().size() > 10) {
                    System.out.println("   ... and " + (result.results().size() - 10) + " more");
                }
            } else {
                System.out.println();
                System.out.println("⚠️  No DMEE trees found matching pattern: " + treeType + "/" + pattern);
            }

            System.out.println();
            System.out.println("✅ Test PASSED: DmeeService.searchDmeeTrees()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            System.err.println();
            return false;
        }
    }

    // =========================================================================
    // Phase 6: Extraction Discovery Tests
    // =========================================================================

    /**
     * Test AbapExtractionService.discoverUserObjects() - User scope
     *
     * <p>
     * Tests the ADT-first + FM fallback strategy for discovering
     * objects created/modified by a specific SAP user.
     *
     * @param username SAP username (e.g., "L_ABAPS_ITA", "DEVELOPER")
     * @return true if test passed
     */
    private boolean testExtractionUser(String username) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: AbapExtractionService.discoverUserObjects()            │");
        System.out.println("│       Phase 6 - Scope: USER (ADT-first + FM fallback)        │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            if (username == null || username.isBlank()) {
                System.out.println("⚠️  No username provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"extract-user L_ABAPS_ITA\"");
                System.out.println("   Note: If empty, uses current SAP user.");
                System.out.println();
            }

            System.out.printf("   Username: %s%n", username != null ? username : "(current user)");
            System.out.println();

            long startTime = System.currentTimeMillis();

            ExtractionDiscovery result = extractionService.discover(
                    ExtractionScope.USER,
                    username);

            long duration = System.currentTimeMillis() - startTime;

            printExtractionResult(result, duration);

            System.out.println();
            System.out.println("✅ Test PASSED: AbapExtractionService.discoverUserObjects()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            e.printStackTrace();
            System.err.println();
            return false;
        }
    }

    /**
     * Test AbapExtractionService.discoverPackageObjects() - Package scope
     *
     * @param packageNames comma-separated package names (e.g., "ZCX",
     *                     "ZMMI1229_0,ZFIE1017")
     * @return true if test passed
     */
    private boolean testExtractionPackage(String packageNames) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: AbapExtractionService.discoverPackageObjects()         │");
        System.out.println("│       Phase 6 - Scope: PACKAGE (recursive hierarchy)         │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            if (packageNames == null || packageNames.isBlank()) {
                System.out.println("⚠️  No package name(s) provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"extract-package ZCX\"");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"extract-package ZMMI1229_0,ZFIE1017\"");
                System.out.println("   Skipping test (requires valid package name).");
                System.out.println();
                return true; // Skip gracefully
            }

            System.out.printf("   Package(s): %s%n", packageNames);
            System.out.println();

            long startTime = System.currentTimeMillis();

            ExtractionDiscovery result = extractionService.discover(
                    ExtractionScope.PACKAGE,
                    packageNames);

            long duration = System.currentTimeMillis() - startTime;

            printExtractionResult(result, duration);

            System.out.println();
            System.out.println("✅ Test PASSED: AbapExtractionService.discoverPackageObjects()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            e.printStackTrace();
            System.err.println();
            return false;
        }
    }

    /**
     * Test AbapExtractionService.discoverTransportObjects() - Transport scope
     *
     * @param transportNumbers comma-separated transport numbers (e.g.,
     *                         "CADK911088")
     * @return true if test passed
     */
    private boolean testExtractionTransport(String transportNumbers) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: AbapExtractionService.discoverTransportObjects()       │");
        System.out.println("│       Phase 6 - Scope: TRANSPORT                             │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            if (transportNumbers == null || transportNumbers.isBlank()) {
                System.out.println("⚠️  No transport number(s) provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"extract-transport CADK911088\"");
                System.out.println(
                        "   Usage: ... -Dspring-boot.run.arguments=\"extract-transport CADK911088,CADK911089\"");
                System.out.println("   Skipping test (requires valid transport number).");
                System.out.println();
                return true; // Skip gracefully
            }

            System.out.printf("   Transport(s): %s%n", transportNumbers);
            System.out.println();

            long startTime = System.currentTimeMillis();

            ExtractionDiscovery result = extractionService.discover(
                    ExtractionScope.TRANSPORT,
                    transportNumbers);

            long duration = System.currentTimeMillis() - startTime;

            printExtractionResult(result, duration);

            System.out.println();
            System.out.println("✅ Test PASSED: AbapExtractionService.discoverTransportObjects()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            e.printStackTrace();
            System.err.println();
            return false;
        }
    }

    /**
     * Test AbapExtractionService.discoverSpecificObjects() - List scope
     *
     * @param objectNames comma-separated object names (e.g.,
     *                    "ZCL_TEST,ZREP_INVOICE")
     * @return true if test passed
     */
    private boolean testExtractionList(String objectNames) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: AbapExtractionService.discoverSpecificObjects()        │");
        System.out.println("│       Phase 6 - Scope: LIST (specific objects by name)       │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            if (objectNames == null || objectNames.isBlank()) {
                System.out.println("⚠️  No object name(s) provided.");
                System.out.println("   Usage: ... -Dspring-boot.run.arguments=\"extract-list ZCL_TEST,ZREP_INVOICE\"");
                System.out.println("   Skipping test (requires valid object names).");
                System.out.println();
                return true; // Skip gracefully
            }

            System.out.printf("   Object(s): %s%n", objectNames);
            System.out.println();

            long startTime = System.currentTimeMillis();

            ExtractionDiscovery result = extractionService.discover(
                    ExtractionScope.LIST,
                    objectNames);

            long duration = System.currentTimeMillis() - startTime;

            printExtractionResult(result, duration);

            System.out.println();
            System.out.println("✅ Test PASSED: AbapExtractionService.discoverSpecificObjects()");
            System.out.println();
            return true;

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Test FAILED: " + e.getMessage());
            e.printStackTrace();
            System.err.println();
            return false;
        }
    }

    /**
     * Helper method to print ExtractionDiscovery result.
     *
     * @param result   ExtractionDiscovery result
     * @param duration execution time in ms
     */
    private void printExtractionResult(ExtractionDiscovery result, long duration) {
        // Validate result
        assert result != null : "Result should not be null";
        assert result.scope() != null : "Scope should not be null";

        // Print summary
        System.out.println("📋 EXTRACTION DISCOVERY RESULT:");
        System.out.println("   Scope:          " + result.scope().getDisplayName());
        System.out.println("   Input:          " + result.scopeInput());
        System.out.println("   Duration:       " + duration + " ms");
        System.out.println("   Total Objects:  " + result.totalObjects());
        System.out.println("   Estimated Size: " + String.format("%.2f MB", result.estimatedSizeMb()));

        // Sources
        if (result.sources() != null && !result.sources().isEmpty()) {
            System.out.println();
            System.out.println("📦 SOURCES (" + result.sources().size() + "):");
            for (var source : result.sources()) {
                System.out.println("   - [" + source.type() + "] " + source.name() +
                        " (" + source.objectCount() + " objects)");
            }
        }

        // Objects by type
        if (result.objectsByType() != null && !result.objectsByType().isEmpty()) {
            System.out.println();
            System.out.println("📊 OBJECTS BY TYPE:");
            for (var entry : result.objectsByType().entrySet()) {
                var info = entry.getValue();
                System.out.println("   - " + info.typeText() + " (" + info.type() + "): " + info.count());
                // Show first 5 object names
                if (info.objectNames() != null && !info.objectNames().isEmpty()) {
                    int maxNames = Math.min(info.objectNames().size(), 5);
                    for (int i = 0; i < maxNames; i++) {
                        System.out.println("       • " + info.objectNames().get(i));
                    }
                    if (info.objectNames().size() > 5) {
                        System.out.println("       ... and " + (info.objectNames().size() - 5) + " more");
                    }
                }
            }
        }

        // Sample objects
        if (result.objects() != null && !result.objects().isEmpty()) {
            System.out.println();
            System.out.println("📝 SAMPLE OBJECTS (first 10 of " + result.objects().size() + "):");
            int maxDisplay = Math.min(result.objects().size(), 10);
            for (int i = 0; i < maxDisplay; i++) {
                var obj = result.objects().get(i);
                System.out.println("   - [" + obj.objectType() + "] " + obj.objectName() +
                        " (pkg: " + obj.devclass() + ")");
            }
            if (result.objects().size() > 10) {
                System.out.println("   ... and " + (result.objects().size() - 10) + " more objects");
            }
        }

        // Warnings
        if (result.warnings() != null && !result.warnings().isEmpty()) {
            System.out.println();
            System.out.println("⚠️  WARNINGS (" + result.warnings().size() + "):");
            for (var warning : result.warnings()) {
                System.out.println("   ! " + warning);
            }
        }
    }
}

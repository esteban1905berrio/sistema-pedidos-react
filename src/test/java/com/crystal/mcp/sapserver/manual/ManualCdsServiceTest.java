package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.CdsSourceResult;
import com.crystal.mcp.sapserver.service.CdsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for CdsService - CDS View source code retrieval.
 *
 * This test uses the CommandLineRunner pattern for isolated execution,
 * avoiding JUnit's tendency to run all methods when targeting specific ones.
 *
 * Usage:
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualCdsServiceTest
 *
 * Or with specific CDS name:
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualCdsServiceTest -Dspring-boot.run.arguments="ZCDS_MY_VIEW"
 *
 * Prerequisites:
 * - SAP connection configured via environment variables
 * - CDS View must exist in the target SAP system
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualCdsServiceTest implements CommandLineRunner {

    @Autowired
    private CdsService cdsService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualCdsServiceTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          Manual Test: CdsService - get_cds_source            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Default CDS View for testing (SAP standard)
        String cdsName = args.length > 0 ? args[0] : "I_BUSINESSPARTNER";

        try {
            // Test 1: Get active version
            // testGetCdsSource(cdsName, "active");

            // Test 2: Get inactive version (may fail if no inactive version exists)
            // testGetCdsSource(cdsName, "inactive");

            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("✅ ALL TESTS COMPLETED SUCCESSFULLY");
            System.out.println("═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println();
            System.err.println("═══════════════════════════════════════════════════════════════");
            System.err.println("❌ TEST FAILED: " + e.getMessage());
            System.err.println("═══════════════════════════════════════════════════════════════");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test getCdsSource method.
     */
    private void testGetCdsSource(String cdsName, String version) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEST: getCdsSource                                           │");
        System.out.println("├──────────────────────────────────────────────────────────────┤");
        System.out.printf("│ CDS Name: %-50s │%n", cdsName);
        System.out.printf("│ Version:  %-50s │%n", version);
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        long startTime = System.currentTimeMillis();

        CdsSourceResult result = cdsService.getCdsSource(cdsName, version);

        long duration = System.currentTimeMillis() - startTime;

        // Validate result
        assert result != null : "Result should not be null";
        assert result.source() != null : "Source should not be null";
        assert !result.source().isEmpty() : "Source should not be empty";
        assert result.cdsName() != null : "CDS name should not be null";
        assert result.version() != null : "Version should not be null";
        assert result.objectType().equals("DDLS") : "Object type should be DDLS";

        // Print results
        System.out.println();
        System.out.println("📋 RESULT:");
        System.out.println("   CDS Name:    " + result.cdsName());
        System.out.println("   Version:     " + result.version());
        System.out.println("   Object Type: " + result.objectType());
        System.out.println("   Source Size: " + result.source().length() + " chars");
        System.out.println("   Duration:    " + duration + " ms");
        System.out.println("   Metadata:    " + result.metadata());
        System.out.println();

        // Print first 500 chars of source
        String preview = result.source().length() > 500
                ? result.source().substring(0, 500) + "..."
                : result.source();
        System.out.println("📄 SOURCE PREVIEW (first 500 chars):");
        System.out.println("─".repeat(60));
        System.out.println(preview);
        System.out.println("─".repeat(60));
        System.out.println();

        System.out.println("✅ Test PASSED: getCdsSource(" + cdsName + ", " + version + ")");
        System.out.println();
    }
}

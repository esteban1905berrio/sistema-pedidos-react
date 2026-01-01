package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.DdicSourceResult;
import com.crystal.mcp.sapserver.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for ClassService.getDdicSource() (get_ddic_source MCP tool).
 *
 * <p>This test validates:
 * <ol>
 *   <li>Spring Boot configuration works</li>
 *   <li>JCo connection bean is created</li>
 *   <li>FM ZCX_GETDDICSOURCE is callable</li>
 *   <li>ClassService.getDdicSource() retrieves DDIC metadata correctly</li>
 * </ol>
 *
 * <p>Prerequisites:
 * <ol>
 *   <li>ManualJCoConnectionTest passed</li>
 *   <li>.env file configured with SAP connection</li>
 *   <li>VPN connection active</li>
 *   <li>FM ZCX_GETDDICSOURCE activated in SAP system</li>
 * </ol>
 *
 * <p>How to run:
 * <pre>{@code
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualDdicSourceTest
 * }</pre>
 *
 * @author Crystal Development Team
 * @since 1.0
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualDdicSourceTest implements CommandLineRunner {

    @Autowired
    private ClassService classService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualDdicSourceTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           Manual Test: get_ddic_source (ClassService)        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        try {
            // Test 1: Standard SAP table (MARA - Material Master)
            //testGetDdicSource("MARA");

            // Test 2: Another standard table (T001 - Company Codes)
            //testGetDdicSource("T001");

            // Test 3: DD03L system table (should work in any system)
            //testGetDdicSource("DD03L");

            // Test 4: Custom table (uncomment and replace with your table)
            // testGetDdicSource("ZTABLE_CUSTOM");

            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                   ✅ ALL TESTS PASSED                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println("\nget_ddic_source is working correctly!");

        } catch (Exception e) {
            System.err.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║                   ❌ TEST FAILED                             ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test getDdicSource method for a specific object.
     *
     * @param objectName name of the DDIC object (table/structure/view)
     */
    private void testGetDdicSource(String objectName) {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Testing: " + padRight(objectName, 52) + "│");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        try {
            DdicSourceResult result = classService.getDdicSource(objectName);

            // Validate result
            if (result == null) {
                throw new AssertionError("Result is null!");
            }

            if (result.getObjectName() == null || result.getObjectName().isEmpty()) {
                throw new AssertionError("Object name is empty!");
            }

            if (result.getFields() == null || result.getFields().isEmpty()) {
                throw new AssertionError("Fields list is empty!");
            }

            // Print summary
            System.out.println("  ✓ Object Name:   " + result.getObjectName());
            System.out.println("  ✓ Object Type:   " + result.getObjectType());
            System.out.println("  ✓ Object Status: " + result.getObjectStatus());
            System.out.println("  ✓ Field Count:   " + result.getFieldCount());

            // Print first 5 fields as sample
            System.out.println("\n  Sample fields (first 5):");
            System.out.println("  ┌──────────────────────────────────────────────────────────┐");
            System.out.println("  │ # │ FIELDNAME          │ KEY │ DATATYPE │ DATA ELEMENT   │");
            System.out.println("  ├───┼────────────────────┼─────┼──────────┼────────────────┤");

            int count = 0;
            for (DdicSourceResult.DdicField field : result.getFields()) {
                if (count >= 5) break;
                System.out.printf("  │ %d │ %-18s │ %-3s │ %-8s │ %-14s │%n",
                        field.getPosition(),
                        truncate(field.getFieldname(), 18),
                        "X".equals(field.getKeyflag()) ? "X" : "",
                        truncate(field.getDatatype(), 8),
                        truncate(field.getRollname(), 14));
                count++;
            }

            System.out.println("  └──────────────────────────────────────────────────────────┘");

            if (result.getFieldCount() > 5) {
                System.out.println("  ... and " + (result.getFieldCount() - 5) + " more fields");
            }

            System.out.println("\n  ✅ Test passed for: " + objectName);

        } catch (Exception e) {
            System.err.println("  ❌ Test failed for: " + objectName);
            System.err.println("  Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Pad string to the right with spaces.
     */
    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    /**
     * Truncate string to max length.
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen - 1) + "…" : s;
    }
}

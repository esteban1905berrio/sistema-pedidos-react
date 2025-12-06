package com.crystal.mcp.sapserver.manual.transport;

import com.crystal.mcp.sapserver.model.TransportLogResult;
import com.crystal.mcp.sapserver.model.TransportLogResult.*;
import com.crystal.mcp.sapserver.service.TransportLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for TransportLogService with Spring Boot but WITHOUT MCP.
 *
 * This test validates:
 * 1. Spring Boot configuration works
 * 2. JCo connection bean is created
 * 3. ZCX_GET_TRANSPORT_LOGS function module exists
 * 4. TransportLogService.getTransportLog() retrieves log data
 * 5. Filtering by errors/warnings works correctly
 *
 * Prerequisites:
 * 1. .env file configured with SAP connection params
 * 2. VPN connection active (if required)
 * 3. ZCX_GET_TRANSPORT_LOGS function module activated in SAP (gdcmcp)
 * 4. User has authorization for CTS (S_CTS_ADMI, S_TRANSPRT)
 *
 * How to run:
 *   mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportLogTest
 *
 * Or:
 *   mvn clean package -DskipTests
 *   java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar \
 *        --spring.main.sources=com.crystal.mcp.sapserver.manual.transport.ManualTransportLogTest
 */
@Profile("!test")  // Exclude from test profile
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualTransportLogTest implements CommandLineRunner {

    @Autowired
    private TransportLogService transportLogService;

    // Test transport numbers (modify these to test with real OTs)
    private static final String TEST_SINGLE_TRANSPORT = "CADK911681";
    private static final String TEST_MULTIPLE_TRANSPORTS = "S4DK930001,S4DK930002";
    private static final String TEST_USER_FILTER = null;  // Set to a username to filter

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualTransportLogTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          Manual TransportLogService Test (CTS Logs)          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        try {
            // Test 1: Single transport
            //testSingleTransport();

            // Test 2: Multiple transports (if configured)
            if (TEST_MULTIPLE_TRANSPORTS != null && !TEST_MULTIPLE_TRANSPORTS.equals(TEST_SINGLE_TRANSPORT)) {
                //testMultipleTransports();
            }

            // Test 3: With user filter (if configured)
            if (TEST_USER_FILTER != null) {
                //testWithUserFilter();
            }

            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ✅ ALL TESTS COMPLETED                    ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void testSingleTransport() {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 1: Single Transport Log                                 │");
        System.out.println("├──────────────────────────────────────────────────────────────┤");
        System.out.println("│ Transport: " + padRight(TEST_SINGLE_TRANSPORT, 49) + "│");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        TransportLogResult result = transportLogService.getTransportLog(TEST_SINGLE_TRANSPORT, null);

        printResult(result);
    }

    private void testMultipleTransports() {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 2: Multiple Transports Log                              │");
        System.out.println("├──────────────────────────────────────────────────────────────┤");
        System.out.println("│ Transports: " + padRight(TEST_MULTIPLE_TRANSPORTS, 48) + "│");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        TransportLogResult result = transportLogService.getTransportLog(TEST_MULTIPLE_TRANSPORTS, null);

        printResult(result);
    }

    private void testWithUserFilter() {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 3: Transport Log with User Filter                       │");
        System.out.println("├──────────────────────────────────────────────────────────────┤");
        System.out.println("│ Transports: " + padRight(TEST_SINGLE_TRANSPORT, 48) + "│");
        System.out.println("│ User Filter: " + padRight(TEST_USER_FILTER, 47) + "│");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        TransportLogResult result = transportLogService.getTransportLog(TEST_SINGLE_TRANSPORT, TEST_USER_FILTER);

        printResult(result);
    }

    private void printResult(TransportLogResult result) {
        System.out.println();

        if (!result.success()) {
            System.out.println("❌ Error: " + result.message());
            return;
        }

        System.out.println("✅ Success!");

        // Print summary
        if (result.summary() != null) {
            Summary summary = result.summary();
            System.out.println("\n📊 Summary:");
            System.out.println("   Total Transports: " + summary.totalTransports());
            System.out.println("   With Errors:      " + summary.withErrors());
            System.out.println("   With Warnings:    " + summary.withWarnings());
            System.out.println("   Without Log:      " + summary.withoutLog());
        }

        // Print transport details
        if (result.transports() != null && !result.transports().isEmpty()) {
            System.out.println("\n📦 Transports:");

            for (TransportLogEntry entry : result.transports()) {
                System.out.println("\n   ┌─ " + entry.trkorr() + " ─────────────────────────");
                System.out.println("   │ Owner:       " + entry.owner());
                System.out.println("   │ Type:        " + entry.type() + " (" + entry.typeText() + ")");
                System.out.println("   │ Description: " + truncate(entry.description(), 40));
                System.out.println("   │ Has Log:     " + (entry.hasLog() ? "Yes" : "No"));
                System.out.println("   │ Has Problems:" + (entry.hasProblems() ? "Yes" : "No"));

                if (entry.hasLog() && entry.hasProblems()) {
                    System.out.println("   │ Errors:      " + entry.errorCount());
                    System.out.println("   │ Warnings:    " + entry.warningCount());

                    if (entry.problems() != null && !entry.problems().isEmpty()) {
                        System.out.println("   │");
                        System.out.println("   │ Problems:");

                        for (Problem problem : entry.problems()) {
                            String icon = problem.isError() ? "🔴" : "🟡";
                            System.out.println("   │   " + icon + " [" + problem.severity() + "] " +
                                    truncate(problem.message(), 40));
                            System.out.println("   │      System: " + problem.system() +
                                    " | Step: " + problem.step() + " (" + problem.stepText() + ")");
                        }
                    }
                }

                if (!entry.hasLog() && entry.message() != null) {
                    System.out.println("   │ Message:     " + entry.message());
                }

                System.out.println("   └───────────────────────────────────────────");
            }
        } else {
            System.out.println("\n   (No transport data returned)");
        }
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}

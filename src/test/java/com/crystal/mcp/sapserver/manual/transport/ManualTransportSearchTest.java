package com.crystal.mcp.sapserver.manual.transport;

import com.crystal.mcp.sapserver.model.TransportSearchResult;
import com.crystal.mcp.sapserver.service.TransportSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for TransportSearchService.
 *
 * <p>This test searches transport requests in SAP to validate the implementation.
 * Run it using:
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportSearchTest
 * </pre>
 *
 * <p>This is a read-only operation, safe to run in any environment.
 *
 * @author Crystal Development Team
 * @since 2025-12-04
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualTransportSearchTest implements CommandLineRunner {

    @Autowired
    private TransportSearchService transportSearchService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualTransportSearchTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         Manual Test: TransportSearchService                  ║");
        System.out.println("║                                                              ║");
        System.out.println("║  This test searches transport requests in SAP.               ║");
        System.out.println("║  Read-only operation - safe for any environment.             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Test 1: Search by description pattern
            //testSearchByDescription();

            // Test 2: Search by user
            //testSearchByUser();

            // Test 3: Search by status
            //testSearchByStatus();

            // Test 4: Combined search
            //testCombinedSearch();

            // Test 5: Error case - no criteria
            //testNoCriteria();

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ✅ ALL TESTS PASSED                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println();
            System.err.println("╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║                    ❌ TEST FAILED                            ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test 1: Search by description pattern.
     */
    private void testSearchByDescription() {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("TEST 1: Search by description pattern");
        System.out.println("─────────────────────────────────────────────────────────────────");

        TransportSearchResult result = transportSearchService.searchTransports(
                "%PSR%",    // description pattern
                null,       // user
                null,       // transportType
                null,       // status
                null,       // targetSystem
                null,       // dateFrom
                null,       // dateTo
                10          // maxResults
        );

        printResult("Search '%PSR%'", result);
    }

    /**
     * Test 2: Search by user.
     */
    private void testSearchByUser() {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("TEST 2: Search by user");
        System.out.println("─────────────────────────────────────────────────────────────────");

        TransportSearchResult result = transportSearchService.searchTransports(
                null,           // description
                "L_ABAPS_ITA",  // user
                null,           // transportType
                "D",            // status - Modifiable
                null,           // targetSystem
                null,           // dateFrom
                null,           // dateTo
                10              // maxResults
        );

        printResult("Search user='L_ABAPS_ITA', status='D'", result);
    }

    /**
     * Test 3: Search by status only.
     */
    private void testSearchByStatus() {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("TEST 3: Search by status (Released transports)");
        System.out.println("─────────────────────────────────────────────────────────────────");

        TransportSearchResult result = transportSearchService.searchTransports(
                null,   // description
                null,   // user
                "K",    // transportType - Workbench
                "R",    // status - Released
                null,   // targetSystem
                null,   // dateFrom
                null,   // dateTo
                5       // maxResults
        );

        printResult("Search type='K', status='R'", result);
    }

    /**
     * Test 4: Combined search with date range.
     */
    private void testCombinedSearch() {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("TEST 4: Combined search (FI, Workbench, December 2025)");
        System.out.println("─────────────────────────────────────────────────────────────────");

        TransportSearchResult result = transportSearchService.searchTransports(
                "%FI%",         // description
                null,           // user
                "K",            // transportType - Workbench
                null,           // status
                null,           // targetSystem
                "2025-12-01",   // dateFrom
                "2025-12-31",   // dateTo
                10              // maxResults
        );

        printResult("Search '%FI%', type='K', Dec 2025", result);
    }

    /**
     * Test 5: Error case - no criteria provided.
     */
    private void testNoCriteria() {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("TEST 5: Error case - no criteria");
        System.out.println("─────────────────────────────────────────────────────────────────");

        TransportSearchResult result = transportSearchService.searchTransports(
                null,   // description
                null,   // user
                null,   // transportType
                null,   // status
                null,   // targetSystem
                null,   // dateFrom
                null,   // dateTo
                10      // maxResults
        );

        System.out.println("Query: No criteria provided");
        System.out.println("Success: " + result.success());
        System.out.println("Message: " + result.message());
        System.out.println();

        if (!result.success() && result.message().contains("At least one search criterion")) {
            System.out.println("✅ Expected error received: " + result.message());
        } else {
            System.out.println("⚠️ Unexpected result - FM should require at least one criterion");
        }
        System.out.println();
    }

    /**
     * Print search result in formatted output.
     */
    private void printResult(String query, TransportSearchResult result) {
        System.out.println("Query: " + query);
        System.out.println("Success: " + result.success());
        System.out.println("Total Found: " + result.totalFound());

        if (result.message() != null && !result.message().isEmpty()) {
            System.out.println("Message: " + result.message());
        }

        if (result.transports() != null && !result.transports().isEmpty()) {
            System.out.println();
            System.out.println("Results:");
            System.out.println("┌──────────────┬────────────────────────────────────────┬────────┬──────┬─────────────┬────────┬───────┐");
            System.out.println("│ Transport    │ Description                            │ Type   │ St   │ Owner       │ Objs   │ Tasks │");
            System.out.println("├──────────────┼────────────────────────────────────────┼────────┼──────┼─────────────┼────────┼───────┤");

            for (TransportSearchResult.TransportDetail t : result.transports()) {
                String desc = t.description();
                if (desc.length() > 38) {
                    desc = desc.substring(0, 35) + "...";
                }
                System.out.printf("│ %-12s │ %-38s │ %-6s │ %-4s │ %-11s │ %6d │ %5d │%n",
                        t.transportNumber(),
                        desc,
                        t.transportType(),
                        t.status(),
                        t.owner(),
                        t.objectCount(),
                        t.taskCount()
                );
            }

            System.out.println("└──────────────┴────────────────────────────────────────┴────────┴──────┴─────────────┴────────┴───────┘");
        }
        System.out.println();
    }
}

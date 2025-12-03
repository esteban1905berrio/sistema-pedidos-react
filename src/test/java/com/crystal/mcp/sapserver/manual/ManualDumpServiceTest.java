package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.DumpDetailResult;
import com.crystal.mcp.sapserver.model.DumpInfo;
import com.crystal.mcp.sapserver.model.DumpListResult;
import com.crystal.mcp.sapserver.service.DumpService;
import com.crystal.mcp.sapserver.service.RfcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Manual test for DumpService with Spring Boot but WITHOUT MCP.
 *
 * This test validates:
 * 1. Spring Boot configuration works
 * 2. JCo connection bean is created
 * 3. RfcAdapter works
 * 4. DumpService.listDumps() retrieves dump list
 * 5. DumpService.getDumpDetails() retrieves dump details
 *
 * Prerequisites:
 * 1. .env file configured with SAP connection params
 * 2. VPN connection active (if required)
 * 3. User has authorization for ST22 (S_DEVELOP with ACTVT=03)
 *
 * How to run:
 *   mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualDumpServiceTest
 *
 * Or:
 *   mvn clean package -DskipTests
 *   java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar \
 *        --spring.main.sources=com.crystal.mcp.sapserver.manual.ManualDumpServiceTest
 */
@Profile("!test")  // Exclude from test profile
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualDumpServiceTest implements CommandLineRunner {

    @Autowired
    private DumpService dumpService;

    @Autowired
    private RfcAdapter rfcAdapter;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualDumpServiceTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           Manual DumpService Test (ST22 Analysis)            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        try {
            // Test 1: List dumps and get first one's details
            testListAndGetDetails();

            // Alternative tests (uncomment as needed):
            // testRawXmlResponse();
            // testListDumpsToday();

            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                   TEST COMPLETED                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║                     TEST FAILED                              ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test: List dumps and get details for the first one found
     */
    private void testListAndGetDetails() {
        System.out.println("┌────────────────]9──────────────────────────────────────────────┐");
        System.out.println("│ Test: List Dumps and Get Details                             │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        // Step 1: List today's dumps
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        System.out.println("\n  Step 1: Listing dumps for " + today);

        DumpListResult listResult = dumpService.listDumps(today, today, "L_ABAPS_ITA" );
        System.out.println("  Result: " + listResult.message());
        System.out.println("  Total dumps found: " + listResult.totalCount());

        if (listResult.dumps() == null || listResult.dumps().isEmpty()) {
            System.out.println("  No dumps found. Try with a wider date range.");
            return;
        }

        // Show first few dumps
        System.out.println("\n  First 5 dumps:");
        int count = 0;
        for (DumpInfo dump : listResult.dumps()) {
            if (count++ >= 5) break;
            System.out.printf("    [%d] %s %s | %s | %s%n",
                    count,
                    dump.date() != null ? dump.date() : "----",
                    dump.time() != null ? dump.time() : "----",
                    dump.errorId() != null ? dump.errorId() : "Unknown",
                    dump.user() != null ? dump.user() : "Unknown");
            System.out.printf("        ID: %s%n", truncate(dump.dumpId(), 60));
        }

        // Step 2: Get details for the first dump
        DumpInfo firstDump = listResult.dumps().get(0);
        System.out.println("\n  Step 2: Getting details for first dump");
        System.out.println("    Dump ID: " + truncate(firstDump.dumpId(), 60));

        DumpDetailResult detailResult = dumpService.getDumpDetails(firstDump.dumpId());
        System.out.println("    Result: " + detailResult.message());

        if (detailResult.runtimeError() != null) {
            System.out.println("\n  ┌─────────────────────────────────────────────────────────────┐");
            System.out.println("  │ Dump Details                                                │");
            System.out.println("  ├─────────────────────────────────────────────────────────────┤");
            System.out.println("  │ Runtime Error: " + padRight(detailResult.runtimeError(), 44) + "│");
            System.out.println("  │ Program:       " + padRight(detailResult.programName() != null ? detailResult.programName() : "-", 44) + "│");
            System.out.println("  │ User:          " + padRight(detailResult.user() != null ? detailResult.user() : "-", 44) + "│");
            System.out.println("  │ Date/Time:     " + padRight((detailResult.date() != null ? detailResult.date() : "-") + " " + (detailResult.time() != null ? detailResult.time() : ""), 44) + "│");
            System.out.println("  ├─────────────────────────────────────────────────────────────┤");

            if (detailResult.shortText() != null) {
                System.out.println("  │ Short Text:                                                 │");
                printWrapped(detailResult.shortText(), "  │   ");
            }

            if (detailResult.whatHappened() != null) {
                System.out.println("  │ What Happened:                                              │");
                printWrapped(detailResult.whatHappened(), "  │   ");
            }

            if (detailResult.errorAnalysis() != null) {
                System.out.println("  │ Error Analysis:                                             │");
                printWrapped(detailResult.errorAnalysis(), "  │   ");
            }

            if (!detailResult.callStack().isEmpty()) {
                System.out.println("  │ Call Stack:                                                 │");
                for (String stackLine : detailResult.callStack()) {
                    System.out.println("  │   " + truncate(stackLine, 55));
                }
            }

            if (!detailResult.sourceCodeLines().isEmpty()) {
                System.out.println("  │ Source Code (first 10 lines):                              │");
                int lineCount = 0;
                for (String srcLine : detailResult.sourceCodeLines()) {
                    if (lineCount++ >= 10) break;
                    System.out.println("  │   " + truncate(srcLine, 55));
                }
            }

            System.out.println("  └─────────────────────────────────────────────────────────────┘");
        } else {
            System.out.println("  Could not retrieve dump details.");
        }
    }

    /**
     * RAW XML TEST: Call ADT endpoint directly and write formatted XML to file
     */
    private void testRawXmlResponse() throws Exception {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ RAW XML Response from /sap/bc/adt/runtime/dumps              │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        Map<String, String> params = new HashMap<>();
        params.put("from", today + "000000");
        params.put("to", today + "235959");
        params.put("user", "L_FI2_ITA");

        System.out.println("  Parameters: from=" + params.get("from") + ", to=" + params.get("to") + ", user=" + params.get("user"));
        System.out.println();

        RfcAdapter.RfcResponse response = rfcAdapter.request(
                "/sap/bc/adt/runtime/dumps",
                "GET",
                null,
                params,
                "",
                "application/atom+xml"
        );

        System.out.println("  HTTP Status: " + response.statusCode());

        if (response.statusCode() == 200) {
            // Write formatted XML to file
            Path xmlPath = Paths.get("/Users/bastianroot/CursorIDEWorkspace/giralmcp/temp/data.xml");
            Files.createDirectories(xmlPath.getParent());

            // Parse and format XML for readability
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xmlDoc = db.parse(new ByteArrayInputStream(response.text().getBytes(StandardCharsets.UTF_8)));

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlDoc), new StreamResult(writer));

            // Decode HTML entities for readability
            String formattedXml = writer.toString()
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'");

            Files.writeString(xmlPath, formattedXml, StandardCharsets.UTF_8);

            System.out.println();
            System.out.println("  ✓ Formatted XML written to: " + xmlPath.toAbsolutePath());
            System.out.println("  File size: " + Files.size(xmlPath) + " bytes");
        } else {
            System.out.println("  ✗ Error: " + response.text());
        }
    }

    /**
     * Test 1: List today's dumps
     */
    private void testListDumpsToday() {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 1: List today's dumps                                   │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        DumpListResult result = dumpService.listDumps(today, today, null);

        printDumpListResult(result, "Today (" + today + ")");
    }

    /**
     * Test 2: List dumps for last 7 days
     */
    private void testListDumpsDateRange() {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 2: List dumps for last 7 days                           │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        String from = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String to = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        DumpListResult result = dumpService.listDumps(from, to, null);

        printDumpListResult(result, "Last 7 days (" + from + " to " + to + ")");
    }

    /**
     * Test 3: List dumps for specific user
     */
    private void testListDumpsForUser(String user) {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 3: List dumps for user: " + padRight(user, 32) + "│");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        DumpListResult result = dumpService.listDumps(today, today, user);

        printDumpListResult(result, "User " + user);
    }

    /**
     * Test 4: Get dump details
     */
    private void testGetDumpDetails(String dumpId) {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ Test 4: Get dump details                                     │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        System.out.println("  Dump ID: " + dumpId);

        DumpDetailResult result = dumpService.getDumpDetails(dumpId);

        printDumpDetailResult(result);
    }

    /**
     * Prints dump list result in a formatted way
     */
    private void printDumpListResult(DumpListResult result, String context) {
        System.out.println("  Context: " + context);
        System.out.println("  Message: " + result.message());
        System.out.println("  Total dumps: " + result.totalCount());

        if (result.dumps() != null && !result.dumps().isEmpty()) {
            System.out.println("\n  ┌─────────────────────────────────────────────────────────────┐");
            System.out.println("  │ Dump List (first 10)                                        │");
            System.out.println("  ├─────────────────────────────────────────────────────────────┤");

            int count = 0;
            for (DumpInfo dump : result.dumps()) {
                if (count++ >= 10) break;

                System.out.printf("  │ %s %s │ %-12s │ %-20s │%n",
                        dump.date() != null ? dump.date() : "----------",
                        dump.time() != null ? dump.time() : "--------",
                        dump.user() != null ? truncate(dump.user(), 12) : "",
                        dump.errorId() != null ? truncate(dump.errorId(), 20) : ""
                );

                if (dump.title() != null) {
                    System.out.println("  │   Title: " + truncate(dump.title(), 50));
                }
                if (dump.programName() != null) {
                    System.out.println("  │   Program: " + dump.programName());
                }
                if (dump.dumpId() != null) {
                    System.out.println("  │   ID: " + truncate(dump.dumpId(), 50));
                }
                System.out.println("  │");
            }

            System.out.println("  └─────────────────────────────────────────────────────────────┘");
        } else {
            System.out.println("  No dumps found for this criteria.");
        }

        System.out.println("  ✓ Test completed successfully");
    }

    /**
     * Prints dump detail result in a formatted way
     */
    private void printDumpDetailResult(DumpDetailResult result) {
        if (result.message().startsWith("Error") || result.runtimeError() == null) {
            System.out.println("  ✗ " + result.message());
            return;
        }

        System.out.println("\n  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │ Dump Details                                                │");
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │ Runtime Error: " + result.runtimeError());
        System.out.println("  │ Date/Time: " + result.date() + " " + result.time());
        System.out.println("  │ User: " + result.user());
        System.out.println("  │ Host: " + result.host());
        System.out.println("  │ Client: " + result.client());
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │ Program: " + result.programName());
        System.out.println("  │ Include: " + result.includeName());
        System.out.println("  │ Line: " + result.lineNumber());
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");

        if (result.shortText() != null) {
            System.out.println("  │ Short Text:");
            System.out.println("  │   " + result.shortText());
        }

        if (result.whatHappened() != null) {
            System.out.println("  │ What Happened:");
            printWrapped(result.whatHappened(), "  │   ");
        }

        if (result.howToFix() != null) {
            System.out.println("  │ How to Fix:");
            printWrapped(result.howToFix(), "  │   ");
        }

        if (!result.callStack().isEmpty()) {
            System.out.println("  │ Call Stack:");
            for (String line : result.callStack()) {
                System.out.println("  │   " + line);
            }
        }

        if (!result.sourceCodeLines().isEmpty()) {
            System.out.println("  │ Source Code:");
            for (String line : result.sourceCodeLines()) {
                System.out.println("  │   " + truncate(line, 60));
            }
        }

        if (!result.variables().isEmpty()) {
            System.out.println("  │ Variables:");
            for (DumpDetailResult.VariableInfo var : result.variables()) {
                System.out.printf("  │   %s (%s) = %s%n",
                        var.name(), var.type(), truncate(var.value(), 40));
            }
        }

        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println("  ✓ Test completed successfully");
    }

    /**
     * Truncates string to max length
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    /**
     * Pads string to specified length
     */
    private String padRight(String str, int length) {
        if (str == null) str = "";
        return String.format("%-" + length + "s", str);
    }

    /**
     * Prints wrapped text with prefix
     */
    private void printWrapped(String text, String prefix) {
        if (text == null) return;
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.length() <= 55) {
                System.out.println(prefix + line);
            } else {
                // Word wrap long lines
                int pos = 0;
                while (pos < line.length()) {
                    int end = Math.min(pos + 55, line.length());
                    System.out.println(prefix + line.substring(pos, end));
                    pos = end;
                }
            }
        }
    }
}

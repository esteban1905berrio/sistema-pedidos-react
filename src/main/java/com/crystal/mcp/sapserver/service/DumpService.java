package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.DumpDetailResult;
import com.crystal.mcp.sapserver.model.DumpInfo;
import com.crystal.mcp.sapserver.model.DumpListResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ABAP Dump (Short Dump / Runtime Error) operations.
 *
 * Provides access to SAP ST22 dump information via ADT REST API.
 * Uses the ADT runtime/dumps endpoint which provides dump feeds.
 *
 * ADT Endpoints used:
 * - GET /sap/bc/adt/runtime/dumps - List dumps (Atom feed)
 * - GET /sap/bc/adt/runtime/dump/{dumpId} - Get dump metadata (XML)
 * - GET /sap/bc/adt/runtime/dump/{dumpId}/summary - Get dump summary (HTML)
 * - GET /sap/bc/adt/runtime/dump/{dumpId}/formatted - Get dump formatted (text/plain)
 *
 * Key classes in SAP:
 * - CL_SABP_RABAX_ADT_RES_DUMPS: ADT resource handler for dumps
 * - CL_RABAX_ADT_URI_BUILDER_DUMPS: URI builder for dump endpoints
 *
 * Reference: SAP table SNAP (short dump storage)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DumpService {

    private final RfcAdapter rfcAdapter;

    // ADT endpoints for dumps
    private static final String DUMPS_LIST_URI = "/sap/bc/adt/runtime/dumps";
    private static final String DUMP_DETAIL_URI = "/sap/bc/adt/runtime/dump";  // Note: singular 'dump', not 'dumps'

    // Content types
    private static final String ATOM_CONTENT_TYPE = "application/atom+xml";
    private static final String ADT_DUMP_CONTENT_TYPE = "application/vnd.sap.adt.runtime.dump.v1+xml";
    private static final String HTML_CONTENT_TYPE = "text/html";
    private static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";

    // Custom FM for ECC fallback (calls RS_ST22_GET_FT internally)
    private static final String FM_GET_DUMP_DETAIL = "ZCX_GET_DUMP_DETAIL";

    // JSON parser for FM response
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List ABAP dumps (short dumps / runtime errors).
     *
     * Retrieves dumps from the ADT runtime/dumps feed endpoint.
     * Supports filtering by date range and user.
     *
     * Token Optimization:
     * - Returns summary info only (~500-1000 tokens)
     * - Use get_dump_details for full content
     *
     * @param dateFrom Start date (YYYY-MM-DD format). Defaults to today.
     * @param dateTo   End date (YYYY-MM-DD format). Defaults to today.
     * @param user     Optional user filter. Null for all users.
     * @return DumpListResult containing list of dump summaries
     */
    public DumpListResult listDumps(String dateFrom, String dateTo, String user) {
        log.info("Listing dumps | dateFrom: {} | dateTo: {} | user: {}",
                dateFrom, dateTo, user);

        try {
            // Build query parameters
            Map<String, String> params = new HashMap<>();

            // Convert dates to timestamps if provided
            if (dateFrom != null && !dateFrom.isEmpty()) {
                params.put("from", convertDateToTimestamp(dateFrom, true));
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                params.put("to", convertDateToTimestamp(dateTo, false));
            }
            if (user != null && !user.isEmpty()) {
                params.put("user", user.toUpperCase());
            }

            // Call ADT API
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    DUMPS_LIST_URI,
                    "GET",
                    null,
                    params,
                    "",
                    ATOM_CONTENT_TYPE
            );

            if (response.statusCode() != 200) {
                log.error("Failed to list dumps: HTTP {} - {}",
                        response.statusCode(), response.text());
                return DumpListResult.error("Failed to list dumps: HTTP " + response.statusCode());
            }
            // Log raw response for debugging
            log.debug("Atom feed response (first 2000 chars): {}",
                    response.text().length() > 2000
                        ? response.text().substring(0, 2000) + "..."
                        : response.text());

            // Parse Atom feed response
            List<DumpInfo> dumps = parseAtomFeed(response.text());

            // Determine effective date range for response
            String effectiveDateFrom = dateFrom != null ? dateFrom :
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String effectiveDateTo = dateTo != null ? dateTo :
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            return DumpListResult.success(dumps, effectiveDateFrom, effectiveDateTo, user);

        } catch (Exception e) {
            log.error("Error listing dumps: {}", e.getMessage(), e);
            return DumpListResult.error("Error listing dumps: " + e.getMessage());
        }
    }

    /**
     * Get detailed information about a specific dump.
     *
     * Strategy (3-phase approach per pr_dumps_analisys.md):
     * 1. Try ADT /formatted endpoint (text/plain) - FULL ST22 content, most valuable
     * 2. Try ADT /summary endpoint (HTML) - subset of formatted, fallback
     * 3. If ADT fails (404/error), call RFC FM ZCX_GET_DUMP_DETAIL - works on ECC systems
     *
     * The formatted endpoint provides the most complete information including:
     * - Variables with values
     * - RFC caller context
     * - Complete kernel call stack
     * - System environment details
     *
     * @param dumpId Dump identifier from list_dumps
     * @return DumpDetailResult with full dump information
     */
    public DumpDetailResult getDumpDetails(String dumpId) {
        log.info("Getting dump details | dumpId: {}", dumpId);

        if (dumpId == null || dumpId.isEmpty()) {
            return DumpDetailResult.error("Dump ID is required");
        }

        try {
            // Strategy 1: Try ADT formatted endpoint (text/plain) - FULL ST22 content
            String formattedUri = DUMP_DETAIL_URI + "/" + dumpId + "/formatted";
            log.debug("Strategy 1: Trying ADT formatted endpoint (full ST22): {}", formattedUri);

            RfcAdapter.RfcResponse formattedResponse = rfcAdapter.request(
                    formattedUri,
                    "GET",
                    null,
                    null,
                    "",
                    TEXT_PLAIN_CONTENT_TYPE
            );

            if (formattedResponse.statusCode() == 200) {
                log.info("ADT formatted endpoint succeeded (S/4HANA), parsing full ST22 text response");
                return parseDumpFormattedText(dumpId, formattedResponse.text());
            }

            log.warn("ADT formatted endpoint failed (HTTP {}), trying summary endpoint",
                    formattedResponse.statusCode());

            // Strategy 2: Try ADT summary endpoint (HTML) - subset fallback
            String summaryUri = DUMP_DETAIL_URI + "/" + dumpId + "/summary";
            log.debug("Strategy 2: Trying ADT summary endpoint (subset): {}", summaryUri);

            RfcAdapter.RfcResponse summaryResponse = rfcAdapter.request(
                    summaryUri,
                    "GET",
                    null,
                    null,
                    "",
                    HTML_CONTENT_TYPE
            );

            if (summaryResponse.statusCode() == 200) {
                log.info("ADT summary endpoint succeeded (S/4HANA), parsing HTML response");
                return parseDumpSummaryHtml(dumpId, summaryResponse.text());
            }

            // Strategy 3: ADT failed, try RFC FM (ECC fallback)
            log.warn("ADT endpoints failed (formatted: {}, summary: {}), trying RFC FM fallback for ECC",
                    formattedResponse.statusCode(), summaryResponse.statusCode());

            return getDumpDetailsViaRfc(dumpId);

        } catch (Exception e) {
            log.error("Error getting dump details: {}", e.getMessage(), e);
            return DumpDetailResult.error("Error getting dump details: " + e.getMessage());
        }
    }

    /**
     * Converts date string to SAP timestamp format.
     *
     * @param date      Date in YYYY-MM-DD format
     * @param isStart   True for start of day (00:00:00), false for end of day (23:59:59)
     * @return Timestamp in YYYYMMDDHHmmss format
     */
    private String convertDateToTimestamp(String date, boolean isStart) {
        // Remove any dashes
        String cleanDate = date.replace("-", "");
        if (isStart) {
            return cleanDate + "000000";
        } else {
            return cleanDate + "235959";
        }
    }

    /**
     * Parses Atom feed response from dumps endpoint.
     *
     * The feed contains entry elements with dump information.
     *
     * @param xmlContent Atom feed XML
     * @return List of DumpInfo objects
     */
    private List<DumpInfo> parseAtomFeed(String xmlContent) throws Exception {
        List<DumpInfo> dumps = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

        // Find all entry elements
        NodeList entries = doc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
        if (entries.getLength() == 0) {
            // Try without namespace
            entries = doc.getElementsByTagName("entry");
        }

        log.debug("Found {} dump entries in feed", entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            DumpInfo dump = parseAtomEntry(entry);
            if (dump != null) {
                dumps.add(dump);
            }
        }

        return dumps;
    }

    /**
     * Parses a single Atom entry into DumpInfo.
     *
     * The summary element contains full HTML with dump details for ECC systems
     * where detail endpoints may not be available. This HTML is stored for
     * fallback analysis.
     */
    private DumpInfo parseAtomEntry(Element entry) {
        try {
            // Extract title (contains error name)
            String title = getElementText(entry, "title");

            // Extract ID element and extract just the dump ID (after last /)
            String idElement = getElementText(entry, "id");
            String dumpId = extractDumpIdFromPath(idElement);

            log.debug("Entry parsing - title: {}, raw id: {}, extracted dumpId: {}", title, idElement, dumpId);

            // Extract updated (timestamp)
            String updated = getElementText(entry, "updated");
            String date = null;
            String time = null;
            if (updated != null && updated.length() >= 19) {
                // Format: 2025-01-15T10:30:45...
                date = updated.substring(0, 10);
                time = updated.substring(11, 19);
            }

            // Extract author (user)
            String user = getElementText(entry, "name");

            // Extract summary - this contains full HTML with dump details
            // Critical for ECC fallback when detail endpoints fail
            String summaryHtml = getElementText(entry, "summary");

            // Decode HTML entities in summary for proper parsing
            String decodedSummary = null;
            if (summaryHtml != null && !summaryHtml.isEmpty()) {
                decodedSummary = summaryHtml
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'");
                log.debug("Parsed summary HTML for dump {} | raw: {} chars | decoded: {} chars",
                        dumpId, summaryHtml.length(), decodedSummary.length());
            } else {
                log.warn("No summary HTML found for dump: {}", dumpId);
            }

            // Extract error ID from title or content
            String errorId = extractErrorId(title);

            // Extract program name from summary if available
            String programName = extractProgramFromSummary(summaryHtml);

            return new DumpInfo(
                    dumpId,
                    date,
                    time,
                    null, // host not in feed
                    user,
                    null, // client not in feed
                    errorId,
                    programName,
                    null, // includeName requires detail call
                    0,    // lineNumber requires detail call
                    title,
                    decodedSummary  // Store decoded HTML for ECC fallback
            );

        } catch (Exception e) {
            log.warn("Failed to parse dump entry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses dump summary HTML response.
     *
     * HTML structure from /summary endpoint:
     * - h4#HEADERX: Header Information (table with Short Text, Runtime Error, Program, etc.)
     * - h4#WHATHAPPENED: What happened? (description)
     * - h4#ERROR: Error analysis (analysis text)
     * - h4#TERMINATION: Information on where terminated
     * - h4#SOURCE: Source Code Extract (with line numbers and highlighting)
     * - h4#STACK: Active Calls/Events (table with call stack)
     */
    private DumpDetailResult parseDumpSummaryHtml(String dumpId, String htmlContent) {
        log.debug("Parsing dump summary HTML, length: {}", htmlContent.length());

        try {
            // Extract header information
            String shortText = extractHtmlTableValue(htmlContent, "Short Text");
            String runtimeError = extractHtmlTableValue(htmlContent, "Runtime Error");
            String programName = extractHtmlTableValue(htmlContent, "Program");
            String dateTime = extractHtmlTableValue(htmlContent, "Date/Time");
            String user = extractHtmlTableValue(htmlContent, "User");
            String client = extractHtmlTableValue(htmlContent, "Client");
            String host = extractHtmlTableValue(htmlContent, "Host");

            // Parse date/time
            String date = null;
            String time = null;
            if (dateTime != null && dateTime.contains(" ")) {
                String[] parts = dateTime.split(" ");
                date = parts[0];
                time = parts.length > 1 ? parts[1] : null;
            }

            // Extract sections
            String whatHappened = extractHtmlSection(htmlContent, "WHATHAPPENED", "ERROR");
            String errorAnalysis = extractHtmlSection(htmlContent, "ERROR", "TERMINATION");
            String terminationInfo = extractHtmlSection(htmlContent, "TERMINATION", "SOURCE");

            // Extract source code lines
            List<String> sourceCodeLines = extractHtmlSourceCode(htmlContent);

            // Extract call stack
            List<String> callStack = extractHtmlCallStack(htmlContent);

            // Build how to fix from termination info
            String howToFix = terminationInfo;

            return DumpDetailResult.success(
                    dumpId, date, time, host, user, client,
                    runtimeError, null, programName, null,
                    0, shortText, whatHappened, howToFix,
                    errorAnalysis, callStack, sourceCodeLines, List.of()
            );

        } catch (Exception e) {
            log.error("Error parsing dump summary HTML: {}", e.getMessage(), e);
            return DumpDetailResult.error("Error parsing dump summary: " + e.getMessage());
        }
    }

    /**
     * Extracts a value from HTML table by row label.
     */
    private String extractHtmlTableValue(String html, String label) {
        // Pattern: <td><b>Label&nbsp;</b></td><td nowrap> Value </td>
        String pattern = "<b>" + label + "&nbsp;</b></td><td";
        int idx = html.indexOf(pattern);
        if (idx < 0) {
            // Try without &nbsp;
            pattern = "<b>" + label + "</b></td><td";
            idx = html.indexOf(pattern);
        }
        if (idx < 0) return null;

        int startIdx = html.indexOf(">", idx + pattern.length());
        if (startIdx < 0) return null;
        startIdx++;

        int endIdx = html.indexOf("</td>", startIdx);
        if (endIdx < 0) return null;

        String value = html.substring(startIdx, endIdx).trim();
        // Clean HTML entities
        return cleanHtmlText(value);
    }

    /**
     * Extracts text content between two HTML section IDs.
     */
    private String extractHtmlSection(String html, String startId, String endId) {
        String startMarker = "id=\"" + startId + "\"";
        int startIdx = html.indexOf(startMarker);
        if (startIdx < 0) return null;

        // Find the closing tag of the header
        int contentStart = html.indexOf("</h4>", startIdx);
        if (contentStart < 0) return null;
        contentStart += 5;

        // Find the next section
        String endMarker = "id=\"" + endId + "\"";
        int endIdx = html.indexOf(endMarker, contentStart);
        if (endIdx < 0) {
            // No end marker, take until end or next h4
            endIdx = html.indexOf("<h4", contentStart);
            if (endIdx < 0) endIdx = html.length();
        } else {
            // Go back to find the h4 opening tag
            endIdx = html.lastIndexOf("<h4", endIdx);
            if (endIdx < contentStart) endIdx = html.length();
        }

        String content = html.substring(contentStart, endIdx);
        return cleanHtmlText(content);
    }

    /**
     * Extracts source code lines from HTML source section.
     */
    private List<String> extractHtmlSourceCode(String html) {
        List<String> lines = new ArrayList<>();

        // Find source table
        int sourceIdx = html.indexOf("id=\"SOURCE\"");
        if (sourceIdx < 0) return lines;

        // Find sourceline divs
        int searchStart = sourceIdx;
        while (true) {
            int lineStart = html.indexOf("class=\"sourceline\"", searchStart);
            if (lineStart < 0) break;

            int divStart = html.indexOf(">", lineStart);
            if (divStart < 0) break;
            divStart++;

            int divEnd = html.indexOf("</div>", divStart);
            if (divEnd < 0) break;

            String lineContent = html.substring(divStart, divEnd);
            String cleanLine = cleanHtmlText(lineContent);
            if (!cleanLine.isEmpty()) {
                lines.add(cleanLine);
            }

            searchStart = divEnd;
        }

        return lines;
    }

    /**
     * Extracts call stack from HTML stack section.
     */
    private List<String> extractHtmlCallStack(String html) {
        List<String> stack = new ArrayList<>();

        // Find stack table
        int stackIdx = html.indexOf("id=\"STACK\"");
        if (stackIdx < 0) return stack;

        // Find table rows after the header
        int tableStart = html.indexOf("<table", stackIdx);
        if (tableStart < 0) return stack;

        int searchStart = tableStart;
        boolean skipHeader = true;
        while (true) {
            int rowStart = html.indexOf("<tr>", searchStart);
            if (rowStart < 0) break;

            int rowEnd = html.indexOf("</tr>", rowStart);
            if (rowEnd < 0) break;

            if (skipHeader) {
                skipHeader = false;
                searchStart = rowEnd;
                continue;
            }

            String rowContent = html.substring(rowStart, rowEnd);
            // Extract event and program from row
            String event = extractTableCell(rowContent, 1);
            String program = extractTableCell(rowContent, 2);
            String include = extractTableCell(rowContent, 3);
            String line = extractTableCell(rowContent, 4);

            if (event != null && !event.isEmpty()) {
                StringBuilder stackLine = new StringBuilder();
                stackLine.append(event);
                if (program != null && !program.isEmpty()) {
                    stackLine.append(" in ").append(program);
                }
                if (include != null && !include.isEmpty() && !include.equals(program)) {
                    stackLine.append(" (").append(include).append(")");
                }
                if (line != null && !line.isEmpty()) {
                    stackLine.append(" line ").append(line);
                }
                stack.add(stackLine.toString());
            }

            searchStart = rowEnd;
        }

        return stack;
    }

    /**
     * Extracts a specific cell (0-indexed) from a table row.
     */
    private String extractTableCell(String rowHtml, int cellIndex) {
        int searchStart = 0;
        for (int i = 0; i <= cellIndex; i++) {
            int tdStart = rowHtml.indexOf("<td", searchStart);
            if (tdStart < 0) return null;

            int contentStart = rowHtml.indexOf(">", tdStart);
            if (contentStart < 0) return null;
            contentStart++;

            int tdEnd = rowHtml.indexOf("</td>", contentStart);
            if (tdEnd < 0) return null;

            if (i == cellIndex) {
                return cleanHtmlText(rowHtml.substring(contentStart, tdEnd));
            }

            searchStart = tdEnd;
        }
        return null;
    }

    /**
     * Cleans HTML text by removing tags and decoding entities.
     */
    private String cleanHtmlText(String html) {
        if (html == null) return null;

        // Remove HTML tags
        String text = html.replaceAll("<[^>]+>", " ");

        // Replace common entities
        text = text.replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'")
                   .replace("<br>", "\n")
                   .replace("<br/>", "\n");

        // Normalize whitespace
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    /**
     * Parses dump formatted text response.
     * Fallback parser for text/plain response from /formatted endpoint.
     */
    private DumpDetailResult parseDumpFormattedText(String dumpId, String textContent) {
        log.debug("Parsing dump formatted text, length: {}", textContent.length());

        try {
            String[] lines = textContent.split("\n");

            String runtimeError = extractFormattedField(lines, "Err.tmpo.ejec.");
            String programName = extractFormattedField(lines, "Programa ABAP");
            String shortText = extractFormattedSection(textContent, "Texto breve", "|");
            String whatHappened = extractFormattedSection(textContent, "¿Qué ha sucedido?", "|");
            String errorAnalysis = extractFormattedSection(textContent, "Anál.errores", "|");
            String howToFix = extractFormattedSection(textContent, "Notas para corregir errores", "|");

            // Extract date/time from header
            String dateTime = extractFormattedField(lines, "Fecha y hora");
            String date = null;
            String time = null;
            if (dateTime != null && dateTime.contains(" ")) {
                String[] parts = dateTime.split(" ");
                date = parts[0];
                time = parts.length > 1 ? parts[1] : null;
            }

            // Extract source code
            List<String> sourceCodeLines = extractFormattedSourceCode(textContent);

            // Extract call stack
            List<String> callStack = extractFormattedCallStack(textContent);

            return DumpDetailResult.success(
                    dumpId, date, time, null, null, null,
                    runtimeError, null, programName, null,
                    0, shortText, whatHappened, howToFix,
                    errorAnalysis, callStack, sourceCodeLines, List.of()
            );

        } catch (Exception e) {
            log.error("Error parsing dump formatted text: {}", e.getMessage(), e);
            return DumpDetailResult.error("Error parsing dump formatted text: " + e.getMessage());
        }
    }

    /**
     * Extracts a field value from formatted text lines.
     */
    private String extractFormattedField(String[] lines, String fieldName) {
        for (String line : lines) {
            if (line.contains(fieldName)) {
                int idx = line.indexOf(fieldName);
                String value = line.substring(idx + fieldName.length()).trim();
                // Remove leading dots or spaces
                while (value.startsWith(".") || value.startsWith(" ")) {
                    value = value.substring(1);
                }
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Extracts a section from formatted text.
     */
    private String extractFormattedSection(String text, String sectionName, String linePrefix) {
        int startIdx = text.indexOf(sectionName);
        if (startIdx < 0) return null;

        // Find the section content (lines starting with |)
        int lineStart = text.indexOf("\n", startIdx);
        if (lineStart < 0) return null;

        StringBuilder content = new StringBuilder();
        String[] lines = text.substring(lineStart).split("\n");
        boolean inSection = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(linePrefix)) {
                inSection = true;
                // Remove the prefix and trailing |
                String value = trimmed.substring(1);
                if (value.endsWith("|")) {
                    value = value.substring(0, value.length() - 1);
                }
                content.append(value.trim()).append(" ");
            } else if (trimmed.startsWith("---") && inSection) {
                break; // End of section
            } else if (inSection && !trimmed.isEmpty() && !trimmed.startsWith("---")) {
                break; // New section started
            }
        }

        String result = content.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /**
     * Extracts source code from formatted text.
     */
    private List<String> extractFormattedSourceCode(String text) {
        List<String> lines = new ArrayList<>();

        int sourceIdx = text.indexOf("Detalle código fuente");
        if (sourceIdx < 0) sourceIdx = text.indexOf("Source Code Extract");
        if (sourceIdx < 0) return lines;

        String[] allLines = text.substring(sourceIdx).split("\n");
        boolean inSource = false;

        for (String line : allLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.contains("|")) {
                // Check if it's a source line (starts with line number)
                String content = trimmed.substring(1);
                if (content.matches("^\\s*\\d+\\|.*") || content.matches("^\\s*>+\\|.*")) {
                    inSource = true;
                    lines.add(content.replace("|", " ").trim());
                }
            } else if (trimmed.startsWith("---") && inSource) {
                break;
            }
        }

        return lines;
    }

    /**
     * Extracts call stack from formatted text.
     */
    private List<String> extractFormattedCallStack(String text) {
        List<String> stack = new ArrayList<>();

        int stackIdx = text.indexOf("Llamadas/Eventos activos");
        if (stackIdx < 0) stackIdx = text.indexOf("Active Calls/Events");
        if (stackIdx < 0) return stack;

        String[] allLines = text.substring(stackIdx).split("\n");
        boolean inStack = false;

        for (String line : allLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && !trimmed.contains("Nº") && !trimmed.contains("Nom.")) {
                String content = trimmed.substring(1);
                if (content.endsWith("|")) {
                    content = content.substring(0, content.length() - 1);
                }
                content = content.trim();
                if (!content.isEmpty() && !content.startsWith("---")) {
                    inStack = true;
                    stack.add(content);
                }
            } else if (trimmed.startsWith("---") && inStack) {
                break;
            }
        }

        return stack;
    }

    /**
     * Parses dump metadata XML response (last resort).
     * This endpoint returns basic info with links to other endpoints.
     */
    private DumpDetailResult parseDumpMetadataXml(String dumpId, String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

        Element root = doc.getDocumentElement();

        // Extract basic info
        String date = getAttributeOrElement(root, "date");
        String time = getAttributeOrElement(root, "time");
        String host = getAttributeOrElement(root, "host");
        String user = getAttributeOrElement(root, "user");
        String client = getAttributeOrElement(root, "client");

        // Extract error info
        String runtimeError = getAttributeOrElement(root, "runtimeError");
        if (runtimeError == null) {
            runtimeError = getAttributeOrElement(root, "errorId");
        }
        String exceptionClass = getAttributeOrElement(root, "exception");

        // Extract source location
        String programName = getAttributeOrElement(root, "program");
        String includeName = getAttributeOrElement(root, "include");
        int lineNumber = 0;
        String lineStr = getAttributeOrElement(root, "line");
        if (lineStr != null && !lineStr.isEmpty()) {
            try {
                lineNumber = Integer.parseInt(lineStr.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // Extract text sections
        String shortText = getElementText(root, "shortText");
        if (shortText == null) {
            shortText = getElementText(root, "title");
        }
        String whatHappened = getElementText(root, "whatHappened");
        String howToFix = getElementText(root, "howToFix");
        String errorAnalysis = getElementText(root, "errorAnalysis");

        // Extract call stack
        List<String> callStack = extractCallStack(root);

        // Extract source code
        List<String> sourceCodeLines = extractSourceCode(root);

        // Extract variables
        List<DumpDetailResult.VariableInfo> variables = extractVariables(root);

        return DumpDetailResult.success(
                dumpId, date, time, host, user, client,
                runtimeError, exceptionClass, programName, includeName,
                lineNumber, shortText, whatHappened, howToFix,
                errorAnalysis, callStack, sourceCodeLines, variables
        );
    }

    /**
     * Extracts dump ID from the full path.
     * Path format: /sap/bc/adt/vit/runtime/dumps/{dumpId}
     * Result: just the {dumpId} part (URL-encoded, e.g., "20251201124503vhs4dapci_S4D_00%20%20...SEBLONDO%20%20%20%20100...")
     */
    private String extractDumpIdFromPath(String path) {
        if (path == null) return null;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    /**
     * Extracts runtime error ID from title.
     * Title format: "Runtime Error: GETWA_NOT_ASSIGNED" or similar
     */
    private String extractErrorId(String title) {
        if (title == null) return null;
        // Look for known patterns
        if (title.contains(":")) {
            String[] parts = title.split(":", 2);
            if (parts.length > 1) {
                return parts[1].trim().split("\\s+")[0];
            }
        }
        // Return first word as fallback
        return title.split("\\s+")[0];
    }

    /**
     * Extracts program name from summary text.
     */
    private String extractProgramFromSummary(String summary) {
        if (summary == null) return null;
        // Look for "Program: ZXXX" or "in ZXXX" patterns
        String[] patterns = {"Program:", "program:", "in program", "PROG="};
        for (String pattern : patterns) {
            int idx = summary.indexOf(pattern);
            if (idx >= 0) {
                String remaining = summary.substring(idx + pattern.length()).trim();
                return remaining.split("\\s+")[0];
            }
        }
        return null;
    }

    /**
     * Gets element text content.
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        // Try with namespace
        nodes = parent.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Gets attribute from child element.
     */
    private String getElementAttribute(Element parent, String tagName, String attrName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Element elem = (Element) nodes.item(0);
            return elem.getAttribute(attrName);
        }
        return null;
    }

    /**
     * Gets value from attribute or child element.
     */
    private String getAttributeOrElement(Element element, String name) {
        // Try attribute first
        String value = element.getAttribute(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Try child element
        return getElementText(element, name);
    }

    /**
     * Extracts call stack from dump detail.
     */
    private List<String> extractCallStack(Element root) {
        List<String> stack = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName("stackEntry");
        if (nodes.getLength() == 0) {
            nodes = root.getElementsByTagName("callStackEntry");
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Element entry = (Element) nodes.item(i);
            String line = entry.getTextContent();
            if (line != null && !line.trim().isEmpty()) {
                stack.add(line.trim());
            }
        }
        return stack;
    }

    /**
     * Extracts source code lines from dump detail.
     */
    private List<String> extractSourceCode(Element root) {
        List<String> lines = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName("sourceLine");
        if (nodes.getLength() == 0) {
            nodes = root.getElementsByTagName("sourceCode");
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Element entry = (Element) nodes.item(i);
            String line = entry.getTextContent();
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Extracts variables from dump detail.
     */
    private List<DumpDetailResult.VariableInfo> extractVariables(Element root) {
        List<DumpDetailResult.VariableInfo> variables = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName("variable");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element entry = (Element) nodes.item(i);
            String name = getAttributeOrElement(entry, "name");
            String type = getAttributeOrElement(entry, "type");
            String value = getAttributeOrElement(entry, "value");
            if (value == null) {
                value = entry.getTextContent();
            }
            if (name != null) {
                variables.add(new DumpDetailResult.VariableInfo(name, type, value));
            }
        }
        return variables;
    }

    /**
     * Fallback method to get dump details via custom RFC FM (for ECC systems).
     *
     * Uses ZCX_GET_DUMP_DETAIL which wraps RS_ST22_GET_FT internally.
     * This is used when ADT endpoints return 404 (common on older SAP ECC systems).
     *
     * @param dumpId Dump identifier containing date, time, user, host, client, modno
     * @return DumpDetailResult with parsed dump information
     */
    private DumpDetailResult getDumpDetailsViaRfc(String dumpId) {
        log.info("Attempting to get dump details via RFC FM: {} | dumpId: {}", FM_GET_DUMP_DETAIL, dumpId);

        try {
            // Parse dump ID to extract components
            // Format: YYYYMMDDHHMMSShost____________________user________client_modno
            // Example: 20251202124503vhs4dapci_S4D_00      SEBLONDO    100 0000000001
            DumpIdComponents components = parseDumpId(dumpId);
            if (components == null) {
                log.error("Failed to parse dump ID: {}", dumpId);
                return DumpDetailResult.error("Invalid dump ID format: " + dumpId);
            }

            log.debug("Parsed dump ID components: date={}, time={}, host={}, user={}, client={}, modno={}",
                    components.date, components.time, components.host, components.user, components.client, components.modno);

            // Build FM parameters
            Map<String, String> params = new HashMap<>();
            params.put("IV_DATUM", components.date);   // YYYYMMDD
            params.put("IV_UZEIT", components.time);   // HHMMSS
            params.put("IV_UNAME", components.user);
            params.put("IV_MANDT", components.client);
            params.put("IV_AHOST", components.host);
            params.put("IV_MODNO", components.modno);

            // Call FM
            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(FM_GET_DUMP_DETAIL, params);

            // Check success
            String success = response.exportParams().get("EV_SUCCESS");
            String message = response.exportParams().get("EV_MESSAGE");
            String dumpJson = response.exportParams().get("EV_DUMP_JSON");

            log.debug("FM response: success={}, message={}", success, message);

            if (!"X".equals(success)) {
                log.warn("FM returned failure: {}", message);
                return DumpDetailResult.error("RFC FM failed: " + message);
            }

            // Parse JSON response
            return parseDumpJsonResponse(dumpId, dumpJson);

        } catch (Exception e) {
            log.error("Error calling RFC FM for dump details: {}", e.getMessage(), e);
            return DumpDetailResult.error("RFC FM error: " + e.getMessage());
        }
    }

    /**
     * Parses dump ID to extract components.
     *
     * Dump ID format from ADT feed:
     * YYYYMMDDHHMMSShost________________________user____________client__modno
     * Example: 20251202124503vhs4dapci_S4D_00                  SEBLONDO    100 0000000001
     *
     * Total length: 71 characters (standard SAP SNAP key)
     */
    private DumpIdComponents parseDumpId(String dumpId) {
        if (dumpId == null || dumpId.length() < 20) {
            return null;
        }

        try {
            // URL decode if needed
            String decoded = java.net.URLDecoder.decode(dumpId, StandardCharsets.UTF_8);

            // Extract fixed positions based on SNAP table key structure
            String date = decoded.substring(0, 8);      // DATUM: YYYYMMDD (8 chars)
            String time = decoded.substring(8, 14);     // UZEIT: HHMMSS (6 chars)
            String host = decoded.substring(14, 46).trim();   // AHOST: 32 chars
            String user = decoded.substring(46, 58).trim();   // UNAME: 12 chars
            String client = decoded.substring(58, 61).trim(); // MANDT: 3 chars
            String modno = decoded.length() > 61 ? decoded.substring(61).trim() : "0"; // MODNO: remaining

            // Validate date format
            if (!date.matches("\\d{8}")) {
                log.warn("Invalid date in dump ID: {}", date);
                return null;
            }

            return new DumpIdComponents(date, time, host, user, client, modno);

        } catch (Exception e) {
            log.error("Error parsing dump ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Record for dump ID components.
     */
    private record DumpIdComponents(
            String date,   // YYYYMMDD
            String time,   // HHMMSS
            String host,   // Application server host
            String user,   // SAP user
            String client, // SAP client
            String modno   // Mode number
    ) {}

    /**
     * Parses JSON response from ZCX_GET_DUMP_DETAIL FM.
     */
    private DumpDetailResult parseDumpJsonResponse(String dumpId, String jsonString) {
        log.debug("Parsing FM JSON response, length: {}", jsonString != null ? jsonString.length() : 0);

        if (jsonString == null || jsonString.isEmpty() || "{}".equals(jsonString)) {
            return DumpDetailResult.error("Empty response from FM");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonString);

            String date = getJsonText(root, "datum");
            String time = getJsonText(root, "uzeit");
            String user = getJsonText(root, "uname");
            String client = getJsonText(root, "mandt");
            String host = getJsonText(root, "ahost");
            String runtimeError = getJsonText(root, "runtimeError");
            String program = getJsonText(root, "program");
            String include = getJsonText(root, "include");
            String line = getJsonText(root, "line");
            String tcode = getJsonText(root, "transactionCode");
            String shortText = getJsonText(root, "shortText");
            String whatHappened = getJsonText(root, "whatHappened");
            String errorAnalysis = getJsonText(root, "errorAnalysis");

            int lineNumber = 0;
            if (line != null && !line.isEmpty()) {
                try {
                    lineNumber = Integer.parseInt(line.trim());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            // Parse call stack array
            List<String> callStack = new ArrayList<>();
            JsonNode stackArray = root.get("callStack");
            if (stackArray != null && stackArray.isArray()) {
                for (JsonNode item : stackArray) {
                    String stackLine = item.asText();
                    if (stackLine != null && !stackLine.isEmpty()) {
                        callStack.add(stackLine);
                    }
                }
            }

            // Parse source code array
            List<String> sourceCode = new ArrayList<>();
            JsonNode sourceArray = root.get("sourceCode");
            if (sourceArray != null && sourceArray.isArray()) {
                for (JsonNode item : sourceArray) {
                    String srcLine = item.asText();
                    if (srcLine != null && !srcLine.isEmpty()) {
                        sourceCode.add(srcLine);
                    }
                }
            }

            log.info("Successfully parsed dump from FM: error={}, program={}, callStack={} entries, sourceCode={} lines",
                    runtimeError, program, callStack.size(), sourceCode.size());

            return DumpDetailResult.success(
                    dumpId, date, time, host, user, client,
                    runtimeError, null, program, include,
                    lineNumber, shortText, whatHappened, null,
                    errorAnalysis, callStack, sourceCode, List.of()
            );

        } catch (Exception e) {
            log.error("Error parsing FM JSON response: {}", e.getMessage(), e);
            return DumpDetailResult.error("Error parsing FM response: " + e.getMessage());
        }
    }

    /**
     * Safely gets text from JSON node.
     */
    private String getJsonText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}

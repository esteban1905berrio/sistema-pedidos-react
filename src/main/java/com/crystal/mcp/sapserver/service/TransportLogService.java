package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.config.JCoConfiguration;
import com.crystal.mcp.sapserver.model.TransportLogResult;
import com.crystal.mcp.sapserver.model.TransportLogResult.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving SAP Transport Log information.
 *
 * <p>This service retrieves transport logs from SAP CTS system using the
 * ZCX_GET_TRANSPORT_LOGS function module, which internally calls
 * STRF_READ_COFILE and TRINT_READ_LOG.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Returns ONLY errors and warnings (not informational messages)</li>
 *   <li>Supports multiple transport numbers as input</li>
 *   <li>Optional filter by transport owner (AS4USER)</li>
 *   <li>Supports all transport types: Workbench (K), Customizing (W), Copies (T)</li>
 * </ul>
 *
 * <h3>Token Optimization:</h3>
 * <ul>
 *   <li>Returns problems only, not full log</li>
 *   <li>Transports without issues are summarized in counts</li>
 * </ul>
 *
 * <h3>Progressive Discovery Integration:</h3>
 * <ul>
 *   <li>Use after list_user_transports or get_transport_objects</li>
 *   <li>Answers: "Did this transport have import errors?"</li>
 * </ul>
 *
 * <p>Thread Safety: Stateless service, thread-safe via JCo connection pool.</p>
 *
 * <h3>Troubleshooting - Known Issues:</h3>
 *
 * <p><b>Error: "Type conflict during a function module call"</b></p>
 * <p>If this error occurs, the problem is <b>NOT</b> in the MCP server Java code.
 * The issue is in the ABAP Function Module (ZCX_GET_TRANSPORT_LOGS) in SAP.
 * This typically happens when the FM calls another FM (like STRF_READ_COFILE
 * or TRINT_READ_LOG) with an incorrect parameter type.</p>
 *
 * <p><b>Solution:</b> Review and fix the FM implementation in SAP via SE37/SE80.
 * Check that all internal FM calls use the correct parameter types as defined
 * in the called FM's signature.</p>
 *
 * <p><b>Common causes:</b></p>
 * <ul>
 *   <li>Passing STRING where CHAR is expected</li>
 *   <li>Using wrong structure type for tables</li>
 *   <li>Mismatched IMPORTING/EXPORTING parameter types</li>
 * </ul>
 *
 * @see TransportLogResult
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportLogService {

    private final JCoConfiguration jCoConfiguration;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get transport log for one or more transports.
     *
     * This method queries transport logs and filters for errors/warnings only.
     * Uses FM ZCX_GET_TRANSPORT_LOGS which calls IF_CTS_REST_API->READ_GLOBAL_INFO.
     *
     * Input Format:
     * - Single: "CADK911088"
     * - Multiple (comma-separated): "CADK911088,CADK911122"
     * - Multiple (JSON array): "[\"CADK911088\", \"CADK911122\"]"
     *
     * Severity Mapping:
     * - Color 6 = Error (E)
     * - Color 2 = Warning (W)
     * - Other colors = Ignored (informational)
     *
     * @param transports Transport number(s) - single, comma-separated, or JSON array
     * @param user Optional filter by transport owner (AS4USER from E070)
     * @return TransportLogResult with problems (errors/warnings) only
     * @throws RuntimeException if FM call fails
     */
    public TransportLogResult getTransportLog(String transports, String user) {
        // Validate input
        if (transports == null || transports.trim().isEmpty()) {
            throw new IllegalArgumentException("Transport number(s) cannot be empty");
        }

        log.info("Getting transport log for: {} (user filter: {})",
                transports, user != null ? user : "none");

        try {
            // Get destination
            JCoDestination destination = jCoConfiguration.jcoDestination();

            // Clear cached metadata for this FM to ensure fresh signature
            // This is needed when FM was recently modified in SAP
            String fmName = "ZCX_GET_TRANSPORT_LOGS";
            try {
                destination.getRepository().removeFunctionTemplateFromCache(fmName);
                log.debug("Cleared cache for FM: {}", fmName);
            } catch (Exception e) {
                log.debug("Could not clear cache for FM {}: {}", fmName, e.getMessage());
            }

            // Get function module
            JCoFunction function = destination.getRepository().getFunction(fmName);

            if (function == null) {
                throw new RuntimeException(
                        "Function module " + fmName + " not found. " +
                        "Please verify FM exists in SAP system (GDC)."
                );
            }

            // Log FM metadata for debugging
            log.debug("FM {} - Import params: {}", fmName,
                    function.getImportParameterList().getMetaData());

            // Set import parameters - use UPPERCASE for parameter names
            JCoParameterList importParams = function.getImportParameterList();
            importParams.setValue("IV_TRANSPORTS", transports.trim() );
            if (user != null && !user.trim().isEmpty()) {
                importParams.setValue("IV_USER", user.trim().toUpperCase());
            }

            // Execute function
            function.execute(destination);

            // Get export parameters
            JCoParameterList exportParams = function.getExportParameterList();
            String hasProblemsFlag = exportParams.getString("EV_HAS_PROBLEMS");
            boolean hasProblems = "X".equals(hasProblemsFlag) || "true".equalsIgnoreCase(hasProblemsFlag);
            String jsonString = exportParams.getString("EV_RESULT_JSON");

            log.info("FM returned hasProblems={}, JSON length={} bytes",
                    hasProblems, jsonString != null ? jsonString.length() : 0);

            // Parse JSON response
            return parseTransportLogJson(jsonString, transports, user, hasProblems);

        } catch (JCoException e) {
            log.error("JCo error calling ZCX_GET_TRANSPORT_LOGS: {}", e.getMessage(), e);
            return TransportLogResult.failure("JCo error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting transport log: {}", e.getMessage(), e);
            return TransportLogResult.failure(e.getMessage());
        }
    }

    /**
     * Parse JSON response from ZCX_GET_TRANSPORT_LOGS function module.
     *
     * Expected JSON structure from ABAP:
     * [
     *   {
     *     "trkorr": "CADK900123",
     *     "owner": "DEVELOPER",
     *     "type": "K",
     *     "typeText": "Workbench",
     *     "description": "FI: Invoice processing",
     *     "hasLog": true,
     *     "hasProblems": true,
     *     "errorCount": 2,
     *     "warningCount": 1,
     *     "problems": [
     *       {
     *         "severity": "E",
     *         "message": "Object locked",
     *         "system": "QAS",
     *         "timestamp": "20251203143022",
     *         "step": "I",
     *         "stepText": "Import"
     *       }
     *     ],
     *     "message": null
     *   }
     * ]
     *
     * @param jsonString JSON array from FM
     * @param transports Original transport input
     * @param user User filter applied
     * @param hasProblems Flag from FM
     * @return TransportLogResult
     */
    private TransportLogResult parseTransportLogJson(
            String jsonString,
            String transports,
            String user,
            boolean hasProblems
    ) {
        try {
            // Parse transport list from input
            List<String> requestedTransports = parseTransportInput(transports);

            // Build query info
            QueryInfo queryInfo = new QueryInfo(
                requestedTransports,
                user,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            );

            // Handle empty or null JSON
            if (jsonString == null || jsonString.trim().isEmpty() || "[]".equals(jsonString.trim())) {
                log.info("No transport data returned from FM");
                return TransportLogResult.success(
                    queryInfo,
                    new Summary(requestedTransports.size(), 0, 0, requestedTransports.size()),
                    List.of()
                );
            }

            // Parse JSON array
            List<Map<String, Object>> rawEntries = objectMapper.readValue(
                jsonString,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Convert to TransportLogEntry objects
            List<TransportLogEntry> entries = new ArrayList<>();
            int withErrors = 0;
            int withWarnings = 0;
            int withoutLog = 0;

            for (Map<String, Object> raw : rawEntries) {
                TransportLogEntry entry = mapToTransportLogEntry(raw);
                entries.add(entry);

                // Update counts
                if (!entry.hasLog()) {
                    withoutLog++;
                } else if (entry.hasErrors()) {
                    withErrors++;
                } else if (entry.hasWarnings()) {
                    withWarnings++;
                }
            }

            // Build summary
            Summary summary = new Summary(
                entries.size(),
                withErrors,
                withWarnings,
                withoutLog
            );

            return TransportLogResult.success(queryInfo, summary, entries);

        } catch (Exception e) {
            log.error("Error parsing JSON from FM: {}", e.getMessage(), e);
            return TransportLogResult.failure("Failed to parse transport log: " + e.getMessage());
        }
    }

    /**
     * Parse transport input into list of transport numbers.
     *
     * Handles:
     * - Single: "CADK911088"
     * - Comma-separated: "CADK911088,CADK911122"
     * - JSON array: "[\"CADK911088\", \"CADK911122\"]"
     *
     * @param transports Input string
     * @return List of transport numbers
     */
    private List<String> parseTransportInput(String transports) {
        List<String> result = new ArrayList<>();
        String input = transports.trim();

        // Handle JSON array format
        if (input.startsWith("[")) {
            try {
                List<String> parsed = objectMapper.readValue(input, new TypeReference<>() {});
                return parsed.stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .filter(s -> !s.isEmpty())
                    .toList();
            } catch (Exception e) {
                log.warn("Failed to parse as JSON array, trying comma-separated: {}", e.getMessage());
            }
        }

        // Handle comma-separated or single
        String[] parts = input.split(",");
        for (String part : parts) {
            String cleaned = part.trim()
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "")
                .toUpperCase();
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }

        return result;
    }

    /**
     * Map raw JSON map to TransportLogEntry record.
     *
     * @param raw Raw map from JSON parsing
     * @return TransportLogEntry record
     */
    @SuppressWarnings("unchecked")
    private TransportLogEntry mapToTransportLogEntry(Map<String, Object> raw) {
        // Extract basic fields
        String trkorr = getStringValue(raw, "trkorr");
        String owner = getStringValue(raw, "owner");
        String type = getStringValue(raw, "type");
        String typeText = getStringValue(raw, "typeText");
        String description = getStringValue(raw, "description");
        boolean hasLog = getBooleanValue(raw, "hasLog");
        boolean hasProblems = getBooleanValue(raw, "hasProblems");
        int errorCount = getIntValue(raw, "errorCount");
        int warningCount = getIntValue(raw, "warningCount");
        String message = getStringValue(raw, "message");

        // Parse problems array
        List<Problem> problems = new ArrayList<>();
        Object problemsRaw = raw.get("problems");
        if (problemsRaw instanceof List<?> problemsList) {
            for (Object problemRaw : problemsList) {
                if (problemRaw instanceof Map<?, ?> problemMap) {
                    problems.add(mapToProblem((Map<String, Object>) problemMap));
                }
            }
        }

        return new TransportLogEntry(
            trkorr,
            owner,
            type,
            typeText,
            description,
            hasLog,
            hasProblems,
            errorCount,
            warningCount,
            problems,
            message
        );
    }

    /**
     * Map raw JSON map to Problem record.
     *
     * @param raw Raw map from JSON parsing
     * @return Problem record
     */
    private Problem mapToProblem(Map<String, Object> raw) {
        return new Problem(
            getStringValue(raw, "severity"),
            getStringValue(raw, "message"),
            getStringValue(raw, "system"),
            getStringValue(raw, "timestamp"),
            getStringValue(raw, "step"),
            getStringValue(raw, "stepText")
        );
    }

    /**
     * Safely get string value from map.
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Safely get boolean value from map.
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s) || "X".equals(s);
        }
        return false;
    }

    /**
     * Safely get int value from map.
     */
    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}

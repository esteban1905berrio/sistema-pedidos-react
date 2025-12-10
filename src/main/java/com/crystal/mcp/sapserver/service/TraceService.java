package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TraceAnalysisResult;
import com.crystal.mcp.sapserver.model.TraceCallStackItem;
import com.crystal.mcp.sapserver.model.TraceDetailedRecord;
import com.crystal.mcp.sapserver.model.TraceTableAccessRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ST05 SQL trace analysis of SAP transactions.
 *
 * Provides TWO approaches for trace analysis:
 *
 * 1. HYBRID (Recommended) - Human-in-the-loop:
 *    - activateTrace(): Agent activates ST05 trace for a user
 *    - USER manually executes any transaction in SAP GUI
 *    - deactivateAndReadTrace(): Agent stops trace and reads results
 *    Benefits: Works with ANY transaction, no BDC needed
 *
 * 2. AUTOMATED (Limited) - For non-screen transactions only:
 *    - traceTransaction(): Executes transaction via RFC and captures trace
 *    Limitation: DUMPS if transaction displays screens/dynpros
 *
 * Use cases:
 * - Analyze standard programs to understand value calculations
 * - Identify tables accessed during transaction execution
 * - Find source code locations where data is read/written
 * - Performance analysis of SQL operations
 *
 * Token Optimization:
 * - Returns structured JSON with configurable max_records limit
 * - Call stack optional (iv_with_call_stack parameter)
 * - Results sorted by duration for quick identification of hot spots
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {

    private final RfcAdapter rfcAdapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Custom FMs for trace analysis
    private static final String FM_TRACE_TRANSACTION = "ZCX_TRACE_TRANSACTION";
    private static final String FM_TRACE_ACTIVATE = "ZCX_TRACE_ACTIVATE";
    private static final String FM_TRACE_DEACTIVATE_READ = "ZCX_TRACE_DEACTIVATE_AND_READ";

    /**
     * Execute transaction with ST05 trace and return analysis results.
     *
     * Activates SQL trace, executes the transaction, deactivates trace, and returns
     * structured analysis of all SQL operations performed during execution.
     *
     * @param transaction    SAP transaction code (e.g., "FMAVCH01", "VA03")
     * @param variant        Selection variant name (for reports). Null for BDC execution.
     * @param traceSql       Enable SQL trace (default: true)
     * @param traceBuffer    Enable buffer trace (default: false)
     * @param traceEnqueue   Enable enqueue trace (default: false)
     * @param withCallStack  Include ABAP call stack in trace (default: true)
     * @param maxRecords     Maximum number of detailed records to return (default: 1000)
     * @return TraceAnalysisResult with trace data or error message
     */
    public TraceAnalysisResult traceTransaction(
            String transaction,
            String variant,
            boolean traceSql,
            boolean traceBuffer,
            boolean traceEnqueue,
            boolean withCallStack,
            int maxRecords) {

        log.info("Tracing transaction | tcode: {} | variant: {} | sql: {} | buffer: {} | enqueue: {} | stack: {} | maxRecords: {}",
                transaction, variant, traceSql, traceBuffer, traceEnqueue, withCallStack, maxRecords);

        if (transaction == null || transaction.isEmpty()) {
            return TraceAnalysisResult.error("Transaction code is required");
        }

        try {
            // Build FM parameters
            Map<String, String> params = new HashMap<>();
            params.put("IV_TRANSACTION", transaction.toUpperCase());

            if (variant != null && !variant.isEmpty()) {
                params.put("IV_VARIANT", variant.toUpperCase());
            }

            params.put("IV_TRACE_SQL", traceSql ? "X" : " ");
            params.put("IV_TRACE_BUFFER", traceBuffer ? "X" : " ");
            params.put("IV_TRACE_ENQUEUE", traceEnqueue ? "X" : " ");
            params.put("IV_WITH_CALL_STACK", withCallStack ? "X" : " ");
            params.put("IV_MAX_RECORDS", String.valueOf(maxRecords > 0 ? maxRecords : 1000));

            // Call FM
            long startTime = System.currentTimeMillis();
            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(FM_TRACE_TRANSACTION, params);
            long callDuration = System.currentTimeMillis() - startTime;

            log.debug("FM call completed in {} ms", callDuration);

            // Check for error
            String errorMessage = response.getExportParam("EV_ERROR_MESSAGE");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                log.warn("FM returned error: {}", errorMessage);
                return TraceAnalysisResult.error(errorMessage);
            }

            // Extract metrics
            int traceDurationMs = parseIntSafe(response.getExportParam("EV_TRACE_DURATION_MS"));
            int totalStatements = parseIntSafe(response.getExportParam("EV_TOTAL_STATEMENTS"));
            int tablesCount = parseIntSafe(response.getExportParam("EV_TABLES_COUNT"));

            // Parse JSON arrays
            String detailedJson = response.getExportParam("EV_DETAILED_RECORDS_JSON");
            String callStackJson = response.getExportParam("EV_CALL_STACK_JSON");
            String tableAccessJson = response.getExportParam("EV_TABLE_ACCESS_JSON");

            List<TraceDetailedRecord> detailedRecords = parseDetailedRecords(detailedJson);
            List<TraceCallStackItem> callStackItems = parseCallStackItems(callStackJson);
            List<TraceTableAccessRecord> tableAccessRecords = parseTableAccessRecords(tableAccessJson);

            log.info("Trace completed | duration: {}ms | statements: {} | tables: {} | callStack: {} entries",
                    traceDurationMs, totalStatements, tablesCount, callStackItems.size());

            return TraceAnalysisResult.success(
                    transaction,
                    variant,
                    null, // program extracted from trace if needed
                    traceDurationMs,
                    null, null, null, null, // dates/times can be extracted from records if needed
                    detailedRecords,
                    callStackItems,
                    tableAccessRecords
            );

        } catch (JCoException e) {
            log.error("RFC error tracing transaction: {}", e.getMessage(), e);
            return TraceAnalysisResult.error("RFC error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error tracing transaction: {}", e.getMessage(), e);
            return TraceAnalysisResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * Simplified trace method with default parameters.
     *
     * Traces SQL only, with call stack, up to 1000 records.
     */
    public TraceAnalysisResult traceTransaction(String transaction, String variant) {
        return traceTransaction(transaction, variant, true, false, false, true, 1000);
    }

    /**
     * Parse detailed records JSON from FM response.
     */
    private List<TraceDetailedRecord> parseDetailedRecords(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<TraceDetailedRecord>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse detailed records JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse call stack items JSON from FM response.
     */
    private List<TraceCallStackItem> parseCallStackItems(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<TraceCallStackItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse call stack JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse table access records JSON from FM response.
     */
    private List<TraceTableAccessRecord> parseTableAccessRecords(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<TraceTableAccessRecord>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse table access JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Safely parse integer from string.
     */
    private int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ============================================================================
    // HYBRID APPROACH - Human-in-the-loop (Recommended)
    // ============================================================================

    /**
     * Result record for trace activation.
     */
    public record TraceActivationResult(
            boolean success,
            String message,
            String startDate,
            String startTime
    ) {
        public static TraceActivationResult success(String message, String startDate, String startTime) {
            return new TraceActivationResult(true, message, startDate, startTime);
        }

        public static TraceActivationResult error(String message) {
            return new TraceActivationResult(false, message, null, null);
        }
    }

    /**
     * Activate ST05 trace for a user (Step 1 of hybrid approach).
     *
     * After calling this, the USER should manually execute their transaction
     * in SAP GUI. Then call deactivateAndReadTrace() to get results.
     *
     * @param traceUser     SAP user to trace (defaults to current user)
     * @param traceSql      Enable SQL trace
     * @param traceBuffer   Enable buffer trace
     * @param traceEnqueue  Enable enqueue trace
     * @param withCallStack Include ABAP call stack in trace
     * @return TraceActivationResult with success/error status and start timestamp
     */
    public TraceActivationResult activateTrace(
            String traceUser,
            boolean traceSql,
            boolean traceBuffer,
            boolean traceEnqueue,
            boolean withCallStack) {

        log.info("Activating trace | user: {} | sql: {} | buffer: {} | enqueue: {} | stack: {}",
                traceUser, traceSql, traceBuffer, traceEnqueue, withCallStack);

        try {
            Map<String, String> params = new HashMap<>();
            if (traceUser != null && !traceUser.isEmpty()) {
                params.put("IV_TRACE_USER", traceUser.toUpperCase());
            }
            params.put("IV_TRACE_SQL", traceSql ? "X" : " ");
            params.put("IV_TRACE_BUFFER", traceBuffer ? "X" : " ");
            params.put("IV_TRACE_ENQUEUE", traceEnqueue ? "X" : " ");
            params.put("IV_WITH_CALL_STACK", withCallStack ? "X" : " ");

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(FM_TRACE_ACTIVATE, params);

            String success = response.getExportParam("EV_SUCCESS");
            String message = response.getExportParam("EV_MESSAGE");
            String startDate = response.getExportParam("EV_START_DATE");
            String startTime = response.getExportParam("EV_START_TIME");

            if ("X".equals(success)) {
                log.info("Trace activated successfully: {}", message);
                return TraceActivationResult.success(message, startDate, startTime);
            } else {
                log.warn("Trace activation failed: {}", message);
                return TraceActivationResult.error(message);
            }

        } catch (JCoException e) {
            log.error("RFC error activating trace: {}", e.getMessage(), e);
            return TraceActivationResult.error("RFC error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error activating trace: {}", e.getMessage(), e);
            return TraceActivationResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * Simplified trace activation with defaults (SQL trace with call stack).
     */
    public TraceActivationResult activateTrace(String traceUser) {
        return activateTrace(traceUser, true, false, false, true);
    }

    /**
     * Deactivate ST05 trace and read results (Step 2 of hybrid approach).
     *
     * Call this AFTER the user has manually executed their transaction in SAP GUI.
     * Returns structured trace analysis results.
     *
     * @param traceUser  SAP user whose trace to read
     * @param maxRecords Maximum number of detailed records to return
     * @return TraceAnalysisResult with trace data or error message
     */
    public TraceAnalysisResult deactivateAndReadTrace(String traceUser, int maxRecords) {
        log.info("Deactivating trace and reading results | user: {} | maxRecords: {}", traceUser, maxRecords);

        try {
            Map<String, String> params = new HashMap<>();
            if (traceUser != null && !traceUser.isEmpty()) {
                params.put("IV_TRACE_USER", traceUser.toUpperCase());
            }
            params.put("IV_MAX_RECORDS", String.valueOf(maxRecords > 0 ? maxRecords : 500));

            long startTime = System.currentTimeMillis();
            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(FM_TRACE_DEACTIVATE_READ, params);
            long callDuration = System.currentTimeMillis() - startTime;

            log.debug("FM call completed in {} ms", callDuration);

            // Check for error
            String errorMessage = response.getExportParam("EV_ERROR_MESSAGE");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                log.warn("FM returned error: {}", errorMessage);
                return TraceAnalysisResult.error(errorMessage);
            }

            // Parse JSON arrays
            String detailedJson = response.getExportParam("EV_DETAILED_JSON");
            String callStackJson = response.getExportParam("EV_CALL_STACK_JSON");
            String tableAccessJson = response.getExportParam("EV_TABLE_ACCESS_JSON");

            List<TraceDetailedRecord> detailedRecords = parseDetailedRecords(detailedJson);
            List<TraceCallStackItem> callStackItems = parseCallStackItems(callStackJson);
            List<TraceTableAccessRecord> tableAccessRecords = parseTableAccessRecords(tableAccessJson);

            // Calculate metrics from results
            int totalStatements = detailedRecords.size();
            int tablesCount = tableAccessRecords.size();
            int traceDurationMs = (int) callDuration;

            log.info("Trace read completed | statements: {} | tables: {} | callStack: {} entries",
                    totalStatements, tablesCount, callStackItems.size());

            return TraceAnalysisResult.success(
                    null, // transaction not known in hybrid approach
                    null, // variant not known
                    null, // program can be extracted from trace
                    traceDurationMs,
                    null, null, null, null,
                    detailedRecords,
                    callStackItems,
                    tableAccessRecords
            );

        } catch (JCoException e) {
            log.error("RFC error reading trace: {}", e.getMessage(), e);
            return TraceAnalysisResult.error("RFC error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error reading trace: {}", e.getMessage(), e);
            return TraceAnalysisResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * Simplified deactivate and read with default max records.
     */
    public TraceAnalysisResult deactivateAndReadTrace(String traceUser) {
        return deactivateAndReadTrace(traceUser, 500);
    }
}

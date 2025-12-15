package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.TraceAnalysisResult;
import com.crystal.mcp.sapserver.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for SAP ST05 SQL Trace Analysis.
 *
 * This component provides TWO approaches for SQL trace analysis:
 *
 * 1. HYBRID (Recommended) - Works with ANY transaction:
 *    - activate_trace(): Agent starts ST05 trace for a user
 *    - USER manually executes their transaction in SAP GUI
 *    - deactivate_and_read_trace(): Agent stops trace and reads results
 *    Benefits: Works with screen transactions, no BDC needed
 *
 * 2. AUTOMATED (Limited) - For non-screen transactions only:
 *    - trace_transaction(): Executes transaction via RFC and captures trace
 *    Limitation: DUMPS if transaction displays screens/dynpros
 *
 * Use Cases:
 * - Understand how a standard program calculates a specific value
 * - Identify tables read during transaction execution
 * - Find source code locations where data is accessed
 * - Performance analysis of SQL operations
 *
 * Example Hybrid Workflow:
 * 1. User: "How does VA03 calculate the net value?"
 * 2. Claude: activate_trace('DEVELOPER')
 * 3. Claude: "Please execute VA03 with your test sales order in SAP GUI"
 * 4. User: Executes VA03 manually
 * 5. Claude: deactivate_and_read_trace('DEVELOPER')
 * 6. Claude: Analyzes tables VBAP, VBAK, KONV in trace
 * 7. Claude: get_class_source('CL_SD_ITEM') → Reads relevant code
 *
 * Requirements:
 * - Custom FMs ZCX_TRACE_ACTIVATE, ZCX_TRACE_DEACTIVATE_AND_READ must exist
 * - User needs S_ADMI_FCD authorization with ST0M/ST0R values
 */
@Component
@RequiredArgsConstructor
public class TraceTools {

    private final TraceService traceService;

    /**
     * MCP Tool: Execute transaction with ST05 trace and analyze SQL operations.
     *
     * Activates SQL trace, executes the specified transaction, deactivates trace,
     * and returns structured analysis of all database operations performed.
     *
     * Token Optimization:
     * - Returns markdown-formatted summary for LLM consumption
     * - Configurable max_records to limit output size
     * - Call stack optional to reduce output
     *
     * Use Case:
     * Use this tool to:
     * - Analyze standard SAP programs to understand calculations
     * - Find which tables are read for specific business operations
     * - Identify source code locations of data access (program:line)
     * - Performance analysis of SQL operations
     *
     * Workflow Example:
     * 1. User: "How is the budget consumed in FM?"
     * 2. Claude: trace_transaction('FMAVCH01', 'PRE') → Gets trace analysis
     * 3. Claude: Identifies tables FMIFIIT, FMAVCHD in trace
     * 4. Claude: get_program_source('RFFMAVC_HANA_VIEW') → Reads relevant code
     * 5. Claude: Explains calculation logic based on trace + source
     *
     * @param transaction     SAP transaction code to execute and trace
     * @param variant         Selection variant name (for reports)
     * @param traceSql        Enable SQL trace (default: true)
     * @param traceBuffer     Enable buffer trace (default: false)
     * @param traceEnqueue    Enable enqueue trace (default: false)
     * @param withCallStack   Include ABAP call stack (default: true)
     * @param maxRecords      Maximum detailed records to return (default: 500)
     * @return TraceAnalysisResult with trace data and markdown summary
     */
    @McpTool(
            description = "Execute SAP transaction with ST05 SQL trace and return analysis. " +
                    "Captures all SQL operations during transaction execution, including: " +
                    "- Tables accessed with operation type (SELECT, INSERT, UPDATE, DELETE) " +
                    "- Source code location (program + line offset) " +
                    "- ABAP call stack for each SQL statement " +
                    "- Execution duration and row counts. " +
                    "Use for analyzing standard programs, understanding value calculations, " +
                    "and identifying data access patterns. " +
                    "Requires transaction to be executable with variant or non-interactively. " +
                    "Token cost: ~2000-5000 tokens (depends on SQL activity). " +
                    "Authorization required: S_ADMI_FCD with ST0M/ST0R values."
    )
    public TraceAnalysisResult trace_transaction(
            @McpToolParam(
                    description = "SAP transaction code to execute and trace. " +
                            "Examples: 'FMAVCH01' (FM availability), 'VA03' (display sales order), " +
                            "'MB51' (material document list). " +
                            "Transaction must be executable non-interactively.",
                    required = true
            )
            String transaction,
            @McpToolParam(
                    description = "Selection variant name for report transactions. " +
                            "Required for report-type transactions (SE38/SA38 programs). " +
                            "Example: 'PRE' for pre-configured selection parameters. " +
                            "Leave empty for transactions using BDC or default values.",
                    required = false
            )
            String variant,
            @McpToolParam(
                    description = "Enable SQL trace (database operations). " +
                            "Default: true. Set to false to exclude SQL from trace.",
                    required = false
            )
            Boolean traceSql,
            @McpToolParam(
                    description = "Enable buffer trace (table buffer operations). " +
                            "Default: false. Enable for buffer analysis scenarios.",
                    required = false
            )
            Boolean traceBuffer,
            @McpToolParam(
                    description = "Enable enqueue trace (lock operations). " +
                            "Default: false. Enable for lock analysis scenarios.",
                    required = false
            )
            Boolean traceEnqueue,
            @McpToolParam(
                    description = "Include ABAP call stack for each SQL statement. " +
                            "Default: true. Shows FORM/METHOD/FM that issued the SQL. " +
                            "Set to false to reduce output size.",
                    required = false
            )
            Boolean withCallStack,
            @McpToolParam(
                    description = "Maximum number of detailed SQL records to return. " +
                            "Default: 500. Increase for comprehensive analysis, " +
                            "decrease for faster response with less tokens.",
                    required = false
            )
            Integer maxRecords
    ) {
        return traceService.traceTransaction(
                transaction,
                variant,
                traceSql != null ? traceSql : true,
                traceBuffer != null ? traceBuffer : false,
                traceEnqueue != null ? traceEnqueue : false,
                withCallStack != null ? withCallStack : true,
                maxRecords != null ? maxRecords : 500
        );
    }

    // ============================================================================
    // HYBRID APPROACH - Human-in-the-loop (Recommended for screen transactions)
    // ============================================================================

    /**
     * MCP Tool: Activate ST05 trace for a user (Step 1 of hybrid approach).
     *
     * After calling this tool, ask the user to manually execute their transaction
     * in SAP GUI. Then call deactivate_and_read_trace() to get results.
     *
     * This approach works with ANY transaction, including those that display
     * screens/dynpros, which would fail with the automated trace_transaction tool.
     *
     * @param traceUser     SAP user to trace (defaults to RFC connection user)
     * @param traceSql      Enable SQL trace (default: true)
     * @param traceBuffer   Enable buffer trace (default: false)
     * @param traceEnqueue  Enable enqueue trace (default: false)
     * @param withCallStack Include ABAP call stack (default: true)
     * @return TraceActivationResult with success status and start timestamp
     */
    @McpTool(
            description = "HYBRID Step 1: Activate ST05 SQL trace for a SAP user. " +
                    "After calling this, ask the user to execute their transaction in SAP GUI, " +
                    "then call deactivate_and_read_trace() to get results. " +
                    "Works with ANY transaction including screen-based ones (VA01, ME21N, etc.). " +
                    "Use this approach when trace_transaction fails with screen/dynpro errors. " +
                    "Token cost: ~100 tokens. " +
                    "Authorization required: S_ADMI_FCD with ST0M value."
    )
    public TraceService.TraceActivationResult activate_trace(
            @McpToolParam(
                    description = "SAP username to trace. " +
                            "Use the username of the person who will execute the transaction. " +
                            "Example: 'DEVELOPER', 'SAPUSER'. " +
                            "Defaults to current user (SY-UNAME) if not specified.",
                    required = false
            )
            String traceUser,
            @McpToolParam(
                    description = "SAP client (mandant) to trace. " +
                            "Example: '100', '200'. " +
                            "Defaults to current client (SY-MANDT) if not specified.",
                    required = false
            )
            String client,
            @McpToolParam(
                    description = "Enable SQL trace (database operations). " +
                            "Default: true. Set to false to exclude SQL from trace.",
                    required = false
            )
            Boolean traceSql,
            @McpToolParam(
                    description = "Enable buffer trace (table buffer operations). " +
                            "Default: false. Enable for buffer analysis scenarios.",
                    required = false
            )
            Boolean traceBuffer,
            @McpToolParam(
                    description = "Enable enqueue trace (lock operations). " +
                            "Default: false. Enable for lock analysis scenarios.",
                    required = false
            )
            Boolean traceEnqueue,
            @McpToolParam(
                    description = "Include ABAP call stack for each SQL statement. " +
                            "Default: true. Shows FORM/METHOD/FM that issued the SQL.",
                    required = false
            )
            Boolean withCallStack
    ) {
        return traceService.activateTrace(
                traceUser,
                client,
                traceSql != null ? traceSql : true,
                traceBuffer != null ? traceBuffer : false,
                traceEnqueue != null ? traceEnqueue : false,
                withCallStack != null ? withCallStack : true
        );
    }

    /**
     * MCP Tool: Deactivate ST05 trace and read results (Step 2 of hybrid approach).
     *
     * Call this AFTER the user has executed their transaction in SAP GUI.
     * Returns structured trace analysis results with tables accessed, SQL operations,
     * and ABAP call stack.
     *
     * @param traceUser  SAP user whose trace to read
     * @param maxRecords Maximum detailed records to return (default: 500)
     * @return TraceAnalysisResult with trace data and markdown summary
     */
    @McpTool(
            description = "HYBRID Step 2: Deactivate ST05 trace and read results. " +
                    "Call this AFTER the user has executed their transaction in SAP GUI. " +
                    "Returns structured analysis of all SQL operations captured, including: " +
                    "- Tables accessed with operation type (SELECT, INSERT, UPDATE, DELETE) " +
                    "- Source code location (program + line offset) " +
                    "- ABAP call stack for each SQL statement " +
                    "- Execution duration and row counts. " +
                    "Token cost: ~2000-5000 tokens (depends on SQL activity). " +
                    "Authorization required: S_ADMI_FCD with ST0R value."
    )
    public TraceAnalysisResult deactivate_and_read_trace(
            @McpToolParam(
                    description = "SAP username whose trace to read. " +
                            "Must match the user specified in activate_trace(). " +
                            "Example: 'DEVELOPER', 'SAPUSER'.",
                    required = false
            )
            String traceUser,
            @McpToolParam(
                    description = "Maximum number of detailed SQL records to return. " +
                            "Default: 500. Increase for comprehensive analysis, " +
                            "decrease for faster response with less tokens.",
                    required = false
            )
            Integer maxRecords
    ) {
        return traceService.deactivateAndReadTrace(
                traceUser,
                maxRecords != null ? maxRecords : 500
        );
    }
}

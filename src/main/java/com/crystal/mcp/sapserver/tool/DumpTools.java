package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.DumpDetailResult;
import com.crystal.mcp.sapserver.model.DumpListResult;
import com.crystal.mcp.sapserver.service.DumpService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for SAP ABAP Dump Analysis (ST22).
 *
 * This component provides tools for analyzing ABAP runtime errors (short dumps)
 * from SAP systems. Equivalent to transaction ST22 functionality.
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Progressive Discovery Workflow:
 * Stage 1: list_dumps → Find dumps by date/user (lightweight)
 * Stage 2: get_dump_details → Get full dump content (if needed)
 *
 * Available Tools:
 * - list_dumps: List ABAP dumps by date range and/or user
 * - get_dump_details: Get detailed information about a specific dump
 *
 * Use Cases:
 * - "Show me today's dumps" → list_dumps()
 * - "What dumps occurred for user DEVELOPER?" → list_dumps(user="DEVELOPER")
 * - "Analyze dump XXXX" → get_dump_details(dumpId)
 * - "Why did program Z001 crash?" → list_dumps() + get_dump_details()
 *
 * ADT Endpoints used:
 * - GET /sap/bc/adt/runtime/dumps - List dumps
 * - GET /sap/bc/adt/vit/runtime/dumps/{dumpId} - Get dump details
 */
@Component
@RequiredArgsConstructor
public class DumpTools {

    private final DumpService dumpService;

    /**
     * MCP Tool: List ABAP dumps (short dumps / runtime errors).
     *
     * This tool retrieves a list of ABAP runtime errors that occurred in the
     * SAP system within the specified date range. Similar to ST22 transaction.
     *
     * Token Optimization:
     * - Stage 1 (list_dumps): ~500-1000 tokens → Find relevant dumps
     * - Stage 2 (get_dump_details): ~2000+ tokens → Get full details
     *
     * Use Case:
     * Use this tool to:
     * - Find dumps that occurred today or in a date range
     * - Filter dumps by user to see specific user's errors
     * - Identify dump IDs for detailed analysis
     * - Monitor system health and error patterns
     *
     * Workflow Example:
     * 1. User: "What errors occurred today?"
     * 2. Claude: list_dumps() → Gets today's dumps
     * 3. User: "Tell me more about the first one"
     * 4. Claude: get_dump_details(dumpId) → Gets full details
     *
     * @param dateFrom Start date in YYYY-MM-DD format. Defaults to today.
     * @param dateTo   End date in YYYY-MM-DD format. Defaults to today.
     * @param user     Optional user filter. Leave empty for all users.
     * @return DumpListResult containing list of dump summaries
     */
    @McpTool(
            description = "List ABAP dumps (short dumps / runtime errors) from SAP system. " +
                    "Equivalent to ST22 transaction. " +
                    "Progressive Discovery Stage 1: Find dumps by date/user. " +
                    "Returns summary info (date, time, user, error type, program). " +
                    "Use get_dump_details for full dump content. " +
                    "Token cost: ~500-1000 tokens. " +
                    "Date format: YYYY-MM-DD. Defaults to today if not specified."
    )
    public DumpListResult list_dumps(
            @McpToolParam(
                    description = "Start date in YYYY-MM-DD format. " +
                            "Example: '2025-01-15'. " +
                            "Defaults to today if not specified.",
                    required = false
            )
            String dateFrom,
            @McpToolParam(
                    description = "End date in YYYY-MM-DD format. " +
                            "Example: '2025-01-15'. " +
                            "Defaults to today if not specified.",
                    required = false
            )
            String dateTo,
            @McpToolParam(
                    description = "Filter by SAP user. " +
                            "Example: 'DEVELOPER', 'BATCHUSER'. " +
                            "Leave empty to see all users' dumps.",
                    required = false
            )
            String user
    ) {
        return dumpService.listDumps(dateFrom, dateTo, user);
    }

    /**
     * MCP Tool: Get detailed information about a specific ABAP dump.
     *
     * This tool retrieves the full content of an ABAP runtime error,
     * including error analysis, call stack, source code, and variable values.
     * Equivalent to viewing a dump in ST22 transaction.
     *
     * Token Optimization:
     * - More expensive than list_dumps
     * - Contains complete dump analysis
     * - Typical: ~2000+ tokens (depends on dump complexity)
     *
     * Use Case:
     * After list_dumps identifies relevant dumps, use this to:
     * - Understand what caused the error
     * - See the source code where error occurred
     * - Analyze the call stack
     * - View variable values at time of error
     * - Get SAP's recommendations for fixing the issue
     *
     * Information Returned:
     * - Runtime error name (e.g., GETWA_NOT_ASSIGNED, MESSAGE_TYPE_X)
     * - Program and line where error occurred
     * - Short text explaining the error
     * - What happened (detailed explanation)
     * - How to fix (SAP recommendations)
     * - ABAP call stack
     * - Source code snippet around error line
     * - Variable values at time of error
     *
     * Workflow Example:
     * 1. User: "Analyze dump 20250115103045..."
     * 2. Claude: get_dump_details(dumpId) → Gets full analysis
     * 3. Claude: Explains error cause and suggests fix
     *
     * @param dumpId Dump identifier from list_dumps result
     * @return DumpDetailResult with full dump information
     */
    @McpTool(
            description = "Get detailed information about a specific ABAP dump. " +
                    "Returns full dump content: error analysis, call stack, source code, variables. " +
                    "Equivalent to viewing a dump in ST22 transaction. " +
                    "Progressive Discovery Stage 2: Get full details after list_dumps. " +
                    "Token cost: ~2000+ tokens (depends on dump complexity). " +
                    "Use dump ID from list_dumps result."
    )
    public DumpDetailResult get_dump_details(
            @McpToolParam(
                    description = "Dump identifier from list_dumps result. " +
                            "Format: 71-character string containing date, time, host, user, client, modno. " +
                            "Example: '20250115103045servername                       DEVELOPER   100 0000000001'",
                    required = true
            )
            String dumpId
    ) {
        return dumpService.getDumpDetails(dumpId);
    }
}

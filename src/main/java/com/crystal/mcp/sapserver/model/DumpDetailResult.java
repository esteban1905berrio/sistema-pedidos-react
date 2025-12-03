package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Detailed information about a specific ABAP dump (short dump / runtime error).
 *
 * Contains the full dump content including:
 * - Error identification (runtime error name, exception)
 * - Source code location (program, include, line)
 * - Error analysis (what happened, how to fix)
 * - ABAP call stack
 * - Variable values at time of error
 *
 * This is the equivalent of viewing a dump in ST22 transaction.
 *
 * @param dumpId           Unique dump identifier
 * @param date             Date of dump occurrence (YYYY-MM-DD)
 * @param time             Time of dump occurrence (HH:MM:SS)
 * @param host             Application server host
 * @param user             SAP user who triggered the dump
 * @param client           SAP client (mandant)
 * @param runtimeError     Runtime error name (e.g., "GETWA_NOT_ASSIGNED")
 * @param exceptionClass   Exception class (if class-based exception)
 * @param programName      ABAP program where error occurred
 * @param includeName      Include where error occurred
 * @param lineNumber       Source code line number
 * @param shortText        Short error description (from SNAPT K section)
 * @param whatHappened     Detailed explanation of what happened (from SNAPT W section)
 * @param howToFix         User hints for fixing the error (from SNAPT T section)
 * @param errorAnalysis    Technical description of the error (from SNAPT U section)
 * @param callStack        ABAP call stack at time of error
 * @param sourceCodeLines  Source code snippet around the error line
 * @param variables        Variables and their values at time of error
 * @param message          Status message or error description
 */
public record DumpDetailResult(
        String dumpId,
        String date,
        String time,
        String host,
        String user,
        String client,
        String runtimeError,
        String exceptionClass,
        String programName,
        String includeName,
        int lineNumber,
        String shortText,
        String whatHappened,
        String howToFix,
        String errorAnalysis,
        List<String> callStack,
        List<String> sourceCodeLines,
        List<VariableInfo> variables,
        String message
) {
    /**
     * Variable information at time of error.
     */
    public record VariableInfo(
            String name,
            String type,
            String value
    ) {}

    /**
     * Creates a successful result.
     */
    public static DumpDetailResult success(
            String dumpId, String date, String time, String host, String user, String client,
            String runtimeError, String exceptionClass, String programName, String includeName,
            int lineNumber, String shortText, String whatHappened, String howToFix,
            String errorAnalysis, List<String> callStack, List<String> sourceCodeLines,
            List<VariableInfo> variables) {
        return new DumpDetailResult(
                dumpId, date, time, host, user, client,
                runtimeError, exceptionClass, programName, includeName,
                lineNumber, shortText, whatHappened, howToFix,
                errorAnalysis, callStack, sourceCodeLines, variables,
                "Dump details retrieved successfully"
        );
    }

    /**
     * Creates an error result.
     */
    public static DumpDetailResult error(String errorMessage) {
        return new DumpDetailResult(
                null, null, null, null, null, null,
                null, null, null, null,
                0, null, null, null,
                null, List.of(), List.of(), List.of(),
                errorMessage
        );
    }
}

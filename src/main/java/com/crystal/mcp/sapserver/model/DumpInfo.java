package com.crystal.mcp.sapserver.model;

/**
 * Information about a single ABAP dump (short dump / runtime error).
 *
 * This record contains the key fields extracted from SNAP table and
 * the dump feed from ADT REST API.
 *
 * Fields correspond to:
 * - SNAP table: DATUM, UZEIT, AHOST, UNAME, MANDT, MODNO
 * - RSDUMPINFO structure: DUMPID, PROGRAMNAME, INCLUDENAME, LINENUMBER
 *
 * @param dumpId      Unique dump identifier (serialized key: date+time+host+user+client+modno)
 * @param date        Date of dump occurrence (YYYY-MM-DD)
 * @param time        Time of dump occurrence (HH:MM:SS)
 * @param host        Application server host where dump occurred
 * @param user        SAP user who triggered the dump
 * @param client      SAP client (mandant)
 * @param errorId     Runtime error name (e.g., "GETWA_NOT_ASSIGNED", "MESSAGE_TYPE_X")
 * @param programName ABAP program where error occurred
 * @param includeName Include/class where error occurred (may differ from program)
 * @param lineNumber  Source code line number where error occurred
 * @param title       Short description of the error
 * @param summaryHtml Full HTML summary from Atom feed (fallback for ECC systems)
 */
public record DumpInfo(
        String dumpId,
        String date,
        String time,
        String host,
        String user,
        String client,
        String errorId,
        String programName,
        String includeName,
        int lineNumber,
        String title,
        String summaryHtml
) {
    /**
     * Creates a DumpInfo with minimal required fields.
     * Used when only basic info is available from the feed.
     */
    public static DumpInfo basic(String dumpId, String date, String time,
                                  String user, String errorId, String title) {
        return new DumpInfo(dumpId, date, time, null, user, null,
                           errorId, null, null, 0, title, null);
    }
}

package com.crystal.mcp.sapserver.model;

import java.util.List;

/**
 * Result of listing ABAP dumps (short dumps / runtime errors).
 *
 * Contains a list of dump summaries for the specified criteria.
 * Use get_dump_details to fetch full details of a specific dump.
 *
 * Progressive Discovery Pattern:
 * - Stage 1: list_dumps → Get dump summaries (this result)
 * - Stage 2: get_dump_details → Get full dump content (if needed)
 *
 * @param dumps       List of dump summaries
 * @param totalCount  Total number of dumps matching criteria
 * @param dateFrom    Start date filter used (YYYY-MM-DD)
 * @param dateTo      End date filter used (YYYY-MM-DD)
 * @param user        User filter used (if any)
 * @param message     Status message or error description
 */
public record DumpListResult(
        List<DumpInfo> dumps,
        int totalCount,
        String dateFrom,
        String dateTo,
        String user,
        String message
) {
    /**
     * Creates a successful result with dumps.
     */
    public static DumpListResult success(List<DumpInfo> dumps, String dateFrom,
                                          String dateTo, String user) {
        String msg = String.format("Found %d dump(s)", dumps.size());
        if (user != null && !user.isEmpty()) {
            msg += " for user " + user;
        }
        return new DumpListResult(dumps, dumps.size(), dateFrom, dateTo, user, msg);
    }

    /**
     * Creates an error result.
     */
    public static DumpListResult error(String errorMessage) {
        return new DumpListResult(List.of(), 0, null, null, null, errorMessage);
    }
}

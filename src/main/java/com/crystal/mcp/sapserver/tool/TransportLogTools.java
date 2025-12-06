package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.TransportLogResult;
import com.crystal.mcp.sapserver.service.TransportLogService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for SAP Transport Log Operations.
 *
 * This component provides tools for retrieving transport logs from SAP CTS system.
 * Part of Progressive Discovery architecture for transport management.
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Key Feature:
 * Returns ONLY errors and warnings from transport logs. Informational messages
 * are filtered out to minimize token usage.
 *
 * Use Cases:
 * - Check if a transport had import errors
 * - Investigate failed transports
 * - Monitor transport quality before release
 * - Troubleshoot import issues across systems
 *
 * Progressive Discovery Integration:
 * - Use after list_user_transports identifies transports
 * - Complements get_transport_objects with log information
 * - Answers: "Why did this transport fail?"
 */
@Component
@RequiredArgsConstructor
public class TransportLogTools {

    private final TransportLogService transportLogService;

    /**
     * MCP Tool: Get transport log with errors and warnings.
     *
     * This tool retrieves transport logs from SAP CTS system and filters
     * to return ONLY problems (errors and warnings). Uses the FM
     * ZCX_GET_TRANSPORT_LOGS which calls IF_CTS_REST_API->READ_GLOBAL_INFO.
     *
     * Token Optimization:
     * - Only returns transports with problems (errors/warnings)
     * - Informational messages are filtered out
     * - Transports without issues are summarized in counts
     * - Typical: ~1,000-3,000 tokens (depends on problem count)
     *
     * Input Formats:
     * - Single: "CADK911088"
     * - Multiple (comma-separated): "CADK911088,CADK911122"
     * - Multiple (JSON array): "[\"CADK911088\", \"CADK911122\"]"
     *
     * Severity Mapping:
     * - E (Error): Critical issues that caused import failure
     * - W (Warning): Non-blocking issues to review
     *
     * Step Types:
     * - I: Import
     * - A: Activation
     * - E: Export
     * - G: Generation
     * - R: Release
     * - D: Distribution
     * - V: Versioning
     *
     * Transport Types Supported:
     * - K: Workbench (development objects)
     * - W: Customizing (configuration)
     * - T: Transport of Copies
     * - S: Development/Correction
     * - Others: All standard CTS types
     *
     * Use Case Workflow:
     * 1. User: "Did transport CADK911088 have any errors?"
     * 2. Claude: get_transport_log("CADK911088") → Returns problems only
     * 3. User: "Check multiple transports for my user"
     * 4. Claude: get_transport_log("CADK911088,CADK911122", "DEVELOPER")
     *
     * @param transports Transport number(s) - single, comma-separated, or JSON array
     * @param user Optional filter by transport owner (AS4USER from E070)
     * @return TransportLogResult with problems (errors/warnings) only
     */
    @McpTool(
            description = "Get transport log with errors and warnings only. " +
                    "Returns problems from transport logs, filtering out informational messages. " +
                    "Supports multiple transports (comma-separated or JSON array). " +
                    "Optional filter by transport owner (user). " +
                    "Token cost: ~1,000-3,000 tokens (depends on problem count). " +
                    "Use Case: 'Did this transport have import errors?' " +
                    "Supports all transport types: Workbench (K), Customizing (W), Copies (T)."
    )
    public TransportLogResult get_transport_log(
            @McpToolParam(
                    description = "Transport number(s) to check. " +
                            "Formats: 'CADK911681' (single), " +
                            "'CADK911088,CADK911122' (comma-separated), " +
                            "'[\"CADK911088\", \"CADK911122\"]' (JSON array). " +
                            "Examples: 'CADK911088', 'DEVK900123,DEVK900124'",
                    required = true
            )
            String transports,
            @McpToolParam(
                    description = "Optional filter by transport owner (AS4USER from E070). " +
                            "If provided, only returns logs for transports owned by this user. " +
                            "Leave empty to get logs for all requested transports. " +
                            "Examples: 'DEVELOPER', 'BASIS_USER'",
                    required = false
            )
            String user
    ) {
        return transportLogService.getTransportLog(transports, user);
    }
}

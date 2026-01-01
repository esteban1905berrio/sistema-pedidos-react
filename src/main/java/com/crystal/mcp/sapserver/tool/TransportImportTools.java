package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.TransportImportResult;
import com.crystal.mcp.sapserver.service.TransportImportService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransportImportTools {

    private final TransportImportService transportImportService;

    @McpTool(description = "Import released transport requests to target system (STMS-like). " +
            "Wrapper for TMS_MGR_IMPORT_TR_REQUEST via ZCX_TMS_IMPORT_REQUEST. " +
            "Imports one or multiple OTs to a target system/client. " +
            "Prerequisites: OTs must be released and in target buffer.")
    public TransportImportResult import_transport_requests(
            @McpToolParam(description = "Target system ID (e.g., 'S4Q', 'P01').") String targetSystem,

            @McpToolParam(description = "Target client (e.g., '100', '200').") String targetClient,

            @McpToolParam(description = "Comma-separated list of transport numbers (e.g., 'DK900123,DK900124').") String transports,

            @McpToolParam(description = "Ignore lock (U1) mode? Default: false.", required = false) Boolean ignoreLock,

            @McpToolParam(description = "Import again (reimport)? Default: true.", required = false) Boolean importAgain) {
        return transportImportService.importTransports(
                targetSystem,
                targetClient,
                transports,
                ignoreLock != null ? ignoreLock : false,
                importAgain != null ? importAgain : true);
    }
}

package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.config.JCoConfiguration;
import com.crystal.mcp.sapserver.model.TransportImportResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportImportService {

    private final JCoConfiguration jCoConfiguration;

    public TransportImportResult importTransports(
            String targetSystem,
            String targetClient,
            String transports,
            boolean ignoreLock,
            boolean importAgain) {
        log.info("Importing transports: {} to {}/{} (ignoreLock={}, importAgain={})",
                transports, targetSystem, targetClient, ignoreLock, importAgain);

        try {
            JCoDestination destination = jCoConfiguration.jcoDestination();
            JCoFunction function = destination.getRepository().getFunction("ZCX_TMS_IMPORT_REQUEST");

            if (function == null) {
                return TransportImportResult.failure("Function module ZCX_TMS_IMPORT_REQUEST not found");
            }

            JCoParameterList importParams = function.getImportParameterList();
            importParams.setValue("IV_TARGET_SYSTEM", targetSystem);
            importParams.setValue("IV_TARGET_CLIENT", targetClient);
            importParams.setValue("IV_TRANSPORTS", transports);
            importParams.setValue("IV_IGNORE_LOCK", ignoreLock ? "X" : "");
            importParams.setValue("IV_IMPORT_AGAIN", importAgain ? "X" : "");

            function.execute(destination);

            JCoParameterList exportParams = function.getExportParameterList();
            String jsonString = exportParams.getString("EV_RESULTS_JSON");

            return parseResults(jsonString);

        } catch (Exception e) {
            log.error("Error importing transports: {}", e.getMessage(), e);
            return TransportImportResult.failure(e.getMessage());
        }
    }

    private TransportImportResult parseResults(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            List<TransportImportResult.TransportResult> results = new ArrayList<>();
            JsonNode resultsNode = root.get("results");
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode node : resultsNode) {
                    results.add(new TransportImportResult.TransportResult(
                            node.get("transport").asText(),
                            node.get("status").asText(),
                            node.get("message").asText(),
                            node.get("success").asBoolean()));
                }
            }

            return new TransportImportResult(
                    root.get("targetSystem").asText(),
                    root.get("targetClient").asText(),
                    root.get("totalRequests").asInt(),
                    root.get("successCount").asInt(),
                    root.get("errorCount").asInt(),
                    results);
        } catch (Exception e) {
            log.error("Error parsing import results: {}", e.getMessage(), e);
            return TransportImportResult.failure("JSON Parse Error: " + e.getMessage());
        }
    }
}

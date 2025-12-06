package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.config.JCoConfiguration;
import com.crystal.mcp.sapserver.model.TransportSearchResult;
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
import java.util.Collections;
import java.util.List;

/**
 * Service for searching transport requests with flexible criteria.
 *
 * This service provides advanced search capabilities for SAP transport requests,
 * allowing searches by description pattern, user, type, status, target system,
 * and date range.
 *
 * Implementation:
 * Uses Z_CX_SEARCH_TRANSPORTS function module which queries E070/E07T/E071 tables
 * with efficient JOINs and returns JSON with transport details including object/task counts.
 *
 * Progressive Discovery Integration:
 * - Alternative to list_user_transports when you need flexible search criteria
 * - Returns lightweight results with counts (no full object list)
 * - Use get_transport_objects to fetch detailed objects for a specific transport
 *
 * Token Cost: ~1,000-3,000 tokens (depends on number of results)
 *
 * Thread Safety: Stateless service, thread-safe via JCoConfiguration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportSearchService {

    private final JCoConfiguration jCoConfiguration;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Search transport requests with flexible criteria.
     *
     * At least one search criterion is required to prevent full table scans.
     * Results are ordered by creation date descending (newest first).
     *
     * Search Criteria:
     * - description: LIKE pattern search (e.g., "%PSR01%", "FI-%")
     * - user: Owner filter (exact or pattern with %)
     * - transportType: K=Workbench, W=Customizing, T=Copies
     * - status: D=Modifiable, R=Released, L=Protected
     * - targetSystem: Target system name (e.g., "S4Q", "S4P")
     * - dateFrom/dateTo: Creation date range (YYYY-MM-DD)
     *
     * Workflow Examples:
     * 1. "Find OTs containing PSR01"
     *    → searchTransports("%PSR01%", null, null, null, null, null, null, 100)
     * 2. "Show open OTs from L_ABAPS_ITA"
     *    → searchTransports(null, "L_ABAPS_ITA", null, "D", null, null, null, 100)
     * 3. "FI Workbench OTs released in December"
     *    → searchTransports("%FI%", null, "K", "R", null, "2025-12-01", "2025-12-31", 100)
     *
     * @param description     Description pattern (LIKE search with % wildcards)
     * @param user            User/owner filter (exact or pattern)
     * @param transportType   Transport type filter (K, W, T)
     * @param status          Status filter (D, R, L)
     * @param targetSystem    Target system filter
     * @param dateFrom        Start date filter (YYYY-MM-DD)
     * @param dateTo          End date filter (YYYY-MM-DD)
     * @param maxResults      Maximum results (1-1000, default 100)
     * @return TransportSearchResult with matching transports
     */
    public TransportSearchResult searchTransports(
            String description,
            String user,
            String transportType,
            String status,
            String targetSystem,
            String dateFrom,
            String dateTo,
            Integer maxResults
    ) {
        log.info("Searching transports - description: {}, user: {}, type: {}, status: {}, " +
                "target: {}, dateFrom: {}, dateTo: {}, maxResults: {}",
                description, user, transportType, status, targetSystem, dateFrom, dateTo, maxResults);

        try {
            // Get destination
            JCoDestination destination = jCoConfiguration.jcoDestination();

            // Get function module
            JCoFunction function = destination.getRepository()
                    .getFunction("Z_CX_SEARCH_TRANSPORTS");

            if (function == null) {
                String errorMsg = "Function module Z_CX_SEARCH_TRANSPORTS not found. " +
                        "Please verify FM exists in SAP system.";
                log.error(errorMsg);
                return new TransportSearchResult(false, errorMsg, 0, Collections.emptyList());
            }

            // Set import parameters
            JCoParameterList importParams = function.getImportParameterList();

            if (description != null && !description.trim().isEmpty()) {
                importParams.setValue("IV_DESCRIPTION", description.trim());
            }
            if (user != null && !user.trim().isEmpty()) {
                importParams.setValue("IV_USER", user.trim().toUpperCase());
            }
            if (transportType != null && !transportType.trim().isEmpty()) {
                importParams.setValue("IV_TRANSPORT_TYPE", transportType.trim().toUpperCase());
            }
            if (status != null && !status.trim().isEmpty()) {
                importParams.setValue("IV_STATUS", status.trim().toUpperCase());
            }
            if (targetSystem != null && !targetSystem.trim().isEmpty()) {
                importParams.setValue("IV_TARGET_SYSTEM", targetSystem.trim().toUpperCase());
            }
            if (dateFrom != null && !dateFrom.trim().isEmpty()) {
                // Convert YYYY-MM-DD to YYYYMMDD
                String dateFromSap = dateFrom.replace("-", "");
                importParams.setValue("IV_DATE_FROM", dateFromSap);
            }
            if (dateTo != null && !dateTo.trim().isEmpty()) {
                // Convert YYYY-MM-DD to YYYYMMDD
                String dateToSap = dateTo.replace("-", "");
                importParams.setValue("IV_DATE_TO", dateToSap);
            }
            if (maxResults != null && maxResults > 0) {
                importParams.setValue("IV_MAX_RESULTS", maxResults);
            }

            // Execute function
            function.execute(destination);

            // Get export parameters
            JCoParameterList exportParams = function.getExportParameterList();
            String successFlag = exportParams.getString("EV_SUCCESS");
            boolean success = "X".equals(successFlag);
            String message = exportParams.getString("EV_MESSAGE");
            int totalFound = exportParams.getInt("EV_TOTAL_FOUND");
            String jsonString = exportParams.getString("EV_RESULTS_JSON");

            if (!success) {
                log.warn("FM returned failure: {}", message);
                return new TransportSearchResult(false, message, 0, Collections.emptyList());
            }

            // Parse JSON response
            log.info("Search completed - totalFound: {}, JSON length: {} bytes",
                    totalFound, jsonString != null ? jsonString.length() : 0);

            return parseSearchResultsJson(jsonString);

        } catch (JCoException e) {
            log.error("JCo error calling Z_CX_SEARCH_TRANSPORTS: {}", e.getMessage(), e);
            return new TransportSearchResult(false, "JCo error: " + e.getMessage(), 0, Collections.emptyList());
        } catch (Exception e) {
            log.error("Error searching transports: {}", e.getMessage(), e);
            return new TransportSearchResult(false, e.getMessage(), 0, Collections.emptyList());
        }
    }

    /**
     * Parse JSON response from Z_CX_SEARCH_TRANSPORTS function module.
     *
     * Expected JSON structure:
     * {
     *   "success": true,
     *   "totalFound": 3,
     *   "transports": [
     *     {
     *       "transport_number": "CADK911197",
     *       "description": "PS WB R001 R002 R006 Carga def proy...",
     *       "transport_type": "K",
     *       "transport_type_desc": "Workbench",
     *       "status": "D",
     *       "status_desc": "Modifiable",
     *       "owner": "L_ABAPS_ITA",
     *       "created_date": "2025-01-15",
     *       "created_time": "10:30:45",
     *       "target_system": "S4Q",
     *       "category": "SYST",
     *       "parent_transport": null,
     *       "object_count": 25,
     *       "task_count": 2
     *     }
     *   ]
     * }
     *
     * @param jsonString JSON string from FM
     * @return TransportSearchResult parsed from JSON
     */
    private TransportSearchResult parseSearchResultsJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new TransportSearchResult(true, "No results", 0, Collections.emptyList());
        }

        try {
            JsonNode root = objectMapper.readTree(jsonString);

            boolean success = root.path("success").asBoolean(true);
            int totalFound = root.path("totalFound").asInt(0);

            List<TransportSearchResult.TransportDetail> transports = new ArrayList<>();
            JsonNode transportsNode = root.path("transports");

            if (transportsNode.isArray()) {
                for (JsonNode transportNode : transportsNode) {
                    String transportNumber = transportNode.path("transport_number").asText("");
                    String description = transportNode.path("description").asText("");
                    String transportType = transportNode.path("transport_type").asText("");
                    String transportTypeDesc = transportNode.path("transport_type_desc").asText("");
                    String status = transportNode.path("status").asText("");
                    String statusDesc = transportNode.path("status_desc").asText("");
                    String owner = transportNode.path("owner").asText("");
                    String createdDate = transportNode.path("created_date").asText("");
                    String createdTime = transportNode.path("created_time").asText("");
                    String targetSystem = transportNode.path("target_system").asText("");
                    String category = transportNode.path("category").asText("");

                    // Handle null parent_transport
                    JsonNode parentNode = transportNode.path("parent_transport");
                    String parentTransport = parentNode.isNull() ? null : parentNode.asText(null);

                    int objectCount = transportNode.path("object_count").asInt(0);
                    int taskCount = transportNode.path("task_count").asInt(0);

                    transports.add(new TransportSearchResult.TransportDetail(
                            transportNumber,
                            description,
                            transportType,
                            transportTypeDesc,
                            status,
                            statusDesc,
                            owner,
                            createdDate,
                            createdTime,
                            targetSystem,
                            category,
                            parentTransport,
                            objectCount,
                            taskCount
                    ));
                }
            }

            return new TransportSearchResult(success, null, totalFound, transports);

        } catch (Exception e) {
            log.error("Error parsing JSON from FM: {}", e.getMessage(), e);
            return new TransportSearchResult(false,
                    "Failed to parse JSON response: " + e.getMessage(),
                    0, Collections.emptyList());
        }
    }
}

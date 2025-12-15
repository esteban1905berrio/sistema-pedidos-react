package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.DmeeSearchResult;
import com.crystal.mcp.sapserver.model.DmeeSearchResult.DmeeReference;
import com.crystal.mcp.sapserver.model.DmeeTreeResult;
import com.crystal.mcp.sapserver.model.DmeeTreeResult.DmeeTreeHeader;
import com.crystal.mcp.sapserver.model.DmeeTreeResult.DmeeTreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for DMEE (Data Medium Exchange Engine) Tree operations.
 *
 * This service provides business logic for retrieving DMEE payment format tree
 * configurations from SAP systems via custom RFC function module.
 *
 * DMEE Overview:
 * - DMEE is SAP's framework for payment file format definitions
 * - Trees define the structure and mapping for payment output files
 * - Common use: bank transfers, direct debits, payment advice notes
 *
 * RFC Function Module:
 * ZCX_UTIL_GET_DMEE_TREE - Custom FM that retrieves tree configuration as JSON
 *
 * Data Sources (SAP Tables):
 * - DMEE_TREE: Tree master data (type, ID, release flag, author)
 * - DMEE_TREE_T: Tree texts/descriptions (language-dependent)
 * - DMEE_TREE_HEAD: Version header (charset, param structure)
 * - DMEE_TREE_NODE: Tree nodes with mapping properties
 * - DMEE_TREE_NODE_T: Node texts and comments (language-dependent)
 *
 * Use Cases:
 * - Extract DMEE trees for migration between S/4HANA and ECC
 * - Analyze tree structure and mapping logic
 * - Document payment format configurations
 * - Copy/adapt trees for different bank formats
 *
 * Progressive Discovery Integration:
 * - Stage 1: search_objects (SearchService) → Find DMEE objects
 * - Stage 2: get_dmee_tree (DmeeService) → Get complete tree configuration
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DmeeService {

    private final RfcAdapter rfcAdapter;

    private static final String FM_GET_DMEE_TREE = "ZCX_UTIL_GET_DMEE_TREE";

    /**
     * Get DMEE Tree configuration.
     *
     * Retrieves complete DMEE tree information including:
     * - Header: tree type, ID, version, description, configuration
     * - Nodes: hierarchical structure with mapping properties
     *
     * @param treeType DMEE tree type (e.g., "PAYM" for payments)
     * @param treeId DMEE tree ID (e.g., "ZFIE1017_CITIBANAMEX")
     * @param version optional version number (null = latest version)
     * @param language language for texts (default: system language)
     * @return DmeeTreeResult containing header and nodes
     * @throws RuntimeException if RFC call fails or tree not found
     */
    public DmeeTreeResult getDmeeTree(String treeType, String treeId, String version, String language) {
        String normalizedType = treeType.toUpperCase().trim();
        String normalizedId = treeId.toUpperCase().trim();

        log.info("Fetching DMEE Tree: type={}, id={}, version={}", normalizedType, normalizedId, version);

        try {
            Map<String, String> importParams = new HashMap<>();
            importParams.put("IV_TREE_TYPE", normalizedType);
            importParams.put("IV_TREE_ID", normalizedId);
            if (version != null && !version.isBlank()) {
                importParams.put("IV_VERSION", version);
            }
            if (language != null && !language.isBlank()) {
                importParams.put("IV_LANGU", language.toUpperCase());
            }

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    FM_GET_DMEE_TREE,
                    importParams
            );

            String rcode = response.getExportParam("EV_RCODE");
            if (!"00".equals(rcode)) {
                String errorMsg = response.getExportParam("EV_MESSAGE");
                if (errorMsg == null || errorMsg.isBlank()) {
                    errorMsg = "DMEE tree not found or access denied";
                }
                log.warn("DMEE tree retrieval failed: {} (RCODE: {})", errorMsg, rcode);
                throw new RuntimeException(String.format(
                        "DMEE tree '%s/%s' not found or access denied: %s",
                        normalizedType, normalizedId, errorMsg
                ));
            }

            String headerJson = response.getExportParam("EV_HEADER_JSON");
            String nodesJson = response.getExportParam("EV_NODES_JSON");

            DmeeTreeHeader header = parseHeader(headerJson);
            List<DmeeTreeNode> nodes = parseNodes(nodesJson);

            log.info("Successfully retrieved DMEE Tree {}/{} ({} nodes)",
                    normalizedType, normalizedId, nodes.size());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("functionModule", FM_GET_DMEE_TREE);
            metadata.put("objectType", "DMEE");

            return new DmeeTreeResult(
                    normalizedType,
                    normalizedId,
                    header,
                    nodes,
                    metadata
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error fetching DMEE Tree %s/%s: %s",
                    normalizedType, normalizedId, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Search DMEE Trees by wildcard pattern.
     *
     * Searches DMEE_TREE and DMEE_TREE_T tables for trees matching the pattern.
     * Supports wildcards: "Z*" (prefix), "*SEPA*" (contains), "*_CT" (suffix).
     *
     * @param treeType DMEE tree type (e.g., "PAYM" for payments)
     * @param pattern wildcard pattern for tree ID (e.g., "Z*", "*SEPA*")
     * @param maxResults maximum results to return (default: 100)
     * @return DmeeSearchResult containing matching DMEE trees
     * @throws RuntimeException if RFC call fails
     */
    public DmeeSearchResult searchDmeeTrees(String treeType, String pattern, Integer maxResults) {
        String normalizedType = treeType.toUpperCase().trim();
        String normalizedPattern = pattern.toUpperCase().trim();
        int limit = (maxResults != null && maxResults > 0) ? maxResults : 100;

        log.info("Searching DMEE Trees: type={}, pattern={}, maxResults={}",
                normalizedType, normalizedPattern, limit);

        try {
            Map<String, String> importParams = new HashMap<>();
            importParams.put("IV_TREE_TYPE", normalizedType);
            importParams.put("IV_TREE_ID", normalizedPattern);
            importParams.put("IV_MAX_RESULTS", String.valueOf(limit));

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    FM_GET_DMEE_TREE,
                    importParams
            );

            String resultsJson = response.getExportParam("EV_RESULTS_JSON");

            List<DmeeReference> results = parseSearchResults(resultsJson);

            log.info("DMEE search completed: {} results found", results.size());

            return new DmeeSearchResult(
                    normalizedType,
                    normalizedPattern,
                    limit,
                    results.size(),
                    results
            );

        } catch (Exception e) {
            String errorMsg = String.format("Error searching DMEE Trees with pattern %s/%s: %s",
                    normalizedType, normalizedPattern, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Parse search results JSON array from ABAP FM response.
     */
    private List<DmeeReference> parseSearchResults(String resultsJson) {
        if (resultsJson == null || resultsJson.isBlank() || resultsJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> resultsList = mapper.readValue(resultsJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<DmeeReference> results = new ArrayList<>();
            for (Map<String, Object> item : resultsList) {
                results.add(new DmeeReference(
                        getString(item, "tree_type"),
                        getString(item, "tree_id"),
                        getString(item, "description")
                ));
            }
            return results;
        } catch (Exception e) {
            log.warn("Failed to parse search results JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse header JSON from ABAP FM response.
     */
    private DmeeTreeHeader parseHeader(String headerJson) {
        if (headerJson == null || headerJson.isBlank() || headerJson.equals("{}")) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            Map<String, Object> headerMap = mapper.readValue(headerJson,
                    new TypeReference<Map<String, Object>>() {});

            return new DmeeTreeHeader(
                    getString(headerMap, "tree_type"),
                    getString(headerMap, "tree_id"),
                    getString(headerMap, "description"),
                    getString(headerMap, "version"),
                    getString(headerMap, "crea_user"),
                    getString(headerMap, "crea_date"),
                    getString(headerMap, "chng_user"),
                    getString(headerMap, "chng_date"),
                    getString(headerMap, "release_flag"),
                    getString(headerMap, "dmeex"),
                    getString(headerMap, "param_struc"),
                    getString(headerMap, "charset"),
                    getString(headerMap, "vers_user"),
                    getString(headerMap, "vers_date")
            );
        } catch (Exception e) {
            log.warn("Failed to parse header JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse nodes JSON array from ABAP FM response.
     */
    private List<DmeeTreeNode> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.isBlank() || nodesJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> nodeList = mapper.readValue(nodesJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<DmeeTreeNode> nodes = new ArrayList<>();
            for (Map<String, Object> node : nodeList) {
                nodes.add(new DmeeTreeNode(
                        getString(node, "node_id"),
                        getString(node, "tech_name"),
                        getString(node, "parent_id"),
                        getString(node, "node_type"),
                        getInt(node, "lev"),
                        getString(node, "text"),
                        getString(node, "node_comment"),
                        getInt(node, "length"),
                        getString(node, "data_type"),
                        getString(node, "mp_const"),
                        getString(node, "mp_sc_tab"),
                        getString(node, "mp_sc_fld"),
                        getString(node, "mp_exit_func"),
                        getString(node, "cv_rule")
                ));
            }
            return nodes;
        } catch (Exception e) {
            log.warn("Failed to parse nodes JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

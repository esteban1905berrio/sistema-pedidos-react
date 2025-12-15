package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.BadiImplementationResult;
import com.crystal.mcp.sapserver.model.BadiImplementationResult.BadiImplementationHeader;
import com.crystal.mcp.sapserver.model.BadiImplementationResult.BadiDefinitionInfo;
import com.crystal.mcp.sapserver.model.BadiImplementationResult.BadiImplementingClass;
import com.crystal.mcp.sapserver.model.BadiSearchResult;
import com.crystal.mcp.sapserver.model.BadiSearchResult.BadiReference;
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
 * Service for BAdI Implementation (SXCI) operations.
 *
 * This service provides business logic for retrieving classic BAdI implementation
 * metadata and related information from SAP systems via custom RFC function module.
 *
 * Classic BAdI vs Enhancement Framework:
 * - Classic BAdI (SXCI): Transaction SE18/SE19, uses SXC_* tables
 * - Enhancement BAdI (ENHO): New framework, uses BADI_* tables
 * This service handles CLASSIC BAdIs only.
 *
 * RFC Function Module:
 * ZCX_UTIL_GET_BADI_IMPL - Custom FM that retrieves BAdI implementation data as JSON
 *
 * Data Sources (SAP Tables):
 * - SXC_ATTR: Implementation attributes (name, active, author, dates)
 * - SXC_EXIT: Implementation ↔ BAdI definition relationship + filter values
 * - SXC_CLASS: Implementing class for each interface
 * - SXS_INTER: BAdI definition interfaces
 * - SXS_ATTRT: BAdI definition texts/descriptions
 * - SXS_ATTR: BAdI definition attributes (multiple_use, filter_dependent)
 *
 * Progressive Discovery Integration:
 * - Stage 1: search_objects (SearchService) → Find SXCI objects
 * - Stage 2: get_badi_implementation (BadiService) → Get implementation details
 * - Stage 3: get_class_source (ClassService) → Get implementing class source
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadiService {

    private final RfcAdapter rfcAdapter;

    private static final String FM_GET_BADI_IMPL = "ZCX_UTIL_GET_BADI_IMPL";

    /**
     * Get BAdI Implementation details.
     *
     * Retrieves complete BAdI implementation information including:
     * - Header: name, description, active status, package, author, dates
     * - BAdI Definitions: which BAdIs this implementation covers with filter values
     * - Implementing Classes: classes that implement the BAdI interfaces
     *
     * @param implementationName name of BAdI implementation (e.g., "ZTEST_BADI_IMPL")
     * @return BadiImplementationResult containing all implementation data
     * @throws RuntimeException if RFC call fails or implementation not found
     */
    public BadiImplementationResult getBadiImplementation(String implementationName) {
        String normalizedName = implementationName.toUpperCase().trim();

        log.info("Fetching BAdI Implementation: {}", normalizedName);

        try {
            Map<String, String> importParams = new HashMap<>();
            importParams.put("IV_IMP_NAME", normalizedName);

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    FM_GET_BADI_IMPL,
                    importParams
            );

            String rcode = response.getExportParam("EV_RCODE");
            if (!"0".equals(rcode) && !rcode.isBlank()) {
                String errorMsg = response.getExportParam("EV_MESSAGE");
                if (errorMsg == null || errorMsg.isBlank()) {
                    errorMsg = "BAdI implementation not found or access denied";
                }
                log.warn("BAdI implementation retrieval failed: {} (RCODE: {})", errorMsg, rcode);
                throw new RuntimeException(String.format(
                        "BAdI implementation '%s' not found or access denied: %s",
                        normalizedName, errorMsg
                ));
            }

            String headerJson = response.getExportParam("EV_HEADER_JSON");
            String definitionsJson = response.getExportParam("EV_DEFINITIONS_JSON");
            String classesJson = response.getExportParam("EV_CLASSES_JSON");

            BadiImplementationHeader header = parseHeader(headerJson);
            List<BadiDefinitionInfo> definitions = parseDefinitions(definitionsJson);
            List<BadiImplementingClass> classes = parseClasses(classesJson);

            log.info("Successfully retrieved BAdI Implementation {} ({} definitions, {} classes)",
                    normalizedName, definitions.size(), classes.size());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("functionModule", FM_GET_BADI_IMPL);
            metadata.put("objectType", "SXCI");

            return new BadiImplementationResult(
                    normalizedName,
                    header,
                    definitions,
                    classes,
                    metadata
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error fetching BAdI Implementation %s: %s",
                    normalizedName, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Search BAdI Implementations by wildcard pattern.
     *
     * Searches SXC_ATTR table for BAdI implementations matching the pattern.
     * Supports wildcards: "Z*" (prefix), "*BADI*" (contains), "*_IMPL" (suffix).
     *
     * @param pattern wildcard pattern (e.g., "Z*", "*BADI*")
     * @param maxResults maximum results to return (default: 100)
     * @return BadiSearchResult containing matching BAdI implementations
     * @throws RuntimeException if RFC call fails
     */
    public BadiSearchResult searchBadiImplementations(String pattern, Integer maxResults) {
        String normalizedPattern = pattern.toUpperCase().trim();
        int limit = (maxResults != null && maxResults > 0) ? maxResults : 100;

        log.info("Searching BAdI Implementations: pattern={}, maxResults={}", normalizedPattern, limit);

        try {
            Map<String, String> importParams = new HashMap<>();
            importParams.put("IV_IMP_NAME", normalizedPattern);
            importParams.put("IV_MAX_RESULTS", String.valueOf(limit));

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    FM_GET_BADI_IMPL,
                    importParams
            );

            String resultsJson = response.getExportParam("EV_RESULTS_JSON");

            List<BadiReference> results = parseSearchResults(resultsJson);

            log.info("BAdI search completed: {} results found", results.size());

            return new BadiSearchResult(
                    normalizedPattern,
                    limit,
                    results.size(),
                    results
            );

        } catch (Exception e) {
            String errorMsg = String.format("Error searching BAdI Implementations with pattern %s: %s",
                    normalizedPattern, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Parse search results JSON array from ABAP FM response.
     */
    private List<BadiReference> parseSearchResults(String resultsJson) {
        if (resultsJson == null || resultsJson.isBlank() || resultsJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> resultsList = mapper.readValue(resultsJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<BadiReference> results = new ArrayList<>();
            for (Map<String, Object> item : resultsList) {
                results.add(new BadiReference(
                        getString(item, "imp_name"),
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
    private BadiImplementationHeader parseHeader(String headerJson) {
        if (headerJson == null || headerJson.isBlank() || headerJson.equals("{}")) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            Map<String, Object> headerMap = mapper.readValue(headerJson,
                    new TypeReference<Map<String, Object>>() {});

            return new BadiImplementationHeader(
                    getString(headerMap, "imp_name"),
                    getString(headerMap, "description"),
                    getBoolean(headerMap, "active"),
                    getString(headerMap, "devclass"),
                    getString(headerMap, "author"),
                    getString(headerMap, "created_on"),
                    getString(headerMap, "changed_by"),
                    getString(headerMap, "changed_on"),
                    getString(headerMap, "mig_enhname")
            );
        } catch (Exception e) {
            log.warn("Failed to parse header JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse BAdI definitions JSON array from ABAP FM response.
     */
    private List<BadiDefinitionInfo> parseDefinitions(String definitionsJson) {
        if (definitionsJson == null || definitionsJson.isBlank() || definitionsJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> defList = mapper.readValue(definitionsJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<BadiDefinitionInfo> definitions = new ArrayList<>();
            for (Map<String, Object> def : defList) {
                @SuppressWarnings("unchecked")
                List<String> interfaces = def.get("interfaces") != null
                        ? (List<String>) def.get("interfaces")
                        : new ArrayList<>();

                definitions.add(new BadiDefinitionInfo(
                        getString(def, "badi_name"),
                        getString(def, "description"),
                        getString(def, "filter_value"),
                        interfaces,
                        getBoolean(def, "multiple_use"),
                        getBoolean(def, "filter_dependent")
                ));
            }
            return definitions;
        } catch (Exception e) {
            log.warn("Failed to parse definitions JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse implementing classes JSON array from ABAP FM response.
     */
    private List<BadiImplementingClass> parseClasses(String classesJson) {
        if (classesJson == null || classesJson.isBlank() || classesJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> classList = mapper.readValue(classesJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<BadiImplementingClass> classes = new ArrayList<>();
            for (Map<String, Object> cls : classList) {
                classes.add(new BadiImplementingClass(
                        getString(cls, "interface_name"),
                        getString(cls, "class_name")
                ));
            }
            return classes;
        } catch (Exception e) {
            log.warn("Failed to parse classes JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String strVal = (String) value;
            return "X".equalsIgnoreCase(strVal) || "true".equalsIgnoreCase(strVal);
        }
        return false;
    }
}

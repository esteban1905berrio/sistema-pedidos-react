package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.EnhancementSearchResult;
import com.crystal.mcp.sapserver.model.EnhancementSearchResult.EnhancementReference;
import com.crystal.mcp.sapserver.model.EnhancementSourceResult;
import com.crystal.mcp.sapserver.model.EnhancementSourceResult.EnhancementHeader;
import com.crystal.mcp.sapserver.model.EnhancementSourceResult.EnhancementElement;
import com.crystal.mcp.sapserver.model.EnhancementSourceResult.EnhancementSourceLine;
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
 * Service for Enhancement Implementation operations.
 *
 * This service provides business logic for retrieving Enhancement Implementation
 * (ENHO) source code and metadata from SAP systems via custom RFC function module.
 *
 * Enhancement Implementations contain:
 * - Hook Implementations (source code injected into enhancement spots)
 * - BAdI Implementations (implementing classes for Business Add-Ins)
 *
 * RFC Function Module:
 * ZCX_GET_ENHANCEMENT_SOURCE - Custom FM that retrieves enhancement data as JSON
 *
 * Progressive Discovery Integration:
 * - Stage 1: search_objects (SearchService) → Find ENHO objects
 * - Stage 2: get_object_structure (ObjectService) → Get enhancement metadata
 * - Stage 3: get_enhancement_source (EnhancementService) → Get source code
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancementService {

    private final RfcAdapter rfcAdapter;

    private static final String FM_GET_ENHANCEMENT_SOURCE = "ZCX_GET_ENHANCEMENT_SOURCE";

    /**
     * Get Enhancement Implementation source code and metadata.
     *
     * Retrieves complete enhancement information including:
     * - Header: name, description, tool type (HOOK_IMPL or BADI_IMPL)
     * - Elements: list of hooks or BAdI implementations
     * - Sources: actual ABAP source code for each element
     *
     * @param enhancementName name of enhancement implementation (e.g., "ZENH_INVOICE_BADI")
     * @param version version number (default 00000 for active)
     * @return EnhancementSourceResult containing all enhancement data
     * @throws RuntimeException if RFC call fails or enhancement not found
     */
    public EnhancementSourceResult getEnhancementSource(String enhancementName, String version) {
        String normalizedName = enhancementName.toUpperCase().trim();
        String normalizedVersion = (version == null || version.isBlank()) ? "00000" : version;

        log.info("Fetching Enhancement Implementation: {} (version: {})", normalizedName, normalizedVersion);

        try {
            Map<String, String> importParams = new HashMap<>();
            importParams.put("IV_ENHANCEMENT_NAME", normalizedName);
            importParams.put("IV_VERSION", normalizedVersion);

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    FM_GET_ENHANCEMENT_SOURCE,
                    importParams
            );

            String rcode = response.getExportParam("EV_RCODE");
            if (!"0".equals(rcode) && !rcode.isBlank()) {
                String errorMsg = response.getExportParam("EV_HEADER_JSON");
                if (errorMsg.isBlank()) {
                    errorMsg = "Enhancement not found or access denied";
                }
                log.warn("Enhancement retrieval failed: {} (RCODE: {})", errorMsg, rcode);
                throw new RuntimeException(String.format(
                        "Enhancement '%s' not found or access denied: %s",
                        normalizedName, errorMsg
                ));
            }

            String headerJson = response.getExportParam("EV_HEADER_JSON");
            String elementsJson = response.getExportParam("EV_ELEMENTS_JSON");
            String sourcesJson = response.getExportParam("EV_SOURCE_JSON");

            EnhancementHeader header = parseHeader(headerJson);
            List<EnhancementElement> elements = parseElements(elementsJson);
            List<EnhancementSourceLine> sourceLines = parseSourceLines(sourcesJson);

            log.info("Successfully retrieved Enhancement {} ({} elements, {} source lines)",
                    normalizedName, elements.size(), sourceLines.size());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("functionModule", FM_GET_ENHANCEMENT_SOURCE);
            metadata.put("version", normalizedVersion);

            return new EnhancementSourceResult(
                    normalizedName,
                    header,
                    elements,
                    sourceLines,
                    metadata
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error fetching Enhancement %s: %s",
                    normalizedName, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Search Enhancement Implementations by wildcard pattern.
     *
     * Searches ENHO table for enhancements matching the pattern.
     * Supports wildcards: "Z*" (prefix), "*INVOICE*" (contains), "*_BADI" (suffix).
     *
     * @param pattern wildcard pattern (e.g., "Z*", "*INVOICE*")
     * @param maxResults maximum results to return (default: 100)
     * @return EnhancementSearchResult containing matching enhancements
     * @throws RuntimeException if RFC call fails
     */
    public EnhancementSearchResult searchEnhancements(String pattern, Integer maxResults) {
        String normalizedPattern = pattern.toUpperCase().trim();
        int limit = (maxResults != null && maxResults > 0) ? maxResults : 100;

        log.info("Searching Enhancement Implementations: pattern={}, maxResults={}", normalizedPattern, limit);

        try {
            Map<String, String> importParams = new HashMap<>();
            importParams.put("IV_ENHANCEMENT_NAME", normalizedPattern);
            importParams.put("IV_MAX_RESULTS", String.valueOf(limit));

            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    FM_GET_ENHANCEMENT_SOURCE,
                    importParams
            );

            String resultsJson = response.getExportParam("EV_RESULTS_JSON");

            List<EnhancementReference> results = parseSearchResults(resultsJson);

            log.info("Enhancement search completed: {} results found", results.size());

            return new EnhancementSearchResult(
                    normalizedPattern,
                    limit,
                    results.size(),
                    results
            );

        } catch (Exception e) {
            String errorMsg = String.format("Error searching Enhancements with pattern %s: %s",
                    normalizedPattern, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Parse search results JSON array from ABAP FM response.
     *
     * Expected JSON format from FM:
     * [
     *   {
     *     "enhancement_name": "ZENH_INVOICE",
     *     "object_type": "CLAS",
     *     "object_name": "ZCL_INVOICE_PROCESSOR"
     *   },
     *   ...
     * ]
     */
    private List<EnhancementReference> parseSearchResults(String resultsJson) {
        if (resultsJson == null || resultsJson.isBlank() || resultsJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> resultsList = mapper.readValue(resultsJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<EnhancementReference> results = new ArrayList<>();
            for (Map<String, Object> item : resultsList) {
                results.add(new EnhancementReference(
                        getString(item, "enhancement_name"),
                        getString(item, "object_type"),
                        getString(item, "object_name")
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
    private EnhancementHeader parseHeader(String headerJson) {
        if (headerJson == null || headerJson.isBlank() || headerJson.equals("{}")) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            Map<String, Object> headerMap = mapper.readValue(headerJson,
                    new TypeReference<Map<String, Object>>() {});

            return new EnhancementHeader(
                    getString(headerMap, "enhancement_name"),
                    getString(headerMap, "description"),
                    getString(headerMap, "tool_type"),
                    getString(headerMap, "tool_type_text"),
                    getString(headerMap, "devclass"),
                    getString(headerMap, "author"),
                    getString(headerMap, "created_on"),
                    getString(headerMap, "changed_by"),
                    getString(headerMap, "changed_on")
            );
        } catch (Exception e) {
            log.warn("Failed to parse header JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse elements JSON array from ABAP FM response.
     */
    private List<EnhancementElement> parseElements(String elementsJson) {
        if (elementsJson == null || elementsJson.isBlank() || elementsJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> elementsList = mapper.readValue(elementsJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<EnhancementElement> elements = new ArrayList<>();
            for (Map<String, Object> elem : elementsList) {
                elements.add(new EnhancementElement(
                        getString(elem, "element_type"),
                        getString(elem, "spot_name"),
                        getString(elem, "program_name"),
                        getString(elem, "full_name"),
                        getString(elem, "badi_name"),
                        getString(elem, "badi_impl"),
                        getString(elem, "impl_class"),
                        getString(elem, "interface_name"),
                        getBoolean(elem, "active")
                ));
            }
            return elements;
        } catch (Exception e) {
            log.warn("Failed to parse elements JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse source lines JSON array from ABAP FM response.
     * GDC FM returns array of {line_no, code} objects.
     */
    private List<EnhancementSourceLine> parseSourceLines(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank() || sourcesJson.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            List<Map<String, Object>> sourcesList = mapper.readValue(sourcesJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<EnhancementSourceLine> sourceLines = new ArrayList<>();
            for (Map<String, Object> src : sourcesList) {
                sourceLines.add(new EnhancementSourceLine(
                        getInteger(src, "line_no"),
                        getString(src, "code")
                ));
            }
            return sourceLines;
        } catch (Exception e) {
            log.warn("Failed to parse source lines JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return "X".equalsIgnoreCase((String) value) || "true".equalsIgnoreCase((String) value);
        return null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

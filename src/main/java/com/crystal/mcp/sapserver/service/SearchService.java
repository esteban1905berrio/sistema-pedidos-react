package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ABAP repository search operations.
 *
 * This service implements Progressive Discovery Stage 1: Quick Search.
 * Returns lightweight object references without fetching full source code.
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 *
 * Supported operations:
 * - Quick search for ABAP objects by keyword
 *
 * Future operations (Progressive Discovery):
 * - Advanced search with filters
 * - Fuzzy search
 * - Search by object type
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final RfcAdapter rfcAdapter;

    /**
     * Search for ABAP objects using quick search.
     *
     * This method implements Progressive Discovery Stage 1:
     * - Returns lightweight results (names, types, URIs)
     * - No source code fetched (saves tokens)
     * - User can then use get_object_structure or get_object_source for details
     *
     * ADT API Endpoint:
     * GET /sap/bc/adt/repository/informationsystem/search
     * ?operation=quickSearch&query={query}&maxResults={maxResults}
     *
     * XML Response Example:
     * <objectReferences xmlns="http://www.sap.com/adt/quicksearch">
     *   <objectReference adtcore:uri="/sap/bc/adt/oo/classes/zcl_test"
     *                    adtcore:type="CLAS/OC"
     *                    adtcore:name="ZCL_TEST"
     *                    adtcore:packageName="ZTEST">
     *     <adtcore:description>Test Class</adtcore:description>
     *   </objectReference>
     * </objectReferences>
     *
     * @param query      search keyword (e.g., "ZCL_", "payment", "*invoice*")
     * @param maxResults maximum results to return (default: 10, max: 100)
     * @return SearchResult containing list of matching objects
     * @throws RuntimeException if search fails
     */
    public SearchResult searchObjects(String query, Integer maxResults) {
        // Set default max results
        int actualMaxResults = (maxResults != null && maxResults > 0)
                ? Math.min(maxResults, 100)
                : 10;

        // Build ADT API URI
        String uri = "/sap/bc/adt/repository/informationsystem/search";

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("operation", "quickSearch");
        params.put("query", query);
        params.put("maxResults", String.valueOf(actualMaxResults));

        log.info("Searching objects: query='{}', maxResults={}",
                query, actualMaxResults);

        try {
            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri,
                    "GET",
                    null,
                    params,
                    "",
                    "application/xml"
            );

            // Check HTTP status
            if (response.statusCode() == 200) {
                // Log raw XML response for debugging
                log.debug("Raw XML response (first 500 chars): {}",
                        response.text().substring(0, Math.min(500, response.text().length())));

                // Parse XML response
                List<SearchResult.ObjectReference> results = parseSearchResults(response.text());

                log.info("Found {} objects for query '{}'", results.size(), query);

                return new SearchResult(
                        query,
                        actualMaxResults,
                        results.size(),  // Note: ADT API doesn't return total count
                        results
                );
            } else {
                String errorMsg = String.format(
                        "Search failed: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error searching objects for query '{}': {}",
                    query, e.getMessage(), e);
            throw new RuntimeException("Failed to search objects", e);
        }
    }

    /**
     * Parse XML search results from ADT API response.
     *
     * Handles namespaces:
     * - xmlns="http://www.sap.com/adt/quicksearch"
     * - xmlns:adtcore="http://www.sap.com/adt/core"
     *
     * @param xmlText XML response body
     * @return list of ObjectReference objects
     */
    private List<SearchResult.ObjectReference> parseSearchResults(String xmlText) {
        List<SearchResult.ObjectReference> results = new ArrayList<>();

        try {
            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);  // Important for namespace handling

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8))
            );

            // Define namespaces
            String ADTCORE_NS = "http://www.sap.com/adt/core";

            // Find all objectReference elements
            NodeList objectRefs = doc.getElementsByTagNameNS("*", "objectReference");

            for (int i = 0; i < objectRefs.getLength(); i++) {
                Element objRef = (Element) objectRefs.item(i);

                // Extract attributes from adtcore namespace
                String name = objRef.getAttributeNS(ADTCORE_NS, "name");
                String type = objRef.getAttributeNS(ADTCORE_NS, "type");
                String uri = objRef.getAttributeNS(ADTCORE_NS, "uri");
                String packageName = objRef.getAttributeNS(ADTCORE_NS, "packageName");

                // Extract description from child element
                String description = "";
                NodeList descNodes = objRef.getElementsByTagNameNS(ADTCORE_NS, "description");
                if (descNodes.getLength() > 0) {
                    description = descNodes.item(0).getTextContent();
                }

                // Create ObjectReference
                SearchResult.ObjectReference ref = new SearchResult.ObjectReference(
                        name != null ? name : "",
                        type != null ? type : "",
                        uri != null ? uri : "",
                        description != null ? description.trim() : "",
                        packageName != null ? packageName : ""
                );

                results.add(ref);

                log.debug("Found object: {} ({}) in package {}",
                        name, type, packageName);
            }

        } catch (Exception e) {
            log.error("Failed to parse search results XML: {}", e.getMessage(), e);
            throw new RuntimeException("XML parsing error", e);
        }

        return results;
    }
}

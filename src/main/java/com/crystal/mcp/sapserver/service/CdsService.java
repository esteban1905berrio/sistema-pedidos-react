package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.CdsSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for CDS View operations.
 *
 * This service provides business logic for retrieving CDS View (Core Data Services)
 * source code from SAP systems via the ADT (ABAP Development Tools) API through RFC.
 *
 * CDS Views are the foundation of SAP's modern data modeling approach:
 * - Used extensively in S/4HANA for analytical and transactional scenarios
 * - Define data models with annotations for Fiori UI generation
 * - Support associations, aggregations, and complex expressions
 *
 * ADT API Endpoint:
 * GET /sap/bc/adt/ddic/ddl/sources/{cdsName}?version={version}
 *
 * Progressive Discovery Integration:
 * - Stage 1: search_objects (SearchService) → Find CDS Views (type DDLS)
 * - Stage 2: get_object_structure (ObjectService) → Get CDS metadata
 * - Stage 3: get_cds_source (CdsService) → Get source code
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdsService {

    private final RfcAdapter rfcAdapter;

    /**
     * Get CDS View source code.
     *
     * Retrieves the DDL source definition of a CDS View from SAP.
     * The source includes:
     * - Annotations (@AbapCatalog, @UI, @Analytics, etc.)
     * - View definition with SELECT statement
     * - Associations and compositions
     * - Parameters (if parameterized view)
     *
     * ADT API Endpoint:
     * GET /sap/bc/adt/ddic/ddl/sources/{cdsName}?version={version}
     *
     * @param cdsName name of CDS View (e.g., "I_BUSINESSPARTNER", "ZCDS_INVOICE")
     * @param version version to retrieve ("active" or "inactive"), defaults to "active"
     * @return CdsSourceResult containing source code and metadata
     * @throws RuntimeException if RFC call fails or CDS View not found
     */
    public CdsSourceResult getCdsSource(String cdsName, String version) {
        // Normalize parameters
        String normalizedName = cdsName.toUpperCase().trim();
        String normalizedVersion = (version == null || version.isBlank()) ? "active" : version.toLowerCase();

        // Build ADT API URI
        String uri = String.format("/sap/bc/adt/ddic/ddl/sources/%s/source/main", normalizedName);

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("version", normalizedVersion);

        log.info("Fetching CDS View source: {} (version: {})", normalizedName, normalizedVersion);

        try {
            // Execute RFC request via adapter
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri,
                    "GET",
                    null,  // no custom headers
                    params,
                    "",    // no body for GET
                    "text/plain"
            );

            // Check HTTP status
            if (response.statusCode() == 200) {
                String source = response.text();
                log.info("Successfully retrieved CDS View {} ({} chars)",
                        normalizedName, source.length());

                // Build metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("contentType", response.headers().getOrDefault("content-type", "text/plain"));
                metadata.put("uri", uri);

                return new CdsSourceResult(
                        source,
                        normalizedName,
                        normalizedVersion,
                        metadata
                );
            } else if (response.statusCode() == 404) {
                String errorMsg = String.format("CDS View '%s' not found (HTTP 404)", normalizedName);
                log.warn(errorMsg);
                throw new RuntimeException(errorMsg);
            } else {
                String errorMsg = String.format(
                        "Failed to get CDS View source: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error fetching CDS View %s: %s",
                    normalizedName, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
}

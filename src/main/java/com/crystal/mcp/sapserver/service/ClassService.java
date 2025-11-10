package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ClassSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for ABAP class operations.
 *
 * This service provides business logic for retrieving ABAP class information
 * from SAP systems via the ADT (ABAP Development Tools) API through RFC.
 *
 * Supported operations (POC phase):
 * - Get class source code by include type (main, implementation, testclasses, macros)
 *
 * Future operations (post-POC):
 * - Get class structure (methods, attributes, visibility)
 * - Get class components (detailed metadata)
 * - Get class includes list
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService {

    private final RfcAdapter rfcAdapter;

    /**
     * Get ABAP class source code.
     *
     * This method replicates the Python ClassService.get_class_source() behavior.
     *
     * ADT API Endpoint Pattern:
     * GET /sap/bc/adt/oo/classes/{className}/source/{includeType}?version={version}
     *
     * Include Types:
     * - main: Class definition (PUBLIC, PROTECTED, PRIVATE sections)
     * - implementation: Method implementations
     * - testclasses: Unit test classes
     * - macros: ABAP macros
     *
     * @param className   name of ABAP class (e.g., "CL_ABAP_CHAR_UTILITIES", "ZTEST_CLASS")
     * @param version     version to retrieve ("active" or "inactive")
     * @param includeType include type to retrieve (default: "main")
     * @return ClassSourceResult containing source code and metadata
     * @throws RuntimeException if RFC call fails or returns non-200 status
     */
    public ClassSourceResult getClassSource(
            String className,
            String version,
            String includeType
    ) {
        // Build ADT API URI
        String uri = String.format("/sap/bc/adt/oo/classes/%s/source/%s",
                className, includeType);

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("version", version);

        log.info("Fetching source for class {} ({}, include: {})",
                className, version, includeType);

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
                log.debug("Successfully retrieved source for {} ({} bytes)",
                        className, response.text().length());

                // Build metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("uri", uri);
                metadata.put("responseHeaders", response.headers());

                return new ClassSourceResult(
                        response.text(),
                        className,
                        version,
                        includeType,
                        metadata
                );
            } else {
                // Handle error responses
                String errorMsg = String.format(
                        "Failed to get class source: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error fetching class source for {}: {}",
                    className, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve class source", e);
        }
    }
}

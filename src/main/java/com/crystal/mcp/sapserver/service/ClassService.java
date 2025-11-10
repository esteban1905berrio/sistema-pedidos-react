package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ClassIncludeResult;
import com.crystal.mcp.sapserver.model.ClassSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ABAP class operations.
 *
 * This service provides business logic for retrieving ABAP class information
 * from SAP systems via the ADT (ABAP Development Tools) API through RFC.
 *
 * Progressive Discovery Integration:
 * - Stage 1: search_objects (SearchService) → Find classes
 * - Stage 2: get_object_structure (ObjectService) → Get class metadata
 * - Stage 2.5: get_class_includes (ClassService) → List includes
 * - Stage 3: get_class_source or get_include_source → Get source code
 *
 * Supported operations:
 * - Get class source code by include type (main, implementation, testclasses, macros)
 * - Get class includes list (with existence check)
 *
 * Future operations:
 * - Get class structure (methods, attributes, visibility)
 * - Get class components (detailed metadata)
 * - Lock/unlock class for editing
 * - Set class source (update code)
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

    /**
     * Get all includes of an ABAP class.
     *
     * This method checks the existence of standard ABAP class include types:
     * - definitions: Class definition (attributes, method declarations)
     * - implementations: Method implementations
     * - testclasses: Unit test classes
     * - macros: Macro definitions
     *
     * Progressive Discovery Stage 2.5:
     * - Use after get_object_structure shows it's a class
     * - Identifies which includes exist (without fetching source)
     * - Enables selective fetching with get_include_source
     * - Enables parallel fetching of multiple includes
     *
     * Token Optimization:
     * - Checks existence only: ~200 tokens
     * - Avoids fetching source: saves ~2,000+ tokens per include
     * - Allows selective fetching of only needed includes
     *
     * ADT API Endpoint Pattern:
     * /sap/bc/adt/oo/classes/{class_name}/includes/{include_type}
     *
     * Workflow Example:
     * 1. User: "What includes does ZCL_INVOICE have?"
     * 2. Claude: get_class_includes("ZCL_INVOICE") → definitions, implementations exist
     * 3. User: "Show me the implementations"
     * 4. Claude: get_include_source("ZCL_INVOICE", "implementations") → Get specific include
     *
     * @param className name of the ABAP class (e.g., "ZCL_TEST")
     * @return ClassIncludeResult containing list of includes with existence info
     */
    public ClassIncludeResult getClassIncludes(String className) {
        // Validate inputs
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be empty");
        }

        // Standard ABAP class include types
        String[] includeTypes = {"definitions", "implementations", "testclasses", "macros"};

        log.info("Getting includes for class: {}", className);

        List<ClassIncludeResult.Include> includes = new ArrayList<>();
        String classNameLower = className.toLowerCase();

        // Check each include type
        for (String includeType : includeTypes) {
            String uri = String.format(
                    "/sap/bc/adt/oo/classes/%s/includes/%s",
                    classNameLower,
                    includeType
            );

            try {
                // Execute RFC request (HEAD would be better, but ADT uses GET)
                RfcAdapter.RfcResponse response = rfcAdapter.request(
                        uri,
                        "GET",
                        null,
                        new HashMap<>(),
                        "",
                        "text/plain"
                );

                // Check if include exists
                if (response.statusCode() == 200) {
                    long sizeBytes = response.text() != null ? response.text().length() : 0;
                    includes.add(new ClassIncludeResult.Include(
                            includeType,
                            uri,
                            true,
                            sizeBytes
                    ));
                    log.debug("Include '{}' exists for class {} ({} bytes)",
                            includeType, className, sizeBytes);
                } else if (response.statusCode() == 404) {
                    // Include doesn't exist (this is normal, not all classes have all includes)
                    log.debug("Include '{}' does not exist for class {}", includeType, className);
                } else {
                    // Unexpected status code
                    log.warn("Unexpected status {} for include '{}' in class {}",
                            response.statusCode(), includeType, className);
                }

            } catch (Exception e) {
                // Log error but continue checking other includes
                log.warn("Error checking include '{}' for class {}: {}",
                        includeType, className, e.getMessage());
            }
        }

        log.info("Retrieved {} includes for class {}", includes.size(), className);

        return new ClassIncludeResult(className, includes.size(), includes);
    }
}

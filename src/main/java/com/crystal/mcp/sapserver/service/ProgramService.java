package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.IncludeSourceResult;
import com.crystal.mcp.sapserver.model.ProgramSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for ABAP program operations.
 *
 * This service handles operations specific to ABAP programs (reports, module pools, etc.).
 * Programs are executable ABAP units that can contain includes, subroutines, and other logic.
 *
 * Progressive Discovery Integration:
 * - Stage 1: search_objects (SearchService) → Find programs
 * - Stage 2: get_object_structure (ObjectService) → Get program metadata, includes list
 * - Stage 3: get_program_source (ProgramService) → Get full program source
 * - Stage 3+: get_include_source (ProgramService) → Get individual include sources
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 *
 * Supported operations:
 * - Get program source code
 * - Get include source code
 *
 * Future operations:
 * - Lock/unlock programs for editing
 * - Set program source (update code)
 * - List program includes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramService {

    private final RfcAdapter rfcAdapter;

    /**
     * Get source code for an ABAP program.
     *
     * This method retrieves the complete source code for an ABAP program
     * (reports, module pools, function group main programs, etc.).
     *
     * Progressive Discovery Stage 3:
     * - Use after search_objects identifies the program
     * - Use after get_object_structure shows program metadata
     * - Only fetches source when actually needed (token optimization)
     *
     * ADT API Endpoint:
     * /sap/bc/adt/programs/programs/{name}/source/main
     *
     * Examples:
     * - Report: ZREP_INVOICE_LIST
     * - Module Pool: SAPMZTEST
     * - Function Group Main: SAPLZFG_UTILS
     *
     * @param programName name of the ABAP program
     * @param version     version to retrieve ("active" or "inactive")
     * @return ProgramSourceResult containing source code and metadata
     * @throws RuntimeException if program not found or access fails
     */
    public ProgramSourceResult getProgramSource(String programName, String version) {
        // Validate inputs
        if (programName == null || programName.trim().isEmpty()) {
            throw new IllegalArgumentException("Program name cannot be empty");
        }

        // Set default version
        String actualVersion = (version != null && !version.isEmpty()) ? version : "active";

        // Build URI
        String uri = String.format("/sap/bc/adt/programs/programs/%s/source/main", programName);

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("version", actualVersion);

        log.info("Fetching source for program: {} (version: {})",
                programName, actualVersion);

        try {
            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri,
                    "GET",
                    null,
                    params,
                    "",
                    "text/plain"
            );

            // Check HTTP status
            if (response.statusCode() == 200) {
                log.debug("Successfully retrieved program source ({} bytes)",
                        response.text().length());

                // Build metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("uri", uri);
                metadata.put("responseHeaders", response.headers());
                metadata.put("sourceLength", response.text().length());

                return new ProgramSourceResult(
                        response.text(),
                        programName,
                        actualVersion,
                        metadata
                );
            } else {
                String errorMsg = String.format(
                        "Failed to get program source: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error fetching program source for '{}': {}",
                    programName, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve program source", e);
        }
    }

    /**
     * Get source code for a program include.
     *
     * This method retrieves the source code for a specific include within a program.
     * Includes are modular ABAP code units that are included in main programs.
     *
     * Progressive Discovery Stage 3+:
     * - Use after get_object_structure lists includes for a program
     * - Use after get_class_includes lists includes for a class
     * - Allows fetching individual includes without loading entire program
     *
     * ADT API Endpoint:
     * /sap/bc/adt/programs/programs/{program}/includes/{include}/source/main
     *
     * Examples:
     * - Top include: ZREP_TOP
     * - Form include: ZREP_F01
     * - Class include: ZCL_TEST===============CCAU (class auxiliary)
     *
     * @param programName name of the parent program
     * @param includeName name of the include
     * @param version     version to retrieve ("active" or "inactive")
     * @return IncludeSourceResult containing source code and metadata
     * @throws RuntimeException if include not found or access fails
     */
    public IncludeSourceResult getIncludeSource(
            String programName,
            String includeName,
            String version
    ) {
        // Validate inputs
        if (programName == null || programName.trim().isEmpty()) {
            throw new IllegalArgumentException("Program name cannot be empty");
        }
        if (includeName == null || includeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Include name cannot be empty");
        }

        // Set default version
        String actualVersion = (version != null && !version.isEmpty()) ? version : "active";

        // Build URI
        String uri = String.format(
                "/sap/bc/adt/programs/programs/%s/includes/%s/source/main",
                programName,
                includeName
        );

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("version", actualVersion);

        log.info("Fetching source for include: {} in program {} (version: {})",
                includeName, programName, actualVersion);

        try {
            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri,
                    "GET",
                    null,
                    params,
                    "",
                    "text/plain"
            );

            // Check HTTP status
            if (response.statusCode() == 200) {
                log.debug("Successfully retrieved include source ({} bytes)",
                        response.text().length());

                // Build metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("uri", uri);
                metadata.put("responseHeaders", response.headers());
                metadata.put("sourceLength", response.text().length());

                return new IncludeSourceResult(
                        response.text(),
                        programName,
                        includeName,
                        actualVersion,
                        metadata
                );
            } else {
                String errorMsg = String.format(
                        "Failed to get include source: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error fetching include source for '{}/{}': {}",
                    programName, includeName, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve include source", e);
        }
    }
}

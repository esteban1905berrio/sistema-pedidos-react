package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.IncludeSourceResult;
import com.crystal.mcp.sapserver.model.ProgramSourceResult;
import com.crystal.mcp.sapserver.model.ProgramModifyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
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
 * - Lock/unlock programs and includes for editing
 * - Modify program/include source code (workflow-based)
 *
 * Future operations:
 * - List program includes
 * - Syntax check before modification
 * - Activation after modification
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
     * GET /sap/bc/adt/programs/includes/{include}/source/main
     * Headers:
     * - Accept: text/plain
     * - Cache-Control: no-cache
     *
     * Examples:
     * - Top include: ZREP_TOP
     * - Form include: ZREP_F01
     * - Class include: ZCL_TEST===============CCAU (class auxiliary)
     *
     * @param programName name of the parent program (for logging/reference only)
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
        if (includeName == null || includeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Include name cannot be empty");
        }

        // programName is kept for backward compatibility but not used in URI
        if (programName == null || programName.trim().isEmpty()) {
            programName = ""; // Use empty string for logging if not provided
        }

        // Set default version
        String actualVersion = (version != null && !version.isEmpty()) ? version : "active";

        // Build URI - includes are accessed directly, not via parent program
        String uri = String.format(
                "/sap/bc/adt/programs/includes/%s/source/main",
                includeName.toLowerCase()
        );

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("version", actualVersion);

        log.info("Fetching source for include: {} (parent: {}, version: {})",
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

    // ============================================================================
    // MODIFICATION WORKFLOW OPERATIONS (Workflow-based tools)
    // ============================================================================

    /**
     * Complete workflow to modify an ABAP program or include.
     *
     * This is a workflow-based tool that orchestrates the complete ADT modification flow:
     * LOCK → MODIFY → UNLOCK
     *
     * Workflow Steps:
     * 1. LOCK: Acquire exclusive lock on object
     * 2. MODIFY: Update source code with new content
     * 3. UNLOCK: Release lock (always executed, even on failure)
     *
     * Based on Python implementation: modification_service.py
     * Reference: docs/requirements/mcp/workflow_based/pr_update_program.md
     *
     * Supports both:
     * - Programs: /sap/bc/adt/programs/programs/{name}
     * - Includes: /sap/bc/adt/programs/includes/{name}
     *
     * @param objectName   name of the program or include
     * @param newSource    new source code to set
     * @param objectType   "program" or "include"
     * @param transport    optional transport number (if null, uses system-assigned transport)
     * @return ProgramModifyResult with detailed workflow execution status
     * @throws RuntimeException if modification workflow fails
     */
    public ProgramModifyResult modifyProgramSource(
            String objectName,
            String newSource,
            String objectType,
            String transport
    ) {
        // Validate inputs
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Object name cannot be empty");
        }
        if (newSource == null) {
            throw new IllegalArgumentException("Source code cannot be null");
        }
        if (objectType == null || (!objectType.equals("program") && !objectType.equals("include"))) {
            throw new IllegalArgumentException("Object type must be 'program' or 'include'");
        }

        log.info("Starting modification workflow for {}: {}", objectType, objectName);

        // Build URIs based on object type
        String objectUri;
        String sourceUri;
        if (objectType.equals("program")) {
            objectUri = String.format("/sap/bc/adt/programs/programs/%s", objectName.toLowerCase());
            sourceUri = String.format("%s/source/main", objectUri);
        } else {
            objectUri = String.format("/sap/bc/adt/programs/includes/%s", objectName.toLowerCase());
            sourceUri = String.format("%s/source/main", objectUri);
        }

        // Initialize result
        ProgramModifyResult result = new ProgramModifyResult();
        result.setUri(sourceUri);
        result.setObjectName(objectName);
        result.setObjectType(objectType);

        String lockHandle = null;

        try {
            // ========================================
            // Step 1: Lock object
            // ========================================
            log.info("Step 1/3: Locking {} '{}'", objectType, objectName);
            LockResult lockResult = lockObject(sourceUri);
            lockHandle = lockResult.lockHandle;

            result.setLocked(true);
            result.setLockHandle(lockHandle);
            result.setTransportNumber(lockResult.transportNumber);
            result.setTransportUser(lockResult.transportUser);
            result.setTransportDescription(lockResult.transportDescription);

            log.info("✓ {} locked successfully (transport: {})", objectType, lockResult.transportNumber);
            result.addMessage("info",
                    String.format("Object locked successfully. Transport: %s", lockResult.transportNumber),
                    "lock");

            // ========================================
            // Step 2: Modify source code
            // ========================================
            log.info("Step 2/3: Modifying source code ({} bytes)", newSource.length());

            // Use transport from lock response if not explicitly provided
            String effectiveTransport = (transport != null && !transport.isEmpty())
                    ? transport
                    : lockResult.transportNumber;

            boolean modified = setObjectSource(sourceUri, newSource, lockHandle, effectiveTransport);
            result.setModified(modified);

            log.info("✓ Source code modified successfully");
            result.addMessage("info",
                    String.format("Source code updated (%d bytes)", newSource.length()),
                    "modify");

        } catch (Exception e) {
            log.error("✗ Modification workflow failed: {}", e.getMessage(), e);
            result.addMessage("error", e.getMessage(), "modify");
            throw new RuntimeException("Modification workflow failed: " + e.getMessage(), e);

        } finally {
            // ========================================
            // Step 3: Unlock (ALWAYS EXECUTE)
            // ========================================
            if (lockHandle != null) {
                try {
                    log.info("Step 3/3: Unlocking {} '{}'", objectType, objectName);
                    unlockObject(sourceUri, lockHandle);
                    result.setUnlocked(true);
                    log.info("✓ {} unlocked successfully", objectType);
                    result.addMessage("info", "Object unlocked successfully", "unlock");

                } catch (Exception unlockError) {
                    log.error("✗ Failed to unlock {}: {}", objectType, unlockError.getMessage());
                    result.addMessage("warning",
                            "Failed to unlock object: " + unlockError.getMessage(),
                            "unlock");
                }
            }
        }

        // Final result
        result.setSuccess(result.isLocked() && result.isModified() && result.isUnlocked());

        if (result.isSuccess()) {
            log.info("✓✓✓ Modification workflow completed successfully for {} '{}'",
                    objectType, objectName);
        } else {
            log.error("✗✗✗ Modification workflow failed for {} '{}'",
                    objectType, objectName);
        }

        return result;
    }

    // ============================================================================
    // LOCK/UNLOCK OPERATIONS (Private helpers)
    // ============================================================================

    /**
     * Lock an ABAP object for editing.
     *
     * ADT API Endpoint:
     * POST /sap/bc/adt/programs/{type}/{name}?_action=LOCK&accessMode=MODIFY
     *
     * Response (XML):
     * <pre>
     * {@code
     * <asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
     *   <asx:values>
     *     <DATA>
     *       <LOCK_HANDLE>93F5650FDF763E3CF1B9FD12266CC9E7E59262CA</LOCK_HANDLE>
     *       <CORRNR>CADK911122</CORRNR>
     *       <CORRUSER>L_ABAPS_ITA</CORRUSER>
     *       <CORRTEXT>FI WB AAC002 Description</CORRTEXT>
     *     </DATA>
     *   </asx:values>
     * </asx:abap>
     * }
     * </pre>
     *
     * @param objectUri URI of the object to lock
     * @return LockResult with handle and transport information
     * @throws RuntimeException if lock fails (already locked, no permissions, etc.)
     */
    private LockResult lockObject(String objectUri) {
        Map<String, String> params = new HashMap<>();
        params.put("_action", "LOCK");
        params.put("accessMode", "MODIFY");

        log.debug("Locking object: {}", objectUri);

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "POST",
                    null,
                    params,
                    "",
                    "application/vnd.sap.as+xml;charset=UTF-8"
            );

            if (response.statusCode() == 200) {
                // Parse XML response to extract lock info
                LockResult lockResult = parseLockResponse(response.text());
                log.debug("Lock acquired: handle={}, transport={}",
                        lockResult.lockHandle.substring(0, Math.min(20, lockResult.lockHandle.length())),
                        lockResult.transportNumber);
                return lockResult;
            } else if (response.statusCode() == 409) {
                // Object is already locked by another user
                String errorMsg = String.format(
                        "Object is already locked by another user. Cannot acquire lock. (HTTP 409)"
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            } else {
                String errorMsg = String.format(
                        "Failed to lock object: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error locking object '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to lock object", e);
        }
    }

    /**
     * Unlock an ABAP object after editing.
     *
     * ADT API Endpoint:
     * POST /sap/bc/adt/programs/{type}/{name}?_action=UNLOCK&lockHandle={handle}
     *
     * @param objectUri  URI of the object to unlock
     * @param lockHandle lock handle from lock operation
     * @return true if unlock successful
     * @throws RuntimeException if unlock fails
     */
    private boolean unlockObject(String objectUri, String lockHandle) {
        Map<String, String> params = new HashMap<>();
        params.put("_action", "UNLOCK");
        params.put("lockHandle", lockHandle);

        log.debug("Unlocking object: {}", objectUri);

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "POST",
                    null,
                    params,
                    "",
                    "application/vnd.sap.as+xml;charset=UTF-8"
            );

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.debug("Object unlocked successfully");
                return true;
            } else {
                String errorMsg = String.format(
                        "Failed to unlock object: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error unlocking object '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to unlock object", e);
        }
    }

    /**
     * Set (update) source code for an ABAP object.
     *
     * IMPORTANT: Object must be locked before calling this method.
     *
     * ADT API Endpoint:
     * PUT /sap/bc/adt/programs/{type}/{name}/source/main?lockHandle={handle}&corrNr={transport}
     *
     * @param objectUri  URI of the object source (/source/main)
     * @param sourceCode new source code content
     * @param lockHandle lock handle from lock operation
     * @param transport  transport number (required for transportable packages)
     * @return true if modification successful
     * @throws RuntimeException if modification fails
     */
    private boolean setObjectSource(
            String objectUri,
            String sourceCode,
            String lockHandle,
            String transport
    ) {
        Map<String, String> params = new HashMap<>();
        params.put("lockHandle", lockHandle);
        if (transport != null && !transport.isEmpty()) {
            params.put("corrNr", transport);
        }

        log.debug("Setting source for object: {} ({} bytes)", objectUri, sourceCode.length());

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "PUT",
                    null,
                    params,
                    sourceCode,
                    "text/plain; charset=utf-8"
            );

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.debug("Source code updated successfully");
                return true;
            } else {
                String errorMsg = String.format(
                        "Failed to set object source: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error setting source for object '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to set object source", e);
        }
    }

    // ============================================================================
    // XML PARSING HELPERS
    // ============================================================================

    /**
     * Parse XML response from LOCK operation.
     *
     * Extracts:
     * - LOCK_HANDLE: Required for subsequent operations
     * - CORRNR: Transport number (system-assigned or existing)
     * - CORRUSER: User who owns the transport
     * - CORRTEXT: Transport description
     *
     * @param xmlResponse XML response from LOCK operation
     * @return LockResult with parsed values
     * @throws RuntimeException if parsing fails or LOCK_HANDLE not found
     */
    private LockResult parseLockResponse(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            Element dataElement = (Element) doc.getElementsByTagName("DATA").item(0);

            if (dataElement == null) {
                throw new RuntimeException("No DATA element found in lock response");
            }

            String lockHandle = getElementText(dataElement, "LOCK_HANDLE");
            String transportNumber = getElementText(dataElement, "CORRNR");
            String transportUser = getElementText(dataElement, "CORRUSER");
            String transportDescription = getElementText(dataElement, "CORRTEXT");

            if (lockHandle == null || lockHandle.isEmpty()) {
                throw new RuntimeException("LOCK_HANDLE not found in response: " + xmlResponse);
            }

            LockResult result = new LockResult();
            result.lockHandle = lockHandle;
            result.transportNumber = transportNumber != null ? transportNumber : "";
            result.transportUser = transportUser != null ? transportUser : "";
            result.transportDescription = transportDescription != null ? transportDescription : "";

            return result;

        } catch (Exception e) {
            log.error("Failed to parse lock response XML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse lock response", e);
        }
    }

    /**
     * Get text content of XML element.
     */
    private String getElementText(Element parent, String tagName) {
        org.w3c.dom.NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Element element = (Element) nodes.item(0);
            return element.getTextContent();
        }
        return null;
    }

    /**
     * Internal class to hold lock response data.
     */
    private static class LockResult {
        String lockHandle;
        String transportNumber;
        String transportUser;
        String transportDescription;
    }
}

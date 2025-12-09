package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.IncludeSourceResult;
import com.crystal.mcp.sapserver.model.ProgramSourceResult;
import com.crystal.mcp.sapserver.model.ProgramModifyResult;
import com.crystal.mcp.sapserver.model.SyntaxCheckMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    private final StatefulModificationService statefulModificationService;
    private final ActivationService activationService;

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
     * LOCK → MODIFY → UNLOCK using stateful connections (JCoContext).
     *
     * Workflow Steps:
     * 1. Begin stateful context (JCoContext)
     * 2. LOCK: Acquire exclusive lock on object (stateful session)
     * 3. SYNTAX CHECK: Validate source before modification (stateful session)
     * 4. MODIFY: Update source code with new content (stateful session)
     * 5. UNLOCK: Release lock (stateful session)
     * 6. End stateful context (JCoContext)
     * 7. ACTIVATE: Activate object (outside stateful context)
     *
     * The stateful context ensures that all operations use the same SAP session,
     * which is critical for maintaining the lock throughout the workflow.
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

        log.info("🔧 Starting stateful modification workflow for {}: {}", objectType, objectName);

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

        // Execute workflow in stateful context
        ProgramModifyResult workflowResult = statefulModificationService.executeStatefulWorkflow(
                objectName,
                () -> {
                    // Initialize result object
                    ProgramModifyResult result = new ProgramModifyResult();
                    result.setUri(sourceUri);
                    result.setObjectName(objectName);
                    result.setObjectType(objectType);

                    // ========================================
                    // Step 1: Lock object (in stateful session)
                    // ========================================
                    log.info("Step 1/4: Locking {} '{}'", objectType, objectName);

                    StatefulModificationService.LockResult lock =
                            statefulModificationService.lockObject(objectUri);

                    result.setLocked(true);
                    result.setLockHandle(lock.lockHandle());
                    result.setTransportNumber(lock.transportNumber());
                    result.setTransportUser(lock.transportUser());
                    result.setTransportDescription(lock.transportDescription());

                    log.info("✓ {} locked successfully (transport: {})", objectType, lock.transportNumber());
                    result.addMessage("info",
                            String.format("Object locked successfully. Transport: %s", lock.transportNumber()),
                            "lock");

                    try {
                        // ========================================
                        // Step 2: Syntax Check (in stateful session)
                        // ========================================
                        log.info("Step 2/4: Running syntax check...");

                        List<SyntaxCheckMessage> syntaxMessages = syntaxCheck(
                                objectUri,      // Object URI (without /source/main)
                                sourceUri,      // Source URI (with /source/main)
                                newSource,
                                "inactive"      // Check against inactive version
                        );

                        // Check for errors
                        List<SyntaxCheckMessage> errors = syntaxMessages.stream()
                                .filter(SyntaxCheckMessage::isError)
                                .collect(Collectors.toList());

                        if (!errors.isEmpty()) {
                            String errorMsg = String.format(
                                    "Syntax check failed with %d error(s):", errors.size()
                            );
                            log.error(errorMsg);

                            for (SyntaxCheckMessage error : errors) {
                                log.error("  {}", error.toFormattedString());
                            }

                            result.addMessage("error", errorMsg, "syntax_check");
                            throw new RuntimeException(errorMsg);
                        }

                        log.info("✓ Syntax check passed ({} message(s))", syntaxMessages.size());
                        result.addMessage("info",
                                String.format("Syntax check passed (%d message(s))", syntaxMessages.size()),
                                "syntax_check");

                        // ========================================
                        // Step 3: Modify source code (in stateful session)
                        // ========================================
                        log.info("Step 3/4: Modifying source code ({} bytes)", newSource.length());

                        // Use transport from lock response if not explicitly provided
                        String effectiveTransport = (transport != null && !transport.isEmpty())
                                ? transport
                                : lock.transportNumber();

                        boolean modified = setObjectSource(
                                sourceUri,
                                newSource,
                                lock.lockHandle(),
                                effectiveTransport
                        );
                        result.setModified(modified);

                        log.info("✓ Source code modified successfully");
                        result.addMessage("info",
                                String.format("Source code updated (%d bytes)", newSource.length()),
                                "modify");

                        return result;

                    } finally {
                        // ========================================
                        // Step 4: Unlock (ALWAYS execute, even on error, in stateful session)
                        // ========================================
                        log.info("Step 4/4: Unlocking {} '{}'", objectType, objectName);
                        statefulModificationService.unlockObject(objectUri, lock.lockHandle());
                        result.setUnlocked(true);
                        log.info("✓ {} unlocked successfully", objectType);
                        result.addMessage("info", "Object unlocked successfully", "unlock");
                    }
                }
        );

        // ========================================
        // Step 5: Check syntax and activate (outside stateful context)
        // ========================================
        if (workflowResult.isSuccess()) {
            log.info("Step 5/5: Checking syntax and activating {} '{}'", objectType, objectName);
            try {
                var activationResult = activationService.checkAndActivate(sourceUri);
                workflowResult.setActivated(activationResult.success());

                if (activationResult.success()) {
                    log.info("✓ {} activated successfully", objectType);
                    workflowResult.addMessage("info", objectType + " activated successfully", "activate");
                } else {
                    log.warn("⚠️  Activation failed with {} errors", activationResult.errors().size());
                    workflowResult.addMessage("warning",
                            String.format("Activation failed: %s", activationResult.message()),
                            "activate");

                    // Add syntax errors as messages
                    for (var error : activationResult.errors()) {
                        workflowResult.addMessage("error",
                                String.format("Line %d: %s", error.line(), error.shortText()),
                                "syntax");
                    }
                }
            } catch (Exception e) {
                log.error("Error during activation", e);
                workflowResult.setActivated(false);
                workflowResult.addMessage("error",
                        "Activation failed: " + e.getMessage(),
                        "activate");
            }
        }

        // Set overall success based on workflow steps
        workflowResult.setSuccess(workflowResult.isLocked()
                && workflowResult.isModified()
                && workflowResult.isUnlocked()
                && workflowResult.isActivated());

        log.info("🎯 {} modification workflow completed: {} (success: {}, activated: {})",
                objectType, objectName, workflowResult.isSuccess(), workflowResult.isActivated());

        return workflowResult;
    }

    /**
     * Complete workflow to modify a function module source code.
     * <p>
     * This is a workflow-based method that orchestrates the complete ADT modification flow:
     * LOCK → MODIFY → UNLOCK
     * <p>
     * Similar to modifyProgramSource but specifically for function modules.
     * Function modules have a different URI structure: /functions/groups/{fg}/fmodules/{fm}
     * <p>
     * Based on Python implementation: modification_service.py::modify_function_module()
     *
     * @param functionModuleName name of the function module (e.g., "Z_TEST_FM")
     * @param functionGroupName  parent function group name (e.g., "ZTEST_FG")
     * @param newSource          new source code to set
     * @param transport          optional transport number (if null, uses system-assigned from LOCK)
     * @return ProgramModifyResult with detailed workflow execution status
     */
    /**
     * Modify function module source code using stateful workflow.
     *
     * This method now uses StatefulModificationService to execute the complete
     * LOCK → MODIFY → UNLOCK workflow with stateful connection support.
     *
     * Workflow:
     * 1. Begin stateful context (JCoContext)
     * 2. Lock function module (stateful session)
     * 3. Syntax check (optional, stateful session)
     * 4. Modify source code (stateful session)
     * 5. Unlock function module (stateful session)
     * 6. End stateful context (JCoContext)
     *
     * The stateful context ensures that all operations use the same SAP session,
     * which is critical for maintaining the lock throughout the workflow.
     *
     * @param functionModuleName name of the function module (e.g., "Z_TEST_FM")
     * @param functionGroupName  parent function group name (e.g., "ZTEST_FG")
     * @param newSource          new source code
     * @param transport          transport number (optional, uses lock response if null)
     * @return ProgramModifyResult with status and metadata
     * @throws RuntimeException if workflow fails
     */
    public ProgramModifyResult modifyFunctionModuleSource(
            String functionModuleName,
            String functionGroupName,
            String newSource,
            String transport
    ) {
        log.info("🔧 Starting stateful modification workflow for function module: {} (group: {})",
                functionModuleName, functionGroupName);

        // Build function module URIs
        String fmUri = String.format("/sap/bc/adt/functions/groups/%s/fmodules/%s",
                functionGroupName.toLowerCase(), functionModuleName.toLowerCase());
        String fmSourceUri = fmUri + "/source/main";

        // Execute workflow in stateful context
        ProgramModifyResult workflowResult = statefulModificationService.executeStatefulWorkflow(
                functionModuleName,
                () -> {
                    // Initialize result object
                    ProgramModifyResult result = new ProgramModifyResult();
                    result.setObjectName(functionModuleName);
                    result.setObjectType("function_module");
                    result.setUri(fmSourceUri);

                    // ========================================
                    // Step 1: Lock function module (in stateful session)
                    // ========================================
                    log.info("Step 1/4: Locking function module...");

                    StatefulModificationService.LockResult lock =
                            statefulModificationService.lockObject(fmUri);

                    result.setLocked(true);
                    result.setLockHandle(lock.lockHandle());
                    result.setTransportNumber(lock.transportNumber());
                    result.setTransportUser(lock.transportUser());
                    result.setTransportDescription(lock.transportDescription());

                    log.info("✓ Function module locked successfully (transport: {})",
                            lock.transportNumber());
                    result.addMessage("info",
                            String.format("Function module locked successfully. Transport: %s",
                                    lock.transportNumber()),
                            "lock");

                    try {
                        // ========================================
                        // Step 2: Syntax Check (in stateful session)
                        // ========================================
                        log.info("Step 2/4: Running syntax check...");

                        List<SyntaxCheckMessage> syntaxMessages = syntaxCheck(
                                fmUri,          // Object URI (without /source/main)
                                fmSourceUri,    // Source URI (with /source/main)
                                newSource,
                                "inactive"      // Check against inactive version
                        );

                        // Check for errors
                        List<SyntaxCheckMessage> errors = syntaxMessages.stream()
                                .filter(SyntaxCheckMessage::isError)
                                .collect(Collectors.toList());

                        if (!errors.isEmpty()) {
                            String errorMsg = String.format(
                                    "Syntax check failed with %d error(s):", errors.size()
                            );
                            log.error(errorMsg);

                            for (SyntaxCheckMessage error : errors) {
                                log.error("  {}", error.toFormattedString());
                            }

                            result.addMessage("error", errorMsg, "syntax_check");
                            throw new RuntimeException(errorMsg);
                        }

                        log.info("✓ Syntax check passed ({} message(s))", syntaxMessages.size());
                        result.addMessage("info",
                                String.format("Syntax check passed (%d message(s))", syntaxMessages.size()),
                                "syntax_check");

                        // ========================================
                        // Step 3: Modify source code (in stateful session)
                        // ========================================
                        log.info("Step 3/4: Modifying source code ({} bytes)", newSource.length());

                        // Use transport from lock response if not explicitly provided
                        String effectiveTransport = (transport != null && !transport.isEmpty())
                                ? transport
                                : lock.transportNumber();

                        boolean modified = setObjectSource(
                                fmSourceUri,
                                newSource,
                                lock.lockHandle(),
                                effectiveTransport
                        );
                        result.setModified(modified);

                        log.info("✓ Source code modified successfully");
                        result.addMessage("info",
                                String.format("Source code updated (%d bytes)", newSource.length()),
                                "modify");

                        return result;

                    } finally {
                        // ========================================
                        // Step 4: Unlock (ALWAYS execute, even on error, in stateful session)
                        // ========================================
                        log.info("Step 4/4: Unlocking function module...");
                        statefulModificationService.unlockObject(fmUri, lock.lockHandle());
                        result.setUnlocked(true);
                        log.info("✓ Function module unlocked successfully");
                        result.addMessage("info", "Function module unlocked successfully", "unlock");
                    }
                }
        );

        // ========================================
        // Step 5: Check syntax and activate (outside stateful context)
        // ========================================
        if (workflowResult.isSuccess()) {
            log.info("Step 5/5: Checking syntax and activating function module '{}'", functionModuleName);
            try {
                var activationResult = activationService.checkAndActivate(fmSourceUri);
                workflowResult.setActivated(activationResult.success());

                if (activationResult.success()) {
                    log.info("✓ Function module activated successfully");
                    workflowResult.addMessage("info", "Function module activated successfully", "activate");
                } else {
                    log.warn("⚠️  Activation failed with {} errors", activationResult.errors().size());
                    workflowResult.addMessage("warning",
                            String.format("Activation failed: %s", activationResult.message()),
                            "activate");

                    // Add syntax errors as messages
                    for (var error : activationResult.errors()) {
                        workflowResult.addMessage("error",
                                String.format("Line %d: %s", error.line(), error.shortText()),
                                "syntax");
                    }
                }
            } catch (Exception e) {
                log.error("Error during activation", e);
                workflowResult.setActivated(false);
                workflowResult.addMessage("error",
                        "Activation failed: " + e.getMessage(),
                        "activate");
            }
        }

        // Set overall success based on workflow steps
        workflowResult.setSuccess(workflowResult.isLocked()
                && workflowResult.isModified()
                && workflowResult.isUnlocked()
                && workflowResult.isActivated());

        log.info("🎯 Function module modification workflow completed: {} (success: {}, activated: {})",
                functionModuleName, workflowResult.isSuccess(), workflowResult.isActivated());

        return workflowResult;
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

        Map<String, String> headers = new HashMap<>();

        // Accept header with ADT lock result versions
        // Supports both lock result formats (v1 and v2)
        headers.put("Accept", "text/plain");
        headers.put("Content-Type", "text/plain; charset=utf-8");
        // User-Agent matching Eclipse ADT
        headers.put("User-Agent",
            "Eclipse/4.36.0.v20250528-1830 (Java " + System.getProperty("java.version") + ") " +
            "ADT/3.50.0 (JavaMCP)");
        // Profiling header (optional but recommended)
        headers.put("X-sap-adt-profiling", "server-time");

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "PUT",
                    headers,
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
    // SYNTAX CHECK OPERATIONS
    // ============================================================================

    /**
     * Perform ABAP syntax check on source code.
     *
     * This is a REQUIRED step before modifying source code via ADT API.
     * ADT performs syntax validation before allowing PUT operations.
     *
     * ADT API Endpoint:
     * POST /sap/bc/adt/checkruns?reporters=abapCheckRun
     *
     * Flow:
     * 1. Encode source code to Base64
     * 2. Build XML body with encoded source
     * 3. Call ADT checkruns endpoint
     * 4. Parse response for syntax errors/warnings
     *
     * @param objectUri   URI of the object (without /source/main)
     *                    Example: /sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r2
     * @param sourceUri   URI of the source/include (with /source/main)
     *                    Example: /sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r2/source/main
     * @param sourceCode  Source code to validate
     * @param version     "active" or "inactive"
     * @return List of syntax check messages (errors, warnings, info)
     * @throws RuntimeException if syntax check call fails
     */
    private List<SyntaxCheckMessage> syntaxCheck(
            String objectUri,
            String sourceUri,
            String sourceCode,
            String version
    ) {
        log.debug("Running syntax check on: {}", objectUri);

        // 1. Encode source to Base64
        String base64Source = Base64.getEncoder()
                .encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));

        // 2. Build XML body
        String xmlBody = buildSyntaxCheckXml(objectUri, sourceUri, base64Source, version);

        // 3. Call ADT API
        Map<String, String> params = new HashMap<>();
        params.put("reporters", "abapCheckRun");

        // Custom headers for syntax check
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.sap.adt.checkmessages+xml");
        headers.put("Content-Type", "application/vnd.sap.adt.checkobjects+xml");
        headers.put("User-Agent", "Eclipse/4.36.0.v20250528-1830 (Java " + System.getProperty("java.version") + ") " +
            "ADT/3.50.0 (JavaMCP)");
        headers.put("X-sap-adt-profiling", "server-time");

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    "/sap/bc/adt/checkruns",
                    "POST",
                    headers,  // Pass custom headers with Accept
                    params,
                    xmlBody,
                    "application/vnd.sap.adt.checkobjects+xml"
            );

            // 4. Parse result
            if (response.statusCode() == 200) {
                List<SyntaxCheckMessage> messages = parseSyntaxCheckResult(response.text());
                log.debug("Syntax check completed. Found {} messages", messages.size());
                return messages;
            } else {
                String errorMsg = String.format(
                        "Failed to run syntax check: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error running syntax check for '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to run syntax check", e);
        }
    }

    /**
     * Build XML body for syntax check request.
     *
     * Format:
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <chkrun:checkObjectList xmlns:chkrun="http://www.sap.com/adt/checkrun"
     *                         xmlns:adtcore="http://www.sap.com/adt/core">
     *   <chkrun:checkObject adtcore:uri="{objectUri}" chkrun:version="{version}">
     *     <chkrun:artifacts>
     *       <chkrun:artifact chkrun:contentType="text/plain; charset=utf-8"
     *                        chkrun:uri="{sourceUri}">
     *         <chkrun:content>{base64Source}</chkrun:content>
     *       </chkrun:artifact>
     *     </chkrun:artifacts>
     *   </chkrun:checkObject>
     * </chkrun:checkObjectList>
     * }
     * </pre>
     *
     * @param objectUri    URI of object (without /source/main)
     * @param sourceUri    URI of source (with /source/main)
     * @param base64Source Base64-encoded source code
     * @param version      "active" or "inactive"
     * @return XML string
     */
    private String buildSyntaxCheckXml(
            String objectUri,
            String sourceUri,
            String base64Source,
            String version
    ) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<chkrun:checkObjectList xmlns:chkrun=\"http://www.sap.com/adt/checkrun\" " +
                        "xmlns:adtcore=\"http://www.sap.com/adt/core\">\n" +
                        "  <chkrun:checkObject adtcore:uri=\"%s\" chkrun:version=\"%s\">\n" +
                        "    <chkrun:artifacts>\n" +
                        "      <chkrun:artifact chkrun:contentType=\"text/plain; charset=utf-8\" " +
                        "chkrun:uri=\"%s\">\n" +
                        "        <chkrun:content>%s</chkrun:content>\n" +
                        "      </chkrun:artifact>\n" +
                        "    </chkrun:artifacts>\n" +
                        "  </chkrun:checkObject>\n" +
                        "</chkrun:checkObjectList>",
                objectUri,
                version,
                sourceUri,
                base64Source
        );
    }

    /**
     * Parse syntax check result XML.
     *
     * Response format:
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <chkrun:checkMessages xmlns:chkrun="http://www.sap.com/adt/checkrun">
     *   <chkrun:messages>
     *     <chkrun:message chkrun:type="error" chkrun:line="10" chkrun:column="5">
     *       <chkrun:text>Syntax error in line 10</chkrun:text>
     *     </chkrun:message>
     *   </chkrun:messages>
     * </chkrun:checkMessages>
     * }
     * </pre>
     *
     * @param xmlResponse XML response from checkruns endpoint
     * @return List of syntax check messages
     */
    private List<SyntaxCheckMessage> parseSyntaxCheckResult(String xmlResponse) {
        List<SyntaxCheckMessage> messages = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            // Find all message elements
            // Note: Use namespace-aware search
            NodeList messageNodes = doc.getElementsByTagNameNS(
                    "http://www.sap.com/adt/checkrun",
                    "message"
            );

            for (int i = 0; i < messageNodes.getLength(); i++) {
                Element messageElement = (Element) messageNodes.item(i);

                SyntaxCheckMessage message = new SyntaxCheckMessage();

                // Extract attributes
                message.setType(messageElement.getAttribute("chkrun:type"));

                String lineStr = messageElement.getAttribute("chkrun:line");
                message.setLine(lineStr.isEmpty() ? 0 : Integer.parseInt(lineStr));

                String columnStr = messageElement.getAttribute("chkrun:column");
                message.setColumn(columnStr.isEmpty() ? 0 : Integer.parseInt(columnStr));

                // Extract text content
                NodeList textNodes = messageElement.getElementsByTagNameNS(
                        "http://www.sap.com/adt/checkrun",
                        "text"
                );
                if (textNodes.getLength() > 0) {
                    message.setText(textNodes.item(0).getTextContent());
                }

                messages.add(message);
            }

            return messages;

        } catch (Exception e) {
            log.error("Failed to parse syntax check result XML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse syntax check result", e);
        }
    }
}

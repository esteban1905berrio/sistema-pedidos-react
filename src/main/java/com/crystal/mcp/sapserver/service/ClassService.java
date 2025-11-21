package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ClassIncludeResult;
import com.crystal.mcp.sapserver.model.ClassModifyResult;
import com.crystal.mcp.sapserver.model.ClassSourceResult;
import com.crystal.mcp.sapserver.model.DdicSourceResult;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
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
    private final StatefulModificationService statefulModificationService;
    private final ActivationService activationService;
    private final JCoDestination destination;

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

    /**
     * Normalize and validate includeType for class modification.
     *
     * Valid include types:
     * - main: Class definition (PUBLIC, PROTECTED, PRIVATE sections)
     * - implementations: Method implementations
     * - testclasses: Unit test classes
     * - macros: ABAP macros
     *
     * Normalizes common variants:
     * - "implementation" → "implementations"
     * - "includes/testclasses" → "testclasses"
     *
     * @param includeType the include type to normalize and validate
     * @return normalized includeType
     * @throws IllegalArgumentException if includeType is invalid
     */
    private String normalizeAndValidateIncludeType(String includeType) {
        // Default to "main" if null or empty
        if (includeType == null || includeType.trim().isEmpty()) {
            return "main";
        }

        // Trim and convert to lowercase for comparison
        String normalized = includeType.trim().toLowerCase();

        // Normalize common variants
        switch (normalized) {
            case "main":
            case "definition":
            case "definitions":
                return "main";

            case "implementation":
            case "implementations":
                return "implementations";

            case "testclass":
            case "testclasses":
            case "includes/testclasses":
                return "testclasses";

            case "macro":
            case "macros":
                return "macros";

            default:
                throw new IllegalArgumentException(
                    String.format("Invalid includeType '%s'. Must be one of: main, implementations, testclasses, macros",
                                  includeType)
                );
        }
    }

    /**
     * Complete workflow to modify an ABAP class source code.
     *
     * This is a workflow-based tool that orchestrates the complete ADT modification flow:
     * LOCK → MODIFY → UNLOCK
     *
     * Workflow Steps:
     * 1. LOCK: Acquire exclusive lock on class
     * 2. MODIFY: Update source code with new content
     * 3. UNLOCK: Release lock (always executed, even on failure)
     *
     * Based on Python implementation: modification_service.py
     * Reference: docs/requirements/mcp/workflow_based/pr_class_modify.md
     *
     * Supports modification of different include types:
     * - main: Class definition (PUBLIC, PROTECTED, PRIVATE sections)
     * - implementations: Method implementations
     * - testclasses: Unit test classes
     * - macros: ABAP macros
     *
     * @param className   name of the class (e.g., "ZCLFIAAC002_CARGA_ACTIVOS_FIJ")
     * @param newSource   new source code to set
     * @param includeType include type to modify (must be: main, implementations, testclasses, or macros)
     * @param transport   optional transport number (if null, uses system-assigned from LOCK)
     * @return ClassModifyResult with detailed workflow execution status
     * @throws IllegalArgumentException if includeType is invalid
     * @throws RuntimeException if modification workflow fails
     */
    public ClassModifyResult modifyClass(
            String className,
            String newSource,
            String includeType,
            String transport
    ) {
        // Validate inputs
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be empty");
        }
        if (newSource == null) {
            throw new IllegalArgumentException("Source code cannot be null");
        }

        // Normalize and validate includeType
        includeType = normalizeAndValidateIncludeType(includeType);

        log.info("🔧 Starting stateful modification workflow for class: {} (include: {})",
                className, includeType);

        // Build URIs
        String classUri = String.format("/sap/bc/adt/oo/classes/%s", className.toLowerCase());
        String sourceUri = String.format("%s/source/%s", classUri, includeType);

        // Capture includeType for lambda (must be effectively final)
        final String finalIncludeType = includeType;

        // Execute workflow in stateful context
        ClassModifyResult workflowResult = statefulModificationService.executeStatefulWorkflow(
                className,
                () -> {
                    // Initialize result object
                    ClassModifyResult result = new ClassModifyResult();
                    result.setUri(sourceUri);
                    result.setClassName(className);
                    result.setIncludeType(finalIncludeType);

                    // ========================================
                    // Step 1: Lock class (in stateful session)
                    // ========================================
                    log.info("Step 1/3: Locking class '{}'", className);

                    StatefulModificationService.LockResult lock =
                            statefulModificationService.lockObject(classUri);

                    result.setLocked(true);
                    result.setLockHandle(lock.lockHandle());
                    result.setTransportNumber(lock.transportNumber());
                    result.setTransportUser(lock.transportUser());
                    result.setTransportDescription(lock.transportDescription());

                    log.info("✓ Class locked successfully (transport: {})", lock.transportNumber());
                    result.addMessage("info",
                            String.format("Class locked successfully. Transport: %s",
                                    lock.transportNumber()),
                            "lock");

                    try {
                        // ========================================
                        // Step 2: Modify source code (in stateful session)
                        // ========================================
                        log.info("Step 2/3: Modifying source code ({} bytes)", newSource.length());

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
                        // Step 3: Unlock (ALWAYS execute, even on error, in stateful session)
                        // ========================================
                        log.info("Step 3/3: Unlocking class '{}'", className);
                        statefulModificationService.unlockObject(classUri, lock.lockHandle());
                        result.setUnlocked(true);
                        log.info("✓ Class unlocked successfully");
                        result.addMessage("info", "Class unlocked successfully", "unlock");
                    }
                }
        );

        // Set overall success based on workflow steps
        workflowResult.setSuccess(workflowResult.isLocked()
                && workflowResult.isModified()
                && workflowResult.isUnlocked());

        // ========================================
        // Step 4: Check syntax and activate (outside stateful context)
        // ========================================
        if (workflowResult.isSuccess()) {
            log.info("Step 4/4: Checking syntax and activating class '{}'", className);
            try {
                var activationResult = activationService.checkAndActivate(sourceUri);
                workflowResult.setActivated(activationResult.success());

                if (activationResult.success()) {
                    log.info("✓ Class activated successfully");
                    workflowResult.addMessage("info", "Class activated successfully", "activate");
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

        log.info("🎯 Class modification workflow completed: {} (success: {}, activated: {})",
                className, workflowResult.isSuccess(), workflowResult.isActivated());

        return workflowResult;
    }


    /**
     * Get DDIC object structure (table/structure/view) from DD03L.
     *
     * <p>This method calls the custom Function Module ZCX_GETDDICSOURCE to retrieve
     * metadata about database tables, structures, and views by querying DD03L.
     *
     * <p>FM Signature:
     * <pre>
     * IMPORTING:
     *   OBJECT_NAME TYPE TABNAME
     * EXPORTING:
     *   OBJECT_TYPE TYPE CHAR10       (TABLE/STRUCTURE/VIEW/APPEND)
     *   OBJECT_STATUS TYPE CHAR10     (ACTIVE/INACTIVE)
     *   FIELDS_JSON TYPE STRING       (Field metadata in JSON format)
     * EXCEPTIONS:
     *   OBJECT_NOT_FOUND
     *   INVALID_OBJECT_TYPE
     * </pre>
     *
     * <p>Field metadata includes:
     * - fieldname: Field name
     * - position: Position in table
     * - rollname: Data element
     * - mandatory: 'X' if mandatory
     * - checktable: Foreign key table
     * - inttype: Internal type (C, N, D, etc.)
     * - intlen: Internal length
     * - datatype: ABAP data type
     * - keyflag: 'X' if key field
     * - reffield: Reference field
     *
     * @param objectName name of table/structure/view (e.g., "MARA", "DD03L", "V_T001")
     * @return DdicSourceResult containing object metadata and field list
     * @throws RuntimeException if FM call fails or object not found
     */
    public DdicSourceResult getDdicSource(String objectName) {
        // Validate inputs
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Object name cannot be empty");
        }

        log.info("Getting DDIC source for object: {}", objectName);

        try {
            // Get function module from repository
            JCoFunction function = destination.getRepository().getFunction("ZCX_GETDDICSOURCE");

            if (function == null) {
                throw new RuntimeException("Function module ZCX_GETDDICSOURCE not found in SAP system");
            }

            // Set import parameters
            function.getImportParameterList().setValue("OBJECT_NAME", objectName.toUpperCase());

            // Execute function module
            function.execute(destination);

            // Get export parameters
            String objectType = function.getExportParameterList().getString("OBJECT_TYPE");
            String objectStatus = function.getExportParameterList().getString("OBJECT_STATUS");
            String fieldsJson = function.getExportParameterList().getString("FIELDS_JSON");

            log.debug("FM returned: type={}, status={}, fieldsJson length={}",
                    objectType, objectStatus, fieldsJson != null ? fieldsJson.length() : 0);

            // Parse fields JSON
            List<DdicSourceResult.DdicField> fields = DdicSourceResult.parseFieldsJson(fieldsJson);

            // Build result
            DdicSourceResult result = new DdicSourceResult(
                    objectName.toUpperCase(),
                    objectType,
                    objectStatus,
                    fields
            );
            result.setRawJson(fieldsJson);

            log.info("Successfully retrieved DDIC source for {} ({} fields, type: {})",
                    objectName, fields.size(), objectType);

            return result;

        } catch (com.sap.conn.jco.JCoException e) {
            // Check for ABAP exceptions
            if (e.getMessage().contains("OBJECT_NOT_FOUND")) {
                String errorMsg = String.format("Object '%s' not found in DD02L", objectName);
                log.error(errorMsg);
                throw new RuntimeException(errorMsg, e);
            } else if (e.getMessage().contains("INVALID_OBJECT_TYPE")) {
                String errorMsg = String.format("No fields found for object '%s' in DD03L", objectName);
                log.error(errorMsg);
                throw new RuntimeException(errorMsg, e);
            } else {
                log.error("JCo error calling ZCX_GETDDICSOURCE for {}: {}",
                        objectName, e.getMessage(), e);
                throw new RuntimeException("Failed to get DDIC source: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error getting DDIC source for {}: {}", objectName, e.getMessage(), e);
            throw new RuntimeException("Failed to get DDIC source", e);
        }
    }

    // ============================================================================
    // LOCK/UNLOCK OPERATIONS (Private helpers)
    // ============================================================================

    /**
     * Result object for lock operation containing lock handle and transport.
     */
    private static class LockResult {
        String lockHandle;
        String transport;

        LockResult(String lockHandle, String transport) {
            this.lockHandle = lockHandle;
            this.transport = transport;
        }
    }

    /**
     * Lock an ABAP class for editing.
     * <p>
     * Implements Step 1 of the documented workflow in pr_class_modify.md:
     * POST /sap/bc/adt/oo/classes/{className}?_action=LOCK&accessMode=MODIFY
     * <p>
     * Required Accept header:
     * application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8,
     * application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9
     * <p>
     * Response XML contains:
     * - LOCK_HANDLE: Token for subsequent operations
     * - CORRNR: Transport request number
     *
     * @param objectUri base class URI (WITHOUT /source/main)
     * @return LockResult containing lockHandle and transport
     */
    private LockResult lockObject(String objectUri) {
        Map<String, String> params = new HashMap<>();
        params.put("_action", "LOCK");
        params.put("accessMode", "MODIFY");

        // Critical: Add required Accept header for ADT lock response
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept",
                "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8, " +
                        "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9");

        log.debug("Locking object: {}", objectUri);

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "POST",
                    headers,
                    params,
                    "",
                    "application/xml"
            );

            if (response.statusCode() == 200) {
                // Parse XML response to extract LOCK_HANDLE and CORRNR
                return parseLockResponse(response.text());

            } else if (response.statusCode() == 409) {
                throw new RuntimeException("Object is already locked by another user (HTTP 409)");
            } else {
                throw new RuntimeException(String.format(
                        "Failed to lock object: HTTP %d - %s",
                        response.statusCode(), response.text()));
            }

        } catch (Exception e) {
            log.error("Error locking object '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to lock object", e);
        }
    }

    /**
     * Parse ADT lock response XML to extract LOCK_HANDLE and CORRNR.
     * <p>
     * Expected XML format:
     * <pre>{@code
     * <asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
     *   <asx:values>
     *     <DATA>
     *       <LOCK_HANDLE>AiEMLCBBUzRVU0VSICAgICAgI...</LOCK_HANDLE>
     *       <CORRNR>CADK911088</CORRNR>
     *       <CORRUSER>USERNAME</CORRUSER>
     *     </DATA>
     *   </asx:values>
     * </asx:abap>
     * }</pre>
     *
     * @param xmlText XML response from LOCK operation
     * @return LockResult containing parsed values
     */
    private LockResult parseLockResponse(String xmlText) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(
                    new java.io.ByteArrayInputStream(xmlText.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );

            // Extract LOCK_HANDLE
            org.w3c.dom.NodeList lockHandleNodes = doc.getElementsByTagName("LOCK_HANDLE");
            String lockHandle = null;
            if (lockHandleNodes.getLength() > 0) {
                lockHandle = lockHandleNodes.item(0).getTextContent().trim();
            }

            // Extract CORRNR (transport)
            org.w3c.dom.NodeList corrNrNodes = doc.getElementsByTagName("CORRNR");
            String transport = null;
            if (corrNrNodes.getLength() > 0) {
                transport = corrNrNodes.item(0).getTextContent().trim();
            }

            if (lockHandle == null || lockHandle.isEmpty()) {
                throw new RuntimeException("Failed to extract LOCK_HANDLE from lock response");
            }

            log.debug("Parsed lock response: handle={}, transport={}", lockHandle, transport);

            return new LockResult(lockHandle, transport);

        } catch (Exception e) {
            log.error("Error parsing lock response XML: {}", e.getMessage());
            throw new RuntimeException("Failed to parse lock response", e);
        }
    }

    /**
     * Get object structure (Step 2 of workflow).
     * <p>
     * GET /sap/bc/adt/oo/classes/{className}/objectstructure?version=active&withShortDescriptions=true
     * <p>
     * This validates the object state before modification.
     *
     * @param objectUri base class URI
     */
    private void getObjectStructure(String objectUri) {
        String structureUri = objectUri + "/objectstructure";

        Map<String, String> params = new HashMap<>();
        params.put("version", "active");
        params.put("withShortDescriptions", "true");

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    structureUri,
                    "GET",
                    null,
                    params,
                    "",
                    "application/xml"
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format(
                        "Failed to get object structure: HTTP %d - %s",
                        response.statusCode(), response.text()));
            }

        } catch (Exception e) {
            log.error("Error getting object structure for '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to get object structure", e);
        }
    }

    /**
     * Get inactive version (Step 4 of workflow).
     * <p>
     * GET /sap/bc/adt/oo/classes/{className}?version=inactive
     * <p>
     * This verifies that the modification was saved as inactive version.
     *
     * @param objectUri base class URI
     */
    private void getInactiveVersion(String objectUri) {
        Map<String, String> params = new HashMap<>();
        params.put("version", "inactive");

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "GET",
                    null,
                    params,
                    "",
                    "application/xml"
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format(
                        "Failed to get inactive version: HTTP %d - %s",
                        response.statusCode(), response.text()));
            }

        } catch (Exception e) {
            log.error("Error getting inactive version for '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to get inactive version", e);
        }
    }

    /**
     * Unlock an ABAP class after editing (Step 5 of workflow).
     * <p>
     * POST /sap/bc/adt/oo/classes/{className}?_action=UNLOCK&lockHandle={lockHandle}
     *
     * @param objectUri  base class URI (WITHOUT /source/main)
     * @param lockHandle lock handle from LOCK operation
     * @return true if unlock successful
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
                    "application/xml"
            );

            boolean success = (response.statusCode() == 200 || response.statusCode() == 204);
            if (success) {
                log.debug("✓ Unlock successful");
            } else {
                log.warn("✗ Unlock failed: HTTP {}", response.statusCode());
            }

            return success;

        } catch (Exception e) {
            log.error("Error unlocking object '{}': {}", objectUri, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Set source code for an ABAP class (Step 3 of workflow).
     * <p>
     * PUT /sap/bc/adt/oo/classes/{className}/source/{includeType}?lockHandle={lockHandle}&corrNr={transport}
     * <p>
     * Content-Type: text/plain; charset=utf-8
     *
     * @param objectUri  source URI (WITH /source/{includeType})
     * @param sourceCode new source code
     * @param lockHandle lock handle from LOCK operation
     * @param transport  transport number
     * @return true if successful
     */
    private boolean setObjectSource(String objectUri, String sourceCode, String lockHandle, String transport) {
        Map<String, String> params = new HashMap<>();
        params.put("lockHandle", lockHandle);
        if (transport != null && !transport.isEmpty()) {
            params.put("corrNr", transport);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/plain");
        headers.put("Content-Type", "text/plain; charset=utf-8");

        log.info("💾 SET SOURCE | URI: {} | Lock Handle: {} | Transport: {} | Size: {} bytes | Stateful: {}",
                objectUri,
                lockHandle.substring(0, Math.min(16, lockHandle.length())) + "...",
                transport,
                sourceCode.length(),
                rfcAdapter.isStatefulContextActive());

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "PUT",
                    headers,
                    params,
                    sourceCode,
                    "text/plain; charset=utf-8"
            );

            boolean success = (response.statusCode() == 200 || response.statusCode() == 204);
            if (success) {
                log.debug("✓ Source code set successfully");
            } else {
                String errorMsg = String.format(
                        "Failed to set source code: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            return success;

        } catch (Exception e) {
            log.error("Error setting source code for '{}': {}", objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to set source code", e);
        }
    }
}

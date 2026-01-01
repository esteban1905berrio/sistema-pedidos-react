package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.CreationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for creating and deleting ABAP objects.
 * <p>
 * This service provides workflow-based operations for creating:
 * - Function Groups (FUGR/F)
 * - Function Modules (FUGR/FF)
 * - Classes (CLAS/OC)
 * - Interfaces (INTF/OI)
 * <p>
 * Also provides deletion functionality for any ABAP object.
 * <p>
 * Based on Python implementation: creation_service.py
 * <p>
 * Workflow Patterns:
 * - Function Group: VALIDATE → REGISTER → CREATE
 * - Function Module: VALIDATE → REGISTER → TRANSPORT_CHECK → CREATE
 * - Class: VALIDATE → CREATE (direct)
 * - Interface: VALIDATE → CREATE (direct)
 * - Delete: DELETE (with transport)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreationService {

    private final RfcAdapter rfcAdapter;

    // Naming patterns for validation
    private static final Pattern FUNCTION_GROUP_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,25}$");
    private static final Pattern FUNCTION_MODULE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,29}$");
    private static final Pattern CLASS_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,29}$");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,29}$");

    /**
     * Create a new function group.
     * <p>
     * Workflow:
     * 1. VALIDATE: Check function group name format
     * 2. REGISTER: Register in ABAP Workbench repository
     * 3. CREATE: Create function group with metadata
     * <p>
     * Based on Python: creation_service.py::create_function_group()
     *
     * @param functionGroupName name of the function group (e.g., "ZTEST_FG")
     * @param description       description (max 60 chars)
     * @param packageName       package (e.g., "$TMP", "ZPACKAGE")
     * @param transport         optional transport number (null for local objects)
     * @return CreationResult with success status and details
     */
    public CreationResult createFunctionGroup(
            String functionGroupName,
            String description,
            String packageName,
            String transport) {
        log.info("🔧 Creating function group: {}", functionGroupName);

        CreationResult result = new CreationResult();
        result.setName(functionGroupName);
        result.setObjectType("FUGR/F");
        result.setPackage_(packageName);
        result.setTransport(transport);

        try {
            // Step 1: Validate function group name
            log.debug("  Step 1/3: Validating function group name...");
            if (!FUNCTION_GROUP_PATTERN.matcher(functionGroupName).matches()) {
                throw new IllegalArgumentException(
                        "Invalid function group name format. Must start with letter, max 26 chars, only A-Z0-9_");
            }

            // Step 2: Register in repository (POST to functions/groups)
            log.debug("  Step 2/3: Registering function group in repository...");
            String registerUri = "/sap/bc/adt/functions/groups";
            String registerXml = buildFunctionGroupXml(functionGroupName, description, packageName, transport);
            log.info("XML being sent:\n{}", registerXml);

            RfcAdapter.RfcResponse registerResponse = rfcAdapter.request(
                    registerUri,
                    "POST",
                    null,
                    null,
                    registerXml,
                    "application/vnd.sap.adt.functions.groups.v2+xml");

            int registerStatus = registerResponse.statusCode();
            if (registerStatus != 200 && registerStatus != 201) {
                throw new RuntimeException("Failed to register function group. Status: " + registerStatus + ", Body: "
                        + registerResponse.text());
            }

            log.debug("  ✓ Function group registered successfully");

            // Step 3: Create function group (object URI)
            String objectUri = "/sap/bc/adt/functions/groups/" + functionGroupName.toLowerCase();
            result.setUri(objectUri);
            result.setSuccess(true);
            result.setMessage("Function group created successfully");

            log.info("✅ Function group created: {}", functionGroupName);

        } catch (Exception e) {
            log.error("❌ Failed to create function group: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Create a new function module in an existing function group.
     * <p>
     * Workflow (based on Eclipse ADT flow documented in pr_fm_manager.md):
     * 1. VALIDATE: Check function module name format (local)
     * 2. VALIDATE: Call ADT validation endpoint
     * 3. TRANSPORT_CHECK: First transport check (get locks info)
     * 4. SSCR_REGISTRATION: Register object/developer in SAP Support Portal
     * 5. TRANSPORT_CHECK: Second transport check (confirm)
     * 6. CREATE: Create function module with corrNr parameter
     * 7. GET_SOURCE: Retrieve initial source (optional)
     * 8. NODE_STRUCTURE: Update repository node structure (optional)
     * <p>
     * Based on Python: creation_service.py::create_function_module()
     * Enhanced with Eclipse ADT flow from pr_fm_manager.md
     *
     * @param functionModuleName name of the function module (e.g., "Z_TEST_FM")
     * @param functionGroupName  parent function group name
     * @param description        description (max 60 chars)
     * @param transport          optional transport number (null for local)
     * @param processingType     optional processing type: null/empty for normal FM,
     *                           "rfc" for RFC-enabled FM
     * @return CreationResult with success status and details
     */
    public CreationResult createFunctionModule(
            String functionModuleName,
            String functionGroupName,
            String description,
            String transport,
            String processingType) {
        log.info("🔧 Creating function module: {} in group {}", functionModuleName, functionGroupName);

        CreationResult result = new CreationResult();
        result.setName(functionModuleName);
        result.setObjectType("FUGR/FF");
        result.setParentName(functionGroupName);
        result.setTransport(transport);

        try {
            // Step 1: Local validation of function module name
            log.debug("  Step 1/8: Validating function module name (local)...");
            if (!FUNCTION_MODULE_PATTERN.matcher(functionModuleName).matches()) {
                throw new IllegalArgumentException(
                        "Invalid function module name format. Must start with letter, max 30 chars, only A-Z0-9_");
            }

            // Step 2: ADT validation endpoint
            log.debug("  Step 2/8: Validating via ADT endpoint...");
            String validationUri = "/sap/bc/adt/functions/validation";
            Map<String, String> validationParams = Map.of(
                    "objtype", "FUGR/FF",
                    "objname", functionModuleName,
                    "fugrname", functionGroupName,
                    "description", description);

            RfcAdapter.RfcResponse validationResponse = rfcAdapter.request(
                    validationUri,
                    "POST",
                    Map.of("Accept", "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.StatusMessage"),
                    validationParams,
                    "",
                    "application/xml");

            int validationStatus = validationResponse.statusCode();
            if (validationStatus != 200 && validationStatus != 201) {
                log.warn("  ⚠ Validation returned non-200 status: {} - continuing anyway", validationStatus);
            } else {
                log.debug("  ✓ ADT validation successful");
            }

            // Step 3: First transport check (get lock information)
            log.debug("  Step 3/8: First transport check (getting lock info)...");
            String fmUri = "/sap/bc/adt/functions/groups/" + functionGroupName.toLowerCase() +
                    "/fmodules/" + functionModuleName.toLowerCase();

            String transportCheckResult = performTransportCheck(fmUri, "I", "ZFI");
            log.debug("  ✓ First transport check completed");
            log.debug("  Transport check result: {}",
                    transportCheckResult.substring(0, Math.min(200, transportCheckResult.length())));

            // Step 4: SSCR Registration (optional, non-blocking)
            log.debug("  Step 4/8: SSCR registration check...");
            try {
                String sscrUri = "/sap/bc/adt/sscr/registration/objects";
                Map<String, String> sscrParams = Map.of("uri", fmUri);

                RfcAdapter.RfcResponse sscrResponse = rfcAdapter.request(
                        sscrUri,
                        "GET",
                        Map.of("Accept", "application/vnd.sap.adt.registration+xml"),
                        sscrParams,
                        "",
                        "application/xml");

                log.debug("  ✓ SSCR registration checked (status: {})", sscrResponse.statusCode());
            } catch (Exception e) {
                log.warn("  ⚠ SSCR registration check failed (non-blocking): {}", e.getMessage());
            }

            // Step 5: Second transport check (confirmation)
            log.debug("  Step 5/8: Second transport check (confirmation)...");
            String transportCheckResult2 = performTransportCheck(fmUri, "I", "ZFI");
            log.debug("  ✓ Second transport check completed");

            // Step 6: Create function module with corrNr parameter
            log.debug("  Step 6/8: Creating function module with transport...");
            String registerUri = "/sap/bc/adt/functions/groups/" + functionGroupName.toLowerCase() + "/fmodules";
            String registerXml = buildFunctionModuleXmlV2(functionModuleName, description, functionGroupName,
                    processingType);

            // Add corrNr parameter if transport is provided
            Map<String, String> createParams = null;
            if (transport != null && !transport.isEmpty()) {
                createParams = Map.of("corrNr", transport);
            }

            RfcAdapter.RfcResponse registerResponse = rfcAdapter.request(
                    registerUri,
                    "POST",
                    Map.of("Content-Type", "application/vnd.sap.adt.functions.fmodules.v2+xml"),
                    createParams,
                    registerXml,
                    "application/vnd.sap.adt.functions.fmodules.v2+xml");

            int registerStatus = registerResponse.statusCode();
            if (registerStatus != 200 && registerStatus != 201) {
                throw new RuntimeException("Failed to create function module. Status: " + registerStatus + ", Body: "
                        + registerResponse.text());
            }

            log.debug("  ✓ Function module created successfully");

            // Step 7: Get source (optional verification)
            log.debug("  Step 7/8: Retrieving initial source...");
            try {
                String sourceUri = fmUri + "/source/main";
                RfcAdapter.RfcResponse sourceResponse = rfcAdapter.request(
                        sourceUri,
                        "GET",
                        null,
                        null,
                        "",
                        "text/plain");
                log.debug("  ✓ Source retrieved (status: {})", sourceResponse.statusCode());
            } catch (Exception e) {
                log.warn("  ⚠ Source retrieval failed (non-blocking): {}", e.getMessage());
            }

            // Step 8: Update node structure (optional)
            log.debug("  Step 8/8: Updating repository node structure...");
            try {
                updateNodeStructure(functionGroupName);
                log.debug("  ✓ Node structure updated");
            } catch (Exception e) {
                log.warn("  ⚠ Node structure update failed (non-blocking): {}", e.getMessage());
            }

            // Set URI and complete
            result.setUri(fmUri);
            result.setSuccess(true);
            result.setMessage("Function module created successfully");

            log.info("✅ Function module created: {}", functionModuleName);

        } catch (Exception e) {
            log.error("❌ Failed to create function module: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Create a new ABAP class.
     * <p>
     * Workflow:
     * 1. VALIDATE: Check class name format
     * 2. CREATE: Direct creation with XML body
     * <p>
     * Based on Python: creation_service.py::create_class()
     *
     * @param className   name of the class (e.g., "ZCL_TEST")
     * @param description description (max 60 chars)
     * @param packageName package (e.g., "$TMP", "ZPACKAGE")
     * @param transport   optional transport number (null for local)
     * @param superclass  optional superclass name
     * @return CreationResult with success status and details
     */
    public CreationResult createClass(
            String className,
            String description,
            String packageName,
            String transport,
            String superclass) {
        log.info("🔧 Creating class: {}", className);

        CreationResult result = new CreationResult();
        result.setName(className);
        result.setObjectType("CLAS/OC");
        result.setPackage_(packageName);
        result.setTransport(transport);

        try {
            // Step 1: Validate class name
            log.debug("  Step 1/2: Validating class name...");
            if (!CLASS_PATTERN.matcher(className).matches()) {
                throw new IllegalArgumentException(
                        "Invalid class name format. Must start with letter, max 30 chars, only A-Z0-9_");
            }

            // Step 2: Create class
            log.debug("  Step 2/2: Creating class...");
            String classUri = "/sap/bc/adt/oo/classes";
            String classXml = buildClassXml(className, description, packageName, transport, superclass);

            RfcAdapter.RfcResponse createResponse = rfcAdapter.request(
                    classUri,
                    "POST",
                    null,
                    null,
                    classXml,
                    "application/vnd.sap.adt.oo.classes.v4+xml");

            int createStatus = createResponse.statusCode();
            if (createStatus != 200 && createStatus != 201) {
                throw new RuntimeException(
                        "Failed to create class. Status: " + createStatus + ", Body: " + createResponse.text());
            }

            log.debug("  ✓ Class created successfully");

            String objectUri = "/sap/bc/adt/oo/classes/" + className.toLowerCase();
            result.setUri(objectUri);
            result.setSuccess(true);
            result.setMessage("Class created successfully");

            log.info("✅ Class created: {}", className);

        } catch (Exception e) {
            log.error("❌ Failed to create class: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Create a new ABAP interface.
     * <p>
     * Workflow:
     * 1. VALIDATE: Check interface name format
     * 2. CREATE: Direct creation with XML body
     * <p>
     * Based on Python: creation_service.py::create_interface()
     *
     * @param interfaceName name of the interface (e.g., "ZIF_TEST")
     * @param description   description (max 60 chars)
     * @param packageName   package (e.g., "$TMP", "ZPACKAGE")
     * @param transport     optional transport number (null for local)
     * @return CreationResult with success status and details
     */
    public CreationResult createInterface(
            String interfaceName,
            String description,
            String packageName,
            String transport) {
        log.info("🔧 Creating interface: {}", interfaceName);

        CreationResult result = new CreationResult();
        result.setName(interfaceName);
        result.setObjectType("INTF/OI");
        result.setPackage_(packageName);
        result.setTransport(transport);

        try {
            // Step 1: Validate interface name
            log.debug("  Step 1/2: Validating interface name...");
            if (!INTERFACE_PATTERN.matcher(interfaceName).matches()) {
                throw new IllegalArgumentException(
                        "Invalid interface name format. Must start with letter, max 30 chars, only A-Z0-9_");
            }

            // Step 2: Create interface
            log.debug("  Step 2/2: Creating interface...");
            String interfaceUri = "/sap/bc/adt/oo/interfaces";
            String interfaceXml = buildInterfaceXml(interfaceName, description, packageName, transport);

            RfcAdapter.RfcResponse createResponse = rfcAdapter.request(
                    interfaceUri,
                    "POST",
                    null,
                    null,
                    interfaceXml,
                    "application/vnd.sap.adt.oo.interfaces.v4+xml");

            int createStatus = createResponse.statusCode();
            if (createStatus != 200 && createStatus != 201) {
                throw new RuntimeException(
                        "Failed to create interface. Status: " + createStatus + ", Body: " + createResponse.text());
            }

            log.debug("  ✓ Interface created successfully");

            String objectUri = "/sap/bc/adt/oo/interfaces/" + interfaceName.toLowerCase();
            result.setUri(objectUri);
            result.setSuccess(true);
            result.setMessage("Interface created successfully");

            log.info("✅ Interface created: {}", interfaceName);

        } catch (Exception e) {
            log.error("❌ Failed to create interface: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete an ABAP object.
     * <p>
     * Supports any object type that can be deleted via ADT API:
     * - Classes (CLAS)
     * - Interfaces (INTF)
     * - Function Groups (FUGR)
     * - Function Modules (FMOD)
     * - Programs (PROG)
     * <p>
     * Based on Python: creation_service.py::delete_object()
     *
     * @param objectUri ADT URI of the object to delete
     * @param transport transport number (required for non-local objects)
     * @return CreationResult with success status and details
     */
    public CreationResult deleteObject(String objectUri, String transport) {
        log.info("🗑️  Deleting object: {}", objectUri);

        CreationResult result = new CreationResult();
        result.setUri(objectUri);
        result.setTransport(transport);

        try {
            // Execute DELETE request
            log.debug("  Executing DELETE request...");

            // Build params map for transport
            Map<String, String> params = null;
            if (transport != null && !transport.isEmpty()) {
                params = Map.of("corrNr", transport);
            }

            RfcAdapter.RfcResponse deleteResponse = rfcAdapter.request(
                    objectUri,
                    "DELETE",
                    null,
                    params,
                    "",
                    "application/xml");

            int deleteStatus = deleteResponse.statusCode();
            if (deleteStatus != 200 && deleteStatus != 204) {
                throw new RuntimeException(
                        "Failed to delete object. Status: " + deleteStatus + ", Body: " + deleteResponse.text());
            }

            log.debug("  ✓ Object deleted successfully");

            result.setSuccess(true);
            result.setMessage("Object deleted successfully");

            log.info("✅ Object deleted: {}", objectUri);

        } catch (Exception e) {
            log.error("❌ Failed to delete object: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Create a new ABAP program.
     * <p>
     * Workflow:
     * 1. VALIDATE: Call ADT validation endpoint
     * 2. SSCR: Registration check (optional)
     * 3. CREATE: Create program with XML body
     *
     * @param programName name of the program (e.g., "ZREP_TEST")
     * @param description description of the program
     * @param packageName package name
     * @param transport   transport request (optional)
     * @return CreationResult with success status and details
     */
    public CreationResult createProgram(
            String programName,
            String description,
            String packageName,
            String transport) {
        log.info("🔧 Creating program: {}", programName);

        CreationResult result = new CreationResult();
        result.setName(programName);
        result.setObjectType("PROG/P");
        result.setPackage_(packageName);
        result.setTransport(transport);

        try {
            // Step 1: ADT validation
            log.debug("  Step 1/3: Validating program...");
            String validationUri = "/sap/bc/adt/programs/validation";
            Map<String, String> validationParams = Map.of(
                    "objname", programName,
                    "packagename", packageName,
                    "description", description,
                    "objtype", "PROG/P");

            RfcAdapter.RfcResponse validationResponse = rfcAdapter.request(
                    validationUri,
                    "POST",
                    Map.of("Accept",
                            "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.programs.validation"),
                    validationParams,
                    "",
                    "application/xml");

            int validationStatus = validationResponse.statusCode();
            if (validationStatus != 200 && validationStatus != 201) {
                throw new RuntimeException(
                        "Validation failed. Status: " + validationStatus + ", Body: " + validationResponse.text());
            }

            // Step 2: SSCR Check (optional, analogous to FM creation)
            try {
                String checkUri = "/sap/bc/adt/sscr/registration/objects";
                String checkParamsUri = "/sap/bc/adt/programs/programs/" + programName.toLowerCase();
                rfcAdapter.request(
                        checkUri,
                        "GET",
                        Map.of("Accept", "application/vnd.sap.adt.registration+xml"),
                        Map.of("uri", checkParamsUri),
                        "",
                        "application/xml");
            } catch (Exception e) {
                log.warn("  ⚠ SSCR registration check failed (non-blocking): {}", e.getMessage());
            }

            // Step 3: Create program
            log.debug("  Step 3/3: Creating program...");
            String createUri = "/sap/bc/adt/programs/programs";
            String createXml = buildProgramXml(programName, description, packageName, transport);

            // Add corrNr parameter if transport is provided
            Map<String, String> createParams = null;
            if (transport != null && !transport.isEmpty()) {
                createParams = Map.of("corrNr", transport);
            }

            RfcAdapter.RfcResponse createResponse = rfcAdapter.request(
                    createUri,
                    "POST",
                    Map.of("Content-Type", "application/vnd.sap.adt.programs.programs.v2+xml"),
                    createParams,
                    createXml,
                    "application/vnd.sap.adt.programs.programs.v2+xml");

            int createStatus = createResponse.statusCode();
            if (createStatus != 200 && createStatus != 201) {
                throw new RuntimeException(
                        "Failed to create program. Status: " + createStatus + ", Body: " + createResponse.text());
            }

            String objectUri = "/sap/bc/adt/programs/programs/" + programName.toLowerCase();
            result.setUri(objectUri);
            result.setSuccess(true);
            result.setMessage("Program created successfully");

            log.info("✅ Program created: {}", programName);

        } catch (Exception e) {
            log.error("❌ Failed to create program: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Create a new ABAP include.
     * <p>
     * Workflow:
     * 1. VALIDATE: Call ADT validation endpoint
     * 2. CREATE: Create include with XML body
     *
     * @param includeName name of the include (e.g., "ZREP_TEST_TOP")
     * @param description description of the include
     * @param packageName package name
     * @param transport   transport request (optional)
     * @return CreationResult with success status and details
     */
    public CreationResult createInclude(
            String includeName,
            String description,
            String packageName,
            String transport) {
        log.info("🔧 Creating include: {}", includeName);

        CreationResult result = new CreationResult();
        result.setName(includeName);
        result.setObjectType("PROG/I");
        result.setPackage_(packageName);
        result.setTransport(transport);

        try {
            // Step 1: ADT validation
            log.debug("  Step 1/2: Validating include...");
            String validationUri = "/sap/bc/adt/includes/validation";
            Map<String, String> validationParams = Map.of(
                    "objname", includeName,
                    "packagename", packageName,
                    "description", description,
                    "objtype", "PROG/I");

            RfcAdapter.RfcResponse validationResponse = rfcAdapter.request(
                    validationUri,
                    "POST",
                    Map.of("Accept",
                            "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.programs.validation"),
                    validationParams,
                    "",
                    "application/xml");

            int validationStatus = validationResponse.statusCode();
            if (validationStatus != 200 && validationStatus != 201) {
                throw new RuntimeException(
                        "Validation failed. Status: " + validationStatus + ", Body: " + validationResponse.text());
            }

            // Step 2: Create include
            log.debug("  Step 2/2: Creating include...");
            String createUri = "/sap/bc/adt/programs/includes";
            String createXml = buildIncludeXml(includeName, description, packageName, transport);

            // Add corrNr parameter if transport is provided
            Map<String, String> createParams = null;
            if (transport != null && !transport.isEmpty()) {
                createParams = Map.of("corrNr", transport);
            }

            RfcAdapter.RfcResponse createResponse = rfcAdapter.request(
                    createUri,
                    "POST",
                    Map.of("Content-Type", "application/vnd.sap.adt.programs.includes.v2+xml"),
                    createParams,
                    createXml,
                    "application/vnd.sap.adt.programs.includes.v2+xml");

            int createStatus = createResponse.statusCode();
            if (createStatus != 200 && createStatus != 201) {
                throw new RuntimeException(
                        "Failed to create include. Status: " + createStatus + ", Body: " + createResponse.text());
            }

            String objectUri = "/sap/bc/adt/programs/includes/" + includeName.toLowerCase();
            result.setUri(objectUri);
            result.setSuccess(true);
            result.setMessage("Include created successfully");

            log.info("✅ Include created: {}", includeName);

        } catch (Exception e) {
            log.error("❌ Failed to create include: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
        }

        return result;
    }

    // ========================================
    // XML Builder Helper Methods
    // ========================================

    /**
     * Build XML body for function group creation.
     * Format matches Eclipse ADT exactly.
     */
    private String buildFunctionGroupXml(String name, String description, String packageName, String transport) {
        // Eclipse ADT format - all in one line after declaration, with language
        // attributes
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<group:abapFunctionGroup xmlns:group=\"http://www.sap.com/adt/functions/groups\" " +
                "xmlns:adtcore=\"http://www.sap.com/adt/core\" " +
                "adtcore:description=\"%s\" " +
                "adtcore:language=\"EN\" " +
                "adtcore:name=\"%s\" " +
                "adtcore:type=\"FUGR/F\" " +
                "adtcore:masterLanguage=\"EN\">" +
                "<adtcore:packageRef adtcore:name=\"%s\"/>" +
                "</group:abapFunctionGroup>").formatted(description, name, packageName);
    }

    /**
     * Build XML body for function module creation (V2 format with container
     * reference).
     * This matches the Eclipse ADT format from pr_fm_manager.md line 218-222.
     *
     * @param name              Function module name
     * @param description       Description
     * @param functionGroupName Parent function group
     * @param processingType    Processing type: null/empty for normal FM, "rfc" for
     *                          RFC-enabled FM
     */
    private String buildFunctionModuleXmlV2(String name, String description, String functionGroupName,
            String processingType) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element
            Element root = doc.createElement("fmodule:abapFunctionModule");
            root.setAttribute("xmlns:fmodule", "http://www.sap.com/adt/functions/fmodules");
            root.setAttribute("xmlns:adtcore", "http://www.sap.com/adt/core");
            root.setAttribute("adtcore:description", description);
            root.setAttribute("adtcore:name", name);
            root.setAttribute("adtcore:type", "FUGR/FF");

            // Add RFC processing type ONLY when explicitly requested
            if ("rfc".equalsIgnoreCase(processingType)) {
                root.setAttribute("fmodule:processingType", "rfc");
                log.debug("  → RFC-enabled function module requested");
            }

            doc.appendChild(root);

            // Container reference (function group)
            Element containerRef = doc.createElement("adtcore:containerRef");
            containerRef.setAttribute("adtcore:name", functionGroupName);
            containerRef.setAttribute("adtcore:type", "FUGR/F");
            containerRef.setAttribute("adtcore:uri", "/sap/bc/adt/functions/groups/" + functionGroupName.toLowerCase());
            root.appendChild(containerRef);

            return xmlToString(doc);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build function module XML V2", e);
        }
    }

    /**
     * Build XML body for function module creation (legacy format - kept for
     * compatibility).
     */
    @Deprecated
    private String buildFunctionModuleXml(String name, String description, String transport) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element
            Element root = doc.createElement("fmodule:abapFunctionModule");
            root.setAttribute("xmlns:fmodule", "http://www.sap.com/adt/functions/fmodules");
            root.setAttribute("xmlns:adtcore", "http://www.sap.com/adt/core");
            root.setAttribute("adtcore:type", "FUGR/FF");
            root.setAttribute("adtcore:description", description);
            root.setAttribute("adtcore:name", name);
            root.setAttribute("fmodule:rfc", "disabled");
            root.setAttribute("fmodule:release", "notReleased");
            doc.appendChild(root);

            return xmlToString(doc);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build function module XML", e);
        }
    }

    /**
     * Build XML body for class creation.
     */
    private String buildClassXml(String name, String description, String packageName, String transport,
            String superclass) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element
            Element root = doc.createElement("class:abapClass");
            root.setAttribute("xmlns:class", "http://www.sap.com/adt/oo/classes");
            root.setAttribute("xmlns:adtcore", "http://www.sap.com/adt/core");
            root.setAttribute("adtcore:type", "CLAS/OC");
            root.setAttribute("adtcore:description", description);
            root.setAttribute("adtcore:name", name);
            root.setAttribute("adtcore:masterLanguage", "EN");
            root.setAttribute("class:final", "false");
            root.setAttribute("class:abstract", "false");
            root.setAttribute("class:visibility", "public");
            root.setAttribute("class:category", "general");
            doc.appendChild(root);

            // Package reference
            Element packageRef = doc.createElement("adtcore:packageRef");
            packageRef.setAttribute("adtcore:name", packageName);
            root.appendChild(packageRef);

            // Superclass reference (optional)
            if (superclass != null && !superclass.isEmpty()) {
                Element superclassRef = doc.createElement("class:superClassRef");
                superclassRef.setAttribute("adtcore:name", superclass);
                root.appendChild(superclassRef);
            }

            return xmlToString(doc);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build class XML", e);
        }
    }

    /**
     * Build XML body for interface creation.
     */
    private String buildInterfaceXml(String name, String description, String packageName, String transport) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element
            Element root = doc.createElement("intf:abapInterface");
            root.setAttribute("xmlns:intf", "http://www.sap.com/adt/oo/interfaces");
            root.setAttribute("xmlns:adtcore", "http://www.sap.com/adt/core");
            root.setAttribute("adtcore:type", "INTF/OI");
            root.setAttribute("adtcore:description", description);
            root.setAttribute("adtcore:name", name);
            root.setAttribute("adtcore:masterLanguage", "EN");
            root.setAttribute("intf:category", "general");
            root.setAttribute("intf:visibility", "public");
            doc.appendChild(root);

            // Package reference
            Element packageRef = doc.createElement("adtcore:packageRef");
            packageRef.setAttribute("adtcore:name", packageName);
            root.appendChild(packageRef);

            return xmlToString(doc);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build interface XML", e);
        }
    }

    /**
     * Convert DOM Document to XML string without declaration.
     */
    private String xmlToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Convert DOM Document to XML string with XML declaration.
     * Required by SAP ADT API for certain endpoints.
     */
    private String xmlToStringWithDeclaration(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Perform transport check for an object.
     * This corresponds to the /sap/bc/adt/cts/transportchecks endpoint.
     * <p>
     * Based on Eclipse ADT flow from pr_fm_manager.md lines 24-105.
     *
     * @param objectUri ADT URI of the object
     * @param operation operation type ("I" for insert, "M" for modify)
     * @param devclass  development class/package
     * @return XML response as string
     */
    private String performTransportCheck(String objectUri, String operation, String devclass) {
        try {
            // Build transport check XML body
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element: asx:abap
            Element root = doc.createElement("asx:abap");
            root.setAttribute("xmlns:asx", "http://www.sap.com/abapxml");
            root.setAttribute("version", "1.0");
            doc.appendChild(root);

            // asx:values
            Element values = doc.createElement("asx:values");
            root.appendChild(values);

            // DATA element
            Element data = doc.createElement("DATA");
            values.appendChild(data);

            // Child elements
            addTextElement(doc, data, "PGMID", "");
            addTextElement(doc, data, "OBJECT", "");
            addTextElement(doc, data, "OBJECTNAME", "");
            addTextElement(doc, data, "DEVCLASS", devclass);
            addTextElement(doc, data, "SUPER_PACKAGE", "");
            addTextElement(doc, data, "OPERATION", operation);
            addTextElement(doc, data, "URI", objectUri);

            String requestXml = xmlToString(doc);

            // Execute transport check
            String transportCheckUri = "/sap/bc/adt/cts/transportchecks";
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    transportCheckUri,
                    "POST",
                    Map.of(
                            "Accept",
                            "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.transport.service.checkData",
                            "Content-Type",
                            "application/vnd.sap.as+xml; charset=UTF-8; dataname=com.sap.adt.transport.service.checkData"),
                    null,
                    requestXml,
                    "application/vnd.sap.as+xml");

            if (response.statusCode() != 200) {
                log.warn("Transport check returned non-200 status: {}", response.statusCode());
            }

            return response.text();

        } catch (Exception e) {
            throw new RuntimeException("Failed to perform transport check: " + e.getMessage(), e);
        }
    }

    /**
     * Update repository node structure after creating an object.
     * This corresponds to POST /sap/bc/adt/repository/nodestructure.
     * <p>
     * Based on Eclipse ADT flow from pr_fm_manager.md lines 229-289.
     *
     * @param functionGroupName parent function group name
     */
    private void updateNodeStructure(String functionGroupName) {
        try {
            // Build node structure request XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element: asx:abap
            Element root = doc.createElement("asx:abap");
            root.setAttribute("xmlns:asx", "http://www.sap.com/abapxml");
            root.setAttribute("version", "1.0");
            doc.appendChild(root);

            // asx:values
            Element values = doc.createElement("asx:values");
            root.appendChild(values);

            // DATA element
            Element data = doc.createElement("DATA");
            values.appendChild(data);

            // TV_NODEKEY
            addTextElement(doc, data, "TV_NODEKEY", "000000");

            String requestXml = xmlToString(doc);

            // Execute node structure update
            String nodeStructureUri = "/sap/bc/adt/repository/nodestructure";
            Map<String, String> params = Map.of(
                    "parent_name", functionGroupName,
                    "parent_tech_name", "SAPL" + functionGroupName,
                    "parent_type", "FUGR/F",
                    "withShortDescriptions", "true");

            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    nodeStructureUri,
                    "POST",
                    Map.of(
                            "Accept",
                            "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.RepositoryObjectTreeContent",
                            "Content-Type", "application/vnd.sap.as+xml; charset=UTF-8; dataname=null"),
                    params,
                    requestXml,
                    "application/vnd.sap.as+xml");

            if (response.statusCode() != 200) {
                log.warn("Node structure update returned non-200 status: {}", response.statusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to update node structure: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to add a text element to a parent element.
     */
    private void addTextElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        if (textContent != null && !textContent.isEmpty()) {
            element.setTextContent(textContent);
        }
        parent.appendChild(element);
    }

    /**
     * Build XML body for program creation.
     * Match Eclipse ADT format.
     */
    private String buildProgramXml(String name, String description, String packageName, String transport) {
        // Eclipse ADT format - similar to function group
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<program:abapProgram xmlns:program=\"http://www.sap.com/adt/programs/programs\" " +
                "xmlns:adtcore=\"http://www.sap.com/adt/core\" " +
                "adtcore:description=\"%s\" " +
                "adtcore:language=\"ES\" " +
                "adtcore:name=\"%s\" " +
                "adtcore:type=\"PROG/P\" " +
                "adtcore:masterLanguage=\"ES\" " +
                "adtcore:masterSystem=\"CAD\" " +
                "adtcore:responsible=\"L_ABAPS_ITA\">" +
                "<adtcore:packageRef adtcore:name=\"%s\"/>" +
                "</program:abapProgram>").formatted(description, name, packageName);
    }

    /**
     * Build XML body for include creation.
     */
    private String buildIncludeXml(String name, String description, String packageName, String transport) {
        // Eclipse ADT format
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<include:abapInclude xmlns:include=\"http://www.sap.com/adt/programs/includes\" " +
                "xmlns:adtcore=\"http://www.sap.com/adt/core\" " +
                "adtcore:description=\"%s\" " +
                "adtcore:language=\"ES\" " +
                "adtcore:name=\"%s\" " +
                "adtcore:type=\"PROG/I\" " +
                "adtcore:masterLanguage=\"ES\" " +
                "adtcore:masterSystem=\"CAD\" " +
                "adtcore:responsible=\"L_ABAPS_ITA\">" +
                "<adtcore:packageRef adtcore:name=\"%s\"/>" +
                "</include:abapInclude>").formatted(description, name, packageName);
    }
}

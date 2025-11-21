package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.config.JCoConfiguration;
import com.crystal.mcp.sapserver.model.ObjectInOpenOTResult;
import com.crystal.mcp.sapserver.model.TableContentsResult;
import com.crystal.mcp.sapserver.model.TransportInfoResult;
import com.crystal.mcp.sapserver.model.TransportInfoListResult;
import com.crystal.mcp.sapserver.model.TransportListResult;
import com.crystal.mcp.sapserver.model.TransportObjectsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
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
import java.util.*;

/**
 * Service for SAP Transport System (CTS) operations.
 *
 * This service handles operations related to SAP Change and Transport System,
 * including listing transports and retrieving transport objects.
 *
 * Progressive Discovery Integration:
 * - Stage 1: list_user_transports → Find available transports
 * - Stage 2: get_transport_objects → Get detailed object list
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 *
 * Supported operations:
 * - List transport requests for a user
 * - Get objects from a transport request
 *
 * Future operations:
 * - Create transport request
 * - Add objects to transport
 * - Release transport
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportService {

    private final RfcAdapter rfcAdapter;
    private final QueryService queryService;
    private final JCoConfiguration jCoConfiguration;

    /**
     * List transport requests for a user.
     *
     * This method retrieves transport requests from the SAP CTS system
     * using the ADT API endpoint /sap/bc/adt/cts/transportrequests.
     *
     * Progressive Discovery Stage 1:
     * - Use to find available transports for a user
     * - Returns lightweight list without object details
     * - Use get_transport_objects to fetch detailed objects
     *
     * ADT API Endpoint:
     * /sap/bc/adt/cts/transportrequests?targets=true&configUri=...
     *
     * Status Values:
     * - D: Modifiable (development)
     * - R: Released
     * - O: Released (With Import Protection)
     * - (empty): All statuses
     *
     * Workflow Example:
     * 1. User: "What transports do I have?"
     * 2. Claude: list_user_transports() → Gets user's transports
     * 3. User: "Show me what's in DEVK900123"
     * 4. Claude: get_transport_objects("DEVK900123") → Gets objects
     *
     * @param user   user ID (null for current user)
     * @param status status filter (D=modifiable, R=released, null=all)
     * @return TransportListResult containing list of transports
     * @throws RuntimeException if query fails
     */
    public TransportListResult listUserTransports(String user, String status) {
        String uri = "/sap/bc/adt/cts/transportrequests";

        // Build query parameters
        // Note: configUri value comes from actual SAP system configuration
        Map<String, String> params = new HashMap<>();
        params.put("targets", "true");
        params.put("configUri", "/sap/bc/adt/cts/transportrequests/searchconfiguration/configurations/0050568BC3BD1EEBBC8FD791A18E5EF1");

        // Build custom headers (as per ADT specification)
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.sap.adt.transportorganizer.v1+xml, application/vnd.sap.adt.transportorganizertree.v1+xml");
        headers.put("User-Agent", "Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)");

        log.info("Listing transports for user: {} (status: {})",
                user != null ? user : "current", status != null ? status : "all");

        try {
            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri,
                    "GET",
                    headers,
                    params,
                    "",
                    "application/xml"
            );

            // Check HTTP status
            if (response.statusCode() == 200) {
                log.debug("Successfully retrieved transports ({} bytes)",
                        response.text().length());

                // Parse XML response
                List<TransportListResult.TransportReference> transports =
                        parseTransportList(response.text(), user, status);

                return new TransportListResult(
                        user,
                        status,
                        transports.size(),
                        transports
                );
            } else {
                String errorMsg = String.format(
                        "Failed to list transports: HTTP %d - %s",
                        response.statusCode(),
                        response.text()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error listing transports: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list transports", e);
        }
    }

    /**
     * Get objects from a transport request.
     *
     * This method retrieves detailed information about objects contained
     * in a transport request, including metadata and object list.
     *
     * NOTE: This is a simplified implementation for Phase 1.
     * Full implementation requires direct RFC calls to E070/E071 tables,
     * which will be added in Phase 2.
     *
     * Progressive Discovery Stage 2:
     * - Use after list_user_transports identifies a transport
     * - Returns complete object list and metadata
     * - More expensive than list_user_transports
     *
     * Workflow Example:
     * 1. User: "Show me what's in DEVK900123"
     * 2. Claude: get_transport_objects("DEVK900123") → Gets full details
     *
     * @param transportNumber transport request number (e.g., "DEVK900123")
     * @param taskNumber      optional task number to filter (for main transports)
     * @return TransportObjectsResult containing objects and metadata
     * @throws RuntimeException if query fails
     */
    public TransportObjectsResult getTransportObjects(
            String transportNumber,
            String taskNumber
    ) {
        // Validate inputs
        if (transportNumber == null || transportNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Transport number cannot be empty");
        }

        log.info("Getting objects for transport: {} (task: {})",
                transportNumber, taskNumber != null ? taskNumber : "all");

        try {
            // Call Z_CX_GET_TRANSPORT_OBJECTS function module
            JCoDestination destination = jCoConfiguration.jcoDestination();
            JCoFunction function = destination.getRepository().getFunction("Z_CX_GET_TRANSPORT_OBJECTS");

            if (function == null) {
                log.error("Function module Z_CX_GET_TRANSPORT_OBJECTS not found");
                return TransportObjectsResult.failure(transportNumber,
                    "Function module Z_CX_GET_TRANSPORT_OBJECTS not found in SAP system");
            }

            // Set import parameters
            JCoParameterList importParams = function.getImportParameterList();
            importParams.setValue("IV_TRANSPORT_NUMBER", transportNumber);
            if (taskNumber != null && !taskNumber.trim().isEmpty()) {
                importParams.setValue("IV_TASK_NUMBER", taskNumber);
            }

            // Execute function
            function.execute(destination);

            // Get export parameters
            JCoParameterList exportParams = function.getExportParameterList();
            String successFlag = exportParams.getString("EV_SUCCESS");
            boolean success = "X".equals(successFlag) || "1".equals(successFlag);
            String message = exportParams.getString("EV_MESSAGE");
            String jsonString = exportParams.getString("EV_TRANSPORT_JSON");

            if (!success) {
                log.error("FM returned failure: {}", message);
                return TransportObjectsResult.failure(transportNumber, message);
            }

            // Parse JSON response
            log.info("JSON from FM (length: {} bytes): {}", jsonString.length(), jsonString);
            return parseTransportObjectsJson(jsonString);

        } catch (JCoException e) {
            log.error("JCo error calling Z_CX_GET_TRANSPORT_OBJECTS: {}", e.getMessage(), e);
            return TransportObjectsResult.failure(transportNumber,
                "JCo error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting transport objects: {}", e.getMessage(), e);
            return TransportObjectsResult.failure(transportNumber, e.getMessage());
        }
    }

    /**
     * Parse JSON response from Z_CX_GET_TRANSPORT_OBJECTS function module.
     *
     * @param jsonString JSON string from FM
     * @return TransportObjectsResult parsed from JSON
     */
    private TransportObjectsResult parseTransportObjectsJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            // Extract basic fields
            boolean success = root.get("success").asBoolean();
            String transportNumber = root.get("transport_number").asText();
            int totalObjects = root.get("total_objects").asInt();

            // Parse metadata
            JsonNode metadataNode = root.get("metadata");
            Map<String, Object> metadata = new HashMap<>();
            metadataNode.fields().forEachRemaining(entry ->
                metadata.put(entry.getKey(), entry.getValue().asText())
            );

            // Parse objects
            List<TransportObjectsResult.TransportObject> objects = new ArrayList<>();
            JsonNode objectsNode = root.get("objects");
            if (objectsNode != null && objectsNode.isArray()) {
                for (JsonNode objNode : objectsNode) {
                    objects.add(new TransportObjectsResult.TransportObject(
                        objNode.get("trkorr").asText(),
                        objNode.get("pgmid").asText(),
                        objNode.get("object_type").asText(),
                        objNode.get("object_name").asText(),
                        objNode.get("lock_flag").asText(""),
                        objNode.get("gennum").asText(""),
                        objNode.get("tab_key").asText("")
                    ));
                }
            }

            // Parse tasks
            List<TransportObjectsResult.Task> tasks = new ArrayList<>();
            JsonNode tasksNode = root.get("tasks");
            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    tasks.add(new TransportObjectsResult.Task(
                        taskNode.get("task_number").asText(),
                        taskNode.get("owner").asText(),
                        taskNode.get("created_date").asText(),
                        taskNode.get("created_time").asText(),
                        taskNode.get("status").asText(),
                        taskNode.get("status_desc").asText(),
                        taskNode.get("description").asText(""),
                        taskNode.get("object_count").asInt()
                    ));
                }
            }

            return new TransportObjectsResult(
                success,
                transportNumber,
                metadata,
                objects,
                totalObjects,
                tasks
            );

        } catch (Exception e) {
            log.error("Error parsing JSON from FM: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse transport list XML response.
     *
     * Handles SAP CTS namespace and extracts transport references from the
     * hierarchical structure organized by category (workbench, customizing, transportofcopies).
     *
     * XML Structure:
     * <tm:root>
     *   <tm:workbench tm:category="Workbench">
     *     <tm:target tm:name="S4Q">
     *       <tm:modifiable tm:status="Modificable">
     *         <tm:request tm:number="S4DK932806" tm:owner="SEBLONDO" tm:desc="..."
     *                     tm:type="K" tm:status="D" ...>
     *           <tm:task tm:number="S4DK932807" ...>
     *             <tm:abap_object .../>
     *           </tm:task>
     *         </tm:request>
     *       </tm:modifiable>
     *     </tm:target>
     *   </tm:workbench>
     *   <tm:customizing tm:category="Customizing">...</tm:customizing>
     *   <tm:transportofcopies tm:category="Transporte de copias">...</tm:transportofcopies>
     * </tm:root>
     *
     * @param xml    XML response from ADT API
     * @param user   user filter (null for all users)
     * @param status status filter (D=modifiable, R=released, null=all)
     * @return List of transport references
     * @throws Exception if XML parsing fails
     */
    private List<TransportListResult.TransportReference> parseTransportList(
            String xml, String user, String status) throws Exception {
        // SAP CTS namespace
        final String NS_TM = "http://www.sap.com/cts/adt/tm";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)
        ));

        List<TransportListResult.TransportReference> transports = new ArrayList<>();

        // Process each category: workbench, customizing, transportofcopies
        String[] categories = {"workbench", "customizing", "transportofcopies"};

        for (String category : categories) {
            NodeList categoryNodes = doc.getElementsByTagNameNS(NS_TM, category);

            for (int i = 0; i < categoryNodes.getLength(); i++) {
                Element categoryElement = (Element) categoryNodes.item(i);
                processCategory(categoryElement, transports, user, status, NS_TM);
            }
        }

        log.info("Parsed {} transports from XML", transports.size());
        return transports;
    }

    /**
     * Process a category element (workbench, customizing, transportofcopies).
     * Recursively processes all request elements within the category.
     */
    private void processCategory(
            Element categoryElement,
            List<TransportListResult.TransportReference> transports,
            String userFilter,
            String statusFilter,
            String namespace) {

        // Get all request elements under this category
        NodeList requestNodes = categoryElement.getElementsByTagNameNS(namespace, "request");

        for (int i = 0; i < requestNodes.getLength(); i++) {
            Element requestElement = (Element) requestNodes.item(i);

            // Extract attributes
            String number = requestElement.getAttribute("tm:number");
            String owner = requestElement.getAttribute("tm:owner");
            String description = requestElement.getAttribute("tm:desc");
            String type = requestElement.getAttribute("tm:type");
            String requestStatus = requestElement.getAttribute("tm:status");

            // Apply filters if specified
            if (userFilter != null && !userFilter.trim().isEmpty() &&
                !owner.equalsIgnoreCase(userFilter)) {
                continue;
            }

            if (statusFilter != null && !statusFilter.trim().isEmpty() &&
                !requestStatus.equalsIgnoreCase(statusFilter)) {
                continue;
            }

            transports.add(new TransportListResult.TransportReference(
                    number, description, requestStatus, owner, type
            ));
        }
    }

    /**
     * Check if an ABAP object is in open (non-released) transport requests.
     *
     * This method queries E071 and E070 tables to determine:
     * - Which transport requests contain the specified object
     * - Whether the object is locked (LOCKFLAG = 'X')
     * - Whether the transport is open/modifiable (TRSTATUS = 'D' or 'L')
     *
     * Workflow Context:
     * - Developer asks: "Can I modify object X?"
     * - This returns list of open transports containing the object
     * - If locked, shows which user has it locked
     * - Released transports (TRSTATUS = 'R') are filtered out
     *
     * Implementation:
     * 1. Query E071 table for objects matching name pattern
     * 2. For each unique transport, query E070 for metadata
     * 3. Filter to keep only open transports (TRSTATUS = 'D' or 'L')
     * 4. Build structured result with transport and object info
     *
     * Progressive Discovery Integration:
     * - Use after search_objects or get_object_structure
     * - Answers: "Is this object in an open transport?"
     * - Token cost: ~1,000-2,000 tokens (depends on results)
     *
     * Reference: python-legacy/app/services/transport_service.py:792
     *
     * @param objectName Object name or pattern to search (supports wildcards)
     *                   Examples: "ZCL_TEST", "%INVOICE%", "ZREP_*"
     * @param objectType Optional object type filter: 'CLAS', 'PROG', 'FUGR', etc.
     *                   Null = search all types
     * @return ObjectInOpenOTResult with list of open transports containing the object
     */
    public ObjectInOpenOTResult getObjectInOpenOT(String objectName, String objectType) {
        // Validate input
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Object name cannot be empty");
        }

        log.info("Checking if object is in open transport: {} (type: {})",
                objectName, objectType != null ? objectType : "all");

        try {
            // Call Z_CX_GET_OBJECT_IN_OPEN_OT function module
            JCoDestination destination = jCoConfiguration.jcoDestination();
            JCoFunction function = destination.getRepository().getFunction("Z_CX_GET_OBJECT_IN_OPEN_OT");

            if (function == null) {
                log.error("Function module Z_CX_GET_OBJECT_IN_OPEN_OT not found");
                return ObjectInOpenOTResult.failure(objectName,
                    "Function module Z_CX_GET_OBJECT_IN_OPEN_OT not found in SAP system");
            }

            // Set import parameters
            JCoParameterList importParams = function.getImportParameterList();
            importParams.setValue("IV_OBJECT_NAME", objectName);
            if (objectType != null && !objectType.trim().isEmpty()) {
                importParams.setValue("IV_OBJECT_TYPE", objectType);
            }

            // Execute function
            function.execute(destination);

            // Get export parameters
            JCoParameterList exportParams = function.getExportParameterList();
            String successFlag = exportParams.getString("EV_SUCCESS");
            boolean success = "X".equals(successFlag);
            String message = exportParams.getString("EV_MESSAGE");
            String jsonString = exportParams.getString("EV_RESULTS_JSON");

            if (!success) {
                log.error("FM returned failure: {}", message);
                return ObjectInOpenOTResult.failure(objectName, message);
            }

            // Parse JSON response
            log.info("JSON from FM (length: {} bytes)", jsonString.length());
            log.debug("JSON content: {}", jsonString);

            return parseObjectInOpenOTJson(jsonString);

        } catch (JCoException e) {
            log.error("JCo error calling Z_CX_GET_OBJECT_IN_OPEN_OT: {}", e.getMessage(), e);
            return ObjectInOpenOTResult.failure(objectName,
                "JCo error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting object in open transport: {}", e.getMessage(), e);
            return ObjectInOpenOTResult.failure(objectName, e.getMessage());
        }
    }

    /**
     * Parse JSON response from Z_CX_GET_OBJECT_IN_OPEN_OT function module.
     *
     * @param jsonString JSON string from FM
     * @return ObjectInOpenOTResult parsed from JSON
     */
    private ObjectInOpenOTResult parseObjectInOpenOTJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            // Extract basic fields
            boolean success = root.get("success").asBoolean();
            String objectName = root.get("objectName").asText();
            String searchPattern = root.get("searchPattern").asText();
            int totalTransports = root.get("totalTransports").asInt();

            // Parse transports array
            List<ObjectInOpenOTResult.TransportInfo> transports = new ArrayList<>();
            JsonNode transportsNode = root.get("transports");

            if (transportsNode != null && transportsNode.isArray()) {
                for (JsonNode transportNode : transportsNode) {
                    // Parse objectInfo
                    JsonNode objInfoNode = transportNode.get("objectInfo");
                    ObjectInOpenOTResult.ObjectInfo objectInfo = null;
                    if (objInfoNode != null) {
                        objectInfo = new ObjectInOpenOTResult.ObjectInfo(
                            objInfoNode.get("objName").asText(),
                            objInfoNode.get("objectType").asText(),
                            objInfoNode.get("pgmid").asText()
                        );
                    }

                    // Parse parentTransport (can be null)
                    JsonNode parentNode = transportNode.get("parentTransport");
                    ObjectInOpenOTResult.ParentTransportInfo parentTransport = null;
                    if (parentNode != null && !parentNode.isNull()) {
                        parentTransport = new ObjectInOpenOTResult.ParentTransportInfo(
                            parentNode.get("transportNumber").asText(),
                            parentNode.get("transportType").asText(),
                            parentNode.get("transportTypeDesc").asText(),
                            parentNode.get("status").asText(),
                            parentNode.get("statusDesc").asText(),
                            parentNode.get("owner").asText(),
                            parentNode.get("description").asText()
                        );
                    }

                    // Build TransportInfo
                    ObjectInOpenOTResult.TransportInfo transportInfo =
                        new ObjectInOpenOTResult.TransportInfo(
                            transportNode.get("transportNumber").asText(),
                            transportNode.get("transportType").asText(),
                            transportNode.get("transportTypeDesc").asText(),
                            transportNode.get("status").asText(),
                            transportNode.get("statusDesc").asText(),
                            transportNode.get("owner").asText(),
                            transportNode.get("createdDate").asText(),
                            transportNode.get("createdTime").asText(),
                            transportNode.get("isLocked").asBoolean(),
                            objectInfo,
                            parentTransport
                        );

                    transports.add(transportInfo);
                }
            }

            return new ObjectInOpenOTResult(
                success,
                objectName,
                searchPattern,
                transports,
                totalTransports
            );

        } catch (Exception e) {
            log.error("Error parsing JSON from FM: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    /**
     * Get transport request metadata without objects.
     *
     * This method retrieves complete metadata for one or more transport requests
     * from E070 table without loading the full object list. Use this
     * when you need transport information but don't need to see all objects.
     *
     * Progressive Discovery Integration:
     * - Lightweight alternative to get_transport_objects
     * - Use when you need metadata only (owner, status, dates, etc.)
     * - For full object details, use get_transport_objects instead
     *
     * Implementation:
     * Uses Z_CX_GET_TRANSPORT_INFO function module which queries E070
     * with efficient JOIN and returns metadata with object/task counts.
     *
     * Supports Multiple Transports:
     * - Single: "CADK911088"
     * - Multiple: "CADK911088,CADK911122"
     *
     * Token Cost: ~500-800 tokens per transport (much cheaper than get_transport_objects)
     *
     * Workflow Examples:
     * 1. "Who owns transport DEVK900123?"
     *    → get_transport_info("DEVK900123") → Returns owner, status
     * 2. "Is transport CADK911088 released?"
     *    → get_transport_info("CADK911088") → Returns status
     * 3. "Get info for multiple transports"
     *    → get_transport_info("CADK911088,CADK911122") → Returns list
     *
     * @param transportNumbers Transport request number(s) - single or comma-separated
     * @return TransportInfoListResult with complete metadata
     * @throws RuntimeException if FM call fails
     */
    public TransportInfoListResult getTransportInfo(String transportNumbers) {
        // Validate input
        if (transportNumbers == null || transportNumbers.trim().isEmpty()) {
            throw new IllegalArgumentException("Transport number(s) cannot be empty");
        }

        log.info("Getting transport info for: {}", transportNumbers);

        try {
            // Get destination
            JCoDestination destination = jCoConfiguration.jcoDestination();

            // Get function module
            JCoFunction function = destination.getRepository()
                    .getFunction("Z_CX_GET_TRANSPORT_INFO");

            if (function == null) {
                throw new RuntimeException(
                        "Function module Z_CX_GET_TRANSPORT_INFO not found. " +
                        "Please verify FM exists in SAP system (GDC)."
                );
            }

            // Set import parameters (now accepts comma-separated list)
            JCoParameterList importParams = function.getImportParameterList();
            importParams.setValue("IV_TRANSPORT_NUMBERS", transportNumbers.trim().toUpperCase());

            // Execute function
            function.execute(destination);

            // Get export parameters
            JCoParameterList exportParams = function.getExportParameterList();
            String successFlag = exportParams.getString("EV_SUCCESS");
            boolean success = "X".equals(successFlag) || "1".equals(successFlag);
            String message = exportParams.getString("EV_MESSAGE");
            String jsonString = exportParams.getString("EV_TRANSPORTS_JSON");

            if (!success) {
                log.error("FM returned failure: {}", message);
                return TransportInfoListResult.failure(message);
            }

            // Parse JSON response (now array)
            log.info("JSON from FM (length: {} bytes)", jsonString.length());
            return parseTransportInfoJsonArray(jsonString);

        } catch (JCoException e) {
            log.error("JCo error calling Z_CX_GET_TRANSPORT_INFO: {}", e.getMessage(), e);
            return TransportInfoListResult.failure("JCo error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting transport info: {}", e.getMessage(), e);
            return TransportInfoListResult.failure(e.getMessage());
        }
    }

    /**
     * Parse JSON response from Z_CX_GET_TRANSPORT_INFO function module.
     *
     * Expected JSON structure:
     * {
     *   "success": true,
     *   "transport_number": "CADK911088",
     *   "transport_type": "K",
     *   "transport_type_desc": "Workbench",
     *   "status": "D",
     *   "status_desc": "Modifiable",
     *   "owner": "USERNAME",
     *   "description": "Transport description",
     *   "created_date": "2025-01-15",
     *   "created_time": "14:30:45",
     *   "target_system": "S4Q",
     *   "category": "CUST",
     *   "parent_transport": null,
     *   "has_objects": true,
     *   "has_tasks": true
     * }
     *
     * @param jsonString JSON string from FM
     * @return TransportInfoResult parsed from JSON
     */
    private TransportInfoResult parseTransportInfoJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            // Extract fields
            boolean success = root.get("success").asBoolean();
            String transportNumber = root.get("transport_number").asText();
            String transportType = root.get("transport_type").asText("");
            String transportTypeDesc = root.get("transport_type_desc").asText("");
            String status = root.get("status").asText("");
            String statusDesc = root.get("status_desc").asText("");
            String owner = root.get("owner").asText("");
            String description = root.get("description").asText("");
            String createdDate = root.get("created_date").asText("");
            String createdTime = root.get("created_time").asText("");
            String targetSystem = root.get("target_system").asText("");
            String category = root.get("category").asText("");

            // Handle null parent_transport
            JsonNode parentNode = root.get("parent_transport");
            String parentTransport = (parentNode != null && !parentNode.isNull())
                    ? parentNode.asText() : null;

            boolean hasObjects = root.get("has_objects").asBoolean(false);
            boolean hasTasks = root.get("has_tasks").asBoolean(false);

            return new TransportInfoResult(
                success,
                transportNumber,
                transportType,
                transportTypeDesc,
                status,
                statusDesc,
                owner,
                description,
                createdDate,
                createdTime,
                targetSystem,
                category,
                parentTransport,
                hasObjects,
                hasTasks
            );

        } catch (Exception e) {
            log.error("Error parsing JSON from FM: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JSON array response from Z_CX_GET_TRANSPORT_INFO function module.
     *
     * Expected JSON structure:
     * [{
     *   "transport_number": "CADK911088",
     *   "transport_type": "K",
     *   "transport_type_desc": "Workbench",
     *   "status": "D",
     *   "status_desc": "Modifiable",
     *   "owner": "USERNAME",
     *   "description": "Transport description",
     *   "created_date": "2025-01-15",
     *   "created_time": "14:30:45",
     *   "target_system": "S4Q",
     *   "category": "CUST",
     *   "parent_transport": null,
     *   "object_count": 15,
     *   "task_count": 2
     * }]
     *
     * @param jsonString JSON array string from FM
     * @return TransportInfoListResult with list of transports
     */
    private TransportInfoListResult parseTransportInfoJsonArray(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            if (!root.isArray()) {
                throw new RuntimeException("Expected JSON array, got: " + root.getNodeType());
            }

            List<TransportInfoListResult.TransportInfo> transports = new ArrayList<>();

            for (JsonNode transportNode : root) {
                // Extract fields
                String transportNumber = transportNode.get("transport_number").asText();
                String transportType = transportNode.get("transport_type").asText("");
                String transportTypeDesc = transportNode.get("transport_type_desc").asText("");
                String status = transportNode.get("status").asText("");
                String statusDesc = transportNode.get("status_desc").asText("");
                String owner = transportNode.get("owner").asText("");
                String description = transportNode.get("description").asText("");
                String createdDate = transportNode.get("created_date").asText("");
                String createdTime = transportNode.get("created_time").asText("");
                String targetSystem = transportNode.get("target_system").asText("");
                String category = transportNode.get("category").asText("");

                // Handle null parent_transport
                JsonNode parentNode = transportNode.get("parent_transport");
                String parentTransport = (parentNode != null && !parentNode.isNull())
                        ? parentNode.asText() : null;

                int objectCount = transportNode.get("object_count").asInt(0);
                int taskCount = transportNode.get("task_count").asInt(0);

                TransportInfoListResult.TransportInfo info = new TransportInfoListResult.TransportInfo(
                    transportNumber,
                    transportType,
                    transportTypeDesc,
                    status,
                    statusDesc,
                    owner,
                    description,
                    createdDate,
                    createdTime,
                    targetSystem,
                    category,
                    parentTransport,
                    objectCount,
                    taskCount
                );

                transports.add(info);
            }

            return TransportInfoListResult.success(transports);

        } catch (Exception e) {
            log.error("Error parsing JSON array from FM: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON array response: " + e.getMessage(), e);
        }
    }

    /**
     * Get objects from E071 table for a given transport or task.
     *
     * @param trkorr Transport or task number
     * @return List of transport objects
     */
    private List<TransportObjectsResult.TransportObject> getObjectsFromE071(String trkorr) {
        log.debug("Querying E071 for objects in transport: {}", trkorr);

        TableContentsResult e071Result = queryService.getTableContents(
                "E071",
                "TRKORR = '" + trkorr + "'",
                1000,
                List.of("TRKORR", "PGMID", "OBJECT", "OBJ_NAME", "LOCKFLAG", "GENNUM", "TABKEY")
        );

        List<TransportObjectsResult.TransportObject> objects = new ArrayList<>();

        for (Map<String, String> row : e071Result.rows()) {
            String objectTrkorr = row.get("TRKORR");
            String pgmid = row.get("PGMID");
            String objectType = row.get("OBJECT");
            String objectName = row.get("OBJ_NAME");
            String lockFlag = row.get("LOCKFLAG");
            String gennum = row.get("GENNUM");
            String tabKey = row.get("TABKEY");

            objects.add(new TransportObjectsResult.TransportObject(
                    objectTrkorr,
                    pgmid != null ? pgmid : "",
                    objectType != null ? objectType : "",
                    objectName != null ? objectName : "",
                    lockFlag != null ? lockFlag : "",
                    gennum != null ? gennum : "",
                    tabKey != null ? tabKey : ""
            ));
        }

        log.debug("Found {} objects in E071 for transport {}", objects.size(), trkorr);
        return objects;
    }

    /**
     * Build transport metadata map from E070 fields.
     *
     * @param transportNumber Transport number
     * @param trFunction      TRFUNCTION (K, S, T, etc.)
     * @param trStatus        TRSTATUS (D, R, L, etc.)
     * @param owner           AS4USER
     * @param date            AS4DATE (YYYYMMDD)
     * @param time            AS4TIME (HHMMSS)
     * @param targetSystem    TARSYSTEM
     * @param category        KORRDEV
     * @param parentTransport STRKORR
     * @param description     AS4TEXT
     * @return Metadata map
     */
    private Map<String, Object> buildTransportMetadata(
            String transportNumber,
            String trFunction,
            String trStatus,
            String owner,
            String date,
            String time,
            String targetSystem,
            String category,
            String parentTransport,
            String description
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transport_number", transportNumber);
        metadata.put("transport_type", trFunction != null ? trFunction : "");
        metadata.put("transport_type_desc", mapTransportType(trFunction));
        metadata.put("status", trStatus != null ? trStatus : "");
        metadata.put("status_desc", mapTransportStatus(trStatus));
        metadata.put("owner", owner != null ? owner : "");
        metadata.put("created_date", formatDate(date));
        metadata.put("created_time", formatTime(time));
        metadata.put("target_system", targetSystem != null ? targetSystem : "");
        metadata.put("category", category != null ? category : "");
        metadata.put("description", description != null ? description : "");
        metadata.put("parent_transport", parentTransport != null ? parentTransport : "");

        return metadata;
    }

    /**
     * Map transport type code to description.
     *
     * @param trFunction TRFUNCTION code
     * @return Human-readable description
     */
    private String mapTransportType(String trFunction) {
        if (trFunction == null) return "";

        return switch (trFunction) {
            case "K" -> "Workbench";
            case "S" -> "Task";
            case "T" -> "Transport of Copies";
            case "W" -> "Workbench Request";
            case "C" -> "Customizing";
            default -> trFunction;
        };
    }

    /**
     * Map transport status code to description.
     *
     * @param trStatus TRSTATUS code
     * @return Human-readable description
     */
    private String mapTransportStatus(String trStatus) {
        if (trStatus == null) return "";

        return switch (trStatus) {
            case "D" -> "Modifiable";
            case "L" -> "Protected";
            case "R" -> "Released";
            case "N" -> "Modifiable (Protected)";
            case "O" -> "Released (With Import Protection)";
            default -> trStatus;
        };
    }

    /**
     * Format date from YYYYMMDD to YYYY-MM-DD.
     *
     * @param date Date string in YYYYMMDD format
     * @return Formatted date or empty string
     */
    private String formatDate(String date) {
        if (date == null || date.length() != 8) {
            return "";
        }
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }

    /**
     * Format time from HHMMSS to HH:MM:SS.
     *
     * @param time Time string in HHMMSS format
     * @return Formatted time or empty string
     */
    private String formatTime(String time) {
        if (time == null || time.length() != 6) {
            return "";
        }
        return time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + time.substring(4, 6);
    }
}

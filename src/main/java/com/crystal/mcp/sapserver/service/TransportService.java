package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ObjectInOpenOTResult;
import com.crystal.mcp.sapserver.model.TableContentsResult;
import com.crystal.mcp.sapserver.model.TransportListResult;
import com.crystal.mcp.sapserver.model.TransportObjectsResult;
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

        // TODO: Full implementation requires RFC call to E070/E071 tables
        // For now, return a placeholder structure
        log.warn("get_transport_objects: Full implementation pending (requires RFC table access)");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transport_number", transportNumber);
        metadata.put("status", "implementation_pending");
        metadata.put("note", "Full implementation requires direct RFC calls to E070/E071 tables");

        return new TransportObjectsResult(
                false,
                transportNumber,
                metadata,
                new ArrayList<>(),
                0,
                new ArrayList<>()
        );
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

        String searchPattern = "%" + objectName.trim() + "%";
        log.info("Checking if object is in open transport: {} (type: {})",
                objectName, objectType != null ? objectType : "all");

        try {
            // Step 1: Query E071 table for objects matching name pattern
            // Note: We search by OBJ_NAME only (not by OBJECT type) because:
            // - Classes can have related objects: METH (methods), CLSD (definition), CPUB (public section)
            // - The class name appears in all related object names (e.g., ZCLMMI1229_SINCRONIZA_INV_MAWMPROCESAR_INFORMACION)
            // - Filtering by OBJECT would exclude methods and other class components
            String whereClause = "OBJ_NAME LIKE '" + searchPattern + "'";

            // objectType parameter is ignored intentionally
            // We return ALL related objects (methods, definitions, etc.)
            if (objectType != null && !objectType.trim().isEmpty()) {
                log.debug("objectType parameter '{}' is ignored - searching all object types", objectType);
            }

            log.debug("E071 WHERE clause: {}", whereClause);

            TableContentsResult e071Result = queryService.getTableContents(
                    "E071",
                    whereClause,
                    1000,  // Max 1000 results
                    List.of("TRKORR", "OBJ_NAME", "OBJECT", "PGMID", "LOCKFLAG")
            );

            if (e071Result.rowCount() == 0) {
                log.info("No objects found in E071 matching: {}", searchPattern);
                return ObjectInOpenOTResult.notFound(objectName, searchPattern);
            }

            log.debug("Found {} objects in E071", e071Result.rowCount());

            // Step 2: Group by unique transport numbers and query E070 for metadata
            Map<String, ObjectInOpenOTResult.TransportInfo> transportMap = new HashMap<>();

            for (Map<String, String> row : e071Result.rows()) {
                String trkorr = row.get("TRKORR");
                String objName = row.get("OBJ_NAME");
                String object = row.get("OBJECT");
                String pgmid = row.get("PGMID");
                String lockFlag = row.get("LOCKFLAG");

                // Skip if already processed this transport
                if (transportMap.containsKey(trkorr)) {
                    continue;
                }

                // Query E070 for transport metadata
                TableContentsResult e070Result = queryService.getTableContents(
                        "E070",
                        "TRKORR = '" + trkorr + "'",
                        1,
                        List.of("TRFUNCTION", "TRSTATUS", "AS4USER", "AS4DATE", "AS4TIME")
                );

                if (e070Result.rowCount() == 0) {
                    log.warn("Transport {} found in E071 but not in E070", trkorr);
                    continue;
                }

                Map<String, String> e070Row = e070Result.rows().get(0);
                String trFunction = e070Row.get("TRFUNCTION");
                String trStatus = e070Row.get("TRSTATUS");
                String owner = e070Row.get("AS4USER");
                String date = e070Row.get("AS4DATE");
                String time = e070Row.get("AS4TIME");

                // Step 3a: If this is a task (TRFUNCTION = 'S'), get parent transport info
                ObjectInOpenOTResult.ParentTransportInfo parentTransport = null;
                String effectiveStatus = trStatus;

                if ("S".equals(trFunction)) {
                    // This is a task, get parent transport
                    String strkorr = e070Row.get("STRKORR");
                    if (strkorr != null && !strkorr.trim().isEmpty()) {
                        log.debug("Task {} belongs to parent transport {}", trkorr, strkorr);

                        // Query E070 for parent transport
                        TableContentsResult parentResult = queryService.getTableContents(
                                "E070",
                                "TRKORR = '" + strkorr + "'",
                                1,
                                List.of("TRFUNCTION", "TRSTATUS", "AS4USER", "AS4TEXT")
                        );

                        if (parentResult.rowCount() > 0) {
                            Map<String, String> parentRow = parentResult.rows().get(0);
                            String parentTrFunction = parentRow.get("TRFUNCTION");
                            String parentTrStatus = parentRow.get("TRSTATUS");
                            String parentOwner = parentRow.get("AS4USER");
                            String parentDesc = parentRow.get("AS4TEXT");

                            // Map parent transport type
                            Map<String, String> typeMap = Map.of(
                                    "K", "Workbench",
                                    "S", "Task",
                                    "T", "Transport of Copies",
                                    "W", "Workbench Request",
                                    "C", "Customizing"
                            );

                            // Map parent status
                            Map<String, String> statusMap = Map.of(
                                    "D", "Modifiable",
                                    "L", "Protected",
                                    "R", "Released",
                                    "N", "Modifiable (Protected)",
                                    "O", "Released (With Import Protection)"
                            );

                            parentTransport = new ObjectInOpenOTResult.ParentTransportInfo(
                                    strkorr,
                                    parentTrFunction,
                                    typeMap.getOrDefault(parentTrFunction, parentTrFunction),
                                    parentTrStatus,
                                    statusMap.getOrDefault(parentTrStatus, parentTrStatus),
                                    parentOwner,
                                    parentDesc
                            );

                            // Use parent status for filtering
                            effectiveStatus = parentTrStatus;
                            log.debug("Parent transport {} status: {}", strkorr, parentTrStatus);
                        }
                    }
                }

                // Step 3b: Filter - only keep open transports (D or L)
                // For tasks, check parent transport status
                if (!"D".equals(effectiveStatus) && !"L".equals(effectiveStatus)) {
                    log.debug("Skipping transport {} (effective status: {})", trkorr, effectiveStatus);
                    continue;
                }

                // Format date: YYYYMMDD → YYYY-MM-DD
                String formattedDate = date;
                if (date != null && date.length() == 8) {
                    formattedDate = date.substring(0, 4) + "-" +
                            date.substring(4, 6) + "-" +
                            date.substring(6, 8);
                }

                // Format time: HHMMSS → HH:MM:SS
                String formattedTime = time;
                if (time != null && time.length() == 6) {
                    formattedTime = time.substring(0, 2) + ":" +
                            time.substring(2, 4) + ":" +
                            time.substring(4, 6);
                }

                // Map transport type
                Map<String, String> typeMap = Map.of(
                        "K", "Workbench",
                        "S", "Task",
                        "T", "Transport of Copies",
                        "W", "Workbench Request",
                        "C", "Customizing"
                );

                // Map status
                Map<String, String> statusMap = Map.of(
                        "D", "Modifiable",
                        "L", "Protected",
                        "R", "Released",
                        "N", "Modifiable (Protected)",
                        "O", "Released (With Import Protection)"
                );

                // Build TransportInfo
                ObjectInOpenOTResult.ObjectInfo objectInfo =
                        new ObjectInOpenOTResult.ObjectInfo(objName, object, pgmid);

                ObjectInOpenOTResult.TransportInfo transportInfo =
                        new ObjectInOpenOTResult.TransportInfo(
                                trkorr,
                                trFunction,
                                typeMap.getOrDefault(trFunction, trFunction),
                                trStatus,
                                statusMap.getOrDefault(trStatus, trStatus),
                                owner,
                                formattedDate,
                                formattedTime,
                                "X".equals(lockFlag),
                                objectInfo,
                                parentTransport
                        );

                transportMap.put(trkorr, transportInfo);
            }

            // Step 4: Build final result
            List<ObjectInOpenOTResult.TransportInfo> transports =
                    new ArrayList<>(transportMap.values());

            log.info("Found {} open transports for object: {}", transports.size(), objectName);

            return new ObjectInOpenOTResult(
                    true,
                    objectName,
                    searchPattern,
                    transports,
                    transports.size()
            );

        } catch (Exception e) {
            log.error("Error checking object in open transport: {}", e.getMessage(), e);
            return ObjectInOpenOTResult.failure(objectName, e.getMessage());
        }
    }
}

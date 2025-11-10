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
     * using the ADT API endpoint /sap/bc/adt/cts/transports.
     *
     * Progressive Discovery Stage 1:
     * - Use to find available transports for a user
     * - Returns lightweight list without object details
     * - Use get_transport_objects to fetch detailed objects
     *
     * ADT API Endpoint:
     * /sap/bc/adt/cts/transports?user={user}&status={status}
     *
     * Status Values:
     * - D: Modifiable (development)
     * - R: Released
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
        String uri = "/sap/bc/adt/cts/transports";

        // Build query parameters
        Map<String, String> params = new HashMap<>();
        if (user != null && !user.trim().isEmpty()) {
            params.put("user", user);
        }
        if (status != null && !status.trim().isEmpty()) {
            params.put("status", status);
        }

        log.info("Listing transports for user: {} (status: {})",
                user != null ? user : "current", status != null ? status : "all");

        try {
            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri,
                    "GET",
                    null,
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
                        parseTransportList(response.text());

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
     * Handles SAP CTS namespace and extracts transport references.
     *
     * XML Structure:
     * <tm:transports>
     *   <tm:transport>
     *     <tm:number>DEVK900123</tm:number>
     *     <tm:description>...</tm:description>
     *     <tm:status>D</tm:status>
     *     <tm:owner>USER</tm:owner>
     *   </tm:transport>
     * </tm:transports>
     *
     * @param xml XML response from ADT API
     * @return List of transport references
     * @throws Exception if XML parsing fails
     */
    private List<TransportListResult.TransportReference> parseTransportList(String xml)
            throws Exception {
        // SAP CTS namespace
        final String NS_TM = "http://www.sap.com/adt/cts/transports";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)
        ));

        List<TransportListResult.TransportReference> transports = new ArrayList<>();

        // Get all transport elements
        NodeList transportNodes = doc.getElementsByTagNameNS(NS_TM, "transport");
        for (int i = 0; i < transportNodes.getLength(); i++) {
            Element transportElement = (Element) transportNodes.item(i);

            String number = getElementText(transportElement, "number", NS_TM);
            String description = getElementText(transportElement, "description", NS_TM);
            String status = getElementText(transportElement, "status", NS_TM);
            String owner = getElementText(transportElement, "owner", NS_TM);
            String type = getElementText(transportElement, "type", NS_TM);

            transports.add(new TransportListResult.TransportReference(
                    number, description, status, owner, type
            ));
        }

        log.info("Parsed {} transports from XML", transports.size());
        return transports;
    }

    /**
     * Helper method to extract text from XML element by namespace and tag.
     *
     * @param parent    parent element
     * @param tagName   tag name to search for
     * @param namespace namespace URI
     * @return text content or empty string if not found
     */
    private String getElementText(Element parent, String tagName, String namespace) {
        NodeList nodes = parent.getElementsByTagNameNS(namespace, tagName);
        if (nodes.getLength() > 0) {
            Element element = (Element) nodes.item(0);
            return element.getTextContent().trim();
        }
        return "";
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
            String whereClause = "OBJ_NAME LIKE '" + searchPattern + "'";
            if (objectType != null && !objectType.trim().isEmpty()) {
                whereClause += " AND OBJECT = '" + objectType.trim() + "'";
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

                // Step 3: Filter - only keep open transports (D or L)
                if (!"D".equals(trStatus) && !"L".equals(trStatus)) {
                    log.debug("Skipping transport {} (status: {})", trkorr, trStatus);
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
                                objectInfo
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

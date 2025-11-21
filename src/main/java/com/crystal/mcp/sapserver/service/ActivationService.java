package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ActivationResult;
import com.crystal.mcp.sapserver.model.InactiveObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ABAP object activation operations.
 * Handles checking inactive objects and activating them via ADT REST API.
 */
@Service
public class ActivationService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationService.class);
    private final RfcAdapter rfcAdapter;

    public ActivationService(RfcAdapter rfcAdapter) {
        this.rfcAdapter = rfcAdapter;
    }

    /**
     * Get all inactive objects in the system.
     *
     * @return List of inactive objects with their transport information
     */
    public List<InactiveObject> getInactiveObjects() {
        logger.info("Getting inactive objects");

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/vnd.sap.adt.inactivectsobjects.v1+xml, application/xml;q=0.8");

            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    "/sap/bc/adt/activation/inactiveobjects",
                    "GET",
                    headers,
                    null,
                    "",
                    "application/xml");

            return parseInactiveObjects(response.text());
        } catch (Exception e) {
            logger.error("Error getting inactive objects", e);
            throw new RuntimeException("Failed to get inactive objects: " + e.getMessage(), e);
        }
    }

    /**
     * Activate ABAP objects.
     *
     * @param objectUris List of ADT URIs of objects to activate
     * @return Activation result with success status and any errors
     */
    public ActivationResult activateObjects(List<String> objectUris) {
        logger.info("Activating {} objects", objectUris.size());

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/xml");
            headers.put("Content-Type", "application/xml");

            // Llamado 1: Preaudit (preauditRequested=true)
            // Body: Simple request (URI + name only)
            // Response: ALWAYS returns XML with inactive objects and metadata
            String simpleRequestBody = buildSimpleActivationRequest(objectUris);

            logger.debug("Calling preaudit activation (Llamado 1)");
            RfcAdapter.RfcResponse preauditResponse = rfcAdapter.request(
                    "/sap/bc/adt/activation?method=activate&preauditRequested=true",
                    "POST",
                    headers,
                    null,
                    simpleRequestBody,
                    "application/xml");

            if (preauditResponse.statusCode() != 200) {
                throw new RuntimeException("Preaudit activation failed with status: " + preauditResponse.statusCode());
            }

            // Parse preaudit response to extract full object metadata
            // Response contains: type, packageName, parentUri needed for Llamado 2
            List<ObjectMetadata> objectMetadata = parsePreauditResponse(preauditResponse.text());

            if (objectMetadata.isEmpty()) {
                logger.warn("No object metadata extracted from preaudit response");
                return new ActivationResult(false, "No objects found to activate", List.of());
            }

            // Llamado 2: Final activation (preauditRequested=false)
            // Body: Full request with type, packageName, parentUri from Llamado 1 response
            // Response: Empty = success, XML <chkl:messages> = syntax errors
            String fullRequestBody = buildFullActivationRequest(objectMetadata);

            logger.debug("Calling final activation (Llamado 2) with {} objects", objectMetadata.size());
            RfcAdapter.RfcResponse finalResponse = rfcAdapter.request(
                    "/sap/bc/adt/activation?method=activate&preauditRequested=false",
                    "POST",
                    headers,
                    null,
                    fullRequestBody,
                    "application/xml");

            if (finalResponse.statusCode() != 200) {
                throw new RuntimeException("Final activation failed with status: " + finalResponse.statusCode());
            }

            // Parse final activation response
            // Empty response = success
            // XML with <chkl:messages> = syntax errors
            return parseActivationResponse(finalResponse.text());

        } catch (Exception e) {
            logger.error("Error activating objects", e);
            throw new RuntimeException("Failed to activate objects: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method for workflows: activates an object directly.
     *
     * IMPORTANT: This method activates the object without checking if it's inactive
     * first.
     * This is the correct Eclipse ADT workflow:
     * LOCK → MODIFY → UNLOCK → ACTIVATE
     *
     * The inactive check (getInactiveObjects) is unnecessary and expensive:
     * - Lists ALL inactive objects in the system (can be hundreds)
     * - Not needed for activation workflow
     * - ADT API handles activation directly
     *
     * @param objectUri ADT URI of the object
     * @return Activation result
     */
    public ActivationResult checkAndActivate(String objectUri) {
        logger.info("Activating object: {}", objectUri);

        // REMOVED: getInactiveObjects() call - unnecessary and expensive
        // Eclipse ADT workflow: LOCK → MODIFY → UNLOCK → ACTIVATE (direct)
        //
        // Old code (commented):
        // List<InactiveObject> inactiveObjects = getInactiveObjects();
        // boolean isInactive = inactiveObjects.stream()
        // .anyMatch(obj -> obj.uri().equals(objectUri));
        // if (!isInactive) {
        // return new ActivationResult(true, "Object already active", List.of());
        // }

        // Activate the object directly
        return activateObjects(List.of(objectUri));
    }

    /**
     * Parse XML response from GET /sap/bc/adt/activation/inactiveobjects
     */
    private List<InactiveObject> parseInactiveObjects(String xml) {
        List<InactiveObject> result = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

            NodeList entries = doc.getElementsByTagNameNS("http://www.sap.com/abapxml/inactiveCtsObjects", "entry");

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);

                // Parse object
                Element objectElement = (Element) entry.getElementsByTagNameNS(
                        "http://www.sap.com/abapxml/inactiveCtsObjects", "object").item(0);

                // Parse transport
                Element transportElement = (Element) entry.getElementsByTagNameNS(
                        "http://www.sap.com/abapxml/inactiveCtsObjects", "transport").item(0);

                // Parse object ref (nested in object)
                NodeList objectRefs = objectElement.getElementsByTagNameNS("http://www.sap.com/adt/core", "ref");
                Element objectRef = objectRefs.getLength() > 0 ? (Element) objectRefs.item(0) : null;

                // Parse transport ref (nested in transport)
                NodeList transportRefs = transportElement.getElementsByTagNameNS("http://www.sap.com/adt/core", "ref");
                Element transportRef = transportRefs.getLength() > 0 ? (Element) transportRefs.item(0) : null;

                // Build InactiveObject
                if (objectRef != null) {
                    String objectUri = objectRef.getAttributeNS("http://www.sap.com/adt/core", "uri");
                    String objectType = objectRef.getAttributeNS("http://www.sap.com/adt/core", "type");
                    String objectName = objectRef.getAttributeNS("http://www.sap.com/adt/core", "name");
                    String objectDesc = objectRef.getAttributeNS("http://www.sap.com/adt/core", "description");
                    String packageName = objectRef.getAttributeNS("http://www.sap.com/adt/core", "packageName");

                    String objectUser = objectElement.getAttributeNS("http://www.sap.com/abapxml/inactiveCtsObjects",
                            "user");
                    boolean deleted = "true".equals(objectElement.getAttributeNS(
                            "http://www.sap.com/abapxml/inactiveCtsObjects", "deleted"));

                    InactiveObject.TransportInfo transportInfo = null;
                    if (transportRef != null) {
                        String transportUri = transportRef.getAttributeNS("http://www.sap.com/adt/core", "uri");
                        String transportName = transportRef.getAttributeNS("http://www.sap.com/adt/core", "name");
                        String transportDesc = transportRef.getAttributeNS("http://www.sap.com/adt/core",
                                "description");
                        String transportUser = transportElement.getAttributeNS(
                                "http://www.sap.com/abapxml/inactiveCtsObjects", "user");
                        boolean linked = "true".equals(transportElement.getAttributeNS(
                                "http://www.sap.com/abapxml/inactiveCtsObjects", "linked"));

                        transportInfo = new InactiveObject.TransportInfo(
                                transportUri, transportName, transportDesc, transportUser, linked);
                    }

                    result.add(new InactiveObject(
                            objectUri, objectType, objectName, objectDesc, packageName,
                            objectUser, deleted, transportInfo));
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing inactive objects XML", e);
            throw new RuntimeException("Failed to parse inactive objects: " + e.getMessage(), e);
        }

        logger.info("Found {} inactive objects", result.size());
        return result;
    }

    /**
     * Build XML request body for activation - simple version (Llamado 1).
     * Only includes URI and name.
     */
    private String buildSimpleActivationRequest(List<String> objectUris) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<adtcore:objectReferences xmlns:adtcore=\"http://www.sap.com/adt/core\">");

        for (String uri : objectUris) {
            // Extract name from URI (last segment)
            String name = uri.substring(uri.lastIndexOf('/') + 1);
            if (name.contains("#")) {
                name = name.substring(0, name.indexOf('#'));
            }

            xml.append("<adtcore:objectReference adtcore:uri=\"").append(uri).append("\" ");
            xml.append("adtcore:name=\"").append(name).append("\"/>");
        }

        xml.append("</adtcore:objectReferences>");
        return xml.toString();
    }

    /**
     * Build XML request body for activation - full version (Llamado 2).
     * Includes full metadata: type, packageName, parentUri.
     */
    private String buildFullActivationRequest(List<ObjectMetadata> metadata) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<adtcore:objectReferences xmlns:adtcore=\"http://www.sap.com/adt/core\">");

        for (ObjectMetadata obj : metadata) {
            xml.append("<adtcore:objectReference adtcore:uri=\"").append(obj.uri()).append("\"");

            if (obj.type() != null && !obj.type().isEmpty()) {
                xml.append(" adtcore:type=\"").append(obj.type()).append("\"");
            }

            xml.append(" adtcore:name=\"").append(obj.name()).append("\"");

            if (obj.packageName() != null && !obj.packageName().isEmpty()) {
                xml.append(" adtcore:packageName=\"").append(obj.packageName()).append("\"");
            }

            if (obj.parentUri() != null && !obj.parentUri().isEmpty()) {
                xml.append(" adtcore:parentUri=\"").append(obj.parentUri()).append("\"");
            }

            xml.append("/>");
        }

        xml.append("</adtcore:objectReferences>");
        return xml.toString();
    }

    /**
     * Object metadata for activation request (Llamado 2).
     */
    private record ObjectMetadata(
        String uri,
        String type,
        String name,
        String packageName,
        String parentUri
    ) {}

    /**
     * Parse preaudit response (Llamado 1) to extract object metadata.
     *
     * Response scenarios:
     * 1. Objects are INACTIVE and need activation -> Returns XML with metadata
     * 2. Objects are ACTIVE (nothing to activate) -> Returns empty body
     *
     * The XML contains full object metadata (type, packageName, parentUri)
     * needed for Llamado 2 request body.
     */
    private List<ObjectMetadata> parsePreauditResponse(String response) {
        List<ObjectMetadata> result = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            logger.debug("Preaudit response is empty - objects are already active or don't exist");
            return result;
        }

        logger.debug("Parsing preaudit response, length: {} bytes", response.length());

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.getBytes()));

            NodeList entries = doc.getElementsByTagNameNS("http://www.sap.com/abapxml/inactiveCtsObjects", "entry");

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);

                // Parse object
                Element objectElement = (Element) entry.getElementsByTagNameNS(
                        "http://www.sap.com/abapxml/inactiveCtsObjects", "object").item(0);

                if (objectElement == null) {
                    continue;
                }

                // Parse object ref (nested in object)
                // Note: <ioc:ref> element has ioc namespace, but attributes have adtcore namespace
                NodeList objectRefs = objectElement.getElementsByTagNameNS(
                        "http://www.sap.com/abapxml/inactiveCtsObjects", "ref");
                if (objectRefs.getLength() == 0) {
                    continue;
                }

                Element objectRef = (Element) objectRefs.item(0);

                // Attributes use adtcore namespace
                String uri = objectRef.getAttributeNS("http://www.sap.com/adt/core", "uri");
                String type = objectRef.getAttributeNS("http://www.sap.com/adt/core", "type");
                String name = objectRef.getAttributeNS("http://www.sap.com/adt/core", "name");
                String packageName = objectRef.getAttributeNS("http://www.sap.com/adt/core", "packageName");
                String parentUri = objectRef.getAttributeNS("http://www.sap.com/adt/core", "parentUri");

                if (uri != null && !uri.isEmpty()) {
                    result.add(new ObjectMetadata(uri, type, name, packageName, parentUri));
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing preaudit response", e);
            throw new RuntimeException("Failed to parse preaudit response: " + e.getMessage(), e);
        }

        logger.info("Extracted metadata for {} objects from preaudit", result.size());
        return result;
    }

    /**
     * Parse final activation response (Llamado 2).
     *
     * Response handling:
     * - Empty response = SUCCESS (objects activated without syntax errors)
     * - XML with <chkl:messages> = FAILURE (syntax errors found)
     *
     * IMPORTANT: Status code is ALWAYS 200, even with syntax errors.
     * Error detection relies on response body content only.
     */
    private ActivationResult parseActivationResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            logger.info("Activation successful (empty response)");
            return new ActivationResult(true, "Activation successful", List.of());
        }

        // Parse error messages
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.getBytes()));

            NodeList messages = doc.getElementsByTagNameNS("http://www.sap.com/abapxml/checklist", "msg");
            List<ActivationResult.ActivationError> errors = new ArrayList<>();

            for (int i = 0; i < messages.getLength(); i++) {
                Element msg = (Element) messages.item(i);

                String objDescr = msg.getAttribute("objDescr");
                String type = msg.getAttribute("type");
                int line = msg.hasAttribute("line") ? Integer.parseInt(msg.getAttribute("line")) : 0;
                String href = msg.getAttribute("href");
                boolean forceSupported = "true".equals(msg.getAttribute("forceSupported"));

                // Get short text
                Element shortTextElem = (Element) msg.getElementsByTagName("txt").item(0);
                String shortText = shortTextElem != null ? shortTextElem.getTextContent() : "";

                errors.add(new ActivationResult.ActivationError(
                        href, objDescr, type, line, href, shortText, forceSupported));
            }

            logger.warn("Activation failed with {} errors", errors.size());
            return new ActivationResult(false, "Activation failed - syntax errors", errors);

        } catch (Exception e) {
            logger.error("Error parsing activation response", e);
            return new ActivationResult(false, "Failed to parse activation response: " + e.getMessage(), List.of());
        }
    }
}

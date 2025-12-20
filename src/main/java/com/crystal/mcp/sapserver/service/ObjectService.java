package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ObjectSourceResult;
import com.crystal.mcp.sapserver.model.ObjectStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generic ABAP object operations.
 *
 * This service handles generic ABAP object operations that work across
 * all object types (classes, programs, function groups, interfaces, etc.).
 *
 * Implements Progressive Discovery Stage 3:
 * - Stage 1: search_objects (SearchService) → Find objects
 * - Stage 2: get_object_structure (ObjectService) → Get metadata
 * - Stage 3: get_object_source (ObjectService) → Get full source code
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 *
 * Supported operations:
 * - Get object source code by URI
 * - Get object structure/metadata
 *
 * Future operations:
 * - Lock/unlock objects for editing
 * - Set object source (update code)
 * - Get object attributes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectService {

    private final RfcAdapter rfcAdapter;

    /**
     * Get structure/metadata for any ABAP object without source code.
     *
     * This is Progressive Discovery Stage 2: Get metadata before fetching full
     * source.
     * Returns object components (methods, attributes, includes) without source
     * code.
     *
     * Progressive Discovery Stage 2:
     * - Use after search_objects (Stage 1) identifies objects
     * - Provides metadata to confirm it's the right object
     * - Avoids fetching full source (saves ~2,000+ tokens)
     * - Use get_object_source (Stage 3) only if source is needed
     *
     * Token Optimization:
     * - Stage 1 (search): ~500 tokens → Find objects
     * - Stage 2 (structure): ~800 tokens → Get metadata (THIS)
     * - Stage 3 (source): ~3,000+ tokens → Get full code
     *
     * ADT API Endpoint Pattern:
     * Appends /objectstructure to the base URI.
     *
     * Examples:
     * - Classes: /sap/bc/adt/oo/classes/{name}/objectstructure
     * - Programs: /sap/bc/adt/programs/programs/{name}/objectstructure
     * - Function Groups: /sap/bc/adt/functions/groups/{name}/objectstructure
     *
     * @param objectUri ADT URI for the object (obtained from search results)
     * @return ObjectStructure containing metadata and components
     * @throws RuntimeException if object not found or access fails
     */
    public ObjectStructure getObjectStructure(String objectUri) {
        // Validate inputs
        if (objectUri == null || objectUri.trim().isEmpty()) {
            throw new IllegalArgumentException("Object URI cannot be empty");
        }

        // Append /objectstructure if not present
        String structureUri = objectUri;
        if (!structureUri.endsWith("/objectstructure")) {
            structureUri = objectUri + "/objectstructure";
        }

        log.info("Fetching structure for object URI: {}", structureUri);

        try {
            // Execute RFC request
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    structureUri,
                    "GET",
                    null,
                    new HashMap<>(),
                    "",
                    "application/xml");

            // Check HTTP status
            if (response.statusCode() == 200) {
                log.debug("Successfully retrieved structure ({} bytes)",
                        response.text().length());

                // Parse XML response
                return parseObjectStructure(response.text(), objectUri);
            } else {
                String errorMsg = String.format(
                        "Failed to get object structure: HTTP %d - %s",
                        response.statusCode(),
                        response.text());
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error fetching object structure for URI '{}': {}",
                    objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve object structure", e);
        }
    }

    /**
     * Parse object structure from ADT XML response.
     *
     * Handles SAP namespaces (adtcore, atom, abapsource) and extracts:
     * - Object metadata (name, type, description)
     * - Components (methods, attributes, includes)
     * - Links (documentation, related resources)
     *
     * @param xml       XML response from ADT API
     * @param objectUri Original object URI
     * @return ObjectStructure with parsed data
     * @throws Exception if XML parsing fails
     */
    private ObjectStructure parseObjectStructure(String xml, String objectUri) throws Exception {
        // SAP ADT namespaces
        final String NS_ADTCORE = "http://www.sap.com/adt/core";
        final String NS_ATOM = "http://www.w3.org/2005/Atom";
        final String NS_ABAPSOURCE = "http://www.sap.com/adt/abapsource";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));

        Element root = doc.getDocumentElement();

        // Extract basic metadata
        String name = getElementText(root, "name", NS_ADTCORE);
        String type = getElementText(root, "type", NS_ADTCORE);
        String description = getElementText(root, "description", NS_ADTCORE);

        // Extract links
        List<ObjectStructure.Link> links = new ArrayList<>();
        NodeList linkNodes = root.getElementsByTagNameNS(NS_ATOM, "link");
        for (int i = 0; i < linkNodes.getLength(); i++) {
            Element linkElement = (Element) linkNodes.item(i);
            String rel = linkElement.getAttribute("rel");
            String href = linkElement.getAttribute("href");
            if (rel != null && !rel.isEmpty() && href != null && !href.isEmpty()) {
                links.add(new ObjectStructure.Link(rel, href));
            }
        }

        // Extract components (methods, attributes, etc.)
        List<ObjectStructure.Component> components = new ArrayList<>();
        NodeList componentNodes = root.getElementsByTagNameNS(NS_ADTCORE, "component");
        for (int i = 0; i < componentNodes.getLength(); i++) {
            Element compElement = (Element) componentNodes.item(i);

            String compName = getElementText(compElement, "name", NS_ADTCORE);
            String compType = getElementText(compElement, "type", NS_ADTCORE);
            String compUri = getElementText(compElement, "uri", NS_ADTCORE);
            String compDescription = getElementText(compElement, "description", NS_ADTCORE);

            // Extract component-specific links
            List<ObjectStructure.Link> compLinks = new ArrayList<>();
            NodeList compLinkNodes = compElement.getElementsByTagNameNS(NS_ATOM, "link");
            for (int j = 0; j < compLinkNodes.getLength(); j++) {
                Element linkElement = (Element) compLinkNodes.item(j);
                String rel = linkElement.getAttribute("rel");
                String href = linkElement.getAttribute("href");
                if (rel != null && !rel.isEmpty() && href != null && !href.isEmpty()) {
                    compLinks.add(new ObjectStructure.Link(rel, href));
                }
            }

            components.add(new ObjectStructure.Component(
                    compName, compType, compUri, compDescription, compLinks));
        }

        return new ObjectStructure(name, type, objectUri, description, components, links);
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
     * Get source code for any ABAP object by URI.
     *
     * This is a generic method that works with any ADT-compatible object type.
     * Used internally by specialized services (ClassService, ProgramService, etc.)
     * and exposed as an MCP tool for direct URI-based access.
     *
     * Progressive Discovery Stage 3:
     * - Use after search_objects (Stage 1) identifies the object
     * - Use after get_object_structure (Stage 2) confirms it's the right one
     * - Only fetches source when actually needed (token optimization)
     *
     * ADT API Endpoint Pattern:
     * The URI is provided by search results or structure queries.
     *
     * Examples:
     * - Classes: /sap/bc/adt/oo/classes/{name}/source/main
     * - Programs: /sap/bc/adt/programs/programs/{name}/source/main
     * - Function Groups: /sap/bc/adt/functions/groups/{name}/source/main
     * - Interfaces: /sap/bc/adt/oo/interfaces/{name}/source/main
     *
     * @param objectUri ADT URI for the object (obtained from search or structure
     *                  queries)
     * @param version   version to retrieve ("active" or "inactive")
     * @return ObjectSourceResult containing source code and metadata
     * @throws RuntimeException if object not found or access fails
     */
    public ObjectSourceResult getObjectSource(String objectUri, String version) {
        // Validate inputs
        if (objectUri == null || objectUri.trim().isEmpty()) {
            throw new IllegalArgumentException("Object URI cannot be empty");
        }

        // Set default version
        String actualVersion = (version != null && !version.isEmpty()) ? version : "active";

        // IMPORTANT: Always append /source/main if not present
        // This ensures we get source code (text/plain) instead of metadata XML
        String sourceUri = objectUri;
        if (!sourceUri.contains("/source/main")) {
            // Remove trailing slash if present
            if (sourceUri.endsWith("/")) {
                sourceUri = sourceUri.substring(0, sourceUri.length() - 1);
            }
            sourceUri = sourceUri + "/source/main";
        }

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("version", actualVersion);

        log.info("Fetching source for object URI: {} (version: {})",
                sourceUri, actualVersion);

        try {
            // Execute RFC request using sourceUri (with /source/main)
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    sourceUri,
                    "GET",
                    null,
                    params,
                    "",
                    "text/plain");

            // Check HTTP status
            if (response.statusCode() == 200) {
                log.debug("Successfully retrieved source ({} bytes)",
                        response.text().length());

                // Build metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("uri", sourceUri); // Use sourceUri (with /source/main)
                metadata.put("responseHeaders", response.headers());
                metadata.put("sourceLength", response.text().length());

                return new ObjectSourceResult(
                        response.text(),
                        sourceUri, // Use sourceUri (with /source/main)
                        actualVersion,
                        metadata);
            } else {
                String errorMsg = String.format(
                        "Failed to get object source: HTTP %d - %s",
                        response.statusCode(),
                        response.text());
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error fetching object source for URI '{}': {}",
                    objectUri, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve object source", e);
        }
    }

    /**
     * Get list of Function Modules in a Function Group.
     *
     * Uses ADT repository/nodestructure endpoint as suggested by user.
     * POST /sap/bc/adt/repository/nodestructure
     *
     * @param fgName Function Group name
     * @return List of Function Module names
     */
    public List<String> getFunctionGroupModules(String fgName) {
        List<String> modules = new ArrayList<>();
        if (fgName == null || fgName.isEmpty())
            return modules;

        try {
            log.debug("Fetching FMs for Group {} via ADT nodestructure", fgName);

            // Build request XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("asx:abap");
            root.setAttribute("xmlns:asx", "http://www.sap.com/abapxml");
            root.setAttribute("version", "1.0");
            doc.appendChild(root);

            Element values = doc.createElement("asx:values");
            root.appendChild(values);

            Element data = doc.createElement("DATA");
            values.appendChild(data);

            addTextElement(doc, data, "TV_NODEKEY", "000000");

            String requestXml = xmlToString(doc);

            // Execute node structure request
            String nodeStructureUri = "/sap/bc/adt/repository/nodestructure";
            Map<String, String> params = Map.of(
                    "parent_name", fgName,
                    "parent_tech_name", fgName, // User suggested using same name
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

            if (response.statusCode() == 200) {
                // Parse response to find FMs
                Document responseDoc = builder
                        .parse(new ByteArrayInputStream(response.text().getBytes(StandardCharsets.UTF_8)));

                // Iterate over SEU_ADT_REPOSITORY_OBJ_NODE elements
                NodeList nodes = responseDoc.getElementsByTagName("SEU_ADT_REPOSITORY_OBJ_NODE");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element element = (Element) nodes.item(i);

                    String name = getTagValue(element, "OBJECT_NAME");
                    String type = getTagValue(element, "OBJECT_TYPE");

                    if (name != null && !name.isEmpty() && "FUGR/FF".equals(type)) {
                        modules.add(name);
                    }
                }
                log.info("Found {} FMs in Group {} via ADT nodestructure", modules.size(), fgName);
            } else {
                log.warn("ADT nodestructure returned status {}: {}", response.statusCode(), response.text());
            }

        } catch (Exception e) {
            log.warn("Failed to get modules for FG {}: {}", fgName, e.getMessage());
        }
        return modules;
    }

    private String getTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private void addTextElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        if (textContent != null && !textContent.isEmpty()) {
            element.setTextContent(textContent);
        }
        parent.appendChild(element);
    }

    private String xmlToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}

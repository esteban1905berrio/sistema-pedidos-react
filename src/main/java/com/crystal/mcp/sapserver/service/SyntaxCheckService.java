package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.SyntaxCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for ABAP syntax checking using ADT checkruns API.
 *
 * Provides syntax validation for any ABAP object type (classes, programs,
 * function modules, etc.) by calling the ADT REST endpoint:
 * POST /sap/bc/adt/checkruns?reporters=abapCheckRun
 */
@Service
public class SyntaxCheckService {

    private static final Logger logger = LoggerFactory.getLogger(SyntaxCheckService.class);

    private final RfcAdapter rfcAdapter;

    public SyntaxCheckService(RfcAdapter rfcAdapter) {
        this.rfcAdapter = rfcAdapter;
    }

    /**
     * Check syntax of an ABAP object.
     *
     * @param objectUri ADT URI of the object (e.g., "/sap/bc/adt/oo/classes/zcl_test/source/main")
     * @param version Version to check: "active" or "inactive" (default: "inactive")
     * @return SyntaxCheckResult with messages and status
     * @throws RuntimeException if check fails
     */
    public SyntaxCheckResult checkSyntax(String objectUri, String version) {
        logger.info("Checking syntax for object: {} (version: {})", objectUri, version);

        String effectiveVersion = (version == null || version.isEmpty()) ? "inactive" : version;

        try {
            // Build request XML
            String requestXml = buildCheckRequestXml(objectUri, effectiveVersion);
            logger.debug("Request XML: {}", requestXml);

            // Call ADT endpoint
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/vnd.sap.adt.checkmessages+xml");

            RfcAdapter.RfcResponse response = rfcAdapter.request(
                "/sap/bc/adt/checkruns?reporters=abapCheckRun",
                "POST",
                headers,
                null, // no query params
                requestXml,
                "application/vnd.sap.adt.checkobjects+xml"
            );

            // Validate response
            if (response.statusCode() != 200) {
                throw new RuntimeException("Syntax check failed with status " + response.statusCode() +
                    ": " + response.text());
            }

            // Parse response XML
            return parseCheckResponse(response.text(), objectUri, effectiveVersion);

        } catch (Exception e) {
            logger.error("Failed to check syntax for object: {}", objectUri, e);
            throw new RuntimeException("Syntax check failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build XML request body for checkruns API.
     */
    private String buildCheckRequestXml(String objectUri, String version) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Root element
        Element root = doc.createElement("chkrun:checkObjectList");
        root.setAttribute("xmlns:chkrun", "http://www.sap.com/adt/checkrun");
        root.setAttribute("xmlns:adtcore", "http://www.sap.com/adt/core");
        doc.appendChild(root);

        // checkObject element
        Element checkObject = doc.createElement("chkrun:checkObject");
        checkObject.setAttribute("adtcore:uri", objectUri);
        checkObject.setAttribute("chkrun:version", version);
        root.appendChild(checkObject);

        // Convert to string
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Parse XML response from checkruns API.
     */
    private SyntaxCheckResult parseCheckResponse(String xml, String objectUri, String version) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        // Get checkReport element
        NodeList reportNodes = doc.getElementsByTagNameNS("http://www.sap.com/adt/checkrun", "checkReport");
        if (reportNodes.getLength() == 0) {
            throw new RuntimeException("No checkReport element found in response");
        }

        Element reportElement = (Element) reportNodes.item(0);
        String status = reportElement.getAttribute("chkrun:status");
        String statusText = reportElement.getAttribute("chkrun:statusText");

        // Parse messages
        List<SyntaxCheckResult.CheckMessage> messages = new ArrayList<>();
        NodeList messageNodes = doc.getElementsByTagNameNS("http://www.sap.com/adt/checkrun", "checkMessage");

        boolean hasErrors = false;
        boolean hasWarnings = false;

        for (int i = 0; i < messageNodes.getLength(); i++) {
            Element messageElement = (Element) messageNodes.item(i);

            String messageUri = messageElement.getAttribute("chkrun:uri");
            String type = messageElement.getAttribute("chkrun:type");
            String shortText = messageElement.getAttribute("chkrun:shortText");

            // Parse line and column from URI fragment (e.g., "#start=133,31")
            Integer line = null;
            Integer column = null;
            if (messageUri != null && messageUri.contains("#start=")) {
                Pattern pattern = Pattern.compile("#start=(\\d+),(\\d+)");
                Matcher matcher = pattern.matcher(messageUri);
                if (matcher.find()) {
                    line = Integer.parseInt(matcher.group(1));
                    column = Integer.parseInt(matcher.group(2));
                }
            }

            SyntaxCheckResult.CheckMessage message = new SyntaxCheckResult.CheckMessage(
                messageUri, type, shortText, line, column
            );

            messages.add(message);

            if ("E".equals(type)) hasErrors = true;
            if ("W".equals(type)) hasWarnings = true;
        }

        logger.info("Syntax check completed: {} messages (errors: {}, warnings: {})",
            messages.size(), hasErrors, hasWarnings);

        return new SyntaxCheckResult(
            objectUri,
            version,
            status,
            statusText,
            messages,
            hasErrors,
            hasWarnings,
            messages.size()
        );
    }
}

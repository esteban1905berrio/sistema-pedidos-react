package com.crystal.mcp.sapserver.service;

import com.sap.conn.jco.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RFC Adapter - HTTP-to-RFC Bridge.
 *
 * This adapter mimics HTTP request/response patterns but executes requests via SAP RFC
 * using the SADT_REST_RFC_ENDPOINT function module. This allows calling ADT (ABAP Development Tools)
 * REST APIs through RFC instead of HTTP.
 *
 * Architecture Pattern:
 * - Receives HTTP-style requests (URI, method, headers, params, body)
 * - Transforms into RFC call structures for SADT_REST_RFC_ENDPOINT
 * - Parses RFC response back into HTTP-style response
 *
 * This design allows the service layer to use familiar HTTP patterns while communicating
 * with SAP via RFC, maintaining compatibility with the Python implementation's architecture.
 *
 * Thread Safety: This class is thread-safe because JCoDestination is thread-safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RfcAdapter {

    private final JCoDestination destination;

    /**
     * Execute HTTP-style request via RFC SADT_REST_RFC_ENDPOINT.
     *
     * This method replicates the Python RfcAdapter.request() behavior.
     *
     * @param uri         ADT API endpoint (e.g., "/sap/bc/adt/oo/classes/CL_TEST/source/main")
     * @param method      HTTP method (GET, POST, PUT, DELETE)
     * @param headers     custom HTTP headers (can be null)
     * @param params      query parameters (can be null)
     * @param body        request body (empty string for GET)
     * @param contentType Content-Type header value
     * @return RfcResponse containing status code, response body, and headers
     * @throws JCoException if RFC call fails
     */
    public RfcResponse request(
            String uri,
            String method,
            Map<String, String> headers,
            Map<String, String> params,
            String body,
            String contentType
    ) throws JCoException {

        String fullUri = buildUri(uri, params);
        Map<String, String> requestHeaders = buildHeaders(headers, contentType);

        log.debug("RFC Request: {} {}", method, fullUri);
        log.debug("Headers: {}", requestHeaders);

        try {
            // Get function module from repository
            JCoFunction function = destination.getRepository()
                    .getFunction("SADT_REST_RFC_ENDPOINT");

            if (function == null) {
                throw new RuntimeException(
                        "SADT_REST_RFC_ENDPOINT not found in SAP system. " +
                                "Ensure ADT is installed and user has authorization."
                );
            }

            // Build REQUEST structure
            JCoStructure request = function.getImportParameterList().getStructure("REQUEST");

            // REQUEST_LINE (method, URI, HTTP version)
            JCoStructure requestLine = request.getStructure("REQUEST_LINE");
            requestLine.setValue("METHOD", method);
            requestLine.setValue("URI", fullUri);
            requestLine.setValue("VERSION", "HTTP/1.1");

            // HEADER_FIELDS (HTTP headers)
            JCoTable headerFields = request.getTable("HEADER_FIELDS");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                headerFields.appendRow();
                headerFields.setValue("NAME", header.getKey());
                headerFields.setValue("VALUE", header.getValue());
            }

            // MESSAGE_BODY (request body, if present)
            if (body != null && !body.isEmpty()) {
                request.setValue("MESSAGE_BODY", body.getBytes(StandardCharsets.UTF_8));
                log.debug("Request body length: {} bytes", body.length());
            }

            // Execute RFC call
            long startTime = System.currentTimeMillis();
            function.execute(destination);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("RFC call completed in {} ms", duration);

            // Parse RESPONSE structure
            JCoStructure response = function.getExportParameterList().getStructure("RESPONSE");

            return parseResponse(response);

        } catch (JCoException e) {
            log.error("RFC call failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Builds full URI with query parameters.
     *
     * @param uri    base URI
     * @param params query parameters
     * @return full URI with query string
     */
    private String buildUri(String uri, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return uri;
        }

        StringBuilder queryString = new StringBuilder(uri);
        queryString.append("?");

        params.forEach((key, value) -> {
            if (value != null) {
                queryString.append(key).append("=").append(value).append("&");
            }
        });

        // Remove trailing &
        if (queryString.charAt(queryString.length() - 1) == '&') {
            queryString.setLength(queryString.length() - 1);
        }

        return queryString.toString();
    }

    /**
     * Builds HTTP headers map with defaults.
     *
     * Replicates Python RfcAdapter header logic:
     * - Accept: content-type or *\/*
     * - Content-Type: specified content type
     * - Cache-Control: no-cache
     * - X-sap-adt-sessiontype: stateless
     *
     * @param headers     custom headers
     * @param contentType Content-Type value
     * @return merged headers map
     */
    private Map<String, String> buildHeaders(Map<String, String> headers, String contentType) {
        Map<String, String> requestHeaders = new HashMap<>();

        // Default Accept header
        String acceptType = contentType.startsWith("application/vnd.sap.adt") ||
                contentType.equals("text/plain")
                ? contentType
                : "*/*";

        requestHeaders.put("Accept", acceptType);
        requestHeaders.put("Cache-Control", "no-cache");
        requestHeaders.put("Content-Type", contentType);
        requestHeaders.put("X-sap-adt-sessiontype", "stateless");

        // Merge custom headers (overrides defaults)
        if (headers != null) {
            requestHeaders.putAll(headers);
        }

        return requestHeaders;
    }

    /**
     * Parses RFC response structure into RfcResponse record.
     *
     * Extracts:
     * - HTTP status code from STATUS_LINE
     * - Response body from MESSAGE_BODY
     * - Response headers from HEADER_FIELDS table
     *
     * @param response RESPONSE structure from SADT_REST_RFC_ENDPOINT
     * @return parsed RfcResponse
     */
    private RfcResponse parseResponse(JCoStructure response) {
        // STATUS_LINE - extract status code
        JCoStructure statusLine = response.getStructure("STATUS_LINE");
        int statusCode = Integer.parseInt(statusLine.getString("STATUS_CODE").trim());

        // MESSAGE_BODY - extract response body
        byte[] messageBodyBytes = response.getByteArray("MESSAGE_BODY");
        String messageBody = new String(messageBodyBytes, StandardCharsets.UTF_8);

        // HEADER_FIELDS - extract response headers
        JCoTable headerFields = response.getTable("HEADER_FIELDS");
        Map<String, String> responseHeaders = new HashMap<>();

        for (int i = 0; i < headerFields.getNumRows(); i++) {
            headerFields.setRow(i);
            String name = headerFields.getString("NAME");
            String value = headerFields.getString("VALUE");
            if (name != null && !name.isEmpty()) {
                responseHeaders.put(name, value);
            }
        }

        log.debug("RFC Response: Status {}", statusCode);
        log.debug("Response body length: {} bytes", messageBody.length());

        return new RfcResponse(statusCode, messageBody, responseHeaders);
    }

    /**
     * Response wrapper record (mimics Python RfcResponse).
     *
     * Immutable record containing:
     * - statusCode: HTTP status code (200, 404, 500, etc.)
     * - text: response body as string
     * - headers: response headers map
     */
    public record RfcResponse(
            int statusCode,
            String text,
            Map<String, String> headers
    ) {
    }
}

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
 * Stateful Connection Support (Phase 1 Implementation):
 * - Supports stateful workflows via JCoContext (SAP JCo feature)
 * - Required for LOCK → MODIFY → UNLOCK workflows
 * - Uses ThreadLocal to track active contexts per thread
 * - Prevents nested contexts and memory leaks
 *
 * Thread Safety: This class is thread-safe because JCoDestination is thread-safe
 * and stateful contexts are isolated per thread via ThreadLocal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RfcAdapter {

    private final JCoDestination destination;

    /**
     * ThreadLocal para rastrear contextos stateful activos.
     *
     * Cada thread mantiene su propio flag booleano indicando si tiene
     * un contexto JCoContext activo. Esto previene:
     * - Contextos anidados (no permitidos en JCo)
     * - Memory leaks por contextos no cerrados
     * - Confusión de estado entre threads
     */
    private static final ThreadLocal<Boolean> statefulContextActive =
            ThreadLocal.withInitial(() -> false);

    /**
     * Inicia un contexto stateful para workflows que requieren sesión única SAP.
     *
     * Un contexto stateful garantiza que todas las llamadas RFC ejecutadas después
     * de beginStatefulContext() usan la MISMA sesión SAP hasta que se llame
     * endStatefulContext().
     *
     * Casos de Uso:
     * - LOCK → MODIFY → UNLOCK workflows (requiere mantener bloqueo)
     * - Transacciones multi-paso
     * - Cualquier workflow que requiera estado persistente entre llamadas
     *
     * Patrón de Uso (OBLIGATORIO):
     * <pre>
     * {@code
     * rfcAdapter.beginStatefulContext();
     * try {
     *     // Todas las llamadas aquí usan la MISMA sesión SAP
     *     rfcAdapter.request(...);  // Llamada 1
     *     rfcAdapter.request(...);  // Llamada 2 (misma sesión que 1)
     *     rfcAdapter.request(...);  // Llamada 3 (misma sesión que 1 y 2)
     * } finally {
     *     // SIEMPRE terminar el contexto
     *     rfcAdapter.endStatefulContext();
     * }
     * }
     * </pre>
     *
     * IMPORTANTE:
     * - SIEMPRE usar try-finally para garantizar endStatefulContext()
     * - NO llamar para operaciones de solo lectura (get*, search*, list*)
     * - NO anidar contextos (lanza IllegalStateException)
     * - Contexto es Thread-Local (cada thread tiene su propio contexto)
     *
     * @throws IllegalStateException si ya existe un contexto activo en este thread
     * @throws JCoException si falla la inicialización del contexto JCo
     *
     * @see #endStatefulContext()
     * @see JCoContext#begin(JCoDestination)
     */
    public void beginStatefulContext() throws JCoException {
        if (statefulContextActive.get()) {
            throw new IllegalStateException(
                    "Stateful context already active in this thread. " +
                    "Nested stateful contexts are not allowed. " +
                    "Ensure endStatefulContext() was called before starting a new context."
            );
        }

        log.debug("Beginning stateful context (thread: {})",
                Thread.currentThread().getName());

        JCoContext.begin(destination);
        statefulContextActive.set(true);

        log.debug("Stateful context started successfully");
    }

    /**
     * Finaliza el contexto stateful y libera la sesión SAP.
     *
     * CRÍTICO: Este método SIEMPRE debe llamarse en un bloque finally para
     * evitar memory leaks y sesiones SAP huérfanas.
     *
     * El método es graceful - si no hay contexto activo, simplemente registra
     * un warning y retorna sin error. Esto permite usar safely en finally
     * incluso si beginStatefulContext() nunca se llamó.
     *
     * Comportamiento:
     * - Si hay contexto activo: lo termina y libera sesión SAP
     * - Si NO hay contexto activo: warning y retorna (no falla)
     * - SIEMPRE limpia el ThreadLocal flag (previene memory leaks)
     *
     * @throws JCoException si falla la finalización del contexto JCo
     *
     * @see #beginStatefulContext()
     * @see JCoContext#end(JCoDestination)
     */
    public void endStatefulContext() throws JCoException {
        if (!statefulContextActive.get()) {
            log.warn("Attempted to end stateful context when none is active " +
                    "(thread: {}). This is safe but indicates a logic issue.",
                    Thread.currentThread().getName());
            return; // Graceful degradation
        }

        try {
            log.debug("Ending stateful context (thread: {})",
                    Thread.currentThread().getName());

            JCoContext.end(destination);

            log.debug("Stateful context ended successfully");

        } finally {
            // SIEMPRE limpiar flag, incluso si end() falla
            // Esto previene memory leaks en ThreadLocal
            statefulContextActive.set(false);
        }
    }

    /**
     * Verifica si hay un contexto stateful activo en el thread actual.
     *
     * Útil para:
     * - Debugging de workflows complejos
     * - Validación en tests unitarios
     * - Logging condicional
     *
     * @return true si hay un contexto JCoContext activo, false si no
     */
    public boolean isStatefulContextActive() {
        return statefulContextActive.get();
    }

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
        //requestHeaders.put("Cache-Control", "no-cache");
        //requestHeaders.put("Content-Type", contentType);
        //requestHeaders.put("X-sap-adt-sessiontype", "stateless");

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

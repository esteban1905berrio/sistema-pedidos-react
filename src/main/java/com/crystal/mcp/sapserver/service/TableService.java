package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.DictionaryObjectRequest;
import com.crystal.mcp.sapserver.model.DictionaryObjectResult;
import com.crystal.mcp.sapserver.service.StatefulModificationService.LockResult;
import com.sap.conn.jco.JCoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para creación de tablas en SAP Data Dictionary.
 *
 * <p>Implementa el workflow completo para crear tablas transparentes (TABL/DT):
 * <ol>
 *   <li>Validate input (nombre, package, fields, transport)</li>
 *   <li>Generate DDL from fields[] (via DdlGenerator)</li>
 *   <li>POST /ddic/tables (create object)</li>
 *   <li>Stateful workflow: LOCK → MODIFY → UNLOCK</li>
 * </ol>
 *
 * <p><b>Reutiliza infraestructura existente:</b>
 * <ul>
 *   <li>StatefulModificationService - Para lock management y contexto stateful</li>
 *   <li>RfcAdapter - Para llamadas ADT vía RFC</li>
 *   <li>DdlGenerator - Para generación de DDL desde fields[]</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> Thread-safe via StatefulModificationService's ThreadLocal context management.
 *
 * @author Crystal Development Team
 * @since 1.0
 * @see StatefulModificationService
 * @see DdlGenerator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableService {

    private final StatefulModificationService statefulModificationService;
    private final RfcAdapter rfcAdapter;
    private final DdlGenerator ddlGenerator;

    /**
     * Crea una nueva tabla en SAP Data Dictionary.
     *
     * <p>Ejecuta el workflow completo: CREATE → LOCK → MODIFY → UNLOCK.
     *
     * @param request Request DTO con nombre, descripción, fields, package, transport
     * @return Resultado con URI, versión, transport, etc.
     * @throws IllegalArgumentException si la validación falla
     * @throws RuntimeException si hay error en SAP (envuelve JCoException)
     */
    public DictionaryObjectResult createTable(DictionaryObjectRequest request) {
        log.info("Creating table: {}", request.getName());

        // 1. Validate input
        request.validate();

        // 2. Generate DDL from fields[]
        String ddl = ddlGenerator.generateTableDdl(
                request.getName(),
                request.getDescription(),
                request.getFields()
        );
        log.debug("Generated DDL for table {}: {} chars", request.getName(), ddl.length());

        // 3. POST /ddic/tables (create object)
        String objectUri = createTableObject(
                request.getName(),
                request.getDescription(),
                request.getPackageName()
        );
        log.info("Table object created: {}", objectUri);

        // 4. Stateful workflow: LOCK → MODIFY → UNLOCK
        return statefulModificationService.executeStatefulWorkflow(
                request.getName(),
                () -> {
                    LockResult lock = statefulModificationService.lockObject(objectUri);
                    log.debug("Table {} locked with handle: {}", request.getName(), lock.lockHandle());

                    try {
                        // Modify table source (set DDL)
                        setTableSource(objectUri, ddl, lock.lockHandle());
                        log.info("Table {} source modified successfully", request.getName());

                        // Build result
                        return buildResult(request, objectUri, lock);

                    } finally {
                        // ALWAYS unlock, even on error
                        statefulModificationService.unlockObject(objectUri, lock.lockHandle());
                        log.debug("Table {} unlocked", request.getName());
                    }
                }
        );
    }

    /**
     * Crea el objeto de tabla en SAP (paso 1: POST /ddic/tables).
     *
     * <p>Envía el XML payload inicial con metadata básica de la tabla.
     * SAP crea el objeto con source vacío (solo campo "client").
     *
     * @param tableName Nombre de la tabla
     * @param description Descripción
     * @param packageName Paquete SAP
     * @return URI ADT del objeto creado (e.g., "/sap/bc/adt/ddic/tables/ytmp_1")
     * @throws RuntimeException si hay error en la creación
     */
    private String createTableObject(String tableName, String description, String packageName) {
        log.debug("Creating table object in SAP: {} in package {}", tableName, packageName);

        // Build XML payload
        String xmlPayload = buildCreateTableXml(tableName, description, packageName);

        // Headers específicos para creación de tablas
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.sap.adt.blues.v1+xml, application/vnd.sap.adt.tables.v2+xml");

        try {
            // Call SAP ADT REST API via RFC
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    "/sap/bc/adt/ddic/tables",
                    "POST",
                    headers,
                    null, // no query params
                    xmlPayload,
                    "application/vnd.sap.adt.tables.v2+xml"
            );

            // Check status
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new RuntimeException("Failed to create table: HTTP " + response.statusCode());
            }

            // Parse response to extract object URI
            return parseObjectUriFromResponse(response.text(), tableName);

        } catch (JCoException e) {
            log.error("Failed to create table object {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to create table object: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el XML payload para POST /ddic/tables.
     *
     * <p>Formato ADT v2:
     * <pre>{@code
     * <blue:blueSource xmlns:blue="http://www.sap.com/wbobj/blue"
     *                  xmlns:adtcore="http://www.sap.com/adt/core"
     *                  adtcore:name="YTMP_1"
     *                  adtcore:type="TABL/DT"
     *                  adtcore:description="Temp"
     *                  adtcore:language="EN"
     *                  adtcore:masterLanguage="EN">
     *   <adtcore:packageRef adtcore:name="$TMP"/>
     * </blue:blueSource>
     * }</pre>
     *
     * @param tableName Nombre de la tabla (uppercase)
     * @param description Descripción
     * @param packageName Paquete
     * @return XML string
     */
    private String buildCreateTableXml(String tableName, String description, String packageName) {
        String tableNameUpper = tableName.toUpperCase();
        String escapedDescription = escapeXml(description);

        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<blue:blueSource xmlns:blue=\"http://www.sap.com/wbobj/blue\" " +
                "xmlns:adtcore=\"http://www.sap.com/adt/core\" " +
                "adtcore:name=\"%s\" " +
                "adtcore:type=\"TABL/DT\" " +
                "adtcore:description=\"%s\" " +
                "adtcore:language=\"EN\" " +
                "adtcore:masterLanguage=\"EN\">\n" +
                "  <adtcore:packageRef adtcore:name=\"%s\"/>\n" +
                "</blue:blueSource>",
                tableNameUpper,
                escapedDescription,
                packageName
        );
    }

    /**
     * Parsea el URI del objeto desde la respuesta ADT XML.
     *
     * <p>La respuesta contiene un link con href="./ytmp_1/source/main".
     * Extraemos el nombre de la tabla y construimos el URI completo.
     *
     * @param xmlResponse Respuesta XML de SAP
     * @param tableName Nombre de la tabla (para construir URI)
     * @return URI ADT completo (e.g., "/sap/bc/adt/ddic/tables/ytmp_1")
     * @throws RuntimeException si no se puede parsear la respuesta
     */
    private String parseObjectUriFromResponse(String xmlResponse, String tableName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8))
            );

            // Extraer atributo adtcore:name del elemento raíz
            Element root = doc.getDocumentElement();
            String nameAttr = root.getAttributeNS("http://www.sap.com/adt/core", "name");

            if (nameAttr != null && !nameAttr.isEmpty()) {
                // Construir URI completo
                return "/sap/bc/adt/ddic/tables/" + nameAttr.toLowerCase();
            }

            // Fallback: construir desde nombre original
            return "/sap/bc/adt/ddic/tables/" + tableName.toLowerCase();

        } catch (Exception e) {
            log.error("Failed to parse object URI from response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse object URI: " + e.getMessage(), e);
        }
    }

    /**
     * Modifica el source de la tabla (PUT /ddic/tables/{name}/source/main).
     *
     * <p>Envía el DDL completo generado por DdlGenerator.
     * Requiere lockHandle obtenido en el paso de LOCK.
     *
     * @param objectUri URI del objeto (e.g., "/sap/bc/adt/ddic/tables/ytmp_1")
     * @param ddl DDL completo a establecer
     * @param lockHandle Lock handle del LOCK previo
     * @throws RuntimeException si hay error en la modificación
     */
    private void setTableSource(String objectUri, String ddl, String lockHandle) {
        log.debug("Setting table source: {} chars, lockHandle: {}", ddl.length(), lockHandle);

        // Construir URI con lockHandle
        String sourceUri = objectUri + "/source/main?lockHandle=" + lockHandle;

        // Headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/plain");

        try {
            // Call SAP ADT REST API via RFC
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    sourceUri,
                    "PUT",
                    headers,
                    null, // no query params (lockHandle ya está en URI)
                    ddl,
                    "text/plain; charset=utf-8"
            );

            // Check status
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to set table source: HTTP " + response.statusCode());
            }

            log.debug("Table source set successfully, response length: {}", response.text().length());

        } catch (JCoException e) {
            log.error("Failed to set table source: {}", e.getMessage());
            throw new RuntimeException("Failed to set table source: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el resultado final.
     *
     * @param request Request original
     * @param objectUri URI del objeto creado
     * @param lock Resultado del LOCK (contiene transport, etc.)
     * @return DTO con información del objeto creado
     */
    private DictionaryObjectResult buildResult(DictionaryObjectRequest request,
                                                String objectUri,
                                                LockResult lock) {
        return DictionaryObjectResult.builder()
                .uri(objectUri)
                .name(request.getName().toUpperCase())
                .version("inactive") // Los objetos recién creados están inactive
                .packageName(request.getPackageName())
                .transport(lock.transportNumber())
                .isLocal(lock.isLocal())
                .message(String.format(
                        "Table %s created successfully in package %s%s",
                        request.getName().toUpperCase(),
                        request.getPackageName(),
                        lock.isLocal() ? " (local)" : " with transport " + lock.transportNumber()
                ))
                .build();
    }

    /**
     * Escapa caracteres especiales XML.
     *
     * @param str String a escapar
     * @return String escapado
     */
    private String escapeXml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

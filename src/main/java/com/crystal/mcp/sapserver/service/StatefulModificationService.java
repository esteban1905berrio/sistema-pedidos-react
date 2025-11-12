package com.crystal.mcp.sapserver.service;

import com.sap.conn.jco.JCoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Base service for stateful modification workflows.
 *
 * Provides centralized infrastructure for LOCK → MODIFY → UNLOCK workflows
 * that require stateful connections (JCoContext).
 *
 * All modification services (ClassService, ProgramService, FutureServices) should use
 * this component to execute stateful workflows instead of implementing their own
 * lock/unlock logic.
 *
 * Benefits:
 * - Eliminates code duplication across services
 * - Centralizes LOCK/UNLOCK logic and XML parsing
 * - Automatic stateful context management (JCoContext)
 * - Consistent error handling and logging
 * - Thread-safe via RfcAdapter's ThreadLocal context management
 *
 * Usage Pattern:
 * <pre>
 * {@code
 * @Service
 * public class ProgramService {
 *     private final StatefulModificationService statefulModificationService;
 *
 *     public ProgramModifyResult modifyProgram(...) {
 *         return statefulModificationService.executeStatefulWorkflow(
 *             programName,
 *             () -> {
 *                 LockResult lock = statefulModificationService.lockObject(uri);
 *                 try {
 *                     // Modify object
 *                     setObjectSource(...);
 *                     return buildResult(...);
 *                 } finally {
 *                     statefulModificationService.unlockObject(uri, lock.lockHandle());
 *                 }
 *             }
 *         );
 *     }
 * }
 * }
 * </pre>
 *
 * Thread Safety: Thread-safe via RfcAdapter's ThreadLocal context management.
 *
 * @see RfcAdapter#beginStatefulContext()
 * @see RfcAdapter#endStatefulContext()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatefulModificationService {

    private final RfcAdapter rfcAdapter;

    /**
     * Resultado del LOCK con datos parseados de ADT.
     *
     * Contiene toda la información retornada por el endpoint de LOCK:
     * - lockHandle: Identificador único del bloqueo (requerido para MODIFY y UNLOCK)
     * - transportNumber: Número de transport request asignado
     * - transportUser: Usuario dueño del transport
     * - transportDescription: Descripción del transport
     * - isLocal: Indica si el objeto es local ($TMP) o transportable
     *
     * @param lockHandle identificador único del bloqueo SAP
     * @param transportNumber número de transport request (e.g., "CADK910827")
     * @param transportUser usuario dueño del transport
     * @param transportDescription descripción del transport
     * @param isLocal true si objeto es local ($TMP), false si es transportable
     */
    public record LockResult(
            String lockHandle,
            String transportNumber,
            String transportUser,
            String transportDescription,
            boolean isLocal
    ) {}

    /**
     * Functional interface para workflows stateful.
     *
     * Permite pasar lógica personalizada que se ejecuta dentro del contexto stateful.
     * El workflow tiene acceso completo a los métodos de esta clase (lockObject, unlockObject)
     * y al RfcAdapter.
     *
     * @param <T> tipo de resultado del workflow
     */
    @FunctionalInterface
    public interface StatefulWorkflow<T> {
        /**
         * Ejecuta el workflow stateful.
         *
         * Este método se ejecuta dentro de un contexto JCoContext activo,
         * lo que garantiza que todas las llamadas RFC usen la misma sesión SAP.
         *
         * @return resultado del workflow
         * @throws Exception cualquier error durante ejecución
         */
        T execute() throws Exception;
    }

    /**
     * Ejecuta un workflow stateful completo.
     *
     * Maneja automáticamente:
     * - Inicio de contexto JCoContext (begin)
     * - Ejecución del workflow custom
     * - Fin de contexto (end) en finally block
     * - Logging y error handling
     *
     * Garantías:
     * - Contexto stateful SIEMPRE se termina (incluso si workflow falla)
     * - Todas las llamadas en el workflow usan la MISMA sesión SAP
     * - ThreadLocal se limpia correctamente (no memory leaks)
     *
     * Patrón de uso:
     * <pre>
     * {@code
     * ModifyResult result = statefulModificationService.executeStatefulWorkflow(
     *     "ZCL_TEST",
     *     () -> {
     *         LockResult lock = statefulModificationService.lockObject(objectUri);
     *         try {
     *             setObjectSource(sourceUri, newSource, lock.lockHandle(), transport);
     *             return buildSuccessResult(lock);
     *         } finally {
     *             statefulModificationService.unlockObject(objectUri, lock.lockHandle());
     *         }
     *     }
     * );
     * }
     * </pre>
     *
     * @param objectName nombre del objeto (para logging)
     * @param workflow   lógica del workflow a ejecutar
     * @param <T>        tipo de resultado
     * @return resultado del workflow
     * @throws RuntimeException si falla el workflow
     */
    public <T> T executeStatefulWorkflow(
            String objectName,
            StatefulWorkflow<T> workflow
    ) {
        log.info("Starting stateful modification workflow for: {}", objectName);
        long startTime = System.currentTimeMillis();

        try {
            // INICIAR CONTEXTO STATEFUL
            rfcAdapter.beginStatefulContext();

            try {
                // EJECUTAR WORKFLOW
                T result = workflow.execute();

                long duration = System.currentTimeMillis() - startTime;
                log.info("Stateful workflow completed successfully for {} in {} ms",
                        objectName, duration);

                return result;

            } finally {
                // TERMINAR CONTEXTO (siempre)
                try {
                    rfcAdapter.endStatefulContext();
                } catch (JCoException e) {
                    log.error("Failed to end stateful context for {}: {}",
                            objectName, e.getMessage());
                    // No re-lanzar: el workflow ya completó
                }
            }

        } catch (Exception e) {
            log.error("Stateful workflow failed for {}: {}",
                    objectName, e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to execute stateful modification workflow for " + objectName,
                    e
            );
        }
    }

    /**
     * Bloquea un objeto ABAP para modificación.
     *
     * Ejecuta POST {uri}?_action=LOCK&accessMode=MODIFY
     * Parsea respuesta XML de ADT para extraer lockHandle y transport.
     *
     * Headers ADT requeridos:
     * - Accept: com.sap.adt.lock.result (versión 0.8 y 0.9)
     * - User-Agent: Eclipse ADT emulation
     * - X-sap-adt-profiling: server-time
     *
     * Respuesta ADT (XML):
     * <pre>
     * {@code
     * <asx:abap>
     *   <asx:values>
     *     <DATA>
     *       <LOCK_HANDLE>ABC123...</LOCK_HANDLE>
     *       <CORRNR>CADK910827</CORRNR>
     *       <CORRUSER>USER</CORRUSER>
     *       <CORRTEXT>Description</CORRTEXT>
     *       <IS_LOCAL/>
     *     </DATA>
     *   </asx:values>
     * </asx:abap>
     * }
     * </pre>
     *
     * Casos de Error:
     * - HTTP 423: Objeto ya bloqueado por otro usuario
     * - HTTP 401/403: Sin permisos para bloquear
     * - HTTP 404: Objeto no existe
     * - HTTP 500: Error interno SAP
     *
     * @param objectUri URI del objeto ADT (e.g., /sap/bc/adt/oo/classes/ZCL_TEST)
     * @return LockResult con lockHandle y datos del transport
     * @throws RuntimeException si falla el lock (objeto ya bloqueado, sin permisos, etc.)
     */
    public LockResult lockObject(String objectUri) {
        log.debug("Locking object: {}", objectUri);

        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("_action", "LOCK");
        params.put("accessMode", "MODIFY");

        // Headers ADT específicos (requeridos por Eclipse ADT protocol)
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept",
                "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8, " +
                        "application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9");
        headers.put("User-Agent",
                "Eclipse/4.36.0 (Java " + System.getProperty("java.version") + ") " +
                        "ADT/3.50.0 (JavaMCP)");
        headers.put("X-sap-adt-profiling", "server-time");

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "POST",
                    headers,
                    params,
                    "",  // Sin body
                    "application/xml"
            );

            if (response.statusCode() == 200) {
                return parseLockResponse(response.text());
            } else if (response.statusCode() == 423) {
                // 423 Locked - Objeto ya bloqueado por otro usuario
                throw new RuntimeException(
                        "Object is locked by another user: HTTP 423 - " + response.text()
                );
            } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                // 401/403 - Sin permisos
                throw new RuntimeException(
                        "Insufficient permissions to lock object: HTTP " +
                                response.statusCode() + " - " + response.text()
                );
            } else {
                // Otros errores
                throw new RuntimeException(
                        String.format("Lock failed: HTTP %d - %s",
                                response.statusCode(), response.text())
                );
            }

        } catch (JCoException e) {
            log.error("RFC error during lock: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to lock object: " + objectUri, e);
        }
    }

    /**
     * Desbloquea un objeto ABAP.
     *
     * Ejecuta POST {uri}?_action=UNLOCK&lockHandle={handle}
     *
     * IMPORTANTE: Siempre llamar en bloque finally para evitar bloqueos huérfanos.
     *
     * Comportamiento:
     * - HTTP 200: Unlock exitoso
     * - HTTP 4xx/5xx: Log warning pero NO lanza exception (estamos en cleanup)
     * - Exception: Log error pero NO re-lanza (estamos en cleanup)
     *
     * Razón: Este método típicamente se llama en finally blocks. Si lanzara
     * exceptions, podría ocultar la exception original del workflow.
     *
     * @param objectUri  URI del objeto ADT
     * @param lockHandle handle obtenido del LOCK
     */
    public void unlockObject(String objectUri, String lockHandle) {
        log.debug("Unlocking object: {} (handle: {})", objectUri, lockHandle);

        Map<String, String> params = new HashMap<>();
        params.put("_action", "UNLOCK");
        params.put("lockHandle", lockHandle);

        try {
            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    objectUri,
                    "POST",
                    null,  // Sin headers custom
                    params,
                    "",
                    "application/xml"
            );

            if (response.statusCode() == 200) {
                log.debug("Successfully unlocked: {}", objectUri);
            } else {
                log.warn("Unlock returned non-200 status: HTTP {} - {}",
                        response.statusCode(), response.text());
                // No lanzar exception: estamos en cleanup
            }

        } catch (Exception e) {
            log.error("Failed to unlock {} (handle: {}): {}",
                    objectUri, lockHandle, e.getMessage());
            // No re-lanzar: estamos en cleanup
        }
    }

    /**
     * Parsea respuesta XML de LOCK.
     *
     * Formato esperado (ADT LOCK response):
     * <pre>
     * {@code
     * <asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
     *   <asx:values>
     *     <DATA>
     *       <LOCK_HANDLE>ADDAAF905CB0DADC25171882FABBF2B71076E9AA</LOCK_HANDLE>
     *       <CORRNR>CADK910827</CORRNR>
     *       <CORRUSER>L_ABAPS_ITA</CORRUSER>
     *       <CORRTEXT>FI WB TRF005 Medios de pago</CORRTEXT>
     *       <IS_LOCAL/>
     *       <IS_LINK_UP/>
     *       <MODIFICATION_SUPPORT/>
     *     </DATA>
     *   </asx:values>
     * </asx:abap>
     * }
     * </pre>
     *
     * Elementos parseados:
     * - LOCK_HANDLE: Identificador único del bloqueo (obligatorio)
     * - CORRNR: Número de transport request (puede estar vacío para $TMP)
     * - CORRUSER: Usuario dueño del transport
     * - CORRTEXT: Descripción del transport
     * - IS_LOCAL: Flag indicando si objeto es local (presencia = true)
     *
     * @param xmlResponse respuesta XML de ADT
     * @return LockResult parseado
     * @throws RuntimeException si falla el parsing
     */
    private LockResult parseLockResponse(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8))
            );

            Element dataElement = (Element) doc.getElementsByTagName("DATA").item(0);

            if (dataElement == null) {
                throw new RuntimeException("Invalid LOCK response: DATA element not found");
            }

            String lockHandle = getElementText(dataElement, "LOCK_HANDLE");
            String transportNumber = getElementText(dataElement, "CORRNR");
            String transportUser = getElementText(dataElement, "CORRUSER");
            String transportDescription = getElementText(dataElement, "CORRTEXT");
            boolean isLocal = dataElement.getElementsByTagName("IS_LOCAL").getLength() > 0;

            if (lockHandle == null || lockHandle.isEmpty()) {
                throw new RuntimeException("Invalid LOCK response: LOCK_HANDLE is empty");
            }

            log.debug("Lock acquired: handle={}, transport={}, user={}, local={}",
                    lockHandle, transportNumber, transportUser, isLocal);

            return new LockResult(
                    lockHandle,
                    transportNumber,
                    transportUser,
                    transportDescription,
                    isLocal
            );

        } catch (Exception e) {
            log.error("Failed to parse lock response: {}", e.getMessage(), e);
            log.error("XML Response: {}", xmlResponse);
            throw new RuntimeException("Failed to parse lock response", e);
        }
    }

    /**
     * Helper para extraer texto de elemento XML.
     *
     * Retorna string vacío si el elemento no existe (en lugar de null).
     * Esto simplifica el manejo de elementos opcionales en el XML.
     *
     * @param parent elemento padre
     * @param tagName nombre del tag a extraer
     * @return contenido del elemento o string vacío si no existe
     */
    private String getElementText(Element parent, String tagName) {
        Element element = (Element) parent.getElementsByTagName(tagName).item(0);
        return element != null ? element.getTextContent() : "";
    }
}

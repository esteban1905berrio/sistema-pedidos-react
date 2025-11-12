# Arquitectura Centralizada para Operaciones de Modificación Stateful

**Fecha**: 2025-11-12
**Objetivo**: Centralizar lógica de LOCK/UNLOCK y gestión de conexiones stateful para todas las operaciones de modificación (Classes, Programs, Function Modules)

---

## 1. Análisis de Situación Actual

### 1.1 Duplicación de Código Identificada

**Servicios con operaciones de modificación**:
- `ClassService.modifyClass()` - Línea 252
- `ClassService.modifyClassSource()` - Línea 383
- `ProgramService.modifyProgramSource()` - Línea 276
- `ProgramService.modifyFunctionModuleSource()` - Línea 444

**Problema**: Cada servicio implementa su propia lógica de:
- LOCK (con parsing de XML)
- UNLOCK
- Manejo de errores
- **Falta**: Gestión de conexión stateful (causa del bug actual)

### 1.2 Patrón Común Identificado

Todos los métodos `modify*()` siguen el mismo workflow:

```
1. LOCK   (POST con _action=LOCK)
   └─> Parsear XML → Extraer lockHandle, transport
2. MODIFY (PUT con lockHandle y corrNr)
3. UNLOCK (POST con _action=UNLOCK)
   └─> En caso de error: unlock anyway
```

**Diferencia**: Solo el URI varía según tipo de objeto:
- Class: `/sap/bc/adt/oo/classes/{name}`
- Program: `/sap/bc/adt/programs/programs/{name}`
- FM: `/sap/bc/adt/functions/groups/{fg}/fmodules/{fm}`

---

## 2. Requisitos de la Solución

### 2.1 Funcionales

✅ **Centralización**:
- Un solo lugar para lógica LOCK/UNLOCK
- Un solo parser XML para respuestas de lock
- Un solo manejador de errores

✅ **Stateful solo para modificaciones**:
- Operaciones **modify*()**: Stateful (JCoContext)
- Operaciones **get*()**: Stateless (sin JCoContext)
- Operaciones **create*()**: Stateless (sin JCoContext)

✅ **Reutilización**:
- ClassService, ProgramService, FutureServices deben usar misma infraestructura
- No duplicar código de lock/unlock
- No duplicar manejo de stateful context

### 2.2 No Funcionales

✅ **Thread Safety**: Usar ThreadLocal para contextos stateful
✅ **Error Handling**: Unlock automático en caso de error
✅ **Memory Safety**: Siempre cerrar JCoContext en finally
✅ **Logging**: Trazabilidad completa del workflow
✅ **Extensibilidad**: Fácil agregar nuevos tipos de objetos

---

## 3. Arquitectura Propuesta

### 3.1 Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                             │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  Class     │  │  Program     │  │  Future      │        │
│  │  Service   │  │  Service     │  │  Service     │        │
│  └─────┬──────┘  └──────┬───────┘  └──────┬───────┘        │
│        │                 │                  │                 │
│        └─────────────────┼──────────────────┘                │
│                          │                                    │
│                          ▼                                    │
│         ┌────────────────────────────────────┐               │
│         │  StatefulModificationService       │               │
│         │  (Base Class / Component)          │               │
│         │                                     │               │
│         │  - executeStatefulWorkflow()       │               │
│         │  - lockObject()                    │               │
│         │  - unlockObject()                  │               │
│         │  - parseLockResponse()             │               │
│         └───────────────┬────────────────────┘               │
│                         │                                     │
└─────────────────────────┼─────────────────────────────────────┘
                          │
                          ▼
            ┌──────────────────────────────┐
            │      RfcAdapter              │
            │                              │
            │  + beginStatefulContext()    │
            │  + endStatefulContext()      │
            │  + request()                 │
            └──────────────┬───────────────┘
                           │
                           ▼
                  ┌────────────────┐
                  │   JCoContext   │
                  │   (SAP JCo)    │
                  └────────────────┘
```

---

## 4. Diseño Detallado

### 4.1 RfcAdapter - Extensión Stateful

**Archivo**: `src/main/java/.../service/RfcAdapter.java`

**Nuevas capacidades**:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RfcAdapter {

    private final JCoDestination destination;

    // ThreadLocal para rastrear contextos activos
    private static final ThreadLocal<Boolean> statefulContextActive =
        ThreadLocal.withInitial(() -> false);

    /**
     * Inicia contexto stateful para workflows que requieren sesión única.
     *
     * Uso: Operaciones LOCK → MODIFY → UNLOCK
     *
     * IMPORTANTE:
     * - Solo llamar para workflows de modificación
     * - SIEMPRE usar try-finally para garantizar endStatefulContext()
     * - NO usar para operaciones de lectura (get*) o creación
     *
     * @throws IllegalStateException si ya existe un contexto activo
     * @throws JCoException si falla la inicialización del contexto
     */
    public void beginStatefulContext() throws JCoException {
        if (statefulContextActive.get()) {
            throw new IllegalStateException(
                "Stateful context already active in this thread. " +
                "Nested contexts are not allowed."
            );
        }

        log.debug("Beginning stateful context (thread: {})",
                  Thread.currentThread().getName());

        JCoContext.begin(destination);
        statefulContextActive.set(true);
    }

    /**
     * Finaliza contexto stateful y libera sesión SAP.
     *
     * CRÍTICO: SIEMPRE llamar en bloque finally para evitar memory leaks.
     *
     * @throws IllegalStateException si no existe contexto activo
     * @throws JCoException si falla la finalización del contexto
     */
    public void endStatefulContext() throws JCoException {
        if (!statefulContextActive.get()) {
            log.warn("Attempted to end stateful context when none is active");
            return; // Graceful degradation
        }

        try {
            log.debug("Ending stateful context (thread: {})",
                      Thread.currentThread().getName());
            JCoContext.end(destination);
        } finally {
            // SIEMPRE limpiar flag, incluso si end() falla
            statefulContextActive.set(false);
        }
    }

    /**
     * Verifica si hay un contexto stateful activo.
     * Útil para debugging y validación.
     */
    public boolean isStatefulContextActive() {
        return statefulContextActive.get();
    }

    // El método request() existente NO cambia
    public RfcResponse request(...) { ... }
}
```

**Ventajas**:
- ✅ API simple y clara
- ✅ Thread-safe (ThreadLocal)
- ✅ Validación de estado
- ✅ Graceful degradation en end()

---

### 4.2 StatefulModificationService - Clase Base

**Archivo**: `src/main/java/.../service/StatefulModificationService.java` (NUEVO)

**Responsabilidad**: Centralizar toda la lógica común de modificación stateful

```java
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
 * All modification services (ClassService, ProgramService, etc.) should use
 * this component to execute stateful workflows.
 *
 * Thread Safety: Thread-safe via RfcAdapter's ThreadLocal context management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatefulModificationService {

    private final RfcAdapter rfcAdapter;

    /**
     * Resultado del LOCK con datos parseados de ADT.
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
     *
     * @param <T> tipo de resultado del workflow
     */
    @FunctionalInterface
    public interface StatefulWorkflow<T> {
        /**
         * Ejecuta el workflow.
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
     * - Inicio de contexto JCoContext
     * - Ejecución del workflow
     * - Fin de contexto (en finally)
     * - Logging y error handling
     *
     * Patrón de uso:
     * <pre>
     * {@code
     * ModifyResult result = statefulModificationService.executeStatefulWorkflow(
     *     "ZCL_TEST",
     *     () -> {
     *         LockResult lock = lockObject(objectUri);
     *         try {
     *             setObjectSource(sourceUri, newSource, lock.lockHandle(), transport);
     *             return buildSuccessResult(lock);
     *         } finally {
     *             unlockObject(objectUri, lock.lockHandle());
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

        // Headers ADT específicos
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
                throw new RuntimeException(
                    "Object is locked by another user: HTTP 423 - " + response.text()
                );
            } else {
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
                log.warn("Unlock returned non-200: HTTP {} - {}",
                        response.statusCode(), response.text());
                // No lanzar exception: ya estamos en cleanup
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
     * Formato esperado:
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

            String lockHandle = getElementText(dataElement, "LOCK_HANDLE");
            String transportNumber = getElementText(dataElement, "CORRNR");
            String transportUser = getElementText(dataElement, "CORRUSER");
            String transportDescription = getElementText(dataElement, "CORRTEXT");
            boolean isLocal = dataElement.getElementsByTagName("IS_LOCAL").getLength() > 0;

            log.debug("Lock acquired: handle={}, transport={}, user={}",
                    lockHandle, transportNumber, transportUser);

            return new LockResult(
                lockHandle,
                transportNumber,
                transportUser,
                transportDescription,
                isLocal
            );

        } catch (Exception e) {
            log.error("Failed to parse lock response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse lock response", e);
        }
    }

    /**
     * Helper para extraer texto de elemento XML.
     */
    private String getElementText(Element parent, String tagName) {
        Element element = (Element) parent.getElementsByTagName(tagName).item(0);
        return element != null ? element.getTextContent() : "";
    }
}
```

---

### 4.3 Uso en Servicios Existentes

#### Ejemplo: ProgramService.modifyFunctionModuleSource()

**ANTES** (código actual con duplicación):

```java
public ProgramModifyResult modifyFunctionModuleSource(...) {
    // Duplicación de lockObject()
    LockResult lock = lockObject(fmUri);

    try {
        // Modificar
        setObjectSource(...);
    } finally {
        // Duplicación de unlockObject()
        unlockObject(fmUri, lock.getLockHandle());
    }
    // ❌ FALTA: JCoContext stateful
}
```

**DESPUÉS** (usando StatefulModificationService):

```java
@Service
@RequiredArgsConstructor
public class ProgramService {

    private final RfcAdapter rfcAdapter;
    private final StatefulModificationService statefulModificationService; // NUEVO

    public ProgramModifyResult modifyFunctionModuleSource(
            String functionModuleName,
            String functionGroupName,
            String newSource,
            String transport
    ) {
        String fmUri = String.format(
            "/sap/bc/adt/functions/groups/%s/fmodules/%s",
            functionGroupName.toLowerCase(),
            functionModuleName.toLowerCase()
        );

        String fmSourceUri = fmUri + "/source/main";

        // ✅ EJECUTAR EN CONTEXTO STATEFUL
        return statefulModificationService.executeStatefulWorkflow(
            functionModuleName,
            () -> {
                // LOCK (contexto stateful activo)
                StatefulModificationService.LockResult lock =
                    statefulModificationService.lockObject(fmUri);

                try {
                    // MODIFY (misma sesión que LOCK)
                    setObjectSource(
                        fmSourceUri,
                        newSource,
                        lock.lockHandle(),
                        transport != null ? transport : lock.transportNumber()
                    );

                    // BUILD RESULT
                    return buildSuccessResult(
                        functionModuleName,
                        "function_module",
                        lock,
                        newSource
                    );

                } finally {
                    // UNLOCK (misma sesión, siempre ejecutar)
                    statefulModificationService.unlockObject(fmUri, lock.lockHandle());
                }
            }
        );
    }

    // setObjectSource() no cambia
    private void setObjectSource(...) { ... }

    // buildSuccessResult() - método helper simplificado
    private ProgramModifyResult buildSuccessResult(...) { ... }
}
```

**Beneficios**:
- ✅ Eliminada duplicación de lockObject()
- ✅ Eliminada duplicación de unlockObject()
- ✅ Eliminada duplicación de parseLockResponse()
- ✅ Contexto stateful manejado automáticamente
- ✅ Código más legible y mantenible
- ✅ Error handling centralizado

---

#### Ejemplo: ClassService.modifyClass()

```java
@Service
@RequiredArgsConstructor
public class ClassService {

    private final RfcAdapter rfcAdapter;
    private final StatefulModificationService statefulModificationService; // NUEVO

    public ClassModifyResult modifyClass(
            String className,
            String newSource,
            String includeType,
            String transport
    ) {
        String classUri = String.format(
            "/sap/bc/adt/oo/classes/%s",
            className.toLowerCase()
        );

        String classSourceUri = classUri + "/source/" + includeType;

        // ✅ EJECUTAR EN CONTEXTO STATEFUL
        return statefulModificationService.executeStatefulWorkflow(
            className,
            () -> {
                // LOCK
                StatefulModificationService.LockResult lock =
                    statefulModificationService.lockObject(classUri);

                try {
                    // MODIFY
                    setObjectSource(
                        classSourceUri,
                        newSource,
                        lock.lockHandle(),
                        transport != null ? transport : lock.transportNumber()
                    );

                    // BUILD RESULT
                    return buildSuccessResult(className, includeType, lock, newSource);

                } finally {
                    // UNLOCK
                    statefulModificationService.unlockObject(classUri, lock.lockHandle());
                }
            }
        );
    }
}
```

---

## 5. Comparación: Antes vs Después

### 5.1 Código Duplicado Eliminado

| Aspecto | ANTES | DESPUÉS |
|---------|-------|---------|
| **lockObject()** | Duplicado en 2+ servicios (100+ líneas cada uno) | Centralizado (1 lugar, ~50 líneas) |
| **unlockObject()** | Duplicado en 2+ servicios (30+ líneas cada uno) | Centralizado (1 lugar, ~30 líneas) |
| **parseLockResponse()** | Duplicado en 2+ servicios (50+ líneas cada uno) | Centralizado (1 lugar, ~40 líneas) |
| **Stateful context** | ❌ No implementado (causa bug) | ✅ Centralizado en RfcAdapter |
| **Error handling** | Disperso y duplicado | Centralizado en executeStatefulWorkflow() |
| **Logging** | Inconsistente entre servicios | Estandarizado |

### 5.2 Líneas de Código

**ANTES**:
- ProgramService: ~200 líneas para modify methods
- ClassService: ~200 líneas para modify methods
- **Total**: ~400 líneas con duplicación

**DESPUÉS**:
- StatefulModificationService: ~300 líneas (base centralizada)
- ProgramService: ~50 líneas por método modify (usa base)
- ClassService: ~50 líneas por método modify (usa base)
- **Total**: ~400 líneas, pero SIN duplicación y CON stateful

**Ahorro**: ~200 líneas de código duplicado eliminado

---

## 6. Matriz de Decisiones: Stateful vs Stateless

### 6.1 Criterios de Decisión

| Operación | Stateful? | Razón |
|-----------|-----------|-------|
| **modify***() | ✅ SÍ | Requiere LOCK → MODIFY → UNLOCK en misma sesión |
| **get***() | ❌ NO | Operación de solo lectura, sin estado persistente |
| **create***() | ❌ NO | Operación atómica única, sin workflow multi-paso |
| **search***() | ❌ NO | Solo lectura |
| **list***() | ❌ NO | Solo lectura |

### 6.2 Regla General

```
if (operation.isModification() && operation.requiresLock()) {
    USE statefulModificationService.executeStatefulWorkflow()
} else {
    USE rfcAdapter.request() directly (stateless)
}
```

---

## 7. Plan de Implementación

### Fase 1: Infraestructura Base
- [ ] Extender RfcAdapter con beginStatefulContext() / endStatefulContext()
- [ ] Crear StatefulModificationService con lockObject(), unlockObject(), executeStatefulWorkflow()
- [ ] Escribir unit tests para RfcAdapter stateful methods
- [ ] Escribir unit tests para StatefulModificationService

### Fase 2: Migrar ProgramService
- [ ] Refactorizar modifyFunctionModuleSource() para usar StatefulModificationService
- [ ] Refactorizar modifyProgramSource() para usar StatefulModificationService
- [ ] Eliminar métodos lockObject(), unlockObject() duplicados
- [ ] Actualizar ManualProgramModifyTest

### Fase 3: Migrar ClassService
- [ ] Refactorizar modifyClass() para usar StatefulModificationService
- [ ] Refactorizar modifyClassSource() para usar StatefulModificationService
- [ ] Eliminar métodos lockObject(), unlockObject() duplicados
- [ ] Actualizar ManualClassModifyTest

### Fase 4: Testing & Validación
- [ ] Ejecutar test manual: testModifyFunctionModule_Success()
- [ ] Verificar bloqueo en SAP (SM12)
- [ ] Verificar modificación exitosa
- [ ] Verificar transport asignado
- [ ] Verificar unlock correcto (SM12)
- [ ] Ejecutar tests para classes

### Fase 5: Documentación
- [ ] JavaDoc completo en StatefulModificationService
- [ ] Actualizar README_JAVA.md con patrón stateful
- [ ] Documentar guía de uso para futuros servicios
- [ ] Actualizar migration plan

---

## 8. Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|-----------|
| **Breaking change en servicios existentes** | Media | Alto | Refactorizar gradualmente, mantener tests |
| **Contexto stateful no cerrado** | Baja | Alto | Finally obligatorio, ThreadLocal cleanup |
| **Pool exhaustion por contextos largos** | Baja | Medio | Minimizar tiempo en workflow, timeouts |
| **Thread confusion en multithreading** | Muy baja | Medio | ThreadLocal aísla contextos |

---

## 9. Métricas de Éxito

### 9.1 Funcionales
- ✅ Bloqueos SAP persisten durante LOCK → MODIFY → UNLOCK
- ✅ Modificaciones exitosas en SAP (verificar con SE24/SE37/SE80)
- ✅ No bloqueos huérfanos (verificar SM12)
- ✅ Transport asignado correctamente

### 9.2 Técnicas
- ✅ Eliminación de ~200 líneas de código duplicado
- ✅ Cobertura de tests: 80%+
- ✅ Sin memory leaks (ThreadLocal limpio)
- ✅ Sin regresiones en operaciones get*() y create*()

---

## 10. Conclusiones

### ✅ Beneficios de la Arquitectura Centralizada

1. **DRY**: Eliminación de duplicación masiva
2. **Stateful correcto**: Soluciona bug de bloqueos
3. **Extensibilidad**: Fácil agregar nuevos servicios
4. **Mantenibilidad**: Un solo lugar para cambios
5. **Testabilidad**: Tests centralizados
6. **Logging consistente**: Trazabilidad unificada

### 📋 Próximos Pasos

1. Implementar RfcAdapter stateful methods
2. Implementar StatefulModificationService
3. Migrar ProgramService
4. Migrar ClassService
5. Testing exhaustivo
6. Documentación

---

**Documento creado**: 2025-11-12
**Autor**: Diseño Arquitectura Centralizada Stateful
**Estado**: ✅ Diseño completo - Listo para implementación

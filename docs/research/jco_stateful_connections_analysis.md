# Investigación: Conexiones Stateful en SAP JCo para Operaciones de Bloqueo

**Fecha**: 2025-11-12
**Objetivo**: Entender cómo implementar conexiones stateful en JCo para mantener bloqueos SAP durante el workflow LOCK → MODIFY → UNLOCK

---

## 1. Problema Identificado

### Descripción del Error
Los objetos ABAP no quedan bloqueados cuando se ejecuta el workflow de modificación de Function Modules:

```
LOCK (FM) → SYNTAX_CHECK → MODIFY (PUT) → UNLOCK
```

### Causa Raíz
**Conexiones Stateless vs Stateful**:

- **Stateless** (actual): Cada llamada RFC usa una conexión diferente del pool
  - LOCK se ejecuta en conexión A → bloqueo en sesión A
  - MODIFY se ejecuta en conexión B → sesión B no tiene el bloqueo
  - **Resultado**: SAP no reconoce el bloqueo en la segunda llamada

- **Stateful** (requerido): Múltiples llamadas usan la MISMA sesión SAP
  - LOCK, MODIFY, UNLOCK → todos en la misma sesión
  - **Resultado**: El bloqueo se mantiene durante todo el workflow

---

## 2. Solución JCo: JCoContext

### API Principal

```java
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;

// Iniciar contexto stateful
JCoContext.begin(destination);
try {
    // Todas las llamadas aquí usan la MISMA sesión SAP
    function1.execute(destination);
    function2.execute(destination);
    function3.execute(destination);
} finally {
    // SIEMPRE terminar el contexto
    JCoContext.end(destination);
}
```

### Características Clave

1. **Thread-Local**: El contexto es local al thread actual
2. **Sesión única**: Todas las llamadas dentro del contexto usan la misma conexión SAP
3. **Finally obligatorio**: Siempre liberar con `JCoContext.end()` para evitar memory leaks
4. **No anidable**: No se puede tener contextos anidados en el mismo thread

---

## 3. Ejemplos de Referencia Encontrados

### 3.1 StatefulCalls.java (Beginner)

**Ubicación**: `resources/jco/examples/com/sap/conn/jco/examples/client/beginner/StatefulCalls.java`

**Patrón Básico**:
```java
// Líneas 53-64
JCoContext.begin(destination);
try {
    // Múltiples llamadas que mantienen estado
    executeCalls(destination, incrementCounter, getCounter);
} finally {
    JCoContext.end(destination);
}
```

**Lección**:
- Patrón try-finally obligatorio
- Todas las llamadas dentro del try comparten sesión

---

### 3.2 StatefulJob.java (Advanced Multithreading)

**Ubicación**: `resources/jco/examples/com/sap/conn/jco/examples/client/advanced/multithreading/job/StatefulJob.java`

**Patrón Multi-Step Job**:
```java
public class StatefulJob extends StatelessJob {

    @Override
    public void runNextStep() {
        if (executedCalls == 0)
            JCoContext.begin(destination);  // Primera llamada: iniciar contexto
        super.runNextStep();                // Ejecutar operación
    }

    @Override
    public void cleanUp() {
        try {
            JCoContext.end(destination);    // Última llamada: terminar contexto
        } catch (JCoException je) {
            ex = je;
        }
        super.cleanUp();
    }
}
```

**Lecciones**:
1. **Inicialización tardía**: `begin()` solo en la primera operación
2. **Cleanup garantizado**: `end()` en método de limpieza
3. **Manejo de errores**: Catch en `end()` para prevenir exceptions no manejadas

---

### 3.3 WorkerThread.java (Session Management)

**Ubicación**: `resources/jco/examples/com/sap/conn/jco/examples/client/advanced/multithreading/WorkerThread.java`

**Patrón con Session Reference**:
```java
// Líneas 44-78
SimpleSessionReference sesRef = sessions.get(job);
if (sesRef == null) {
    sesRef = new SimpleSessionReference();
    sessions.put(job, sesRef);
}
localSessionReference.set(sesRef);  // Thread-local

try {
    job.runNextStep();
} catch (Throwable th) {
    th.printStackTrace();
}

if (job.isFinished()) {
    sesRef = sessions.remove(job);
    simpleSessionReferenceProvider.fireServerFinishedEvent(sesRef.getID());
    job.cleanUp();
} else {
    queue.add(job);  // Re-encolar para siguiente paso
}
```

**Lecciones**:
1. **Session tracking**: Usar `JCoSessionReference` para rastrear sesiones
2. **Thread-local storage**: Mantener referencia en `ThreadLocal`
3. **Multi-step jobs**: Soportar workflows con múltiples pasos
4. **Cleanup al finalizar**: Liberar sesión cuando el job termina

---

## 4. Comparación: Stateless vs Stateful

| Aspecto | Stateless (Actual) | Stateful (Requerido) |
|---------|-------------------|---------------------|
| **Conexión por llamada** | Diferente (pool) | Misma (fija) |
| **Estado entre llamadas** | ❌ No se mantiene | ✅ Se mantiene |
| **Bloqueos SAP** | ❌ Se pierden | ✅ Persisten |
| **Uso de memoria** | Bajo | Medio |
| **Concurrencia** | Alta | Media |
| **Complejidad** | Baja | Media |
| **Caso de uso** | Operaciones independientes | Workflows multi-paso |

---

## 5. Implementación Actual (RfcAdapter)

### Código Actual (Stateless)

```java
// src/main/java/.../service/RfcAdapter.java líneas 100-102
public RfcResponse request(...) throws JCoException {
    // ...
    function.execute(destination);  // ❌ Cada execute() usa conexión diferente
    // ...
}
```

**Problema**:
- Cada llamada a `request()` es independiente
- No hay contexto compartido entre LOCK y MODIFY
- Los bloqueos SAP se pierden

---

## 6. Diseño de Solución

### Opción A: Modificar RfcAdapter (Recomendada)

**Ventajas**:
- ✅ Reutiliza código existente
- ✅ Cambio mínimo en services
- ✅ API compatible con código actual

**Implementación**:
```java
public class RfcAdapter {

    // Thread-local para rastrear contextos activos
    private static final ThreadLocal<Boolean> contextActive =
        ThreadLocal.withInitial(() -> false);

    // API pública para workflows stateful
    public void beginStatefulContext() throws JCoException {
        if (contextActive.get()) {
            throw new IllegalStateException("Stateful context already active");
        }
        JCoContext.begin(destination);
        contextActive.set(true);
    }

    public void endStatefulContext() throws JCoException {
        if (!contextActive.get()) {
            throw new IllegalStateException("No active stateful context");
        }
        try {
            JCoContext.end(destination);
        } finally {
            contextActive.set(false);
        }
    }

    // request() no cambia - funciona tanto stateful como stateless
    public RfcResponse request(...) { ... }
}
```

**Uso en ProgramService**:
```java
public ProgramModifyResult modifyFunctionModuleSource(...) {
    try {
        // INICIAR CONTEXTO STATEFUL
        rfcAdapter.beginStatefulContext();

        try {
            // LOCK → MODIFY → UNLOCK (todos en misma sesión)
            LockResult lock = lockObject(fmUri);
            setObjectSource(fmSourceUri, newSource, lock.getLockHandle(), ...);
            unlockObject(fmUri, lock.getLockHandle());

        } finally {
            // TERMINAR CONTEXTO (siempre)
            rfcAdapter.endStatefulContext();
        }

    } catch (JCoException e) {
        // manejo de errores
    }
}
```

---

### Opción B: Crear StatefulRfcAdapter (Alternativa)

**Ventajas**:
- ✅ Separación de responsabilidades clara
- ✅ No afecta código stateless existente

**Desventajas**:
- ❌ Duplicación de código
- ❌ Más clases para mantener

**Implementación**:
```java
@Component
public class StatefulRfcAdapter {

    private final JCoDestination destination;
    private final RfcAdapter rfcAdapter;

    public <T> T executeInStatefulContext(StatefulWorkflow<T> workflow)
            throws JCoException {
        JCoContext.begin(destination);
        try {
            return workflow.execute(rfcAdapter);
        } finally {
            JCoContext.end(destination);
        }
    }
}

@FunctionalInterface
public interface StatefulWorkflow<T> {
    T execute(RfcAdapter adapter) throws JCoException;
}
```

**Uso**:
```java
ProgramModifyResult result = statefulRfcAdapter.executeInStatefulContext(adapter -> {
    LockResult lock = lockObject(adapter, fmUri);
    setObjectSource(adapter, fmSourceUri, newSource, ...);
    unlockObject(adapter, fmUri, lock.getLockHandle());
    return buildResult(...);
});
```

---

## 7. Consideraciones de Diseño

### Thread Safety
- ✅ `JCoContext` es thread-local → seguro para multithreading
- ✅ `JCoDestination` es thread-safe → puede compartirse entre threads
- ⚠️ Cada thread debe manejar su propio contexto stateful

### Error Handling
```java
public void endStatefulContext() throws JCoException {
    try {
        JCoContext.end(destination);
    } catch (JCoException e) {
        log.error("Failed to end stateful context: {}", e.getMessage());
        throw e;  // Re-lanzar para que caller maneje
    } finally {
        contextActive.set(false);  // Limpiar flag SIEMPRE
    }
}
```

### Memory Leaks Prevention
- **CRÍTICO**: Siempre llamar `end()` en bloque `finally`
- **CRÍTICO**: No dejar contextos abiertos en caso de exception
- **CRÍTICO**: No compartir contextos entre threads

### Connection Pool Impact
- Stateful context "reserva" una conexión del pool
- Conexión no regresa al pool hasta `end()`
- **Implicación**: Reducir tiempo en contexto stateful al mínimo necesario

---

## 8. Plan de Implementación Recomendado

### Fase 1: Extender RfcAdapter (Opción A)
1. ✅ Agregar `beginStatefulContext()` y `endStatefulContext()`
2. ✅ Agregar ThreadLocal para rastrear estado
3. ✅ Documentar API con JavaDoc
4. ✅ Escribir unit tests

### Fase 2: Actualizar ProgramService
1. ✅ Modificar `modifyFunctionModuleSource()` para usar contexto stateful
2. ✅ Aplicar mismo patrón a `modifyProgramSource()` (si aplica)
3. ✅ Aplicar mismo patrón a `ClassService.modifyClass()` (si aplica)

### Fase 3: Testing
1. ✅ Test manual con `ManualProgramModifyTest.testModifyFunctionModule_Success()`
2. ✅ Verificar bloqueo en SAP (SM12)
3. ✅ Verificar modificación exitosa
4. ✅ Verificar unlock correcto

### Fase 4: Validación
1. ✅ Probar con FM real: `ZFI_DMEE_ITAU_R6`
2. ✅ Verificar logs de debugging
3. ✅ Verificar transporte asignado
4. ✅ Confirmar código modificado en SAP

---

## 9. Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|-----------|
| **Contextos no cerrados** | Alto - Memory leak | Finally obligatorio, ThreadLocal cleanup |
| **Exception en medio de workflow** | Medio - Objeto bloqueado | Try-catch-finally, unlock en catch |
| **Pool exhaustion** | Medio - Timeouts | Minimizar tiempo en contexto stateful |
| **Thread confusion** | Bajo - Lógica incorrecta | ThreadLocal aísla contextos |
| **Nested contexts** | Bajo - IllegalStateException | Validar con flag booleano |

---

## 10. Referencias

### Documentación SAP JCo
- **Intro.html**: `resources/jco/javadoc/intro.html`
- **JCoContext JavaDoc**: `com.sap.conn.jco.JCoContext`
- **JCoSessionReference**: `com.sap.conn.jco.ext.JCoSessionReference`

### Ejemplos de Código
1. `StatefulCalls.java` - Patrón básico
2. `StatefulJob.java` - Multi-step workflow
3. `WorkerThread.java` - Session management
4. `SimpleSessionReference.java` - Session tracking

### Archivos del Proyecto
- **RfcAdapter actual**: `src/main/java/.../service/RfcAdapter.java`
- **ProgramService**: `src/main/java/.../service/ProgramService.java`
- **Test manual**: `src/test/java/.../manual/ManualProgramModifyTest.java`

---

## 11. Conclusiones

### ✅ Hallazgos Clave
1. **Causa raíz identificada**: Conexiones stateless no mantienen bloqueos SAP
2. **Solución disponible**: JCoContext en JCo SDK
3. **Patrón claro**: `begin()` → operaciones → `end()` en finally
4. **Ejemplos de referencia**: Múltiples ejemplos en `resources/jco/examples/`

### 📋 Próximos Pasos
1. Implementar `beginStatefulContext()` / `endStatefulContext()` en RfcAdapter
2. Modificar `modifyFunctionModuleSource()` para usar contexto stateful
3. Ejecutar test manual para validación
4. Documentar comportamiento en JavaDoc

### ⚠️ Puntos Críticos
- **SIEMPRE** usar try-finally para cerrar contextos
- **NO** compartir contextos entre threads
- **MINIMIZAR** tiempo en contexto stateful (impacto en pool)
- **VALIDAR** que contexto esté activo antes de operaciones

---

**Documento creado**: 2025-11-12
**Autor**: Investigación JCo Stateful Connections
**Estado**: ✅ Investigación completa - Listo para implementación

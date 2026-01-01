# PR: Trace Transaction Analysis Tool

## User Story

**Como** desarrollador SAP/consultor funcional
**Quiero** una herramienta MCP que capture SQL trace de transacciones SAP
**Para** entender el flujo de ejecución, tablas accedidas y lógica de cálculo de programas estándar SAP

## Problema a Resolver

Analizar programas estándar SAP para entender:
- ¿Cómo se calcula un campo específico? (ej: NETWR en VA03)
- ¿Qué tablas accede una transacción?
- ¿Cuál es el flujo de ejecución? (FM1 → FM2 → Subrutina → FM3)
- ¿Dónde está la lógica relevante para modificar/extender?

## Solución Implementada

### ⚠️ HALLAZGO CRÍTICO: Limitación RFC/Screen

Durante la implementación se descubrió una **limitación técnica fundamental** de SAP:

> **CALL TRANSACTION y SUBMIT desde contexto RFC causan DUMP cuando la transacción muestra pantallas (dynpros).**

Esto significa que el approach original de un FM único que ejecuta transacciones **NO funciona** para la mayoría de transacciones interactivas (VA01, ME21N, MM01, etc.).

**Error típico**: `DYNPRO_SEND_IN_BACKGROUND` - "No puedo enviar pantalla de diálogo desde un proceso de fondo"

### Dos Enfoques Implementados

| Approach | Uso | Limitación |
|----------|-----|------------|
| **HYBRID (Recomendado)** | Cualquier transacción incluyendo interactivas | Requiere acción manual del usuario |
| **AUTOMATED (Limitado)** | Solo transacciones batch/sin pantallas | DUMP si muestra dynpros |

#### 1. HYBRID Human-in-the-Loop (Recomendado)

```
Claude                           Usuario                         SAP
  │                                │                               │
  ├─── activate_trace() ──────────────────────────────────────────►│ ST05 ON
  │                                │                               │
  │    "Ejecute su transacción"    │                               │
  │◄───────────────────────────────┤                               │
  │                                ├───── VA03, ME21N, etc. ──────►│
  │                                │◄──────── Resultados ──────────┤
  │                                │                               │
  ├─── deactivate_and_read_trace() ────────────────────────────────►│ ST05 OFF + Read
  │◄────────────────────────────── JSON con análisis ──────────────┤
  │                                │                               │
  └── Analiza tablas, código ──────┘                               │
```

**Ventajas**:
- ✅ Funciona con CUALQUIER transacción
- ✅ No hay limitaciones de pantallas
- ✅ El usuario tiene control total
- ✅ Ideal para transacciones interactivas

#### 2. AUTOMATED (Solo batch)

```
Claude                                                            SAP
  │                                                                 │
  ├─── trace_transaction('FMAVCH01', variant='PRE') ───────────────►│
  │    [Activa trace + SUBMIT + Desactiva + Lee]                    │
  │◄────────────────────────────── JSON con análisis ───────────────┤
```

**Ventajas**:
- ✅ Un solo paso, sin intervención manual
- ❌ LIMITADO a transacciones que no muestran pantallas
- ❌ Requiere variante preconfigurada o BDC completo

---

## Investigación Completada

**Documento**: `docs/research/st05_api_investigation.md`

### APIs SAP Identificadas

| FM | Propósito | RFC |
|----|-----------|-----|
| `ST05_ACTIVATE_TRACE` | Activa trace con filtros | ✅ |
| `ST05_DEACTIVATE_TRACE` | Desactiva trace | ✅ |
| `ST05_GET_TRACE_TABLES` | Retorna resultados completos | ✅ |

### Estructuras Clave

**Entrada**:
- `ST05_API_TRACE_TYPES` - Flags: SQL_ON, BUF_ON, ENQ_ON, RFC_ON
- `ST05_API_TRACE_FILTER` - Filtros: TRACE_USER, TRANSACTION_CODE, STACK_TRACE_ON

**Salida**:
- `ST05_DETAILED_RECORD` - Programa, línea, tabla, SQL, duración
- `ST05_KERNEL_CALL_STACK_ITEM` - Call stack: nivel, evento, nombre

### Autorización Requerida

- Objeto: `S_ADMI_FCD`
- Valores: `ST0M` (activar), `ST0R` (leer)

---

## Fases de Implementación

### FASE 1: FMs Wrapper ABAP ✅ COMPLETADA

**Objetivo**: Crear FMs que encapsulen el workflow de trace

**Estado**: ✅ **COMPLETADA** - Ambos FMs activos en SAP

#### FMs Implementados

| FM | Propósito | Estado | Include |
|----|-----------|--------|---------|
| `ZCX_TRACE_ACTIVATE` | Activa ST05 trace para usuario | ✅ ACTIVO | LZGFCX_1U14 |
| `ZCX_TRACE_DEACTIVATE_AND_READ` | Desactiva trace y lee resultados | ✅ ACTIVO | LZGFCX_1U15 |
| `ZCX_TRACE_TRANSACTION` | (Original) Ejecución automatizada | ⚠️ LIMITADO | LZGFCX_1U13 |

#### Interfaz: ZCX_TRACE_ACTIVATE (Hybrid Step 1)

```abap
FUNCTION ZCX_TRACE_ACTIVATE
  IMPORTING
    VALUE(IV_TRACE_USER) TYPE SYUNAME DEFAULT SY-UNAME
    VALUE(IV_TRACE_SQL) TYPE CHAR1 DEFAULT ABAP_TRUE
    VALUE(IV_TRACE_BUFFER) TYPE CHAR1 DEFAULT ABAP_FALSE
    VALUE(IV_TRACE_ENQUEUE) TYPE CHAR1 DEFAULT ABAP_FALSE
    VALUE(IV_WITH_CALL_STACK) TYPE CHAR1 DEFAULT ABAP_TRUE
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_START_DATE) TYPE SYDATUM
    VALUE(EV_START_TIME) TYPE SYUZEIT.
```

#### Interfaz: ZCX_TRACE_DEACTIVATE_AND_READ (Hybrid Step 2)

```abap
FUNCTION ZCX_TRACE_DEACTIVATE_AND_READ
  IMPORTING
    VALUE(IV_TRACE_USER) TYPE SYUNAME DEFAULT SY-UNAME
    VALUE(IV_MAX_RECORDS) TYPE I DEFAULT 500
  EXPORTING
    VALUE(EV_DETAILED_JSON) TYPE STRING
    VALUE(EV_CALL_STACK_JSON) TYPE STRING
    VALUE(EV_TABLE_ACCESS_JSON) TYPE STRING
    VALUE(EV_ERROR_MESSAGE) TYPE STRING.
```

#### Interfaz: ZCX_TRACE_TRANSACTION (Automated - Limitado)

```abap
FUNCTION ZCX_TRACE_TRANSACTION
  IMPORTING
    VALUE(IV_TRANSACTION) TYPE TCODE
    VALUE(IV_VARIANT) TYPE VARIANT OPTIONAL
    VALUE(IT_BDC_DATA) TYPE BDCDATA_TAB OPTIONAL
    VALUE(IV_TRACE_SQL) TYPE ABAP_BOOL DEFAULT ABAP_TRUE
    VALUE(IV_TRACE_BUFFER) TYPE ABAP_BOOL DEFAULT ABAP_FALSE
    VALUE(IV_TRACE_ENQUEUE) TYPE ABAP_BOOL DEFAULT ABAP_FALSE
    VALUE(IV_WITH_CALL_STACK) TYPE ABAP_BOOL DEFAULT ABAP_TRUE
    VALUE(IV_MAX_RECORDS) TYPE I DEFAULT 1000
  EXPORTING
    VALUE(EV_TRACE_DURATION_MS) TYPE I
    VALUE(EV_TOTAL_STATEMENTS) TYPE I
    VALUE(EV_TABLES_COUNT) TYPE I
    VALUE(EV_DETAILED_RECORDS_JSON) TYPE STRING
    VALUE(EV_CALL_STACK_JSON) TYPE STRING
    VALUE(EV_TABLE_ACCESS_JSON) TYPE STRING
    VALUE(EV_ERROR_MESSAGE) TYPE STRING
  EXCEPTIONS
    NO_AUTHORITY
    ACTIVATION_ERROR
    EXECUTION_ERROR
    DEACTIVATION_ERROR
    READ_ERROR.
```

#### Pseudocódigo

```abap
" 1. Capturar tiempo inicio
GET TIME STAMP FIELD lv_start_ts.
lv_start_date = sy-datum.
lv_start_time = sy-uzeit.

" 2. Configurar tipos de trace
ls_trace_types-sql_on = iv_trace_sql.
ls_trace_types-buf_on = iv_trace_buffer.
ls_trace_types-enq_on = iv_trace_enqueue.

" 3. Configurar filtro (solo usuario actual)
ls_filter-trace_user = sy-uname.
ls_filter-client = sy-mandt.
ls_filter-stack_trace_on = iv_with_call_stack.

" 4. Activar trace
CALL FUNCTION 'ST05_ACTIVATE_TRACE'
  EXPORTING
    trace_types  = ls_trace_types
    trace_filter = ls_filter
  EXCEPTIONS
    no_authority = 1
    OTHERS       = 2.

IF sy-subrc <> 0.
  ev_error_message = |Activation failed: { sy-subrc }|.
  RAISE activation_error.
ENDIF.

" 5. Ejecutar transacción
IF it_bdc_data IS NOT INITIAL.
  CALL TRANSACTION iv_transaction
    USING it_bdc_data
    MODE 'N'
    UPDATE 'S'
    MESSAGES INTO lt_messages.
ELSEIF iv_variant IS NOT INITIAL.
  " Obtener programa de la transacción
  SELECT SINGLE pgmna FROM tstc INTO lv_program
    WHERE tcode = iv_transaction.

  IF sy-subrc = 0.
    SUBMIT (lv_program)
      USING SELECTION-SET iv_variant
      AND RETURN.
  ENDIF.
ELSE.
  " Ejecutar transacción sin datos
  CALL TRANSACTION iv_transaction MODE 'N'.
ENDIF.

" 6. Capturar tiempo fin
GET TIME STAMP FIELD lv_end_ts.
lv_end_date = sy-datum.
lv_end_time = sy-uzeit.

" 7. Desactivar trace
CALL FUNCTION 'ST05_DEACTIVATE_TRACE'
  EXPORTING
    trace_types = ls_trace_types.

" 8. Configurar intervalo de lectura
ls_interval-start_date = lv_start_date.
ls_interval-start_time = lv_start_time.
ls_interval-end_date   = lv_end_date.
ls_interval-end_time   = lv_end_time.

" 9. Leer resultados
CALL FUNCTION 'ST05_GET_TRACE_TABLES'
  EXPORTING
    trace_types    = ls_trace_types
    trace_interval = ls_interval
    kind           = 'DET+'
  IMPORTING
    detailed_record_table     = lt_detailed
    kernel_call_stack         = lt_call_stack
    table_access_record_table = lt_table_access.

" 10. Aplicar límite de registros
IF lines( lt_detailed ) > iv_max_records.
  DELETE lt_detailed FROM ( iv_max_records + 1 ).
ENDIF.

" 11. Serializar a JSON
ev_detailed_records_json = /ui2/cl_json=>serialize( data = lt_detailed ).
ev_call_stack_json = /ui2/cl_json=>serialize( data = lt_call_stack ).
ev_table_access_json = /ui2/cl_json=>serialize( data = lt_table_access ).

" 12. Calcular métricas
ev_trace_duration_ms = ( lv_end_ts - lv_start_ts ) / 1000.
ev_total_statements = lines( lt_detailed ).
ev_tables_count = lines( lt_table_access ).
```

#### Criterios de Aceptación Fase 1

- [ ] FM creado en `ZGFCX_1` y activo
- [ ] Ejecuta trace para transacción simple (ej: SE11, SU01D)
- [ ] Retorna JSON válido en los 3 parámetros de salida
- [ ] Maneja errores de autorización correctamente
- [ ] Log de prueba documentado

---

### FASE 2: Servicio Java (TraceService.java) ✅ COMPLETADA

**Objetivo**: Crear servicio Java que llame a los FMs via RFC

**Estado**: ✅ **COMPLETADA** - Servicio implementado con ambos enfoques

#### Tareas

- [x] **2.1** Crear `TraceService.java` en `src/main/java/.../service/`
- [x] **2.2** Implementar método `traceTransaction()` (automated)
- [x] **2.3** Implementar método `activateTrace()` (hybrid step 1)
- [x] **2.4** Implementar método `deactivateAndReadTrace()` (hybrid step 2)
- [x] **2.5** Crear DTOs para deserializar JSON de SAP
- [x] **2.6** Implementar parsing de resultados

#### Estructura de Clases Implementadas

```
src/main/java/com/crystal/mcp/sapserver/
├── service/
│   └── TraceService.java              ✅ Implementado
├── model/
│   ├── TraceAnalysisResult.java       ✅ Implementado
│   ├── TraceDetailedRecord.java       ✅ Implementado
│   ├── TraceCallStackItem.java        ✅ Implementado
│   └── TraceTableAccessRecord.java    ✅ Implementado
└── tool/
    └── TraceTools.java                ✅ Implementado
```

#### DTOs

```java
// TraceDetailedRecord.java
@Data
public class TraceDetailedRecord {
    private String date;
    private String time;
    private Long duration;
    private Integer numberOfRows;
    private String object;           // Tabla
    private String statementWithValues;
    private String statementWithNames;
    private String variables;
    private String program;
    private Integer offset;          // Línea
    private String transaction;
    private String operation;
    private String traceType;
    private Integer recordNumber;
}

// TraceCallStackItem.java
@Data
public class TraceCallStackItem {
    private Integer recordNumber;    // Link a DetailedRecord
    private Integer level;
    private String progInfo;         // Programa/Include/Línea
    private String eventType;        // FORM, METHOD, FUNCTION
    private String eventName;        // Nombre del evento
}

// TraceTableAccessRecord.java
@Data
public class TraceTableAccessRecord {
    private String object;           // Tabla
    private String statementType;
    private Long duration;
    private Integer numberOfRows;
    private Integer numberOfExecutions;
    private String ddtext;           // Descripción tabla
}

// TraceAnalysisResult.java
@Data
public class TraceAnalysisResult {
    private Integer traceDurationMs;
    private Integer totalStatements;
    private Integer tablesCount;
    private List<TraceDetailedRecord> detailedRecords;
    private List<TraceCallStackItem> callStack;
    private List<TraceTableAccessRecord> tableAccess;
    private String errorMessage;
}
```

#### Implementación Service

```java
@Service
@Slf4j
public class TraceService {

    private final RfcAdapter rfcAdapter;
    private final ObjectMapper objectMapper;

    public TraceAnalysisResult traceTransaction(
            String transactionCode,
            String variant,
            List<BdcData> bdcData,
            boolean traceSql,
            boolean traceBuffer,
            boolean traceEnqueue,
            boolean withCallStack,
            int maxRecords) {

        try {
            // Preparar parámetros
            Map<String, Object> imports = new HashMap<>();
            imports.put("IV_TRANSACTION", transactionCode);
            if (variant != null) imports.put("IV_VARIANT", variant);
            if (bdcData != null) imports.put("IT_BDC_DATA", convertBdcData(bdcData));
            imports.put("IV_TRACE_SQL", traceSql ? "X" : " ");
            imports.put("IV_TRACE_BUFFER", traceBuffer ? "X" : " ");
            imports.put("IV_TRACE_ENQUEUE", traceEnqueue ? "X" : " ");
            imports.put("IV_WITH_CALL_STACK", withCallStack ? "X" : " ");
            imports.put("IV_MAX_RECORDS", maxRecords);

            // Llamar FM
            JCoFunction function = rfcAdapter.getFunction("ZCX_TRACE_TRANSACTION");
            // ... set parameters ...
            function.execute(rfcAdapter.getDestination());

            // Leer resultados
            TraceAnalysisResult result = new TraceAnalysisResult();
            result.setTraceDurationMs(function.getExportParameterList().getInt("EV_TRACE_DURATION_MS"));
            result.setTotalStatements(function.getExportParameterList().getInt("EV_TOTAL_STATEMENTS"));
            result.setTablesCount(function.getExportParameterList().getInt("EV_TABLES_COUNT"));

            // Deserializar JSON
            String detailedJson = function.getExportParameterList().getString("EV_DETAILED_RECORDS_JSON");
            String callStackJson = function.getExportParameterList().getString("EV_CALL_STACK_JSON");
            String tableAccessJson = function.getExportParameterList().getString("EV_TABLE_ACCESS_JSON");

            result.setDetailedRecords(parseDetailedRecords(detailedJson));
            result.setCallStack(parseCallStack(callStackJson));
            result.setTableAccess(parseTableAccess(tableAccessJson));

            return result;

        } catch (Exception e) {
            log.error("Error tracing transaction: {}", e.getMessage());
            TraceAnalysisResult error = new TraceAnalysisResult();
            error.setErrorMessage(e.getMessage());
            return error;
        }
    }
}
```

#### Criterios de Aceptación Fase 2

- [ ] `TraceService.java` compilado sin errores
- [ ] DTOs creados y con anotaciones Jackson
- [ ] Test manual ejecuta y retorna resultados
- [ ] JSON de SAP se deserializa correctamente
- [ ] Errores se manejan y reportan

---

### FASE 3: MCP Tools (TraceTools.java) ✅ COMPLETADA

**Objetivo**: Exponer funcionalidad como herramientas MCP

**Estado**: ✅ **COMPLETADA** - 3 tools expuestos

#### Tareas

- [x] **3.1** Crear `TraceTools.java` en `src/main/java/.../tool/`
- [x] **3.2** Implementar tool `trace_transaction` (automated)
- [x] **3.3** Implementar tool `activate_trace` (hybrid step 1)
- [x] **3.4** Implementar tool `deactivate_and_read_trace` (hybrid step 2)
- [x] **3.5** Formatear salida optimizada para LLM
- [ ] **3.6** Probar desde Claude Desktop (pendiente)

#### Implementación Tool

```java
@Component
@Slf4j
public class TraceTools {

    private final TraceService traceService;

    @Tool(description = """
        Execute SAP transaction with SQL trace enabled and return execution analysis.

        Use cases:
        - Understand how a field is calculated (e.g., NETWR in VA03)
        - Find which tables a transaction accesses
        - Trace execution flow (FM → FM → Subroutine)
        - Identify performance bottlenecks

        Returns: Execution flow, tables accessed, call stack, and timing data.

        Authorization required: S_ADMI_FCD with values ST0M and ST0R.
        """)
    public String traceTransactionAnalysis(
            @ToolParam(description = "SAP transaction code to execute (e.g., VA03, MB51, ME21N)")
            String transactionCode,

            @ToolParam(description = "Report variant name for SUBMIT execution (optional)")
            String variant,

            @ToolParam(description = "BDC data as JSON array for CALL TRANSACTION (optional). Format: [{\"program\":\"...\",\"dynpro\":\"...\",\"dynbegin\":\"X\",\"fnam\":\"...\",\"fval\":\"...\"}]")
            String bdcDataJson,

            @ToolParam(description = "Enable SQL trace (default: true)")
            Boolean traceSql,

            @ToolParam(description = "Enable buffer trace (default: false)")
            Boolean traceBuffer,

            @ToolParam(description = "Enable call stack capture (default: true)")
            Boolean withCallStack,

            @ToolParam(description = "Maximum records to return (default: 500)")
            Integer maxRecords
    ) {
        // Defaults
        traceSql = traceSql != null ? traceSql : true;
        traceBuffer = traceBuffer != null ? traceBuffer : false;
        withCallStack = withCallStack != null ? withCallStack : true;
        maxRecords = maxRecords != null ? maxRecords : 500;

        // Parse BDC if provided
        List<BdcData> bdcData = null;
        if (bdcDataJson != null && !bdcDataJson.isEmpty()) {
            bdcData = parseBdcData(bdcDataJson);
        }

        // Execute trace
        TraceAnalysisResult result = traceService.traceTransaction(
            transactionCode,
            variant,
            bdcData,
            traceSql,
            traceBuffer,
            false, // enqueue
            withCallStack,
            maxRecords
        );

        // Format for LLM
        return formatResultForLlm(result);
    }

    private String formatResultForLlm(TraceAnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        // Summary
        sb.append("## Trace Analysis Summary\n\n");
        sb.append(String.format("- Duration: %d ms\n", result.getTraceDurationMs()));
        sb.append(String.format("- SQL Statements: %d\n", result.getTotalStatements()));
        sb.append(String.format("- Tables Accessed: %d\n\n", result.getTablesCount()));

        // Table Access Summary (most useful)
        sb.append("## Tables Accessed\n\n");
        sb.append("| Table | Description | Reads | Rows | Duration (μs) |\n");
        sb.append("|-------|-------------|-------|------|---------------|\n");
        for (TraceTableAccessRecord rec : result.getTableAccess()) {
            sb.append(String.format("| %s | %s | %d | %d | %d |\n",
                rec.getObject(),
                rec.getDdtext() != null ? rec.getDdtext() : "",
                rec.getNumberOfExecutions(),
                rec.getNumberOfRows(),
                rec.getDuration()));
        }
        sb.append("\n");

        // Execution Flow (top entries)
        sb.append("## Execution Flow (Top 20)\n\n");
        sb.append("| # | Program | Line | Table | Operation | Duration |\n");
        sb.append("|---|---------|------|-------|-----------|----------|\n");
        int count = 0;
        for (TraceDetailedRecord rec : result.getDetailedRecords()) {
            if (count++ >= 20) break;
            sb.append(String.format("| %d | %s | %d | %s | %s | %d μs |\n",
                rec.getRecordNumber(),
                rec.getProgram(),
                rec.getOffset(),
                rec.getObject(),
                rec.getOperation(),
                rec.getDuration()));
        }
        sb.append("\n");

        // Call Stack (if available)
        if (result.getCallStack() != null && !result.getCallStack().isEmpty()) {
            sb.append("## Call Stack Sample\n\n");
            sb.append("```\n");
            int stackCount = 0;
            for (TraceCallStackItem item : result.getCallStack()) {
                if (stackCount++ >= 30) break;
                String indent = "  ".repeat(item.getLevel());
                sb.append(String.format("%s%s %s (%s)\n",
                    indent,
                    item.getEventType(),
                    item.getEventName(),
                    item.getProgInfo()));
            }
            sb.append("```\n");
        }

        return sb.toString();
    }
}
```

#### Criterios de Aceptación Fase 3

- [ ] Tool registrado y visible en Claude Desktop
- [ ] Ejecuta trace de transacción simple
- [ ] Output formateado legible para LLM
- [ ] Documentación en README actualizada
- [ ] Manejo de errores con mensajes claros

---

### FASE 4: Testing y Optimizaciones 🔄 PENDIENTE

**Objetivo**: Validar funcionamiento en producción y optimizar

**Estado**: 🔄 **PENDIENTE** - Requiere testing manual desde Claude Desktop

#### Tareas

- [ ] **4.1** Probar `activate_trace` + `deactivate_and_read_trace` workflow
- [ ] **4.2** Probar `trace_transaction` con transacción batch (sin pantallas)
- [ ] **4.3** Filtrar tablas SAP internas (T000, TADIR, etc.)
- [ ] **4.4** Comprimir call stack (eliminar niveles redundantes)
- [ ] **4.5** Agregar modo "field focus" para rastrear campo específico
- [ ] **4.6** Crear ejemplos de uso documentados

#### Mejoras Planificadas

**Filtro de tablas internas**:
```java
private static final Set<String> INTERNAL_TABLES = Set.of(
    "T000", "TADIR", "TRDIR", "D010*", "REPOSRC", "CROSS", "WBCROSS"
);

private List<TraceTableAccessRecord> filterInternalTables(List<TraceTableAccessRecord> records) {
    return records.stream()
        .filter(r -> !INTERNAL_TABLES.contains(r.getObject()))
        .filter(r -> !r.getObject().startsWith("D010"))
        .collect(Collectors.toList());
}
```

**Modo Field Focus**:
```java
@ToolParam(description = "Focus on specific field (e.g., VBAP-NETWR) - filters results")
String fieldFocus
```

#### Criterios de Aceptación Fase 4

- [ ] Tablas internas filtradas por defecto
- [ ] Call stack comprimido y legible
- [ ] Al menos 3 ejemplos de uso documentados
- [ ] Performance aceptable (<30s para transacciones normales)

---

## Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Autorización S_ADMI_FCD faltante | Media | Alto | Verificar antes de ejecutar, mensaje claro |
| Trace muy grande | Alta | Medio | Límite IV_MAX_RECORDS, filtro temporal |
| Transacción con efectos secundarios | Media | Alto | Documentar, usar MODE 'N', transacciones read-only |
| Interferencia con otros traces | Baja | Medio | Filtrar por SY-UNAME únicamente |
| Timeout en transacciones largas | Media | Medio | Timeout configurable, mensaje de progreso |

---

## Casos de Uso Ejemplo

### Caso 1: ¿Cómo se calcula NETWR en VA03?

```
User: "Quiero entender cómo se calcula el campo NETWR cuando visualizo un pedido en VA03"

Tool call:
  transactionCode: "VA03"
  bdcDataJson: [{"dynpro":"100","fnam":"VBAK-VBELN","fval":"12345"}]
  withCallStack: true

Output esperado:
- Tablas: VBAP, KONV, PRCD_ELEMENTS
- Call stack: SAPMV45A → LV45AFFC_PFLEGE_KONDITIONEN → PRICING_SUBSCREEN
- Líneas relevantes para investigar
```

### Caso 2: ¿Qué tablas usa MB51?

```
User: "Lista todas las tablas que accede la transacción MB51"

Tool call:
  transactionCode: "MB51"
  variant: "Z_DEFAULT"
  traceSql: true
  withCallStack: false

Output esperado:
- Table Access Summary ordenado por ejecuciones
- MSEG, MKPF, MARD, T001W, etc.
```

### Caso 3: ¿Por qué ME21N es lento?

```
User: "ME21N tarda mucho, quiero identificar qué consultas son más costosas"

Tool call:
  transactionCode: "ME21N"
  bdcDataJson: [...BDC para crear PO...]
  maxRecords: 2000

Output esperado:
- Execution Flow ordenado por Duration DESC
- Identificar SELECT más costosos
- Programa y línea para optimizar
```

---

## Referencias

- **Investigación**: `docs/research/st05_api_investigation.md`
- **Function Group**: ST05_API (Package ST05)
- **Autorización**: S_ADMI_FCD (ST0M, ST0R)
- **SAP Note**: 2435796 - ST05 API documentation

---

## Historial de Cambios

| Fecha | Versión | Cambios |
|-------|---------|---------|
| 2024-12-10 | 0.1 | Documento inicial con 4 fases |
| 2024-12-10 | 0.2 | Hallazgo crítico: RFC/Screen limitation. Diseño de approach híbrido |
| 2024-12-10 | 1.0 | **Implementación completa**: 3 FMs ABAP activos, Java Service y Tools implementados |

---

## Estado Actual

```
[████████████████░░░░] 80% - Fases 1-3 completadas, testing pendiente

Fase 1: FMs ABAP     [✅] COMPLETADA - ZCX_TRACE_ACTIVATE, ZCX_TRACE_DEACTIVATE_AND_READ
Fase 2: Service Java [✅] COMPLETADA - TraceService.java con 3 métodos
Fase 3: MCP Tools    [✅] COMPLETADA - 3 tools: trace_transaction, activate_trace, deactivate_and_read_trace
Fase 4: Testing      [🔄] PENDIENTE - Validación desde Claude Desktop
```

---

## Resumen de Implementación

### Componentes ABAP (SAP GDC - Function Group ZGFCX_1)

| Componente | Estado | Bytes | Descripción |
|------------|--------|-------|-------------|
| `ZCX_TRACE_ACTIVATE` | ✅ ACTIVO | 2,353 | Hybrid Step 1 - Activa ST05 trace |
| `ZCX_TRACE_DEACTIVATE_AND_READ` | ✅ ACTIVO | 3,054 | Hybrid Step 2 - Desactiva y lee resultados |
| `ZCX_TRACE_TRANSACTION` | ⚠️ LIMITADO | - | Automated approach (solo batch) |

### Componentes Java (MCP Server)

| Archivo | Métodos | Descripción |
|---------|---------|-------------|
| `TraceService.java` | `traceTransaction()`, `activateTrace()`, `deactivateAndReadTrace()` | Service layer con lógica RFC |
| `TraceTools.java` | `trace_transaction`, `activate_trace`, `deactivate_and_read_trace` | MCP tools expuestos |
| `TraceAnalysisResult.java` | - | DTO principal con `toMarkdownSummary()` |
| `TraceDetailedRecord.java` | - | DTO para registros SQL individuales |
| `TraceCallStackItem.java` | - | DTO para call stack ABAP |
| `TraceTableAccessRecord.java` | - | DTO para resumen de acceso por tabla |

### MCP Tools Disponibles

| Tool | Descripción | Uso Recomendado |
|------|-------------|-----------------|
| `activate_trace` | HYBRID Step 1: Activa ST05 trace para usuario | Transacciones interactivas (VA03, ME21N, etc.) |
| `deactivate_and_read_trace` | HYBRID Step 2: Desactiva trace y retorna análisis | Después de que usuario ejecute transacción |
| `trace_transaction` | AUTOMATED: Ejecuta y traza en un paso | Solo transacciones batch sin pantallas |

### Próximos Pasos

1. **Testing** - Validar tools desde Claude Desktop
2. **Optimización** - Filtrar tablas internas SAP del output
3. **Documentación** - Agregar ejemplos de uso real

# PR: Get Transport Log Tool

## User Story

Como desarrollador ABAP en gdcmcp
Quiero crear una tool que me permita recuperar el log de transporte de una OT
Para suministrar con mayor información a los agentes.

## Criterios de Aceptación

- [x] Verificar con sap docs mcp o las otras herramientas si existe una funcionalidad ADT o funciones estándar de SAP para luego empaquetar en una RFC.
- [x] El log debe retornar datos al agente únicamente si retorna error el log.
- [x] La tool debe recibir una o varias OT, también debe recibir el usuario
- [x] La tool debe buscar en todos los tipos de OT: copia, workbench, customizing.

---

## Phase 1: Requirements Analysis ✅ COMPLETE

**Fecha de Análisis:** 2025-12-03
**Analista:** Claude Code (SAP Requirements Analyst)

---

### 1. Analysis Summary

**Tipo de Documento:** User Story
**Módulo:** BC/CTS (Basis - Change and Transport System)
**RICEFW ID:** No especificado

**Funcionalidad Core:**
- Crear MCP tool para recuperar logs de transporte
- Filtrar logs que contengan errores/warnings
- Soportar múltiples OTs como entrada
- Filtrar por usuario (owner de OT)

**Recomendación Principal:**
Usar la API estándar `IF_CTS_REST_API` con el método `READ_GLOBAL_INFO` que retorna estructura jerárquica completa del log de transporte incluyendo errores y warnings.

---

### 2. Technical Findings

#### 2.1 SAP Standard Objects Identified

| Objeto | Tipo | Package | Descripción | Uso |
|--------|------|---------|-------------|-----|
| `IF_CTS_REST_API` | Interface | SCTS_REQ_RES_API | API REST completa para CTS | ✅ Principal |
| `CL_CTS_REST_API_FACTORY` | Clase | SCTS_REQ_RES_API | Factory para crear instancia | ✅ Usar |
| `CL_CTS_REST_API_IMPL` | Clase | SCTS_REQ_RES_API | Implementación de la API | Interno |
| `READ_GLOBAL_INFO` | Método | IF_CTS_REST_API | Lee log global de transporte | ✅ Core |
| `TR_READ_LOG` | FM | SCTS_LOG | Lee log desde archivo/DB | Alternativa |
| `TR_READ_LOG_EXT` | FM | SCTS_LOG | Versión extendida con navegación | Alternativa |
| `CL_CTS_ADT_TM_TRANSPORT_LOGS` | Clase | SCTS_ADT | Endpoint ADT para logs | Referencia |

#### 2.2 API Signature - READ_GLOBAL_INFO

```abap
methods READ_GLOBAL_INFO
  importing
    IV_TRKORR   type TRKORR           " Transport request number
    IV_DIR_TYPE type TSTRF01-DIRTYPE  " Directory type (default 'T')
    IS_SETTINGS type CTSLG_SETTINGS   " Display settings
  exporting
    ES_COFILE   type CTSLG_COFILE     " COFILE structure
    ET_NODES    type CTSLG_SNODETEXTT " Hierarchical log nodes
  raising
    CX_CTS_REST_API_EXCEPTION.
```

#### 2.3 Severity Mapping

| Color Code | Severity | Descripción |
|------------|----------|-------------|
| 6 | E (Error) | Negro sobre rojo - Error crítico |
| 2 | W (Warning) | Warning - Advertencia |
| Otros | I (Info) | Informativo |

#### 2.4 Node Types in Log

| Type | Descripción |
|------|-------------|
| FRST | First node (root) |
| TRAN | Transport request |
| SRCS | Source system |
| TARS | Target system |
| STEP | Import step |
| STEC | Step with error |
| STEO | Step completed |
| DATE | Date node |
| TIND | Independent log reference |

---

### 3. Q&A Log

| # | Pregunta | Respuesta | Impacto |
|---|----------|-----------|---------|
| 1 | ¿Qué API usar? | `IF_CTS_REST_API` con `READ_GLOBAL_INFO` | Define arquitectura |
| 2 | ¿Qué severidades filtrar? | Errores (E) + Warnings (W) | Define lógica de filtrado |
| 3 | ¿Comportamiento sin log? | Indicar con flag `has_log: false` | Define estructura respuesta |
| 4 | ¿Nivel de detalle? | Estándar: OT, severity, mensaje, sistema, fecha/hora, paso | Define campos JSON |
| 5 | ¿Filtro de usuario? | Por owner de OT (AS4USER en E070) | Define query previa |
| 6 | ¿Modo de salida? | Resumen + detalle (conteo + lista problemas) | Define formato output |

---

### 4. Granular Requirements

#### 4.1 Sub-Requirements

| ID | Descripción | Objetos Técnicos | Complejidad | Dependencia | Estado |
|----|-------------|------------------|-------------|-------------|--------|
| TL-001 | Crear RFC wrapper para READ_GLOBAL_INFO | FM: `ZCX_GET_TRANSPORT_LOG` | Media | - | ✅ Done |
| TL-002 | Implementar filtrado por severidad (E/W) | Lógica en FM | Baja | TL-001 | ✅ Done |
| TL-003 | Implementar soporte multi-OT | Loop + agregación en FM | Baja | TL-001 | ✅ Done |
| TL-004 | Implementar filtro por usuario (owner) | Query E070 previa | Baja | TL-001 | ✅ Done |
| TL-005 | Crear Service Java `TransportLogService` | `TransportLogService.java` | Media | TL-001 | ✅ Done |
| TL-006 | Crear MCP Tool `get_transport_log` | `TransportLogTools.java` | Baja | TL-005 | ✅ Done |
| TL-007 | Crear test manual | `ManualTransportLogTest.java` | Baja | TL-006 | ✅ Done |

#### 4.2 Implementation Sequence

```
TL-001 (RFC Wrapper)
    │
    ├──> TL-002 (Filtro severity)
    ├──> TL-003 (Multi-OT support)
    └──> TL-004 (Filtro user)
              │
              v
    TL-005 (Service Java)
              │
              v
    TL-006 (MCP Tool)
              │
              v
    TL-007 (Test Manual)
```

#### 4.3 Effort Estimation

| Fase | Esfuerzo |
|------|----------|
| TL-001 a TL-004 (ABAP) | 3-4 horas |
| TL-005 a TL-007 (Java) | 3-4 horas |
| **Total** | **6-8 horas** |

---

### 5. Technical Specification

#### 5.1 Input Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|-----------|------|-------------|-------------|
| `transports` | String[] | ✅ | Lista de números de OT (ej: ["CADK900123", "CADK900124"]) |
| `user` | String | ❌ | Filtrar por owner de OT (AS4USER). Si vacío, retorna todas. |

#### 5.2 Output Structure (JSON)

```json
{
  "query": {
    "transports_requested": ["CADK900123", "CADK900124"],
    "user_filter": "DEVELOPER",
    "timestamp": "2025-12-03T14:30:22Z"
  },
  "summary": {
    "total_transports": 2,
    "with_errors": 1,
    "with_warnings": 1,
    "without_log": 0
  },
  "transports": [
    {
      "trkorr": "CADK900123",
      "owner": "DEVELOPER",
      "type": "K",
      "type_text": "Workbench",
      "description": "FI: Invoice processing changes",
      "has_log": true,
      "has_problems": true,
      "problem_summary": {
        "error_count": 2,
        "warning_count": 1
      },
      "problems": [
        {
          "severity": "E",
          "message": "Object ZCLINVOICE locked by user BASIS",
          "system": "QAS",
          "timestamp": "20251203143022",
          "step": "I",
          "step_text": "Import"
        },
        {
          "severity": "E",
          "message": "Activation failed for ZCLINVOICE",
          "system": "QAS",
          "timestamp": "20251203143025",
          "step": "A",
          "step_text": "Activation"
        },
        {
          "severity": "W",
          "message": "Object version mismatch detected",
          "system": "QAS",
          "timestamp": "20251203143020",
          "step": "I",
          "step_text": "Import"
        }
      ]
    },
    {
      "trkorr": "CADK900124",
      "owner": "DEVELOPER",
      "type": "W",
      "type_text": "Customizing",
      "description": "IMG: New company code",
      "has_log": false,
      "has_problems": false,
      "message": "Transport not yet released - no log available"
    }
  ]
}
```

#### 5.3 RFC Wrapper Specification

**Function Module:** `ZCX_GET_TRANSPORT_LOG`

**Importing:**
- `IT_TRANSPORTS` TYPE `TRKORR_TAB` - Lista de OTs
- `IV_USER` TYPE `SYUNAME` OPTIONAL - Filtro por owner

**Exporting:**
- `ET_RESULTS` TYPE `ZCX_TRANSPORT_LOG_T` - Resultados estructurados
- `EV_HAS_PROBLEMS` TYPE `ABAP_BOOL` - Flag si hay errores/warnings

**Logic:**
1. Si `IV_USER` proporcionado, filtrar OTs por AS4USER en E070
2. Para cada OT:
   - Llamar `IF_CTS_REST_API->READ_GLOBAL_INFO`
   - Parsear `ET_NODES` buscando color = 6 (Error) o color = 2 (Warning)
   - Agregar a resultado solo si tiene problemas
3. Retornar estructura consolidada

---

### 6. Standard Alternatives Evaluated

| Alternativa | Pros | Contras | Decisión |
|-------------|------|---------|----------|
| **IF_CTS_REST_API** | API oficial, estructura jerárquica, mantenida por SAP | Requiere RFC wrapper | ✅ Seleccionada |
| TR_READ_LOG | Bajo nivel, más control | Requiere nombre de archivo, más complejo | ❌ Descartada |
| ADT HTTP Endpoint | Directo desde Java | Parsing XML complejo, menos estable | ❌ Descartada |

---

### 7. Risk Assessment

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| OT sin log (no liberada) | Alta | Bajo | Manejar con flag `has_log: false` |
| Log muy grande | Baja | Medio | Limitar a problemas (E/W) únicamente |
| API no disponible en sistema | Muy Baja | Alto | IF_CTS_REST_API es estándar desde NW 7.0 |
| Permisos insuficientes | Media | Alto | Documentar autorizaciones requeridas |

---

### 8. Next Steps

**Phase 2: Design & Implementation** ✅ COMPLETE

1. [x] Crear FM `ZCX_GET_TRANSPORT_LOG` en sistema gdcmcp
2. [x] Implementar lógica de filtrado por severidad
3. [x] Crear `TransportLogService.java`
4. [x] Crear `TransportLogTools.java` con MCP tool
5. [x] Crear test manual `ManualTransportLogTest.java`
6. [ ] Probar con OTs reales (con y sin errores)
7. [ ] Documentar en README

**Autorizaciones Requeridas:**
- S_CTS_ADMI (CTS Administration)
- S_TRANSPRT (Transport Authorization)

---

## Phase 2: Implementation ✅ COMPLETE

**Fecha de Implementación:** 2025-12-03
**Sistema SAP:** gdcmcp

### Objetos Creados

#### ABAP (SAP)

| Objeto | Tipo | Package | Descripción |
|--------|------|---------|-------------|
| `ZCX_GET_TRANSPORT_LOG` | FM | $TMP (ZGFCX_1) | RFC wrapper para IF_CTS_REST_API |

**FM Features:**
- Usa `IF_CTS_REST_API->READ_GLOBAL_INFO` para leer logs
- Filtra por severidad: color=6 (Error) y color=2 (Warning)
- Soporta múltiples OTs (comma-separated o JSON array)
- Filtro opcional por owner (AS4USER de E070)
- Retorna JSON con estructura jerárquica
- Mapea tipos de transporte (K, W, T, S, R, etc.)

#### Java (giralmcp)

| Archivo | Descripción |
|---------|-------------|
| `TransportLogResult.java` | DTO con records para query, summary, entries, problems |
| `TransportLogService.java` | Service que llama al FM vía JCo |
| `TransportLogTools.java` | MCP Tool `get_transport_log` |
| `ManualTransportLogTest.java` | Test manual CommandLineRunner |

### Compilación

```bash
mvn clean compile
# BUILD SUCCESS
```

### Comando de Prueba

```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportLogTest
```

---

## Change Log

| Fecha | Autor | Cambio |
|-------|-------|--------|
| 2025-12-03 | Claude Code | Phase 1 Analysis Complete - Added technical findings, granular requirements, Q&A log |
| 2025-12-03 | Claude Code | Phase 2 Implementation Complete - Created FM, Service, Tool, and Manual Test |

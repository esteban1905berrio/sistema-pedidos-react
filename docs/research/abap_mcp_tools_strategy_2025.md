# Estrategia de Exposición ABAP → MCP Tools 2025

**Fecha:** 2025-11-07
**Autor:** Investigación automatizada (Claude Code)
**Estado:** Hallazgos de investigación
**Contexto:** PR Transport of Copy - Creación de utilidades ABAP consumibles desde MCP Server

---

## Resumen Ejecutivo

Esta investigación evalúa las estrategias óptimas para exponer utilidades ABAP como herramientas (tools) consumibles desde un servidor MCP (Model Context Protocol), con foco en:

- **Conectividad S/4HANA On-Premise** (prioridad 1)
- **Análisis y reporting automatizado** (caso de uso principal)
- **Balance claridad/eficiencia** (optimización de tokens)
- **Acceso total** (lectura + escritura + transportes)

### Hallazgos Clave

1. ✅ **ADT REST API vía RFC** es el enfoque óptimo para On-Premise
2. ⚠️ **No existe documentación oficial** → Discovery vía ADT Communication Log
3. 🎯 **Progressive Discovery Pattern** reduce tokens 70%+
4. 📦 **Workflow-Based Tools** (atómicas) vs API mirrors
5. 🔒 **Stateful Sessions** requeridas para locks

---

## 1. ADT API: Estructura y Descubrimiento

### 1.1 Naturaleza de ADT API

**Definición:**
ADT (ABAP Development Tools) es "una REST API entregada sobre RFC" con doble accesibilidad:

```
┌─────────────────────────────────────┐
│      ADT REST API Endpoints         │
├─────────────────────────────────────┤
│  Acceso RFC:                        │
│  → SADT_REST_RFC_ENDPOINT           │
│  → Stateful por defecto             │
│  → Conexión SAPGUI nativa           │
│                                     │
│  Acceso HTTP/HTTPS:                 │
│  → Transaction SICF                 │
│  → Path: /sap/bc/adt                │
│  → Stateless por defecto            │
└─────────────────────────────────────┘
```

**Conclusión:** Nuestra implementación actual (RFC → `SADT_REST_RFC_ENDPOINT`) es el enfoque correcto para On-Premise.

### 1.2 Método de Descubrimiento

**Problema:** SAP no publica documentación oficial de los endpoints REST ADT.

**Solución:** ADT Communication Log en Eclipse

```bash
# Proceso de descubrimiento:
1. Abrir Eclipse con ADT plugin
2. Realizar operación objetivo (lock, unlock, modify, etc.)
3. Window → Show View → Other → ABAP → ADT Communication Log
4. Inspeccionar:
   - URI del endpoint
   - HTTP Method (GET/POST/PUT/DELETE)
   - Headers (Content-Type, Accept)
   - Body structure (XML/JSON)
   - Response format
```

**Endpoints Confirmados (Proyecto Actual):**

| Endpoint | Método | Propósito |
|----------|--------|-----------|
| `/sap/bc/adt/oo/classes/{name}/source/{type}` | GET | Código fuente de clases |
| `/sap/bc/adt/programs/programs/{name}` | GET | Código fuente de programas |
| `/sap/bc/adt/repository/informationsystem/search` | POST | Búsqueda de objetos |
| `/sap/bc/adt/activation` | POST | Activación de objetos |
| `{object_uri}?_action=LOCK&accessMode=MODIFY` | POST | Lock de objetos |

### 1.3 Stateful vs Stateless

**Comportamiento Crítico:**

```python
# ❌ PROBLEMA: Locks requieren stateful, creates requieren stateless
# Desde Basis 7.51+, ADT soporta stateful sessions

# Solución en RfcAdapter (implementado):
adapter.set_statefulness(True)  # Para locks
adapter.set_statefulness(False) # Para creates
```

**Implicación:** Nuestra arquitectura ya soporta esto correctamente (`RfcAdapter.set_statefulness()`).

---

## 2. Conectividad: RFC vs OData

### 2.1 Comparación Técnica

| Aspecto | RFC (BAPI) | OData API |
|---------|------------|-----------|
| **On-Premise** | ✅ Disponible | ✅ Disponible |
| **Cloud Public Edition** | ❌ No disponible | ✅ Único método |
| **Latencia** | Muy baja (conexión directa) | Media (HTTP overhead) |
| **Stateful Operations** | ✅ Nativo | ⚠️ Requiere session management |
| **Query Capabilities** | Limitadas | Ricas ($filter, $expand, $select) |
| **Documentación** | Pobre (BAPI Explorer) | Buena (SAP API Hub) |
| **Tooling Support** | Limitado | Amplio (Postman, SAP Cloud SDK) |
| **Trend SAP** | Legacy (backward compatibility) | ⭐ Estrategia futura |

### 2.2 Decisión para On-Premise

**Recomendación:** Mantener RFC como mecanismo principal, evaluar OData como complemento.

**Justificación:**

1. ✅ **RFC permite acceso directo a ADT** → Sin necesidad de activar SICF
2. ✅ **Stateful nativo** → Locks funcionan sin complejidad adicional
3. ✅ **Infraestructura existente** → `RfcConnectionPool` ya implementado
4. ✅ **59 tools funcionando** → No hay bloqueadores técnicos
5. ⚠️ **OData útil para:** Consultas complejas, filtrado avanzado, RAP objects

**Estrategia Híbrida:**

```
┌────────────────────────────────────────┐
│   MCP Server (brootpersonalagent)     │
├────────────────────────────────────────┤
│  Core Operations (59 tools):          │
│  → RFC → SADT_REST_RFC_ENDPOINT        │
│  → Classes, Programs, Transports       │
│  → Locks, Activations, Modifications   │
│                                        │
│  Advanced Queries (futuro):            │
│  → HTTP → OData V4                     │
│  → CDS Views, RAP Entities             │
│  → Complex $filter, $expand            │
└────────────────────────────────────────┘
```

---

## 3. Exposición de Function Modules

### 3.1 Métodos Disponibles

#### Método 1: RAP (RESTful ABAP Programming)

**Stack Moderno:**

```abap
@EndUserText.label: 'Custom Entity for FM Exposure'
define custom entity ZCE_TRANSPORT_UTILS
  with parameters
    p_request : abap.char(10)
{
  key request_id : abap.char(10);
      description : abap.char(60);
      owner      : abap.char(12);
      // ...
}
```

```abap
CLASS zcl_rap_query_transport DEFINITION PUBLIC
  IMPLEMENTING INTERFACES if_rap_query_provider.

  METHOD if_rap_query_provider~select.
    " Call FM and transform to RAP format
    CALL FUNCTION 'Z_TRANSPORT_UTILITIES'
      EXPORTING iv_request = lv_request
      IMPORTING et_data = lt_data.
  ENDMETHOD.
ENDCLASS.
```

**Ventajas:**
- ✅ Declarativo (menos código)
- ✅ Auto-generación de OData V4
- ✅ Fiori UI ready
- ✅ Integración CDS nativa

**Desventajas:**
- ❌ Requiere S/4HANA 2020+
- ❌ Curva de aprendizaje
- ❌ No disponible en ECC

#### Método 2: HTTP Handler + SICF (Tradicional)

```abap
CLASS zcl_http_transport_utils DEFINITION PUBLIC
  CREATE PUBLIC.

  PUBLIC SECTION.
    INTERFACES if_http_extension.
ENDCLASS.

CLASS zcl_http_transport_utils IMPLEMENTATION.
  METHOD if_http_extension~handle_request.
    DATA(lv_method) = server->request->get_method( ).
    DATA(lv_path) = server->request->get_header_field( '~path' ).

    CASE lv_method.
      WHEN 'GET'.
        " Handle GET /transport/{id}
      WHEN 'POST'.
        " Handle POST /transport/copy
    ENDCASE.

    server->response->set_content_type( 'application/json' ).
    server->response->set_cdata( lv_json ).
  ENDMETHOD.
ENDCLASS.
```

**Configuración SICF:**
- Transaction: `SICF`
- Path: `/sap/bc/zrest/transport_utils`
- Handler Class: `ZCL_HTTP_TRANSPORT_UTILS`

**Ventajas:**
- ✅ Compatible con cualquier versión
- ✅ Control total del protocolo
- ✅ Simple y directo

**Desventajas:**
- ❌ Más código boilerplate
- ❌ Security manual (session IDs)
- ❌ No autodocumentado

### 3.2 Decisión para el Proyecto

**Opción Recomendada:** Crear Function Modules + Wrapper ADT

```
┌──────────────────────────────────────────────────────┐
│  MCP Tool: transport_copy                            │
│  ↓                                                    │
│  Service: TransportService.copy_transport()          │
│  ↓                                                    │
│  RfcAdapter.request(uri="/sap/bc/adt/cts/...")       │
│  ↓                                                    │
│  SADT_REST_RFC_ENDPOINT (función estándar SAP)       │
│  ↓                                                    │
│  Z_TRANSPORT_COPY (FM custom)                        │
│     ↓                                                 │
│     - CALL FUNCTION 'TRINT_READ_REQUEST'             │
│     - CALL FUNCTION 'TR_COPY_COMM'                   │
│     - Return structured data                         │
└──────────────────────────────────────────────────────┘
```

**Justificación:**
1. ✅ **Reutiliza infraestructura** ADT existente
2. ✅ **Sin necesidad de SICF** → Solo FM en ABAP
3. ✅ **Compatible con arquitectura actual**
4. ✅ **Fácil versionamiento** (activar/desactivar FM)

---

## 4. MCP Design Patterns: 4 Patrones Clave

### 4.1 Pattern 1: Semantic Search

**Problema:** Cargar todas las tool definitions consume tokens excesivos.

**Solución:** Vector DB para discovery dinámico.

```python
# En lugar de:
mcp.list_tools() → 59 tools × 500 tokens = 29,500 tokens

# Usar:
query = "Quiero copiar un transporte"
relevant_tools = vector_search(query, top_k=3)
# → transport_copy, get_transport_objects, create_transport
# 3 tools × 500 tokens = 1,500 tokens (95% reducción)
```

**Implementación Sugerida:**

```python
# app/mcp/semantic_search.py
from sentence_transformers import SentenceTransformer
import chromadb

class ToolDiscovery:
    def __init__(self):
        self.model = SentenceTransformer('all-MiniLM-L6-v2')
        self.client = chromadb.Client()
        self.collection = self.client.create_collection("mcp_tools")

    def index_tools(self, tools):
        for tool in tools:
            embedding = self.model.encode(tool.description)
            self.collection.add(
                documents=[tool.description],
                embeddings=[embedding],
                metadatas=[{"name": tool.name}],
                ids=[tool.name]
            )

    def search(self, query: str, top_k: int = 5):
        query_embedding = self.model.encode(query)
        results = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k
        )
        return results['metadatas'][0]
```

**Decisión:** ⚠️ Evaluar después de Phase 1 (puede ser overkill con 59 tools).

### 4.2 Pattern 2: Workflow-Based Design ⭐

**Principio:** "Think of MCP tools as tailored toolkits that help an AI achieve a particular task, not as API mirrors."

**Antipatrón (API Mirror):**

```python
# ❌ Múltiples tools granulares:
1. create_transport()
2. add_object_to_transport()
3. add_object_to_transport()
4. add_object_to_transport()
5. release_transport()

# 5 tool calls × 2 turns = 10 turns × 500 tokens = 5,000 tokens
```

**Patrón Correcto (Workflow Atómico):**

```python
# ✅ Una tool completa:
deploy_objects_with_transport(
    objects=["ZCLFIE1017_1", "ZSDI1038C_1"],
    transport_desc="Migration Phase 2",
    auto_release=False
)

# 1 tool call × 1 turn = 1 turn × 800 tokens = 800 tokens (84% reducción)
```

**Ejemplo Real del Proyecto:**

```python
# ✅ YA IMPLEMENTADO:
modify_function_module(
    fm_name, fg_name, new_source,
    transport?, auto_activate?, validate_syntax?
)
# → Ejecuta internamente: LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE

# En lugar de:
# lock() → syntax_check() → modify() → unlock() → activate()
```

**Decisión:** ✅ Adoptar para nuevas tools (ya aplicado en `modify_*` workflows).

### 4.3 Pattern 3: Code Mode

**Concepto:** Permitir a Claude escribir código ejecutable en sandbox en lugar de tool calls secuenciales.

**Caso de Uso:** Análisis masivo de transportes.

**Antipatrón (Tool Calls):**

```python
# ❌ 50 llamadas secuenciales:
for request in transport_list:
    result = get_transport_objects(request)
    # Process...
# 50 turns × 1,000 tokens = 50,000 tokens
```

**Patrón Correcto (Code Mode):**

```python
# ✅ Claude escribe código ejecutable:
"""
import mcp_abap_client as sap

requests = sap.list_user_transports(status='D')
analysis = []

for req in requests:
    objects = sap.get_transport_objects(req['number'])
    analysis.append({
        'request': req['number'],
        'object_count': len(objects),
        'has_classes': any(o['type'] == 'CLAS' for o in objects)
    })

print(analysis)  # Solo resultado final al LLM
"""
# 1 ejecución × 200 tokens (resultado) = 200 tokens (99.6% reducción)
```

**Implementación Sugerida:**

```python
# app/mcp/tools/code_execution.py
@mcp.tool()
def execute_abap_analysis(code: str) -> dict:
    """
    Execute Python code in sandbox with SAP client available.
    Available modules: mcp_abap_client, pandas, json
    """
    sandbox = CodeSandbox(
        allowed_modules=['mcp_abap_client', 'pandas', 'json'],
        timeout=60
    )
    result = sandbox.execute(code)
    return {
        "output": result.stdout,
        "errors": result.stderr,
        "execution_time": result.duration
    }
```

**Decisión:** ⭐ Alta prioridad para Phase 2 (análisis/reporting automatizado es el caso de uso principal).

### 4.4 Pattern 4: Progressive Discovery ⭐⭐

**Concepto:** Guiar a Claude a través de etapas lógicas de descubrimiento.

**4 Stages:**

```
Stage 1: Discover Services
┌────────────────────────────┐
│ Tool: list_service_categories()│
│ Return: ["Transport", "CDS", │
│          "RAP", "Repository"]  │
└────────────────────────────┘
        ↓
Stage 2: Identify Category
┌────────────────────────────┐
│ Tool: get_category_tools(  │
│   category="Transport"     │
│ )                          │
│ Return: ["copy", "compare",│
│          "release", ...]   │
└────────────────────────────┘
        ↓
Stage 3: Retrieve Action Details
┌────────────────────────────┐
│ Tool: get_tool_schema(     │
│   category="Transport",    │
│   action="copy"            │
│ )                          │
│ Return: Full schema + docs │
└────────────────────────────┘
        ↓
Stage 4: Execute Operation
┌────────────────────────────┐
│ Tool: transport_copy(      │
│   source="DEVK900123",     │
│   target_system="QAS"      │
│ )                          │
└────────────────────────────┘
```

**Token Analysis:**

```
Traditional (upfront exposure):
- 59 tools × 500 tokens = 29,500 tokens (inicial)

Progressive Discovery:
- Stage 1: 10 categories × 50 tokens = 500 tokens
- Stage 2: 8 tools × 100 tokens = 800 tokens
- Stage 3: 1 schema × 500 tokens = 500 tokens
- Stage 4: Execution
Total: 1,800 tokens (94% reducción)
```

**Implementación Propuesta:**

```python
# app/mcp/tools/discovery.py

TOOL_CATEGORIES = {
    "Repository": {
        "description": "Source code and object operations",
        "tools": ["get_class_source", "search_objects", ...]
    },
    "Transport": {
        "description": "Transport request management",
        "tools": ["create_transport", "get_transport_objects", ...]
    },
    # ...
}

@mcp.tool()
def list_service_categories() -> list[dict]:
    """Stage 1: Discover available service categories."""
    return [
        {"name": k, "description": v["description"]}
        for k, v in TOOL_CATEGORIES.items()
    ]

@mcp.tool()
def get_category_tools(category: str) -> list[str]:
    """Stage 2: Get tools available in a category."""
    return TOOL_CATEGORIES[category]["tools"]

@mcp.tool()
def get_tool_schema(category: str, tool_name: str) -> dict:
    """Stage 3: Get full schema and documentation for a tool."""
    # Return complete schema only when needed
    pass
```

**Decisión:** ⭐⭐ Máxima prioridad - implementar en Phase 1.

---

## 5. Estrategia de Optimización de Tokens

### 5.1 Principios Generales

**Less is More:**

1. **Granularidad Óptima:** Una tool = Un objetivo de usuario completo
2. **Datos Concisos:** Solo información esencial en respuestas
3. **Lazy Loading:** Defer full schemas hasta que sean necesarios
4. **Batch Operations:** Procesar en servidor, no en cliente

### 5.2 Formato de Respuestas

**❌ Antipatrón (Verbose):**

```json
{
  "status": "success",
  "message": "The transport request DEVK900123 has been successfully retrieved",
  "timestamp": "2025-11-07T10:30:00Z",
  "user": "DEVELOPER",
  "data": {
    "transport_request": {
      "number": "DEVK900123",
      "description": "Phase 2 Migration",
      "owner": "DEVELOPER",
      "status": "D",
      "target_system": "QAS",
      "objects": [
        {
          "pgmid": "R3TR",
          "object": "CLAS",
          "obj_name": "ZCLTEST",
          "description": "Test class for migration",
          "package": "ZTEST",
          "author": "DEVELOPER",
          ...
        }
      ]
    }
  }
}
```
**~850 tokens**

**✅ Patrón Correcto (Conciso):**

```json
{
  "request": "DEVK900123",
  "desc": "Phase 2 Migration",
  "owner": "DEVELOPER",
  "status": "D",
  "objects": [
    {"type": "CLAS", "name": "ZCLTEST", "pkg": "ZTEST"},
    {"type": "PROG", "name": "ZREPORT", "pkg": "ZTEST"}
  ]
}
```
**~180 tokens (79% reducción)**

**Decisión:** ✅ Implementar response formatters concisos en todos los services.

### 5.3 Recomendaciones Específicas

| Aspecto | Implementación |
|---------|----------------|
| **Field Names** | Abreviar (description → desc, package → pkg) |
| **Timestamps** | Omitir excepto si necesario |
| **Metadata** | Solo incluir si usuario lo solicita |
| **Error Messages** | Formato compacto: `{"error": "FM_NOT_FOUND", "obj": "ZTEST"}` |
| **Arrays Grandes** | Paginar (max 50 items) + `has_more` flag |
| **XML Responses** | Convertir a JSON compacto |

---

## 6. Arquitectura Propuesta

### 6.1 Layer Structure

```
┌────────────────────────────────────────────────────────────┐
│                    MCP Layer (app/mcp/)                    │
├────────────────────────────────────────────────────────────┤
│  Discovery Tools:                                          │
│  - list_service_categories()                               │
│  - get_category_tools(category)                            │
│  - get_tool_schema(category, tool)                         │
│                                                            │
│  Workflow Tools (atomic):                                  │
│  - deploy_objects_with_transport()                         │
│  - analyze_transport_batch()                               │
│  - compare_and_copy_transport()                            │
│                                                            │
│  Execution Tools (code mode):                              │
│  - execute_abap_analysis(code)                             │
├────────────────────────────────────────────────────────────┤
│                  Service Layer (app/services/)             │
├────────────────────────────────────────────────────────────┤
│  TransportService, CdsService, RapService, ...             │
│  - Orchestrate multi-step operations                       │
│  - Format responses (concise JSON)                         │
│  - Error handling with clear messages                      │
├────────────────────────────────────────────────────────────┤
│                   Core Layer (app/core/)                   │
├────────────────────────────────────────────────────────────┤
│  RfcAdapter:                                               │
│  - request(uri, method, headers, body)                     │
│  - set_statefulness(True/False)                            │
│                                                            │
│  RfcConnectionPool:                                        │
│  - Manage RFC connections                                  │
│  - Health checks                                           │
│  - Retry logic                                             │
├────────────────────────────────────────────────────────────┤
│              ABAP Layer (SAP System)                       │
├────────────────────────────────────────────────────────────┤
│  Standard ADT:                                             │
│  - SADT_REST_RFC_ENDPOINT                                  │
│                                                            │
│  Custom Function Modules:                                  │
│  - Z_TRANSPORT_COPY                                        │
│  - Z_TRANSPORT_COMPARE                                     │
│  - Z_VALIDATE_OBJECT_NAMES                                 │
│  - Z_BATCH_ANALYSIS_UTILITIES                              │
│                                                            │
│  (Exposed via ADT wrapper endpoints)                       │
└────────────────────────────────────────────────────────────┘
```

### 6.2 Tool Organization

**Current (59 tools):**

```
app/mcp/tools/
├── class_tools.py           # 3 tools
├── search_tools.py          # 1 tool
├── program_tools.py         # 3 tools
├── ddic_tools.py            # 4 tools
├── transport_tools.py       # 14 tools
├── modification_tools.py    # 3 tools
├── activation_tools.py      # 3 tools
├── quality_tools.py         # 4 tools
├── lifecycle_tools.py       # 4 tools
├── whereused_tools.py       # 2 tools
├── cds_tools.py             # 4 tools
├── rap_tools.py             # 8 tools
└── enhancement_tools.py     # 3 tools
```

**Proposed (Progressive Discovery + Workflows):**

```
app/mcp/tools/
├── _discovery/
│   ├── __init__.py
│   ├── categories.py        # list_service_categories()
│   ├── tools.py             # get_category_tools()
│   └── schema.py            # get_tool_schema()
│
├── _workflows/              # Atomic operations
│   ├── transport_workflows.py
│   ├── deployment_workflows.py
│   └── analysis_workflows.py
│
├── _execution/              # Code mode
│   └── sandbox.py           # execute_abap_analysis()
│
└── _granular/               # Existing 59 tools (lazy-loaded)
    ├── class_tools.py
    ├── transport_tools.py
    └── ...
```

---

## 7. Plan de Implementación por Fases

### Phase 1: Progressive Discovery (2-3 semanas)

**Objetivo:** Reducir token usage 70%+ sin afectar funcionalidad.

**Tareas:**

1. ✅ Crear estructura `app/mcp/tools/_discovery/`
2. ✅ Implementar `list_service_categories()`
3. ✅ Implementar `get_category_tools()`
4. ✅ Implementar `get_tool_schema()`
5. ✅ Migrar 59 tools a lazy-loading
6. ✅ Testing: Verificar funcionamiento completo
7. ✅ Documentar en `docs/architecture/progressive-discovery.md`

**Entregables:**
- Sistema de discovery funcional
- Reducción medible de tokens (baseline vs progressive)
- Documentación completa

### Phase 2: Workflow-Based Tools (3-4 semanas)

**Objetivo:** Crear 5-8 workflows atómicos para casos de uso común.

**Tareas:**

1. ✅ Identificar workflows más frecuentes
2. ✅ Diseñar signatures de tools workflow-based
3. ✅ Implementar:
   - `deploy_objects_with_transport()`
   - `analyze_transport_batch()`
   - `compare_and_copy_transport()`
   - `validate_and_create_package_structure()`
4. ✅ Testing: Casos de uso end-to-end
5. ✅ Documentar en `docs/architecture/workflow-tools.md`

**Entregables:**
- 5-8 workflow tools funcionales
- Casos de uso documentados
- Comparación performance vs granular tools

### Phase 3: Code Execution Mode (4-5 semanas)

**Objetivo:** Permitir análisis masivo via código ejecutable.

**Tareas:**

1. ✅ Diseñar sandbox seguro (RestrictedPython o similar)
2. ✅ Crear cliente ABAP simplificado (`mcp_abap_client`)
3. ✅ Implementar `execute_abap_analysis(code)`
4. ✅ Whitelist módulos permitidos (pandas, json, etc.)
5. ✅ Testing: Seguridad, timeout, error handling
6. ✅ Documentar en `docs/architecture/code-execution.md`

**Entregables:**
- Sandbox funcional y seguro
- Cliente ABAP simplificado
- Ejemplos de análisis complejos

### Phase 4: Custom ABAP Utilities (5-6 semanas)

**Objetivo:** Crear FMs custom para operaciones no cubiertas por ADT.

**Tareas:**

1. ✅ Diseñar FMs:
   - `Z_TRANSPORT_COPY` (copy across systems)
   - `Z_TRANSPORT_COMPARE` (diff analysis)
   - `Z_VALIDATE_OBJECT_NAMES` (naming conventions)
   - `Z_BATCH_ANALYSIS_UTILITIES` (mass operations)
2. ✅ Crear ADT wrapper endpoints
3. ✅ Implementar services Python correspondientes
4. ✅ Testing: Integración completa
5. ✅ Documentar en `docs/architecture/custom-abap-utilities.md`

**Entregables:**
- 4+ FMs custom funcionando
- Integración ADT completa
- Documentación de interfaces

---

## 8. Recursos ADT Adicionales (Futuro)

### 8.1 Reportes ABAP

**Endpoint Potencial:** `/sap/bc/adt/reports/{report_name}`

**Capacidades:**
- Ejecutar reportes con variantes
- Obtener resultados en formato estructurado
- Análisis de performance (runtime analysis)

**Prioridad:** Media (Phase 5+)

### 8.2 Function Modules via ADT

**Endpoint Potencial:** `/sap/bc/adt/functions/groups/{fg}/fmodules/{fm}`

**Capacidades:**
- Lectura de signatures
- Testing de FMs
- Where-used analysis

**Prioridad:** Alta (ya parcialmente implementado)

### 8.3 Servicios OData

**Endpoint:** Directo (no vía ADT)

**Capacidades:**
- Consultas complejas ($filter, $expand)
- CDS Views consumption
- RAP Entities

**Prioridad:** Media-Alta (complemento a RFC)

---

## 9. Matriz de Decisión

| Criterio | RFC (Actual) | HTTP/ADT | OData | Decisión |
|----------|--------------|----------|-------|----------|
| **On-Premise Support** | ✅ Nativo | ✅ Requiere SICF | ✅ Requiere Gateway | ✅ RFC |
| **Stateful Operations** | ✅ Nativo | ⚠️ Manual | ⚠️ Manual | ✅ RFC |
| **Latency** | ✅ Muy baja | ⚠️ Media | ⚠️ Media | ✅ RFC |
| **Query Capabilities** | ⚠️ Limitadas | ⚠️ Limitadas | ✅ Ricas | ➕ OData (complemento) |
| **Infrastructure** | ✅ Ya implementado | ❌ Requiere trabajo | ❌ Requiere trabajo | ✅ RFC |
| **Cloud Compatibility** | ❌ No disponible | ✅ Disponible | ✅ Disponible | ⚠️ Evaluar futuro |
| **Token Efficiency** | ✅ Progr. Discovery | ✅ Progr. Discovery | ✅ Progr. Discovery | ✅ Todos (con patterns) |
| **Custom Logic** | ✅ FMs custom | ⚠️ HTTP Handlers | ⚠️ RAP | ✅ FMs custom |

**Decisión Final:**

```
PRIMARY: RFC → SADT_REST_RFC_ENDPOINT (59 tools actuales)
├── + Progressive Discovery (Phase 1)
├── + Workflow Tools (Phase 2)
├── + Code Execution (Phase 3)
└── + Custom FMs (Phase 4)

SECONDARY: OData V4 (Phase 5+, caso de uso: complex queries)
```

---

## 10. Métricas de Éxito

### 10.1 Token Optimization

| Métrica | Baseline (Actual) | Target (Post-Implementation) |
|---------|-------------------|------------------------------|
| **Initial Tool Load** | 29,500 tokens | 500 tokens |
| **Average Operation** | 2,000 tokens | 500 tokens |
| **Complex Analysis** | 50,000 tokens | 1,000 tokens |
| **Reduction %** | - | 75-95% |

### 10.2 Performance

| Métrica | Baseline | Target |
|---------|----------|--------|
| **Tool Discovery Time** | N/A | < 200ms |
| **Workflow Execution** | N/A (multiple calls) | Single call |
| **Code Execution Timeout** | N/A | 60s max |
| **RFC Connection Pool Hit Rate** | ~80% | > 95% |

### 10.3 Developer Experience

| Métrica | Baseline | Target |
|---------|----------|--------|
| **Tools Documented** | 59/59 (100%) | 100% + workflows |
| **Code Examples** | README only | Per-tool docs |
| **Error Messages** | Technical | User-friendly |

---

## 11. Referencias

### 11.1 Documentación Oficial

- [SAP ADT Feature Matrix](https://community.sap.com/t5/technology-blog-posts-by-sap/adt-feature-availability-matrix-for-as-abap-releases/ba-p/13027468)
- [ABAP RESTful Programming Model](https://blog.sap-press.com/what-is-the-restful-abap-programming-model)
- [Model Context Protocol Spec](https://modelcontextprotocol.io/specification/2025-06-18)

### 11.2 Community Resources

- [abap-adt-api (GitHub)](https://github.com/marcellourbani/abap-adt-api)
- [mcp-abap-abap-adt-api (GitHub)](https://github.com/mario-andreschak/mcp-abap-abap-adt-api)
- [Joys and Sorrows of ADT API](https://community.sap.com/t5/application-development-and-automation-blog-posts/joys-and-sorrows-of-the-abap-developer-tools-api/ba-p/13409390)

### 11.3 Design Patterns

- [Less is More: 4 MCP Design Patterns](https://dev.to/klavisai/less-is-more-4-design-patterns-for-building-better-mcp-servers-3gpf)
- [Code Execution with MCP](https://www.anthropic.com/engineering/code-execution-with-mcp)
- [Advanced MCP Optimization Guide](https://joelwembo.medium.com/advanced-guide-optimizing-large-language-models-with-model-context-protocol-mcp-performance-2020184dd605)

---

## 12. Conclusiones

### 12.1 Decisiones Técnicas

1. ✅ **Mantener RFC como mecanismo principal** (On-Premise focus)
2. ✅ **Implementar Progressive Discovery** (Phase 1 - Alta prioridad)
3. ✅ **Crear Workflow Tools** (Phase 2 - Alta prioridad)
4. ✅ **Agregar Code Execution** (Phase 3 - Media-Alta prioridad)
5. ✅ **Desarrollar Custom FMs** (Phase 4 - Media prioridad)
6. ⚠️ **Evaluar OData** (Phase 5+ - Complementario)

### 12.2 Beneficios Esperados

| Beneficio | Impacto |
|-----------|---------|
| **Token Reduction** | 75-95% en operaciones típicas |
| **Performance** | 50%+ faster (workflows vs multiple calls) |
| **User Experience** | Menos prompts, más autonomía para Claude |
| **Scalability** | Soporta 100s de tools sin degradación |
| **Maintainability** | Código más organizado y testeable |

### 12.3 Próximos Pasos

1. ✅ Presentar hallazgos al usuario
2. ✅ Obtener aprobación para Phase 1
3. ✅ Crear PR actualizado con plan de fases
4. ✅ Iniciar implementación Phase 1

---

## 13. Evaluación Migración a Java

### 13.1 Problema Crítico: PyRFC Archivado

**Hallazgo Crítico (Diciembre 2024):**

> "SAP can no longer maintain PyRFC due to changing priorities. The latest version is built with an older RFC SDK which is no longer supported by SAP." — **GitHub SAP/PyRFC**

**Estado del Proyecto:**
- ❌ **Repositorio archivado** oficialmente
- ❌ **No acepta Pull Requests** ni issues
- ❌ **RFC SDK obsoleto** (versión anterior no soportada)
- ❌ **Múltiples problemas no resueltos** (2024):
  - Compilación fallida en Raspberry Pi Bookworm
  - Errores `Module 'pyrfc' has no attribute Connection`
  - Instalación imposible en AWS Databricks
  - Builds locales requeridas (no wheels precompilados)

**Implicación:** Mantener PyRFC a largo plazo **NO ES VIABLE**.

### 13.2 Comparación SAP JCo vs PyRFC

| Aspecto | PyRFC | SAP JCo | Ganador |
|---------|-------|---------|---------|
| **Estabilidad Cross-Platform** | ❌ Compilación manual por OS | ✅ Binarios precompilados oficiales | **JCo** |
| **Mantenimiento SAP** | ❌ Archivado (Dic 2024) | ✅ Soporte oficial activo | **JCo** |
| **Thread Safety** | ❌ NO thread-safe (pooling manual) | ✅ Thread-safe nativo | **JCo** |
| **Connection Pooling** | ❌ Implementación manual | ✅ Pooling automático integrado | **JCo** |
| **Distribución** | ❌ Compilar en cada máquina | ✅ JAR + natives (fácil) | **JCo** |
| **Instalación** | ❌ Compleja (setup.sh, compilar) | ✅ Simple (JAR + classpath) | **JCo** |
| **Performance** | ⚠️ AsyncIO ayuda, streaming lento | ✅ JNI optimizado | **JCo** |
| **Production Ready** | ❌ Inestable (según requerimiento) | ✅ Estable en producción | **JCo** |

**Thread Safety PyRFC:**
> "The SAP NW RFC Library is not thread safe, neither the pyrfc is. The recommended design is to instantiate a pool of client instances." — **GitHub Issue #46**

**JCo Thread Safety:**
> "JCoDestination will internally create and use distinct RFC client connection objects for each session, and every thread is treated as separate session by default." — **Stack Overflow**

**JCo Connection Pooling:**
> "In JCo 3.0/3.1 the pooling is done automatically within JCo runtime, if the destination is configured accordingly. The SAP JCo does not allocate connections in advance; the initial request opens the first connection." — **SAP Documentation**

### 13.3 MCP SDK en Java

**✅ SDK Oficial Disponible: Spring AI MCP**

**Lanzamiento:**
- **Febrero 2025**: Spring AI + Anthropic colaboración
- **Repositorio**: [modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)
- **Requisito**: Java 17+
- **Licencia**: MIT

**Arquitectura:**

```
┌─────────────────────────────────────┐
│   Spring AI MCP Java SDK            │
├─────────────────────────────────────┤
│  Client/Server Layer                │
│  → Protocol operations              │
│                                     │
│  Session Layer                      │
│  → McpClientSession / McpServer     │
│                                     │
│  Transport Layer                    │
│  → STDIO, HTTP SSE, WebSocket       │
└─────────────────────────────────────┘
```

**Spring Boot Starters Disponibles:**

```xml
<!-- STDIO Transport (Claude Desktop) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>

<!-- HTTP SSE (Spring MVC) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>

<!-- HTTP SSE (WebFlux Reactive) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
</dependency>
```

**Tool Definition (Anotaciones):**

```java
@Tool(description = "Get ABAP class source code")
public String getClassSource(
    @ToolParam(description = "Class name") String className,
    @ToolParam(description = "Version: active|inactive") String version
) {
    return classService.getSource(className, version);
}
```

**Registro Automático:**
> "In version 1.1.0-M2, manual callback registration is no longer needed as Spring's component scanning handles everything automatically." — **Spring AI Docs**

### 13.4 Matriz de Decisión Arquitectónica

#### Opción A: Full Java Migration 🟢 **RECOMENDADA**

**Stack:**
- Spring Boot 3.x + Spring AI MCP Java SDK
- SAP JCo 3.1.x para conectividad SAP
- Maven/Gradle para build
- Docker para empaquetado

**Pros:**
- ✅ **Estabilidad**: JCo mantenido por SAP, thread-safe
- ✅ **SDK Oficial**: Spring AI MCP con soporte activo
- ✅ **Distribución**: JAR ejecutable + Docker image
- ✅ **Pooling**: Connection pooling automático
- ✅ **Ecosystem**: Spring Boot production-ready
- ✅ **Observabilidad**: Spring Actuator + Micrometer

**Cons:**
- ❌ **Rewrite Completo**: 59 tools + 17 services
- ❌ **Learning Curve**: Spring AI MCP
- ❌ **JCo Licensing**: Verificar licenciamiento SAP
- ❌ **Native Libraries**: Distribución `.so`/`.dll` requerida

**Esfuerzo:** 6-8 semanas (1 desarrollador Java senior)

---

#### Opción B: Arquitectura Híbrida 🟡 **NO RECOMENDADA**

**Variante B1: Python MCP + Java Tools (gRPC)**

```
Claude → Python MCP Server → gRPC → Java Service (JCo)
```

**Pros:**
- ✅ Mantiene capa MCP en Python
- ✅ Reusa JCo para estabilidad

**Cons:**
- ❌ **Complejidad Dual**: 2 runtimes (Python + Java)
- ❌ **Performance Overhead**: Serialización gRPC
- ❌ **Deployment Complejo**: Docker multi-stage
- ❌ **Debugging Difícil**: Cross-language
- ❌ **PyRFC Archivado**: Problema no resuelto

**Performance gRPC:**
> "Python gRPC servers using AsyncIO offer significant performance boost. However, streaming RPCs create extra threads, making them much slower than unary RPCs in Python, unlike other languages." — **gRPC Best Practices**

**Variante B2: Python MCP + Java JNI**

```
Python → JNI → Java (JCo)
```

**Cons:**
- ❌ **JNI Complexity**: Memoria management, JVM lifecycle
- ❌ **Portability**: JNI builds por plataforma
- ❌ **Crash Risk**: Errores JNI crashean proceso

**Conclusión Híbrida:** Añade complejidad sin resolver problema raíz.

---

#### Opción C: Mejorar Python Actual 🔴 **NO VIABLE**

**Estrategias Consideradas:**
1. Fork PyRFC internamente
2. Contribuir fixes a PyRFC
3. Reimplementar pooling robusto

**Por qué NO es viable:**
- ❌ **Proyecto Archivado**: SAP no acepta PRs
- ❌ **Mantenimiento**: Requiere expertise RFC SDK
- ❌ **Thread Safety**: Problema arquitectónico
- ❌ **Cross-Platform**: Compilación local siempre

---

## 14. Estrategia Docker & VPN

### 14.1 Problema: Container + VPN del Host

**Escenario:**
- SAP accesible solo via VPN corporativa (instalada en host)
- Container necesita acceso a SAP
- Evitar instalar VPN dentro de cada container

### 14.2 Solución Recomendada: Network Mode `host`

**Docker Compose:**

```yaml
services:
  mcp-server:
    image: myorg/mcp-sap-server:latest
    network_mode: host  # Container usa red del host
    environment:
      SAP_ASHOST: sap.internal.corp
      SAP_ROUTER: /H/vpn-router/S/3299
```

**Ventajas:**
- ✅ **VPN Transparente**: Container accede a recursos VPN del host
- ✅ **Sin Configuración Extra**: No routing tables custom
- ✅ **Bajo Overhead**: Latencia mínima vs bridge mode

**Desventajas:**
- ⚠️ **Solo Linux**: No funciona en Mac/Windows Docker Desktop
- ⚠️ **Menos Aislamiento**: Container comparte IPs del host
- ⚠️ **Port Conflicts**: Puertos deben estar libres en host

### 14.3 Docker Desktop Integration

**Endpoint Security:**
> "Firewalls, VPNs, and security tools like Crowdstrike see traffic coming from `com.docker.backend` process, so firewall and endpoint security software can apply rules directly to `com.docker.backend`." — **Docker Desktop Docs**

**Aplicar Reglas:**
- Windows: `com.docker.backend.exe`
- Mac: `com.docker.backend`
- Linux: `qemu`

### 14.4 Alternativa: VPN Container + Service Network

```yaml
services:
  vpn:
    image: openvpn-client
    cap_add:
      - NET_ADMIN
    volumes:
      - ./vpn-config:/etc/openvpn

  mcp-server:
    image: myorg/mcp-sap-server
    network_mode: "service:vpn"  # Usa red del container VPN
    depends_on:
      - vpn
```

**Ventajas:**
- ✅ Funciona en Mac/Windows
- ✅ Mejor aislamiento
- ✅ VPN configuration as code

**Desventajas:**
- ❌ Requiere credenciales VPN en container
- ❌ Más complejo de debuggear
- ❌ Depende de VPN client image

### 14.5 Docker MCP Catalog (Distribución Oficial)

**Estadísticas 2025:**
- **220+ MCP servers** en catálogo oficial
- **1M+ pulls** en primeras semanas
- Registry: [github.com/docker/mcp-registry](https://github.com/docker/mcp-registry)
- Hub: [hub.docker.com/mcp](https://hub.docker.com/mcp)

**Proceso de Publicación:**
1. Containerizar MCP server
2. Submit PR a `docker/mcp-registry`
3. Revisión y aprobación
4. **Disponible en <24 horas** en Docker Desktop

**Ventajas:**
- ✅ **Distribución Centralizada**: Discovery via Docker Hub
- ✅ **Seguridad**: Imágenes firmadas digitalmente
- ✅ **Versionado**: Tags semánticos (v1.0.0)
- ✅ **Multi-Platform**: ARM64 + AMD64 automáticos

### 14.6 Dockerfile Multi-Stage con JCo

**Problema JCo:**
> "It is not allowed to rename or repackage the original archive 'sapjco3.jar'. The default system class loader does not handle jar files inside jar files." — **SAP JCo Docs**

**Solución: External Dependencies**

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY lib/sapjco3.jar ./lib/
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy native JCo libraries (platform-specific)
COPY lib/sapjco3.jar /app/lib/
COPY lib/libsapjco3.so /usr/lib/  # Linux x64

# Copy application
COPY --from=builder /app/target/mcp-server.jar /app/

# Configure java.library.path
ENV LD_LIBRARY_PATH=/usr/lib:$LD_LIBRARY_PATH

CMD ["java", "-Djava.library.path=/usr/lib", "-jar", "mcp-server.jar"]
```

**Multi-Platform Builds:**

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag myorg/mcp-sap-server:1.0.0 \
  --push .
```

**⚠️ GraalVM Native Image NO Viable:**

> "Compiling a project containing SAP JCo library fails with a fatal error in GraalVM 23.0.0 CE. The error occurs in `com.sap.i18n.cp.ConverterJNI`." — **GitHub Issue #6970**

**Por qué falla:**
- JCo usa JNI extensivamente
- Reflection dinámica compleja
- Native converters no linkean

**Conclusión:** Deploy como **JAR tradicional**, NO native image.

### 14.7 Observabilidad en Producción

**Problema MCP:**
> "Without observability, it is difficult to trace how an agent made a decision, which tools were invoked, or why certain failures occurred. MCP servers are increasingly labelled as black boxes." — **Glama AI Blog (2025)**

**Retos:**
- MCP usa **Server-Sent Events (SSE)** + HTTP Streaming
- Tooling tradicional no diseñado para protocolos asíncronos
- Alto volumen de datos (deep context)

**Soluciones 2025:**

| Solución | Fortaleza | Integración |
|----------|-----------|-------------|
| **Sentry** | MCP específico, 1-line instrumentation | JavaScript SDK |
| **OpenTelemetry** | Estándar industria | Java Agent, Spring Boot |
| **Moesif** | JSON-RPC deep visibility | API platform |
| **SigNoz** | Open-source, self-hosted | OpenTelemetry backend |

**Health Checks Pattern:**

```java
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/liveness")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("ALIVE");
    }

    @GetMapping("/readiness")
    public ResponseEntity<HealthStatus> readiness() {
        HealthStatus status = new HealthStatus();
        status.setSapConnection(checkSapConnection());
        status.setDependencies(checkDependencies());

        return status.isHealthy()
            ? ResponseEntity.ok(status)
            : ResponseEntity.status(503).body(status);
    }
}
```

**Checks Recomendados:**
- ✅ **Liveness**: Proceso corriendo
- ✅ **Readiness**: SAP conectividad OK
- ✅ **Dependencies**: Redis, PostgreSQL, etc.

**Spring Boot Actuator:**

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Métricas Custom JCo:**

```java
@Component
public class JCoMetrics {
    public JCoMetrics(MeterRegistry registry) {
        Gauge.builder("jco.pool.active", pool, Pool::getActive)
             .register(registry);
    }
}
```

---

## 15. Recomendación Final Consolidada

### 15.1 Decisión Arquitectónica

**✅ Full Java Migration con Spring AI MCP**

**Justificación Técnica:**

1. **PyRFC Archivado (Crítico)**
   - Proyecto oficialmente abandonado (Dic 2024)
   - RFC SDK obsoleto no soportado
   - Múltiples issues sin resolver
   - **NO VIABLE a largo plazo**

2. **JCo Estabilidad Probada**
   - Thread-safe nativo
   - Connection pooling automático
   - Binarios precompilados multi-platform
   - Soporte oficial SAP activo

3. **MCP SDK Oficial Java**
   - Spring AI MCP (Feb 2025)
   - Soporte activo Anthropic + Spring
   - Ecosystem production-ready
   - Anotaciones declarativas

4. **Distribución Simplificada**
   - Docker MCP Catalog oficial (220+ servers)
   - JAR ejecutable + natives externalizados
   - Multi-platform builds automáticos
   - VPN: `network_mode: host` funciona

5. **Ecosystem Empresarial**
   - Spring Boot: Health checks, Actuator, Metrics
   - OpenTelemetry: Observabilidad estándar
   - Prometheus/Grafana: Monitoring integrado
   - Stack empresarial conocido

### 15.2 Riesgos Mitigados

| Riesgo | Mitigación |
|--------|------------|
| **Cross-platform builds** | Docker multi-arch automático |
| **Native libraries JCo** | Externalizadas en Dockerfile |
| **VPN corporate** | `network_mode: host` transparente |
| **Mantenimiento** | Stack Spring Boot estándar |
| **Distribución** | Docker MCP Catalog oficial |
| **Observabilidad** | OpenTelemetry + Spring Actuator |

### 15.3 Plan de Implementación Consolidado

**Estrategia Dual:**

```
┌──────────────────────────────────────────┐
│  FASE 1-4: Optimización Python          │
│  (10-14 semanas)                         │
├──────────────────────────────────────────┤
│  Phase 1: Progressive Discovery (2-3w)  │
│  Phase 2: Workflow Tools (3-4w)         │
│  Phase 3: Code Execution (4-5w)         │
│  Phase 4: Custom ABAP FMs (5-6w)        │
│                                          │
│  RESULTADO: 75-95% token reduction      │
└──────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────┐
│  FASE 5: Migración Java                 │
│  (6-8 semanas)                           │
├──────────────────────────────────────────┤
│  1. Setup & POC (1 semana)               │
│     - Spring Boot + Spring AI MCP        │
│     - SAP JCo integration                │
│     - Docker image con natives           │
│                                          │
│  2. Core Services (2 semanas)            │
│     - Migrar ClassService, ProgramSvc    │
│     - JCo connection pooling             │
│     - Unit + Integration tests           │
│                                          │
│  3. Remaining Tools (2 semanas)          │
│     - 59 MCP tools migration             │
│     - Transport/CDS/RAP tools            │
│                                          │
│  4. Production Ready (1 semana)          │
│     - Health checks, Actuator, Metrics   │
│     - OpenTelemetry instrumentation      │
│     - Docker multi-platform              │
│                                          │
│  5. Testing & Docs (1 semana)            │
│     - Load testing (JMeter)              │
│     - Security scanning (Trivy)          │
│     - Migration guide                    │
│                                          │
│  6. Deployment (1 semana)                │
│     - Staging deployment                 │
│     - Production rollout                 │
│     - Monitoring setup                   │
│                                          │
│  RESULTADO: Sistema estable, escalable  │
└──────────────────────────────────────────┘
```

**Total Estimado:** 16-22 semanas (4-5.5 meses)

### 15.4 Métricas de Éxito Consolidadas

| Métrica | Baseline (Actual) | Post Phase 1-4 | Post Phase 5 (Java) |
|---------|-------------------|----------------|---------------------|
| **Token Usage** | 29,500 inicial | 500 (98% ↓) | 500 (maintained) |
| **Estabilidad** | PyRFC inestable | PyRFC (temporal) | JCo estable ✅ |
| **Cross-Platform** | Compilación manual | Compilación manual | Binarios oficiales ✅ |
| **Distribución** | setup.sh complejo | Docker image | Docker MCP Catalog ✅ |
| **Mantenimiento** | PyRFC archivado ❌ | PyRFC archivado ❌ | JCo mantenido ✅ |
| **Observabilidad** | Logs básicos | Logs básicos | OpenTelemetry ✅ |

### 15.5 Decisiones Pendientes

| Decisión | Responsable | Deadline |
|----------|-------------|----------|
| ⚠️ **Verificar licenciamiento SAP JCo** | Team Lead | Antes Phase 5 |
| ✅ **Aprobar Phase 1 (Progressive Discovery)** | Product Owner | Inmediato |
| ✅ **Asignar recursos Phase 5 (Java)** | Engineering Manager | Post-Phase 4 |
| ✅ **Preparar infraestructura Docker** | DevOps | Post-Phase 4 |

### 15.6 Próximos Pasos Inmediatos

1. ✅ **Iniciar Phase 1**: Progressive Discovery en Python (2-3 semanas)
2. ✅ **Preparar POC Java**: Spring Boot + JCo en paralelo (R&D)
3. ⚠️ **Validar Licensing**: Contactar SAP sobre JCo producción
4. ✅ **Configurar CI/CD**: Docker builds multi-platform
5. ✅ **Documentar arquitectura**: Target state (Java)

---

**Fecha de última actualización:** 2025-11-07
**Versión:** 2.0 (Ampliada con evaluación Java + Docker)
**Autor:** Claude Code (Automated Research)

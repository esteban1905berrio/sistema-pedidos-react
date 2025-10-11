# Plan de Migración de Funcionalidades MCP ABAP ADT

**Fecha**: 2025-01-10
**Versión**: 2.0 (Clarificado enfoque RFC)
**Autor**: Bastian Root

## 📌 Resumen Ejecutivo

**Objetivo**: Migrar 50+ tools de los proyectos de referencia (mcp-abap-abap-adt-api y abap-adt-py) a nuestra implementación **usando RFC en lugar de HTTP**.

**Diferenciador Clave**: Mientras los proyectos de referencia usan HTTP/ADT APIs directamente, nosotros utilizamos **RfcAdapter** para ejecutar las mismas operaciones via RFC (`SADT_REST_RFC_ENDPOINT`), eliminando la necesidad de tener ADT HTTP habilitado en SAP.

**Alcance**:
- ✅ Migrar TODAS las tools (no solo un subset)
- ✅ Mantener RfcAdapter como capa de transporte
- ✅ Reutilizar lógica de parsing y validaciones
- ✅ Reutilizar interfaces MCP y schemas
- ❌ NO cambiar a HTTP directo

---

## Análisis Comparativo de Funcionalidades

### Estado Actual: brootpersonalagent (21 tools implementados)

#### ✅ **Implementados**

**Fase Inicial** (6 tools):
1. `get_class_source` - Obtener código fuente de clases
2. `get_class_structure` - Estructura y metadatos de clases
3. `get_object_source` - Código fuente genérico por URI
4. `search_objects` - Búsqueda de objetos con wildcards
5. `get_program_source` - Código fuente de programas
6. `get_include_source` - Código fuente de includes

**FASE 1 - Repository & Source Retrieval** (9 tools):
7. `get_class_includes` - Lista de includes de una clase (main, test, macros)
8. `get_class_components` - Componentes de clase categorizados por tipo
9. `get_object_structure` - Estructura de cualquier objeto ABAP por URI
10. `get_object_types` - Lista de todos los tipos de objetos ABAP disponibles
11. `adt_discovery` - Capacidades ADT del sistema
12. `get_feature_details` - Detalles de features ADT específicas
13. `get_node_contents` - Navegación del árbol del repository
14. `find_object_path` - Ruta completa de un objeto en el repository
15. *(mainPrograms - pendiente)*

**FASE 2 - DDIC & Data Dictionary** (6 tools):
16. `get_ddic_element` - Definiciones DDIC (tablas, estructuras, dominios, etc.)
17. `ddic_repository_access` - Acceso directo al repository DDIC
18. `get_annotation_definitions` - Definiciones de anotaciones CDS
19. `package_search_help` - Búsqueda y autocompletado de paquetes
20. `get_table_contents` - Previsualización de contenido de tablas
21. `run_query` - Ejecutar queries personalizadas

---

## Proyectos de Referencia

### **mcp-abap-abap-adt-api** (TypeScript) - 100+ tools
- **Ubicación**: `mcp-abap-abap-adt-api/`
- **Fuente**: https://github.com/mario-andreschak/mcp-abap-abap-adt-api
- **Implementación**: HTTP/ADT APIs vía abap-adt-api library
- **Conexión**: HTTP requests directos a ADT REST endpoints
- **Requisito**: ADT HTTP habilitado en SAP system

### **abap-adt-py** (Python) - 15+ operaciones
- **Ubicación**: `abap-adt-py/`
- **Fuente**: Python library for ADT REST API
- **Implementación**: HTTP requests directos a ADT
- **Conexión**: `requests.Session` con HTTP Basic Auth
- **Requisito**: ADT HTTP habilitado en SAP system

### **Uso de los Proyectos de Referencia**

| Componente | Qué REUTILIZAMOS | Qué NO migramos |
|------------|------------------|-----------------|
| **abap-adt-py** | ✅ Lógica de parsing XML<br>✅ Estructura de requests<br>✅ Validaciones de parámetros<br>✅ Manejo de errores | ❌ Capa de conexión HTTP<br>❌ `requests.Session`<br>❌ HTTP Basic Auth |
| **mcp-abap-adt-api** | ✅ Interfaces MCP tools<br>✅ ADT URI endpoints<br>✅ Schemas de parámetros<br>✅ Workflows y patrones | ❌ Capa de conexión HTTP<br>❌ `abap-adt-api` library<br>❌ HTTP transport |

**Nuestro Valor Diferencial**:
- ✅ **Todas** las funcionalidades de ambos proyectos
- ✅ **RFC** en lugar de HTTP (vía `SADT_REST_RFC_ENDPOINT`)
- ✅ **No requiere** ADT HTTP habilitado en SAP
- ✅ **Mayor seguridad**: Sin exposición HTTP directa

---

## Enfoque y Arquitectura de Migración

### 🎯 ¿Qué Estamos Migrando?

**MIGRAMOS**: TODAS las tools de los proyectos de referencia (50+ tools)
**ADAPTAMOS**: La capa de conexión de HTTP a RFC
**REUTILIZAMOS**: Lógica de parsing, interfaces MCP, validaciones
**MANTENEMOS**: Nuestra arquitectura RFC existente

### 🏗️ Stack Tecnológico Comparativo

#### **Proyectos de Referencia (HTTP)**
```
┌─────────────────────────────────────────┐
│  MCP Tools (FastMCP / TypeScript)       │
├─────────────────────────────────────────┤
│  Business Logic (Python / TypeScript)   │
├─────────────────────────────────────────┤
│  HTTP Client (requests / abap-adt-api)  │ ← Usamos HTTP
├─────────────────────────────────────────┤
│  Network (HTTP/HTTPS)                   │
├─────────────────────────────────────────┤
│  SAP ADT REST API (HTTP endpoints)      │
└─────────────────────────────────────────┘
  Requisito: ADT HTTP habilitado en SAP
```

#### **Nuestra Implementación (RFC)** 🔥
```
┌─────────────────────────────────────────┐
│  MCP Tools (FastMCP Python)             │ ← Mismas interfaces
├─────────────────────────────────────────┤
│  Services Layer (Python)                │ ← Misma lógica
├─────────────────────────────────────────┤
│  RfcAdapter (HTTP-style interface)      │ ← NUESTRA INNOVACIÓN
├─────────────────────────────────────────┤
│  PyRFC (Python RFC SDK bindings)        │
├─────────────────────────────────────────┤
│  SAP NetWeaver RFC SDK (librfc32)       │
├─────────────────────────────────────────┤
│  SADT_REST_RFC_ENDPOINT (RFC FM)        │ ← Llamamos via RFC
├─────────────────────────────────────────┤
│  SAP ADT REST API (backend)             │ ← Mismo backend
└─────────────────────────────────────────┘
  Requisito: SAP RFC SDK instalado localmente
  Ventaja: NO requiere ADT HTTP habilitado
```

### 🔑 Componente Clave: RfcAdapter

El `RfcAdapter` es nuestra **capa de abstracción** que permite:
- Usar la misma lógica de los proyectos de referencia
- Mantener una interfaz HTTP-style familiar
- Ejecutar requests via RFC en lugar de HTTP

**Ejemplo - Referencia vs Nuestra Implementación:**

```python
# ============================================
# REFERENCIA: abap-adt-py (HTTP)
# ============================================
import requests

session = requests.Session()
session.auth = HTTPBasicAuth(username, password)

response = session.get(
    url=f"{sap_host}/sap/bc/adt/oo/classes/ZTEST/source/main",
    headers={"Accept": "*/*"},
    params={"version": "active"}
)

source_code = response.text  # Código fuente


# ============================================
# NUESTRA IMPLEMENTACIÓN (RFC)
# ============================================
from app.core.rfc_adapter import RfcAdapter
from pyrfc import Connection

conn = Connection(ashost=sap_host, sysnr=sysnr, ...)
adapter = RfcAdapter(conn)

response = adapter.request(
    uri="/sap/bc/adt/oo/classes/ZTEST/source/main",
    method="GET",
    params={"version": "active"},
    body=""
)

source_code = response.text  # Mismo resultado!
```

**Flujo de Datos:**

```
Request HTTP-style
      ↓
RfcAdapter.request()
      ↓
Build RFC request dict
      ↓
conn.call("SADT_REST_RFC_ENDPOINT", REQUEST=request_dict)
      ↓
RFC call to SAP
      ↓
SADT_REST_RFC_ENDPOINT ejecuta HTTP call internamente
      ↓
ADT REST API procesa
      ↓
Response vía RFC
      ↓
RfcResponse (wrapper HTTP-style)
      ↓
Same interface as HTTP response!
```

### 📋 Proceso de Migración de cada Tool

Para cada tool de los proyectos de referencia seguimos este proceso:

```
1. ANALIZAR referencias
   ├─ abap-adt-py: ¿Tiene implementación?
   │  └─ SÍ: Reusar lógica de parsing y validaciones
   │  └─ NO: Investigar en TypeScript project
   └─ mcp-abap-abap-adt-api: ¿Tiene tool?
      └─ SÍ: Reusar interface MCP y ADT URI

2. IDENTIFICAR componentes
   ├─ ADT URI endpoint (de referencias)
   ├─ Método HTTP (GET/POST/PUT/DELETE)
   ├─ Parámetros y body (de referencias)
   └─ Response format (XML/JSON)

3. IMPLEMENTAR Service
   ├─ Crear método en Service apropiado
   ├─ Usar RfcAdapter.request() ← NO HTTP directo
   ├─ Reusar parsing de abap-adt-py (si existe)
   └─ Manejo de errores

4. CREAR MCP Tool
   ├─ Decorador @mcp.tool()
   ├─ Schema según TypeScript reference
   └─ Llamar Service method

5. TESTING
   ├─ Unit test (mock RfcAdapter)
   └─ Integration test (RFC real)
```

### 📊 Tabla Comparativa: HTTP vs RFC

| Aspecto | HTTP (Referencias) | RFC (Nuestro) | Ganador |
|---------|-------------------|---------------|---------|
| **Requisitos** | ADT HTTP habilitado | RFC SDK instalado | = |
| **Seguridad** | Exposición HTTP | Sin HTTP expuesto | ✅ RFC |
| **Firewall** | Puerto HTTP (44300+) | Puerto RFC (33XX) | = |
| **Performance** | HTTP + Red | RFC (binario) | ✅ RFC |
| **Complejidad Setup** | Baja (HTTP simple) | Media (SDK install) | ❌ HTTP |
| **Tools Disponibles** | 50+ tools | 6 → 50+ (migrando) | = |
| **Compatibilidad** | SAP >= 7.40 SP08 | SAP >= 4.6C | ✅ RFC |
| **Debugging** | Fácil (HTTP logs) | Media (RFC traces) | ❌ HTTP |

**Conclusión**: RFC ofrece ventajas de seguridad y no requiere ADT HTTP, ideal para ambientes restringidos.

---

## Nuevo Orden de Implementación

### **PRIORIDADES REVISADAS**

1. **FASE 1**: Repository & Source Retrieval (Lectura de código y metadatos)
2. **FASE 2**: DDIC & Data Dictionary (Metadatos del diccionario)
3. **FASE 3**: Transport Management (Gestión de transportes)
4. **FASE 4**: Object Modification (Edición de objetos)
5. **FASE 5**: Code Quality (Calidad de código)
6. **FASE 6**: Unit Testing & Advanced Features

---

## Plan de Implementación por Fases

### **FASE 1: Repository & Source Retrieval** ✅ COMPLETADA
**Objetivo**: Completar capacidades de lectura de código fuente y metadatos del repository
**Estado**: ✅ Implementada (2025-01-10)
**Archivos creados**:
- `app/services/discovery_service.py`
- `app/services/navigation_service.py`
- `app/mcp/tools/discovery_tools.py`
- `app/mcp/tools/navigation_tools.py`
**Archivos extendidos**:
- `app/services/class_service.py` (3 nuevos métodos)
- `app/mcp/tools/class_tools.py` (3 nuevas tools)

**🔑 Enfoque de Migración para esta Fase**:
- Tools que NO existen en abap-adt-py (solo TypeScript)
- Implementar desde cero usando RfcAdapter
- Reusar patrones de parsing XML de tools similares

**Ejemplo de Migración - `classIncludes`**:

```python
# ============================================
# REFERENCIA: mcp-abap-abap-adt-api (TypeScript)
# ============================================
# GET /sap/bc/adt/oo/classes/{class_name}/includes
# NO existe en abap-adt-py

# ============================================
# NUESTRA IMPLEMENTACIÓN (RFC)
# ============================================
class ClassService:
    def get_class_includes(self, class_name: str) -> List[dict]:
        """Get all includes of a class via RFC."""

        # Llamada via RfcAdapter (NO HTTP)
        response = self.adapter.request(
            uri=f"/sap/bc/adt/oo/classes/{class_name}/includes",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            # Parse XML (reusar patrón de get_class_structure)
            includes = self._parse_includes_xml(response.text)
            return includes
        else:
            raise Exception(f"Failed to get includes: {response.text}")
```

#### Sprint 1.1: Enhanced Object Structure & Discovery
**Tools a implementar**:

1. **`objectStructure` (mejorado)**
   - **Service**: Extender `app/services/class_service.py`
   - **Método**: `get_object_structure(object_uri: str) -> dict`
   - **ADT URI**: `GET {object_uri}/objectstructure`
   - **Descripción**: Versión genérica para cualquier tipo de objeto (no solo clases)
   - **Referencia**: `mcp-abap-abap-adt-api/ObjectHandlers.ts`
   - **Prioridad**: Alta

2. **`findObjectPath`**
   - **Service**: `app/services/search_service.py` (extender)
   - **Método**: `find_object_path(uri: str) -> dict`
   - **ADT URI**: `GET /sap/bc/adt/repository/nodestructure?uri={uri}`
   - **Descripción**: Obtener ruta completa del objeto en el árbol del repository
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

3. **`objectTypes`**
   - **Service**: `app/services/discovery_service.py` (nuevo)
   - **Método**: `get_object_types() -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/repository/typestructure`
   - **Descripción**: Lista de todos los tipos de objetos ABAP disponibles
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

#### Sprint 1.2: Class-Specific Metadata
**Tools a implementar**:

4. **`classIncludes`**
   - **Service**: `app/services/class_service.py`
   - **Método**: `get_class_includes(class_name: str) -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/oo/classes/{class_name}/includes`
   - **Descripción**: Lista de todos los includes de una clase (main, test, macros, etc.)
   - **Referencia**: TypeScript only
   - **Prioridad**: Alta

5. **`classComponents`**
   - **Service**: `app/services/class_service.py`
   - **Método**: `get_class_components(class_name: str) -> dict`
   - **ADT URI**: Similar a objectStructure pero específico para componentes de clase
   - **Descripción**: Métodos, atributos, eventos, tipos internos
   - **Referencia**: TypeScript only
   - **Prioridad**: Alta

#### Sprint 1.3: Node Navigation & Main Programs
**Tools a implementar**:

6. **`nodeContents`**
   - **Service**: `app/services/navigation_service.py` (nuevo)
   - **Método**: `get_node_contents(object_uri: str, node_uri: str) -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/repository/nodestructure`
   - **Descripción**: Contenido de un nodo del repository (paquete, carpeta, etc.)
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

7. **`mainPrograms`**
   - **Service**: `app/services/program_service.py` (extender)
   - **Método**: `get_main_programs(include_uri: str) -> List[str]`
   - **ADT URI**: Basado en include context
   - **Descripción**: Obtener programas principales que usan un include
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

#### Sprint 1.4: Advanced Discovery
**Tools a implementar**:

8. **`featureDetails`**
   - **Service**: `app/services/discovery_service.py`
   - **Método**: `get_feature_details(feature_name: str) -> dict`
   - **ADT URI**: `GET /sap/bc/adt/discovery`
   - **Descripción**: Detalles de features ADT disponibles
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

9. **`adtDiscovery`**
   - **Método**: `adt_discovery() -> dict`
   - **ADT URI**: `GET /sap/bc/adt/discovery`
   - **Descripción**: Capacidades ADT del sistema
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

**Testing Fase 1**:
- Test de cada tool con objetos reales
- Test de navegación por el repository
- Validación de estructuras de clases complejas

---

### **FASE 2: DDIC & Data Dictionary** ✅ COMPLETADA
**Objetivo**: Acceso a metadatos del diccionario de datos ABAP
**Estado**: ✅ Implementada (2025-01-10)
**Archivos creados**:
- `app/services/ddic_service.py`
- `app/services/query_service.py`
- `app/mcp/tools/ddic_tools.py`
- `app/mcp/tools/query_tools.py`
**Archivos actualizados**:
- `app/mcp/server.py` (registrados todos los nuevos servicios y tools)

#### Sprint 2.1: Dictionary Elements
**Tools a implementar**:

1. **`ddicElement`**
   - **Service**: `app/services/ddic_service.py` (nuevo)
   - **Método**: `get_ddic_element(element_name: str, element_type: str) -> dict`
   - **ADT URI**: `GET /sap/bc/adt/ddic/{type}/{name}`
   - **Descripción**: Obtener definición de elementos DDIC (tablas, estructuras, tipos)
   - **Tipos soportados**:
     - `tables` - Tablas de base de datos
     - `structures` - Estructuras
     - `dataelements` - Elementos de datos
     - `domains` - Dominios
     - `tableTypes` - Tipos tabla
   - **Referencia**: TypeScript only
   - **Prioridad**: Alta

2. **`ddicRepositoryAccess`**
   - **Service**: `app/services/ddic_service.py`
   - **Método**: `ddic_repository_access(path: str) -> dict`
   - **ADT URI**: `GET /sap/bc/adt/ddic/repository/{path}`
   - **Descripción**: Acceso directo al repository DDIC
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

#### Sprint 2.2: Annotations & Metadata
**Tools a implementar**:

3. **`annotationDefinitions`**
   - **Service**: `app/services/ddic_service.py`
   - **Método**: `get_annotation_definitions() -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/ddic/annotations`
   - **Descripción**: Definiciones de anotaciones CDS disponibles
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

4. **`packageSearchHelp`**
   - **Service**: `app/services/ddic_service.py`
   - **Método**: `package_search_help(query: str) -> List[str]`
   - **ADT URI**: `GET /sap/bc/adt/packages`
   - **Descripción**: Búsqueda de paquetes con autocompletado
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

#### Sprint 2.3: Data Queries
**Tools a implementar**:

5. **`tableContents`**
   - **Service**: `app/services/query_service.py` (nuevo)
   - **Método**: `get_table_contents(table_name: str, max_rows: int = 100, where_clause: str = None) -> dict`
   - **ADT URI**: `POST /sap/bc/adt/datapreview/freestyle`
   - **Descripción**: Previsualización de contenido de tablas
   - **Parámetros**:
     - `table_name`: Nombre de la tabla
     - `max_rows`: Límite de filas (default: 100)
     - `where_clause`: Condición WHERE opcional
   - **Referencia**: TypeScript only
   - **Prioridad**: Alta

6. **`runQuery`**
   - **Service**: `app/services/query_service.py`
   - **Método**: `run_query(query_definition: dict) -> dict`
   - **ADT URI**: Query execution endpoint
   - **Descripción**: Ejecutar queries personalizadas
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

**Testing Fase 2**:
- Test con tablas estándar (T000, USR02, DD02L, etc.)
- Test con estructuras y tipos de datos
- Validación de queries con WHERE clauses
- Test de búsqueda de paquetes

---

### **FASE 3: Transport Management** (2 días)
**Objetivo**: Gestión completa de órdenes de transporte

**🔑 Enfoque de Migración para esta Fase**:
- Tools que SOLO existen en TypeScript (abap-adt-py NO tiene transport management)
- Implementación completamente nueva usando RfcAdapter
- Parsing de XML específico de CTS (Change Transport System)

**Ejemplo de Migración - `transportInfo`**:

```python
# ============================================
# REFERENCIA: mcp-abap-abap-adt-api (TypeScript)
# ============================================
# GET /sap/bc/adt/cts/transportinformation
# NO existe en abap-adt-py - Solo en TypeScript

# ============================================
# NUESTRA IMPLEMENTACIÓN (RFC)
# ============================================
class TransportService:
    def transport_info(self, obj_source_url: str, dev_class: str = None) -> dict:
        """Get transport info via RFC (not HTTP)."""

        params = {"uri": obj_source_url}
        if dev_class:
            params["devclass"] = dev_class

        # Llamada via RfcAdapter
        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transportinformation",
            method="GET",
            params=params,
            body=""
        )

        if response.status_code == 200:
            # Parse XML de transporte
            transport_data = self._parse_transport_info(response.text)
            return {
                "transport_number": transport_data.get("number"),
                "status": transport_data.get("status"),
                "locked_by": transport_data.get("locked_by")
            }
        else:
            raise Exception(f"Failed to get transport info: {response.text}")

    def _parse_transport_info(self, xml_text: str) -> dict:
        """Parse CTS XML response."""
        root = ET.fromstring(xml_text)
        # Namespaces específicos de CTS
        ns = {"tm": "http://www.sap.com/adt/cts/transports"}
        # Extraer información del XML...
        return {...}
```

#### Sprint 3.1: Transport Info & Creation
**Tools a implementar**:

1. **`transportInfo`**
   - **Service**: `app/services/transport_service.py` (nuevo)
   - **Método**: `transport_info(obj_source_url: str, dev_class: str = None, operation: str = None) -> dict`
   - **ADT URI**: `GET /sap/bc/adt/cts/transportinformation`
   - **Parámetros**:
     - `obj_source_url`: URI del objeto
     - `dev_class`: Clase de desarrollo (opcional)
     - `operation`: Tipo de operación (opcional)
   - **Retorna**: Información de transporte incluyendo TRKORR, locks, etc.
   - **Referencia**: TypeScript only (critical)
   - **Prioridad**: 🔥 Crítica

2. **`createTransport`**
   - **Service**: `app/services/transport_service.py`
   - **Método**: `create_transport(obj_source_url: str, request_text: str, devclass: str, transport_layer: str = None) -> str`
   - **ADT URI**: `POST /sap/bc/adt/cts/transports`
   - **Body**: XML con datos del transporte
   - **Parámetros**:
     - `obj_source_url`: URI del objeto
     - `request_text`: Descripción del transporte
     - `devclass`: Paquete/clase desarrollo
     - `transport_layer`: Capa de transporte (opcional)
   - **Retorna**: Transport number (TRKORR)
   - **Referencia**: TypeScript only (critical)
   - **Prioridad**: 🔥 Crítica

#### Sprint 3.2: Transport Configuration
**Tools a implementar**:

3. **`hasTransportConfig`**
   - **Método**: `has_transport_config() -> bool`
   - **ADT URI**: `GET /sap/bc/adt/cts/transports/config`
   - **Descripción**: Verificar si existe configuración de transportes
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

4. **`transportConfigurations`**
   - **Método**: `get_transport_configurations() -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/cts/transports/configurations`
   - **Descripción**: Listar configuraciones de transporte disponibles
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

5. **`getTransportConfiguration`**
   - **Método**: `get_transport_configuration(url: str) -> dict`
   - **Parámetros**:
     - `url`: URL de la configuración
   - **Descripción**: Obtener configuración específica de transporte
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

6. **`setTransportsConfig`**
   - **Método**: `set_transports_config(uri: str, etag: str, config: dict) -> dict`
   - **Descripción**: Configurar settings de transporte
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

7. **`createTransportsConfig`**
   - **Método**: `create_transports_config() -> dict`
   - **Descripción**: Crear nueva configuración de transporte
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

#### Sprint 3.3: User Transports & Operations
**Tools a implementar**:

8. **`userTransports`**
   - **Método**: `user_transports(user: str, targets: bool = False) -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/cts/transports`
   - **Parámetros**:
     - `user`: Usuario SAP
     - `targets`: Incluir sistemas destino (opcional)
   - **Descripción**: Lista de transportes de un usuario
   - **Referencia**: TypeScript only
   - **Prioridad**: Alta

9. **`transportsByConfig`**
   - **Método**: `transports_by_config(config_uri: str, targets: bool = False) -> List[dict]`
   - **Descripción**: Transportes según configuración
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

10. **`transportRelease`**
    - **Método**: `transport_release(transport_number: str, ignore_locks: bool = False, ignore_atc: bool = False) -> dict`
    - **ADT URI**: `POST /sap/bc/adt/cts/transports/{trkorr}/release`
    - **Parámetros**:
      - `transport_number`: TRKORR
      - `ignore_locks`: Ignorar locks (opcional)
      - `ignore_atc`: Ignorar checks ATC (opcional)
    - **Descripción**: Liberar orden de transporte
    - **Referencia**: TypeScript only
    - **Prioridad**: Alta

11. **`transportDelete`**
    - **Método**: `transport_delete(transport_number: str) -> bool`
    - **ADT URI**: `DELETE /sap/bc/adt/cts/transports/{trkorr}`
    - **Descripción**: Eliminar transporte (solo si no está liberado)
    - **Referencia**: TypeScript only
    - **Prioridad**: Media

#### Sprint 3.4: Transport Collaboration
**Tools a implementar**:

12. **`transportSetOwner`**
    - **Método**: `transport_set_owner(transport_number: str, target_user: str) -> bool`
    - **ADT URI**: `POST /sap/bc/adt/cts/transports/{trkorr}/owner`
    - **Descripción**: Cambiar propietario del transporte
    - **Referencia**: TypeScript only
    - **Prioridad**: Media

13. **`transportAddUser`**
    - **Método**: `transport_add_user(transport_number: str, user: str) -> bool`
    - **ADT URI**: `POST /sap/bc/adt/cts/transports/{trkorr}/users`
    - **Descripción**: Añadir usuario colaborador al transporte
    - **Referencia**: TypeScript only
    - **Prioridad**: Media

14. **`systemUsers`**
    - **Método**: `get_system_users() -> List[str]`
    - **ADT URI**: `GET /sap/bc/adt/cts/users`
    - **Descripción**: Lista de usuarios del sistema SAP
    - **Referencia**: TypeScript only
    - **Prioridad**: Baja

15. **`transportReference`**
    - **Método**: `transport_reference(pgmid: str, obj_wbtype: str, obj_name: str, tr_number: str = None) -> dict`
    - **ADT URI**: `GET /sap/bc/adt/cts/transportreference`
    - **Parámetros**:
      - `pgmid`: Program ID (ej: 'R3TR')
      - `obj_wbtype`: Workbench type (ej: 'PROG', 'CLAS')
      - `obj_name`: Nombre del objeto
      - `tr_number`: Número de transporte (opcional)
    - **Descripción**: Referencias de transporte de un objeto
    - **Referencia**: TypeScript only
    - **Prioridad**: Media

**Testing Fase 3**:
- Test de creación de transporte en sistema DEV
- Test de obtención de transport info
- Test de asignación de objetos a transportes
- Test de listado de transportes de usuario
- Test de liberación (SOLO en sistema de pruebas, NO en producción)
- Test de colaboración (añadir usuarios)

---

### **FASE 4: Object Modification** (2-3 días)
**Objetivo**: Habilitar edición completa de objetos ABAP

**🔑 Enfoque de Migración para esta Fase**:
- Tools que SÍ existen en abap-adt-py (referencia de lógica disponible!)
- Reutilizar parsing y validaciones de abap-adt-py
- Adaptar solo la capa de conexión a RfcAdapter

**Ejemplo de Migración - `lock`**:

```python
# ============================================
# REFERENCIA: abap-adt-py/api/lock.py (HTTP)
# ============================================
from requests import Session

def lock(http_request_parameters, object_uri: str) -> str:
    response = request(
        http_request_parameters=http_request_parameters,
        uri=object_uri,
        body="",
        params={"_action": "LOCK", "accessMode": "MODIFY"},
        method="POST",
    )
    if response.status_code == 200:
        lock_handle = find_xml_element_text(response.text, ".//LOCK_HANDLE")
        return lock_handle
    else:
        raise Exception(f"Failed to lock {object_uri}")


# ============================================
# NUESTRA IMPLEMENTACIÓN (RFC)
# ============================================
class ObjectService:
    def lock(self, object_uri: str, access_mode: str = "MODIFY") -> str:
        """Lock object via RFC (not HTTP)."""

        # Mismo patrón, pero via RfcAdapter
        response = self.adapter.request(
            uri=object_uri,
            method="POST",
            params={"_action": "LOCK", "accessMode": access_mode},
            body=""
        )

        if response.status_code == 200:
            # Reusar lógica de parsing de abap-adt-py
            lock_handle = self._extract_lock_handle(response.text)
            return lock_handle
        else:
            raise Exception(f"Failed to lock {object_uri}: {response.text}")

    def _extract_lock_handle(self, xml_text: str) -> str:
        """Extract LOCK_HANDLE from XML (same logic as abap-adt-py)."""
        root = ET.fromstring(xml_text)
        lock_handle_elem = root.find(".//LOCK_HANDLE")
        if lock_handle_elem is not None:
            return lock_handle_elem.text
        raise Exception("LOCK_HANDLE not found in response")
```

**Lo que cambia**: `requests.Session` → `RfcAdapter`
**Lo que se reutiliza**: Lógica de parsing, validaciones, manejo de errores

#### Sprint 4.1: Lock/Unlock
**Tools a implementar**:

1. **`lock`**
   - **Service**: `app/services/object_service.py` (nuevo)
   - **Método**: `lock(object_uri: str, access_mode: str = "MODIFY") -> str`
   - **ADT URI**: `POST {object_uri}?_action=LOCK&accessMode=MODIFY`
   - **Parámetros**:
     - `object_uri`: URI del objeto ABAP
     - `access_mode`: Modo de acceso (default: "MODIFY")
   - **Retorna**: `LOCK_HANDLE` (string) extraído del response XML/headers
   - **Referencia**: Ambos (Python: `abap-adt-py/api/lock.py`, TypeScript: `ObjectLockHandlers.ts`)
   - **Prioridad**: 🔥 Crítica

2. **`unlock`**
   - **Service**: `app/services/object_service.py`
   - **Método**: `unlock(object_uri: str, lock_handle: str) -> bool`
   - **ADT URI**: `POST {object_uri}?_action=UNLOCK&lockHandle={handle}`
   - **Parámetros**:
     - `object_uri`: URI del objeto ABAP
     - `lock_handle`: Handle obtenido del lock
   - **Retorna**: `True` si unlock exitoso
   - **Referencia**: Ambos
   - **Prioridad**: 🔥 Crítica

#### Sprint 4.2: Source Management
**Tools a implementar**:

3. **`setObjectSource`**
   - **Service**: `app/services/class_service.py` (método ya existe `set_class_source`)
   - **MCP Tool**: Crear en `app/mcp/tools/class_tools.py`
   - **Método**: Ya implementado como `set_object_source(object_uri, source_code, lock_handle)`
   - **ADT URI**: `PUT {object_uri}?lockHandle={handle}`
   - **Parámetros**:
     - `object_uri`: URI del objeto (con `/source/main`)
     - `source_code`: Código fuente completo
     - `lock_handle`: Handle del lock
     - `transport`: Número de transporte (opcional)
   - **Referencia**: Ambos
   - **Prioridad**: 🔥 Crítica
   - **Nota**: Ya existe método en service, solo falta exponerlo como MCP tool

#### Sprint 4.3: Activation
**Tools a implementar**:

4. **`activate` / `activateByName`**
   - **Service**: `app/services/activation_service.py` (nuevo)
   - **Método**: `activate(object_name: str, object_uri: str, preaudit: bool = True) -> dict`
   - **ADT URI**: `POST /sap/bc/adt/activation`
   - **Query Params**: `?method=activate&preauditRequested=true`
   - **Body XML**:
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <adtcore:objectReferences xmlns:adtcore="http://www.sap.com/adt/core">
         <adtcore:objectReference adtcore:uri="{object_uri}" adtcore:name="{object_name}"/>
     </adtcore:objectReferences>
     ```
   - **Response**: Parse XML para verificar `activationExecuted="true"` o `generationExecuted="true"`
   - **Referencia**: Ambos (Python: `abap-adt-py/api/activate.py`, TypeScript: `ObjectManagementHandlers.ts`)
   - **Prioridad**: 🔥 Crítica

5. **`activateObjects`**
   - **Método**: `activate_objects(objects: List[dict], preaudit: bool = True) -> dict`
   - **ADT URI**: `POST /sap/bc/adt/activation`
   - **Descripción**: Activar múltiples objetos en batch
   - **Body XML**: Múltiples `<adtcore:objectReference>` en el mismo request
   - **Referencia**: TypeScript only
   - **Prioridad**: Alta

6. **`inactiveObjects`**
   - **Método**: `get_inactive_objects() -> List[dict]`
   - **ADT URI**: `GET /sap/bc/adt/activation/inactiveobjects`
   - **Descripción**: Lista de objetos inactivos del usuario actual
   - **Retorna**: Lista de objetos con URI, nombre, tipo, usuario, etc.
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

**Testing Fase 4**:
- Test workflow completo: lock → edit → activate → unlock
- Test con diferentes tipos de objetos (clase, programa, include)
- Test de manejo de errores de lock (objeto ya locked por otro usuario)
- Test de activación con errores de sintaxis
- Test de activación batch de múltiples objetos
- Test de listado de objetos inactivos

---

### **FASE 5: Code Quality** (1-2 días)
**Objetivo**: Validación y formato de código

#### Sprint 5.1: Syntax Check
**Tools a implementar**:

1. **`syntaxCheck` / `syntaxCheckCode`**
   - **Service**: `app/services/code_quality_service.py` (nuevo)
   - **Método**: `syntax_check(object_uri: str, include_uri: str, source: str, version: str = "active") -> List[dict]`
   - **ADT URI**: `POST /sap/bc/adt/checkruns?reporters=abapCheckRun`
   - **Body XML**:
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <chkrun:checkObjectList xmlns:chkrun="http://www.sap.com/adt/checkrun" xmlns:adtcore="http://www.sap.com/adt/core">
       <chkrun:checkObject adtcore:uri="{object_uri}" chkrun:version="{version}">
         <chkrun:artifacts>
           <chkrun:artifact chkrun:contentType="text/plain; charset=utf-8" chkrun:uri="{include_uri}">
             <chkrun:content>{base64_encoded_source}</chkrun:content>
           </chkrun:artifact>
         </chkrun:artifacts>
       </chkrun:checkObject>
     </chkrun:checkObjectList>
     ```
   - **Parámetros**:
     - `object_uri`: URI del objeto (ej: `/sap/bc/adt/oo/classes/ZTEST`)
     - `include_uri`: URI del include (ej: `/sap/bc/adt/oo/classes/ZTEST/source/main`)
     - `source`: Código fuente a verificar
     - `version`: "active" o "inactive"
   - **Response**: Parse XML para extraer mensajes de error/warning con línea, offset, tipo, texto
   - **Referencia**: Ambos (Python: `abap-adt-py/api/syntax.py`, TypeScript: `CodeAnalysisHandlers.ts`)
   - **Prioridad**: 🔥 Alta

2. **`syntaxCheckCdsUrl`**
   - **Método**: `syntax_check_cds(cds_url: str) -> List[dict]`
   - **ADT URI**: `POST /sap/bc/adt/checkruns` (variant para CDS)
   - **Descripción**: Syntax check específico para CDS views
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

3. **`syntaxCheckTypes`**
   - **Método**: `get_syntax_check_types() -> List[str]`
   - **ADT URI**: `GET /sap/bc/adt/checkruns/types`
   - **Descripción**: Tipos de checks disponibles en el sistema
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

#### Sprint 5.2: Pretty Printer
**Tools a implementar**:

4. **`prettyPrinter`**
   - **Service**: `app/services/code_quality_service.py`
   - **Método**: `prettyprint(source: str) -> str`
   - **ADT URI**: `POST /sap/bc/adt/abapsource/prettyprinter`
   - **Content-Type**: `text/plain`
   - **Body**: Código fuente sin formatear
   - **Retorna**: Código fuente formateado
   - **Referencia**: Ambos (Python: `abap-adt-py/api/prettyprint.py`, TypeScript: `PrettyPrinterHandlers.ts`)
   - **Prioridad**: Alta

5. **`prettyPrinterSetting`**
   - **Método**: `get_prettyprint_settings() -> dict`
   - **ADT URI**: `GET /sap/bc/adt/abapsource/prettyprinter/settings`
   - **Descripción**: Obtener configuración actual de pretty printer
   - **Retorna**: Dict con `indentation` (bool) y `style` (string)
   - **Referencia**: Ambos
   - **Prioridad**: Media

6. **`setPrettyPrinterSetting`**
   - **Método**: `set_prettyprint_settings(indent: bool, style: str) -> bool`
   - **ADT URI**: `PUT /sap/bc/adt/abapsource/prettyprinter/settings`
   - **Parámetros**:
     - `indent`: True/False para indentación
     - `style`: Estilo de formato (ej: "keywordUpper", "keywordLower")
   - **Descripción**: Configurar pretty printer del usuario
   - **Referencia**: Ambos
   - **Prioridad**: Media

**Testing Fase 5**:
- Test de syntax check con código correcto (sin errores)
- Test de syntax check con errores de sintaxis (verificar líneas y mensajes)
- Test de syntax check con warnings
- Test de pretty printer con código sin formatear
- Test de configuración de pretty printer
- Test de pretty printer con diferentes estilos

---

### **FASE 6: Object Lifecycle & Unit Testing** (2 días)

#### Sprint 6.1: Create/Delete Objects
**Tools a implementar**:

1. **`create` / `createObject`**
   - **Service**: `app/services/creation_service.py` (nuevo)
   - **Método**: `create(object_type: str, name: str, parent: str, description: str, responsible: str) -> bool`
   - **ADT URI**: Varía según tipo de objeto
     - Clases: `POST /sap/bc/adt/oo/classes`
     - Programas: `POST /sap/bc/adt/programs/programs`
   - **Body XML**: Específico para cada tipo de objeto
   - **Parámetros**:
     - `object_type`: Tipo de objeto (ej: "PROG/P", "CLAS/OC")
     - `name`: Nombre del objeto
     - `parent`: Paquete padre (ej: "$TMP", "ZPACKAGE")
     - `description`: Descripción del objeto
     - `responsible`: Usuario responsable
   - **Referencia**: Ambos (Python: `abap-adt-py/api/create.py`, TypeScript: `ObjectRegistrationHandlers.ts`)
   - **Prioridad**: Alta

2. **`validateNewObject`**
   - **Método**: `validate_new_object(name: str, type: str, parent: str) -> dict`
   - **ADT URI**: `POST /sap/bc/adt/objects/validation`
   - **Descripción**: Validar antes de crear (nombre disponible, paquete válido, etc.)
   - **Retorna**: Dict con validación y mensajes de error si aplica
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

3. **`delete` / `deleteObject`**
   - **Service**: `app/services/creation_service.py`
   - **Método**: `delete(object_uri: str, lock_handle: str) -> bool`
   - **ADT URI**: `DELETE {object_uri}?lockHandle={handle}`
   - **Parámetros**:
     - `object_uri`: URI del objeto a eliminar
     - `lock_handle`: Handle del lock (objeto debe estar locked)
   - **Referencia**: Ambos (Python: `abap-adt-py/api/delete.py`, TypeScript: `ObjectDeletionHandlers.ts`)
   - **Prioridad**: Alta
   - **⚠️ CUIDADO**: Operación destructiva, requiere confirmación

4. **`objectRegistrationInfo`**
   - **Método**: `get_object_registration_info(object_type: str) -> dict`
   - **ADT URI**: `GET /sap/bc/adt/objects/registration/{type}`
   - **Descripción**: Información de registro de tipos de objetos (templates, campos requeridos)
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

#### Sprint 6.2: Unit Testing
**Tools a implementar**:

5. **`runUnitTest` / `unitTestRun`**
   - **Service**: `app/services/unittest_service.py` (nuevo)
   - **Método**: `run_unit_test(object_uri: str, flags: dict = None) -> List[dict]`
   - **ADT URI**: `POST /sap/bc/adt/abapunit/testruns`
   - **Body XML**:
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <aunit:runConfiguration xmlns:aunit="http://www.sap.com/adt/aunit">
       <external>
         <coverage active="false"/>
       </external>
       <adtcore:objectSets xmlns:adtcore="http://www.sap.com/adt/core">
         <objectSet kind="inclusive">
           <adtcore:objectReferences>
             <adtcore:objectReference adtcore:uri="{object_uri}"/>
           </adtcore:objectReferences>
         </objectSet>
       </adtcore:objectSets>
     </aunit:runConfiguration>
     ```
   - **Parámetros**:
     - `object_uri`: URI del objeto con tests
     - `flags`: Flags opcionales (coverage, risk level, duration, etc.)
   - **Response**: Parse XML para obtener resultados de tests (alerts, successes, failures)
   - **Referencia**: Ambos (Python: `abap-adt-py/api/unittest.py`, TypeScript: `UnitTestHandlers.ts`)
   - **Prioridad**: Alta

6. **`unitTestEvaluation`**
   - **Método**: `unit_test_evaluation(class_name: str, flags: dict = None) -> dict`
   - **ADT URI**: Variant de unit test run con evaluación
   - **Descripción**: Evaluación detallada de resultados de tests
   - **Referencia**: TypeScript only
   - **Prioridad**: Media

7. **`createTestInclude`**
   - **Método**: `create_test_include(class_name: str, lock_handle: str, transport: str = None) -> bool`
   - **ADT URI**: `POST /sap/bc/adt/oo/classes/{class_name}/includes` (type: testclasses)
   - **Parámetros**:
     - `class_name`: Nombre de la clase
     - `lock_handle`: Lock de la clase
     - `transport`: Número de transporte (opcional)
   - **Descripción**: Crear include de test classes para una clase
   - **Referencia**: Ambos (Python: `abap-adt-py/api/create.py`, TypeScript: `UnitTestHandlers.ts`)
   - **Prioridad**: Media

8. **`unitTestOccurrenceMarkers`**
   - **Método**: `get_test_occurrence_markers(url: str, source: str) -> List[dict]`
   - **ADT URI**: `POST /sap/bc/adt/abapunit/occurrencemarkers`
   - **Descripción**: Marcadores de cobertura de tests en el código
   - **Retorna**: Ubicaciones de código cubierto/no cubierto por tests
   - **Referencia**: TypeScript only
   - **Prioridad**: Baja

**Testing Fase 6**:
- Test de creación de objetos simples (programa, clase) en $TMP
- Test de validación antes de crear
- Test de eliminación de objetos de prueba
- Test de ejecución de unit tests en clases con tests
- Test de creación de test include
- Test de interpretación de resultados de tests (success/failure/error)

---

## Arquitectura de Implementación

### Nuevos Archivos a Crear (Orden de Prioridad)

```
app/
├── services/
│   ├── ✅ discovery_service.py       # FASE 1: Object types, ADT discovery
│   ├── ✅ navigation_service.py      # FASE 1: Node contents, navigation
│   ├── ✅ ddic_service.py           # FASE 2: DDIC elements, annotations
│   ├── ✅ query_service.py          # FASE 2: Table queries, data preview
│   ├── 🔲 transport_service.py      # FASE 3: Transport management
│   ├── 🔲 object_service.py         # FASE 4: Lock/unlock operations
│   ├── 🔲 activation_service.py     # FASE 4: Activation logic
│   ├── 🔲 code_quality_service.py   # FASE 5: Syntax check, prettyprint
│   ├── 🔲 creation_service.py       # FASE 6: Create/delete objects
│   └── 🔲 unittest_service.py       # FASE 6: Unit test execution
├── mcp/tools/
│   ├── ✅ discovery_tools.py        # FASE 1: Register discovery tools
│   ├── ✅ navigation_tools.py       # FASE 1: Register navigation tools
│   ├── ✅ ddic_tools.py            # FASE 2: Register DDIC tools
│   ├── ✅ query_tools.py           # FASE 2: Register query tools
│   ├── 🔲 transport_tools.py       # FASE 3: Register transport tools
│   ├── 🔲 lock_tools.py            # FASE 4: Register lock/unlock tools
│   ├── 🔲 activation_tools.py      # FASE 4: Register activation tools
│   ├── 🔲 code_quality_tools.py    # FASE 5: Register quality tools
│   ├── 🔲 creation_tools.py        # FASE 6: Register creation/deletion tools
│   └── 🔲 unittest_tools.py        # FASE 6: Register unit test tools
└── tests/
    ├── 🔲 test_discovery.py         # Tests para FASE 1
    ├── 🔲 test_navigation.py        # Tests para FASE 1
    ├── 🔲 test_ddic.py             # Tests para FASE 2
    ├── 🔲 test_query.py            # Tests para FASE 2
    ├── 🔲 test_transport.py        # Tests para FASE 3
    ├── 🔲 test_object_service.py   # Tests para FASE 4
    ├── 🔲 test_activation.py       # Tests para FASE 4
    ├── 🔲 test_code_quality.py     # Tests para FASE 5
    ├── 🔲 test_creation.py         # Tests para FASE 6
    ├── 🔲 test_unittest.py         # Tests para FASE 6
    └── 🔲 test_integration_workflow.py  # End-to-end workflow tests
```

### Patrón de Implementación Estándar

Para cada tool seguir este patrón consistente:

```python
# 1. Service Method (app/services/{category}_service.py)
class TransportService:
    """Service for transport management operations."""

    def __init__(self, connection: Connection):
        self.adapter = RfcAdapter(connection)

    def transport_info(self, obj_source_url: str, dev_class: str = None) -> dict:
        """
        Get transport information for an object.

        Args:
            obj_source_url: URI of the object
            dev_class: Development class (optional)

        Returns:
            dict: Transport information including TRKORR, locks, etc.
        """
        params = {"uri": obj_source_url}
        if dev_class:
            params["devclass"] = dev_class

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transportinformation",
            method="GET",
            params=params,
            body=""
        )

        if response.status_code == 200:
            # Parse XML response
            transport_data = self._parse_transport_info(response.text)
            return transport_data
        else:
            raise Exception(f"{response.status_code} - Failed to get transport info")

    def _parse_transport_info(self, xml_text: str) -> dict:
        """Parse transport info XML response."""
        # Implementation...
        pass

# 2. MCP Tool Registration (app/mcp/tools/transport_tools.py)
from mcp.server.fastmcp import FastMCP
from pydantic import Field
from app.services.transport_service import TransportService

def register_transport_tools(mcp: FastMCP, transport_service: TransportService):
    """Register transport-related tools with MCP server."""

    @mcp.tool(
        name="transportInfo",
        description="Get transport information for an ABAP object. "
                   "Returns transport number, locks, and other transport metadata."
    )
    def transport_info(
        obj_source_url: str = Field(
            description="URI of the object (e.g., '/sap/bc/adt/oo/classes/ZTEST')"
        ),
        dev_class: str = Field(
            default=None,
            description="Development class (optional)"
        )
    ) -> dict:
        """Get transport information for an object."""
        return transport_service.transport_info(obj_source_url, dev_class)

# 3. Register in MCP Server (app/mcp/server.py)
from app.services.transport_service import TransportService
from app.mcp.tools.transport_tools import register_transport_tools

# Initialize service
transport_service = TransportService(conn)

# Register tools
register_transport_tools(mcp, transport_service)

# 4. Unit Test (app/tests/test_transport.py)
import pytest
from app.services.transport_service import TransportService

def test_transport_info(mock_connection):
    """Test getting transport info for an object."""
    service = TransportService(mock_connection)

    # Mock response
    mock_connection.set_response("""
        <?xml version="1.0"?>
        <tm:root xmlns:tm="http://www.sap.com/adt/cts/transports">
            <tm:number>DEVK900123</tm:number>
            <tm:status>modifiable</tm:status>
        </tm:root>
    """)

    result = service.transport_info("/sap/bc/adt/oo/classes/ZTEST")

    assert result["number"] == "DEVK900123"
    assert result["status"] == "modifiable"
```

---

## Resumen de Tools por Fase

| Fase | # Tools | Estado | Valor % | Prioridad |
|------|---------|--------|---------|-----------|
| **FASE 1**: Repository & Source | 9 tools | ✅ COMPLETADA | 20% | Media |
| **FASE 2**: DDIC & Dictionary | 6 tools | ✅ COMPLETADA | 15% | Alta |
| **FASE 3**: Transport Management | 15 tools | 🔲 Pendiente | 30% | 🔥 Crítica |
| **FASE 4**: Object Modification | 6 tools | 🔲 Pendiente | 25% | 🔥 Crítica |
| **FASE 5**: Code Quality | 6 tools | 🔲 Pendiente | 5% | Alta |
| **FASE 6**: Lifecycle & Testing | 8 tools | 🔲 Pendiente | 5% | Media |
| **TOTAL** | **50 tools** | **35% completado** | **100%** | - |

---

## Métricas de Éxito por Fase

### FASE 1: Repository & Source Retrieval
- ✅ Navegar árbol completo del repository SAP
- ✅ Obtener estructura de cualquier tipo de objeto (no solo clases)
- ✅ Listar todos los includes de una clase (main, test, macros)
- ✅ Obtener componentes detallados de clases (métodos, atributos, eventos)
- ✅ Descubrir capacidades ADT del sistema

### FASE 2: DDIC & Data Dictionary
- ✅ Leer definiciones de tablas, estructuras, elementos de datos, dominios
- ✅ Consultar contenido de tablas con WHERE clauses y límite de filas
- ✅ Acceder a definiciones de anotaciones CDS
- ✅ Buscar paquetes con autocompletado
- ✅ Ejecutar queries personalizadas en el sistema

### FASE 3: Transport Management
- ✅ Obtener información de transporte para objetos
- ✅ Crear transportes programáticamente con descripción y devclass
- ✅ Asignar objetos a transportes existentes
- ✅ Listar transportes de usuario
- ✅ Liberar transportes (en sistemas de desarrollo)
- ✅ Gestión de usuarios colaboradores en transportes
- ✅ Configuración de transportes del sistema

### FASE 4: Object Modification
- ✅ Workflow completo: lock → edit → activate → unlock
- ✅ Lock exitoso con obtención de LOCK_HANDLE
- ✅ Modificación de source code con lock válido
- ✅ Activación de objetos con validación preaudit
- ✅ Activación batch de múltiples objetos
- ✅ Manejo de objetos inactivos
- ✅ Gestión de errores (lock conflicts, activation errors)

### FASE 5: Code Quality
- ✅ Syntax check con mensajes detallados (línea, offset, tipo, texto)
- ✅ Syntax check para CDS views
- ✅ Pretty print con configuración personalizada
- ✅ Configuración persistente de pretty printer
- ✅ Diferentes estilos de formato (keywordUpper, keywordLower, etc.)

### FASE 6: Object Lifecycle & Testing
- ✅ Crear objetos (clases, programas) en paquetes o $TMP
- ✅ Validar antes de crear (nombre disponible, paquete válido)
- ✅ Eliminar objetos con seguridad (requiere lock)
- ✅ Ejecutar unit tests y obtener resultados
- ✅ Crear test includes para clases
- ✅ Interpretación de resultados de tests (success/failure/error)
- ✅ Cobertura de tests (occurrence markers)

---

## Estrategia de Testing

### Tests Unitarios
Cada service debe tener tests unitarios que:
- Mockean el RfcAdapter
- Validan parsing de responses XML
- Verifican manejo de errores
- Cubren casos edge (parámetros opcionales, valores vacíos)

### Tests de Integración
Cada fase debe tener tests de integración que:
- Usan conexión RFC real a sistema SAP de desarrollo
- Validan workflow end-to-end
- Crean y limpian objetos de prueba
- NO afectan datos productivos

### Test Environments
- **Desarrollo (DEV)**: Para todos los tests de creación/modificación
- **Testing (TST)**: Para validación de transportes
- **⚠️ NUNCA ejecutar tests destructivos en producción (PRD)**

---

## Consideraciones de Seguridad

### Operaciones Críticas
Las siguientes operaciones requieren validación extra:

1. **`delete` / `deleteObject`**: Operación destructiva
   - Requiere confirmación del usuario
   - Solo en sistemas DEV/TST
   - Validar que objeto está locked

2. **`transportRelease`**: Liberar transporte
   - Solo en sistemas DEV
   - Validar que no hay errores ATC (a menos que ignore_atc=true)
   - Confirmar con usuario antes de liberar

3. **`setObjectSource`**: Modificar código
   - Requiere lock válido
   - Validar sintaxis antes con `syntaxCheck`
   - Guardar backup del código original

4. **`activate`**: Activar objetos
   - Validar preaudit results
   - No activar si hay errores críticos
   - Informar al usuario de warnings

### Environment Variables
Validar en `.env`:
```bash
SAP_ASHOST=<dev-system>  # Nunca apuntar a PRD en desarrollo
SAP_CLIENT=<dev-client>  # Usar cliente de desarrollo
SAP_USER=<dev-user>      # Usuario con permisos apropiados
```

---

## Documentación de ADT URIs

### Tabla de Referencia de Endpoints Principales

| Categoría | Endpoint | Método | Descripción |
|-----------|----------|--------|-------------|
| **Discovery** |
| Object Types | `/sap/bc/adt/repository/typestructure` | GET | Tipos de objetos |
| ADT Discovery | `/sap/bc/adt/discovery` | GET | Capacidades ADT |
| Features | `/sap/bc/adt/discovery/{feature}` | GET | Detalles de feature |
| **Repository** |
| Object Structure | `{object_uri}/objectstructure` | GET | Estructura de objeto |
| Node Structure | `/sap/bc/adt/repository/nodestructure` | GET | Árbol de repository |
| Class Includes | `/sap/bc/adt/oo/classes/{name}/includes` | GET | Includes de clase |
| **DDIC** |
| DDIC Element | `/sap/bc/adt/ddic/{type}/{name}` | GET | Elemento DDIC |
| Annotations | `/sap/bc/adt/ddic/annotations` | GET | Anotaciones CDS |
| Packages | `/sap/bc/adt/packages` | GET | Búsqueda paquetes |
| Data Preview | `/sap/bc/adt/datapreview/freestyle` | POST | Contenido tablas |
| **Transport** |
| Transport Info | `/sap/bc/adt/cts/transportinformation` | GET | Info de transporte |
| Create Transport | `/sap/bc/adt/cts/transports` | POST | Crear transporte |
| User Transports | `/sap/bc/adt/cts/transports` | GET | Transportes usuario |
| Release Transport | `/sap/bc/adt/cts/transports/{trkorr}/release` | POST | Liberar |
| **Lock/Unlock** |
| Lock | `{object_uri}?_action=LOCK` | POST | Bloquear objeto |
| Unlock | `{object_uri}?_action=UNLOCK` | POST | Desbloquear |
| **Source** |
| Get Source | `{object_uri}/source/main` | GET | Leer código |
| Set Source | `{object_uri}/source/main` | PUT | Escribir código |
| **Activation** |
| Activate | `/sap/bc/adt/activation` | POST | Activar objetos |
| Inactive Objects | `/sap/bc/adt/activation/inactiveobjects` | GET | Objetos inactivos |
| **Quality** |
| Syntax Check | `/sap/bc/adt/checkruns` | POST | Verificar sintaxis |
| Pretty Print | `/sap/bc/adt/abapsource/prettyprinter` | POST | Formatear código |
| **Lifecycle** |
| Create Object | `/sap/bc/adt/{type}/{path}` | POST | Crear objeto |
| Delete Object | `{object_uri}` | DELETE | Eliminar objeto |
| **Unit Tests** |
| Run Tests | `/sap/bc/adt/abapunit/testruns` | POST | Ejecutar tests |

---

## Recomendaciones Finales

### Enfoque de Migración (IMPORTANTE)

**✅ LO QUE HACEMOS**:
1. Migrar **TODAS** las 50+ tools de los proyectos de referencia
2. Usar **RfcAdapter** para todas las conexiones (NO HTTP directo)
3. Reutilizar lógica de parsing de abap-adt-py cuando exista
4. Reutilizar interfaces MCP de mcp-abap-abap-adt-api
5. Mantener misma funcionalidad, diferente transporte

**❌ LO QUE NO HACEMOS**:
1. NO cambiar RfcAdapter a HTTP
2. NO implementar solo subset de tools
3. NO duplicar implementación HTTP de referencias
4. NO ignorar los proyectos de referencia

**🎯 OBJETIVO FINAL**:
- 50+ tools funcionando via RFC
- Misma funcionalidad que mcp-abap-abap-adt-api
- Ventaja: NO requiere ADT HTTP habilitado en SAP
- Arquitectura: RfcAdapter como capa de abstracción

### Priorización
1. **FASE 3 (Transport)** y **FASE 4 (Modification)** son las más críticas para workflows productivos
2. **FASE 1 (Repository)** y **FASE 2 (DDIC)** establecen fundamentos sólidos
3. **FASE 5 (Quality)** mejora experiencia de desarrollo
4. **FASE 6 (Lifecycle)** complementa funcionalidad completa

### Mejores Prácticas de Migración

1. **Para cada tool nuevo**:
   - ✅ Revisar abap-adt-py: ¿Existe? → Reusar lógica
   - ✅ Revisar TypeScript: Obtener ADT URI y schema MCP
   - ✅ Implementar con RfcAdapter (NO HTTP)
   - ✅ Test unitario + integración

2. **Patrón de código**:
   ```python
   # SIEMPRE usar RfcAdapter
   response = self.adapter.request(...)  # ✅ Correcto

   # NUNCA usar HTTP directo
   response = requests.get(...)  # ❌ Incorrecto
   ```

3. **Reutilización**:
   - Parsing XML de abap-adt-py → ✅ Adaptar
   - Validaciones de abap-adt-py → ✅ Reusar
   - Conexión HTTP de abap-adt-py → ❌ NO usar

4. **Validar con tests de integración**: Cada fase debe tener test end-to-end con RFC real

5. **Documentar ADT URIs**: Mantener tabla de referencia actualizada

6. **Manejo de errores robusto**: Parse XML errors, HTTP status codes (via RfcResponse)

7. **Logging detallado**: Debug level para request/response RFC, Info para operaciones

### Features Avanzadas NO Incluidas
Las siguientes features tienen bajo ROI y alta complejidad:
- **Git Integration** (9 tools): Gestión de repos Git en ABAP
- **Debugging** (18 tools): Debugger interactivo
- **ATC** (8 tools): ABAP Test Cockpit
- **Refactoring** (6 tools): Extract method, rename, etc.
- **Tracing** (7 tools): Performance tracing
- **Service Bindings** (3 tools): OData service publishing
- **Revisions** (1 tool): Version history

**Recomendación**: Evaluar necesidad real antes de implementar estas features.

---

## Estimación Total

| Fase | Tools | Estado | Días Usados | Valor Acumulado |
|------|-------|--------|-------------|-----------------|
| FASE 1 | 9 | ✅ COMPLETADA | 1 | 20% |
| FASE 2 | 6 | ✅ COMPLETADA | 1 | 35% |
| FASE 3 | 15 | 🔲 Pendiente | 0 | 65% |
| FASE 4 | 6 | 🔲 Pendiente | 0 | 90% |
| FASE 5 | 6 | 🔲 Pendiente | 0 | 95% |
| FASE 6 | 8 | 🔲 Pendiente | 0 | 100% |
| **TOTAL** | **50 tools** | **35% completado** | **2/15 días** | **35%** |

**Recomendación**:
- Implementar **Fases 1-4** para obtener **90% del valor** en ~8-11 días
- **Fases 5-6** son opcionales pero altamente recomendadas para completar la suite

---

## Próximos Pasos

1. ✅ Aprobar este plan de migración
2. ✅ **FASE 1 COMPLETADA**: Repository & Source Retrieval (2025-01-10)
3. ✅ **FASE 2 COMPLETADA**: DDIC & Data Dictionary (2025-01-10)
4. ✅ Actualizar server.py con todos los nuevos servicios y tools
5. 🔲 Escribir tests unitarios para FASE 1 y FASE 2
6. 🔲 Escribir tests de integración end-to-end
7. 🔲 Iniciar FASE 3: Transport Management
8. 🔲 Continuar con implementación secuencial por fases

---

**Documento generado**: 2025-01-10
**Última actualización**: 2025-01-10 (FASE 1 y FASE 2 completadas)
**Próxima revisión**: Después de completar tests de FASE 1 y FASE 2

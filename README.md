# 🎉 MCP ABAP ADT RFC Server

**Servidor MCP completo para operaciones ABAP via RFC - 63+ Tools Implementadas**

Servidor MCP (Model Context Protocol) que permite interactuar con sistemas SAP ABAP a través de RFC SDK. Este servidor expone **60 herramientas** organizadas en **10 categorías funcionales**, permitiendo a Claude Code y otras herramientas LLM realizar operaciones completas de desarrollo ABAP: desde búsqueda y lectura hasta modificación, activación, testing, análisis de referencias, y gestión de CDS Views, objetos RAP y enhancements.

## ✨ Funcionalidades Principales

### 🔍 Repository & Source (10 tools)
**Búsqueda y lectura de objetos ABAP**
- `search_objects` - Búsqueda con wildcards (ej: `ZCL_*`, `*UTIL*`)
- `get_class_source` - Código fuente completo de clases
- `get_class_structure` - Metadata, métodos y atributos
- `get_program_source` - Código de programas/reports
- `get_include_source` - Código de includes
- `get_object_source` - Código por URI genérico
- `get_object_types` - Tipos de objetos disponibles
- `adt_discovery` - Capacidades ADT del sistema
- `get_node_contents` - Navegación de árbol de repositorio
- `get_package_objects` - **NUEVO** Obtener objetos de un paquete consultando TADIR con filtros avanzados (8 campos: PGMID, OBJECT, OBJ_NAME, SRCSYSTEM, AUTHOR, DEVCLASS, CREATED_ON, CHECK_DATE). Filtros: object_types, author, created_from/to

### 📚 DDIC & Data Dictionary (6 tools)
**Acceso a diccionario de datos y queries**
- `get_ddic_element` - Definiciones de tablas, estructuras, elementos
- `ddic_repository_access` - Acceso directo a repositorio DDIC
- `get_annotation_definitions` - Anotaciones CDS
- `package_search_help` - Búsqueda y autocompletado de paquetes
- `get_table_contents` - Preview de contenido de tablas
- `run_query` - Ejecución de queries SQL personalizadas

### 🚚 Transport Management (14 tools)
**Gestión completa de órdenes de transporte**
- `get_transport_request` - Data completa de transporte (tasks + objects)
- `get_transport_tasks` - Tareas del transporte
- `get_transport_objects` - Objetos del transporte (con filtro por tarea)
- `transport_info` - Info de transporte para un objeto
- `create_transport` - Crear nuevo transporte
- `list_user_transports` - Transportes del usuario
- `add_object_to_transport` - Asignar objeto a transporte
- `release_transport` - Liberar transporte
- `get_transport_config` - Configuración del sistema
- `delete_transport` - Eliminar transporte
- `set_transport_owner` - Cambiar propietario
- `add_transport_user` - Añadir colaborador
- `get_system_users` - Usuarios del sistema
- `get_transport_reference` - Referencias de transporte

### ✏️ Object Modification (6 tools)
**Modificación completa de objetos ABAP**
- `lock` - Bloquear objeto para edición (retorna LOCK_HANDLE)
- `unlock` - Desbloquear objeto
- `set_object_source` - Modificar código fuente
- `activate` - Activar objeto individual
- `activate_objects` - Activación en batch
- `get_inactive_objects` - Listar objetos inactivos

### 🎨 Code Quality (4 tools)
**Validación y formato de código**
- `syntax_check` - Verificación de sintaxis ABAP
- `prettyprint` - Formateo automático según estándares SAP
- `get_prettyprint_settings` - Obtener configuración de formato
- `set_prettyprint_settings` - Configurar estilo de formato

### 🚀 High-Level Modification Workflows (4 tools - NEW!)
**Workflows completos LOCK → SYNTAX → MODIFY → UNLOCK → ACTIVATE**
- `modify_function_module` - Workflow completo para modificar módulo de función
- `modify_class` - Workflow completo para modificar clase ABAP
- `modify_program` - Workflow completo para modificar programa/reporte
- `modify_include` - Workflow completo para modificar include de programa

**Características:**
- ✅ Sintaxis validación automática (previene código inválido)
- ✅ Manejo de locks con try-finally (garantiza liberación)
- ✅ Activación automática opcional
- ✅ Reporte detallado de estado por paso

### 🏗️ Lifecycle & Testing (4 tools)
**Creación, eliminación y testing**
- `create_class` - Crear nueva clase ABAP
- `delete_object` - Eliminar objeto (⚠️ requiere lock)
- `validate_object_name` - Validar nombre según convenciones SAP
- `run_unit_tests` - Ejecutar unit tests con cobertura opcional

### 🔎 Where-Used Analysis (2 tools)
**Análisis de referencias y usos**
- `get_where_used` - Encontrar dónde se usa un objeto en el sistema
- `get_where_used_dependencies` - Obtener grafo detallado de dependencias

### 📊 CDS Views & Core Data Services (4 tools)
**Gestión de CDS Views y DDL**
- `get_cds_view_metadata` - Metadata de CDS views incluyendo SQL view name
- `get_cds_view_source` - Código DDL de CDS views
- `search_cds_views_by_sqlview` - Búsqueda por nombre de SQL view
- `get_cds_view_properties` - Propiedades de package, owner y estado API

### 🏛️ RAP Objects & OData Services (8 tools)
**Gestión completa de objetos RAP y servicios OData**
- `get_service_binding` - Metadata de service binding (SRVB)
- `get_service_definition_metadata` - Metadata de service definition (SRVD)
- `get_service_definition_source` - Código fuente de service definition
- `get_odata_service_info` - Información y endpoints de servicios OData
- `get_metadata_extension` - Metadata extension (DDLX) para anotaciones UI
- `get_ddlx_parser_info` - Definiciones de anotaciones disponibles
- `get_behavior_definition` - Código fuente de behavior definition (BDEF)
- `explore_rap_object` - Exploración inteligente de relaciones entre objetos RAP

### 🔧 Enhancement Operations (3 tools)
**Gestión de ampliaciones/enhancements**
- `search_enhancements` - Búsqueda de enhancements en package (tipos ENHO)
- `get_enhancement_metadata` - Metadata incluyendo hook implementations
- `get_enhancement_source` - Código fuente de bloques ENHANCEMENT

## 🚀 Token Optimization Features

El servidor implementa un sistema completo de optimización de tokens para manejar respuestas grandes de SAP sin exceder límites de caracteres:

### 📦 Paginación Inteligente
- **Offset-based pagination** con metadata completa (has_more, next_offset, current_page)
- **Default page size**: 50 objetos (configurable hasta 1000)
- **Workflow**: Recuperación incremental guiada por metadata de paginación

**Ejemplo**:
```python
# Primera página
result = get_package_objects("ZFI", max_rows=50)
# → pagination: {has_more: true, next_offset: 50}

# Siguiente página
if result["pagination"]["has_more"]:
    next_result = get_package_objects("ZFI", offset=50)
```

### 🎯 Formatos de Respuesta Flexibles
- **detailed** (default): Metadata completa (100% datos)
- **summary**: Nombres + conteos (~90% reducción)
- **types_only**: Solo conteos (~99% reducción)

**Estrategia**: Start broad → Drill down
```python
# 1. Vista general rápida
get_package_objects("ZFI", response_format="types_only")
# → {CLAS: 7, PROG: 121}

# 2. Nombres específicos
get_package_objects("ZFI", response_format="summary", object_types=["CLAS"])
# → {CLAS: {count: 7, names: ["ZCLS1", "ZCLS2", ...]}}

# 3. Metadata completa con paginación
get_package_objects("ZFI", response_format="detailed", object_types=["CLAS"], max_rows=20)
```

### ✂️ CHARACTER_LIMIT Automático
- **Límite global**: 25,000 caracteres por respuesta
- **Truncamiento inteligente** con mensajes educativos
- **Sugerencias contextuales**: Paginación, filtros, formatos compactos

### 🔀 Fragmentación de Objetos Grandes
Para clases grandes que exceden CHARACTER_LIMIT:
```python
# 1. Recuperar include principal
result = get_class_source("ZCLCXR1002_UTIL")

# 2. Si está truncado, listar includes disponibles
includes = get_class_includes("ZCLCXR1002_UTIL")

# 3. Recuperar includes específicos
main = get_class_source("ZCLCXR1002_UTIL", include_type="main")
impl = get_class_source("ZCLCXR1002_UTIL", include_type="implementation")
tests = get_class_source("ZCLCXR1002_UTIL", include_type="testclasses")
```

**Include types disponibles**:
- `main`: Definición de clase (PUBLIC section)
- `implementation`: Implementaciones de métodos
- `testclasses`: ABAP Unit tests
- `macros`: Definiciones de macros

### 📊 Métricas de Optimización

| Escenario | Antes | Después | Reducción |
|-----------|-------|---------|-----------|
| Package ZFI (241 objetos) | 120K chars | 25K chars (detailed) | 79% |
| Package ZFI (summary) | 120K chars | 12K chars | 90% |
| Package ZFI (types_only) | 120K chars | 1.2K chars | 99% |
| Clase grande (50K) | 50K chars | 4 × 12.5K (fragmentado) | Completo |

**Documentación completa**: Ver `docs/architecture/token-optimization-strategy.md`

## 📋 Prerequisites

1. **SAP NetWeaver RFC SDK** installed
   - Download from SAP Support Portal
   - Install to `/usr/local/nwrfcsdk` (or configure path)

2. **Python 3.11+** with `uv` package manager

3. **SAP System Access**
   - Application server host and port
   - Valid SAP credentials
   - ADT (ABAP Development Tools) enabled

## 🔧 Installation

### Automated Setup (Recommended)

Use the unified setup script that handles everything automatically:

```bash
cd /path/to/brootpersonalagent

# Run automated setup
./setup.sh
```

**What the script does**:
- ✅ Detects OS (macOS/Linux/Windows)
- ✅ Configures SAP RFC SDK environment variables
- ✅ Creates virtual environment
- ✅ Installs all dependencies (uv or pip)
- ✅ Compiles PyRFC automatically
- ✅ Validates `.env` configuration
- ✅ Configures Claude Desktop integration
- ✅ Verifies installation

### Manual Dependency Installation

If you prefer manual installation or need to customize dependencies:

**Production Dependencies:**
```bash
# Install from requirements.txt
pip install -r requirements.txt

# OR using uv (faster)
uv pip install -r requirements.txt
```

**Development Dependencies (Optional):**
```bash
# Install development tools (pytest, ruff, pyright)
pip install -r requirements-dev.txt

# OR using uv
uv pip install -r requirements-dev.txt
```

**PyRFC (SAP RFC SDK Binding):**
```bash
# IMPORTANT: PyRFC must be compiled manually
# Requires SAP NetWeaver RFC SDK installed first

# 1. Set environment variables
export SAPNWRFC_HOME=/Users/local/nwrfcsdk  # or your SDK path
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH  # macOS
# OR
export LD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$LD_LIBRARY_PATH  # Linux

# 2. Compile and install PyRFC
cd PyRFC
python3 -m pip install .
cd ..
```

**Core Dependencies Included:**
- `pydantic>=2.11.0` - Data validation
- `python-dotenv>=1.1.0` - Environment configuration
- `mcp>=1.15.0` - Model Context Protocol framework
- `httpx>=0.28.0` - HTTP client (MCP requirement)
- `pytest>=8.4.0` - Testing (dev only)
- `pyrfc==3.4` - SAP RFC SDK bindings (manual install)

**Note:** See `requirements.txt` for complete dependency list with version constraints.

### Configure SAP Credentials

Create or edit `.env` file with your SAP credentials:

```bash
# Copy example and edit
cp .env.example .env
code .env
```

```env
# Required Settings
SAP_ASHOST=your.sap.server.com
SAP_SYSNR=00
SAP_CLIENT=100
SAP_USER=your_username
SAP_PASSWD=your_password

# Optional Settings
SAP_LANG=EN
SAP_ROUTER=/H/router.host/S/sapdp99

# Test Configuration (optional)
TEST_CLASS_NAME=CL_ABAP_CHAR_UTILITIES
TEST_SEARCH_QUERY=CL_ABAP*
TEST_PROGRAM_NAME=SAPBC_START_PROGRAMS
```

### Using with Claude Desktop

If you used `./setup.sh`, Claude Desktop is already configured automatically.

To verify the configuration:
```bash
# macOS
cat ~/Library/Application\ Support/Claude/claude_desktop_config.json

# Linux
cat ~/.config/Claude/claude_desktop_config.json

# Windows
type %APPDATA%\Claude\claude_desktop_config.json
```

**Manual Configuration** (if needed):

See [INSTALLATION.md](INSTALLATION.md) for detailed manual setup instructions.

## 🚀 Usage

### With Claude Code

1. **Restart Claude Code** to load the MCP server
2. The server will appear as **"ABAP-ADT-RFC-Server"** with 60 available tools
3. Ask Claude Code natural language questions:

```
🔍 Búsqueda:
- "Search for all custom classes starting with ZCL_MM"
- "Find all programs matching ZREP*"

📖 Lectura:
- "Show me the source code of class CL_ABAP_CHAR_UTILITIES"
- "What methods does class CL_HTTP_CLIENT have?"
- "Get table ZTCXR1000_1 contents"

🚚 Transportes:
- "List my transport requests"
- "Show me objects in transport S4DK932806"

🔎 Where-Used:
- "Where is class ZCLMMI1229_SINCRONIZA_INV_MAWM being used?"
- "Show me all references to this class in the codebase"

📊 CDS Views:
- "Get metadata for CDS view ZIFII1008_2"
- "Show me the DDL source of CDS view ZIFII1008_2"

🏛️ RAP Objects:
- "Get the service binding for ZUI_RAP_O2_ZTCXR1003_1"
- "Show me the behavior definition for ZC_RAP_ZTCXR1003_1"
- "Explore all components of RAP object ZSERVICE_DEF"

🔧 Enhancements:
- "Find all enhancements in package ZI1008"
- "Show me the source code of enhancement ZFII1008_1"
- "What hook implementations exist in enhancement ZFII1008_1?"

📦 Package Objects (con filtros avanzados):
- "Get all objects from package ZMMI1229_0"
- "Show me object inventory for package ZFII1008_0"
- "What objects are in package $TMP grouped by type?"
- "Get only classes and programs from package ZMMI1229_0"
- "Show me objects created by author DEVELOPER in package ZFII1008_0"
- "Get objects from $TMP created between 2025-01-01 and 2025-12-31"
- "Show me CLAS objects by author SAP from package $TMP"

✏️ Modificación completa:
- "Read class ZCL_TEST, create transport, lock it, add a BREAK-POINT
   to method PROCESS, check syntax, format code, activate, and unlock"
```

### Direct Testing (Phase-by-Phase)

```bash
# Run all tests
.venv/bin/python -m pytest app/tests/ -v

# FASE 1: Repository & Source
.venv/bin/python -m pytest app/tests/test_debug_search.py -v
# Tests: search_objects, get_class_source, get_class_structure

# FASE 2: DDIC & Data Dictionary
.venv/bin/python -m pytest app/tests/test_debug_query.py -v
# Tests: get_table_contents, run_query, get_ddic_element

# FASE 3: Transport Management
.venv/bin/python -m pytest app/tests/test_debug_transport_s4dk932806.py -v
# Tests: get_transport_request, get_transport_tasks, get_transport_objects

# FASE 4: Object Modification
.venv/bin/python -m pytest app/tests/test_fase4_object_modification.py -v
# Tests: lock, set_object_source, activate, unlock

# FASE 5: Code Quality
.venv/bin/python -m pytest app/tests/test_fase5_code_quality.py -v
# Tests: syntax_check, prettyprint, get_prettyprint_settings

# FASE 6: Lifecycle & Testing
.venv/bin/python -m pytest app/tests/test_fase6_lifecycle.py -v
# Tests: create_class, delete_object, run_unit_tests

# FASE 7: Where-Used Analysis
.venv/bin/python -m pytest app/tests/test_fase7_whereused.py -v
# Tests: get_where_used, get_where_used_dependencies

# New Categories: CDS Views, RAP Objects, Enhancements
.venv/bin/python -m pytest app/tests/test_cds_category.py -v
# Tests: get_cds_view_metadata, get_cds_view_source (2/4 passing)

.venv/bin/python -m pytest app/tests/test_enhancement_category.py -v
# Tests: search_enhancements, get_enhancement_metadata, get_enhancement_source (3/3 passing ✅)

# RAP testing pending
# .venv/bin/python -m pytest app/tests/test_rap_category.py -v
```

## 🔧 Setup Script

The project includes a unified installation script:

### `setup.sh` - Complete Automated Setup
Handles everything in a single command:
- ✅ Detects OS (macOS/Linux/Windows)
- ✅ Configures SAP RFC SDK environment variables
- ✅ Creates virtual environment
- ✅ Installs dependencies (uv or pip)
- ✅ Compiles PyRFC automatically
- ✅ Validates `.env` configuration
- ✅ Configures Claude Desktop integration
- ✅ Verifies installation

```bash
./setup.sh
```

After running the setup, you can run tests directly:
```bash
.venv/bin/python -m pytest app/tests/ -v
```

## 📁 Project Structure

```
app/
├── core/
│   ├── config.py                  # SAP configuration (RFC credentials)
│   ├── rfc_connection.py          # RFC connection pool
│   └── rfc_adapter.py             # HTTP→RFC adapter (key component)
│
├── services/                       # 17 Services Implemented
│   ├── class_service.py           # ✅ FASE 1: Class operations
│   ├── search_service.py          # ✅ FASE 1: Search objects
│   ├── program_service.py         # ✅ FASE 1: Program/Include ops
│   ├── discovery_service.py       # ✅ FASE 1: ADT capabilities
│   ├── navigation_service.py      # ✅ FASE 1: Repository tree
│   ├── ddic_service.py            # ✅ FASE 2: DDIC elements
│   ├── query_service.py           # ✅ FASE 2: SQL queries
│   ├── transport_service.py       # ✅ FASE 3: Transport mgmt (14 tools)
│   ├── object_service.py          # ✅ FASE 4: Lock/unlock/modify
│   ├── activation_service.py      # ✅ FASE 4: Activation
│   ├── code_quality_service.py    # ✅ FASE 5: Syntax/format
│   ├── creation_service.py        # ✅ FASE 6: Create/delete
│   ├── unittest_service.py        # ✅ FASE 6: Run unit tests
│   ├── whereused_service.py       # ✅ FASE 7: Where-used analysis
│   ├── cds_service.py             # ✅ NEW: CDS Views & DDL
│   ├── rap_service.py             # ✅ NEW: RAP Objects & OData
│   └── enhancement_service.py     # ✅ NEW: Enhancements/Ampliaciones
│
├── mcp/
│   ├── server.py                  # Main MCP server
│   └── tools/                     # 60 MCP Tools
│       ├── class_tools.py         # 3 tools
│       ├── search_tools.py        # 1 tool
│       ├── program_tools.py       # 2 tools
│       ├── discovery_tools.py     # 2 tools
│       ├── navigation_tools.py    # 3 tools (get_node_contents, find_object_path, get_package_objects)
│       ├── ddic_tools.py          # 4 tools
│       ├── query_tools.py         # 2 tools
│       ├── transport_tools.py     # 14 tools
│       ├── object_tools.py        # 3 tools
│       ├── activation_tools.py    # 3 tools
│       ├── code_quality_tools.py  # 4 tools
│       ├── creation_tools.py      # 3 tools
│       ├── unittest_tools.py      # 1 tool
│       ├── whereused_tools.py     # 2 tools
│       ├── cds_tools.py           # 4 tools ✨ NEW
│       ├── rap_tools.py           # 8 tools ✨ NEW
│       └── enhancement_tools.py   # 3 tools ✨ NEW
│
└── tests/                          # Phase Validation Tests
    ├── test_debug_search.py       # ✅ FASE 1 validation
    ├── test_debug_query.py        # ✅ FASE 2 validation
    ├── test_debug_transport_*.py  # ✅ FASE 3 validation
    ├── test_fase4_*.py            # ✅ FASE 4 validation
    ├── test_fase5_*.py            # ✅ FASE 5 validation
    ├── test_fase6_*.py            # ✅ FASE 6 validation
    ├── test_fase7_*.py            # ✅ FASE 7 validation
    ├── test_cds_category.py       # ✅ NEW: CDS tests (2/4 passing)
    ├── test_enhancement_category.py # ✅ NEW: Enhancement tests (3/3 passing ✅)
    └── test_rap_category.py       # ⏳ NEW: RAP tests (pending)
```

## 🏗️ Key Architecture: RfcAdapter

The **RfcAdapter** is the core component that enables HTTP-style requests to be converted to RFC calls:

```python
# HTTP-style API
response = adapter.request(
    uri="/sap/bc/adt/oo/classes/ZTEST/source/main",
    method="GET",
    params={"version": "active"}
)

# ⬇️ Internally calls SAP RFC function:
# CALL FUNCTION 'SADT_REST_RFC_ENDPOINT'
#   EXPORTING
#     URI          = '/sap/bc/adt/oo/classes/ZTEST/source/main'
#     METHOD       = 'GET'
#     QUERY_STRING = 'version=active'
```

**Benefits**:
- ✅ Reuses logic from HTTP-based ADT projects
- ✅ Compatible with standard ADT endpoints
- ✅ Connection pooling for performance
- ✅ Consistent error handling

## 🧪 Testing Quick Commands

### Test Individual Tools

```bash
# Test search functionality
uv run python app/tests/test_debug_search.py

# Test query and DDIC access
uv run python app/tests/test_debug_query.py

# Test transport operations with real transport
uv run python app/tests/test_debug_transport_s4dk932806.py

# Test complete modification workflow
uv run python app/tests/test_fase4_object_modification.py

# Test code quality tools
uv run python app/tests/test_fase5_code_quality.py

# Test class creation and lifecycle
uv run python app/tests/test_fase6_lifecycle.py
```

### Run All Tests with Pytest

```bash
# All tests
uv run pytest app/tests/ -v

# With coverage
uv run pytest app/tests/ --cov=app --cov-report=html

# Specific test file
uv run pytest app/tests/test_integration.py -v -s
```

## 🛠️ Development

### Code Quality Tools

```bash
# Type checking
uv run pyright app/

# Linting
uv run ruff check app/

# Formatting
uv run ruff format app/
```

## 🔄 Complete Workflow Example

```mermaid
graph LR
    A[🔍 Search] --> B[📖 Read Source]
    B --> C[🚚 Create Transport]
    C --> D[🔒 Lock Object]
    D --> E[✏️ Modify Code]
    E --> F[✔️ Syntax Check]
    F --> G[🎨 Pretty Print]
    G --> H[✨ Activate]
    H --> I[🧪 Unit Test]
    I --> J[🔓 Unlock]
```

**All 10 steps available through the MCP tools:**
1. `search_objects` - Find object
2. `get_class_source` - Read code
3. `create_transport` - Create transport
4. `lock` - Lock for editing
5. `set_object_source` - Modify code
6. `syntax_check` - Validate syntax
7. `prettyprint` - Format code
8. `activate` - Activate object
9. `run_unit_tests` - Run tests
10. `unlock` - Release lock

## ⚠️ Troubleshooting

### RFC Library Not Found

```bash
# Ensure environment variables are set
export SAPNWRFC_HOME=/usr/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
```

### Connection Errors

- Verify SAP credentials in `.env`
- Check network connectivity to SAP server
- Confirm SAP router string if using
- Ensure ADT is enabled on target system

### MCP Server Not Loading

- Restart Claude Code completely
- Check `.mcp.json` configuration
- Verify `cwd` path is correct
- Check logs: Look for Python errors in Claude Code console

## 🔒 Security Notes

- **Never commit `.env`** file with credentials
- Use environment variables for sensitive data
- Consider using SAP Secure Network Communication (SNC)
- Implement proper access controls in SAP

## 📊 Project Metrics

- ✅ **60 tools** implemented (10 categories)
- ✅ **17 services** created
- ✅ **100%** implementation coverage
- ✅ **68%** test coverage (41/60 tools fully tested)
- 🆕 **16 new tools** added: CDS Views (4), RAP Objects (8), Enhancements (3), Package Objects (1)
- ⏱️ **Rapid development**: 3 new categories implemented
- 🔍 **Where-Used**: Successfully finds and displays code references with context
- 🎯 **Enhancement testing**: 100% pass rate (3/3 tools validated)
- 📦 **Package exploration**: TADIR-based package object inventory with 8 essential fields and advanced filtering (object_types, author, date range)

## 📄 Additional Documentation

- [📋 Complete Implementation Plan](docs/requirements/pr_migration_plan_mcp_tools.md)
- [🆕 New MCP Tools - CDS, RAP, Enhancements](docs/requirements/pr_new_mcp_tools_adt.md)
- [🎉 Project Completion Report](PROYECTO_COMPLETADO.md)

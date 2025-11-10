# Python MCP Server - Legacy/Reference

**Estado**: Archivado pero funcional
**Fecha de archivo**: 2025-11-08
**Proyecto principal**: Java MCP Server (../src/)

---

## ⚠️ Importante

Este proyecto Python **permanece funcional** pero ya no es el proyecto principal. Se mantiene como:

1. **Referencia**: Para consultar implementaciones durante la migración a Java
2. **Fallback**: Sistema de respaldo si Java tiene problemas
3. **Testing**: Validación de comportamiento esperado

**Nuevas funcionalidades**: Solo se implementan en Java (`../src/`)

---

## Estructura

```
python-legacy/
├── app/                          # Código fuente principal
│   ├── core/                     # Infraestructura (RFC, adapter, config)
│   ├── services/                 # 17 servicios de negocio
│   ├── mcp/                      # Servidor MCP y tools
│   │   ├── server.py             # Main MCP server
│   │   └── tools/                # 59 herramientas MCP
│   └── tests/                    # 76+ archivos de tests
├── PyRFC/                        # SAP RFC SDK bindings para Python
├── .venv/                        # Virtual environment
├── pyproject.toml                # Configuración del proyecto
├── uv.lock                       # Dependencias bloqueadas
└── PYTHON_LEGACY.md              # Este archivo
```

---

## Herramientas MCP Disponibles (59 total)

### 1. Repository & Source (9 tools)
- `get_class_source` - Código fuente de clase ABAP
- `get_class_structure` - Metadata de clase
- `get_object_source` - Código de cualquier objeto por URI
- `get_class_includes` - Includes de una clase
- `get_class_components` - Componentes detallados
- `get_object_structure` - Estructura de objeto genérico
- `search_objects` - Búsqueda con wildcards
- `get_program_source` - Código de programa/report
- `get_include_source` - Código de include

### 2. Data Dictionary (4 tools)
- `get_ddic_element` - Definición DDIC (tablas, dominios, etc.)
- `ddic_repository_access` - Acceso directo a repositorio DDIC
- `get_annotation_definitions` - Definiciones de anotaciones CDS
- `package_search_help` - Búsqueda de paquetes

### 3. Query & Preview (2 tools)
- `get_table_contents` - Preview de tabla con filtros
- `run_query` - Consultas SQL custom

### 4. Transport Management (14 tools)
- `list_user_transports` - Transportes de usuario
- `get_transport_objects` - Objetos en transporte
- `get_transport_tasks` - Tareas de transporte
- `transport_info` - Historia de transporte
- `create_transport` - Crear nuevo transporte
- `add_object_to_transport` - Agregar objeto a TR
- `release_transport` - Liberar transporte
- `get_transport_config` - Configuración del sistema
- `delete_transport` - Eliminar transporte
- `set_transport_owner` - Cambiar propietario
- `add_transport_user` - Agregar colaborador
- `get_system_users` - Lista de usuarios
- `get_transport_reference` - Referencias de transporte

### 5. Object Modification (3 tools)
- `lock` - Bloquear objeto para edición
- `unlock` - Desbloquear objeto
- `set_object_source` - Modificar código fuente

### 6. Activation (3 tools)
- `activate` - Activar objeto individual
- `activate_objects` - Activación batch
- `get_inactive_objects` - Lista de objetos inactivos

### 7. Code Quality (4 tools)
- `syntax_check` - Validar sintaxis ABAP
- `prettyprint` - Formatear código
- `get_prettyprint_settings` - Obtener configuración formatter
- `set_prettyprint_settings` - Configurar formatter

### 8. Lifecycle (7 tools)
- `create_function_group` - Crear grupo de funciones
- `create_function_module` - Crear módulo de función
- `create_class` - Crear clase ABAP
- `create_interface` - Crear interfaz ABAP
- `delete_object` - Eliminar objeto
- `validate_object_name` - Validar nombre
- `run_unit_tests` - Ejecutar unit tests

### 9. Where-Used Analysis (2 tools)
- `get_usage_references` - Referencias de uso
- `get_usage_snippets` - Snippets de código donde se usa

### 10. CDS Views (4 tools)
- `get_cds_view_metadata` - Metadata de vista CDS
- `get_cds_view_source` - DDL source de CDS
- `search_cds_views_by_sqlview` - Búsqueda por SQL view
- `get_cds_view_properties` - Propiedades de CDS

### 11. RAP Objects (8 tools)
- `get_service_binding` - Service Binding metadata
- `get_service_definition_metadata` - Service Definition metadata
- `get_service_definition_source` - Service Definition source
- `get_odata_service_info` - Info de servicio OData
- `get_metadata_extension` - DDLX metadata extension
- `get_ddlx_parser_info` - Parser info DDLX
- `get_behavior_definition` - BDEF source
- `explore_rap_object` - Exploración completa RAP

### 12. Enhancements (3 tools)
- `search_enhancements` - Búsqueda de enhancements
- `get_enhancement_metadata` - Metadata de enhancement
- `get_enhancement_source` - Código de enhancement

### 13. High-Level Workflows (4 tools)
- `modify_function_module` - Workflow completo: LOCK → MODIFY → UNLOCK → ACTIVATE
- `modify_class` - Workflow completo para clases
- `modify_program` - Workflow completo para programas
- `modify_include` - Workflow completo para includes

---

## Instalación y Configuración

### Requisitos Previos

- Python 3.11+
- SAP NetWeaver RFC SDK instalado en `/Users/local/nwrfcsdk`
- uv package manager (o pip)

### Setup

```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent/python-legacy

# Crear virtual environment
python3 -m venv .venv

# Activar virtual environment
source .venv/bin/activate

# Instalar PyRFC (SAP RFC bindings)
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
cd PyRFC
pip install .
cd ..

# Instalar dependencias del proyecto
uv sync

# Verificar instalación
python -c "from pyrfc import Connection; print('PyRFC OK')"
python -c "from app.core.config import SAPConfig; print('App imports OK')"
```

---

## Uso

### Ejecutar Tests

```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent/python-legacy

# Activar virtual environment si no está activo
source .venv/bin/activate

# Ejecutar todos los tests
.venv/bin/python -m pytest app/tests/ -v

# Ejecutar categoría específica
.venv/bin/python -m pytest app/tests/test_cds_category.py -v
.venv/bin/python -m pytest app/tests/test_transport_category.py -v

# Con coverage
.venv/bin/python -m pytest app/tests/ --cov=app --cov-report=html
```

### Ejecutar Servidor MCP

El servidor Python MCP se ejecuta a través de Claude Desktop configurado en `../.mcp.json`:

```json
{
  "mcpServers": {
    "CRY": {
      "command": "/ruta/absoluta/python-legacy/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/ruta/absoluta/python-legacy",
      "env": {
        "SAP_ASHOST": "172.27.154.8",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "usuario",
        "SAP_PASSWD": "contraseña",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router/S/port",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "PYTHONPATH": "/ruta/absoluta/python-legacy"
      }
    },
    "GDC": {
      "command": "/ruta/absoluta/python-legacy/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/ruta/absoluta/python-legacy",
      "env": {
        "SAP_ASHOST": "172.28.0.56",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "200",
        "SAP_USER": "usuario",
        "SAP_PASSWD": "contraseña",
        "SAP_LANG": "ES",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "PYTHONPATH": "/ruta/absoluta/python-legacy"
      }
    }
  }
}
```

### Ejecutar Herramientas Manualmente

```python
# python-legacy/test_manual.py
from dotenv import load_dotenv
import os
from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.class_service import ClassService

load_dotenv()

# Configurar conexión SAP
sap_config = SAPConfig(
    ashost=os.getenv("SAP_ASHOST"),
    sysnr=os.getenv("SAP_SYSNR"),
    client=os.getenv("SAP_CLIENT"),
    user=os.getenv("SAP_USER"),
    passwd=os.getenv("SAP_PASSWD"),
    lang=os.getenv("SAP_LANG", "EN"),
    saprouter=os.getenv("SAP_ROUTER"),
)

# Crear pool de conexiones
pool = RfcConnectionPool(sap_config, pool_size=1)

# Usar servicio
service = ClassService(pool)
result = service.get_class_source("CL_ABAP_CHAR_UTILITIES")
print(result["source"][:500])  # Primeros 500 caracteres
```

---

## Arquitectura

### Flujo de Ejecución

```
Claude Code (LLM)
    ↓
MCP Tool Call (JSON-RPC)
    ↓
app/mcp/tools/*.py (MCP Tool Definition)
    ↓
app/services/*.py (Business Logic)
    ↓
app/core/rfc_adapter.py (HTTP-to-RFC Bridge)
    ↓
PyRFC (Python RFC SDK bindings)
    ↓
SADT_REST_RFC_ENDPOINT (FM en SAP)
    ↓
SAP ADT REST API
    ↓
SAP ABAP System
```

### Componentes Clave

- **`app/core/rfc_connection.py`**: Connection pooling thread-safe
- **`app/core/rfc_adapter.py`**: Convierte HTTP requests → RFC calls
- **`app/services/`**: 17 servicios de negocio (ClassService, TransportService, etc.)
- **`app/mcp/server.py`**: Servidor MCP principal
- **`app/mcp/tools/`**: 59 herramientas MCP registradas

---

## Troubleshooting

### Error: "ModuleNotFoundError: No module named 'pyrfc'"

**Solución**:
```bash
cd python-legacy/PyRFC
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
pip install .
```

### Error: "Connection to SAP system failed"

**Solución**:
1. Verificar que `.env` tiene los parámetros correctos
2. Verificar conectividad de red: `ping <SAP_ASHOST>`
3. Verificar SAP Router si se usa: `SAP_ROUTER=/H/host/S/port`
4. Verificar logs en `../logs/python/`

### Error: "SADT_REST_RFC_ENDPOINT not found"

**Solución**:
- ADT no está instalado en el sistema SAP
- Usuario no tiene autorización ADT
- Contactar equipo SAP Basis

### Tests fallan después de reorganización

**Solución**:
```bash
# Verificar que PYTHONPATH apunta a python-legacy/
export PYTHONPATH=/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent/python-legacy

# Recrear virtual environment
rm -rf .venv
python3 -m venv .venv
source .venv/bin/activate
cd PyRFC && pip install . && cd ..
uv sync
```

---

## Migración a Java

Este proyecto Python está siendo migrado progresivamente a Java. Ver el plan completo en:

**Plan de Migración**: `../docs/requirements/mcp/migration_plan.md`

**Estado de migración**:
```
[████░░░░░░░░░░░░░░░░] 1/59 tools (1.7%) - Fase 0 en progreso
```

**Herramientas ya migradas a Java**:
- ✅ `get_class_source`

**Próximas migraciones** (Fase 1):
- Repository & Source (8 tools restantes)
- Data Dictionary (4 tools)
- Transport Management (3 tools core)

---

## Mantenimiento

### Actualizaciones

**Política**: Este proyecto ya no recibe nuevas funcionalidades.

**Bug fixes**: Solo se corrigen bugs críticos que bloqueen producción.

**Dependencias**: Se actualizan solo si hay vulnerabilidades de seguridad.

### Soporte

Para problemas con este proyecto Python legacy:
1. Consultar logs en `../logs/python/`
2. Ejecutar tests: `.venv/bin/python -m pytest app/tests/ -v`
3. Comparar con implementación Java equivalente
4. Crear issue en proyecto principal

---

## Referencias

- **Proyecto Java principal**: `../src/` y `../README.md`
- **Plan de migración**: `../docs/requirements/mcp/migration_plan.md`
- **Documentación original**: `../README_PYTHON_ORIGINAL.md` (si existe)
- **PyRFC Documentation**: https://github.com/SAP/PyRFC
- **MCP Protocol**: https://modelcontextprotocol.io

---

**Última actualización**: 2025-11-08
**Versión**: 1.0.0 (archivado)
**Contacto**: Crystal Development Team

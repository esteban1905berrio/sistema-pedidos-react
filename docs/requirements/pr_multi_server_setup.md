# PR: Configuración Multi-Servidor SAP para MCP

## Descripción del Requerimiento

Configurar acceso simultáneo a múltiples sistemas SAP (DEV, QA, PRD) desde Claude Code usando el MCP Server ABAP-ADT-RFC sin modificaciones en el código del servidor.

**Caso de Uso Principal**: Workflows cross-system como:
- Comparar objetos entre sistemas (DEV vs QA)
- Copiar código fuente de un sistema a otro
- Buscar objetos en múltiples entornos simultáneamente
- Validar consistencia de configuraciones entre sistemas

## Solución Técnica

### Arquitectura Seleccionada: Múltiples Instancias MCP

Se utiliza el patrón de **múltiples instancias del MCP Server**, donde cada instancia se conecta a un sistema SAP específico. Esta arquitectura:

- ✅ Cumple con principios MCP de statelessness
- ✅ No requiere modificaciones en el código del MCP Server
- ✅ Proporciona aislamiento completo entre sistemas
- ✅ Permite workflows cross-system mediante orquestación del LLM
- ✅ Configuración explícita y clara en el lado del cliente

**Cómo Funciona**:
1. El cliente (Claude Code) configura múltiples servidores MCP en `.mcp.json`
2. Cada servidor MCP es una instancia separada del mismo código
3. Cada instancia lee sus variables de entorno (SAP_ASHOST, SAP_USER, etc.)
4. El LLM puede llamar tools de cualquier servidor en la misma conversación
5. El usuario hace preguntas en lenguaje natural, el LLM decide qué servidor usar

## Configuración Paso a Paso

### Paso 1: Preparar Archivos de Variables de Entorno

Crea archivos `.env` separados para cada sistema SAP:

**Archivo: `.env.dev`**
```bash
# SAP Development System
SAP_ASHOST=sap-dev.company.com
SAP_SYSNR=00
SAP_CLIENT=100
SAP_USER=your_username
SAP_PASSWD=your_password
SAP_LANG=EN
SAP_ROUTER=/H/router/S/port  # Opcional

# Test Configuration
TEST_CLASS_NAME=CL_ABAP_CHAR_UTILITIES
TEST_SEARCH_QUERY=CL_ABAP*
TEST_PROGRAM_NAME=SAPBC_START_PROGRAMS
```

**Archivo: `.env.qa`**
```bash
# SAP Quality Assurance System
SAP_ASHOST=sap-qa.company.com
SAP_SYSNR=01
SAP_CLIENT=100
SAP_USER=your_username
SAP_PASSWD=your_password
SAP_LANG=EN
SAP_ROUTER=/H/router/S/port  # Opcional

# Test Configuration
TEST_CLASS_NAME=CL_ABAP_CHAR_UTILITIES
TEST_SEARCH_QUERY=CL_ABAP*
TEST_PROGRAM_NAME=SAPBC_START_PROGRAMS
```

**⚠️ Seguridad**: Agrega estos archivos a `.gitignore`:
```bash
echo ".env.dev" >> .gitignore
echo ".env.qa" >> .gitignore
echo ".env.prd" >> .gitignore
```

### Paso 2: Configurar Cliente MCP (`.mcp.json`)

Configura múltiples instancias del MCP Server en la configuración del cliente:

**Para Claude Desktop** (`~/Library/Application Support/Claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "SAP-DEV": {
      "command": "/absolute/path/to/brootpersonalagent/.venv/bin/python",
      "args": ["-m", "app.main"],
      "env": {
        "SAP_ASHOST": "sap-dev.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
      }
    },
    "SAP-QA": {
      "command": "/absolute/path/to/brootpersonalagent/.venv/bin/python",
      "args": ["-m", "app.main"],
      "env": {
        "SAP_ASHOST": "sap-qa.company.com",
        "SAP_SYSNR": "01",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
      }
    }
  }
}
```

**Para Claude Code CLI** (`.mcp.json` en el proyecto):
```json
{
  "mcpServers": {
    "SAP-DEV": {
      "command": ".venv/bin/python",
      "args": ["-m", "app.main"],
      "env": {
        "SAP_ASHOST": "sap-dev.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
      }
    },
    "SAP-QA": {
      "command": ".venv/bin/python",
      "args": ["-m", "app.main"],
      "env": {
        "SAP_ASHOST": "sap-qa.company.com",
        "SAP_SYSNR": "01",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
      }
    }
  }
}
```

**Notas Importantes**:
- Usa rutas absolutas para `command` en Claude Desktop
- Usa rutas relativas (`.venv/bin/python`) en Claude Code CLI
- Las variables `SAPNWRFC_HOME` y `DYLD_LIBRARY_PATH` son críticas para PyRFC
- Cada instancia es un proceso Python independiente

### Paso 3: Verificar Conexión

**Reinicia el cliente MCP** (Claude Desktop o Claude Code) para cargar la nueva configuración.

**Prueba básica** en la conversación con Claude:
```
Usuario: "Lista los servidores MCP disponibles"
Claude: Muestra SAP-DEV y SAP-QA en la lista

Usuario: "Busca la clase CL_ABAP_CHAR_UTILITIES en SAP-DEV"
Claude: Llama a SAP-DEV.search_objects(...)

Usuario: "Ahora búscala en SAP-QA"
Claude: Llama a SAP-QA.search_objects(...)
```

## Guía de Usuario: Workflows Cross-System

### 1. Comparar Código Fuente Entre Sistemas

**Prompt**:
```
"Compara la clase ZTEST_CLASS entre DEV y QA y muéstrame las diferencias"
```

**Lo que hace Claude internamente**:
1. Llama `SAP-DEV.get_class_source("ZTEST_CLASS")`
2. Llama `SAP-QA.get_class_source("ZTEST_CLASS")`
3. Compara ambos resultados
4. Muestra diferencias en formato legible

### 2. Copiar Objetos de un Sistema a Otro

**Prompt**:
```
"Copia el programa ZREPORT de DEV a QA"
```

**Lo que hace Claude internamente**:
1. `SAP-DEV.get_program_source("ZREPORT")` → obtiene código fuente
2. `SAP-QA.create_transport("Copy ZREPORT from DEV", "ZPACKAGE")` → crea transporte
3. `SAP-QA.lock("/sap/bc/adt/programs/programs/ZREPORT")` → bloquea objeto
4. `SAP-QA.set_object_source(...)` → escribe código de DEV
5. `SAP-QA.syntax_check(...)` → valida sintaxis
6. `SAP-QA.activate(...)` → activa objeto
7. `SAP-QA.unlock(...)` → desbloquea objeto

### 3. Buscar Objetos en Múltiples Sistemas

**Prompt**:
```
"Busca todos los programas que empiecen con Z_INVOICE en DEV y QA"
```

**Lo que hace Claude internamente**:
1. `SAP-DEV.search_objects("Z_INVOICE*")`
2. `SAP-QA.search_objects("Z_INVOICE*")`
3. Compara resultados y muestra tabla comparativa

### 4. Validar Configuración de CDS Views

**Prompt**:
```
"Verifica que la CDS view ZI_CUSTOMER tenga la misma estructura en DEV, QA y PRD"
```

**Lo que hace Claude internamente**:
1. `SAP-DEV.get_cds_view_source("ZI_CUSTOMER")`
2. `SAP-QA.get_cds_view_source("ZI_CUSTOMER")`
3. `SAP-PRD.get_cds_view_source("ZI_CUSTOMER")`
4. Compara DDL source code
5. Reporta diferencias o confirma consistencia

### 5. Análisis de Enhancements Entre Sistemas

**Prompt**:
```
"Muéstrame qué enhancements del package ZFI1008 existen en DEV pero no en QA"
```

**Lo que hace Claude internamente**:
1. `SAP-DEV.search_enhancements("ZFI1008")`
2. `SAP-QA.search_enhancements("ZFI1008")`
3. Compara listas y reporta diferencias

## Notas Importantes

### Seguridad

1. **Credenciales**:
   - NUNCA commitas archivos con credenciales al repositorio
   - Usa variables de entorno o archivos `.env` en `.gitignore`
   - Considera usar gestores de secretos para entornos de producción

2. **Aislamiento de Entornos**:
   - Cada instancia MCP tiene su propio proceso y pool de conexiones
   - No hay riesgo de "cross-contamination" entre sistemas
   - Los cambios en un sistema NO afectan al otro

3. **Permisos SAP**:
   - El usuario debe tener permisos adecuados en cada sistema
   - Considera usar usuarios diferentes para DEV vs PRD
   - Valida que el usuario tenga acceso a ADT REST APIs

### Limitaciones

1. **No hay parámetro `server_alias` en los tools**:
   - No puedes llamar un solo tool que compare automáticamente entre sistemas
   - El LLM debe hacer múltiples llamadas (esto es automático y transparente)

2. **Recursos del Sistema**:
   - Cada instancia MCP consume memoria (~100-200 MB por proceso Python)
   - 2-3 instancias simultáneas es razonable en máquinas modernas
   - Más de 5 instancias puede requerir optimización

3. **Transacciones Cross-System**:
   - No hay soporte nativo para transacciones distribuidas
   - Si copiar un objeto falla a mitad de camino, debes manejar rollback manualmente

### Troubleshooting

**Problema**: "No se puede conectar al servidor SAP-DEV"
- **Solución**: Verifica que `SAP_ASHOST`, `SAP_SYSNR`, `SAP_CLIENT` sean correctos
- Valida conectividad de red: `ping sap-dev.company.com`
- Revisa logs del MCP Server en stderr

**Problema**: "Error: SAPNWRFC_HOME not set"
- **Solución**: Asegúrate de que `SAPNWRFC_HOME` y `DYLD_LIBRARY_PATH` estén en `.mcp.json`
- Verifica que el SDK esté instalado en la ruta especificada

**Problema**: "Connection pool exhausted"
- **Solución**: Aumenta `pool_size` en `RfcConnectionPool` (default: 5)
- O reduce operaciones concurrentes

## Escalabilidad Futura

### Agregar Más Sistemas

Para agregar un tercer sistema (ej: PRD), simplemente agrega otra entrada en `.mcp.json`:

```json
"SAP-PRD": {
  "command": ".venv/bin/python",
  "args": ["-m", "app.main"],
  "env": {
    "SAP_ASHOST": "sap-prd.company.com",
    "SAP_SYSNR": "02",
    "SAP_CLIENT": "100",
    "SAP_USER": "your_username_prd",
    "SAP_PASSWD": "your_password_prd",
    "SAP_LANG": "EN",
    "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
    "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
  }
}
```

### Automatización

Puedes crear un script que genere `.mcp.json` desde archivos `.env`:

```bash
#!/bin/bash
# generate_mcp_config.sh

# Lee variables de .env.dev, .env.qa, .env.prd
# Genera .mcp.json con múltiples entradas
# Valida configuración
```

## Referencias

- **MCP Best Practices**: Statelessness, múltiples servidores, cliente como orquestador
- **Arquitectura del MCP Server**: `docs/architecture/current_architecture.md`
- **Configuración RFC**: `INSTALLATION.md`
- **Skill MCP-Builder**: Principios de diseño agent-centric

## Estado del Requerimiento

- **Fase**: Diseño aprobado
- **Implementación**: Solo configuración (NO requiere código)
- **Testing**: Pendiente en proyecto cliente
- **Documentación**: ✅ Completado

---

**Fecha de Creación**: 2025-10-23
**Última Actualización**: 2025-10-23
**Autor**: Bastian Root

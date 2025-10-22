# Instalación del MCP ABAP en Claude Desktop

Este documento explica cómo configurar el servidor MCP ABAP para usarlo en Claude Desktop y otros proyectos.

---

## Opción 1: Configuración Automática Completa (Recomendado)

### Paso 1: Ejecuta el script de configuración unificado

```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent
./setup.sh
```

**El script automatiza todo el proceso**:
- ✅ Detecta tu sistema operativo (macOS/Windows/Linux)
- ✅ Configura variables de entorno del SAP RFC SDK
- ✅ Crea el virtual environment si no existe
- ✅ Instala todas las dependencias (pip o uv)
- ✅ Compila PyRFC automáticamente
- ✅ Valida la configuración .env
- ✅ Configura Claude Desktop automáticamente
- ✅ Verifica que todo funciona correctamente

### Paso 2: Configura tus credenciales SAP

El script crea `.env.example` si no tienes `.env`:

```bash
# Copia el ejemplo
cp .env.example .env

# Edita con tus credenciales
code .env
```

```env
SAP_ASHOST=vhs4dapci.crystal.com.co
SAP_SYSNR=00
SAP_CLIENT=100
SAP_USER=TUUSUARIO
SAP_PASSWD=TUPASSWORD
SAP_LANG=EN
SAP_ROUTER=/H/190.145.188.150/S/sapdp99
```

### Paso 3: Reinicia Claude Desktop

1. Cierra completamente Claude Desktop
2. Vuelve a abrir Claude Desktop
3. Busca el ícono 🔧 (herramientas) en la interfaz
4. Deberías ver "ABAP-ADT-RFC-Server" con indicador verde ✅

### Paso 4: Prueba el MCP

En Claude Desktop, escribe:
```
Usa el MCP de ABAP para buscar clases que empiecen con ZCL_TEST
```

---

## Opción 2: Configuración Manual

### macOS

1. Abre el archivo de configuración:
```bash
code ~/Library/Application\ Support/Claude/claude_desktop_config.json
```

2. Agrega esta configuración:
```json
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": "/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent",
      "env": {
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
      }
    }
  }
}
```

### Windows

1. Abre el archivo de configuración:
```cmd
notepad %APPDATA%\Claude\claude_desktop_config.json
```

2. Agrega esta configuración:
```json
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": "C:\\Users\\TuUsuario\\proyecto\\brootpersonalagent\\.venv\\Scripts\\python.exe",
      "args": ["-m", "app.main"],
      "cwd": "C:\\Users\\TuUsuario\\proyecto\\brootpersonalagent",
      "env": {
        "SAPNWRFC_HOME": "C:\\nwrfcsdk",
        "PATH": "C:\\nwrfcsdk\\lib;%PATH%"
      }
    }
  }
}
```

---

## Opción 3: Múltiples Sistemas SAP

Si trabajas con diferentes sistemas (DEV, QA, PRD), puedes configurar múltiples servidores MCP:

```json
{
  "mcpServers": {
    "ABAP-DEV": {
      "command": "/path/to/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/path/to/brootpersonalagent",
      "env": {
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "SAP_ASHOST": "dev.sap.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "dev_user",
        "SAP_PASSWD": "dev_pass"
      }
    },
    "ABAP-QA": {
      "command": "/path/to/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/path/to/brootpersonalagent",
      "env": {
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "SAP_ASHOST": "qa.sap.com",
        "SAP_SYSNR": "01",
        "SAP_CLIENT": "200",
        "SAP_USER": "qa_user",
        "SAP_PASSWD": "qa_pass"
      }
    }
  }
}
```

Luego puedes especificar qué sistema usar:
```
Usa ABAP-DEV para buscar la clase ZCL_TEST
Usa ABAP-QA para activar el programa ZTEST_PROG
```

---

## Opción 4: Usar en Otros Proyectos

### Método 1: Symlink del virtual environment

```bash
# Desde tu otro proyecto
ln -s /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent/.venv .venv-abap

# Configura Claude Desktop para ese proyecto
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": "/path/to/otro-proyecto/.venv-abap/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent"
    }
  }
}
```

### Método 2: Instalación como paquete

```bash
# Instala en el virtual env de tu otro proyecto
cd /path/to/otro-proyecto
source .venv/bin/activate
pip install -e /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent
```

---

## Verificación de la Instalación

### 1. Verifica que el servidor inicia correctamente

```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent
.venv/bin/python -m app.main
```

Deberías ver el servidor iniciarse sin errores. Presiona Ctrl+C para detenerlo.

### 2. Verifica la conexión SAP con tests

```bash
# Prueba la conexión y funcionalidad básica
.venv/bin/python -m pytest app/tests/test_integration.py -v
```

### 3. Verifica en Claude Desktop

1. **Busca el ícono de herramientas** 🔧 en Claude Desktop
2. **Verifica que aparece** "ABAP-ADT-RFC-Server"
3. **El indicador debe estar verde** ✅
4. **Prueba una consulta**:
   ```
   Lista las primeras 5 clases que empiezan con CL_ABAP
   ```

---

## Troubleshooting

### Problema: El MCP no aparece en Claude Desktop

**Solución**:
1. Verifica que el archivo de configuración existe:
   ```bash
   # macOS
   ls -la ~/Library/Application\ Support/Claude/claude_desktop_config.json

   # Windows
   dir %APPDATA%\Claude\claude_desktop_config.json
   ```

2. Verifica que el JSON es válido:
   ```bash
   cat ~/Library/Application\ Support/Claude/claude_desktop_config.json | python -m json.tool
   ```

3. Revisa los logs de Claude Desktop:
   ```bash
   # macOS
   tail -f ~/Library/Logs/Claude/mcp*.log
   ```

### Problema: "Connection failed" o "RFC error"

**Solución**:
1. Verifica las credenciales en `.env`:
   ```bash
   cat .env
   ```

2. Verifica la conexión SAP directamente:
   ```bash
   ping vhs4dapci.crystal.com.co
   ```

3. Verifica el SAP Router:
   ```bash
   # Asegúrate que el router está accesible
   ping 190.145.188.150
   ```

4. Revisa los logs del servidor:
   ```bash
   tail -f logs/dev_rfc.log
   ```

### Problema: "Module not found: pyrfc"

**Solución rápida**:
```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent
./setup.sh  # El script reinstalará todo automáticamente
```

**Solución manual**:
```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent
source .venv/bin/activate
cd PyRFC
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
python -m pip install .
```

### Problema: El servidor se cae después de un tiempo

**Solución**:
✅ **Ya está resuelto** - Las mejoras de estabilidad implementadas (2025-10-21) solucionan este problema.

Ver [docs/STABILITY_IMPROVEMENTS.md](docs/STABILITY_IMPROVEMENTS.md) para detalles.

---

## Herramientas Disponibles (59 Tools)

Una vez configurado, tendrás acceso a 59 herramientas MCP organizadas en 12 categorías:

### 📦 Repository & Source (9 tools)
- `get_class_source` - Obtener código fuente de clases
- `get_program_source` - Obtener código de programas
- `get_object_source` - Obtener fuente de cualquier objeto
- `search_objects` - Buscar objetos por patrón

### 📊 Data Dictionary (4 tools)
- `get_ddic_element` - Obtener definiciones de tablas/estructuras
- `get_table_contents` - Preview de datos de tabla
- `run_query` - Ejecutar queries SQL

### 🚚 Transport Management (14 tools)
- `create_transport` - Crear orden de transporte
- `list_user_transports` - Listar tus transportes
- `add_object_to_transport` - Agregar objeto a transporte
- `release_transport` - Liberar transporte

### ✏️ Object Modification (3 tools)
- `lock` - Bloquear objeto para edición
- `unlock` - Desbloquear objeto
- `set_object_source` - Modificar código fuente

### ✅ Activation (3 tools)
- `activate` - Activar un objeto
- `activate_objects` - Activar múltiples objetos
- `get_inactive_objects` - Listar objetos inactivos

### 🔍 CDS Views (4 tools)
- `get_cds_view_metadata` - Metadata de vista CDS
- `get_cds_view_source` - DDL source de CDS
- `search_cds_views_by_sqlview` - Buscar por SQL view

### 🎯 RAP Objects (8 tools)
- `get_service_binding` - Service binding (SRVB)
- `get_behavior_definition` - BDEF source
- `get_odata_service_info` - Info de servicio OData
- `explore_rap_object` - Explorar relaciones RAP

### 🔧 Enhancements (3 tools)
- `search_enhancements` - Buscar ampliaciones
- `get_enhancement_metadata` - Metadata de ENHO
- `get_enhancement_source` - Source de enhancement

**Ver [README.md](README.md) para lista completa y ejemplos.**

---

## Soporte

Si encuentras problemas:

1. **Revisa los logs**: `tail -f logs/dev_rfc.log`
2. **Consulta la documentación**: [docs/STABILITY_IMPROVEMENTS.md](docs/STABILITY_IMPROVEMENTS.md)
3. **Verifica la configuración**: `cat .env`
4. **Prueba la conexión**: Script de verificación arriba

---

## Próximos Pasos

Después de instalar:

1. ✅ Prueba las herramientas básicas (search, get_class_source)
2. ✅ Explora las 59 herramientas disponibles
3. ✅ Crea transportes y modifica objetos
4. ✅ Explora objetos CDS y RAP
5. ✅ Automatiza tareas repetitivas con Claude Desktop

**¡Disfruta de tu nuevo MCP ABAP! 🚀**

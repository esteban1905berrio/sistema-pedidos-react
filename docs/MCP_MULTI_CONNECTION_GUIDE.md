# Guía de Configuración Multi-Conexión MCP

Esta guía explica cómo configurar múltiples conexiones SAP usando el MCP Server, compatible con **Claude Desktop**, **Cursor**, **Windsurf** y cualquier cliente MCP.

## Enfoque de Configuración

### ✅ **Principio Fundamental**

**Todas las credenciales SAP deben estar en el archivo `.mcp.json`**, NO en archivos `.env`.

- El archivo `.env` es **SOLO para desarrollo local y tests**
- Cada conexión MCP es una instancia separada del servidor con sus propias credenciales
- Este enfoque es escalable: puedes tener 2, 10, o 100 conexiones SAP

### 🔧 **Configuración del `.mcp.json`**

Cada sistema SAP se configura como un servidor MCP separado:

```json
{
  "mcpServers": {
    "SAP-PRD": {
      "command": "/ruta/al/proyecto/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/ruta/al/proyecto",
      "env": {
        "SAP_ASHOST": "172.27.154.8",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "usuario_prd",
        "SAP_PASSWD": "password_prd",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router.com/S/3299",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib:/usr/local/lib:/usr/lib",
        "PYTHONPATH": "/ruta/al/proyecto",
        "DYLD_FALLBACK_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "MAX_CONTENT_LENGTH": "100000"
      }
    },
    "SAP-QAS": {
      "command": "/ruta/al/proyecto/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/ruta/al/proyecto",
      "env": {
        "SAP_ASHOST": "172.28.0.56",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "200",
        "SAP_USER": "usuario_qas",
        "SAP_PASSWD": "password_qas",
        "SAP_LANG": "ES",
        "SAP_ROUTER": "",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib:/usr/local/lib:/usr/lib",
        "PYTHONPATH": "/ruta/al/proyecto",
        "DYLD_FALLBACK_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "MAX_CONTENT_LENGTH": "100000"
      }
    },
    "SAP-DEV": {
      "command": "/ruta/al/proyecto/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/ruta/al/proyecto",
      "env": {
        "SAP_ASHOST": "172.29.0.100",
        "SAP_SYSNR": "01",
        "SAP_CLIENT": "300",
        "SAP_USER": "usuario_dev",
        "SAP_PASSWD": "password_dev",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "",
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib:/usr/local/lib:/usr/lib",
        "PYTHONPATH": "/ruta/al/proyecto",
        "DYLD_FALLBACK_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "MAX_CONTENT_LENGTH": "100000"
      }
    }
  }
}
```

## Variables de Entorno Requeridas

### **Variables SAP (obligatorias)**

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SAP_ASHOST` | Host del servidor SAP | `172.27.154.8` |
| `SAP_SYSNR` | Número de sistema | `00` |
| `SAP_CLIENT` | Mandante | `100` |
| `SAP_USER` | Usuario SAP | `DEVELOPER` |
| `SAP_PASSWD` | Contraseña | `MyPassword123` |
| `SAP_LANG` | Idioma | `EN`, `ES`, `DE` |
| `SAP_ROUTER` | SAProuter (opcional) | `/H/router/S/3299` o `""` |

### **Variables del Sistema (obligatorias)**

| Variable | Descripción | Valor (macOS) |
|----------|-------------|---------------|
| `SAPNWRFC_HOME` | Ruta al SAP RFC SDK | `/Users/local/nwrfcsdk` |
| `DYLD_LIBRARY_PATH` | Rutas de librerías | `/Users/local/nwrfcsdk/lib:/usr/local/lib:/usr/lib` |
| `PYTHONPATH` | Ruta del proyecto | `/ruta/al/proyecto` |
| `DYLD_FALLBACK_LIBRARY_PATH` | Ruta fallback | `/Users/local/nwrfcsdk/lib` |
| `MAX_CONTENT_LENGTH` | Límite de contenido | `100000` |

## Uso en Diferentes Clientes MCP

### **Claude Desktop**

Ubica el archivo `.mcp.json` en:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

### **Cursor**

Ubica el archivo `.mcp.json` en la raíz de tu proyecto o workspace.

### **Windsurf**

Ubica el archivo `.mcp.json` según la configuración de Windsurf (consulta su documentación).

## Casos de Uso

### **1. Copiar Objetos Entre Sistemas**

```
Usuario: Copia la clase ZCLS001 desde SAP-DEV a SAP-QAS

Claude usa:
- mcp__SAP-DEV__get_class_source("ZCLS001")
- mcp__SAP-QAS__create_class("ZCLS001", ...)
```

### **2. Comparar Configuraciones**

```
Usuario: Compara la tabla ZTABLE en PRD vs QAS

Claude usa:
- mcp__SAP-PRD__get_table_contents("ZTABLE")
- mcp__SAP-QAS__get_table_contents("ZTABLE")
```

### **3. Promoción de Transporte**

```
Usuario: Verifica que la OT DEVK900123 existe en todos los ambientes

Claude usa:
- mcp__SAP-DEV__get_transport_request("DEVK900123")
- mcp__SAP-QAS__get_transport_request("DEVK900123")
- mcp__SAP-PRD__get_transport_request("DEVK900123")
```

## Desarrollo Local

Para desarrollo local y tests, usa el archivo `.env`:

```bash
# .env (para desarrollo local solamente)
LOAD_DOTENV=1
SAP_ASHOST=localhost
SAP_SYSNR=00
SAP_CLIENT=100
SAP_USER=developer
SAP_PASSWD=dev_password
SAP_LANG=EN
SAP_ROUTER=
```

**IMPORTANTE**: El archivo `.env` **SOLO** se carga si defines `LOAD_DOTENV=1`. Cuando se ejecuta como MCP server, esta variable NO está definida, por lo que el `.env` se ignora.

## Solución de Problemas

### **Error: "Connection timeout"**

**Causa**: Usando SAProuter incorrecto para el sistema destino.

**Solución**: Verifica que `SAP_ROUTER` esté correctamente configurado:
- Si NO usas router: `"SAP_ROUTER": ""`
- Si usas router: `"SAP_ROUTER": "/H/router_host/S/port"`

### **Error: "Missing required environment variables"**

**Causa**: Variables SAP no están definidas en `.mcp.json`.

**Solución**: Asegúrate de que TODAS las variables SAP estén en el bloque `env` del `.mcp.json`.

### **Error: "Wrong credentials"**

**Causa**: Usuario/contraseña incorrectos O caracteres especiales mal escapados.

**Solución**: Si la contraseña tiene caracteres especiales (`*`, `"`, `\`, etc.), escápalos correctamente en JSON:
- `"SAP_PASSWD": "Pass**word"` → Correcto
- Si tienes problemas, prueba con comillas escapadas: `"SAP_PASSWD": "\"Pass**word\""`

## Arquitectura

```
Cliente MCP (Claude, Cursor, etc.)
    ↓
.mcp.json (credenciales por sistema)
    ↓
MCP Server instancia 1 (SAP-PRD) → Sistema SAP PRD
MCP Server instancia 2 (SAP-QAS) → Sistema SAP QAS
MCP Server instancia 3 (SAP-DEV) → Sistema SAP DEV
```

Cada instancia del MCP Server:
1. Lee las variables de entorno del `.mcp.json`
2. **NO** carga el archivo `.env` (a menos que `LOAD_DOTENV=1`)
3. Se conecta a su sistema SAP asignado
4. Es completamente independiente de las otras instancias

## Ventajas de Este Enfoque

✅ **Escalable**: Puedes tener N conexiones SAP sin modificar código
✅ **Compatible**: Funciona con cualquier cliente MCP
✅ **Seguro**: Credenciales centralizadas en `.mcp.json`
✅ **Simple**: No requiere archivos `.env` separados por sistema
✅ **Mantenible**: Una configuración clara por sistema

## Ejemplo Completo

Ver el archivo `.mcp.json.example` en la raíz del proyecto para un ejemplo completo.

# Plan de Implementación de Features MCP

**Proyecto**: giralmcp (SAP ABAP MCP Server)
**Fecha**: 2025-11-25
**Versión**: 1.0
**Estado**: Propuesta

---

## Resumen Ejecutivo

Este documento describe las funcionalidades del Model Context Protocol (MCP) que pueden implementarse en el servidor SAP ABAP para mejorar la experiencia del usuario y aprovechar al máximo las capacidades del protocolo.

### Estado Actual

| Categoría | Estado | Cobertura |
|-----------|--------|-----------|
| Tools | ✅ Implementado | 117 tools en 13 archivos |
| Resources | ❌ No implementado | 0% |
| Prompts | ❌ No implementado | 0% |
| Progress Reporting | ❌ No implementado | 0% |
| Logging MCP | ❌ No implementado | 0% |
| Elicitation | ❌ No implementado | 0% |
| Notifications | ❌ No implementado | 0% |
| Completions | ❌ No implementado | 0% |
| Cancellation | ❌ No implementado | 0% |

**Conclusión**: El servidor utiliza aproximadamente 30% de las capacidades de MCP.

---

## Categorías MCP: Definiciones y Aplicaciones

### 1. Tools (✅ Implementado)

#### Definición

**Tools** son funciones ejecutables que el LLM puede invocar para realizar acciones en el sistema SAP. Representan operaciones con posibles efectos secundarios (crear, modificar, eliminar) o consultas complejas.

#### Características

| Aspecto | Descripción |
|---------|-------------|
| **Iniciativa** | El LLM decide cuándo llamarlos basándose en el contexto |
| **Side Effects** | Pueden modificar datos en SAP |
| **Parámetros** | Reciben argumentos tipados con validación |
| **Respuesta** | Retornan datos estructurados (JSON) |

#### Flujo de Ejecución

```
Usuario: "Muéstrame el código de ZCL_INVOICE"
    │
    ▼
LLM analiza contexto y decide llamar tool
    │
    ▼
MCP Client: tools/call { name: "get_class_source", arguments: { className: "ZCL_INVOICE" }}
    │
    ▼
MCP Server: Ejecuta ClassService.getClassSource()
    │
    ▼
SAP System: Retorna código fuente via RFC
    │
    ▼
LLM: Presenta resultado al usuario
```

#### Estado en giralmcp

**Implementación**: Completa y bien estructurada.

```
src/main/java/com/crystal/mcp/sapserver/tool/
├── ActivationTools.java      → activateObjects, getInactiveObjects
├── ClassTools.java           → get_class_source, get_class_includes, modify_class
├── CreationTools.java        → create_class, create_function_group, create_function_module
├── DeletionTools.java        → delete_object
├── DictionaryTools.java      → create_table
├── NavigationTools.java      → get_package_objects, get_table_contents
├── ObjectTools.java          → get_object_source, get_object_structure
├── PackageHierarchyTools.java → getPackageHierarchy
├── ProgramTools.java         → get_program_source, modify_program_source, modify_function_module
├── SearchTools.java          → search_objects
├── SyntaxCheckTools.java     → check_syntax
├── TransportCopyTools.java   → create_transport_copy
└── TransportTools.java       → list_user_transports, get_transport_objects, get_transport_info
```

#### Mejoras Potenciales

- [ ] Estandarizar naming convention (actualmente mezcla snake_case y camelCase)
- [ ] Agregar campo `_meta` en respuestas para información auxiliar
- [ ] Implementar `outputSchema` para validación de respuestas

---

### 2. Resources (❌ No Implementado)

#### Definición

**Resources** son datos expuestos como URIs que el LLM puede leer de forma pasiva. A diferencia de los Tools, los Resources son **solo lectura** y están diseñados para proporcionar contexto sin ejecutar acciones.

#### Características

| Aspecto | Tools | Resources |
|---------|-------|-----------|
| **Propósito** | Ejecutar acciones | Exponer datos para lectura |
| **Side Effects** | Sí pueden tener | Solo lectura, sin efectos |
| **Caching** | No recomendado | El cliente puede cachear |
| **Subscripción** | No aplica | Soporte para notificaciones de cambio |
| **Descubrimiento** | `tools/list` | `resources/list` + templates |

#### Analogía

Resources funcionan como un **sistema de archivos virtual** que el LLM puede explorar:

```
sap://
├── classes/
│   ├── ZCL_INVOICE/
│   │   ├── definition        → Código de definición
│   │   ├── implementation    → Código de implementación
│   │   └── methods           → Lista de métodos (metadata)
│   └── ZCL_CUSTOMER/
│       └── ...
├── transports/
│   ├── DEVK900123/
│   │   ├── info              → Metadata del transporte
│   │   └── objects           → Lista de objetos
│   └── DEVK900124/
│       └── ...
├── packages/
│   └── ZFINANCE/
│       ├── structure         → Jerarquía de subpaquetes
│       └── objects           → Objetos en el paquete
└── tables/
    └── MARA/
        ├── fields            → Definición de campos
        └── sample            → Datos de muestra (limitados)
```

#### Beneficio vs Tools

```
┌─────────────────────────────────────────────────────────────┐
│ ESCENARIO: Usuario pregunta "¿Qué métodos tiene ZCL_INVOICE?" │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ SIN Resources (actual):                                      │
│ ┌──────────┐    get_class_source()    ┌──────────┐          │
│ │   LLM    │ ───────────────────────► │   SAP    │          │
│ └──────────┘                          └──────────┘          │
│      │                                      │               │
│      │◄─────── 5000 líneas de código ───────┤               │
│      │                                                      │
│      ▼                                                      │
│ LLM parsea mentalmente para encontrar métodos               │
│ Resultado: Lento, muchos tokens, propenso a errores         │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ CON Resources (propuesto):                                   │
│ ┌──────────┐  resources/read           ┌──────────┐         │
│ │   LLM    │  sap://class/ZCL_INVOICE/ │   SAP    │         │
│ └──────────┘  methods ────────────────►└──────────┘         │
│      │                                      │               │
│      │◄─────── Lista de 15 métodos ─────────┤               │
│      │         (solo nombres y firmas)                      │
│      ▼                                                      │
│ Resultado: Rápido, pocos tokens, preciso                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Subscripciones

Los clientes pueden subscribirse a cambios en resources:

```json
// Cliente subscribe
{
  "method": "resources/subscribe",
  "params": { "uri": "sap://transport/DEVK900123/objects" }
}

// Servidor notifica cuando cambia
{
  "method": "notifications/resources/updated",
  "params": { "uri": "sap://transport/DEVK900123/objects" }
}
```

#### Propuesta de Implementación

**Resource Templates** (URIs dinámicas):

| Template | Descripción | Ejemplo |
|----------|-------------|---------|
| `sap://class/{name}/definition` | Definición de clase | `sap://class/ZCL_INVOICE/definition` |
| `sap://class/{name}/methods` | Lista de métodos | `sap://class/ZCL_INVOICE/methods` |
| `sap://transport/{id}/objects` | Objetos en transporte | `sap://transport/DEVK900123/objects` |
| `sap://transport/{id}/info` | Info del transporte | `sap://transport/DEVK900123/info` |
| `sap://package/{name}/objects` | Objetos en paquete | `sap://package/ZFINANCE/objects` |
| `sap://table/{name}/fields` | Campos de tabla | `sap://table/MARA/fields` |
| `sap://user/{id}/transports` | Transportes de usuario | `sap://user/DEVELOPER/transports` |

**Prioridad**: 🔴 Alta
**Esfuerzo**: Medio (2-3 semanas)
**Impacto**: Alto - Mejora significativa en eficiencia y UX

---

### 3. Prompts (❌ No Implementado)

#### Definición

**Prompts** son templates reutilizables que estructuran interacciones comunes con el LLM. Proporcionan "recetas" predefinidas para tareas frecuentes, asegurando consistencia y calidad en las respuestas.

#### Características

| Aspecto | Tools | Prompts |
|---------|-------|---------|
| **Retorna** | Datos/resultados de SAP | Texto estructurado para el LLM |
| **Propósito** | Ejecutar acción específica | Guiar conversación/análisis |
| **Selección** | LLM decide automáticamente | Usuario selecciona explícitamente |
| **Argumentos** | Parámetros técnicos | Contexto para el template |

#### Analogía

Prompts son como **plantillas de documento** que el usuario selecciona:

```
┌─────────────────────────────────────────────────────────────┐
│ PROMPTS DISPONIBLES:                                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ [📝 review_abap_code]                                        │
│     "Realiza code review de código ABAP"                     │
│     Parámetros: className, focusAreas (opcional)             │
│                                                              │
│ [🔍 analyze_transport]                                       │
│     "Analiza contenido y riesgos de un transporte"           │
│     Parámetros: transportId                                  │
│                                                              │
│ [📚 explain_class]                                           │
│     "Explica propósito y arquitectura de una clase"          │
│     Parámetros: className, depth (básico/detallado)          │
│                                                              │
│ [🐛 debug_dump]                                              │
│     "Analiza un dump ABAP y sugiere soluciones"              │
│     Parámetros: dumpText, programName                        │
│                                                              │
│ [✅ migration_checklist]                                     │
│     "Genera checklist para migrar objeto a producción"       │
│     Parámetros: objectName, objectType                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Flujo de Uso

```
1. Usuario: "Quiero hacer code review de ZCL_INVOICE"

2. Cliente muestra prompts disponibles:
   → Usuario selecciona "review_abap_code"

3. Cliente solicita prompt:
   { "method": "prompts/get",
     "params": { "name": "review_abap_code",
                 "arguments": { "className": "ZCL_INVOICE" }}}

4. Servidor retorna template expandido:
   {
     "messages": [
       {
         "role": "user",
         "content": {
           "type": "text",
           "text": "Por favor revisa el código de la clase ZCL_INVOICE enfocándote en:\n\n1. Cumplimiento de naming conventions Crystal\n2. Manejo de excepciones\n3. Performance (SELECT en loops, N+1 queries)\n4. Seguridad (authority checks, SQL injection)\n5. Legibilidad y documentación\n\nUsa get_class_source para obtener el código y proporciona feedback estructurado con:\n- Línea del problema\n- Severidad (crítico/mayor/menor)\n- Descripción del issue\n- Sugerencia de corrección"
         }
       }
     ]
   }

5. LLM ejecuta el análisis siguiendo el template
```

#### Propuesta de Implementación

| Prompt | Descripción | Argumentos |
|--------|-------------|------------|
| `review_abap_code` | Code review completo | `className`, `focusAreas?` |
| `analyze_transport` | Análisis de transporte | `transportId` |
| `explain_class` | Explicación de clase | `className`, `depth?` |
| `debug_dump` | Análisis de dump | `dumpText`, `programName?` |
| `migration_checklist` | Checklist pre-producción | `objectName`, `objectType` |
| `compare_objects` | Comparar dos versiones | `objectName`, `version1`, `version2` |
| `generate_test_class` | Generar unit tests | `className`, `methodName?` |
| `document_function` | Documentar FM | `functionName` |

**Prioridad**: 🟡 Media
**Esfuerzo**: Bajo (1 semana)
**Impacto**: Medio - Mejora consistencia y guía al usuario

---

### 4. Progress Reporting (❌ No Implementado)

#### Definición

**Progress Reporting** es un mecanismo para reportar el avance de operaciones largas al cliente. Permite que el usuario vea feedback en tiempo real en lugar de esperar en silencio.

#### Características

| Aspecto | Descripción |
|---------|-------------|
| **Dirección** | Servidor → Cliente (notificaciones) |
| **Trigger** | Cliente envía `progressToken` en request |
| **Contenido** | Progreso actual, total, mensaje descriptivo |
| **Frecuencia** | A discreción del servidor |

#### Flujo de Ejecución

```
┌──────────────────────────────────────────────────────────────────┐
│ ACTIVACIÓN DE 50 OBJETOS CON PROGRESS REPORTING                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Cliente envía request con progressToken:                       │
│    {                                                              │
│      "method": "tools/call",                                      │
│      "params": {                                                  │
│        "name": "activateObjects",                                 │
│        "arguments": { "objectUris": [...50 URIs...] },            │
│        "_meta": { "progressToken": "act-123" }                    │
│      }                                                            │
│    }                                                              │
│                                                                   │
│ 2. Servidor procesa y envía notificaciones:                       │
│                                                                   │
│    ┌─────────┐                              ┌─────────┐          │
│    │ Cliente │◄────── progress 5/50 ────────│ Servidor│          │
│    │         │        "Activando ZCL_A..."  │         │          │
│    │         │◄────── progress 15/50 ───────│         │          │
│    │         │        "Activando ZCL_B..."  │         │          │
│    │         │◄────── progress 30/50 ───────│         │          │
│    │         │        "Activando ZREP_X..." │         │          │
│    │         │◄────── progress 50/50 ───────│         │          │
│    │         │        "Completado"          │         │          │
│    └─────────┘                              └─────────┘          │
│                                                                   │
│ 3. Finalmente, respuesta completa:                                │
│    { "result": { "activated": 48, "errors": 2, "details": [...] }}│
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Operaciones Candidatas en giralmcp

| Operación | Tiempo típico | Beneficio de Progress |
|-----------|---------------|----------------------|
| `activateObjects` (múltiples) | 30s - 5min | 🔴 Alto |
| `search_objects` (query amplio) | 10s - 1min | 🟡 Medio |
| `create_transport_copy` (múltiples) | 20s - 2min | 🔴 Alto |
| `get_package_objects` (paquete grande) | 5s - 30s | 🟡 Medio |
| `check_syntax` (múltiples objetos) | 10s - 1min | 🟡 Medio |

#### Estructura de Notificación

```json
{
  "jsonrpc": "2.0",
  "method": "notifications/progress",
  "params": {
    "progressToken": "act-123",
    "progress": 25,
    "total": 50,
    "message": "Activando objeto ZCL_INVOICE_PROCESSOR..."
  }
}
```

**Prioridad**: 🔴 Alta
**Esfuerzo**: Bajo (3-5 días)
**Impacto**: Alto - Mejora significativa en UX para operaciones largas

---

### 5. Logging MCP (❌ No Implementado)

#### Definición

**Logging MCP** permite al servidor enviar mensajes de log al cliente, haciéndolos visibles en la UI de Claude Desktop. A diferencia del logging tradicional (a archivo), estos logs son para **transparencia con el usuario**.

#### Características

| Aspecto | Logging Tradicional | Logging MCP |
|---------|---------------------|-------------|
| **Destino** | `logs/sap-mcp-server.log` | Claude Desktop UI |
| **Audiencia** | Desarrollador | Usuario final |
| **Propósito** | Debug técnico | Transparencia de operaciones |
| **Niveles** | DEBUG, INFO, WARN, ERROR | debug, info, notice, warning, error, critical, alert, emergency |

#### Niveles de Log

```
┌───────────────────────────────────────────────────────────┐
│ NIVELES DE LOG MCP (de menor a mayor severidad)           │
├───────────────────────────────────────────────────────────┤
│                                                           │
│ debug     → Información detallada para troubleshooting    │
│ info      → Información general de operaciones            │
│ notice    → Eventos normales pero significativos          │
│ warning   → Situaciones anómalas no críticas              │
│ error     → Errores que afectan la operación              │
│ critical  → Errores críticos del sistema                  │
│ alert     → Acción inmediata requerida                    │
│ emergency → Sistema inutilizable                          │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

#### Ejemplo de Uso

```
Usuario: "Modifica la clase ZCL_TEST agregando un nuevo método"

┌─────────────────────────────────────────────────────────────┐
│ CLAUDE DESKTOP - Log Panel                                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ [14:32:01] INFO    Connecting to SAP system S4D...           │
│ [14:32:02] INFO    Connection established (pool: 3/5)        │
│ [14:32:02] INFO    Locking object ZCL_TEST...                │
│ [14:32:03] INFO    Lock acquired successfully                │
│ [14:32:03] INFO    Transport assigned: DEVK900456            │
│ [14:32:03] INFO    Reading current source code...            │
│ [14:32:04] INFO    Modifying class definition...             │
│ [14:32:05] WARNING Object has 2 syntax warnings (non-blocking)│
│ [14:32:05] INFO    Saving changes...                         │
│ [14:32:06] INFO    Unlocking object...                       │
│ [14:32:06] INFO    Modification complete                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Configuración de Nivel

El cliente puede solicitar cambiar el nivel de logging:

```json
// Cliente solicita solo warnings y superiores
{
  "method": "logging/setLevel",
  "params": { "level": "warning" }
}

// Servidor confirma
{
  "result": {}
}

// A partir de ahora, solo envía warning, error, critical, etc.
```

#### Capability Declaration

```json
{
  "capabilities": {
    "logging": {}
  }
}
```

**Prioridad**: 🟡 Media
**Esfuerzo**: Bajo (2-3 días)
**Impacto**: Medio - Mejora transparencia y debugging

---

### 6. Elicitation (❌ No Implementado)

#### Definición

**Elicitation** permite al servidor solicitar información adicional al usuario durante la ejecución de una operación. Es como un "popup de confirmación" dentro del flujo MCP.

#### Características

| Aspecto | Descripción |
|---------|-------------|
| **Dirección** | Servidor → Cliente → Usuario → Cliente → Servidor |
| **Propósito** | Obtener confirmación o datos adicionales |
| **Schema** | JSON Schema para validar respuesta |
| **Acciones** | accept, decline, cancel |

#### Flujo de Ejecución

```
┌──────────────────────────────────────────────────────────────────┐
│ ELICITATION: Objeto bloqueado por otro usuario                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Usuario: "Modifica ZCL_INVOICE"                                │
│                                                                   │
│ 2. Servidor detecta que objeto está bloqueado:                    │
│    - Locked by: JOHN_DOE                                          │
│    - Transport: DEVK900123                                        │
│                                                                   │
│ 3. Servidor envía elicitation:                                    │
│    {                                                              │
│      "method": "elicitation/create",                              │
│      "params": {                                                  │
│        "message": "ZCL_INVOICE está bloqueado por JOHN_DOE\n"     │
│                   "en transporte DEVK900123.\n\n"                 │
│                   "¿Desea continuar de todos modos?",             │
│        "schema": {                                                │
│          "type": "object",                                        │
│          "properties": {                                          │
│            "continue": {                                          │
│              "type": "boolean",                                   │
│              "description": "Agregar a mi propio transporte"      │
│            },                                                     │
│            "forceUnlock": {                                       │
│              "type": "boolean",                                   │
│              "description": "Forzar desbloqueo (requiere auth)"   │
│            }                                                      │
│          }                                                        │
│        }                                                          │
│      }                                                            │
│    }                                                              │
│                                                                   │
│ 4. Claude Desktop muestra UI al usuario:                          │
│    ┌─────────────────────────────────────────┐                   │
│    │ ⚠️ ZCL_INVOICE está bloqueado           │                   │
│    │                                         │                   │
│    │ Bloqueado por: JOHN_DOE                 │                   │
│    │ Transporte: DEVK900123                  │                   │
│    │                                         │                   │
│    │ ☐ Agregar a mi propio transporte        │                   │
│    │ ☐ Forzar desbloqueo (requiere auth)     │                   │
│    │                                         │                   │
│    │ [Continuar]  [Cancelar]                 │                   │
│    └─────────────────────────────────────────┘                   │
│                                                                   │
│ 5. Usuario responde:                                              │
│    {                                                              │
│      "action": "accept",                                          │
│      "content": { "continue": true, "forceUnlock": false }        │
│    }                                                              │
│                                                                   │
│ 6. Servidor continúa con la operación                             │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Casos de Uso en giralmcp

| Escenario | Mensaje | Opciones |
|-----------|---------|----------|
| **Objeto bloqueado** | "Objeto bloqueado por {user}" | Continuar / Forzar / Cancelar |
| **Múltiples coincidencias** | "Encontré 3 clases" | Seleccionar una |
| **Confirmación destructiva** | "¿Eliminar {object}?" | Confirmar / Cancelar |
| **Transporte requerido** | "Se necesita transporte" | Crear nuevo / Usar existente |
| **Syntax errors** | "Hay {n} errores de sintaxis" | Continuar / Abortar |
| **Objetos dependientes** | "Hay {n} objetos dependientes" | Ver lista / Continuar |

**Prioridad**: 🟡 Media
**Esfuerzo**: Medio (1-2 semanas)
**Impacto**: Alto - Workflows interactivos sin romper flujo

---

### 7. Notifications (❌ No Implementado)

#### Definición

**Notifications** son mensajes unidireccionales del servidor al cliente para informar sobre cambios en el estado. No requieren respuesta del cliente.

#### Tipos de Notifications

| Notification | Trigger | Uso |
|--------------|---------|-----|
| `notifications/tools/list_changed` | Tools disponibles cambian | Actualizar lista de tools |
| `notifications/resources/list_changed` | Resources disponibles cambian | Actualizar lista de resources |
| `notifications/resources/updated` | Resource subscrito cambia | Re-leer resource |
| `notifications/progress` | Progreso de operación | Mostrar barra de progreso |
| `notifications/message` | Log message | Mostrar en panel de logs |

#### Flujo para Resource Updates

```
┌──────────────────────────────────────────────────────────────────┐
│ SUBSCRIPCIÓN Y NOTIFICACIÓN DE CAMBIOS                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Cliente subscribe a transporte:                                │
│    {                                                              │
│      "method": "resources/subscribe",                             │
│      "params": { "uri": "sap://transport/DEVK900123/objects" }    │
│    }                                                              │
│                                                                   │
│ 2. Servidor confirma:                                             │
│    { "result": {} }                                               │
│                                                                   │
│ 3. [Tiempo después] Otro usuario agrega objeto al transporte      │
│                                                                   │
│ 4. Servidor detecta cambio y notifica:                            │
│    {                                                              │
│      "method": "notifications/resources/updated",                 │
│      "params": {                                                  │
│        "uri": "sap://transport/DEVK900123/objects",               │
│        "title": "Transport DEVK900123 updated"                    │
│      }                                                            │
│    }                                                              │
│                                                                   │
│ 5. Claude Desktop muestra notificación al usuario                 │
│                                                                   │
│ 6. Cliente puede re-leer el resource para ver cambios             │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Implementación en SAP

Para detectar cambios en SAP, hay dos estrategias:

| Estrategia | Descripción | Complejidad |
|------------|-------------|-------------|
| **Polling** | Verificar periódicamente tabla E070/E071 | Baja |
| **SAP Events** | Usar Business Events o Change Documents | Alta |

**Prioridad**: 🟡 Media
**Esfuerzo**: Medio-Alto (2-3 semanas)
**Impacto**: Medio - Sincronización en tiempo real

---

### 8. Completions (❌ No Implementado)

#### Definición

**Completions** proporciona autocompletado de argumentos mientras el usuario escribe. El servidor sugiere valores válidos basándose en datos reales de SAP.

#### Flujo de Ejecución

```
┌──────────────────────────────────────────────────────────────────┐
│ AUTOCOMPLETADO DE NOMBRE DE CLASE                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Usuario escribe en Claude Desktop:                             │
│    > get_class_source className="ZCL_INV                          │
│                                      ▲                            │
│                                      │ cursor                     │
│                                                                   │
│ 2. Cliente envía request de completion:                           │
│    {                                                              │
│      "method": "completion/complete",                             │
│      "params": {                                                  │
│        "ref": {                                                   │
│          "type": "ref/argument",                                  │
│          "name": "className"                                      │
│        },                                                         │
│        "argument": {                                              │
│          "name": "className",                                     │
│          "value": "ZCL_INV"                                       │
│        }                                                          │
│      }                                                            │
│    }                                                              │
│                                                                   │
│ 3. Servidor busca en SAP objetos que empiecen con "ZCL_INV"       │
│                                                                   │
│ 4. Servidor responde con sugerencias:                             │
│    {                                                              │
│      "completion": {                                              │
│        "values": [                                                │
│          "ZCL_INVOICE",                                           │
│          "ZCL_INVENTORY",                                         │
│          "ZCL_INVOICE_PROCESSOR",                                 │
│          "ZCL_INVOICE_VALIDATOR"                                  │
│        ],                                                         │
│        "hasMore": true,                                           │
│        "total": 12                                                │
│      }                                                            │
│    }                                                              │
│                                                                   │
│ 5. Claude Desktop muestra dropdown:                               │
│    ┌─────────────────────────────┐                               │
│    │ ZCL_INVOICE                 │                               │
│    │ ZCL_INVENTORY               │                               │
│    │ ZCL_INVOICE_PROCESSOR       │                               │
│    │ ZCL_INVOICE_VALIDATOR       │                               │
│    │ ... (8 más)                 │                               │
│    └─────────────────────────────┘                               │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Campos Candidatos para Autocompletado

| Campo | Fuente de datos |
|-------|-----------------|
| `className` | Búsqueda de clases en SAP |
| `programName` | Búsqueda de programas |
| `functionModuleName` | Búsqueda de FMs |
| `transportId` | Transportes del usuario |
| `packageName` | Paquetes existentes |
| `tableName` | Tablas en diccionario |

#### Capability Declaration

```json
{
  "capabilities": {
    "completions": {}
  }
}
```

**Prioridad**: 🟢 Baja
**Esfuerzo**: Medio (1-2 semanas)
**Impacto**: Bajo-Medio - Mejora UX pero no crítico

---

### 9. Cancellation (❌ No Implementado)

#### Definición

**Cancellation** permite al usuario cancelar operaciones en progreso. El servidor debe manejar la cancelación gracefully, limpiando recursos y reportando estado parcial.

#### Flujo de Ejecución

```
┌──────────────────────────────────────────────────────────────────┐
│ CANCELACIÓN DE ACTIVACIÓN MASIVA                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Usuario inicia activación de 100 objetos                       │
│    Request ID: "req-456"                                          │
│                                                                   │
│ 2. Servidor comienza a procesar:                                  │
│    - Objeto 1/100: Activado ✓                                     │
│    - Objeto 2/100: Activado ✓                                     │
│    - ...                                                          │
│    - Objeto 25/100: Activando...                                  │
│                                                                   │
│ 3. Usuario decide cancelar:                                       │
│    Claude Desktop: [Cancelar operación]                           │
│                                                                   │
│ 4. Cliente envía notification de cancelación:                     │
│    {                                                              │
│      "method": "notifications/cancelled",                         │
│      "params": {                                                  │
│        "requestId": "req-456",                                    │
│        "reason": "User requested cancellation"                    │
│      }                                                            │
│    }                                                              │
│                                                                   │
│ 5. Servidor recibe cancelación:                                   │
│    - Detiene procesamiento                                        │
│    - Libera locks pendientes                                      │
│    - Prepara reporte de estado parcial                            │
│                                                                   │
│ 6. Servidor responde con estado parcial:                          │
│    {                                                              │
│      "result": {                                                  │
│        "status": "cancelled",                                     │
│        "processed": 25,                                           │
│        "total": 100,                                              │
│        "activated": 24,                                           │
│        "errors": 1,                                               │
│        "remaining": 75                                            │
│      }                                                            │
│    }                                                              │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Consideraciones de Implementación

| Aspecto | Consideración |
|---------|---------------|
| **Race condition** | La cancelación puede llegar después de completar |
| **Cleanup** | Siempre liberar locks y recursos |
| **Estado parcial** | Reportar qué se completó antes de cancelar |
| **Atomicidad** | Operaciones individuales son atómicas, batch no |

**Prioridad**: 🟢 Baja
**Esfuerzo**: Medio (1-2 semanas)
**Impacto**: Bajo - Útil pero no crítico

---

## Plan de Implementación

### Fase 1: Prompts y Resources (4-5 semanas)

**Objetivo**: Implementar las dos primitivas fundamentales de MCP que transforman la experiencia del usuario.

#### Fase 1.1: Prompts (Semanas 1-2)

| Feature | Prioridad | Esfuerzo | Semana |
|---------|-----------|----------|--------|
| Infraestructura Prompts | 🔴 Alta | Bajo | 1 |
| Prompts SAP básicos | 🔴 Alta | Bajo | 1-2 |
| Prompts avanzados | 🟡 Media | Bajo | 2 |

**Entregables Prompts**:
- [ ] Infraestructura para definir y registrar Prompts (`@McpPrompt` o equivalente)
- [ ] `prompts/list` handler
- [ ] `prompts/get` handler
- [ ] **8 Prompts iniciales**:

| Prompt | Descripción | Argumentos |
|--------|-------------|------------|
| `review_abap_code` | Code review completo siguiendo estándares Crystal | `className`, `focusAreas?` |
| `analyze_transport` | Análisis de contenido, riesgos y dependencias | `transportId` |
| `explain_class` | Explicación de propósito, arquitectura y uso | `className`, `depth?` |
| `debug_dump` | Análisis de dump ABAP con sugerencias | `dumpText`, `programName?` |
| `migration_checklist` | Checklist pre-release a producción | `objectName`, `objectType` |
| `generate_unit_test` | Generar clase de test ABAP Unit | `className`, `methodName?` |
| `document_function_module` | Documentar FM con ejemplos | `functionModuleName` |
| `compare_versions` | Comparar versión activa vs inactiva | `objectName`, `objectType` |

#### Fase 1.2: Resources (Semanas 3-5)

| Feature | Prioridad | Esfuerzo | Semana |
|---------|-----------|----------|--------|
| Infraestructura Resources | 🔴 Alta | Medio | 3 |
| Resource Templates (URIs) | 🔴 Alta | Medio | 3-4 |
| Resources List | 🔴 Alta | Bajo | 4 |
| Resources Read | 🔴 Alta | Medio | 4-5 |

**Entregables Resources**:
- [ ] Infraestructura para definir Resources (`@McpResource` o equivalente)
- [ ] `resources/list` handler (lista resources disponibles)
- [ ] `resources/templates/list` handler (lista templates de URI)
- [ ] `resources/read` handler (lee contenido de resource)
- [ ] **10 Resource Templates iniciales**:

| Template URI | Descripción | Retorna |
|--------------|-------------|---------|
| `sap://class/{name}/definition` | Código de definición de clase | Source code (PUBLIC/PROTECTED/PRIVATE) |
| `sap://class/{name}/implementation` | Código de implementación | Source code (METHOD...ENDMETHOD) |
| `sap://class/{name}/methods` | Lista de métodos (metadata) | JSON: [{name, visibility, parameters}] |
| `sap://class/{name}/attributes` | Lista de atributos | JSON: [{name, type, visibility}] |
| `sap://transport/{id}/info` | Metadata del transporte | JSON: {owner, status, description, target} |
| `sap://transport/{id}/objects` | Objetos en transporte | JSON: [{pgmid, object, objName}] |
| `sap://package/{name}/objects` | Objetos en paquete | JSON: [{type, name, description}] |
| `sap://package/{name}/hierarchy` | Jerarquía de subpaquetes | JSON: árbol de paquetes |
| `sap://table/{name}/fields` | Campos de tabla DDIC | JSON: [{fieldname, type, length, key}] |
| `sap://user/{id}/transports` | Transportes del usuario | JSON: [{trkorr, status, description}] |

**Beneficio clave de Resources vs Tools**:

```
ANTES (solo Tools):
Usuario: "¿Qué métodos tiene ZCL_INVOICE?"
→ get_class_source() retorna 5000 líneas
→ LLM parsea mentalmente
→ Alto consumo de tokens, lento

DESPUÉS (con Resources):
Usuario: "¿Qué métodos tiene ZCL_INVOICE?"
→ resources/read "sap://class/ZCL_INVOICE/methods"
→ Retorna JSON con 15 métodos (nombres y firmas)
→ Bajo consumo de tokens, rápido, preciso
```

### Fase 2: Progress y Logging (2 semanas)

**Objetivo**: Implementar feedback en tiempo real para operaciones largas.

| Feature | Prioridad | Esfuerzo | Semana |
|---------|-----------|----------|--------|
| Progress Reporting | 🔴 Alta | Bajo | 6 |
| Logging MCP | 🟡 Media | Bajo | 6-7 |

**Entregables**:
- [ ] `notifications/progress` para operaciones largas
- [ ] Integrar progress en: `activateObjects`, `create_transport_copy`, `search_objects` (query amplio)
- [ ] `logging` capability declaration
- [ ] `logging/setLevel` handler
- [ ] `notifications/message` para operaciones de modificación (LOCK/MODIFY/UNLOCK)

**Operaciones con Progress**:

| Operación | Mensaje de progreso |
|-----------|---------------------|
| `activateObjects` | "Activando objeto {n}/{total}: {objectName}" |
| `create_transport_copy` | "Copiando transporte {n}/{total}: {transportId}" |
| `search_objects` (>100 resultados) | "Buscando... {n} resultados encontrados" |
| `get_package_objects` (paquete grande) | "Cargando objetos del paquete... {n}/{total}" |

### Fase 3: Interactive Features (2-3 semanas)

**Objetivo**: Implementar features interactivos y confirmaciones.

| Feature | Prioridad | Esfuerzo | Semana |
|---------|-----------|----------|--------|
| Elicitation | 🟡 Media | Medio | 8-9 |
| Notifications (list_changed) | 🟡 Media | Bajo | 9-10 |

**Entregables**:
- [ ] `elicitation/create` handler
- [ ] Elicitation para confirmaciones:
  - Objeto bloqueado por otro usuario → "¿Continuar de todos modos?"
  - Múltiples coincidencias en búsqueda → "¿Cuál desea seleccionar?"
  - Confirmación de eliminación → "¿Está seguro de eliminar {object}?"
  - Syntax errors detectados → "Hay {n} errores. ¿Continuar?"
- [ ] `notifications/tools/list_changed`
- [ ] `notifications/resources/list_changed`

### Fase 4: Advanced Features (3-4 semanas)

**Objetivo**: Implementar features avanzados para UX premium.

| Feature | Prioridad | Esfuerzo | Semana |
|---------|-----------|----------|--------|
| Resource Subscriptions | 🟡 Media | Medio | 11-12 |
| Completions | 🟢 Baja | Medio | 12-13 |
| Cancellation | 🟢 Baja | Medio | 13-14 |

**Entregables**:
- [ ] `resources/subscribe` y `resources/unsubscribe` handlers
- [ ] `notifications/resources/updated` cuando cambia un recurso subscrito
- [ ] Polling periódico de transportes subscritos (cada 30s)
- [ ] `completion/complete` handler
- [ ] Autocompletado para: className, transportId, packageName, tableName
- [ ] `notifications/cancelled` handler
- [ ] Cancelación graceful para operaciones batch (activación, copia de transportes)

---

## Arquitectura Propuesta

### Estructura de Paquetes

```
src/main/java/com/crystal/mcp/sapserver/
├── config/
│   └── McpCapabilitiesConfiguration.java    # Declaración de capabilities
├── resource/                                 # 🆕 NUEVO
│   ├── ResourceRegistry.java                # Registro de resources
│   ├── ClassResources.java                  # Resources de clases
│   ├── TransportResources.java              # Resources de transportes
│   └── PackageResources.java                # Resources de paquetes
├── prompt/                                   # 🆕 NUEVO
│   ├── PromptRegistry.java                  # Registro de prompts
│   └── SapPrompts.java                      # Definiciones de prompts
├── notification/                             # 🆕 NUEVO
│   ├── NotificationService.java             # Servicio de notificaciones
│   └── ProgressReporter.java                # Helper para progress
├── elicitation/                              # 🆕 NUEVO
│   └── ElicitationService.java              # Servicio de elicitation
├── completion/                               # 🆕 NUEVO
│   └── CompletionService.java               # Servicio de autocompletado
├── service/
│   └── ... (existentes)
└── tool/
    └── ... (existentes)
```

### Capabilities Declaration

```java
@Configuration
public class McpCapabilitiesConfiguration {

    @Bean
    public ServerCapabilities serverCapabilities() {
        return ServerCapabilities.builder()
            // Tools (ya implementado)
            .tools(ToolsCapability.builder()
                .listChanged(true)
                .build())

            // Resources (nuevo)
            .resources(ResourcesCapability.builder()
                .listChanged(true)
                .subscribe(true)
                .build())

            // Prompts (nuevo)
            .prompts(PromptsCapability.builder()
                .listChanged(true)
                .build())

            // Logging (nuevo)
            .logging(LoggingCapability.builder().build())

            // Completions (nuevo)
            .completions(CompletionsCapability.builder().build())

            .build();
    }
}
```

---

## Métricas de Éxito

| Métrica | Actual | Fase 1 | Fase 2 | Fase 3 | Fase 4 |
|---------|--------|--------|--------|--------|--------|
| Cobertura MCP | 30% | 55% | 65% | 75% | 85% |
| Features implementados | 1/9 | 3/9 | 5/9 | 7/9 | 9/9 |
| Prompts disponibles | 0 | 8 | 8 | 8 | 8+ |
| Resource templates | 0 | 10 | 10 | 10 | 10+ |
| UX Score (subjetivo) | 6/10 | 7.5/10 | 8/10 | 8.5/10 | 9/10 |

### Cronograma Resumen

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TIMELINE DE IMPLEMENTACIÓN                       │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Semana:  1    2    3    4    5    6    7    8    9   10   11   12  13 │
│           │    │    │    │    │    │    │    │    │    │    │    │    │
│  FASE 1   ├────┴────┼────┴────┴────┤                                   │
│  Prompts  ██████████│              │                                   │
│  Resources          │██████████████│                                   │
│                     │              │                                   │
│  FASE 2             │              ├────┴────┤                         │
│  Progress           │              │█████████│                         │
│  Logging            │              │█████████│                         │
│                     │              │         │                         │
│  FASE 3             │              │         ├────┴────┴────┤          │
│  Elicitation        │              │         │██████████████│          │
│  Notifications      │              │         │██████████████│          │
│                     │              │         │              │          │
│  FASE 4             │              │         │              ├────┴────┤│
│  Subscriptions      │              │         │              │█████████││
│  Completions        │              │         │              │█████████││
│  Cancellation       │              │         │              │█████████││
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘

Duración total estimada: 13-14 semanas
```

---

## Referencias

- [MCP Specification](https://modelcontextprotocol.io/specification)
- [Spring AI MCP SDK](https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2)
- [MCP Python SDK (referencia)](https://github.com/modelcontextprotocol/python-sdk)
- [MCP Servers Examples](https://github.com/modelcontextprotocol/servers)

---

**Última actualización**: 2025-11-25
**Autor**: Claude + Developer
**Estado**: Propuesta para revisión

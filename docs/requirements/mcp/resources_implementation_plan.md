# Plan de Implementación: MCP Resources

**Proyecto**: giralmcp (SAP ABAP MCP Server)
**Fecha**: 2025-12-08
**Feature Branch**: `feature/mcp-resources-implementation`
**Estado**: Fase 1 y 2 Completadas - Testing en progreso

---

## Resumen Ejecutivo

Este documento detalla la implementación de **MCP Resources** en el servidor SAP ABAP MCP. Los Resources son URIs de solo lectura que exponen datos de SAP de forma estructurada, permitiendo al LLM obtener contexto específico sin ejecutar acciones.

### Diferencia Clave: Resources vs Tools

| Aspecto | Tools (actual) | Resources (nuevo) |
|---------|---------------|-------------------|
| **Propósito** | Ejecutar acciones | Exponer datos para lectura |
| **Side Effects** | Sí (modificaciones) | No (solo lectura) |
| **Caching** | No recomendado | Cliente puede cachear |
| **Descubrimiento** | `tools/list` | `resources/list` + `resources/templates/list` |
| **Ejemplo** | `get_class_source(ZCL_TEST)` | `sap://class/ZCL_TEST/definition` |

---

## Investigación: Spring AI MCP SDK

### Clases Clave Identificadas

Basado en el repositorio `spring-projects-experimental/spring-ai-mcp`:

#### 1. McpSchema.java - Estructuras de Datos

```
Record Resource:
- uri: String (URI del recurso)
- name: String (nombre descriptivo)
- description: String (opcional)
- mimeType: String (opcional, ej: "application/json")
- annotations: Annotations (opcional)

Record ResourceTemplate:
- uriTemplate: String (patrón RFC 6570, ej: "sap://class/{name}/methods")
- name: String
- description: String
- mimeType: String
- annotations: Annotations

Record ReadResourceRequest:
- uri: String (URI específica a leer)

Record ReadResourceResult:
- contents: List<ResourceContents>

Interface ResourceContents:
- TextResourceContents: Para contenido textual (text, mimeType)
- BlobResourceContents: Para contenido binario (blob base64)
```

#### 2. McpServerFeatures.java - Registros de Resources

```
Record SyncResourceRegistration:
- resource: McpSchema.Resource
- readHandler: Function<ReadResourceRequest, ReadResourceResult>

Record AsyncResourceRegistration:
- resource: McpSchema.Resource
- readHandler: Function<ReadResourceRequest, Mono<ReadResourceResult>>

Campo resourceTemplates:
- Lista de ResourceTemplate para URIs parametrizadas
```

#### 3. Handlers MCP

```
resources/list       → Lista todos los resources estáticos disponibles
resources/templates/list → Lista templates de URI (RFC 6570)
resources/read       → Lee contenido de un resource específico por URI
resources/subscribe  → Subscribe a cambios (opcional, fase posterior)
```

---

## Arquitectura Propuesta

### Estructura de Paquetes

```
src/main/java/com/crystal/mcp/sapserver/
├── resource/                              # 🆕 NUEVO PAQUETE
│   ├── ResourceConfiguration.java        # Configuración y registro de resources
│   ├── SapResourceHandler.java           # Handler central para resources/read
│   │
│   ├── handler/                           # Handlers por categoría
│   │   ├── ClassResourceHandler.java     # sap://class/{name}/*
│   │   ├── TransportResourceHandler.java # sap://transport/{id}/*
│   │   ├── PackageResourceHandler.java   # sap://package/{name}/*
│   │   ├── TableResourceHandler.java     # sap://table/{name}/*
│   │   └── UserResourceHandler.java      # sap://user/{id}/*
│   │
│   └── model/                             # DTOs específicos de resources
│       ├── ClassMethodsResource.java     # Lista de métodos
│       ├── ClassAttributesResource.java  # Lista de atributos
│       └── TransportObjectsResource.java # Objetos en transporte
│
├── config/
│   └── McpCapabilitiesConfiguration.java # 🆕 Agregar resources capability
│
├── service/                               # Existente (reutilizar)
│   ├── ClassService.java
│   ├── TransportService.java
│   └── NavigationService.java
│
└── tool/                                  # Existente
    └── ...
```

### Flujo de Datos

```
┌─────────────────────────────────────────────────────────────────┐
│                     FLUJO DE RESOURCES                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Cliente MCP                                                  │
│     └─► resources/templates/list                                 │
│         └─► Retorna: ["sap://class/{name}/methods", ...]         │
│                                                                  │
│  2. Cliente MCP                                                  │
│     └─► resources/read { uri: "sap://class/ZCL_INVOICE/methods" }│
│         │                                                        │
│         ▼                                                        │
│  3. SapResourceHandler                                           │
│     └─► Parsea URI: scheme=sap, category=class, id=ZCL_INVOICE   │
│         └─► Delega a ClassResourceHandler.handleMethods()        │
│             │                                                    │
│             ▼                                                    │
│  4. ClassResourceHandler                                         │
│     └─► Llama ClassService.getClassComponents("ZCL_INVOICE")     │
│         └─► Transforma a ResourceContents (JSON)                 │
│             │                                                    │
│             ▼                                                    │
│  5. Respuesta                                                    │
│     └─► { contents: [{ uri, mimeType: "application/json",        │
│                        text: "[{name:'METHOD1', ...}]" }] }      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Resource Templates a Implementar

### Fase 1: Core Resources (Prioridad Alta)

| # | Template URI | Descripción | Fuente de Datos |
|---|--------------|-------------|-----------------|
| 1 | `sap://class/{name}/definition` | Código de definición de clase | `ClassService.getClassSource(name, "active", "main")` |
| 2 | `sap://class/{name}/implementation` | Código de implementación | `ClassService.getClassSource(name, "active", "implementation")` |
| 3 | `sap://class/{name}/methods` | Lista de métodos (metadata) | `ObjectService.getObjectStructure()` → filtrar métodos |
| 4 | `sap://class/{name}/attributes` | Lista de atributos | `ObjectService.getObjectStructure()` → filtrar atributos |
| 5 | `sap://transport/{id}/info` | Metadata del transporte | `TransportService.getTransportInfo(id)` |
| 6 | `sap://transport/{id}/objects` | Objetos en transporte | `TransportService.getTransportObjects(id)` |

### Fase 2: Extended Resources (Prioridad Media)

| # | Template URI | Descripción | Fuente de Datos |
|---|--------------|-------------|-----------------|
| 7 | `sap://package/{name}/objects` | Objetos en paquete | `NavigationService.getPackageObjects(name)` |
| 8 | `sap://package/{name}/hierarchy` | Jerarquía de subpaquetes | `PackageHierarchyService.getPackageHierarchy(name)` |
| 9 | `sap://table/{name}/fields` | Campos de tabla DDIC | `ClassService.getDdicSource(name)` |
| 10 | `sap://user/{id}/transports` | Transportes del usuario | `TransportService.listUserTransports(id)` |

---

## Plan de Implementación

### Milestone 1: Infraestructura Base (Semana 1)

**Objetivo**: Establecer la infraestructura para soportar MCP Resources.

#### Tareas:

1. **Crear paquete `resource/`**
   - Crear estructura de directorios
   - Definir interfaces base

2. **Implementar `ResourceConfiguration.java`**
   - Registrar Resource Templates con Spring AI MCP
   - Declarar capability de resources

3. **Implementar `SapResourceHandler.java`**
   - Parser de URIs (`sap://category/id/subtype`)
   - Router a handlers específicos por categoría
   - Manejo de errores y URIs inválidas

4. **Crear modelos DTO para resources**
   - `ClassMethodInfo`: name, visibility, parameters, returnType
   - `ClassAttributeInfo`: name, type, visibility
   - Reutilizar modelos existentes donde sea posible

#### Entregables:
- [x] Paquete `resource/` creado
- [x] `ClassResourceProvider.java` con 4 resources
- [x] `TransportResourceProvider.java` con 2 resources
- [x] `PackageResourceProvider.java` con 2 resources
- [x] `TableResourceProvider.java` con 1 resource
- [ ] Tests de integración

### Milestone 2: Class Resources (Semana 2)

**Objetivo**: Implementar los 4 Resource Templates de clases.

#### Tareas:

1. **Implementar `ClassResourceHandler.java`**
   - `handleDefinition(className)` → Código de definición
   - `handleImplementation(className)` → Código de implementación
   - `handleMethods(className)` → Lista de métodos (JSON)
   - `handleAttributes(className)` → Lista de atributos (JSON)

2. **Integrar con ClassService existente**
   - Reutilizar `getClassSource()` para definition/implementation
   - Agregar método para extraer métodos/atributos de estructura

3. **Formato de respuesta**
   - definition/implementation: `text/plain` (código ABAP)
   - methods/attributes: `application/json` (metadata estructurada)

#### Entregables:
- [x] `ClassResourceProvider.java` completo (usando @McpResource)
- [x] 4 Resource Templates funcionales
- [ ] Tests de integración con SAP

### Milestone 3: Transport Resources (Semana 2-3)

**Objetivo**: Implementar los 2 Resource Templates de transportes.

#### Tareas:

1. **Implementar `TransportResourceHandler.java`**
   - `handleInfo(transportId)` → Metadata del transporte
   - `handleObjects(transportId)` → Lista de objetos

2. **Formato de respuesta**
   - info: `application/json` con owner, status, description, target
   - objects: `application/json` con lista de objetos [{pgmid, object, objName}]

#### Entregables:
- [x] `TransportResourceProvider.java` completo
- [x] 2 Resource Templates funcionales
- [ ] Tests de integración

### Milestone 4: Extended Resources (Semana 3)

**Objetivo**: Implementar los 4 Resource Templates restantes.

#### Tareas:

1. **Implementar `PackageResourceHandler.java`**
   - `handleObjects(packageName)`
   - `handleHierarchy(packageName)`

2. **Implementar `TableResourceHandler.java`**
   - `handleFields(tableName)`

3. **Implementar `UserResourceHandler.java`**
   - `handleTransports(userId)`

#### Entregables:
- [x] `PackageResourceProvider.java` (objects, hierarchy)
- [x] `TableResourceProvider.java` (fields)
- [ ] `UserResourceProvider.java` (transports) - Pendiente
- [x] 3/4 Resource Templates adicionales funcionales
- [ ] Suite completa de tests

### Milestone 5: Documentación y Refinamiento (Semana 4)

**Objetivo**: Documentar y refinar la implementación.

#### Tareas:

1. **Actualizar CLAUDE.md**
   - Documentar Resource Templates disponibles
   - Agregar ejemplos de uso

2. **Actualizar README_JAVA.md**
   - Sección de MCP Resources
   - Guía de extensión para nuevos resources

3. **Performance testing**
   - Medir latencia de resources vs tools equivalentes
   - Optimizar si es necesario

---

## Consideraciones Técnicas

### Integración con Spring AI MCP

Spring AI MCP SDK maneja automáticamente:
- Serialización JSON de respuestas
- Routing de requests MCP
- Gestión del lifecycle de recursos

**Patrón de registro** (basado en investigación del SDK):

```
// Pseudocódigo - estructura conceptual
@Configuration
public class ResourceConfiguration {

    // Registrar templates de URI
    List<ResourceTemplate> templates = [
        ResourceTemplate("sap://class/{name}/methods", "Class Methods", ...),
        ResourceTemplate("sap://transport/{id}/info", "Transport Info", ...),
        ...
    ];

    // Handler para resources/read
    Function<ReadResourceRequest, ReadResourceResult> readHandler = (request) -> {
        String uri = request.uri();
        // Parse URI y delegar a handler específico
        return sapResourceHandler.handle(uri);
    };
}
```

### Manejo de URIs

Esquema: `sap://{category}/{id}/{subtype}`

| Componente | Descripción | Ejemplos |
|------------|-------------|----------|
| `sap://` | Esquema fijo | - |
| `category` | Tipo de objeto SAP | class, transport, package, table, user |
| `id` | Identificador del objeto | ZCL_INVOICE, DEVK900123, ZFINANCE |
| `subtype` | Aspecto específico | definition, methods, objects, fields |

### Reutilización de Servicios Existentes

Los Resources reutilizan los servicios existentes:

| Resource | Servicio | Método |
|----------|----------|--------|
| class/*/definition | ClassService | getClassSource() |
| class/*/methods | ObjectService | getObjectStructure() |
| transport/*/info | TransportService | getTransportInfo() |
| transport/*/objects | TransportService | getTransportObjects() |
| package/*/objects | NavigationService | getPackageObjects() |

---

## Beneficios Esperados

### Reducción de Tokens

| Consulta | Sin Resources (Tool) | Con Resources | Ahorro |
|----------|----------------------|---------------|--------|
| "¿Qué métodos tiene ZCL_INVOICE?" | ~5000 tokens (código completo) | ~500 tokens (lista JSON) | 90% |
| "¿Qué objetos tiene DEVK900123?" | ~2000 tokens | ~300 tokens | 85% |
| "¿Qué campos tiene tabla MARA?" | ~1500 tokens | ~400 tokens | 73% |

### Mejor UX

- **Respuestas más rápidas**: Datos estructurados vs parsing de código
- **Menor uso de contexto**: LLM puede trabajar con más información
- **Cacheabilidad**: Cliente puede cachear resources frecuentes

---

## Siguiente Paso Inmediato

Comenzar con **Milestone 1: Infraestructura Base**:

1. Crear el paquete `src/main/java/com/crystal/mcp/sapserver/resource/`
2. Implementar `ResourceConfiguration.java` con registro de templates
3. Implementar `SapResourceHandler.java` con parsing de URIs

---

## Referencias

- [MCP Specification - Resources](https://spec.modelcontextprotocol.io/specification/server/resources/)
- [Spring AI MCP SDK](https://github.com/spring-projects-experimental/spring-ai-mcp)
- [RFC 6570 - URI Template](https://datatracker.ietf.org/doc/html/rfc6570)
- [Plan General MCP Features](./mcp_features_implementation_plan.md)

---

**Última actualización**: 2025-12-08
**Autor**: Claude + Developer

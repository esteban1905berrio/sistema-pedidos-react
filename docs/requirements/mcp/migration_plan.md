# Plan de Migración: Python → Java MCP Server

**Fecha creación**: 2025-11-08
**Estado**: En progreso - Fase 0
**Proyecto**: SAP ABAP MCP Server Migration

---

## Resumen Ejecutivo

Este plan documenta la migración progresiva de **59 herramientas MCP** desde Python (PyRFC) hacia Java (SAP JCo), transformando el proyecto de un POC a un servidor MCP de producción siguiendo las mejores prácticas identificadas en la investigación `docs/research/abap_mcp_tools_strategy_2025.md`.

**Objetivos principales**:
- Java se convierte en el proyecto principal
- Python se archiva como legacy/referencia pero permanece funcional
- Migración progresiva y controlada
- Aplicar optimizaciones: Progressive Discovery, Workflow-based Tools
- Preparar para producción con Docker multi-platform

**Duración estimada**: 9-28 semanas (dependiendo de fases opcionales)

---

## Estado Actual del Proyecto

### Proyecto Python (Legacy - Funcional)

**Ubicación actual**: `app/`
**Ubicación futura**: `python-legacy/`

**Métricas**:
- ✅ **59 herramientas MCP** implementadas en 10 categorías
- ✅ **17 servicios** funcionales
- ✅ **76+ archivos de tests** (67% coverage)
- ✅ **2 servidores MCP** configurados: CRY (sistema Crystal), GDC (sistema GDC)

**Categorías de herramientas**:
1. Repository & Source (9 tools)
2. Data Dictionary (4 tools)
3. Query & Preview (2 tools)
4. Transport Management (14 tools)
5. Object Modification (3 tools)
6. Activation (3 tools)
7. Code Quality (4 tools)
8. Lifecycle (4 tools)
9. Where-Used Analysis (2 tools)
10. CDS Views & RAP (12 tools)
11. Enhancements (3 tools)

**Tecnologías**:
- PyRFC para conectividad SAP
- FastMCP para servidor MCP
- SAP NetWeaver RFC SDK (nwrfcsdk)
- Arquitectura: RfcAdapter → SADT_REST_RFC_ENDPOINT → ADT APIs

### Proyecto Java (Principal - POC)

**Ubicación actual**: `java-mcp-server/`
**Ubicación futura**: `./` (raíz del proyecto)

**Métricas**:
- ✅ **1 herramienta MCP** implementada: `get_class_source`
- ✅ **6 clases Java** funcionales
- ✅ Infraestructura base lista (Spring Boot + SAP JCo)
- 🔄 **15% completado** respecto a Python

**Tecnologías**:
- SAP JCo 3.1.x para conectividad SAP
- Spring Boot 3.4.0 + Spring AI MCP SDK 1.1.0-M4
- Maven 3.9+
- Java 21+
- Arquitectura: RfcAdapter → JCo → SADT_REST_RFC_ENDPOINT → ADT APIs

**Estado de herramientas**:
```
[████░░░░░░░░░░░░░░░░] 1/59 tools (1.7%)
```

---

## Estrategia de Migración

### Principios Guía

1. **Python permanece funcional**: No se elimina, se archiva
2. **Migración progresiva**: Por categorías, priorizando herramientas core
3. **Java como referencia futura**: Nueva funcionalidad solo en Java
4. **Optimizaciones integradas**: Aplicar Progressive Discovery y Workflow-based desde el inicio
5. **Testing obligatorio**: 80%+ coverage para cada herramienta migrada

### Reorganización del Proyecto

**Estructura actual**:
```
brootpersonalagent/
├── app/                    # Python MCP server (59 tools)
├── java-mcp-server/        # Java MCP server (1 tool)
├── PyRFC/                  # Python SAP RFC bindings
├── docs/
└── resources/
```

**Estructura futura** (después de Fase 0):
```
brootpersonalagent/
├── src/                          # Java principal (migrado desde java-mcp-server/)
│   └── main/java/com/crystal/mcp/sapserver/
├── lib/                          # SAP JCo SDK (migrado desde java-mcp-server/)
├── python-legacy/                # Python archivado pero funcional
│   ├── app/                      # 59 MCP tools originales
│   ├── PyRFC/                    # SAP RFC bindings
│   ├── .venv/                    # Virtual environment
│   ├── pyproject.toml
│   ├── uv.lock
│   └── PYTHON_LEGACY.md          # Documentación de uso
├── docs/                         # Sin cambios
│   ├── requirements/
│   │   └── mcp/
│   │       └── migration_plan.md # ESTE ARCHIVO
│   └── architecture/
├── resources/                    # Sin cambios (carpeta separada)
├── logs/
│   ├── python/                   # Logs del servidor Python
│   └── java/                     # Logs del servidor Java
├── pom.xml                       # Maven config en raíz
├── README.md                     # Documentación Java principal
├── CLAUDE.md                     # Instrucciones para Claude (Java-first)
└── .mcp.json                     # Configuración actualizada
```

---

## Fases de Migración

### Fase 0: Reorganización del Proyecto ⏳ EN PROGRESO

**Duración**: 1 semana
**Estado**: Iniciada 2025-11-08

**Objetivo**: Establecer Java como proyecto principal y archivar Python como legacy.

#### Tareas

##### 1. Documentación (30 min)
- [x] Crear `docs/requirements/mcp/migration_plan.md` (este documento)

##### 2. Preparar estructura python-legacy (1 hora)
- [ ] Crear directorio `python-legacy/`
- [ ] Crear `python-legacy/PYTHON_LEGACY.md` con instrucciones de uso
- [ ] Preparar estructura de subdirectorios

##### 3. Mover archivos Python con git (2 horas)
- [ ] `git mv app/ python-legacy/app/`
- [ ] `git mv PyRFC/ python-legacy/PyRFC/`
- [ ] `git mv pyproject.toml python-legacy/`
- [ ] `git mv uv.lock python-legacy/`
- [ ] `git mv .venv/ python-legacy/.venv/` (o recrear después)

##### 4. Promover Java a raíz (1 hora)
- [ ] `git mv java-mcp-server/src/ ./src/`
- [ ] `git mv java-mcp-server/lib/ ./lib/`
- [ ] `git mv java-mcp-server/pom.xml ./pom.xml`
- [ ] `git mv java-mcp-server/README.md ./README_JAVA.md`
- [ ] Eliminar directorio vacío `java-mcp-server/`

##### 5. Recrear entorno Python (30 min)
- [ ] `cd python-legacy`
- [ ] `python3 -m venv .venv`
- [ ] `.venv/bin/pip install -e PyRFC/`
- [ ] `uv sync`
- [ ] Verificar imports: `from app.core.config import SAPConfig`

##### 6. Actualizar .mcp.json (30 min)
- [ ] Actualizar ruta CRY: `"cwd": ".../brootpersonalagent/python-legacy"`
- [ ] Actualizar ruta GDC: `"cwd": ".../brootpersonalagent/python-legacy"`
- [ ] Actualizar ruta giralmcp: `"-f", ".../brootpersonalagent/pom.xml"`
- [ ] Actualizar comando giralmcp si es necesario

##### 7. Reorganizar logs (15 min)
- [ ] Crear `logs/python/`
- [ ] Crear `logs/java/`
- [ ] Mover logs existentes a subdirectorios apropiados
- [ ] Actualizar `.gitignore` para `logs/**/*.log`

##### 8. Actualizar documentación raíz (1 hora)
- [ ] Actualizar `README.md` con enfoque Java-first
- [ ] Actualizar `CLAUDE.md` con nueva estructura
- [ ] Agregar sección "Python Legacy" en ambos
- [ ] Documentar comandos para ambos proyectos

##### 9. Validación completa (1 hora)
- [ ] ✅ Maven build desde raíz: `mvn clean package`
- [ ] ✅ Java tests: `mvn test`
- [ ] ✅ giralmcp MCP server inicia sin errores
- [ ] ✅ Python tests: `cd python-legacy && .venv/bin/python -m pytest app/tests/ -v`
- [ ] ✅ CRY MCP server inicia sin errores
- [ ] ✅ GDC MCP server inicia sin errores
- [ ] ✅ Logs se escriben en directorios correctos

##### 10. Git commit (15 min)
- [ ] Crear rama: `git checkout -b feature/project-reorganization`
- [ ] Commit: `git commit -m "Fase 0: Reorganizar proyecto - Java principal, Python legacy"`
- [ ] Push: `git push origin feature/project-reorganization`

#### Entregables Fase 0

- ✅ Estructura de proyecto reorganizada
- ✅ Python funcional en `python-legacy/`
- ✅ Java en raíz como proyecto principal
- ✅ 3 servidores MCP funcionales (CRY, GDC, giralmcp)
- ✅ Documentación actualizada
- ✅ Logs organizados por proyecto
- ✅ Commit en feature branch

---

### Fase 1: Paridad Funcional Core (Categorías Básicas)

**Duración**: 6 semanas
**Estado**: Pendiente
**Prioridad**: Alta

**Objetivo**: Migrar las 16 herramientas más críticas para alcanzar paridad funcional básica.

#### Categorías a Migrar

##### 1.1 Repository & Source (9 tools) - Semanas 2-3

**Herramientas Python a migrar**:
- `get_class_source` ✅ (YA EXISTE en Java)
- `get_class_structure`
- `get_object_source`
- `get_class_includes`
- `get_class_components`
- `get_object_structure`
- `search_objects`
- `get_program_source`
- `get_include_source`

**Servicios Java a crear**:
- ✅ `ClassService.java` (parcialmente existe)
- `SearchService.java`
- `ProgramService.java`
- `ObjectService.java`

**Patrón de migración**:
```java
// 1. Servicio Java (ejemplo: ClassService.java)
@Service
public class ClassService extends BaseService {
    public ClassSourceResult getClassSource(String className, String version, String includeType) {
        // Usar RfcAdapter para llamar ADT API
        Map<String, Object> response = rfcAdapter.request(
            "/sap/bc/adt/oo/classes/" + className + "/source/" + includeType,
            "GET",
            Map.of("version", version),
            ""
        );
        // Parsear respuesta y retornar DTO
        return new ClassSourceResult(response);
    }
}

// 2. MCP Tool (ejemplo: ClassTools.java)
@Component
public class ClassTools {
    @Tool(description = "Get ABAP class source code")
    public String getClassSource(
        @Param(description = "Class name") String className,
        @Param(description = "Version: active or inactive") String version,
        @Param(description = "Include type: main, implementation, testclasses") String includeType
    ) {
        return classService.getClassSource(className, version, includeType).toJson();
    }
}

// 3. Test (ejemplo: ClassServiceTest.java)
@SpringBootTest
class ClassServiceTest {
    @Test
    void testGetClassSource() {
        // Given
        String className = "CL_ABAP_CHAR_UTILITIES";

        // When
        ClassSourceResult result = classService.getClassSource(className, "active", "main");

        // Then
        assertNotNull(result.getSource());
        assertTrue(result.getSource().contains("CLASS"));
    }
}
```

##### 1.2 Data Dictionary (4 tools) - Semana 4

**Herramientas Python a migrar**:
- `get_ddic_element`
- `ddic_repository_access`
- `get_annotation_definitions`
- `package_search_help`

**Servicios Java a crear**:
- `DdicService.java`
- `PackageService.java`

##### 1.3 Transport Management (3 tools core) - Semana 5

**Herramientas Python a migrar** (primeras 3 críticas):
- `list_user_transports`
- `get_transport_objects`
- `transport_info`

**Servicio Java a crear**:
- `TransportService.java`

**Nota**: Las 11 herramientas restantes de transporte se migran en Fase 1.4

#### Tareas Técnicas Transversales

##### Infraestructura (Semana 2)
- [ ] Implementar `BaseService.java` para herencia común
- [ ] Implementar retry logic con `RetryHandler.java`
- [ ] Implementar circuit breaker pattern
- [ ] Configurar logging estructurado (SLF4J + Logback)
- [ ] Actualizar `RfcAdapter.java` con soporte stateful/stateless

##### Testing (continuo)
- [ ] Configurar JUnit 5 + Mockito
- [ ] Crear `SapTestConfig.java` para integration tests
- [ ] Alcanzar 80%+ coverage por cada herramienta
- [ ] Crear suite de smoke tests

##### Documentación (continuo)
- [ ] JavaDoc completo para todos los servicios
- [ ] Actualizar README.md con herramientas migradas
- [ ] Documentar patrones de migración en `docs/architecture/`

#### Entregables Fase 1

- ✅ 16 herramientas MCP migradas a Java
- ✅ 4 servicios Java nuevos (Class, Search, Program, Ddic, Transport parcial)
- ✅ 80%+ test coverage
- ✅ Infraestructura de retry y circuit breaker
- ✅ Documentación técnica completa
- ✅ CI/CD pipeline básico (opcional)

**Progreso esperado al final de Fase 1**:
```
[████████░░░░░░░░░░░░] 16/59 tools (27%)
```

---

### Fase 2: Paridad Funcional Completa (Resto de Categorías)

**Duración**: 11 semanas (Semanas 8-18)
**Estado**: Pendiente
**Prioridad**: Media

**Objetivo**: Migrar las 43 herramientas restantes para alcanzar 100% paridad con Python.

#### Categorías a Migrar

##### 2.1 Transport Management Completo (11 tools) - Semanas 8-10

**Herramientas Python a migrar**:
- `create_transport`
- `get_transport_tasks`
- `add_object_to_transport`
- `release_transport`
- `get_transport_config`
- `delete_transport`
- `set_transport_owner`
- `add_transport_user`
- `get_system_users`
- `get_transport_reference`

**Actualización**:
- Extender `TransportService.java`

##### 2.2 Object Modification (3 tools) - Semana 11

**Herramientas Python a migrar**:
- `lock`
- `unlock`
- `set_object_source`

**Servicio Java a crear**:
- `ModificationService.java`

**Consideraciones**:
- Implementar manejo de statefulness en RfcAdapter
- Gestión de lock handles
- Validación de permisos

##### 2.3 Activation (3 tools) - Semana 12

**Herramientas Python a migrar**:
- `activate`
- `activate_objects`
- `get_inactive_objects`

**Servicio Java a crear**:
- `ActivationService.java`

##### 2.4 Code Quality (4 tools) - Semana 13

**Herramientas Python a migrar**:
- `syntax_check`
- `prettyprint`
- `get_prettyprint_settings`
- `set_prettyprint_settings`

**Servicio Java a crear**:
- `CodeQualityService.java`

##### 2.5 Lifecycle (4 tools) - Semana 14

**Herramientas Python a migrar**:
- `create_function_group`
- `create_function_module`
- `create_class`
- `create_interface`
- `delete_object`
- `validate_object_name`
- `run_unit_tests`

**Servicio Java a crear**:
- `LifecycleService.java`

##### 2.6 Query & Preview (2 tools) - Semana 15

**Herramientas Python a migrar**:
- `get_table_contents`
- `run_query`

**Servicio Java a crear**:
- `QueryService.java`

##### 2.7 Where-Used Analysis (2 tools) - Semana 16

**Herramientas Python a migrar**:
- `get_usage_references`
- `get_usage_snippets`

**Servicio Java a crear**:
- `WhereUsedService.java`

##### 2.8 CDS Views & RAP (12 tools) - Semanas 17-18

**Herramientas Python a migrar**:
- `get_cds_view_metadata`
- `get_cds_view_source`
- `search_cds_views_by_sqlview`
- `get_cds_view_properties`
- `get_service_binding`
- `get_service_definition_metadata`
- `get_service_definition_source`
- `get_odata_service_info`
- `get_metadata_extension`
- `get_ddlx_parser_info`
- `get_behavior_definition`
- `explore_rap_object`

**Servicios Java a crear**:
- `CdsService.java`
- `RapService.java`

##### 2.9 Enhancements (3 tools) - Semana 18

**Herramientas Python a migrar**:
- `search_enhancements`
- `get_enhancement_metadata`
- `get_enhancement_source`

**Servicio Java a crear**:
- `EnhancementService.java`

#### Entregables Fase 2

- ✅ 43 herramientas MCP migradas (total: 59/59)
- ✅ 13 servicios Java completos
- ✅ 100% paridad funcional con Python
- ✅ Test coverage 80%+ global
- ✅ Documentación completa de APIs

**Progreso esperado al final de Fase 2**:
```
[████████████████████] 59/59 tools (100%)
```

---

### Fase 3: Optimizaciones - Progressive Discovery

**Duración**: 2-3 semanas (Semanas 19-21)
**Estado**: Pendiente
**Prioridad**: Alta (optimización crítica)

**Objetivo**: Implementar Progressive Discovery para reducir uso de tokens de 29,500 → 500 tokens en exploraciones típicas.

**Referencia**: `docs/research/abap_mcp_tools_strategy_2025.md` - Phase 1

#### Estrategia

**Problema actual**:
```
Exploración típica: "¿Dónde se maneja el error XYZ?"
→ get_class_source(ClassA) = 8,000 tokens
→ get_class_source(ClassB) = 7,500 tokens
→ get_class_source(ClassC) = 6,800 tokens
→ get_class_source(ClassD) = 7,200 tokens
TOTAL: 29,500 tokens (solo para explorar)
```

**Solución Progressive Discovery**:
```
→ search_objects("*ERROR*") = 500 tokens (lista de candidatos)
→ get_class_structure(ClassA) = 300 tokens (métodos + atributos)
→ get_class_source(ClassA, method="handleError") = 1,200 tokens (solo método relevante)
TOTAL: 2,000 tokens (93% reducción)
```

#### Herramientas a Implementar

##### 3.1 Lazy Source Retrieval
- [ ] `get_class_structure_extended`: Metadata + signatures sin código
- [ ] `get_method_source`: Código de un solo método
- [ ] `get_attribute_usage`: Dónde se usa un atributo específico

##### 3.2 Smart Search
- [ ] `search_with_context`: Búsqueda + metadata básica
- [ ] `filter_by_type`: Filtrar resultados por tipo de objeto
- [ ] `rank_by_relevance`: Ordenar por relevancia (tamaño, complejidad)

##### 3.3 Incremental Loading
- [ ] `get_class_outline`: Solo estructura sin implementación
- [ ] `expand_method`: Expandir método específico bajo demanda
- [ ] `get_dependencies`: Grafo de dependencias sin código

#### Implementación Java

**Nuevo servicio**: `ProgressiveDiscoveryService.java`

```java
@Service
public class ProgressiveDiscoveryService extends BaseService {

    // Level 1: Búsqueda + metadata ligera
    public List<ObjectOutline> searchWithContext(String pattern) {
        // Combina search_objects + get_object_structure para cada resultado
        // Retorna solo: nombre, tipo, descripción, número de métodos/atributos
    }

    // Level 2: Estructura sin código
    public ClassStructure getClassOutline(String className) {
        // Similar a get_class_structure pero sin source code
        // Solo métodos/atributos con signatures
    }

    // Level 3: Código específico
    public String getMethodSource(String className, String methodName) {
        // Extrae solo el código de un método específico
        // Usa ADT API con parámetro de fragmentación
    }
}
```

#### Entregables Fase 3

- ✅ 9 nuevas herramientas de Progressive Discovery
- ✅ `ProgressiveDiscoveryService.java`
- ✅ Reducción 85%+ en uso de tokens para exploraciones
- ✅ Documentación de patrones de uso
- ✅ Ejemplos de workflows optimizados

**Total herramientas al final de Fase 3**: 68 tools (59 + 9 nuevas)

---

### Fase 4: Optimizaciones - Workflow-Based Tools

**Duración**: 3-4 semanas (Semanas 22-25)
**Estado**: Pendiente
**Prioridad**: Media-Alta

**Objetivo**: Reducir roundtrips de tool calls en 80% mediante workflows atómicos.

**Referencia**: `docs/research/abap_mcp_tools_strategy_2025.md` - Phase 2

#### Estrategia

**Problema actual**:
```
Workflow de modificación de clase:
1. lock(classUri) → call 1
2. get_class_source(className) → call 2
3. modify_source(classUri, newSource) → call 3 (verificar sintaxis)
4. unlock(classUri) → call 4
5. activate(className) → call 5
TOTAL: 5 roundtrips (varios segundos)
```

**Solución Workflow-Based**:
```
→ modify_and_activate_class(className, modificationFn) → call 1
TOTAL: 1 roundtrip (sub-segundo)
```

#### Workflows a Implementar

##### 4.1 Modification Workflows (YA IMPLEMENTADOS EN PYTHON)

En Python ya existen:
- `modify_function_module`
- `modify_class`
- `modify_program`
- `modify_include`

**Migrar a Java**:
- [ ] `ModificationWorkflow.modifyFunctionModule()`
- [ ] `ModificationWorkflow.modifyClass()`
- [ ] `ModificationWorkflow.modifyProgram()`
- [ ] `ModificationWorkflow.modifyInclude()`

##### 4.2 Creation Workflows (NUEVOS)

- [ ] `create_and_implement_class`: Crear + implementar + activar
- [ ] `create_function_group_with_modules`: Crear FG + FMs en una sola operación
- [ ] `clone_and_modify_object`: Clonar objeto existente con modificaciones

##### 4.3 Analysis Workflows (NUEVOS)

- [ ] `analyze_dependencies`: Análisis completo de dependencias
- [ ] `impact_analysis`: Qué se afecta si modifico X
- [ ] `usage_report`: Reporte completo de where-used

##### 4.4 Transport Workflows (NUEVOS)

- [ ] `create_transport_and_add_objects`: Crear TR + agregar lista de objetos
- [ ] `validate_and_release_transport`: Validar + liberar con checks automáticos

#### Implementación Java

**Nuevo paquete**: `com.crystal.mcp.sapserver.workflows`

```java
@Component
public class ModificationWorkflow {

    @Tool(description = "Complete workflow: lock → modify → unlock → activate")
    public WorkflowResult modifyAndActivateClass(
        @Param String className,
        @Param String newSource,
        @Param String transport,
        @Param boolean validateSyntax
    ) {
        WorkflowResult result = new WorkflowResult();
        String lockHandle = null;

        try {
            // 1. Lock
            result.addStep("lock", "in_progress");
            lockHandle = modificationService.lock(classUri);
            result.addStep("lock", "success");

            // 2. Validate syntax (opcional)
            if (validateSyntax) {
                result.addStep("syntax_check", "in_progress");
                List<Error> errors = codeQualityService.syntaxCheck(classUri, newSource);
                if (!errors.isEmpty()) {
                    result.addStep("syntax_check", "failed", errors);
                    return result;
                }
                result.addStep("syntax_check", "success");
            }

            // 3. Modify
            result.addStep("modify", "in_progress");
            modificationService.setObjectSource(classUri, newSource, lockHandle, transport);
            result.addStep("modify", "success");

            // 4. Unlock
            result.addStep("unlock", "in_progress");
            modificationService.unlock(classUri, lockHandle);
            lockHandle = null; // Released
            result.addStep("unlock", "success");

            // 5. Activate
            result.addStep("activate", "in_progress");
            activationService.activate(className, classUri);
            result.addStep("activate", "success");

            result.setOverallStatus("success");

        } catch (Exception e) {
            result.setOverallStatus("failed");
            result.setError(e.getMessage());

            // Cleanup: intentar liberar lock si existe
            if (lockHandle != null) {
                try {
                    modificationService.unlock(classUri, lockHandle);
                } catch (Exception unlockError) {
                    // Log pero no fallar
                }
            }
        }

        return result;
    }
}
```

#### Entregables Fase 4

- ✅ 12 workflows atómicos implementados
- ✅ Reducción 80% en roundtrips
- ✅ Error handling robusto con rollback automático
- ✅ Documentación de workflows disponibles
- ✅ Tests de integración para workflows completos

**Total herramientas al final de Fase 4**: 80 tools (68 + 12 workflows)

---

### Fase 5: Productización (OPCIONAL)

**Duración**: 3-5 semanas
**Estado**: Pendiente
**Prioridad**: Baja (solo si se requiere deployment)

**Objetivo**: Preparar para producción con Docker, CI/CD, y monitoreo.

#### Tareas

##### 5.1 Containerización (1 semana)
- [ ] Crear `Dockerfile` multi-stage para Java
- [ ] Configurar multi-platform builds (amd64/arm64)
- [ ] Crear `docker-compose.yml` para desarrollo
- [ ] Publicar imagen en registry (Artifact Registry / Docker Hub)

##### 5.2 CI/CD (1 semana)
- [ ] Configurar GitHub Actions / GitLab CI
- [ ] Pipeline: build → test → package → deploy
- [ ] Code quality gates (JaCoCo coverage, SonarQube)
- [ ] Automated deployment a GCP Cloud Run (opcional)

##### 5.3 Monitoreo y Observabilidad (1 semana)
- [ ] Integrar Spring Boot Actuator
- [ ] Métricas: Prometheus + Grafana
- [ ] Logging estructurado con context (correlation IDs)
- [ ] Health checks y readiness probes

##### 5.4 Seguridad (1 semana)
- [ ] Secrets management (Google Secret Manager / Vault)
- [ ] TLS/SSL para comunicación SAP
- [ ] Auditoría de llamadas SAP
- [ ] Rate limiting y throttling

##### 5.5 Documentación Final (1 semana)
- [ ] OpenAPI/Swagger para herramientas MCP
- [ ] Architecture Decision Records (ADRs)
- [ ] Runbooks para operaciones
- [ ] Migration guide final (Python → Java)

#### Entregables Fase 5

- ✅ Imagen Docker multi-platform
- ✅ CI/CD pipeline funcional
- ✅ Monitoreo en producción
- ✅ Documentación operacional completa
- ✅ Seguridad hardened

---

## Riesgos y Mitigaciones

### Riesgos Técnicos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Incompatibilidades SAP JCo vs PyRFC | Media | Alto | Testing exhaustivo con sistemas reales |
| Regresiones en herramientas migradas | Alta | Medio | Suite de regression tests automatizada |
| Performance degradation | Baja | Medio | Benchmarking continuo Python vs Java |
| Problemas con ADT APIs no documentadas | Media | Alto | Usar Python como referencia de comportamiento |

### Riesgos de Proyecto

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Python legacy no mantenido | Baja | Bajo | Documentación clara, tests automatizados |
| Cambios en ADT APIs SAP | Baja | Alto | Monitoreo de releases SAP, versionado de APIs |
| Falta de expertise SAP JCo | Media | Medio | Documentación exhaustiva, knowledge sharing |

---

## Criterios de Éxito

### Fase 0 (Reorganización)
- ✅ 3 servidores MCP funcionales post-reorganización
- ✅ 0 regresiones en herramientas Python
- ✅ Java como proyecto principal en raíz

### Fase 1 (Paridad Core)
- ✅ 16 herramientas críticas migradas
- ✅ 80%+ test coverage
- ✅ Performance similar a Python (±10%)

### Fase 2 (Paridad Completa)
- ✅ 59/59 herramientas migradas
- ✅ 100% feature parity con Python
- ✅ 0 regresiones

### Fase 3 (Progressive Discovery)
- ✅ Reducción 85%+ en tokens para exploraciones
- ✅ Workflows de descubrimiento documentados

### Fase 4 (Workflows)
- ✅ Reducción 80% en roundtrips
- ✅ Error handling robusto
- ✅ Workflows estables en producción

### Fase 5 (Producción)
- ✅ 99.9% uptime en producción
- ✅ CI/CD funcional
- ✅ Documentación operacional completa

---

## Timeline Gantt

```
Semanas  1    2    3    4    5    6    7    8    9   10   11   12   13   14   15   16   17   18   19   20   21   22   23   24   25
Fase 0   [====]
Fase 1        [====================Repository & Source====================]
                                                                      [====DDIC====][===Transport===]
Fase 2                                                                                      [========Transport Complete=========]
                                                                                                                            [Mod][Act][Quality][Lifecycle][Query][Where][===CDS & RAP===][Enh]
Fase 3                                                                                                                                                                              [===Progressive Discovery===]
Fase 4                                                                                                                                                                                                  [========Workflows========]
Fase 5   (OPCIONAL - después de Fase 4)                                                                                                                                                                                          [===Docker===][=CI/CD=][Monitor][Security][Docs]
```

**Leyenda**:
- `[====]` Fase en progreso
- `Mod` = Modification
- `Act` = Activation
- `Enh` = Enhancements

---

## Métricas de Progreso

### Herramientas Migradas

```
Fase 0:  [██░░░░░░░░░░░░░░░░░░]   1/59 tools (1.7%)
Fase 1:  [████████░░░░░░░░░░░░]  16/59 tools (27%)
Fase 2:  [████████████████████]  59/59 tools (100%)
Fase 3:  [████████████████████]  68/68 tools (100% + optimizations)
Fase 4:  [████████████████████]  80/80 tools (100% + workflows)
```

### Cobertura de Tests

```
Meta:    80% coverage mínimo
Actual:  0% (Java POC sin tests)
Fase 1:  80%+ (16 tools)
Fase 2:  80%+ (59 tools)
```

### Performance (Tool Call Latency)

```
Baseline Python:  ~500ms por call
Meta Java:        ~500ms ±10%
Progressive:      ~200ms (60% reducción)
Workflows:        ~800ms (vs 2500ms antes)
```

---

## Recursos y Dependencias

### Equipo Requerido

- **1 desarrollador Java senior** (Fases 1-4)
- **1 desarrollador Python** (soporte legacy, Fases 0-2)
- **1 SAP Basis consultant** (soporte RFC/ADT, Fases 1-2)

### Dependencias Externas

- ✅ SAP JCo 3.1.x (licencia SAP)
- ✅ SAP NetWeaver RFC SDK (licencia SAP)
- ✅ Acceso a sistemas SAP de desarrollo
- ⏳ Aprobación para deployment (Fase 5)

### Infraestructura

**Desarrollo**:
- macOS con Java 21+ y Maven 3.9+
- Python 3.11+ con uv

**Testing**:
- SAP S/4HANA development system (CRY / GDC)
- JUnit 5 + Mockito

**Producción** (Fase 5):
- Docker + Kubernetes / Cloud Run
- Monitoring: Prometheus + Grafana
- Secrets: Google Secret Manager

---

## Aprobaciones y Cambios

| Fecha | Versión | Cambios | Aprobador |
|-------|---------|---------|-----------|
| 2025-11-08 | 1.0 | Plan inicial creado | Pendiente |

---

## Referencias

- `docs/research/abap_mcp_tools_strategy_2025.md` - Investigación de optimizaciones
- `python-legacy/README.md` - Documentación proyecto Python (futuro)
- `README.md` - Documentación proyecto Java
- `CLAUDE.md` - Instrucciones para Claude Code

---

**Última actualización**: 2025-11-08
**Próxima revisión**: Al completar Fase 0
**Contacto**: Crystal Development Team

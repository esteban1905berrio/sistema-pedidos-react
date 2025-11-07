# PR: Estrategia Tools ABAP Consumibles desde MCP Server

**ID**: PR-MCP-TOOLS-001
**Estado**: ✅ Investigación Completada → Pendiente Aprobación Implementation
**Fecha Creación**: 2025-11-07
**Última Actualización**: 2025-11-07
**Responsable**: Development Team
**Prioridad**: Alta

---

## User Story

Como desarrollador MCP
Quiero crear clases en ABAP con utilidades
Para luego ser consumidas desde una tools del servidor MCP

---

## Criterios de Aceptación

- [x] Investigar de qué forma podemos crear tools en ABAP consumibles desde MCP Server
- [x] Evaluar qué otro tipo de recursos se pueden consumir con ADT (reportes, FM, OData)
- [x] Validar alternativas de conexión con SAP S/4HANA On-Premise y Cloud (prioridad On-Premise)
- [x] Proponer estrategia para interfazar eficientemente con MCP Server (granularidad, tokens, optimización)
- [ ] **PENDIENTE**: Implementar Phase 1 (Progressive Discovery)

---

## Investigación Completada

### Documento Principal

**Ver hallazgos completos en**: [`docs/research/abap_mcp_tools_strategy_2025.md`](/docs/research/abap_mcp_tools_strategy_2025.md)

### Resumen Ejecutivo

La investigación evaluó **4 patrones MCP** para optimización de tokens y **3 opciones arquitectónicas** (Python, Java, Híbrido) para migración del servidor actual.

---

## Decisiones Técnicas

### 1. Conectividad SAP: RFC (Actual) ✅

**Decisión**: Mantener RFC como mecanismo principal para On-Premise.

| Criterio | RFC | HTTP/ADT | OData | Decisión |
|----------|-----|----------|-------|----------|
| On-Premise Support | ✅ Nativo | ⚠️ Requiere SICF | ⚠️ Requiere Gateway | **RFC** |
| Stateful Operations | ✅ Nativo | ⚠️ Manual | ⚠️ Manual | **RFC** |
| Latency | ✅ Muy baja | ⚠️ Media | ⚠️ Media | **RFC** |
| Infrastructure | ✅ Implementado | ❌ Trabajo adicional | ❌ Trabajo adicional | **RFC** |
| Token Efficiency | ✅ Con patterns | ✅ Con patterns | ✅ Con patterns | **Todos** |

**Estrategia Híbrida (Futuro):**
- **Core**: RFC → `SADT_REST_RFC_ENDPOINT` (59 tools actuales)
- **Advanced Queries**: OData V4 (complemento para CDS Views, RAP)

### 2. Patrones MCP: 4 Patrones Implementables

#### Pattern 1: Progressive Discovery ⭐⭐ (PRIORIDAD MÁXIMA)

**Reducción Tokens:** 94% (29,500 → 1,800)

**Concepto:**
```
Stage 1: list_service_categories() → 10 categorías × 50 tokens = 500
Stage 2: get_category_tools("Transport") → 8 tools × 100 = 800
Stage 3: get_tool_schema("transport_copy") → 1 schema × 500 = 500
Stage 4: Ejecutar tool
```

**Implementación**: Phase 1 (2-3 semanas)

---

#### Pattern 2: Workflow-Based Tools ⭐ (ALTA PRIORIDAD)

**Reducción Tokens:** 84% (5,000 → 800)

**Principio**: Una tool = Un objetivo completo de usuario

**Ejemplo:**
```python
# ❌ Antipatrón (5 tool calls):
create_transport() → add_object() × 3 → release_transport()

# ✅ Patrón (1 tool call):
deploy_objects_with_transport(
    objects=["ZCLTEST", "ZREPORT"],
    desc="Phase 2",
    auto_release=False
)
```

**Implementación**: Phase 2 (3-4 semanas)

---

#### Pattern 3: Code Execution Mode ⭐ (MEDIA-ALTA PRIORIDAD)

**Reducción Tokens:** 99.6% (50,000 → 200)

**Concepto**: Claude escribe código ejecutable en sandbox en lugar de tool calls secuenciales.

**Caso de Uso**: Analizar 50 transportes y detectar duplicados

```python
# Claude genera código (ejecuta en servidor):
transports = sap.list_user_transports(user="DEV", status="D")
analysis = []
for transport in transports:
    objects = sap.get_transport_objects(transport['number'])
    # Procesamiento...
result = {"total": 50, "duplicates": [...]}  # Solo esto vuelve al LLM
```

**Implementación**: Phase 3 (4-5 semanas)

---

#### Pattern 4: Semantic Search ⚠️ (EVALUAR POST-PHASE 1)

**Concepto**: Vector DB para discovery dinámico de tools relevantes.

**Decisión**: Puede ser overkill con 59 tools. Evaluar después de Progressive Discovery.

### 3. Exposición Function Modules ABAP

**Opción Recomendada**: Crear FMs custom + Wrapper ADT

```
MCP Tool → Service → RfcAdapter → SADT_REST_RFC_ENDPOINT → Z_FM_CUSTOM
```

**Ventajas:**
- ✅ Reutiliza infraestructura ADT existente
- ✅ Sin necesidad de SICF (solo FM en ABAP)
- ✅ Compatible con arquitectura actual
- ✅ Fácil versionamiento

**FMs Propuestos (Phase 4):**
- `Z_TRANSPORT_COPY`: Copiar transportes entre sistemas
- `Z_TRANSPORT_COMPARE`: Diff analysis de transportes
- `Z_VALIDATE_OBJECT_NAMES`: Naming conventions validation
- `Z_BATCH_ANALYSIS_UTILITIES`: Mass operations

### 4. Optimización Formato Respuestas

**Formato Conciso (79% reducción tokens):**

```json
{
  "request": "DEVK900123",
  "desc": "Phase 2 Migration",
  "owner": "DEVELOPER",
  "objects": [
    {"type": "CLAS", "name": "ZCLTEST", "pkg": "ZTEST"}
  ]
}
```

**Principios:**
- Field names abreviados (description → desc)
- Omitir timestamps y metadata innecesaria
- Paginar arrays grandes (max 50 items)
- Convertir XML a JSON compacto

---

## Plan de Implementación

### FASE 1-4: Optimización Python (10-14 semanas)

#### Phase 1: Progressive Discovery (2-3 semanas)

**Objetivo**: Reducir token usage 70%+ sin afectar funcionalidad.

**Tareas:**
1. ✅ Crear estructura `app/mcp/tools/_discovery/`
2. ✅ Implementar `list_service_categories()`
3. ✅ Implementar `get_category_tools(category)`
4. ✅ Implementar `get_tool_schema(category, tool)`
5. ✅ Migrar 59 tools a lazy-loading
6. ✅ Testing: Verificar funcionamiento completo
7. ✅ Documentar en `docs/architecture/progressive-discovery.md`

**Entregables:**
- Sistema de discovery funcional
- Reducción medible de tokens (baseline vs progressive)
- Documentación completa

---

#### Phase 2: Workflow-Based Tools (3-4 semanas)

**Objetivo**: Crear 5-8 workflows atómicos para casos de uso común.

**Tools Propuestas:**
- `deploy_objects_with_transport()`
- `analyze_transport_batch()`
- `compare_and_copy_transport()`
- `validate_and_create_package_structure()`

**Entregables:**
- 5-8 workflow tools funcionales
- Casos de uso documentados
- Comparación performance vs granular tools

---

#### Phase 3: Code Execution Mode (4-5 semanas)

**Objetivo**: Permitir análisis masivo via código ejecutable.

**Tareas:**
1. ✅ Diseñar sandbox seguro (RestrictedPython)
2. ✅ Crear cliente ABAP simplificado (`mcp_abap_client`)
3. ✅ Implementar `execute_abap_analysis(code)`
4. ✅ Whitelist módulos permitidos (pandas, json)
5. ✅ Testing: Seguridad, timeout, error handling
6. ✅ Documentar en `docs/architecture/code-execution.md`

**Entregables:**
- Sandbox funcional y seguro
- Cliente ABAP simplificado
- Ejemplos de análisis complejos

---

#### Phase 4: Custom ABAP Utilities (5-6 semanas)

**Objetivo**: Crear FMs custom para operaciones no cubiertas por ADT.

**FMs a Implementar:**
- `Z_TRANSPORT_COPY`
- `Z_TRANSPORT_COMPARE`
- `Z_VALIDATE_OBJECT_NAMES`
- `Z_BATCH_ANALYSIS_UTILITIES`

**Entregables:**
- 4+ FMs custom funcionando
- Integración ADT completa
- Documentación de interfaces

---

## Referencias

- **Investigación Completa**: [`docs/research/abap_mcp_tools_strategy_2025.md`](/docs/research/abap_mcp_tools_strategy_2025.md)
- **Migración Java**: [`docs/requirements/mcp/pr_mcp_java.md`](/docs/requirements/mcp/pr_mcp_java.md)

---

## Métricas de Éxito

| Métrica | Baseline | Target Phase 1-4 | Mejora |
|---------|----------|------------------|--------|
| **Initial Tool Load** | 29,500 tokens | 500 tokens | 98% ↓ |
| **Average Operation** | 2,000 tokens | 500 tokens | 75% ↓ |
| **Complex Analysis** | 50,000 tokens | 1,000 tokens | 98% ↓ |
| **Tool Scalability** | 59 tools max | 100s tools | ∞ |

---

## Próximos Pasos

1. ✅ **Aprobar Phase 1**: Progressive Discovery (2-3 semanas)
2. ✅ **Iniciar Implementación**: Crear estructura `_discovery/`
3. ⚠️ **Evaluar licenciamiento SAP JCo**: Para Phase 5 (migración Java)
4. ✅ **Configurar CI/CD**: Docker builds multi-platform (paralelo)

---

**Última Actualización**: 2025-11-07
**Estado**: ✅ Investigación Completada → **Pendiente Aprobación Phase 1**
**Próxima Revisión**: Post-Phase 1 implementation

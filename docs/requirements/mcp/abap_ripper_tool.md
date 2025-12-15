# ABAP Object Ripper Tool - Planificación

**Fecha**: 2025-12-15
**Estado**: ✅ COMPLETADO (Fase 5 completada)
**Versión**: 0.8.0

---

## Decisiones Confirmadas

| Aspecto | Decisión |
|---------|----------|
| **Propósito** | Referencia técnica + Análisis de código + Training Data (fase posterior) |
| **Prefijo FMs** | `ZCX_UTIL_*` |
| **Function Group** | `ZGFCX_1` (existente) |
| **Selección objetos** | Por paquete (con recursividad) |
| **Metadatos DDIC** | Completo (campos, textos, FK, índices, includes) |
| **Variants** | ❌ No incluir, solo código fuente |
| **Textos/Traducciones** | ❌ No incluir, solo código |
| **Límite tamaño** | Sin límite |
| **Objetos con errores** | ✅ Extraer con flag de warning en metadata |
| **Formato source** | Pretty-print normalizado |
| **Historial transportes** | ❌ No, solo código actual |
| **Paralelismo** | Configurable (default: 4 hilos) |
| **Output path** | Sugerir default `./extracted/<package>` + confirmar |

---

## 1. Visión General

### Objetivo
Herramienta MCP modular para extraer objetos ABAP relevantes de sistemas SAP S/4HANA on-premise, destinada a:
- **Referencia técnica**: Documentación de patrones y soluciones implementadas
- **Análisis de código**: Auditoría y evaluación de calidad
- **Training Data** (fase posterior): Datos para entrenar modelos LLM

### Principios de Diseño
1. **Ligero**: Sin overhead al sistema SAP
2. **Discreto**: FMs con naming genérico (`ZCX_UTIL_*`)
3. **Modular**: LLM solo para análisis, Java para extracción
4. **Estructura ADT**: Compatible con Eclipse ADT file structure

---

## 2. Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        Claude Code (LLM)                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ Análisis        │  │ Selección       │  │ Recomendaciones │  │
│  │ de Paquetes     │  │ Inteligente     │  │ de Calidad      │  │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘  │
└───────────┼─────────────────────┼─────────────────────┼─────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Java MCP Server (giralmcp)                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ RipperService   │  │ ExtractionSvc   │  │ FileWriterSvc   │  │
│  │ (orchestration) │  │ (parallel)      │  │ (ADT format)    │  │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘  │
└───────────┼─────────────────────┼─────────────────────┼─────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                        SAP S/4HANA                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ ZCX_UTIL_*      │  │ ADT REST API    │  │ Standard RFCs   │  │
│  │ (custom FMs)    │  │ (via RFC)       │  │ (RFC_READ_*)    │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Objetos a Extraer

### 3.1 INCLUIR (Alta Prioridad)

| Tipo | Código | Descripción | Valor para Training |
|------|--------|-------------|---------------------|
| **Clases** | CLAS | Clases ABAP OO | ⭐⭐⭐ Alto |
| **Interfaces** | INTF | Interfaces ABAP | ⭐⭐⭐ Alto |
| **Programas** | PROG | Reports, Module Pools | ⭐⭐ Medio |
| **Function Modules** | FUNC | Módulos de función | ⭐⭐⭐ Alto |
| **Function Groups** | FUGR | Grupos de funciones | ⭐⭐ Medio |
| **Includes** | PROG (I) | Includes de programa | ⭐⭐ Medio |
| **CDS Views** | DDLS | Core Data Services | ⭐⭐⭐ Alto |
| **Table Types** | TTYP | Tipos tabla | ⭐⭐ Medio |
| **Structures** | TABL (S) | Estructuras DDIC | ⭐⭐ Medio |
| **Tables** | TABL (T) | Tablas transparentes Z | ⭐⭐ Medio |
| **Data Elements** | DTEL | Elementos de datos | ⭐ Bajo |
| **Domains** | DOMA | Dominios | ⭐ Bajo |
| **Search Helps** | SHLP | Ayudas de búsqueda | ⭐ Bajo |

### 3.2 INCLUIR (Customizing Técnico)

| Tipo | Código | Descripción | Valor |
|------|--------|-------------|-------|
| **DMEE Trees** | DMEE | Formatos de pago | ⭐⭐⭐ Muy Alto |
| **BADIs** | SXCI | Implementaciones BADI | ⭐⭐⭐ Alto |
| **Enhancements** | ENHO | Enhancement implementations | ⭐⭐⭐ Alto |
| **Enhancement Spots** | ENHS | Spots de enhancement | ⭐⭐ Medio |
| **CMOD Projects** | CMOD | Proyectos de ampliación | ⭐⭐ Medio |
| **User Exits** | EXIT | Exits de usuario | ⭐⭐ Medio |
| **BTE Events** | BTE | Business Transaction Events | ⭐⭐ Medio |
| **Smartforms** | SSFO | Formularios Smart | ⭐⭐ Medio |
| **Adobe Forms** | SFPF | Formularios Adobe | ⭐⭐ Medio |

### 3.3 INCLUIR (Fiori/UI5) - Fase 2

| Tipo | Código | Descripción | Valor |
|------|--------|-------------|-------|
| **OData Services** | SEGW | Servicios OData | ⭐⭐⭐ Alto |
| **CDS Annotations** | DDLA | Anotaciones Fiori | ⭐⭐⭐ Alto |
| **BSP Applications** | WAPA | Apps BSP/UI5 | ⭐⭐ Medio |

### 3.4 OMITIR

| Tipo | Razón |
|------|-------|
| **Objetos $TMP** | Temporales, sin valor |
| **Objetos SAP estándar** | No son custom, ya documentados |
| **Generados (CDS artifacts)** | Auto-generados, redundantes |
| **Test classes aisladas** | Solo útiles con contexto |
| **Objetos ZTEST*, ZDEMO*** | Pruebas sin valor productivo |
| **Objetos sin activar** | Incompletos |
| **Includes vacíos** | Sin contenido |
| **Message classes** | Textos, no código |

---

## 4. Estructura de Salida (ADT-Compatible)

```
<output_directory>/
├── manifest.json                    # Catálogo de extracción
├── extraction_report.json           # Resumen y estadísticas
│
├── packages/
│   └── <PACKAGE_NAME>/
│       ├── package_info.json        # Metadata del paquete
│       │
│       ├── classes/
│       │   └── <CLASS_NAME>/
│       │       ├── <class>.clas.abap           # Definición
│       │       ├── <class>.clas.impl.abap      # Implementación
│       │       ├── <class>.clas.testclasses.abap  # Tests (si existe)
│       │       └── <class>.clas.json           # Metadata
│       │
│       ├── interfaces/
│       │   └── <INTF_NAME>/
│       │       ├── <intf>.intf.abap
│       │       └── <intf>.intf.json
│       │
│       ├── function_groups/
│       │   └── <FUGR_NAME>/
│       │       ├── <fugr>.fugr.abap            # Main program
│       │       ├── includes/
│       │       │   └── <include>.abap
│       │       ├── function_modules/
│       │       │   └── <fm_name>/
│       │       │       ├── <fm>.func.abap      # Source
│       │       │       └── <fm>.func.json      # Signature
│       │       └── <fugr>.fugr.json
│       │
│       ├── programs/
│       │   └── <PROG_NAME>/
│       │       ├── <prog>.prog.abap
│       │       ├── includes/
│       │       │   └── <include>.abap
│       │       └── <prog>.prog.json
│       │
│       ├── ddic/
│       │   ├── tables/
│       │   │   └── <TABLE_NAME>.tabl.json      # Estructura completa
│       │   ├── structures/
│       │   │   └── <STRUCT_NAME>.tabl.json
│       │   ├── data_elements/
│       │   │   └── <DTEL_NAME>.dtel.json
│       │   ├── domains/
│       │   │   └── <DOMA_NAME>.doma.json
│       │   ├── table_types/
│       │   │   └── <TTYP_NAME>.ttyp.json
│       │   └── search_helps/
│       │       └── <SHLP_NAME>.shlp.json
│       │
│       ├── cds/
│       │   └── <CDS_NAME>/
│       │       ├── <cds>.ddls.asddls           # Source
│       │       ├── <cds>.ddls.json             # Metadata
│       │       └── <cds>.dcls.asdcls           # Access control (si existe)
│       │
│       ├── enhancements/
│       │   ├── badi_implementations/
│       │   │   └── <IMPL_NAME>.impl.json
│       │   ├── enhancement_implementations/
│       │   │   └── <ENHO_NAME>.enho.abap
│       │   └── cmod_projects/
│       │       └── <CMOD_NAME>.cmod.json
│       │
│       ├── dmee/
│       │   └── <DMEE_TREE>/
│       │       ├── <tree>.dmee.json            # Estructura árbol
│       │       └── function_modules/           # FMs asociados
│       │           └── <fm>.func.abap
│       │
│       └── forms/
│           ├── smartforms/
│           │   └── <SF_NAME>.ssfo.json
│           └── adobe_forms/
│               └── <AF_NAME>.sfpf.json
```

---

## 5. Function Modules Requeridos en SAP

### 5.1 FMs Custom a Crear

| FM | Propósito | Complejidad |
|----|-----------|-------------|
| `ZCX_UTIL_GET_PKG_OBJECTS` | Listar objetos de un paquete con filtros | Media |
| `ZCX_UTIL_GET_DDIC_FULL` | Estructura DDIC completa (campos, FK, índices) | Alta |
| `ZCX_UTIL_GET_DMEE_TREE` | Extraer árbol DMEE completo | Alta |
| `ZCX_UTIL_GET_ENHANCEMENT` | Obtener implementaciones de enhancement | Media |
| `ZCX_UTIL_GET_BADI_IMPL` | Obtener implementaciones BADI | Media |
| `ZCX_UTIL_GET_CDS_SOURCE` | Obtener source de CDS View | Baja |

### 5.2 FMs/APIs Estándar a Usar

| FM/API | Propósito |
|--------|-----------|
| ADT REST API | Código fuente de clases, programas, FMs |
| `SADT_REST_RFC_ENDPOINT` | Bridge HTTP-RFC (ya usado) |
| `RFC_READ_TABLE` | Lectura de tablas config (si necesario) |
| `RS_FUNCTIONMODULE_*` | Metadata de FMs |
| `SEO_CLASS_*` | Metadata de clases |

---

## 6. MCP Tools a Implementar

### 6.1 Tools de Análisis (LLM-Driven)

```java
@Tool(description = "Analyze packages and recommend objects for extraction")
analyzePackagesForExtraction(
    List<String> packageNames,
    boolean recursive,
    String purpose  // "training", "reference", "audit"
) → ExtractionRecommendation

@Tool(description = "Evaluate code quality of objects before extraction")
evaluateCodeQuality(
    String objectUri,
    String objectType
) → QualityReport
```

### 6.2 Tools de Extracción (Java-Only, No LLM)

```java
@Tool(description = "Extract all objects from packages to local filesystem")
extractPackageObjects(
    List<String> packageNames,
    String outputPath,
    ExtractionConfig config
) → ExtractionResult

@Tool(description = "Extract DDIC structure with full metadata")
extractDdicStructure(
    String objectName,
    String objectType,  // TABLE, STRUCTURE, DTEL, DOMA
    String outputPath
) → DdicExtractionResult

@Tool(description = "Extract DMEE payment format tree")
extractDmeeTree(
    String treeId,
    String formatType,
    String outputPath
) → DmeeExtractionResult

@Tool(description = "Extract enhancement/BADI implementations")
extractEnhancements(
    String packageName,
    String outputPath
) → EnhancementExtractionResult
```

### 6.3 Tools de Utilidad

```java
@Tool(description = "Get extraction progress and statistics")
getExtractionStatus(String extractionId) → ExtractionStatus

@Tool(description = "Validate extracted objects integrity")
validateExtraction(String outputPath) → ValidationResult
```

---

## 7. Flujo de Trabajo

```
Usuario                    LLM (Claude)                 Java Service
   │                           │                            │
   │ "Extraer paquete ZFI"     │                            │
   ├──────────────────────────►│                            │
   │                           │                            │
   │    ¿A qué carpeta?        │                            │
   │◄──────────────────────────┤                            │
   │                           │                            │
   │  "/output/zfi_extract"    │                            │
   ├──────────────────────────►│                            │
   │                           │                            │
   │                           │ analyzePackagesForExtraction()
   │                           ├───────────────────────────►│
   │                           │◄───────────────────────────┤
   │                           │ ExtractionRecommendation   │
   │                           │                            │
   │   Análisis: 50 paquetes   │                            │
   │   ~2,500 objetos          │                            │
   │   Recomendación: excluir  │                            │
   │   ZTEST*, $TMP            │                            │
   │   ¿Proceder?              │                            │
   │◄──────────────────────────┤                            │
   │                           │                            │
   │        Sí                 │                            │
   ├──────────────────────────►│                            │
   │                           │                            │
   │                           │ extractPackageObjects()    │
   │                           ├───────────────────────────►│
   │                           │     (parallel extraction)  │
   │                           │          ...               │
   │                           │◄───────────────────────────┤
   │                           │ ExtractionResult           │
   │                           │                            │
   │   ✓ Extracción completa   │                            │
   │   - 2,450 objetos         │                            │
   │   - 125 MB                │                            │
   │   - 3 errores (log)       │                            │
   │◄──────────────────────────┤                            │
```

---

## 8. Configuración de Extracción

```json
{
  "extraction": {
    "outputPath": "./extracted/{package}",
    "recursive": true,
    "parallelThreads": 4,
    "confirmPath": true,

    "includeObjectTypes": [
      "CLAS", "INTF", "PROG", "FUGR", "FUNC",
      "TABL", "DTEL", "DOMA", "TTYP", "SHLP",
      "DDLS", "ENHO", "SXCI", "DMEE"
    ],

    "excludePatterns": [
      "$TMP",
      "ZTEST*",
      "ZDEMO*",
      "*_GENERATED",
      "*_TMP_*"
    ],

    "source": {
      "prettyPrint": true,
      "includeVariants": false,
      "includeTexts": false,
      "includeTransportHistory": false,
      "maxSizeBytes": null
    },

    "ddic": {
      "includeFields": true,
      "includeFieldTexts": true,
      "includeForeignKeys": true,
      "includeIndexes": true,
      "includeIncludes": true
    },

    "quality": {
      "skipInactive": false,
      "skipEmpty": true,
      "flagProblematic": true,
      "extractWithWarnings": true
    }
  }
}
```

---

## 9. Fases de Implementación

### Fase 1: Core Extraction (MVP)
- [ ] Crear FM `ZCX_UTIL_GET_PKG_OBJECTS`
- [ ] Crear FM `ZCX_UTIL_GET_DDIC_FULL`
- [ ] Implementar `RipperService.java`
- [ ] Implementar `extractPackageObjects()` tool
- [ ] Implementar `extractDdicStructure()` tool
- [ ] Estructura de carpetas ADT
- [ ] manifest.json generation

### Fase 2: Enhancements & DMEE
- [ ] Crear FM `ZCX_UTIL_GET_DMEE_TREE`
- [ ] Crear FM `ZCX_UTIL_GET_ENHANCEMENT`
- [ ] Crear FM `ZCX_UTIL_GET_BADI_IMPL`
- [ ] Implementar `extractDmeeTree()` tool
- [ ] Implementar `extractEnhancements()` tool

### Fase 3: Analysis & Quality
- [ ] Implementar `analyzePackagesForExtraction()` (LLM)
- [ ] Implementar `evaluateCodeQuality()` (LLM)
- [ ] Quality flags en metadata
- [ ] Extraction recommendations

### Fase 4: Fiori/CDS
- [ ] Implementar extracción CDS Views
- [ ] Implementar extracción OData services
- [ ] Implementar extracción annotations

---

## 10. Consideraciones de Seguridad

### En SAP
- FMs solo lectura (no modifican datos)
- Autorización requerida: `S_DEVELOP` (display)
- Sin acceso a datos de negocio sensibles
- Logging de extracciones en SM21

### En Local
- Output path validado (no system dirs)
- No credenciales en archivos extraídos
- Sanitización de nombres de archivo

---

## 11. Preguntas Resueltas ✅

| Pregunta | Decisión |
|----------|----------|
| ¿Incluir variants de programas? | ❌ No, solo código fuente |
| ¿Extraer textos/traducciones? | ❌ No, solo código |
| ¿Incluir transport history? | ❌ No, solo estado actual |
| ¿Límite de tamaño por objeto? | Sin límite |
| ¿Formato de source? | Pretty-print normalizado |
| ¿Objetos con errores? | Extraer con flag warning |
| ¿Paralelismo? | Configurable, default 4 |
| ¿Output path? | Default + confirmar |

---

## 12. Estimación de Esfuerzo

| Componente | Estimación |
|------------|------------|
| FMs ABAP (5) | 3-4 días |
| Java Services | 4-5 días |
| MCP Tools | 2-3 días |
| File Writer (ADT) | 2-3 días |
| Testing | 3-4 días |
| **Total** | **~15-20 días** |

---

## 13. Inventario MCP Server Actual (Verificación Exhaustiva)

> **Metodología**: Inventario realizado via `grep -h "@McpTool" src/.../tool/*.java`
>
> **Resultado**: 21 archivos `*Tools.java`, 48 métodos `@McpTool`

### 13.1 Tools Existentes - Source Code (Ripper Core)

| Tool | Ubicación | Tipo Objeto | Estado |
|------|-----------|-------------|--------|
| `get_class_source` | ClassTools.java:63 | CLAS | ✅ |
| `get_class_includes` | ClassTools.java:125 | CLAS includes | ✅ |
| `get_ddic_source` | ClassTools.java:187 | TABL, DTEL, VIEW | ✅ |
| `get_program_source` | ProgramTools.java:81 | PROG | ✅ |
| `get_include_source` | ProgramTools.java:142 | PROG includes | ✅ |
| `get_object_source` | ObjectTools.java:134 | INTF, FUGR, FUNC | ✅ |
| `get_object_structure` | ObjectTools.java:74 | Cualquier tipo (metadata) | ✅ |
| `get_cds_source` | CdsTools.java:54 | DDLS (CDS Views) | ✅ |
| `get_enhancement_source` | EnhancementTools.java:66 | ENHO (Enhancements) | ✅ |
| `get_badi_implementation` | BadiTools.java:70 | SXCI (BAdI Implementation) | ✅ **NUEVO** |

### 13.2 Tools Existentes - Navegación (Ripper Discovery)

| Tool | Ubicación | FM Backend |
|------|-----------|------------|
| `get_package_objects` | NavigationTools.java:91 | Z_CX_GET_PACKAGE_OBJECTS |
| `getPackageHierarchy` | PackageHierarchyTools.java:82 | Z_CX_GET_PACKAGE_HIERARCHY |
| `search_objects` | SearchTools.java:62 | ADT REST |

### 13.3 Tools Existentes - Extracción a Filesystem

| Tool | Ubicación |
|------|-----------|
| `extract_abap_components` | ComponentExtractionTools.java:86 |
| `sync_manifest_with_code` | ComponentExtractionTools.java:160 |
| `install_abap_components` | ComponentInstallationTools.java |

### 13.4 Prompts MCP Existentes (13)

**Prompts originales (8):**
```
review_abap_code, analyze_transport, explain_class, debug_dump,
migration_checklist, generate_unit_test, document_function_module, compare_versions
```

**Prompts nuevos Ripper Tool (5) - Fase 2:**
```
analyze_package_for_extraction, generate_extraction_report, explain_cds_view,
explain_enhancement, evaluate_code_quality_batch
```

---

## 14. Gap Analysis Corregido

### 14.1 Cobertura Actual (~95%)

| Tipo Objeto | Tool Existente | Estado |
|-------------|----------------|--------|
| CLAS | `get_class_source` | ✅ Cubierto |
| INTF | `get_object_source` | ✅ Cubierto |
| PROG | `get_program_source` | ✅ Cubierto |
| FUGR | `get_object_source` | ✅ Cubierto |
| FUNC | `get_object_source` | ✅ Cubierto |
| TABL | `get_ddic_source` | ✅ Cubierto |
| DTEL | `get_ddic_source` | ✅ Cubierto |
| DDLS | `get_cds_source` | ✅ Cubierto |
| ENHO | `get_enhancement_source` | ✅ Cubierto |
| SXCI | `get_badi_implementation` | ✅ Cubierto **NUEVO** |

### 14.2 Sin Cobertura (1 tipo) - Requieren Desarrollo

| Tipo | Tool Propuesta | Requerimiento |
|------|----------------|---------------|
| ~~**DDLS**~~ | ~~`get_cds_source`~~ | ~~ADT: `/sap/bc/adt/ddic/ddl/sources/{name}` (sin FM)~~ ✅ IMPLEMENTADO |
| ~~**ENHO**~~ | ~~`get_enhancement_source`~~ | ~~FM: `ZCX_GET_ENHANCEMENT_SOURCE`~~ ✅ IMPLEMENTADO |
| ~~**SXCI**~~ | ~~`get_badi_implementation`~~ | ~~FM: `ZCX_UTIL_GET_BADI_IMPL`~~ ✅ IMPLEMENTADO |
| **DMEE** | `get_dmee_source` | FM: `ZCX_UTIL_GET_DMEE_TREE` |

### 14.3 ⚠️ Correcciones Importantes

| Error Evitado | Razón |
|---------------|-------|
| ❌ NO crear `DdicTools.java` | Ya existe `get_ddic_source` en ClassTools.java:187 |
| ❌ NO crear tools para INTF/FUGR/FUNC | `get_object_source` las cubre genéricamente |
| ❌ NO incluir MCP Resources | Rechazado - fase posterior opcional |
| ❌ NO crear FM `ZCX_UTIL_GET_CDS_SOURCE` | CDS usa ADT REST directo, sin FM custom |

---

## 15. Plan de Implementación Aprobado

### Fase 1: CDS Views (1-2 días) ✅ COMPLETADA

**Archivos creados:**
```
src/main/java/com/crystal/mcp/sapserver/
├── service/CdsService.java              ✅
├── tool/CdsTools.java                   ✅
└── model/CdsSourceResult.java           ✅

src/test/java/com/crystal/mcp/sapserver/manual/
└── ManualCdsServiceTest.java            ✅
```

**Tareas completadas:**
1. ✅ Implementar `CdsService.java` usando ADT endpoint `/sap/bc/adt/ddic/ddl/sources/{name}`
2. ✅ Crear `CdsTools.java` con tool `get_cds_source`
3. ✅ Crear model `CdsSourceResult.java`
4. ✅ Test manual (CommandLineRunner)

**Comando de test:**
```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualCdsServiceTest
```

### Fase 2: Prompts (1 día) ✅ COMPLETADA

**Archivo modificado:** `SapPromptProvider.java`

**Nuevos Prompts (5):**

| Prompt | Descripción | Estado |
|--------|-------------|--------|
| `analyze_package_for_extraction` | Analizar paquete y recomendar qué extraer | ✅ |
| `generate_extraction_report` | Generar reporte de extracción | ✅ |
| `explain_cds_view` | Explicar CDS view y asociaciones | ✅ |
| `explain_enhancement` | Explicar enhancement/BADI | ✅ |
| `evaluate_code_quality_batch` | Evaluar calidad de múltiples objetos | ✅ |

**Detalles implementación:**
- Total prompts en servidor: 8 existentes + 5 nuevos = **13 prompts**
- Cada prompt incluye argumentos estructurados para LLM
- Los prompts delegan a tools existentes o futuras herramientas de extracción

### Fase 3: Enhancement Tool (2-3 días) ✅ COMPLETADA

**Archivos creados:**
```
src/main/java/com/crystal/mcp/sapserver/
├── service/EnhancementService.java         ✅
├── tool/EnhancementTools.java              ✅
└── model/EnhancementSourceResult.java      ✅
```

**SAP:** FM `ZCX_GET_ENHANCEMENT_SOURCE` en `ZGFCX_1` ✅ ACTIVO en GDC

**Tareas completadas:**
1. ✅ Implementar `EnhancementService.java` usando FM custom via RfcAdapter.callFunctionModule()
2. ✅ Crear `EnhancementTools.java` con tool `get_enhancement_source`
3. ✅ Crear model `EnhancementSourceResult.java` con records:
   - `EnhancementHeader` (metadata del enhancement)
   - `EnhancementElement` (hooks o BAdI implementations)
   - `EnhancementSourceLine` (líneas de código)
4. ✅ FM `ZCX_GET_ENHANCEMENT_SOURCE` activo en GDC con soporte para:
   - BADI_IMPL (implementaciones BAdI)
   - HOOK_IMPL (inyección de código)

**Estructura del response:**
```json
{
  "enhancementName": "ZENH_EXAMPLE",
  "header": {
    "enhancementName": "...",
    "description": "...",
    "toolType": "BADI_IMPL|HOOK_IMPL",
    "toolTypeText": "...",
    "devclass": "...",
    "author": "...",
    "createdOn": "...",
    "changedBy": "...",
    "changedOn": "..."
  },
  "elements": [{
    "elementType": "...",
    "spotName": "...",
    "programName": "...",
    "fullName": "...",
    "badiName": "...",
    "badiImpl": "...",
    "implClass": "...",
    "interfaceName": "...",
    "active": true
  }],
  "sourceLines": [
    {"lineNo": 1, "code": "METHOD if_ex_..."},
    {"lineNo": 2, "code": "  DATA: lv_..."}
  ],
  "metadata": {
    "functionModule": "ZCX_GET_ENHANCEMENT_SOURCE",
    "version": "00000"
  }
}
```

### Fase 4: BAdI Tool (2-3 días) ✅ COMPLETADA

**Archivos creados:**
```
src/main/java/com/crystal/mcp/sapserver/
├── service/BadiService.java              ✅
├── tool/BadiTools.java                   ✅
└── model/BadiImplementationResult.java   ✅
```

**SAP:** FM `ZCX_UTIL_GET_BADI_IMPL` en `ZGFCX_1` ✅ ACTIVO en GDC

**Tareas completadas:**
1. ✅ Implementar `BadiService.java` usando FM custom via RfcAdapter.callFunctionModule()
2. ✅ Crear `BadiTools.java` con tool `get_badi_implementation`
3. ✅ Crear model `BadiImplementationResult.java` con records:
   - `BadiImplementationHeader` (metadata del BAdI implementation)
   - `BadiDefinitionInfo` (BAdI definitions cubiertas)
   - `BadiImplementingClass` (clases que implementan interfaces)
4. ✅ FM `ZCX_UTIL_GET_BADI_IMPL` activo en GDC con consultas a:
   - SXC_ATTR (implementation attributes)
   - SXC_ATTRT (implementation texts)
   - SXC_EXIT (BAdI definitions relationship)
   - SXC_CLASS (implementing classes)
   - SXS_ATTR (BAdI definition attributes)
   - SXS_ATTRT (BAdI definition texts)
   - SXS_INTER (BAdI interfaces)

**Estructura del response:**
```json
{
  "implementationName": "ZFIE1017_BADI_EXAMPLE",
  "header": {
    "implementationName": "...",
    "description": "...",
    "active": true,
    "devclass": "ZFIE",
    "author": "...",
    "createdOn": "20231015",
    "changedBy": "...",
    "changedOn": "20240520",
    "migrationEnhancement": ""
  },
  "badiDefinitions": [{
    "badiName": "BADI_SD_SALES",
    "description": "BAdI for Sales Document Processing",
    "filterValue": "0001",
    "interfaces": ["IF_EX_BADI_SD_SALES"],
    "isMultipleUse": true,
    "isFilterDependent": true
  }],
  "implementingClasses": [{
    "interfaceName": "IF_EX_BADI_SD_SALES",
    "className": "ZCL_IMPL_SD_SALES"
  }],
  "metadata": {
    "functionModule": "ZCX_UTIL_GET_BADI_IMPL",
    "objectType": "SXCI"
  }
}
```

### Fase 5: DMEE Tool (2-3 días) ✅ COMPLETADA

**Archivos creados:**
```
src/main/java/com/crystal/mcp/sapserver/
├── service/DmeeService.java              ✅
├── tool/DmeeTools.java                   ✅
└── model/DmeeTreeResult.java             ✅
```

**SAP:** FM `ZCX_UTIL_GET_DMEE_TREE` en `ZGFCX_1`
- ✅ ACTIVO en GIRAL (S/4HANA) - incluye campo `DMEEX`
- ✅ ACTIVO en GDC (ECC) - sin campo `DMEEX` (no existe en ECC)

**Tareas completadas:**
1. ✅ Implementar `DmeeService.java` usando FM custom via RfcAdapter.callFunctionModule()
2. ✅ Crear `DmeeTools.java` con tool `get_dmee_tree`
3. ✅ Crear model `DmeeTreeResult.java` con records:
   - `DmeeTreeHeader` (metadata del árbol: tipo, ID, versión, charset, autor)
   - `DmeeTreeNode` (nodos con mapping: source table/field, exit function, conversion rule)
4. ✅ FM `ZCX_UTIL_GET_DMEE_TREE` activo con consultas a:
   - DMEE_TREE (master data del árbol)
   - DMEE_TREE_T (textos/descripciones)
   - DMEE_TREE_HEAD (configuración de versión)
   - DMEE_TREE_NODE (nodos con propiedades de mapping)
   - DMEE_TREE_NODE_T (textos de nodos)

**Estructura del response:**
```json
{
  "treeType": "PAYM",
  "treeId": "ZFIE1017_CITIBANAMEX",
  "header": {
    "treeType": "PAYM",
    "treeId": "ZFIE1017_CITIBANAMEX",
    "description": "Formato Citibanamex México",
    "version": "001",
    "createdBy": "DEVELOPER",
    "createdOn": "20231015",
    "changedBy": "DEVELOPER",
    "changedOn": "20240520",
    "releaseFlag": "X",
    "paramStructure": "ZDMEE_PAYM_STRUCT",
    "charset": "UTF-8",
    "versionUser": "DEVELOPER",
    "versionDate": "20240520"
  },
  "nodes": [{
    "nodeId": "0001",
    "techName": "ROOT",
    "parentId": "",
    "nodeType": "ROOT",
    "level": 0,
    "text": "Root Node",
    "nodeComment": "",
    "length": 0,
    "dataType": "",
    "mappingConstant": "",
    "mappingSourceTable": "",
    "mappingSourceField": "",
    "mappingExitFunction": "",
    "conversionRule": ""
  }],
  "metadata": {
    "functionModule": "ZCX_UTIL_GET_DMEE_TREE",
    "objectType": "DMEE"
  }
}
```

---

## 16. Resumen de Entregables

### Java - Archivos Completados ✅

```
src/main/java/com/crystal/mcp/sapserver/
├── tool/
│   ├── CdsTools.java              # Fase 1 ✅
│   ├── EnhancementTools.java      # Fase 3 ✅
│   ├── BadiTools.java             # Fase 4 ✅
│   └── DmeeTools.java             # Fase 5 ✅
├── service/
│   ├── CdsService.java            # Fase 1 ✅
│   ├── EnhancementService.java    # Fase 3 ✅
│   ├── BadiService.java           # Fase 4 ✅
│   └── DmeeService.java           # Fase 5 ✅
└── model/
    ├── CdsSourceResult.java       # Fase 1 ✅
    ├── EnhancementSourceResult.java # Fase 3 ✅
    ├── BadiImplementationResult.java # Fase 4 ✅
    └── DmeeTreeResult.java        # Fase 5 ✅

src/test/java/com/crystal/mcp/sapserver/manual/
├── ManualCdsServiceTest.java      # Fase 1 ✅
└── ManualAbapRipperToolsTest.java # Fases 3,4,5 ✅
```

### SAP ABAP - FMs Completados ✅

```
ZCX_GET_ENHANCEMENT_SOURCE  # Fase 3 ✅ (ACTIVO en GDC)
ZCX_UTIL_GET_BADI_IMPL      # Fase 4 ✅ (ACTIVO en GDC)
ZCX_UTIL_GET_DMEE_TREE      # Fase 5 ✅ (ACTIVO en GIRAL S/4HANA + GDC ECC)
```

> **Nota**: El FM `ZCX_UTIL_GET_DMEE_TREE` tiene dos versiones:
> - **GIRAL (S/4HANA)**: Incluye campo `DMEEX` (formato mejorado S/4HANA)
> - **GDC (ECC)**: Sin campo `DMEEX` (no existe en sistemas ECC)

### Modificaciones Completadas ✅

```
SapPromptProvider.java      # Fase 2 ✅ (+5 prompts)
```

---

## 17. Estimación Revisada

| Fase | Contenido | Duración |
|------|-----------|----------|
| 1 | CDS Views (ADT, sin FM) | 1-2 días |
| 2 | 5 Prompts nuevos | 1 día |
| 3 | Enhancement + FM ABAP | 2-3 días |
| 4 | BAdI + FM ABAP | 2-3 días |
| 5 | DMEE + FM ABAP (opcional) | 2-3 días |
| **Total** | | **6-12 días** |

---

## Referencias

- Estructura ADT existente: `abap/` en este proyecto
- FM existente similar: `ZCX_GETDDICSOURCE` en `ZGFCX_1`
- Herramientas de extracción existentes: `extract_abap_components` tool

---

## Changelog

| Fecha | Versión | Cambios |
|-------|---------|---------|
| 2025-12-12 | 0.1.0 | Documento inicial con decisiones confirmadas |
| 2025-12-12 | 0.2.0 | Análisis de gap completado: FMs existentes, tools reutilizables |
| 2025-12-12 | 0.3.0 | **APROBADO**: Inventario 48 tools, gap corregido (~80% cobertura), plan 5 fases, 5 prompts nuevos, sin Resources |
| 2025-12-12 | 0.4.0 | **Fase 1 COMPLETADA**: `get_cds_source` tool implementado (CdsService, CdsTools, CdsSourceResult, ManualCdsServiceTest) |
| 2025-12-15 | 0.5.0 | **Fase 2 COMPLETADA**: 5 prompts nuevos implementados en SapPromptProvider.java (analyze_package_for_extraction, generate_extraction_report, explain_cds_view, explain_enhancement, evaluate_code_quality_batch) |
| 2025-12-15 | 0.6.0 | **Fase 3 COMPLETADA**: `get_enhancement_source` tool implementado (EnhancementService, EnhancementTools, EnhancementSourceResult). FM `ZCX_GET_ENHANCEMENT_SOURCE` activo en GDC. Cobertura ~90% |
| 2025-12-15 | 0.7.0 | **Fase 4 COMPLETADA**: `get_badi_implementation` tool implementado (BadiService, BadiTools, BadiImplementationResult). FM `ZCX_UTIL_GET_BADI_IMPL` activo en GDC. Cobertura ~95% |
| 2025-12-15 | 0.8.0 | **Fase 5 COMPLETADA**: `get_dmee_tree` tool implementado (DmeeService, DmeeTools, DmeeTreeResult). FM `ZCX_UTIL_GET_DMEE_TREE` activo en GIRAL (S/4HANA) y GDC (ECC con adaptación sin DMEEX). Test consolidado ManualAbapRipperToolsTest.java. **PROYECTO COMPLETADO** - Cobertura ~100% |


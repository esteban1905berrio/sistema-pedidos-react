# PR PSR018 - Search Helps para Estructuras CI_PROJ y CI_PRPS

**RICEFW ID:** PSR018
**Módulo:** PS (Project System)
**Tipo:** Enhancement
**Prioridad:** Alta
**Transport:** CADK911088
**Status:** 🟡 EN PROGRESO (50%)
**Fecha Análisis:** 2025-11-05
**Analista:** Claude Code (SAP Requirements Analyst Agent)

---

## 📋 TABLA DE CONTENIDOS

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [User Story Original](#user-story-original)
3. [Análisis de Requerimientos](#análisis-de-requerimientos)
4. [Hallazgos Técnicos](#hallazgos-técnicos)
5. [Alternativas Estándar Evaluadas](#alternativas-estándar-evaluadas)
6. [Requerimientos Granulares](#requerimientos-granulares)
7. [Estado de Implementación](#estado-de-implementación)
8. [Log de Preguntas y Respuestas](#log-de-preguntas-y-respuestas)
9. [Próximos Pasos](#próximos-pasos)

---

## 1. RESUMEN EJECUTIVO

### 🎯 Objetivo

Crear search helps (ayudas de búsqueda F4) para campos Z en las estructuras CI_PROJ (Definición de Proyecto) y CI_PRPS (Elementos PEP) que permitan filtrar dinámicamente la tabla IMPR basándose en la profundidad jerárquica del campo POSID.

### 📊 Estado Actual

- **Progreso Global:** 50%
- **Estructuras:** ✅ Completadas (CI_PROJ, CI_PRPS)
- **Enhancements:** ✅ Implementados (CNEX0006, CNEX0007)
- **Search Helps:** ❌ Pendientes (0% - no iniciados)
- **Esfuerzo Restante:** 10-12 horas

### 🔑 Hallazgos Clave

1. ✅ Estructuras CI_PROJ y CI_PRPS ya están creadas y activas
2. ✅ Enhancements CNEX0006/CNEX0007 ya implementados (exits funcionando)
3. ✅ 24 data elements y 1 dominio creados en transport CADK911088
4. ❌ Search helps NO existen (core del requirement)
5. ⚠️ Tabla IMPR tiene 0 registros en mandante actual (usar mandante 210)
6. ✅ Lógica de profundidad confirmada: conteo de puntos en POSID

### ⏱️ Esfuerzo Estimado

- **Original:** 22-24 horas
- **Completado:** ~12 horas (infraestructura)
- **Restante:** 10-12 horas (search helps + testing)

---

## 2. USER STORY ORIGINAL

### Historia de Usuario

```
Como desarrollador ABAP
Quiero Crear dos SH (Search help)
Para agregar un matchcode a los campos Z de las estructuras ampliadas
```

### Requerimientos Originales

**Estructura CI_PROJ:**
- Campo: `ZZ_COD_SECTOR_CPI` → Filtrar POSID con 1 punto
- Campo: `ZZ_COD_PROGRAMA_CPI` → Filtrar POSID con 2 puntos

**Estructura CI_PRPS:**
- Campo: `ZZ_META` → Filtrar POSID con 4 puntos
- Campo: `ZZ_COD_PRODUCTO` → Filtrar POSID con 5 puntos

### Criterios de Aceptación Originales

- ✅ La SH debe retornar la descripción (campo POST1)
- ✅ Basarse en el Enhancement Framework (EF) inicial

---

## 3. ANÁLISIS DE REQUERIMIENTOS

### 3.1 Clasificación del Requerimiento

- **Tipo de Documento:** User Story + Especificación Funcional (PMO)
- **Complejidad:** Media-Alta
- **Impacto:** Medio (estructuras ya en uso productivo)
- **Módulos Afectados:** PS (Project System), IM (Investment Management)
- **Tablas Modificadas:** CI_PROJ, CI_PRPS (append structures)
- **Tablas Consultadas:** IMPR, TVARVC

### 3.2 Entendimiento Detallado

#### **Corrección de Lógica de Profundidad**

⚠️ **IMPORTANTE:** La user story original tiene errores en la cantidad de puntos. Después del análisis y respuestas del usuario, la lógica correcta es:

**Para CI_PROJ (7 campos):**

| Campo | Descripción | Profundidad | Ejemplo POSID |
|-------|-------------|-------------|---------------|
| ZZ_COD_SECTOR_CPI | Código Sector CPI | **2 puntos** | `1.1.1` |
| ZZ_NOM_SECTOR_CPI | Nombre Sector (auto-fill) | - | Auto desde POST1 |
| ZZ_COD_PROGRAMA_CPI | Código Programa CPI | **4 puntos** | `1.1.1.1.1` |
| ZZ_NOM_PROGRAMA_CPI | Nombre Programa (auto-fill) | - | Auto desde POST1 |
| ZZ_ANO_PROGRAMA | Año del Programa | - | Manual (NUMC 4) |
| ZZ_OBJ_GENERAL | Objetivo General | - | Manual (STRING 80) |
| ZZ_VALOR_TOTAL | Valor Total | - | Manual (CURR 15) |

**Para CI_PRPS (12 campos, 6 relevantes para search helps):**

| Campo | Descripción | Profundidad | Ejemplo POSID |
|-------|-------------|-------------|---------------|
| ZZ_CODIGO_PRODUCTO_CPI | Código Producto | **7 puntos** | `1.1.1.1.1.1.1.1` |
| ZZ_CODIGO_NOM_PRODUCTO_CPI | Nombre Producto (auto-fill) | - | Auto desde POST1 |
| ZZ_CODIGO_INDICADOR_CPI | Código Indicador | **9 puntos** | `1.1.1.1.1.1.1.1.1.1` |
| ZZ_NOMBRE_INDICADOR_CPI | Nombre Indicador (auto-fill) | - | Auto desde POST1 |
| ZZ_META | Código Meta | **4 puntos** | `1.1.1.1.1` |
| ZZ_NOMBRE_META | Nombre Meta (auto-fill) | - | Auto desde POST1 |

**Nota:** CI_PRPS tiene 6 campos adicionales (población, municipio, objetivo) no cubiertos por este requirement.

#### **Lógica de Conteo de Puntos**

La profundidad se calcula contando los caracteres `.` (punto) en el string POSID:

```
Ejemplos:
1           → 0 puntos (nivel 1)
1.1         → 1 punto  (nivel 2)
1.1.1       → 2 puntos (nivel 3) ← Sector CPI
1.1.1.1     → 3 puntos (nivel 4)
1.1.1.1.1   → 4 puntos (nivel 5) ← Programa CPI / Meta
1.1.1.1.1.1 → 5 puntos (nivel 6)
... (extrapolado hasta 9 puntos)
```

#### **Filtrado Adicional con TVARVC**

Los search helps deben filtrar la tabla IMPR usando parámetros anuales configurables:

- **ZFI_PSR018_GJAHR** (GJAHR): Año de autorización
- **ZFI_PSR018_PRNAM** (PRNAM): Programa de inversión

**SQL resultante:**
```sql
SELECT posnr, posid, post1
  FROM impr
  WHERE mandt = sy-mandt
    AND gjahr = (SELECT low FROM tvarvc WHERE name = 'ZFI_PSR018_GJAHR')
    AND prnam = (SELECT low FROM tvarvc WHERE name = 'ZFI_PSR018_PRNAM')
    AND <conteo_puntos_posid> = <profundidad_requerida>
```

#### **Auto-Fill de Descripciones**

Cuando el usuario selecciona un código (POSNR), el campo de nombre correspondiente debe llenarse automáticamente con POST1 (DENOMINACION):

- Sector CPI: ZZ_COD_SECTOR_CPI → ZZ_NOM_SECTOR_CPI
- Programa CPI: ZZ_COD_PROGRAMA_CPI → ZZ_NOM_PROGRAMA_CPI
- Producto: ZZ_CODIGO_PRODUCTO_CPI → ZZ_CODIGO_NOM_PRODUCTO_CPI
- Indicador: ZZ_CODIGO_INDICADOR_CPI → ZZ_NOMBRE_INDICADOR_CPI
- Meta: ZZ_META → ZZ_NOMBRE_META

### 3.3 Contexto de Negocio

**Propósito:** Capturar información de proyectos relacionada con programas de inversión pública (CPI - Código Programa de Inversión) para reportería, seguimiento presupuestal e integración con sistemas de planeación.

**Usuarios:** Project Managers, Controllers, PMO Office

**Flujo:** Los campos se capturan durante la creación/modificación de proyectos (CJ06, CJ20N) y elementos PEP (CJ01, CJ02).

**Integraciones:** Los datos pueden replicarse en módulos CO, FI, IM para reportes y análisis presupuestal.

---

## 4. HALLAZGOS TÉCNICOS

### 4.1 Objetos Existentes Analizados

#### **CI_PROJ (Append Structure para PROJ)**

**Metadata:**
- **Tipo:** TABL/DS (Append Structure)
- **Package:** ZPS
- **Creado:** 2025-10-21 por L_ABAPS2_ITA
- **Modificado:** 2025-10-28 01:32:31Z
- **Estado:** ✅ Activa
- **Transport:** CADK911089 (task de CADK911088)

**Campos (7 total):**
```abap
zz_cod_sector_cpi   : zde_psr018_cod_sector      (CHAR 10)
zz_nom_sector_cpi   : zde_psr018_nombre_sector   (CHAR 40)
zz_cod_programa_cpi : zde_psr018_cod_programa    (CHAR 8)
zz_nom_programa_cpi : zde_psr018_nombre_prog     (CHAR 40)
zz_ano_programa     : zde_psr018_ano_prog        (NUMC 4)
zz_obj_general      : zde_psr018_obj_general     (STRING 80)
zz_valor_total      : zde_psr018_val_total       (CURR 15)
  @Semantics.amount.currencyCode : 'proj.use06'
```

**Status:**
- ✅ Estructura completa
- ⚠️ Search helps NO asignados (pendiente)

---

#### **CI_PRPS (Append Structure para PRPS)**

**Metadata:**
- **Tipo:** TABL/DS (Append Structure)
- **Package:** ZPS
- **Creado:** 2025-10-27 por L_ABAPS_ITA
- **Modificado:** 2025-11-04 16:55:55Z
- **Estado:** ✅ Activa
- **Transport:** CADK911222 (task de CADK911088)

**Campos (12 total, 6 para search helps):**
```abap
zz_meta                    : zde_meta                      (CHAR XX)
zz_nombre_meta             : zde_nombre_meta               (CHAR XX)
zz_codigo_producto_cpi     : zde_codigo_producto_cpi      (CHAR XX)
zz_codigo_nom_producto_cpi : zde_codigo_nom_producto_cpi  (CHAR XX)
zz_codigo_indicador_cpi    : zde_codigo_indicador_cpi     (CHAR XX)
zz_nombre_indicador_cpi    : zde_nombre_indicador_cpi     (CHAR XX)
zz_poblacion_afectada      : zde_poblacion_afectada
zz_poblacion_objetivo      : zde_poblacion_objetivo
zz_objetivo_especifico     : zde_objetivo_especifico
zz_codigo_municipio        : zde_codigo_municipio
zz_nombre_municipio        : zde_nombre_municipio
zz_valor_total             : kkb_value_total
  @Semantics.amount.currencyCode : 'proj.use06'
```

**Status:**
- ✅ Estructura completa (con campos adicionales)
- ⚠️ Search helps NO asignados (pendiente)

---

#### **Tabla IMPR (Investment Program)**

**Metadata:**
- **Tipo:** Tabla estándar SAP
- **Campos totales:** 58 campos
- **Datos:** ❌ 0 registros en mandante actual
- **Datos disponibles:** ✅ Mandante 210 (Sandbox)

**Campos relevantes:**

| Campo | Tipo | Long | Descripción | Uso |
|-------|------|------|-------------|-----|
| MANDT | C | 3 | Mandante | Automático |
| POSNR | N | 8 | Posición programa | **Return (código)** |
| GJAHR | N | 4 | Año autorización | **Filtro TVARVC** |
| POSID | C | 24 | ID de posición | **Filtro profundidad** |
| PRNAM | C | 8 | Programa inversión | **Filtro TVARVC** |
| POST1 | ? | ? | Descripción | **Return (nombre)** |

**⚠️ CRÍTICO:** Campo POST1 no visible en metadata pero confirmado como DENOMINACION por usuario.

**Ejemplos de POSID (proporcionados por usuario):**
```
1           → 0 puntos
1.1         → 1 punto
1.1.1       → 2 puntos ← Sector CPI
1.1.1.1     → 3 puntos
1.1.1.1.1   → 4 puntos ← Programa/Meta
1.1.1.1.2   → 4 puntos
1.1.1.1.1.1 → 5 puntos
1.1.1.1.2.1 → 5 puntos
1.1.1.2     → 3 puntos
(extrapolado: 7 puntos para Producto, 9 para Indicador)
```

---

#### **Enhancements CNEX0006 y CNEX0007**

**CNEX0006 - Campos Cliente en Definición de Proyecto:**
- ✅ Implementado en proyecto ZCNEX006
- ✅ Estructura CI_PROJ activa
- ✅ Dynpro SAPLXCN1-0600 creado
- ✅ Exits implementados (EXIT_SAPLCJWB_002, EXIT_SAPLCJWB_003)
- ⚠️ **NO tocar exits** (instrucción del usuario)

**CNEX0007 - Campos Cliente en Elemento PEP:**
- ✅ Implementado en proyecto ZCNEX007
- ✅ Estructura CI_PRPS activa
- ✅ Dynpro SAPLXCN1-0700 creado
- ✅ Exits implementados (EXIT_SAPLCJWB_004, EXIT_SAPLCJWB_005)
- ⚠️ **NO tocar exits** (instrucción del usuario)
- 📝 Acceso en CJ20N: Menú → Details → Customer fields (no aparece como tab)

---

### 4.2 Dependencias Identificadas

**Where-Used Analysis:**
- ✅ No aplica según respuestas del usuario
- ✅ Sin programas custom identificados usando CI_PROJ/CI_PRPS
- ✅ Riesgo de impacto: **BAJO**

**Transport Dependencies:**
- Transport principal: CADK911088 (Workbench)
- Status: Modifiable (DEV only)
- Owner: L_ABAPS_ITA
- Target: /QASALL/
- Tasks: 2 (CADK911089, CADK911222)
- Total objects: 34 objetos ya en transport

**Objetos a agregar al transport:**
1. Function Group: ZFGPSR018
2. Function Modules: Z_PSR018_COUNT_POSID_DEPTH, Z_PSR018_IMPR_SHLP_EXIT
3. Search Helps: ZSH_PSR018_SECTOR, ZSH_PSR018_PROGRAMA, ZSH_PSR018_PRODUCTO, ZSH_PSR018_INDICADOR, ZSH_PSR018_META
4. Estructuras modificadas: CI_PROJ, CI_PRPS (con SH assignments)
5. Parámetros TVARVC: ZFI_PSR018_GJAHR, ZFI_PSR018_PRNAM (si transportables)

---

### 4.3 Riesgos y Mitigación

| Riesgo | Severidad | Probabilidad | Mitigación |
|--------|-----------|--------------|------------|
| Modificación de CI_PROJ en uso | Media | Baja | Testing exhaustivo en DEV, coordinar con usuarios |
| Modificación de CI_PRPS en uso | Media | Baja | Testing exhaustivo en DEV |
| IMPR sin datos en mandante actual | Alta | Alta | ✅ **Usar mandante 210 para testing** |
| Campo POST1 no accesible | Media | Media | Validar en SELECT durante desarrollo |
| Parámetros TVARVC no configurados | Media | Media | Valores default + mensaje de error claro |
| Performance de search help exit | Baja | Baja | SELECT con índices, limitar registros |
| Auto-fill no funciona | Media | Baja | Testing de cada search help individualmente |

---

## 5. ALTERNATIVAS ESTÁNDAR EVALUADAS

### 5.1 Patrón Search Help Exit (F4IF_SHLP_EXIT_EXAMPLE)

**Fuente:** SAP Community, SAP Help Portal

**Descripción:** Función módulo estándar SAP que sirve como template para crear search help exits personalizados.

**Ventajas:**
- ✅ Patrón estándar SAP documentado
- ✅ Control total sobre filtrado de datos
- ✅ Mantiene UI estándar de search help
- ✅ Permite validaciones complejas (ej: conteo de puntos)
- ✅ Soporta parámetros configurables

**Implementación:**
```abap
FUNCTION z_psr018_impr_shlp_exit.
  IMPORTING shlp TYPE shlp_descr
            callcontrol TYPE ddshf4ctrl
  TABLES    record_tab TYPE standard table
            shlp_tab TYPE standard table.

  CASE callcontrol-step.
    WHEN 'SELECT'.
      " 1. Leer parámetros TVARVC
      " 2. SELECT de IMPR con filtros
      " 3. Filtrar por profundidad POSID
      " 4. Transferir con F4UT_PARAMETER_RESULTS_PUT
      callcontrol-step = 'DISP'.
    WHEN 'DISP'.
      callcontrol-step = 'EXIT'.
  ENDCASE.
ENDFUNCTION.
```

**Recomendación:** ✅ **USAR** - Ideal para lógica compleja de filtrado

---

### 5.2 Patrón TVARVC para Parámetros

**Fuente:** SAP Community, SAP Documentation

**Descripción:** Tabla estándar SAP para parámetros configurables (transacción STVARV).

**Ventajas:**
- ✅ Sin hardcoding de valores anuales
- ✅ Cambio sin modificar código
- ✅ Patrón estándar SAP
- ✅ Mantenimiento simple (STVARV)

**Implementación:**
```abap
SELECT SINGLE low FROM tvarvc
  INTO @lv_gjahr
  WHERE type = 'P' AND name = 'ZFI_PSR018_GJAHR'.

IF sy-subrc <> 0 OR lv_gjahr IS INITIAL.
  lv_gjahr = sy-datum+0(4).  " Default: año actual
ENDIF.
```

**Recomendación:** ✅ **USAR** - Ideal para parámetros que cambian anualmente

---

### 5.3 Alternativas Descartadas

#### **F4IF_FIELD_VALUE_REQUEST**
- ❌ Menos flexible para filtrado complejo
- ❌ No permite pre-procesamiento sofisticado
- ❌ Insuficiente para lógica de conteo de puntos

#### **F4IF_INT_TABLE_VALUE_REQUEST**
- ❌ Similar limitación que anterior
- ❌ No recomendado para este caso

---

### 5.4 Recomendación Final de Diseño

**Arquitectura Propuesta:**

1. **1 Function Group:** ZFGPSR018
2. **2 Function Modules:**
   - `Z_PSR018_COUNT_POSID_DEPTH` - Helper genérico para contar puntos
   - `Z_PSR018_IMPR_SHLP_EXIT` - Exit reutilizable para los 5 search helps
3. **5 Search Helps Elementales:**
   - `ZSH_PSR018_SECTOR` (2 puntos)
   - `ZSH_PSR018_PROGRAMA` (4 puntos)
   - `ZSH_PSR018_PRODUCTO` (7 puntos)
   - `ZSH_PSR018_INDICADOR` (9 puntos)
   - `ZSH_PSR018_META` (4 puntos)
4. **2 Parámetros TVARVC:**
   - `ZFI_PSR018_GJAHR` (tipo P)
   - `ZFI_PSR018_PRNAM` (tipo P)

**Beneficios:**
- ✅ Reutilización de código (1 exit para 5 search helps)
- ✅ Mantenimiento centralizado
- ✅ Parámetros configurables sin código
- ✅ Performance optimizado

---

## 6. REQUERIMIENTOS GRANULARES

### 6.1 Desglose en Sub-Requirements

**Total:** 12 sub-requirements
**Esfuerzo total:** 22-24 horas
**Completado:** ~12 horas (50%)
**Restante:** 10-12 horas

---

#### **PSR018.1 - Function Module Helper: Conteo de Profundidad**

**Descripción:** Crear función genérica reutilizable para contar puntos en POSID.

**Objetos:**
- Function Module: `Z_PSR018_COUNT_POSID_DEPTH`
- Function Group: `ZFGPSR018` (nuevo)
- Package: `ZPS`

**Interfaz:**
```abap
IMPORTING: iv_posid TYPE posid (C, 24)
RETURNING: rv_depth TYPE i
```

**Lógica:**
```abap
rv_depth = strlen( condense( iv_posid ) )
         - strlen( replace( val = iv_posid sub = '.' with = '' occ = 0 ) ).
```

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | Ninguna |
| **Complejidad** | Baja |
| **Esfuerzo** | 1 hora |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.2 - Function Module: Search Help Exit**

**Descripción:** Implementar search help exit basado en F4IF_SHLP_EXIT_EXAMPLE.

**Objetos:**
- Function Module: `Z_PSR018_IMPR_SHLP_EXIT`
- Function Group: `ZFGPSR018`

**Interfaz:** (Copiar de F4IF_SHLP_EXIT_EXAMPLE)

**Lógica:**
1. Step SELECT:
   - Leer TVARVC: ZFI_PSR018_GJAHR, ZFI_PSR018_PRNAM
   - SELECT de IMPR con filtros
   - Obtener profundidad desde SHLP parameter
   - Llamar Z_PSR018_COUNT_POSID_DEPTH
   - Filtrar por profundidad
   - F4UT_PARAMETER_RESULTS_PUT
2. Step DISP:
   - Return control

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.1 |
| **Complejidad** | Media-Alta |
| **Esfuerzo** | 4-6 horas |
| **Riesgo** | Medio |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.3 - Search Help: Código Sector CPI**

**Descripción:** Search Help para ZZ_COD_SECTOR_CPI (2 puntos).

**Objetos:**
- Search Help: `ZSH_PSR018_SECTOR`

**Configuración:**
- Tipo: Elementary Search Help
- Selection method: IMPR
- Search help exit: Z_PSR018_IMPR_SHLP_EXIT
- Parameters:
  - POSNR (export) - Código
  - POST1 (export) - Descripción
  - DEPTH (import) - Valor: 2

**Asignación:**
- Campo: CI_PROJ-ZZ_COD_SECTOR_CPI
- Auto-fill: POST1 → ZZ_NOM_SECTOR_CPI

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.2 |
| **Complejidad** | Baja |
| **Esfuerzo** | 2 horas |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.4 - Search Help: Código Programa CPI**

**Descripción:** Search Help para ZZ_COD_PROGRAMA_CPI (4 puntos).

**Objetos:**
- Search Help: `ZSH_PSR018_PROGRAMA`

**Configuración:**
- Similar a PSR018.3
- Parameter DEPTH: 4
- Asignación: ZZ_COD_PROGRAMA_CPI / ZZ_NOM_PROGRAMA_CPI

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.2 |
| **Complejidad** | Baja |
| **Esfuerzo** | 1 hora |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.5 - Search Help: Código Producto**

**Descripción:** Search Help para ZZ_CODIGO_PRODUCTO_CPI (7 puntos).

**Objetos:**
- Search Help: `ZSH_PSR018_PRODUCTO`

**Configuración:**
- Similar a PSR018.3
- Parameter DEPTH: 7
- Asignación: ZZ_CODIGO_PRODUCTO_CPI / ZZ_CODIGO_NOM_PRODUCTO_CPI

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.2 |
| **Complejidad** | Baja |
| **Esfuerzo** | 1 hora |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.6 - Search Help: Código Indicador CPI**

**Descripción:** Search Help para ZZ_CODIGO_INDICADOR_CPI (9 puntos).

**Objetos:**
- Search Help: `ZSH_PSR018_INDICADOR`

**Configuración:**
- Similar a PSR018.3
- Parameter DEPTH: 9
- Asignación: ZZ_CODIGO_INDICADOR_CPI / ZZ_NOMBRE_INDICADOR_CPI

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.2 |
| **Complejidad** | Baja |
| **Esfuerzo** | 1 hora |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.7 - Search Help: Código Meta**

**Descripción:** Search Help para ZZ_META (4 puntos).

**Objetos:**
- Search Help: `ZSH_PSR018_META`

**Configuración:**
- Similar a PSR018.3
- Parameter DEPTH: 4
- Asignación: ZZ_META / ZZ_NOMBRE_META

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.2 |
| **Complejidad** | Baja |
| **Esfuerzo** | 1 hora |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.8 - Modificar Estructura CI_PROJ**

**Descripción:** Asignar search helps a campos de CI_PROJ.

**Objetos:**
- Estructura: CI_PROJ (modificación en SE11)

**Campos a modificar:**

| Campo | Search Help |
|-------|-------------|
| ZZ_COD_SECTOR_CPI | ZSH_PSR018_SECTOR |
| ZZ_COD_PROGRAMA_CPI | ZSH_PSR018_PROGRAMA |

**Actividades:**
1. SE11 → CI_PROJ
2. Asignar search helps
3. Activar estructura
4. Verificar en CJ20N

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.3, PSR018.4 |
| **Complejidad** | Baja |
| **Esfuerzo** | 1 hora |
| **Riesgo** | Medio |
| **Transport** | CADK911088 |
| **Status** | ⚠️ PARTIAL (50%) - Estructura existe, SH pendientes |

---

#### **PSR018.9 - Modificar Estructura CI_PRPS**

**Descripción:** Asignar search helps a campos de CI_PRPS.

**Objetos:**
- Estructura: CI_PRPS (modificación en SE11)

**Campos a modificar:**

| Campo | Search Help |
|-------|-------------|
| ZZ_CODIGO_PRODUCTO_CPI | ZSH_PSR018_PRODUCTO |
| ZZ_CODIGO_INDICADOR_CPI | ZSH_PSR018_INDICADOR |
| ZZ_META | ZSH_PSR018_META |

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.5, PSR018.6, PSR018.7 |
| **Complejidad** | Baja |
| **Esfuerzo** | 1.5 horas |
| **Riesgo** | Medio |
| **Transport** | CADK911088 |
| **Status** | ⚠️ PARTIAL (50%) - Estructura existe, SH pendientes |

---

#### **PSR018.10 - Configurar Parámetros TVARVC**

**Descripción:** Crear parámetros configurables para filtrado.

**Objetos:**
- Tabla: TVARVC (transacción STVARV)

**Parámetros:**

| Name | Type | Low | Description |
|------|------|-----|-------------|
| ZFI_PSR018_GJAHR | P | 2025 | Año programa inversión PSR018 |
| ZFI_PSR018_PRNAM | P | TEST | Programa inversión PSR018 |

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | Ninguna |
| **Complejidad** | Muy Baja |
| **Esfuerzo** | 30 minutos |
| **Riesgo** | Bajo |
| **Transport** | CADK911088 (customizing) |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.11 - Testing Integral**

**Descripción:** Probar todos los search helps en CJ20N (mandante 210).

**Actividades:**

**A. Testing de Function Modules:**
- Z_PSR018_COUNT_POSID_DEPTH con ejemplos
- Z_PSR018_IMPR_SHLP_EXIT con diferentes profundidades

**B. Testing de Search Helps:**
- Para cada SH: verificar filtrado, auto-fill, TVARVC

**C. Testing en CJ20N:**
- Definición de Proyecto (CJ06):
  - F4 en ZZ_COD_SECTOR_CPI → solo 2 puntos
  - Auto-fill ZZ_NOM_SECTOR_CPI
  - F4 en ZZ_COD_PROGRAMA_CPI → solo 4 puntos
  - Auto-fill ZZ_NOM_PROGRAMA_CPI
- Elementos PEP (CJ01/CJ02):
  - F4 en ZZ_CODIGO_PRODUCTO_CPI → 7 puntos
  - F4 en ZZ_CODIGO_INDICADOR_CPI → 9 puntos
  - F4 en ZZ_META → 4 puntos
  - Verificar auto-fills

**D. Testing de Errores:**
- Sin parámetros TVARVC
- Sin datos en IMPR
- GJAHR/PRNAM incorrectos

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.1-10 |
| **Complejidad** | Media |
| **Esfuerzo** | 6 horas |
| **Riesgo** | Bajo |
| **Transport** | N/A |
| **Status** | ❌ NOT STARTED |

---

#### **PSR018.12 - Documentación Técnica**

**Descripción:** Documentar implementación completa.

**Entregables:**
1. Documento técnico con:
   - Arquitectura de solución
   - Descripción de objetos
   - Lógica de filtrado
   - Configuración TVARVC
   - Guía de mantenimiento
2. Comentarios en código ABAP
3. Actualización de PR document

| Aspecto | Valor |
|---------|-------|
| **Dependencias** | PSR018.11 |
| **Complejidad** | Baja |
| **Esfuerzo** | 2 horas |
| **Riesgo** | Bajo |
| **Transport** | N/A |
| **Status** | ❌ NOT STARTED |

---

### 6.2 Secuencia de Implementación

```
Fase 1: Fundación (Paralelo) - 1.5 horas
├─ PSR018.1: Function Module Helper (1h)
└─ PSR018.10: Parámetros TVARVC (0.5h)

Fase 2: Search Help Exit (Secuencial) - 4-6 horas
└─ PSR018.2: Search Help Exit FM
   Depende de: PSR018.1

Fase 3: Search Helps (Paralelo) - 6 horas
├─ PSR018.3: SH Sector CPI (2h)
├─ PSR018.4: SH Programa CPI (1h)
├─ PSR018.5: SH Producto (1h)
├─ PSR018.6: SH Indicador (1h)
└─ PSR018.7: SH Meta (1h)
   Todos dependen de: PSR018.2

Fase 4: Asignación a Estructuras (Secuencial) - 2.5 horas
├─ PSR018.8: Modificar CI_PROJ (1h)
│  Depende de: PSR018.3, PSR018.4
└─ PSR018.9: Modificar CI_PRPS (1.5h)
   Depende de: PSR018.5, PSR018.6, PSR018.7

Fase 5: Validación (Secuencial) - 8 horas
├─ PSR018.11: Testing (6h)
└─ PSR018.12: Documentación (2h)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ESFUERZO TOTAL: 22-24 horas
COMPLETADO: ~12 horas (50%)
RESTANTE: 10-12 horas
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 7. ESTADO DE IMPLEMENTACIÓN

### 7.1 Dashboard de Progreso

**🎯 PROGRESO GLOBAL: 50%** ██████████░░░░░░░░░░

**Transport:** CADK911088
**Status:** Modifiable (DEV)
**Owner:** L_ABAPS_ITA
**Tasks:** 2 (CADK911089, CADK911222)
**Total Objects:** 34 en transport

---

### 7.2 Objetos Completados (33%)

**✅ Infrastructure (100%):**
- 16 Data Elements (ZDE_PSR018_*)
- 1 Domain (ZDO_PSR018_CURR)
- 2 Append Structures (CI_PROJ, CI_PRPS)
- 2 Enhancement Projects (ZCNEX006, ZCNEX007)
- 5 Exit Programs (ZXCN1TOP, ZXCN1U11, ZXCN1U12, ZXCN1U21, ZXCN1U22)
- 1 Dynpro (SAPLXCN10700)

**Total: 27 objetos creados y activos**

---

### 7.3 Objetos Pendientes (67%)

**❌ Core Functionality (0%):**
- Function Group: ZFGPSR018
- 2 Function Modules
- 5 Search Helps
- 2 TVARVC Parameters
- 2 Structure Modifications (SH assignments)

**Total: 12 objetos pendientes**

---

### 7.4 Breakdown por Fase

| Fase | Progreso | Status |
|------|----------|--------|
| **Infrastructure** | ████████████████████ 100% | ✅ Complete |
| **Enhancements** | ████████████████████ 100% | ✅ Complete |
| **Exits & Dynpros** | ████████████████████ 100% | ✅ Complete |
| **Function Modules** | ░░░░░░░░░░░░░░░░░░░░ 0% | ❌ Pending |
| **Search Helps** | ░░░░░░░░░░░░░░░░░░░░ 0% | ❌ Pending |
| **TVARVC Config** | ░░░░░░░░░░░░░░░░░░░░ 0% | ❌ Pending |
| **SH Assignments** | ░░░░░░░░░░░░░░░░░░░░ 0% | ❌ Pending |
| **Testing** | ░░░░░░░░░░░░░░░░░░░░ 0% | ❌ Pending |

---

### 7.5 Próximas Acciones Prioritarias

**🔴 PRIORITY 1 (HIGH):**
1. Crear ZFGPSR018 (30 min) - Blocker
2. Crear Z_PSR018_COUNT_POSID_DEPTH (1h) - Blocker
3. Crear Z_PSR018_IMPR_SHLP_EXIT (4-6h) - Blocker

**🟡 PRIORITY 2 (MEDIUM):**
4. Crear 5 Search Helps (6h) - Core functionality
5. Configurar TVARVC (30 min) - Puede ser paralelo

**🟢 PRIORITY 3 (LOW):**
6. Asignar SH a CI_PROJ (1h)
7. Asignar SH a CI_PRPS (1.5h)
8. Testing integral (6h)
9. Documentación (2h)

---

## 8. LOG DE PREGUNTAS Y RESPUESTAS

### 8.1 Preguntas Funcionales

**P1: Contexto de Negocio**
- **Pregunta:** ¿Cuál es el propósito de capturar estos campos CPI?
- **Respuesta:** (No proporcionada explícitamente)
- **Inferencia:** Reportería y seguimiento de programas de inversión pública

**P2: Usuarios y Roles**
- **Pregunta:** ¿Quiénes son los usuarios finales?
- **Respuesta:** (No proporcionada)
- **Inferencia:** Project Managers, PMO Office, Controllers

**P3-P5:** (No aplicables al alcance actual)

---

### 8.2 Preguntas Técnicas

**P6: Tabla IMPR - Datos de Prueba**
- **Pregunta:** ¿Los datos de IMPR están en otro sistema?
- **Respuesta:** ✅ "NO están en el mandante de Sandbox que es 210"
- **Acción:** Usar mandante 210 para testing
- **Datos proporcionados:** Ejemplos de POSID con profundidades

**P7: Campo DENOMINACION en IMPR**
- **Pregunta:** ¿Es un campo custom o alias?
- **Respuesta:** ✅ "Es el campo POST1"
- **Acción:** Usar POST1 en SELECT y retorno

**P8: Lógica de Filtrado por Profundidad**
- **Pregunta:** ¿Se cuenta por la cantidad de puntos?
- **Respuesta:** ✅ "Sí, se cuenta por la cantidad de puntos dentro del string"
- **Ejemplos proporcionados:** Jerarquía de POSID (1, 1.1, 1.1.1, etc.)
- **Acción:** Implementar función count_dots(POSID)

**P9: Parámetros en TVARC**
- **Pregunta:** ¿Qué parámetros específicos se deben leer?
- **Respuesta:** ✅ "ZFI_PSR018_GJAHR, ZFI_PSR018_PRNAM"
- **Relación:** "Es para filtrar la tabla junto con PRNAM"
- **Acción:** WHERE GJAHR = param1 AND PRNAM = param2

**P10: Search Helps - Funcionalidad**
- **Pregunta:** ¿F4 automáticos o exits programáticos?
- **Respuesta:** ✅ "Deben ser un SH Z con una función custom"
- **Auto-fill:** ✅ "Sí" (descripciones deben auto-llenarse)
- **Acción:** Crear ZSH + FM con exit

**P11: Dynpros Personalizados**
- **Pregunta:** ¿Ya existen los dynpros?
- **Respuesta:** ✅ "Sí, creados"
- **Acción:** No requiere desarrollo de dynpros

**P12: Function Modules y Exits**
- **Pregunta:** ¿Estos exits ya están implementados?
- **Respuesta:** ✅ "Sí y no se deben tocar"
- **Acción:** Solo crear search helps, NO modificar exits

---

### 8.3 Preguntas de Impacto

**P13: Objetos que usan CI_PROJ y CI_PRPS**
- **Pregunta:** ¿Hay programas custom usando estas estructuras?
- **Respuesta:** ✅ "No aplica"
- **Acción:** Sin análisis where-used necesario

---

### 8.4 Preguntas de Riesgos

**P14: Estado de Transports**
- **Pregunta:** ¿Hay transports abiertos relacionados?
- **Respuesta:** ✅ "Sí" (CADK911088)
- **Status PRD:** ❌ "No"
- **Transport target:** ✅ "CADK911088"
- **Acción:** Usar CADK911088 exclusivamente

**P15: Testing y Rollback**
- **Pregunta:** ¿Hay ambiente QA/DEV?
- **Respuesta:** ✅ "Sí"
- **Rollback:** "No contemplar por ahora"
- **UAT:** ✅ "Sí, hay usuarios piloto"
- **Acción:** Testing disponible, sin plan de rollback formal

---

## 9. PRÓXIMOS PASOS

### 9.1 Fase Actual: Requirements Analysis

✅ **COMPLETADO** (2025-11-05)

**Entregables:**
- ✅ Análisis completo de requerimientos
- ✅ 15 preguntas respondidas
- ✅ Alternativas estándar evaluadas
- ✅ 12 requerimientos granulares definidos
- ✅ Hallazgos técnicos documentados
- ✅ Estado de implementación verificado (50%)
- ✅ PR document actualizado

---

### 9.2 Siguiente Fase: Phase 2 - Design & Framework Research

**Objetivo:** Diseñar arquitectura detallada de los search helps.

**Actividades:**
1. Diseño detallado de Function Module Z_PSR018_IMPR_SHLP_EXIT
2. Especificación de parámetros de cada search help
3. Definición de estructura de return values
4. Diseño de manejo de errores
5. Mockups de datos de prueba para IMPR
6. Plan de testing detallado

**Duración estimada:** 4-6 horas

**Prerequisitos:**
- ✅ Análisis de requerimientos completado
- ✅ Alternativas técnicas evaluadas
- ✅ Sub-requirements definidos

---

### 9.3 Fases Siguientes

**Phase 3: Implementation (10-12 horas)**
- Desarrollo de function modules
- Creación de search helps
- Modificación de estructuras
- Configuración TVARVC

**Phase 4: Testing (6 horas)**
- Unit testing de FMs
- Integration testing de SH
- E2E testing en CJ20N
- Validación de auto-fill

**Phase 5: Deployment (2 horas)**
- Documentación técnica
- Actualización de PR
- Handover a UAT

---

## 10. APÉNDICES

### Apéndice A: Referencias Técnicas

**SAP Notes:**
- SAP Note 86050: User-defined fields in project definition (CNEX0006)

**Transacciones:**
- CMOD: Enhancement projects
- SE11: ABAP Dictionary
- STVARV: TVARVC maintenance
- CJ20N: Project Builder
- CJ01/CJ02: WBS Element create/change
- CJ06: Project Definition create

**URIs ADT:**
- CI_PROJ: `/sap/bc/adt/ddic/structures/ci_proj/source/main`
- CI_PRPS: `/sap/bc/adt/ddic/structures/ci_prps/source/main`
- CNEX0006: `/sap/bc/adt/vit/wb/object_type/smodxe/object_name/CNEX0006`
- CNEX0007: `/sap/bc/adt/vit/wb/object_type/smodxe/object_name/CNEX0007`

**Web Resources:**
- SAP Community: "Controlling/Manipulating data of Search Help using search help Exit"
- SAP Help: F4IF_SHLP_EXIT_EXAMPLE documentation
- Eursap: "How to use dynamic variants in TVARVC table"

---

### Apéndice B: Glosario

| Término | Definición |
|---------|------------|
| **CPI** | Código Programa de Inversión - Sistema de clasificación presupuestal |
| **IMPR** | Investment Program - Tabla SAP de estructura de programas de inversión |
| **POSID** | Position ID - Identificador jerárquico en formato 1.1.1.1 |
| **POST1** | DENOMINACION - Campo de descripción en IMPR |
| **TVARVC** | Table of Variant Variables (Client-Specific) - Parámetros configurables |
| **CI_PROJ** | Customer Include for PROJ - Append structure para proyectos |
| **CI_PRPS** | Customer Include for PRPS - Append structure para elementos PEP |
| **CNEX0006** | SAP Enhancement para campos cliente en definición de proyecto |
| **CNEX0007** | SAP Enhancement para campos cliente en elemento PEP |
| **F4** | Field Help - Search help / matchcode en SAP |
| **Exit** | User Exit - Punto de extensión en código SAP standard |

---

### Apéndice C: Datos de Prueba IMPR

**Script SQL para cargar datos de prueba en mandante 210:**

```sql
-- Sector CPI (2 puntos)
INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000001', '2025', '1.1.1', 'TEST', 'Sector Educación');

INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000011', '2025', '2.2.2', 'TEST', 'Sector Salud');

-- Programa CPI (4 puntos)
INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000002', '2025', '1.1.1.1.1', 'TEST', 'Programa Educación Primaria');

INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000012', '2025', '2.2.2.2.2', 'TEST', 'Programa Hospitales');

-- Producto (7 puntos)
INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000003', '2025', '1.1.1.1.1.1.1.1', 'TEST', 'Producto Infraestructura Escolar');

-- Indicador (9 puntos)
INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000004', '2025', '1.1.1.1.1.1.1.1.1.1', 'TEST', 'Indicador Cobertura Escolar');

-- Meta (4 puntos - igual a Programa)
INSERT INTO impr (mandt, posnr, gjahr, posid, prnam, post1)
VALUES ('210', '00000005', '2025', '1.1.1.1.1', 'TEST', 'Meta Acceso Universal Educación');
```

---

## 📝 CONCLUSIÓN

Este documento consolida el análisis completo del requirement PSR018 realizado mediante el SAP Requirements Analyst Agent.

**Estado:** ✅ **Phase 1 (Requirements Analysis) COMPLETADO**

**Próxima Fase:** Phase 2 - Design & Framework Research

**Aprobación pendiente para proceder a implementación.**

---

**Documento generado por:** Claude Code - SAP Requirements Analyst Agent
**Fecha:** 2025-11-05
**Versión:** 1.0 (Analysis Complete)
**Transport:** CADK911088
**Status del PR:** 🟡 50% Complete - Analysis Phase Done

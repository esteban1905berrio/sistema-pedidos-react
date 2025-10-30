# Mapeo Completo: CSV → ZEFIAAC002_1 → BAPI (67 Campos)

## ⚠️ ACTUALIZACIÓN CRÍTICA: Análisis de 67 Columnas CSV

**Fecha:** 2025-10-28
**Versión:** 2.0 - Análisis completo con nombres nemotécnicos

---

## Tabla de Mapeo Completa (66 Campos CSV + 40 Campos Legacy = 106 Total)

### SECCIÓN 1: CAMPOS DEL CSV - DATOS MAESTROS (30 campos)
**Columnas CSV: 2-31**

| Col# | CSV | Campo Estructura | Tipo ABAP | Estructura BAPI | Notas |
|------|-----|------------------|-----------|-----------------|-------|
| 2 | ANLN1 | asset | ANLN1 | BAPI1022_KEY | Número principal del activo |
| 3 | ANLN2 | subnumber | ANLN2 | BAPI1022_KEY | Subnúmero del activo |
| 4 | ANLKL | assetclass | ANLKL | BAPI1022_KEY | Clase de activo |
| 5 | BUKRS | companycode | BUKRS | BAPI1022_KEY | Sociedad |
| 6 | NASSETS | nassets | CHAR3 | BAPI1022_KEY | **NUEVO** - Cantidad activos (siempre 1) |
| 7 | TXT50 | descript | TXT50_ANLA | BAPI1022_FEGLG001 | Denominación |
| 8 | TXA50 | descript2 | TXA50_ANLA | BAPI1022_FEGLG001 | Denominación 2 |
| 9 | ANLHTXT | main_descript | ANLHTXT | BAPI1022_FEGLG001 | Texto número principal AF |
| 10 | SERNR | serial_no | SERNR | BAPI1022_FEGLG001 | Número de serie |
| 11 | INVNR | invent_no | INVNR | BAPI1022_FEGLG001 | Número de inventario |
| 12 | MENGE | quantity | MENGE_D | BAPI1022_FEGLG001 | Cantidad |
| 13 | MEINS | base_uom | MEINS | BAPI1022_FEGLG001 | Unidad de medida |
| 14 | IVDAT | date | INVDA | BAPI1022_FEGLG001 | Último inventario el |
| 15 | INKEN | include_in_list | INKEN | BAPI1022_FEGLG001 | Indicador de inventario |
| 16 | INVZU | note | INVZU | BAPI1022_FEGLG001 | Nota de inventario |
| 17 | AKTIV | cap_date | AKTIVD | BAPI1022_FEGLG001 | Capitalizado el |
| 18 | KOSTL | costcenter | KOSTL | BAPI1022_FEGLG003 | Centro de coste |
| 19 | KOSTLV | resp_cctr | KOSTL | BAPI1022_FEGLG003 | CeCo responsable |
| 20 | WERKS | plant | WERKS_D | BAPI1022_FEGLG003 | Centro |
| 21 | STORT | location | STORT | BAPI1022_FEGLG003 | Emplazamiento |
| 22 | RAUMN | room | RAUMN | BAPI1022_FEGLG003 | **NUEVO** - Local/Sala |
| 23 | KFZKZ | plate_no | KFZKZ | BAPI1022_FEGLG003 | Matrícula de vehículo |
| 24 | GEBER | fund | GEBER | BAPI1022_FEGLG003 | **NUEVO** - Fondo (heredado CeCo) |
| 25 | FISTL | funds_center | FISTL | BAPI1022_FEGLG003 | **NUEVO** - Centro gestor (heredado CeCo) |
| 26 | PS_PSP_PNR2 | wbs_element_cost | PS_POSID | BAPI1022_FEGLG003 | Elemento PEP (heredado CeCo) |
| 27 | PRCTR | profit_center | PRCTR | BAPI1022_FEGLG003 | **NUEVO** - Centro beneficio (heredado CeCo) |
| 28 | SEGMENT | segment | FB_SEGMENT | BAPI1022_FEGLG003 | **NUEVO** - Segmento (heredado CeCo) |
| 29 | XSTIL | shutdown | XSTIL | BAPI1022_FEGLG003 | Paralizado |
| 30 | LIFNR | vendor_no | LIFNR | BAPI1022_FEGLG001 | Acreedor |
| 31 | AIBN1 | orig_asset | ANLN1 | BAPI1022_FEGLG001 | AF origen en traslados |

### SECCIÓN 2: CAMPOS DEL CSV - CLASIFICACIÓN (6 campos)
**Columnas CSV: 32-37**

| Col# | CSV | Campo Estructura | Tipo ABAP | Estructura BAPI | Notas |
|------|-----|------------------|-----------|-----------------|-------|
| 32 | ORD41 | evalgroup1 | ORD41 | BAPI1022_FEGLG004 | Criterio clasificación 1 |
| 33 | ORD42 | evalgroup2 | ORD42 | BAPI1022_FEGLG004 | Criterio clasificación 2 |
| 34 | ORD43 | evalgroup3 | ORD43 | BAPI1022_FEGLG004 | Criterio clasificación 3 |
| 35 | ORD44 | evalgroup4 | ORD44 | BAPI1022_FEGLG004 | Criterio clasificación 4 |
| 36 | GDLGRP | evalgroup5 | GDLGRP | BAPI1022_FEGLG004 | Criterio clasificación 5 |
| 37 | ANLUE | super_number | ANLUE | BAPI1022_FEGLG004 | **NUEVO** - Supranúmero (cédula empleado) |

### SECCIÓN 3: CAMPOS DEL CSV - AMPLIACIÓN Z (10 campos)
**Columnas CSV: 38-47**

| Col# | CSV Original | Nombre Nemotécnico | Campo Estructura | Tipo ABAP | Estructura BAPI | Notas |
|------|--------------|-------------------|------------------|-----------|-----------------|-------|
| 38 | *vacío* | **ZMUNICIP** | zmunicip | CHAR50 | BAPIPAREX | **NUEVO** - Municipio |
| 39 | *vacío* | **ZMATINMOB** | zmatinmob | CHAR50 | BAPIPAREX | **NUEVO** - Matrícula inmobiliaria |
| 40 | *vacío* | **ZFICHCATA** | zfichcata | CHAR50 | BAPIPAREX | **NUEVO** - Ficha catastral |
| 41 | *vacío* | **ZANIOIMP** | zanioimp | GJAHR | BAPIPAREX | **NUEVO** - Año impuesto predial |
| 42 | *vacío* | **ZVALORIMP** | zvalorimp | WRBTR | BAPIPAREX | **NUEVO** - Valor pagado impuesto |
| 43 | *vacío* | **ZFECHAIMP** | zfechaimp | DATUM | BAPIPAREX | **NUEVO** - Fecha pago impuesto |
| 44 | *vacío* | **ZCOMODAT** | zcomodat | CHAR50 | BAPIPAREX | **NUEVO** - Comodatario |
| 45 | *vacío* | **ZCONTCOMO** | zcontcomo | CHAR50 | BAPIPAREX | **NUEVO** - Contrato comodato |
| 46 | *vacío* | **ZFECHINI** | zfechini | DATUM | BAPIPAREX | **NUEVO** - Fecha inicio comodato |
| 47 | *vacío* | **ZFECHFIN** | zfechfin | DATUM | BAPIPAREX | **NUEVO** - Fecha fin comodato |

### SECCIÓN 4: CAMPOS DEL CSV - VALORACIÓN Y AMORTIZACIÓN (20 campos)
**Columnas CSV: 48-67**

| Col# | CSV Original | Nombre Nemotécnico | Campo Estructura | Tipo ABAP | Estructura BAPI | Notas |
|------|--------------|-------------------|------------------|-----------|-----------------|-------|
| 48 | AFABE | ✅ AFABE | area | AFABE | BAPI1022_DEP_AREAS | Área de valoración |
| 49 | AFASL | ✅ AFASL | dep_key | AFASL | BAPI1022_DEP_AREAS | Clave de amortización |
| 50 | NDABJ | ✅ NDABJ | exp_ulife_yrs | NDABJ | BAPI1022_DEP_AREAS | Vida útil Años |
| 51 | *vacío* | **NDPER** | ulife_prds | NDPER | BAPI1022_DEP_AREAS | Vida útil Meses |
| 52 | *vacío* | **AFABG** | odep_start_date | AFABG | BAPI1022_DEP_AREAS | Fecha inicio amortización |
| 53 | GJAHR | ✅ GJAHR | fisc_year | GJAHR | BAPI1022_CUMVAL | Ejercicio fiscal actual |
| 54 | *vacío* | **CURRENCY** | currency | WAERS | BAPI1022_CUMVAL | Clave de la moneda |
| 55 | *vacío* | **ZCLVREVAL** | zclvreval | CHAR4 | - | Clave revalorización (áreas 17/18) |
| 56 | *vacío* | **SCHRW** | scrapvalue | SCHRW | BAPI1022_DEP_AREAS | Valor residual importe (área 1) |
| 57 | *vacío* | **SCHRWPROZ** | scrapvalue_prctg | SCHRW_PROZ | BAPI1022_DEP_AREAS | Valor residual % (área 1) |
| 58 | KANSW | ✅ KANSW | acq_value | KANSW | BAPI1022_CUMVAL | Valor acumulado adquisición |
| 59 | KAUFW | ✅ KAUFW | rev_repl | KAUFW | BAPI1022_CUMVAL | Revalorización acum valor |
| 60 | KNAFA | ✅ KNAFA | ord_dep | KNAFA | BAPI1022_CUMVAL | Amortización normal acum |
| 61 | KAUFN | ✅ KAUFN | rev_ord_dep | KAUFN | BAPI1022_CUMVAL | Revalorización acum amort |
| 62 | *vacío* | **BWASL** | bwasl | BWASL | - | Clase de movimiento |
| 63 | KANZA ⚠️ | **ANZAH** | down_payment | ANZAH | BAPI1022_CUMVAL | ⚠️ ERROR CSV: dice "Anticipos" pero usa KANZA |
| 64 | *vacío* | **KANZA** | capitalization_year | KANZA | BAPI1022_CUMVAL | **NUEVO** - Capitalización año curso |
| 65 | *vacío* | **ZAMORTANIO** | zamortanio | WRBTR | BAPIPAREX | **CUSTOM** - Amortización año curso |
| 66 | *vacío* | **ZREVAMANO** | zrevamano | WRBTR | BAPIPAREX | **CUSTOM** - Revalorización amort año |
| 67 | *vacío* | **ZAMOREVANO** | zamorevano | WRBTR | BAPIPAREX | **CUSTOM** - Amort revalorización año |

**Total Campos CSV: 66 campos activos (67 columnas incluyendo encabezado)**

---

### SECCIÓN 5: CAMPOS LEGACY - NO EN CSV (40 campos)

Estos campos NO están en el nuevo CSV pero deben mantenerse en la estructura por compatibilidad con BAPIs y otros procesos:

| # | Campo Estructura | Tipo ABAP | Estructura BAPI | Notas |
|---|------------------|-----------|-----------------|-------|
| 68 | testrun | TESTRUN | - | Modo de prueba |
| 69 | xsubno | XANLGR | - | Indicador subnúmero |
| 70 | neg_values | XNEGA | - | Indicador valores negativos |
| 71 | history | XHIST_AM | - | Campo de control histórico |
| 72 | base_uom_iso | MEINS_ISO | BAPI1022_FEGLG001 | UOM ISO |
| 73 | currency_iso | WAERS_ISO | BAPI1022_CUMVAL | Código ISO moneda |
| 74 | acq_yr | VYEAR | - | Año comparativo |
| 75 | acq_prd | VMNTH | - | Período comparativo |
| 76 | orig_acq_yr | URJHR | BAPI1022_FEGLG001 | Año adquisición original |
| 77 | orig_value | URWRT | BAPI1022_FEGLG001 | Valor adquisición original |
| 78 | orig_acq_date | AIBDT | BAPI1022_FEGLG001 | Fecha adquisición original |
| 79 | exp_ulife_prds | NDABP | BAPI1022_DEP_AREAS | Vida útil meses esperados |
| 80 | ulife_yrs | NDABJ | BAPI1022_DEP_AREAS | Vida útil años (alternativa) |
| 81 | from_date | ADATU | BAPI1022_FEGLG003 | Fecha desde |
| 82 | to_date | BDATU | BAPI1022_FEGLG003 | Fecha hasta |
| 83 | deact_date | DEAKT | BAPI1022_FEGLG003 | Fecha de desactivación |
| 84 | person_no | PERNR_D | BAPI1022_FEGLG003 | Número de personal (legacy) |
| 85 | intern_ord | AUFNR | BAPI1022_FEGLG003 | Orden interna CO |
| 86 | area_postval | AFABE_D | BAPI1022_POSTVAL | Área valoración contabilización |
| 87 | rev_repl_postval | AUFWB | BAPI1022_POSTVAL | Revalorización contabilización |
| 88 | ord_dep_postval | NAFAG | BAPI1022_POSTVAL | Amortización contabilización |
| 89 | spe_dep | SAFAG | BAPI1022_POSTVAL | Amortización especial |
| 90 | unp_dep | AAFAG | BAPI1022_POSTVAL | Amortización no planificada |
| 91 | trans_res | MAFAG | BAPI1022_POSTVAL | Resultados transferencia |
| 92 | interest | DZINSG | BAPI1022_POSTVAL | Intereses |
| 93 | rev_cum_ord_dep | AUFNG | BAPI1022_POSTVAL | Rev. amortización acumulada |
| 94-106 | zreserved01-10 | CHAR100 | - | Campos reservados para extensiones |

**Total Campos Legacy: 40 campos**

---

## ⚠️ HALLAZGOS CRÍTICOS

### 1. Error en CSV - Campo 63 (KANZA mal ubicado)

**Problema:**
- **Campo 63 (CSV):** Nombre del campo = "Anticipos acumulados" → Nombre técnico = **KANZA**
- **Campo 64 (CSV):** Nombre del campo = "Capitalización del año en curso" → Nombre técnico = *vacío*

**Error:** En SAP, KANZA significa "Capitalización del año en curso", NO "Anticipos acumulados"

**Corrección Aplicada:**
```
Campo 63: down_payment (ANZAH) - Anticipos acumulados
Campo 64: capitalization_year (KANZA) - Capitalización del año en curso
```

### 2. Campos Custom NO en BAPI Estándar (3 campos)

Estos campos NO existen en `BAPI_FIXEDASSET_OVRTAKE_CREATE` y requieren lógica personalizada:

| Campo | Columna CSV | Solución |
|-------|-------------|----------|
| zamortanio | 65 | Usar EXTENSIONIN (BAPIPAREX) o custom code |
| zrevamano | 66 | Usar EXTENSIONIN (BAPIPAREX) o custom code |
| zamorevano | 67 | Usar EXTENSIONIN (BAPIPAREX) o custom code |

### 3. Campos Condicionales (5 campos)

Solo aplican en ciertas condiciones:

| Campo | Condición |
|-------|-----------|
| zclvreval | Solo para áreas de valoración 17 y 18 |
| scrapvalue | Solo para área de valoración 1 |
| scrapvalue_prctg | Solo para área de valoración 1 |
| bwasl | Depende del tipo de movimiento |
| down_payment | Anticipos acumulados (ANZAH) |

---

## Resumen Final

### Conteo Total de Campos en ZEFIAAC002_1

| Categoría | Cantidad | Detalle |
|-----------|----------|---------|
| **CSV - Datos Maestros** | 30 | Identificación, descripción, inventario, organización |
| **CSV - Clasificación** | 6 | Criterios de clasificación + supranúmero |
| **CSV - Ampliación Z** | 10 | Campos Z para gestión de propiedades |
| **CSV - Valoración** | 20 | Áreas, amortización, valores contables, condicionales |
| **Legacy** | 40 | Campos de compatibilidad BAPI |
| **TOTAL** | **106** | Campos totales en estructura |

### Campos Nuevos Agregados (20 total)

**SAP Estándar (7):**
1. nassets (CHAR3) → KEY
2. room (RAUMN) → FEGLG003
3. fund (GEBER) → FEGLG003
4. funds_center (FISTL) → FEGLG003
5. profit_center (PRCTR) → FEGLG003
6. segment (SEGMENT) → FEGLG003
7. super_number (ANLUE) → FEGLG004

**Campos Z (10):**
8. zmunicip → BAPIPAREX
9. zmatinmob → BAPIPAREX
10. zfichcata → BAPIPAREX
11. zanioimp → BAPIPAREX
12. zvalorimp → BAPIPAREX
13. zfechaimp → BAPIPAREX
14. zcomodat → BAPIPAREX
15. zcontcomo → BAPIPAREX
16. zfechini → BAPIPAREX
17. zfechfin → BAPIPAREX

**Campos Custom (3):**
18. zamortanio → BAPIPAREX (NO en BAPI estándar)
19. zrevamano → BAPIPAREX (NO en BAPI estándar)
20. zamorevano → BAPIPAREX (NO en BAPI estándar)

---

## Estructura ABAP Final (Estructura DDIC - Eclipse ADT)


---

## Métodos de Mapeo Requeridos

### 1. mp_key
```abap
METHOD mp_key.
  e_es_key-companycode = i_es_dato_actf-companycode.
  e_es_key-asset       = i_es_dato_actf-asset.
  e_es_key-subnumber   = i_es_dato_actf-subnumber.
  e_es_key-nassets     = i_es_dato_actf-nassets.  " NUEVO
ENDMETHOD.
```

### 2. mp_timedependentdata
```abap
METHOD mp_timedependentdata.
  " ... campos existentes ...
  e_es_timedependentdata-room       = i_es_dato_actf-room.         " NUEVO
  e_es_timedependentdata-fund       = i_es_dato_actf-fund.         " NUEVO
  e_es_timedependentdata-funds_ctr  = i_es_dato_actf-funds_center. " NUEVO
  e_es_timedependentdata-profit_ctr = i_es_dato_actf-profit_center." NUEVO
  e_es_timedependentdata-segment    = i_es_dato_actf-segment.      " NUEVO
ENDMETHOD.
```

### 3. mp_allocations
```abap
METHOD mp_allocations.
  " ... campos existentes ...
  e_es_allocations-assetsupno = i_es_dato_actf-super_number.  " NUEVO
ENDMETHOD.
```

### 4. mp_cumulatedvalues
```abap
METHOD mp_cumulatedvalues.
  DATA: ls_cumval TYPE bapi1022_cumval.

  " ... lógica existente ...
  ls_cumval-down_payment = i_es_dato_actf-down_payment.           " ANZAH
  ls_cumval-down_payment = i_es_dato_actf-capitalization_year.    " KANZA (NUEVO)

  APPEND ls_cumval TO e_ti_cumulatedvalues.
ENDMETHOD.
```

### 5. mp_extensionin (NUEVO MÉTODO)
```abap
METHOD mp_extensionin.
  DATA: ls_extension TYPE bapiparex.

  " Campos Z - Propiedades
  IF i_es_dato_actf-zmunicip IS NOT INITIAL.
    ls_extension-structure  = 'CI_ANLA'.
    ls_extension-valuepart1 = |ZMUNICIP={ i_es_dato_actf-zmunicip }|.
    ls_extension-valuepart2 = |ZMATINMOB={ i_es_dato_actf-zmatinmob }|.
    ls_extension-valuepart3 = |ZFICHCATA={ i_es_dato_actf-zfichcata }|.
    ls_extension-valuepart4 = |ZANIOIMP={ i_es_dato_actf-zanioimp }|.
    APPEND ls_extension TO e_ti_extensionin.
    CLEAR ls_extension.
  ENDIF.

  IF i_es_dato_actf-zvalorimp IS NOT INITIAL.
    ls_extension-structure  = 'CI_ANLA'.
    ls_extension-valuepart1 = |ZVALORIMP={ i_es_dato_actf-zvalorimp }|.
    ls_extension-valuepart2 = |ZFECHAIMP={ i_es_dato_actf-zfechaimp }|.
    ls_extension-valuepart3 = |ZCOMODAT={ i_es_dato_actf-zcomodat }|.
    ls_extension-valuepart4 = |ZCONTCOMO={ i_es_dato_actf-zcontcomo }|.
    APPEND ls_extension TO e_ti_extensionin.
    CLEAR ls_extension.
  ENDIF.

  IF i_es_dato_actf-zfechini IS NOT INITIAL.
    ls_extension-structure  = 'CI_ANLA'.
    ls_extension-valuepart1 = |ZFECHINI={ i_es_dato_actf-zfechini }|.
    ls_extension-valuepart2 = |ZFECHFIN={ i_es_dato_actf-zfechfin }|.
    APPEND ls_extension TO e_ti_extensionin.
    CLEAR ls_extension.
  ENDIF.

  " Campos CUSTOM - Año en curso
  IF i_es_dato_actf-zamortanio IS NOT INITIAL.
    ls_extension-structure  = 'CI_ANLA'.
    ls_extension-valuepart1 = |ZAMORTANIO={ i_es_dato_actf-zamortanio }|.
    ls_extension-valuepart2 = |ZREVAMANO={ i_es_dato_actf-zrevamano }|.
    ls_extension-valuepart3 = |ZAMOREVANO={ i_es_dato_actf-zamorevano }|.
    APPEND ls_extension TO e_ti_extensionin.
  ENDIF.
ENDMETHOD.
```

---

## Campos Heredados (NO cargar desde CSV)

Estos campos se heredan automáticamente del Centro de Coste y NO deben cargarse explícitamente:

1. **fund (GEBER)** - Fondo
2. **funds_center (FISTL)** - Centro gestor
3. **wbs_element_cost (PS_PSP_PNR2)** - Elemento PEP
4. **profit_center (PRCTR)** - Centro de beneficio
5. **segment (SEGMENT)** - Segmento

**Acción:** Dejar estos campos vacíos en el CSV. SAP los completará automáticamente.

---

## Validaciones de Campos Obligatorios

Según BAPI_FIXEDASSET_OVRTAKE_CREATE:

### Obligatorios Siempre:
- companycode (BUKRS)
- asset (ANLN1) - Si se deja vacío, SAP genera automáticamente
- assetclass (ANLKL)
- descript (TXT50)
- cap_date (AKTIV)
- area (AFABE)
- dep_key (AFASL)
- currency (CURRENCY)

### Obligatorios si se Cargan Saldos:
- fisc_year (GJAHR)
- acq_value (KANSW)

### Condicionales:
- base_uom (MEINS) - Obligatorio si quantity > 0
- nassets - Siempre debe ser "1" para carga individual

---

**Fecha Generación:** 2025-10-28
**Versión Documento:** 2.1 - Estructura DDIC corregida
**Estado:** ✅ Listo para implementación

**Cambios v2.1:**
- ✅ Estructura DDIC normal (NO vista CDS) con referencias a dominios SAP (bf_*)
- ✅ Categoría de extensibilidad corregida: `#NOT_EXTENSIBLE`
- ✅ Anotaciones @Semantics solo para cantidades y montos críticos
- ✅ Formato simplificado sin labels extensos, siguiendo patrón Eclipse ADT

**Cambios v2.0:**
- ✅ Análisis completo de 67 columnas CSV (66 campos de datos)
- ✅ Nombres nemotécnicos creados para 30 campos vacíos
- ✅ Corrección error campo 63 (KANZA vs ANZAH)
- ✅ Identificación de 3 campos custom (zamortanio, zrevamano, zamorevano)
- ✅ Total actualizado: 106 campos (66 CSV + 40 legacy)

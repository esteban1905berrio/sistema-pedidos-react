# PR: Ajuste de Cargue de Activos Fijos - ZFIAAC002

## Historia de Usuario

**Como** desarrollador ABAP
**Quiero** ajustar el proceso de cargue de Activos Fijos del programa ZFIAAC002 en el sistema SAP GDC
**Para** adaptar la estructura del archivo de cargue a los nuevos requerimientos de negocio y garantizar la correcta migración de datos maestros y saldos de activos fijos

---

## 1. ANÁLISIS DE SITUACIÓN ACTUAL

### 1.1 Programa Actual: ZFIAAC002

**Identificación:**
- **Programa Principal:** ZFIAAC002
- **Paquete:** ZFI
- **Descripción:** "FIAAC002 Carga de Saldos de Activos y Datos maestros"
- **Última modificación:** 2025-10-26 20:58:44

**Arquitectura del Programa:**
```
ZFIAAC002 (Programa principal)
├── ZFIAAC002V_1       (Variables y declaraciones)
├── ZFIAAC002CD1_1     (Definiciones de clases)
└── ZFIAAC002CI1_1     (Implementación de clases)
```

**Características Técnicas:**
- Utiliza clase local `LCL_CARGA_ACTIVO_FIJO` para encapsular la lógica
- Soporta ejecución en background (batch) y foreground
- Permite cargar hasta 5 archivos simultáneamente (pa_arch, pa_arch2...pa_arch5)
- Modo de prueba configurable (pa_test)
- Modo debug disponible (pa_debug)
- Verificación de autorizaciones implementada

**BAPIs Realmente Utilizadas (verificado en código):**
1. **BAPI_FIXEDASSET_OVRTAKE_CREATE** - Crear activos fijos con migración de saldos (AS91)
2. **BAPI_FIXEDASSET_CHANGE** - Modificar datos maestros de activos existentes
3. **BAPI_FIXEDASSET_OVRTAKE_POST** - Contabilizar valores acumulados y del año
4. **BAPI_ASSET_REVERSAL_POST** - Anular movimientos contables (en caso de reproceso)

**Nota:** Los comentarios del programa mencionan `BAPI_ASSET_CREATE` y `BAPI_ASSET_POSTING_POST`, pero el código real utiliza las BAPIs listadas arriba (clase `ZCLFIAAC002_CARGA_ACTIVOS_FIJ`).

### 1.2 Estructura Actual: ZEFIAAC002_1

**Tipo:** Estructura DDIC (TABL/DS)
**Descripción:** "Datos activo fijo"
**Total de campos actuales:** 81 campos

**Campos Principales (Resumido):**

| Categoría | Campos Clave |
|-----------|-------------|
| **Identificación** | companycode, asset, subnumber, assetclass |
| **Descripción** | descript, descript2, main_descript, serial_no |
| **Organización** | plant, location, costcenter, resp_cctr |
| **Inventario** | invent_no, quantity, base_uom, date, include_in_list |
| **Valoración** | area, dep_key, ulife_yrs, ulife_prds, cap_date |
| **Valores Contables** | acq_value, ord_dep, rev_repl, rev_ord_dep |
| **Clasificación** | evalgroup1-5, person_no, vendor_no |
| **Control** | testrun, xsubno, neg_values, shutdown |

---

## 2. ANÁLISIS DE NUEVA ESTRUCTURA

### 2.1 Archivo CSV de Entrada

**Ubicación:** `resources/Plantilla Datos Maestros_FI_Activos Fijos_Bienes Inmuebles_V2_06.10.2025 - Copy of Activos Fijos.csv`

**Características:**
- **Total de campos:** 65 columnas
- **Total de registros:** ~14,127 líneas (incluyendo encabezados)
- **Formato:** CSV con separador de comas
- **Estructura:** 5 filas de encabezado + datos

**Filas de Encabezado:**
1. **Fila 1:** Nombre del campo (descripción funcional)
2. **Fila 2:** Recomendaciones específicas del campo
3. **Fila 3:** Nombre técnico SAP
4. **Fila 4:** Tabla SAP de origen
5. **Fila 5:** Longitud del campo

### 2.2 Mapeo de Campos - Nueva Estructura

#### **Campos de Datos Maestros (ANLA/ANLZ) - 30 campos**

| # | Nombre Funcional | Campo Técnico | Tabla | Longitud | Obligatorio |
|---|-----------------|---------------|-------|----------|-------------|
| 1 | Número principal del activo | ANLN1 | ANLA | 12 | ✅ Sí |
| 2 | Subnúmero | ANLN2 | ANLA | 4 | ❌ No |
| 3 | Clase de activo | ANLKL | ANLA | 8 | ✅ Sí |
| 4 | Sociedad | BUKRS | ANLA | 4 | ✅ Sí |
| 5 | Cantidad de activos iguales | NASSETS | - | 3 | ✅ Sí (siempre 1) |
| 6 | Denominación | TXT50 | ANLA | 50 | ✅ Sí |
| 7 | Denominación 2 | TXA50 | ANLA | 50 | ❌ No |
| 8 | Texto número principal AF | ANLHTXT | ANLH | 50 | ❌ No |
| 9 | Número de serie | SERNR | ANLA | 18 | ✅ Sí |
| 10 | Número de inventario | INVNR | ANLA | 25 | ❌ No (automático) |
| 11 | Cantidad | MENGE | ANLA | 13 | ❌ Condicional |
| 12 | Unidad | MEINS | ANLA | 13 | ✅ Si hay cantidad |
| 13 | Último inventario el | IVDAT | ANLA | 8 | ❌ No |
| 14 | Indicador de inventario | INKEN | ANLA | 1 | ✅ Sí (siempre X) |
| 15 | Nota de inventario | INVZU | ANLA | 15 | ❌ No |
| 16 | Capitalizado el | AKTIV | ANLA | 8 | ✅ Sí |
| 17 | Centro de coste | KOSTL | ANLZ | 10 | ✅ Sí |
| 18 | CeCo responsable | KOSTLV | ANLZ | 10 | ✅ Sí |
| 19 | Centro | WERKS | ANLZ | 4 | ❌ No |
| 20 | Emplazamiento | STORT | ANLZ | 10 | ❌ No |
| 21 | Local | RAUMN | ANLZ | 8 | ❌ No |
| 22 | Matrícula de vehículo | KFZKZ | ANLZ | 15 | ❌ No |
| 23 | Fondo | GEBER | ANLZ | 10 | ❌ Heredado |
| 24 | Centro gestor | FISTL | ANLZ | 16 | ❌ Heredado |
| 25 | Elemento PEP | PS_PSP_PNR2 | ANLZ | 8 | ❌ Heredado |
| 26 | Centro de beneficio | PRCTR | ANLZ | 10 | ❌ Heredado |
| 27 | Segmento | SEGMENT | ANLZ | 10 | ❌ Heredado |
| 28 | Paralizado | XSTIL | ANLZ | 1 | ❌ No |
| 29 | Acreedor | LIFNR | ANLA | 10 | ❌ No |
| 30 | AF origen en traslados | AIBN1 | ANLA | 12 | ✅ Sí |

#### **Campos de Clasificación - 6 campos**

| # | Nombre Funcional | Campo Técnico | Tabla | Longitud | Obligatorio |
|---|-----------------|---------------|-------|----------|-------------|
| 31 | Criterio clasificación 1 | ORD41 | ANLA | 4 | ❌ No |
| 32 | Criterio clasificación 2 | ORD42 | ANLA | 4 | ❌ No |
| 33 | Criterio clasificación 3 | ORD43 | ANLA | 4 | ❌ No |
| 34 | Criterio clasificación 4 | ORD44 | ANLA | 4 | ❌ No |
| 35 | Criterio clasificación 5 | GDLGRP | ANLA | 8 | ❌ No |
| 36 | Supranúmero | ANLUE | ANLA | 12 | ❌ No (cédula empleado) |

#### **Campos de Ampliación Z (Customizing) - 11 campos**

| # | Nombre Funcional | Campo Técnico | Tabla | Notas |
|---|-----------------|---------------|-------|-------|
| 37 | Z - Municipio | (Ampliación) | Custom | Campo Z específico |
| 38 | Z - Matrícula inmobiliaria | (Ampliación) | Custom | Campo Z específico |
| 39 | Z - Ficha catastral | (Ampliación) | Custom | Campo Z específico |
| 40 | Z - Año del Impuesto | (Ampliación) | Custom | Campo Z específico |
| 41 | Z - Valor pagado impuesto | (Ampliación) | Custom | Campo Z específico |
| 42 | Z - Fecha pago impuesto | (Ampliación) | Custom | Formato DD.MM.AAAA |
| 43 | Z - Comodatario | (Ampliación) | Custom | Campo Z específico |
| 44 | Z - Contrato comodato nro | (Ampliación) | Custom | Campo Z específico |
| 45 | Z - Fecha de inicio | (Ampliación) | Custom | Formato DD.MM.AAAA |
| 46 | Z - Fecha de terminación | (Ampliación) | Custom | Formato DD.MM.AAAA |
| 47-50 | (Sin datos) | - | - | Reservados |

#### **Campos de Valoración y Amortización (ANLAB/ANLB/ANLC) - 18 campos**

| # | Nombre Funcional | Campo Técnico | Tabla | Longitud | Obligatorio |
|---|-----------------|---------------|-------|----------|-------------|
| 51 | Área de valoración | AFABE | ANLAB | 2 | ✅ Sí |
| 52 | Clave de amortización | AFASL | ANLAB | 4 | ✅ Sí |
| 53 | Vida útil Años | NDABJ | ANLC | 3 | ✅ Sí |
| 54 | Vida útil Meses | NDPER | ANLB | - | ❌ No |
| 55 | Fecha inicio amortización | AFABG | ANLB | - | ✅ Sí |
| 56 | Ejercicio fiscal actual | GJAHR | ANLC | 4 | ✅ Sí (2025) |
| 57 | Clave de la moneda | CURRENCY | - | 3 | ✅ Sí (COP) |
| 58 | Clave revalorización | - | - | - | ❌ Condicional (17/18) |
| 59 | Valor residual importe | - | - | 23 | ❌ Condicional (área 1) |
| 60 | Valor residual % | - | - | - | ❌ Condicional (área 1) |
| 61 | Valor acumulado adquisición | KANSW | ANLC | 23 | ✅ Sí |
| 62 | Revalorización acum valor | KAUFW | ANLC | 23 | ✅ Sí |
| 63 | Amortización normal acum | KNAFA | ANLC | 23 | ✅ Sí (negativo) |
| 64 | Revalorización acum amort | KAUFN | ANLC | 23 | ✅ Sí |
| 65 | Clase de movimiento | - | - | - | ❌ No |
| 66 | Anticipos acumulados | - | - | 23 | ❌ No |
| 67 | Capitalización año curso | KANZA | ANLC | 23 | ✅ Sí |
| 68 | Amortización año curso | - | - | 23 | ❌ No |

---

## 3. ANÁLISIS GAP (DIFERENCIAS)

### 3.1 Mapeo Exacto: Campos CSV vs ZEFIAAC002_1

**Leyenda:**
- ✅ **Match Directo** - Campo existe en estructura con mismo nombre técnico
- 🔄 **Match con Mapeo** - Campo existe pero con nombre diferente en estructura
- ❌ **Faltante** - Campo no existe en estructura actual
- ⚠️ **Nuevo Campo Z** - Campo de ampliación que debe crearse

#### **Mapeo Completo de 49 Campos CSV → Estructura**

| # | Campo CSV | Campo en ZEFIAAC002_1 | Estado | Notas |
|---|-----------|----------------------|--------|-------|
| 1 | ANLN1 | asset | 🔄 Match | Mapear ANLN1 → asset |
| 2 | ANLN2 | subnumber | 🔄 Match | Mapear ANLN2 → subnumber |
| 3 | ANLKL | assetclass | 🔄 Match | Mapear ANLKL → assetclass |
| 4 | BUKRS | companycode | 🔄 Match | Mapear BUKRS → companycode |
| 5 | NASSETS | ❌ | ❌ Faltante | Agregar campo nassets (valor fijo: 1) |
| 6 | TXT50 | descript | 🔄 Match | Mapear TXT50 → descript |
| 7 | TXA50 | descript2 | 🔄 Match | Mapear TXA50 → descript2 |
| 8 | ANLHTXT | main_descript | 🔄 Match | Mapear ANLHTXT → main_descript |
| 9 | SERNR | serial_no | 🔄 Match | Mapear SERNR → serial_no |
| 10 | INVNR | invent_no | 🔄 Match | Mapear INVNR → invent_no |
| 11 | MENGE | quantity | 🔄 Match | Mapear MENGE → quantity |
| 12 | MEINS | base_uom | 🔄 Match | Mapear MEINS → base_uom |
| 13 | IVDAT | date | 🔄 Match | Mapear IVDAT → date |
| 14 | INKEN | include_in_list | 🔄 Match | Mapear INKEN → include_in_list |
| 15 | INVZU | note | 🔄 Match | Mapear INVZU → note |
| 16 | AKTIV | cap_date | 🔄 Match | Mapear AKTIV → cap_date |
| 17 | KOSTL | costcenter | 🔄 Match | Mapear KOSTL → costcenter |
| 18 | KOSTLV | resp_cctr | 🔄 Match | Mapear KOSTLV → resp_cctr |
| 19 | WERKS | plant | 🔄 Match | Mapear WERKS → plant |
| 20 | STORT | location | 🔄 Match | Mapear STORT → location |
| 21 | RAUMN | ❌ | ❌ Faltante | Agregar campo room (RAUMN) |
| 22 | KFZKZ | plate_no | 🔄 Match | Mapear KFZKZ → plate_no |
| 23 | GEBER | ❌ | ❌ Faltante | Agregar campo fund (GEBER) - Heredado CeCo |
| 24 | FISTL | ❌ | ❌ Faltante | Agregar campo funds_center (FISTL) - Heredado CeCo |
| 25 | PS_PSP_PNR2 | wbs_element_cost | 🔄 Match | Mapear PS_PSP_PNR2 → wbs_element_cost |
| 26 | PRCTR | ❌ | ❌ Faltante | Agregar campo profit_center (PRCTR) - Heredado CeCo |
| 27 | SEGMENT | ❌ | ❌ Faltante | Agregar campo segment (SEGMENT) - Heredado CeCo |
| 28 | XSTIL | shutdown | 🔄 Match | Mapear XSTIL → shutdown |
| 29 | LIFNR | vendor_no | 🔄 Match | Mapear LIFNR → vendor_no |
| 30 | AIBN1 | orig_asset | 🔄 Match | Mapear AIBN1 → orig_asset |
| 31 | ORD41 | evalgroup1 | 🔄 Match | Mapear ORD41 → evalgroup1 |
| 32 | ORD42 | evalgroup2 | 🔄 Match | Mapear ORD42 → evalgroup2 |
| 33 | ORD43 | evalgroup3 | 🔄 Match | Mapear ORD43 → evalgroup3 |
| 34 | ORD44 | evalgroup4 | 🔄 Match | Mapear ORD44 → evalgroup4 |
| 35 | GDLGRP | evalgroup5 | 🔄 Match | Mapear GDLGRP → evalgroup5 |
| 36 | ANLUE | ❌ | ❌ Faltante | Agregar campo super_number (ANLUE - cédula empleado) |
| 37 | **Z-Municipio** | ❌ | ⚠️ Campo Z | **Crear campo zmunicipium** |
| 38 | **Z-Matrícula inmob** | ❌ | ⚠️ Campo Z | **Crear campo zmatricula_inmob** |
| 39 | **Z-Ficha catastral** | ❌ | ⚠️ Campo Z | **Crear campo zficha_catastral** |
| 40 | **Z-Año impuesto** | ❌ | ⚠️ Campo Z | **Crear campo zanio_impuesto** |
| 41 | **Z-Valor impuesto** | ❌ | ⚠️ Campo Z | **Crear campo zvalor_impuesto** |
| 42 | **Z-Fecha pago imp** | ❌ | ⚠️ Campo Z | **Crear campo zfecha_pago_imp** |
| 43 | **Z-Comodatario** | ❌ | ⚠️ Campo Z | **Crear campo zcomodatario** |
| 44 | **Z-Contrato comod** | ❌ | ⚠️ Campo Z | **Crear campo zcontrato_comoda** |
| 45 | **Z-Fecha inicio** | ❌ | ⚠️ Campo Z | **Crear campo zfecha_inicio_com** |
| 46 | **Z-Fecha fin** | ❌ | ⚠️ Campo Z | **Crear campo zfecha_fin_com** |
| 47 | AFABE | area | 🔄 Match | Mapear AFABE → area |
| 48 | AFASL | dep_key | 🔄 Match | Mapear AFASL → dep_key |
| 49 | NDABJ | exp_ulife_yrs | 🔄 Match | Mapear NDABJ → exp_ulife_yrs |
| 50 | NDPER | ulife_prds | 🔄 Match | Mapear NDPER → ulife_prds |
| 51 | AFABG | odep_start_date | 🔄 Match | Mapear AFABG → odep_start_date |
| 52 | GJAHR | fisc_year | 🔄 Match | Mapear GJAHR → fisc_year |
| 53 | CURRENCY | currency | ✅ Match | Mismo nombre técnico |
| 54 | KANSW | acq_value | 🔄 Match | Mapear KANSW → acq_value |
| 55 | KAUFW | rev_repl | 🔄 Match | Mapear KAUFW → rev_repl |
| 56 | KNAFA | ord_dep | 🔄 Match | Mapear KNAFA → ord_dep |
| 57 | KAUFN | rev_ord_dep | 🔄 Match | Mapear KAUFN → rev_ord_dep |
| 58 | KANZA | ❌ | ❌ Faltante | Agregar campo capitalization_year (KANZA) |

**Resumen del Análisis:**
- ✅ **Match Directo:** 1 campo (CURRENCY)
- 🔄 **Match con Mapeo:** 40 campos (requieren transformación de nombres)
- ❌ **Faltantes SAP Estándar:** 7 campos (NASSETS, RAUMN, GEBER, FISTL, PRCTR, SEGMENT, ANLUE, KANZA)
- ⚠️ **Nuevos Campos Z:** 10 campos de ampliación

**Total:** 58 campos activos (de 65 columnas CSV, excluyendo vacías)

### 3.2 Campos en ZEFIAAC002_1 que NO están en CSV

**Campos existentes en estructura actual que el CSV nuevo no utiliza:**

| Campo en Estructura | Tipo de Dato | Decisión Recomendada |
|---------------------|--------------|---------------------|
| `history` | xhist_am | ⚠️ **Mantener** - Campo de control histórico |
| `deact_date` | bf_deakt | ⚠️ **Mantener** - Fecha de desactivación |
| `person_no` | pernr_d | ⚠️ **Mantener** - Reemplazado por ANLUE en CSV |
| `intern_ord` | aufnr | ⚠️ **Mantener** - Orden interna CO |
| `purch_new` | xneu_am | ❌ **Eliminar** - No usado en nuevo proceso |
| `vendor` | bf_liefe | ❌ **Eliminar** - Redundante con vendor_no |
| `manufacturer` | bf_herst | ❌ **Eliminar** - No requerido |
| `trade_id` | bf_rassc | ❌ **Eliminar** - No requerido |
| `country` | bf_am_land1 | ❌ **Eliminar** - No requerido |
| `type_name` | bf_typbz_anla | ❌ **Eliminar** - No requerido |
| `orig_acq_yr` | bf_urjhr | ⚠️ **Mantener** - Año adquisición original |
| `orig_value` | bf_urwrt | ⚠️ **Mantener** - Valor adquisición original |
| `inhouse_prod_percentage` | bf_antei | ❌ **Eliminar** - No usado |
| `area_postval` | bf_afabe_d | ⚠️ **Mantener** - Área valoración contabilización |
| `rev_repl_postval` | bf_aufwb | ⚠️ **Mantener** - Revalorización contabilización |
| `ord_dep_postval` | bf_nafag | ⚠️ **Mantener** - Amortización contabilización |
| `spe_dep` | bf_safag | ⚠️ **Mantener** - Amortización especial |
| `unp_dep` | bf_aafag | ⚠️ **Mantener** - Amortización no planificada |
| `trans_res` | bf_mafag | ⚠️ **Mantener** - Resultados transferencia |
| `interest` | bf_dzinsg | ⚠️ **Mantener** - Intereses |
| `rev_cum_ord_dep` | bf_aufng | ⚠️ **Mantener** - Rev. amortización acumulada |
| `currency_iso` | waers_iso | ⚠️ **Mantener** - Código ISO moneda |
| `exp_ulife_prds` | bf_ndabp | ⚠️ **Mantener** - Vida útil meses esperados |
| `xsubno` | xanlgr_1 | ⚠️ **Mantener** - Indicador subnúmero |
| `testrun` | testrun | ⚠️ **Mantener** - Modo de prueba |
| `base_uom_iso` | meins_iso | ⚠️ **Mantener** - UOM ISO |
| `scrapvalue` | bf_schrw | ⚠️ **Mantener** - Valor residual |
| `neg_values` | bf_xnega | ⚠️ **Mantener** - Indicador valores negativos |
| `acq_yr` | bf_vyear | ⚠️ **Mantener** - Año comparativo |
| `acq_prd` | bf_vmnth | ⚠️ **Mantener** - Período comparativo |
| `scrapvalue_prctg` | schrw_proz | ⚠️ **Mantener** - % valor residual |
| `from_date` | bf_adatu | ⚠️ **Mantener** - Fecha desde |
| `to_date` | bf_bdatu | ⚠️ **Mantener** - Fecha hasta |
| `orig_acq_date` | bf_aibdt | ⚠️ **Mantener** - Fecha adquisición original |

**Resumen:**
- ⚠️ **Mantener:** 26 campos (requeridos por BAPIs o lógica de negocio)
- ❌ **Eliminar:** 7 campos (no usados, redundantes)

**Decisión:** **NO eliminar ningún campo** de ZEFIAAC002_1 para mantener compatibilidad con BAPIs y otros procesos. Solo **AGREGAR** los 17 campos nuevos (7 SAP estándar + 10 campos Z).

### 3.3 Impacto en el Código - Transformación de Nombres

**La estructura ZEFIAAC002_1 usa nombres "amigables" mientras que el CSV usa nombres técnicos SAP.**

**Solución:** El método `consolidar_datos` en la clase `LCL_CARGA_ACTIVO_FIJO` ya realiza esta transformación mediante:

```abap
" Mapeo automático de columnas CSV → Estructura
LOOP AT ti_contenido_linea ASSIGNING FIELD-SYMBOL(<fs_valor_campo>).
  nombre_columna = ti_columnas[ sy-tabix ]-nombre_columna.
  ASSIGN COMPONENT nombre_columna OF STRUCTURE <fs_es_dato_activofijo>
         TO <fs_valor_es_activo_fijo>.
  <fs_valor_es_activo_fijo> = <fs_valor_campo>.
ENDLOOP.
```

**Ajustes Requeridos:**

1. **Actualizar tabla de homologación `ti_columnas`** para mapear:
   - CSV: `ANLN1` → Estructura: `asset`
   - CSV: `BUKRS` → Estructura: `companycode`
   - CSV: `TXT50` → Estructura: `descript`
   - (... resto de 40 campos con mapeo)

2. **Crear campos faltantes** en ZEFIAAC002_1:
   ```abap
   nassets             : char3,        "NASSETS
   room                : raumn,        "RAUMN
   fund                : geber,        "GEBER
   funds_center        : fistl,        "FISTL
   profit_center       : prctr,        "PRCTR
   segment             : segment,      "SEGMENT
   super_number        : anlue,        "ANLUE (cédula empleado)
   capitalization_year : kanza,        "KANZA
   zmunicipium         : char50,       "Z-Municipio
   zmatricula_inmob    : char50,       "Z-Matrícula inmobiliaria
   zficha_catastral    : char50,       "Z-Ficha catastral
   zanio_impuesto      : gjahr,        "Z-Año impuesto
   zvalor_impuesto     : curr23_2,     "Z-Valor impuesto
   zfecha_pago_imp     : datum,        "Z-Fecha pago impuesto
   zcomodatario        : char50,       "Z-Comodatario
   zcontrato_comoda    : char50,       "Z-Contrato comodato
   zfecha_inicio_com   : datum,        "Z-Fecha inicio comodato
   zfecha_fin_com      : datum         "Z-Fecha fin comodato
   ```

**No se requiere cambiar la nomenclatura existente** - solo extender la estructura y actualizar el mapeo CSV → Estructura.

### 3.4 Mapeo de Campos Faltantes a Estructuras BAPI

Los **17 campos nuevos** (7 SAP estándar + 10 Z) deben mapearse a los parámetros correctos de **BAPI_FIXEDASSET_OVRTAKE_CREATE**.

#### **Tabla de Mapeo Completa: Campos Faltantes → Parámetros BAPI**

| # | Campo CSV | Campo Estructura | Tipo ABAP | Estructura BAPI | Parámetro BAPI | Método a Actualizar | Notas |
|---|-----------|------------------|-----------|-----------------|----------------|---------------------|-------|
| 1 | NASSETS | nassets | CHAR3 | - | key-nassets | mp_key | Cantidad de activos idénticos (siempre 1) |
| 2 | RAUMN | room | RAUMN | BAPI1022_FEGLG003 | timedependentdata-room | mp_timedependentdata | Local/Sala |
| 3 | GEBER | fund | GEBER | BAPI1022_FEGLG003 | timedependentdata-fund | mp_timedependentdata | Fondo (heredado CeCo) |
| 4 | FISTL | funds_center | FISTL | BAPI1022_FEGLG003 | timedependentdata-funds_ctr | mp_timedependentdata | Centro gestor (heredado CeCo) |
| 5 | PRCTR | profit_center | PRCTR | BAPI1022_FEGLG003 | timedependentdata-profit_ctr | mp_timedependentdata | Centro de beneficio (heredado CeCo) |
| 6 | SEGMENT | segment | SEGMENT | BAPI1022_FEGLG003 | timedependentdata-segment | mp_timedependentdata | Segmento (heredado CeCo) |
| 7 | ANLUE | super_number | ANLUE | BAPI1022_FEGLG004 | allocations-assetsupno | mp_allocations | Supranúmero (cédula empleado) |
| 8 | KANZA | capitalization_year | KANZA | BAPI1022_CUMVAL | cumulatedvalues-down_payment | mp_cumulatedvalues | Capitalización año en curso |
| 9 | Z-Municipio | zmunicipium | CHAR50 | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Municipio |
| 10 | Z-Matrícula | zmatricula_inmob | CHAR50 | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Matrícula inmobiliaria |
| 11 | Z-Ficha | zficha_catastral | CHAR50 | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Ficha catastral |
| 12 | Z-Año Imp | zanio_impuesto | GJAHR | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Año impuesto predial |
| 13 | Z-Valor Imp | zvalor_impuesto | CURR23_2 | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Valor pagado impuesto |
| 14 | Z-Fecha Imp | zfecha_pago_imp | DATUM | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Fecha pago impuesto |
| 15 | Z-Comodat | zcomodatario | CHAR50 | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Comodatario |
| 16 | Z-Contrato | zcontrato_comoda | CHAR50 | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Contrato comodato |
| 17 | Z-F.Inicio | zfecha_inicio_com | DATUM | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Fecha inicio comodato |
| 18 | Z-F.Fin | zfecha_fin_com | DATUM | BAPIPAREX | extensionin | mp_extensionin | Campo Z - Fecha fin comodato |

#### **Resumen del Mapeo:**

**A. Campos SAP Estándar (8 campos):**
- **1 campo** → Parámetro `KEY` (NASSETS)
- **5 campos** → Estructura `BAPI1022_FEGLG003` (Time-Dependent Data)
  - room, fund, funds_ctr, profit_ctr, segment
- **1 campo** → Estructura `BAPI1022_FEGLG004` (Allocations)
  - assetsupno (cédula empleado)
- **1 campo** → Estructura `BAPI1022_CUMVAL` (Cumulated Values)
  - down_payment (capitalización año)

**B. Campos Z (10 campos):**
- **10 campos** → Tabla `EXTENSIONIN` (BAPIPAREX)
  - Todos los campos de ampliación Z

#### **Estructuras BAPI Involucradas:**

**1. BAPI1022_FEGLG003 (Datos Dependientes del Tiempo):**
```abap
DATA: ls_timedependentdata TYPE bapi1022_feglg003.

ls_timedependentdata-room        = lv_raumn.         " RAUMN
ls_timedependentdata-fund        = lv_geber.         " GEBER
ls_timedependentdata-funds_ctr   = lv_fistl.         " FISTL
ls_timedependentdata-profit_ctr  = lv_prctr.         " PRCTR
ls_timedependentdata-segment     = lv_segment.       " SEGMENT
ls_timedependentdata-costcenter  = lv_kostl.         " Ya existente
ls_timedependentdata-resp_cctr   = lv_kostlv.        " Ya existente
ls_timedependentdata-plant       = lv_werks.         " Ya existente
ls_timedependentdata-location    = lv_stort.         " Ya existente
ls_timedependentdata-shutdown    = lv_xstil.         " Ya existente
```

**2. BAPI1022_FEGLG004 (Clasificaciones):**
```abap
DATA: ls_allocations TYPE bapi1022_feglg004.

ls_allocations-evalgroup1 = lv_ord41.                " Ya existente
ls_allocations-evalgroup2 = lv_ord42.                " Ya existente
ls_allocations-evalgroup3 = lv_ord43.                " Ya existente
ls_allocations-evalgroup4 = lv_ord44.                " Ya existente
ls_allocations-evalgroup5 = lv_gdlgrp.               " Ya existente
ls_allocations-assetsupno = lv_anlue.                " NUEVO: ANLUE (cédula empleado)
```

**3. BAPI1022_CUMVAL (Valores Acumulados):**
```abap
DATA: lt_cumulatedvalues TYPE TABLE OF bapi1022_cumval.

APPEND VALUE #(
  fisc_year    = lv_gjahr                             " Ya existente
  area         = lv_afabe                             " Ya existente
  acq_value    = lv_kansw                             " Ya existente
  rev_repl     = lv_kaufw                             " Ya existente
  ord_dep      = lv_knafa                             " Ya existente
  rev_ord_dep  = lv_kaufn                             " Ya existente
  down_payment = lv_kanza                             " NUEVO: KANZA (capitalización año)
  currency     = 'COP'                                " Ya existente
) TO lt_cumulatedvalues.
```

**4. BAPIPAREX (Extensión para Campos Z):**
```abap
DATA: lt_extensionin TYPE TABLE OF bapiparex.

" Estructura de ampliación (debe verificarse en sistema)
" Puede ser CI_ANLA o ZZSTRUCTURE creada en customizing

APPEND VALUE #(
  structure  = 'CI_ANLA'                              " O estructura append correspondiente
  valuepart1 = |ZMUNICIPIUM={ lv_municipio }|
  valuepart2 = |ZMATRICULA_INMOB={ lv_matricula }|
  valuepart3 = |ZFICHA_CATASTRAL={ lv_ficha }|
  valuepart4 = |ZANIO_IMPUESTO={ lv_anio_imp }|
) TO lt_extensionin.

APPEND VALUE #(
  structure  = 'CI_ANLA'
  valuepart1 = |ZVALOR_IMPUESTO={ lv_valor_imp }|
  valuepart2 = |ZFECHA_PAGO_IMP={ lv_fecha_imp }|
  valuepart3 = |ZCOMODATARIO={ lv_comodatario }|
  valuepart4 = |ZCONTRATO_COMODA={ lv_contrato }|
) TO lt_extensionin.

APPEND VALUE #(
  structure  = 'CI_ANLA'
  valuepart1 = |ZFECHA_INICIO_COM={ lv_fecha_ini }|
  valuepart2 = |ZFECHA_FIN_COM={ lv_fecha_fin }|
) TO lt_extensionin.
```

#### **Métodos de la Clase a Actualizar:**

**Clase:** `ZCLFIAAC002_CARGA_ACTIVOS_FIJ`

**Métodos a modificar:**

1. **mp_key** - Agregar campo `nassets`
   ```abap
   METHOD mp_key.
     e_es_key-companycode = i_es_dato_actf-companycode.
     e_es_key-asset       = i_es_dato_actf-asset.
     e_es_key-subnumber   = i_es_dato_actf-subnumber.
     e_es_key-nassets     = i_es_dato_actf-nassets.      " NUEVO
   ENDMETHOD.
   ```

2. **mp_timedependentdata** - Agregar 5 campos nuevos
   ```abap
   METHOD mp_timedependentdata.
     " ... lógica existente ...
     e_es_timedependentdata-room       = i_es_dato_actf-room.        " NUEVO
     e_es_timedependentdata-fund       = i_es_dato_actf-fund.        " NUEVO
     e_es_timedependentdata-funds_ctr  = i_es_dato_actf-funds_center." NUEVO
     e_es_timedependentdata-profit_ctr = i_es_dato_actf-profit_center." NUEVO
     e_es_timedependentdata-segment    = i_es_dato_actf-segment.    " NUEVO
   ENDMETHOD.
   ```

3. **mp_allocations** - Agregar campo `assetsupno`
   ```abap
   METHOD mp_allocations.
     " ... lógica existente ...
     e_es_allocations-assetsupno = i_es_dato_actf-super_number.     " NUEVO (ANLUE)
   ENDMETHOD.
   ```

4. **mp_cumulatedvalues** - Agregar campo `down_payment`
   ```abap
   METHOD mp_cumulatedvalues.
     DATA: ls_cumval TYPE bapi1022_cumval.

     " ... lógica existente para área de valoración ...
     ls_cumval-down_payment = i_es_dato_actf-capitalization_year.   " NUEVO (KANZA)

     APPEND ls_cumval TO e_ti_cumulatedvalues.
   ENDMETHOD.
   ```

5. **mp_extensionin** - CREAR MÉTODO NUEVO para campos Z
   ```abap
   METHOD mp_extensionin.
     DATA: ls_extension TYPE bapiparex.

     " Validar que exista estructura append CI_ANLA o crear estructura custom
     " Formato: CAMPO=VALOR separados por pipe o coma

     IF i_es_dato_actf-zmunicipium IS NOT INITIAL.
       ls_extension-structure  = 'CI_ANLA'.
       ls_extension-valuepart1 = |ZMUNICIPIUM={ i_es_dato_actf-zmunicipium }|.
       ls_extension-valuepart2 = |ZMATRICULA_INMOB={ i_es_dato_actf-zmatricula_inmob }|.
       ls_extension-valuepart3 = |ZFICHA_CATASTRAL={ i_es_dato_actf-zficha_catastral }|.
       ls_extension-valuepart4 = |ZANIO_IMPUESTO={ i_es_dato_actf-zanio_impuesto }|.
       APPEND ls_extension TO e_ti_extensionin.
       CLEAR ls_extension.
     ENDIF.

     IF i_es_dato_actf-zvalor_impuesto IS NOT INITIAL.
       ls_extension-structure  = 'CI_ANLA'.
       ls_extension-valuepart1 = |ZVALOR_IMPUESTO={ i_es_dato_actf-zvalor_impuesto }|.
       ls_extension-valuepart2 = |ZFECHA_PAGO_IMP={ i_es_dato_actf-zfecha_pago_imp }|.
       ls_extension-valuepart3 = |ZCOMODATARIO={ i_es_dato_actf-zcomodatario }|.
       ls_extension-valuepart4 = |ZCONTRATO_COMODA={ i_es_dato_actf-zcontrato_comoda }|.
       APPEND ls_extension TO e_ti_extensionin.
       CLEAR ls_extension.
     ENDIF.

     IF i_es_dato_actf-zfecha_inicio_com IS NOT INITIAL.
       ls_extension-structure  = 'CI_ANLA'.
       ls_extension-valuepart1 = |ZFECHA_INICIO_COM={ i_es_dato_actf-zfecha_inicio_com }|.
       ls_extension-valuepart2 = |ZFECHA_FIN_COM={ i_es_dato_actf-zfecha_fin_com }|.
       APPEND ls_extension TO e_ti_extensionin.
     ENDIF.

   ENDMETHOD.
   ```

6. **ejecutar_bapi_crear** - Agregar parámetro `extensionin`
   ```abap
   METHOD ejecutar_bapi_crear.
     DATA: lt_extensionin TYPE TABLE OF bapiparex.

     " ... código existente ...

     " Mapear campos Z a extensión
     lt_extensionin = mp_extensionin( i_es_dato_actf ).

     CALL FUNCTION 'BAPI_FIXEDASSET_OVRTAKE_CREATE'
       EXPORTING
         key                 = ls_key
         generaldata         = ls_generaldata
         " ... otros parámetros ...
       TABLES
         depreciationareas   = lt_depreciationareas
         cumulatedvalues     = lt_cumulatedvalues
         extensionin         = lt_extensionin              " NUEVO
         return              = lt_return.
   ENDMETHOD.
   ```

---

## 4. REQUERIMIENTOS FUNCIONALES

### 4.1 Ajuste de Estructura ZEFIAAC002_1

**RF-01: Extender Estructura ZEFIAAC002_1**

Agregar **17 campos nuevos** a la estructura ZEFIAAC002_1:

**A. Campos SAP Estándar (7 campos):**
```abap
nassets             : char3,        "NASSETS - Cantidad activos (siempre 1)
room                : raumn,        "RAUMN - Local/sala
fund                : geber,        "GEBER - Fondo (heredado CeCo)
funds_center        : fistl,        "FISTL - Centro gestor (heredado CeCo)
profit_center       : prctr,        "PRCTR - Centro beneficio (heredado CeCo)
segment             : segment,      "SEGMENT - Segmento (heredado CeCo)
super_number        : anlue,        "ANLUE - Supranúmero (cédula empleado)
capitalization_year : kanza         "KANZA - Capitalización año en curso
```

**B. Campos de Ampliación Z (10 campos):**
```abap
zmunicipium        : char50,   "Z - Municipio
zmatricula_inmob   : char50,   "Z - Matrícula inmobiliaria
zficha_catastral   : char50,   "Z - Ficha catastral
zanio_impuesto     : gjahr,    "Z - Año del Impuesto
zvalor_impuesto    : curr23_2, "Z - Valor pagado impuesto predial
zfecha_pago_imp    : datum,    "Z - Fecha pago impuesto predial
zcomodatario       : char50,   "Z - Comodatario
zcontrato_comoda   : char50,   "Z - Contrato comodato número
zfecha_inicio_com  : datum,    "Z - Fecha inicio comodato
zfecha_fin_com     : datum     "Z - Fecha terminación comodato
```

**Total de campos en ZEFIAAC002_1:** 81 (actuales) + 17 (nuevos) = **98 campos**

**RF-02: Mapeo de Campos Heredados**

Los siguientes campos se heredan automáticamente del CeCo y NO deben cargarse desde el CSV:
- GEBER (Fondo)
- FISTL (Centro gestor)
- PS_PSP_PNR2 (Elemento PEP)
- PRCTR (Centro de beneficio)
- SEGMENT (Segmento)

**RF-03: Validación de Campos Obligatorios**

Validar que los siguientes campos obligatorios tengan valores:
- ANLN1, BUKRS, ANLKL (Identificación básica)
- TXT50, SERNR, AKTIV (Descripción y fechas)
- KOSTL, KOSTLV (Organización)
- AFABE, AFASL, GJAHR, CURRENCY (Valoración)
- KANSW (Valor de adquisición)

### 4.2 Ajuste del Proceso de Carga

**RF-04: Lectura de Nueva Estructura CSV**

- Saltar las primeras 5 líneas de encabezado
- Parsear 65 campos por registro
- Validar tipos de datos según longitud especificada
- Manejo de campos vacíos según obligatoriedad

**RF-05: Transformación de Datos**

| Transformación | Detalle |
|----------------|---------|
| Fechas | Convertir formato DD.MM.AAAA a YYYYMMDD |
| Números | Eliminar separadores de miles, ajustar decimales |
| Moneda | Forzar COP como moneda |
| Ejercicio | Forzar 2025 como ejercicio actual |
| NASSETS | Forzar valor 1 (cantidad de activos) |
| INKEN | Forzar valor 'X' (indicador inventario) |

**RF-06: Actualización de Llamadas BAPI**

Ajustar las llamadas a las BAPIs para incluir los nuevos campos en las estructuras correspondientes:

**BAPI_FIXEDASSET_OVRTAKE_CREATE - Actualización Completa:**
```abap
METHOD ejecutar_bapi_crear.
  DATA: ls_key                 TYPE bapi1022_key,
        ls_generaldata         TYPE bapi1022_feglg001,
        ls_timedependentdata   TYPE bapi1022_feglg003,        " ACTUALIZADO con 5 campos nuevos
        ls_allocations         TYPE bapi1022_feglg004,        " ACTUALIZADO con assetsupno
        lt_depreciationareas   TYPE TABLE OF bapi1022_dep_areas,
        lt_cumulatedvalues     TYPE TABLE OF bapi1022_cumval, " ACTUALIZADO con down_payment
        lt_extensionin         TYPE TABLE OF bapiparex,       " NUEVO para campos Z
        lt_return              TYPE TABLE OF bapiret2.

  " 1. KEY - Agregar NASSETS
  ls_key = mp_key( i_es_dato_actf ).

  " 2. GENERAL DATA (sin cambios)
  ls_generaldata = mp_generaldata( i_es_dato_actf ).

  " 3. TIME DEPENDENT DATA - 5 campos nuevos
  ls_timedependentdata = mp_timedependentdata( i_es_dato_actf ).
  " Campos nuevos: room, fund, funds_ctr, profit_ctr, segment

  " 4. ALLOCATIONS - 1 campo nuevo
  ls_allocations = mp_allocations( i_es_dato_actf ).
  " Campo nuevo: assetsupno (ANLUE - cédula empleado)

  " 5. CUMULATED VALUES - 1 campo nuevo
  lt_cumulatedvalues = mp_cumulatedvalues( i_es_dato_actf ).
  " Campo nuevo: down_payment (KANZA - capitalización año)

  " 6. EXTENSION - 10 campos Z nuevos
  lt_extensionin = mp_extensionin( i_es_dato_actf ).
  " Campos Z: municipio, matrícula, ficha catastral, impuestos, comodato

  CALL FUNCTION 'BAPI_FIXEDASSET_OVRTAKE_CREATE'
    EXPORTING
      key                 = ls_key                    " ACTUALIZADO
      createsubnumber     = i_createsubnumber
      testrun             = i_testrun
      generaldata         = ls_generaldata
      generaldatax        = ls_generaldatax
      inventory           = ls_inventory
      inventoryx          = ls_inventoryx
      postinginformation  = ls_postinginformation
      postinginformationx = ls_postinginformationx
      timedependentdata   = ls_timedependentdata      " ACTUALIZADO
      timedependentdatax  = ls_timedependentdatax
      allocations         = ls_allocations            " ACTUALIZADO
      allocationsx        = ls_allocationsx
      origin              = ls_origin
      originx             = ls_originx
    IMPORTING
      companycode         = e_companycode
      asset               = e_asset
      subnumber           = e_subnumber
      assetcreated        = e_assetcreated
    TABLES
      depreciationareas   = lt_depreciationareas
      depreciationareasx  = lt_depreciationareasx
      cumulatedvalues     = lt_cumulatedvalues        " ACTUALIZADO
      postedvalues        = lt_postedvalues
      extensionin         = lt_extensionin            " NUEVO
      return              = lt_return.

  IF line_exists( lt_return[ type = 'E' ] ).
    " Manejo de errores
  ELSE.
    CALL FUNCTION 'BAPI_TRANSACTION_COMMIT'
      EXPORTING
        wait = 'X'.
  ENDIF.
ENDMETHOD.
```

**Resumen de Cambios en RF-06:**
- **KEY:** Agregar campo `nassets` (siempre 1)
- **TIMEDEPENDENTDATA:** Agregar 5 campos (room, fund, funds_ctr, profit_ctr, segment)
- **ALLOCATIONS:** Agregar 1 campo (assetsupno)
- **CUMULATEDVALUES:** Agregar 1 campo (down_payment)
- **EXTENSIONIN:** Crear nuevo método para 10 campos Z

**BAPI_FIXEDASSET_CHANGE:**
```abap
" Modificar datos maestros con campos Z
CALL FUNCTION 'BAPI_FIXEDASSET_CHANGE'
  EXPORTING
    companycode        = lv_bukrs
    asset              = lv_anln1
    generaldata        = ls_generaldata
    timedependentdata  = ls_timedependentdata   " ACTUALIZADO
    allocations        = ls_allocations         " ACTUALIZADO
  TABLES
    extensionin        = lt_extensionin         " NUEVO
    return             = lt_return.
```

### 4.3 Manejo de Áreas de Valoración

**RF-07: Procesar Múltiples Áreas**

El CSV puede tener múltiples filas para el mismo activo (una por área de valoración):
- Área 1: Valoración contable principal
- Áreas 17/18: Revalorización (requiere clave de revalorización)

Lógica:
1. Agrupar registros CSV por ANLN1+ANLN2+BUKRS
2. Crear maestro de activo UNA sola vez (BAPI_ASSET_CREATE)
3. Procesar saldos por cada área (BAPI_ASSET_POSTING_POST)

---

## 5. REQUERIMIENTOS NO FUNCIONALES

**RNF-01: Performance**
- Procesar archivos con hasta 15,000 registros
- Tiempo máximo: 30 minutos en modo batch
- Commit cada 1000 registros

**RNF-02: Logging**
- Log detallado de errores por campo y registro
- ALV con resumen de carga (exitosos, errores, warnings)
- Exportación de log a archivo

**RNF-03: Seguridad**
- Verificación de autorización para transacción AS01/AS91
- Validación de sociedad autorizada
- Modo de prueba (no commit) obligatorio en primer intento

**RNF-04: Compatibilidad**
- Mantener compatibilidad con archivos antiguos (modo legacy)
- Estructura ZEFIAAC002_1 debe ser retrocompatible

---

## 6. CRITERIOS DE ACEPTACIÓN

### Fase 1: Análisis (Completado ✅)
- ✅ Análisis de estructura actual ZEFIAAC002_1
- ✅ Análisis de nueva estructura CSV (65 campos)
- ✅ Identificación de campos nuevos y cambios
- ✅ Mapeo completo de campos técnicos
- ✅ Identificación de BAPIs y proceso de carga

### Fase 2: Diseño (Pendiente)
- [ ] Diseño técnico de ampliaciones Z en DDIC
- [ ] Diseño de rutina de lectura CSV (5 líneas de header)
- [ ] Diseño de transformación y validación de datos
- [ ] Diseño de llamadas a BAPI con nuevos campos
- [ ] Diseño de logging y reporte de errores

### Fase 3: Implementación (Pendiente)
- [ ] Extender estructura ZEFIAAC002_1 con campos Z
- [ ] Actualizar rutina de lectura CSV
- [ ] Implementar validaciones de campos obligatorios
- [ ] Actualizar llamadas BAPI_ASSET_CREATE
- [ ] Implementar manejo de múltiples áreas de valoración
- [ ] Actualizar ALV de resultados

### Fase 4: Testing (Pendiente)
- [ ] Prueba unitaria con 10 registros
- [ ] Prueba con archivo completo (modo test)
- [ ] Validación de datos creados en AS03
- [ ] Prueba de rollback en caso de error
- [ ] Prueba de performance con 15,000 registros

### Fase 5: Despliegue (Pendiente)
- [ ] Documentación de usuario
- [ ] Capacitación a usuarios finales
- [ ] Migración en ambiente de calidad
- [ ] Aprobación de pruebas integradas
- [ ] Pase a productivo

---

## 7. RIESGOS Y CONSIDERACIONES

### 7.1 Riesgos Técnicos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Campos Z no existen en sistema | Media | Alto | Crear append structure o CI_ANLA |
| BAPIs no soportan campos Z | Alta | Alto | Usar tabla EXTENSIONIN de BAPI |
| Performance con 15K registros | Media | Medio | Commits parciales, parallel cursor |
| Datos inválidos en CSV | Alta | Medio | Validación exhaustiva pre-BAPI |

### 7.2 Consideraciones Funcionales

1. **Múltiples Áreas de Valoración:** Confirmar con Funcional si el CSV tiene múltiples filas por activo o una sola fila con campos repetidos por área

2. **Campos Heredados:** Verificar que FONDO, FISTL, etc. realmente se heredan del CeCo o deben cargarse explícitamente

3. **Número de Inventario:** Confirmado como automático, pero validar si SAP genera o debe asignarse

4. **Valores Negativos:** La amortización debe cargarse como negativa (ej: -10000)

---

## 8. ESTIMACIÓN DE ESFUERZO

| Fase | Esfuerzo (horas) | Responsable |
|------|------------------|-------------|
| Diseño Técnico | 8h | Desarrollador ABAP |
| Creación campos Z | 4h | Desarrollador ABAP + Basis |
| Desarrollo carga CSV | 12h | Desarrollador ABAP |
| Ajuste BAPIs | 8h | Desarrollador ABAP |
| Testing unitario | 8h | Desarrollador ABAP |
| Testing integrado | 16h | Equipo Funcional + Técnico |
| Documentación | 4h | Desarrollador ABAP |
| **TOTAL** | **60h** | **~8 días laborales** |

---

## 9. DOCUMENTACIÓN TÉCNICA DE REFERENCIA

### 9.1 Objetos SAP Involucrados

**Programas:**
- ZFIAAC002 - Programa principal de carga
- ZFIAAC002V_1 - Variables y declaraciones
- ZFIAAC002CD1_1 - Definiciones de clases
- ZFIAAC002CI1_1 - Implementación de clases

**Estructuras:**
- ZEFIAAC002_1 - Estructura de datos de activo fijo (a extender)

**BAPIs:**
- BAPI_FIXEDASSET_OVRTAKE_CREATE - Creación con migración de saldos
- BAPI_FIXEDASSET_CHANGE - Modificación de datos maestros
- BAPI_FIXEDASSET_OVRTAKE_POST - Contabilización de valores
- BAPI_ASSET_REVERSAL_POST - Anulación de movimientos

**Tablas SAP Estándar:**
- ANLA - Activos fijos: Datos maestros generales
- ANLH - Datos del activo fijo: Denominación de activo principal
- ANLZ - Activos fijos: Asignación temporalmente dependiente
- ANLAB - Activos fijos: Áreas de amortización
- ANLB - Activos fijos: Valores de amortización
- ANLC - Activos fijos: Valores de amortización (operaciones del año)

### 9.2 Transacciones SAP

- AS01 - Crear activo fijo (individual)
- AS91 - Crear activo fijo (carga masiva)
- AS03 - Visualizar activo fijo
- ABAA - Informe de activos fijos
- SE11 - Dictionary ABAP (para extender estructura)
- SE38 - Editor ABAP (para modificar programa)

---

## 10. PREGUNTAS PARA FUNCIONAL

Antes de iniciar la Fase 2 (Diseño), se requiere clarificar:

1. **Campos Z:**
   - ¿Los campos Z ya existen en el sistema o deben crearse?
   - ¿Hay un append structure CI_ANLA configurado?
   - ¿Qué tipo de datos debe tener cada campo Z?

2. **Múltiples Áreas:**
   - ¿El CSV tiene múltiples filas por activo (una por área)?
   - ¿O tiene una sola fila con valores repetidos para cada área?
   - ¿Qué áreas de valoración se utilizan en la empresa?

3. **Campos Heredados:**
   - Confirmar que FONDO, FISTL, PEP, PRCTR, SEGMENT se heredan del CeCo
   - ¿O deben cargarse explícitamente desde el CSV?

4. **Validaciones:**
   - ¿Hay validaciones adicionales de negocio (ej: KOSTL debe existir)?
   - ¿Hay reglas de negocio para valores residuales?

5. **Testing:**
   - ¿Hay un ambiente de testing con datos de prueba?
   - ¿Quién validará los datos cargados en AS03?

---

## 11. PRÓXIMOS PASOS

### Inmediatos (Post-Análisis)
1. ✅ Revisar documento refinado con el equipo
2. ⏳ Obtener respuestas a preguntas funcionales
3. ⏳ Validar estimación de esfuerzo
4. ⏳ Aprobar inicio de Fase 2 (Diseño)

### Fase 2 (Diseño)
1. Crear documento de diseño técnico detallado
2. Diseñar estructura de campos Z en SE11
3. Diseñar clases y métodos para nueva lógica
4. Crear mockups de pantallas de selección y ALV
5. Validar diseño con arquitecto técnico

---

## 12. CONTROL DE CAMBIOS

| Fecha | Versión | Autor | Cambio |
|-------|---------|-------|--------|
| 2025-10-28 | 1.0 | Claude AI | Análisis inicial y refinamiento de requerimiento |
| 2025-10-28 | 1.1 | Claude AI | Mapeo completo de 65 campos del CSV |
| 2025-10-28 | 1.2 | Claude AI | Análisis GAP y requerimientos funcionales detallados |
| 2025-10-28 | 1.3 | Claude AI | **Corrección crítica:** Actualización de BAPIs reales utilizadas en el código |
| 2025-10-28 | 1.4 | Claude AI | **Mapeo exacto CSV → ZEFIAAC002_1:** Tabla completa de 58 campos con estados (✅Match, 🔄Mapeo, ❌Faltante, ⚠️CampoZ), identificación precisa de 17 campos a agregar (7 SAP + 10 Z) |
| 2025-10-28 | 1.5 | Claude AI | **Mapeo completo BAPI:** Nueva sección 3.4 con tabla completa de mapeo de 17 campos faltantes a parámetros BAPI (KEY, FEGLG003, FEGLG004, CUMVAL, BAPIPAREX). Incluye código ABAP detallado para actualizar 6 métodos de la clase ZCLFIAAC002_CARGA_ACTIVOS_FIJ. Actualización de RF-06 con implementación completa de BAPI_FIXEDASSET_OVRTAKE_CREATE. |

---

**Estado del Documento:** ✅ **LISTO PARA REVISIÓN**
**Próxima Acción:** Reunión de validación con equipo funcional y técnico

**Contenido Completo:**
- ✅ Análisis de situación actual (programa ZFIAAC002 y estructura ZEFIAAC002_1)
- ✅ Análisis de nueva estructura CSV (65 campos, 5 filas de encabezado)
- ✅ Mapeo exacto de 58 campos CSV → Estructura (Sección 3.1)
- ✅ Identificación de 17 campos faltantes (7 SAP + 10 Z)
- ✅ Mapeo completo de campos faltantes a estructuras BAPI (Sección 3.4)
- ✅ Código ABAP detallado para actualizar métodos de clase
- ✅ Requerimientos funcionales y no funcionales
- ✅ Criterios de aceptación por fases
- ✅ Estimación de esfuerzo (60h / 8 días laborales)
- ✅ Preguntas para funcional antes de iniciar Fase 2

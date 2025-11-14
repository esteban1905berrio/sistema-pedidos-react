# ABAP Function Module Development Rules

## ⚠️ REGLA FUNDAMENTAL: Firmas sin Comentarios

### ❌ NUNCA HACER ESTO:
```abap
FUNCTION ZCX_GETDDICSOURCE.
*"----------------------------------------------------------------------
*"*"Local Interface:
*"  IMPORTING
*"     VALUE(OBJECT_NAME) TYPE  TABNAME
*"  EXPORTING
*"     VALUE(OBJECT_TYPE) TYPE  CHAR10
*"  EXCEPTIONS
*"      OBJECT_NOT_FOUND
*"----------------------------------------------------------------------
```

**Error**: `Parameter comment blocks are not allowed` (HTTP 400)

### ✅ SIEMPRE HACER ESTO:
```abap
FUNCTION ZCX_GETDDICSOURCE
  IMPORTING
    VALUE(OBJECT_NAME) TYPE TABNAME
  EXPORTING
    VALUE(OBJECT_TYPE) TYPE CHAR10
    VALUE(OBJECT_STATUS) TYPE CHAR10
    VALUE(FIELDS_JSON) TYPE STRING
  EXCEPTIONS
    OBJECT_NOT_FOUND
    INVALID_OBJECT_TYPE.
```

---

## Reglas de Firma de Function Modules

### 1. **Sin Comentarios en Firma**
- ❌ NO usar comentarios `*"` en sección de parámetros
- ❌ NO usar bloques decorativos `*"----`
- ❌ NO usar `*"*"Local Interface:`
- ✅ Solo código limpio, sin comentarios

### 2. **Formato de Parámetros**

```abap
FUNCTION <NOMBRE_FM>
  IMPORTING
    VALUE(<PARAM1>) TYPE <TIPO1>
    VALUE(<PARAM2>) TYPE <TIPO2>
    REFERENCE(<PARAM3>) TYPE <TIPO3>
  EXPORTING
    VALUE(<PARAM4>) TYPE <TIPO4>
  CHANGING
    VALUE(<PARAM5>) TYPE <TIPO5>
  TABLES
    <TABLE1> STRUCTURE <ESTRUCTURA1>
  EXCEPTIONS
    <EXCEPCION1>
    <EXCEPCION2>.
```

**Características**:
- Sin comentarios
- Indentación de 2 o 4 espacios
- Un parámetro por línea
- Punto final (`.`) después de excepciones o último parámetro

### 3. **VALUE vs REFERENCE**

```abap
IMPORTING
  VALUE(IV_MATNR) TYPE MATNR          " Pasa copia (recomendado)
  REFERENCE(IR_DATA) TYPE REF TO DATA " Pasa referencia

EXPORTING
  VALUE(EV_RESULT) TYPE STRING        " Devuelve copia (recomendado)
  REFERENCE(ER_TABLE) TYPE REF TO DATA
```

**Regla general**: Usar `VALUE()` por defecto para parámetros simples.

### 4. **Naming Conventions**

| Tipo      | Prefijo | Ejemplo                  |
|-----------|---------|--------------------------|
| Import    | IV_     | IV_OBJECT_NAME           |
| Export    | EV_     | EV_OBJECT_TYPE           |
| Changing  | CV_     | CV_STATUS                |
| Table     | IT_/ET_ | IT_FIELDS, ET_RESULTS    |
| Reference | IR_/ER_ | IR_DATA, ER_OBJECT       |

**Excepción**: Cuando el parámetro es obvio, se puede omitir prefijo:
```abap
IMPORTING
  VALUE(OBJECT_NAME) TYPE TABNAME   " Nombre simple y claro
```

### 5. **Excepciones**

```abap
EXCEPTIONS
  OBJECT_NOT_FOUND
  INVALID_OBJECT_TYPE
  AUTHORIZATION_FAILED
  SYSTEM_ERROR.
```

**Reglas**:
- Sin comentarios
- Sin parámetros (las excepciones ABAP no llevan parámetros)
- Nombres en MAYÚSCULAS
- Usar guiones bajos para separar palabras

---

## Implementación de Firmas

### Método 1: Via SE37 (Recomendado para Producción)

1. Crear FM con código básico
2. Configurar firma en SE37 GUI
3. Activar
4. Actualizar código vía ADT/modify_function_module

**Ventajas**:
- No hay problemas con comentarios
- Validación automática de tipos
- Interface gráfica intuitiva

### Método 2: Via Código (Solo desarrollo local)

```abap
" Crear FM vacío primero
" Luego modificar con firma SIN COMENTARIOS

FUNCTION Z_MI_FM
  IMPORTING
    VALUE(IV_INPUT) TYPE STRING
  EXPORTING
    VALUE(EV_OUTPUT) TYPE STRING
  EXCEPTIONS
    INPUT_INVALID.

  " Implementación aquí

ENDFUNCTION.
```

---

## Ejemplos Completos

### Ejemplo 1: FM Simple
```abap
FUNCTION Z_GET_MATERIAL_DESC
  IMPORTING
    VALUE(IV_MATNR) TYPE MATNR
  EXPORTING
    VALUE(EV_MAKTX) TYPE MAKTX
  EXCEPTIONS
    MATERIAL_NOT_FOUND.

  SELECT SINGLE maktx
    FROM makt
    INTO ev_maktx
    WHERE matnr = iv_matnr
      AND spras = sy-langu.

  IF sy-subrc <> 0.
    RAISE material_not_found.
  ENDIF.

ENDFUNCTION.
```

### Ejemplo 2: FM con Tabla
```abap
FUNCTION Z_GET_MATERIALS_LIST
  IMPORTING
    VALUE(IV_WERKS) TYPE WERKS_D
  TABLES
    ET_MATERIALS STRUCTURE MARA
  EXCEPTIONS
    NO_DATA_FOUND.

  SELECT *
    FROM mara
    INTO TABLE et_materials
    WHERE werks = iv_werks.

  IF sy-subrc <> 0.
    RAISE no_data_found.
  ENDIF.

ENDFUNCTION.
```

### Ejemplo 3: FM con JSON (ZCX_GETDDICSOURCE)
```abap
FUNCTION ZCX_GETDDICSOURCE
  IMPORTING
    VALUE(OBJECT_NAME) TYPE TABNAME
  EXPORTING
    VALUE(OBJECT_TYPE) TYPE CHAR10
    VALUE(OBJECT_STATUS) TYPE CHAR10
    VALUE(FIELDS_JSON) TYPE STRING
  EXCEPTIONS
    OBJECT_NOT_FOUND
    INVALID_OBJECT_TYPE.

  DATA: lv_tabclass TYPE dd02l-tabclass,
        lt_fields TYPE STANDARD TABLE OF dd03l,
        ls_field TYPE dd03l,
        lv_json TYPE string.

  " Implementación...

ENDFUNCTION.
```

---

## Validación Automática

### Pre-commit Check (Futuro)
```bash
# Script para validar FMs antes de commit
grep -n '\*".*Local Interface' function_module.abap
# Si encuentra match: FAIL
```

### CI/CD Pipeline
```yaml
# .gitlab-ci.yml
validate_fm_signatures:
  script:
    - ./scripts/validate_abap_signatures.sh
    - if grep -r '\*".*IMPORTING\|\*".*EXPORTING' *.abap; then
        echo "ERROR: Comments found in FM signature"
        exit 1
      fi
```

---

## Troubleshooting

### Error: "Parameter comment blocks are not allowed"

**Causa**: Comentarios `*"` en sección de firma

**Solución**:
```abap
" ❌ MAL
FUNCTION Z_FM.
*"----------------------------------------------------------------------
*"*"Local Interface:
*"  IMPORTING
*"     VALUE(IV_PARAM) TYPE STRING
*"----------------------------------------------------------------------

" ✅ BIEN
FUNCTION Z_FM
  IMPORTING
    VALUE(IV_PARAM) TYPE STRING.
```

### Error: "Syntax error in FUNCTION signature"

**Causa**: Falta punto final (`.`) o formato incorrecto

**Solución**:
```abap
" ❌ MAL
FUNCTION Z_FM
  IMPORTING
    VALUE(IV_PARAM) TYPE STRING

" ✅ BIEN
FUNCTION Z_FM
  IMPORTING
    VALUE(IV_PARAM) TYPE STRING.
```

---

## Checklist de Revisión

Antes de hacer commit/activar un FM:

- [ ] ❌ Sin comentarios `*"` en firma
- [ ] ✅ Firma limpia (solo keywords y parámetros)
- [ ] ✅ Punto final (`.`) al final de firma
- [ ] ✅ Indentación consistente (2-4 espacios)
- [ ] ✅ Tipos válidos (TABNAME, STRING, CHAR10, etc.)
- [ ] ✅ VALUE() para parámetros simples
- [ ] ✅ Excepciones sin parámetros
- [ ] ✅ Nombres descriptivos

---

## Referencias

- SAP ADT API: No permite comentarios en firmas de FM
- SE37 Transaction: Genera firmas sin comentarios
- ABAP Naming Conventions: https://help.sap.com/docs/naming-conventions

---

**Última actualización**: 2025-11-14
**Autor**: Crystal Development Team
**Regla crítica**: NUNCA comentarios en firmas de FM

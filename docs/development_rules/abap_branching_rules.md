# ABAP Branching Rules - Evitar Lógica Redundante

## Regla Principal

> **En bifurcaciones (IF/ELSE, CASE), solo debe estar dentro de cada rama el código que realmente difiere. El código común debe extraerse antes, después, o a un FORM/método.**

---

## Anti-Patrón: Código Duplicado en Ramas

```abap
" ❌ MAL - Código común duplicado en cada rama
IF lv_condicion = abap_true.
  lv_pattern = iv_input.                           " común
  REPLACE ALL OCCURRENCES OF '*' IN lv_pattern.    " común
  TRANSLATE lv_pattern TO UPPER CASE.              " común

  SELECT * FROM tabla_a INTO TABLE lt_results      " específico
    WHERE campo LIKE lv_pattern.

  IF sy-subrc <> 0.                                " común
    ev_message = |No data found|.                  " común
    RETURN.                                        " común
  ENDIF.                                           " común

  ev_json = /ui2/cl_json=>serialize( lt_results ). " común
ELSE.
  lv_pattern = iv_input.                           " DUPLICADO
  REPLACE ALL OCCURRENCES OF '*' IN lv_pattern.    " DUPLICADO
  TRANSLATE lv_pattern TO UPPER CASE.              " DUPLICADO

  SELECT * FROM tabla_b INTO TABLE lt_results      " específico
    WHERE campo LIKE lv_pattern.

  IF sy-subrc <> 0.                                " DUPLICADO
    ev_message = |No data found|.                  " DUPLICADO
    RETURN.                                        " DUPLICADO
  ENDIF.                                           " DUPLICADO

  ev_json = /ui2/cl_json=>serialize( lt_results ). " DUPLICADO
ENDIF.
```

---

## Patrón Correcto: Código Común Extraído

```abap
" ✅ BIEN - Código común antes/después de bifurcación

" Código común ANTES
lv_pattern = iv_input.
REPLACE ALL OCCURRENCES OF '*' IN lv_pattern WITH '%'.
TRANSLATE lv_pattern TO UPPER CASE.

" Bifurcación con SOLO código específico
IF lv_condicion = abap_true.
  SELECT * FROM tabla_a INTO TABLE lt_results
    WHERE campo LIKE lv_pattern.
ELSE.
  SELECT * FROM tabla_b INTO TABLE lt_results
    WHERE campo LIKE lv_pattern.
ENDIF.

" Código común DESPUÉS
IF sy-subrc <> 0.
  ev_message = |No data found|.
  RETURN.
ENDIF.

ev_json = /ui2/cl_json=>serialize( lt_results ).
```

---

## Variantes del Patrón

### Variante A: Código Común en FORMs

Cuando el código común es complejo o se usa en múltiples FMs:

```abap
" Código común en FORM (TOP include)
PERFORM preparar_patron USING iv_input CHANGING lv_pattern.

" Bifurcación mínima
IF lv_tipo = 'A'.
  SELECT * FROM tabla_a INTO TABLE lt_results WHERE campo LIKE lv_pattern.
ELSEIF lv_tipo = 'B'.
  SELECT * FROM tabla_b INTO TABLE lt_results WHERE campo LIKE lv_pattern.
ENDIF.

" Código común en FORM
PERFORM procesar_resultados USING lt_results CHANGING ev_json ev_message.
```

### Variante B: CASE con Código Específico Mínimo

```abap
" Preparación común
DATA(lv_tabla) = determine_table( iv_tipo ).

" Bifurcación solo para lo que varía
CASE iv_tipo.
  WHEN 'ENH'.
    SELECT enhname FROM enhobj INTO TABLE @lt_results WHERE enhname LIKE @lv_pattern.
  WHEN 'BADI'.
    SELECT imp_name FROM sxc_attr INTO TABLE @lt_results WHERE imp_name LIKE @lv_pattern.
  WHEN 'DMEE'.
    SELECT tree_id FROM dmee_tree INTO TABLE @lt_results WHERE tree_id LIKE @lv_pattern.
ENDCASE.

" Procesamiento común
PERFORM serializar_respuesta USING lt_results CHANGING ev_json.
```

### Variante C: Dynamic SELECT (Avanzado)

Cuando la única diferencia es la tabla/campos:

```abap
" Determinar tabla dinámicamente
CASE iv_tipo.
  WHEN 'ENH'.  lv_tabla = 'ENHOBJ'.   lv_campo = 'ENHNAME'.
  WHEN 'BADI'. lv_tabla = 'SXC_ATTR'. lv_campo = 'IMP_NAME'.
ENDCASE.

" SELECT dinámico (sin bifurcación)
SELECT (lv_campo) FROM (lv_tabla) INTO TABLE @lt_results
  WHERE (lv_campo) LIKE @lv_pattern.

" Procesamiento común
ev_json = /ui2/cl_json=>serialize( lt_results ).
```

---

## Checklist de Revisión

Antes de commit, verificar en cada bifurcación:

- [ ] ¿Hay código IDÉNTICO en múltiples ramas?
- [ ] ¿Ese código puede moverse ANTES de la bifurcación?
- [ ] ¿Ese código puede moverse DESPUÉS de la bifurcación?
- [ ] ¿El código común debería extraerse a un FORM?
- [ ] ¿Solo queda código ESPECÍFICO dentro de cada rama?

---

## Beneficios

| Aspecto | Con Duplicación | Sin Duplicación |
|---------|-----------------|-----------------|
| Mantenimiento | Cambiar en N lugares | Cambiar en 1 lugar |
| Errores | Fácil olvidar una rama | Imposible inconsistencia |
| Legibilidad | Difícil ver diferencias | Diferencias evidentes |
| Testing | Testear N copias | Testear 1 vez |

---

**Última actualización**: 2025-12-15
**Aplica a**: Todo código ABAP con bifurcaciones

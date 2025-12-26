*&---------------------------------------------------------------------*
*& Programa             ZCXR1006_1
*& RICEFW               R1006
*& Autor                JMVALENC
*& Descripción          Monitor Mantenimiento tablas Z
*& Fecha                5/04/2021
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 5/04/2021   S4DK900384  Jose Miguel Valencia       Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxr1006_1 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& Includes
*&---------------------------------------------------------------------*
INCLUDE:
  <icon>,
  zcxr1006p_1,  ".Definicion de Parametros
  zcxr1006c_1.  ".Definicion Clases, Metodos

*&---------------------------------------------------------------------*
*& Evento: INITIALIZATION
*&---------------------------------------------------------------------*
INITIALIZATION.
  ".Creamos instancia de la clase principal
  go_cl_principal = NEW lcl_principal( ).
  SET HANDLER go_cl_principal->on_click FOR ALL INSTANCES.

AT SELECTION-SCREEN.
  CLEAR: des_tbl.

  SELECT SINGLE ddtext
  FROM dd02t
  WHERE tabname = @so_table-low
  INTO @des_tbl.

  IF sy-subrc IS NOT INITIAL.
    MESSAGE s007(e2) WITH so_table-low DISPLAY LIKE 'E'.
  ENDIF.

*&---------------------------------------------------------------------*
*& Evento: START-OF-SELECTION
*&---------------------------------------------------------------------*
START-OF-SELECTION.

  go_cl_principal->obtener_datos(
    EXPORTING
      i_tabla            = so_table-low
      i_max_aciertos     = pa_acier
      i_filtro           = pa_filt
    EXCEPTIONS
      tabla_no_permitida = 1
      tabla_no_existe    = 2
      error_en_condicion = 3
      OTHERS             = 4
  ).

  IF sy-subrc <> 0.

    CASE sy-subrc.
      WHEN 1.
        ".La tabla debe empezar por Z para ser tratada.
        MESSAGE i029 DISPLAY LIKE lcl_principal=>gc_e.
      WHEN 2.
      WHEN 3.
        MESSAGE i052 DISPLAY LIKE lcl_principal=>gc_e.
    ENDCASE.
  ENDIF.
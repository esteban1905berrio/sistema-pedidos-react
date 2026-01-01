*&---------------------------------------------------------------------*
*& Programa             ZCXR1001_3
*& RICEFW               R1001
*& Autor                Sebastian Restrepo Villa
*& Descripción          Ejecucion Metodos de Consumo de GCP
*& Fecha                27Mar2025
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 27Mar2025                Sebastian Restrepo Villa      Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxr1001_3 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& Includes
*&---------------------------------------------------------------------*
INCLUDE:
  <icons>,
  zcxr1001p_3,  ".Include de parametros
  zcxr1001c_3.  ".Include de Clases
*  zcxr1001o_1,  ".PBO
*  zcxr1001i_1.  ".PAI

*----------------------------------------------------------------------*
* evento SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layad                *
*----------------------------------------------------------------------*
*AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.
*  go_principal->f4_layouts(
*    EXPORTING
*      p_restrict = if_salv_c_layout=>restrict_none
*      p_handle   = space
*    CHANGING
*      p_layout   = pa_layou
*  ).
*
*AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_stat-low.
*  go_principal->f4_status(
*    EXPORTING
*      p_restrict = if_salv_c_layout=>restrict_none
*      p_handle   = space
*    CHANGING
*      p_status   = so_stat-low
*  ).


*&---------------------------------------------------------------------*
*& Evento: INITIALIZATION
*&---------------------------------------------------------------------*
INITIALIZATION.
  go_principal = NEW lcl_principal(  ).

*&---------------------------------------------------------------------*
*& Evento: START-OF-SELECTION
*&---------------------------------------------------------------------*
START-OF-SELECTION.
  ".Inicio del proceso
*  go_principal->start_of_selection( ).

*&---------------------------------------------------------------------*
*& Evento: END-OF-SELECTION
*&---------------------------------------------------------------------*
END-OF-SELECTION.
  BREAK srestrev.
  IF sy-batch = abap_true.
    go_principal->procesar_mensaje( ).
  ELSE.
    go_principal->display_alv( ).
  ENDIF.
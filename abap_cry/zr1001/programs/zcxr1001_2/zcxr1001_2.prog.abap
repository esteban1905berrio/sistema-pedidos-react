*&---------------------------------------------------------------------*
*& Programa             ZCXR1001_2
*& RICEFW               R1001
*& Autor                Sebastian Restrepo Villa
*& Descripción          Generacion procesos agendados
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
REPORT zcxr1001_2 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& Includes
*&---------------------------------------------------------------------*
INCLUDE:
  <icons>,
  ZCXR1001P_2,  ".Include de parametros
  ZCXR1001C_2.  ".Include de Clases
*  zcxr1001o_1,  ".PBO
*  zcxr1001i_1.  ".PAI

*----------------------------------------------------------------------*
* evento SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layad                *
*----------------------------------------------------------------------*
AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.
  go_principal->f4_layouts(
    EXPORTING
      p_restrict = if_salv_c_layout=>restrict_none
      p_handle   = space
    CHANGING
      p_layout   = pa_layou
  ).

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
  go_principal->start_of_selection( ).

*&---------------------------------------------------------------------*
*& Evento: END-OF-SELECTION
*&---------------------------------------------------------------------*
END-OF-SELECTION.
  break srestrev.
  IF sy-batch = abap_true.
    go_principal->reprocesar_informacion( i_procesar_msg_directo = abap_true ).
  ELSE.
    go_principal->display_alv( ).
  ENDIF.
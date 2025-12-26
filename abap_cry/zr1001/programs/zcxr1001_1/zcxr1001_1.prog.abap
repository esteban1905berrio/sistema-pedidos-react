*&---------------------------------------------------------------------*
*& Programa             ZCXR1001_1
*& RICEFW               R1001
*& Autor                JMVALENC
*& Descripción          Monitor de Integraciones
*& Fecha                10/02/2021
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 10/02/2021   S4DK900113  Jose Miguel Valencia       Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxr1001_1 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& Includes
*&---------------------------------------------------------------------*
INCLUDE:
  <icons>,
  zcxr1001p_1,  ".Include de parametros
  zcxr1001c_1.  ".Include de Clases
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

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_stat-low.
  go_principal->f4_status(
    EXPORTING
      p_restrict = if_salv_c_layout=>restrict_none
      p_handle   = space
    CHANGING
      p_status   = so_stat-low
  ).


*&---------------------------------------------------------------------*
*& Evento: INITIALIZATION
*&---------------------------------------------------------------------*
INITIALIZATION.

  go_principal = NEW lcl_principal(  ).

*&---------------------------------------------------------------------*
*& Evento: START-OF-SELECTION
*&---------------------------------------------------------------------*
START-OF-SELECTION.
  IF pa_pdir IS INITIAL."Procesar mensaje
    pa_pdir = COND #( WHEN sy-batch = abap_true THEN abap_true ELSE abap_false ).
  ENDIF.
  ".Inicio del proceso
  go_principal->start_of_selection( ).

*&---------------------------------------------------------------------*
*& Evento: END-OF-SELECTION
*&---------------------------------------------------------------------*
END-OF-SELECTION.
*  go_principal->visualizar_alv( ).
  "+SLS - Procesar mensajes directamente{
  IF pa_pdir = abap_true OR sy-batch = abap_true.
    IF pa_tdmsg = abap_true."+SLS 05062023: Para procesar todos los mensajes consultados cuando se realiza una ejecucion en fondo
        go_principal->reprocesar_multiples_msg( i_procesar_msg_directo = abap_true
                                                i_procesar_todos_los_msg = pa_tdmsg "Reporcesar/procesar todos los mensajes seleccionados
                                              ).
    ELSE.
      go_principal->reprocesar_informacion( i_procesar_msg_directo = abap_true ).
    ENDIF.
  ELSE.
    go_principal->display_alv( ).
  ENDIF.
  "}
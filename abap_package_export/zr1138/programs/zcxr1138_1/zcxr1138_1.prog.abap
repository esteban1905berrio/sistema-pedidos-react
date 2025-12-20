*&---------------------------------------------------------------------*
*& Programa             ZCXR1138_1
*& RICEFW               1138
*& Autor                Carlos E. Gutierrez
*& Descripción          Reporte Copia Textos
*& Fecha                12Ene2023
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 12Ene2023   S4DK919277  Carlos E. Gutierrez        Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxr1138_1 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& Includes
*&---------------------------------------------------------------------*
INCLUDE:
  <icons>,
 ZCXR1138P_1, ".Include de parametros
 ZCXR1138C_1.  ".Include de Clases

*----------------------------------------------------------------------*
* evento SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou                *
*----------------------------------------------------------------------*
AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.
  go_principal->f4_layouts(
    EXPORTING
      p_restrict = if_salv_c_layout=>restrict_none
      p_handle   = space
    CHANGING
      p_layout   = pa_layou
  ).

*&---------------------------------------------------------------------*
*& Evento: INITIALIZATION
*&---------------------------------------------------------------------*
INITIALIZATION.
  go_principal = NEW lcl_principal( ).

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
  go_principal->end_of_selection( ).
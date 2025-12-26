*&---------------------------------------------------------------------*
*& Programa             ZCXR1170_1
*& RICEFW               R1170
*& Autor                JMVALENC
*& Descripción          Reporte Borrado Eanes tabla FSH_EANREC
*& Fecha                15/01/2025
*&---------------------------------------------------------------------*
REPORT zcxr1170_1 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& INCLUDES
*&---------------------------------------------------------------------*
INCLUDE:
  <icon>,
  zcxr1170p_1,
  zcxr1170c_1.

INITIALIZATION.
  gcl_control = NEW lcl_control( ).

START-OF-SELECTION.
  gcl_control->start_of_selection( ).

END-OF-SELECTION.
  gcl_control->end_of_selection( ).
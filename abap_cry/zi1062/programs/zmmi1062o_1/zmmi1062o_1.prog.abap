*&---------------------------------------------------------------------*
*& Include zmmi1062o_1
*&---------------------------------------------------------------------*
MODULE mostrar_datos OUTPUT.

  lcl_controlador=>mostrar_alv( CHANGING c_ti_datos = lcl_controlador=>gti_ins_lavado ).

ENDMODULE.                    "mostrar_datos OUTPUT

*----------------------------------------------------------------------*
*  MODULE status_0100 OUTPUT
*----------------------------------------------------------------------*
*
*----------------------------------------------------------------------*
MODULE status_0100 OUTPUT.
  SET PF-STATUS 'GS_0001'.
  SET TITLEBAR 'GT_0001'.
ENDMODULE.                    "status_0100 OUTPUT
*&---------------------------------------------------------------------*
*& Include zfir1113o_1
*&---------------------------------------------------------------------*
MODULE status_0100 OUTPUT.
  SET PF-STATUS 'GS_0001'.
  SET TITLEBAR 'GT_0001'.
ENDMODULE.

MODULE visualizar_datos OUTPUT.
  go_control_elementos->visualizar_reporte(
    EXPORTING
      i_agrupado = pa_agru
      i_reporte  = sy-repid
      i_layout   = pa_layou ).
ENDMODULE.
*&---------------------------------------------------------------------*
*& Include zppr1157o_1
*&---------------------------------------------------------------------*

MODULE status_0100 OUTPUT.
  SET PF-STATUS 'GS_0001'.
  SET TITLEBAR 'GT_0001' .
ENDMODULE.

MODULE visualizar_datos OUTPUT.

  go_proceo_infotint->visualizar_datos(
      i_reporte    = sy-repid
      i_layout     = pa_layou ).

ENDMODULE.
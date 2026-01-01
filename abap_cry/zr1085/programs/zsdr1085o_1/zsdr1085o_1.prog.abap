*&---------------------------------------------------------------------*
*& Include zsdr1085o_1
*&---------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*& Module STATUS_0100 OUTPUT
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
MODULE status_0100 OUTPUT.
  SET PF-STATUS 'GS_0001'.
  g_cantidad_pedidos = go_monitor->obtener_cantidad_pedidos( ).
  CONDENSE g_cantidad_pedidos NO-GAPS.
  SET TITLEBAR 'GT_0001' WITH g_cantidad_pedidos.
ENDMODULE.

MODULE visualizar_datos OUTPUT.
  go_monitor->visualizar_datos(
    EXPORTING
      i_consolidado = pa_cons
      i_reporte    = sy-repid
      i_layout     = pa_layou ).
ENDMODULE.
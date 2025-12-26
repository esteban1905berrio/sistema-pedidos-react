*&---------------------------------------------------------------------*
*& Include zmmr1047p_2
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Include          ZMMR1047P_1
*&---------------------------------------------------------------------*

SELECTION-SCREEN BEGIN OF BLOCK bloq1 WITH FRAME TITLE TEXT-001.
  SELECT-OPTIONS:
   so_vkorg FOR zclmmr1047_pedidos_pendientes=>ges_parametros-vkorg NO INTERVALS NO-EXTENSION OBLIGATORY,
   so_wbstk FOR zclmmr1047_pedidos_pendientes=>ges_parametros-wbstk NO INTERVALS NO-EXTENSION OBLIGATORY,
   so_fkstk FOR zclmmr1047_pedidos_pendientes=>ges_parametros-fkstk NO INTERVALS NO-EXTENSION OBLIGATORY,
*   so_matnr FOR zclmmr1047_pedidos_pendientes=>ges_parametros-matnr,
*   so_mtart FOR zclmmr1047_pedidos_pendientes=>ges_parametros-mtart MATCHCODE OBJECT h_t134 OBLIGATORY DEFAULT 'ZMOD',
*   so_mstae FOR zclmmr1047_pedidos_pendientes=>ges_parametros-mstae DEFAULT '',
*   so_vmsta FOR zclmmr1047_pedidos_pendientes=>ges_parametros-vmsta MATCHCODE OBJECT h_tvms DEFAULT '',
   so_werks FOR zclmmr1047_pedidos_pendientes=>ges_parametros-werks OBLIGATORY DEFAULT '1130',
*   so_lgort FOR zclmmr1047_pedidos_pendientes=>ges_parametros-lgort DEFAULT '0500',
   so_fecds FOR zclmmr1047_pedidos_pendientes=>ges_parametros-erdat NO INTERVALS NO-EXTENSION OBLIGATORY.
*  PARAMETERS:
*   pa_berid TYPE berid .
*  SELECT-OPTIONS:
*   so_dismm FOR zclmmr1047_pedidos_pendientes=>ges_parametros-dismm NO INTERVALS NO-EXTENSION OBLIGATORY MATCHCODE OBJECT h_t438a DEFAULT 'PD'.
SELECTION-SCREEN END OF BLOCK bloq1.

SELECTION-SCREEN BEGIN OF BLOCK bloq2 WITH FRAME TITLE TEXT-009.
  PARAMETERS: pa_test AS CHECKBOX.
SELECTION-SCREEN END OF BLOCK bloq2.
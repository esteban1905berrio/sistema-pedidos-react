*&---------------------------------------------------------------------*
*& Include zsdr1085p_1
*&---------------------------------------------------------------------*

TABLES: vbak, vbap, likp, vbrk, /scmtms/d_torrot, ekpo, ekko.

DATA: BEGIN OF ges_parametros_pantalla,
        pedido          TYPE char10,
        clase_documento TYPE char4,
        motivo_pedido   TYPE char4,
      END OF ges_parametros_pantalla.

DATA: go_monitor         TYPE REF TO zclsdr1085_monitor_pedidos,
      go_log             TYPE REF TO zclcxr1002_log_aplicacion,
      g_cantidad_pedidos TYPE char10,
      ok_code            TYPE syucomm,
      gok_code_activo    TYPE syucomm.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.

  SELECT-OPTIONS: so_vkorg FOR vbak-vkorg,
                  so_vtweg FOR vbak-vtweg,
                  so_spart FOR vbak-spart,
                  so_werks FOR vbap-werks,
                  so_ekwrk FOR ekpo-werks,
                  so_erdat FOR vbak-erdat, "Fecha Creacion
                  so_auart FOR ges_parametros_pantalla-clase_documento MATCHCODE OBJECT zhsdr1085_clase_documentos, "Clase Pedido
                  so_moped FOR ges_parametros_pantalla-motivo_pedido MATCHCODE OBJECT zhsdr1085_motivo_pedido, "Motivo Pedido
                  so_vdatu FOR vbak-vdatu, "Fecha Preferente Entrega Cabecera
                  so_vbelp FOR ges_parametros_pantalla-pedido MATCHCODE OBJECT zhsdr1085_pedido_venta_traslad, "Pedido
                  so_bstnk FOR vbak-bstnk, "Orden de Compra
                  so_repag FOR vbak-kunnr, "Responsable de pago
                  so_encom FOR vbak-kunnr, "Encargado comercial
                  so_ekgrp FOR ekko-ekgrp, "Grupo de compra
                  so_vbele FOR likp-vbeln, "Entrega
                  so_nguia FOR likp-zz1_numeroguia_dlh,
                  so_vbelf FOR vbrk-vbeln, "Factura
                  so_tknum FOR /scmtms/d_torrot-tor_id,"Numero Transporte
                  so_fftp  FOR vbak-erdat."Fecha fin Transporte

  PARAMETERS: pa_ppent TYPE flag AS CHECKBOX,
              pa_unest TYPE flag AS CHECKBOX,
              pa_mtftl TYPE flag AS CHECKBOX.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-bk2.

  PARAMETERS: pa_cons TYPE flag RADIOBUTTON GROUP tprp,
              pa_detl TYPE flag RADIOBUTTON GROUP tprp.

SELECTION-SCREEN END OF BLOCK bk2.

SELECTION-SCREEN BEGIN OF BLOCK bk3 WITH FRAME TITLE TEXT-bk3.
  PARAMETERS: pa_layou TYPE disvariant-variant.
SELECTION-SCREEN END OF BLOCK bk3.
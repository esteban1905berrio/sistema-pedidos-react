*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1085 - Monitor de Pedidos
* Programa     : zsdr1085_1
* Tipo Objeto  : Reporte
* Descripción  : Monitor de Pedidos
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 19.01.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 19.01.2022    S4DK905891   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zsdr1085_1.

INCLUDE zsdr1085p_1.
INCLUDE zsdr1085o_1.
INCLUDE zsdr1085i_1.

INITIALIZATION.

AT SELECTION-SCREEN OUTPUT.

  LOOP AT SCREEN.
    IF screen-name = 'SO_VKORG-LOW' OR
       screen-name = 'SO_WERKS-LOW' OR
       screen-name = 'SO_ERDAT-LOW'.
      screen-required = '2'.
      MODIFY SCREEN.
    ENDIF.
  ENDLOOP.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.

  zclcxr1002_util=>f4_layout( EXPORTING i_report = sy-repid CHANGING c_layout = pa_layou ).

START-OF-SELECTION.
  "    ,*-.
  "    |  |
  ",.  |  |
  "| |_|  | ,.
  "`---.  |_| |
  "    |  .--`
  "    |  |
  "    |  | to start.....
  go_monitor = NEW zclsdr1085_monitor_pedidos(  ).
  "      | °   &   ° |
  "{¨<X>¨|___/[|]\___|¨<X>¨}"
  go_monitor->generar_monitor_pedidos_venta( EXPORTING
                                                  i_r_vkorg                     = so_vkorg[]
                                                  i_r_vtweg                     = so_vtweg[]
                                                  i_r_spart                     = so_spart[]
                                                  i_r_werks                     = so_werks[]
                                                  i_r_erdat                     = so_erdat[]
                                                  i_r_auart                     = so_auart[]
                                                  i_r_augru                     = so_moped[]
                                                  i_r_vdatu                     = so_vdatu[]
                                                  i_r_vbelp                     = so_vbelp[]
                                                  i_r_bstnk                     = so_bstnk[]
                                                  i_r_responsable_de_pago       = so_repag[]
                                                  i_r_encargado_comercial       = so_encom[]
                                                  i_r_vbele                     = so_vbele[]
                                                  i_r_ekgrp                     = so_ekgrp[]
                                                  i_r_numero_guia               = so_nguia[]
                                                  i_r_vbelf                     = so_vbelf[]
                                                  i_r_numero_transporte         = so_tknum[]
                                                  i_fecha_fin_transporte        = so_fftp[]
                                                  i_un_medida_estadistica       = pa_unest
                                                  i_pedido_pendiente_x_entregar = pa_ppent
                                                  i_consolidado                 = pa_cons
                                                  i_detalllado                  = pa_detl
                                                  i_material_flete              = pa_mtftl
*                                                  i_optimizar_consulta          = pa_optim
                                                IMPORTING
                                                  e_rc                        = DATA(g_rc)
                                            ).
  "                        (
  "                     (  ) (
  "                      )    )
  "         |||||||     (  ( (
  "        ( O   O )        )
  " ____oOO___(_)___OOo____(
  "(_______________________)
  "           JOINT
  go_monitor->generar_monitor_pedidotraslado( EXPORTING
                                                    i_r_vkorg                     = so_vkorg[]
                                                    i_r_vtweg                     = so_vtweg[]
                                                    i_r_spart                     = so_spart[]
                                                    i_r_werks                     = so_werks[]
                                                    i_r_centro_receptor           = so_ekwrk[]
                                                    i_r_erdat                     = so_erdat[]
                                                    i_r_auart                     = so_auart[]
                                                    i_r_bsgru                     = so_moped[]
                                                    i_r_vdatu                     = so_vdatu[]
                                                    i_r_vbelp                     = so_vbelp[]
                                                    i_r_bstnk                     = so_bstnk[]
                                                    i_r_responsable_de_pago       = so_repag[]
                                                    i_r_ekgrp                     = so_ekgrp[]
                                                    i_r_vbele                     = so_vbele[]
                                                    i_r_numero_guia               = so_nguia[]
                                                    i_r_vbelf                     = so_vbelf[]
                                                    i_r_numero_transporte         = so_tknum[]
                                                    i_fecha_fin_transporte        = so_fftp[]
                                                    i_un_medida_estadistica       = pa_unest
                                                    i_pedido_pendiente_x_entregar = pa_ppent
                                                    i_consolidado                 = pa_cons
                                                    i_detalllado                  = pa_detl
                                                  IMPORTING
                                                    e_rc                          = g_rc
                                             ).

END-OF-SELECTION.

  go_log =  zclcxr1002_log_aplicacion=>get_instancia( ).
  go_log->mostrar_log( i_ventana_emergente = abap_true i_limpiar_mensajes = abap_true ).

  IF go_monitor->gti_pedidos_monitor IS NOT INITIAL.
    CALL SCREEN 100.
  ELSE.
    MESSAGE s002(wusl) DISPLAY LIKE 'E'.
  ENDIF.
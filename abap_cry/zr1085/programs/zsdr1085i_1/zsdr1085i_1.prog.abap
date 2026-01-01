*&---------------------------------------------------------------------*
*& Include zsdr1085i_1
*&---------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE user_command_exit INPUT.

  gok_code_activo = ok_code.
  CLEAR: ok_code.

  CASE gok_code_activo.
    WHEN 'CANCEL'.
      LEAVE TO SCREEN 0.
  ENDCASE.

ENDMODULE.

MODULE user_command INPUT.

  gok_code_activo = ok_code.
  CLEAR: ok_code.

  CASE gok_code_activo.
    WHEN 'ACTUALIZAR'.
      go_monitor->generar_monitor_pedidos_venta(  i_r_vkorg                     = so_vkorg[]
                                                  i_r_vtweg                     = so_vtweg[]
                                                  i_r_spart                     = so_spart[]
                                                  i_r_werks                     = so_werks[]
                                                  i_r_erdat                     = so_erdat[]
                                                  i_r_auart                     = so_auart[]
                                                  i_r_augru                     = so_moped[]
                                                  i_r_vdatu                     = so_vdatu[]
                                                  i_r_vbelp                     = so_vbelp[]
                                                  i_r_bstnk                     = so_bstnk[]
                                                  i_r_ekgrp                     = so_ekgrp[]
                                                  i_r_responsable_de_pago       = so_repag[]
                                                  i_r_encargado_comercial       = so_encom[]
                                                  i_r_vbele                     = so_vbele[]
                                                  i_r_numero_guia               = so_nguia[]
                                                  i_r_vbelf                     = so_vbelf[]
                                                  i_r_numero_transporte         = so_tknum[]
                                                  i_fecha_fin_transporte        = so_fftp[]
                                                  i_un_medida_estadistica       = pa_unest
                                                  i_pedido_pendiente_x_entregar = pa_ppent
                                                  i_consolidado                 = pa_cons
                                                  i_detalllado                  = pa_detl
                                               ).

      go_monitor->generar_monitor_pedidotraslado(   i_r_vkorg                     = so_vkorg[]
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
                                                 ).
      go_monitor->go_alv->refresh_table_display(  ).

    WHEN 'BACK' OR 'EXIT'.
      LEAVE TO SCREEN 0.
  ENDCASE.

ENDMODULE.
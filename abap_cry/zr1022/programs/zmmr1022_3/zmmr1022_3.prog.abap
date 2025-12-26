*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1022 - Reporte Ipis Tiendas
* Programa     : zmmr1022_3
* Tipo Objeto  : Reporte
* Descripción  : Reporte Ipis Tiendas
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 18.01.2023
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 27.12.2022    S4DK919369   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmr1022_3.

INCLUDE: zmmr1022p_3.

AT SELECTION-SCREEN OUTPUT.
  IF so_lgort[] IS INITIAL.
    so_lgort = VALUE #( sign = 'I' option = 'EQ' low = '0500' ).
    INSERT so_lgort INTO TABLE so_lgort[].
  ENDIF.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.

  zclcxr1002_util=>f4_layout( EXPORTING i_report = sy-repid CHANGING c_layout = pa_layou ).

START-OF-SELECTION.

  go_reporte_ipis_tienda = NEW zclmmr1022_reporte_ipis_tienda(  ).

  go_reporte_ipis_tienda->iniciar_seleccion_de_datos(
    EXPORTING
      i_r_documento_inventario = so_iblnr[]
      i_r_ejercicio            = so_gjahr[]
      i_r_centro               = so_werks[]
      i_r_almacen              = so_lgort[]
      i_r_material             = so_matnr[]
      i_r_fecha_documento      = so_bldat[]
  ).

END-OF-SELECTION.
  TRY.
      go_reporte_ipis_tienda->visualizar_inventario_ipis( i_layout = pa_layou i_id_reporte = sy-repid ).
    CATCH cx_t100_msg INTO DATA(o_cx).
      MESSAGE s208(00) WITH o_cx->get_longtext( ) DISPLAY LIKE 'E'.
  ENDTRY.
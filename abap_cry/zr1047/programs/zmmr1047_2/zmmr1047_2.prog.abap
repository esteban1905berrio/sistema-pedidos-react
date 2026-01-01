*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1047 - Pedidos pendientes
* Programa     : ZMMR1047_2
* Tipo Objeto  : Reporte
* Descripción  : Modificar datos entregas.
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 11.12.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 11.12.2022    S4DK917739   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmr1047_2.

INCLUDE: zmmr1047p_2.


INITIALIZATION.

  DATA(go_pedidos_pendientes) = NEW zclmmr1047_pedidos_pendientes( ).

  IF so_fecds-low IS INITIAL.
    so_fecds = VALUE #( sign   = 'I' option = 'EQ' low = ( sy-datum - 15 ) ).
    INSERT so_fecds INTO TABLE so_fecds[].
  ENDIF.

  IF so_vkorg-low IS INITIAL.
    so_vkorg = VALUE #( sign   = 'I' option = 'EQ' low = 1110 ).
    INSERT so_vkorg INTO TABLE so_vkorg[].
  ENDIF.

  IF so_wbstk-low IS INITIAL.
    so_wbstk = VALUE #( sign   = 'I' option = 'EQ' low = 'C' ).
    INSERT so_wbstk INTO TABLE so_wbstk[].
  ENDIF.

  IF so_fkstk-low IS INITIAL.
    so_fkstk = VALUE #( sign   = 'I' option = 'NE' low = 'C' ).
    INSERT so_fkstk INTO TABLE so_fkstk[].
  ENDIF.

START-OF-SELECTION.

  go_pedidos_pendientes->generar_pedidos_pendientes(
    EXPORTING
      i_r_fecha_entrega_desde = so_fecds[]
      i_r_org_venta           = so_vkorg[]
      i_r_centro              = so_werks[]
      i_r_est_movimiento      = so_wbstk[]
      i_r_est_factura         = so_fkstk[]
      i_ejecutar_en_test      = pa_test
  ).
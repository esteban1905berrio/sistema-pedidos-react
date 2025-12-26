@AbapCatalog.sqlViewName: 'ZISDR1085_4'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entragas de venta'
define view ZISDR1085_EntregaVenta
  as select from    I_SDDocumentProcessFlow as _flujoDocumentos

    inner join      lips                    as _entrega_posicion            on  _entrega_posicion.vbeln = _flujoDocumentos.SubsequentDocument
                                                                            and _entrega_posicion.posnr = _flujoDocumentos.SubsequentDocumentItem
    inner join      likp                    as _entrega                     on _entrega.vbeln = _entrega_posicion.vbeln
    inner join      ztcxr1000_2             as _parametroTipoPosicionPedido on  _parametroTipoPosicionPedido.ricefw  = 'R1085'
                                                                            and _parametroTipoPosicionPedido.idparam = 'TIPO_PEDID'
                                                                            and _parametroTipoPosicionPedido.low     = _flujoDocumentos.PrecedingDocumentCategory
    left outer join zttmr1083_1             as _log_carga_descarga          on  _log_carga_descarga.entrega = _entrega.vbeln
                                                                            and _log_carga_descarga.estatus = 'C'
    left outer join zz1_4221312f3fd0        as desc_status                  on  desc_status.code     = _entrega.zz1_statustransportist_dlh
                                                                            and desc_status.language = $session.system_language


{
  key _flujoDocumentos.PrecedingDocument                                         as pedido,
  key _flujoDocumentos.PrecedingDocumentItem                                     as posicion_pedido,
  key _flujoDocumentos.SubsequentDocument                                        as entrega,
  key SubsequentDocumentItem                                                     as posicion_entrega,
      vkorg,
      vtwiv,
      kunag,
      _entrega_posicion.werks                                                    as werks,
      _entrega_posicion.lgort                                                    as lgort,
      _entrega_posicion.charg                                                    as lote,
      _entrega_posicion.matnr                                                    as matnr,
      concat( concat( concat( vkorg, vtwiv ), kunag ), _entrega_posicion.matnr ) as objek,
      _entrega_posicion.erdat                                                    as fecha_creacion_entrega,
      lfart                                                                      as Clase_entrega,
      anzpk,
      kostk,
      _entrega_posicion.vrkme                                                    as unidadmedida_entrega,
      _entrega_posicion.lfimg                                                    as cantidad_entrega,
      _entrega_posicion.kosta                                                    as Estado_picking,
      _entrega.vlstk                                                             as estado_picking_ewm,
      _entrega.zz1_numeroguia_dlh                                                as guia_transporte,
      @Semantics.quantity.unitOfMeasure : 'vbfa.meins'
      QuantityInBaseUnit                                                         as Cantidad_Despachada,
      _entrega_posicion.wbsta                                                    as estado_movimiento_mercancia,
      _entrega.zz1_statustransportist_dlh                                        as Status_Novedad_Transportista,
      desc_status.description                                                    as Desc_Sta_Novedad_Transportista,
      _entrega.zz1_fechaentregaclient_dlh                                        as Fecha_entrega_Cliente,
      _entrega.zz1_novedadtransportis_dlh                                        as Novedad_Transportista,
      _entrega.zz1_numeroguia_dlh                                                as zz1_numeroguia_dlh,
      _entrega_posicion.uecha,
      _entrega.zz1_numeroguianueva_dlh,
      _entrega.zz1_tipovinculo_dlh,
      _entrega.zz1_horaentregacliente_dlh,
      _entrega.zz1_fechprivisit_dlh,
      _entrega.zz1_horaprivisit_dlh,
      _log_carga_descarga.orden_flete,
      _log_carga_descarga.unidad_flete,
      PrecedingDocumentCategory,
      SubsequentDocumentCategory

}
where
  //Entregas
  (
       SubsequentDocumentCategory = 'J'
    or SubsequentDocumentCategory = 'T'
  )

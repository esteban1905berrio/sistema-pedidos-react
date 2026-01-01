@AbapCatalog.sqlViewName: 'ZIMMR1085_4'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entragas de pedidos de traslado'
define view ZIMMR1085_ENTREGA_TRASLADO
  as select distinct from    likp                   as _entrega
    inner join      I_DeliveryDocumentItem as _entrega_posicion   on _entrega_posicion.DeliveryDocument = _entrega.vbeln
    left outer join zttmr1083_1            as _log_carga_descarga on  _log_carga_descarga.entrega = _entrega.vbeln
                                                                  and _log_carga_descarga.estatus = 'C'
    left outer join zz1_4221312f3fd0       as desc_status         on  desc_status.code     = _entrega.zz1_statustransportist_dlh
                                                                  and desc_status.language = $session.system_language


{
  key _entrega.vbeln                                                                as entrega,
  key _entrega_posicion.DeliveryDocumentItem                                        as posicion_entrega,

      vkorg,
      vtwiv,
      kunag,
      _entrega_posicion.Material                                                    as matnr,
      concat( concat( concat( vkorg, vtwiv ), kunag ), _entrega_posicion.Material ) as objek,
      _entrega_posicion.CreationDate                                                as fecha_creacion_entrega,
      lfart                                                                         as Clase_entrega,
      anzpk,
      kostk,
      _entrega_posicion.DeliveryQuantityUnit                                        as unidadmedida_entrega,
      _entrega_posicion.ActualDeliveryQuantity                                      as cantidad_entrega,
      _entrega_posicion.PickingStatus                                               as Estado_picking,
      _entrega.vlstk                                                                as estado_picking_ewm,
      _entrega.zz1_numeroguia_dlh                                                   as guia_transporte,
      _entrega_posicion.GoodsMovementStatus                                         as estado_movimiento_mercancia,
      _entrega.zz1_statustransportist_dlh                                           as Status_Novedad_Transportista,
      desc_status.description                                                       as Desc_Sta_Novedad_Transportista,
      _entrega.zz1_fechaentregaclient_dlh                                           as Fecha_entrega_Cliente,
      _entrega.zz1_novedadtransportis_dlh                                           as Novedad_Transportista,
      _entrega.zz1_numeroguia_dlh                                                   as zz1_numeroguia_dlh,
      _entrega_posicion.ReferenceSDDocument                                         as vgbel,
      _entrega_posicion.ReferenceSDDocumentItem                                     as vgpos,
      _entrega_posicion.HigherLvlItmOfBatSpltItm                                    as uecha,
      _entrega.zz1_numeroguianueva_dlh,
      _entrega.zz1_tipovinculo_dlh,
      _entrega.zz1_horaentregacliente_dlh,
      _log_carga_descarga.orden_flete,
      _log_carga_descarga.unidad_flete
}

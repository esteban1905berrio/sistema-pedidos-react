@AbapCatalog.sqlViewName: 'ZIMMR1085_9'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Historial Doc: Consolidado entrega cross-docking'
define view ZIMMR1085_HISTDOC_CROSSDOCKING
  as select from ekko                       as _pedido_compra
    inner join   ztcxr1000_2                as _tipo_pedido_crossdocking on  _tipo_pedido_crossdocking.ricefw  = 'R1085'
                                                                         and _tipo_pedido_crossdocking.idparam = 'CLPD_CRSDC'
                                                                         and _tipo_pedido_crossdocking.low     = _pedido_compra.bsart              
    inner join   ekbe                       as _historialDocumento       on _historialDocumento.ebeln = _pedido_compra.ebeln

    inner join   ZIMMR1085_ENTREGA_TRASLADO as _entrega_posicion         on  _entrega_posicion.entrega          = _historialDocumento.belnr

    -- Para escenario de Pafois donde no existen posiciones 90* se descarta en esta vista dado que funcionalmente se debe
    -- Definir una logica adicional que solo aplique para entregas SIN "Posicion superior de Lote"
                                                                         and _entrega_posicion.posicion_entrega = concat(
      '00', buzei
    )
                                                                         and _entrega_posicion.vgbel            = _historialDocumento.ebeln
                                                                         and _historialDocumento.ebelp          = substring(
      _entrega_posicion.vgpos, 2, 5
    )
{
  key   _historialDocumento.ebeln,
  key   ebelp,
  key   zekkn,
  key   vgabe,
  key   belnr                          as entrega,
        bwart,
        _entrega_posicion.vkorg,
        _entrega_posicion.vtwiv,
        _entrega_posicion.kunag,
        _entrega_posicion.matnr,
        _entrega_posicion.objek,
        _entrega_posicion.fecha_creacion_entrega,
        _entrega_posicion.Clase_entrega,
        _entrega_posicion.anzpk,
        _entrega_posicion.kostk,
        _entrega_posicion.unidadmedida_entrega,
        @Semantics.quantity.unitOfMeasure: 'unidadmedida_entrega'
        //-SLS 07022023 - Se modifica fuente para la cantidad entregada, dada la doplicidad que se presenta en la EKBE
        //por posicion de entrega con diferentes ejercicios. lo que hace que se repita la cantidad en el SUM
        //        sum(_entrega_posicion.cantidad_entrega) as cantidad_entrega,
        sum(_historialDocumento.menge) as cantidad_entrega,
        //NO se trae esta columna para evitar que se abran las posiciones
        ''                             as Estado_picking,
        _entrega_posicion.estado_picking_ewm,
        _entrega_posicion.guia_transporte,
        ''                             as estado_movimiento_mercancia,
        _entrega_posicion.Status_Novedad_Transportista,
        _entrega_posicion.Desc_Sta_Novedad_Transportista,
        _entrega_posicion.Fecha_entrega_Cliente,
        _entrega_posicion.Novedad_Transportista,
        _entrega_posicion.zz1_numeroguia_dlh,
        //_entrega_posicion.uecha,
        _entrega_posicion.zz1_numeroguianueva_dlh,
        _entrega_posicion.zz1_tipovinculo_dlh,
        _entrega_posicion.zz1_horaentregacliente_dlh,
        _entrega_posicion.orden_flete,
        _entrega_posicion.unidad_flete
}
where
  vgabe = '8'
group by
  _historialDocumento.ebeln,
  ebelp,
  zekkn,
  vgabe,
  belnr,
  bwart,
  _entrega_posicion.vkorg,
  _entrega_posicion.vtwiv,
  _entrega_posicion.kunag,
  _entrega_posicion.matnr,
  _entrega_posicion.objek,
  _entrega_posicion.fecha_creacion_entrega,
  _entrega_posicion.Clase_entrega,
  _entrega_posicion.anzpk,
  kostk,
  _entrega_posicion.unidadmedida_entrega,
  //  _entrega_posicion.Estado_picking,
  _entrega_posicion.estado_picking_ewm,
  _entrega_posicion.guia_transporte,
  //  _entrega_posicion.estado_movimiento_mercancia,
  _entrega_posicion.Status_Novedad_Transportista,
  _entrega_posicion.Desc_Sta_Novedad_Transportista,
  _entrega_posicion.Fecha_entrega_Cliente,
  _entrega_posicion.Novedad_Transportista,
  _entrega_posicion.zz1_numeroguia_dlh,
  _entrega_posicion.zz1_numeroguianueva_dlh,
  _entrega_posicion.zz1_tipovinculo_dlh,
  _entrega_posicion.zz1_horaentregacliente_dlh,
  _entrega_posicion.orden_flete,
  _entrega_posicion.unidad_flete

@AbapCatalog.sqlViewName: 'ZIMMR1085_8'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Historial Doc: Consolidado entrega con Pos superior Lote'
define view ZIMMR1085_HISTORIAL_DOCUMENTO
  as select from ekbe                       as _historialDocumento
    inner join   ZIMMR1085_ENTREGA_TRASLADO as _entrega_posicion on  _entrega_posicion.entrega          = _historialDocumento.belnr
    -- 27.12.2021 - Consulta de posiciones de entrega cuando estas tienen "Posicion superior de Lote"
    -- Para escenario de Pafois donde no existen posiciones 90* se descarta en esta vista dado que funcionalmente se debe
    -- Definir una logica adicional que solo aplique para entregas SIN "Posicion superior de Lote"
                                                                 and _entrega_posicion.posicion_entrega = concat(
      '90', buzei
    )
                                                                 and _entrega_posicion.vgbel            = _historialDocumento.ebeln
                                                                 and _historialDocumento.ebelp          = substring(
      _entrega_posicion.vgpos, 2, 5
    )
  --27.12.2021 - Se comenta dado que este filtro no contempla las entregas EWM{
  --and _entrega_posicion.Estado_picking     <> ''
  --}
{
  key   ebeln,
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
        //NO se trae esta columna para evitar que se abran las posiciones
        //        _entrega_posicion.estado_movimiento_mercancia,
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
      gjahr <> '0000'
  and vgabe =  '8'
group by
  ebeln,
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
union select from ZIMMR1085_HISTDOC_CROSSDOCKING
{
  key ebeln,
  key ebelp,
  key zekkn,
  key vgabe,
  key entrega,
      bwart,
      vkorg,
      vtwiv,
      kunag,
      matnr,
      objek,
      fecha_creacion_entrega,
      Clase_entrega,
      anzpk,
      kostk,
      unidadmedida_entrega,
      cantidad_entrega,
      Estado_picking,
      estado_picking_ewm,
      guia_transporte,
      estado_movimiento_mercancia,
      Status_Novedad_Transportista,
      Desc_Sta_Novedad_Transportista,
      Fecha_entrega_Cliente,
      Novedad_Transportista,
      zz1_numeroguia_dlh,
      zz1_numeroguianueva_dlh,
      zz1_tipovinculo_dlh,
      zz1_horaentregacliente_dlh,
      orden_flete,
      unidad_flete
}

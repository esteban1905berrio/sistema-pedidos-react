@AbapCatalog.sqlViewName: 'ZIMMR1085_1'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Monitor de pedidos de translados'
define view ZIMMR1085_MONI_PEDIDO_TRASLADO
  as select from    ZIMMR1085_PEDIDOS_TRASLADO     as _pedidoTraslado
    left outer join ZIMMR1085_INTERLOCUTORTRASLADO as _solicitante             on _solicitante.kunnr = _pedidoTraslado.kunnr
    left outer join eket                           as _reparto                 on  _reparto.ebeln = _pedidoTraslado.ebeln
                                                                               and _reparto.ebelp = _pedidoTraslado.ebelp
                                                                               and _reparto.etenr = '0001'
  //    left outer join ZIMMR1085_ENTREGA_TRASLADO     as _entrega_posicion        on  _entrega_posicion.entrega          = _pedidoTraslado.entrega
  //                                                                               and _entrega_posicion.posicion_entrega = _pedidoTraslado.posicion_entrega
    left outer join ZISDR1085_ENTREGA_TRANSPORTE   as _transporteFO            on _transporteFO.entrega = _pedidoTraslado.entrega
    left outer join ZISDR1085_ENTREGA_TM_FU        as _transporteFU            on _transporteFU.entrega_erp = _pedidoTraslado.entrega
    left outer join ZIMMR1085_entradamercancia_his as _historial_ent_mercancia on  _historial_ent_mercancia.Ebeln = _pedidoTraslado.ebeln
                                                                               and _historial_ent_mercancia.Ebelp = _pedidoTraslado.ebelp
    left outer join ZISDR1085_ASIGNACION_STOCK     as _asignacion_stock        on  _asignacion_stock.pedido_traslado          = _pedidoTraslado.ebeln
                                                                               and _asignacion_stock.posicion_pedido_traslado = _pedidoTraslado.ebelp
    left outer join mara                           as _material                on _material.matnr = _pedidoTraslado.matnr
    left outer join mean                           as _ean                     on _ean.matnr = _material.matnr
    left outer join wrf_charvalt                   as _caracteristica_color    on  _material.color_atinn       = _caracteristica_color.atinn
                                                                               and _material.color             = _caracteristica_color.atwrt
                                                                               and _caracteristica_color.spras = $session.system_language
    left outer join wrf_charvalt                   as _caracteristica_talla    on  _material.size1_atinn       = _caracteristica_talla.atinn
                                                                               and _material.size1             = _caracteristica_talla.atwrt
                                                                               and _caracteristica_talla.spras = $session.system_language
    left outer join makt                           as _material_txt            on  _material_txt.matnr = _material.matnr
                                                                               and _material_txt.spras = $session.system_language
{
  key _pedidoTraslado.ebeln                       as pedido,
  key _pedidoTraslado.ebelp                       as posicion_pedido,
      _pedidoTraslado.vkorg,
      _pedidoTraslado.vtweg,
      _pedidoTraslado.spart,
      _pedidoTraslado.loekz                       as indicador_borrado_cabecera,
      _pedidoTraslado.posicion_es_estadistica,
      _pedidoTraslado.entrega_parcial_posicion,
      _pedidoTraslado.indic_entrega_salida_completa,
      _pedidoTraslado.ihrez                       as pedido_cliente,
      _pedidoTraslado.indicador_borrado,
      _pedidoTraslado.clase_documento,
      _pedidoTraslado.fecha_creacion_pedido,
      _reparto.eindt                              as Fecha_Preferente_Entrega,
      _pedidoTraslado.matnr,
      _material_txt.maktx,
      _material.size1                             as talla,
      _caracteristica_talla.atwtb                 as descripcion_talla,
      _material.color,
      _caracteristica_color.atwtb                 as descripcion_color,
      _ean.ean11,
      _material.brand_id                          as marca,
      _pedidoTraslado.werks,
      _pedidoTraslado.centro_receptor,
      _reparto.dat01                              as Fecha_confirmada,
      @Semantics.quantity.unitOfMeasure : 'arun_bdbs.material_baseunit'
      _asignacion_stock.cantidad_asignada,
      _pedidoTraslado.motivo_pedido,
      _pedidoTraslado.descripcion_motivo_de_pedido,
      _pedidoTraslado.grupo_compra,
      _pedidoTraslado.descripcion_grupo_compra,
      _pedidoTraslado.Organizacion_Ventas,
      _pedidoTraslado.segmento,
      _pedidoTraslado.Canal_distribucion,
      _pedidoTraslado.Centro,
      _pedidoTraslado.lgort                       as almacen_receptor,
      _pedidoTraslado.reslo                       as lgort,
      _pedidoTraslado.moneda,
      _pedidoTraslado.unidad_venta,
      _pedidoTraslado.unidad_medida,
      _pedidoTraslado.Cantidad_pedida,
      _pedidoTraslado.cantidad_confirmada,
      _pedidoTraslado.valor_neto,
      _pedidoTraslado.valor_neto_pedido,
      _pedidoTraslado.valor_neto_pedido           as precio_exportacion,
      _pedidoTraslado.porcentaje_cumplimiento,
      _pedidoTraslado.status_cancelado,
      case
      when _pedidoTraslado.elikz = '' or _pedidoTraslado.elikz is null then
        ( _reparto.menge - _reparto.wamng )
      else
        0
      end                                         as cantidad_pendiente,
      _pedidoTraslado.elikz                       as indicador_entrega_completa,
      //entrega
      _pedidoTraslado.entrega,
      //_pedidoTraslado.posicion_entrega,//NO relevante debido a que se acumula por entrega
      _pedidoTraslado.fecha_creacion_entrega,
      Clase_entrega,
      unidadmedida_entrega,
      _pedidoTraslado.cantidad_entrega,
      Estado_picking,
      estado_picking_ewm,
      guia_transporte,
      _reparto.wamng                              as Cantidad_Despachada,
      _pedidoTraslado.estado_movimiento_mercancia,
      cast( _pedidoTraslado.anzpk  as abap.int4 ) as cantidad_cajas_erp,
      _pedidoTraslado.kostk,
      //_pedidoTraslado.uecha,//NO relevande debido a que se acumula por entrega
      _pedidoTraslado.zz1_numeroguianueva_dlh,
      _pedidoTraslado.zz1_numeroguia_dlh,
      _pedidoTraslado.zz1_tipovinculo_dlh,
      _pedidoTraslado.zz1_horaentregacliente_dlh  as hora_entrega_cliente,
      //Transporte
      _transporteFO.numero_transporte,
      ltrim( _transporteFU.FU_TM, '0' )           as FU_TM,
      _transporteFO.agente_servicio,
      _transporteFO.desc_agente_servicio,
      _transporteFO.fecha_actual_transporte,
      _pedidoTraslado.Status_Novedad_Transportista,
      _pedidoTraslado.Desc_Sta_Novedad_Transportista,
      _pedidoTraslado.Fecha_entrega_Cliente,
      _pedidoTraslado.Novedad_Transportista,
      //Solicitante
      _solicitante.kunnr                          as solicitante,
      _solicitante.name1                          as nombre_solicitante,
      _solicitante.stcd1                          as id_fiscal_solicitante,
      _solicitante.land1                          as pais_solicitante,
      _solicitante.city1                          as ciudad_solicitante,
      _solicitante.tel_number                     as telefono_solicitante,
      _solicitante.mc_street                      as direccion_solicitante,
      _solicitante.transpzone                     as zonatransporte_solicitante,
      //
      _historial_ent_mercancia.Budat              as fecha_contabiliza_recepcion,
      _historial_ent_mercancia.Menge              as cantidad_recibida,
      'X'                                         as pedido_traslado,
      _pedidoTraslado.criterios_agrupa_entrega
      
      

}

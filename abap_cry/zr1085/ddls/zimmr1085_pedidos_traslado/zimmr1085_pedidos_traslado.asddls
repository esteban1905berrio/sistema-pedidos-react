@AbapCatalog.sqlViewName: 'ZIMMR1085_7'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Pedidos de traslado'
define view ZIMMR1085_PEDIDOS_TRASLADO
  as select from    ekpv                          as _datosTraslado
    inner join      ekpo                          as _documentoCompra_detalle       on  _datosTraslado.ebeln           =  _documentoCompra_detalle.ebeln
                                                                                    and _datosTraslado.ebelp           =  _documentoCompra_detalle.ebelp
                                                                                    //Se comenta para dar paso a escenario de posiciones con indicador de borrado
                                                                                    //and _documentoCompra_detalle.stapo <> 'X'
    inner join      ekko                          as _documentoCompra_Cabecera      on _documentoCompra_detalle.ebeln = _documentoCompra_Cabecera.ebeln
  //Vista que contiene historial de documento consolidado por entrega.
  //Se descarta el detalle a nivel de posicion dado que existen posiciones de pedido
  //asociados a varias posiciones de la misma entrega
    left outer join ZIMMR1085_HISTORIAL_DOCUMENTO as _historialDocumento            on  _historialDocumento.ebeln = _documentoCompra_detalle.ebeln
                                                                                    and _historialDocumento.ebelp = _documentoCompra_detalle.ebelp
//    left outer join ekbe                          as _historiaDoc_entrada_mercancia on  _historiaDoc_entrada_mercancia.ebeln = _documentoCompra_detalle.ebeln
//                                                                                    and _historiaDoc_entrada_mercancia.ebelp = _documentoCompra_detalle.ebelp
//                                                                                    and _historialDocumento.bwart            = '101'
    left outer join tbsgt                         as _motivoPedido                  on  _motivoPedido.bsgru = _documentoCompra_detalle.bsgru
                                                                                    and _motivoPedido.spras = $session.system_language
    left outer join t024                          as _grupoCompra                   on _grupoCompra.ekgrp = _documentoCompra_Cabecera.ekgrp
    
    left outer join likp                          as _cab_entrega on _cab_entrega.vbeln = _historialDocumento.entrega 

{
  key _documentoCompra_Cabecera.ebeln,
  key _documentoCompra_detalle.ebelp,
      _documentoCompra_detalle.pstyp    as tipo_posicion,
      _documentoCompra_Cabecera.bsart   as clase_documento,
      _documentoCompra_Cabecera.loekz,
      _datosTraslado.vkorg,
      _datosTraslado.vtweg,
      _datosTraslado.spart,
      _documentoCompra_detalle.loekz    as indicador_borrado,
      _documentoCompra_detalle.stapo    as posicion_es_estadistica,
      _documentoCompra_detalle.eglkz    as indic_entrega_salida_completa,
      _documentoCompra_Cabecera.ihrez,
      _documentoCompra_detalle.elikz,
      _documentoCompra_detalle.kztlf    as entrega_parcial_posicion,
      _documentoCompra_detalle.matnr,
      _documentoCompra_detalle.bsgru    as motivo_pedido,
      _motivoPedido.bezei               as descripcion_motivo_de_pedido,
      _documentoCompra_Cabecera.ekgrp   as grupo_compra,
      _grupoCompra.eknam                as descripcion_grupo_compra,
      _datosTraslado.vkorg              as Organizacion_Ventas,
      _documentoCompra_detalle.sgt_rcat as segmento,
      _datosTraslado.vtweg              as Canal_distribucion,
      _documentoCompra_Cabecera.reswk   as Centro,
//      _documentoCompra_Cabecera.grwcu   as moneda, //-SLS 20022023
      _documentoCompra_Cabecera.waers   as moneda,
      _documentoCompra_Cabecera.aedat   as fecha_creacion_pedido,
      _datosTraslado.kunnr,
      _documentoCompra_Cabecera.reswk   as werks,
      _documentoCompra_detalle.werks    as centro_receptor,
      _documentoCompra_detalle.lgort,
      _documentoCompra_detalle.reslo,
      _documentoCompra_detalle.meins    as unidad_venta,
      _documentoCompra_detalle.meins    as unidad_medida,
      _documentoCompra_detalle.menge    as Cantidad_pedida,
      _documentoCompra_detalle.cnfm_qty as cantidad_confirmada,
      _documentoCompra_detalle.effwr    as valor_neto,
      _documentoCompra_detalle.netwr    as valor_neto_pedido,
      case
      when _documentoCompra_detalle.menge <> 0 then
        division( _documentoCompra_detalle.cnfm_qty , _documentoCompra_detalle.menge, 3 )
      else
        0
      end                               as porcentaje_cumplimiento,
      //      case
      //      when _documentoCompra_detalle.elikz = '' OR _documentoCompra_detalle.elikz is null then
      //        ( _documentoCompra_detalle.menge -  _datosTraslado )
      //      ELSE
      //        0
      //        end as cantidad_pendiente
      case
      when _documentoCompra_detalle.loekz = 'L' then
      'X'
      else
       ''
      end                               as status_cancelado,
      _historialDocumento.zekkn,
      _historialDocumento.vgabe,
      _historialDocumento.entrega,
      _historialDocumento.bwart,
      _historialDocumento.vtwiv,
      _historialDocumento.kunag,
      _historialDocumento.objek,
      _historialDocumento.fecha_creacion_entrega,
      _historialDocumento.Clase_entrega,
      _historialDocumento.anzpk,
      _historialDocumento.kostk,
      _historialDocumento.unidadmedida_entrega,
      _historialDocumento.cantidad_entrega,
      _historialDocumento.Estado_picking,
      _historialDocumento.estado_picking_ewm,
      _historialDocumento.guia_transporte,
      _historialDocumento.estado_movimiento_mercancia,
      _historialDocumento.Status_Novedad_Transportista,
      _historialDocumento.Desc_Sta_Novedad_Transportista,
      _historialDocumento.Fecha_entrega_Cliente,
      _historialDocumento.Novedad_Transportista,
      _historialDocumento.zz1_numeroguia_dlh,
      _historialDocumento.zz1_numeroguianueva_dlh,
      _historialDocumento.zz1_tipovinculo_dlh,
      _historialDocumento.zz1_horaentregacliente_dlh,
      _historialDocumento.orden_flete,
      _historialDocumento.unidad_flete,
      _cab_entrega.zukrl as criterios_agrupa_entrega

}

@AbapCatalog.sqlViewName: 'ZISDR1085_2'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@ObjectModel.usageType.serviceQuality: #X
@ObjectModel.usageType.sizeCategory: #L
@ObjectModel.usageType.dataClass: #MIXED
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Datos Monitor Pedidos de Venta'
define view ZISDR1085_MonitorPedidos
  as select from    ZISDR1085_PEDIDO_VENTA        as _pedido
    left outer join ZISDR1085_EntregaVenta        as _entrega_posicion         on  _entrega_posicion.pedido          = _pedido.vbeln
                                                                               and _entrega_posicion.posicion_pedido = _pedido.posnr
  //    left outer join ausp                           as _caracteristica        on  _caracteristica.objek = _pedido.objek
  //                                                                             and _caracteristica.atinn = '0000001215' //= 'SD_PLU'
  //                                                                             and _caracteristica.klart = '052'
    left outer join ZISDR1085_ENTREGA_TRANSPORTE  as _transporteFO             on _transporteFO.entrega = _entrega_posicion.entrega
    left outer join ZISDR1085_ENTREGA_TM_FU       as _transporteFU             on _transporteFU.entrega_erp = _entrega_posicion.entrega
    left outer join ZPSDR1085_STATUSPLANOOLA      as _statusPlanoOla           on _statusPlanoOla.entrega = _entrega_posicion.entrega
  //Facturas asociadas a Entrega
    left outer join ZISDR1085_FacturaVenta        as _facturaEntrega           on  _facturaEntrega.fksto            = '' //Factura NO anulada
                                                                               and _facturaEntrega.entrega          = _entrega_posicion.entrega
                                                                               and _facturaEntrega.posicion_entrega = _entrega_posicion.posicion_entrega
  //Facturas asociadas a Pedido
    left outer join ZISDR1085_FACTURAVENTAPEDIDO  as _facturaPedido            on  _facturaPedido.fksto           = '' //Factura NO anulada
                                                                               and _facturaPedido.pedido          = _pedido.vbeln
                                                                               and _facturaPedido.posicion_pedido = _pedido.posnr
    left outer join ZISDR1085_ASIGNACION_STOCK    as _asignacion_stock         on  _asignacion_stock.pedido          = _pedido.vbeln
                                                                               and _asignacion_stock.posicion_pedido = _pedido.posnr
  //    left outer join ZISDR1085_INVENTARIODISPONIBLE as _inventarioDisponible  on  _inventarioDisponible.matnr = _pedido.matnr
  //                                                                             and _inventarioDisponible.werks = _pedido.werks
  //                                                                             and _inventarioDisponible.lgort = _pedido.lgort

    left outer join ZISDR1085_Inventario_traslado as _Inventario_traslado      on _Inventario_traslado.vbeln = _pedido.vbeln
    left outer join vbep                          as _reparto                  on  _reparto.vbeln = _pedido.vbeln
                                                                               and _reparto.posnr = _pedido.posnr
                                                                               and _reparto.etenr = '0001'
    left outer join mara                          as _material                 on _material.matnr = _pedido.matnr
    left outer join mean                          as _ean                      on _ean.matnr = _material.matnr
    left outer join wrf_charvalt                  as _caracteristica_color     on  _material.color_atinn       = _caracteristica_color.atinn
                                                                               and _material.color             = _caracteristica_color.atwrt
                                                                               and _caracteristica_color.spras = $session.system_language
    left outer join wrf_charvalt                  as _caracteristica_talla     on  _material.size1_atinn       = _caracteristica_talla.atinn
                                                                               and _material.size1             = _caracteristica_talla.atwrt
                                                                               and _caracteristica_talla.spras = $session.system_language
    left outer join makt                          as _material_txt             on  _material_txt.matnr = _material.matnr
                                                                               and _material_txt.spras = $session.system_language

  //    left outer join ZISDR1085_IDOC_FI             as _documento_fi          on _documento_fi.pedido_cliente = _datos_comercial.bstkd
    left outer join tvlst                         as _motivo_bloque_entrega    on  _motivo_bloque_entrega.lifsp = _pedido.Motivo_Bloqueo_Entrega
                                                                               and _motivo_bloque_entrega.spras = $session.system_language
    left outer join tvfst                         as _motivo_bloque_factura    on  _motivo_bloque_factura.faksp = _pedido.Motivo_Bloqueo_Factura
                                                                               and _motivo_bloque_factura.spras = $session.system_language
    left outer join tvagt                         as _motivo_rechazo           on  _motivo_rechazo.abgru = _pedido.motivo_rechazo
                                                                               and _motivo_rechazo.spras = $session.system_language
  //
    left outer join fsh_v_vass_tl                 as _datos_adicionales        on  _datos_adicionales.fsh_vgbel       =  _pedido.vbeln
                                                                               and _datos_adicionales.fsh_vas_sub_ser =  '20_3'
                                                                               and _datos_adicionales.fsh_transaction <> ''
    left outer join fsh_v_vass_tl                 as _datos_adicionalesEmpaque on  _datos_adicionalesEmpaque.fsh_vgbel       =  _pedido.vbeln
                                                                               and _datos_adicionalesEmpaque.fsh_vas_sub_ser =  '10_1'
                                                                               and _datos_adicionalesEmpaque.fsh_transaction <> ''
    left outer join tvaut                         as _motivo_pedido            on  _motivo_pedido.augru = _pedido.motivo_pedido
                                                                               and _motivo_pedido.spras = $session.system_language
    left outer join zz1_d6030c7a705b              as _motivo_pedidoPosicion    on  _motivo_pedidoPosicion.code     = _pedido.motivo_pedido_posicion
                                                                               and _motivo_pedidoPosicion.language = $session.system_language
    left outer join zz1_ee79e2b74c06              as _clasifiacion             on  _clasifiacion.code     = _pedido.zz1_clasificacion_sdh
                                                                               and _clasifiacion.language = $session.system_language
    left outer join ztfii1014_6                   as _tipomediopago            on _tipomediopago.ctmpg = _pedido.zz1_tipomediopago_sdh
{
  key _pedido.vbeln                                as pedido,
  key _pedido.posnr                                as posicion_pedido,
      _pedido.vkorg,
      _pedido.vtweg,
      _pedido.spart,
      _pedido.werks,
      _pedido.lgort,
      //      _entrega_posicion.lote,
      _pedido.erdat                                as fecha_creacion_pedido,
      _pedido.erzet                                as hora_creacion_pedido,
      _pedido.vdatu                                as Fecha_Preferente_Entrega,
      _pedido.kunnr,
      _pedido.auart                                as clase_documento,
      _pedido.objek,
      _pedido.sgt_rcat                             as segmento_necesidad,
      _pedido.kdmat                                as plu,
      _pedido.pedido_cliente,
      _pedido.cmfre                                as Fecha_liberacion,
      Fecha_Anulacion,
      zz1_clasificacion_sdh                        as clasificacion,
      //      clasificacion_desc,
      _clasifiacion.description                    as clasificacion_desc,

      zz1_marca_sdh                                as Marca_sd,
      zz1_tipomediopago_sdh                        as Tipo_Medio_Pago,
      //      tipomediopago_desc,
      _tipomediopago.cmdtx                         as tipomediopago_desc,
      zz1_mediopago_sdh                            as Medio_Pago,
      mediopago_desc,

      //      _pedido.lifsk                                                                                     as Motivo_Bloqueo_Entrega,
      //      _motivo_bloque_entrega.vtext                                                                      as Motivo_Bloqueo_Entrega_desc,
      //      _pedido.faksk                                                                                     as Motivo_Bloqueo_Factura,
      //      _motivo_bloque_factura.vtext                                                                      as Motivo_Bloqueo_Factura_desc,
      //      _pedido_posicion.abgru                                                                            as motivo_rechazo,
      //      _motivo_rechazo.bezei                                                                             as Motivo_Rechazo_desc,

      _pedido.Motivo_Bloqueo_Entrega,
      _motivo_bloque_entrega.vtext                 as Motivo_Bloqueo_Entrega_desc,
      _pedido.Motivo_Bloqueo_Factura,
      _motivo_bloque_factura.vtext                 as Motivo_Bloqueo_Factura_desc,
      _pedido.motivo_rechazo,
      _motivo_rechazo.bezei                        as Motivo_Rechazo_desc,

      _pedido.antlf                                as cantidad_entregada_parcial,
      _pedido.kzwi1                                as valor_neto,
      sgt_rcat                                     as segmento,
      _pedido.gbstk                                as Estado_global_de_procesamiento,
      _pedido.waerk                                as moneda,
      //      @Semantics.quantity.unitOfMeasure : 'mara.meins'
      //      _inventarioDisponible.inventario_disponible,
      @Semantics.quantity.unitOfMeasure : 'arun_bdbs.material_baseunit'
      _asignacion_stock.cantidad_asignada,
      _pedido.precio_neto_unitario,
      //      _pedido.precio_sugerido,
      //      _pedido.precio_cliente,
      //      _pedido.precio_sugerido_cliente,
      //      _pedido.margen_cliente,
      //      _pedido.precio_exportacion,
      @Semantics.amount.currencyCode: 'moneda'
      _facturaEntrega.wavwr                        as costo_interno,
      @Semantics.amount.currencyCode: 'moneda'
      _pedido.netwr                                as valor_neto_pedido,
      _pedido.knumv,
      mbdat                                        as Fecha_confirmada,
      _pedido.pstyv                                as Tipo_posicion,
      _pedido.matnr,
      _material_txt.maktx,
      _pedido.material_generico,
      _pedido.desc_material_generico,
      _material.size1                              as talla,
      _caracteristica_talla.atwtb                  as descripcion_talla,
      _material.color,
      _caracteristica_color.atwtb                  as descripcion_color,
      _ean.ean11,
      _material.brand_id                           as marca,
      _material.matkl                              as grupo_de_articulo,
      _pedido.vrkme                                as unidad_venta,
      _pedido.meins                                as unidad_medida,
      //Pedido paquete completo
      _pedido.material_cliente                     as material_cliente,
      _pedido.descripcion_mat_cliente,
      _pedido.talla_cliente,
      _pedido.descripcion_talla_cliente,
      _pedido.color_cliente,
      _pedido.descripcion_color_cliente,
      _pedido.motivo_pedido,

      //      _pedido.descripcion_motivo_de_pedido,
      _motivo_pedido.bezei                         as descripcion_motivo_de_pedido,

      _pedido.motivo_pedido_posicion,

      //      _pedido.descripcion_motivo_pedido_pos,
      _motivo_pedidoPosicion.description           as descripcion_motivo_pedido_pos,

      --Inicio SGR 25.11.2022 Cambio de la extracción para la marca de regalo
      //      _datos_adicionalesEmpaque.fsh_transaction    as ecommerce_empaque_regalo,
      //      SUBSTRING(_datos_adicionalesEmpaque.fsh_cust_field1, 1, 1) as ecommerce_empaque_regalo,
      _datos_adicionalesEmpaque.fsh_cust_field1    as ecommerce_empaque_regalo,
      --Fin SGR 25.11.2022 Cambio de la extracción para la marca de regalo
      //      case
      //      when _pedido.ecommerce_empaque_regalo != '' or _pedido.ecommerce_empaque_regalo is not null then
      //       'X'
      //      end                                          as ecommerce_empaque_regalo,
      //      _pedido.ecommerce_empaque_regalo,
      //      _pedido.ecommerce_destinatario,
      //      _pedido.ecommerce_remitente,
      _datos_adicionales.fsh_cust_field1           as ecommerce_remitente,
      _datos_adicionales.fsh_cust_field2           as ecommerce_destinatario,

      @Semantics.quantity.unitOfMeasure: 'unidad_medida'
      _pedido.kwmeng                               as Cantidad_pedida,
      _pedido.kbmeng                               as cantidad_confirmada,
      _pedido.mwsbp                                as pedido_impuesto_posicion,
      _Inventario_traslado.ihrez                   as Guia_Devolucion,
      //entrega
      _entrega_posicion.entrega,
      _entrega_posicion.posicion_entrega,
      _entrega_posicion.fecha_creacion_entrega,
      Clase_entrega,
      _entrega_posicion.SubsequentDocumentCategory as tipo_documento_entrega,
      unidadmedida_entrega,
      _entrega_posicion.cantidad_entrega,
      Estado_picking,
      estado_picking_ewm,
      guia_transporte,
      _entrega_posicion.Cantidad_Despachada,
      _entrega_posicion.estado_movimiento_mercancia,
      cast( _entrega_posicion.anzpk as abap.int4 ) as cantidad_cajas_erp,
      _entrega_posicion.kostk,
      _entrega_posicion.orden_flete,
      _entrega_posicion.unidad_flete,
      _entrega_posicion.uecha,
      _entrega_posicion.zz1_numeroguianueva_dlh,
      _entrega_posicion.zz1_numeroguia_dlh,
      _entrega_posicion.zz1_tipovinculo_dlh,
      _entrega_posicion.zz1_horaentregacliente_dlh as hora_entrega_cliente,
      _statusPlanoOla.status_plano_ola,
      //Factura
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.factura
      else
        _facturaPedido.factura end                 as factura,
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.Fecha_creacion_registro
      else
        _facturaPedido.Fecha_creacion_registro end as fecha_factura,
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.posicion_factura
      else
        _facturaPedido.posicion_factura end        as posicion_factura,
      @Semantics.amount.currencyCode: 'moneda'
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.valor_factura
      else
        _facturaPedido.valor_factura end           as valor_factura,
      @Semantics.quantity.unitOfMeasure: 'unidadmedida_entrega'
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.cantidad_facturada
      else
        _facturaPedido.cantidad_facturada end      as cantidad_facturada,
      @Semantics.amount.currencyCode: 'moneda'
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.valor_neto
      else
        _facturaPedido.valor_neto end              as valor_neto_factura,
      @Semantics.amount.currencyCode: 'moneda'
      case
      when _facturaEntrega.factura is not null then
        _facturaEntrega.total_impuesto
      else
        _facturaPedido.total_impuesto end          as total_impuesto_factura,
      //    "{ Inicio SGR 16.11.2022 | Borrar interlocutores
      //////////////      //tiendas
      //////////////      tienda,
      //////////////      nombre_tienda,
      //////////////      id_fiscal_tienda,
      //////////////      pais_tienda,
      //////////////      telefono_tienda,
      //////////////      direccion_tienda,
      //////////////      zonatransporte_tienda,
      //////////////      //Solicitante
      //////////////      solicitante,
      //////////////      nombre_solicitante,
      //////////////      id_fiscal_solicitante,
      //////////////      pais_solicitante,
      //////////////      ciudad_solicitante,
      //////////////      telefono_solicitante,
      //////////////      direccion_solicitante,
      //////////////      zonatransporte_solicitante,
      //////////////      //Responsable pago
      //////////////      responsable_pago,
      //////////////      nombre_responsable_pago,
      //////////////      id_fiscal_responsable_pago,
      //////////////      pais_responsable_pago,
      //////////////      ciudad_responsable_pago,
      //////////////      telefono_responsable_pago,
      //////////////      direccion_responsable_pago,
      //////////////      zonatransporte_resp_pago,
      //////////////      //destinatario
      //////////////      destinatario,
      //////////////      nombre_destinatario,
      //////////////      id_fiscal_destinatario,
      //////////////      pais_destinatario,
      //////////////      ciudad_destinatario,
      //////////////      telefono_destinatario,
      //////////////      direccion_destinatario,
      //////////////      zonatransporte_destinatario,
      //////////////      //Encargado Comercial
      //////////////      encargado_comercial,
      //////////////      nombre_encargado_comercial,
      //////////////      id_fiscal_encargado_comercial,
      //////////////      pais_encargado_comercial,
      //////////////      ciudad_encargado_comercial,
      //////////////      telefono_encargado_comercial,
      //////////////      direccion_encargado_comercial,
      //    "{ Fin SGR 16.11.2022 | Borrar interlocutores
      //FI
      _pedido.idoc_fi,
      _pedido.documento_fi,
      _pedido.sociedad_fi,
      _pedido.ejercicio_fi,
      //Transporte
      _transporteFO.numero_transporte,
      _transporteFU.FU_TM,
      _transporteFO.agente_servicio,
      _transporteFO.desc_agente_servicio,
      _transporteFO.fecha_actual_transporte,
      _entrega_posicion.Status_Novedad_Transportista,
      _entrega_posicion.Desc_Sta_Novedad_Transportista,
      _entrega_posicion.Fecha_entrega_Cliente,
      _entrega_posicion.Novedad_Transportista,
      _entrega_posicion.zz1_fechprivisit_dlh,
      _entrega_posicion.zz1_horaprivisit_dlh

}

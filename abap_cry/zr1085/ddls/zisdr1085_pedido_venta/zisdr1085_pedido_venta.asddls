@AbapCatalog.sqlViewName: 'ZISDR1085_10'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@ObjectModel.usageType.serviceQuality: #X
@ObjectModel.usageType.sizeCategory: #XL
@ObjectModel.usageType.dataClass: #MIXED
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Pedidos de venta para monitor'
define view ZISDR1085_PEDIDO_VENTA
  as select from    vbak                           as _pedido
    inner join      vbap                           as _pedido_posicion          on _pedido_posicion.vbeln = _pedido.vbeln
    left outer join mara                           as _materialGenerico         on _materialGenerico.matnr = _pedido_posicion.matnr
    left outer join makt                           as _desc_material_generico   on  //_desc_material_generico.matnr = _pedido_posicion.upmat
                                                                                    _desc_material_generico.matnr = _materialGenerico.satnr //+SLS 29.04.2023:Ajustar material generico
                                                                                and _desc_material_generico.spras = $session.system_language
    left outer join knmt                           as _info_material_cliente    on  _info_material_cliente.vkorg = _pedido_posicion.vkorg_ana
                                                                                and _info_material_cliente.vtweg = _pedido_posicion.vtweg_ana
                                                                                and _info_material_cliente.kunnr = _pedido_posicion.kunnr_ana
                                                                                and _info_material_cliente.matnr = _pedido_posicion.matnr
    left outer join ZISDR1085_DATOS_COMERCIALES    as _datos_comercial          on _datos_comercial.vbeln = _pedido_posicion.vbeln
    left outer join ZISDR1085_IDOC_FI              as _documento_fi             on _documento_fi.pedido_cliente = _datos_comercial.bstkd
    left outer join ZISDR1085_PED_PAQCOMPLETO      as _pedidoPaqCompleto        on  _pedidoPaqCompleto.matnr = _pedido_posicion.upmat
                                                                                and _pedidoPaqCompleto.Vkorg = _pedido_posicion.vkorg_ana
                                                                                and _pedidoPaqCompleto.Vtweg = _pedido_posicion.vtweg_ana
                                                                                and _pedidoPaqCompleto.Kunnr = _pedido_posicion.kunnr_ana
    left outer join ZPSDR1085_MATALLACOLOR_CLIENTE as _caracteristicaColorTalla on  _caracteristicaColorTalla.Matnr         = _pedido_posicion.matnr
                                                                                and _caracteristicaColorTalla.IdConvCliente = _pedidoPaqCompleto.fsh_sc_cid
                                                                                and _caracteristicaColorTalla.Idioma        = $session.system_language

  //    left outer join       ZISDR1085_PED_PAQCOMPLETO      as _pedidoPaqCompleto        on  _pedidoPaqCompleto.vbeln = _pedido_posicion.vbeln
  //                                                                                      and _pedidoPaqCompleto.posnr = _pedido_posicion.posnr
  //    "{ Inicio SGR 16.11.2022 | Borrar interlocutores
  ////////    left outer join ZISDR1085_INTERLO_TIENDA       as _tiendas                  on _tiendas.pedido = _pedido.vbeln
  ////////    left outer join ZISDR1085_INTERLO_SOLICITANTE  as _solicitante              on _solicitante.pedido = _pedido.vbeln
  ////////    left outer join ZISDR1085_INTERLO_RESP_PAGO    as _responsable_pago         on _responsable_pago.pedido = _pedido.vbeln
  ////////    left outer join ZISDR1085_INTERLO_DESTINATARIO as _destinatario             on _destinatario.pedido = _pedido.vbeln
  ////////    left outer join ZISDR1085_INTERLO_ENCOMERCIAL  as _encargadoComercial       on _encargadoComercial.pedido = _pedido.vbeln
  //    "{ Fin SGR 16.11.2022 | Borrar interlocutores
  //    left outer join       prcd_elements                  as _precioSugerido           on  _precioSugerido.knumv = _pedido.knumv
  //                                                                                      and _precioSugerido.kposn = _pedido_posicion.posnr
  //                                                                                      and _precioSugerido.kschl = 'ZPRS'
  //    left outer join       prcd_elements                  as _precioCliente            on  _precioCliente.knumv = _pedido.knumv
  //                                                                                      and _precioCliente.kposn = _pedido_posicion.posnr
  //                                                                                      and _precioCliente.kschl = 'EDI1'
  //    left outer join       prcd_elements                  as _precioSugeridoCliente    on  _precioSugeridoCliente.knumv = _pedido.knumv
  //                                                                                      and _precioSugeridoCliente.kposn = _pedido_posicion.posnr
  //                                                                                      and _precioSugeridoCliente.kschl = 'EDI2'
  //    left outer join       prcd_elements                  as _margenCliente            on  _margenCliente.knumv = _pedido.knumv
  //                                                                                      and _margenCliente.kposn = _pedido_posicion.posnr
  //                                                                                      and _margenCliente.kschl = 'ZMAR'
  //    left outer join       prcd_elements                  as _precioExportacion        on  _precioExportacion.knumv = _pedido.knumv
  //                                                                                      and _precioExportacion.kposn = _pedido_posicion.posnr
  //                                                                                      and _precioExportacion.kschl = 'ZPEX'

  //    left outer join fsh_v_vass_tl                  as _datos_adicionales        on  _datos_adicionales.fsh_vgbel       =  _pedido_posicion.vbeln
  //                                                                                and _datos_adicionales.fsh_vas_sub_ser =  '20_3'
  //                                                                                and _datos_adicionales.fsh_transaction <> ''
  //    left outer join fsh_v_vass_tl                  as _datos_adicionalesEmpaque on  _datos_adicionalesEmpaque.fsh_vgbel       =  _pedido_posicion.vbeln
  //                                                                                and _datos_adicionalesEmpaque.fsh_vas_sub_ser =  '10_1'
  //                                                                                and _datos_adicionalesEmpaque.fsh_transaction <> ''
  //    left outer join tvaut                          as _motivo_pedido            on  _motivo_pedido.augru = _pedido.augru
  //                                                                                and _motivo_pedido.spras = $session.system_language
  //    left outer join zz1_d6030c7a705b               as _motivo_pedidoPosicion    on  _motivo_pedidoPosicion.code     = _pedido_posicion.zz1_augru_sdi
  //                                                                                and _motivo_pedidoPosicion.language = $session.system_language
  //    left outer join zz1_ee79e2b74c06               as _clasifiacion             on  _clasifiacion.code     = _pedido.zz1_clasificacion_sdh
  //                                                                                and _clasifiacion.language = $session.system_language
  //    left outer join ztfii1014_6                    as _tipomediopago            on _tipomediopago.ctmpg = _pedido.zz1_tipomediopago_sdh

    left outer join ZISDR1085_MEDIO_PAGO           as _mediopago                on _mediopago.Cmdpg_2 = _pedido.zz1_mediopago_sdh

{
  key _pedido.vbeln,
  key _pedido_posicion.posnr,
      _pedido.vkorg,
      _pedido.vtweg,
      _pedido.spart,
      _pedido_posicion.werks,
      _pedido.vbtyp,
      _pedido.erdat,
      _pedido.erzet,
      _pedido.vdatu,
      _pedido.kunnr,
      _pedido.auart,
      _datos_comercial.bstkd                                                                            as pedido_cliente,
      _pedido.cmfre,
      _pedido.fsh_candate                                                                               as Fecha_Anulacion,
      _pedido.augru                                                                                     as motivo_pedido,
      //      _motivo_pedido.bezei                                                                              as descripcion_motivo_de_pedido,
      _pedido_posicion.zz1_augru_sdi                                                                    as motivo_pedido_posicion,
      //      _motivo_pedidoPosicion.description                                                                as descripcion_motivo_pedido_pos,
      concat( concat( concat( _pedido.vkorg, _pedido.vtweg ), _pedido.kunnr ), _pedido_posicion.matnr ) as objek,
      zz1_clasificacion_sdh,
      //      _clasifiacion.description                                                                         as clasificacion_desc,
      zz1_marca_sdh,
      zz1_tipomediopago_sdh,
      //      _tipomediopago.cmdtx                                                                              as tipomediopago_desc,
      zz1_mediopago_sdh,
      _mediopago.Cmdtx                                                                                  as mediopago_desc,

      _pedido.lifsk                                                                                     as Motivo_Bloqueo_Entrega,
      //      _motivo_bloque_entrega.vtext                                                                      as Motivo_Bloqueo_Entrega_desc,
      _pedido.faksk                                                                                     as Motivo_Bloqueo_Factura,
      //      _motivo_bloque_factura.vtext                                                                      as Motivo_Bloqueo_Factura_desc,
      _pedido_posicion.abgru                                                                            as motivo_rechazo,
      //      _motivo_rechazo.bezei                                                                             as Motivo_Rechazo_desc,

      _pedido.waerk,
      _pedido_posicion.lgort,
      _pedido_posicion.netpr                                                                            as precio_neto_unitario,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      _precioSugerido.kbetr                                                                             as precio_sugerido,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      _precioCliente.kbetr                                                                              as precio_cliente,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      _precioSugeridoCliente.kbetr                                                                      as precio_sugerido_cliente,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      _margenCliente.kbetr                                                                              as margen_cliente,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      _precioExportacion.kbetr                                                                          as precio_exportacion,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      cast( _precioSugerido.kbetr as abap.curr( 15, 2 ))                                                as precio_sugerido,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      cast( _precioCliente.kbetr as abap.curr( 15, 2 ))                                                 as precio_cliente,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      cast( _precioSugeridoCliente.kbetr as abap.curr( 15, 2 ))                                         as precio_sugerido_cliente,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      cast( _margenCliente.kbetr as abap.curr( 15, 2 ))                                                 as margen_cliente,
      //      @Semantics.amount.currencyCode: 'waerk'
      //      cast( _precioExportacion.kbetr as abap.curr( 15, 2 ))                                             as precio_exportacion,
      _pedido.netwr,
      _pedido.knumv,
      _pedido.gbstk,
      _pedido_posicion.antlf,
      _pedido_posicion.kzwi1,
      sgt_rcat,
      pstyv,
      _pedido_posicion.wavwr,
      case
      when _pedido_posicion.upmat is initial or _pedido_posicion.upmat is null then
        _materialGenerico.satnr
      else
        _pedido_posicion.upmat
      end                                                                                               as material_generico,
      _desc_material_generico.maktx                                                                     as desc_material_generico,
      _pedido_posicion.matnr,
      _pedido_posicion.vrkme,
      _pedido_posicion.meins,
      @Semantics.quantity.unitOfMeasure: 'meins'
      _pedido_posicion.kwmeng,
      _pedido_posicion.kbmeng,
      _pedido_posicion.mwsbp,
      //      _datos_adicionales.fsh_cust_field1                                                                as ecommerce_remitente,
      //      _datos_adicionales.fsh_cust_field2                                                                as ecommerce_destinatario,
      _info_material_cliente.kdmat,
      //      case
      //      when _datos_adicionalesEmpaque.fsh_transaction != '' or _datos_adicionalesEmpaque.fsh_transaction is not null then
      //        'X'
      //      else
      //        ''
      //      end                                                                                               as ecommerce_empaque_regalo,
      //      _datos_adicionalesEmpaque.fsh_transaction                                                         as ecommerce_empaque_regalo,
      //Pedido paquete completo
      _pedidoPaqCompleto.material_cliente                                                               as material_cliente,
      _pedidoPaqCompleto.descripcion_mat_cliente,
      _caracteristicaColorTalla.TallaCaracteristica                                                     as talla_cliente,
      _caracteristicaColorTalla.TallaDescripcionCaract                                                  as descripcion_talla_cliente,
      _caracteristicaColorTalla.ColorCaracteristica                                                     as color_cliente,
      _caracteristicaColorTalla.ColorDescripcionCaract                                                  as descripcion_color_cliente,
      //    "{ Inicio SGR 16.11.2022 | Borrar interlocutores
      //////////      //tiendas
      //////////      _tiendas.kunnr                                                                                    as tienda,
      //////////      _tiendas.name1                                                                                    as nombre_tienda,
      //////////      _tiendas.stcd1                                                                                    as id_fiscal_tienda,
      //////////      _tiendas.land1                                                                                    as pais_tienda,
      //////////      _tiendas.tel_number                                                                               as telefono_tienda,
      //////////      _tiendas.mc_street                                                                                as direccion_tienda,
      //////////      _tiendas.transpzone                                                                               as zonatransporte_tienda,
      //////////      //Solicitante
      //////////      _solicitante.kunnr                                                                                as solicitante,
      //////////      _solicitante.name1                                                                                as nombre_solicitante,
      //////////      _solicitante.stcd1                                                                                as id_fiscal_solicitante,
      //////////      _solicitante.land1                                                                                as pais_solicitante,
      //////////      _solicitante.city1                                                                                as ciudad_solicitante,
      //////////      _solicitante.tel_number                                                                           as telefono_solicitante,
      //////////      _solicitante.mc_street                                                                            as direccion_solicitante,
      //////////      _solicitante.transpzone                                                                           as zonatransporte_solicitante,
      //////////      //Responsable pago
      //////////      _responsable_pago.kunnr                                                                           as responsable_pago,
      //////////      _responsable_pago.name1                                                                           as nombre_responsable_pago,
      //////////      _responsable_pago.stcd1                                                                           as id_fiscal_responsable_pago,
      //////////      _responsable_pago.land1                                                                           as pais_responsable_pago,
      //////////      _responsable_pago.city1                                                                           as ciudad_responsable_pago,
      //////////      _responsable_pago.tel_number                                                                      as telefono_responsable_pago,
      //////////      _responsable_pago.mc_street                                                                       as direccion_responsable_pago,
      //////////      _responsable_pago.transpzone                                                                      as zonatransporte_resp_pago,
      //////////      //destinatario
      //////////      _destinatario.kunnr                                                                               as destinatario,
      //////////      _destinatario.name1                                                                               as nombre_destinatario,
      //////////      _destinatario.stcd1                                                                               as id_fiscal_destinatario,
      //////////      _destinatario.land1                                                                               as pais_destinatario,
      //////////      _destinatario.city1                                                                               as ciudad_destinatario,
      //////////      _destinatario.tel_number                                                                          as telefono_destinatario,
      //////////      _destinatario.mc_street                                                                           as direccion_destinatario,
      //////////      _responsable_pago.transpzone                                                                      as zonatransporte_destinatario,
      //////////      //Encargado Comercial
      //////////      _encargadoComercial.kunnr                                                                         as encargado_comercial,
      //////////      _encargadoComercial.name1                                                                         as nombre_encargado_comercial,
      //////////      _encargadoComercial.stcd1                                                                         as id_fiscal_encargado_comercial,
      //////////      _encargadoComercial.land1                                                                         as pais_encargado_comercial,
      //////////      _encargadoComercial.city1                                                                         as ciudad_encargado_comercial,
      //////////      _encargadoComercial.tel_number                                                                    as telefono_encargado_comercial,
      //////////      _encargadoComercial.mc_street                                                                     as direccion_encargado_comercial,
      //    "{ Fin SGR 16.11.2022 | Borrar interlocutores
      //FI
      _documento_fi.numero_idoc                                                                         as idoc_fi,
      _documento_fi.numero_documento_fi                                                                 as documento_fi,
      _documento_fi.sociedad                                                                            as sociedad_fi,
      _documento_fi.ejercicio                                                                           as ejercicio_fi
}

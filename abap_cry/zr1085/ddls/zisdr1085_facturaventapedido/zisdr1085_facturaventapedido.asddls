@AbapCatalog.sqlViewName: 'ZISDR1085_14'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Facturas de venta asociada a pedido'
define view ZISDR1085_FACTURAVENTAPEDIDO
  as select from I_SDDocumentProcessFlow as flujo_documentos
    inner join   vbrp                    as _BillingDocumentItem on  flujo_documentos.SubsequentDocument     = _BillingDocumentItem.vbeln
                                                                 and flujo_documentos.SubsequentDocumentItem = _BillingDocumentItem.posnr
    inner join   vbrk                    as _BillingDocument     on _BillingDocument.vbeln = _BillingDocumentItem.vbeln
{
  key flujo_documentos.SubsequentDocument                                              as factura,
  key SubsequentDocumentItem                                                           as posicion_factura,
  key flujo_documentos.PrecedingDocument                                               as pedido,
  key flujo_documentos.PrecedingDocumentItem                                           as posicion_pedido,
      _BillingDocument.fksto,
      @Semantics.amount.currencyCode: 'moneda'
      cast( ( _BillingDocument.netwr + _BillingDocument.mwsbk ) as abap.curr( 15,2 ) ) as valor_factura,
      @ObjectModel.foreignKey.association: '_TransactionCurrency'
      _BillingDocument.waerk                                                           as moneda,
      _BillingDocumentItem.pstyv,
      _BillingDocumentItem.posar                                                       as Clase_de_posicion,
      _BillingDocumentItem.shkzg                                                       as Posicion_devolucion,
      _BillingDocumentItem.erdat                                                       as Fecha_creacion_registro,
      _BillingDocumentItem.erzet                                                       as Hora_entrada,
      _BillingDocumentItem.spart                                                       as Sector,
      _BillingDocumentItem.vkbur,
      _BillingDocumentItem.matnr,
      @Semantics.quantity.unitOfMeasure: 'Unidad_medida_venta'
      _BillingDocumentItem.fkimg                                                       as cantidad_facturada,
      _BillingDocumentItem.vrkme                                                       as Unidad_medida_venta,
      @Semantics.amount.currencyCode: 'moneda'
      _BillingDocumentItem.netwr                                                       as valor_neto,
      @Semantics.amount.currencyCode: 'moneda'
      _BillingDocument.mwsbk                                                           as total_impuesto

}
where
  (    //Pedido
       //Escenario en donde las facturas estan ligadas directamente con el pedido


       flujo_documentos.PrecedingDocumentCategory = 'C'
    or flujo_documentos.PrecedingDocumentCategory = 'K'
  )    //Factura
  and(
       SubsequentDocumentCategory                 = 'M'
    or SubsequentDocumentCategory                 = 'O'
    or SubsequentDocumentCategory                 = 'P'
    or SubsequentDocumentCategory                 = 'C'
  )

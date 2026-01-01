@AbapCatalog.sqlViewName: 'ZISDR1085_3'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Facturas de venta desde entrega'
define view ZISDR1085_FacturaVenta
  as select from I_SDDocumentProcessFlow as flujo_documentos
    inner join   vbrp                    as _BillingDocumentItem on  flujo_documentos.SubsequentDocument     = _BillingDocumentItem.vbeln
                                                                 and flujo_documentos.SubsequentDocumentItem = _BillingDocumentItem.posnr
    inner join   vbrk                    as _BillingDocument     on _BillingDocument.vbeln = _BillingDocumentItem.vbeln
{
  key flujo_documentos.SubsequentDocument                                              as factura,
  key SubsequentDocumentItem                                                           as posicion_factura,
  key flujo_documentos.PrecedingDocument                                               as entrega,
  key flujo_documentos.PrecedingDocumentItem                                           as posicion_entrega,
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
      _BillingDocument.mwsbk                                                           as total_impuesto,
      @Semantics.amount.currencyCode: 'moneda'
      _BillingDocumentItem.wavwr

}
where
  (    //Entregas

       flujo_documentos.PrecedingDocumentCategory = 'J'
    or flujo_documentos.PrecedingDocumentCategory = 'H'
    or flujo_documentos.PrecedingDocumentCategory = 'T'

  ) //Factura
  and(
       SubsequentDocumentCategory                 = 'M'
    or SubsequentDocumentCategory                 = 'O'
    or SubsequentDocumentCategory                 = 'P'
    or SubsequentDocumentCategory                 = 'C'
  )

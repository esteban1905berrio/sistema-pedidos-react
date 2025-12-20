@AbapCatalog.sqlViewName: 'ZMK_BA_PO_RPT_V1'
@AbapCatalog.viewEnhancementCategory: [ #NONE ]
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Purchase Order Report'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
  serviceQuality: #X,
  sizeCategory: #S,
  dataClass: #MIXED
}

@VDM.viewType: #BASIC
define view ZMK_BA_PO_RPT 
  as select from I_PurchaseOrderAPI01 as Header
      inner join I_PurchaseOrderItemAPI01 as Item
              on Item.PurchaseOrder = Header.PurchaseOrder
{
  key Header.PurchaseOrder,
  key Item.PurchaseOrderItem,
      Header.PurchaseOrderType,
      Header.PurchaseOrderDate,
      Header.CompanyCode,
      Header.SupplyingPlant,
      
      Item.Material,
      
      @Semantics.amount.currencyCode: 'DocumentCurrency'
      Item.NetPriceAmount,
      Item.DocumentCurrency,
      
      @Semantics.quantity.unitOfMeasure: 'PurchaseOrderQuantityUnit'
      Item.OrderQuantity,
      Item.PurchaseOrderQuantityUnit
}

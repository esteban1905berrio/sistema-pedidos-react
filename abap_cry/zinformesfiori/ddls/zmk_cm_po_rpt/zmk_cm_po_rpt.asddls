@AbapCatalog.sqlViewName: 'ZMK_CM_PO_RPT_V3'
@AbapCatalog.viewEnhancementCategory: [ #NONE ]
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Purchase Order Report'
@Metadata.ignorePropagatedAnnotations: false
@ObjectModel.usageType: {
  serviceQuality: #X,
  sizeCategory: #S,
  dataClass: #MIXED
}

@Metadata.allowExtensions: true
@VDM.viewType: #CONSUMPTION
define view ZMK_CM_PO_RPT as select from ZMK_CP_PO_RPT
{
  key PurchaseOrder,
  key PurchaseOrderItem,
      PurchaseOrderType,
      PurchaseOrderDate,
      CompanyCode,
      SupplyingPlant,
      Material,
      NetPriceAmount,
      DocumentCurrency,
      OrderQuantity,
      PurchaseOrderQuantityUnit,
      TotalNetValue 
}

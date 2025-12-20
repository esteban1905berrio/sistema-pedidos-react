@AbapCatalog.sqlViewName: 'ZMK_CP_PO_RPT_V2'
@AbapCatalog.viewEnhancementCategory: [ #NONE ]
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Purchase Order Report'
@Metadata.ignorePropagatedAnnotations: false
@ObjectModel.usageType: {
  serviceQuality: #X,
  sizeCategory: #S,
  dataClass: #MIXED
}

@VDM.viewType: #COMPOSITE
define view ZMK_CP_PO_RPT as select from ZMK_BA_PO_RPT
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
      
      
      // Calculation: Total Net Value
      cast(
        cast(
          NetPriceAmount as abap.dec( 15, 2 ) ) * OrderQuantity as abap.dec( 15, 2 ) ) as TotalNetValue
}

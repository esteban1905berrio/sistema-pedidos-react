@AbapCatalog.sqlViewName: 'ZPSDR1085_5'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Inventario disponible por entrega'
define view ZPSDR1085_INVENTARIO_ENTREGA
  as select from ZTSDR1085_2
{
  key matnr,
  key werks,
  key lgort,
  key charg,
      vrkme,
      @Semantics.quantity.unitOfMeasure : 'lips.vrkme'
      cantidad_entrega
}

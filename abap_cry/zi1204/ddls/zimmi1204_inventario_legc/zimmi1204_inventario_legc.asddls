@AbapCatalog.sqlViewName: 'ZIMMI1204_2'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Conciliación inventarios Legacy-S4'
@OData.publish: true
define view zimmi1204_inventario_legc 
  as select from ztmmi1204_1
{
  key centro,
  key almacen,
  key material_variante,
  key lote,
  @Semantics.quantity.unitOfMeasure: 'unidad_medida'
  stock,
  material_generico,
  talla,
  color,  
  unidad_medida
}

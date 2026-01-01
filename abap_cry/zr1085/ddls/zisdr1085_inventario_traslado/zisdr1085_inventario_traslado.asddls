@AbapCatalog.sqlViewName: 'ZISDR1085_7'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Inventario para traslado sin documento'
define view ZISDR1085_Inventario_traslado
  as select distinct from ztsdr1095_1
{
  key vbeln,
  key ihrez
}

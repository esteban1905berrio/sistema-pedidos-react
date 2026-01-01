@AbapCatalog.sqlViewName: 'ZISDR1085_15'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Datos comerciales'
define view ZISDR1085_DATOS_COMERCIALES
  as select distinct from vbkd
{
  key vbeln,
      bstkd
}

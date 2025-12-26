@AbapCatalog.sqlViewName: 'ZISDR1085_11'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Medios de pago'
define view ZISDR1085_MEDIO_PAGO
  as select from ztfii1014_7
{
  key ztfii1014_7.zidtv        as Zidtv,
  key ztfii1014_7.cmdpg        as Cmdpg,
      substring( cmdpg, 1, 2 ) as Cmdpg_2,
      ztfii1014_7.cmdtx        as Cmdtx
}

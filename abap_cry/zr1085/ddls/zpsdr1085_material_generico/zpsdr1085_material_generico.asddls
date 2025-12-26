@AbapCatalog.sqlViewName: 'ZPSDR1085_3'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Materiales genericos'
define view ZPSDR1085_MATERIAL_GENERICO
  as select from mara
{
  key    matnr,
         case
             when satnr = '' then
                 matnr
             else
                 satnr
             end as generico
}

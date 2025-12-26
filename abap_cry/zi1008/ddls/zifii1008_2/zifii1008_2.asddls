@AbapCatalog.sqlViewName: 'ZIFII1008_LOGFI'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Informacion Idoc Financieros'
define view ZIFII1008_2
  as select from ZTFI1008_2 as k
{
  k.numero_idoc,
  k.mestyp,
  k.status,
  k.statyp,
  k.statxt,
  k.stapa1,
  k.stapa2,
  k.stapa3,
  k.stapa4,
  k.stamid,
  k.stamno,
  k.uname,
  k.logdat,
  k.logtim,
  k.countr
}

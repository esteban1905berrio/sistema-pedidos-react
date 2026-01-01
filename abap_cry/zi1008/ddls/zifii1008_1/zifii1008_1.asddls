@AbapCatalog.sqlViewName: 'ZIFII1008_IDOCFI'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Informacion Idoc Financieros'
define view ZIFII1008_1
  as select from    ZTFI1008_2 as log
    left outer join ZTFI1008_1 as k on  log.numero_idoc = k.numero_idoc
                                    and log.mestyp      = k.mestyp
{
  key log.numero_idoc,
  key log.mestyp,
      log.status,
      k.sociedad,
      k.belnr,
      k.gjahr,
      k.tipo_doc,
      k.bldat,
      k.budat,
      log.statyp,
      log.statxt,
      log.stapa1,
      log.stapa2,
      log.stapa3,
      log.stapa4,
      log.stamid,
      log.stamno,
      log.uname,
      log.logdat,
      log.logtim,
      k.segnam,
      log.countr
      //( p_numero_idoc: $parameters.p_numero_idoc )
}

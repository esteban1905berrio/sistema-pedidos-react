@AbapCatalog.sqlViewName: 'ZISDR1085_12'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Datos TM para una entrega'
define view ZISDR1085_ENTREGA_TRANSPORTE
  as select from    /scmtms/d_torrot as _transporte
    inner join      /scmtms/d_tordrf as _documento_referencia on _documento_referencia.parent_key = _transporte.db_key
    inner join      likp             as _entrega              on _entrega.vbeln = substring(
      _documento_referencia.btd_id, 26, 10
    )

    left outer join lfa1             as bp                    on bp.lifnr = _transporte.tspid
    left outer join ztcxr1000_2      as _parametros           on  _parametros.ricefw  = 'R1085'
                                                              and _parametros.idparam = 'EVENT_CODE'
    left outer join /scmtms/d_torexe as _ejecucionTransporte  on  _ejecucionTransporte.parent_key = _transporte.db_key
                                                              and _ejecucionTransporte.event_code = _parametros.low
{
  key _transporte.db_key,
  key _transporte.tor_id as numero_transporte,
  key _transporte.tor_cat,
  key _entrega.vbeln     as entrega,
      _transporte.tspid  as agente_servicio,
      name1              as desc_agente_servicio,
      max( _ejecucionTransporte.actual_date ) as fecha_actual_transporte, //Debe tomar la ultima fecha
      max( _ejecucionTransporte.created_on ) as fecha_transporte //Debe tomar la ultima fecha
}
where
  _transporte.tor_cat = 'TO'
group by _transporte.db_key, tor_id, tor_cat, _entrega.vbeln, _transporte.tspid, name1

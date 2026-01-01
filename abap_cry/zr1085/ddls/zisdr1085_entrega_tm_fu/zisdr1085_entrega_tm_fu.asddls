@AbapCatalog.sqlViewName: 'ZISDR1085_8'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Valor FU de la entrega TM'
define view ZISDR1085_ENTREGA_TM_FU
  as
  //  //Entregas EWM
  //  select from  /scmtms/d_torrot as _transporteFU
  //    inner join /scdl/db_proci_o as _entregaEWM           on _transporteFU.labeltxt = ltrim(
  //      _entregaEWM.docno, '0'
  //    )
  //    inner join /scdl/db_refdoc  as _entregaEWMReferencia on _entregaEWMReferencia.docid = _entregaEWM.docid
  //    inner join likp             as _entregaERP_desde_EWM on _entregaERP_desde_EWM.vbeln = _entregaEWMReferencia.refdocno
  //
  //
  //{
  //      //key _transporteFU.db_key,
  //  key max( _transporteFU.tor_id)     as FU_TM,
  //  key _transporteFU.labeltxt         as Etiqueta,
  //      _entregaEWMReferencia.refdocno as entrega_erp,
  //      _entregaEWM.docno              as entrega_ewm
  //}
  //where
  //  _transporteFU.tor_cat = 'FU'
  //group by
  //  _transporteFU.labeltxt,
  //  _entregaEWMReferencia.refdocno,
  //  _entregaEWM.docno

  //Entregas ERP
  //union
  //  select from       /scmtms/d_torrot as _transporteFU
  //    inner join      likp             as _entregaERP           on _transporteFU.labeltxt = ltrim(
  //      _entregaERP.vbeln, '0'
  //    )

  select from       likp             as _entregaERP
  --Tabla de referencia de las entregas en TM
    inner join      /scmtms/d_tordrf as _transporteReferencia on _transporteReferencia.btd_id = concat(
      '0000000000000000000000000', _entregaERP.vbeln
    )
  --Tabla de los numeros de transporte
    inner join      /scmtms/d_torrot as _transporteFU         on _transporteFU.db_key = _transporteReferencia.parent_key

  --Tabla de referencia con las entregas de EWM
    left outer join /scdl/db_refdoc  as _entregaEWMReferencia on  _entregaEWMReferencia.refdoccat = 'ERP'
                                                              and _entregaEWMReferencia.doccat    = 'PDO'
                                                              and _entregaEWMReferencia.refitemno = '0000000000'
                                                              and _entregaEWMReferencia.refdocno  = _entregaERP.vbeln
  --Tabla de cabecera de las entregas de EWM
    left outer join /scdl/db_proch_o as _entregaEWM           on _entregaEWM.docid = _entregaEWMReferencia.docid

{
      //key _transporteFU.db_key,
  key max( _transporteFU.tor_id) as FU_TM,
  key _transporteFU.labeltxt     as Etiqueta,
      _entregaERP.vbeln          as entrega_erp,
      _entregaEWM.docno          as entrega_ewm
}
where
  _transporteFU.tor_cat = 'FU'
group by
  _transporteFU.labeltxt,
  _entregaERP.vbeln,
  _entregaEWM.docno

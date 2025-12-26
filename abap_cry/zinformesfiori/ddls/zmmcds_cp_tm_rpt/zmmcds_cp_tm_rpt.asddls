@AbapCatalog.sqlViewName: 'ZMMCDS_CP_TM_V1'
@AbapCatalog.viewEnhancementCategory: [ #NONE ]
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'CDS Consulta Informacion Transportista'
@Metadata.ignorePropagatedAnnotations: false
@ObjectModel.usageType: {
  serviceQuality: #X,
  sizeCategory: #S,
  dataClass: #MIXED
}

@VDM.viewType: #COMPOSITE
define view zmmcds_cp_tm_rpt
  as select distinct from ZISDR1085_ENTREGA_TM_FU      as t1
    left outer join       ZISDR1085_ENTREGA_TRANSPORTE as t2 on t1.entrega_erp = t2.entrega
    left outer join       likp                         as t3 on t3.vbeln = t2.entrega
    left outer join       vepo                         as t4 on t4.vbeln = t2.entrega
    left outer join       vekp                         as t5 on t4.venum = t5.venum
  --left outer join       makt                         as t6 on  t6.matnr = t5.vhilm
  --                                                       and t6.spras = $session.system_language
{
  
  key right( t2.numero_transporte, 10 ) as transporte,
  key t1.entrega_erp                    as entrega,
  key t5.exidv                          as ump,
  --    t2.agente_servicio                as cod_agente,
      t2.desc_agente_servicio           as agente
}

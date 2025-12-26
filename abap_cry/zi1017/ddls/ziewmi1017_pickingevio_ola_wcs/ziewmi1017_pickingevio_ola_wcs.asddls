@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Picking 2 Etapas datos envio Olas a WCS'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define view entity ZIEWMI1017_PICKINGEVIO_OLA_WCS
  as select from    /scdl/db_proci_o as es
    inner join      /scdl/db_refdoc  as r   on  r.docid     = es.docid
                                            and r.itemid    = es.itemid
                                            and r.refdoccat = 'ERP'
    inner join      vbss             as dpc on dpc.vbeln = r.refdocno
    inner join      mean             as me  on me.matnr = es.productno
    left outer join marm             as um  on  um.matnr = es.productno
                                            and um.meinh = 'BAG' --Unidad de medida BOL
    left outer join mcha             as mc  on  mc.matnr = es.productno
                                            and mc.charg = es.batchno
    left outer join /scwm/tmapwhnum  as a   on a.whnumwme = es./scwm/whno

{

  key    es.docid,
  key    es.itemid,
  key    es.docno   as entrega_salida,
  key    es.itemno,
         es.productno,
         me.ean11,
         um.umrez   as UNxPaquete,
         es.uom     as unidadmedida,
         @Semantics.quantity.unitOfMeasure: 'unidadmedida'
         es.qty     as cantidad,
         es./scwm/whno,
         r.refdocno as entrega_ewm,
         dpc.sammg  as ola,
         a.whnumerp as lgort,
         es.batchno,
         es.status_pick,
         mc.sgt_scat

}

@AbapCatalog.sqlViewName: 'ZMMCDS_CM_TM_V2'
@AbapCatalog.viewEnhancementCategory: [ #NONE ]
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'CDS Consulta Informacion Transportista'
@Metadata.ignorePropagatedAnnotations: false
@ObjectModel.usageType: {
  serviceQuality: #X,
  sizeCategory: #S,
  dataClass: #MIXED
}

@Metadata.allowExtensions: true
@VDM.viewType: #CONSUMPTION
@ObjectModel.semanticKey: [ 'ump' ]
define view zmmcds_cm_tm_rpt
  as select from zmmcds_cp_tm_rpt
{
  @EndUserText.label: 'No. Transporte'
  key transporte,
  @EndUserText.label: 'No. Entrega'
  key entrega,
  @EndUserText.label: 'No. Caja'
  key ump,
  @EndUserText.label: 'Transportista'
      agente
}

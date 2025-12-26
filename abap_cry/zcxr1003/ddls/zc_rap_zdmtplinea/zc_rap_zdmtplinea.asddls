@AbapCatalog.sqlViewName: 'ZCVIEWZDMTPLINEA'
@AbapCatalog.compiler.compareFilter: true
//@AccessControl.authorizationCheck: #NOT_REQUIRED
//@EndUserText.label: 'Discontinuation Indicator Value Help'
//@ClientHandling.algorithm: #SESSION_VARIABLE 
//@VDM.viewType: #CONSUMPTION
//@ObjectModel.dataCategory: #TEXT
//@ObjectModel.representativeKey: 'zzDiscontinuationInd'
//@ObjectModel.usageType.serviceQuality: #C
//@ObjectModel.usageType.sizeCategory: #S
//@ObjectModel.usageType.dataClass: #CUSTOMIZING
define view ZC_RAP_ZDMTPLINEA 
as select from I_DomainFixedValue
{
  @ObjectModel.text.element:  [ 'DomainText' ]    
  key cast ( DomainValue as zedtplinea ) as ztipolinea,

  @Semantics.text: true
  _DomainFixedValueText[1: Language = $session.system_language].DomainText as DomainText    
}
 where SAPDataDictionaryDomain    = 'ZDMTPLINEA'

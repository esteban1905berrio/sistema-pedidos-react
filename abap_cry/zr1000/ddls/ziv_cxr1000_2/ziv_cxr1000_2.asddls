@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Vista de rango de parametros'
//@Metadata.ignorePropagatedAnnotations: true
@Metadata.allowExtensions: true

//@UI: {
//  headerInfo: {
//    typeName: 'Rango',
//    typeNamePlural: 'Rangos',
//    title: { value: 'sign' },
//    description: { value: 'low' }
//  }
//}



define view entity ZIV_CXR1000_2 as select from ztcxr1000_2
association to parent ZIV_CXR1000_1 as _parent
    on $projection.modulo  = _parent.modulo
    and $projection.ricefw  = _parent.ricefw
    and $projection.idcomo  = _parent.idcomo
    and $projection.idparam = _parent.idparam
{
  
  @UI.facet: [
      {
        id: 'General',
        purpose: #STANDARD,
        type: #IDENTIFICATION_REFERENCE,
        label: 'Rango'
      }
   ]
   
  

  key modulo,
  key ricefw,
  key idcomo,
  key idparam,
  
  @UI.lineItem: [ { position: 10, label: 'Consecutivo' } ]
  @UI.identification: [ { position: 10, label: 'Consecutivo' } ]
  @EndUserText.label: 'Consecutivo'
  key consec,
  
  @UI.lineItem: [ { position: 15, label: 'sign' } ]
  @UI.identification: [ { position: 15, label: 'sign' } ]
  @EndUserText.label: 'sign'
      sign,
      
  @UI.lineItem: [ { position: 20, label: 'Option' } ]
  @UI.identification: [ { position: 20 , label: 'Option' } ]
  @EndUserText.label: 'Option'
      opti,
      
  @UI.lineItem: [ { position: 30, label: 'Low' } ]
  @UI.identification: [ { position: 30, label: 'Low'} ]
  @EndUserText.label: 'Low'
      low,
  
  @UI.lineItem: [ { position: 40, label: 'High' } ]
  @UI.identification: [ { position: 40, label: 'High' } ]
  @EndUserText.label: 'High'
      high,
      erdat,
      ernam,
      
  _parent
}

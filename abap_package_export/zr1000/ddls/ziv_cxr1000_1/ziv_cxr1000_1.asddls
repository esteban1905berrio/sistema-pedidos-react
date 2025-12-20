@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Vista de parametros'
@Metadata.ignorePropagatedAnnotations: true
@Metadata.allowExtensions: true

@UI: {
  headerInfo: {
    typeName: 'Parámetro',
    typeNamePlural: 'Parámetros',
    title:          { value: 'HeaderTitle' }, 
    description: { value: 'idparam' }
  }
}

define root view entity ZIV_CXR1000_1 as select from ztcxr1000_1
composition [0..*] of ZIV_CXR1000_2 as _rangos 
{   
  
  @UI.facet: [
  {
    id: 'General',
    purpose: #STANDARD,
    type: #IDENTIFICATION_REFERENCE,
    label: 'Cabecera'
  },
  {
    id: 'Rangos',
    purpose: #STANDARD,
    type: #LINEITEM_REFERENCE,
    targetElement: '_rangos',
    label: 'Rangos'
  }
]
  @UI.lineItem: [ { position: 10, label: 'Módulo' } ]
  @UI.identification: [ { position: 10 } ]
  @UI.selectionField: [{ position: 10 }]
  @Consumption.valueHelpDefinition: [{ entity:{
      name: 'ZIV_CXR1000_1',
      element: 'modulo'
  } }]
  key modulo,
  
  @UI.lineItem: [ { position: 20, label: 'Ricefw' } ]
  @UI.identification: [ { position: 20 } ]
  @UI.selectionField: [{ position: 20 }]
  @EndUserText.label: 'Ricefw'
  key ricefw,
  
  @UI.lineItem: [ { position: 30, label: 'Idcomo' } ]
  @UI.identification: [ { position: 30 } ]
  key idcomo ,
  
  @UI.lineItem: [ { position: 40, label: 'Idparam' } ]
  @UI.identification: [ { position: 40 } ]
  @UI.selectionField: [{ position: 30 }]
  @EndUserText.label: 'IdParam'
  key idparam,
  
  @UI.lineItem: [ { position: 50, label: 'Descripción' } ]
  @UI.identification: [ { position: 50 } ]
  @EndUserText.label: 'Descripción'
      descparam,
  
  @UI.lineItem: [ { position: 50, label: 'Fecha' } ]
  @UI.identification: [ { position: 50 } ]
      erdat,
      
//  @UI.lineItem: [ { position: 60, label: 'Criti' } ]
//  @UI.identification: [ { position: 60 } ]  
//
//  case
//    when erdat > '19990101' then 3
//    else 3
//  end as Criticality,
        
//  @UI.lineItem: [ { position: 70, label: 'Usuario', criticality: 'Criticality', criticalityRepresentation: #WITHOUT_ICON } ]
  @UI.lineItem: [ { position: 70, label: 'Usuario' } ]
  @UI.identification: [ { position: 70 } ]    
  
      ernam,
    
//      @UI.lineItem: [
//    {
//      
//      label: 'Positive (Dummy)',
//      dataAction: 'ButtonAction',
//      type: #FOR_ACTION,
//      criticalityRepresentation: #WITHOUT_ICON,
////      criticality: 'Criticality',
//      position: 80
//    }
//  ]
//  @UI.identification: [ { position: 80, label: 'Positive (Dummy)',
//      dataAction: 'ButtonAction' } ]
//      
  _rangos
}

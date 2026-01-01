@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Vista de consumo detalle orden de compra'
@Metadata.ignorePropagatedAnnotations: true
define view entity ZVCMMR1021_2
  as select from ZVMMR1021_2
  association to parent ZVCXMMR1021_1 as _cabecera on  $projection.sociedad = _cabecera.sociedad
                                                   and $projection.centro   = _cabecera.centro
                                                   and $projection.almacen  = _cabecera.almacen
                                                   and $projection.pedido   = _cabecera.pedido
{   
      @UI.facet: [
      {
        id: 'General',
        purpose: #STANDARD,
        type: #IDENTIFICATION_REFERENCE,
        label: 'Detalle'
      }
      ]
   
      @UI.lineItem: [ { position: 10, label: 'sociedad' } ]
      @UI.identification: [ { position: 10 } ]
  key ZVMMR1021_2.sociedad,
      @UI.lineItem: [ { position: 20, label: 'centro' } ]
      @UI.identification: [ { position: 20 } ]
  key ZVMMR1021_2.centro,
      @UI.lineItem: [ { position: 30, label: 'almacen' } ]
      @UI.identification: [ { position: 30 } ]
  key ZVMMR1021_2.almacen,

  key ZVMMR1021_2.pedido,
      @UI.lineItem: [ { position: 40, label: 'posicion' } ]
      @UI.identification: [ { position: 40 } ]
  key ZVMMR1021_2.posicion,

      @Semantics.quantity.unitOfMeasure: 'UMBase'
      @UI.lineItem: [ { position: 50, label: 'cantidad' } ]
      @UI.identification: [ { position: 50 } ]
      ZVMMR1021_2.cantidad,
      
      @UI.lineItem: [ { position: 60, label: 'cantConfirmada' } ]
      @UI.identification: [ { position: 60 } ]
      @Semantics.quantity.unitOfMeasure: 'UMBase'
      ZVMMR1021_2.cantConfirmada,

      ZVMMR1021_2.UMBase,

//      @UI: {
//      lineItem: [{
//         type: #AS_DATAPOINT,
//         importance: #HIGH,
//         position: 10
//      }],
//      dataPoint: {
//         title: 'Porcentaje cumplido',
//         valueFormat.numberOfFractionalDigits: 2,
//         minimumValue: 0,
//         maximumValue: 100,
//         criticalityCalculation: {
//             improvementDirection: #MAXIMIZE,
//             deviationRangeLowValue: 25,
//             toleranceRangeLowValue: 60
//         }
//      }
//      }
//
//      ZVMMR1021_2.Porcentajecumplido,
//
//
//      @UI.lineItem: [ { position: 70, label: 'PorcentajecumplidoItem' } ]
//      @UI.identification: [ { position: 70 } ]
//      ZVMMR1021_2.PorcentajecumplidoItem,


      _cabecera // Make association public
}

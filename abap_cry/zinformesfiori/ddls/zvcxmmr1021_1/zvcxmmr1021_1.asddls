@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Vista de consumo ZVMMR1021'
@Metadata.ignorePropagatedAnnotations: true
@Metadata.allowExtensions: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}

@UI: {
  headerInfo: {
    typeName: 'Orden de compra',
    typeNamePlural: 'Ordenes de compra',
    title.label: 'Reporte de ordenes de compra',
    description: { label: 'Pedido', value: 'pedido' }
  }
}

@UI.chart: [
    { 
      qualifier: 'chartDefault',
      chartType: #COLUMN,
      dimensions: ['centro'],
      measures: ['Porcentajecumplido'],
      title: 'Porcentaje Confirmación por Centro',
       measureAttributes: [{
        measure: 'porcentajecumplido',
        role: #AXIS_1
    }],
    dimensionAttributes: [{
        dimension: 'Centro',
        role: #SERIES
    }]
    }
  ]
  
  @UI.presentationVariant: [
    {
       qualifier: 'Default', visualizations: [{ type: #AS_CHART, qualifier: 'chartDefault' }]
     }
   ]

define root view entity ZVCXMMR1021_1
  as select from ZVMMR1021
  composition [0..*] of ZVCMMR1021_2   as _detalle
//  association [0..1] to ZVCMMR1021_KPI_CENTRO as _kpi on  ZVMMR1021.sociedad = _kpi.sociedad
//                                               and ZVMMR1021.centro   = _kpi.centro
{


      @UI.facet: [
       {
         id: 'General',
         purpose: #STANDARD,
         type: #IDENTIFICATION_REFERENCE,
         label: 'Cabecera'
       },
       {
          id: 'Detalle',
          purpose: #STANDARD,
          type: #LINEITEM_REFERENCE,
          targetElement: '_detalle',
          label: 'Detalle'
       }
       ]
       
       
        


      @UI.lineItem: [ { position: 10, label: 'Sociedad' } ]
      @UI.identification: [ { position: 10 } ]
      @UI.selectionField: [{ position: 10 }]
      @Consumption.filter: { selectionType: #RANGE, mandatory: true }
  key ZVMMR1021.sociedad,
      @UI.lineItem: [ { position: 20, label: 'Centro' } ]
      @UI.identification: [ { position: 20 } ]
      @UI.selectionField: [{ position: 20 }]
      @Consumption.filter: { selectionType: #RANGE, mandatory: true }

  key ZVMMR1021.centro,
  key ZVMMR1021.almacen,

      @UI.lineItem: [ { position: 30,
                        label: 'Pedido' } ]
      @UI.identification: [ { position: 30 } ]
      @UI.selectionField: [{ position: 30 }]
  key ZVMMR1021.pedido,
      
      @ObjectModel.virtualElement: true
      @ObjectModel.virtualElementCalculatedBy: 'ABAP:ZCLMMR1021_RAP'
      @UI: {
          
          lineItem: [{ type: #AS_DATAPOINT, importance: #HIGH, position: 5 }],
          dataPoint: {
              title: 'Porcentaje cumplido (centro)' ,
              valueFormat.numberOfFractionalDigits: 2,
              minimumValue: 0,
              maximumValue: 100,
              criticalityCalculation: {
                  improvementDirection: #MAXIMIZE,
                  deviationRangeLowValue: 25,
                  toleranceRangeLowValue: 60
              }
          }
      }
//      @EndUserText.label
//      _kpi.Porcentajecumplido,
      
     0.00 as Porcentajecumplido,

      _detalle

      //      ZVMMR1021.centroSum,
      //      ZVMMR1021.creadoPor,
      //      ZVMMR1021.claseDocumento,
      //      ZVMMR1021.fechaDocumento,
      //      ZVMMR1021.grupoCompras,
      //      ZVMMR1021.motivoPedido,
      //      ZVMMR1021.grupoArticulos,
      //      ZVMMR1021.txtGrupoArticulos,
      //      ZVMMR1021.acreedor,
      //      ZVMMR1021.nombreAcreedor,
      //      ZVMMR1021.condicionesPago,
      //      ZVMMR1021.ciudadOrigen,
      //      ZVMMR1021.incoterms1,
      //      ZVMMR1021.incoterms2,
      //      ZVMMR1021.moneda_cab,
      //
      //      @ObjectModel.virtualElement: true
      //      @ObjectModel.virtualElementCalculatedBy: 'ABAP:ZCLMMR1021_RAP'
      //       ZVMMR1021.tipoCambioMon,
      //      ZVMMR1021.descMoneda,
      //       @UI.lineItem: [ { position: 40, label: 'Centro' } ]
      //      @UI.identification: [ { position: 40 } ]
      //
      //      ZVMMR1021.posicion,
      //      ZVMMR1021.tipoPosicion,
      //      ZVMMR1021.tipoImputacion,
      //      ZVMMR1021.tipoMaterial,
      //      ZVMMR1021.material,
      //      ZVMMR1021.descMaterial,
      //      ZVMMR1021.talla,
      //      ZVMMR1021.color,
      //      ZVMMR1021.materialRes,
      //      ZVMMR1021.lote,
      //      ZVMMR1021.loteProveedor,
      //      ZVMMR1021.categValoracion,
      //
      //
      //      @Semantics.quantity.unitOfMeasure: 'UMBase'
      //      @UI.lineItem: [ { position: 50, label: 'cantidad' } ]
      //      @UI.identification: [ { position: 50 } ]
      //      ZVMMR1021.cantidad,
      //
      //
      //
      //
      //      ZVMMR1021.UMBase,
      //      ZVMMR1021.moneda_pos,
      //
      //
      //      @Semantics.amount.currencyCode: 'moneda_pos'
      //      ZVMMR1021.pr_ne_pos,
      //
      //      ZVMMR1021.vr_ne_pos,
      //      ZVMMR1021.vr_un_pos,
      //
      //      ZVMMR1021.vr_to_ped,
      //
      //      ZVMMR1021.vr_un_po_me,
      //
      //      ZVMMR1021.solicitudPedido,
      //      ZVMMR1021.solicitante,
      //      ZVMMR1021.notaDeEntrega,
      //      ZVMMR1021.fechaFactura,
      //      ZVMMR1021.usuario,
      //      ZVMMR1021.fechaDeEntrega,
      //      ZVMMR1021.docMaterial,
      //      ZVMMR1021.claseMov,
      //      ZVMMR1021.fecha_sm, //fecha entrada de mercancia
      //      ZVMMR1021.fecha_em, //fecha entrada de mercancia
      //      ZVMMR1021.cant_em,
      //
      //      ZVMMR1021.imp_ml,
      //
      //
      //      ZVMMR1021.im_un_me,
      //
      //      ZVMMR1021.vr_un_me,
      //
      //      ZVMMR1021.monedaLocal,
      //      ZVMMR1021.mon_rep,
      //      ZVMMR1021.referencia,
      //      ZVMMR1021.nroNecesidad,
      //      ZVMMR1021.ind_ent_fin,
      //
      //      @Semantics.quantity.unitOfMeasure: 'UMBase'
      //      ZVMMR1021.cantSalida,
      //      ZVMMR1021.ctaMayor,
      //      ZVMMR1021.centroCoste,
      //      ZVMMR1021.orden,
      //      ZVMMR1021.txt_material_2,
      //
      //      @UI.lineItem: [ { position: 60, label: 'cantConfirmada' } ]
      //      @UI.identification: [ { position: 60 } ]
      //      @Semantics.quantity.unitOfMeasure: 'UMBase'
      //      ZVMMR1021.cantConfirmada,
      //
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
      //      ZVMMR1021.Porcentajecumplido,
      //
      //
      //      @UI.lineItem: [ { position: 70, label: 'PorcentajecumplidoItem' } ]
      //      @UI.identification: [ { position: 70 } ]
      //      ZVMMR1021.PorcentajecumplidoItem,
      //
      //      ZVMMR1021.ean11,
      //      ZVMMR1021.satnr,
      //      ZVMMR1021.zz1_fesalfabric_pdi,
      //      ZVMMR1021.loekz,
      //      ZVMMR1021.tipoHistorialPedido,
      //      ZVMMR1021.entrega,
      //      ZVMMR1021.paq_ped,
      //      ZVMMR1021.paq_ent,
      //      ZVMMR1021.cant_pendiente,
      //      ZVMMR1021.status_ped,
      //
      //      ZVMMR1021.nroGuiaTranspZ,
      //
      //      ZVMMR1021.un_med_bolsa,
      //
      //
      //      ZVMMR1021.segnecesidad,
      //      ZVMMR1021.segstock,
      //
      //      ZVMMR1021.uname2,
      //      ZVMMR1021.udate                                                                                                               as udate,
      //      ZVMMR1021.utime                                                                                                               as utime,
      //
      //      ZVMMR1021.uname3,
      //      ZVMMR1021.udate2,
      //      ZVMMR1021.utime2,
      //
      //      ZVMMR1021.txtpecomtela,
      //
      //
      //      @UI.selectionField: [{ position: 10 }] //"se muestra en el Filter Bar
      //      ZVMMR1021.CheckVerifFactura, //se crea para realizar check de ver verificacion factura
      //
      //      @UI.selectionField: [{ position: 20 }] //"se muestra en el Filter Bar
      //      ZVMMR1021.CheckPedidosPenndientes //se crea para realizar check de solo pedidos pendientes

}

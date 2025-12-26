//@AbapCatalog.viewEnhancementCategory: #NONE
@EndUserText.label: 'KPI: % cumplimiento por centro'

define view entity ZVCMMR1021_KPI_CENTRO
  as select from ZVMMR1021_2
{
  key ZVMMR1021_2.sociedad,
  key ZVMMR1021_2.centro,

      cast( sum( ZVMMR1021_2.cantConfirmada ) as abap.dec(23,2) ) as SumCantConfirmada,
      cast( sum( ZVMMR1021_2.cantidad )as abap.dec(23,2) )       as SumCantidad,

      @UI: {
        lineItem: [{ type: #AS_DATAPOINT, importance: #HIGH, position: 10 }],
        dataPoint: {
          title: 'Porcentaje cumplido',
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

      cast(
      case
      when sum( ZVMMR1021_2.cantidad ) = 0 then cast( 0 as abap.dec(7,2) )
      else
        cast(
          division( (cast( sum( ZVMMR1021_2.cantConfirmada ) as abap.dec(23,2) )
            * cast( 100 as abap.dec(5,2) ) )
          , cast( sum( ZVMMR1021_2.cantidad ) as abap.dec(23,2) ), 2 )
        as abap.dec(7,2) )
      end
      as abap.dec(7,2))                 as Porcentajecumplido

}
group by
  ZVMMR1021_2.sociedad,
  ZVMMR1021_2.centro

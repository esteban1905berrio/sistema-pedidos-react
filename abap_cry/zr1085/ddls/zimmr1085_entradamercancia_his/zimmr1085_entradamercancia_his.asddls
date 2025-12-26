@AbapCatalog.sqlViewName: 'ZIMMR1085_3'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Historial entrada de mercancia'
define view ZIMMR1085_entradamercancia_his
  as select from ekbe
{
  key ekbe.ebeln        as Ebeln,
  key ekbe.ebelp        as Ebelp,
  key ekbe.zekkn        as Zekkn,
  key ekbe.vgabe        as Vgabe,
      max(ekbe.gjahr)   as Gjahr,//+ 03.01.2023 - valor maximo
      ekbe.bewtp        as Bewtp,
      ekbe.bwart        as Bwart,
      max(ekbe.budat)   as Budat,
      @Semantics.quantity.unitOfMeasure : 'ekpo.meins'
      sum( ekbe.menge ) as Menge,
      @Semantics.quantity.unitOfMeasure : 'ekpo.meins'
      sum( ekbe.bpmng ) as Bpmng
}
where
  bwart = '101'
group by
  ekbe.ebeln,
  ekbe.ebelp,
  ekbe.zekkn,
  ekbe.vgabe,
//  ekbe.gjahr, //- 03.01.2023 - Incluimos el maximo valor del ejercicio para evitar duplicidad de registros
  ekbe.bewtp,
  ekbe.bwart

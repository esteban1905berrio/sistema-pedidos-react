@AbapCatalog.sqlViewName: 'ZPSDR1085_6'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Inventario disponible por entrega'
define view ZPSDR1085_CANTIDAD_MAT_PEDIDO
  as select from lips
{
  key vgbel as pedido,
  key vgpos as posicion_pedido,
  key matnr,
  key werks,
  key lgort,
  key charg,
      vrkme,
      @Semantics.quantity.unitOfMeasure : 'lips.vrkme'
      sum( lfimg ) as cantidad_entrega
}
where
  wbsta <> 'C'
  and lfimg > 0
group by
  vgbel,
  vgpos,
  matnr,
  werks,
  lgort,
  charg,
  vrkme

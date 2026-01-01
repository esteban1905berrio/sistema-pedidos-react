@AbapCatalog.sqlViewName: 'ZISDR1085_6'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Asignacion stock'

define view ZISDR1085_ASIGNACION_STOCK
  as select distinct from arun_bdbs

{
  key    salesdoc_num     as pedido,
  key    salesdoc_item    as posicion_pedido,
  key    purchdoc_num     as pedido_traslado,
  key    purchdoc_item    as posicion_pedido_traslado,
         material_baseunit,
         @Semantics.quantity.unitOfMeasure : 'arun_bdbs.material_baseunit'
         sum( alloc_qty ) as cantidad_asignada
}
where
  (
       stock_source = 'C'
    or stock_source = 'S'
  )
  and  arun_status  = 'F'
group by
  salesdoc_num,
  salesdoc_item,
  purchdoc_num,
  purchdoc_item,
  material_baseunit

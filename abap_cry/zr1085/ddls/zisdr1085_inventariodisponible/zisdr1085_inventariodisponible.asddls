@AbapCatalog.sqlViewName: 'ZISDR1085_13'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Inventario disponible'
define view ZISDR1085_INVENTARIODISPONIBLE
  as select from    nsdm_e_mchb                    as _inventario
    left outer join ZPSDR1085_INVENTARIO_ENTREGA   as _inventarioEntrega        on  _inventarioEntrega.matnr = _inventario.matnr
                                                                                and _inventarioEntrega.werks = _inventario.werks
                                                                                and _inventarioEntrega.lgort = _inventario.lgort
                                                                                and _inventarioEntrega.charg = _inventario.charg
    left outer join ztcxr1000_2                    as _parametroExcluirSegmento on  _parametroExcluirSegmento.ricefw  =  'R1085'
                                                                                and _parametroExcluirSegmento.idparam =  'EXCL_SEGME'
                                                                                and _inventario.sgt_scat              <> _parametroExcluirSegmento.low

    left outer join ZISDR1085_ASIGNACIONSTOCK_INVE as _asignacionStock          on  _asignacionStock.plant            = _inventario.werks
                                                                                and _asignacionStock.material         = _inventario.matnr
                                                                                and _asignacionStock.storage_location = _inventario.lgort
                                                                                and _asignacionStock.batch            = _inventario.charg

{
  key _inventario.matnr,
  key _inventario.werks,
  key _inventario.lgort,
/*      @Semantics.quantity.unitOfMeasure : 'lips.vrkme'
      sum( _inventario.clabs ) as libre,
      @Semantics.quantity.unitOfMeasure : 'mara.meins'
      sum( _asignacionStock.cantidad_asignada ) as cantidad_asignada,
*/      
      @Semantics.quantity.unitOfMeasure : 'mara.meins'
      sum( _inventarioEntrega.cantidad_entrega ) as cantidad_entrega,
      @Semantics.quantity.unitOfMeasure : 'mara.meins'
      ( sum( _inventario.clabs ) - 
        sum( case when _asignacionStock.cantidad_asignada is null then 0 else _asignacionStock.cantidad_asignada end) - 
        sum( case when _inventarioEntrega.cantidad_entrega is null then 0 else _inventarioEntrega.cantidad_entrega end) ) as inventario_disponible
/*      (
       case
          when sum( _asignacionStock.cantidad_asignada ) is not null and sum( _inventarioEntrega.cantidad_entrega ) is not null then
            ( sum( _inventario.clabs ) - sum( _asignacionStock.cantidad_asignada ) - sum( _inventarioEntrega.cantidad_entrega ) )
          when sum( _asignacionStock.cantidad_asignada ) is not null then
            sum( _inventario.clabs ) - sum( _asignacionStock.cantidad_asignada )
          when sum( _inventarioEntrega.cantidad_entrega ) is not null then
            sum( _inventario.clabs )  - sum( _inventarioEntrega.cantidad_entrega )
          else
            sum( _inventario.clabs )
          end
      ) as inventario_disponible
*/
}
group by
  _inventario.matnr,
  _inventario.werks,
  _inventario.lgort
//  cantidad_entrega,
//  cantidad_asignada

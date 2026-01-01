@AbapCatalog.sqlViewName: 'ZISDR1085_22'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Inventario disponible'
define view ZISDR1085_INVENTARIODISPO_LOTE
  as select from    nsdm_v_mchb                    as _inventario
    left outer join ZPSDR1085_INVENTARIO_ENTREGA   as _inventarioEntrega        on  _inventarioEntrega.matnr = _inventario.matnr
                                                                                and _inventarioEntrega.werks = _inventario.werks
                                                                                and _inventarioEntrega.lgort = _inventario.lgort
                                                                                and _inventarioEntrega.charg = _inventario.charg
    left outer join ztcxr1000_2                    as _parametroExcluirSegmento on  _parametroExcluirSegmento.ricefw  =  'R1085'
                                                                                and _parametroExcluirSegmento.idparam =  'EXCL_SEGME'
                                                                                and _inventario.sgt_scat              <> _parametroExcluirSegmento.low

    left outer join ZISDR1085_ASIGNACIONSTOCK_INVE as _asignacionStock          on  _asignacionStock.plant            = _inventario.werks
                                                                                and _asignacionStock.material         = _inventario.matnr
                                                                                and _asignacionStock.batch            = _inventario.charg
                                                                                and _asignacionStock.storage_location = _inventario.lgort
{
  key _inventario.matnr,
  key _inventario.werks,
  key _inventario.lgort,
  key _inventario.charg,
      @Semantics.quantity.unitOfMeasure : 'lips.vrkme'
      _inventarioEntrega.cantidad_entrega,
      @Semantics.quantity.unitOfMeasure : 'mara.meins'
      (
       case
          when _asignacionStock.cantidad_asignada is not null and _inventarioEntrega.cantidad_entrega is not null then
            ( sum( _inventario.clabs ) - _asignacionStock.cantidad_asignada - _inventarioEntrega.cantidad_entrega )
          when _asignacionStock.cantidad_asignada is not null then
            sum( _inventario.clabs ) - _asignacionStock.cantidad_asignada
          when _inventarioEntrega.cantidad_entrega is not null then
            sum( _inventario.clabs )  - _inventarioEntrega.cantidad_entrega
          else
            sum( _inventario.clabs )
          end
      ) as inventario_disponible

}
group by
  _inventario.matnr,
  _inventario.werks,
  _inventario.lgort,
  _inventario.charg,
  cantidad_entrega,
  cantidad_asignada

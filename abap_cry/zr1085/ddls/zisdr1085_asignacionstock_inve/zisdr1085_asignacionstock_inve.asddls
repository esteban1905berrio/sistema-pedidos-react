@AbapCatalog.sqlViewName: 'ZISDR1085_23'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Asignacion stock para calculo de inventario'

define view ZISDR1085_ASIGNACIONSTOCK_INVE
  as select distinct from arun_bdbs   as _asignacionStock
    left outer join       ztcxr1000_2 as _parametroExcluirSegmento on  _parametroExcluirSegmento.ricefw  =  'R1085'
                                                                   and _parametroExcluirSegmento.idparam =  'EXCL_SEGME'
                                                                   and _asignacionStock.batch            <> _parametroExcluirSegmento.low
    left outer join       ztcxr1000_2 as _parametroARUN_STATUS     on  _parametroARUN_STATUS.ricefw  = 'R1085'
                                                                   and _parametroARUN_STATUS.idparam = 'ARUNSTATUS'
                                                                   and _asignacionStock.arun_status  = substring(
      _parametroARUN_STATUS.low, 1, 1
    )
    left outer join       ztcxr1000_2 as _parametroREQ_IND         on  _parametroREQ_IND.ricefw  = 'R1085'
                                                                   and _parametroREQ_IND.idparam = 'REQIND'
                                                                   and _parametroREQ_IND.low     = _asignacionStock.req_ind

{
  _asignacionStock.plant,
  _asignacionStock.material,
  _asignacionStock.storage_location,
  _asignacionStock.batch,
  material_baseunit,
  @Semantics.quantity.unitOfMeasure : 'arun_bdbs.material_baseunit'
  sum( alloc_qty ) as cantidad_asignada
}

group by
  plant,
  material,
  _asignacionStock.storage_location,
  batch,
  material_baseunit

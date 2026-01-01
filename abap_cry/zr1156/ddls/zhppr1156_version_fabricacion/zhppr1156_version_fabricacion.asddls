@AbapCatalog.sqlViewName: 'ZHPPR1156_1'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Consulta de versiones de fabricación'
define view ZHPPR1156_VERSION_FABRICACION
  as select from mkal as _version_fabricacion
    join         mara as _material on _material.matnr = _version_fabricacion.matnr
{
  werks as centro,
  _version_fabricacion.matnr,
  verid as version_fabr,
  text1 as descripcion,
  _material.meins,
  mksp
}
where mksp = ''
union select from ztppr1156_2
{
  centro,
  ''           as matnr,
  version_fabr,
  des_ver_fabr as descripcion,
  meins,
  ''           as mksp
}

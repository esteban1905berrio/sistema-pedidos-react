@AbapCatalog.sqlViewName: 'ZIMMR1085_2'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Interlocutores del pedido de ventas'
define view ZIMMR1085_INTERLOCUTORTRASLADO
  as select from    kna1  as cliente
    inner join      adrc  as direccion              on direccion.addrnumber = cliente.adrnr
    left outer join t005u as clave_provincia        on  clave_provincia.land1 = direccion.country
                                                    and clave_provincia.bland = direccion.region
                                                    and clave_provincia.spras = $session.system_language
{

  cliente.kunnr,
  cliente.adrnr,
  cliente.land1,
  cliente.name1,
  cliente.name2,
  cliente.ort01,
  cliente.pstlz,
  cliente.regio,
  cliente.sortl,
  cliente.stras,
  cliente.stcd1,
  cliente.telf1,
  cliente.telfx,
  cliente.xcpdk,
  cliente.katr7,
  direccion.date_from,
  direccion.nation,
  direccion.name3,
  direccion.name4,
  direccion.city1,
  direccion.post_code1,
  direccion.transpzone,
  direccion.street,
  direccion.str_suppl1,
  direccion.str_suppl2,
  direccion.str_suppl3,
  direccion.location,
  direccion.country,
  direccion.region,
  direccion.tel_number,
  direccion.mc_street,
  clave_provincia.bezei
}

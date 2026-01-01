@AbapCatalog.sqlViewName: 'ZISDR1085_1'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Interlocutores del pedido de ventas'
define view ZISDR1085_InterlocutoresPedido
  as select from    vbpa  as interlocutor
    inner join      kna1  as cliente                on cliente.kunnr = interlocutor.kunnr
    inner join      adrc  as direccion              on direccion.addrnumber = cliente.adrnr
    left outer join t005u as clave_provincia        on  clave_provincia.land1 = direccion.country
                                                    and clave_provincia.bland = direccion.region
                                                    and clave_provincia.spras = $session.system_language
    left outer join vbpa3 as id_fiscal_interlocutor on  id_fiscal_interlocutor.vbeln = interlocutor.vbeln
                                                    and id_fiscal_interlocutor.posnr = interlocutor.posnr
                                                    and id_fiscal_interlocutor.parvw = interlocutor.parvw
{
  key interlocutor.vbeln as pedido,
  key interlocutor.posnr,
  key interlocutor.parvw,
  key interlocutor.kunnr,
  interlocutor.adrnr,
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
where
  interlocutor.posnr = '000000'

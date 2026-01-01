@AbapCatalog.sqlViewName: 'ZISDR1085_19'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Interlocutor pedido venta - Solicitante'
define view ZISDR1085_INTERLO_RESP_PAGO
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
      cliente.regio,
      cliente.sortl,
      cliente.stcd1,
      cliente.telf1,
      direccion.city1,
      direccion.transpzone,
      direccion.street,
      direccion.location,
      direccion.country,
      direccion.region,
      direccion.mc_street,
      direccion.tel_number,
      clave_provincia.bezei
}
where
      interlocutor.posnr = '000000'
  and interlocutor.parvw = 'RG'

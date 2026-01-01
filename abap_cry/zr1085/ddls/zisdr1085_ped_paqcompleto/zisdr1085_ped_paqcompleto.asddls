@AbapCatalog.sqlViewName: 'ZISDR1085_17'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Pedidos paquete completo'
define view ZISDR1085_PED_PAQCOMPLETO
  as select from    ZPSDR1085_MATERIAL_GENERICO as _material
    left outer join knmt                        as _materialCliente on _materialCliente.matnr = _material.generico
    left outer join knvv                        as _maestroCliente  on  _maestroCliente.vkorg = _materialCliente.vkorg
                                                                    and _maestroCliente.vtweg = _materialCliente.vtweg
                                                                    and _maestroCliente.kunnr = _materialCliente.kunnr
  //    left outer join ZPSDR1085_MATALLACOLOR_CLIENTE as _caracteristicaColorTalla on  _caracteristicaColorTalla.Matnr         = _pedidoPosicion.matnr
  //                                                                                and _caracteristicaColorTalla.IdConvCliente = _maestroCliente.fsh_sc_cid
  //                                                                                and _caracteristicaColorTalla.Idioma        = $session.system_language

{
  key _materialCliente.vkorg                       as Vkorg,
  key _materialCliente.vtweg                       as Vtweg,
  key _materialCliente.kunnr                       as Kunnr,
  key _material.matnr,
      _materialCliente.ernam                       as Ernam,
      _materialCliente.erdat                       as Erdat,
      _materialCliente.sortl                       as Sortl,
      _materialCliente.kdmat                       as material_cliente,
      _materialCliente.postx                       as descripcion_mat_cliente,
      _materialCliente.lprio                       as Lprio,
      _materialCliente.minlf                       as Minlf,
      _materialCliente.meins                       as Meins,
      _materialCliente.chspl                       as Chspl,
      _materialCliente.kztlf                       as Kztlf,
      _materialCliente.antlf                       as Antlf,
      _materialCliente.untto                       as Untto,
      _materialCliente.uebto                       as Uebto,
      _materialCliente.uebtk                       as Uebtk,
      _materialCliente.werks                       as Werks,
      _materialCliente.rdprf                       as Rdprf,
      _materialCliente.megru                       as Megru,
      _materialCliente.j_1btxsdc                   as J1btxsdc,
      _materialCliente.vwpos                       as Vwpos,
      _materialCliente.vrkme_t                     as VrkmeT,
      _materialCliente.umvkn_t                     as UmvknT,
      _materialCliente.umvkz_t                     as UmvkzT,
      _materialCliente.guid                        as Guid,
      _materialCliente.dummy_slscusmat_incl_eew_ps as DummySlscusmatInclEewPs,
      _maestroCliente.fsh_sc_cid
      //      _caracteristicaColorTalla.IdConvCliente,
      //      _caracteristicaColorTalla.ColorCaracteristica    as color_cliente,
      //      _caracteristicaColorTalla.ColorDescripcionCaract as descripcion_color_cliente,
      //      _caracteristicaColorTalla.TallaCaracteristica    as talla_cliente,
      //      _caracteristicaColorTalla.TallaDescripcionCaract as descripcion_talla_cliente

}

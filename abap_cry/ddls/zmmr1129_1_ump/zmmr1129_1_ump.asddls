@AbapCatalog.sqlViewName: 'ZMMR1129_1_UMP'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Reporte UMP'
define view ZMMR1129_1_REP_UMP as 
    select from vekp
    left outer join vepo
          on vekp.venum = vepo.venum
    left outer join mara
          on vepo.matnr = mara.matnr
    left outer join makt
          on mara.matnr = makt.matnr
{
    vekp.exidv,
    vekp.venum,
    vekp.erlkz,
    vekp.lgnum,
    vekp.brgew,
    vekp.ntgew,
    vekp.gewei_max,
    vekp.tarag,
    vekp.gewei,
    vekp.laeng,
    vekp.breit,
    vekp.hoehe,
    vekp.meabm,
    vekp.btvol,
    vekp.voleh,
    vekp.voleh_max,
    vekp.spe_ident_01,
    vekp.spe_ident_02,
    vekp.spe_ident_03,
    vekp.vhilm,
    vekp.inhalt,
    vekp.vpobj,
    vekp.vpobjkey,
    vekp.erdat,
    
    vepo.sgt_scat,
    vepo.charg,
    vepo.vemng,
    vepo.vemeh,
    vepo.vbeln,
    vepo.posnr,
    vepo.sobkz,
    vepo.sonum,
    vepo.werks,
    vepo.lgort,
    
    mara.matnr,
    mara.color,
    mara.size1,
    mara.brand_id,
    
    makt.maktx,
    makt.spras  
}

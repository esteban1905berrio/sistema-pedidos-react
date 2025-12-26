@AbapCatalog.sqlViewName: 'ZVMMR1021_1'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'Vista sql 2'
@Metadata.ignorePropagatedAnnotations: true
define view  ZVMMR1021
  as select  distinct from ZPMMR1021_ConsultaPrincipal as ZPMMR1021
    left outer join ztcxr1000_2                 as parametro_exclusionDEBE_HABER on  parametro_exclusionDEBE_HABER.ricefw  = 'R1021'
                                                                                 and parametro_exclusionDEBE_HABER.idparam = 'SHKZG'
                                                                                 and ZPMMR1021.debeHaberRes                = parametro_exclusionDEBE_HABER.low
    left outer join ztcxr1000_2                 as parametro_exclusionmtart      on  parametro_exclusionmtart.ricefw  = 'R1021'
                                                                                 and parametro_exclusionmtart.idparam = 'MTART'
                                                                                 and ZPMMR1021.tipoMaterialRes        = parametro_exclusionmtart.low
    left outer join ztcxr1000_2                 as parametro_entradas            on  parametro_entradas.ricefw  = 'R1021'
                                                                                 and parametro_entradas.idparam = 'VGABE_ENT'
    left outer join ZPMMR1021_ConsultaPrincipal as ZPMMR1021_ent                 on  ZPMMR1021_ent.documento  = ZPMMR1021.documento
                                                                                 and ZPMMR1021_ent.ekbe_vgabe = parametro_entradas.low
    left outer join ztcxr1000_2                 as parametro_salidas             on  parametro_entradas.ricefw  = 'R1021'
                                                                                 and parametro_entradas.idparam = 'VGABE_SAL'
    left outer join ZPMMR1021_ConsultaPrincipal as ZPMMR1021_sal                 on  ZPMMR1021_sal.documento  = ZPMMR1021.documento
                                                                                 and ZPMMR1021_sal.ekbe_vgabe = parametro_salidas.low
//    left outer join ztcxr1000_2                 as parametro_cl_mov              on  parametro_entradas.ricefw  = 'R1021'
//                                                                                 and parametro_entradas.idparam = 'CL_MVTO'
//    left outer join ztcxr1000_2                 as parametro_tcode               on  parametro_entradas.ricefw  = 'R1021'
//                                                                                 and parametro_entradas.idparam = 'TCODELIB'
//    left outer join cdhdr                       as cdhdr                         on  cdhdr.objectclas = 'EINKBELEG'
//                                                                                 and cdhdr.objectid   = ZPMMR1021.documento
//                                                                                 and cdhdr.tcode      = parametro_tcode.low
{                                                                                 
     key ZPMMR1021.sociedad,
      key ZPMMR1021.centro,
      key ZPMMR1021.almacen,
    
      key ZPMMR1021.documento as pedido
      
      
}

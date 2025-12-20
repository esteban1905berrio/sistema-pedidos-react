@AbapCatalog.sqlViewName: 'ZVMMR1021_2C'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #NOT_REQUIRED
@EndUserText.label: 'vista sql'




define view  ZVMMR1021_2
  as select from ZPMMR1021_ConsultaPrincipal as ZPMMR1021
    left outer join ztcxr1000_2                 as parametro_exclusionDEBE_HABER on  parametro_exclusionDEBE_HABER.ricefw  = 'R1021'
                                                                                 and parametro_exclusionDEBE_HABER.idparam = 'SHKZG'
                                                                                 and ZPMMR1021.debeHaberRes                = parametro_exclusionDEBE_HABER.low
    left outer join ztcxr1000_2                 as parametro_exclusionmtart      on  parametro_exclusionmtart.ricefw  = 'R1021'
                                                                                 and parametro_exclusionmtart.idparam = 'MTART'
                                                                                 and ZPMMR1021.tipoMaterialRes        = parametro_exclusionmtart.low
    left outer join ztcxr1000_2                 as parametro_entradas            on  parametro_entradas.ricefw  = 'R1021'
                                                                                 and parametro_entradas.idparam = 'VGABE_ENT'
//    left outer join ZPMMR1021_ConsultaPrincipal as ZPMMR1021_ent                 on  ZPMMR1021_ent.documento  = ZPMMR1021.documento
//                                                                                 and ZPMMR1021_ent.ekbe_vgabe = parametro_entradas.low
    left outer join ztcxr1000_2                 as parametro_salidas             on  parametro_salidas.ricefw  = 'R1021'
                                                                                 and parametro_salidas.idparam = 'VGABE_SAL'
//    left outer join ZPMMR1021_ConsultaPrincipal as ZPMMR1021_sal                 on  ZPMMR1021_sal.documento  = ZPMMR1021.documento
//                                                                                 and ZPMMR1021_sal.ekbe_vgabe = parametro_salidas.low
    left outer join ztcxr1000_2                 as parametro_cl_mov              on  parametro_cl_mov.ricefw  = 'R1021'
                                                                                 and parametro_cl_mov.idparam = 'CL_MVTO'
    left outer join ztcxr1000_2                 as parametro_tcode               on  parametro_tcode.ricefw  = 'R1021'
                                                                                 and parametro_tcode.idparam = 'TCODELIB'
    left outer join cdhdr                       as cdhdr                         on  cdhdr.objectclas = 'EINKBELEG'
                                                                                 and cdhdr.objectid   = ZPMMR1021.documento
                                                                                 and cdhdr.tcode      = parametro_tcode.low
{



  key ZPMMR1021.sociedad,
  key ZPMMR1021.centro,
  key ZPMMR1021.almacen,

  key ZPMMR1021.documento                                                                                                       as pedido,
  
  key ZPMMR1021.posDocumento                                                                                                    as posicion,
      ZPMMR1021.centroSum,
      ZPMMR1021.creadoPor,
      ZPMMR1021.claseDocumento,
      ZPMMR1021.fechaDocumento,
      ZPMMR1021.grupoCompras,
      ZPMMR1021.motivoPedido,
      ZPMMR1021.grupoArticulos,
      ZPMMR1021.txtGrupoArticulos,
      ZPMMR1021.acreedor,
      ZPMMR1021.nombreAcreedor,
      ZPMMR1021.condicionesPago,
      ZPMMR1021.ciudadOrigen,
      ZPMMR1021.incoterms1,
      ZPMMR1021.incoterms2,
      ZPMMR1021.moneda                                                                                                          as moneda_cab,

      cast( ZPMMR1021.tipoCambioMon as abap.dec(9,5) )                                                                          as tipoCambioMon,
      ZPMMR1021.descMoneda,
      
      
      ZPMMR1021.tipoPosicion,
      ZPMMR1021.tipoImputacion,
      ZPMMR1021.tipoMaterial,
      ZPMMR1021.material,
      ZPMMR1021.descMaterial,
      ZPMMR1021.talla,
      ZPMMR1021.color,
      ZPMMR1021.materialRes,
      ZPMMR1021.lote,
      ZPMMR1021.loteProveedor,
      ZPMMR1021.categValoracion,
      ZPMMR1021.cantidad,




      ZPMMR1021.UMBase,
      ZPMMR1021.moneda                                                                                                          as moneda_pos,

      ZPMMR1021.precioNetoPedido                                                                                                as pr_ne_pos,

      case
       when ZPMMR1021.ekbe_shkzg = 'H'
        then cast( ZPMMR1021.valorNetoPosicion as abap.dec(23,2) ) *-1
       else cast( ZPMMR1021.valorNetoPosicion as abap.dec(23,2) )
      end                                                                                                                       as vr_ne_pos,


      case
        when ZPMMR1021.ekbe_shkzg = 'H'
         then cast( ZPMMR1021.valorUnitPosicion as abap.dec(23,2) ) *-1
        else cast( ZPMMR1021.valorUnitPosicion as abap.dec(23,2) )
       end                                                                                                                      as vr_un_pos,

      case
       when ZPMMR1021.ekbe_shkzg = 'H'
        then cast( ZPMMR1021.valorUnitPosicion as abap.dec(23,2) ) *-1
       else cast( ZPMMR1021.valorUnitPosicion as abap.dec(23,2) )
      end                                                                                                                       as vr_to_ped,

      case
      when ZPMMR1021.ekbe_shkzg = 'H'
       then ZPMMR1021.valorUnitGastosME  *-1
      else ZPMMR1021.valorUnitGastosME
      end                                                                                                                       as vr_un_po_me,

      ZPMMR1021.solicitudPedido,
      ZPMMR1021.solicitante,
      ZPMMR1021.notaDeEntrega,
      ZPMMR1021.fechaFactura,
      ZPMMR1021.creadoPor                                                                                                       as usuario,
      ZPMMR1021.fechaDeEntrega,
      ZPMMR1021.docMaterial,
      ZPMMR1021.claseMov,
      
      
      case
      when ZPMMR1021.ekbe_vgabe = parametro_entradas.low
       then ZPMMR1021.fechaContab 
      else ''
      end as fecha_em,
      
      case
      when ZPMMR1021.ekbe_vgabe = parametro_salidas.low
       then ZPMMR1021.fechaContab 
        else ''
      end as fecha_sm,
      
       
//      ZPMMR1021_sal.fechaContab                                                                                                 as fecha_sm, //fecha entrada de mercancia
//      ZPMMR1021_ent.fechaContab                                                                                                 as fecha_em, //fecha entrada de mercancia




      case
      when ZPMMR1021.ekbe_shkzg = 'H'
       then cast( ZPMMR1021.ekbe_bpmng as abap.dec(23,2) )  *-1
      else cast( ZPMMR1021.ekbe_bpmng as abap.dec(23,2) )
      end                                                                                                                       as cant_em,


      case
      when ZPMMR1021.ekbe_shkzg = 'H'
       then
        case when ZPMMR1021.claseMov = parametro_cl_mov.low or ZPMMR1021.claseMov = parametro_cl_mov.high
             then cast( ZPMMR1021.importeML as abap.dec(23,2) )
            else cast( ZPMMR1021.importeML as abap.dec(23,2) ) * -1
         end
      else cast( ZPMMR1021.importeML as abap.dec(23,2) )
      end                                                                                                                       as imp_ml,

      //      case
      //      when ZPMMR1021.ekbe_shkzg = 'H'
      //       then cast( ZPMMR1021.importeML as abap.dec(23,2) ) *-1
      //      else cast( ZPMMR1021.importeML as abap.dec(23,2) )
      //      end                       as imp_ml,

      case
       when ZPMMR1021.ekbe_shkzg = 'H'
        then cast( ZPMMR1021.importeUnitME as abap.dec(23,2) ) *-1
       else cast( ZPMMR1021.importeUnitME as abap.dec(23,2) )
      end                                                                                                                       as im_un_me,

      case
       when ZPMMR1021.ekbe_shkzg = 'H'
        then ZPMMR1021.valorUnitME  *-1
       else  ZPMMR1021.valorUnitME
      end                                                                                                                       as vr_un_me,

      ZPMMR1021.monedaLocal,
      ZPMMR1021.moneda                                                                                                          as mon_rep,
      ZPMMR1021.referencia,
      ZPMMR1021.nroNecesidad,
      ZPMMR1021.indEntregaComp                                                                                                  as ind_ent_fin,
      ZPMMR1021.cantSalida,
      ZPMMR1021.ctaMayor,
      ZPMMR1021.centroCoste,
      ZPMMR1021.orden,
      ZPMMR1021.descMaterial                                                                                                    as txt_material_2,

      ZPMMR1021.cantConfirmada,

//      case
//      when  ZPMMR1021.cantidad <> 0
//      then
//      division(cast ( ZPMMR1021.cantConfirmada  as abap.dec(15,2) ) * 100 ,  cast( ZPMMR1021.cantidad  as abap.dec(15,2) ) , 2) 
//      //      division( ( cast( ZPMMR1021.cantidad  as abap.dec(15,2) ) * cast ( ZPMMR1021.cantConfirmada  as abap.dec(15,2) ) , 100 ) as Porcentajecumplido,
//      else 0
//      end as Porcentajecumplido,
//      
//      case
//      when  ZPMMR1021.cantidad <> 0
//      then
//      division(cast ( ZPMMR1021.cantConfirmada  as abap.dec(15,2) ) * 100 ,  cast( ZPMMR1021.cantidad  as abap.dec(15,2) ) , 2) 
//      else 0
//      end as PorcentajecumplidoItem,

      ZPMMR1021.ean11,
      ZPMMR1021.satnr,
      ZPMMR1021.zz1_fesalfabric_pdi,
      ZPMMR1021.loekz,
      ZPMMR1021.tipoHistorialPedido,
      ZPMMR1021.entrega,

      case
       when ZPMMR1021.entrega <> ''
       then ZPMMR1021.paqPedido
       else 0
      end                                                                                                                       as paq_ped,

      case
       when ZPMMR1021.entrega <> ''
       then ZPMMR1021.paqEntrega
       else 0
      end                                                                                                                       as paq_ent,

      case
       when ZPMMR1021.ekbe_shkzg = 'H'
        then  ZPMMR1021.cantPendiente *-1
       else ZPMMR1021.cantPendiente
      end                                                                                                                       as cant_pendiente,

      case
       when ZPMMR1021.indEntregaComp = 'X' or ZPMMR1021.cantPendiente <= 0
         then 'Concluido'
        else 'Pendiente'
       end                                                                                                                      as status_ped,

      ZPMMR1021.nroGuiaTranspZ,

      case
       when ZPMMR1021.entrega <> ''
        then ZPMMR1021.UMBolsa
       else ''
      end                                                                                                                       as un_med_bolsa,


      ZPMMR1021.segNecesidad                                                                                                    as segnecesidad,
      ZPMMR1021.segStock                                                                                                        as segstock,

      cdhdr.username                                                                                                            as uname2,
      cdhdr.udate                                                                                                               as udate,
      cdhdr.utime                                                                                                               as utime,

      cdhdr.username                                                                                                            as uname3,
      cdhdr.udate                                                                                                               as udate2,
      cdhdr.utime                                                                                                               as utime2,

      ''                                                                                                                        as txtpecomtela,


      cast(
         case
           when ZPMMR1021.tipoHistorialPedido <> ' ' and ZPMMR1021.claseMov = ' '
             then 'X'
           else ' '
         end
       as bool)                                                                                                                 as CheckVerifFactura, //se crea para realizar check de ver verificacion factura

      cast(
         case
           when ZPMMR1021.cantPendiente  <= 0
             then 'X'
           else ' '
         end
       as bool)                                                                                                                 as CheckPedidosPenndientes //se crea para realizar check de solo pedidos pendientes


} 

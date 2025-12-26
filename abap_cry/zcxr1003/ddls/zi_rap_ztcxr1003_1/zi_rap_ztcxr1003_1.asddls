@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Maestro de Etiquetas SATO'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define root view entity ZI_RAP_ZTCXR1003_1 as select from ztcxr1003_1 as Etiquetas
composition [0..*] of ZI_RAP_ZTCXR1003_3 as _ZTCXR1003_3
composition [0..*] of ZI_RAP_ZTCXR1003_6 as _ZTCXR1003_6
composition [0..*] of ZI_RAP_ZTCXR1003_5 as _ZTCXR1003_5
composition [0..*] of ZI_RAP_ZTCXR1003_7 as _ZTCXR1003_7
composition [0..*] of ZI_RAP_ZTCXR1003_9 as _ZTCXR1003_9
 {
    key etiqueta_uuid as EtiquetaUuid,
    etiqueta_id as EtiquetaId,
    ds_etiqueta as DsEtiqueta,
    dinamica as Dinamica,
    copias as Copias,
    archivo as Archivo,
    clase as Clase,
    num_det as NumDet,
    alto as Alto,
    comienzo as Comienzo,
    op_interlocutor as OpInterlocutor,
    archivo2 as Archivo2,
    prog_extract as ProgExtract,
    ftp as Ftp,
//    'https://i7.pngguru.com/preview/423/632/57/computer-icons-purchase-order-order-fulfillment-purchasing-order-icon.jpg' as Url,
    @Semantics.user.createdBy: true
    created_by as CreatedBy,
    @Semantics.systemDateTime.createdAt: true
    created_at as CreatedAt,
    @Semantics.user.lastChangedBy: true
    last_change_by as LastChangeBy,
    @Semantics.systemDateTime.lastChangedAt: true
    last_change_at as LastChangeAt,
    @Semantics.systemDateTime.localInstanceLastChangedAt: true
    local_last_changed_at as LocalLastChangedAt,
         
    /* Associations */
    _ZTCXR1003_3,
    _ZTCXR1003_6,
    _ZTCXR1003_5,
    _ZTCXR1003_7,
    _ZTCXR1003_9
    
}

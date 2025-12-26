@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Campos para impresión SATO'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define view entity ZI_RAP_ZTCXR1003_3 as select from ztcxr1003_3 as Campos
   association to parent ZI_RAP_ZTCXR1003_1 as _ZTCXR1003_1 on $projection.EtiquetaUuid = _ZTCXR1003_1.EtiquetaUuid
   association [0..1] to ZI_RAP_ZTCXR1003_2 as _ZTCXR1003_2 on $projection.CampoId      = _ZTCXR1003_2.CampoId
 {
    key campo_uuid as CampoUuid,
    etiqueta_uuid as EtiquetaUuid,
    campo_id as CampoId,
    reg_det as RegDet,
    reg_total as RegTotal,
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
    _ZTCXR1003_1,
    _ZTCXR1003_2
    
    
}

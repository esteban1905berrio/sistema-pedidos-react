@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Usuarios para impresora'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define view entity ZI_RAP_ZTCXR1003_5 as select from ztcxr1003_5 as Usuarios
   association to parent ZI_RAP_ZTCXR1003_1 as _ZTCXR1003_1 on $projection.EtiquetaUuid = _ZTCXR1003_1.EtiquetaUuid
   association [0..1] to ZI_RAP_ZTCXR1003_4 as _ZTCXR1003_4 on $projection.ImpresoraId  = _ZTCXR1003_4.ImpresoraId

 {
    key usuario_uuid as UsuarioUuid,
    etiqueta_uuid as EtiquetaUuid,
    usuario_id as UsuarioId,
    impresora_id as ImpresoraId,
    campo_local as CampoLocal,
    imp_local_id as ImpLocalId,
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
    _ZTCXR1003_4
    
}

@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Usuarios para impresora'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define root view entity ZI_RAP_ZTCXR1003_5N as select from ztcxr1003_5 as Usuarios {
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
    local_last_changed_at as LocalLastChangedAt
}

@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Campos Etiqueta'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define root view entity ZI_RAP_ZTCXR1003_2 as select from ztcxr1003_2 as CamposEt {
    key campo_uuid as CampoUuid,
    campo_id as CampoId,
    ds_campo as DsCampo,
    tabla as Tabla,
    campo as Campo,
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

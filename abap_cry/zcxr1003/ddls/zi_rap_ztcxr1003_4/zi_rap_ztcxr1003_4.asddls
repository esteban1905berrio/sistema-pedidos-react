@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Impresora Etiquetas SATO'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define root view entity ZI_RAP_ZTCXR1003_4 as select from ztcxr1003_4 as Impresoras 
{
    key impresora_uuid as ImpresoraUuid,
    impresora_id as ImpresoraId,
    ds_impresora as DsImpresora,
    ubicacion as Ubicacion,
    ruta as Ruta,
    nomsap as Nomsap,
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

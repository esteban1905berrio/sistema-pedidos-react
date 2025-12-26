@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Impresión Dinámica'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define view entity ZI_RAP_ZTCXR1003_6 as select from ZTCXR1003_6 as EtiquetasDin 
association to parent ZI_RAP_ZTCXR1003_1 as _ZTCXR1003_1 on $projection.EtiquetaUuid = _ZTCXR1003_1.EtiquetaUuid

{
    key impdin_uuid as ImpdinUuid,
    etiqueta_uuid as EtiquetaUuid,
    tipo_linea as TipoLinea,
    linea as Linea,
    fijo as Fijo,
    dato as Dato,
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
    
    _ZTCXR1003_1
    
}

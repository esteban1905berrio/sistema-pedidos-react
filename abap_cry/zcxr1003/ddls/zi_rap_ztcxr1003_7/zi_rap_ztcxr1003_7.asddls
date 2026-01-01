@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Mensajes'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define view entity ZI_RAP_ZTCXR1003_7 as select from ztcxr1003_7 as Mensajes
  association to parent ZI_RAP_ZTCXR1003_1 as _ZTCXR1003_1 on $projection.EtiquetaUuid = _ZTCXR1003_1.EtiquetaUuid
 {
    key mensaje_uuid as MensajeUuid,
    etiqueta_uuid as EtiquetaUuid,
    mensaje_id as MensajeId,
    aplicacion as Aplicacion,
    accion_ppf as  AccionPpf,
    sel_etiqueta as SelEtiqueta,
    desc_mensaje as DescMensaje,
    num_copias as NumCopias,
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

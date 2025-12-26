@AbapCatalog.viewEnhancementCategory: [#NONE]
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Entidad Monitor Etiquetas'
@Metadata.ignorePropagatedAnnotations: true
@ObjectModel.usageType:{
    serviceQuality: #X,
    sizeCategory: #S,
    dataClass: #MIXED
}
define view entity ZI_RAP_ZTCXR1003_9 as select from ztcxr1003_9 as Monitor
  association to parent ZI_RAP_ZTCXR1003_1 as _ZTCXR1003_1 on $projection.EtiquetaUuid = _ZTCXR1003_1.EtiquetaUuid
 {

    key registro_uuid as RegistroUuid,
    etiqueta_uuid as EtiquetaUuid,
    consecutivo_id as ConsecutivoId,
    status as Status,
        case status
            when 'ERROR' then 1
        when 'SUCESS' then 3
        else 0
        end as Criticality, 
    transaccion as Transaccion,
    msj_json as MsjJson,
    msj_status as MsjStatus,
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


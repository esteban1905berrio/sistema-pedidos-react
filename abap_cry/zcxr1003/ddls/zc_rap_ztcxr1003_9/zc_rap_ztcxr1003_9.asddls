@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_9'
@AccessControl.authorizationCheck: #CHECK@Search.searchable: true
@Metadata.allowExtensions: true
define view entity ZC_RAP_ZTCXR1003_9 as projection on ZI_RAP_ZTCXR1003_9 {
    key RegistroUuid,
    EtiquetaUuid,
    ConsecutivoId,
    Status,
    @Search.defaultSearchElement: true
    Transaccion,
    MsjJson,
    MsjStatus,
    Criticality,
    @Search.defaultSearchElement: true
    CreatedBy,
    @Search.defaultSearchElement: true
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt,
    /* Associations */
    _ZTCXR1003_1: redirected to parent ZC_RAP_ZTCXR1003_1
}

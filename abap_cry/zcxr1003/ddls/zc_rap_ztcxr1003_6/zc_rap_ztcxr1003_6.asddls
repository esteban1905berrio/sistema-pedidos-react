@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_6'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define view entity ZC_RAP_ZTCXR1003_6 as projection on ZI_RAP_ZTCXR1003_6 as EtiquetasDin {
    key ImpdinUuid,
    @Search.defaultSearchElement: true
    EtiquetaUuid,
    TipoLinea,
    Linea,
    Fijo,
    Dato,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt,
    /* Associations */
    _ZTCXR1003_1: redirected to parent ZC_RAP_ZTCXR1003_1
}

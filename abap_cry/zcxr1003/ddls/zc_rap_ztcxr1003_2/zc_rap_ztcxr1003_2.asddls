@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_2'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define root view entity ZC_RAP_ZTCXR1003_2 as projection on ZI_RAP_ZTCXR1003_2 as CamposEt {
    key CampoUuid,
    @Search.defaultSearchElement: true
    CampoId,
    DsCampo,
    Tabla,
    Campo,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt
}

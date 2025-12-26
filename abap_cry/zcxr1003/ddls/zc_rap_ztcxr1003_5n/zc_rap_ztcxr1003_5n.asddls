@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_5N'
@AccessControl.authorizationCheck: #CHECK
define ROOT view entity ZC_RAP_ZTCXR1003_5N as projection on ZI_RAP_ZTCXR1003_2 {
    key CampoUuid,
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

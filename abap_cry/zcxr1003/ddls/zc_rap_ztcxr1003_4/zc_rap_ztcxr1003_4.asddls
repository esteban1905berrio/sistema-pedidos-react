@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_4'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define root view entity ZC_RAP_ZTCXR1003_4 as projection on ZI_RAP_ZTCXR1003_4 as Impresoras {
    key ImpresoraUuid,
    @Search.defaultSearchElement: true
    ImpresoraId,
    DsImpresora,
    Ubicacion,
    Ruta,
    Nomsap,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt
        
}

@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_5'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define view entity ZC_RAP_ZTCXR1003_5 as projection on ZI_RAP_ZTCXR1003_5 as Usuarios {
    key UsuarioUuid,
    EtiquetaUuid,
     @Search.defaultSearchElement: true
    UsuarioId,
    @Consumption.valueHelpDefinition: [{ entity: { name: 'ZI_RAP_ZTCXR1003_4', element: 'ImpresoraId'} }]
    @ObjectModel.text.element: ['DsImpresora']
    @Search.defaultSearchElement: true
    ImpresoraId,
    _ZTCXR1003_4.DsImpresora,
    CampoLocal,
    ImpLocalId,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt,
    /* Associations */
    _ZTCXR1003_1: redirected to parent ZC_RAP_ZTCXR1003_1,
    _ZTCXR1003_4
}

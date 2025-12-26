@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_3'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define view entity ZC_RAP_ZTCXR1003_3 as projection on ZI_RAP_ZTCXR1003_3 {
    key CampoUuid,
    EtiquetaUuid,
    @Consumption.valueHelpDefinition: [{ entity: { name: 'ZI_RAP_ZTCXR1003_2', element: 'CampoId'} }]
    @ObjectModel.text.element: ['DsCampo']
    @Search.defaultSearchElement: true
    CampoId,
    _ZTCXR1003_2.DsCampo,
    RegDet,
    RegTotal,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt,
    /* Associations */
    _ZTCXR1003_1: redirected to parent ZC_RAP_ZTCXR1003_1,
    _ZTCXR1003_2
}

@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_1'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define root view entity ZC_RAP_ZTCXR1003_1 as projection on ZI_RAP_ZTCXR1003_1 as Etiquetas {
    key EtiquetaUuid,
@Search.defaultSearchElement: true
    EtiquetaId,
    DsEtiqueta,
    Dinamica,
    Copias,
    Archivo,
    Clase,
    NumDet,
    Alto,
    Comienzo,
    OpInterlocutor,
    Archivo2,
    ProgExtract,
    Ftp,
    @ObjectModel.virtualElementCalculatedBy: 'ABAP:ZCLCXR1003_CUST_IMAGEN'
      virtual CustImageURL: abap.string( 256 ),
//    Url,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt,
    /* Associations */
    _ZTCXR1003_3: redirected to composition child ZC_RAP_ZTCXR1003_3,
    _ZTCXR1003_6: redirected to composition child ZC_RAP_ZTCXR1003_6,
    _ZTCXR1003_5: redirected to composition child ZC_RAP_ZTCXR1003_5,
    _ZTCXR1003_7: redirected to composition child ZC_RAP_ZTCXR1003_7,
    _ZTCXR1003_9: redirected to composition child ZC_RAP_ZTCXR1003_9
    
}

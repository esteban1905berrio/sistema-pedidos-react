@EndUserText.label: 'Projection ZI_RAP_ZTCXR1003_7'
@AccessControl.authorizationCheck: #CHECK
@Search.searchable: true
@Metadata.allowExtensions: true
define view entity ZC_RAP_ZTCXR1003_7 as projection on ZI_RAP_ZTCXR1003_7 as Mensajes {
    key MensajeUuid,
    EtiquetaUuid,
    @Search.defaultSearchElement: true
    MensajeId,
    Aplicacion,
    AccionPpf,
    SelEtiqueta,
    DescMensaje,
    NumCopias,
    CreatedBy,
    CreatedAt,
    LastChangeBy,
    LastChangeAt,
    LocalLastChangedAt,
    /* Associations */
    _ZTCXR1003_1: redirected to parent ZC_RAP_ZTCXR1003_1
}

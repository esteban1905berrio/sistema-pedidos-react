*&---------------------------------------------------------------------*
*& Include zmmr1022p_3
*&---------------------------------------------------------------------*

DATA: BEGIN OF ges_parametros_pantalla,
        iblnr TYPE ikpf-iblnr, "Documento para inventario
        gjahr TYPE ikpf-gjahr, "Ejercicio
        bldat TYPE ikpf-bldat, "Fecha de documento en documento
        werks TYPE iseg-werks, "Centro
        lgort TYPE iseg-lgort,  "Almacén
        matnr TYPE iseg-matnr,  "Material
      END OF ges_parametros_pantalla,

      go_reporte_ipis_tienda TYPE REF TO zclmmr1022_reporte_ipis_tienda.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.

  SELECT-OPTIONS: so_iblnr FOR ges_parametros_pantalla-iblnr,
                  so_gjahr FOR ges_parametros_pantalla-gjahr,
                  so_werks FOR ges_parametros_pantalla-werks,
                  so_lgort FOR ges_parametros_pantalla-lgort NO INTERVALS NO-EXTENSION,
                  so_matnr FOR ges_parametros_pantalla-matnr,
                  so_bldat FOR ges_parametros_pantalla-bldat.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-bk2.
  PARAMETERS: pa_layou TYPE disvariant-variant.
SELECTION-SCREEN END OF BLOCK bk2.
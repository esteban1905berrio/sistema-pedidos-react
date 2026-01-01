*&---------------------------------------------------------------------*
*& Include zfir1005p_2
*&---------------------------------------------------------------------*
TABLES: ztfir1005_5.

*&---------------------------------------------------------------------*
*& Declaraciones
*&---------------------------------------------------------------------*


*&---------------------------------------------------------------------*
*& Pantalla de seleccion
*&---------------------------------------------------------------------*
SELECTION-SCREEN BEGIN OF BLOCK lista WITH FRAME TITLE TEXT-001.

  SELECT-OPTIONS: so_bukrs FOR ztfir1005_5-bukrs,
                  so_gjahr FOR ztfir1005_5-gjahr,
                  so_belnr FOR ztfir1005_5-belnr,
                  so_hkont FOR ztfir1005_5-hkont,
                  so_monat FOR ztfir1005_5-monat,
                  so_abeln FOR ztfir1005_5-belnr_an,
                  so_estat FOR ztfir1005_5-estatus.

SELECTION-SCREEN END OF BLOCK lista.

PARAMETERS: pa_layou TYPE disvariant-variant.
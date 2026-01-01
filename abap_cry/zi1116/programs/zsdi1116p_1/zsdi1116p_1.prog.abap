*&---------------------------------------------------------------------*
*& Include zsdi1116pa_1
*&---------------------------------------------------------------------*
TABLES: mvke, mean.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-001.
  SELECT-OPTIONS:
       " Organización de ventas
       so_vkorg   FOR mvke-vkorg NO INTERVALS OBLIGATORY,
       " Canal de distribución
       so_vtweg   FOR mvke-vtweg NO INTERVALS OBLIGATORY,
       " Número de material
       so_matnr   FOR mvke-matnr,
       " Número de artículo europeo (EAN)
       so_ean11   FOR mean-ean11 MATCHCODE OBJECT zhsd_ean11 OBLIGATORY.

  PARAMETERS:
    "Fecha precio
    pa_fechp TYPE a002-datbi,
    pa_spras TYPE spras DEFAULT sy-langu.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN: BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-002.
  PARAMETERS: pa_categ AS CHECKBOX,
              pa_ddic  AS CHECKBOX,
              pa_atri  AS CHECKBOX,
              pa_prod  AS CHECKBOX,
              pa_impmt AS CHECKBOX.
SELECTION-SCREEN: END OF BLOCK bk2 .
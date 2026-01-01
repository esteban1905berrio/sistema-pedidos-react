*&---------------------------------------------------------------------*
*& Include zsdc1020p_1
*&---------------------------------------------------------------------*
TABLES: sscrfields.

DATA: go_carga_masiva_sup TYPE REF TO zclsdc1020_carga_masiva_sup.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.

  PARAMETERS: pa_narch TYPE ibipparms-path.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN FUNCTION KEY 1.
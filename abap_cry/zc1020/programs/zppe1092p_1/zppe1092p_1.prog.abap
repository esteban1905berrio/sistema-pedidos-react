*&---------------------------------------------------------------------*
*& Include zppe1092p_1
*&---------------------------------------------------------------------*
TABLES: sscrfields.

DATA: go_crea_of TYPE REF TO zclppe1092_cre_masiva_of.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.

  PARAMETERS: pa_narch TYPE ibipparms-path.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN FUNCTION KEY 1.
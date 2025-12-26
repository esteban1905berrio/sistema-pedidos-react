*&---------------------------------------------------------------------*
*& Include zfic1021p_1
*&---------------------------------------------------------------------*
TABLES: sscrfields.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-bk2.


  PARAMETERS: pa_bukrs  TYPE bukrs OBLIGATORY,
              pa_clnom  TYPE ztfic1021_2-clase_nomina OBLIGATORY,
              pa_perio  TYPE ztfic1021_1-periodo_nomina OBLIGATORY.

SELECTION-SCREEN END OF BLOCK bk2.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.


  PARAMETERS: pa_arch  TYPE localfile.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN FUNCTION KEY 1.
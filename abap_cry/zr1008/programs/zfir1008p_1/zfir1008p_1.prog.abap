*&---------------------------------------------------------------------*
*& Include zfir1008p_1
*&---------------------------------------------------------------------*
TABLES: kna1, acdoca, t003.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-tl1.
  PARAMETERS     : pa_bukrs LIKE t001-bukrs OBLIGATORY.
  PARAMETERS     : pa_bldat LIKE sy-datum OBLIGATORY.
  PARAMETERS     : pa_dias(4)  TYPE n OBLIGATORY DEFAULT '120'.
  SELECT-OPTIONS : so_kunnr FOR  kna1-kunnr MODIF ID wk1.
SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-tl2.
  PARAMETERS : pa_file  TYPE file_name OBLIGATORY DEFAULT 'c:\temp\datos.txt',
               pa_suscr TYPE char6 OBLIGATORY.
SELECTION-SCREEN END OF BLOCK bk2.
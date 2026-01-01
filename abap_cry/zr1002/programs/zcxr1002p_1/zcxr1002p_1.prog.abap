*&---------------------------------------------------------------------*
*& Include zcxr1002p_1
*&---------------------------------------------------------------------*
TABLES: edid4.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE  TEXT-001 .

  SELECT-OPTIONS: so_docnm FOR edid4-docnum OBLIGATORY,
                  so_nmseg FOR edid4-segnam OBLIGATORY,
                  so_numsg FOR edid4-segnum.

  PARAMETERS: pa_nmcp TYPE field_name OBLIGATORY,
              pa_vlcp TYPE char250.

SELECTION-SCREEN END OF BLOCK bk1.
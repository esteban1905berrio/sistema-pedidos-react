*&---------------------------------------------------------------------*
*& Include zfii1008p_4
*&---------------------------------------------------------------------*
TABLES: edidc.


SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-001.

  SELECT-OPTIONS: so_fecha FOR edidc-credat OBLIGATORY,
                  so_mesty FOR edidc-mestyp.
  PARAMETERS: pa_maxct LIKE syst-dbcnt DEFAULT 100000.

SELECTION-SCREEN END OF BLOCK bk1.
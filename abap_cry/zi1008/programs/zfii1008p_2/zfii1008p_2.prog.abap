*&---------------------------------------------------------------------*
*& Include zfii1008p_2
*&---------------------------------------------------------------------*

TABLES: edidc, edid4.

SELECTION-SCREEN BEGIN OF BLOCK bk1.

  SELECT-OPTIONS: so_docnm FOR edidc-docnum,
                  so_mestp FOR edidc-mestyp,
                  so_fecha FOR edidc-credat,
                  so_hora  FOR edidc-cretim,
                  so_segnm FOR edid4-segnam.

SELECTION-SCREEN END OF BLOCK bk1.
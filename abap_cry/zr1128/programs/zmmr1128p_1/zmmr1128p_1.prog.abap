*&---------------------------------------------------------------------*
*& Include zmmr1128p_1
*&---------------------------------------------------------------------*

TABLES: zfim11, likp,ekpo .

DATA: go_mensajes_ans_pkms TYPE REF TO zclmmr1128_mensajes_ans_pkms,
      go_log               TYPE REF TO zclcxr1002_log_aplicacion.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.

  SELECT-OPTIONS: so_bukrs FOR zfim11-bukrs NO INTERVALS NO-EXTENSION OBLIGATORY,
                  so_nimpo FOR zfim11-konnr NO INTERVALS NO-EXTENSION,
                  so_vbeln FOR likp-vbeln NO-EXTENSION,
                  so_lgort FOR ekpo-lgort NO INTERVALS NO-EXTENSION.

SELECTION-SCREEN END OF BLOCK bk1.
*&---------------------------------------------------------------------*
*& Include zfir1005p_1
*&---------------------------------------------------------------------*
TYPE-POOLS : icon.

TABLES glu1.

CLASS lcl_controlador DEFINITION DEFERRED.

DATA: ok_code_delta TYPE syucomm,
      ok_code       TYPE syucomm,
      go_controlador TYPE REF TO lcl_controlador.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-001.

  PARAMETERS: pa_bukrs    LIKE glu1-bukrs MEMORY ID buk OBLIGATORY,
              pa_ryear    LIKE glu1-ryear DEFAULT sy-datum(4) OBLIGATORY MEMORY ID gjr,
              pa_rldnr    LIKE glfunct-rldnr MATCHCODE OBJECT fins_ledger_all_w_filter OBLIGATORY DEFAULT '0L',
              pa_rldn2    LIKE glfunct-rldnr MATCHCODE OBJECT fins_ledger_all_w_filter OBLIGATORY,
              pa_rvers    LIKE glu1-rvers OBLIGATORY DEFAULT '001',
              pa_frper(3) OBLIGATORY DEFAULT sy-datum+4(2),
              pa_toper(3) OBLIGATORY DEFAULT sy-datum+4(2).

  SELECT-OPTIONS: so_racct FOR glu1-racct.

SELECTION-SCREEN END OF BLOCK bk1.


SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-002.

  PARAMETERS: pa_blart    TYPE bkpf-blart OBLIGATORY,
              pa_monat    TYPE monat OBLIGATORY DEFAULT sy-datum+4(2),
              pa_fecon    TYPE sy-datum OBLIGATORY DEFAULT sy-datum,
              pa_sbal     NO-DISPLAY,
              pa_sdif     AS CHECKBOX DEFAULT 'X'.

SELECTION-SCREEN END OF BLOCK bk2.

PARAMETERS: pa_layou TYPE disvariant-variant.
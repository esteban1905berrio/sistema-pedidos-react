*&---------------------------------------------------------------------*
*& Include zmmi1009p_2
*&---------------------------------------------------------------------*
TABLES: ztmmi1009_2.

DATA: go_proceso_mensajes TYPE REF TO zclmmi1009_procesa_msj_encola,
      gti_tipo_mensaje    TYPE zclmmi1009_procesa_msj_encola=>gtp_ti_tipo_mensaje.


SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-001.

  SELECT-OPTIONS: so_fecha FOR ztmmi1009_2-erdat,
                  so_matnr FOR ztmmi1009_2-matnr,
                  so_mtart FOR ztmmi1009_2-mtart.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-002.

  PARAMETERS: pa_cola  TYPE flag AS CHECKBOX DEFAULT abap_true,
              pa_error TYPE flag AS CHECKBOX DEFAULT abap_true.

SELECTION-SCREEN END OF BLOCK bk2.
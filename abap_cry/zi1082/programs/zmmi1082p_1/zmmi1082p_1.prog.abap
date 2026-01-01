*&---------------------------------------------------------------------*
*& Include zmmi1082p_1
*&---------------------------------------------------------------------*

TABLES: ztmmi1082_1, /sapapo/ev_usr, scal, tfacd.

DATA: ok_code       TYPE sy-ucomm,
      _ok_code      TYPE sy-ucomm,
      gti_ucomm     TYPE TABLE OF sy-ucomm,
      go_reposicion TYPE REF TO zclmmi1082_modifica_reposicion.

SELECTION-SCREEN BEGIN OF SCREEN 0101 AS SUBSCREEN.

  SELECT-OPTIONS: so_loc FOR ztmmi1082_1-localizacion,
                  so_dsm FOR /sapapo/ev_usr-m_wday,
                  so_cld FOR ztmmi1082_1-calendario,
                  so_tpl FOR ztmmi1082_1-tipo_localizacion.

SELECTION-SCREEN END OF SCREEN 0101.

SELECTION-SCREEN BEGIN OF SCREEN 0201 AS SUBSCREEN.

  SELECT-OPTIONS: so_locz FOR ztmmi1082_1-localizacion,
                  so_rtpl FOR ztmmi1082_1-tipo_localizacion.

  PARAMETERS: pa_rep  TYPE flag RADIOBUTTON GROUP gr1 USER-COMMAND rep DEFAULT 'X',
              pa_nrep TYPE flag RADIOBUTTON GROUP gr1.

SELECTION-SCREEN END OF SCREEN 0201.

SELECTION-SCREEN BEGIN OF SCREEN 0301 AS SUBSCREEN.

  PARAMETERS: pa_narch TYPE ibipparms-path.

SELECTION-SCREEN END OF SCREEN 0301.

SELECTION-SCREEN BEGIN OF SCREEN 0401 AS SUBSCREEN.

  SELECT-OPTIONS: so_calen FOR tfacd-ident OBLIGATORY,
                  so_cfech FOR scal-date NO-EXTENSION.

SELECTION-SCREEN END OF SCREEN 0401.
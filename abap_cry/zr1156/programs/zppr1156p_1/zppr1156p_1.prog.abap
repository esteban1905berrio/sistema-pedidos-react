*&---------------------------------------------------------------------*
*& Include zppr1156p_1
*&---------------------------------------------------------------------*
CLASS lcl_controlador DEFINITION DEFERRED.

TABLES: sscrfields.

DATA go_controlador TYPE REF TO lcl_controlador.

DATA:
  gv_centro       TYPE t001w-werks,
  gv_mat_generico TYPE mara-satnr,
  gv_version_fab  TYPE mkal-verid.

SELECTION-SCREEN: FUNCTION KEY 1.
SELECTION-SCREEN: FUNCTION KEY 2.

SELECTION-SCREEN BEGIN OF BLOCK b1 WITH FRAME TITLE TEXT-t01.

  PARAMETERS: pa_carga TYPE c RADIOBUTTON GROUP g1 USER-COMMAND rb  DEFAULT 'X',
              pa_actua TYPE c RADIOBUTTON GROUP g1.

SELECTION-SCREEN END OF BLOCK b1.

* Selección de documento
SELECTION-SCREEN BEGIN OF BLOCK b2 WITH FRAME TITLE TEXT-t02.

  PARAMETERS: pa_archi LIKE rlgrap-filename MODIF ID gp2.

SELECTION-SCREEN END OF BLOCK b2.


SELECTION-SCREEN BEGIN OF BLOCK b3 WITH FRAME TITLE TEXT-t03.

  SELECT-OPTIONS:
  so_centr FOR gv_centro MODIF ID gp1 OBLIGATORY,
  so_matge FOR gv_mat_generico MODIF ID gp1,
  so_vf    FOR gv_version_fab MODIF ID gp1 MATCHCODE OBJECT ZHPPR1156_VERSION_FABRICACION.

SELECTION-SCREEN END OF BLOCK b3.
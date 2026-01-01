*&---------------------------------------------------------------------*
*& Include zmmi1062p_1
*&---------------------------------------------------------------------*

TABLES: ztmmi1062_1, sscrfields.

DATA: okcode_n TYPE syucomm,
      ok_code      TYPE syucomm,
      icono_log  TYPE rsfunc_txt.


*-------------------------------------------------------------------------------*
* Pantalla de seleccion
*-------------------------------------------------------------------------------*
SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE text-010.
"Indica que se ejecuta en JOB programado para enviar pedidos con estado 9
SELECTION-SCREEN FUNCTION KEY 1.
SELECT-OPTIONS: so_cdlav FOR ztmmi1062_1-codlavado.

PARAMETERS: pa_dnlv TYPE ztmmi1062_1-denlavado.
SELECTION-SCREEN END OF BLOCK bk1.
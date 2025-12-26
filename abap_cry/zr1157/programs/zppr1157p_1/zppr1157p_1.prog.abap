*&---------------------------------------------------------------------*
*& Include zppr1157p_1
*&---------------------------------------------------------------------*
TABLES: ztppi1209_1.

DATA: go_proceo_infotint TYPE REF TO zclppr1157_productos_quimicos,
      gok_code_activo    TYPE syucomm,
      ok_code            TYPE syucomm.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-bk1.

  SELECT-OPTIONS: so_centr FOR ztppi1209_1-centro OBLIGATORY,
                  so_fenvi FOR ztppi1209_1-fecha_envio OBLIGATORY, "Fecha envio
                  so_mtleg FOR ztppi1209_1-material_generico, "Material generico legado
                  so_matnr FOR ztppi1209_1-material_generico, "Material generico SAP
                  so_color FOR ztppi1209_1-color,"Fecha fin Transporte
                  so_alter FOR ztppi1209_1-alternativa_escala,"Alternativa
                  so_indi FOR  ztppi1209_1-ind_actualizado NO INTERVALS NO-EXTENSION.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN BEGIN OF BLOCK bk3 WITH FRAME TITLE TEXT-bk3.

  PARAMETERS: pa_ppent TYPE flag AS CHECKBOX,
              pa_vidup TYPE flag AS CHECKBOX,
              pa_pajob TYPE flag NO-DISPLAY . "AS CHECKBOX

SELECTION-SCREEN END OF BLOCK bk3.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE TEXT-bk2.
  PARAMETERS: pa_layou TYPE disvariant-variant.
SELECTION-SCREEN END OF BLOCK bk2.
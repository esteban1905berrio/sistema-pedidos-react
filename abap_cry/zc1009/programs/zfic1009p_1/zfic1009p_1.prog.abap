*&---------------------------------------------------------------------*
*& Include zfic1009p_1
*&---------------------------------------------------------------------*


CONSTANTS: gcte_max_proc     TYPE i VALUE 1,
           gcte_id_sh_buffer TYPE char10 VALUE 'MID'.


CLASS lcl_carga_activo_fijo DEFINITION DEFERRED.

DATA: go_carga_activofijo  TYPE REF TO lcl_carga_activo_fijo,
      gti_log_creacion     TYPE zttfic1009_1,
      gti_log_icon         TYPE zclfic1009_carga_activos_fijos=>tp_ti_log,
      gti_datos_activofijo TYPE zttfic1009_2,
      g_proceso_act        TYPE i,
      g_procesos_term      TYPE i,
      g_procesos_enviados  TYPE i,
      g_procesos_recibidos TYPE i.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE TEXT-001.

  PARAMETERS: pa_arch  TYPE rlgrap-filename OBLIGATORY,
              pa_arch2 TYPE rlgrap-filename,
              pa_arch3 TYPE rlgrap-filename,
              pa_arch4 TYPE rlgrap-filename,
              pa_arch5 TYPE rlgrap-filename,
              pa_test  TYPE flag AS CHECKBOX,
              pa_fondo TYPE flag AS CHECKBOX,
              pa_debug TYPE flag AS CHECKBOX.

SELECTION-SCREEN END OF BLOCK bk1.
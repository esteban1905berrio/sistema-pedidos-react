*&---------------------------------------------------------------------*
*& Include zmmi1062cd_1
*&---------------------------------------------------------------------*
CLASS lcl_controlador DEFINITION INHERITING FROM zclmmi1062_instruccion_lavado.

  PUBLIC SECTION.

    CLASS-DATA: gti_ins_lavado    TYPE gtp_ti_instlavado,
                gti_catalog       TYPE lvc_t_fcat,
                go_alv            TYPE REF TO cl_gui_alv_grid,
                go_dock_container TYPE REF TO cl_gui_docking_container.

    CLASS-METHODS:
      iniciar_proceso
      IMPORTING
         i_r_codigo_lavado            LIKE so_cdlav[]
         value(i_denominacion_lavado) TYPE ztmmi1062_1-denlavado,
      mostrar_alv
        CHANGING
          c_ti_datos TYPE ANY TABLE,
      liberar_objetos_alv.

  PROTECTED SECTION.

  PRIVATE SECTION.

ENDCLASS.
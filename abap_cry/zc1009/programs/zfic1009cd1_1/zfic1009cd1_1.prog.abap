*&---------------------------------------------------------------------*
*& Include zfic1009cd1_1
*&---------------------------------------------------------------------*

CLASS lcl_carga_activo_fijo DEFINITION.

  PUBLIC SECTION.

    INTERFACES: zifcxr1002_alvgrid.
    ALIASES modificar_catalogo FOR zifcxr1002_alvgrid~modificar_catalogo.

    TYPES: gtp_ti_datos_temporales    TYPE STANDARD TABLE OF ztfic1009_2 WITH EMPTY KEY.

    DATA: nr_escribelog     TYPE i VALUE 2000,
          separador_archivo TYPE char01 VALUE cl_abap_char_utilities=>horizontal_tab.

    METHODS:
      iniciar_proceso
        IMPORTING
          i_arch               TYPE rlgrap-filename
          i_fondo              TYPE flag
          i_debug              TYPE flag
        CHANGING
          c_ti_dato_activofijo TYPE zttfic1009_2,
      constructor,
      eliminar_reg_temp_db
        IMPORTING
          i_arch TYPE rlgrap-filename,
      get_nombre_archivo
        IMPORTING
          i_arch        TYPE rlgrap-filename
        RETURNING
          VALUE(r_arch) TYPE string,
      conf_ejecucion_fondo
        IMPORTING
          i_fondo     TYPE flag
          i_narchivo  TYPE rlgrap-filename
          i_narchivo2 TYPE rlgrap-filename
          i_narchivo3 TYPE rlgrap-filename
          i_narchivo4 TYPE rlgrap-filename
          i_narchivo5 TYPE rlgrap-filename
          i_test      TYPE flag,
      ejecutar_proceso_en_paralelo
        IMPORTING
          i_debug             TYPE flag DEFAULT space
          i_ti_dat_activofijo TYPE zttfic1009_2
          i_test              TYPE testrun
          i_arch              TYPE rlgrap-filename.

    METHODS get_datos_db
      IMPORTING
        i_arch          TYPE rlgrap-filename
      RETURNING
        VALUE(r_ti_dat) TYPE gtp_ti_datos_temporales.

    METHODS cargar_datos
      IMPORTING
        i_arch  TYPE rlgrap-filename
        i_fondo TYPE flag.

    METHODS ejecucion_finalizada
      IMPORTING
        p_task TYPE clike .

    CLASS-METHODS:
      matchcode
        CHANGING
          c_arch TYPE rlgrap-filename,
      verificar_autorizacion.

    METHODS:
      hd_hotspot_click        FOR EVENT hotspot_click
        OF cl_gui_alv_grid
        IMPORTING e_row_id
                  e_column_id
                  es_row_no.

  PROTECTED SECTION.
    DATA: o_log TYPE REF TO zclcxr1002_log_aplicacion,
          o_cx  TYPE REF TO cx_root.
  PRIVATE SECTION.

    METHODS:
      consolidar_datos
        CHANGING
          c_ti_dato_activofijo TYPE zttfic1009_2
          c_ti_datos_archivo   TYPE zclcxr1002_cargar_archivo=>tp_ti_con_name
        RAISING
          cx_t100_msg.

    METHODS:
      validar_transformar_datos
        CHANGING c_ti_datos TYPE zttfic1009_2.

    METHODS ejecutar_en_fondo
      IMPORTING
*        i_ti_datos_cargados TYPE zttcx0001
        i_narchivo  TYPE rlgrap-filename
        i_narchivo2 TYPE rlgrap-filename
        i_narchivo3 TYPE rlgrap-filename
        i_narchivo4 TYPE rlgrap-filename
        i_narchivo5 TYPE rlgrap-filename
        i_test      TYPE flag.
    METHODS escribir_log_db
      IMPORTING
        i_arch            TYPE rlgrap-filename
      CHANGING
        c_ti_log_creacion TYPE zttfic1009_1.
    METHODS asignar_fecha_hora_log
      CHANGING
        c_ti_log_creacion TYPE zttfic1009_1.



ENDCLASS.
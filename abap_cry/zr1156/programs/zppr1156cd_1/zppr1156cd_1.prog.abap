*&---------------------------------------------------------------------*
*& Include zppr1156_1_cld
*&---------------------------------------------------------------------*
CLASS lcl_controlador DEFINITION.


  PUBLIC SECTION.


    TYPES: BEGIN OF tp_excel,
             icon         TYPE icon_d,
             centro       TYPE werks_d,
             mat_generico TYPE zed_matralgen,
             version_fabr  TYPE verid,
             umb          TYPE zed_unmedida,
             ran_inicial  TYPE zed_rango_inicial_textil,
             ran_final    TYPE zed_rango_final_textil,
             mensaje      TYPE /aif/temse_lgmes,

           END OF tp_excel,
           tp_ti_excel TYPE STANDARD TABLE OF tp_excel.

    TYPES: tp_r_centro   TYPE RANGE OF werks_d,
           tp_r_material TYPE RANGE OF matnr,
*           tp_r_taldiam  TYPE RANGE OF ztppr1156_4-grupo_talla_diametro,
           tp_datos_log  TYPE STANDARD TABLE OF ztppr1156_1.



    DATA: ti_alv     TYPE tp_ti_excel,
          ti_alv_aux TYPE tp_ti_excel,
          es_alv     TYPE tp_excel,
          es_alv_aux TYPE tp_excel.



*d Lectura de archivo
*    DATA: ti_archivo TYPE TABLE OF ZECXR1002_1.

    DATA: ti_archivo TYPE TABLE OF alsmex_tabline,
          es_archivo LIKE LINE OF ti_archivo.



    CLASS-METHODS: obtener_instancia
      RETURNING
        VALUE(ro_instance) TYPE REF TO lcl_controlador.

    METHODS: obtener_excel
      IMPORTING
        iv_filename TYPE rlgrap-filename
      EXPORTING
        e_ti_data   TYPE tp_ti_excel
      RAISING cx_sy_conversion_no_number,

      validar_guardar_campos
        CHANGING
          c_ti_data TYPE tp_ti_excel,

      seleccionar_archivo
        CHANGING
          cv_filename TYPE rlgrap-filename,

      modificar_pantalla
        IMPORTING
          iv_cargar TYPE c,

      start_of_selection
        IMPORTING
          iv_filename TYPE rlgrap-filename
          iv_p_cargar TYPE c
          ir_centro   TYPE tp_r_centro
          ir_material TYPE tp_r_material
*          ir_taldiam  TYPE tp_r_taldiam
         RAISING cx_sy_conversion_no_number.

  PRIVATE SECTION.



    METHODS:
      mostrar_alv
        EXPORTING
          et_alv TYPE tp_ti_excel,

      registrar_log
        IMPORTING
          i_ti_table_log TYPE tp_datos_log,

      guardar_datos
        IMPORTING
          i_ti_table_log TYPE tp_datos_log.


    CLASS-DATA: instance  TYPE REF TO lcl_controlador.


ENDCLASS.
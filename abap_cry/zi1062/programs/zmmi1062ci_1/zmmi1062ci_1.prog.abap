*&---------------------------------------------------------------------*
*& Include zmmi1062ci_1
*&---------------------------------------------------------------------*
CLASS lcl_controlador IMPLEMENTATION.

  METHOD iniciar_proceso.

    DATA: condicion TYPE string.

    CLEAR: gti_ins_lavado.

    condicion = i_denominacion_lavado.
    TRANSLATE condicion TO UPPER CASE.
    CONCATENATE '%' condicion '%' INTO condicion.

    SELECT *
      FROM ztmmi1062_1
      INTO TABLE gti_ins_lavado
      WHERE codlavado IN i_r_codigo_lavado
      AND   denlavado LIKE condicion.

    SORT gti_ins_lavado BY codlavado posicion.

  ENDMETHOD.                    "init_process


  METHOD mostrar_alv.
    DATA: perc      TYPE i,
          extension TYPE i VALUE 100.

    IF go_dock_container IS NOT BOUND AND cl_gui_alv_grid=>offline( ) IS INITIAL.

      CREATE OBJECT go_dock_container
        EXPORTING
          side                        = cl_gui_docking_container=>dock_at_bottom
        EXCEPTIONS
          cntl_error                  = 1
          cntl_system_error           = 2
          create_error                = 3
          lifetime_error              = 4
          lifetime_dynpro_dynpro_link = 5
          OTHERS                      = 6.

      go_dock_container->get_ratio( IMPORTING ratio = perc ).
      extension = ( 500 * extension / perc ).

      go_dock_container->set_extension( extension ).

    ENDIF.

    IF go_alv IS NOT BOUND.

      CREATE OBJECT go_alv
        EXPORTING
          i_parent = go_dock_container.

      gti_catalog = zclcxr1002_util=>construir_catalogo( i_ti = c_ti_datos
                                                          i_optimizar_columnas         = abap_true  ).

*      modificar_catalogo( CHANGING c_ti_catalog = g_ti_catalog ).

      go_alv->set_table_for_first_display(
      CHANGING
          it_outtab                     = c_ti_datos " Output Table
          it_fieldcatalog               = gti_catalog " Field Catalog
        EXCEPTIONS
          invalid_parameter_combination = 1
          program_error                 = 2
          too_many_lines                = 3
          OTHERS                        = 4  ).

      IF sy-subrc <> 0.
        MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                   WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
      ENDIF.

    ELSE.

      go_alv->refresh_table_display( ).

      IF sy-subrc <> 0.
        MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                   WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
      ENDIF.

    ENDIF.
  ENDMETHOD.                    "display_alv



  METHOD liberar_objetos_alv.
    IF go_dock_container IS BOUND.
      go_dock_container->free( ).
      go_alv->free( ).
      FREE: go_dock_container, go_alv.
    ENDIF.
  ENDMETHOD.                    "liberar_objetos_alv

ENDCLASS.                    "lcl_controller IMPLEMENTATION
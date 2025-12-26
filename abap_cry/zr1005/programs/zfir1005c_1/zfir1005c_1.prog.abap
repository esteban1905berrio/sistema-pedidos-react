*&---------------------------------------------------------------------*
*& Include zfir1005c_1
*&---------------------------------------------------------------------*
CLASS lcl_controlador DEFINITION INHERITING FROM zclfir1005_impuesto_diferido.

  PUBLIC SECTION.

    DATA:
          gti_catalogo TYPE lvc_t_fcat.

    DATA: o_alv            TYPE REF TO cl_gui_alv_grid,
          o_dock_container TYPE REF TO cl_gui_docking_container.

    METHODS:
      iniciar_proceso
        IMPORTING
          i_bukrs                      TYPE bukrs
          i_ejercicio                  TYPE glu1-ryear
          i_ledger_base                TYPE glfunct-rldnr
          i_ledger_comparacion         TYPE glfunct-rldnr
          i_version                    TYPE glu1-rvers
          i_mes_inicio                 TYPE char3
          i_mes_final                  TYPE char3
          i_r_numero_cuenta            TYPE tp_r_numero_cuenta
          i_clase_documento            TYPE bkpf-blart
          i_periodo                    TYPE monat
          i_fecha_contabilizacion      TYPE sy-datum
          i_presentar_solo_diferencias TYPE flag,
      consultar_datos
        RETURNING VALUE(r_ti) TYPE REF TO data,
      mostrar_alv
        IMPORTING
          i_layout             TYPE slis_vari
          i_ledger_comparacion TYPE glfunct-rldnr,
      crear_catalogo
        CHANGING
          c_ti_catalogo TYPE lvc_t_fcat,
      data_changed FOR EVENT data_changed  OF cl_gui_alv_grid
        IMPORTING er_data_changed,
      hotspot_click FOR EVENT hotspot_click OF cl_gui_alv_grid
        IMPORTING e_row_id e_column_id.



  PROTECTED SECTION.

  PRIVATE SECTION.
ENDCLASS.

CLASS lcl_controlador IMPLEMENTATION.

  METHOD iniciar_proceso.
    BREAK seblondo.
    cargar_parametrizacion( EXPORTING
                                i_bukrs = i_bukrs
                            IMPORTING
                                e_ti_cuentas_contabilizacion =  DATA(ti_cuentas_contabilizacion) ).

    DATA(ti_comparacion_cuentas) = consultar_comparacion_ledger( i_bukrs              = i_bukrs
                                                                i_ejercicio          = i_ejercicio
                                                                i_ledger_base        = i_ledger_base
                                                                i_ledger_comparacion = i_ledger_comparacion
                                                                i_version            = i_version
                                                                i_mes_inicio         = i_mes_inicio
                                                                i_mes_final          = i_mes_final
                                                                i_r_numero_cuenta    = i_r_numero_cuenta
                                                                i_ti_cuentas_contabilizacion = ti_cuentas_contabilizacion ).


    consolidar_impuesto_diferido(
      EXPORTING
        i_bukrs                 = i_bukrs
        i_solo_diferencia       = i_presentar_solo_diferencias
        i_fecha_contabilizacion = i_fecha_contabilizacion
      CHANGING
        c_ti_diferencias        = ti_comparacion_cuentas
        c_ti_impuesto_diferido  = gti_impuesto_diferido  ).

  ENDMETHOD.

  METHOD consultar_datos.

  ENDMETHOD.

  METHOD mostrar_alv.
    DATA: porcentaje TYPE i,
          extension  TYPE i VALUE 100,
          ti_sort    TYPE lvc_t_sort.

    IF o_dock_container IS NOT BOUND AND cl_gui_alv_grid=>offline( ) IS INITIAL.

      CREATE OBJECT o_dock_container
        EXPORTING
          side                        = cl_gui_docking_container=>dock_at_bottom
        EXCEPTIONS
          cntl_error                  = 1
          cntl_system_error           = 2
          create_error                = 3
          lifetime_error              = 4
          lifetime_dynpro_dynpro_link = 5
          OTHERS                      = 6.

      o_dock_container->get_ratio( IMPORTING ratio = porcentaje ).
      extension = ( 500 * extension / porcentaje ).

      o_dock_container->set_extension( extension ).

    ENDIF.

    IF o_alv IS NOT BOUND.

      o_alv = NEW cl_gui_alv_grid(  i_parent = o_dock_container ).

      crear_catalogo( CHANGING  c_ti_catalogo = gti_catalogo ).

      SET HANDLER data_changed  FOR o_alv.
      SET HANDLER hotspot_click FOR o_alv.

      o_alv->set_adjust_design( adjust_design = 1 ).

      ti_sort = VALUE #( ( spos = '1' fieldname = 'BUKRS' up = abap_true ) ).

      o_alv->set_table_for_first_display(
      EXPORTING
        i_save     = 'A'
        is_layout  = VALUE #( zebra = abap_true smalltitle = space cwidth_opt = abap_true
                              info_fname = 'LINE_COLOR'
                              grid_title = |{ TEXT-tlg } { i_ledger_comparacion }|
                            )
        is_variant = VALUE #( variant = i_layout report = sy-repid username = sy-uname )
        it_toolbar_excluding = VALUE #( ( cl_gui_alv_grid=>mc_fc_detail )
                                        ( cl_gui_alv_grid=>mc_fc_check )
                                        ( cl_gui_alv_grid=>mc_fc_refresh )
                                        ( cl_gui_alv_grid=>mc_fc_loc_insert_row )
                                        ( cl_gui_alv_grid=>mc_fc_loc_delete_row )
                                        ( cl_gui_alv_grid=>mc_fc_loc_append_row )
                                        ( cl_gui_alv_grid=>mc_fc_loc_copy_row )
                                        ( cl_gui_alv_grid=>mc_fc_loc_copy )
                                        ( cl_gui_alv_grid=>mc_fc_loc_paste )
                                        ( cl_gui_alv_grid=>mc_fc_loc_undo )
                                        ( cl_gui_alv_grid=>mc_fc_loc_paste_new_row )
                                      )
      CHANGING
          it_outtab                     = gti_impuesto_diferido " Output Table
          it_fieldcatalog               = gti_catalogo " Field Catalog
          it_sort                       = ti_sort
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

      o_alv->refresh_table_display( ).

      IF sy-subrc <> 0.
        MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                   WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
      ENDIF.

    ENDIF.
  ENDMETHOD.

  METHOD crear_catalogo.

    TRY.
        c_ti_catalogo = zclcxr1002_util=>construir_catalogo( i_ti = gti_impuesto_diferido  ).
      CATCH cx_dynamic_check cx_static_check.
    ENDTRY.

    LOOP AT c_ti_catalogo ASSIGNING FIELD-SYMBOL(<fs_es_catalogo>).

      CASE <fs_es_catalogo>-fieldname.
        WHEN 'BUKRS' OR 'CUENTA'.
          <fs_es_catalogo>-fix_column = abap_true.
        WHEN 'LEDGER_BASICO'.
          <fs_es_catalogo>-coltext = 'Ledger Basico'.
          <fs_es_catalogo>-outputlen = 18.
          <fs_es_catalogo>-cfieldname = 'MONEDA'.
        WHEN 'LEDGER_COMPARATIVO'.
          <fs_es_catalogo>-coltext = 'Ledger Comparativo'.
          <fs_es_catalogo>-outputlen = 18.
          <fs_es_catalogo>-cfieldname = 'MONEDA'.
        WHEN 'DIFERENCIA'.
          <fs_es_catalogo>-coltext = 'Diferencias'.
          <fs_es_catalogo>-outputlen = 18.
          <fs_es_catalogo>-cfieldname = 'MONEDA'.
        WHEN 'PERMANENTE'.
          <fs_es_catalogo>-coltext = 'Permanente'.
          <fs_es_catalogo>-checkbox = abap_true.
        WHEN 'IMPONIBLE'.
          <fs_es_catalogo>-coltext = 'Imponible'.
          <fs_es_catalogo>-outputlen = 10.
          <fs_es_catalogo>-icon      = abap_true.
          <fs_es_catalogo>-hotspot  = abap_true.
        WHEN 'DEDUCIBLE'.
          <fs_es_catalogo>-coltext = 'Deducible'.
          <fs_es_catalogo>-outputlen = 10.
          <fs_es_catalogo>-icon      = abap_true.
          <fs_es_catalogo>-hotspot  = abap_true.
        WHEN 'PORCENTAJE'.
          <fs_es_catalogo>-coltext = '% porcentaje'.
          <fs_es_catalogo>-outputlen = 10.
        WHEN 'CATEGORIA'.
          <fs_es_catalogo>-coltext = 'Categoria'.
          <fs_es_catalogo>-outputlen = 10.
        WHEN 'IMPUESTO_DIFERIDO'.
          <fs_es_catalogo>-coltext = 'Imp. Diferido'.
          <fs_es_catalogo>-outputlen = 18.
          <fs_es_catalogo>-do_sum = abap_true.
          <fs_es_catalogo>-cfieldname = 'MONEDA'.
        WHEN 'TIPO_IMPUESTO'.
          <fs_es_catalogo>-coltext = 'Tipo Impuesto'.
          <fs_es_catalogo>-outputlen = 15.
        WHEN 'PRKEY' OR 'STUFE'.
          DELETE TABLE c_ti_catalogo FROM <fs_es_catalogo>.
      ENDCASE.

    ENDLOOP.

  ENDMETHOD.

  METHOD data_changed.

    LOOP AT er_data_changed->mt_good_cells INTO DATA(es_good_cell).

      READ TABLE gti_impuesto_diferido ASSIGNING FIELD-SYMBOL(<fs_es_impuesto_diferido>)
                                       INDEX es_good_cell-row_id.

      CASE es_good_cell-fieldname.
        WHEN 'PERMANENTE'.
          <fs_es_impuesto_diferido>-permanente = es_good_cell-value.
        WHEN 'IMPONIBLE'.
          <fs_es_impuesto_diferido>-imponible = es_good_cell-value.
        WHEN 'DEDUCIBLE'.
          <fs_es_impuesto_diferido>-deducible = es_good_cell-value.
      ENDCASE..

    ENDLOOP.

  ENDMETHOD.

  METHOD hotspot_click.


    READ TABLE gti_impuesto_diferido ASSIGNING FIELD-SYMBOL(<fs_es_impuesto_diferido>)
                                     INDEX e_row_id-index.
    IF sy-subrc EQ 0.
      " Solo realizar cambios si la cuenta NO es permanente
      IF <fs_es_impuesto_diferido>-permanente IS INITIAL.
        <fs_es_impuesto_diferido>-impuesto_diferido = <fs_es_impuesto_diferido>-impuesto_diferido * -1.

        " Si se hizo clic en deducible
        IF e_column_id EQ 'DEDUCIBLE'.
          <fs_es_impuesto_diferido>-deducible = icon_radiobutton.
          <fs_es_impuesto_diferido>-imponible = icon_wd_radio_button_empty.
          IF <fs_es_impuesto_diferido>-tipo_impuesto EQ gc_activo.
            <fs_es_impuesto_diferido>-tipo_impuesto = gc_pasivo.
          ELSE.
            <fs_es_impuesto_diferido>-tipo_impuesto = gc_activo.
          ENDIF.

          " Si se hizo clic en imponible
        ELSEIF e_column_id EQ 'IMPONIBLE'.
          <fs_es_impuesto_diferido>-deducible = icon_wd_radio_button_empty.
          <fs_es_impuesto_diferido>-imponible = icon_radiobutton.

          IF <fs_es_impuesto_diferido>-tipo_impuesto EQ gc_activo.
            <fs_es_impuesto_diferido>-tipo_impuesto = gc_pasivo.
          ELSE.
            <fs_es_impuesto_diferido>-tipo_impuesto = gc_activo.
          ENDIF.

        ENDIF.

        o_alv->refresh_table_display(
          EXPORTING
            is_stable = VALUE #( col = abap_true row = abap_true )
          EXCEPTIONS
            finished  = 1
            OTHERS    = 2 ).

        IF sy-subrc <> 0.
          MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
        ENDIF.
      ENDIF.
    ENDIF.

  ENDMETHOD.

ENDCLASS.
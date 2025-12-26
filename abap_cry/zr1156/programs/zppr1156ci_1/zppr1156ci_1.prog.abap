*&---------------------------------------------------------------------*
*& Include zppr1156_1_cli
*&---------------------------------------------------------------------*
CLASS lcl_controlador IMPLEMENTATION.

  METHOD obtener_instancia.
    IF instance IS NOT BOUND.
      " Crear la instancia controladora
      CREATE OBJECT instance.
    ENDIF.

    ro_instance = instance.
  ENDMETHOD.

  METHOD seleccionar_archivo.
    CALL FUNCTION 'KD_GET_FILENAME_ON_F4'
      EXPORTING
        mask          = ',Todos los archivos,*.*'
      CHANGING
        file_name     = cv_filename
      EXCEPTIONS
        mask_too_long = 1.
  ENDMETHOD.

  METHOD modificar_pantalla.
    LOOP AT SCREEN.
      CASE screen-group1.
        WHEN 'GP1'.
          IF iv_cargar EQ abap_true.
            screen-active = 0.
            screen-invisible = 1.
          ELSE.
            screen-active = 1.
            screen-invisible = 0.
            CLEAR: pa_archi.
          ENDIF.
        WHEN 'GP2'.
          IF iv_cargar EQ abap_false.
            screen-active = 0.
            screen-invisible = 1.
          ELSE.
            screen-active = 1.
            screen-invisible = 0.
            CLEAR: so_matge[], so_vf[].
          ENDIF.
      ENDCASE.
      MODIFY SCREEN.
    ENDLOOP.
  ENDMETHOD.

  METHOD start_of_selection.

    DATA: ti_filtro_vista TYPE STANDARD TABLE OF vimsellist.
    CONSTANTS: c_centro       TYPE vimsellist-viewfield VALUE 'CENTRO',
               c_mat_generico TYPE vimsellist-viewfield VALUE 'MAT_GENERICO',
               "c_taldiam      TYPE vimsellist-viewfield VALUE 'GR_TALDIAM',
               c_or           TYPE vimsellist-and_or VALUE 'OR',
               c_and          TYPE vimsellist-and_or VALUE 'AND',
               c_zvppr1156_1  TYPE dd02v-tabname VALUE 'ZVPPR1156_1'.

    IF iv_p_cargar EQ abap_true.
      IF iv_filename IS NOT INITIAL.

        me->obtener_excel(
          EXPORTING
            iv_filename = iv_filename
          IMPORTING
            e_ti_data   = ti_alv
        ).

        me->validar_guardar_campos(
          CHANGING
            c_ti_data = ti_alv
        ).

*  Mostrar datos de ALV.
        me->mostrar_alv(
          IMPORTING
            et_alv = ti_alv_aux
        ).


      ELSE.
        MESSAGE s003(zpp10) DISPLAY LIKE 'E'.
      ENDIF.
    ELSEIF ir_centro IS NOT INITIAL.

      CALL FUNCTION 'VIEW_RANGETAB_TO_SELLIST'
        EXPORTING
          fieldname          = c_centro
          append_conjunction = c_and
        TABLES
          sellist            = ti_filtro_vista
          rangetab           = so_centr.

      IF so_matge IS NOT INITIAL.

        CALL FUNCTION 'VIEW_RANGETAB_TO_SELLIST'
          EXPORTING
            fieldname          = c_mat_generico
            append_conjunction = c_and
          TABLES
            sellist            = ti_filtro_vista
            rangetab           = so_matge.
      ENDIF.

      IF so_vf[] IS NOT INITIAL.
        CALL FUNCTION 'VIEW_RANGETAB_TO_SELLIST'
          EXPORTING
            fieldname          = 'VERSION_FABR'
            append_conjunction = c_and
          TABLES
            sellist            = ti_filtro_vista
            rangetab           = so_vf[].
      ENDIF.

      CALL FUNCTION 'VIEW_MAINTENANCE_CALL'
        EXPORTING
          action                       = 'S'
          view_name                    = c_zvppr1156_1
        TABLES
          dba_sellist                  = ti_filtro_vista
        EXCEPTIONS
          client_reference             = 1
          foreign_lock                 = 2
          invalid_action               = 3
          no_clientindependent_auth    = 4
          no_database_function         = 5
          no_editor_function           = 6
          no_show_auth                 = 7
          no_tvdir_entry               = 8
          no_upd_auth                  = 9
          only_show_allowed            = 10
          system_failure               = 11
          unknown_field_in_dba_sellist = 12
          view_not_found               = 13
          maintenance_prohibited       = 13.

      IF sy-subrc <> 0.
        MESSAGE ID sy-msgid TYPE'S' NUMBER sy-msgno
          WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
      ENDIF.
    ELSE.

      MESSAGE s008(zpp10) DISPLAY LIKE 'E'.

    ENDIF.
  ENDMETHOD.

  METHOD obtener_excel.

    DATA: material TYPE mara-matnr.
    "Cambiar ZCXR1002_ALSM_EXCEL_TO_ITABLE'

    CALL FUNCTION 'ALSM_EXCEL_TO_INTERNAL_TABLE'
      EXPORTING
        filename                = iv_filename
        i_begin_col             = 1
        i_begin_row             = 2
        i_end_col               = 5
        i_end_row               = 5000
      TABLES
        intern                  = ti_archivo
      EXCEPTIONS
        inconsistent_parameters = 1
        upload_ole              = 2
        OTHERS                  = 3.

    SORT ti_archivo.

    IF NOT ti_archivo[] IS INITIAL.
      LOOP AT ti_archivo INTO DATA(es_archivo).

        CASE es_archivo-col.

          WHEN 1.
            es_alv-centro = es_archivo-value.

          WHEN 2.
            material = es_archivo-value.
            CALL FUNCTION 'CONVERSION_EXIT_MATN1_INPUT'
              EXPORTING
                input  = es_archivo-value
              IMPORTING
                output = material.
            es_alv-mat_generico = material.
          WHEN 3.
            es_alv-version_fabr = es_archivo-value.

          WHEN 4.
            REPLACE ALL OCCURRENCES OF '.' IN es_archivo-value WITH space.
            REPLACE ALL OCCURRENCES OF ',' IN es_archivo-value WITH '.'.
            es_alv-ran_inicial = es_archivo-value.

          WHEN 5.
            REPLACE ALL OCCURRENCES OF '.' IN es_archivo-value WITH space.
            REPLACE ALL OCCURRENCES OF ',' IN es_archivo-value WITH '.'.
            es_alv-ran_final = es_archivo-value.
        ENDCASE.

        AT END OF row.
          APPEND es_alv TO e_ti_data.
          CLEAR: es_alv, material.
        ENDAT.

      ENDLOOP.
    ENDIF.

  ENDMETHOD.

  METHOD validar_guardar_campos.
    TYPE-POOLS slis.
    DATA: es_mara        TYPE mara,
          es_t001w       TYPE t001w,
          ti_ztppr1156_1 TYPE TABLE OF ztppr1156_1,
          es_ztppr1156_1 TYPE ztppr1156_1,
          es_escalas     TYPE zvppr1156_1. "Adicion Andres Diaz Gomez 29/09/2025

    DATA: lv_ran_inicial_existente TYPE zed_rango_inicial_textil,
          lv_unidad_medida         TYPE t006-msehi,
          lv_ran_final_existente   TYPE zed_rango_final_textil,
          lv_solapamiento          TYPE abap_bool,
          lv_error_rango           TYPE abap_bool,
          vim_abort_saving         TYPE abap_bool.

    SORT c_ti_data BY centro mat_generico ran_inicial. "version_fabr." mod Andres Diaz Gomez 6/10/2025
    DELETE ADJACENT DUPLICATES FROM c_ti_data COMPARING centro mat_generico version_fabr.

    CHECK c_ti_data IS NOT INITIAL.

    " Filtrar datos de MARA
    DATA(ti_alv_aux2) = c_ti_data[].
    SORT ti_alv_aux2 BY mat_generico.
    DELETE ti_alv_aux2 WHERE mat_generico IS INITIAL.
    DELETE ADJACENT DUPLICATES FROM ti_alv_aux2 COMPARING mat_generico.

    IF ti_alv_aux2 IS NOT INITIAL.
      SELECT satnr, meins FROM mara INTO TABLE @DATA(ti_mara)
      FOR ALL ENTRIES IN @ti_alv_aux2
      WHERE satnr = @ti_alv_aux2-mat_generico.
    ENDIF.

    " Filtrar datos de T001W
    ti_alv_aux2 = c_ti_data[].
    SORT ti_alv_aux2 BY centro.
    DELETE ADJACENT DUPLICATES FROM ti_alv_aux2 COMPARING centro.
    SELECT werks FROM t001w INTO TABLE @DATA(ti_t001w) FOR ALL ENTRIES IN @ti_alv_aux2 WHERE werks = @ti_alv_aux2-centro.

    " Filtrar datos de ZTPPR1156_2
    ti_alv_aux2 = c_ti_data[].
    SORT ti_alv_aux2 BY centro mat_generico version_fabr.
    DELETE ADJACENT DUPLICATES FROM ti_alv_aux2 COMPARING centro mat_generico version_fabr.

    IF ti_alv_aux2 IS NOT INITIAL.
      SELECT * FROM zippr1156_version_fabricacion INTO TABLE @DATA(ti_versiones_fabricacion)
      FOR ALL ENTRIES IN @ti_alv_aux2
      WHERE centro = @ti_alv_aux2-centro
      AND matnr = @ti_alv_aux2-mat_generico
      AND version_fabr = @ti_alv_aux2-version_fabr.
    ENDIF.

    " Filtrar registros existentes en ZTPPR1156_1
    ti_alv_aux2 = c_ti_data[].
    SORT ti_alv_aux2 BY mat_generico version_fabr centro.
    DELETE ADJACENT DUPLICATES FROM ti_alv_aux2 COMPARING mat_generico version_fabr centro.

    IF ti_alv_aux2 IS NOT INITIAL.
      SELECT * FROM ztppr1156_1 INTO TABLE @DATA(ti_registros_existentes)
      FOR ALL ENTRIES IN @ti_alv_aux2 WHERE centro = @ti_alv_aux2-centro
      AND version_fabr = @ti_alv_aux2-version_fabr.
    ENDIF.

    DATA(ti_verfab_completas) = ti_registros_existentes.

    LOOP AT c_ti_data ASSIGNING FIELD-SYMBOL(<fs_es_data>).
      APPEND CORRESPONDING #( <fs_es_data> ) TO ti_verfab_completas.
    ENDLOOP.

    SORT ti_verfab_completas BY centro mat_generico version_fabr.
    DELETE ADJACENT DUPLICATES FROM ti_verfab_completas COMPARING centro mat_generico version_fabr.
    BREAK seblondo.
    " Validar los datos
    LOOP AT c_ti_data INTO DATA(es_alv).

      lv_error_rango = abap_false.

      es_escalas = CORRESPONDING #( es_alv ).

      es_alv-umb = COND #( WHEN line_exists( ti_mara[ satnr = es_alv-mat_generico ] ) THEN
                              ti_mara[ satnr = es_alv-mat_generico ]-meins
                           ELSE
                             VALUE #( ti_versiones_fabricacion[ version_fabr = es_alv-version_fabr ]-meins OPTIONAL ) ).

      es_escalas-meins = es_alv-umb.

      zclppr1156_util_escalas_prod=>validar_rango_referencia(
        EXPORTING
          i_ti_escalas       = ti_verfab_completas
          i_es_escalas       = es_escalas "CORRESPONDING #( es_alv ) Ajuste Andres Diaz Gomez 29/09/2025
        CHANGING
          c_mensaje          = es_alv-mensaje
          c_vim_abort_saving = lv_error_rango ).

      IF lv_error_rango IS NOT INITIAL.
        es_alv-icon = icon_red_light.
        APPEND es_alv TO ti_alv_aux.
      ENDIF.

      IF es_alv-mat_generico IS NOT INITIAL AND
        NOT line_exists( ti_mara[ satnr = es_alv-mat_generico ] ).
        es_alv-icon = icon_red_light.
        MESSAGE e005(zpp10) INTO es_alv-mensaje.
        APPEND es_alv TO ti_alv_aux.
        "MODIFY ti_alv FROM es_alv TRANSPORTING icon mensaje WHERE mat_generico = es_alv-mat_generico.
        CONTINUE.
      ENDIF.

      READ TABLE ti_t001w INTO es_t001w WITH KEY werks = es_alv-centro.
      IF sy-subrc <> 0.
        es_alv-icon = icon_red_light.
        MESSAGE e000(zpp10) INTO es_alv-mensaje.
        APPEND es_alv TO ti_alv_aux.
        "MODIFY ti_alv FROM es_alv TRANSPORTING icon mensaje WHERE centro = es_alv-centro.
        CONTINUE.
      ENDIF.

      DATA(es_vf) = VALUE #( ti_versiones_fabricacion[ centro = es_alv-centro
                                                       matnr  = es_alv-mat_generico
                                                       version_fabr = es_alv-version_fabr ] OPTIONAL ).

      "Validar VF bloqueada
      IF es_vf-mksp NE space.
        es_alv-icon = icon_red_light.
        MESSAGE e648(61) WITH es_alv-version_fabr es_alv-mat_generico es_alv-centro INTO es_alv-mensaje.
        APPEND es_alv TO ti_alv_aux.
        CLEAR: es_vf.
        CONTINUE.
      ENDIF.

      IF es_vf IS INITIAL.
        es_alv-icon = icon_red_light.
        MESSAGE e531(6p) WITH es_alv-version_fabr INTO es_alv-mensaje.
        APPEND es_alv TO ti_alv_aux.
        CONTINUE.
      ENDIF.

      IF es_alv-ran_inicial > es_alv-ran_final OR es_alv-ran_inicial = es_alv-ran_final.
        es_alv-icon = icon_red_light.
        MESSAGE e002(zpp10) INTO es_alv-mensaje.
        APPEND es_alv TO ti_alv_aux.
        CONTINUE.
      ENDIF.
      " registro_duplicado = abap_true.
      IF line_exists( ti_registros_existentes[ centro = es_alv-centro mat_generico = es_alv-mat_generico version_fabr = es_alv-version_fabr ] ).
        MESSAGE e011(zpp10) WITH es_alv-centro es_alv-mat_generico es_alv-version_fabr INTO es_alv-mensaje.
        APPEND VALUE #( icon = icon_red_light centro = es_alv-centro
                        ran_inicial = es_alv-ran_inicial ran_final = es_alv-ran_final
                        mat_generico = es_alv-mat_generico version_fabr = es_alv-version_fabr
                        mensaje = es_alv-mensaje ) TO ti_alv_aux.
      ENDIF.

      "Validr solapamiento de rango a partir de los 3 ultimos digitos de la Version de Fabricacion
      lv_solapamiento = abap_false.

      LOOP AT ti_verfab_completas INTO DATA(es_registros_existentes) WHERE
      centro = es_alv-centro AND
      mat_generico = es_alv-mat_generico.

        IF es_registros_existentes-version_fabr NE es_alv-version_fabr.

          IF es_registros_existentes-version_fabr+1(3) = es_alv-version_fabr+1(3) AND
             ( ( es_alv-ran_inicial BETWEEN es_registros_existentes-ran_inicial AND es_registros_existentes-ran_final ) OR
             ( es_alv-ran_final   BETWEEN es_registros_existentes-ran_inicial AND es_registros_existentes-ran_final ) OR
             ( es_alv-ran_inicial < es_registros_existentes-ran_inicial AND es_alv-ran_final > es_registros_existentes-ran_final ) ).

            lv_solapamiento = abap_true.
            EXIT.
          ENDIF.
        ELSEIF es_alv-ran_inicial = es_alv-ran_final.
          MESSAGE s012(zpp10) WITH es_alv-centro es_alv-mat_generico es_alv-version_fabr INTO es_alv-mensaje.
          vim_abort_saving = abap_true.
          EXIT.
        ENDIF.
      ENDLOOP.

      IF lv_solapamiento IS NOT INITIAL.
        es_alv-icon = icon_red_light.
        MESSAGE e009(zpp10) WITH es_registros_existentes-centro es_registros_existentes-version_fabr INTO es_alv-mensaje.
        APPEND es_alv TO ti_alv_aux.
        lv_solapamiento = abap_false.
        "MODIFY ti_alv INDEX sy-tabix FROM es_alv TRANSPORTING icon mensaje.
        CONTINUE.

      ENDIF.

      "Validar que no existan errores en las validaciones
      CHECK NOT line_exists( ti_alv_aux[ icon = icon_red_light centro = es_alv-centro
                                         mat_generico = es_alv-mat_generico
                                         version_fabr = es_alv-version_fabr ] ).

      es_alv-icon = icon_green_light.
      es_ztppr1156_1-centro = es_alv-centro.
      es_ztppr1156_1-mat_generico = es_alv-mat_generico.
      es_ztppr1156_1-version_fabr = es_alv-version_fabr.
      es_ztppr1156_1-ran_final = es_alv-ran_final.
      es_ztppr1156_1-ran_inicial = es_alv-ran_inicial.



      CALL FUNCTION 'CONVERSION_EXIT_CUNIT_INPUT'
        EXPORTING
          input          = es_alv-umb
          language       = sy-langu
        IMPORTING
          output         = es_alv-umb
        EXCEPTIONS
          unit_not_found = 1
          OTHERS         = 2.

      APPEND es_ztppr1156_1 TO ti_ztppr1156_1.
      MESSAGE e004(zpp10) INTO es_alv-mensaje.
      APPEND es_alv TO ti_alv_aux.

    ENDLOOP.

    zclppr1156_util_escalas_prod=>validar_material_generico(
      EXPORTING
        i_ti_escalas        = ti_ztppr1156_1
      IMPORTING
        e_ti_material_error = DATA(ti_material_no_generico)
    ).

    IF ti_material_no_generico IS NOT INITIAL.
      DATA(v_material) = ti_material_no_generico[ 1 ].
      MESSAGE s010(zpp10) DISPLAY LIKE 'E' WITH v_material.
      vim_abort_saving = abap_true.
    ENDIF.

***    CHECK vim_abort_saving = abap_false.

    me->guardar_datos( i_ti_table_log = ti_ztppr1156_1 ).


  ENDMETHOD.

  METHOD registrar_log.

    DATA: action TYPE c.

    ".Se crea instancia para el manejo del log
    DATA(o_log) = NEW zclppr1156_log_ztppr1156_1(  ).

    DATA(ti_table_log) = i_ti_table_log[].
    SORT ti_table_log BY centro mat_generico version_fabr.
    DELETE ADJACENT DUPLICATES FROM ti_table_log COMPARING centro mat_generico version_fabr .

    SELECT * FROM ztppr1156_1 INTO TABLE @DATA(ti_escalas_produccion)
      FOR ALL ENTRIES IN @ti_table_log
      WHERE centro = @ti_table_log-centro AND
            mat_generico = @ti_table_log-mat_generico AND
            version_fabr = @ti_table_log-version_fabr.
    IF sy-subrc NE 0.

    ENDIF.

    LOOP AT i_ti_table_log INTO DATA(es_table_log).

      READ TABLE ti_escalas_produccion TRANSPORTING NO FIELDS
        WITH KEY centro = es_table_log-centro
                 mat_generico = es_table_log-mat_generico
                 version_fabr = es_table_log-version_fabr.
      IF sy-subrc EQ 0.
        action = 'U'.
      ELSE.
        action = 'I'.
      ENDIF.

      ".Registramos log
      o_log->registrar_log(
        EXPORTING
          i_es_data = es_table_log
          i_accion  = action
      ).
    ENDLOOP.
  ENDMETHOD.


  METHOD mostrar_alv.


    DATA: o_column            TYPE REF TO cl_salv_column_table,
          o_alv               TYPE REF TO cl_salv_table,
          o_display           TYPE REF TO cl_salv_display_settings,
          o_toolbar_functions TYPE REF TO cl_salv_functions_list,
          o_layout_settings   TYPE REF TO cl_salv_layout,
          es_layout_key       TYPE salv_s_layout_key,
          cx_salv_msg         TYPE REF TO cx_salv_msg,
          o_columnas          TYPE REF TO  cl_salv_columns_table.


    IF et_alv[] IS NOT INITIAL.

      TRY.
          cl_salv_table=>factory(
            IMPORTING
              r_salv_table = o_alv
            CHANGING
              t_table      = et_alv ).

        CATCH cx_salv_msg INTO cx_salv_msg.

      ENDTRY.

      o_toolbar_functions = o_alv->get_functions( ).
      o_toolbar_functions->set_all( EXPORTING value = if_salv_c_bool_sap=>true ).

      " Formato Zebra
      o_display = o_alv->get_display_settings( ).
      o_display->set_striped_pattern( abap_true ).

      o_layout_settings = o_alv->get_layout( ).
      es_layout_key-report = sy-repid.
      o_layout_settings->set_key( es_layout_key ).
      o_layout_settings->set_save_restriction( if_salv_c_layout=>restrict_none ).

      o_columnas = o_alv->get_columns( ).
*      me->build_fieldcat( ).
      o_columnas->set_optimize( 'X' ).

      o_column ?= o_columnas->get_column( columnname = 'ICON' ).
      o_column->set_output_length( value = 20 ).
      o_column->set_long_text( value = 'Estado' ).
      o_column->set_short_text( value = 'Estado' ).
      o_column->set_medium_text( value = 'Estado' ).

      " Instanciar método de despliegue
      o_alv->display( ).

    ENDIF.
    "display_alv

  ENDMETHOD.

  METHOD guardar_datos.
    me->registrar_log( i_ti_table_log = i_ti_table_log ).
    MODIFY ztppr1156_1 FROM TABLE i_ti_table_log.
    IF sy-subrc EQ 0.
      COMMIT WORK AND WAIT.
    ENDIF.
  ENDMETHOD.

ENDCLASS.
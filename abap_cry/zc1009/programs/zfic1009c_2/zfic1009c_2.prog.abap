*&---------------------------------------------------------------------*
*& Include zfic1009c_2
*&---------------------------------------------------------------------*

CLASS lcl_controlador DEFINITION.

  PUBLIC SECTION.
    INTERFACES: zifcxr1002_alvgrid.
    ALIASES modificar_catalogo FOR zifcxr1002_alvgrid~modificar_catalogo.

    METHODS:
      hd_hotspot_click        FOR EVENT hotspot_click
        OF cl_gui_alv_grid
        IMPORTING e_row_id
                  e_column_id
                  es_row_no,
      truncar_tabla_log,
      inicializar.

  PROTECTED SECTION.

  PRIVATE SECTION.

ENDCLASS.

CLASS lcl_controlador IMPLEMENTATION.

  METHOD modificar_catalogo.
    LOOP AT c_ti_catalogo ASSIGNING FIELD-SYMBOL(<fs_es_catalogo>).
      CASE <fs_es_catalogo>-fieldname.
        WHEN 'ANLN1_C'.
          <fs_es_catalogo>-coltext =  TEXT-005.
          <fs_es_catalogo>-hotspot =  abap_true.
        WHEN 'MESSAGE'.
          <fs_es_catalogo>-outputlen =  45.
*        WHEN 'TYPE' OR 'IDMSG' OR 'NUMERO'.
*          <fs_es_catalogo>-no_out  = abap_true.
      ENDCASE.
    ENDLOOP.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~registrar_evento_alv.

    SET HANDLER hd_hotspot_click FOR c_o_alvgrid.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai.

    CASE i_ok_code.
      WHEN 'BACK' OR 'EXIT'.
        LEAVE TO SCREEN 0.
      WHEN 'ACTUALIZAR'.
        zclfic1009_carga_activos_fijos=>mostrar_resultados_log( EXPORTING
                                                                  i_conservar_screen   = abap_true
                                                                  i_r_asset            = so_asset[]
                                                                  i_r_flnam            = so_flnam[]
                                                                  i_r_fecha            = so_fecha[]
                                                                  i_r_hora             = so_hora[]
                                                                  i_r_tipo             = so_tipo[]
                                                                  i_o_grid_log_handler = me
                                                                 CHANGING
                                                                  c_ti_log = gti_log_icon   ).

    ENDCASE.

  ENDMETHOD.

  METHOD hd_hotspot_click.

    DATA(es_log_data) = gti_log_icon[ e_row_id ].

    CHECK es_log_data-anln1_c IS NOT INITIAL.

    SET PARAMETER ID 'AN1' FIELD es_log_data-anln1_c.
    SET PARAMETER ID 'AN2' FIELD es_log_data-anln2.
    SET PARAMETER ID 'BUK' FIELD es_log_data-bukrs.
    CALL TRANSACTION 'AS03' AND SKIP FIRST SCREEN.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai_at_exit_command.

  ENDMETHOD.

  METHOD truncar_tabla_log.

    DATA: l_respuesta TYPE string.

    "valida decision de usuario
    CALL FUNCTION 'POPUP_TO_CONFIRM'
      EXPORTING
        titlebar       = TEXT-002    " Title of dialog box
        text_question  = TEXT-003    " Question text in dialog box
      IMPORTING
        answer         = l_respuesta    " Return values: '1', '2', 'A'
      EXCEPTIONS
        text_not_found = 1
        OTHERS         = 2.

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                 WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

    "        '1' Si
    "        '2' No
    "        'A' 'Cancel' pushbutton
    CHECK l_respuesta EQ '1'.
    "elimina todos los datos de la tabla de log
    DELETE FROM ztfic1009_1.

    IF sy-subrc IS INITIAL.
      COMMIT WORK AND WAIT.
    ENDIF.

    MESSAGE s390(/SEHS/DG_DGA).

  ENDMETHOD.

  METHOD inicializar.
    APPEND VALUE #( low = sy-datum ) TO so_fecha[].
    sscrfields-functxt_01 = TEXT-004.
  ENDMETHOD.

ENDCLASS.
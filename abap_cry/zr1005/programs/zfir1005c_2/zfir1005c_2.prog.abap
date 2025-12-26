*&---------------------------------------------------------------------*
*& Include zfir1005c_2
*&---------------------------------------------------------------------*
CLASS lcl_controlador DEFINITION INHERITING FROM zclfir1005_impuesto_diferido.

  PUBLIC SECTION.

    INTERFACES:zifcxr1002_alvgrid.

    CLASS-DATA: gti_catalogo TYPE lvc_t_fcat.

    METHODS:
      construir_catalogo_historico
        IMPORTING
          i_ti                 TYPE tp_ti_historial_imp_diferido
        RETURNING
          VALUE(r_ti_catalogo) TYPE lvc_t_fcat,
      mostrar_datos_salida,
      hotspot_click FOR EVENT hotspot_click OF cl_gui_alv_grid
        IMPORTING e_row_id e_column_id.

  PROTECTED SECTION.

  PRIVATE SECTION.
ENDCLASS.

CLASS lcl_controlador IMPLEMENTATION.

  METHOD hotspot_click.

    CHECK e_column_id-fieldname = 'BELNR' OR e_column_id-fieldname = 'BELNR_AN'.

    TRY.
        DATA(es_historico) = gti_historico_imp_diferido[ e_row_id-index  ].
        ASSIGN COMPONENT e_column_id-fieldname OF STRUCTURE es_historico TO FIELD-SYMBOL(<fs_belnr>).
      CATCH cx_sy_itab_line_not_found.
        RETURN.
    ENDTRY.

    SET PARAMETER ID 'BLN' FIELD <fs_belnr>.
    SET PARAMETER ID 'BUK' FIELD es_historico-bukrs.
    SET PARAMETER ID 'GJR' FIELD es_historico-gjahr.
    CALL TRANSACTION 'FB03' AND SKIP FIRST SCREEN.

  ENDMETHOD.

  METHOD construir_catalogo_historico.

    r_ti_catalogo = zclcxr1002_util=>construir_catalogo( i_ti  = i_ti i_optimizar_columnas = abap_true ).

    LOOP AT r_ti_catalogo ASSIGNING FIELD-SYMBOL(<fs_es_catalogo>).

      CASE <fs_es_catalogo>-fieldname.
        WHEN 'HKONT'.
          <fs_es_catalogo>-coltext = 'Cuenta'.
        WHEN 'CATEGORIA'.
          <fs_es_catalogo>-col_pos = 6.
        WHEN 'MONAT'.
          <fs_es_catalogo>-coltext = 'Mes'.

      ENDCASE.
    ENDLOOP.

  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~modificar_catalogo.

  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai.

  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai_at_exit_command.

  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~registrar_evento_alv.

    SET HANDLER hotspot_click FOR c_o_alvgrid.

  ENDMETHOD.

  METHOD mostrar_datos_salida.

    IF gti_historico_imp_diferido IS NOT INITIAL.
      CALL FUNCTION 'ZCXR1002_MOSTRARALV_01'
        EXPORTING
          i_ti_datos      = REF #( gti_historico_imp_diferido )
          i_ti_catalogo   = construir_catalogo_historico( gti_historico_imp_diferido )
          i_titulo        = TEXT-tgr
          i_o_ctr_alvgrid = me
          i_es_layout     = VALUE lvc_s_layo( zebra = abap_true )
          i_es_variante   = VALUE disvariant( variant = pa_layou report = sy-repid username = sy-uname )
          i_ti_sort       = VALUE lvc_t_sort( ( spos = 1 fieldname = 'BUKRS'  up = abap_true down = abap_true group = abap_true subtot = abap_true )
                                              ( spos = 2 fieldname = 'GJAHR'  up = abap_true down = abap_true group = abap_true subtot = abap_true )
                                            ).
    ELSE.
      MESSAGE s002(wusl) DISPLAY LIKE 'E'.
    ENDIF.

  ENDMETHOD.

ENDCLASS.
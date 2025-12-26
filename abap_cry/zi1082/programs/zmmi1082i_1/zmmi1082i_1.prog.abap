*&---------------------------------------------------------------------*
*& Include zmmi1082i_1
*&---------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE user_command INPUT.

  _ok_code = ok_code.
  CLEAR: ok_code.

  IF go_reposicion IS NOT BOUND.
    go_reposicion =  NEW zclmmi1082_modifica_reposicion(  ).
  ENDIF.

  CASE _ok_code.
    WHEN 'EJECUTAR'.

      CASE sy-dynnr.
        WHEN 100.
          go_reposicion->visualizar_reposiciones( i_r_localizacion          = so_loc[]
                                                  i_r_dias_semana           = so_dsm[]
                                                  i_r_calendario            = so_cld[]
                                                  i_r_tipo_localizacion     = so_tpl[] ).
        WHEN 200.
          go_reposicion->preparar_env_reposicion_manual( i_r_tipo_localizacion = so_rtpl[]
                                                         i_r_localizacion      = so_locz[]
                                                         i_reponer             = pa_rep ).
        WHEN 300.
          go_reposicion->cargar_archivo( i_nombre_archivo = pa_narch ).
        WHEN 400.
          go_reposicion->consultar_calendario(  i_r_calendario = so_calen[] i_r_fecha = so_cfech[] ).
      ENDCASE.

    WHEN 'CONSULTAR'.
      CALL SCREEN '0100'.
    WHEN 'REPMANUAL'.
      CALL SCREEN '0200'.
    WHEN 'CARGAARCH'.
      CALL SCREEN '0300'.
    WHEN 'CALENDARIO'.
      CALL SCREEN '0400'.
    WHEN 'BACK' OR 'EXIT'.
      IF sy-dynnr = 100.
        LEAVE TO SCREEN 0.
      ELSE.
        LEAVE TO SCREEN 100.
      ENDIF.
  ENDCASE.

ENDMODULE.
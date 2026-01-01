*&---------------------------------------------------------------------*
*& Include zfir1113i_1
*&---------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND_EXIT  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE user_command_exit INPUT.

  gok_code_activo = ok_code.
  CLEAR: ok_code.

  CASE gok_code_activo.
    WHEN 'CANCEL'.
      LEAVE TO SCREEN 0.
  ENDCASE.

ENDMODULE.

MODULE user_command INPUT.

  gok_code_activo = ok_code.
  CLEAR: ok_code.

  CASE gok_code_activo.
    WHEN 'ENVIAR'.
       go_control_elementos->preparar_datos_para_legado( ).
    WHEN 'ACTUALIZAR'.
      go_control_elementos->iniciar_proceso_datos(
        EXPORTING
          i_r_matnr   = so_matnr[]
          i_r_werks   = so_werks[]
          i_r_kadat   = so_kadat[]
          i_r_klvar   = so_klvar[]
          i_detallado = pa_deta
          i_agrupado  = pa_agru ).
    WHEN 'BACK' OR 'EXIT'.
      LEAVE TO SCREEN 0.
  ENDCASE.

ENDMODULE.
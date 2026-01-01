*&---------------------------------------------------------------------*
*& Include zppr1157i_1
*&---------------------------------------------------------------------*

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
    WHEN 'ACTUALIZAR'.

      go_proceo_infotint->go_alv->refresh_table_display(  ).

    WHEN 'BACK' OR 'EXIT'.
      LEAVE TO SCREEN 0.
    WHEN 'LOG'.
      zclppr1157_productos_quimicos=>visualizar_log( ).
  ENDCASE.

ENDMODULE.
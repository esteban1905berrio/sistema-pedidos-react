*&---------------------------------------------------------------------*
*& Include zmmi1062i_1
*&---------------------------------------------------------------------*
MODULE user_command_exit INPUT.

  LEAVE PROGRAM.

ENDMODULE.

MODULE user_command INPUT.

  okcode_n = ok_code.

  CLEAR ok_code.

  CASE okcode_n.
    WHEN 'BACK' OR  'EXIT'.
      LEAVE TO SCREEN 0.
  ENDCASE.

ENDMODULE.
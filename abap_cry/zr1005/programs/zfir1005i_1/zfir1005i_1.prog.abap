*&---------------------------------------------------------------------*
*& Include zfir1005i_1
*&---------------------------------------------------------------------*

MODULE user_command INPUT.

  ok_code_delta = ok_code.

  CLEAR: ok_code.

  CASE ok_code_delta.
    WHEN 'BACK' OR 'EXIT' OR 'CANCEL'.
      LEAVE TO SCREEN 0.
    WHEN 'CONTAB' OR 'CONTABTEST'.
      go_controlador->o_alv->check_changed_data(  ).
      go_controlador->contabilizar_impuesto_diferido(
        EXPORTING
          i_test                  = COND #( WHEN ok_code_delta = 'CONTABTEST' THEN abap_true )
          i_bukrs                 = pa_bukrs
          i_ejercicio             = pa_ryear
          i_mes_final             = pa_frper
          i_ledger_base           = pa_rldnr
          i_ledger_comparacion    = pa_rldn2
          i_fecha_contabilizacion = pa_fecon
          i_clase_documento       = pa_blart
          i_periodo               = pa_monat
        CHANGING
          c_ti_impuesto_diferido  = go_controlador->gti_impuesto_diferido    ).


    WHEN 'PERMANENTE'.

        go_controlador->procesar_cuentas_permanente(
          EXPORTING
            i_bukrs                = pa_bukrs
          CHANGING
            c_ti_impuesto_diferido = go_controlador->gti_impuesto_diferido ).

  ENDCASE.

ENDMODULE.
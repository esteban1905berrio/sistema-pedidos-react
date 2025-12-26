*&---------------------------------------------------------------------*
*& Include          ZPPFR1156_1_EVT
*&---------------------------------------------------------------------*

INITIALIZATION.

  MOVE 'Descargar Formato'(043) TO sscrfields-functxt_01.
  MOVE 'Log de tablas'(043) TO sscrfields-functxt_02.
*  Instanciar objeto controlador.
  IF go_controlador IS INITIAL.
    go_controlador = lcl_controlador=>obtener_instancia( ).
  ENDIF.


AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_archi.
  go_controlador->seleccionar_archivo(
    CHANGING
      cv_filename = pa_archi
  ).

AT SELECTION-SCREEN.
  CASE sscrfields-ucomm.
    WHEN 'FC01'.
      zclppr1156_util_escalas_prod=>descargar_plantilla_excel( ).
    WHEN 'FC02'.

      SUBMIT RSSCD100
      VIA SELECTION-SCREEN
      WITH OBJEKT = 'ZPP_ZTPPR1156_*'
      AND RETURN.

  ENDCASE.

AT SELECTION-SCREEN OUTPUT.
  go_controlador->modificar_pantalla( iv_cargar = pa_carga ).


START-OF-SELECTION.
  TRY.
      go_controlador->start_of_selection(
        EXPORTING
          iv_filename = pa_archi
          iv_p_cargar = pa_carga
          ir_centro   = so_centr[]
          ir_material = so_matge[]
          "ir_taldiam  = so_taldi[]
      ).
    CATCH cx_sy_conversion_no_number INTO DATA(lo_exception).
      MESSAGE lo_exception->get_text( ) TYPE 'S' DISPLAY LIKE 'E'.
  ENDTRY.
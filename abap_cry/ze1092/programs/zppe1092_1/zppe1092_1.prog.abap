*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: E1092 - Creación masiva de órdenes de fabricación
* Programa     : zsdc1020_1
* Tipo Objeto  : Reporte
* Descripción  : Creación masiva de órdenes de fabricación
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 20.08.2023
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 20.07.2023    S4DK914611   Sebastian Londoño         Creación
*----------------------------------------------------------------------*

REPORT zppe1092_1.

INCLUDE: zppe1092p_1.

INITIALIZATION.
  MOVE 'Descargar Formato'(043) TO sscrfields-functxt_01.
  go_crea_of = NEW zclppe1092_cre_masiva_of(  ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_narch.

  CALL FUNCTION 'F4_FILENAME'
    IMPORTING
      file_name = pa_narch.

AT SELECTION-SCREEN.
  CASE sscrfields-ucomm.
    WHEN 'FC01'.
      zclppe1092_cre_masiva_of=>descargar_plantilla( ).
  ENDCASE.

START-OF-SELECTION.

  go_crea_of->iniciar_proceso_creacion_of( i_nombre_archivo = pa_narch ).

END-OF-SELECTION.
  TRY.
      go_crea_of->visualizar_resultado_carga( ).
    CATCH cx_t100_msg INTO DATA(o_cx).
      MESSAGE s208(00) WITH o_cx->get_longtext( ) DISPLAY LIKE 'E'.
  ENDTRY.
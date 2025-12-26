*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: C1020 - Carga masiva Supply Protection
* Programa     : zsdc1020_1
* Tipo Objeto  : Reporte
* Descripción  : Carga masiva Supply Protection
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 20.02.2023
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 20.02.2023    S4DK914611   Sebastian Londoño         Creación
*----------------------------------------------------------------------*

REPORT zsdc1020_1.

INCLUDE: zsdc1020p_1.

INITIALIZATION.
  MOVE 'Descargar Formato'(043) TO sscrfields-functxt_01.
  go_carga_masiva_sup = NEW zclsdc1020_carga_masiva_sup(  ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_narch.

  CALL FUNCTION 'F4_FILENAME'
    IMPORTING
      file_name = pa_narch.

AT SELECTION-SCREEN.
  CASE sscrfields-ucomm.
    WHEN 'FC01'.
      zclsdc1020_carga_masiva_sup=>descargar_plantilla( ).
  ENDCASE.

START-OF-SELECTION.

  go_carga_masiva_sup->iniciar_carga( i_nombre_archivo = pa_narch ).

END-OF-SELECTION.
  TRY.
      go_carga_masiva_sup->visualizar_resultado_carga( ).
    CATCH cx_t100_msg INTO DATA(o_cx).
      MESSAGE s208(00) WITH o_cx->get_longtext( ) DISPLAY LIKE 'E'.
  ENDTRY.
*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1157
* Programa     : zppr1157_1
* Tipo Objeto  : Reporte
* Descripción  : Actualización de productos químicos a la lista de
*                materiales en SAP
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 19.01.2024
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 19.01.2024    S4DK905891   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zppr1157_1.

INCLUDE zppr1157p_1.
INCLUDE zppr1157o_1.
INCLUDE zppr1157i_1.

INITIALIZATION.

  go_proceo_infotint = NEW zclppr1157_productos_quimicos( ).

AT SELECTION-SCREEN OUTPUT.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.

  zclcxr1002_util=>f4_layout( EXPORTING i_report = sy-repid CHANGING c_layout = pa_layou ).

START-OF-SELECTION.
  "                        (
  "                     (  ) (
  "                      )    )
  "         |||||||     (  ( (
  "        ( O   O )        )
  " ____oOO___(_)___OOo____(
  "(_______________________)
  "           JOINT
  go_proceo_infotint->iniciar_tratamiento_infotint(
    EXPORTING
      i_r_centro                  = so_centr[]
      i_r_fecha_envio             = so_fenvi[]
      i_r_material_legado         = so_mtleg[]
      i_r_material                = so_matnr[]
      i_r_color                   = so_color[]
      i_r_alternativa             = so_alter[]
      i_pendiente_procesar        = pa_ppent
      i_visualizar_duplicados     = pa_vidup
      i_ejecutar_jobs_agrupados   = pa_pajob
      i_r_indicador_actualizacion = so_indi[] ).

END-OF-SELECTION.

  CHECK sy-batch IS INITIAL AND pa_vidup = abap_false.

  IF go_proceo_infotint->gti_lista_infotint IS NOT INITIAL.
    CALL SCREEN 100.
  ELSE.
    MESSAGE s002(wusl) DISPLAY LIKE 'E'.
  ENDIF.
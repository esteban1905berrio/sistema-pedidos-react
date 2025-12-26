*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1113 - Reporte control de elementos
* Programa     : ZFIR1113_3
* Tipo Objeto  : Reporte
* Descripción  : Reporte control de elementos
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 21.03.2024
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 21.03.2024    S4DK917548   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zfir1113_3.

INCLUDE zfir1113p_1.
INCLUDE zfir1113o_1.
INCLUDE zfir1113i_1.

INITIALIZATION.

  go_control_elementos = NEW zclfir1113_control_elementos( ).

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
  go_control_elementos->iniciar_proceso_datos(
    EXPORTING
      i_r_matnr   = so_matnr[]
      i_r_werks   = so_werks[]
      i_r_kadat   = so_kadat[]
      i_r_klvar   = so_klvar[]
      i_detallado = pa_deta
      i_agrupado  = pa_agru ).

END-OF-SELECTION.

  IF go_control_elementos->gti_control_elemento_detallado IS NOT INITIAL.
    CALL SCREEN 100.
  ELSE.
    MESSAGE s002(wusl) DISPLAY LIKE 'E'.
  ENDIF.
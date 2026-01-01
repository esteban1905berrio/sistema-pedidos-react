*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1005
* Programa     : zfir1005_1
* Tipo Objeto  : Reporte
* Descripción  : Contabilizazicon de impuestos diferidos
*
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 17.03.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.03.2021   D01K928334    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*

REPORT zfir1005_1.

INCLUDE zfir1005p_1.
INCLUDE zfir1005c_1.
INCLUDE zfir1005o_1.
INCLUDE zfir1005i_1.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.
  zclcxr1002_util=>f4_layout( CHANGING c_layout = pa_layou ).

START-OF-SELECTION.

  go_controlador = NEW lcl_controlador( ).

  go_controlador->iniciar_proceso(    i_bukrs                      = pa_bukrs
                                      i_ejercicio                  = pa_ryear
                                      i_ledger_base                = pa_rldnr
                                      i_ledger_comparacion         = pa_rldn2
                                      i_version                    = pa_rvers
                                      i_mes_inicio                 = pa_frper
                                      i_mes_final                  = pa_toper
                                      i_r_numero_cuenta            = so_racct[]
                                      i_clase_documento            = pa_blart
                                      i_periodo                    = pa_monat
                                      i_fecha_contabilizacion      = pa_fecon
                                      i_presentar_solo_diferencias = pa_sdif  ).

END-OF-SELECTION.

  IF go_controlador->gti_impuesto_diferido IS NOT INITIAL.
    CALL SCREEN 100.
  ELSE.
    MESSAGE s002(wusl) DISPLAY LIKE 'E'.
  ENDIF.
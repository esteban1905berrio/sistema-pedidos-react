*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: C1012
* Programa     : zfic1021_1
* Tipo Objeto  : Reporte
* Descripción  : Carga Contabilizacion Resgristros de Nomina
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 03.06.2024
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 03.06.2024    S4DK928228   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zfic1021_1.

INCLUDE zfic1021p_1.
INCLUDE zfic1021o_1.
INCLUDE zfic1021i_1.

INITIALIZATION.
  MOVE 'Visualizar Log'(043) TO sscrfields-functxt_01.
  DATA(go_contabilizacion_nomina) = NEW zclfic1021_contabiliza_nomina( ).

AT SELECTION-SCREEN OUTPUT.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_arch.
  pa_arch = zclcxr1002_cargar_archivo=>matchcode_csv( ).

AT SELECTION-SCREEN.
  CASE sscrfields-ucomm.
    WHEN 'FC01'.
      go_contabilizacion_nomina->visualizar_log_db( i_bukrs          = pa_bukrs
                                                    i_clase_nomina   = pa_clnom
                                                    i_periodo_nomina = pa_perio ).
  ENDCASE.

START-OF-SELECTION.
  IF pa_arch IS NOT INITIAL.
    go_contabilizacion_nomina->iniciar_proceso_de_carga( i_bukrs          = pa_bukrs
                                                         i_clase_nomina   = pa_clnom
                                                         i_periodo_nomina = pa_perio
                                                         i_nombre_archivo = pa_arch ).
  ELSE.
    MESSAGE s055(00) DISPLAY LIKE 'E'.
  ENDIF.

END-OF-SELECTION.
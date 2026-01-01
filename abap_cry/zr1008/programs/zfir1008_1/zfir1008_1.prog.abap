*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1008
* Programa     : zfir1008_1
* Tipo Objeto  : Reporte
* Descripción  : Reporte Datacredito_Cartera
*
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 09.04.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 09.04.2021   S4DK900419    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*

REPORT zfir1008_1.

INCLUDE zfir1008p_1.
INCLUDE zfir1008c_1.

START-OF-SELECTION.

  DATA(o_controlador) = NEW lcl_controlador( ).

  o_controlador->iniciar_proceso(
    EXPORTING
      i_bukrs   = pa_bukrs
      i_bldat   = pa_bldat
      i_dias    = pa_dias
      i_r_kunnr = so_kunnr[] ).

END-OF-SELECTION.

  IF o_controlador->gti_saldos_cartera_clientes IS NOT INITIAL.
    o_controlador->visualizar_datos( ).
  ELSE.
    MESSAGE s002(wusl) DISPLAY LIKE 'E'.
  ENDIF.
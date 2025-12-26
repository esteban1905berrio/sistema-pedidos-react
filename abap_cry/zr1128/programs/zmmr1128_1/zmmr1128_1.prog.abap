*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1128 - Generación de Mensaje ASN PKMS
* Programa     : ZMMR1128_1
* Tipo Objeto  : Reporte
* Descripción  : Generar el ASN hacia PKMS para las entregas de las importaciones que ya están liquidadas.
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 11.11.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 11.11.2022    S4DK917739   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmr1128_1.

INCLUDE zmmr1128p_1.
INCLUDE zmmr1128o_1.
INCLUDE zmmr1128i_1.

AT SELECTION-SCREEN OUTPUT.

  LOOP AT SCREEN.
    IF screen-name = 'SO_NIMPO-LOW' OR screen-name = 'SO_VBELN-LOW'.

      screen-required = 2.
      MODIFY SCREEN.

    ENDIF.

  ENDLOOP.

INITIALIZATION.

  go_mensajes_ans_pkms = NEW zclmmr1128_mensajes_ans_pkms( ).

START-OF-SELECTION.

  go_mensajes_ans_pkms->procesar_msj_ans_hacia_pkms( i_r_vbeln = so_vbeln[]
                                                     i_r_nimpo = so_nimpo[] i_r_bukrs = so_bukrs[]
                                                     i_r_lgort = so_lgort[]
                                                   ).

END-OF-SELECTION.

  go_log = zclcxr1002_log_aplicacion=>get_instancia(  ).

  IF go_log->get_log( ) IS NOT INITIAL.
    go_log->mostrar_log( i_ventana_emergente = abap_true ).
  ELSE.
    go_mensajes_ans_pkms->visualizar_resultado( ).
  ENDIF.
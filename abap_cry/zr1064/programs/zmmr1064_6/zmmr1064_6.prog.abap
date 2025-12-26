*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1064 - Poblar temporada homologada
* Programa     : zsdr1085_1
* Tipo Objeto  : Reporte
* Descripción  : Poblar temporada homologada
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 01.05.2023
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 01.05.2023    S4DK922196   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmr1064_6.

INCLUDE zmmr1064p_6.

START-OF-SELECTION.

  IF sy-batch = abap_false AND pa_fondo = abap_true.
    zclmmr1064_gestion_temporada=>homologar_temporada_en_fondo( i_nombre_reporte = sy-repid ).
  ELSE.
    zclmmr1064_gestion_temporada=>homologar_temporada_masiva( ).
  ENDIF.

END-OF-SELECTION.
  IF pa_fondo = abap_true.
    WRITE 'Job programado.'.
  ENDIF.

  IF sy-batch = abap_true.
    WRITE 'Proceso finalizado.'.
  ENDIF.
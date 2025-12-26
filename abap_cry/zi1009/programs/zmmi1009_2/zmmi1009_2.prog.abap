*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: I1009 - Procesamiento mensajes FLEX encolados
* Programa     : ZMMI1009_2
* Tipo Objeto  : Reporte
* Descripción  : Procesamiento mensajes FLEX encolados
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 06.06.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 06.06.2022    S4DK905891   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmi1009_2 MESSAGE-ID zcx01.

INCLUDE:  zmmi1009p_2.

START-OF-SELECTION.
  go_proceso_mensajes = NEW zclmmi1009_procesa_msj_encola( i_r_ricefw = VALUE #( ( sign = 'I' option = 'EQ' low = 'I1009' ) ) ).

  IF pa_error = abap_true.
    APPEND 'E' TO gti_tipo_mensaje.
  ENDIF.

  IF pa_cola = abap_true.
    APPEND zclmmi1009_procesa_msj_encola=>gc_mensaje_encolado TO gti_tipo_mensaje.
  ENDIF.

  go_proceso_mensajes->iniciar_proceso_de_mensajes( i_r_fecha_creacion = so_fecha[]
                                                    i_r_material       = so_matnr[]
                                                    i_r_tipo_material  = so_mtart[]
                                                    i_ti_tipo_mensaje  = gti_tipo_mensaje
                                                  ).
*-------------------------------------------------------------------------------*
* Información General
*-------------------------------------------------------------------------------*
* Identificador: C1009
* Programa     : zfic1009_1
* Tipo Objeto  : Reporte de carga
* Descripción  : Carga de activos fijos
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 12.09.2021
*-------------------------------------------------------------------------------*
* Ordenes de Transporte
*-------------------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*-------------------------------------------------------------------------------*
* 01.12.2021    S4DK902544  Sebastian Londono  Creación Inicial
*-------------------------------------------------------------------------------*

REPORT zfic1009_1.

INCLUDE zfic1009p_1.
INCLUDE zfic1009cd1_1.
INCLUDE zfic1009ci1_1.

INITIALIZATION.
  lcl_carga_activo_fijo=>verificar_autorizacion( ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_arch.
  lcl_carga_activo_fijo=>matchcode( CHANGING c_arch = pa_arch ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_arch2.
  lcl_carga_activo_fijo=>matchcode( CHANGING c_arch = pa_arch2 ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_arch3.
  lcl_carga_activo_fijo=>matchcode( CHANGING c_arch = pa_arch3 ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_arch4.
  lcl_carga_activo_fijo=>matchcode( CHANGING c_arch = pa_arch4 ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_arch5.
  lcl_carga_activo_fijo=>matchcode( CHANGING c_arch = pa_arch5 ).

START-OF-SELECTION.
  go_carga_activofijo = NEW lcl_carga_activo_fijo( ).

  IF sy-batch IS INITIAL AND pa_fondo IS INITIAL AND ( pa_arch2 IS NOT INITIAL OR pa_arch3 IS NOT INITIAL OR pa_arch4 IS NOT INITIAL OR
                               pa_arch5 IS NOT INITIAL ).
    MESSAGE s028(zfi01) DISPLAY LIKE zclcxr1002_util=>gc_e.
    LEAVE LIST-PROCESSING.
  ENDIF.

  go_carga_activofijo->conf_ejecucion_fondo(
    EXPORTING
      i_fondo     = pa_fondo
      i_narchivo  = pa_arch
      i_narchivo2 = pa_arch2
      i_narchivo3 = pa_arch3
      i_narchivo4 = pa_arch4
      i_narchivo5 = pa_arch5
      i_test      = pa_test ).

  go_carga_activofijo->iniciar_proceso( EXPORTING i_arch = pa_arch  i_fondo = pa_fondo i_debug = pa_debug CHANGING c_ti_dato_activofijo = gti_datos_activofijo ).
  go_carga_activofijo->iniciar_proceso( EXPORTING i_arch = pa_arch2 i_fondo = pa_fondo i_debug = pa_debug CHANGING c_ti_dato_activofijo = gti_datos_activofijo ).
  go_carga_activofijo->iniciar_proceso( EXPORTING i_arch = pa_arch3 i_fondo = pa_fondo i_debug = pa_debug CHANGING c_ti_dato_activofijo = gti_datos_activofijo ).
  go_carga_activofijo->iniciar_proceso( EXPORTING i_arch = pa_arch4 i_fondo = pa_fondo i_debug = pa_debug CHANGING c_ti_dato_activofijo = gti_datos_activofijo ).
  go_carga_activofijo->iniciar_proceso( EXPORTING i_arch = pa_arch5 i_fondo = pa_fondo i_debug = pa_debug CHANGING c_ti_dato_activofijo = gti_datos_activofijo ).
*&---------------------------------------------------------------------*
*& Programa             ZCXC1017_1
*& RICEFW               C1017
*& Autor                SEBLONDO
*& Descripción          Cargar etiquetas
*& Fecha                05/05/2022
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 05/05/2022  S4DK908719 Sebastian Londono          Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxc1017_1.

INCLUDE zcxc1017p_1.

INITIALIZATION .

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_nach.
  CALL FUNCTION 'KD_GET_FILENAME_ON_F4'
    EXPORTING
      static    = abap_true
    CHANGING
      file_name = pa_nach.

AT SELECTION-SCREEN ON BLOCK bk1 .

*  IF  pa_ntbl IS INITIAL.
*    MESSAGE e124(/aif/mes).
*  ENDIF.

START-OF-SELECTION.

*  IF pa_ntbl(1) NE 'Z'.
*    MESSAGE s021(zcx01) DISPLAY LIKE 'E'.
*  ELSE.
    NEW zclcxc1017_cargar_etiqueta(  )->cargar_etiqueta( i_nombre_archivo = CONV #( pa_nach ) i_nombre_tabla = pa_ntbl ).

*  ENDIF.
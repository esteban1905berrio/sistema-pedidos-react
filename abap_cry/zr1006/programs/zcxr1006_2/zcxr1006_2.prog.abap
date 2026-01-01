*&---------------------------------------------------------------------*
*& Programa             zcxr1006_2
*& RICEFW               R1006
*& Autor                SEBLONDO
*& Descripción          Cargar tabla Z apartir de archivo plano
*& Fecha                5/10/2021
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 5/10/2021   S4DK904186  Sebastian Londono          Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxr1006_2.

INCLUDE: zcxr1006p_2,  ".Definicion de Parametros
         zcxr1006c_2.  ".Definicion Clases, Metodos

INITIALIZATION .

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_nach.
  CALL FUNCTION 'KD_GET_FILENAME_ON_F4'
    EXPORTING
      static    = abap_true
    CHANGING
      file_name = pa_nach.

AT SELECTION-SCREEN ON BLOCK bk1 .

  IF  pa_ntbl IS INITIAL.
    MESSAGE e124(/aif/mes).
  ENDIF.

START-OF-SELECTION.

  IF pa_ntbl(1) NE 'Z'.
    MESSAGE s021(zcx01) DISPLAY LIKE 'E'.
  ELSE.
    lcl_controlador=>cargar_tabla(
                                    i_nombre_archivo         = CONV #( pa_nach )
                                    i_nombre_tabla           = pa_ntbl
                                    i_ignorar_linea_cabecera = pa_ilc
                                    i_ignorar_mandante       = pa_iman
                                    i_test                   = pa_visu
                                    i_truncar_tabla          = pa_trtb ).
  ENDIF.
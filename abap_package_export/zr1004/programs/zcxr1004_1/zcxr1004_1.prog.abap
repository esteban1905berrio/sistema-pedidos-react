*&---------------------------------------------------------------------*
*& Programa             ZCXR1004_1
*& RICEFW               R1004
*& Autor                JMVALENC
*& Descripción          Reporte Log de Modificaciones
*& Fecha                2/02/2021
*&---------------------------------------------------------------------*
*& Control de Modificaciones
*&---------------------------------------------------------------------*
*& Fecha       OT          Autor                      Solicitud
*&------------ ----------- -------------------------  -----------------*
*& 2/02/2021   S4DK900073  Jose Miguel Valencia       Creacion
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*& Programa Principal
*&---------------------------------------------------------------------*
REPORT zcxr1004_1 MESSAGE-ID zcx01.

*&---------------------------------------------------------------------*
*& Includes
*&---------------------------------------------------------------------*
INCLUDE:
  zcxr1004p_1,  ".Definicion de Parametros
  zcxr1004c_1.  ".Definicion Clases, Metodos


*&---------------------------------------------------------------------*
*& Evento: INITIALIZATION
*&---------------------------------------------------------------------*
INITIALIZATION.
  ".Creamos instancia de la clase principal
  go_cl_principal = NEW lcl_principal( ).

*&---------------------------------------------------------------------*
*& Eventos: AT SELECTION-SCREEN.
*&---------------------------------------------------------------------*
AT SELECTION-SCREEN ON VALUE-REQUEST FOR: pa_obj.

  ".Ayuda de busqueda de los objetos de modificacion
  go_cl_principal->obtener_objectid_f4(
    CHANGING
      c_obj = pa_obj
  ).

*&---------------------------------------------------------------------*
*& Evento: START-OF-SELECTION
*&---------------------------------------------------------------------*
START-OF-SELECTION.
  ".Obtenemos los parametros
  go_cl_principal->obtener_parametros( ).

  ".Obtenemos la informacion
  go_cl_principal->obtener_datos( ).

*&---------------------------------------------------------------------*
*& Evento: END-OF-SELECTION
*&---------------------------------------------------------------------*
END-OF-SELECTION.
  go_cl_principal->mostrar_alv( ).
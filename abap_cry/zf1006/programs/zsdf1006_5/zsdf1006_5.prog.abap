*&---------------------------------------------------------------------*
*& Include zsdf1006_5
*&---------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: F1006
* Programa : zsdf1006_5
* Tipo Objeto : Programa / Include
* Descripción : Lista de empaque, basados en norma de embalaje
* Autor Prog. : Sebastian Londono
* Fecha Creac. : 27.07.2023
*----------------------------------------------------------------------*
* Órdenes de Transporte
*----------------------------------------------------------------------*
* Fecha      | CR#        | Autor             | Modificación
*----------------------------------------------------------------------*
* 27.07.2023   S4DK905274  Sebastian Londono    Versión Inicial
*----------------------------------------------------------------------*

*---------------------------------------------------------------------*
*       FORM ENTRY
*---------------------------------------------------------------------*
* Rutina de inicio para el mensaje de impresión asociado.
*---------------------------------------------------------------------*
FORM entry USING return_code us_screen ##CALLED ##PERF_NO_TYPE.

  IF cl_cos_utilities=>is_cloud( ).
    MESSAGE e006(vld) WITH nast-kschl INTO DATA(lv_message) ##NEEDED.
    CALL FUNCTION 'NAST_PROTOCOL_UPDATE' ##FM_SUBRC_OK
      EXPORTING
        msg_arbgb = sy-msgid
        msg_nr    = sy-msgno
        msg_ty    = sy-msgty
        msg_v1    = sy-msgv1
        msg_v2    = sy-msgv2
        msg_v3    = sy-msgv3
        msg_v4    = sy-msgv4
      EXCEPTIONS
        OTHERS    = 1. "#EC CI_SUBRC
    return_code = 1.
    RETURN.
  ENDIF.

  "Ejecuta el proceso principal para los mensajes de impresión
  NEW zclsdf1006_listas_empaque( )->ejecutar_mensaje_zlen(
    EXPORTING
      i_es_nast     = nast
      i_es_tnapr    = tnapr
      i_es_addr_key = addr_key
      i_us_screen   = us_screen
    IMPORTING
      e_retcode     = return_code
  ).

**********************************************************************

ENDFORM.
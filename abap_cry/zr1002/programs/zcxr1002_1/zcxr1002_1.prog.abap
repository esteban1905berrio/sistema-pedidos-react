*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1002
* Programa     : zcxr1002_1
* Tipo Objeto  : Reporte
* Descripción  : Modificar contenido IDOC
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 17.10.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.10.2021   S4DK904378    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*
REPORT zcxr1002_1.

INCLUDE zcxr1002p_1.

START-OF-SELECTION.

  DATA(gti_mensajes) = zclcxr1002_util_idoc=>modificar_segmento_idoc(   EXPORTING
                                                                          i_r_numero_idoc     = so_docnm[]
                                                                          i_nombre_campo      = CONV #( pa_nmcp )
                                                                          i_valor_campo       = CONV #( pa_vlcp )
                                                                          i_r_nombre_segmento = so_nmseg[]
                                                                          i_r_numero_segmento = so_numsg[] ).

END-OF-SELECTION.

  CALL FUNCTION 'ZCXR1002_MOSTRARALV_01'
    EXPORTING
      i_ti_datos = REF #( gti_mensajes ).
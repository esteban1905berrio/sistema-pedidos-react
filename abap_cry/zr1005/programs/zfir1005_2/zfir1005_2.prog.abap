*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1005
* Programa     : zfir1005_2
* Tipo Objeto  : Reporte
* Descripción  : Reporte historico de impuestos diferidos
*
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 09.05.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.03.2021   D01K928334    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*
REPORT zfir1005_2.

INCLUDE zfir1005p_2.
INCLUDE zfir1005c_2.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_layou.
  zclcxr1002_util=>f4_layout( CHANGING c_layout = pa_layou ).

START-OF-SELECTION.

  DATA(go_controlador) = NEW lcl_controlador(  ).

  go_controlador->cargar_historico_imp_diferido(
                                                    i_r_bukrs         = so_bukrs[]
                                                    i_r_gjahr         = so_gjahr[]
                                                    i_r_belnr         = so_belnr[]
                                                    i_r_hkont         = so_hkont[]
                                                    i_r_monat         = so_monat[]
                                                    i_r_doc_anulacion = so_abeln[]
                                                    i_r_estado        = so_estat[]
                                                ).

END-OF-SELECTION.

  go_controlador->mostrar_datos_salida(  ).
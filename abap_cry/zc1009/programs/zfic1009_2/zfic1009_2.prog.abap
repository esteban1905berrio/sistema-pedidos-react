*-------------------------------------------------------------------------------*
* Información General
*-------------------------------------------------------------------------------*
* Identificador: C1009
* Programa     : zfic1009_1
* Tipo Objeto  : Reporte de carga
* Descripción  : Log Carga de activos fijos
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 12.09.2021
*-------------------------------------------------------------------------------*
* Ordenes de Transporte
*-------------------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*-------------------------------------------------------------------------------*
* 01.12.2021    S4DK902544  Sebastian Londono  Creación Inicial
*-------------------------------------------------------------------------------*
REPORT zfic1009_2.


INCLUDE zfic1009p_2.
INCLUDE zfic1009c_2.

INITIALIZATION.
  DATA(go_controlador) = NEW lcl_controlador(  ).
  go_controlador->inicializar( ).


AT SELECTION-SCREEN.

  CASE sscrfields-ucomm.
    WHEN'FC01'.
      go_controlador->truncar_tabla_log( ).
  ENDCASE.

START-OF-SELECTION.
  zclfic1009_carga_activos_fijos=>mostrar_resultados_log( EXPORTING
                                                           i_conservar_screen   = abap_false
                                                           i_r_asset            = so_asset[]
                                                           i_r_flnam            = so_flnam[]
                                                           i_r_fecha            = so_fecha[]
                                                           i_r_hora             = so_hora[]
                                                           i_r_tipo             = so_tipo[]
                                                           i_o_grid_log_handler = go_controlador
                                                          CHANGING
                                                           c_ti_log = gti_log_icon   ).
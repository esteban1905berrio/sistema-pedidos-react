*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: I1116 - Interfaces materiales a los diferentes sistemas legados ECOMMERCE
* Programa     : ZSDI1116_1
* Tipo Objeto  : Reporte
* Descripción  : Interfaces materiales a los diferentes sistemas legados ECOMMERCE
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 14.02.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 14.02.2022    S4DK906384   Sebastian Londoño         Creación
REPORT zsdi1116_1.

INCLUDE zsdi1116p_1.


START-OF-SELECTION.

  DATA(go_controlador) = NEW zclsdi1116_envio_mat_ecommerce( ).
  IF pa_categ IS NOT INITIAL OR pa_ddic IS NOT INITIAL OR
     pa_atri IS NOT INITIAL OR pa_prod IS NOT INITIAL OR
     pa_impmt IS NOT INITIAL.

    IF pa_impmt IS NOT INITIAL AND pa_fechp IS INITIAL.
      MESSAGE s005(spm_cust_check) DISPLAY LIKE 'E'.
    ELSE.

*     Se crea el log
      go_controlador->crear_log( ).

*     Consulta de parámetro de tipo de material
      go_controlador->consulta_param_tipo_material(
        IMPORTING
          es_param = DATA(e_es_param) ).

*     Consulta de talla y color en DB
      go_controlador->consulta_talla_color(
        EXPORTING
          i_es_param  = e_es_param
        IMPORTING
          e_v_flag    = DATA(v_flag)
        CHANGING
          c_r_ean     = so_ean11[] ).

*     Validar que tenga talla y color
      CHECK v_flag IS INITIAL.

      go_controlador->generar_archivo(
        EXPORTING
          i_r_ean               = so_ean11[]
          i_r_material          = so_matnr[]
          i_r_vkorg             = so_vkorg[]
          i_r_vtweg             = so_vtweg[]
          i_fecha_precio        = pa_fechp
          i_categoria_productos = pa_categ
          i_diccionario_datos   = pa_ddic
          i_atributos_productos = pa_atri
          i_productos_e_items   = pa_prod
          i_impuesto_material   = pa_impmt
       IMPORTING
          e_datos_transferidos  = DATA(datos_transferidos) ).
    ENDIF.
  ELSE.
    MESSAGE s528(12) DISPLAY LIKE 'E'.
  ENDIF.

END-OF-SELECTION.

  IF v_flag IS NOT INITIAL.
    go_controlador->grabar_log( ).
  ENDIF.

  IF datos_transferidos IS NOT INITIAL.
    MESSAGE s039(zcx01) .
*    CALL SCREEN 100.
  ELSE.
*    MESSAGE s002(wusl) DISPLAY LIKE 'E'.
  ENDIF.
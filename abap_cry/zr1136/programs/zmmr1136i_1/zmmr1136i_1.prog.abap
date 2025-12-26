*&---------------------------------------------------------------------*
*& Include zmmr1136i_1
*&---------------------------------------------------------------------*

*&---------------------------------------------------------------------*
*&      Module  TRANSFERIR_VALORES_ENTRADA  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE transferir_valores_dyn_entrada INPUT.

  g_filas_totales = sy-loopc.
  g_indice_linea_actual   = sy-stepl + g_i_line.

  IF sy-dynnr = 300.
    MODIFY go_consumo_repuestos->g_ti_busqueda FROM go_consumo_repuestos->ges_busqueda INDEX g_indice_linea_actual.
  ELSE.
    MODIFY go_consumo_repuestos->gti_consumo FROM go_consumo_repuestos->ges_consumo INDEX g_indice_linea_actual.
  ENDIF.

ENDMODULE.
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND_0300  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE user_command_0300 INPUT.

  ok_code_temporal = ok_code.
  CLEAR: ok_code.

  CASE ok_code_temporal.

    WHEN 'PRIMERA'.

      g_i_line = 0.
      g_limite_superior_indice = 8.

    WHEN 'ULTIMA'.

      g_i_line     =  g_contador_de_lineas - g_filas_totales.
      g_i_y_v_next = 1.
      g_i_y_v_prev = 1.

    WHEN 'ABAJO'.
      " boton de paginacion abajo
      go_consumo_repuestos->desplazar_pagina_hacia_abajo( ).

    WHEN 'ARRIBA'.
      " boton de paginacion arriba
      go_consumo_repuestos->desplazar_pagina_hacia_arriba( ).

    WHEN 'CONTINUAR' OR 'ENTER'.

      go_consumo_repuestos->consultar_material_pieza( ).

    WHEN 'ATRAS'.

      LEAVE PROGRAM.

    WHEN 'REINICIAR'.

      go_consumo_repuestos->reiniciar_valores_dynpro_300( ).

    WHEN 'NUMPARTE'.

      go_consumo_repuestos->reiniciar_valores_dynpro_300( ).


  ENDCASE.

ENDMODULE.                 " USER_COMMAND_0300  INPUT
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND_0400  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE user_command_0400 INPUT.

  ok_code_temporal = ok_code.
  CLEAR: ok_code.

  CASE ok_code_temporal.

    WHEN 'PRIMERA'.

      g_i_line = 0.
      g_limite_superior_indice = 8.
      g_i_y_v_prev = 0.

    WHEN 'ULTIMA'.
      g_i_line =  g_contador_de_lineas - g_filas_totales.
      g_i_y_v_next = 1.
      g_i_y_v_prev = 1.

    WHEN 'ABAJO'.

      go_consumo_repuestos->desplazar_pagina_hacia_abajo( ).

    WHEN 'ARRIBA'.

      go_consumo_repuestos->desplazar_pagina_hacia_arriba( ).

    WHEN 'CONTINUAR' OR 'ENTER'.
      "Dynpro de consumo
      CALL SCREEN 0500.

    WHEN 'ATRAS'.

      LEAVE TO SCREEN 0.

    WHEN 'SALIR'.

      LEAVE PROGRAM.

  ENDCASE.

ENDMODULE.                 " USER_COMMAND_0400  INPUT
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND_0500  INPUT
*&---------------------------------------------------------------------*
*       Botonse dynprp de consumo
*----------------------------------------------------------------------*
MODULE user_command_0500 INPUT.

  ok_code_temporal = ok_code.
  CLEAR: ok_code.

  CASE ok_code_temporal.
    WHEN 'GRABAR'.

      go_consumo_repuestos->realizar_consumo_de_mercancia(  ).

    WHEN 'ENTER'.

      GET CURSOR FIELD g_nombre_campo_cursor.

      CASE g_nombre_campo_cursor.

        WHEN 'GO_CONSUMO_REPUESTOS->G_CEDULA'.

          IF go_consumo_repuestos->g_cedula IS NOT INITIAL.

            go_consumo_repuestos->realizar_consumo_de_mercancia(  ).

          ELSE.
            g_posicion_cursor_pant_consumo = 3.
            go_consumo_repuestos->mostrar_mensaje( i_es_mensaje = VALUE #( number = '371' ) ).

          ENDIF.
        WHEN 'GO_CONSUMO_REPUESTOS->G_CANTIDAD_CONSUMO'.

          IF go_consumo_repuestos->g_menge => go_consumo_repuestos->g_cantidad_consumo AND  go_consumo_repuestos->g_cantidad_consumo > 0.
            g_posicion_cursor_pant_consumo = 2.
            EXIT.
          ELSE.
            "Consumo no válido
            go_consumo_repuestos->mostrar_mensaje( i_es_mensaje = VALUE #( number = '374' ) ).
            EXIT.
          ENDIF.

        WHEN 'GO_CONSUMO_REPUESTOS->G_CENTRO_DE_COSTO'.

          IF go_consumo_repuestos->g_centro_de_costo IS NOT INITIAL.
            g_posicion_cursor_pant_consumo = 3.
            EXIT.
          ELSE.
            "El centro de costo debe ir lleno
            go_consumo_repuestos->mostrar_mensaje( i_es_mensaje = VALUE #( number = '375' ) ).
            EXIT.
          ENDIF.

      ENDCASE.

    WHEN 'ATRAS'.
      LEAVE TO SCREEN 0.
    WHEN 'SALIR'.
      LEAVE PROGRAM.
  ENDCASE.


ENDMODULE.                 " USER_COMMAND_0500  INPUT
*&---------------------------------------------------------------------*
*&      Module  ATRAS_300  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE atras_300 INPUT.

  ok_code_temporal = ok_code.
  CLEAR: ok_code.

  IF ok_code_temporal EQ 'ATRAS'.
    CLEAR go_consumo_repuestos->g_ti_busqueda.
    LEAVE PROGRAM.
  ENDIF. .

ENDMODULE.                 " ATRAS_300  INPUT
*&---------------------------------------------------------------------*
*&      Module  ATRAS_400  INPUT
*&---------------------------------------------------------------------*
*       Salida
*----------------------------------------------------------------------*
MODULE atras_400 INPUT.

  IF  sy-ucomm EQ 'SALIR'.

    LEAVE PROGRAM.

  ENDIF.
ENDMODULE.                 " ATRAS_400  INPUT
*&---------------------------------------------------------------------*
*&      Module  ATRAS_500  INPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE atras_500 INPUT.

  IF sy-ucomm EQ 'SALIR'.

    LEAVE PROGRAM.

  ENDIF.

ENDMODULE.                 " ATRAS_500  INPUT
*&---------------------------------------------------------------------*
*&      Module  USER_COMMAND_0600  INPUT
*&---------------------------------------------------------------------*
*       Eventos Dynpro 600
*----------------------------------------------------------------------*
MODULE user_command_0600 INPUT.

  CASE sy-ucomm.
    WHEN 'ACEPTAR'.
      LEAVE TO SCREEN 0.
    WHEN OTHERS.
      LEAVE TO SCREEN 0.

  ENDCASE.

ENDMODULE.                 " USER_COMMAND_0600  INPUT
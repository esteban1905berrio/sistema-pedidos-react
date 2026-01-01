*&---------------------------------------------------------------------*
*& Include zmmr1136o_1
*&---------------------------------------------------------------------*

*----------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*&      Module  STATUS_0300  OUTPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE status_0300 OUTPUT.
  SET PF-STATUS 'GS_0001 '.
ENDMODULE.                 " STATUS_0300  OUTPUT

MODULE validar_tabla_consumo OUTPUT.

  go_consumo_repuestos->validar_tabla_consumo(  ).

ENDMODULE.

*&---------------------------------------------------------------------*
*&      Module  ARRIBA_ABAJO  OUTPUT
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
MODULE arriba_abajo OUTPUT.

  DATA : y_v_div  TYPE i,
         y_v_d    TYPE f,
         y_v_temp TYPE i.

  IF sy-dynnr = 300 .
    g_contador_de_lineas = lines( go_consumo_repuestos->g_ti_busqueda ).
  ELSE.
    g_contador_de_lineas = lines( go_consumo_repuestos->gti_consumo ).
  ENDIF.

  y_v_d = g_contador_de_lineas / 8.
  g_i_y_v_limit = ceil( y_v_d ).
  y_v_temp = g_i_y_v_limit - 1.

  IF g_contador_de_lineas LE 8.

    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ABAJO' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ULTIMA' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ARRIBA' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_PRIMERA' ).
  ELSEIF g_i_y_v_next  = g_i_y_v_limit .
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ABAJO' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ULTIMA' ).
    go_consumo_repuestos->hacer_visible_campo_pantalla( i_nombre_componente_pantalla = 'BT_ARRIBA' ).
    go_consumo_repuestos->hacer_visible_campo_pantalla( i_nombre_componente_pantalla = 'BT_PRIMERA' ).
  ELSEIF g_i_y_v_prev IS INITIAL.
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ARRIBA' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_PRIMERA' ).
  ELSEIF g_i_y_v_next GT g_i_y_v_limit.
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ABAJO' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ULTIMA' ).
  ELSEIF y_v_temp = g_i_y_v_next.
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ABAJO' ).
    go_consumo_repuestos->ocultar_campo_pantalla( i_nombre_componente_pantalla = 'BT_ULTIMA' ).
  ENDIF.

ENDMODULE.                 " ARRIBA_ABAJO  OUTPUT

MODULE transferir_valores_dynp_salida OUTPUT.

  go_consumo_repuestos->transferir_valores_dynp_salida(  ).

ENDMODULE.                 " TRANS_BUSQUEDA_SALIDA  OUTPUT
*&---------------------------------------------------------------------*
*&      Module  STATUS_0400  OUTPUT
*&---------------------------------------------------------------------*
*       Status dynpro 0400
*----------------------------------------------------------------------*
MODULE status_0400 OUTPUT.
  SET PF-STATUS 'GS_0001 '.
ENDMODULE.                 " STATUS_0400  OUTPUT
*&---------------------------------------------------------------------*
*&      Module  STATUS_0500  OUTPUT
*&---------------------------------------------------------------------*
*       Status dynpro 0500
*----------------------------------------------------------------------*
MODULE status_0500 OUTPUT.
  SET PF-STATUS 'GS_0001 '.

  CASE g_posicion_cursor_pant_consumo.
    WHEN 1.
      LOOP AT SCREEN.
        IF screen-name = 'GO_CONSUMO_REPUESTOS->G_CANTIDAD_CONSUMO'.
          SET CURSOR FIELD 'GO_CONSUMO_REPUESTOS->G_CANTIDAD_CONSUMO'.
          MODIFY SCREEN.
          EXIT.
        ENDIF.

      ENDLOOP.
    WHEN 2.
      LOOP AT SCREEN.
        IF screen-name = 'GO_CONSUMO_REPUESTOS->G_CENTRO_DE_COSTO'.
          SET CURSOR FIELD 'GO_CONSUMO_REPUESTOS->G_CENTRO_DE_COSTO'.
          MODIFY SCREEN.
          EXIT.
        ENDIF.


      ENDLOOP.
    WHEN 3.
      LOOP AT SCREEN.
        IF screen-name = 'GO_CONSUMO_REPUESTOS->G_CEDULA'.
          SET CURSOR FIELD 'GO_CONSUMO_REPUESTOS->G_CEDULA'.
          MODIFY SCREEN.
          CLEAR g_posicion_cursor_pant_consumo.
          EXIT.
        ENDIF.


      ENDLOOP.
  ENDCASE.

ENDMODULE.                 " STATUS_0500  OUTPUT
*&---------------------------------------------------------------------*
*&      Module  ENCRIPCEDULA  OUTPUT
*&---------------------------------------------------------------------*
*       Encriptamos la cedula al usuario
*----------------------------------------------------------------------*
MODULE aplicar_mascara_campo_cedula OUTPUT.

  LOOP AT SCREEN.
    IF screen-name = 'GO_CONSUMO_REPUESTOS->G_CEDULA'.
      screen-invisible = '1'.
      MODIFY SCREEN.
      EXIT.
    ENDIF.
  ENDLOOP.

ENDMODULE.                 " ENCRIPCEDULA  OUTPUT
*&---------------------------------------------------------------------*
*&      Module  CHECK_SELEC  OUTPUT
*&---------------------------------------------------------------------*
*       Activamos y desactivamos los check de selección
*----------------------------------------------------------------------*
MODULE activar_check_de_buqueda OUTPUT.

*.Se desactiva el check si no hay valores de busqueda
  IF go_consumo_repuestos->g_ti_cbusqueda IS INITIAL.

    LOOP AT SCREEN.
      IF screen-name = 'GO_CONSUMO_REPUESTOS->GES_BUSQUEDA-CHECK'.
        screen-active    = '0'.
        screen-invisible = '1'.
        MODIFY SCREEN.
        EXIT.
      ENDIF.
    ENDLOOP.

  ENDIF.

ENDMODULE.                 " CHECK_SELEC  OUTPUT
*&---------------------------------------------------------------------*
*&      Module  STATUS_0600  OUTPUT
*&---------------------------------------------------------------------*
*       Status Dynpro 0600
*----------------------------------------------------------------------*
MODULE status_0600 OUTPUT.

  SET PF-STATUS 'GS_0002'.
  SET TITLEBAR 'GT_0001'.

ENDMODULE.                 " STATUS_0600  OUTPUT
*&---------------------------------------------------------------------*
*& Module AGREGAR_FILA_BUSQUEDA OUTPUT
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
MODULE agregar_fila_busqueda OUTPUT.
  "Agregar fila vacia para evitar DUMP
  IF go_consumo_repuestos->g_ti_busqueda IS INITIAL.
    APPEND INITIAL LINE TO go_consumo_repuestos->g_ti_busqueda.
  ENDIF.
ENDMODULE.
*&---------------------------------------------------------------------*
*& Module REMOVER_FILA_BUSQUEDA OUTPUT
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
MODULE remover_fila_busqueda OUTPUT.
  "Eliminar filas vacias
  DELETE go_consumo_repuestos->g_ti_busqueda WHERE lgort = space AND charg = space.
ENDMODULE.
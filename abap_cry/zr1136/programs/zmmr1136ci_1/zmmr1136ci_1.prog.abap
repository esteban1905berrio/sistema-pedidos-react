*&---------------------------------------------------------------------*
*& Include zmmr1136ci_1
*&---------------------------------------------------------------------*

CLASS lcl_controlador IMPLEMENTATION.

  METHOD iniciar_proceso.

    go_log = zclcxr1002_log_aplicacion=>get_instancia( ).

    cargar_parametros( ).

    CALL SCREEN 300.

  ENDMETHOD.


  METHOD cargar_parametros.

    CONSTANTS: c_parametro_material TYPE zzedidparam VALUE 'MTART',
               c_parametro_centro   TYPE zzedidparam VALUE 'WERKS'.

    DATA(o_parametros) = NEW zclcxr1000_parametros( i_modulo = 'MM' i_ricefw = 'R1136'  ).

    g_centro_configurado   = VALUE #(  o_parametros->gti_parametros[ idparam = c_parametro_centro ]-low OPTIONAL ).

    gr_material_configurado = VALUE #( FOR es_parametro IN o_parametros->gti_parametros WHERE ( idparam = c_parametro_material )
                                                ( CORRESPONDING #( es_parametro MAPPING option = opti ) )
                                            ).

    IF g_centro_configurado IS INITIAL.
      mostrar_mensaje( EXPORTING i_es_mensaje = VALUE #( id = gc_msj_id number = '315' msgv1 = c_parametro_centro )  ).
    ENDIF.

    IF gr_material_configurado IS INITIAL.
      mostrar_mensaje( EXPORTING i_es_mensaje = VALUE #( id = gc_msj_id number = '315' msgv1 = c_parametro_material )  ).
    ENDIF.

  ENDMETHOD.


  METHOD mostrar_mensaje.

    DATA es_mensaje    TYPE g_tp_mensaje.

    es_mensaje = CORRESPONDING #( i_es_mensaje ).

    IF es_mensaje-id IS INITIAL.
      es_mensaje-id = gc_msj_id.
    ENDIF.

    IF es_mensaje-screen IS INITIAL.
      MOVE '0999' TO es_mensaje-screen.
    ENDIF.

    CALL FUNCTION 'CALL_MESSAGE_SCREEN'
      EXPORTING
        i_msgid          = es_mensaje-id
        i_lang           = sy-langu
        i_msgno          = es_mensaje-number
        i_msgv1          = es_mensaje-msgv1
        i_msgv2          = es_mensaje-msgv2
        i_msgv3          = es_mensaje-msgv3
        i_msgv4          = es_mensaje-msgv4
        i_message_screen = '0999'
        i_non_lmob_envt  = 'X'
      IMPORTING
        o_answer         = es_mensaje-answer
      EXCEPTIONS
        invalid_message1 = 1
        OTHERS           = 2.

  ENDMETHOD.

  METHOD desplazar_pagina_hacia_abajo.

    y_lv_d = g_contador_de_lineas / 8.
    y_lv_div = ceil( y_lv_d ).
    y_curr_p_num = y_lv_div * 8.
    y_v_index = g_i_y_v_next + 1.
    IF g_i_y_v_next < y_lv_div.
      g_i_y_v_next = g_i_y_v_next + 1.
    ELSE.
      g_i_y_v_next = y_lv_div.
    ENDIF.
    g_i_y_v_prev = g_i_y_v_next.
    IF g_i_y_v_next <> y_lv_div.
      g_limite_superior_indice = g_contador_de_lineas - 8 *  g_i_y_v_next.
      IF g_limite_superior_indice > 8.
        g_limite_superior_indice = 8 * g_i_y_v_next.
      ENDIF.
      g_limite_inferior_ndice = 1.
      g_i_line = g_i_line + g_filas_totales.
      g_limite_de_filas = y_curr_p_num - g_filas_totales.
      IF g_i_line > g_limite_de_filas.
        g_i_line = g_limite_de_filas.
      ENDIF.
    ELSE.
      g_i_y_v_next = g_i_y_v_next - 1.
    ENDIF.

  ENDMETHOD.

  METHOD desplazar_pagina_hacia_arriba.

    DATA : y_v_index    TYPE sy-index,
           y_lv_d       TYPE f,
           y_lv_div     TYPE i,
           y_curr_p_num TYPE i,
           g_i_linestep TYPE i. "Obtener Linea del step loop

    g_limite_superior_indice = 8 * g_i_y_v_next.
    IF g_limite_inferior_ndice < 0.
      g_limite_inferior_ndice = 1.
    ENDIF.
    IF g_i_y_v_next > 0.
      g_i_y_v_next = g_i_y_v_next - 1.
    ELSE.
      g_i_y_v_next = 0.
    ENDIF.
    g_i_y_v_prev = g_i_y_v_next.
    IF g_i_line NE 0 AND y_curr_p_num GT 8.
      g_i_line = g_i_y_v_next * 8.
    ELSE.
      g_i_line = 0.
      y_v_index = g_i_y_v_next - 1.
    ENDIF.
    IF g_i_line < 0.
      g_i_line = 0.
    ENDIF.

  ENDMETHOD.

  METHOD consultar_material_pieza.

    go_log->remover_mensage( ).

    IF g_matnr IS NOT INITIAL.

      IF gb_numparte IS INITIAL.

        obtener_material( i_matnr = g_matnr ).

      ELSEIF gb_numparte IS NOT INITIAL.

        obtener_material_desde_pieza( i_matnr = g_matnr ).

      ENDIF.

      "Limpiamos el campo del material
      CLEAR: g_matnr.

      asignar_valores_consultados( ).

    ELSE.

      limpiar_pant_busqueda_material(  ).

      "Material no existe en el maestro.
      mostrar_mensaje( i_es_mensaje = VALUE #( id = 'WJ' number = '023' msgv1 = |{ g_matnr ALPHA = OUT }| ) ).

    ENDIF.

    go_log->remover_mensage( ).
  ENDMETHOD.


  METHOD obtener_material.

    DATA: numero_material TYPE matnr.

    CHECK i_matnr IS NOT INITIAL.

    CALL FUNCTION 'CONVERSION_EXIT_MATN1_INPUT'
      EXPORTING
        input        = i_matnr
      IMPORTING
        output       = numero_material
      EXCEPTIONS
        length_error = 1
        OTHERS       = 2.

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
              WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

    CLEAR:  g_ti_busqueda, ges_busqueda, ges_mara.

    "Obtenemos el material a procesar por el número de material
    SELECT SINGLE
      FROM mara AS _material
      INNER JOIN makt AS _desc_material ON
      _material~matnr = _desc_material~matnr
      FIELDS _material~matnr, meins, matkl, _desc_material~maktx
      WHERE ( _material~matnr EQ @numero_material
        OR    _material~bismt EQ @numero_material )
        AND _material~mtart IN @gr_material_configurado
        AND _desc_material~spras = @sy-langu
      INTO @ges_mara.

    IF sy-subrc IS INITIAL.

      CLEAR: g_ti_mchb.

      IF ges_mara IS NOT INITIAL.

        g_maktx = ges_mara-maktx.
        "Convertimos a formato externo
        CALL FUNCTION 'CONVERSION_EXIT_MATN1_OUTPUT'
          EXPORTING
            input  = ges_mara-matnr
          IMPORTING
            output = g_vmatnr.

      ENDIF.

      "Obtenemos los Datos de almacén para el material
      SELECT matnr, werks, lgort, lgpbe, labst
       FROM mard
       WHERE matnr EQ @ges_mara-matnr
         AND werks EQ @g_centro_configurado
       INTO CORRESPONDING FIELDS OF TABLE @g_ti_mard.

      IF g_ti_mard IS NOT INITIAL .

        "Obtenemos los lotes
        SELECT matnr, werks, lgort, charg, clabs
          FROM mchb
          FOR ALL ENTRIES IN @g_ti_mard
          WHERE matnr EQ @g_ti_mard-matnr
            AND werks EQ @g_ti_mard-werks
            AND lgort EQ @g_ti_mard-lgort
        INTO TABLE @g_ti_mchb.

        "No existe en MCHB, se asigna  Stock valorado de libre utilización de MARD
        IF sy-subrc NE 0.

          "No se encontraron lote para el material & en el almacén &.
          go_log->set_es_log( i_es = VALUE #( id = 'M7' number = '821' type = 'E' message_v1 = g_matnr message_v2 = g_centro_configurado )  ).

          CLEAR: g_matnr.
*                ,g_maktx.  ".comentario Jose Miguel Valencia 01.06.2023

          g_ti_mchb = VALUE #( FOR es_mard IN g_ti_mard
                               ( matnr = es_mard-matnr werks = es_mard-werks charg = gc_lote_en_blanco lgort = es_mard-lgort clabs = es_mard-labst )
                             ).

        ENDIF.
      ELSE.
        "El material no tiene existencias en centro / almacen
        go_log->set_es_log( i_es = VALUE #( id = gc_msj_id number = '317' type = 'E' message_v1 = |{ g_matnr ALPHA = OUT }| message_v2 = g_centro_configurado )  ).
      ENDIF.

    ELSE.

      CLEAR: g_maktx.
      "Material no existe en el maestro.
      go_log->set_es_log( i_es = VALUE #( id = 'WJ' number = '023' type = 'E' message_v1 = |{ g_matnr ALPHA = OUT }| )  ).

    ENDIF.

  ENDMETHOD.


  METHOD obtener_material_desde_pieza.

    CLEAR:  g_ti_busqueda, ges_busqueda, ges_mara.

    SELECT SINGLE
      FROM ztmmr1136_1 AS _parte
      INNER JOIN mara AS _material
      ON _material~matnr = _parte~matnr
      FIELDS _material~matnr, _parte~nroparte
      WHERE _parte~nroparte EQ @i_matnr
        AND mtart IN @gr_material_configurado
      INTO @DATA(es_numero_material).

    IF sy-subrc IS INITIAL.
      obtener_material( i_matnr = es_numero_material-matnr ).
    ELSE.
      "El número de pieza de fabricante no existe.
      go_log->set_es_log( i_es = VALUE #( id = gc_msj_id number = '369' type = 'E' message_v1 = |{ g_matnr ALPHA = OUT }| )  ).
    ENDIF.
  ENDMETHOD.


  METHOD asignar_valores_consultados.

    LOOP AT g_ti_mchb ASSIGNING FIELD-SYMBOL(<fs_es_mchb>).

      ges_busqueda = CORRESPONDING #( VALUE #( g_ti_mard[ matnr = <fs_es_mchb>-matnr
                                                          werks = <fs_es_mchb>-werks
                                                          lgort = <fs_es_mchb>-lgort ] OPTIONAL ) ).

      ges_busqueda = VALUE #( BASE ges_busqueda charg = <fs_es_mchb>-charg clabs = <fs_es_mchb>-clabs ).

      APPEND ges_busqueda TO g_ti_busqueda.

      IF <fs_es_mchb>-charg = gc_lote_en_blanco.
        CLEAR <fs_es_mchb>-charg.
      ENDIF.

    ENDLOOP.

    IF g_ti_busqueda IS INITIAL AND go_log->get_log( ) IS NOT INITIAL.

      DATA(ti_log) = go_log->get_log( ).

      mostrar_mensaje( i_es_mensaje = CORRESPONDING #( ti_log[ 1 ] MAPPING msgv1 = message_v1 msgv2 = message_v2
                                                                           msgv3 = message_v3 msgv4 = message_v4 )  ).
      reiniciar_valores_dynpro_300( ).

    ENDIF.

    DELETE g_ti_busqueda WHERE lgort = space AND charg = space.

    SORT g_ti_busqueda DESCENDING BY lgort charg.
    DELETE ADJACENT DUPLICATES FROM g_ti_busqueda COMPARING lgort charg.

    "Obtenemos una copia con todos los datos
    g_ti_cbusqueda = g_ti_busqueda.

  ENDMETHOD.

  METHOD reiniciar_valores_dynpro_300.

    CLEAR:  g_ti_busqueda, g_ti_cbusqueda,
             ges_busqueda,
             g_matnr,
             g_maktx,
             g_cmatnr,
             g_vmatnr,
             g_cmatnr,
             gti_consumo.

  ENDMETHOD.


  METHOD ocultar_campo_pantalla.

    LOOP AT SCREEN.
      IF screen-name = i_nombre_componente_pantalla.
        screen-active    = '0'.
        screen-invisible = '1'.
        MODIFY SCREEN.
        EXIT.
      ENDIF.
    ENDLOOP.

  ENDMETHOD.


  METHOD hacer_visible_campo_pantalla.

    LOOP AT SCREEN.
      IF screen-name = i_nombre_componente_pantalla.
        screen-active = '1'.
        MODIFY SCREEN.
        EXIT.
      ENDIF.
    ENDLOOP.

  ENDMETHOD.


  METHOD transferir_valores_dynp_salida.

    IF ges_busqueda-check IS NOT INITIAL.

      consultar_desde_seleccion( ).

    ENDIF.

    g_indice_linea_actual = sy-stepl + g_i_line.

    IF sy-dynnr = 300 .
      READ TABLE g_ti_busqueda INTO ges_busqueda INDEX g_indice_linea_actual.
    ELSE.
      READ TABLE gti_consumo INTO ges_consumo INDEX g_indice_linea_actual.
    ENDIF.

  ENDMETHOD.


  METHOD consultar_desde_seleccion.

    READ TABLE g_ti_cbusqueda INTO ges_busqueda INDEX  g_posicion_cursor.

    g_menge = ges_busqueda-clabs.

    CLEAR ges_busqueda-check.

    MODIFY g_ti_busqueda FROM ges_busqueda INDEX g_posicion_cursor.
    "datos para los text box de la ventana consumo
    READ TABLE g_ti_mard INTO DATA(es_mard)
               WITH TABLE KEY matnr = ges_busqueda-matnr
                              werks = ges_busqueda-werks
                              lgort = ges_busqueda-lgort.

    IF sy-subrc IS INITIAL.

      g_werks = es_mard-werks.
      g_lgort = es_mard-lgort.
      g_lgpbe = es_mard-lgpbe.

      obtener_pedidos_pendientes( ).

      "dynpro de visualizacion previa a consumo
      CALL SCREEN 400.

    ENDIF.
  ENDMETHOD.


  METHOD obtener_pedidos_pendientes.

    DATA: r_almacen TYPE RANGE OF ekpo-lgort.

    CLEAR: gti_consumo, ges_consumo.

    IF ges_busqueda-charg IS NOT INITIAL AND
       ges_busqueda-charg NE gc_lote_en_blanco.
      zclcxr1002_util=>crear_rango(
        EXPORTING
          i_low      = ges_busqueda-lgort
        CHANGING
          c_ti_rango = r_almacen ).
    ENDIF.

    "documentos correspondientes al material
    SELECT ebeln, ebelp, loekz, matnr, werks, lgort, matkl
      FROM ekpo
      WHERE loekz EQ @space
        AND matnr EQ @ges_busqueda-matnr
        AND werks EQ @ges_busqueda-werks
        AND lgort IN @r_almacen
        AND matkl EQ @ges_mara-matkl
        AND elikz EQ @space
      INTO TABLE @g_ti_ekpo.

    IF g_ti_ekpo IS NOT INITIAL.

      "Consultar pedidos
      SELECT ebeln, eindt, menge, wemng
        FROM eket
        FOR ALL ENTRIES IN @g_ti_ekpo
        WHERE ebeln EQ @g_ti_ekpo-ebeln
          AND ebelp EQ @g_ti_ekpo-ebelp
        INTO TABLE @g_ti_eket.

      IF sy-subrc EQ 0.

        LOOP AT g_ti_eket INTO DATA(es_eket).
          "Cantidad de los pedidos pendientes
          es_eket-menge = es_eket-menge - es_eket-wemng.
          "Validamos que este pendiente
          IF es_eket-menge GT 0.

            ges_consumo = CORRESPONDING #( es_eket ).

            "Fecha formato externo
            CALL FUNCTION 'CONVERT_DATE_TO_EXTERNAL'
              EXPORTING
                date_internal            = es_eket-eindt
              IMPORTING
                date_external            = ges_consumo-eindt
              EXCEPTIONS
                date_internal_is_invalid = 1
                OTHERS                   = 2.

            APPEND ges_consumo TO gti_consumo.

          ENDIF.
        ENDLOOP.

      ENDIF.

    ENDIF.

  ENDMETHOD.


  METHOD validar_tabla_consumo.

    DATA es_consumo LIKE LINE OF gti_consumo.

    IF gti_consumo IS INITIAL.

      "Fecha formato externo
      CALL FUNCTION 'CONVERT_DATE_TO_EXTERNAL'
        EXPORTING
          date_internal            = sy-datum
        IMPORTING
          date_external            = es_consumo-eindt
        EXCEPTIONS
          date_internal_is_invalid = 1
          OTHERS                   = 2.

      APPEND es_consumo TO gti_consumo.

    ENDIF.

  ENDMETHOD.


  METHOD realizar_consumo_de_mercancia.

    "Consultamos la Cédula
    SELECT SINGLE taxnum
      FROM dfkkbptaxnum
      WHERE taxnum EQ @g_cedula
      INTO @g_cedula.

    IF sy-subrc EQ 0.

      converti_cedula( ).
      realizar_movimiento_mercancias(  ).

    ELSE.
      "Cedula no valida
      mostrar_mensaje( i_es_mensaje = VALUE #( number = '371' msgv1 = sy-uname ) ).
    ENDIF.

  ENDMETHOD.


  METHOD converti_cedula.

    DATA l_i_cont TYPE i.

    SHIFT g_cedula LEFT DELETING LEADING '0'.

    REPLACE ALL OCCURRENCES OF '.' IN g_cedula WITH ''.

    l_i_cont = strlen( g_cedula ).

    IF l_i_cont GT 10.

      g_cedula = g_cedula+2(10).

    ENDIF.

  ENDMETHOD.


  METHOD realizar_movimiento_mercancias.

    DATA: es_datos_cabecera         TYPE bapi2017_gm_head_01,
          es_codigos                TYPE bapi2017_gm_code,
          es_datos_cabecera_retorno TYPE bapi2017_gm_head_ret,
          numero_documento_material TYPE bapi2017_gm_head_ret-mat_doc,
          es_posicion               TYPE bapi2017_gm_item_create,
          es_msg_retorno            TYPE bapiret2,
          ti_datos_posicion         TYPE TABLE OF bapi2017_gm_item_create,
          ti_msg_retorno            TYPE TABLE OF bapiret2.

    es_datos_cabecera = VALUE #( pstng_date = sy-datum doc_date = sy-datum ref_doc_no_long = g_cedula ).
    es_codigos-gm_code         = '06'.

    es_posicion = VALUE #( material =  ges_mara-matnr plant = g_werks
                           stge_loc = g_lgort move_type = '201' entry_qnt = g_cantidad_consumo
                           entry_uom = ges_mara-meins batch = COND #( WHEN ges_busqueda-charg = gc_lote_en_blanco THEN
                                                                        space
                                                                      ELSE
                                                                        ges_busqueda-charg
                                                                    )
                         ).

    "centro de costo a formato interno
    CALL FUNCTION 'CONVERSION_EXIT_ALPHA_INPUT'
      EXPORTING
        input  = g_centro_de_costo
      IMPORTING
        output = es_posicion-costcenter.

    APPEND es_posicion TO ti_datos_posicion.

    CALL FUNCTION 'BAPI_GOODSMVT_CREATE'
      EXPORTING
        goodsmvt_header  = es_datos_cabecera
        goodsmvt_code    = es_codigos
      IMPORTING
        goodsmvt_headret = es_datos_cabecera_retorno
        materialdocument = numero_documento_material
      TABLES
        goodsmvt_item    = ti_datos_posicion
        return           = ti_msg_retorno.

    IF ti_msg_retorno IS INITIAL.

      CALL FUNCTION 'BAPI_TRANSACTION_COMMIT'
        EXPORTING
          wait = abap_true.

      g_posicion_cursor_pant_consumo = 2.

      mostrar_mensaje( i_es_mensaje = VALUE #( id = gc_msj_id number = '316'
                                               msgv1 = numero_documento_material
                                             )
                     ).
      limpiar_pantalla_consumo(  ).
      limpiar_pant_busqueda_material(  ).
      g_matnr = ges_mara-matnr.
      consultar_material_pieza( ).

      LEAVE TO SCREEN 300.

    ELSE.

      READ TABLE ti_msg_retorno INTO es_msg_retorno INDEX 1.

      "Error proveniente de la MIGO
      mostrar_mensaje( i_es_mensaje = CORRESPONDING #( es_msg_retorno MAPPING msgv1 = message_v1
                                                                           msgv2 = message_v2
                                                                           msgv3 = message_v3
                                                                           msgv4 = message_v4
                                                      )
                     ).

    ENDIF.

  ENDMETHOD.


  METHOD limpiar_pant_busqueda_material.
    CLEAR: g_maktx, g_cmatnr, g_vmatnr, g_ti_busqueda, ges_busqueda,
           g_posicion_cursor, g_limite_inferior_ndice, g_limite_superior_indice.
  ENDMETHOD.


  METHOD limpiar_pantalla_consumo.
    CLEAR: g_cantidad_consumo, g_cedula, g_centro_de_costo.
  ENDMETHOD.

ENDCLASS.
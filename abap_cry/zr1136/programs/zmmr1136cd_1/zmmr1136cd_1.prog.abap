*&---------------------------------------------------------------------*
*& Include zmmr1136cd_1
*&---------------------------------------------------------------------*

CLASS lcl_controlador DEFINITION.

  PUBLIC SECTION.

    CONSTANTS: gc_msj_id TYPE bapiret2-id VALUE 'ZMM01',
               gc_lote_en_blanco TYPE char10 VALUE ' * * * * '.

    TYPES: "Mensajes Dynpro 0600
      BEGIN OF g_tp_mensaje,
        id     TYPE t100-arbgb,  "Área funcional
        number TYPE t100-msgnr,  "Número de mensaje
        msgv1  TYPE sprot_u-var1, "Variable de mensaje
        msgv2  TYPE sprot_u-var2, "Variable de mensaje
        msgv3  TYPE sprot_u-var3, "Variable de mensaje
        msgv4  TYPE sprot_u-var4, "Variable de mensaje
        screen TYPE sy-dynnr,    "Número del dynpro actual
        answer TYPE char1,
      END OF g_tp_mensaje,
      BEGIN OF g_tp_busqueda,
        matnr    TYPE matnr,       "Número de material
        werks    TYPE werks_d,     "Centro
        lgort    TYPE lgort_d,     "Almacén
        charg    TYPE charg_d,     "Número de lote
        clabs    TYPE mchb-clabs , "Stock valorado de libre Util.
        check(1) TYPE c ,           "Radio
      END OF g_tp_busqueda,

      " Definicion de tabla para el step loop de consumo
      BEGIN OF g_tp_consumo,
        ebeln TYPE ebeln,        "Número del documento de compras
        eindt TYPE char10,
        menge TYPE etmen,        "Cantidad de reparto
      END OF g_tp_consumo,

      " Tipo de Materiales
      BEGIN OF g_tp_mara,
        matnr TYPE matnr,       "Número de material
        meins TYPE mara-meins,  "Unidad de medida base
        matkl TYPE matkl,       "Grupo de artículos
        maktx TYPE maktx,      "Texto breve de material
      END OF   g_tp_mara,

      " Tipo de datos de almacén para el material
      BEGIN OF g_tp_mard,
        matnr TYPE matnr,       "Número de material
        werks TYPE werks_d,     "Centro
        lgort TYPE lgort_d,     "Almacén
        lgpbe TYPE lgpbe,       "Ubicación
        labst TYPE mard-labst,  " Stock valorado de libre utilización
      END OF   g_tp_mard,

      "Stocks de lotes
      BEGIN OF g_tp_mchb,
        matnr TYPE matnr,       "Número de material
        werks TYPE werks_d,     "Centro
        lgort TYPE lgort_d,     "almacén
        charg TYPE charg_d,     "Número de lote
        clabs TYPE labst,       "Stock valorado de libre utilización
      END OF   g_tp_mchb,

      "Posición del documento de compras
      BEGIN OF g_tp_ekpo,
        ebeln TYPE ebeln,       "Número del documento de compras
        ebelp TYPE ebelp,       "posición del documento de compras
        loekz TYPE loekz,       "Clase de activos fijos para borrado
        matnr TYPE matnr,       "Número de material
        werks TYPE werks,       "Centro
        lgort TYPE lgort_d,     "Almacén
        matkl TYPE matkl,       "Grupo de artículos
      END OF   g_tp_ekpo,

      "Repartos del plan de entregas
      BEGIN OF g_tp_eket,
        ebeln TYPE ebeln,       "Número del documento de compras
        eindt TYPE eindt,       "Fecha de entrega de posición
        menge TYPE etmen,       "Cantidad
        wemng TYPE wemng,       "Cantidad entrada de mercancías
      END   OF g_tp_eket,

      BEGIN OF g_tp_field_msj,
        mensaje1 TYPE char25,
        mensaje2 TYPE char25,
        mensaje3 TYPE char25,
        mensaje4 TYPE char25,
      END OF g_tp_field_msj.

    DATA:
      " Tabla de la ventana de  busqueda
      g_ti_busqueda  TYPE STANDARD TABLE OF g_tp_busqueda,
      " Tabla con todos los datos de la ventana de busqueda
      g_ti_cbusqueda TYPE STANDARD TABLE OF g_tp_busqueda,
      " Tabla de la ventana de Consumo
      gti_consumo    TYPE STANDARD TABLE OF g_tp_consumo,
      " Tabla Datos de almacén para el material
      g_ti_mard      TYPE HASHED TABLE OF g_tp_mard
                     WITH UNIQUE KEY matnr werks lgort,
      " Tabla stocks de lotes
      g_ti_mchb      TYPE STANDARD TABLE OF g_tp_mchb,
      " Tabla Posición del documento de compras
      g_ti_ekpo      TYPE STANDARD TABLE OF g_tp_ekpo,
      " Tabla Repartos del plan de entregas
      g_ti_eket      TYPE STANDARD TABLE OF g_tp_eket.

    DATA:
      " Estructura de la ventana de busqueda
      ges_busqueda TYPE g_tp_busqueda,
      " Estructura de la ventana de consumo
      ges_consumo  TYPE g_tp_consumo,
      " Estructura de Materiales
      ges_mara     TYPE g_tp_mara.

    DATA: go_log                  TYPE REF TO zclcxr1002_log_aplicacion,
          g_centro_configurado    TYPE marc-werks,
          gr_material_configurado TYPE RANGE OF marc-matnr,
          " Numero de Material
          g_matnr                 TYPE matnr,
          " Numero de Material formato interno
          g_cmatnr                TYPE matnr,
          " Texto breve de material
          g_maktx                 TYPE maktx,
          " Numero de Visualización formato interno
          g_vmatnr                TYPE matnr,
          " Centro
          g_werks                 TYPE werks_d,
          " Almacén
          g_lgort                 TYPE lgort_d,
          " Cantidad
          g_menge                 TYPE etmen,
          " Ubicación
          g_lgpbe                 TYPE lgpbe,
          " Cantidad en unidad de medida de entrada
          g_cantidad_consumo      TYPE erfmg,
          " Centro de coste
          g_centro_de_costo       TYPE kostl,
          " Cédula
          g_cedula                TYPE icnum.

    "Navegacion pagina
    DATA : y_v_index    TYPE sy-index,
           y_lv_d       TYPE f,
           y_lv_div     TYPE i,
           y_curr_p_num TYPE i,
           g_i_linestep TYPE i. "Obtener Linea del step loop

    METHODS:
      iniciar_proceso,
      mostrar_mensaje
        IMPORTING
          VALUE(i_es_mensaje) TYPE g_tp_mensaje,
      desplazar_pagina_hacia_abajo,
      desplazar_pagina_hacia_arriba,
      consultar_material_pieza,
      reiniciar_valores_dynpro_300,
      ocultar_campo_pantalla
        IMPORTING
          i_nombre_componente_pantalla TYPE string,
      hacer_visible_campo_pantalla
        IMPORTING
          i_nombre_componente_pantalla TYPE string,
      transferir_valores_dynp_salida,
      validar_tabla_consumo,
      realizar_consumo_de_mercancia,
      limpiar_pant_busqueda_material,
      limpiar_pantalla_consumo.

  PROTECTED SECTION.

  PRIVATE SECTION.
    METHODS:
      cargar_parametros,
      obtener_material
        IMPORTING
          VALUE(i_matnr) TYPE matnr,
      obtener_material_desde_pieza
        IMPORTING
          VALUE(i_matnr) TYPE matnr,
      asignar_valores_consultados,
      consultar_desde_seleccion,
      obtener_pedidos_pendientes,
      converti_cedula,
      realizar_movimiento_mercancias.

ENDCLASS.
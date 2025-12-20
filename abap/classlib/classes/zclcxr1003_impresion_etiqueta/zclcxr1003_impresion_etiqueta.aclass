CLASS zclcxr1003_impresion_etiqueta DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION..
    TYPES:
      g_ti_ra_etiquetas TYPE RANGE OF ztcxr1003_1-etiqueta_id.
    METHODS:
      constructor
        IMPORTING
          i_es_info_mensaje TYPE ztcxr1001_1 OPTIONAL,

      "! <p class="shorttext synchronized" lang="en"></p>
      "!
      "! @parameter i_n_sufijo | <p class="shorttext synchronized" lang="en"></p>
      "! @parameter i_c_ftp | <p class="shorttext synchronized" lang="en">Indicador para Impresión en servidor</p>
      "! @parameter i_datos_desencadena | <p class="shorttext synchronized" lang="en">Impresión con un dato que desencadene la extracción</p>
      "! @parameter i_remover_ceros | Remover los decimales 0 a la derecha (tomando el punto como separador)
      "! @parameter c_es_impresion | <p class="shorttext synchronized" lang="en">Estructura de cabecera para impresión</p>
      "! @parameter c_ti_detalle | <p class="shorttext synchronized" lang="en">Tabla para detalle en la impresión de la etiqueta</p>
      etiqueta
        IMPORTING
          i_n_sufijo          TYPE numc4       OPTIONAL
          i_c_ftp             TYPE char1       OPTIONAL " Impresión en servidor 27.07.2021 - SAGIRALD
          i_datos_desencadena TYPE REF TO data OPTIONAL " Impresión con un dato que desencadene la extracción 28.12.2021 - SAGIRALD
          i_remover_ceros     TYPE flag        OPTIONAL " Remover los decimales 0 a la derecha (tomando el punto como separador) 25.05.2022 - SAGIRALD
        EXPORTING
          e_ti_bapiret2       TYPE bapiret2_t
        CHANGING
          c_es_impresion      TYPE zecxr1003_2
          c_ti_detalle        TYPE zttcxr1003_1,
      consumir_api
        IMPORTING
          i_ti_impresion TYPE zttcxr1003_2,
      remover_ceros
        CHANGING
          c_valor_campo TYPE string,
      get_info_etiqueta
        IMPORTING
          i_ti_ra_etiquetas TYPE  g_ti_ra_etiquetas
        EXPORTING
          e_ti_impresion    TYPE zttcxr1003_2,
      envio_desde_monitor
        EXPORTING
          e_ti_return   TYPE bapiret2_t
        CHANGING
          i_datos_proxy TYPE REF TO data.

  PROTECTED SECTION.
  PRIVATE SECTION.

    TYPES:
*    g_ti_ra_etiquetas TYPE RANGE OF ZTCXR1003_1-etiqueta_id,

*----------------------------------------------------------------------*
* Definición de Tipos
*----------------------------------------------------------------------*
      ".Etiquetas
      BEGIN OF g_tp_ztcxr1003_7,
        mensaje_uuid  TYPE  sysuuid_x16,
        etiqueta_uuid TYPE  sysuuid_x16,
        mensaje_id    TYPE  zedmensajeid,
        num_copias    TYPE  zednumcopias,
      END OF g_tp_ztcxr1003_7 .
    TYPES:
      ".Usuarios
      BEGIN OF g_tp_ztcxr1003_5,
        usuario_uuid  TYPE  sysuuid_x16,
        etiqueta_uuid TYPE sysuuid_x16,
        usuario_id    TYPE xubname,
        impresora_id  TYPE zedimpresoraid,
        campo_local   TYPE zedcamlocal,
        imp_local_id  TYPE zedimplocal,
      END OF g_tp_ztcxr1003_5 .
    TYPES:
      ".Impresora
      BEGIN OF g_tp_ztcxr1003_4,
        impresora_uuid TYPE sysuuid_x16,
        impresora_id   TYPE zedimpresoraid,
        ruta           TYPE zedruta,
      END OF g_tp_ztcxr1003_4 .
    TYPES:
      BEGIN OF g_tp_ztcxr1003_3,
        campo_uuid    TYPE sysuuid_x16,
        etiqueta_uuid TYPE sysuuid_x16,
        campo_id      TYPE zedcampoid,
        reg_det       TYPE zedregdet,
        reg_total     TYPE zedregtot,
      END OF g_tp_ztcxr1003_3 .
    TYPES:
      BEGIN OF g_tp_dd03l,
        fieldname TYPE dd03l-fieldname,
        inttype   TYPE dd03l-inttype,
      END OF g_tp_dd03l .
    TYPES:
      g_tp_ti_ztcxr1003_3 TYPE STANDARD TABLE OF g_tp_ztcxr1003_3 .
    TYPES:
      g_tp_ti_ztcxr1003_6 TYPE TABLE FOR READ RESULT zi_rap_ztcxr1003_6.
*----------------------------------------------------------------------*
* Inicio Definición de Constantes para la impresión en servidor
* 27.07.2021 - SAGIRALD
*----------------------------------------------------------------------*
    TYPES: gtp_ti_ztcxr1003_7 TYPE STANDARD TABLE OF g_tp_ztcxr1003_7 WITH EMPTY KEY.
*----------------------------------------------------------------------*
* Fin Definición de Constantes para la impresión en servidor
* 27.07.2021 - SAGIRALD
*----------------------------------------------------------------------*
    TYPES:
*----------------------------------------------------------------------*
*      Definición de Tipos
*----------------------------------------------------------------------*
      BEGIN OF g_tp_es_parametros,
        dirlocal   TYPE string,
        imp_local  TYPE zedruta,
        log_x      TYPE zedimpresoraid,
        adminetiq  TYPE so_recname,
        diretiquet TYPE char100,
        url_srv    TYPE char100,
      END OF g_tp_es_parametros .

*----------------------------------------------------------------------*
* Definición de constantes
*----------------------------------------------------------------------*
    CONSTANTS g_cte_x TYPE c VALUE 'X' ##NO_TEXT.
    CONSTANTS g_cte_e TYPE c VALUE 'E' ##NO_TEXT.
    CONSTANTS g_cte_i TYPE c VALUE 'I' ##NO_TEXT.
    CONSTANTS g_cte_eq TYPE char2 VALUE 'EQ' ##NO_TEXT.
    CONSTANTS g_cte_c TYPE c VALUE 'C' ##NO_TEXT.
    CONSTANTS g_cte_p TYPE c VALUE 'P' ##NO_TEXT.
    CONSTANTS g_cte_s TYPE c VALUE 'S' ##NO_TEXT.
*----------------------------------------------------------------------*
* Inicio Definición de Constantes para la impresión en servidor
* 27.07.2021 - SAGIRALD
*----------------------------------------------------------------------*
    CONSTANTS: g_cte_aplica TYPE char20 VALUE 'ETIQUETAS',
*............Configuracion nuevo servidor FTP
               g_cte_ftp    TYPE char20 VALUE 'ETIQUETAS2',
               g_cte_modulo TYPE char2 VALUE 'CX',
               g_cte_id     TYPE char10 VALUE 'T00011'.
*----------------------------------------------------------------------*
* Fin Definición de Constantes para la impresión en servidor
* 27.07.2021 - SAGIRALD
*----------------------------------------------------------------------*
    DATA:
*----------------------------------------------------------------------*
* Definición de Tablas Internas
*----------------------------------------------------------------------*
      g_th_ztcxr1003_1 TYPE HASHED TABLE OF ztcxr1003_1 WITH UNIQUE KEY etiqueta_uuid .
    DATA:
      g_ti_ztcxr1003_3 TYPE STANDARD TABLE OF g_tp_ztcxr1003_3 .
    DATA:
      g_ti_ztcxr1003_4 TYPE STANDARD TABLE OF g_tp_ztcxr1003_4 .
    DATA:
      g_ti_ztcxr1003_5 TYPE STANDARD TABLE OF g_tp_ztcxr1003_5 .
    DATA:
      g_ti_ztcxr1003_7 TYPE STANDARD TABLE OF g_tp_ztcxr1003_7 .
    DATA:
      g_ra_ti_inttype  TYPE RANGE OF char1 .
    DATA:
      g_th_dd03l       TYPE HASHED TABLE OF g_tp_dd03l WITH UNIQUE KEY fieldname .
*----------------------------------------------------------------------*
*      Definición de Estructuras
*----------------------------------------------------------------------*
    DATA g_es_parametros TYPE g_tp_es_parametros .

*----------------------------------------------------------------------*
* Inicio Definición de parametro para la impresión en servidor
* 25.05.2022 - SAGIRALD
*----------------------------------------------------------------------*
    DATA: g_imprimir_en_servidor TYPE zzedlow,
          gr_correos             TYPE RANGE OF zzedlow.
*----------------------------------------------------------------------*
* Fin Definición de parametro para la impresión en servidor
* 25.05.2022 - SAGIRALD
*----------------------------------------------------------------------*

    METHODS clear_variables .
    METHODS get_parametros .
    METHODS get_data
      IMPORTING
        !i_es_impresion      TYPE zecxr1003_2
      RETURNING
        VALUE(r_ti_bapiret2) TYPE bapiret2_t.
    METHODS set_data
      IMPORTING
        !i_n_sufijo                TYPE numc4       OPTIONAL
        !i_c_ftp                   TYPE char1       OPTIONAL " Impresión en servidor 27.07.2021 - SAGIRALD
        VALUE(i_datos_desencadena) TYPE REF TO data OPTIONAL " Impresión con un dato que desencadene la extracción 28.12.2021 - SAGIRALD
        i_remover_ceros            TYPE flag        OPTIONAL " Remover los decimales 0 a la derecha (tomando el punto como separador) 25.05.2022 - SAGIRALD
      CHANGING
        !c_es_impresion            TYPE zecxr1003_2
        !c_ti_detalle              TYPE zttcxr1003_1
        c_ti_bapiret2              TYPE bapiret2_t.
    METHODS reemplazar_dato
      IMPORTING
        !i_c_tipo     LIKE g_cte_c
        !i_es_detalle TYPE zecxr1003_2
        !i_ti_campos  TYPE g_tp_ti_ztcxr1003_3
        !i_ti_codsato TYPE g_tp_ti_ztcxr1003_6
      CHANGING
        !c_ti_texto   TYPE zttcxr1003_c1024 .
    METHODS reemplazar_detalle
      IMPORTING
        !i_i_alto     TYPE zi_rap_ztcxr1003_1-alto
        !i_i_comienzo TYPE zi_rap_ztcxr1003_1-comienzo
        !i_ti_detalle TYPE zttcxr1003_1
        !i_ti_campos  TYPE g_tp_ti_ztcxr1003_3
        !i_ti_codsato TYPE g_tp_ti_ztcxr1003_6
      CHANGING
        !c_ti_texto   TYPE zttcxr1003_c1024 .
*----------------------------------------------------------------------*
* Inicio Definición de Metodos para la determinación de etiquetas
* Validando todos los escenarios
* 27.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
    TYPES: BEGIN OF gtp_es_zi_rap_ztcxr1003_1,
             etiquetauuid       TYPE sysuuid_x16,
             etiquetaid         TYPE zedetiquetaid,
             dsetiqueta         TYPE zedesetiqueta,
             dinamica           TYPE zedinamica,
             copias             TYPE zedecopias,
             archivo            TYPE zdearchivo,
             clase              TYPE zdeclase,
             numdet             TYPE zdenum_det,
             alto               TYPE zdealto,
             comienzo           TYPE zdecomienzo,
             opinterlocutor     TYPE zdeinter,
             archivo2           TYPE zdearchivo,
             progextract        TYPE zdeprog_ext,
             ftp                TYPE zdeftp,
             createdby          TYPE syuname,
             createdat          TYPE timestampl,
             lastchangeby       TYPE syuname,
             lastchangeat       TYPE timestampl,
             locallastchangedat TYPE timestampl,
           END OF gtp_es_zi_rap_ztcxr1003_1,
           gtp_ti_ztcxr1003_3 TYPE STANDARD TABLE OF g_tp_ztcxr1003_3,
           BEGIN OF gtp_es_zi_rap_ztcxr1003_5,
             usuariouuid        TYPE sysuuid_x16,
             etiquetauuid       TYPE sysuuid_x16,
             usuarioid          TYPE xubname,
             impresoraid        TYPE zedimpresoraid,
             campolocal         TYPE zedcamlocal,
             implocalid         TYPE zedimplocal,
             createdby          TYPE syuname,
             createdat          TYPE timestampl,
             lastchangeby       TYPE syuname,
             lastchangeat       TYPE timestampl,
             locallastchangedat TYPE timestampl,
           END OF gtp_es_zi_rap_ztcxr1003_5.
    METHODS:
      determinar_etiqueta
        IMPORTING
          i_mensaje_id            TYPE ztcxr1003_7-mensaje_id
          i_accion_ppf            TYPE ztcxr1003_7-accion_ppf
          i_aplicacion            TYPE ztcxr1003_7-aplicacion
          i_sel_etiqueta          TYPE ztcxr1003_7-sel_etiqueta
        RETURNING
          VALUE(r_ti_ztcxr1003_7) TYPE gtp_ti_ztcxr1003_7,
      obtener_etiqueta
        IMPORTING
          i_mensaje_id            TYPE ztcxr1003_7-mensaje_id
          i_accion_ppf            TYPE ztcxr1003_7-accion_ppf
          i_aplicacion            TYPE ztcxr1003_7-aplicacion
          i_sel_etiqueta          TYPE ztcxr1003_7-sel_etiqueta
        RETURNING
          VALUE(r_ti_ztcxr1003_7) TYPE gtp_ti_ztcxr1003_7,
      generar_archivo_plano
        IMPORTING
          i_es_impresion         TYPE zecxr1003_2
          i_ti_ztcxr1003_3       TYPE gtp_ti_ztcxr1003_3
          i_es_ztcxr1003_1       TYPE gtp_es_zi_rap_ztcxr1003_1
          i_es_ztcxr1003_5       TYPE gtp_es_zi_rap_ztcxr1003_5
          i_consecutivo          TYPE string
          i_impresora            TYPE string
          i_numero_copias        TYPE zedecopias
          i_sin_detalle          TYPE flag
          i_remover_ceros        TYPE flag
          i_omitir_vacio_detalle TYPE flag
        EXPORTING
          e_ti_detalle           TYPE zttcxr1003_1
        CHANGING
          c_ti_detalle           TYPE zttcxr1003_1
          c_ti_archivo           TYPE zttcxr1003_c1024
          c_nombre_archivo       TYPE string,
      guardar_archivos
        IMPORTING
          VALUE(i_ti_archivo)     TYPE zttcxr1003_c1024
          i_es_ztcxr1003_1        TYPE gtp_es_zi_rap_ztcxr1003_1
          i_es_ztcxr1003_5        TYPE gtp_es_zi_rap_ztcxr1003_5
          i_ftp                   TYPE flag
          VALUE(i_nombre_archivo) TYPE string.
*----------------------------------------------------------------------*
* Inicio Definición de Metodos para la determinación de etiquetas
* Validando todos los escenarios
* 27.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
ENDCLASS.



CLASS zclcxr1003_impresion_etiqueta IMPLEMENTATION.


  METHOD constructor.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : zclcxr1003_impresion_etiqueta
* Método       : Constructor
* Descripción  : Constructor
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 2 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 2 mar. 2021    xxxxxx       Cristian Montoya  Version Inicial
*----------------------------------------------------------------------*

    g_ra_ti_inttype = VALUE #( sign   = g_cte_i
                           option   = g_cte_eq
                            ( low   = 'i' )
                            ( low   = 'b')
                            ( low   = 's')
                            ( low   = 'P')
                            ( low   = 'F' ) ).

    ".Obtener Parámetros configurados
    get_parametros( ).


  ENDMETHOD.


  METHOD get_parametros.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : zclcxr1003_impresion_etiqueta
* Método       : get_parametros
* Descripción  : Obtener parámetros configurados del proceso
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 2 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 2 mar. 2021    xxxxxx         Cristian Montoya  Version Inicial
*----------------------------------------------------------------------*

*----------------------------------------------------------------------*
* Definición de constantes
*----------------------------------------------------------------------*
    CONSTANTS:
      l_cte_ricefw     TYPE ztcxr1000_1-ricefw  VALUE 'R1003',
      l_cte_modulo     TYPE ztcxr1000_1-modulo  VALUE 'CX',
      l_cte_direclocal TYPE ztcxr1000_1-idparam VALUE 'DIRECLOCAL',
      l_cte_imprelocal TYPE ztcxr1000_1-idparam VALUE 'IMPRELOCAL',
      l_cte_activ_log  TYPE ztcxr1000_1-idparam VALUE 'ACTIV_LOG',
      l_cte_adminetiqu TYPE ztcxr1000_1-idparam VALUE 'ADMINETIQU',
      l_cte_diretiquet TYPE ztcxr1000_1-idparam VALUE 'DIRETIQUET',
      l_cte_url_srv    TYPE ztcxr1000_1-idparam VALUE 'URL_SRV'.

*----------------------------------------------------------------------*
* Definición Tablas
*----------------------------------------------------------------------*
    DATA:
     l_ti_ra_comodin TYPE RANGE OF char100.



    ".Instancia Constructor
    DATA(l_o_parametros) = NEW zclcxr1000_parametros(
      i_modulo = l_cte_modulo
      i_ricefw = l_cte_ricefw ).


    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = l_cte_direclocal
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    IF line_exists( l_ti_ra_comodin[ 1 ] ).

      g_es_parametros-dirlocal = l_ti_ra_comodin[ 1 ]-low.

    ENDIF.

    CLEAR:
      l_ti_ra_comodin.



    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = l_cte_imprelocal
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    IF line_exists( l_ti_ra_comodin[ 1 ] ).

      g_es_parametros-imp_local = l_ti_ra_comodin[ 1 ]-low.

    ENDIF.

    CLEAR:
      l_ti_ra_comodin.


    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = l_cte_activ_log
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    IF line_exists( l_ti_ra_comodin[ 1 ] ).

      g_es_parametros-log_x = l_ti_ra_comodin[ 1 ]-low.

    ENDIF.

    CLEAR:
      l_ti_ra_comodin.

    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = l_cte_adminetiqu
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    IF line_exists( l_ti_ra_comodin[ 1 ] ).

      g_es_parametros-adminetiq = l_ti_ra_comodin[ 1 ]-low.

    ENDIF.

    CLEAR:
      l_ti_ra_comodin.


    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = l_cte_diretiquet
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    IF line_exists( l_ti_ra_comodin[ 1 ] ).

      g_es_parametros-diretiquet = l_ti_ra_comodin[ 1 ]-low.

    ENDIF.

    CLEAR:
      l_ti_ra_comodin.

    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = l_cte_diretiquet
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    IF line_exists( l_ti_ra_comodin[ 1 ] ).

      g_es_parametros-url_srv = l_ti_ra_comodin[ 1 ]-low.

    ENDIF.

*----------------------------------------------------------------------*
* Inicio Definición de parametro para la impresión en servidor
* 25.05.2022 - SAGIRALD
*----------------------------------------------------------------------*
    CLEAR: l_ti_ra_comodin.
    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = 'IMP_SERVID'
                                   IMPORTING
                                        e_ti_valrango           = l_ti_ra_comodin
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).

    g_imprimir_en_servidor = VALUE #( l_ti_ra_comodin[ 1 ]-low OPTIONAL ).

    l_o_parametros->get_parametro( EXPORTING
                                        i_idparam               = 'CORREOS'
                                   IMPORTING
                                        e_ti_valrango           = gr_correos
                                   EXCEPTIONS
                                        parametro_no_encontrado = 1 ).
*----------------------------------------------------------------------*
* Fin Definición de parametro para la impresión en servidor
* 25.05.2022 - SAGIRALD
*----------------------------------------------------------------------*


  ENDMETHOD.


  METHOD get_data.
*----------------------------------------------------------------------*
* Inicio Definición de Metodos para la determinación de etiquetas
* Validando todos los escenarios
* 27.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
*    "Selección Etiquetas en base a la clase de mensaje
*    SELECT  mensaje_uuid,etiqueta_uuid,mensaje_id,num_copias
*      FROM ztcxr1003_7
*      INTO TABLE @g_ti_ztcxr1003_7
*      WHERE mensaje_id    EQ @i_es_impresion-mensaje AND
*           aplicacion     EQ @i_es_impresion-aplicacion AND
*           accion_ppf     EQ @i_es_impresion-accion_ppf AND
*           sel_etiqueta   EQ @i_es_impresion-seleccion.

    g_ti_ztcxr1003_7 = determinar_etiqueta( i_mensaje_id   = i_es_impresion-mensaje
                                            i_accion_ppf   = i_es_impresion-accion_ppf
                                            i_aplicacion   = i_es_impresion-aplicacion
                                            i_sel_etiqueta = i_es_impresion-seleccion ).


*    IF sy-subrc EQ 0.
    IF g_ti_ztcxr1003_7 IS NOT INITIAL.
*----------------------------------------------------------------------*
* Inicio Definición de Metodos para la determinación de etiquetas
* Validando todos los escenarios
* 27.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
*..Etiquetas encontradas para la clase de menesaje
      SELECT *
       FROM ztcxr1003_1
       INTO TABLE @g_th_ztcxr1003_1
       FOR ALL ENTRIES IN @g_ti_ztcxr1003_7
       WHERE etiqueta_uuid EQ @g_ti_ztcxr1003_7-etiqueta_uuid.


      IF sy-subrc EQ 0.


        "Campos de las etiquetas
        SELECT  b~campo_uuid, a~etiqueta_uuid,b~campo_id, a~reg_det, a~reg_total
        INTO TABLE @g_ti_ztcxr1003_3
        FROM ztcxr1003_2 AS b
        INNER JOIN ztcxr1003_3 AS a ON b~campo_id EQ a~campo_id
        FOR ALL ENTRIES IN @g_th_ztcxr1003_1
        WHERE a~etiqueta_uuid EQ @g_th_ztcxr1003_1-etiqueta_uuid.

        SORT g_ti_ztcxr1003_3 BY campo_id ASCENDING.

        ".Etiquetas por Usuario
        SELECT usuario_uuid,etiqueta_uuid,usuario_id,impresora_id,campo_local,imp_local_id
        FROM ztcxr1003_5
        INTO TABLE @g_ti_ztcxr1003_5
        FOR ALL ENTRIES IN @g_th_ztcxr1003_1
        WHERE etiqueta_uuid EQ @g_th_ztcxr1003_1-etiqueta_uuid.

        IF sy-subrc EQ 0.

          SELECT impresora_uuid, impresora_id, ruta
           FROM ztcxr1003_4
           INTO TABLE @g_ti_ztcxr1003_4
           FOR ALL ENTRIES IN @g_ti_ztcxr1003_5
           WHERE impresora_id EQ @g_ti_ztcxr1003_5-impresora_id.


        ENDIF.

      ENDIF.
    ELSE.
      APPEND VALUE bapiret2( id         = 'ZCX01'
                             number     = '093'
                             type       = 'E'
                             message_v1 = i_es_impresion-mensaje
                             message_v2 = i_es_impresion-accion_ppf
                             message_v3 = i_es_impresion-aplicacion
                             message_v4 = i_es_impresion-seleccion  ) TO r_ti_bapiret2.
    ENDIF.

  ENDMETHOD.


  METHOD etiqueta.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : zclcxr1003_impresion_etiqueta
* Método       : etiqueta
* Descripción  : Extraer información de las etiquetas
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 4 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 4 mar. 2021   xxxxxx       Cristian Montoya   Version Inicial
* 28.12.2021    S4DK900235   Santiago Giraldo   Ejecución de extracción
*----------------------------------------------------------------------*

    me->clear_variables( ).
*----------------------------------------------------------------------*
* Inicio añadir variable de retorno mensajes
* 05.10.2023 - SAGIRALD
*----------------------------------------------------------------------*
    ".Obtener
*    get_data( c_es_impresion ).
    e_ti_bapiret2 = get_data( c_es_impresion ).
*----------------------------------------------------------------------*
* Fin añadir variable de retorno mensajes
* 05.10.2023 - SAGIRALD
*----------------------------------------------------------------------*

*----------------------------------------------------------------------*
* Inicio añadir variable para ejecutar extracción
* 28.12.2021 - SAGIRALD
*----------------------------------------------------------------------*

*    ".Obtener
*    set_data(
*      CHANGING
*       c_es_impresion = c_es_impresion
*       c_ti_detalle   = c_ti_detalle ).

    ".Obtener
    set_data( EXPORTING
                    i_c_ftp             = i_c_ftp
                    i_datos_desencadena = i_datos_desencadena
                    i_remover_ceros     = i_remover_ceros
              CHANGING
                    c_es_impresion      = c_es_impresion
                    c_ti_detalle        = c_ti_detalle
                    c_ti_bapiret2       = e_ti_bapiret2 ).

*----------------------------------------------------------------------*
* Fin añadir variable para ejecutar extracción
* 28.12.2021 - SAGIRALD
*----------------------------------------------------------------------*


  ENDMETHOD.


  METHOD set_data.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : zclcxr1003_impresion_etiqueta
* Método       : set_data
* Descripción  : Mapear
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 4 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 4 mar. 2021    xxxxxx       Cristian Montoya  Version Inicial
* 25.05.2022    S4DK900235   Santiago Giraldo   Ejecución de extracción
*----------------------------------------------------------------------*

*----------------------------------------------------------------------*
*      Definición de Variables
*----------------------------------------------------------------------*
    DATA:
      l_s_consecuti  TYPE string,
      "{SGR Inicio
*      l_s_impresora  TYPE ztcxr1003_5-imp_local_id,
      l_s_impresora  TYPE string,
      "{SGR Fin
      l_n_valor      TYPE p DECIMALS 2,
*----------------------------------------------------------------------*
* Inicio Definición de Variables para Impresión en servidor
* 27.07.2021 - SAGIRALD
*----------------------------------------------------------------------*
      l_ti_archivo2  TYPE re_t_string,
      l_es_interface TYPE ztcxr1002_2,
      l_ti_bapiret2  TYPE bapiret2_t,
      et_dinamica    TYPE flag.
*----------------------------------------------------------------------*
* Fin Definición de Variables para Impresión en servidor
* 27.07.2021 - SAGIRALD
*----------------------------------------------------------------------*
    DATA: numero TYPE p.
*----------------------------------------------------------------------*
*        Definición Interfaces
*----------------------------------------------------------------------*
    DATA:
     l_o_interface TYPE REF TO zifcx1003_etiqueta.

    DATA(l_s_filename) = VALUE string(  ).
    "{20.09.2022 SGR Inicio -> Sobre escritura de archivos
    DATA(timestamp) = VALUE timestampl( ).
    GET TIME STAMP FIELD timestamp.
    l_s_filename = timestamp.
    REPLACE ALL OCCURRENCES OF '.' IN l_s_filename WITH ''.

    IF i_n_sufijo IS INITIAL.
      CONCATENATE sy-uname '-' l_s_filename INTO l_s_filename.
    ELSE.
      CONCATENATE sy-uname '-' l_s_filename '-' i_n_sufijo INTO l_s_filename.
    ENDIF.
*    IF i_n_sufijo IS INITIAL.
*      CONCATENATE sy-uname '-' sy-datum '-' sy-uzeit INTO l_s_filename.
*    ELSE.
*      CONCATENATE sy-uname '-' sy-datum '-' sy-uzeit '-' i_n_sufijo INTO l_s_filename.
*    ENDIF.
    "{20.09.2022 SGR Fin -> Sobre escritura de archivos

*.Información Log
    DATA(l_es_ztcxr1003_8) = VALUE ztcxr1003_8(
    usuario     = sy-uname
    tcode       = sy-tcode
    fecha       = sy-datum
    hora        = sy-uzeit
    programa    = sy-cprog ).

    SELECT  fieldname, inttype
      FROM dd03l
      WHERE tabname = 'ZECXR1003_1'
      INTO TABLE @g_th_dd03l.

    DATA(es_impresion) = CORRESPONDING zecxr1003_1( c_es_impresion ).

    IF i_datos_desencadena IS NOT INITIAL AND es_impresion IS INITIAL.

      DATA(o_extraccion_generica) = NEW zclewme1040_extraccion_gen_ump( ).

      CALL METHOD o_extraccion_generica->zifcxr1003_extraccion_etiqueta~obtener_datos
        CHANGING
          c_datos_desencadenantes = i_datos_desencadena
          c_ti_etiqueta           = c_ti_detalle
          c_es_impresion          = c_es_impresion.

      DATA(omitir_detalle_vacio) = abap_true.

    ENDIF.

*..Se recorren las etiquetas asociadas a la clase de mensaje
    LOOP AT g_ti_ztcxr1003_7 INTO DATA(l_es_ztcxr1003_7) .
      CLEAR:
       c_es_impresion-c00000.

      l_s_consecuti =  sy-tabix.

      READ ENTITIES OF zi_rap_ztcxr1003_1
      ENTITY etiquetas
      ALL FIELDS WITH VALUE #( ( etiquetauuid = l_es_ztcxr1003_7-etiqueta_uuid ) )
      RESULT DATA(l_ti_ztcxr1003_1).

      IF line_exists( l_ti_ztcxr1003_1[ 1 ] ).
        DATA(l_es_ztcxr1003_1) = l_ti_ztcxr1003_1[ 1 ].

      ELSE.
        ".La etiqueta & no existe.
        MESSAGE e004(zcx01) WITH l_es_ztcxr1003_7-etiqueta_uuid.

      ENDIF.


      DATA(l_c_marca) =  l_es_ztcxr1003_1-opinterlocutor.

      SET PARAMETER ID 'MARC' FIELD l_c_marca.
*   Ubicacion fisico del archivo label
      CONCATENATE '"' l_es_ztcxr1003_1-archivo '"'  INTO l_es_ztcxr1003_1-archivo.
      CONCATENATE '"' l_es_ztcxr1003_1-archivo2 '"' INTO l_es_ztcxr1003_1-archivo2.
********************************************************************************************
* SAGIRALD 09.09.2022 Solucionar el numero de copias por etiqueta
********************************************************************************************
*      DATA(l_c_numcopias) = l_es_ztcxr1003_7-num_copias.
      DATA(l_c_numcopias) = l_es_ztcxr1003_1-copias.

      ".Cantidad Copias
      IF c_es_impresion-copias IS INITIAL.
        IF l_es_ztcxr1003_7-num_copias IS NOT INITIAL.
          IF l_es_ztcxr1003_7-num_copias < 0.
            CONTINUE.
          ENDIF.
        ELSE.
          l_c_numcopias = l_es_ztcxr1003_1-copias.
        ENDIF.
      ELSE.
        l_c_numcopias = c_es_impresion-copias.
      ENDIF.

********************************************************************************************
* SAGIRALD 09.09.2022
********************************************************************************************

*   Obtener el nombre de la impresora
      IF l_es_ztcxr1003_1-dinamica IS INITIAL.
        "IF c_es_impresion-id_impr IS INITIAL.
        READ ENTITIES OF zi_rap_ztcxr1003_1
        ENTITY etiquetas BY \_ztcxr1003_5
        FIELDS ( usuariouuid etiquetauuid usuarioid impresoraid campolocal implocalid   )
        WITH VALUE #( ( etiquetauuid = l_es_ztcxr1003_1-etiquetauuid    ) )
        RESULT DATA(l_ti_ztcxr1003_5).

        IF line_exists( l_ti_ztcxr1003_5[ usuarioid = sy-uname ] ).
          DATA(l_es_ztcxr1003_5) = l_ti_ztcxr1003_5[ usuarioid = sy-uname ].

        ELSE.
********************************************************************************************
* SAGIRALD 14.02.2022
********************************************************************************************
***************************          ".Impresora
***************************          CALL FUNCTION 'ZCXR1003_IMPRESORA_USUARIO'
***************************            EXPORTING
***************************              i_c_etiqueta_uuid = l_es_ztcxr1003_1-etiquetauuid
***************************            IMPORTING
***************************              e_c_impresora     = c_es_impresion-id_impr.
***************************
***************************          l_es_ztcxr1003_5-impresoraid = c_es_impresion-id_impr.
**          l_es_ztcxr1003_5-impresoraid = 'SATO'.
          IF i_c_ftp = abap_false.
*            MESSAGE i078(zcx01) WITH sy-uname l_es_ztcxr1003_1-etiquetaid.
            DATA(guid_16) = VALUE guid_16( ).
            CALL FUNCTION 'GUID_CREATE'
              IMPORTING
                ev_guid_16 = guid_16.

            timestamp = VALUE timestampl( ).
            GET TIME STAMP FIELD timestamp.
            DATA(es_usuario) = VALUE ztcxr1003_5( usuario_uuid          = guid_16
                                                  etiqueta_uuid         = l_es_ztcxr1003_7-etiqueta_uuid
                                                  usuario_id            = sy-uname
                                                  impresora_id          = 'SATO'
                                                  campo_local           = abap_true
                                                  imp_local_id          = 'SATO'
                                                  created_by            = sy-uname
                                                  created_at            = timestamp
                                                  last_change_by        = sy-uname
                                                  last_change_at        = timestamp
                                                  local_last_changed_at = timestamp ).
            INSERT ztcxr1003_5 FROM es_usuario.
            "No se realiza el commit porque puede ser lanzado desde una clase de mensaje y si se
            "realiza el commit genera un dump (Al final de la clase de mensaje, realiza un commit)
            l_es_ztcxr1003_5 = VALUE #( usuariouuid  = es_usuario-usuario_uuid
                                        etiquetauuid = es_usuario-etiqueta_uuid
                                        usuarioid    = es_usuario-usuario_id
                                        impresoraid  = es_usuario-impresora_id
                                        campolocal   = es_usuario-campo_local
                                        implocalid   = es_usuario-imp_local_id ).
          ELSE.
*            DATA: timestamp TYPE timestampl.
            MESSAGE i078(zcx01) WITH sy-uname l_es_ztcxr1003_1-etiquetaid INTO DATA(mensaje).
            APPEND VALUE bapiret2( id         = 'ZCX01'
                                   number     = '078'
                                   type       = 'E'
                                   message_v1 = sy-uname
                                   message_v2 = l_es_ztcxr1003_1-etiquetaid ) TO c_ti_bapiret2.

            GET TIME STAMP FIELD timestamp.
            DATA(es_ztcxr1003_10) = VALUE ztcxr1003_10( timestamp = timestamp
                                                        menssaje  = mensaje
                                                        etiqueta  = l_es_ztcxr1003_1-etiquetaid
                                                        hora      = sy-uzeit
                                                        fecha     = sy-datum
                                                        usuario   = sy-uname ).
            MODIFY ztcxr1003_10 FROM es_ztcxr1003_10.

            IF gr_correos IS NOT INITIAL.
              zclcxr1002_util=>enviar_correo( i_ti_destinatarios          = VALUE #( FOR wa IN gr_correos ( smtp_addr = wa-low ) )
                                              i_ti_texto_cuerpo_correo    = VALUE #( ( line = mensaje ) )
                                              i_asunto                    = 'Error en impresión de etiquetas' ).
            ENDIF.
            RETURN.
          ENDIF.


********************************************************************************************
* SAGIRALD 14.02.2022
********************************************************************************************
        ENDIF.

*
        "{SGR Inicio
*        IF l_es_ztcxr1003_5-campolocal IS INITIAL.
        IF l_es_ztcxr1003_5-campolocal IS INITIAL AND c_es_impresion-id_impr IS NOT INITIAL.
          "{SGR Fin

          IF NOT line_exists( g_ti_ztcxr1003_4[ impresora_id = c_es_impresion-id_impr ] ).
            ".La impresora &1 no existe para el usuario &2 y la etiqueta &3
*            MESSAGE e001(zcx001)  WITH c_es_impresion-id_impr sy-uname l_es_ztcxr1003_1-etiquetaid.
            APPEND VALUE bapiret2( id         = 'ZCX001'
                                   number     = '001'
                                   type       = 'E'
                                   message_v1 = c_es_impresion-id_impr
                                   message_v2 = sy-uname
                                   message_v3 = l_es_ztcxr1003_1-etiquetaid ) TO c_ti_bapiret2.
            RETURN.
          ELSE.

            DATA(l_es_ztcxr1003_4) = g_ti_ztcxr1003_4[ impresora_id = c_es_impresion-id_impr ].
            l_s_impresora = l_es_ztcxr1003_4-ruta.

          ENDIF.

          IF l_es_ztcxr1003_5-implocalid  IS INITIAL.
*            IF line_exists( g_es_parametros-imp_local[ 1 ] ).
            DATA(l_es_imp_local) = g_es_parametros-imp_local.
*            ENDIF.
*            l_s_impresora = g_es_parametros-imp_local.
          ELSE.
            l_s_impresora = l_es_ztcxr1003_5-implocalid.
          ENDIF.

        ELSE.

          IF l_es_ztcxr1003_5-implocalid IS INITIAL.
            l_s_impresora = g_es_parametros-imp_local.
          ELSE.
            l_s_impresora = l_es_ztcxr1003_5-implocalid.
          ENDIF.

        ENDIF.

        CONCATENATE '"' l_s_impresora '"' INTO l_s_impresora.
      ENDIF.

*...Validamos si NO existe impresora.
      IF l_s_impresora EQ '""'.
        CONTINUE.
      ENDIF.
*----------------------------------------------------------------------*
* Inicio ejecución extracción generica
* 04.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
*      """""...Ejeción de clase parametrizada
*      IF c_es_impresion-datos NE 'X' AND l_es_ztcxr1003_1-clase IS NOT INITIAL.
*
**     llamar la clase que obtiene los datos
*        CREATE OBJECT l_o_interface TYPE (l_es_ztcxr1003_1-clase).
*
*        CALL METHOD l_o_interface->get_data
*          CHANGING
*            c_es_impresion = c_es_impresion
*            c_ti_detalle   = c_ti_detalle.
*
**     Cancelación de etiqueta en curso
*        IF c_es_impresion-c00000 = 'X'.
*          CONTINUE.
*        ENDIF.
*      ENDIF.
*      BREAK sagirald.
*****      DATA(es_impresion) = CORRESPONDING zecxr1003_1( c_es_impresion ).
*****
*****      IF i_datos_desencadena IS NOT INITIAL AND es_impresion IS INITIAL.
*****
*****        DATA(o_extraccion_generica) = NEW zclewme1040_extraccion_gen_ump( ).
*****
*****        CALL METHOD o_extraccion_generica->zifcxr1003_extraccion_etiqueta~obtener_datos
*****          CHANGING
*****            c_datos_desencadenantes = i_datos_desencadena
*****            c_ti_etiqueta           = c_ti_detalle
*****            c_es_impresion          = c_es_impresion.
*****
*****        DATA(omitir_detalle_vacio) = abap_true.
*****
*****      ENDIF.

*----------------------------------------------------------------------*
* Fin ejecución extracción generica
* 04.01.2022 - SAGIRALD
*----------------------------------------------------------------------*


*...Tabla auxiliar con los campos
      DATA(l_ti_ztcxr1003_3) = g_ti_ztcxr1003_3.
      DELETE l_ti_ztcxr1003_3 WHERE etiqueta_uuid NE l_es_ztcxr1003_1-etiquetauuid.
      IF l_ti_ztcxr1003_3 IS INITIAL.
        ".No existen campos para la etiqueta &.
*        MESSAGE e002(zcx001) WITH l_es_ztcxr1003_1-etiquetaid.
        APPEND VALUE bapiret2( id         = 'ZCX001'
                               number     = '002'
                               type       = 'E'
                               message_v1 = l_es_ztcxr1003_1-etiquetaid ) TO c_ti_bapiret2.
        RETURN.
      ENDIF.

      ".Obtener el codigo sato de la etiqueta dinámica
*----------------------------------------------------------------------*
* Inicio ejecución dinamico en fondo
* 04.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
      IF l_es_ztcxr1003_5-campolocal IS INITIAL OR i_c_ftp EQ abap_true.
        READ ENTITIES OF zi_rap_ztcxr1003_1
        ENTITY etiquetas  BY \_ztcxr1003_6
        ALL   FIELDS WITH VALUE #( ( etiquetauuid = l_es_ztcxr1003_7-etiqueta_uuid ) )
        RESULT DATA(l_ti_ztcxr1003_6).

        IF lines( l_ti_ztcxr1003_6 ) > 0.
*          et_dinamica = abap_true.
        ENDIF.
      ENDIF.

      IF et_dinamica EQ abap_true.
***      IF l_es_ztcxr1003_5-campolocal IS INITIAL.
***
***        READ ENTITIES OF zi_rap_ztcxr1003_1
***        ENTITY etiquetas  BY \_ztcxr1003_6
***        ALL   FIELDS WITH VALUE #( ( etiquetauuid = l_es_ztcxr1003_7-etiqueta_uuid ) )
***        RESULT DATA(l_ti_ztcxr1003_6).
*----------------------------------------------------------------------*
* Fin ejecución dinamico en fondo
* 04.01.2022 - SAGIRALD
*----------------------------------------------------------------------*
        SORT l_ti_ztcxr1003_6 BY linea.

        IF l_ti_ztcxr1003_6 IS INITIAL.
          ".La etiqueta & dinámica no tiene código.
*          MESSAGE e002(zcx001) WITH l_es_ztcxr1003_1-etiquetaid.
          APPEND VALUE bapiret2( id         = 'ZCX001'
                                 number     = '002'
                                 type       = 'E'
                                 message_v1 = l_es_ztcxr1003_1-etiquetaid ) TO c_ti_bapiret2.
          RETURN.
        ENDIF.

        DATA(l_ti_archivo) = VALUE zttcxr1003_c1024(  ).
        ".Poner los valores del encabezado
        CALL METHOD me->reemplazar_dato
          EXPORTING
            i_c_tipo     = g_cte_c
            i_es_detalle = c_es_impresion
            i_ti_campos  = l_ti_ztcxr1003_3
            i_ti_codsato = l_ti_ztcxr1003_6
          CHANGING
            c_ti_texto   = l_ti_archivo.

*     Reemplazar los valores de los detalles
        CALL METHOD me->reemplazar_detalle
          EXPORTING
            i_i_alto     = l_es_ztcxr1003_1-alto
            i_i_comienzo = l_es_ztcxr1003_1-comienzo
            i_ti_detalle = c_ti_detalle
            i_ti_campos  = l_ti_ztcxr1003_3
            i_ti_codsato = l_ti_ztcxr1003_6
          CHANGING
            c_ti_texto   = l_ti_archivo.

        ".Poner los valores del encabezado
        CALL METHOD me->reemplazar_dato
          EXPORTING
            i_c_tipo     = g_cte_p
            i_es_detalle = c_es_impresion
            i_ti_campos  = l_ti_ztcxr1003_3
            i_ti_codsato = l_ti_ztcxr1003_6
          CHANGING
            c_ti_texto   = l_ti_archivo.

        DATA(l_es_print_parameters) = VALUE pri_params(  ).
        DATA(l_c_valid_flag) = VALUE char1( ).

        CALL FUNCTION 'GET_PRINT_PARAMETERS'
          EXPORTING
            no_dialog            = 'X'
            user                 = sy-uname
          IMPORTING
            out_parameters       = l_es_print_parameters
            valid                = l_c_valid_flag
          EXCEPTIONS
            invalid_print_params = 2
            OTHERS               = 4.

        NEW-PAGE PRINT ON PARAMETERS l_es_print_parameters NO DIALOG.

        LOOP AT l_ti_archivo INTO DATA(l_s_linea).
          " Corrección codigo de barras, ejecutando un salto de liena
          WRITE:/ l_s_linea.
        ENDLOOP.

        NEW-PAGE PRINT OFF.
*----------------------------------------------------------------------*
* Inicio imprimir varias veces la etiqueta si falta detalle
* 26.09.2022 - SAGIRALD
*----------------------------------------------------------------------*
        CONCATENATE l_s_filename '-' l_s_consecuti '.JOB' INTO l_s_filename.

        CONCATENATE 'PRINT' l_c_numcopias INTO l_s_linea SEPARATED BY space.
        APPEND l_s_linea  TO l_ti_archivo.

        "Cerrar el archivo
        l_s_linea  = 'QUIT'.
        APPEND l_s_linea  TO l_ti_archivo.

        guardar_archivos( i_ti_archivo     = l_ti_archivo
                          i_es_ztcxr1003_1 = CORRESPONDING #( l_es_ztcxr1003_1 )
                          i_es_ztcxr1003_5 = CORRESPONDING #( l_es_ztcxr1003_5 )
                          i_ftp            = i_c_ftp
                          i_nombre_archivo = l_s_filename ).

*.se completa el nombre del archivo(txt) adicionando la impresora para enviarlo junto con la secuencia de escape
      ELSE.

        IF c_ti_detalle IS INITIAL.
          APPEND CORRESPONDING #( c_es_impresion ) TO c_ti_detalle.
          DATA(detalle_agregado) = abap_true.
        ENDIF.
        DATA(ti_detalle) = c_ti_detalle.

        WHILE lines( ti_detalle ) > 0.

          GET TIME STAMP FIELD timestamp.
          l_s_filename = timestamp.
          REPLACE ALL OCCURRENCES OF '.' IN l_s_filename WITH ''.

          IF i_n_sufijo IS INITIAL.
            CONCATENATE sy-uname '-' l_s_filename INTO l_s_filename.
          ELSE.
            CONCATENATE sy-uname '-' l_s_filename '-' i_n_sufijo INTO l_s_filename.
          ENDIF.

          DATA(consecutivo) = |{ l_s_consecuti }-{ sy-index }|.
          generar_archivo_plano( EXPORTING
                                    i_es_impresion         = c_es_impresion
                                    i_ti_ztcxr1003_3       = l_ti_ztcxr1003_3
                                    i_es_ztcxr1003_1       = CORRESPONDING #( l_es_ztcxr1003_1 )
                                    i_es_ztcxr1003_5       = CORRESPONDING #( l_es_ztcxr1003_5 )
                                    i_impresora            = l_s_impresora
                                    i_consecutivo          = consecutivo "l_s_consecuti
                                    i_sin_detalle          = detalle_agregado
                                    i_numero_copias        = l_c_numcopias
                                    i_remover_ceros        = i_remover_ceros
                                    i_omitir_vacio_detalle = omitir_detalle_vacio
                                 IMPORTING
                                    e_ti_detalle           = DATA(ti_detalle_sobrante)
                                 CHANGING
                                    c_ti_detalle           = ti_detalle
                                    c_ti_archivo           = l_ti_archivo
                                    c_nombre_archivo       = l_s_filename ).

          guardar_archivos( i_ti_archivo     = l_ti_archivo
                            i_es_ztcxr1003_1 = CORRESPONDING #( l_es_ztcxr1003_1 )
                            i_es_ztcxr1003_5 = CORRESPONDING #( l_es_ztcxr1003_5 )
                            i_ftp            = i_c_ftp
                            i_nombre_archivo = l_s_filename ).
          ti_detalle = ti_detalle_sobrante.
          CLEAR: l_s_filename, l_ti_archivo.
        ENDWHILE.


*            CONCATENATE 'LABEL' l_es_ztcxr1003_1-archivo2 INTO l_s_linea SEPARATED BY space.
*            APPEND l_s_linea TO l_ti_archivo.
**     Adicionar la primer linea a la etiqueta que es la impresora
*            CONCATENATE 'PRINTER' l_s_impresora INTO l_s_linea SEPARATED BY space.
*            APPEND l_s_linea TO l_ti_archivo.
*
*            DATA(l_i_numreg) = lines( c_ti_detalle ).
**----------------------------------------------------------------------*
** Inicio Validación que el denominador no sea cero
** 04.01.2022 - SAGIRALD
**----------------------------------------------------------------------*
*            IF l_es_ztcxr1003_1-numdet IS NOT INITIAL.
**          BREAK sagirald.
*              DATA(l_i_result) = l_i_numreg MOD l_es_ztcxr1003_1-numdet.
*            ENDIF.
**----------------------------------------------------------------------*
** Fin Validación que el denominador no sea cero
** 04.01.2022 - SAGIRALD
**----------------------------------------------------------------------*
*            IF l_i_result GT 0.
*              l_i_numreg = l_i_numreg + l_es_ztcxr1003_1-numdet - l_i_result.
*            ENDIF.
*
*            DATA(l_i_idx) = VALUE i( ).
*            l_i_idx = 0.
**----------------------------------------------------------------------*
** Inicio ejecución con detalle
** 04.01.2022 - SAGIRALD
**----------------------------------------------------------------------*
**        IF l_i_numreg EQ 0.
**----------------------------------------------------------------------*
** Fin ejecución con detalle
** 04.01.2022 - SAGIRALD
**----------------------------------------------------------------------*
*
*            DATA(l_s_valor) = VALUE string(  ).
*            "Cambiar el encabezado
*            LOOP AT l_ti_ztcxr1003_3 INTO DATA(l_es_ztcxr1003_3) WHERE reg_det <> 'X'.
*
*              ASSIGN l_es_ztcxr1003_3-campo_id TO FIELD-SYMBOL(<l_fs_campo>).
*
*              ASSIGN COMPONENT <l_fs_campo> OF STRUCTURE c_es_impresion TO FIELD-SYMBOL(<l_fs_valor>).
*
*              MOVE <l_fs_valor> TO l_s_valor.
*
**         Busca el tipo de dato del campo para saber si es numerico y posteriormentes validar que no sea
**         cero (caso en el que se enviara en blanco
*              READ TABLE g_th_dd03l INTO DATA(l_es_dd031) WITH TABLE KEY
*                                   fieldname = l_es_ztcxr1003_3-campo_id.
**----------------------------------------------------------------------*
** Inicio remover los ceros a la derecha cuando son decimales
** 23.05.2022 - SAGIRALD
**----------------------------------------------------------------------*
*              IF l_s_valor CS '.' AND i_remover_ceros EQ abap_true.
*                SPLIT l_s_valor AT '.' INTO DATA(valor1) DATA(valor2) DATA(valor3).
*                IF valor3 IS INITIAL.
***              "Validar si es un numero
***              CALL FUNCTION 'CATS_ITS_MAKE_STRING_NUMERICAL'
***                EXPORTING
***                  input_string  = condense( l_s_valor )
***                IMPORTING
***                  value         = numero
***                EXCEPTIONS
***                  not_numerical = 1
***                  OTHERS        = 2.
***              IF sy-subrc = 0.
***                DO.
***                  DATA(longitud) = strlen( l_s_valor ) - 1.
***                  "Validar si el ultimo digito es cero para remover los decimales a la izq (Solo los cero)
***                  IF l_s_valor+longitud(1) EQ '0'.
***                    l_s_valor = l_s_valor(longitud).
***                  ELSEIF l_s_valor+longitud(1) EQ '.'.
***                    l_s_valor = l_s_valor(longitud).
***                    EXIT.
***                  ELSE.
***                    EXIT.
***                  ENDIF.
***                ENDDO.
***              ENDIF.
*                  remover_ceros( CHANGING c_valor_campo = l_s_valor ).
*                ENDIF.
*                CLEAR: valor1, valor2, valor3.
*              ENDIF.
**----------------------------------------------------------------------*
** Fin remover los ceros a la derecha cuando son decimales
** 23.05.2022 - SAGIRALD
**----------------------------------------------------------------------*
*              CONCATENATE 'SET' l_es_ztcxr1003_3-campo_id '=' '"' INTO l_s_linea SEPARATED BY space.
*
*
*
*              IF l_es_dd031-inttype IN g_ra_ti_inttype[].
*                MOVE l_s_valor TO l_n_valor.
*                IF l_n_valor = 0.
*                  CONCATENATE l_s_linea '"' INTO l_s_linea.
*                ELSE.
*                  DATA(l_i_valor_trun) = VALUE i(  ).
*                  l_i_valor_trun = trunc( l_n_valor ).
*                  DATA(l_n_diferencia) = l_n_valor - l_i_valor_trun.
**                mod.ini.aalfaroh SA50836
*                  "Si los decimales son difs a 0, o el parametro dice q este centro debe mostrar decimales
*                  IF l_n_diferencia         >  0.
**                OR g_c_mostrar_decimales  EQ l_cte_x.
*                    MOVE l_n_valor TO l_s_valor.
*                    CONDENSE l_s_valor.
*                    CONCATENATE l_s_linea l_s_valor '"' INTO l_s_linea.
**                mod.fin.aalfaroh SA50836
*                  ELSE.
*                    MOVE l_i_valor_trun TO l_s_valor.
*                    CONCATENATE l_s_linea l_s_valor '"' INTO l_s_linea.
*                  ENDIF.
*
*                ENDIF.
*              ELSE.
*
*                CONCATENATE l_s_linea l_s_valor '"' INTO l_s_linea.
*
*              ENDIF.
*
*              APPEND l_s_linea TO l_ti_archivo.
*
*            ENDLOOP.
**----------------------------------------------------------------------*
** Inicio ejecución con detalle
** 04.01.2022 - SAGIRALD
**----------------------------------------------------------------------*
*            DATA(indice)     = VALUE i( ).
*
*            "Cambiar el detalle
*            LOOP AT c_ti_detalle ASSIGNING FIELD-SYMBOL(<fs_es_detalle>).
*              indice = indice + 1.
*              LOOP AT l_ti_ztcxr1003_3 ASSIGNING FIELD-SYMBOL(<fs_es_ztcxr1003_3>) WHERE reg_det = abap_true.
*
*                DATA(campo) = |<FS_ES_DETALLE>-{ <fs_es_ztcxr1003_3>-campo_id }|.
*
*                ASSIGN (campo) TO FIELD-SYMBOL(<fs_valor>).
*                DATA(valor_campo) = |{ <fs_valor> }|.
*                "*----------------------------------------------------------------------*
*                "* Inicio remover los ceros a la derecha cuando son decimales
*                "* 23.05.2022 - SAGIRALD
*                "*----------------------------------------------------------------------*
*                "Si contiene puntos (verificar si son decimales)
*                IF valor_campo CS '.' AND i_remover_ceros EQ abap_true.
*                  SPLIT valor_campo AT '.' INTO valor1 valor2 valor3.
*                  IF valor3 IS INITIAL.
***                "Validar si es un numero
***                CALL FUNCTION 'CATS_ITS_MAKE_STRING_NUMERICAL'
***                  EXPORTING
***                    input_string  = condense( valor_campo )
***                  IMPORTING
***                    value         = numero
***                  EXCEPTIONS
***                    not_numerical = 1
***                    OTHERS        = 2.
***                IF sy-subrc = 0.
***                  DO.
***                    longitud = strlen( valor_campo ) - 1.
***                    "Validar si el ultimo digito es cero para remover los decimales a la izq (Solo los cero)
***                    IF valor_campo+longitud(1) EQ '0'.
***                      valor_campo = valor_campo(longitud).
***                    ELSEIF valor_campo+longitud(1) EQ '.'.
***                      valor_campo = valor_campo(longitud).
***                      EXIT.
***                    ELSE.
***                      EXIT.
***                    ENDIF.
***                  ENDDO.
***                ENDIF.
*                    remover_ceros( CHANGING c_valor_campo = valor_campo ).
*                  ENDIF.
*                  CLEAR: valor1, valor2, valor3.
*                ENDIF.
*                "*----------------------------------------------------------------------*
*                "* Inicio omitir los valores vacios cuando es extracción de UMp
*                "* 02.09.2022 - SAGIRALD
*                "*----------------------------------------------------------------------*
*                IF omitir_detalle_vacio = abap_true AND <fs_valor> IS ASSIGNED AND <fs_valor> IS INITIAL.
*                  CONTINUE.
*                ENDIF.
*                "*----------------------------------------------------------------------*
*                "* Fin omitir los valores vacios cuando es extracción de UMp
*                "* 02.09.2022 - SAGIRALD
*                "*----------------------------------------------------------------------*
*                "*----------------------------------------------------------------------*
*                "* Fin remover los ceros a la derecha cuando son decimales
*                "* 23.05.2022 - SAGIRALD
*                "*----------------------------------------------------------------------*
*                IF <fs_valor> IS ASSIGNED.
*
*                  l_s_linea = |SET { <fs_es_ztcxr1003_3>-campo_id }{ indice } = "{ COND char100( WHEN <fs_valor> IS INITIAL THEN space ELSE |{ valor_campo }| ) }"|.
*                  APPEND l_s_linea TO l_ti_archivo.
*                  UNASSIGN: <fs_valor>.
*
*                ENDIF.
*
*              ENDLOOP.
*            ENDLOOP.
*
*
**        ELSE.
**
**
**        ENDIF.
**----------------------------------------------------------------------*
** Fin ejecución con detalle
** 04.01.2022 - SAGIRALD
**----------------------------------------------------------------------*
*
*            IF l_es_ztcxr1003_5-impresoraid IS NOT INITIAL.
*              CONCATENATE '-' l_es_ztcxr1003_5-impresoraid INTO DATA(l_c_idimpr).
*            ENDIF.
*          ENDIF.
**      ENDIF.
*
*          CONCATENATE l_s_filename '-' l_s_consecuti l_c_idimpr '.JOB' INTO DATA(l_s_filename3).
*
**----------------------------------------------------------------------*
** Inicio validación cuando sea dinamica
** 23.05.2022 - SAGIRALD
**----------------------------------------------------------------------*
*          IF et_dinamica NE abap_true.
**----------------------------------------------------------------------*
** Fin validación cuando sea dinamica
** 23.05.2022 - SAGIRALD
**----------------------------------------------------------------------*
**           Poner el numero de copias
*            CONCATENATE 'PRINT' l_c_numcopias INTO l_s_linea SEPARATED BY space.
*            APPEND l_s_linea  TO l_ti_archivo.
*
**     Cerrar el archivo
*            l_s_linea  = 'QUIT'.
*            APPEND l_s_linea  TO l_ti_archivo.
**----------------------------------------------------------------------*
** Inicio validación cuando sea dinamica
** 23.05.2022 - SAGIRALD
**----------------------------------------------------------------------*
*          ENDIF.
**----------------------------------------------------------------------*
** Fin validación cuando sea dinamica
** 23.05.2022 - SAGIRALD
**----------------------------------------------------------------------*
**----------------------------------------------------------------------*
** Inicio Impresión en servidor
** 27.07.2021 - SAGIRALD
**----------------------------------------------------------------------*
*
*          IF l_es_ztcxr1003_5-campolocal IS INITIAL OR i_c_ftp EQ abap_true OR ( sy-tcode = '/SCWM/RFUI' OR sy-batch = abap_true ).
**      IF l_es_ztcxr1003_5-campolocal IS INITIAL.
*
*            DATA(l_s_filename2) = l_s_filename3.
*            CONDENSE l_s_filename2 NO-GAPS.
*            MOVE l_ti_archivo TO l_ti_archivo2.
*            DATA(l_aplicacion)  = g_cte_ftp. "ETIQUETAS2 (Nuevo Servidor FTP)
*
*            IF l_es_ztcxr1003_1-ftp IS INITIAL.
*              l_aplicacion = g_cte_aplica.
*            ENDIF.
*
*            zclcxr1002_dir_interfaces=>directorio_de_interfaces( EXPORTING i_accion       = 'W'
*                                                                           i_archivo      = l_s_filename2
*                                                                           i_tipo_archivo = 'O'
*                                                                           i_modulo       = g_cte_modulo
*                                                                           i_aplicacion   = l_aplicacion
*                                                                           i_id_interface = g_cte_id
*                                                                 IMPORTING e_es_interface = l_es_interface
*                                                                           e_ti_bapiret2  = l_ti_bapiret2
*                                                                 CHANGING  c_ti_archivo   = l_ti_archivo2 ).
*
*            IF l_ti_bapiret2 IS INITIAL.
*
*              l_es_ztcxr1003_8-id_etiqueta = l_es_ztcxr1003_1-etiquetaid.
*              l_es_ztcxr1003_8-servidor = l_es_interface-host_destino.
*
*              DATA(command) = VALUE char255( ).
*              command       = |sh /usr/sap/interfaces/etiquetas/shell/ftp.sh { l_es_interface-host_destino } { to_lower( l_es_interface-usuario ) }| &
*                              | { to_lower( l_es_interface-password ) } { to_lower( l_es_interface-dir_destino ) } { l_s_filename2 }|.
*
*              IF g_imprimir_en_servidor EQ abap_true.
*                CALL 'SYSTEM' ID 'COMMAND' FIELD command.
*              ENDIF.
*
*            ELSE.
*              CLEAR: l_ti_bapiret2.
*            ENDIF.
*
**----------------------------------------------------------------------*
** Fin Impresión en servidor
** 27.07.2021 - SAGIRALD
**----------------------------------------------------------------------*
*
*          ELSE.
*
**        IF line_exists( g_es_parametros-dirlocal[ 1 ] ).
*            DATA(l_c_dirlocal) = g_es_parametros-dirlocal.
**        ENDIF.
*
*            CONCATENATE l_c_dirlocal l_s_filename3 INTO l_s_filename3.
*
*            CALL FUNCTION 'GUI_DOWNLOAD'
*              EXPORTING
*                filename                = l_s_filename3
*              TABLES
*                data_tab                = l_ti_archivo
*              EXCEPTIONS
*                file_write_error        = 1
*                no_batch                = 2
*                gui_refuse_filetransfer = 3
*                invalid_type            = 4
*                no_authority            = 5
*                unknown_error           = 6
*                header_not_allowed      = 7
*                separator_not_allowed   = 8
*                filesize_not_allowed    = 9
*                header_too_long         = 10
*                dp_error_create         = 11
*                dp_error_send           = 12
*                dp_error_write          = 13
*                unknown_dp_error        = 14
*                access_denied           = 15
*                dp_out_of_memory        = 16
*                disk_full               = 17
*                dp_timeout              = 18
*                file_not_found          = 19
*                dataprovider_exception  = 20
*                control_flush_error     = 21
*                OTHERS                  = 22.
*            IF sy-subrc <> 0.
*              ".Error escribiendo archivo de etiqueta &1 en servidor Label Gallery
*              MESSAGE e023(zcx01).
*
*            ENDIF.
*
*            CLEAR:
*              l_ti_archivo,
*              c_es_impresion-copias,
*              l_c_idimpr.
*----------------------------------------------------------------------*
* Fin imprimir varias veces la etiqueta si falta detalle
* 26.09.2022 - SAGIRALD
*----------------------------------------------------------------------*

      ENDIF.


    ENDLOOP.




  ENDMETHOD.


  METHOD reemplazar_dato.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Método       :reemplazar_dato
* Descripción  : Datos Cabecera Etiqueta
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 8 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 8 mar. 2021    xxxxxx         Cristian Montoya  Version Inicial
*----------------------------------------------------------------------*

*----------------------------------------------------------------------*
*             Definiciòn Variables
*----------------------------------------------------------------------*
    DATA:
      l_s_valor      TYPE string,
      l_s_space      TYPE c LENGTH 2 VALUE ' ',
      l_n_valor      TYPE p DECIMALS 2,
      l_n_diferencia TYPE p DECIMALS 2.

*----------------------------------------------------------------------*
*                       Definiciòn Rangos
*----------------------------------------------------------------------*
    DATA:
     l_ra_ti_inttype TYPE RANGE OF char1.

    l_ra_ti_inttype = VALUE #( sign   = g_cte_i
                           option   = g_cte_eq
                            ( low   = 'i' )
                            ( low   = 'b')
                            ( low   = 's')
                            ( low   = 'P')
                            ( low   = 'F' ) ).



    " Doble espacio para el espacio simple de los datos
    CONCATENATE space space INTO DATA(l_s_doblespacio).

    LOOP AT i_ti_codsato INTO DATA(l_es_codsato) WHERE tipolinea = i_c_tipo.
      DATA(l_s_cadena) = l_es_codsato-dato.
      "{SGR Inicio
      SPLIT l_s_cadena AT '<' INTO TABLE DATA(ti_split).
      LOOP AT ti_split ASSIGNING FIELD-SYMBOL(<fs_es_split>).
        IF <fs_es_split> CS '>'.
          SPLIT <fs_es_split> AT '>' INTO DATA(campo) DATA(otro).
          IF strlen( campo ) = 6.
            EXIT.
          ENDIF.
        ENDIF.
      ENDLOOP.
      LOOP AT i_ti_campos INTO DATA(l_es_campos) WHERE campo_id = campo.
*      LOOP AT i_ti_campos INTO DATA(l_es_campos).
        "{SGR Fin
        ASSIGN l_es_campos-campo_id TO FIELD-SYMBOL(<l_fs_campo>).
        ASSIGN COMPONENT <l_fs_campo> OF STRUCTURE i_es_detalle
        TO FIELD-SYMBOL(<l_fs_valor>).
        MOVE <l_fs_valor> TO l_s_valor.

        "Tomer el valor real en string
        CONCATENATE '<' l_es_campos-campo_id '>' INTO DATA(l_s_nomcampo).

        READ TABLE g_th_dd03l INTO DATA(l_es_dd031) WITH TABLE KEY
                             fieldname = l_es_campos-campo_id.


        IF l_es_dd031-inttype IN l_ra_ti_inttype[].
          MOVE l_s_valor TO l_n_valor.
          IF l_n_valor = 0.
            CLEAR l_s_valor.
          ELSE.
            REPLACE ALL OCCURRENCES OF '.' IN l_s_valor WITH ','.
          ENDIF.
        ELSE.

          SPLIT l_s_valor AT space INTO TABLE DATA(l_ti_linea).
          DELETE l_ti_linea WHERE table_line EQ space.
          LOOP AT l_ti_linea INTO DATA(l_es_linea).
            IF sy-tabix = 1.
              l_s_valor = l_es_linea.
            ELSE.
**            CONCATENATE l_s_valor space g_es_linea INTO l_s_valor SEPARATED BY space."{-@Cambio 27092017 hjsotelo}
              CONCATENATE l_s_valor l_es_linea INTO l_s_valor SEPARATED BY l_s_space.  "{+@Cambio 27092017 hjsotelo}
            ENDIF.
          ENDLOOP.



        ENDIF.

        "Reemplazar la variable con su valor
        REPLACE ALL OCCURRENCES OF l_s_nomcampo IN l_s_cadena WITH l_s_valor.
        CLEAR l_s_valor.
        "{SGR Inicio
*        APPEND l_s_cadena TO c_ti_texto.
        "{SGR Fin

      ENDLOOP.
      "{SGR Inicio
      IF l_s_cadena CS |<{ campo }>|.
        "Reemplazar la variable con vacio
        REPLACE ALL OCCURRENCES OF |<{ campo }>| IN l_s_cadena WITH space.
      ENDIF.
      APPEND l_s_cadena TO c_ti_texto.
      "{SGR Fin
    ENDLOOP.

  ENDMETHOD.


  METHOD reemplazar_detalle.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Método       : reemplazar_detalle
* Descripción  : Reemplezar Campos de la Etiqueta
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 8 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 8 mar. 2021    xxxxxx         Cristian Montoya  Version Inicial
*----------------------------------------------------------------------*

*----------------------------------------------------------------------*
*               Definición de Datos
*----------------------------------------------------------------------*
    DATA:
      l_s_valor      TYPE string,
      l_n_valor      TYPE p DECIMALS 2,
      l_n_diferencia TYPE p DECIMALS 2.

*----------------------------------------------------------------------*
*                       Definiciòn Rangos
*----------------------------------------------------------------------*
    DATA:
     l_ra_ti_inttype TYPE RANGE OF char1.

    l_ra_ti_inttype = VALUE #( sign   = g_cte_i
                           option   = g_cte_eq
                            ( low   = 'i' )
                            ( low   = 'b')
                            ( low   = 's')
                            ( low   = 'P')
                            ( low   = 'F' ) ).


    "Cantidad Registros
    DATA(l_i_catdet)   = lines( i_ti_detalle ).
    DATA(l_i_catplant) = lines( i_ti_codsato ).

    DATA(l_i_alto) = VALUE zi_rap_ztcxr1003_1-comienzo( ).
    DATA(l_s_alto) = VALUE string( ).

    LOOP AT i_ti_detalle INTO DATA(l_es_detalle).
      DATA(l_i_contdetl) = sy-tabix.

      IF l_i_contdetl EQ 1.
        l_i_alto = i_i_comienzo.
      ELSE.
        l_i_alto = l_i_alto + i_i_alto.
      ENDIF.

      l_s_alto = l_i_alto.

      LOOP AT i_ti_codsato INTO DATA(l_es_codsato) WHERE tipolinea EQ 'D'.

        DATA(l_i_contpant) = sy-tabix.
        DATA(l_s_cadena)   = l_es_codsato-dato.
        ".Reemplazar el alto del detalle
        DATA(l_s_nomcampo) = '<ALTO>'.

        "Reemplazar la variable con su valor
        REPLACE ALL OCCURRENCES OF l_s_nomcampo IN l_s_cadena WITH l_s_alto.

        "{SGR Inicio
        SPLIT l_s_cadena AT '<' INTO TABLE DATA(ti_split).
        LOOP AT ti_split ASSIGNING FIELD-SYMBOL(<fs_es_split>).
          IF <fs_es_split> CS '>'.
            SPLIT <fs_es_split> AT '>' INTO DATA(campo) DATA(otro).
            IF strlen( campo ) = 6.
              EXIT.
            ENDIF.
          ENDIF.
        ENDLOOP.
        LOOP AT i_ti_campos INTO DATA(l_es_campos) WHERE campo_id = campo.
*      LOOP AT i_ti_campos INTO DATA(l_es_campos).
          "{SGR Fin

          ASSIGN l_es_campos-campo_id TO FIELD-SYMBOL(<l_fs_campo>).
          ASSIGN COMPONENT <l_fs_campo> OF STRUCTURE l_es_detalle
          TO FIELD-SYMBOL(<l_fs_valor>).
          MOVE <l_fs_valor> TO l_s_valor.

          "Tomer el valor real en string
          CONCATENATE '<' l_es_campos-campo_id '>' INTO l_s_nomcampo.

          READ TABLE g_th_dd03l INTO DATA(l_es_dd031) WITH TABLE KEY
                               fieldname = l_es_campos-campo_id.


          IF l_es_dd031-inttype IN l_ra_ti_inttype[].
            MOVE l_s_valor TO l_n_valor.
            IF l_n_valor = 0.
              CLEAR l_s_valor.
            ELSE.
              REPLACE ALL OCCURRENCES OF '.' IN l_s_valor WITH ','.
            ENDIF.
          ELSE.

            SPLIT l_s_valor AT space INTO TABLE DATA(l_ti_linea).
            DELETE l_ti_linea WHERE table_line EQ space.
            LOOP AT l_ti_linea INTO DATA(l_es_linea).
              IF sy-tabix = 1.
                l_s_valor = l_es_linea.
              ELSE.
                CONCATENATE l_s_valor l_es_linea INTO l_s_valor SEPARATED BY space.
              ENDIF.

            ENDLOOP.

          ENDIF.

          "Reemplazar la variable con su valor
          REPLACE ALL OCCURRENCES OF l_s_nomcampo IN l_s_cadena WITH l_s_valor.
          CLEAR:
           l_s_valor.


        ENDLOOP.
*...*.Se valida si es la ultia lina del la plantilla
        IF l_i_contpant EQ l_i_catplant.
*.Se valida si no existen mas detalle
          IF l_i_contdetl EQ l_i_catdet.
            APPEND l_s_cadena TO c_ti_texto.
          ELSE.
            CONTINUE.
          ENDIF.
        ELSE.
          APPEND l_s_cadena TO c_ti_texto.
        ENDIF.

      ENDLOOP.
    ENDLOOP.

    IF sy-subrc NE 0 AND i_ti_detalle IS INITIAL.

      LOOP AT i_ti_codsato INTO l_es_codsato WHERE tipolinea EQ 'D'.
*.Auque para este escenario las posciciones simpre van a estar vacias,
*.Esneceario la logica para completar las plantillas
        l_s_cadena = l_es_codsato-dato.
*...Se define la altura de la posicion.
        l_s_alto = i_i_comienzo.
*.Reemplazar el alto del detalle
        l_s_nomcampo = '<ALTO>'.

        "Reemplazar la variable con su valor
        REPLACE ALL OCCURRENCES OF l_s_nomcampo IN l_s_cadena WITH l_s_alto.

        LOOP AT i_ti_campos INTO DATA(l_es_campo).

          ASSIGN l_es_campo-campo_id TO <l_fs_campo>.
*Asigna el valor de la estructrua detalle a el fielsymbols
          ASSIGN COMPONENT <l_fs_campo> OF STRUCTURE l_es_detalle
                                    TO <l_fs_valor> .
*.Se mueve el valor de cursor a la variables
          MOVE <l_fs_valor> TO l_s_valor.
*.Comcatena el nombre del las posiciones de los campos
          CONCATENATE '<' l_es_campo-campo_id '>' INTO l_s_nomcampo.

          "Reemplazar la variable con su valor
          REPLACE ALL OCCURRENCES OF l_s_nomcampo IN l_s_cadena WITH l_s_valor.



        ENDLOOP.

        APPEND l_s_cadena TO c_ti_texto.
*Se limpian las variables
        CLEAR l_s_cadena.
      ENDLOOP.
    ENDIF.



  ENDMETHOD.


  METHOD consumir_api.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : ZSDXXXXXX
* Método       : consumir_api
* Descripción  : Consumir API Impresión
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 26 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 26 mar. 2021    xxxxxx         Cristian Montoya  Version Inicial
*----------------------------------------------------------------------*

*----------------------------------------------------------------------*
*            Definición de Constantes
*----------------------------------------------------------------------*
    DATA:
      l_cte_application TYPE string VALUE 'application/json;charset=UTF-8',
      l_cte_plantilla   TYPE char20 VALUE '"nombrePlantilla":',
      l_cte_impresora   TYPE char15 VALUE '"impresora":',
      l_cte_grupo       TYPE char20 VALUE 'grupoEtiquetas":[{ ',
      l_cte_cantidad    TYPE char20 VALUE 'cantidad":',
      l_cte_variables   TYPE char20 VALUE 'variables":{',
      l_cte_label       TYPE char10 VALUE 'LABEL',
      l_cte_zecxr1003_2 TYPE ddobjname VALUE 'ZECXR1003_2'.

*----------------------------------------------------------------------*
*            Definición de Tablas Internas
*----------------------------------------------------------------------*
    DATA:
     l_ti_campos TYPE STANDARD TABLE OF dfies.

*----------------------------------------------------------------------*
*            Definición de Variables
*----------------------------------------------------------------------*
    DATA:
      l_s_msg_json  TYPE string,
      l_s_url       TYPE string,
      l_i_tabix     TYPE i,
      l_i_tabix_imp TYPE i,
      l_c_coma      TYPE char3,
      l_c_rpta      TYPE char255,
      l_c_llaves    TYPE char3.

    l_s_msg_json = '{' && l_cte_plantilla && l_cte_label && 'ETIQUETA-ARCHIVO22",'.

*...Campos Etiqueta
    CALL FUNCTION 'DDIF_FIELDINFO_GET'
      EXPORTING
        tabname        = l_cte_zecxr1003_2
      TABLES
        dfies_tab      = l_ti_campos
      EXCEPTIONS
        not_found      = 1
        internal_error = 2
        OTHERS         = 3.

    READ TABLE i_ti_impresion INTO DATA(l_es_impresion) INDEX 1.

    "Impresora
    l_s_msg_json = l_s_msg_json && '"' && l_es_impresion-id_impr && '",'.
    "Grupo Etiquetas
    l_s_msg_json = l_s_msg_json && l_cte_grupo.


    "Se recorre las etiquetas a imprimir
    LOOP AT i_ti_impresion INTO l_es_impresion.
      ADD 1 TO l_i_tabix_imp.

      "Cantidad
      l_s_msg_json = l_s_msg_json && l_cte_cantidad && l_es_impresion-copias && ','.
      "Variables
      l_s_msg_json = l_s_msg_json && l_cte_variables.

      CLEAR:
       l_i_tabix.
      "Se recorre los campos para etiqueta
      LOOP AT l_ti_campos INTO DATA(l_es_campos).
        ADD 1 TO l_i_tabix.

        ASSIGN COMPONENT l_es_campos-fieldname OF STRUCTURE l_es_impresion TO FIELD-SYMBOL(<l_fs_campo>).
        IF sy-subrc NE 0.
          CONTINUE.
        ENDIF.

        IF <l_fs_campo> IS INITIAL.
          CONTINUE.
        ENDIF.


        IF l_i_tabix EQ lines( l_ti_campos ).
          l_c_coma = '"}}'.
        ELSE.
          l_c_coma = '",'.
        ENDIF.
        "Campo
        l_s_msg_json = l_s_msg_json && '"' && l_es_campos-fieldname &&  '": "' && <l_fs_campo> && l_c_coma.


      ENDLOOP.

      IF l_i_tabix_imp EQ lines( i_ti_impresion ).
        l_c_llaves = ']}'.
      ELSE.
        l_c_llaves = ',{'.
      ENDIF.

      "Nueva Etiqueta
      l_s_msg_json = l_s_msg_json && l_c_llaves.

    ENDLOOP.


    "Endpoint Servicio
*    IF line_exists( g_es_parametros-url_srv[ 1 ]  ).
    l_s_url = g_es_parametros-url_srv.
*    ENDIF.

*...crear instancia para realizar peticion http
    cl_http_client=>create_by_url(
      EXPORTING
        url                = l_s_url
      IMPORTING
        client             = DATA(l_o_http_client)
      EXCEPTIONS
        argument_not_found = 1
        plugin_not_active  = 2
        internal_error     = 3
        OTHERS             = 4 ).

*...configuración de cabecera de la peticion
    l_o_http_client->request->set_method( if_http_request=>co_request_method_post ).
    l_o_http_client->propertytype_logon_popup = if_http_client=>co_disabled.
    l_o_http_client->request->set_content_type( content_type = l_cte_application ).

*...Enveloped
    DATA(l_i_xml_len) = strlen( l_s_msg_json ).

    l_o_http_client->request->set_cdata( data = l_s_msg_json offset = 0 length = l_i_xml_len ).

    l_o_http_client->send(
      EXCEPTIONS
        http_communication_failure = 1
        http_invalid_state         = 2 ).


*...retorna respues
    l_o_http_client->receive(
      EXCEPTIONS
        http_communication_failure = 1
        http_invalid_state         = 2
        http_processing_failed     = 3 ).


    IF sy-subrc IS  INITIAL.
      l_c_rpta = l_o_http_client->response->get_cdata( ).
*    e_rc = 0.
    ELSE.
      l_o_http_client->get_last_error(
        IMPORTING
          code    = DATA(l_c_codigo_retorno)    " Return Value, Return Value After ABAP Statements
          message = DATA(l_c_mensaje) ).    " l_c_mensaje Number

      l_c_rpta = l_c_mensaje.
*    e_rc = 4.

    ENDIF.



  ENDMETHOD.


  METHOD clear_variables.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : zclcxr1003_impresion_etiqueta
* Método       : etiqueta
* Descripción  : Limpiar Variables Globales
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 4 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 4 mar. 2021    xxxxxx         Cristian Montoya  Version Inicial
*----------------------------------------------------------------------

    CLEAR:
     g_th_ztcxr1003_1,
     g_th_dd03l,
     g_ti_ztcxr1003_3,
     g_ti_ztcxr1003_4,
     g_ti_ztcxr1003_5,
     g_ti_ztcxr1003_7.

  ENDMETHOD.


  METHOD get_info_etiqueta.
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: R1003
* Clase        : zclcxr1003_impresion_etiqueta
* Método       : get_info_etiqueta
* Descripción  : Obtener Información Etiquetas
* Autor Prog.  : Cristian Montoya
* Fecha Creac. : 4 mar. 2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 4 mar. 2021    xxxxxx         Cristian Montoya  Version Inicial
*----------------------------------------------------------------------

*----------------------------------------------------------------------*
*         Definición de Estructuras
*----------------------------------------------------------------------*
    DATA:
      l_es_impresion TYPE zecxr1003_2.

    SELECT etiqueta_uuid,etiqueta_id
     FROM ztcxr1003_1
     INTO TABLE @DATA(l_ti_ztcxr1003_1)
     WHERE etiqueta_id IN @i_ti_ra_etiquetas.


    LOOP AT l_ti_ztcxr1003_1 INTO DATA(l_es_ztcxr1003_1).

      READ ENTITIES OF zi_rap_ztcxr1003_1
      ENTITY etiquetas BY \_ztcxr1003_7
      ALL FIELDS  "WITH " ( usuariouuid etiquetauuid usuarioid impresoraid campolocal implocalid   )
      WITH VALUE #( ( etiquetauuid = l_es_ztcxr1003_1-etiqueta_uuid    ) )
      RESULT DATA(l_ti_ztcxr1003_7).


      IF line_exists( l_ti_ztcxr1003_7[ 1 ] ).

        READ TABLE l_ti_ztcxr1003_7 INTO DATA(l_es_ztcxr1003_7) INDEX 1.

        APPEND VALUE #(
        etiqueta_id    = l_es_ztcxr1003_1-etiqueta_id
        mensaje        = l_es_ztcxr1003_7-mensajeid
        aplicacion     = l_es_ztcxr1003_7-aplicacion
        accion_ppf     = l_es_ztcxr1003_7-accionppf
        seleccion      = l_es_ztcxr1003_7-seletiqueta ) TO e_ti_impresion.

      ENDIF.

    ENDLOOP.


  ENDMETHOD.


  METHOD obtener_etiqueta.

    SELECT  mensaje_uuid,etiqueta_uuid,mensaje_id,num_copias
      FROM ztcxr1003_7
      WHERE ( ( mensaje_id    EQ @i_mensaje_id AND
                mensaje_id    IS NOT INITIAL ) OR
              ( accion_ppf    EQ @i_accion_ppf AND
                accion_ppf    IS NOT INITIAL ) ) AND
                aplicacion    EQ @i_aplicacion   AND
                sel_etiqueta  EQ @i_sel_etiqueta AND
                sel_etiqueta  IS NOT INITIAL
     INTO TABLE @r_ti_ztcxr1003_7.

  ENDMETHOD.


  METHOD determinar_etiqueta.

    r_ti_ztcxr1003_7 = obtener_etiqueta( i_mensaje_id   = i_mensaje_id
                                         i_accion_ppf   = i_accion_ppf
                                         i_aplicacion   = i_aplicacion
                                         i_sel_etiqueta = i_sel_etiqueta ).
    CHECK r_ti_ztcxr1003_7 IS INITIAL.

    IF i_sel_etiqueta CS '|'.

      SPLIT i_sel_etiqueta AT '|' INTO DATA(valor1) DATA(valor2) DATA(valor3).


      r_ti_ztcxr1003_7 = obtener_etiqueta( i_mensaje_id   = i_mensaje_id
                                           i_accion_ppf   = i_accion_ppf
                                           i_aplicacion   = i_aplicacion
                                           i_sel_etiqueta = CONV #( |{ valor1 }{ '|' }{ valor2 }| ) ).
      CHECK r_ti_ztcxr1003_7 IS INITIAL.

      r_ti_ztcxr1003_7 = obtener_etiqueta( i_mensaje_id   = i_mensaje_id
                                           i_accion_ppf   = i_accion_ppf
                                           i_aplicacion   = i_aplicacion
                                           i_sel_etiqueta = CONV #( valor2 ) ).

      CHECK r_ti_ztcxr1003_7 IS INITIAL.

      r_ti_ztcxr1003_7 = obtener_etiqueta( i_mensaje_id   = i_mensaje_id
                                           i_accion_ppf   = i_accion_ppf
                                           i_aplicacion   = i_aplicacion
                                           i_sel_etiqueta = CONV #( valor1 ) ).

      CHECK r_ti_ztcxr1003_7 IS INITIAL.

    ENDIF.

    r_ti_ztcxr1003_7 = obtener_etiqueta( i_mensaje_id   = i_mensaje_id
                                         i_accion_ppf   = i_accion_ppf
                                         i_aplicacion   = i_aplicacion
                                         i_sel_etiqueta = '*' ).
  ENDMETHOD.

  METHOD generar_archivo_plano.

    DATA: es_linea     LIKE LINE OF c_ti_archivo,
          valor_numero TYPE p DECIMALS 2.

    CONCATENATE 'LABEL' i_es_ztcxr1003_1-archivo2 INTO es_linea SEPARATED BY space.
    APPEND es_linea TO c_ti_archivo.
    "Adicionar la primer linea a la etiqueta que es la impresora
    CONCATENATE 'PRINTER' i_impresora INTO es_linea SEPARATED BY space.
    APPEND es_linea TO c_ti_archivo.

    DATA(numreg) = lines( c_ti_detalle ).

    IF i_es_ztcxr1003_1-numdet IS NOT INITIAL.

      DATA(l_i_result) = numreg MOD i_es_ztcxr1003_1-numdet.
    ENDIF.

    IF l_i_result GT 0.
      numreg = numreg + i_es_ztcxr1003_1-numdet - l_i_result.
    ENDIF.

    DATA(valor) = VALUE string( ).

    "Cambiar el encabezado
    LOOP AT i_ti_ztcxr1003_3 INTO DATA(es_ztcxr1003_3) WHERE reg_det <> 'X'.

      ASSIGN es_ztcxr1003_3-campo_id TO FIELD-SYMBOL(<fs_campo>).

      ASSIGN COMPONENT <fs_campo> OF STRUCTURE i_es_impresion TO FIELD-SYMBOL(<fs_valor>).

      MOVE <fs_valor> TO valor.

*     Busca el tipo de dato del campo para saber si es numerico y posteriormentes validar que no sea
*     cero (caso en el que se enviara en blanco
      READ TABLE g_th_dd03l INTO DATA(es_dd031) WITH TABLE KEY
                           fieldname = es_ztcxr1003_3-campo_id.

      IF valor CS '.' AND i_remover_ceros EQ abap_true.
        SPLIT valor AT '.' INTO DATA(valor1) DATA(valor2) DATA(valor3).
        IF valor3 IS INITIAL.

          remover_ceros( CHANGING c_valor_campo = valor ).

        ENDIF.
        CLEAR: valor1, valor2, valor3.
      ENDIF.

      CONCATENATE 'SET' es_ztcxr1003_3-campo_id '=' '"' INTO es_linea SEPARATED BY space.

      IF es_dd031-inttype IN g_ra_ti_inttype[].
        MOVE valor TO valor_numero.
        IF valor_numero = 0.
          CONCATENATE es_linea '"' INTO es_linea.
        ELSE.
          DATA(valor_truncado) = VALUE i( ).
          valor_truncado = trunc( valor_numero ).
          DATA(diferencia) = valor_numero - valor_truncado.

          "Si los decimales son difs a 0, o el parametro dice q este centro debe mostrar decimales
          IF diferencia > 0.
            MOVE valor_numero TO valor.
            CONDENSE valor.
            CONCATENATE es_linea valor '"' INTO es_linea.
          ELSE.
            MOVE valor_truncado TO valor.
            CONCATENATE es_linea valor '"' INTO es_linea.
          ENDIF.

        ENDIF.
      ELSE.

        CONCATENATE es_linea valor '"' INTO es_linea.

      ENDIF.

      APPEND es_linea TO c_ti_archivo.

    ENDLOOP.

    DATA(indice) = VALUE i( ).
    e_ti_detalle = c_ti_detalle.

    IF line_exists( i_ti_ztcxr1003_3[ reg_det = abap_true ] ) AND i_sin_detalle <> abap_true.
      "Cambiar el detalle
      LOOP AT c_ti_detalle ASSIGNING FIELD-SYMBOL(<fs_es_detalle>).
        indice = indice + 1.
        IF indice > i_es_ztcxr1003_1-numdet.
          EXIT.
        ENDIF.
        DELETE e_ti_detalle INDEX 1.

        LOOP AT i_ti_ztcxr1003_3 ASSIGNING FIELD-SYMBOL(<fs_es_ztcxr1003_3>) WHERE reg_det = abap_true.

          DATA(campo) = |<FS_ES_DETALLE>-{ <fs_es_ztcxr1003_3>-campo_id }|.

          ASSIGN (campo) TO <fs_valor>.
          DATA(valor_campo) = |{ <fs_valor> }|.
          "Si contiene puntos (verificar si son decimales)
          IF valor_campo CS '.' AND i_remover_ceros EQ abap_true.
            SPLIT valor_campo AT '.' INTO valor1 valor2 valor3.
            IF valor3 IS INITIAL.

              remover_ceros( CHANGING c_valor_campo = valor_campo ).

            ENDIF.
            CLEAR: valor1, valor2, valor3.
          ENDIF.

          IF i_omitir_vacio_detalle = abap_true AND <fs_valor> IS ASSIGNED AND <fs_valor> IS INITIAL.
            CONTINUE.
          ENDIF.

          IF <fs_valor> IS ASSIGNED.

            es_linea = |SET { <fs_es_ztcxr1003_3>-campo_id }{ indice } = "{ COND char100( WHEN <fs_valor> IS INITIAL THEN space ELSE |{ valor_campo }| ) }"|.
            APPEND es_linea TO c_ti_archivo.
            UNASSIGN: <fs_valor>.

          ENDIF.

        ENDLOOP.
      ENDLOOP.
    ELSE.
      CLEAR: e_ti_detalle.
    ENDIF.

    IF i_es_ztcxr1003_5-impresoraid IS NOT INITIAL.
      CONCATENATE '-' i_es_ztcxr1003_5-impresoraid INTO DATA(id_impresion).
    ENDIF.

    c_nombre_archivo = |{ c_nombre_archivo }-{ i_consecutivo }{ id_impresion }.JOB|.
*    CONCATENATE c_nombre_archivo '-' i_consecutivo id_impresion '.JOB' INTO DATA(l_s_filename3).

    "Poner el numero de copias
    CONCATENATE 'PRINT' i_numero_copias INTO es_linea SEPARATED BY space.
    APPEND es_linea  TO c_ti_archivo.

    "Cerrar el archivo
    es_linea  = 'QUIT'.
    APPEND es_linea  TO c_ti_archivo.

  ENDMETHOD.

  METHOD remover_ceros.

    DATA: numero          TYPE p,
          caracteres      TYPE fist-searchw,
          caracteres_init TYPE fist-searchw.

    TRY.
        DATA(valor_alternativo) = c_valor_campo.
        CONDENSE valor_alternativo NO-GAPS.
        caracteres = valor_alternativo.
        REPLACE '.' IN caracteres WITH ''.

        CALL FUNCTION 'SF_SPECIALCHAR_DELETE'
          EXPORTING
            with_specialchar    = caracteres
          IMPORTING
            without_specialchar = caracteres_init
          EXCEPTIONS
            result_word_empty   = 1
            OTHERS              = 2.

        IF sy-subrc <> 0.
*                 MESSAGE ID SY-MSGID TYPE SY-MSGTY NUMBER SY-MSGNO
*                   WITH SY-MSGV1 SY-MSGV2 SY-MSGV3 SY-MSGV4.
        ENDIF.

        CHECK caracteres EQ caracteres_init.

        "Validar si es un numero
        CALL FUNCTION 'CATS_ITS_MAKE_STRING_NUMERICAL'
          EXPORTING
            input_string  = condense( c_valor_campo )
          IMPORTING
            value         = numero
          EXCEPTIONS
            not_numerical = 1
            OTHERS        = 2.
        IF sy-subrc = 0.
          c_valor_campo = valor_alternativo.
          DO.
            DATA(longitud) = strlen( c_valor_campo ) - 1.
            "Validar si el ultimo digito es cero para remover los decimales a la izq (Solo los cero)
            IF c_valor_campo+longitud(1) EQ '0'.
              c_valor_campo = c_valor_campo(longitud).
            ELSEIF c_valor_campo+longitud(1) EQ '.'.
              c_valor_campo = c_valor_campo(longitud).
              EXIT.
            ELSE.
              EXIT.
            ENDIF.
          ENDDO.
        ENDIF.
      CATCH cx_root.
        c_valor_campo = c_valor_campo.
    ENDTRY.

  ENDMETHOD.

  METHOD guardar_archivos.

    DATA: es_interface TYPE ztcxr1002_2,
          ti_bapiret2  TYPE bapiret2_t,
          ti_archivo2  TYPE re_t_string.

    IF i_es_ztcxr1003_5-campolocal IS INITIAL OR i_ftp EQ abap_true OR ( sy-tcode = '/SCWM/RFUI' OR sy-batch = abap_true ).

*----------------------------------------------------------------------*
* Inicio Impresión enviando a PO
* 04.10.2023 - SAGIRALD
*----------------------------------------------------------------------*
**          DATA(nombre_archivo) = i_nombre_archivo.
**          CONDENSE nombre_archivo NO-GAPS.
**          MOVE i_ti_archivo TO ti_archivo2.
**          DATA(l_aplicacion)  = g_cte_ftp. "ETIQUETAS2 (Nuevo Servidor FTP)
**
**          IF i_es_ztcxr1003_1-ftp IS INITIAL.
**            l_aplicacion = g_cte_aplica.
**          ENDIF.
**
**          zclcxr1002_dir_interfaces=>directorio_de_interfaces( EXPORTING i_accion       = 'W'
**                                                                         i_archivo      = nombre_archivo
**                                                                         i_tipo_archivo = 'O'
**                                                                         i_modulo       = g_cte_modulo
**                                                                         i_aplicacion   = l_aplicacion
**                                                                         i_id_interface = g_cte_id
**                                                               IMPORTING e_es_interface = es_interface
**                                                                         e_ti_bapiret2  = ti_bapiret2
**                                                               CHANGING  c_ti_archivo   = ti_archivo2 ).
**
**          IF ti_bapiret2 IS INITIAL.
**
***        l_es_ztcxr1003_8-id_etiqueta = i_es_ztcxr1003_1-etiquetaid.
***        l_es_ztcxr1003_8-servidor = es_interface-host_destino.
**
**            DATA(command) = VALUE char255( ).
**            command       = |sh /usr/sap/interfaces/etiquetas/shell/ftp.sh { es_interface-host_destino } { to_lower( es_interface-usuario ) }| &
**                            | { to_lower( es_interface-password ) } { to_lower( es_interface-dir_destino ) } { nombre_archivo }|.
**
**            IF g_imprimir_en_servidor EQ abap_true.
**              CALL 'SYSTEM' ID 'COMMAND' FIELD command.
**            ENDIF.
**
**          ENDIF.
**----------------------------------------------------------------------*
** Fin Impresión en servidor
** 27.07.2021 - SAGIRALD
**----------------------------------------------------------------------*
      TRY.
          DATA(es_proxy) = VALUE zpxetiqueta_enviar( etiqueta_enviar = VALUE #( nombre_archivo = condense( val = i_nombre_archivo )
                                                                                row            = VALUE #( FOR wa IN i_ti_archivo ( texto = wa ) ) ) ).
          "Instanciar al monitor
          DATA(o_monitor) = NEW zclcxr1001_monitor_integracion( i_ricefw          = 'I1197'
                                                                i_es_info_mensaje = VALUE ztcxr1001_1(
                                                                                                       pid       = 'SENDER' "Tipo de proxy
                                                                                                       fecha_ini = sy-datum "Fecha de inicio
                                                                                                       hora_ini  = sy-uzeit "Hora inicio
                                                                                                       hora_fin  = sy-uzeit "Fecha fin
                                                                                                       ob_system = 'SAP'    "Sistema Origen
                                                                                                       ob_name   = 'ZPXCO_ETIQUETA_ENVIAR_OUT' "Nombre de la clase del proxy
                                                                                                       ib_system = 'FTP'  "Sistema Destino
                                                                                                       ib_name   = 'Integracion de Etiquetas al Servidor de impresion' "Descripción breve
                                                                                                       adminuser = sy-uname
                                                                                                       aenam     = sy-uname
                                                                                                       ernam     = sy-uname
                                                                                                     )
                                                                i_datos_proxy = es_proxy "Datos del proxy
                                                                i_no_commit   = abap_true ).

          o_monitor->main( i_datos_proxy = REF #( es_proxy ) ). "Enviar referencia de los datos del proxy
        CATCH zcxr1001_excepciones_monitor.
      ENDTRY.

*----------------------------------------------------------------------*
* Fin Impresión enviando a PO
* 04.10.2023 - SAGIRALD
*----------------------------------------------------------------------*

    ELSE.

*        IF line_exists( g_es_parametros-dirlocal[ 1 ] ).
      DATA(l_c_dirlocal) = g_es_parametros-dirlocal.
*        ENDIF.

      CONCATENATE l_c_dirlocal i_nombre_archivo INTO i_nombre_archivo.

      CALL FUNCTION 'GUI_DOWNLOAD'
        EXPORTING
          filename                = i_nombre_archivo
        TABLES
          data_tab                = i_ti_archivo
        EXCEPTIONS
          file_write_error        = 1
          no_batch                = 2
          gui_refuse_filetransfer = 3
          invalid_type            = 4
          no_authority            = 5
          unknown_error           = 6
          header_not_allowed      = 7
          separator_not_allowed   = 8
          filesize_not_allowed    = 9
          header_too_long         = 10
          dp_error_create         = 11
          dp_error_send           = 12
          dp_error_write          = 13
          unknown_dp_error        = 14
          access_denied           = 15
          dp_out_of_memory        = 16
          disk_full               = 17
          dp_timeout              = 18
          file_not_found          = 19
          dataprovider_exception  = 20
          control_flush_error     = 21
          OTHERS                  = 22.
      IF sy-subrc <> 0.
        ".Error escribiendo archivo de etiqueta &1 en servidor Label Gallery
        MESSAGE s023(zcx01) DISPLAY LIKE 'E'.
      ENDIF.

    ENDIF.

  ENDMETHOD.

  METHOD envio_desde_monitor.

    CONSTANTS: gc_mensaje_id_generico TYPE char5 VALUE 'ZCX01'.
    DATA: o_async_messaging TYPE REF TO if_wsprotocol_async_messaging.

    "Obtener los datos de la referencia
    ASSIGN i_datos_proxy->* TO FIELD-SYMBOL(<fs_datos_proxy>).

    TRY.
        DATA(o_enviar_proxy) = NEW zpxco_etiqueta_enviar_out(  ). "Intancia Clase de integración

        "Asignarle una cola especifica dependiendo del sistema destino para que no queden represados en la SMQ2
        o_async_messaging ?= o_enviar_proxy->get_protocol( if_wsprotocol=>async_messaging ).
        o_async_messaging->set_serialization_context( 'R1003_EOIO_1' ).

        "Enviar a PI/PO
        o_enviar_proxy->etiqueta_enviar_async( output = <fs_datos_proxy> ).

        "Mensaje exitodo del envio
        MESSAGE s037(zcx01) INTO DATA(mtext).
        APPEND VALUE #( type    = 'S'
                        id      = gc_mensaje_id_generico
                        number  = '037'
                        message = mtext ) TO e_ti_return.
      CATCH cx_ai_system_fault INTO DATA(error).
        "Error, no se pudo comunicar
        APPEND VALUE #( type    = 'E'
                        id      = gc_mensaje_id_generico
                        number  = '000'
                        message = error->get_text( ) ) TO e_ti_return.
    ENDTRY.

  ENDMETHOD.

ENDCLASS.
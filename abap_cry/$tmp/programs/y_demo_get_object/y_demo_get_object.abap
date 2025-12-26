*&---------------------------------------------------------------------*
*& Report y_demo_get_object
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT y_demo_get_object MESSAGE-ID eu.

TABLES:
  seoclasstx,
  tadir,
  tlibt,
  d020s,
  trdir.

CLASS:
  lcl_source_scan DEFINITION DEFERRED.

DATA:
  lo_sscan   TYPE REF TO lcl_source_scan,
  lv_sstring TYPE text255,
  lv_appl    TYPE taplt-appl.

SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE bk1.
  PARAMETERS  p_debug TYPE xfeld AS CHECKBOX.
SELECTION-SCREEN: END OF BLOCK bk1.
SELECTION-SCREEN BEGIN OF BLOCK a11 WITH FRAME TITLE a11.
  SELECT-OPTIONS    devclass FOR tadir-devclass.
SELECTION-SCREEN: END OF BLOCK a11.
SELECTION-SCREEN  BEGIN OF BLOCK: a05 WITH FRAME TITLE a05.
SELECT-OPTIONS    sstring     FOR lv_sstring NO INTERVALS MODIF ID dsp.
PARAMETERS: p_regex TYPE xfeld AS CHECKBOX MODIF ID dsp.
SELECTION-SCREEN: END OF BLOCK a05,
BEGIN OF BLOCK a10 WITH FRAME TITLE a10.
SELECT-OPTIONS:   repname  FOR trdir-name MEMORY ID rs_scan_repid,
                  dynnr    FOR d020s-dnum,
                  subc     FOR trdir-subc,
                  appl     FOR lv_appl,
                  cnam     FOR trdir-cnam MATCHCODE OBJECT user_addr,
                  unam     FOR trdir-unam MATCHCODE OBJECT user_addr.
SELECTION-SCREEN: END OF BLOCK a10,
BEGIN OF BLOCK a12 WITH FRAME TITLE a12.
SELECT-OPTIONS:   funcgrp  FOR tlibt-area.
SELECTION-SCREEN: END OF BLOCK a12,
BEGIN OF BLOCK a13 WITH FRAME TITLE a13.
SELECT-OPTIONS:   p_class  FOR seoclasstx-clsname.
SELECTION-SCREEN: END OF BLOCK a13,
BEGIN OF BLOCK a20 WITH FRAME TITLE a20.
PARAMETERS: plusminu(2) TYPE n DEFAULT 2,
            inclu       TYPE xfeld AS CHECKBOX DEFAULT 'X',
            modiass     TYPE xfeld AS CHECKBOX USER-COMMAND dummy,
            comment     TYPE xfeld AS CHECKBOX DEFAULT 'X'.
SELECTION-SCREEN: END OF BLOCK a20,
BEGIN OF BLOCK a30 WITH FRAME TITLE a30.
PARAMETERS: rb_code RADIOBUTTON GROUP r10,
            rb_dyn  RADIOBUTTON GROUP r10,
            rb_all  RADIOBUTTON GROUP r10 DEFAULT 'X',
            p_vers  TYPE xfeld AS CHECKBOX.
SELECTION-SCREEN: END OF BLOCK a30.

CLASS lcx_scan_exceptions DEFINITION INHERITING FROM cx_static_check FINAL.
ENDCLASS.

CLASS lcl_log_aplicacion DEFINITION.

  PUBLIC SECTION.

    CONSTANTS: gc_id_generico TYPE string VALUE 'LOG_GENERICO'.

    TYPES:
      BEGIN OF gtp_es_log,
        icon             TYPE icon_d,
        fila_referencia  TYPE char255,
        fila_descripcion TYPE char255,
        columna          TYPE kcd_ex_col_n.
        INCLUDE TYPE bapiret2.
    TYPES: END OF gtp_es_log .

    TYPES: BEGIN OF gtp_es_manejador_log,
             id           TYPE string,
             id_manejador TYPE balloghndl,
             es_cab       TYPE bal_s_log,
             principal    TYPE flag,
           END OF gtp_es_manejador_log,

           BEGIN OF gtp_es_instancia_log,
             id    TYPE string,
             o_log TYPE REF TO lcl_log_aplicacion,
           END OF gtp_es_instancia_log.

    TYPES:
      gtp_ti_log           TYPE STANDARD TABLE OF gtp_es_log WITH NON-UNIQUE KEY fila_referencia,
      gtp_ti_manejador_log TYPE STANDARD TABLE OF gtp_es_manejador_log WITH KEY id_manejador,
      gtp_ti_instancia_log TYPE STANDARD TABLE OF gtp_es_instancia_log WITH KEY id.

    CLASS-DATA: g_id_manejador TYPE balloghndl.

    CLASS-METHODS:

      "! <strong>Descripción:</strong>
      "! <p>
      "! Genear una intancia del Log basado en la estructura BAPIRET2.<br/>
      "! Si se envia el parametro <strong>i_es_cabecera_log</strong> con los valores
      "! object y subobject se crea una instancia para almacenar el log en la base de datos
      "! y visualizar en la transaccion SLG1
      "! </p>
      "! <strong>Identificador:</strong>
                                                            "! R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      "! @parameter i_es_cabecera_log | datos del object/subobject
      "! @parameter r_o_log | Instancia del log
      "! @parameter i_identificador | Identificador del Log
      get_instancia
        IMPORTING
          VALUE(i_es_cabecera_log)   TYPE bal_s_log OPTIONAL
          VALUE(i_identificador)     TYPE string OPTIONAL
          i_descartar_msj_duplicados TYPE flag DEFAULT abap_false
        RETURNING
          VALUE(r_o_log)             TYPE REF TO lcl_log_aplicacion,

      "! <strong>Descripción:</strong>
      "! <p>
      "! Crear instancia objeto de Log, transaccion SLG0
      "! </p>
      "! <strong>Identificador:</strong>
                                                            "! R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      "! @parameter i_id_externo      | Id de log generado por de forma manual
      "! @parameter i_es_cabecera_log | Datos de configuracion del objeto y subobjeto
      "! @parameter i_principal | Instancia de log por defecto
      crear_instancia_log_bd
        IMPORTING
          VALUE(i_id_externo)      TYPE string
          VALUE(i_es_cabecera_log) TYPE bal_s_log
          i_principal              TYPE flag DEFAULT space,
      "! Liberar objetos estatios del Log
      "!
      liberar.

    METHODS:

      "! <strong>Descripción:</strong>
      "! <p>
      "! Agregar un mensaje individual al log.
      "! <p>Si se suministra solo el ID y NUMBER, se consulta el mensaje en base de datos(Tabla T100).</p>
      "! <p>Si se suministra el parametro <strong>i_manejador_log</strong>, el mensaje se agrega al objeto manejador de log indicado </p>
      "! </p>
      "! <strong>Identificador:</strong>
                                                            "! R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      "!
      "! @parameter i_es | Datos del mensaje
      "! @parameter i_manejador_log | ID Objeto Log al que se requiere agregar el mensaje
      set_es_log
        IMPORTING
          VALUE(i_es)                TYPE gtp_es_log
          VALUE(i_manejador_log)     TYPE balloghndl DEFAULT g_id_manejador
          i_descartar_msj_duplicados TYPE flag DEFAULT abap_false,
      set_ti_log
        IMPORTING
          VALUE(i_ti)                TYPE gtp_ti_log
          VALUE(i_manejador_log)     TYPE balloghndl OPTIONAL
          i_descartar_msj_duplicados TYPE flag DEFAULT abap_false,
      set_id_log_actual
        IMPORTING
          VALUE(i_id_log) TYPE string,
      set_o_cx_log
        IMPORTING
          i_o_cx_log TYPE REF TO cx_root,
      "! <strong>Descripción:</strong>
      "! <p>
      "! Retorna tabla interna con datos del log<br/>
      "! </p>
      "! <strong>Identificador:</strong> R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      "!
      "! @parameter r_ti | Datos de log
      get_log
        RETURNING
          VALUE(r_ti) TYPE gtp_ti_log,
      get_errores
        RETURNING
          VALUE(r_ti) TYPE gtp_ti_log,
      get_advertencias
        RETURNING
          VALUE(r_ti) TYPE gtp_ti_log,
      get_manejador_log_db
        RETURNING
          VALUE(r_ti) TYPE gtp_ti_manejador_log,

      "! Recupera el ultimo mensaje agregado al log
      "!
      "! @parameter r_es | estuctura con mensaje
      get_ultimo_msg
        RETURNING
          VALUE(r_es) TYPE gtp_es_log,
      "! <strong>Descripción:</strong>
      "! <p>
      "! Agregar un mensaje individual al log.
      "! Visualizar log en ALV
      "! </p>
      "! <strong>Identificador:</strong>
                                                            "! R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      "!
      "! @parameter i_alv |
      "! @parameter i_ventana_emergente |
      "! @parameter i_titulo |
      "! @parameter i_manejador_log | ID Objeto Log que se requiere visualizar
      "! @parameter i_limpiar_mensajes | Limpiar mensajes del log
      mostrar_log
        IMPORTING
          i_alv                  TYPE flag DEFAULT abap_true
          i_ventana_emergente    TYPE flag OPTIONAL
          VALUE(i_titulo)        TYPE string OPTIONAL
          VALUE(i_manejador_log) TYPE balloghndl OPTIONAL
          i_limpiar_mensajes     TYPE flag OPTIONAL
          i_asignar_icono        TYPE flag OPTIONAL,
      "! <strong>Descripción:</strong>
      "! <p>
      "! Agregar un mensaje individual al log.
      "!Guardar log en base de datos
      "! </p>
      "! <strong>Identificador:</strong>
                                                            "! R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      guardar
        IMPORTING
          VALUE(i_id_log)        TYPE string OPTIONAL
        RETURNING
          VALUE(r_ti_numero_log) TYPE bal_t_lgnm,
      remover_mensage
        IMPORTING
          i_tipo             TYPE gtp_es_log-type OPTIONAL
          i_fila_descripcion TYPE gtp_es_log-fila_descripcion OPTIONAL
          i_fila_referencia  TYPE gtp_es_log-fila_referencia OPTIONAL,
      evaluar_valor_retorno
        IMPORTING
          i_fila_referencia TYPE string OPTIONAL
          i_sysubrc         TYPE sysubrc
        RETURNING
          VALUE(r_error)    TYPE flag.



  PROTECTED SECTION.

    DATA: gti_log         TYPE gtp_ti_log.

    CLASS-DATA: gti_manejador_log          TYPE gtp_ti_manejador_log,
                g_id_log_actual            TYPE string,
                g_descartar_msj_duplicados TYPE flag.

  PRIVATE SECTION.

    CLASS-DATA: gti_instancias_log TYPE gtp_ti_instancia_log,
                go_log             TYPE REF TO lcl_log_aplicacion.
    METHODS unico_manejador_de_log
      RETURNING
        VALUE(r_unico_manejador) TYPE flag.

ENDCLASS.



CLASS lcl_log_aplicacion IMPLEMENTATION.

  METHOD get_advertencias.

    DATA: es_datgo_log LIKE LINE OF gti_log.

    LOOP AT gti_log INTO es_datgo_log  WHERE  type = 'W'.
      APPEND es_datgo_log TO r_ti.
    ENDLOOP.

  ENDMETHOD.                    "get_advertenica

  METHOD get_errores.

    DATA: es_datgo_log LIKE LINE OF gti_log.

    LOOP AT gti_log INTO es_datgo_log  WHERE  type = 'E'.
      APPEND es_datgo_log TO r_ti.
    ENDLOOP.

  ENDMETHOD.                    "get_errores

  METHOD get_instancia.

    i_identificador = COND #( WHEN i_identificador IS INITIAL THEN
                                gc_id_generico
                              ELSE
                                i_identificador ).

    g_descartar_msj_duplicados = i_descartar_msj_duplicados.
    g_id_log_actual = i_identificador.

    IF line_exists( gti_instancias_log[ id = i_identificador ] ).

      DATA(es_log) = gti_instancias_log[ id = i_identificador ].
      go_log = es_log-o_log.
      g_id_manejador = VALUE #( gti_manejador_log[ id = es_log-id ]-id_manejador OPTIONAL ).

    ELSE.

      go_log = NEW lcl_log_aplicacion(  ).

      "solo crear un log persistente cuando se envien estos datos
      IF i_es_cabecera_log IS NOT INITIAL.
        crear_instancia_log_bd( i_id_externo = i_identificador i_es_cabecera_log = i_es_cabecera_log i_principal = abap_true ).
      ENDIF.

      INSERT VALUE #( id = i_identificador o_log = go_log ) INTO TABLE gti_instancias_log.

    ENDIF.

    r_o_log = go_log.

  ENDMETHOD.                    "get_intancia

  METHOD get_log.
    r_ti = gti_log.
  ENDMETHOD.                    "get_log

  METHOD liberar.
    CLEAR: gti_manejador_log, g_id_manejador, gti_instancias_log, g_descartar_msj_duplicados.

    CHECK go_log IS BOUND.
    FREE: go_log.

  ENDMETHOD.                    "liberar

  METHOD remover_mensage.
    IF i_tipo IS NOT INITIAL.
      DELETE gti_log WHERE type = i_tipo.
    ELSEIF i_fila_referencia IS NOT INITIAL.
      DELETE gti_log WHERE fila_referencia = i_fila_referencia.
    ELSEIF i_fila_descripcion IS NOT INITIAL AND i_fila_referencia IS NOT INITIAL.
      DELETE gti_log WHERE fila_descripcion = i_fila_descripcion AND fila_referencia = i_fila_referencia.
    ELSE.
      CLEAR gti_log.
    ENDIF.

  ENDMETHOD.                    "remover_mensage

  METHOD set_es_log.
    "recupera texto de mensaje, en caso de aplicar
    IF i_es-message IS INITIAL AND ( i_es-number IS NOT INITIAL AND i_es-id IS NOT INITIAL ).
      MESSAGE  ID i_es-id TYPE i_es-type
      NUMBER i_es-number
      WITH i_es-message_v1 i_es-message_v2 i_es-message_v3 i_es-message_v4
      INTO i_es-message.
    ENDIF.
    "Verificar si el mensaje ya existe
    IF ( i_descartar_msj_duplicados = abap_true OR g_descartar_msj_duplicados = abap_true )
    AND line_exists( gti_log[ id = i_es-id number = i_es-number type = i_es-type
                              message_v1 = i_es-message_v1 message_v2 = i_es-message_v2
                              parameter = i_es-parameter ] ).
      RETURN.
    ENDIF.

    IF line_exists( gti_manejador_log[ id_manejador = i_manejador_log ] )
       OR ( unico_manejador_de_log( ) = abap_true AND gti_manejador_log IS NOT INITIAL ).

      CALL FUNCTION 'BAL_LOG_MSG_ADD'
        EXPORTING
          i_log_handle     = COND #( WHEN i_manejador_log IS NOT INITIAL THEN
                                            i_manejador_log
                                          ELSE
                                            gti_manejador_log[ 1 ]-id_manejador )
          i_s_msg          = CORRESPONDING bal_s_msg( i_es MAPPING msgid = id msgno = number msgty = type
                                                               msgv1 = message_v1 msgv2 = message_v2
                                                               msgv3 = message_v3 msgv4 = message_v4 )
        EXCEPTIONS
          log_not_found    = 1
          msg_inconsistent = 2
          log_is_full      = 3
          OTHERS           = 4.
    ENDIF.

    CONDENSE i_es-fila_referencia NO-GAPS.

    APPEND i_es TO gti_log.

  ENDMETHOD.                    "set_es_log

  METHOD set_ti_log.

    LOOP AT i_ti ASSIGNING FIELD-SYMBOL(<fs_es_log>).
      set_es_log( i_es = <fs_es_log> i_manejador_log = i_manejador_log i_descartar_msj_duplicados = i_descartar_msj_duplicados ).
    ENDLOOP.

  ENDMETHOD.                    "set_ti_log

  METHOD guardar.

    DATA: ti_manejador_log TYPE bal_t_logh.

    IF i_id_log IS NOT INITIAL.
      ti_manejador_log = VALUE #( FOR es_log_manejador IN gti_manejador_log WHERE ( id = i_id_log )
                                  ( es_log_manejador-id_manejador )
                                ).
    ELSE.
      ti_manejador_log = VALUE #( FOR es_log_manejador IN gti_manejador_log
                                ( es_log_manejador-id_manejador )
                              ).
    ENDIF.

    CHECK ti_manejador_log IS NOT INITIAL.

    CALL FUNCTION 'BAL_DB_SAVE'
      EXPORTING
*       i_save_all       = abap_true
        i_t_log_handle   = ti_manejador_log
      IMPORTING
        e_new_lognumbers = r_ti_numero_log
      EXCEPTIONS
        log_not_found    = 1
        save_not_allowed = 2
        numbering_error  = 3
        OTHERS           = 4.

  ENDMETHOD.

  METHOD crear_instancia_log_bd.

    DATA: cronomarcador_utc TYPE tzntstmps,
          huso_horario      TYPE timezone VALUE 'UTC'.

    "agregar id externo
    IF i_es_cabecera_log-extnumber IS INITIAL.

      CONVERT DATE sy-datum TIME sy-uzeit
      INTO TIME STAMP cronomarcador_utc TIME ZONE huso_horario.

      IF i_id_externo IS INITIAL.
        i_es_cabecera_log-extnumber = shift_left( val = CONV string( cronomarcador_utc ) sub = space ).
      ELSE.
        i_es_cabecera_log-extnumber = i_id_externo.
      ENDIF.
    ENDIF.

    IF i_es_cabecera_log-aldate_del IS INITIAL.
      i_es_cabecera_log-aldate_del = sy-datum + 5.
    ENDIF.

    CALL FUNCTION 'BAL_LOG_CREATE'
      EXPORTING
        i_s_log                 = i_es_cabecera_log
      IMPORTING
        e_log_handle            = g_id_manejador
      EXCEPTIONS
        log_header_inconsistent = 1
        OTHERS                  = 2.

    IF sy-subrc IS INITIAL.
      APPEND VALUE #( id = i_id_externo id_manejador = g_id_manejador es_cab = i_es_cabecera_log principal = i_principal )
      TO gti_manejador_log.
    ENDIF.
  ENDMETHOD.

  METHOD get_manejador_log_db.

    r_ti = gti_manejador_log.

  ENDMETHOD.

  METHOD mostrar_log.

    DATA: ti_bapiret2 TYPE bapiret2_t,
          r_ti_log    TYPE REF TO data.

    ti_bapiret2 = CORRESPONDING #( gti_log ).

    CHECK ti_bapiret2 IS NOT INITIAL.

    IF i_ventana_emergente = abap_true.

      IF lines( ti_bapiret2 ) = 1.
        APPEND VALUE #( id = '00' type = ti_bapiret2[ 1 ]-type number = '208' ) TO ti_bapiret2.
      ENDIF.

      CALL FUNCTION 'C14ALD_BAPIRET2_SHOW'
        TABLES
          i_bapiret2_tab = ti_bapiret2.

    ELSE.

      IF i_asignar_icono = abap_true.
        LOOP AT gti_log ASSIGNING FIELD-SYMBOL(<fs_es_log>).
          <fs_es_log>-icon = SWITCH #( <fs_es_log>-type
                                          WHEN 'E' THEN
                                              icon_red_light
                                          WHEN 'S' OR 'I' THEN
                                              icon_green_light
                                          WHEN 'W' THEN
                                              icon_yellow_light  ).
        ENDLOOP.

        r_ti_log = REF #( gti_log ).
      ELSE.
        r_ti_log = REF #( ti_bapiret2 ).
      ENDIF.

      CALL FUNCTION 'ZCXR1002_MOSTRARALV_01'
        EXPORTING
          i_ti_datos = r_ti_log
          i_titulo   = i_titulo.

    ENDIF.

    IF i_limpiar_mensajes = abap_true.
      CLEAR: gti_log.
    ENDIF.

  ENDMETHOD.

  METHOD set_o_cx_log.

    DATA: o_cx_msg   TYPE REF TO cx_t100_msg,
          es_log     LIKE LINE OF gti_log,
          ti_mensaje TYPE string_t,
          mensaje    TYPE string.

    TRY.
        o_cx_msg ?= i_o_cx_log.

        IF o_cx_msg IS BOUND.
          MESSAGE  ID o_cx_msg->t100_msgid TYPE 'S'
          NUMBER o_cx_msg->t100_msgno
          WITH o_cx_msg->t100_msgv1 o_cx_msg->t100_msgv2 o_cx_msg->t100_msgv3 o_cx_msg->t100_msgv4
          INTO mensaje.

          APPEND VALUE #( id = o_cx_msg->t100_msgid number = o_cx_msg->t100_msgno type = 'E' message = mensaje
                          message_v1 = o_cx_msg->t100_msgv1 message_v2 = o_cx_msg->t100_msgv2 message_v3 = o_cx_msg->t100_msgv3
                          message_v4 = o_cx_msg->t100_msgv4 )
          TO gti_log.

        ENDIF.
      CATCH cx_root.

        mensaje = COND #( WHEN i_o_cx_log->get_longtext( ) IS NOT INITIAL THEN
                          i_o_cx_log->get_longtext( )
                        ELSE
                          i_o_cx_log->get_text( ) ).

        CALL FUNCTION 'SOTR_SERV_STRING_TO_TABLE'
          EXPORTING
            text        = mensaje
            line_length = 50
          TABLES
            text_tab    = ti_mensaje.
        TRY.
            es_log-id = 'ZCX01'.
            es_log-number = '000'.
            es_log-type = 'E'.
            es_log-message = mensaje.
            es_log-message_v1 = ti_mensaje[ 1 ].
            es_log-message_v2 = ti_mensaje[ 2 ].
            es_log-message_v3 = ti_mensaje[ 3 ].
            es_log-message_v4 = ti_mensaje[ 4 ].
          CATCH cx_sy_itab_line_not_found.
        ENDTRY.

        APPEND es_log TO gti_log.
    ENDTRY.



  ENDMETHOD.

  METHOD evaluar_valor_retorno.
    IF i_sysubrc IS NOT INITIAL.
      set_es_log( i_es = VALUE #( fila_referencia = i_fila_referencia
                                  type = sy-msgty id = sy-msgid number = sy-msgno
                                  message_v1 = sy-msgv1 message_v2 = sy-msgv2
                                  message_v3 = sy-msgv3 message_v4 = sy-msgv4 ) ).
      r_error = abap_true.
    ENDIF.
  ENDMETHOD.


  METHOD unico_manejador_de_log.

    DATA(ti_manejador_log) = gti_manejador_log.

    SORT ti_manejador_log BY id_manejador.

    DELETE ti_manejador_log WHERE id_manejador = space.
    DELETE ADJACENT DUPLICATES FROM ti_manejador_log COMPARING id_manejador.

    r_unico_manejador = COND #( WHEN lines( ti_manejador_log ) = 1 THEN
                                 abap_true
                                ELSE
                                 abap_false
                              ).

  ENDMETHOD.

  METHOD get_ultimo_msg.

    r_es = gti_log[ lines( gti_log ) ].

  ENDMETHOD.

  METHOD set_id_log_actual.
    g_id_log_actual = i_id_log.
  ENDMETHOD.

ENDCLASS.

CLASS lcl_source_scan DEFINITION FINAL.

  PUBLIC SECTION.

    DATA: go_log TYPE REF TO lcl_log_aplicacion.

    METHODS:
      constructor,

      f4_class
        CHANGING
          cv_class_name TYPE clike,

      f4_function_group
        IMPORTING
          iv_group_name TYPE clike,

      f4_repname
        CHANGING
          cv_repname TYPE clike,

      pbo,

      start,
      get_class_source_code
        IMPORTING
          VALUE(i_class_name) TYPE tadir-obj_name
        CHANGING
          c_t_source          TYPE abaptxt255_tab,
      get_cds_view_source
        IMPORTING
          i_class_name TYPE tadir-obj_name
        CHANGING
          c_t_source   TYPE abaptxt255_tab,
      "! <strong>Descripción:</strong>
      "! <p>
      "! Obtiene las referencias de un objeto ABAP específico en el sistema.<br/>
      "! Busca todas las referencias donde se utiliza el objeto indicado.
      "! </p>
      "! <strong>Identificador:</strong>
                                                            "! R1002
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 21.01.2025  \ S4DK900021 \ Sebastian Londono \ Creacion Inicial \
      "! @parameter i_object_name | Nombre del objeto a buscar referencias
      "! @parameter i_object_type | Tipo del objeto (PROG, CLAS, INTF, etc.)
      "! @parameter r_ti_references | Tabla con las referencias encontradas
      get_object_references
        IMPORTING
          VALUE(i_object_name)   TYPE tadir-obj_name
          VALUE(i_object_type)   TYPE tadir-object OPTIONAL
        RETURNING
          VALUE(r_ti_references) TYPE string.

  PROTECTED SECTION.
    TYPES:
      BEGIN OF ty_dynpro,
        devclass        TYPE devclass,
        single_obj_name TYPE tadir-obj_name,
        repname         LIKE d020s-prog,
        dynnr           LIKE d020s-dnum,
      END OF ty_dynpro.

    TYPES:
      BEGIN OF ty_ls_objname,
        report TYPE sy-repid,
        dynnr  TYPE sy-dynnr,
      END OF ty_ls_objname.

    TYPES: BEGIN OF ty_source,
             package         TYPE devclass,
             obj_name        TYPE repname,
             single_obj_name TYPE tadir-obj_name,
             obj_type        TYPE tadir-object,
             source          TYPE string,
           END OF ty_source,

           ty_t_source TYPE STANDARD TABLE OF ty_source WITH EMPTY KEY.
    TYPES:
      BEGIN OF gtp_es_datos_zip,
        nombre    TYPE string,
        contenido TYPE xstring,
      END OF gtp_es_datos_zip .

    TYPES: gtp_r_string            TYPE RANGE OF string .

    TYPES:
      gtp_ti_datos_zip        TYPE STANDARD TABLE OF gtp_es_datos_zip .
    TYPES:
      gtp_ti_xstring          TYPE STANDARD TABLE OF x255 WITH EMPTY KEY .

    TYPES: BEGIN OF ty_s_object,
             devclass        TYPE devclass,
             obj_name        TYPE tadir-obj_name,
             object_type     TYPE tadir-object,
             single_obj_name TYPE tadir-obj_name,
           END OF ty_s_object,

           ty_t_object TYPE STANDARD TABLE OF ty_s_object WITH EMPTY KEY.

    DATA:
      go_alv           TYPE REF TO cl_salv_hierseq_table,
      gv_hit_count     TYPE i,
      gv_sstring       TYPE string,
      gv_dynp_found    TYPE xfeld,
      gv_vers_found    TYPE xfeld,
      gt_dynpro        TYPE STANDARD TABLE OF ty_dynpro,
      gt_object        TYPE STANDARD TABLE OF ty_s_object, "tadir-obj_name,
      gt_vrsd          TYPE HASHED TABLE OF vrsd
                      WITH UNIQUE KEY objname versno,
      gt_source        TYPE abaptxt255_tab,
      gt_source_detail TYPE ty_t_source,
      gv_report        TYPE syrepid,
      gv_dynpro        TYPE sydynnr,

      BEGIN OF gs_alv_header,
        repname TYPE tadir-obj_name,
        dynnr   TYPE sy-dynnr,
        expand  TYPE xfeld,
        versno  TYPE vrsd-versno,
      END OF gs_alv_header,

      gt_alv_header LIKE STANDARD TABLE OF gs_alv_header,

      BEGIN OF gs_alv_item,
        repname    TYPE sy-repid,
        dynnr      TYPE sy-dynnr,
        versno     TYPE vrsd-versno,
        line_no    TYPE rsrow,
        text       TYPE text255,
        hit        TYPE xfeld,
        cell_color TYPE lvc_t_scol,
      END OF gs_alv_item,

      gt_alv_item LIKE STANDARD TABLE OF gs_alv_item.

    CONSTANTS:
      gc_x TYPE xfeld VALUE 'X'.

    METHODS:
      add_to_hitlist
        IMPORTING
          iv_report      TYPE clike
          iv_dynpro      TYPE clike OPTIONAL
          iv_source_line TYPE clike
          iv_tabix       TYPE sy-tabix
          iv_hit         TYPE xfeld
          iv_versno      TYPE vrsd-versno,

      call_abap_editor
        IMPORTING
          is_alv_item LIKE gs_alv_item,

      call_screen_painter
        IMPORTING
          is_alv_item LIKE gs_alv_item,

      display,

      display_abap_version
        IMPORTING
          is_alv_item LIKE gs_alv_item,

      display_screen_painter_version
        IMPORTING
          is_alv_item LIKE gs_alv_item,

      display_version_management
        IMPORTING
          is_alv_header LIKE gs_alv_header,

      get_alv_instance,
      get_dynpro_flow_logic
        IMPORTING
                  iv_report       TYPE clike
                  iv_dynpro       TYPE clike
        RETURNING VALUE(rt_dflow) TYPE abaptxt255_tab,

      get_hit_set
        IMPORTING
          iv_report TYPE clike
          iv_dynpro TYPE clike OPTIONAL
          it_abap   TYPE abaptxt255_tab
          iv_tabix  TYPE sy-tabix
          iv_versno TYPE vrsd-versno,

      get_version_numbers
        IMPORTING
                  iv_report      TYPE clike
                  iv_dynpro      TYPE clike OPTIONAL
        RETURNING VALUE(rt_vrsd) LIKE gt_vrsd,

      get_dynpros,
      get_source_names,

      get_source_by_version
        IMPORTING
                  iv_report      TYPE clike
                  iv_dynpro      TYPE clike OPTIONAL
                  iv_versno      TYPE vrsd-versno
        RETURNING VALUE(rt_abap) TYPE abaptxt255_tab,

      get_report_names,
      get_function_names,
      get_class_names,
      get_interface_names,
      get_includes,

      search_abap_source   RAISING lcx_scan_exceptions,
      search_dynpro_source RAISING lcx_scan_exceptions,

      search_source        RAISING lcx_scan_exceptions,

      set_alv_attributes,
      "! @parameter i_ti |
      "! @parameter i_r_excluir_columnas | Nombre de columnas a excluir
      "! @parameter i_omitir_salto_linea |
      "! @parameter i_caracter_exc_columna | Caracter que identifica las columnas que no se tendran en cuenta en la conversion
      "! @parameter i_separador | Caracter mediante el cual se separaran los valores en cada columna
      "! @parameter i_incluir_encabezado | Incluye en la primera línea el texto de las columnas o descripciones
      "! @parameter i_tam_txt_columna | El tamaño de los textos de las columnas a incluir (Large, Medium, Small, Otro:Nombre Columna)
      "! @parameter r_str | Cadena de texto con contendio de tabla interna
      ti_a_string
        IMPORTING
          !i_ti                   TYPE ANY TABLE
          !i_r_excluir_columnas   TYPE gtp_r_string OPTIONAL
          !i_omitir_salto_linea   TYPE flag DEFAULT abap_false
          !i_caracter_exc_columna TYPE string OPTIONAL
          !i_separador            TYPE string DEFAULT ';'
          !i_incluir_encabezado   TYPE flag DEFAULT abap_false
          !i_tam_txt_columna      TYPE char01 OPTIONAL
        RETURNING
          VALUE(r_str)            TYPE string ,
      "! Convierte el contenido de una tabla interna
      "! en una cadena de texto (String)
      "!
      "! @parameter i_es |
      "! @parameter i_r_excluir_columnas | Nombre de columnas a excluir
      "! @parameter i_caracter_exc_columna | Caracter que identifica las columnas que no se tendran en cuenta en la conversion
      "! @parameter i_separador | Caracter mediante el cual se separaran los valores en cada columna
      "! @parameter i_incluir_encabezado | Incluye en la primera línea el texto de las columnas o descripciones
      "! @parameter i_longitud_texto_columna | El tamaño de los textos de las columnas a incluir (Large, Medium, Small, Otro:Nombre Columna)
      "! @parameter r_str | Cadena de texto con contendio de tabla interna
      "! @parameter i_ti_componente_estructura | Nombres de las columnas
      es_a_string
        IMPORTING
          !i_es                      TYPE any
          !i_r_excluir_columnas      TYPE gtp_r_string OPTIONAL
          !i_caracter_exc_columna    TYPE string OPTIONAL
          !i_separador               TYPE string DEFAULT ';'
          !i_incluir_encabezado      TYPE flag DEFAULT abap_false
          !i_longitud_texto_columna  TYPE char01 OPTIONAL
          i_ti_componente_estructura TYPE cl_abap_structdescr=>component_table OPTIONAL
        RETURNING
          VALUE(r_str)               TYPE string,
      "! Crear archivo ZIP
      "!
      "! @parameter i_ti_contenido_zip | Nombre del archivo y contenido binario
      "! @parameter e_longitud_salida | Longitud del archivo generado
      "! @parameter e_ti_binario | Archivo ZIP en formato binario
      crear_archivo_zip
        IMPORTING
          !i_ti_contenido_zip TYPE gtp_ti_datos_zip
        EXPORTING
          !e_longitud_salida  TYPE i
          !e_ti_binario       TYPE gtp_ti_xstring,
      export_data_to_zip,
      get_object_reference,

      on_link_click
        FOR EVENT link_click OF cl_salv_events_hierseq
        IMPORTING
          sender
          level
          row
          column.
  PRIVATE SECTION.
    METHODS get_package_hierachy.
    METHODS get_enhancement.
    METHODS get_cds_view.

ENDCLASS.                    "lcl_source_scan DEFINITION

*----------------------------------------------------------------------*
*       CLASS lcl_source_scan IMPLEMENTATION
*----------------------------------------------------------------------*
*       ABAP source scanner
*----------------------------------------------------------------------*
CLASS lcl_source_scan IMPLEMENTATION.
  METHOD display_screen_painter_version.
    DATA:
      lv_object_name TYPE versobjnam,
      ls_infolna     TYPE vrsinfolna,
      ls_infolnb     TYPE vrsinfolnb,
      ls_vrsd        LIKE LINE OF gt_vrsd,
      ls_object_name TYPE ty_ls_objname.

    ls_object_name-report = is_alv_item-repname.
    ls_object_name-dynnr  = is_alv_item-dynnr.
    lv_object_name        = ls_object_name.

    READ TABLE gt_vrsd WITH TABLE KEY objname = lv_object_name
                                      versno  = is_alv_item-versno
                                      INTO ls_vrsd.

    CHECK sy-subrc IS INITIAL.

    ls_infolna = lv_object_name.
    MOVE-CORRESPONDING ls_vrsd TO ls_infolnb.

    CALL FUNCTION 'RS_SCRP_SHOW_VERS'
      EXPORTING
        infolna = ls_infolna
        infolnb = ls_infolnb
        objname = lv_object_name
        versno  = is_alv_item-versno
      EXCEPTIONS
        OTHERS  = 0.

  ENDMETHOD.                    "display_screen_painter_version

  METHOD display_abap_version.
    DATA:
      lt_trdir       TYPE STANDARD TABLE OF trdir,
      lv_object_name TYPE versobjnam,
      lv_title       TYPE sy-title,
      lt_abap        TYPE abaptxt255_tab.

    lv_object_name = is_alv_item-repname.

*   Display report version
    CALL FUNCTION 'SVRS_GET_REPS_FROM_OBJECT'
      EXPORTING
        object_name                  = lv_object_name
        object_type                  = 'REPS'
        versno                       = is_alv_item-versno
        iv_no_release_transformation = gc_x
      TABLES
        repos_tab                    = lt_abap
        trdir_tab                    = lt_trdir
      EXCEPTIONS
        no_version                   = 1
        OTHERS                       = 2.

    CHECK sy-subrc IS INITIAL.

    CONCATENATE 'Programm:'(004)
                is_alv_item-repname
                'Version'(005)
                 is_alv_item-versno
                 INTO lv_title SEPARATED BY space.

    EDITOR-CALL FOR lt_abap TITLE lv_title DISPLAY-MODE.

  ENDMETHOD.                    "display_abap_version

  METHOD call_screen_painter.
    CALL FUNCTION 'RS_SCRP'
      EXPORTING
        abl_line    = is_alv_item-line_no
        dynnr       = is_alv_item-dynnr
        progname    = is_alv_item-repname
        wanted_mode = 'SHOW'
      EXCEPTIONS
        OTHERS      = 0.

  ENDMETHOD.                    "call_screen_painter

  METHOD call_abap_editor.
    CALL FUNCTION 'EDITOR_PROGRAM'
      EXPORTING
        appid   = 'PG'
        display = gc_x
        program = is_alv_item-repname
        line    = is_alv_item-line_no
        topline = is_alv_item-line_no
      EXCEPTIONS
        OTHERS  = 0.

  ENDMETHOD.                    "call_abap_editor

  METHOD display_version_management.
    IF is_alv_header-dynnr IS INITIAL.
*     call version management for programs
      CALL FUNCTION 'RS_PROGRAM_VERSIONS'
        EXPORTING
          progname = is_alv_header-repname
        EXCEPTIONS
          OTHERS   = 0.
    ELSE.
      CALL FUNCTION 'RS_SCRP_VERSION'
        EXPORTING
          dynnr     = is_alv_header-dynnr
          progname  = is_alv_header-repname
          no_update = gc_x.
    ENDIF.
  ENDMETHOD.                    "display_version_management

  METHOD constructor.
    DATA:
      ls_restrict    TYPE sscr_restrict,
      ls_opt_list    TYPE sscr_opt_list,
      ls_association TYPE sscr_ass.

    ls_opt_list-name       = 'RESTRICT'.
    ls_opt_list-options-cp = gc_x.
    ls_opt_list-options-eq = gc_x.

    APPEND ls_opt_list TO ls_restrict-opt_list_tab.

    ls_association-kind    = 'S'.
    ls_association-name    = 'SSTRING'.
    ls_association-sg_main = 'I'.
    ls_association-op_main = ls_association-op_addy = 'RESTRICT'.

    APPEND ls_association TO ls_restrict-ass_tab.

    CALL FUNCTION 'SELECT_OPTIONS_RESTRICT'
      EXPORTING
        program     = sy-repid
        restriction = ls_restrict
      EXCEPTIONS
        OTHERS      = 0.

  ENDMETHOD.                    "constructor

  METHOD get_dynpro_flow_logic.
    DATA: ls_dhead  TYPE d020s,
          lt_dfield TYPE STANDARD TABLE OF d021s,
          lt_dflow  TYPE STANDARD TABLE OF d022s,
          lt_dmatch TYPE STANDARD TABLE OF d023s,

          BEGIN OF ls_dynp_id,
            prog TYPE d020s-prog,
            dnum TYPE d020s-dnum,
          END OF ls_dynp_id.

    ls_dynp_id-prog = iv_report.
    ls_dynp_id-dnum = iv_dynpro.

    IMPORT DYNPRO ls_dhead lt_dfield lt_dflow lt_dmatch ID ls_dynp_id.

    rt_dflow = lt_dflow.
  ENDMETHOD.                    "get_dynpro_flow_logic

  METHOD on_link_click.
    DATA:
      ls_alv_header LIKE LINE OF gt_alv_header,
      ls_alv_item   LIKE LINE OF gt_alv_item.

    CASE level.
      WHEN '1'.
        READ TABLE gt_alv_header INDEX row INTO ls_alv_header.
        CHECK sy-subrc IS INITIAL.

        display_version_management( ls_alv_header ).

      WHEN '2'.
        READ TABLE gt_alv_item INDEX row INTO ls_alv_item.
        CHECK sy-subrc IS INITIAL.

        IF ls_alv_item-dynnr IS INITIAL.
          IF ls_alv_item-versno IS INITIAL.
            call_abap_editor( ls_alv_item ).
          ELSE.
            display_abap_version( ls_alv_item ).
          ENDIF.

          SET PARAMETER ID 'RID' FIELD sy-repid.
        ELSE.
*         Call screen painter
          IF ls_alv_item-versno IS INITIAL.
            call_screen_painter( ls_alv_item ).
          ELSE.
            display_screen_painter_version( ls_alv_item ).
          ENDIF.
        ENDIF.
    ENDCASE.
  ENDMETHOD.                    "on_link_click

  METHOD set_alv_attributes.
    DATA:
      lo_layout    TYPE REF TO cl_salv_layout,
      lo_events    TYPE REF TO cl_salv_events_hierseq,
      lo_functions TYPE REF TO cl_salv_functions_list,
      lo_level     TYPE REF TO cl_salv_hierseq_level,
      lo_column    TYPE REF TO cl_salv_column_hierseq,
      lo_columns   TYPE REF TO cl_salv_columns_hierseq,
      lt_columns   TYPE salv_t_column_ref,
      ls_columns   LIKE LINE OF lt_columns,
      lo_settings  TYPE REF TO cl_salv_display_settings,
      lv_title     TYPE lvc_title,
      lv_hits      TYPE lvc_title,
      ls_color     TYPE lvc_s_colo,
      ls_layout    TYPE salv_s_layout_key,
      lt_functions TYPE salv_t_ui_func.

*   Layout
    ls_layout-report = sy-repid.
    ls_layout-handle = 'SCAN'.

    lo_layout = go_alv->get_layout( ).
    lo_layout->set_key( ls_layout ).
    lo_layout->set_save_restriction( ).

*   Function keys/buttons
    lo_functions = go_alv->get_functions( ).
    lo_functions->set_all( gc_x ).

*   exclude the following functions (column paging buttons)
    lt_functions = lo_functions->get_functions( ).

*   Display settings
    lo_settings = go_alv->get_display_settings( ).

*   Title
    lv_hits = gv_hit_count.
    SHIFT lv_hits LEFT DELETING LEADING space.

    CONCATENATE lv_hits
                'Treffer'(001)
                INTO lv_hits SEPARATED BY space.

    lv_title = 'Source Scan für String:'(002).

    CONCATENATE lv_title
                gv_sstring
                INTO lv_title SEPARATED BY space.

    CONCATENATE lv_title
                lv_hits
                INTO lv_title SEPARATED BY ' - '.

    lo_settings->set_list_header( lv_title ).

*   Event handling
    lo_events = go_alv->get_event( ).
    SET HANDLER on_link_click FOR lo_events.

*   Field catalog
    TRY.
*       Field catalog/columns - header table
        lo_columns  = go_alv->get_columns( '1' ).
        lt_columns = lo_columns->get( ).

        TRY.
            lo_columns->set_expand_column( 'EXPAND' ).

            lo_level = go_alv->get_level( '1' ).
            lo_level->set_items_expanded( gc_x ).

          CATCH cx_salv_data_error.
        ENDTRY.

        LOOP AT lt_columns INTO ls_columns.
          CASE ls_columns-columnname.
            WHEN 'EXPAND'.
              ls_columns-r_column->set_technical( ).

            WHEN 'DYNNR'.
              IF gv_dynp_found IS INITIAL.
                ls_columns-r_column->set_technical( ).
              ELSE.
                ls_columns-r_column->set_output_length( '15' ).
              ENDIF.

            WHEN 'VERSNO'.
              IF gv_vers_found IS INITIAL.
                ls_columns-r_column->set_technical( ).
              ELSE.
                ls_columns-r_column->set_leading_zero( gc_x ).
                ls_columns-r_column->set_output_length( '15' ).
                TRY.
                    lo_column ?= ls_columns-r_column.
                    lo_column->set_cell_type( if_salv_c_cell_type=>hotspot ).
                  CATCH cx_sy_move_cast_error.
                ENDTRY.
              ENDIF.
          ENDCASE.
        ENDLOOP.

*       Field catalog/columns - item table
        lo_columns = go_alv->get_columns( '2' ).

        TRY.
            lo_columns->set_color_column( 'CELL_COLOR' ).
          CATCH cx_salv_data_error.
        ENDTRY.

        lt_columns = lo_columns->get( ).

        LOOP AT lt_columns INTO ls_columns.
          CASE ls_columns-columnname.
            WHEN 'REPNAME'.
              ls_columns-r_column->set_technical( ).

            WHEN 'DYNNR'.
              ls_columns-r_column->set_technical( ).

            WHEN 'VERSNO'.
              ls_columns-r_column->set_technical( ).

            WHEN 'CELL_COLOR'.
              ls_columns-r_column->set_technical( ).

            WHEN 'HIT'.
              ls_columns-r_column->set_technical( ).

            WHEN 'LINE_NO'.
              ls_color-col = '4'.
              TRY.
                  lo_column ?= ls_columns-r_column.
                  lo_column->set_color( ls_color ).
                  lo_column->set_leading_zero( gc_x ).
                CATCH cx_sy_move_cast_error.
              ENDTRY.

            WHEN 'TEXT'.
              TRY.
                  lo_column ?= ls_columns-r_column.
                  lo_column->set_cell_type( if_salv_c_cell_type=>hotspot ).
                CATCH cx_sy_move_cast_error.
              ENDTRY.

          ENDCASE.
        ENDLOOP.
      CATCH cx_salv_not_found.
    ENDTRY.

  ENDMETHOD.                    "set_alv_attributes

  METHOD get_alv_instance.
    DATA:
      lt_alv_bind TYPE salv_t_hierseq_binding,
      ls_alv_bind LIKE LINE OF lt_alv_bind.

    ls_alv_bind-master = ls_alv_bind-slave = 'REPNAME'.
    APPEND ls_alv_bind TO lt_alv_bind.

    ls_alv_bind-master = ls_alv_bind-slave = 'DYNNR'.
    APPEND ls_alv_bind TO lt_alv_bind.

    ls_alv_bind-master = ls_alv_bind-slave = 'VERSNO'.
    APPEND ls_alv_bind TO lt_alv_bind.

    TRY.
        CALL METHOD cl_salv_hierseq_table=>factory
          EXPORTING
            t_binding_level1_level2 = lt_alv_bind
          IMPORTING
            r_hierseq               = go_alv
          CHANGING
            t_table_level1          = gt_alv_header
            t_table_level2          = gt_alv_item.

      CATCH cx_salv_data_error.
      CATCH cx_salv_not_found.
    ENDTRY.

  ENDMETHOD.                    "get_alv_instance

  METHOD f4_repname.
    CALL FUNCTION 'REPOSITORY_INFO_SYSTEM_F4'
      EXPORTING
        object_type          = 'PROG'
        object_name          = cv_repname
        suppress_selection   = 'X'
      IMPORTING
        object_name_selected = cv_repname
      EXCEPTIONS
        cancel               = 0.
  ENDMETHOD.                                                "f4_repname

  METHOD f4_function_group.
    DATA:
      lv_fname TYPE dynfnam.

    lv_fname = iv_group_name.

    CALL FUNCTION 'RS_HELP_HANDLING'
      EXPORTING
        dynpfield                 = lv_fname
        dynpname                  = sy-dynnr
        object                    = 'FG  '
        progname                  = sy-repid
        suppress_selection_screen = 'X'.

  ENDMETHOD.                    "f4_function_group

  METHOD f4_class.
    CALL FUNCTION 'F4_DD_ALLTYPES'
      EXPORTING
        object               = cv_class_name
        suppress_selection   = gc_x
        display_only         = space
        only_types_for_clifs = gc_x
      IMPORTING
        result               = cv_class_name.
  ENDMETHOD.                                                "f4_class

  METHOD display.

    DATA text TYPE c LENGTH 150.

    IF gv_hit_count IS INITIAL.
      go_log->set_es_log( i_es = VALUE #( id = 'EU' number = '326' type = 'S' message_v1 = gv_sstring ) ).
      RETURN.
    ENDIF.

    IF sy-batch IS INITIAL.
      text = |DISPLAY { gv_hit_count } HITS...|.
      CALL FUNCTION 'SAPGUI_PROGRESS_INDICATOR'
        EXPORTING
          text = text.
    ENDIF.

    SORT gt_alv_item BY repname dynnr versno line_no hit DESCENDING.
    DELETE ADJACENT DUPLICATES FROM gt_alv_item COMPARING repname dynnr versno line_no.

    get_alv_instance( ).
    CHECK go_alv IS NOT INITIAL.

    set_alv_attributes( ).

    go_alv->display( ).

  ENDMETHOD.                    "display

  METHOD add_to_hitlist.
    DATA:
      ls_col LIKE LINE OF gs_alv_item-cell_color.

    gs_alv_item-repname = iv_report.
    gs_alv_item-dynnr   = iv_dynpro.
    gs_alv_item-line_no = iv_tabix.
    gs_alv_item-versno  = iv_versno.
    gs_alv_item-text    = iv_source_line.

    IF iv_hit IS NOT INITIAL.
      gs_alv_item-hit = gc_x.
      ADD 1 TO gv_hit_count.
      ls_col-fname     = 'TEXT'.
      ls_col-color-col = '5'.
      APPEND ls_col TO gs_alv_item-cell_color.
    ENDIF.

    APPEND gs_alv_item TO gt_alv_item.

    CLEAR gs_alv_item.
  ENDMETHOD.                    "add_to_hitlist

  METHOD get_hit_set.
    DATA: lv_end     TYPE i,
          lv_start   TYPE i,
          lv_xtabix  TYPE sy-tabix,
          lv_hitline TYPE xfeld.

    FIELD-SYMBOLS:
      <lv_abap> TYPE any.

    lv_start = iv_tabix - plusminu .
    lv_end   = iv_tabix + plusminu.

    IF lv_start < 1.
      lv_start = 1.
    ENDIF.

    WHILE lv_start <= lv_end.
      READ TABLE it_abap ASSIGNING <lv_abap> INDEX lv_start.
      IF sy-subrc IS NOT INITIAL.
        EXIT.
      ENDIF.

      lv_xtabix = sy-tabix.

      IF lv_start = iv_tabix.
        lv_hitline = gc_x.
      ELSE.
        CLEAR lv_hitline.
      ENDIF.

      ADD 1 TO lv_start.

      IF comment IS NOT INITIAL.
        IF modiass IS INITIAL.
          IF <lv_abap>(1) = '*'
          OR <lv_abap>(1) = '"'.
            CONTINUE.
          ENDIF.
        ELSE.
          IF <lv_abap>(1) = '*'.
            IF  <lv_abap>(2) = '*{' OR <lv_abap>(2) = '*}'.
            ELSE.
              CONTINUE.
            ENDIF.
          ENDIF.
        ENDIF.
      ENDIF.

      CALL METHOD add_to_hitlist
        EXPORTING
          iv_report      = iv_report
          iv_dynpro      = iv_dynpro
          iv_source_line = <lv_abap>
          iv_tabix       = lv_xtabix
          iv_hit         = lv_hitline
          iv_versno      = iv_versno.

    ENDWHILE.

  ENDMETHOD.                    "get_hit_set

  METHOD get_source_by_version.
    DATA:
      lv_object_name TYPE versobjnam,
      ls_object_name TYPE ty_ls_objname,
      lt_trdir       TYPE STANDARD TABLE OF trdir,
      lt_d022s       TYPE STANDARD TABLE OF d022s.

    IF iv_dynpro IS INITIAL.
      lv_object_name = iv_report.

      CALL FUNCTION 'SVRS_GET_REPS_FROM_OBJECT'
        EXPORTING
          object_name                  = lv_object_name
          object_type                  = 'REPS'
          versno                       = iv_versno
          iv_no_release_transformation = 'X'
        TABLES
          repos_tab                    = rt_abap
          trdir_tab                    = lt_trdir
        EXCEPTIONS
          OTHERS                       = 0.
    ELSE.
      ls_object_name-report = iv_report.
      ls_object_name-dynnr  = iv_dynpro.

      lv_object_name = ls_object_name.

      CALL FUNCTION 'SVRS_GET_VERSION_DYNP_40'
        EXPORTING
          object_name = lv_object_name
          versno      = iv_versno
        TABLES
          d022s_tab   = lt_d022s
        EXCEPTIONS
          OTHERS      = 1.

      CHECK sy-subrc IS INITIAL AND lt_d022s IS NOT INITIAL.

      APPEND LINES OF lt_d022s TO rt_abap.

    ENDIF.
  ENDMETHOD.                    "get_source_by_version

  METHOD get_version_numbers.
    DATA:
      ls_objname TYPE ty_ls_objname,
      lv_objtype TYPE vrsd-objtype,
      lv_objname TYPE versobjnam,
      lv_versno  TYPE versno,
      lt_vrsn    TYPE STANDARD TABLE OF vrsn,
      lt_vrsd    TYPE STANDARD TABLE OF vrsd.

    ls_objname-report = iv_report.
    ls_objname-dynnr  = iv_dynpro.
    lv_objname        = ls_objname.

    IF iv_dynpro IS INITIAL.
      lv_objtype = 'REPS'.
    ELSE.
      lv_objtype = 'DYNP'.
    ENDIF.

    CALL FUNCTION 'SVRS_GET_VERSION_DIRECTORY_46'
      EXPORTING
        objname      = lv_objname
        objtype      = lv_objtype
      TABLES
        lversno_list = lt_vrsn
        version_list = lt_vrsd
      EXCEPTIONS
        OTHERS       = 1.

    CHECK sy-subrc IS INITIAL .

    SORT lt_vrsd BY objname versno.
    DELETE ADJACENT DUPLICATES FROM lt_vrsd COMPARING objname versno.

    rt_vrsd = lt_vrsd.

    DELETE TABLE rt_vrsd WITH TABLE KEY objname = lv_objname
                                        versno  = lv_versno.

    SORT rt_vrsd.

    CHECK iv_dynpro IS NOT INITIAL.
*   For dynpros we need to save the version information for the version display
*   this is not required for source code
    INSERT LINES OF rt_vrsd INTO TABLE gt_vrsd.

  ENDMETHOD.                    "get_version_Numbers

  METHOD search_abap_source.

    DATA: t_class_source  LIKE gt_source,
          percentage      TYPE i,
          old_percentage  TYPE i VALUE -1,
          object_it_index TYPE i,
          text            TYPE c LENGTH 150.

    IF p_debug = abap_true.
      BREAK-POINT.
    ENDIF.

    SORT gt_object BY object_type single_obj_name.

    LOOP AT gt_object INTO DATA(s_object).

      object_it_index = sy-tabix.

      gv_report = s_object-obj_name.

      IF sy-batch IS INITIAL.
        percentage = sy-tabix * 100 / lines( gt_object ).
        text = |SEARCH ABAP SOURCES ({ sy-tabix }/{ lines( gt_object ) })...|.

        IF old_percentage <> percentage.
          CALL FUNCTION 'SAPGUI_PROGRESS_INDICATOR'
            EXPORTING
              percentage = percentage
              text       = text.
          old_percentage = percentage.
        ENDIF.
      ENDIF.

      IF s_object-object_type = 'CLAS'.

        CLEAR: gt_source.

        get_class_source_code( EXPORTING i_class_name = s_object-single_obj_name
                               CHANGING  c_t_source   = gt_source ).

        IF gt_source IS INITIAL.
          LOOP AT gt_object INTO DATA(s_object_class) FROM object_it_index WHERE single_obj_name = s_object-single_obj_name.

            DELETE gt_object INDEX sy-tabix.

            READ REPORT s_object_class-obj_name INTO t_class_source.

            IF sy-subrc IS INITIAL.
              APPEND LINES OF t_class_source TO gt_source.
            ENDIF.

          ENDLOOP.
        ELSE.
          DELETE gt_object WHERE single_obj_name = s_object-single_obj_name.
        ENDIF.

      ELSEIF s_object-object_type = 'DDLS'.

        CLEAR: gt_source.

        get_cds_view_source( EXPORTING i_class_name = s_object-single_obj_name
                             CHANGING  c_t_source   = gt_source ).
      ELSE.

        READ REPORT gv_report INTO gt_source.
        IF sy-subrc IS NOT INITIAL.
          CONTINUE.
        ENDIF.

      ENDIF.

      APPEND INITIAL LINE TO gt_source_detail ASSIGNING FIELD-SYMBOL(<fs_s_source_detail>).
      <fs_s_source_detail> = VALUE #( package = s_object-devclass
                                      obj_name = s_object-obj_name
                                      single_obj_name = s_object-single_obj_name
                                      obj_type = s_object-object_type
                                      source = ti_a_string( i_ti = gt_source i_separador = space ) ).
      <fs_s_source_detail>-source = replace( val = <fs_s_source_detail>-source sub = '\r' with = space ).
      <fs_s_source_detail>-source = replace( val = <fs_s_source_detail>-source sub = '\n' with = space ).

      search_source( ).

    ENDLOOP.

    FREE gt_object.

  ENDMETHOD.

  METHOD ti_a_string.

    DATA: str_line            TYPE string,
          o_stdescribe        TYPE REF TO cl_abap_structdescr,
          o_tabledescr        TYPE REF TO cl_abap_tabledescr,
          o_elm_des           TYPE REF TO cl_abap_elemdescr,
          ti_columnas_tabla_d TYPE cl_abap_structdescr=>component_table.

    FIELD-SYMBOLS: <fs_es_datos> TYPE any,
                   <fs_dato>     TYPE any,
                   <fs_ti_datos> TYPE ANY TABLE.

    TRY.

        o_tabledescr ?= cl_abap_structdescr=>describe_by_data( i_ti ).
        o_stdescribe ?= o_tabledescr->get_table_line_type( ).

        DATA(ti_columnas_tabla) = o_stdescribe->get_components( ).

        "busca estructuras anidadas
        LOOP AT ti_columnas_tabla INTO DATA(es_componente) WHERE name = space OR as_include EQ abap_true.
          o_stdescribe ?=  es_componente-type.
          APPEND LINES OF o_stdescribe->get_components( ) TO ti_columnas_tabla_d.
        ENDLOOP..

        DELETE ti_columnas_tabla_d WHERE name = space.
        "agrega componenes anidados
        APPEND LINES OF ti_columnas_tabla_d TO ti_columnas_tabla.

        DELETE ti_columnas_tabla WHERE name = space.

      CATCH cx_sy_move_cast_error ##NO_HANDLER.

    ENDTRY.

    LOOP AT i_ti ASSIGNING <fs_es_datos>.

      str_line = es_a_string( i_es                       = <fs_es_datos>
                              i_r_excluir_columnas       = i_r_excluir_columnas
                              i_caracter_exc_columna     = i_caracter_exc_columna
                              i_separador                = i_separador
                              i_incluir_encabezado       = abap_false
                              i_ti_componente_estructura = ti_columnas_tabla ).

      IF i_omitir_salto_linea = abap_true.
        r_str = r_str && str_line.
      ELSE.
        r_str = r_str && str_line && cl_abap_char_utilities=>cr_lf.
      ENDIF.

    ENDLOOP.

    """"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""
    "Funcionalidad para incluir el encabezado de la tabla
    IF i_incluir_encabezado = abap_true.
      CLEAR str_line.

      "Recorrer los campos de la tabla
      LOOP AT ti_columnas_tabla ASSIGNING FIELD-SYMBOL(<fs_es_componentes_tabla>).

        IF i_r_excluir_columnas IS NOT INITIAL AND
           <fs_es_componentes_tabla>-name IN i_r_excluir_columnas.
          CONTINUE.
        ENDIF.
        "columnas que se excluyen
        IF i_caracter_exc_columna IS NOT INITIAL AND
           <fs_es_componentes_tabla>-name(2) EQ i_caracter_exc_columna.
          CONTINUE.
        ENDIF.

        TRY.
            "Asigna la referencia del tipo del campo
            o_elm_des ?= <fs_es_componentes_tabla>-type.
          CATCH  cx_sy_move_cast_error cx_root INTO DATA(o_cx).
            CONTINUE.
        ENDTRY.

        CASE i_tam_txt_columna.
          WHEN 'L'.
            str_line = |{ str_line }{ i_separador }{ o_elm_des->get_ddic_field( )-scrtext_l }|.
          WHEN 'M'.
            str_line = |{ str_line }{ i_separador }{ o_elm_des->get_ddic_field( )-scrtext_m }|.
          WHEN 'S'.
            str_line = |{ str_line }{ i_separador }{ o_elm_des->get_ddic_field( )-scrtext_s }|.
          WHEN OTHERS.
            str_line = |{ str_line }{ i_separador }{ <fs_es_componentes_tabla>-name }|.
        ENDCASE.

      ENDLOOP.

      REPLACE FIRST OCCURRENCE OF i_separador IN str_line WITH space.

      IF i_omitir_salto_linea = abap_true.
        r_str = str_line && r_str.
      ELSE.
        r_str = str_line && cl_abap_char_utilities=>cr_lf && r_str.
      ENDIF.

    ENDIF.

  ENDMETHOD.

  METHOD es_a_string.

    DATA: str_line                 TYPE string,
          o_stdescribe             TYPE REF TO cl_abap_structdescr,
          o_tabledescr             TYPE REF TO cl_abap_tabledescr,
          o_elm_des                TYPE REF TO cl_abap_elemdescr,
          ti_columnas_tabla_d      TYPE cl_abap_structdescr=>component_table,
          indice_columna           TYPE i,
          r_tipo_no_dato_permitido TYPE RANGE OF c.

    FIELD-SYMBOLS: <fs_dato>     TYPE any,
                   <fs_ti_datos> TYPE ANY TABLE.

    r_tipo_no_dato_permitido = VALUE #( sign = 'I' option = 'EQ' ( low = 'h' )
                                                                 ( low = 'u' )
                                                                 ( low = 'v' ) ).

    IF i_ti_componente_estructura IS INITIAL.
      TRY.

          o_stdescribe ?= cl_abap_structdescr=>describe_by_data( i_es ).

          DATA(ti_componente_estructura) = o_stdescribe->get_components( ).

          "busca estructuras anidadas
          LOOP AT ti_componente_estructura INTO DATA(es_componente) WHERE name = space OR as_include EQ abap_true.
            o_stdescribe ?=  es_componente-type.
            APPEND LINES OF o_stdescribe->get_components( ) TO ti_columnas_tabla_d.
          ENDLOOP..

          DELETE ti_columnas_tabla_d WHERE name = space.
          "agrega componenes anidados
          APPEND LINES OF ti_columnas_tabla_d TO ti_componente_estructura.

          DELETE ti_componente_estructura WHERE name = space.

        CATCH cx_sy_move_cast_error ##NO_HANDLER.

      ENDTRY.

    ELSE.
      ti_componente_estructura = i_ti_componente_estructura.
    ENDIF.

    DO.

      indice_columna = sy-index.

      ASSIGN COMPONENT indice_columna OF STRUCTURE i_es TO <fs_dato>.

      IF sy-subrc IS NOT INITIAL.
        EXIT.
      ENDIF.

      IF i_r_excluir_columnas IS NOT INITIAL AND
         ti_componente_estructura[ indice_columna ]-name IN i_r_excluir_columnas.
        CONTINUE.
      ENDIF.
      "columnas que se excluyen
      IF i_caracter_exc_columna IS NOT INITIAL AND
         ti_componente_estructura[ indice_columna ]-name(2) EQ i_caracter_exc_columna.
        CONTINUE.
      ENDIF.

      TRY.
          DESCRIBE FIELD <fs_dato> TYPE DATA(tp_componente).

          CHECK tp_componente NOT IN r_tipo_no_dato_permitido.

          str_line = |{ str_line }{ i_separador }{ <fs_dato> }|.
        CATCH cx_root.

      ENDTRY.

    ENDDO.

    REPLACE FIRST OCCURRENCE OF i_separador IN str_line WITH space.

    r_str = str_line.

    """"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""
    "Funcionalidad para incluir el encabezado de la tabla
    IF i_incluir_encabezado = abap_true.
      CLEAR str_line.

      "Recorrer los campos de la tabla
      LOOP AT ti_componente_estructura ASSIGNING FIELD-SYMBOL(<fs_es_componentes_tabla>).

        IF i_r_excluir_columnas IS NOT INITIAL AND
           <fs_es_componentes_tabla>-name IN i_r_excluir_columnas.
          CONTINUE.
        ENDIF.
        "columnas que se excluyen
        IF i_caracter_exc_columna IS NOT INITIAL AND
           <fs_es_componentes_tabla>-name(2) EQ i_caracter_exc_columna.
          CONTINUE.
        ENDIF.

        "Asigna la referencia del tipo del campo
        o_elm_des ?= <fs_es_componentes_tabla>-type.

        CASE i_longitud_texto_columna.
          WHEN 'L'.
            str_line = |{ str_line }{ i_separador }{ o_elm_des->get_ddic_field( )-scrtext_l }|.
          WHEN 'M'.
            str_line = |{ str_line }{ i_separador }{ o_elm_des->get_ddic_field( )-scrtext_m }|.
          WHEN 'S'.
            str_line = |{ str_line }{ i_separador }{ o_elm_des->get_ddic_field( )-scrtext_s }|.
          WHEN OTHERS.
            str_line = |{ str_line }{ i_separador }{ <fs_es_componentes_tabla>-name }|.
        ENDCASE.

      ENDLOOP.

      REPLACE FIRST OCCURRENCE OF i_separador IN str_line WITH space.

      r_str = str_line && cl_abap_char_utilities=>cr_lf && r_str.

    ENDIF.

  ENDMETHOD.

  METHOD search_source.
    DATA:
      lt_source_vers  TYPE abaptxt255_tab,
      lv_string_found TYPE xfeld,
      lt_vrsd         TYPE STANDARD TABLE OF vrsd,
      ls_vrsd         LIKE LINE OF lt_vrsd,
      lv_number       TYPE i,
      lv_index        TYPE i,
      lt_results      TYPE match_result_tab,
      ls_result       LIKE LINE OF lt_results,
      ls_sstring      LIKE LINE OF sstring.

    CHECK sstring[] IS NOT INITIAL.

    IF p_vers IS INITIAL.
      lv_number = 1.
    ELSE.
      lt_vrsd = get_version_numbers( iv_report = gv_report
                                     iv_dynpro = gv_dynpro ).

      lv_number = lines( lt_vrsd ) + 1.
    ENDIF.

    DO lv_number TIMES.
      CLEAR lv_string_found.

      IF sy-index = 1.
        CLEAR ls_vrsd.
      ELSE.
        lv_index = sy-index - 1.
        READ TABLE lt_vrsd INDEX lv_index INTO ls_vrsd.
        CHECK sy-subrc IS INITIAL.

        lt_source_vers = get_source_by_version( iv_report = gv_report
                                                iv_dynpro = gv_dynpro
                                                iv_versno = ls_vrsd-versno ).

        IF lt_source_vers IS NOT INITIAL.
          gt_source = lt_source_vers.
        ELSE.
          CONTINUE.
        ENDIF.
      ENDIF.

      LOOP AT sstring INTO ls_sstring.
        REFRESH lt_results.

        IF p_regex IS INITIAL.
          FIND ALL OCCURRENCES OF ls_sstring-low IN TABLE gt_source
            IN CHARACTER MODE
            IGNORING CASE
            RESULTS lt_results.
        ELSE.
          TRY.
              FIND ALL OCCURRENCES OF REGEX ls_sstring-low IN TABLE gt_source
                IN CHARACTER MODE
                IGNORING CASE
                RESULTS lt_results.
            CATCH cx_sy_regex.
*             invalid regex -> stop processing
              go_log->set_es_log( i_es = VALUE #( id = 'EU' number = '384' type = 'E' message_v1 = ls_sstring-low ) ).
              RAISE EXCEPTION TYPE lcx_scan_exceptions.
          ENDTRY.
        ENDIF.

        CHECK lt_results IS NOT INITIAL.

        lv_string_found = gc_x.

        SORT lt_results BY line.
        DELETE ADJACENT DUPLICATES FROM lt_results COMPARING line.

        LOOP AT lt_results INTO ls_result.
          CALL METHOD get_hit_set
            EXPORTING
              iv_report = gv_report
              iv_dynpro = gv_dynpro
              it_abap   = gt_source
              iv_tabix  = ls_result-line
              iv_versno = ls_vrsd-versno.
        ENDLOOP.

      ENDLOOP.
      IF lv_string_found IS NOT INITIAL.
*       Add ALV header entry
        CLEAR gs_alv_header.

        gs_alv_header-repname = gv_report.
        gs_alv_header-dynnr   = gv_dynpro.
        gs_alv_header-versno  = ls_vrsd-versno.
        APPEND gs_alv_header TO gt_alv_header.

        IF gv_dynpro IS NOT INITIAL.
          gv_dynp_found = gc_x.
        ENDIF.

        IF ls_vrsd-versno IS NOT INITIAL.
          gv_vers_found = gc_x.
        ENDIF.
      ENDIF.
    ENDDO.

  ENDMETHOD.

  METHOD search_dynpro_source.

    DATA ls_dynpro LIKE LINE OF gt_dynpro.

    IF sy-batch IS INITIAL.
      CALL FUNCTION 'SAPGUI_PROGRESS_INDICATOR'
        EXPORTING
          text = 'SEARCH DYNPRO SOURCES...'.
    ENDIF.

    LOOP AT gt_dynpro INTO ls_dynpro.
      REFRESH gt_source.

      gv_report = ls_dynpro-repname.
      gv_dynpro = ls_dynpro-dynnr.

      gt_source = get_dynpro_flow_logic( iv_report = ls_dynpro-repname
                                         iv_dynpro = ls_dynpro-dynnr ).

      CHECK gt_source IS NOT INITIAL.

      APPEND INITIAL LINE TO gt_source_detail ASSIGNING FIELD-SYMBOL(<fs_s_source_detail>).
      <fs_s_source_detail> = VALUE #( package = ls_dynpro-devclass
                                      obj_name = |{ ls_dynpro-repname } { ls_dynpro-dynnr }|
                                      single_obj_name = ls_dynpro-single_obj_name
                                      obj_type = 'DYNR'
                                      source = ti_a_string( i_ti = gt_source i_separador = space ) ).
      <fs_s_source_detail>-source = replace( val = <fs_s_source_detail>-source sub = '\r' with = space ).
      <fs_s_source_detail>-source = replace( val = <fs_s_source_detail>-source sub = '\n' with = space ).

      search_source( ).

    ENDLOOP.

  ENDMETHOD.

  METHOD get_dynpros.

    CHECK gt_object IS NOT INITIAL.

    SELECT prog AS repname dnum AS dynnr INTO CORRESPONDING FIELDS OF TABLE gt_dynpro
      FROM d020s
      FOR ALL ENTRIES IN gt_object
      WHERE prog = gt_object-obj_name
      AND   dnum IN dynnr.

    LOOP AT gt_dynpro ASSIGNING FIELD-SYMBOL(<fs_s_dynpro>).
      DATA(ls_objet) = VALUE #( gt_object[ obj_name = <fs_s_dynpro>-repname ] OPTIONAL ).
      <fs_s_dynpro>-devclass = ls_objet-devclass.
      <fs_s_dynpro>-single_obj_name = ls_objet-single_obj_name.
    ENDLOOP.

  ENDMETHOD.                    "get_dynpros

  METHOD get_includes.
    DATA:
      lt_inc           TYPE STANDARD TABLE OF ty_s_object,
      lt_inc_tmp       TYPE STANDARD TABLE OF tadir-obj_name,
      lv_program       TYPE sy-repid,
      lv_obj           TYPE tadir-obj_name,
      source_class     TYPE string,
      class_name       TYPE seoclsname,
      t_class_includes TYPE seoincl_t.

    CHECK inclu IS NOT INITIAL.

    LOOP AT gt_object INTO DATA(s_objet).    "for classes we already have the includes

      lv_obj = s_objet-obj_name.

      IF lv_obj+30(2) = 'CP' AND s_objet-object_type = 'CLAS'. "Class Pool
        DELETE gt_object INDEX sy-tabix.

        class_name = lv_obj(30).
        TRANSLATE class_name USING '= '.

        cl_oo_classname_service=>get_all_class_includes(
          EXPORTING
            class_name = class_name
          RECEIVING
            result     = t_class_includes
          EXCEPTIONS
            OTHERS     = 0 ).

        DELETE t_class_includes WHERE table_line+30(2) = 'CS' OR table_line+30(2) = 'CP'.

        APPEND LINES OF VALUE ty_t_object( FOR s_class IN t_class_includes
                                           ( devclass = s_objet-devclass obj_name = s_class
                                             object_type = s_objet-object_type
                                             single_obj_name = s_objet-single_obj_name ) )
        TO lt_inc.

      ELSEIF lv_obj+30(2) = 'IP'. "Interface Pool
        DELETE gt_object INDEX sy-tabix.

        lv_obj+31(1) = 'U'.
        APPEND VALUE #( devclass = s_objet-devclass obj_name = lv_obj
                        object_type = s_objet-object_type
                        single_obj_name = s_objet-single_obj_name ) TO lt_inc.
      ENDIF.

      REFRESH lt_inc_tmp.
      lv_program = lv_obj.

      CALL FUNCTION 'RS_GET_ALL_INCLUDES'
        EXPORTING
          program    = lv_program
        TABLES
          includetab = lt_inc_tmp
        EXCEPTIONS
          OTHERS     = 0.
      APPEND LINES OF VALUE ty_t_object( FOR s_inc_tmp IN lt_inc_tmp
                                       ( devclass = s_objet-devclass obj_name = s_inc_tmp
                                         object_type = s_objet-object_type
                                         single_obj_name = s_objet-single_obj_name ) )
      TO lt_inc.

      "add E Includes
      SELECT include, substring( include,31,10 ) AS ext FROM d010inc
        WHERE master = @lv_program
        INTO TABLE @DATA(includes_ext).

      LOOP AT includes_ext ASSIGNING FIELD-SYMBOL(<include>) WHERE ext(1) = srext_ext_enhancement.
        INSERT VALUE #( devclass = s_objet-devclass obj_name = <include>-include
                        object_type = s_objet-object_type
                        single_obj_name = s_objet-single_obj_name ) INTO TABLE lt_inc.
      ENDLOOP.
    ENDLOOP.

    SORT lt_inc.
    DELETE ADJACENT DUPLICATES FROM lt_inc.

    APPEND LINES OF lt_inc TO gt_object.

  ENDMETHOD.

  METHOD get_report_names.
    SELECT devclass, obj_name, object AS object_type, obj_name  AS single_obj_name
    INTO CORRESPONDING FIELDS OF TABLE @gt_object
      FROM tadir
      WHERE pgmid  = 'R3TR'
      AND   object = 'PROG'
      AND   delflag = @space
      AND   devclass IN @devclass.                      "#EC CI_GENBUFF
  ENDMETHOD.                    "get_report_names

  METHOD get_function_names.
    DATA:
      lt_obj     TYPE STANDARD TABLE OF ty_s_object,
      lv_obj     TYPE tadir-obj_name,
      lv_fgroup  TYPE rs38l-area,
      lv_program TYPE progname.

    FIELD-SYMBOLS:
      <lv_obj> LIKE LINE OF lt_obj.

    SELECT devclass, obj_name, object AS object_type INTO CORRESPONDING FIELDS OF TABLE @lt_obj
      FROM tadir
      WHERE pgmid  = 'R3TR'
      AND   object = 'FUGR'
      AND   delflag = @space
      AND   devclass IN @devclass
      AND   obj_name IN @funcgrp.                       "#EC CI_GENBUFF

    LOOP AT lt_obj ASSIGNING <lv_obj>.
      lv_fgroup = <lv_obj>-obj_name.
      CLEAR lv_program.

      CALL FUNCTION 'FUNCTION_INCLUDE_CONCATENATE'
        CHANGING
          program       = lv_program
          complete_area = lv_fgroup
        EXCEPTIONS
          OTHERS        = 1.

      CHECK sy-subrc IS INITIAL AND lv_program IS NOT INITIAL.

      lv_obj = lv_program.
      APPEND VALUE #( devclass = <lv_obj>-devclass obj_name = lv_obj
                      object_type = <lv_obj>-object_type
                      single_obj_name = <lv_obj>-obj_name ) TO gt_object.
    ENDLOOP.
  ENDMETHOD.                    "get_function_names

  METHOD get_class_names.
    DATA lt_obj TYPE STANDARD TABLE OF ty_s_object.
    DATA ls_obj LIKE LINE OF lt_obj.

    SELECT devclass, obj_name, object AS object_type INTO CORRESPONDING FIELDS OF TABLE @lt_obj
      FROM tadir
      WHERE pgmid  = 'R3TR'
      AND   object = 'CLAS'
      AND   delflag = @space
      AND   devclass IN @devclass
      AND   obj_name IN @p_class.                       "#EC CI_GENBUFF

    LOOP AT lt_obj INTO ls_obj.
      APPEND VALUE #( devclass = ls_obj-devclass
                      obj_name = cl_oo_classname_service=>get_classpool_name( |{ ls_obj-obj_name }| )
                      object_type = ls_obj-object_type
                      single_obj_name = ls_obj-obj_name )
      TO gt_object.
    ENDLOOP.

  ENDMETHOD.

  METHOD get_interface_names.
    DATA lt_obj TYPE STANDARD TABLE OF ty_s_object.
    DATA ls_obj LIKE LINE OF lt_obj.

    SELECT devclass, obj_name, object AS object_type INTO CORRESPONDING FIELDS OF TABLE @lt_obj
      FROM tadir
      WHERE pgmid  = 'R3TR'
      AND   object = 'INTF'
      AND   delflag = @space
      AND   devclass IN @devclass
      AND   obj_name IN @p_class.                       "#EC CI_GENBUFF

    LOOP AT lt_obj INTO ls_obj.
      APPEND VALUE #( devclass = ls_obj-devclass
                      object_type = ls_obj-object_type
                      obj_name = cl_oo_classname_service=>get_interfacepool_name( |{ ls_obj-obj_name }| )
                      single_obj_name = ls_obj-obj_name )
      TO gt_object.
    ENDLOOP.

  ENDMETHOD.

  METHOD get_source_names.

    IF repname IS INITIAL.
      IF devclass[] IS NOT INITIAL.
        get_package_hierachy( ).
        get_report_names( ).
        get_function_names( ).
        get_class_names( ).
        get_interface_names( ).
        get_enhancement(  ).
        get_cds_view(  ).
      ENDIF.

      IF funcgrp[] IS NOT INITIAL.
        get_function_names( ).
      ENDIF.

      IF p_class[] IS NOT INITIAL.
        get_class_names( ).
        get_interface_names( ).
      ENDIF.
    ENDIF.

    IF repname[] IS NOT INITIAL OR
       cnam[]    IS NOT INITIAL OR
       unam[]    IS NOT INITIAL OR
       subc[]    IS NOT INITIAL OR
       appl[]    IS NOT INITIAL.

      SELECT devclass, name AS obj_name
      APPENDING CORRESPONDING FIELDS OF TABLE @gt_object
        FROM trdir
        INNER JOIN tadir ON
        tadir~obj_name = trdir~name
        WHERE name IN @repname
        AND   cnam IN @cnam
        AND   unam IN @unam
        AND   subc IN @subc
        AND   delflag = @space
        AND   appl IN @appl.
    ENDIF.

    IF rb_code IS INITIAL.
      get_dynpros( ).
    ENDIF.

    SORT gt_object.
    DELETE ADJACENT DUPLICATES FROM gt_object.

  ENDMETHOD.                    "get_source_names

  METHOD start.
    DATA:
     ls_sstring LIKE LINE OF sstring[].

    go_log = lcl_log_aplicacion=>get_instancia(  ).

    IF NOT modiass IS INITIAL.
      REFRESH sstring.
      ls_sstring-sign    = 'I'.
      ls_sstring-option  = 'EQ'.
      ls_sstring-low     = '^\*\{'.
      APPEND ls_sstring TO sstring.
      ls_sstring-low     = '^\*\}'.
      APPEND ls_sstring TO sstring.

      p_regex = gc_x.
    ENDIF.

    READ TABLE sstring[] INTO ls_sstring INDEX 1.
    IF lines( sstring[] ) = 1.
      gv_sstring = ls_sstring-low.
    ELSE.
      CONCATENATE ls_sstring-low
                  '...'
                  INTO gv_sstring.
    ENDIF.

    IF sy-batch IS INITIAL.
      CALL FUNCTION 'SAPGUI_PROGRESS_INDICATOR'
        EXPORTING
          text = 'GET INCLUDES...'.
    ENDIF.

    IF p_debug = abap_true.
      BREAK-POINT.
    ENDIF.


    get_source_names( ).
    get_includes( ).

    IF rb_dyn IS INITIAL.
      TRY.
          search_abap_source( ).
        CATCH lcx_scan_exceptions.
          RETURN.
      ENDTRY.
    ENDIF.

    IF rb_code IS INITIAL.
      TRY.
          search_dynpro_source( ).
        CATCH lcx_scan_exceptions.
          RETURN.
      ENDTRY.
    ENDIF.

    IF p_debug = abap_true.
      BREAK-POINT.
    ENDIF.

*We print this information:
    get_object_reference(  ).

    export_data_to_zip( ).

    go_log->mostrar_log( i_ventana_emergente = abap_true i_limpiar_mensajes = abap_true ).
    "cl_demo_output=>display( json_source ).
***    display( ).
  ENDMETHOD.

  METHOD get_object_reference.

    LOOP AT gt_source_detail REFERENCE INTO DATA(r_s_source_detail_gr)
                             GROUP BY ( package = r_s_source_detail_gr->package ).

      DATA(lt_founds) = zclcx_object_reference=>get_object_references( iv_object_name = r_s_source_detail_gr->obj_name
                                                                       iv_object_type = CONV #( r_s_source_detail_gr->obj_type )
                                                                       iv_depth       = 5 ).

    ENDLOOP.

  ENDMETHOD.

  METHOD export_data_to_zip.

    DATA: x_contenido         TYPE xstring,
          nombre_archivo      TYPE string,
          directorio          TYPE string,
          directorio_completo TYPE string,
          tipo_de_fichero     TYPE char50,
          lt_source_detail    LIKE gt_source_detail,
          lt_zip_content      TYPE gtp_ti_datos_zip,
          accion_usuario      TYPE i.

    tipo_de_fichero = 'text/plain; charset=utf-8'.

    LOOP AT gt_source_detail REFERENCE INTO DATA(r_s_source_detail_gr)
                             GROUP BY ( package = r_s_source_detail_gr->package ).

      CLEAR: lt_source_detail.

      LOOP AT GROUP r_s_source_detail_gr ASSIGNING FIELD-SYMBOL(<fs_s_source>).
        APPEND <fs_s_source> TO lt_source_detail.
      ENDLOOP.

      DATA(json_source) = /ui2/cl_json=>serialize( data        = lt_source_detail
                                                   compress    = abap_true
                                                   pretty_name = /ui2/cl_json=>pretty_mode-camel_case ).

      CALL FUNCTION 'SCMS_STRING_TO_XSTRING'
        EXPORTING
          text     = json_source
          mimetype = tipo_de_fichero
        IMPORTING
          buffer   = x_contenido
        EXCEPTIONS
          failed   = 1
          OTHERS   = 2.

      IF sy-subrc <> 0.
        MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
          WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
      ENDIF.

      APPEND VALUE #( nombre = |source_{ r_s_source_detail_gr->package }_{ sy-datum }{ sy-uzeit }.json| contenido = x_contenido )
      TO lt_zip_content.

    ENDLOOP.

    crear_archivo_zip(
      EXPORTING
        i_ti_contenido_zip = lt_zip_content
      IMPORTING
        e_longitud_salida  = DATA(longitud_salida_zip)
        e_ti_binario       = DATA(ti_archivo_zip) ).

    cl_gui_frontend_services=>file_save_dialog(
      EXPORTING
*       window_title              =
        default_extension         = 'zip'
        default_file_name         = |{ sy-sysid }_export_template_{ sy-datum }{ sy-uzeit }|
*       with_encoding             =
        file_filter               = '*.zip'
*       initial_directory         =
*       prompt_on_overwrite       = 'X'
      CHANGING
        filename                  = nombre_archivo
        path                      = directorio
        fullpath                  = directorio_completo
        user_action               = accion_usuario
      EXCEPTIONS
        cntl_error                = 1
        error_no_gui              = 2
        not_supported_by_gui      = 3
        invalid_default_file_name = 4
        OTHERS                    = 5 ).

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
        WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

    CHECK accion_usuario = 0.

    directorio_completo = directorio_completo.

    cl_gui_frontend_services=>gui_download(
      EXPORTING
        bin_filesize            = longitud_salida_zip
        filename                = directorio_completo
        filetype                = 'BIN'
      CHANGING
        data_tab                = ti_archivo_zip
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
        not_supported_by_gui    = 22
        error_no_gui            = 23
        OTHERS                  = 24 ).

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
        WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

  ENDMETHOD.

  METHOD crear_archivo_zip.

    DATA: o_zip       TYPE REF TO cl_abap_zip,
          zip_content TYPE xstring.

    CREATE OBJECT o_zip.

    "agregar contenido a ZIP
    LOOP AT i_ti_contenido_zip INTO DATA(es_zip_data).

      o_zip->add( name = es_zip_data-nombre content = es_zip_data-contenido ).

    ENDLOOP.

    zip_content   = o_zip->save( ).

* convierte xstring a binary
    CALL FUNCTION 'SCMS_XSTRING_TO_BINARY'
      EXPORTING
        buffer        = zip_content
      IMPORTING
        output_length = e_longitud_salida
      TABLES
        binary_tab    = e_ti_binario.

  ENDMETHOD.

  METHOD pbo.
    DATA ls_screen TYPE screen.

    CHECK modiass IS NOT INITIAL.

    REFRESH sstring[].
    CLEAR   sstring.
    CLEAR   p_regex.

    LOOP AT SCREEN INTO ls_screen.
      CHECK ls_screen-group1 = 'DSP'.
      ls_screen-input = '0'.
      MODIFY SCREEN FROM ls_screen.
    ENDLOOP.
  ENDMETHOD.                    "pbo

  METHOD get_package_hierachy.

    SELECT * FROM tdevc
    WHERE parentcl IN @devclass
    INTO TABLE @DATA(t_package).

    devclass[] = VALUE #( BASE devclass[]
                          FOR s_package IN t_package
                          ( sign = 'I' option = 'EQ' low = s_package-devclass ) ).

  ENDMETHOD.


  METHOD get_enhancement.

    DATA lt_obj TYPE STANDARD TABLE OF ty_s_object.
    DATA ls_obj LIKE LINE OF lt_obj.

    SELECT devclass, enhinclude AS obj_name, object AS object_type
    FROM enhincinx AS e
    INNER JOIN tadir AS t ON 'R3TR' = t~pgmid
                         AND 'ENHO' = t~object
                         AND   delflag = @space
                         AND e~enhname = t~obj_name
    WHERE t~devclass IN @devclass
    INTO CORRESPONDING FIELDS OF TABLE @lt_obj.

    LOOP AT lt_obj INTO ls_obj.
      APPEND VALUE #( devclass = ls_obj-devclass object_type = ls_obj-object_type
                      obj_name = ls_obj-obj_name
                      single_obj_name = ls_obj-obj_name )
      TO gt_object.
    ENDLOOP.

  ENDMETHOD.


  METHOD get_class_source_code.
    DATA: t_class_source LIKE gt_source,
          clstype        TYPE seoclstype,
          source         TYPE seop_source_string,
          pool_source    TYPE seop_source_string,
          source_line    TYPE LINE OF seop_source_string,
          tabix          TYPE sytabix,
          includes       TYPE seop_methods_w_include,
          include        TYPE seop_method_w_include,
          cifref         TYPE REF TO if_oo_clif_incl_naming,
          clsref         TYPE REF TO if_oo_class_incl_naming,
          l_string       TYPE string,
          intref         TYPE REF TO if_oo_interface_incl_naming.

    CALL METHOD cl_oo_include_naming=>get_instance_by_cifkey
      EXPORTING
        cifkey = CONV #( i_class_name )
      RECEIVING
        cifref = cifref
      EXCEPTIONS
        OTHERS = 1.

    IF sy-subrc <> 0.
      go_log->set_es_log( i_es = VALUE #( id = 'OO' number = '003' type = 'W' message_v1 = i_class_name ) ).
      RETURN.
    ENDIF.

    CASE cifref->clstype.
      WHEN seoc_clstype_class.
        clsref ?= cifref.
        READ REPORT clsref->class_pool
          INTO pool_source.
        LOOP AT pool_source INTO source_line.
          IF source_line CS 'CLASS-POOL'
            OR source_line CS 'class-pool'.
            APPEND source_line TO c_t_source."WRITE / source_line.
            tabix = sy-tabix.
            EXIT.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->locals_old
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->locals_def
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->locals_imp
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->macros
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->public_section
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->protected_section
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        READ REPORT clsref->private_section
          INTO source.
        LOOP AT source
          INTO source_line.
          IF source_line NS '*"*'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.
        CONCATENATE 'CLASS' i_class_name 'IMPLEMENTATION' INTO l_string SEPARATED BY space.
        LOOP AT pool_source FROM tabix INTO source_line.

          tabix = sy-tabix.

          IF source_line CS 'ENDCLASS'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
          IF source_line CS l_string.

            APPEND source_line TO c_t_source."WRITE / source_line.

            EXIT.
          ENDIF.
        ENDLOOP.
* method implementation
        includes = clsref->get_all_method_includes( ).
        LOOP AT includes
          INTO include.
          READ REPORT include-incname
            INTO source.

          LOOP AT source
            INTO source_line.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDLOOP.
        ENDLOOP.
        LOOP AT pool_source FROM tabix INTO source_line.
          IF source_line CS 'ENDCLASS'.
            APPEND source_line TO c_t_source."WRITE / source_line.
          ENDIF.
        ENDLOOP.

        "Test unit class
        READ REPORT clsref->tests INTO t_class_source.

        IF sy-subrc IS INITIAL.
          APPEND LINES OF t_class_source TO c_t_source.
        ENDIF.

      WHEN seoc_clstype_interface.
        intref ?= cifref.
        READ REPORT intref->interface_pool
          INTO source.
        LOOP AT source INTO source_line.
          APPEND source_line TO c_t_source."WRITE / source_line.
        ENDLOOP.

        READ REPORT intref->public_section
          INTO source.
        LOOP AT source INTO source_line.
          APPEND source_line TO c_t_source."WRITE / source_line.
        ENDLOOP.

    ENDCASE.
  ENDMETHOD.


  METHOD get_cds_view.

    SELECT devclass, obj_name, object AS object_type, obj_name AS single_obj_name
    APPENDING CORRESPONDING FIELDS OF TABLE @gt_object
      FROM tadir
      WHERE pgmid  = 'R3TR'
      AND   object = 'DDLS'
      AND   delflag = @space
      AND   devclass IN @devclass.

  ENDMETHOD.


  METHOD get_cds_view_source.

    DATA: lv_srcname    TYPE ddlname,
          mv_got_state  TYPE objstate,
          ms_ddlsrc     TYPE ddddlsrcv,
          mo_dd_handler TYPE REF TO if_dd_ddl_handler_internal.

    lv_srcname = i_class_name.

    " check if source with that name exists or if it is the
    " name of a generated object
    DATA ls_src TYPE ddddlsrc ##needed.
    DATA ls_dep TYPE ddldependency.
    SELECT ddlname FROM ddddlsrc UP TO 1 ROWS
    INTO ls_src-ddlname
      WHERE ddlname = lv_srcname.
      EXIT.
    ENDSELECT.
    IF sy-subrc <> 0.
      " no src with this name; check for generated object in other source
      SELECT ddlname FROM ddldependency UP TO 1 ROWS INTO ls_dep-ddlname
        WHERE objectname = lv_srcname.                  "#EC CI_NOORDER
      ENDSELECT.
      IF sy-subrc = 0 .
        lv_srcname = ls_dep-ddlname.
        MESSAGE lv_srcname TYPE 'S' DISPLAY LIKE 'W'.
      ENDIF.
      " if nothing is found here either,
      " we'll run into "object not found" later 'officially'
    ENDIF.

    TRY.
        mo_dd_handler = cl_dd_ddl_handler_factory=>create_internal( ).

        CALL METHOD mo_dd_handler->read
          EXPORTING
            name          = lv_srcname
            get_state     = 'M'    " Version of DDL source to be read
            get_internals = abap_true
          IMPORTING
            ddddlsrcv_wa  = ms_ddlsrc
            got_state     = mv_got_state.
        IF mv_got_state IS INITIAL.
          RAISE EXCEPTION TYPE cx_dd_ddl_read
            EXPORTING
              textid = cx_dd_ddl_read=>ddl_not_found.
        ENDIF.

        APPEND ms_ddlsrc-source TO c_t_source.

      CATCH cx_dd_ddl_read INTO DATA(ex).
        go_log->set_o_cx_log( i_o_cx_log = ex ).
    ENDTRY.

  ENDMETHOD.

  METHOD get_object_references.

  ENDMETHOD.

ENDCLASS.                    "lcl_source_scan IMPLEMENTATION

INITIALIZATION.

  DATA(go_source) = NEW lcl_source_scan( ).

START-OF-SELECTION.
  go_source->start( ).

end-OF-SELECTION.
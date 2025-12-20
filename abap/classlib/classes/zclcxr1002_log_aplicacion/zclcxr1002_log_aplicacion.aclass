CLASS zclcxr1002_log_aplicacion DEFINITION
  PUBLIC
  CREATE PRIVATE .

  PUBLIC SECTION.

    CONSTANTS: gc_id_generico TYPE string VALUE 'LOG_GENERICO'.

    TYPES:
      BEGIN OF gtp_es_log,
        icon             TYPE icon_d,
        fila_referencia  TYPE zed_filareferencia,
        fila_descripcion TYPE zed_filadescripcion,
        columna          TYPE kcd_ex_col_n,
        probclass        TYPE balprobcl.
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
             o_log TYPE REF TO zclcxr1002_log_aplicacion,
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
          VALUE(r_o_log)             TYPE REF TO zclcxr1002_log_aplicacion,

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
          VALUE(r_error)    TYPE flag,
      mostrar_log_instancia_bd.



  PROTECTED SECTION.

    DATA: gti_log         TYPE gtp_ti_log.

    CLASS-DATA: gti_manejador_log          TYPE gtp_ti_manejador_log,
                g_id_log_actual            TYPE string,
                g_descartar_msj_duplicados TYPE flag.

  PRIVATE SECTION.

    CLASS-DATA: gti_instancias_log TYPE gtp_ti_instancia_log,
                go_log             TYPE REF TO zclcxr1002_log_aplicacion.
    METHODS unico_manejador_de_log
      RETURNING
        VALUE(r_unico_manejador) TYPE flag.

ENDCLASS.



CLASS zclcxr1002_log_aplicacion IMPLEMENTATION.

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

      go_log = NEW zclcxr1002_log_aplicacion(  ).

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
                                                               msgv3 = message_v3 msgv4 = message_v4
                                                               probclass = probclass )
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

  METHOD mostrar_log_instancia_bd.

    CALL FUNCTION 'BAL_DSP_LOG_DISPLAY'
      EXCEPTIONS
        profile_inconsistent = 1
        internal_error       = 2
        no_data_available    = 3
        no_authority         = 4
        OTHERS               = 5.

    IF sy-subrc <> 0.
      mostrar_log( ).
*     MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
*       WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

  ENDMETHOD.

ENDCLASS.
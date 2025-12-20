CLASS zclcxr1001_monitor_integracion DEFINITION
  PUBLIC
  CREATE PUBLIC .

  PUBLIC SECTION.

    "{ Inicio SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito
    CONSTANTS: gc_id_memoria TYPE char20 VALUE 'ID_MEMORIA_REPROCESO'.
    "{ Fin SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito

    TYPES:
      ".Tipo Info adicional mensaje
      BEGIN OF gtp_info_ad,
        ricefw    TYPE zzedricefw,
        json      TYPE zzedpayload,
        tipo_dato TYPE zedtipoobjeto,
      END OF gtp_info_ad .

    CONSTANTS: BEGIN OF gc_id_mensaje_encolado,
                 id         TYPE symsgid VALUE 'ZCX01',
                 numero_msj TYPE symsgno VALUE '999',
                 tipo       TYPE char1 VALUE 'W',
               END OF gc_id_mensaje_encolado.

    CONSTANTS gc_000 TYPE symsgno VALUE '000' ##NO_TEXT.
    CONSTANTS gc_036 TYPE symsgno VALUE '036' ##NO_TEXT.
    CONSTANTS gc_999 TYPE symsgno VALUE '999' ##NO_TEXT.
    CONSTANTS gc_zcx01 TYPE symsgid VALUE 'ZCX01' ##NO_TEXT.
    CLASS-DATA gc_a TYPE char1 VALUE 'A' ##NO_TEXT.
    CLASS-DATA gc_e TYPE char1 VALUE 'E' ##NO_TEXT.
    CLASS-DATA gc_s TYPE char1 VALUE 'S' ##NO_TEXT.
    CLASS-DATA gc_w TYPE char1 VALUE 'W' ##NO_TEXT.
*&----------------------------------------------------------------------*
*& Definicion de Objetos
*&----------------------------------------------------------------------*
    DATA go_log_aplicacion TYPE REF TO zclcxr1002_log_aplicacion .
*&----------------------------------------------------------------------*
*& Definicion de Tablas Internas
*&----------------------------------------------------------------------*
*      gti_log         TYPE bapiret2_tab,
    DATA gti_datos_ptoxy TYPE REF TO data .
*&----------------------------------------------------------------------*
*& Definicion de Estructuras
*&----------------------------------------------------------------------*
    DATA ges_info_mensaje TYPE ztcxr1001_1 .

    DATA:
      gc_no_commit TYPE flag.

    "! Constructor
    "! @parameter i_ricefw | Codigo RICEFW del proceso
    "! @parameter i_es_info_mensaje | Informacion del Mensaje
    "! @parameter i_datos_proxy | Informacion para ser procesada
    "! @parameter i_no_commit | Flag confirmacion de NO Commit
    "! @raising zcxr1001_excepciones_monitor | Manejo de Excepcion
    METHODS constructor
      IMPORTING
        VALUE(i_ricefw)          TYPE zzedricefw OPTIONAL
        VALUE(i_es_info_mensaje) TYPE ztcxr1001_1 OPTIONAL
        VALUE(i_datos_proxy)     TYPE any OPTIONAL
        VALUE(i_no_commit)       TYPE flag OPTIONAL
      RAISING
        zcxr1001_excepciones_monitor .

    METHODS:
      "! Metodo Principal
      "!
      "! @parameter i_datos_proxy | Informacion para ser procesada
      "! @parameter i_registrar_respuesta | Guardar datos de respuesta
      "! @parameter e_datos_proxy | Datos de la peticion
      "! @parameter e_ti_msg_respuesta | Resultado del proceso
      main
        IMPORTING
          VALUE(i_datos_proxy)      TYPE REF TO data OPTIONAL
          i_registrar_respuesta     TYPE flag DEFAULT abap_false
        EXPORTING
          VALUE(e_datos_proxy)      TYPE REF TO data
          VALUE(e_ti_msg_respuesta) TYPE bapiret2_t,
      registrar_respuesta
        IMPORTING
          i_ejecutar_commit        TYPE flag DEFAULT abap_true
          VALUE(i_info_mensaje)    TYPE ztcxr1001_1 OPTIONAL
          VALUE(i_datos_respuesta) TYPE REF TO data
        CHANGING
          c_ti_return              TYPE bapiret2_t OPTIONAL.

    CLASS-METHODS:
      retener_mensaje
        IMPORTING
          i_ricefw                      TYPE ztcxr1001_3-ricefw
          i_msgguid                     TYPE ztcxr1001_3-msgguid
          VALUE(i_paso_detener_proceso) TYPE ztcxr1001_3-nropaso
        CHANGING
          c_ti_return                   TYPE bapiret2_t ,
      completar_mensajes
        CHANGING
          c_ti_msg_retorno TYPE bapiret2_t,
      registrar_xml
        IMPORTING
          i_ejecutar_commit     TYPE flag DEFAULT abap_true
          VALUE(i_info_mensaje) TYPE ztcxr1001_1
          VALUE(i_xml)          TYPE string
        CHANGING
          c_ti_return           TYPE bapiret2_t OPTIONAL,
      registrar_data_gcp
        IMPORTING
          i_ejecutar_commit     TYPE flag DEFAULT abap_true
          VALUE(i_info_mensaje) TYPE ztcxr1001_1
          i_id_msg_gcp          TYPE ztcxr1001_1-id_msg_gcp
          i_topico_gcp          TYPE ztcxr1001_1-topico_gcp
        CHANGING
          c_ti_return           TYPE bapiret2_t OPTIONAL.

  PROTECTED SECTION.

    "! Metodo para deserializar la informacion del JSON
    "! @parameter i_es_info_mensaje | Informacion del Mensaje
    "! @parameter e_data | Informacion para ser Re-procesada
    CLASS-METHODS deserializar_json
      IMPORTING
        VALUE(i_es_info_mensaje) TYPE ztcxr1001_1
      EXPORTING
        !e_data                  TYPE REF TO data .
    CLASS-METHODS serializar_datos_json
      IMPORTING
        !i_data       TYPE REF TO data
      RETURNING
        VALUE(e_json) TYPE string .
  PRIVATE SECTION.

    CLASS-METHODS
      get_outbound_message_id
        RETURNING
          VALUE(r_messageid) TYPE sxmsmguid.
    "! Método para obtener el Message ID
    "! @parameter r_messageid | Identificador del mensaje
    CLASS-METHODS get_message_id
      RETURNING
        VALUE(r_messageid) TYPE sxmsmguid .
    "! Metodo para obtener la informacion del mensaje
    "! @parameter i_messageid | Identificador del mensaje
    "! @parameter i_es_info_ad | Informacion adicional
    "! @parameter e_es_info_mensaje | Informacion del Mensaje
    "! @raising zcxr1001_excepciones_monitor | Manejo de Excepcion
    METHODS get_informacion_mensaje
      IMPORTING
        VALUE(i_messageid)  TYPE sxmsmguid
        VALUE(i_es_info_ad) TYPE gtp_info_ad
      EXPORTING
        !e_es_info_mensaje  TYPE ztcxr1001_1
      RAISING
        zcxr1001_excepciones_monitor .
    METHODS guardar_mensaje
      IMPORTING
        !i_info_mensaje TYPE ztcxr1001_1 .
    "! Metodo para registrar la informacion del mensaje cuando esta viene de una RFC
    "! @parameter i_es_info_ad | Informacion adicional del mensaje
    "! @parameter c_es_info_mensaje | Informacion del Mensaje
    METHODS set_informacion_rfc
      IMPORTING
        VALUE(i_es_info_ad) TYPE gtp_info_ad
      CHANGING
        !c_es_info_mensaje  TYPE ztcxr1001_1 .
    "! Metodo para registrar el log del proceso
    "! @parameter i_msgid | ID del mensaje
    "! @parameter i_es_pasos | Registro del paso
    "! @parameter i_ti_return | Informacion de los errores
    METHODS registrar_log
      IMPORTING
        VALUE(i_msgid)         TYPE sxmsmguid
        VALUE(i_es_pasos)      TYPE ztcxr1001_2
        VALUE(i_nro_ejecucion) TYPE i
        VALUE(i_ti_return)     TYPE bapiret2_t .
    "! Metodo que retorna el ID Mensaje desde un rango de numeros
    "! @parameter c_idmensaje | ID Mensaje
    CLASS-METHODS get_rango_idmensaje
      RETURNING
        VALUE(c_idmensaje) TYPE cep_counter .
    METHODS save_json
      IMPORTING
        !i_datos_proxy TYPE any
        !i_es_pasos    TYPE ztcxr1001_2 .
ENDCLASS.



CLASS zclcxr1001_monitor_integracion IMPLEMENTATION.


  METHOD constructor.

*&----------------------------------------------------------------------*
*&     Definición de variables
*&----------------------------------------------------------------------*
    DATA:
      v_part1      TYPE  abap_abstypename,
      v_part2      TYPE  abap_abstypename,
      v_class_name TYPE  abap_abstypename,
      v_type_name  TYPE  abap_abstypename.


*&----------------------------------------------------------------------*
*&     Definición de variables de referencia
*&----------------------------------------------------------------------*
    DATA:
      lo_type_def  TYPE REF TO cl_abap_typedescr,
      lo_stdescribe        TYPE REF TO cl_abap_structdescr,
      lo_tabledescr        TYPE REF TO cl_abap_tabledescr.

    CLEAR:
           ges_info_mensaje,
           gc_no_commit.
    gc_no_commit = i_no_commit.
    "Si es procesamiento Nuevo
    IF i_es_info_mensaje-msgguid IS INITIAL.
      "SE COMENTAREA Esto es debido a que cuando se envia un mensaje sin nada de datos para garantizar de quede
      "en el monitor se requiere almenos el RICEF.
*      IF i_datos_proxy IS NOT INITIAL.
      ".Obtenemos el JSON
      DATA(json) = serializar_datos_json( REF #( i_datos_proxy )  ).

      ". Using RTTS to get the runtime type information of the internal table
      lo_type_def  = cl_abap_tabledescr=>describe_by_data( i_datos_proxy ).

      ".Obtenemos el nombre del tipo de Objeto
      DATA(tipo_objeto) = lo_type_def->absolute_name+6.
      "+{SLS 23072025 - extraer tipo de objeto cuando pueda ser de una clase global
      TRY.
          IF lo_type_def->absolute_name CS '\TYPE=%_T00'.

            lo_tabledescr ?= lo_type_def.
            lo_stdescribe ?= lo_tabledescr->get_table_line_type( ).
            IF lo_stdescribe->absolute_name CS 'CLASS='.
              lo_type_def ?= lo_stdescribe.
              tipo_objeto = lo_stdescribe->absolute_name.
            ENDIF.
          ENDIF.
        CATCH cx_sy_move_cast_error cx_root INTO DATA(o_cx_cast) ##NO_HANDLER.

      ENDTRY.
      "}+SLS

*Inicio Adicion 21Mar2024 Sebastian Restrepo Villa
*NOTA: Si el tipo hace referencia a una clase se debe construir el tipo de manera estatica para accederlo
      IF lo_type_def->absolute_name CS 'CLASS='.
        SPLIT lo_type_def->absolute_name AT '=' INTO v_part1 v_part2 v_type_name.
        SPLIT v_part2 AT '\' INTO v_class_name v_part1.

        CONCATENATE v_class_name
                    '=>'
                    v_type_name
               INTO tipo_objeto.
      ENDIF.
*Fin Adicion 21Mar2024 Sebastian Restrepo Villa

      ".Informacion adicional del mensaje
      DATA(es_info_adicional) = VALUE gtp_info_ad( ricefw = i_ricefw
                                                   json = json
                                                   tipo_dato = tipo_objeto ).
*      ENDIF.

      ".Obtenemos el identificador del mensaje
      DATA(messageid) = me->get_message_id( ).

      TRY .
          ".Obtenemos la informacion del mensaje
          me->get_informacion_mensaje(
            EXPORTING
              i_messageid   = messageid
              i_es_info_ad  = es_info_adicional
            IMPORTING
              e_es_info_mensaje = ges_info_mensaje
          ).
        CATCH zcxr1001_excepciones_monitor.
          ges_info_mensaje = i_es_info_mensaje.
          ges_info_mensaje-msgguid = messageid.
          ".Registramos la informacion que llega de la RFC
          me->set_informacion_rfc(
            EXPORTING
              i_es_info_ad      = es_info_adicional
            CHANGING
              c_es_info_mensaje = ges_info_mensaje
          ).
      ENDTRY.
      "Por defecto Registrar como Inactivo
      ges_info_mensaje-status = icon_led_inactive.
      ".Registramos la informacion del mensaje
      me->guardar_mensaje( i_info_mensaje = ges_info_mensaje ).
    ELSE.
      ges_info_mensaje = i_es_info_mensaje.
    ENDIF.

  ENDMETHOD.


  METHOD get_message_id.
    DATA:
          idmensaje	TYPE cep_counter.

    TRY.
        ".Obtenemos Message ID
        CALL METHOD cl_proxy_access=>get_inbound_message_key
          IMPORTING
            message_id = r_messageid.
      CATCH cx_ai_system_fault.
        "En caso de que no se ejecute por proxy se consume un rango para definir un ID
*        CALL METHOD get_rango_idmensaje
*          RECEIVING
*            c_idmensaje = idmensaje.
    ENDTRY.

    IF r_messageid IS INITIAL.

      r_messageid = get_outbound_message_id( ).

      "En caso de que no se ejecute por proxy se consume un rango para definir un ID
      CALL METHOD get_rango_idmensaje
        RECEIVING
          c_idmensaje = idmensaje.

      r_messageid = idmensaje.
    ENDIF.

  ENDMETHOD.


  METHOD get_informacion_mensaje.

*&----------------------------------------------------------------------*
*& Definicion de Tablas internas
*&----------------------------------------------------------------------*
    DATA:
      ti_idmensaje     TYPE sxmsmguidt,
      ti_pipeline      TYPE sxms_tab_sxmspid,
      ti_info_mensaje  TYPE sxi_message_data_list,
      ti_tabla_binaria TYPE STANDARD TABLE OF x255.

*&----------------------------------------------------------------------*
*& Definicion de variables
*&----------------------------------------------------------------------*
    DATA:
      fecha_ejecucion TYPE sydatum,
      hora_ejecucion  TYPE syuzeit,
      hora_final      TYPE syuzeit,
      payload_xs      TYPE xstring,
      payload_s       TYPE string,
      ouput_longitud  TYPE i.


    ti_idmensaje = VALUE #( ( i_messageid ) ).

    ".Obtenemos la informacion del mensaje
    CALL FUNCTION 'SXMB_GET_MESSAGE_DATA'
      EXPORTING
        im_message_list      = ti_idmensaje
        im_pipeline_list     = ti_pipeline
      IMPORTING
        ex_message_data_list = ti_info_mensaje
      EXCEPTIONS
        not_authorized       = 1
        OTHERS               = 2.
    IF sy-subrc <> 0.
      RAISE EXCEPTION TYPE zcxr1001_excepciones_monitor
        EXPORTING
          textid = zcxr1001_excepciones_monitor=>idmsg_no_encontrado
          arg_1  = CONV #( i_messageid ).
    ENDIF.

    ".Verificamos si existe informacion del mensaje
    IF NOT line_exists( ti_info_mensaje[ 1 ] ).
      RAISE EXCEPTION TYPE zcxr1001_excepciones_monitor
        EXPORTING
          textid = zcxr1001_excepciones_monitor=>idmsg_no_encontrado
          arg_1  = CONV #( i_messageid ).
    ENDIF.

    TRY.
        DATA(es_datos) = ti_info_mensaje[ msgguid = i_messageid ].

        DATA(es_sxmsmkey) = VALUE sxmsmkey( msgid = es_datos-msgguid
                                            pid = es_datos-pid ).

        ".Consultamos informacion complementaria del mensaje
        SELECT SINGLE msgguid, pid, msg_size
          FROM sxmspmast
         WHERE msgguid EQ @es_datos-msgguid
           AND pid     EQ @es_datos-pid
          INTO @DATA(es_complemento_info).

        ".Obtenemos la fecha y hora de ejecucion
        CONVERT TIME STAMP es_datos-inittimest TIME ZONE sy-zonlo
            INTO DATE fecha_ejecucion TIME hora_ejecucion.

        ".Obtenemos la hora de finalizacion
        CONVERT TIME STAMP es_datos-exetimest TIME ZONE sy-zonlo
            INTO TIME hora_final.

        ".Obtenemos la informacion del payload
        CALL FUNCTION 'SXMB_GET_MESSAGE_PAYLOAD'
          EXPORTING
            im_msgkey      = es_sxmsmkey
          IMPORTING
            ex_msg_bytes   = payload_xs
          EXCEPTIONS
            not_authorized = 1
            no_message     = 2
            internal_error = 3
            no_payload     = 4
            OTHERS         = 5.
        IF sy-subrc EQ 0.

          ouput_longitud = xstrlen( payload_xs ).

          ".Transformacion el payload de xstring a binary
          CALL FUNCTION 'SCMS_XSTRING_TO_BINARY'
            EXPORTING
              buffer        = payload_xs
            IMPORTING
              output_length = ouput_longitud
            TABLES
              binary_tab    = ti_tabla_binaria.

          ".Transformamos el payload de binary a string
          CALL FUNCTION 'SCMS_BINARY_TO_STRING'
            EXPORTING
              input_length = ouput_longitud
*             encoding     = 'UTF-8'
            IMPORTING
              text_buffer  = payload_s
            TABLES
              binary_tab   = ti_tabla_binaria
            EXCEPTIONS
              failed       = 1
              OTHERS       = 2.
        ENDIF.

        e_es_info_mensaje = VALUE #(
            msgguid     = es_datos-msgguid
            pid         = es_datos-pid
            ricefw      = i_es_info_ad-ricefw
            msgtype     = es_datos-msgtype
            msgstate    = es_datos-msgstate
            fecha_ini   = fecha_ejecucion
            hora_ini    = hora_ejecucion
            hora_fin    = hora_final
            ob_system   = es_datos-ob_system
            ob_ns       = es_datos-ob_ns
            ob_name     = es_datos-ob_name
            ib_system   = es_datos-ib_system
            ib_ns       = es_datos-ib_ns
            ib_name     = es_datos-ib_name
            tcode       = sy-tcode
            idcola      = es_datos-queueint
            adminuser   = es_datos-adminuser
            msg_size    = es_complemento_info-msg_size
            payload     = payload_s
            json        = i_es_info_ad-json
            nropaso     = 1
            tipo_dato   = i_es_info_ad-tipo_dato
            erdat       = sy-datum
            ernam       = sy-uname
        ).

      CATCH cx_sy_itab_line_not_found.
    ENDTRY.
  ENDMETHOD.


  METHOD main.
*&----------------------------------------------------------------------*
*&     Definición de Objetos
*&----------------------------------------------------------------------*
    DATA:
      lo_objeto TYPE REF TO object.

*&----------------------------------------------------------------------*
*&     Definición de tablas internas
*&----------------------------------------------------------------------*
    DATA:
      ti_return      TYPE bapiret2_t.

*&----------------------------------------------------------------------*
*&     Definición de Variables
*&----------------------------------------------------------------------*
    DATA:
      message     TYPE bapi_msg,
      datos_proxy TYPE REF TO data.

    "{ Inicio SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito
    DATA: reproceso TYPE flag.
    "{ Fin SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito

    ".Validamos si el proceso
    IF i_datos_proxy IS NOT INITIAL.
      datos_proxy = i_datos_proxy.
    ELSE.
      ".Obtenemos la informacion del mensaje (datos enviados desde el proxy)
      me->deserializar_json(
        EXPORTING
          i_es_info_mensaje = ges_info_mensaje
        IMPORTING
          e_data            = datos_proxy
      ).
    ENDIF.

    "{ Inicio SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito
    IMPORT reproceso TO reproceso FROM MEMORY ID gc_id_memoria.
    EXPORT reproceso FROM abap_false TO MEMORY ID gc_id_memoria.
    "{ Fin SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito

    DATA prox_ejecucion TYPE i.
    ".Obtenemos el nro de ejeucion del proceso
*    select msgguid, nroejecucion, nropaso, type, erdat, erzet
    SELECT MAX( nroejecucion )
*    SELECT *
      FROM ztcxr1001_3
     WHERE ricefw  EQ @ges_info_mensaje-ricefw
       AND msgguid EQ @ges_info_mensaje-msgguid
      INTO @DATA(nro_ejecucion).
    IF sy-subrc EQ 0.
      prox_ejecucion = nro_ejecucion.
      ADD 1 TO prox_ejecucion.

      ".Consultamos la informacion de la ultima ejecucion
      SELECT msgguid, nroejecucion, nropaso, type, erdat, erzet
        FROM ztcxr1001_3
       WHERE ricefw  EQ @ges_info_mensaje-ricefw
         AND msgguid EQ @ges_info_mensaje-msgguid
         AND nroejecucion EQ @nro_ejecucion
       ORDER BY nropaso DESCENDING, type ASCENDING "El tipo es con el fin de que si hay error quede en las primeras posiciones
        INTO TABLE @DATA(ti_log_ejecucion).

    ELSE.
      prox_ejecucion = 1.
    ENDIF.

    ".Obtenemos la parametrizacion de los objetos a ejecutar
    SELECT *
      FROM ztcxr1001_2
     WHERE ricefw  EQ @ges_info_mensaje-ricefw
*       AND nropaso GE @ges_info_mensaje-nropaso
       AND veralv  EQ @space
     ORDER BY PRIMARY KEY
      INTO TABLE @DATA(ti_param_objetos).
    IF sy-subrc EQ 0.

      ".Obtenemos el nombre de la clase
      TRY.
          DATA(nombre_clase) = ti_param_objetos[ 1 ]-clase.

          ".creamos una instancia de la clase que tienen el proceso a ejecutar
          CREATE OBJECT lo_objeto TYPE (nombre_clase)
                EXPORTING
                  i_es_info_mensaje   = ges_info_mensaje.

        CATCH cx_sy_itab_line_not_found.
        CATCH cx_sy_dyn_call_param_not_found.
          RETURN.
      ENDTRY.

      ".Recorremos la parametrizacion para empezar con la ejecucion de los pasos del proceso
      LOOP AT ti_param_objetos ASSIGNING FIELD-SYMBOL(<fs_pasos>).
        CLEAR: ti_return.
        "esto es en el ELSE del ejecutar_siempre
        ".buscamos en la tabla del log si la ejecucion anterior del paso existe error
        ".si no existe error, y el verificamos si el paso es ejecutable o no
        ".

        IF <fs_pasos>-ejecuta_siempre EQ abap_true.
          TRY .
              ".Llamamos el metodo segun el paso del proceso
              CALL METHOD lo_objeto->(<fs_pasos>-metodo)
                IMPORTING
                  e_ti_return   = ti_return
                CHANGING
                  i_datos_proxy = datos_proxy.
            CATCH cx_ai_application_fault cx_sy_no_handler INTO DATA(o_cx).
              "Error al Ejecutar Paso &1 &2 &3 &4
              MESSAGE i036(zcx01) INTO message WITH <fs_pasos>-desc_paso sy-datum sy-uzeit o_cx->get_text( )."+SLS descripcion de la excepcion
              APPEND VALUE bapiret2(
              type        = gc_e
              id          = gc_zcx01
              number      = gc_036
              message     = message
              message_v1  = <fs_pasos>-desc_paso
              message_v2  = sy-datum
              message_v3  = sy-uzeit
              message_v4  = space
              ) TO ti_return.
          ENDTRY.
        ELSE.

          IF prox_ejecucion EQ 1.
            TRY .
                ".Llamamos el metodo segun el paso del proceso
                CALL METHOD lo_objeto->(<fs_pasos>-metodo)
                  IMPORTING
                    e_ti_return   = ti_return
                  CHANGING
                    i_datos_proxy = datos_proxy.
              CATCH cx_ai_application_fault cx_sy_no_handler INTO DATA(o_cx_1).
                "Error al Ejecutar Paso &1 &2 &3 &4
                MESSAGE i036(zcx01) INTO message WITH <fs_pasos>-desc_paso sy-datum sy-uzeit o_cx_1->get_text( )."+SLS descripcion de la excepcion
                APPEND VALUE bapiret2(
                type        = gc_e
                id          = gc_zcx01
                number      = gc_036
                message     = message
                message_v1  = <fs_pasos>-desc_paso
                message_v2  = sy-datum
                message_v3  = sy-uzeit
                message_v4  = space
                ) TO ti_return.
            ENDTRY.
          ELSE.
            ".Obtenemos el paso de la ultima ejecucion
            TRY.
                DATA(es_log_ejecucion) = ti_log_ejecucion[ 1 ].

                ".validamos el paso de la ultima ejecucion con la actual
                IF es_log_ejecucion-nropaso GT <fs_pasos>-nropaso.
                  CONTINUE.
                ELSE.
                  "+SLS - Ajustar logica para mensajes detenidos que requieres reprocesar pasos sin ejecucion
                  "       Se evalua que el paso a ejecutar no se haya ejecutado de forma correcta ( ejecuciones terminadas con S o W ) anteriormente{
                  ".Verificamos si el paso de la ejecucion anterior tuvo error
*                  IF es_log_ejecucion-type EQ gc_e OR es_log_ejecucion-type EQ gc_a.

                  IF NOT line_exists( ti_log_ejecucion[ nropaso = <fs_pasos>-nropaso ] ) "El paso no se ha ejecutado
                     OR
                     "El paso se ejecuto pero con errores
                     ( line_exists( ti_log_ejecucion[ nropaso = <fs_pasos>-nropaso type = gc_e ] ) OR
                       line_exists( ti_log_ejecucion[ nropaso = <fs_pasos>-nropaso type = gc_a ] ) ).
                    "}
                    TRY .
                        ".Llamamos el metodo segun el paso del proceso
                        CALL METHOD lo_objeto->(<fs_pasos>-metodo)
                          IMPORTING
                            e_ti_return   = ti_return
                          CHANGING
                            i_datos_proxy = datos_proxy.
                      CATCH cx_ai_application_fault cx_sy_no_handler cx_root INTO DATA(o_cx_2)."+SLS descripcion de la excepcion
                        "Error al Ejecutar Paso &1 &2 &3 &4
                        MESSAGE i036(zcx01) INTO message WITH <fs_pasos>-desc_paso sy-datum sy-uzeit o_cx_2->get_text( ).
                        APPEND VALUE bapiret2(
                        type        = gc_e
                        id          = gc_zcx01
                        number      = gc_036
                        message     = message
                        message_v1  = <fs_pasos>-desc_paso
                        message_v2  = sy-datum
                        message_v3  = sy-uzeit
                        message_v4  = space
                        ) TO ti_return.
                    ENDTRY.
                  ELSE.
                    CONTINUE.
                  ENDIF.
                ENDIF.

              CATCH cx_sy_itab_line_not_found cx_sy_no_handler INTO o_cx.
            ENDTRY.

          ENDIF.

        ENDIF.

        "{ Inicio SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito
        IF reproceso = abap_true.
          APPEND VALUE bapiret2( id         = 'ZCX01'
                                 number     = '092'
                                 type       = 'I'
                                 message_v1 = sy-uname ) TO ti_return.
        ENDIF.
        "{ Fin SGR 14.08.2023 | Verificar si el usuario puede reprocesar mensajes con exito

        "+SLS: Completar los mensajes que solo vienen con ID y NUMBER
        completar_mensajes( CHANGING c_ti_msg_retorno = ti_return ).

        ".Validamos si existe informacion del log
        IF line_exists( ti_return[ 1 ] ).
          me->registrar_log(
            EXPORTING
              i_msgid           = ges_info_mensaje-msgguid
              i_es_pasos        = <fs_pasos>
              i_nro_ejecucion   = prox_ejecucion
              i_ti_return       = ti_return
          ).
        ENDIF.

        "Si llega el mensaje ZCX999 es indicativo de que debe terminar en OK la ejecucion de los demas pasos
        IF line_exists( ti_return[ id = gc_zcx01 number = gc_999 ] ).
          APPEND LINES OF ti_return TO e_ti_msg_respuesta.
          "Salir del loop de la ejecucion de los pasos
          EXIT.
        ENDIF.

        ".Verificamos si el metodo permite continuar con el siguiente paso
        IF <fs_pasos>-permite_err IS INITIAL.
          ".Validamos si existe algun error
          IF line_exists( ti_return[ type = gc_e ] ) OR
             line_exists( ti_return[ type = gc_a ] ) .
            APPEND LINES OF ti_return TO e_ti_msg_respuesta.
            EXIT.
          ENDIF.
        ENDIF.

        APPEND LINES OF ti_return TO e_ti_msg_respuesta.

      ENDLOOP.

    ENDIF.

    e_datos_proxy = datos_proxy.

  ENDMETHOD.


  METHOD serializar_datos_json.
    ".Convertimos la informacion en JSON
    e_json = /ui2/cl_json=>serialize( data = REF #( i_data )
                                      compress = abap_true
                                      pretty_name = /ui2/cl_json=>pretty_mode-camel_case ).
  ENDMETHOD.


  METHOD deserializar_json.

*&----------------------------------------------------------------------*
*&     Definición de field-symbols
*&----------------------------------------------------------------------*
    FIELD-SYMBOLS: <fs_any> TYPE any.

    DATA:
          lv_type TYPE char200.

*NOTA: con el objetivo de poder referenciar tipos desde clases se amplica la longitud de la viable
    lv_type = i_es_info_mensaje-tipo_dato. "Adicion 21Mar2024 Sebastian Restrepo Villa

    ".Creamos referencia del objeto del proxy
    CREATE DATA e_data TYPE (lv_type).
    ASSIGN e_data->* TO <fs_any>.

    ".deserialize JSON string json into internal table lt_flight doing camelCase to ABAP like field name mapping
    /ui2/cl_json=>deserialize( EXPORTING json = i_es_info_mensaje-json
                                         pretty_name = /ui2/cl_json=>pretty_mode-camel_case
                                CHANGING data = <fs_any> ).

  ENDMETHOD.


  METHOD registrar_log.

    DATA: c_numero_maximo_ejecuciones TYPE i VALUE 250.
*&----------------------------------------------------------------------*
*&     Definición de tablas internas
*&----------------------------------------------------------------------*
    DATA:
      ti_ztcxr1001_3 TYPE STANDARD TABLE OF ztcxr1001_3 WITH EMPTY KEY.

*&----------------------------------------------------------------------*
*&     Definición de estructuras
*&----------------------------------------------------------------------*
    DATA:
       es_ztcxr1001_3 TYPE ztcxr1001_3.

*&----------------------------------------------------------------------*
*&     Definición de field-symbols
*&----------------------------------------------------------------------*
    FIELD-SYMBOLS:
      <fs_return> TYPE bapiret2.

*&----------------------------------------------------------------------*
*&     Definición de variables
*&----------------------------------------------------------------------*
    DATA:
      conse       TYPE i,
      status_paso TYPE icon_d.

    DATA(fecha) = sy-datum.
    DATA(hora) = sy-uzeit.

    "+SLS 01.12.2022 - Ajustar numero maximo de ejecuciones = 250{
    i_nro_ejecucion = COND #( WHEN i_nro_ejecucion > c_numero_maximo_ejecuciones THEN
                                1
                              ELSE
                               i_nro_ejecucion
                            ).
    "}

    ".Consultamos la informacion del log
    SELECT *
     FROM ztcxr1001_3
    WHERE ricefw  EQ @i_es_pasos-ricefw
      AND msgguid EQ @i_msgid
      AND nropaso EQ @i_es_pasos-nropaso
     INTO TABLE @DATA(ti_log_pasos).
    IF sy-subrc NE 0.

      ".Recorremos la informacion del log
      CLEAR conse.
      LOOP AT i_ti_return ASSIGNING <fs_return>.
        CLEAR: es_ztcxr1001_3.

        es_ztcxr1001_3 = VALUE #( mandt     = sy-mandt
                                  ricefw    = i_es_pasos-ricefw
                                  msgguid   = i_msgid
                                  nroejecucion = i_nro_ejecucion
                                  nropaso   = i_es_pasos-nropaso
                                  type      = <fs_return>-type
                                  id        = <fs_return>-id
                                  nromsg    = <fs_return>-number
                                  message   = <fs_return>-message
                                  message_v1 = <fs_return>-message_v1
                                  message_v2 = <fs_return>-message_v2
                                  message_v3 = <fs_return>-message_v3
                                  message_v4 = <fs_return>-message_v4
                                  linea     = <fs_return>-row
                                  "{ Inicio SGR 14.08.2023 | Agregar usuario a la insercion de mensajes
                                  uname     = sy-uname
                                  "{ Fin SGR 14.08.2023 | Agregar usuario a la insercion de mensajes
                                  erdat     = fecha
                                  erzet     = hora
                                   ).
        ADD 1 TO conse.
        es_ztcxr1001_3-consec = conse.
        APPEND es_ztcxr1001_3 TO ti_ztcxr1001_3.

      ENDLOOP.

    ELSE.

      DATA(lineas) = lines( ti_log_pasos ).

      ".Recorremos la informacion del log
      LOOP AT i_ti_return ASSIGNING <fs_return>.
        CLEAR: es_ztcxr1001_3.

        es_ztcxr1001_3 = VALUE #( mandt     = sy-mandt
                                  ricefw    = i_es_pasos-ricefw
                                  msgguid   = i_msgid
                                  nroejecucion = i_nro_ejecucion
                                  nropaso   = i_es_pasos-nropaso
                                  type      = <fs_return>-type
                                  id        = <fs_return>-id
                                  nromsg    = <fs_return>-number
                                  message   = <fs_return>-message
                                  message_v1 = <fs_return>-message_v1
                                  message_v2 = <fs_return>-message_v2
                                  message_v3 = <fs_return>-message_v3
                                  message_v4 = <fs_return>-message_v4
                                  linea     = <fs_return>-row
                                  "{ Inicio SGR 14.08.2023 | Agregar usuario a la insercion de mensajes
                                  uname     = sy-uname
                                  "{ Fin SGR 14.08.2023 | Agregar usuario a la insercion de mensajes
                                  erdat     = fecha
                                  erzet     = hora
                                   ).
        ADD 1 TO lineas.
        es_ztcxr1001_3-consec = lineas.

        APPEND es_ztcxr1001_3 TO ti_ztcxr1001_3.

      ENDLOOP.
    ENDIF.


    IF line_exists( ti_ztcxr1001_3[ 1 ] ).

      ".Verificamos si existen errores de tipo ERROR o ABORT
      IF line_exists( ti_ztcxr1001_3[ ricefw  = i_es_pasos-ricefw
                                      nropaso = i_es_pasos-nropaso
                                      type    = gc_e ] ) OR
         line_exists( ti_ztcxr1001_3[ ricefw  = i_es_pasos-ricefw
                                      nropaso = i_es_pasos-nropaso
                                      type    = gc_a ] ).

        status_paso = icon_led_red.

        ".Verificamos si existen errores de tipo WARNING
      ELSEIF line_exists( ti_ztcxr1001_3[ ricefw  = i_es_pasos-ricefw
                                          nropaso = i_es_pasos-nropaso
                                          type    = gc_w ] ).

        status_paso = icon_led_yellow.

        ".Verificamos si existen errores de tipo INFO o SUCCESS
      ELSE.
        status_paso = icon_led_green.
      ENDIF.

      ".Actualizamos el status y nro paso del mensaje
      UPDATE ztcxr1001_1
         SET status  = status_paso
             nropaso = i_es_pasos-nropaso
             aedat   = fecha
             aenam   = sy-uname
       WHERE msgguid = i_msgid.

      ".Actualizamos el status del paso
*      UPDATE ztcxr1001_2
*         SET status_paso = status_paso
**             aedat  = fecha
**             aezet  = hora
*       WHERE ricefw  = i_es_pasos-ricefw
*         AND nropaso = i_es_pasos-nropaso.

      TRY.
          INSERT ztcxr1001_3 FROM TABLE ti_ztcxr1001_3.
          IF gc_no_commit IS INITIAL.
            COMMIT WORK.
          ENDIF.
        CATCH cx_sy_open_sql_db.
          IF gc_no_commit IS INITIAL.
            ROLLBACK WORK.
          ENDIF.
      ENDTRY.

    ENDIF.


  ENDMETHOD.


  METHOD set_informacion_rfc.

*    ".ID Mensaje
*    c_es_info_mensaje-msgguid = get_message_id( ).

    ".Informacion adicional
    c_es_info_mensaje-ricefw      = i_es_info_ad-ricefw.
    c_es_info_mensaje-json        = i_es_info_ad-json.
    c_es_info_mensaje-nropaso     = 1.
    c_es_info_mensaje-tipo_dato   = i_es_info_ad-tipo_dato.
    IF c_es_info_mensaje-fecha_ini IS INITIAL.
      c_es_info_mensaje-fecha_ini = sy-datum.
    ENDIF.
    IF c_es_info_mensaje-hora_ini IS INITIAL.
      c_es_info_mensaje-hora_ini  = sy-uzeit.
    ENDIF.
    c_es_info_mensaje-erdat       = sy-datum.
*    c_es_info_mensaje-ernam       = sy-uzeit.
    c_es_info_mensaje-ernam       = sy-uname.
    IF c_es_info_mensaje-ib_name IS INITIAL.
      c_es_info_mensaje-ib_name = sy-cprog.
    ENDIF.

*    ".Registramos la informacion del mensaje
*    me->set_informacion_mensaje( i_info_mensaje = c_es_info_mensaje ).

  ENDMETHOD.


  METHOD get_rango_idmensaje.

    ".Obtenemos el ID mensaje
    CALL FUNCTION 'NUMBER_GET_NEXT'
      EXPORTING
        nr_range_nr             = '01'
        object                  = 'ZCXR1001_1'
*       quantity                = '1'
*       subobject               = space
*       toyear                  = '0000'
*       ignore_buffer           = space
      IMPORTING
        number                  = c_idmensaje
*       quantity                =
*       returncode              =
      EXCEPTIONS
        interval_not_found      = 1
        number_range_not_intern = 2
        object_not_found        = 3
        quantity_is_0           = 4
        quantity_is_not_1       = 5
        interval_overflow       = 6
        buffer_overflow         = 7
        OTHERS                  = 8.
  ENDMETHOD.


  METHOD guardar_mensaje.
*&----------------------------------------------------------------------*
*& Definicion de Types
*&----------------------------------------------------------------------*
    TYPES:
      tp_ti_ztcxr1001_1 TYPE STANDARD TABLE OF ztcxr1001_1 WITH EMPTY KEY.

    DATA(ti_info_mensaje) = VALUE tp_ti_ztcxr1001_1( ( i_info_mensaje ) ).
    TRY.
        INSERT ztcxr1001_1 FROM TABLE ti_info_mensaje.
      CATCH cx_sy_open_sql_db.
    ENDTRY.

    IF gc_no_commit IS INITIAL.
      COMMIT WORK.
    ENDIF.

  ENDMETHOD.


  METHOD save_json.

    DATA:
          es_ztcxr1001_4 TYPE ztcxr1001_4.

    ".Obtenemos el JSON
    DATA(json) = serializar_datos_json( REF #( i_datos_proxy )  ).

    CLEAR es_ztcxr1001_4.
    es_ztcxr1001_4-ricefw   = ges_info_mensaje-ricefw.
    es_ztcxr1001_4-msgguid  = ges_info_mensaje-msgguid.
    es_ztcxr1001_4-pid      = ges_info_mensaje-pid.
    es_ztcxr1001_4-nropaso  =
    es_ztcxr1001_4-json     = json.
    es_ztcxr1001_4-erdat    = sy-datum.
    es_ztcxr1001_4-erzet    = sy-uzeit.

    MODIFY ztcxr1001_4 FROM es_ztcxr1001_4.

  ENDMETHOD.


  METHOD get_outbound_message_id.

    DATA: o_protocol_messageid    TYPE REF TO if_wsprotocol_message_id.

  ENDMETHOD.


  METHOD retener_mensaje.

    "{ Inicio SGR 18.07.2023 | Verificar que el numero de paso no sea inicial
*    SELECT *
*     FROM ztcxr1001_3
*    WHERE ricefw  EQ @i_ricefw
*      AND msgguid EQ @i_msgguid
*      AND nropaso EQ @i_paso_detener_proceso
*     INTO TABLE @DATA(ti_log_pasos).
*
*    "Solo se activa la validación para el primer registro en el monitor
*    IF lines( ti_log_pasos ) <= 1.
*      MESSAGE i999(zcx01) INTO DATA(message).
*      APPEND VALUE bapiret2( type = gc_id_mensaje_encolado-tipo
*                             id   = gc_id_mensaje_encolado-id
*                             number = gc_id_mensaje_encolado-numero_msj message = message message_v1 = sy-datum message_v2  = sy-uzeit ) TO c_ti_return.
*    ENDIF.

    "Si el paso es vacio, no debe realizar ninguna verificacion
    CHECK i_paso_detener_proceso IS NOT INITIAL.

    "Obtener el paso anterior para verificar que se haya ejecutado
    DATA(paso_anterior) = i_paso_detener_proceso - 1.

    "Si el paso a detener el proceso es inicial, es porque se debe de frenar en el primer paso y no debe hacere la consulta
    IF paso_anterior IS NOT INITIAL.

      "Si el paso no es inicial, debe de realizar la verificacion de que se haya ejecutado el paso anterior
      SELECT SINGLE *
       FROM ztcxr1001_3
      WHERE ricefw  EQ @i_ricefw
        AND msgguid EQ @i_msgguid
        AND nropaso EQ @paso_anterior
       INTO @DATA(es_log_paso_anterior).

      "Si no encuentra datos, es porque el paso anterior no se ha ejecutado, por ende, poner la bandera como no ejecutado
      IF sy-subrc <> 0.
        DATA(no_retener_mensaje) = abap_true.
      ENDIF.
    ENDIF.

    "Obtener los mensajes del paso a parar, para verificar si ya fue ejecutado
    SELECT SINGLE *
     FROM ztcxr1001_3
    WHERE ricefw  EQ @i_ricefw
      AND msgguid EQ @i_msgguid
      AND nropaso EQ @i_paso_detener_proceso
     INTO @DATA(es_log_paso).

    "Si se encontraron datos es porque el paso ya fue ejecutado y ya no deberia de parar
    IF sy-subrc = 0.
      no_retener_mensaje = abap_true.
    ENDIF.

    "Si el paso anterior se ejecuto y el paso a frenarlo no se ha ejecutado, sacar el mensaje y frenar la integracion
    IF no_retener_mensaje = abap_false.
      MESSAGE i999(zcx01) INTO DATA(message).
      APPEND VALUE bapiret2( type   = gc_id_mensaje_encolado-tipo
                             id     = gc_id_mensaje_encolado-id
                             number = gc_id_mensaje_encolado-numero_msj message = message message_v1 = sy-datum message_v2  = sy-uzeit ) TO c_ti_return.
    ENDIF.
    "{ Fin SGR 18.07.2023

  ENDMETHOD.


  METHOD completar_mensajes.

    LOOP AT c_ti_msg_retorno ASSIGNING FIELD-SYMBOL(<fs_es_msg>)
                             WHERE message IS INITIAL AND
                                 ( type IS NOT INITIAL AND number IS NOT INITIAL AND id IS NOT INITIAL ).

      MESSAGE  ID <fs_es_msg>-id TYPE <fs_es_msg>-type
      NUMBER <fs_es_msg>-number
      WITH <fs_es_msg>-message_v1 <fs_es_msg>-message_v2 <fs_es_msg>-message_v3 <fs_es_msg>-message_v4
      INTO <fs_es_msg>-message.
    ENDLOOP.

  ENDMETHOD.


  METHOD registrar_respuesta.

    DATA: cuerpo_respuesta TYPE string.

    IF i_info_mensaje IS INITIAL.
      i_info_mensaje = ges_info_mensaje.
    ENDIF.

    CHECK i_info_mensaje-msgguid IS NOT INITIAL AND
          i_info_mensaje-ricefw IS NOT INITIAL.

    "{ Inicio SGR 23.05.2024 | Agregar logica para validar si es XML o JSON
*    cuerpo_respuesta = /ui2/cl_json=>serialize( data        = i_datos_respuesta
*                                                compress    = abap_true
*                                                pretty_name = /ui2/cl_json=>pretty_mode-camel_case ).
    ASSIGN i_datos_respuesta->* TO FIELD-SYMBOL(<fs_datos>).
    TRY.
        IF to_upper( <fs_datos>(10) ) CS 'XML'.
          cuerpo_respuesta = <fs_datos>.
        ELSE.
          cuerpo_respuesta = /ui2/cl_json=>serialize( data        = i_datos_respuesta
                                                      compress    = abap_true
                                                      pretty_name = /ui2/cl_json=>pretty_mode-camel_case ).
        ENDIF.
      CATCH cx_root.
        cuerpo_respuesta = /ui2/cl_json=>serialize( data        = i_datos_respuesta
                                                    compress    = abap_true
                                                    pretty_name = /ui2/cl_json=>pretty_mode-camel_case ).
    ENDTRY.
    "{ Fin SGR 23.05.2024

    UPDATE ztcxr1001_1
    SET response = cuerpo_respuesta
    WHERE msgguid = i_info_mensaje-msgguid
    AND ricefw = i_info_mensaje-ricefw.

    IF sy-subrc IS INITIAL.

      APPEND VALUE bapiret2( id = 'ZCX01' type = 'S' number = '094' message_v1 = i_info_mensaje-msgguid )
      TO c_ti_return.

      IF i_ejecutar_commit = abap_true.
        COMMIT WORK AND WAIT.
      ENDIF.

    ELSE.
      APPEND VALUE bapiret2( id = 'ZCX01' type = 'W' number = '095' message_v1 = i_info_mensaje-msgguid )
      TO c_ti_return.
    ENDIF.

  ENDMETHOD.


  METHOD registrar_xml.

    CHECK i_info_mensaje-msgguid IS NOT INITIAL AND
          i_info_mensaje-ricefw  IS NOT INITIAL.

    IF i_info_mensaje-payload IS INITIAL.
      SELECT SINGLE payload
      FROM ztcxr1001_1
      WHERE msgguid = @i_info_mensaje-msgguid
        AND ricefw  = @i_info_mensaje-ricefw
        INTO @i_info_mensaje-payload.
    ENDIF.

    IF i_info_mensaje-payload IS INITIAL.
      UPDATE ztcxr1001_1
      SET payload = @i_xml
      WHERE msgguid = @i_info_mensaje-msgguid
        AND ricefw  = @i_info_mensaje-ricefw.

      IF sy-subrc IS INITIAL.
        APPEND VALUE bapiret2( id = gc_zcx01 type = gc_s number = '098' message_v1 = i_info_mensaje-msgguid ) TO c_ti_return.
        IF i_ejecutar_commit = abap_true.
          COMMIT WORK AND WAIT.
        ENDIF.
      ELSE.
        APPEND VALUE bapiret2( id = gc_zcx01 type = gc_w number = '099' message_v1 = i_info_mensaje-msgguid ) TO c_ti_return.
      ENDIF.
    ELSE.
      APPEND VALUE bapiret2( id = gc_zcx01 type = gc_w number = '100' message_v1 = i_info_mensaje-msgguid ) TO c_ti_return.
    ENDIF.

  ENDMETHOD.

  METHOD registrar_data_gcp.

    CHECK i_info_mensaje-msgguid IS NOT INITIAL AND
          i_info_mensaje-ricefw  IS NOT INITIAL.

    IF i_info_mensaje-topico_gcp IS INITIAL OR
       i_info_mensaje-id_msg_gcp IS INITIAL.
      SELECT SINGLE topico_gcp, id_msg_gcp
      FROM ztcxr1001_1
      WHERE msgguid = @i_info_mensaje-msgguid
        AND ricefw  = @i_info_mensaje-ricefw
        INTO ( @i_info_mensaje-topico_gcp, @i_info_mensaje-id_msg_gcp ).
    ENDIF.

    IF i_info_mensaje-id_msg_gcp IS INITIAL.
      UPDATE ztcxr1001_1
      SET topico_gcp = @i_topico_gcp,
          id_msg_gcp = @i_id_msg_gcp
      WHERE msgguid = @i_info_mensaje-msgguid
        AND ricefw  = @i_info_mensaje-ricefw.

      IF sy-subrc IS INITIAL.
        APPEND VALUE bapiret2( id = gc_zcx01 type = gc_s number = '112' message_v1 = i_info_mensaje-msgguid ) TO c_ti_return.
        IF i_ejecutar_commit = abap_true.
          COMMIT WORK AND WAIT.
        ENDIF.
      ELSE.
        APPEND VALUE bapiret2( id = gc_zcx01 type = gc_w number = '113' message_v1 = i_info_mensaje-msgguid ) TO c_ti_return.
      ENDIF.
    ELSE.
      APPEND VALUE bapiret2( id = gc_zcx01 type = gc_w number = '111' message_v1 = i_info_mensaje-msgguid ) TO c_ti_return.
    ENDIF.

  ENDMETHOD.

ENDCLASS.
CLASS zclcxr1002_consumo_google DEFINITION
  PUBLIC
  CREATE PUBLIC .

  PUBLIC SECTION.

    CONSTANTS: gc_msgid         TYPE syst_msgid VALUE 'ZCX01',
               gc_http_response TYPE symsgv     VALUE 'HTTP Response:'.

    TYPES: gtp_ti_mensaje TYPE TABLE OF string WITH EMPTY KEY.

    METHODS:
      constructor
        IMPORTING
          iv_client_key TYPE /goog/keyname
        RAISING
          cx_t100_msg.

  PROTECTED SECTION.

    METHODS:
      publish_topics
        IMPORTING
          iv_topic_name        TYPE string
          iv_msg               TYPE string
          iv_msg_id            TYPE guid_32
          i_r_attributes       TYPE REF TO data
        RETURNING
          VALUE(r_ti_bapiret2) TYPE bapiret2_t
        RAISING
          cx_t100_msg,
      pull_subscriptions
        IMPORTING
          iv_subscription     TYPE string
          iv_max_messages     TYPE int4 DEFAULT 1
        EXPORTING
          e_ti_bapiret2       TYPE bapiret2_t
        RETURNING
          VALUE(r_ti_mensaje) TYPE gtp_ti_mensaje
        RAISING
          cx_t100_msg.

  PRIVATE SECTION.

    DATA: go_client TYPE REF TO /goog/cl_pubsub_v1.

ENDCLASS.



CLASS ZCLCXR1002_CONSUMO_GOOGLE IMPLEMENTATION.


  METHOD constructor.

    TRY.
        CREATE OBJECT go_client
          EXPORTING
            iv_key_name = iv_client_key.

      CATCH /goog/cx_sdk INTO DATA(lo_exception).
        RAISE EXCEPTION TYPE cx_t100_msg
          EXPORTING
            t100_msgid = gc_msgid "'ZCX01'
            t100_msgno = '000'
            t100_msgv1 = lo_exception->get_text( ).
    ENDTRY.

  ENDMETHOD.


  METHOD pull_subscriptions.

    DATA: es_input_ack          TYPE /goog/cl_pubsub_v1=>ty_001,
          es_subscription_info  TYPE /goog/cl_pubsub_v1=>ty_038,
          es_list_subscriptions TYPE /goog/cl_pubsub_v1=>ty_014.

    DATA:
          v_cnt_consultas TYPE int4.



    " 1. Configurar timeout HTTP
*        go_client->set_http_timeout( 60 ).

    " 2. Configurar parámetros de pull (en cada solicitud)
    DATA(es_input) = VALUE /goog/cl_pubsub_v1=>ty_026( max_messages       = iv_max_messages ).   " Solicitar hasta 50 mensajes
*                                                           return_immediately = abap_false      ). " Esperar mensajes disponibles

*    " 3. Si la clase soporta control de flujo avanzado (depende de versión)
*    IF go_client->has_flow_control( ) = abap_true.
*      DATA(ls_flow_control) = VALUE /goog/cl_pubsub_v1=>ty_flow_control(
*        max_outstanding_messages  = 100       " Máx. mensajes en procesamiento
*        max_outstanding_bytes     = 10485760  " 10MB en bytes
*      ).
*      go_client->set_flow_control( ls_flow_control ).
*    ENDIF.

*NOTA: hacer esto la cantidad de veces necesaria hasta que la cola no tenga registros, esto
*      debido a que cuando se consume la cola, no llega siempre toda la informacion de la misma
    WHILE v_cnt_consultas LT iv_max_messages.
      v_cnt_consultas = v_cnt_consultas + 1.

      TRY.
          CALL METHOD go_client->pull_subscriptions
            EXPORTING
              iv_p_projects_id      = CONV #( go_client->gv_project_id ) "lv_p_projects_id
              iv_p_subscriptions_id = iv_subscription
              is_input              = es_input "-SLS 24072025 - No enviar cantidad de mensajes
            IMPORTING
              es_output             = DATA(es_output_msg)
              ev_ret_code           = DATA(return_code)
              ev_err_text           = DATA(error_text)
              es_err_resp           = DATA(es_error_response).


        CATCH /goog/cx_sdk INTO DATA(lo_exception).
          RAISE EXCEPTION TYPE cx_t100_msg
            EXPORTING
              t100_msgid = gc_msgid "'ZCX01'
              t100_msgno = '000'
              t100_msgv1 = lo_exception->get_text( ).

          v_cnt_consultas = iv_max_messages.
          EXIT. "Salir del While
      ENDTRY.

      IF go_client->is_success( return_code ) = abap_true.
        IF es_output_msg-received_messages IS NOT INITIAL.
          "Messages published to Pub/Sub should be base-64 encoded, hence in order to get the exact message, we need to decode the data field.
          "However, attributes published to Pub/Sub should be accessible without any additional logic.
          LOOP AT es_output_msg-received_messages ASSIGNING FIELD-SYMBOL(<fs_es_message>).
            DATA(msg) = cl_http_utility=>decode_base64( encoded = <fs_es_message>-message-data ).
            APPEND msg TO r_ti_mensaje.
            APPEND <fs_es_message>-ack_id TO es_input_ack-ack_ids.
          ENDLOOP.

          MESSAGE i107(zcx01) INTO DATA(mensaje).

          APPEND VALUE #( id         = gc_msgid "'ZCX01'
                          number     = '107'
                          type       = 'I'
                          message    = mensaje ) TO e_ti_bapiret2.

          TRY.
              "Call API method: pubsub.projects.subscriptions.acknowledge
              "Acknowledge the messages so it is not pulled again.
              CALL METHOD go_client->acknowledge_subscriptions
                EXPORTING
                  iv_p_projects_id      = CONV #( go_client->gv_project_id ) "lv_p_projects_id
                  iv_p_subscriptions_id = iv_subscription
                  is_input              = es_input_ack
                IMPORTING
                  es_output             = DATA(es_output_ack)
                  ev_ret_code           = return_code
                  ev_err_text           = error_text
                  es_err_resp           = es_error_response.
            CATCH /goog/cx_sdk INTO lo_exception.
              RAISE EXCEPTION TYPE cx_t100_msg
                EXPORTING
                  t100_msgid = gc_msgid "'ZCX01'
                  t100_msgno = '000'
                  t100_msgv1 = lo_exception->get_text( ).

              v_cnt_consultas = iv_max_messages.
              EXIT. "Salir del While
          ENDTRY.

          IF go_client->is_success( return_code ).
            MESSAGE i000(zcx01) WITH gc_http_response condense( val = |{ return_code }| ) error_text INTO mensaje.
            APPEND VALUE #( id         = gc_msgid "'ZCX01'
                            number     = '000'
                            type       = 'S'
                            message    = mensaje
                            message_v1 = gc_http_response "'HTTP Response:'
                            message_v2 = condense( val = |{ return_code }| ) "return_code
                            message_v3 = error_text ) TO e_ti_bapiret2.

          ELSE.
            MESSAGE i000(zcx01) WITH gc_http_response condense( val = |{ return_code }| ) error_text INTO mensaje.
            APPEND VALUE #( id         = gc_msgid "'ZCX01'
                            number     = '000'
                            type       = 'E'
                            message    = mensaje
                            message_v1 = gc_http_response "'HTTP Response:'
                            message_v2 = condense( val = |{ return_code }| )
                            message_v3 = error_text ) TO e_ti_bapiret2.
          ENDIF.
        ELSE.
          MESSAGE i106(zcx01) INTO mensaje.
          APPEND VALUE #( id         = gc_msgid "'ZCX01'
                          number     = '106'
                          type       = 'S'
                          message    = mensaje ) TO e_ti_bapiret2.

          v_cnt_consultas = iv_max_messages.
          EXIT. "Salir del While
        ENDIF.

      ELSE.
        MESSAGE i000(zcx01) WITH gc_http_response condense( val = |{ return_code }| ) error_text INTO mensaje.
        APPEND VALUE #( id         = gc_msgid "'ZCX01'
                        number     = '000'
                        type       = 'E'
                        message_v1 = gc_http_response "'HTTP Response:'
                        message_v2 = condense( val = |{ return_code }| )
                        message_v3 = error_text
                        message    = mensaje ) TO e_ti_bapiret2.
      ENDIF.
    ENDWHILE.

  ENDMETHOD.


  METHOD publish_topics.

    TRY.

        DATA(es_input) = VALUE /goog/cl_pubsub_v1=>ty_023( messages = VALUE #( ( attributes = i_r_attributes
                                                                                 message_id = iv_msg_id
                                                                                 data       = cl_http_utility=>encode_base64( unencoded = iv_msg ) ) ) ).

        APPEND VALUE #( id         = gc_msgid "'ZCX01'
                        number     = '105'
                        type       = 'I'
                        message_v1 = iv_msg_id ) TO r_ti_bapiret2.

        CALL METHOD go_client->publish_topics
          EXPORTING
            iv_p_projects_id = CONV #( go_client->gv_project_id )
            iv_p_topics_id   = iv_topic_name
            is_input         = es_input
          IMPORTING
            es_output        = DATA(es_output_msg)
            ev_ret_code      = DATA(return_code)
            ev_err_text      = DATA(error_text)
            es_err_resp      = DATA(es_error_response).

      CATCH /goog/cx_sdk INTO DATA(lo_exception).
        RAISE EXCEPTION TYPE cx_t100_msg
          EXPORTING
            t100_msgid = gc_msgid "'ZCX01'
            t100_msgno = '000'
            t100_msgv1 = lo_exception->get_text( ).
    ENDTRY.
    IF go_client->is_success( return_code ) = abap_true.
      MESSAGE i104(zcx01) INTO DATA(mensaje).
      APPEND VALUE #( id         = gc_msgid "'ZCX01'
                      number     = '104'
                      type       = 'S'
                      message    = mensaje ) TO r_ti_bapiret2.
    ELSE.
      MESSAGE i000(zcx01) WITH gc_http_response condense( val = |{ return_code }| ) error_text INTO mensaje.
      APPEND VALUE #( id         = gc_msgid "'ZCX01'
                      number     = '000'
                      type       = 'E'
                      message_v1 = gc_http_response "'HTTP Response:'
                      message_v2 = return_code
                      message_v3 = error_text
                      message    = mensaje  ) TO r_ti_bapiret2.
    ENDIF.
  ENDMETHOD.
ENDCLASS.
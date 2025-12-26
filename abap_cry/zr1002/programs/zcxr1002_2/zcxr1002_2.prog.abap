*&---------------------------------------------------------------------*
*& Report zcxr1002_2
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT zcxr1002_2.

SELECTION-SCREEN BEGIN OF BLOCK b1 WITH FRAME TITLE TEXT-001.
  SELECTION-SCREEN SKIP.
  PARAMETERS: p_user TYPE sy-uname DEFAULT sy-uname.
SELECTION-SCREEN END OF BLOCK b1.

START-OF-SELECTION.
  TYPES: BEGIN OF gtp_es_mensaje,
           _type              TYPE string,
           _code              TYPE string,
           _description       TYPE string,
           _short_description TYPE string,
           _error_code        TYPE string,
           _error_field       TYPE string,
           _resource_id       TYPE string,
           _quick_info        TYPE string,
           _business_keys     TYPE string,
*        _Reason_Code_Override_Type_Code": null,
           _resource_type     TYPE string,
           _component_name    TYPE string,
         END OF gtp_es_mensaje,
         gtp_ti_mensajes TYPE STANDARD TABLE OF gtp_es_mensaje WITH EMPTY KEY,
         BEGIN OF gtp_es_mensaje2,
           _message TYPE gtp_ti_mensajes,
           _size    TYPE string,
         END OF gtp_es_mensaje2,
         BEGIN OF gtp_es_data,
           _messages TYPE gtp_es_mensaje2,
           topic     TYPE string,
           _msg_id   TYPE string,
         END OF gtp_es_data,
         BEGIN OF gtp_es_error,
           error TYPE gtp_ti_mensajes,
         END OF gtp_es_error,
         BEGIN OF gtp_es_errores,
           _messages              TYPE string,
           _reference_key9        TYPE string,
           _user                  TYPE string,
           _exception_class       TYPE string,
           _reference_key8        TYPE string,
           _created_timestamp     TYPE string,
           _reference_key7        TYPE string,
           _reference_key6        TYPE string,
           _reference_key5        TYPE string,
           _reference_key4        TYPE string,
           _unique_job_id         TYPE string,
           _process               TYPE string,
           _repost_count          TYPE string,
           _message_business_unit TYPE string,
           _updated_by            TYPE string,
           _broker_cluster_name   TYPE string,
           _consumer_component    TYPE string,
           _failed_message_id     TYPE string,
           _purge_date            TYPE string,
           _queue_name            TYPE string,
           _message_type          TYPE string,
           _span_id               TYPE string,
           _message_location_id   TYPE string,
           _status                TYPE string,
           _repost_error_count    TYPE string,
           _updated_timestamp     TYPE string,
           _created_by            TYPE string,
           _exception_messages    TYPE string,
           _exception_trace       TYPE string,
           _next_runtime          TYPE string,
           _exception_reason      TYPE string,
           _business_key          TYPE string,
           _org_id                TYPE string,
           _header                TYPE string,
           _reference_key10       TYPE string,
           _reference_key3        TYPE string,
           _context_id            TYPE string,
           _trace_id              TYPE string,
           _reference_key2        TYPE string,
           _payload               TYPE string,
           _reference_key1        TYPE string,
           _p_k                   TYPE string,
           _error_code            TYPE string,
           _error_message         TYPE string,
           _compression_type      TYPE string,
           _producer_component    TYPE string,
           _unique_identifier     TYPE string,
           _messageid             TYPE string,
         END OF gtp_es_errores.

  DATA: gti_data                    TYPE STANDARD TABLE OF gtp_es_errores, "gtp_es_data,
        gti_bapiret2                TYPE bapiret2_t,
        ges_data                    TYPE gtp_es_data,
        ges_error                   TYPE gtp_es_error,
        gti_ztcxr1001_3             TYPE STANDARD TABLE OF ztcxr1001_3 WITH DEFAULT KEY,
        ges_ztcxr1001_3             TYPE ztcxr1001_3,
        g_nombre_lista_distribucion TYPE soobjinfi1-obj_name,
        gti_lista_distribucion      TYPE TABLE OF sodlienti1.

  TRY.
      NEW zclcxr1002_consumo_mawm( )->traer_subscripcion(
        EXPORTING
          i_subscripcion = 'CRYS_OB_XNT_HST_Error_GCPQ'
          i_cnt_mensajes = 50
        IMPORTING
          e_ti_bapiret2  = gti_bapiret2
        CHANGING
          e_ti_data      = gti_data ).

      IF gti_data IS NOT INITIAL.

        SELECT *
        FROM ztcxr1002_4
        INTO TABLE @DATA(ti_conversion).

        g_nombre_lista_distribucion = 'Z_ERROR_MAWM'.

        "Lee la lista de dsitribución
        CALL FUNCTION 'SO_DLI_READ_API1'
          EXPORTING
            dli_name                   = g_nombre_lista_distribucion
*           dli_id                     = space
            shared_dli                 = abap_true
*       IMPORTING
*           dli_data                   =
          TABLES
            dli_entries                = gti_lista_distribucion
          EXCEPTIONS
            dli_not_exist              = 1
            operation_no_authorization = 2
            parameter_error            = 3
            x_error                    = 4
            OTHERS                     = 5.

        IF sy-subrc <> 0.
          APPEND VALUE #( member_adr = 'salopera@crystal.com.co' ) TO gti_lista_distribucion.
          APPEND VALUE bapiret2( type       = 'E'
                                 id         = 'ZCX01'
                                 number     = '000'
                                 message    = |No se encontro correos en la lista de distribución { g_nombre_lista_distribucion }|
                                 message_v1 = |No se encontro correos en la lista de distribución|
                                 message_v2 = g_nombre_lista_distribucion ) TO gti_bapiret2.
        ENDIF.

        LOOP AT gti_data ASSIGNING FIELD-SYMBOL(<fs_es_data2>).

          /ui2/cl_json=>deserialize( EXPORTING
                                            json        = <fs_es_data2>-_payload
                                            pretty_name = /ui2/cl_json=>pretty_mode-camel_case
                                         CHANGING
                                            data        = ges_data ).

          SELECT SINGLE *
           FROM ztcxr1001_1
           WHERE topico_gcp = @ges_data-topic
             AND id_msg_gcp = @ges_data-_msg_id
           INTO @DATA(es_monitor).

          IF es_monitor IS INITIAL.
            APPEND VALUE bapiret2( type       = 'E'
                                   id         = 'ZCX01'
                                   number     = '000'
                                   message    = |No se encontro mensaje relacionado para ID { ges_data-_msg_id } tópico { ges_data-topic }|
                                   message_v1 = |No se encontro mensaje relacionado para ID|
                                   message_v2 = ges_data-_msg_id
                                   message_v3 = 'tópico'
                                   message_v4 = ges_data-topic ) TO gti_bapiret2.
          ELSE.
            SELECT MAX( nroejecucion )
            FROM ztcxr1001_3
            WHERE msgguid = @es_monitor-msgguid
              AND ricefw  = @es_monitor-ricefw
            INTO @DATA(max_mensaje).
          ENDIF.


          DELETE ges_data-_messages-_message WHERE _code <> 'ExceptionMessages'.
          DATA(ti_correo) = VALUE soli_tab( ( line = '<!DOCTYPE html>' )
                                            ( line = '<html>' )
                                            ( line = '<head>' )
                                            ( line = '<style>' )
                                            ( line = 'table {' )
                                            ( line = '  font-family: arial, sans-serif;' )
                                            ( line = '  border-collapse: collapse;' )
                                            ( line = '  ' )
                                            ( line = '}' )
                                            ( line = '' )
                                            ( line = 'td, th {' )
                                            ( line = '  border: 1px solid #dddddd;' )
                                            ( line = '  text-align: left;' )
                                            ( line = '  padding: 8px;' )
                                            ( line = '}' )
                                            ( line = '' )
                                            ( line = 'tr:nth-child(even) {' )
                                            ( line = '  background-color: #dddddd;' )
                                            ( line = '}' )
                                            ( line = '</style>' )
                                            ( line = '</head>' )
                                            ( line = '<body>' )
                                            ( line = '' )
**                                        ( line = '<h1>Asunto: Error Manhatthan Active [Topico homologado]</h1>' )
                                            ( line = '<p>Se han generado los siguentes errores en la integración con Manhatthan Active:</p>' )
                                            ( line = '' )
                                            ( line = '<table style= "width: 100%;">' )
                                            ( line = '  <tr>' )
                                            ( line = '    <th>Type</th>' )
                                            ( line = '    <th>Code</th>' )
                                            ( line = '    <th>Description</th>' )
                                            ( line = '    <th>ShortDescription</th>' )
                                            ( line = '    <th>ErrorField</th>' )
                                            ( line = '    <th>ComponentName</th>' )
                                            ( line = '  </tr>' ) ).
          LOOP AT ges_data-_messages-_message ASSIGNING FIELD-SYMBOL(<fs_es_mensaje>).
            <fs_es_mensaje>-_description = |{ '{' } "error": { <fs_es_mensaje>-_description } { '}' }|.
            /ui2/cl_json=>deserialize( EXPORTING
                                          json        = <fs_es_mensaje>-_description
                                          pretty_name = /ui2/cl_json=>pretty_mode-camel_case
                                       CHANGING
                                          data        = ges_error ).
            max_mensaje = max_mensaje + 1.
            DATA(indice) = 1.
            LOOP AT ges_error-error ASSIGNING FIELD-SYMBOL(<fs_es_error>).
              APPEND LINES OF VALUE soli_tab( ( line = '  <tr>' )
                                              ( line = |    <td>{ <fs_es_error>-_type }</td>|  )
                                              ( line = |    <td>{ <fs_es_error>-_code }</td>| )
                                              ( line = |    <td>{ <fs_es_error>-_description }</td>| )
                                              ( line = |    <td>{ <fs_es_error>-_short_description }</td>| )
                                              ( line = |    <td>{ <fs_es_error>-_error_field }</td>| )
                                              ( line = |    <td>{ <fs_es_error>-_component_name }</td>| )
                                              ( line = '  </tr>' ) ) TO ti_correo.

              IF es_monitor IS NOT INITIAL.

                ges_ztcxr1001_3 = VALUE #( ricefw       = es_monitor-ricefw
                                           msgguid      = es_monitor-msgguid
                                           nroejecucion = max_mensaje
                                           nropaso      = '1'
                                           consec       = indice
                                           type         = 'E'
                                           id           = 'ZCX01'
                                           nromsg       = '000'
                                           message      = <fs_es_error>-_description
                                           message_v1   = ''
                                           message_v2   = ''
                                           message_v3   = ''
                                           message_v4   = ''
                                           linea        = ''
                                           uname        = sy-uname
                                           erdat        = sy-datum
                                           erzet        = sy-uzeit ).
                indice = indice + 1.
                DO 4 TIMES.
                  IF strlen( <fs_es_error>-_description ) > 50.
                    CASE sy-index.
                      WHEN 1.
                        ges_ztcxr1001_3-message_v1 = <fs_es_error>-_description(50).
                      WHEN 2.
                        ges_ztcxr1001_3-message_v2 = <fs_es_error>-_description(50).
                      WHEN 3.
                        ges_ztcxr1001_3-message_v3 = <fs_es_error>-_description(50).
                      WHEN 4.
                        ges_ztcxr1001_3-message_v4 = <fs_es_error>-_description(50).
                    ENDCASE.
                    <fs_es_error>-_description = <fs_es_error>-_description+50.
                  ELSE.
                    CASE sy-index.
                      WHEN 1.
                        ges_ztcxr1001_3-message_v1 = <fs_es_error>-_description.
                      WHEN 2.
                        ges_ztcxr1001_3-message_v2 = <fs_es_error>-_description.
                      WHEN 3.
                        ges_ztcxr1001_3-message_v3 = <fs_es_error>-_description.
                      WHEN 4.
                        ges_ztcxr1001_3-message_v4 = <fs_es_error>-_description.
                    ENDCASE.
                    EXIT.
                  ENDIF.
                ENDDO.

                APPEND ges_ztcxr1001_3 TO gti_ztcxr1001_3.

                CLEAR: ges_ztcxr1001_3.
              ENDIF.

            ENDLOOP.

          ENDLOOP.

          APPEND LINES OF VALUE soli_tab( ( line = '</table>' )
                                          ( line = '' )
                                          ( line = |<p>Es posible hacer la revisión en Manhattan con el TraceId: { <fs_es_data2>-_trace_id }</p>| ) ) TO ti_correo.
          MESSAGE i114(zcx01) WITH <fs_es_data2>-_trace_id INTO DATA(mensaje).
          APPEND VALUE #( ricefw       = es_monitor-ricefw
                          msgguid      = es_monitor-msgguid
                          nroejecucion = max_mensaje
                          nropaso      = '1'
                          consec       = indice
                          type         = 'I'
                          id           = 'ZCX01'
                          nromsg       = '114'
                          message      = mensaje
                          message_v1   = <fs_es_data2>-_trace_id
                          linea        = ''
                          uname        = sy-uname
                          erdat        = sy-datum
                          erzet        = sy-uzeit ) TO gti_ztcxr1001_3.

          IF es_monitor IS NOT INITIAL.
            INSERT ztcxr1001_3 FROM TABLE gti_ztcxr1001_3.

            UPDATE ztcxr1001_1
              SET status = '@5C@'
              WHERE msgguid = @es_monitor-msgguid
                AND pid     = @es_monitor-pid
                AND ricefw  = @es_monitor-ricefw.

            APPEND LINES OF VALUE soli_tab( ( line = '<p>Favor revisar en el monitor de integraciones con:</p>' )
                                            ( line = '<table>' )
                                            ( line = '  <tr>' )
                                            ( line = '    <td>Ricefw:</td>' )
                                            ( line = |    <td>{ es_monitor-ricefw }</td>| )
                                            ( line = '  </tr>' )
                                            ( line = '  <tr>' )
                                            ( line = '    <td>Fecha envío:</td>' )
                                            ( line = |    <td>{ es_monitor-fecha_ini+6 }.{ es_monitor-fecha_ini+4(2) }.{ es_monitor-fecha_ini(4) }</td>| )
                                            ( line = '  </tr>' )
                                            ( line = '  <tr>' )
                                            ( line = '    <td>Id MSG:</td>' )
                                            ( line = |    <td>{ es_monitor-msgguid }</td>| )
                                            ( line = '  </tr>' )
                                            ( line = '</table>' )
                                            ( line = '' ) ) TO ti_correo.
          ENDIF.

          APPEND LINES OF VALUE soli_tab( ( line = '<p>Saludos.</p>' )
                                          ( line = '' )
                                          ( line = '</body>' )
                                          ( line = '</html>' ) ) TO ti_correo.

          IF gti_lista_distribucion IS NOT INITIAL.
            zclcxr1002_util=>enviar_correo(
                EXPORTING
                    i_ti_destinatarios       = VALUE #( FOR wa IN gti_lista_distribucion ( smtp_addr = wa-member_adr ) )
                    i_ti_texto_cuerpo_correo = ti_correo
                    i_asunto                 = |Error Manhatthan Active { VALUE #( ti_conversion[ topico = es_monitor-topico_gcp ]-descripcion OPTIONAL ) }|
                    i_tipo_documento         = 'HTM' ).
          ELSE.
            COMMIT WORK.
          ENDIF.

          CLEAR: gti_ztcxr1001_3, es_monitor, max_mensaje, ti_correo, mensaje.
        ENDLOOP.
      ENDIF.
    CATCH cx_t100_msg INTO DATA(o_cx_msg).

      APPEND VALUE #( id         = o_cx_msg->t100_msgid
                      number     = o_cx_msg->t100_msgno
                      type       = 'E'
                      message    = o_cx_msg->get_longtext( )
                      message_v1 = o_cx_msg->t100_msgv1
                      message_v2 = o_cx_msg->t100_msgv2
                      message_v3 = o_cx_msg->t100_msgv3
                      message_v4 = o_cx_msg->t100_msgv4 ) TO gti_bapiret2.
  ENDTRY.

  cl_rmsl_message=>display( gti_bapiret2 ).
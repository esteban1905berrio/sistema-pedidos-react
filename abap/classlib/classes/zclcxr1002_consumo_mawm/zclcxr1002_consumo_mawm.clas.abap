CLASS zclcxr1002_consumo_mawm DEFINITION
  PUBLIC
  INHERITING FROM zclcxr1002_consumo_google
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    CONSTANTS: gc_client_key TYPE /goog/keyname VALUE 'MAMW_ACTIVE_KEY'.

    TYPES:
      BEGIN OF gtp_es_atributos_publicacion,
        organization           TYPE string,
        selected_business_unit TYPE string,
        location               TYPE string,
        user                   TYPE string,
        message_type           TYPE string,
        ref_key1               TYPE string,
        ref_key2               TYPE string,
        ref_key3               TYPE string,
        ref_key4               TYPE string,
        ref_key5               TYPE string,
      END OF gtp_es_atributos_publicacion,
      BEGIN OF gtp_es_reemplazar_en_json,
        nombre_sap  TYPE string,
        nombre_json TYPE string,
      END OF gtp_es_reemplazar_en_json,
      gtp_ti_reemplazo TYPE STANDARD TABLE OF gtp_es_reemplazar_en_json WITH EMPTY KEY,
      BEGIN OF gtp_es_context_information,
        _messages     TYPE string,
        _organization TYPE string,
        _user_id      TYPE string,
        _user_locale  TYPE string,
        _location     TYPE string,
      END OF gtp_es_context_information.

    METHODS:
      constructor
        RAISING
          cx_t100_msg,
      publicar_topico
        IMPORTING
          i_es_atributos       TYPE gtp_es_atributos_publicacion
          i_nombre_topico      TYPE string
          VALUE(i_datos_proxy) TYPE any
          i_comprimir          TYPE flag DEFAULT abap_true
          i_ti_reemplazo_json  TYPE gtp_ti_reemplazo OPTIONAL
        EXPORTING
          e_msg_id             TYPE string
        RETURNING
          VALUE(r_ti_bapiret2) TYPE bapiret2_t,
      traer_subscripcion
        IMPORTING
          i_subscripcion          TYPE string
          i_cnt_mensajes          TYPE int4 DEFAULT 1
          i_retornar_msg_original TYPE flag OPTIONAL
        EXPORTING
          e_ti_bapiret2           TYPE bapiret2_t
        CHANGING
          VALUE(e_ti_data)        TYPE ANY TABLE
        RAISING
          cx_t100_msg.

  PROTECTED SECTION.
  PRIVATE SECTION.

    TYPES: BEGIN OF gtp_es_atrib_publicacion,
             organization           TYPE string,
             selected_business_unit TYPE string,
             location               TYPE string,
             user                   TYPE string,
             message_type           TYPE string,
             msg_id_pk              TYPE string,
             ref_key1               TYPE string,
             ref_key2               TYPE string,
             ref_key3               TYPE string,
             ref_key4               TYPE string,
             ref_key5               TYPE string,
           END OF gtp_es_atrib_publicacion.

    METHODS:
      obtener_id_mensaje
        RETURNING
          VALUE(r_id_mensaje) TYPE guid_32.

ENDCLASS.



CLASS ZCLCXR1002_CONSUMO_MAWM IMPLEMENTATION.


  METHOD constructor.

    super->constructor( iv_client_key = gc_client_key ).

  ENDMETHOD.


  METHOD publicar_topico.

    DATA: o_es_descripcion         TYPE REF TO cl_abap_structdescr,
          ti_componentes           TYPE abap_component_tab,
          o_data_descripcion       TYPE REF TO cl_abap_datadescr,
          o_estructura_descripcion TYPE REF TO cl_abap_structdescr,
          o_data                   TYPE REF TO data,
          ref_data                 TYPE REF TO data.

    FIELD-SYMBOLS: <fs_es_estructura> TYPE any.

    e_msg_id = obtener_id_mensaje( ).

    DATA(es_atributos) = CORRESPONDING gtp_es_atrib_publicacion( i_es_atributos ).
    es_atributos-msg_id_pk = e_msg_id.

    TRY.

        o_es_descripcion ?= cl_abap_tabledescr=>describe_by_data( i_datos_proxy ).
        ti_componentes = o_es_descripcion->get_components( ).

        IF NOT line_exists( ti_componentes[ name = 'TOPIC' ] ).
          o_data_descripcion ?= cl_abap_elemdescr=>get_string( ).
          APPEND VALUE #( name = 'TOPIC'
                          type = o_data_descripcion ) TO ti_componentes.
        ENDIF.

        IF NOT line_exists( ti_componentes[ name = '_MSG_ID' ] ).
          o_data_descripcion ?= cl_abap_elemdescr=>get_string( ).
          APPEND VALUE #( name = '_MSG_ID'
                          type = o_data_descripcion ) TO ti_componentes.
        ENDIF.

        o_estructura_descripcion ?= cl_abap_structdescr=>create( ti_componentes ).

        CREATE DATA o_data TYPE HANDLE o_estructura_descripcion.
        ASSIGN o_data->* TO <fs_es_estructura>.

        <fs_es_estructura> = CORRESPONDING #( i_datos_proxy ).
        ASSIGN COMPONENT 'TOPIC' OF STRUCTURE <fs_es_estructura> TO FIELD-SYMBOL(<fs_valor>).
        IF <fs_valor> IS ASSIGNED.
          <fs_valor> = i_nombre_topico.
        ENDIF.
        ASSIGN COMPONENT '_MSG_ID' OF STRUCTURE <fs_es_estructura> TO <fs_valor>.
        IF <fs_valor> IS ASSIGNED.
          <fs_valor> = e_msg_id.
        ENDIF.

        ref_data = REF #( <fs_es_estructura> ).

      CATCH cx_root.

        ASSIGN COMPONENT '_MSG_ID' OF STRUCTURE <fs_es_estructura> TO <fs_valor>.
        IF <fs_valor> IS ASSIGNED.
          <fs_valor> = e_msg_id.
        ENDIF.
        ref_data = REF #( i_datos_proxy ).

    ENDTRY.

    DATA(json) = /ui2/cl_json=>serialize( data        = ref_data "REF #( i_datos_proxy )
                                          compress    = i_comprimir "abap_true
                                          pretty_name = /ui2/cl_json=>pretty_mode-camel_case ).

    IF i_ti_reemplazo_json IS SUPPLIED AND i_ti_reemplazo_json IS NOT INITIAL.
      LOOP AT i_ti_reemplazo_json ASSIGNING FIELD-SYMBOL(<fs_es_reemplazo>).
        REPLACE ALL OCCURRENCES OF <fs_es_reemplazo>-nombre_sap IN json WITH <fs_es_reemplazo>-nombre_json.
      ENDLOOP.
    ENDIF.

    TRY.

        r_ti_bapiret2 = publish_topics( iv_topic_name  = i_nombre_topico
                                        iv_msg         = json
                                        iv_msg_id      = CONV #( e_msg_id )
                                        i_r_attributes = REF #( es_atributos ) ).

      CATCH cx_t100_msg INTO DATA(o_cx_msg).

        MESSAGE ID o_cx_msg->t100_msgid TYPE 'S'
           NUMBER o_cx_msg->t100_msgno
           WITH o_cx_msg->t100_msgv1 o_cx_msg->t100_msgv2 o_cx_msg->t100_msgv3 o_cx_msg->t100_msgv4
           INTO DATA(mensaje).

        APPEND VALUE #( id         = o_cx_msg->t100_msgid
                        number     = o_cx_msg->t100_msgno
                        type       = 'E'
                        message    = mensaje
                        message_v1 = o_cx_msg->t100_msgv1
                        message_v2 = o_cx_msg->t100_msgv2
                        message_v3 = o_cx_msg->t100_msgv3
                        message_v4 = o_cx_msg->t100_msgv4 ) TO r_ti_bapiret2.
    ENDTRY.

  ENDMETHOD.


  METHOD traer_subscripcion.

    " Tabla interna para almacenar los valores de InvSync_Summary_End
    DATA: ti_componentes           TYPE abap_component_tab,
          o_estructura_descripcion TYPE REF TO cl_abap_structdescr,
          o_data                   TYPE REF TO data,
          o_data_descripcion       TYPE REF TO cl_abap_datadescr,
          ti_mensaje               TYPE gtp_ti_mensaje,
          o_es_descripcion         TYPE REF TO cl_abap_structdescr,
          o_ti_descripcion         TYPE REF TO cl_abap_tabledescr,
          o_data_ref               TYPE REF TO data.

    data:
          mensaje TYPE BAPI_MSG.

    FIELD-SYMBOLS: <fs_es_estructura> TYPE any,
                   <fs_ti_contenido>  TYPE ANY TABLE.

    CLEAR: e_ti_bapiret2.

    TRY.

        pull_subscriptions(
          EXPORTING
            iv_subscription = i_subscripcion
            iv_max_messages = i_cnt_mensajes
          IMPORTING
            e_ti_bapiret2   = e_ti_bapiret2
          RECEIVING
            r_ti_mensaje    = ti_mensaje ).

      CATCH cx_t100_msg INTO DATA(o_cx_msg).

        MESSAGE ID o_cx_msg->t100_msgid TYPE 'S'
           NUMBER o_cx_msg->t100_msgno
           WITH o_cx_msg->t100_msgv1 o_cx_msg->t100_msgv2 o_cx_msg->t100_msgv3 o_cx_msg->t100_msgv4
           INTO mensaje.

        APPEND VALUE #( id         = o_cx_msg->t100_msgid
                        number     = o_cx_msg->t100_msgno
                        type       = 'E'
                        message    = mensaje
                        message_v1 = o_cx_msg->t100_msgv1
                        message_v2 = o_cx_msg->t100_msgv2
                        message_v3 = o_cx_msg->t100_msgv3
                        message_v4 = o_cx_msg->t100_msgv4 ) TO e_ti_bapiret2.
    ENDTRY.

*Inicio Modificacion 03Oct2025 Sebastian Restrepo Villa
*NOTA: Se cambia la ubicacion de este codigo debido a que si fallaba la conexion con GCP y ya se habia procesado un bloque, el programa omitia los mensajes ya procesados y se perdian
    IF ti_mensaje IS NOT INITIAL.
      "Creando la estructura | Para poder recibir la estructura enviada de MAWM
      IF i_retornar_msg_original = abap_false.
        o_data_descripcion ?= cl_abap_tabledescr=>describe_by_name( 'GTP_ES_CONTEXT_INFORMATION' ).
        APPEND VALUE #( name = '_CONTEXT_INFORMATION'
                        type = o_data_descripcion ) TO ti_componentes.
        o_data_descripcion ?= cl_abap_elemdescr=>get_i( ).
        APPEND VALUE #( name = '_CURRENT_PAGE'
                        type = o_data_descripcion ) TO ti_componentes.
        o_data_descripcion ?= cl_abap_tabledescr=>describe_by_data( e_ti_data ).
        APPEND VALUE #( name = '_EXPORT_DOCUMENTS'
                        type = o_data_descripcion ) TO ti_componentes.
        o_data_descripcion ?= cl_abap_elemdescr=>get_string( ).
        APPEND VALUE #( name = '_UNIQUE_JOB_ID'
                        type = o_data_descripcion ) TO ti_componentes.
        o_data_descripcion ?= cl_abap_elemdescr=>get_i( ).
        APPEND VALUE #( name = '_TOTAL_PAGES'
                        type = o_data_descripcion ) TO ti_componentes.

        o_estructura_descripcion ?= cl_abap_structdescr=>create( ti_componentes ).

        CREATE DATA o_data TYPE HANDLE o_estructura_descripcion.
        ASSIGN o_data->* TO <fs_es_estructura>.

        LOOP AT ti_mensaje ASSIGNING FIELD-SYMBOL(<fs_es_mensaje>).
          /ui2/cl_json=>deserialize( EXPORTING json        = <fs_es_mensaje>
                                               pretty_name = /ui2/cl_json=>pretty_mode-camel_case
                                     CHANGING  data        = <fs_es_estructura> ).
          ASSIGN ('<FS_ES_ESTRUCTURA>-_UNIQUE_JOB_ID') TO FIELD-SYMBOL(<fs_id_job>).
          IF <fs_id_job> IS ASSIGNED AND <fs_id_job> IS NOT INITIAL.
            MESSAGE i108(zcx01) WITH <fs_id_job> INTO mensaje.
            APPEND VALUE #( id         = gc_msgid "'ZCX01'
                            number     = '108'
                            type       = 'I'
                            message_v1 = <fs_id_job>
                            message    = mensaje ) TO e_ti_bapiret2.
          ENDIF.
          ASSIGN ('<FS_ES_ESTRUCTURA>-_EXPORT_DOCUMENTS') TO <fs_ti_contenido>.
          IF <fs_ti_contenido> IS INITIAL.
            "{ Si no existe ExportDocument, intentar castear la estructura directa
            IF o_data_ref IS INITIAL.
              o_ti_descripcion ?= cl_abap_structdescr=>describe_by_data( e_ti_data ).
              o_es_descripcion ?= o_ti_descripcion->get_table_line_type( ).
              CREATE DATA o_data_ref TYPE HANDLE o_es_descripcion.
            ENDIF.
            ASSIGN o_data_ref->* TO FIELD-SYMBOL(<fs_es_contenido>).
            /ui2/cl_json=>deserialize( EXPORTING json        = <fs_es_mensaje>
                                                 pretty_name = /ui2/cl_json=>pretty_mode-camel_case
                                       CHANGING  data        = <fs_es_contenido> ).
            IF <fs_es_contenido> IS NOT INITIAL.
              INSERT <fs_es_contenido> INTO TABLE <fs_ti_contenido>.
            ENDIF.
          ENDIF.
          LOOP AT <fs_ti_contenido> ASSIGNING FIELD-SYMBOL(<fs_es_data>).
            INSERT <fs_es_data> INTO TABLE e_ti_data.
          ENDLOOP.
          CLEAR: <fs_es_estructura>.
          UNASSIGN: <fs_ti_contenido>, <fs_es_contenido>, <fs_id_job>.
        ENDLOOP.
      ELSE.
        e_ti_data = ti_mensaje.
      ENDIF.
    ENDIF.
*Fin Modificacion 03Oct2025 Sebastian Restrepo Villa

  ENDMETHOD.


  METHOD obtener_id_mensaje.

    CALL FUNCTION 'GUID_CREATE'
      IMPORTING
        ev_guid_32 = r_id_mensaje.

  ENDMETHOD.
ENDCLASS.
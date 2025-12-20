CLASS zclcxc1017_cargar_etiqueta DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    METHODS:
      cargar_etiqueta
        IMPORTING
          i_nombre_archivo TYPE string
          i_nombre_tabla   TYPE dd02l-tabname.

    CLASS-METHODS: cargar_tabla
      IMPORTING
        i_nombre_archivo         TYPE string
        i_nombre_tabla           TYPE dd02l-tabname OPTIONAL
        i_ignorar_linea_cabecera TYPE flag OPTIONAL
        i_ignorar_mandante       TYPE flag OPTIONAL
        i_test                   TYPE flag OPTIONAL
        i_truncar_tabla          TYPE flag OPTIONAL
      RETURNING
        VALUE(r_ti_bd)           TYPE REF TO data.
  PROTECTED SECTION.
  PRIVATE SECTION.
    METHODS cargar_ztcxr1003_1
      IMPORTING
        i_nombre_archivo TYPE string
        i_nombre_tabla   TYPE dd02l-tabname OPTIONAL.
    METHODS cargar_ztcxr1003_2
      IMPORTING
        i_nombre_archivo TYPE string.
    METHODS cargar_ztcxr1003_3
      IMPORTING
        i_nombre_archivo TYPE string.
    METHODS cargar_ztcxr1003_4
      IMPORTING
        i_nombre_archivo TYPE string.
    METHODS cargar_ztcxr1003_5
      IMPORTING
        i_nombre_archivo TYPE string.
    METHODS cargar_ztcxr1003_6
      IMPORTING
        i_nombre_archivo TYPE string.
    METHODS cargar_ztcxr1003_7
      IMPORTING
        i_nombre_archivo TYPE string.
ENDCLASS.



CLASS zclcxc1017_cargar_etiqueta IMPLEMENTATION.

  METHOD cargar_tabla.

    TYPES: BEGIN OF tp_es_datos_archivo,
             registro TYPE string,
           END OF tp_es_datos_archivo,
           "Structure for Reading Field Names of DataBase Table
           BEGIN OF tp_es_dd03vv,
             tabname   TYPE tabname,
             fieldname TYPE fieldname,
             position  TYPE tabfdpos,
           END OF tp_es_dd03vv .

    DATA: ti_datos_archivo_plano  TYPE STANDARD TABLE OF tp_es_datos_archivo,
          es_datos_archivo_plano  LIKE LINE OF ti_datos_archivo_plano,
          ti_campos_tabla_interna TYPE zclcxr1002_util=>gtp_ti_estructura_tabla,
          ti_split                TYPE STANDARD TABLE OF string,
          valor_campo_archivo     TYPE string,
          nombre_archivo          TYPE string,
          indice                  TYPE i,
          nombre_tabla            TYPE dd02l-tabname, " DataBase Table Name
          r_es_bd                 TYPE REF TO data,
          ti_dd03vv               TYPE STANDARD TABLE OF dd03vv, "Internal Table and work area for Reading Field Names of DataBase Table
          es_dd03vv               LIKE LINE OF ti_dd03vv,
          nombre_del_campo        TYPE fieldname,
          contador_campos_tabla   TYPE i.

    FIELD-SYMBOLS : <fs_ti_bd>       TYPE ANY TABLE,
                    <fs_es_ti_bd>    TYPE any,
                    <fs_valor_campo> TYPE any.

    nombre_archivo = i_nombre_archivo.

    "FM Cargar archivo
    CALL FUNCTION 'GUI_UPLOAD'
      EXPORTING
        filename                = nombre_archivo
        replacement             = '#'
        codepage                = '4110'
      TABLES
        data_tab                = ti_datos_archivo_plano
      EXCEPTIONS
        file_open_error         = 1
        file_read_error         = 2
        no_batch                = 3
        gui_refuse_filetransfer = 4
        invalid_type            = 5
        no_authority            = 6
        unknown_error           = 7
        bad_data_format         = 8
        header_not_allowed      = 9
        separator_not_allowed   = 10
        header_too_long         = 11
        unknown_dp_error        = 12
        access_denied           = 13
        dp_out_of_memory        = 14
        disk_full               = 15
        dp_timeout              = 16
        OTHERS                  = 17.

    IF sy-subrc <> 0.
      MESSAGE e014(gle_mca_excel_upload) WITH nombre_archivo.
    ELSE.

      "Crear tabla interna{
      es_datos_archivo_plano = ti_datos_archivo_plano[ 1 ].
      SPLIT es_datos_archivo_plano-registro AT ';' INTO TABLE ti_split.


      ti_campos_tabla_interna = VALUE #( FOR es_cabecera_plano IN ti_split
                              ( nombre = shift_right( val = es_cabecera_plano ) campo = 'STRING'  )
                         ).
      TRY.
          r_ti_bd = zclcxr1002_util=>crear_ti_dinamica( EXPORTING i_tipo_clave = cl_abap_tabledescr=>keydefkind_empty CHANGING c_ti_campos = ti_campos_tabla_interna ).
        CATCH cx_sy_struct_creation INTO DATA(o_cx_st).

        CATCH cx_static_check cx_dynamic_check INTO DATA(o_cx).
          DATA(texto) = o_cx->get_text( ) && o_cx->get_longtext( ).
      ENDTRY.

      ASSIGN r_ti_bd->* TO <fs_ti_bd>.

      CREATE DATA r_es_bd LIKE LINE OF <fs_ti_bd>.
      ASSIGN r_es_bd->* TO <fs_es_ti_bd>.
      "}

      LOOP AT ti_datos_archivo_plano INTO es_datos_archivo_plano FROM 2.

        CLEAR: ti_split, <fs_es_ti_bd>.

        SPLIT es_datos_archivo_plano-registro AT ';' INTO TABLE ti_split.

        DO.

          indice = sy-index.

          CLEAR: valor_campo_archivo.

          READ TABLE ti_split INDEX indice INTO valor_campo_archivo.

          ASSIGN COMPONENT indice OF STRUCTURE <fs_es_ti_bd> TO <fs_valor_campo>.

          IF sy-subrc IS INITIAL.
            <fs_valor_campo> = valor_campo_archivo.
          ELSE.
            EXIT.
          ENDIF.

        ENDDO.

        INSERT <fs_es_ti_bd> INTO TABLE  <fs_ti_bd> .

      ENDLOOP.

      IF i_test = abap_true.

        TRY.
            CALL FUNCTION 'ZCXR1002_MOSTRARALV_01'
              EXPORTING
                i_ti_datos    = r_ti_bd
                i_ti_catalogo = zclcxr1002_util=>construir_catalogo( i_ti = <fs_ti_bd> i_optimizar_columnas = abap_true ).
          CATCH cx_dynamic_check cx_static_check.
            "handle exception
        ENDTRY.

      ELSE.

      ENDIF.

    ENDIF.

  ENDMETHOD.

  METHOD cargar_etiqueta.

***    MODIFY ztcxr1003_1 FROM ( SELECT * FROM ztcxr1003_1 USING CLIENT '100' ).
***    MODIFY ztcxr1003_2 FROM ( SELECT * FROM ztcxr1003_2 USING CLIENT '100' ).
BREAK-POINT.
***    MODIFY ztcxr1003_3 FROM ( SELECT * FROM ztcxr1003_3 USING CLIENT '120' ).
***    MODIFY ztcxr1003_4 FROM ( SELECT * FROM ztcxr1003_4 USING CLIENT '100' ).
***    MODIFY ztcxr1003_5 FROM ( SELECT * FROM ztcxr1003_5 USING CLIENT '100' ).
***    MODIFY ztcxr1003_6 FROM ( SELECT * FROM ztcxr1003_6 USING CLIENT '100' ).
***    MODIFY ztcxr1003_7 FROM ( SELECT * FROM ztcxr1003_7 USING CLIENT '100' ).
***
***    COMMIT WORK AND WAIT.

***    cargar_ztcxr1003_1( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_2( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_3( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_4( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_5( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_6( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_7( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_8( EXPORTING i_nombre_archivo = i_nombre_archivo ).
***    cargar_ztcxr1003_9( EXPORTING i_nombre_archivo = i_nombre_archivo ).

  ENDMETHOD.


  METHOD cargar_ztcxr1003_1.
    DATA: ti_ztcxr1003_1 TYPE STANDARD TABLE OF ztcxr1003_1,
          es_ztcxr1003_1 TYPE ztcxr1003_1,
          guid_16        TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_1> LIKE LINE OF ti_ztcxr1003_1.



    SELECT *
    FROM ztcxr1003_1
    INTO TABLE @DATA(ti_ztcxr1003_1_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_1) = cargar_tabla( i_nombre_archivo = i_nombre_archivo i_nombre_tabla = i_nombre_tabla ).

    ASSIGN r_ti_ztcxr1003_1->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      CLEAR: es_ztcxr1003_1.
      es_ztcxr1003_1 = CORRESPONDING #( <fs_es_db> ).

      IF line_exists( ti_ztcxr1003_1_db[ etiqueta_id = es_ztcxr1003_1-etiqueta_id ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_1 ASSIGNING <es_ztcxr1003_1>.

      <es_ztcxr1003_1> = es_ztcxr1003_1.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_1>-etiqueta_uuid = guid_16.
      <es_ztcxr1003_1>-clase = space.

      <es_ztcxr1003_1>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_1>-created_at.
      <es_ztcxr1003_1>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_1>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_1>-local_last_changed_at.

    ENDLOOP.

    MODIFY ztcxr1003_1 FROM TABLE ti_ztcxr1003_1.

    COMMIT WORK AND WAIT.

  ENDMETHOD.


  METHOD cargar_ztcxr1003_2.
    DATA: ti_ztcxr1003_2 TYPE STANDARD TABLE OF ztcxr1003_2,
          es_ztcxr1003_2 TYPE ztcxr1003_2,
          guid_16        TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_2> LIKE LINE OF ti_ztcxr1003_2.



    SELECT *
    FROM ztcxr1003_2
    INTO TABLE @DATA(ti_ztcxr1003_2_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_2) = cargar_tabla( i_nombre_archivo = i_nombre_archivo  ).

    ASSIGN r_ti_ztcxr1003_2->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      CLEAR: es_ztcxr1003_2.
      es_ztcxr1003_2 = CORRESPONDING #( <fs_es_db> ).

      IF line_exists( ti_ztcxr1003_2_db[ campo_id = es_ztcxr1003_2-campo_id ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_2 ASSIGNING <es_ztcxr1003_2>.

      <es_ztcxr1003_2> = es_ztcxr1003_2.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_2>-campo_uuid  = guid_16.

      <es_ztcxr1003_2>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_2>-created_at.
      <es_ztcxr1003_2>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_2>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_2>-local_last_changed_at.

    ENDLOOP.

    MODIFY ztcxr1003_2 FROM TABLE ti_ztcxr1003_2.
    COMMIT WORK AND WAIT.

  ENDMETHOD.


  METHOD cargar_ztcxr1003_3.
    DATA: ti_ztcxr1003_3             TYPE STANDARD TABLE OF ztcxr1003_3,
          es_ztcxr1003_3             TYPE ztcxr1003_3,
          ct_etiquetas_no_encontrada TYPE string_t,
          guid_16                    TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_3> LIKE LINE OF ti_ztcxr1003_3.

    SELECT *
    FROM ztcxr1003_3
    INTO TABLE @DATA(ti_ztcxr1003_3_db).
    "Etiquetas
    SELECT *
    FROM ztcxr1003_1
    INTO TABLE @DATA(ti_ztcxr1003_1_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_3) = cargar_tabla( i_nombre_archivo = i_nombre_archivo  ).

    ASSIGN r_ti_ztcxr1003_3->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      ASSIGN COMPONENT 'ETIQUETA_ID' OF STRUCTURE <fs_es_db> TO FIELD-SYMBOL(<fs_etiqueta_id>).

      IF sy-subrc IS NOT INITIAL.
        BREAK-POINT.
      ENDIF.

      CLEAR: es_ztcxr1003_3.
      es_ztcxr1003_3 = CORRESPONDING #( <fs_es_db> ).

      IF line_exists( ti_ztcxr1003_3_db[ etiqueta_uuid = <fs_etiqueta_id> campo_id = es_ztcxr1003_3-campo_id ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_3 ASSIGNING <es_ztcxr1003_3>.

      <es_ztcxr1003_3> = es_ztcxr1003_3.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_3>-campo_uuid  = guid_16.
      TRY.
          <es_ztcxr1003_3>-etiqueta_uuid  = ti_ztcxr1003_1_db[ etiqueta_id = <fs_etiqueta_id> ]-etiqueta_uuid.
        CATCH cx_sy_itab_line_not_found.
          APPEND <fs_etiqueta_id> TO ct_etiquetas_no_encontrada.
      ENDTRY.
      <es_ztcxr1003_3>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_3>-created_at.
      <es_ztcxr1003_3>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_3>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_3>-local_last_changed_at.

    ENDLOOP.

    DELETE ti_ztcxr1003_3 WHERE etiqueta_uuid IS INITIAL.

    MODIFY ztcxr1003_3 FROM TABLE ti_ztcxr1003_3.
    COMMIT WORK AND WAIT.

  ENDMETHOD.


  METHOD cargar_ztcxr1003_4.
    DATA: ti_ztcxr1003_4 TYPE STANDARD TABLE OF ztcxr1003_4,
          es_ztcxr1003_4 TYPE ztcxr1003_4,
          guid_16        TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_4> LIKE LINE OF ti_ztcxr1003_4.

    SELECT *
    FROM ztcxr1003_4
    INTO TABLE @DATA(ti_ztcxr1003_4_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_4) = cargar_tabla( i_nombre_archivo = i_nombre_archivo  ).

    ASSIGN r_ti_ztcxr1003_4->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      CLEAR: es_ztcxr1003_4.
      es_ztcxr1003_4 = CORRESPONDING #( <fs_es_db> ).

      IF line_exists( ti_ztcxr1003_4_db[ impresora_id = es_ztcxr1003_4-impresora_id ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_4 ASSIGNING <es_ztcxr1003_4>.

      <es_ztcxr1003_4> = es_ztcxr1003_4.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_4>-impresora_uuid  = guid_16.

      <es_ztcxr1003_4>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_4>-created_at.
      <es_ztcxr1003_4>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_4>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_4>-local_last_changed_at.

    ENDLOOP.

    MODIFY ztcxr1003_4 FROM TABLE ti_ztcxr1003_4.
    COMMIT WORK AND WAIT.
  ENDMETHOD.


  METHOD cargar_ztcxr1003_5.
    DATA: ti_ztcxr1003_5             TYPE STANDARD TABLE OF ztcxr1003_5,
          es_ztcxr1003_5             TYPE ztcxr1003_5,
          ct_etiquetas_no_encontrada TYPE string_t,
          guid_16                    TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_5> LIKE LINE OF ti_ztcxr1003_5.

    SELECT *
    FROM ztcxr1003_5
    INTO TABLE @DATA(ti_ztcxr1003_5_db).
    "Etiquetas
    SELECT *
    FROM ztcxr1003_1
    INTO TABLE @DATA(ti_ztcxr1003_1_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_5) = cargar_tabla( i_nombre_archivo = i_nombre_archivo  ).

    ASSIGN r_ti_ztcxr1003_5->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      ASSIGN COMPONENT 'ETIQUETA_ID' OF STRUCTURE <fs_es_db> TO FIELD-SYMBOL(<fs_etiqueta_id>).

      CLEAR: es_ztcxr1003_5.
      es_ztcxr1003_5 = CORRESPONDING #( <fs_es_db> ).
      "descartar registros que ya existen
      IF line_exists( ti_ztcxr1003_5_db[ etiqueta_uuid = <fs_etiqueta_id> usuario_id = es_ztcxr1003_5-usuario_id ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_5 ASSIGNING <es_ztcxr1003_5>.

      <es_ztcxr1003_5> = es_ztcxr1003_5.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_5>-usuario_uuid  = guid_16.
      TRY.
          <es_ztcxr1003_5>-etiqueta_uuid  = ti_ztcxr1003_1_db[ etiqueta_id = <fs_etiqueta_id> ]-etiqueta_uuid.
        CATCH cx_sy_itab_line_not_found.
          APPEND <fs_etiqueta_id> TO ct_etiquetas_no_encontrada.
      ENDTRY.
      <es_ztcxr1003_5>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_5>-created_at.
      <es_ztcxr1003_5>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_5>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_5>-local_last_changed_at.

    ENDLOOP.

    DELETE ti_ztcxr1003_5 WHERE etiqueta_uuid IS INITIAL.

    MODIFY ztcxr1003_5 FROM TABLE ti_ztcxr1003_5.
    COMMIT WORK AND WAIT.
  ENDMETHOD.


  METHOD cargar_ztcxr1003_6.
    DATA: ti_ztcxr1003_6             TYPE STANDARD TABLE OF ztcxr1003_6,
          es_ztcxr1003_6             TYPE ztcxr1003_6,
          ct_etiquetas_no_encontrada TYPE string_t,
          guid_16                    TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_6> LIKE LINE OF ti_ztcxr1003_6.

    SELECT *
    FROM ztcxr1003_6
    INTO TABLE @DATA(ti_ztcxr1003_6_db).
    "Etiquetas
    SELECT *
    FROM ztcxr1003_1
    INTO TABLE @DATA(ti_ztcxr1003_1_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_6) = cargar_tabla( i_nombre_archivo = i_nombre_archivo  ).

    ASSIGN r_ti_ztcxr1003_6->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      ASSIGN COMPONENT 'ETIQUETA_ID' OF STRUCTURE <fs_es_db> TO FIELD-SYMBOL(<fs_etiqueta_id>).

      CLEAR: es_ztcxr1003_6.
      es_ztcxr1003_6 = CORRESPONDING #( <fs_es_db> ).
      "descartar registros que ya existen
      IF line_exists( ti_ztcxr1003_6_db[ etiqueta_uuid = <fs_etiqueta_id>
                                         tipo_linea = es_ztcxr1003_6-tipo_linea
                                         linea = es_ztcxr1003_6-linea ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_6 ASSIGNING <es_ztcxr1003_6>.

      <es_ztcxr1003_6> = es_ztcxr1003_6.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_6>-impdin_uuid  = guid_16.
      TRY.
          <es_ztcxr1003_6>-etiqueta_uuid  = ti_ztcxr1003_1_db[ etiqueta_id = <fs_etiqueta_id> ]-etiqueta_uuid.
        CATCH cx_sy_itab_line_not_found.
          APPEND <fs_etiqueta_id> TO ct_etiquetas_no_encontrada.
      ENDTRY.
      <es_ztcxr1003_6>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_6>-created_at.
      <es_ztcxr1003_6>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_6>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_6>-local_last_changed_at.

    ENDLOOP.

    DELETE ti_ztcxr1003_6 WHERE etiqueta_uuid IS INITIAL.

    MODIFY ztcxr1003_6 FROM TABLE ti_ztcxr1003_6.
    COMMIT WORK AND WAIT.
  ENDMETHOD.


  METHOD cargar_ztcxr1003_7.
    DATA: ti_ztcxr1003_7             TYPE STANDARD TABLE OF ztcxr1003_7,
          es_ztcxr1003_7             TYPE ztcxr1003_7,
          ct_etiquetas_no_encontrada TYPE string_t,
          guid_16                    TYPE guid_16.

    FIELD-SYMBOLS: <fs_ti_bd>       TYPE ANY TABLE,
                   <es_ztcxr1003_7> LIKE LINE OF ti_ztcxr1003_7.

    SELECT *
    FROM ztcxr1003_7
    INTO TABLE @DATA(ti_ztcxr1003_7_db).
    "Etiquetas
    SELECT *
    FROM ztcxr1003_1
    INTO TABLE @DATA(ti_ztcxr1003_1_db).

    BREAK-POINT.

    DATA(r_ti_ztcxr1003_7) = cargar_tabla( i_nombre_archivo = i_nombre_archivo  ).

    ASSIGN r_ti_ztcxr1003_7->* TO <fs_ti_bd>.

    LOOP AT <fs_ti_bd> ASSIGNING FIELD-SYMBOL(<fs_es_db>).

      ASSIGN COMPONENT 'ETIQUETA_ID' OF STRUCTURE <fs_es_db> TO FIELD-SYMBOL(<fs_etiqueta_id>).

      CLEAR: es_ztcxr1003_7.
      es_ztcxr1003_7 = CORRESPONDING #( <fs_es_db> ).
      "descartar registros que ya existen
      IF line_exists( ti_ztcxr1003_7_db[ etiqueta_uuid = <fs_etiqueta_id> ] ).
        CONTINUE.
      ENDIF.

      APPEND INITIAL LINE TO ti_ztcxr1003_7 ASSIGNING <es_ztcxr1003_7>.

      <es_ztcxr1003_7> = es_ztcxr1003_7.

      CALL FUNCTION 'GUID_CREATE'
        IMPORTING
          ev_guid_16 = guid_16.

      <es_ztcxr1003_7>-mensaje_uuid  = guid_16.
      TRY.
          <es_ztcxr1003_7>-etiqueta_uuid  = ti_ztcxr1003_1_db[ etiqueta_id = <fs_etiqueta_id> ]-etiqueta_uuid.
        CATCH cx_sy_itab_line_not_found.
          APPEND <fs_etiqueta_id> TO ct_etiquetas_no_encontrada.
      ENDTRY.
      <es_ztcxr1003_7>-created_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_7>-created_at.
      <es_ztcxr1003_7>-last_change_by = 'SALOPERA'.
      GET TIME STAMP FIELD <es_ztcxr1003_7>-last_change_at.
      GET TIME STAMP FIELD <es_ztcxr1003_7>-local_last_changed_at.

    ENDLOOP.

    DELETE ti_ztcxr1003_7 WHERE etiqueta_uuid IS INITIAL.

    MODIFY ztcxr1003_7 FROM TABLE ti_ztcxr1003_7.
    COMMIT WORK AND WAIT.
  ENDMETHOD.

ENDCLASS.
*&---------------------------------------------------------------------*
*& Include zcxr1006c_2
*&---------------------------------------------------------------------*
CLASS lcl_controlador DEFINITION.

  PUBLIC SECTION.

    CLASS-METHODS: cargar_tabla
      IMPORTING
        i_nombre_archivo         TYPE string
        i_nombre_tabla           TYPE dd02l-tabname
        i_ignorar_linea_cabecera TYPE flag
        i_ignorar_mandante       TYPE flag
        i_test                   TYPE flag
        i_truncar_tabla          TYPE flag.

ENDCLASS.

CLASS lcl_controlador IMPLEMENTATION.

  METHOD cargar_tabla.

    TYPES: BEGIN OF tp_es_datos_archivo,
             rec TYPE string,
           END OF tp_es_datos_archivo,
           "Structure for Reading Field Names of DataBase Table
           BEGIN OF tp_es_dd03vv,
             tabname   TYPE tabname,
             fieldname TYPE fieldname,
             position  TYPE tabfdpos,
           END OF tp_es_dd03vv .

    DATA: ti_datos_archivo_plano TYPE STANDARD TABLE OF tp_es_datos_archivo,
          es_datos_archivo_plano LIKE LINE OF ti_datos_archivo_plano,
          ti_split               TYPE STANDARD TABLE OF string,
          valor_campo_archivo    TYPE string,
          nombre_archivo         TYPE string,
          indice                 TYPE i,
          r_ti_datos             TYPE REF TO data,
          nombre_tabla           TYPE dd02l-tabname, " DataBase Table Name
          r_ti_bd                TYPE REF TO data,
          r_es_bd                TYPE REF TO data,
          ti_dd03vv              TYPE STANDARD TABLE OF dd03vv, "Internal Table and work area for Reading Field Names of DataBase Table
          es_dd03vv              LIKE LINE OF ti_dd03vv,
          nombre_del_campo       TYPE fieldname,
          contador_campos_tabla  TYPE i.

    FIELD-SYMBOLS : <fs_ti_bd>       TYPE ANY TABLE,
                    <fs_es_ti_bd>    TYPE any,
                    <fs_valor_campo> TYPE any.

    nombre_archivo = i_nombre_archivo.

    "FM to read file content from path provided
    CALL FUNCTION 'GUI_UPLOAD'
      EXPORTING
        filename                = nombre_archivo
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
    ENDIF.

    IF i_nombre_tabla IS NOT INITIAL.

      nombre_tabla = i_nombre_tabla.
      "Check If table exist in Data Dictionary and no of columns in table

      SELECT COUNT(*)
      FROM dd03vv
      INTO  contador_campos_tabla
      WHERE tabname = nombre_tabla
      AND   as4local = 'A'.

      IF sy-subrc = 0 AND contador_campos_tabla GT 0 .

        "Dynamic Internal Table Defination
        CREATE DATA r_ti_bd TYPE TABLE OF (i_nombre_tabla).
        ASSIGN r_ti_bd->* TO <fs_ti_bd> .

        "Dynamic Work Area Defination
        CREATE DATA r_es_bd LIKE LINE OF <fs_ti_bd>.
        ASSIGN r_es_bd->* TO <fs_es_ti_bd> .

        SELECT *
        FROM dd03vv
        INTO TABLE ti_dd03vv
        WHERE tabname = nombre_tabla
        AND as4local = 'A' .

        SORT ti_dd03vv BY  position .

        LOOP AT ti_datos_archivo_plano INTO es_datos_archivo_plano.
          " Ignore header Row if Checkbox is checked
          IF sy-tabix = 1 AND i_ignorar_linea_cabecera = abap_true.
            CONTINUE.
          ENDIF.

          SPLIT es_datos_archivo_plano-rec AT ';' INTO TABLE ti_split.
          "Read Field Name from Internal Table one by one
          DO  contador_campos_tabla TIMES.

            indice = sy-index .
            "omitir mandante
            IF i_ignorar_mandante = abap_true.
              CHECK indice > 1.
            ELSE.
              "Consultar valor indice - 1 para omitir mandante
              indice = indice - 1.
            ENDIF.

            READ TABLE ti_dd03vv INTO es_dd03vv
            WITH KEY position = indice BINARY SEARCH .
            nombre_del_campo =  es_dd03vv-fieldname .

            READ TABLE ti_split INDEX indice INTO valor_campo_archivo.

            IF es_dd03vv-inttype = 'D' AND valor_campo_archivo IS NOT INITIAL.
              REPLACE ALL OCCURRENCES OF '.' IN valor_campo_archivo WITH space.
              CONCATENATE valor_campo_archivo+4(4) valor_campo_archivo+2(2) valor_campo_archivo(2) INTO valor_campo_archivo.
            ENDIF.

            "Assign Data to each field
            ASSIGN COMPONENT nombre_del_campo OF STRUCTURE
            <fs_es_ti_bd> TO <fs_valor_campo>.

            IF sy-subrc IS INITIAL.
              <fs_valor_campo> = valor_campo_archivo.
            ENDIF.

          ENDDO.

          "Insert row into Dynamic Internal table
          INSERT <fs_es_ti_bd> INTO TABLE  <fs_ti_bd> .

        ENDLOOP.

        IF i_test = abap_true.

          r_ti_datos = REF #( <fs_ti_bd> ).
          TRY.
              CALL FUNCTION 'ZCXR1002_MOSTRARALV_01'
                EXPORTING
                  i_ti_datos    = r_ti_datos
                  i_ti_catalogo = zclcxr1002_util=>construir_catalogo( i_ti = <fs_ti_bd> i_optimizar_columnas = abap_true ).
            CATCH cx_dynamic_check cx_static_check.
              "handle exception
          ENDTRY.

        ELSE.
          IF i_truncar_tabla = abap_true.
            CALL FUNCTION 'DB_TRUNCATE_TABLE'
              EXPORTING
                tabname = i_nombre_tabla.

          ENDIF.
          MODIFY (i_nombre_tabla) FROM TABLE  <fs_ti_bd>.
          COMMIT WORK AND WAIT.
          IF sy-subrc = 0.
            WRITE : 'Tabla ',i_nombre_tabla ,'actualizada'.
          ENDIF.
        ENDIF.
      ELSE.
        MESSAGE e182(/sehs/ba_misc1) WITH i_nombre_tabla.
      ENDIF.

    ENDIF.


  ENDMETHOD.

ENDCLASS.
"! <p class="shorttext synchronized" lang="es">Funcion para la carga de archivos planos y de excel</p>
CLASS zclcxr1002_cargar_archivo DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    TYPES:
      tp_ti_file_table TYPE STANDARD TABLE OF file_table WITH EMPTY KEY .
    TYPES:
      tp_ti_field TYPE RANGE OF zecxr1002_1-col .
    TYPES:
      tp_ti_con_name TYPE STANDARD TABLE OF string .
    TYPES:
      BEGIN OF tp_es_plantilla,
        colum1  TYPE char2_256,
        colum2  TYPE char2_256,
        colum3  TYPE char2_256,
        colum4  TYPE char2_256,
        colum5  TYPE char2_256,
        colum6  TYPE char2_256,
        colum7  TYPE char2_256,
        colum8  TYPE char2_256,
        colum9  TYPE char2_256,
        colum10 TYPE char2_256,
        colum11 TYPE char2_256,
        colum12 TYPE char2_256,
      END OF tp_es_plantilla .
    TYPES:
      tp_ti_plantilla TYPE STANDARD TABLE OF tp_es_plantilla WITH NON-UNIQUE KEY colum1 .

    TYPES:
      gtp_ti_xstring          TYPE STANDARD TABLE OF x255 WITH EMPTY KEY .

    TYPES:
      BEGIN OF gtp_es_datos_zip,
        nombre    TYPE string,
        contenido TYPE xstring,
      END OF gtp_es_datos_zip .

    TYPES:
      gtp_ti_datos_zip        TYPE STANDARD TABLE OF gtp_es_datos_zip .

    TYPES:
      BEGIN OF gtp_es_contenido_archivo,
        nombre        TYPE string,
        contenido_str TYPE string,
      END OF gtp_es_contenido_archivo .

    TYPES:
      gtp_ti_contenido_archivo        TYPE STANDARD TABLE OF gtp_es_contenido_archivo .

    TYPES: BEGIN OF gtp_es_archivo,
             name TYPE string,
             date TYPE d,
             time TYPE t,
             size TYPE i,
           END OF gtp_es_archivo.
    TYPES: gtp_ti_archivo TYPE TABLE OF gtp_es_archivo.

    CONSTANTS cte_string TYPE field_name VALUE 'STRING' ##NO_TEXT.
    CONSTANTS cte_td_str TYPE field_name VALUE 'STRING=*' ##NO_TEXT.
    CONSTANTS cte_cl_msg TYPE string VALUE 'ZCX01' ##NO_TEXT.
    CONSTANTS cte_col_ini_datos TYPE i VALUE 3 ##NO_TEXT.
    CONSTANTS cte_fila_ini_campo TYPE i VALUE 3 ##NO_TEXT.
    CONSTANTS cte_end_col TYPE i VALUE 160 ##NO_TEXT.

    "! Crear archivo ZIP
    "!
    "! @parameter i_ti_contenido_zip | Nombre del archivo y contenido binario
    "! @parameter e_longitud_salida | Longitud del archivo generado
    "! @parameter e_ti_binario | Archivo ZIP en formato binario
    CLASS-METHODS crear_archivo_zip
      IMPORTING
        !i_ti_contenido_zip TYPE gtp_ti_datos_zip
      EXPORTING
        !e_longitud_salida  TYPE i
        !e_ti_binario       TYPE gtp_ti_xstring .

    CLASS-METHODS descomprimir_archivo_zip
      IMPORTING
        i_ti_archivo_zip   TYPE gtp_ti_xstring
      EXPORTING
        e_ti_contenido_zip TYPE gtp_ti_contenido_archivo .

    CLASS-METHODS matchcode_csv
      RETURNING
        VALUE(r_ruta_archivo) TYPE char1024.

    "! Despliega Matchcode para seleccionar archivos de Excel
    "!
    "! @parameter r_ti_lst_archivo | Archivo seleccionado
    CLASS-METHODS matchcode_excel
      RETURNING
        VALUE(r_ti_lst_archivo) TYPE tp_ti_file_table .
    "! Despliega Matchcode para seleccionar directorio desde su PC
    "!
    "! @parameter r_ti_lst_directorio | Archivo seleccionado
    CLASS-METHODS matchcode_directorio
      RETURNING
        VALUE(r_s_directorio) TYPE string .
    "! Cargar un archivo de excel y llevarlo a una tabla interna
    "!
    "! @parameter i_filename | Nombre de archivo
    "! @parameter i_begin_col | Columna inicial
    "! @parameter i_begin_row | Fila inicial
    "! @parameter i_end_col | Columna final
    "! @parameter i_end_row | Fila final
    "! @parameter i_org_tab | Indicador: Cambiar estructura de tabla de retorno
    "! @parameter e_ti_datos | tabla con datos de archivo
    "! @parameter r_ti_datos | tabla con datos de archivo de tipo fila columna
    "! @raising cx_t100_msg |
    CLASS-METHODS cargar_excel
      IMPORTING
        !i_filename       TYPE localfile
        !i_begin_col      TYPE i DEFAULT 1
        !i_begin_row      TYPE i DEFAULT 1
        !i_end_col        TYPE i DEFAULT cte_end_col
        !i_end_row        TYPE i DEFAULT 17000
        !i_n_hoja         TYPE i OPTIONAL
        !i_org_tab        TYPE flag DEFAULT abap_false
      EXPORTING
        !e_ti_datos       TYPE REF TO data
      RETURNING
        VALUE(r_ti_datos) TYPE zttcxr1002_2
      RAISING
        cx_t100_msg .
    CLASS-METHODS descargar_tabla_excel
      IMPORTING
        !i_filename TYPE localfile
        !i_ext      TYPE localfile
        !i_ti_datos TYPE ANY TABLE
      RAISING
        cx_t100_msg .
    "! Cargar archivo de excel en formato estandar para cargue de información
    "!
    "! @parameter i_nom_archivo | Ruta/Nombre archivo
    "! @parameter i_excl_vcol | nro. col - Excluir validación de DOMINIO para columnas en este parametro
    "! @parameter i_ti_ti_col_str   | Nombre Col. - Omitir tipo de columna y definir como string, envie STRING=* para crear toda la tabla como String
    "! @parameter i_n_hoja | Cargar hoja especifica
    "! @parameter i_n_fila | Fila desde donde inician los datos
    "! @parameter i_n_col | Numero de columna en la que inician los datos
    "! @parameter i_conc_estruc | Conservar nombre de los campos que vienen en el archivo de carga
    "! @parameter i_conv_num_dec | Convertir separador de decimales y miles [. ,]
    "! @parameter i_col_id | Nombre de la columna que identifica la fila
    "! @parameter r_ti_datos | tabla con datos de archivo
    "! @parameter i_fila_ini_campo | Fila en donde inician la desc de los campos
    "! @raising cx_dynamic_check |
    "! @raising cx_t100_msg |
    CLASS-METHODS cargar_excel_ft_carga
      IMPORTING
        !i_nom_archivo    TYPE localfile
        !i_excl_vcol      TYPE tp_ti_field OPTIONAL
        !i_ti_col_str     TYPE tp_ti_con_name OPTIONAL
        !i_n_hoja         TYPE i DEFAULT 2
        !i_n_fila         TYPE i DEFAULT 12
        !i_n_col          TYPE i DEFAULT cte_col_ini_datos
        !i_end_col        TYPE i DEFAULT cte_end_col
        !i_cons_estruc    TYPE flag DEFAULT abap_false
        !i_conv_num_dec   TYPE flag DEFAULT abap_false
        !i_col_id         TYPE field_name OPTIONAL
        !i_r_ti_data      TYPE REF TO data OPTIONAL
        !i_fila_ini_campo TYPE i DEFAULT cte_fila_ini_campo
      CHANGING
        !c_ti_datos       TYPE zttcxr1002_2 OPTIONAL
      RETURNING
        VALUE(r_ti_datos) TYPE REF TO data
      RAISING
        cx_dynamic_check
        cx_t100_msg .
    "! Cargar archivo plano desde carpeta local o desde el servidor
    "! @parameter i_c_filename | Ruta/Nombre archivo
    "! @parameter i_c_servidor | Cuando es 'X' indica que se cargara desde el servidor de aplicación
    "! @parameter i_c_interfaz | Si es 'X' Se retorna tabla estructurada en columnas, de lo contrario en tabla de tipo string
    "! @parameter e_ti_estruc | tabla con datos estructurados colum1, colum2, colum3 .... columN Cuando C_INTERFAZ no es vacio.
    "! @parameter e_ti_datos | Tabla de String cuando C_INTERFAZ es vacio.
    "! @raising cx_t100_msg |
    CLASS-METHODS cargar_plano
      IMPORTING
        !i_c_filename  TYPE localfile
        !i_c_servidor  TYPE c DEFAULT abap_false
        !i_c_separador TYPE char01 DEFAULT space
        !i_c_interfaz  TYPE c DEFAULT abap_false
      EXPORTING
        !e_ti_estruc   TYPE tp_ti_plantilla
        !e_ti_datos    TYPE tp_ti_con_name
      RAISING
        cx_t100_msg .
    "! Cambiar ubicacion de archivo dentro del servidor
    "!
    "! @parameter i_c_origen | Ruta origen de archivo en servidor
    "! @parameter i_c_destino | Ruta deestino de archivo en servidor.
    "! @raising cx_t100_msg |
    CLASS-METHODS mover_archivo_servidor
      IMPORTING
        !i_c_origen  TYPE localfile
        !i_c_destino TYPE localfile
      RAISING
        cx_t100_msg .
    CLASS-METHODS incrementar_val
      IMPORTING
        !i_val_incr    TYPE i DEFAULT 1
      CHANGING
        !c_valor       TYPE i
      RETURNING
        VALUE(r_valor) TYPE i .
    CLASS-METHODS cargar_excel_multiples_hojas
      IMPORTING
        !i_filename       TYPE localfile
        !i_begin_col      TYPE i DEFAULT 1
        !i_begin_row      TYPE i DEFAULT 1
        !i_end_col        TYPE i DEFAULT cte_end_col
        !i_end_row        TYPE i DEFAULT 17000
        !i_n_hojas        TYPE i DEFAULT 1
      RETURNING
        VALUE(r_ti_datos) TYPE zttcxr1002_3
      RAISING
        cx_t100_msg .
  PROTECTED SECTION.

  PRIVATE SECTION.

    CLASS-DATA o_zcx_gen TYPE REF TO cx_t100_msg .
    CLASS-DATA o_cx TYPE REF TO cx_root .
    CLASS-DATA cte_directorio TYPE string VALUE 'C:' ##NO_TEXT.

    CLASS-METHODS remplazar_comp_repetido
      IMPORTING
        !i_cons_estruc TYPE flag OPTIONAL
      CHANGING
        !c_ti_tab_estr TYPE zclcxr1002_util=>gtp_ti_estructura_tabla .
    CLASS-METHODS llenar_tabla
      IMPORTING
        !i_ti_datos        TYPE zttcxr1002_2
        !i_ti_tab_estr     TYPE zclcxr1002_util=>gtp_ti_estructura_tabla
        VALUE(i_excl_vcol) TYPE tp_ti_field OPTIONAL
        !i_ti_col_str      TYPE tp_ti_field OPTIONAL
        VALUE(i_n_fila)    TYPE i DEFAULT 12
        !i_n_col           TYPE i
        !i_conv_num_dec    TYPE flag DEFAULT abap_false
        !i_col_id          TYPE field_name
        !i_fila_ini_campo  TYPE i DEFAULT cte_fila_ini_campo
      CHANGING
        !c_ti_datos        TYPE REF TO data .
ENDCLASS.



CLASS zclcxr1002_cargar_archivo IMPLEMENTATION.


  METHOD cargar_excel.

    " Cargar archico de excel, permite carga de columnas tipo string
    CALL FUNCTION 'ZCXR1002_ALSM_EXCEL_TO_ITABLE'
      EXPORTING
        i_filename              = i_filename
        i_begin_col             = i_begin_col
        i_begin_row             = i_begin_row
        i_end_col               = i_end_col
        i_end_row               = i_end_row
        i_sheet                 = i_n_hoja
      IMPORTING
        e_ti_intern             = r_ti_datos
      EXCEPTIONS
        inconsistent_parameters = 1
        upload_ole              = 2
        OTHERS                  = 3.

    IF sy-subrc IS NOT INITIAL AND sy-subrc NE 2.
      RAISE EXCEPTION TYPE cx_t100_msg
        EXPORTING
*         textid     =
*         previous   =
          t100_msgid = 'err_cagr_excel'
*         t100_msgno =
          t100_msgv1 = |{ i_filename }|
*         t100_msgv2 =
*         t100_msgv3 =
*         t100_msgv4 =
        .

    ENDIF.

  ENDMETHOD.

  METHOD matchcode_csv.

    DATA: rc             TYPE i,
          ti_lst_archivo TYPE tp_ti_file_table.

    cl_gui_frontend_services=>file_open_dialog(
      EXPORTING
        window_title   = CONV #( TEXT-001 )
        file_filter    = | { TEXT-002 } (*.csv;*.txt)|
        multiselection = abap_false
      CHANGING
        file_table     = ti_lst_archivo
        rc             = rc ).

    r_ruta_archivo = VALUE #( ti_lst_archivo[ 1 ] OPTIONAL ).

  ENDMETHOD.

  METHOD matchcode_excel.

    DATA: l_rc TYPE i.

    cl_gui_frontend_services=>file_open_dialog(
      EXPORTING
        window_title   = CONV #( TEXT-001 )
        file_filter    = | { TEXT-002 } (*.xls;*.xlsx)|
        multiselection = abap_false
      CHANGING
        file_table     = r_ti_lst_archivo
        rc             = l_rc ).

  ENDMETHOD.


  METHOD cargar_excel_ft_carga.

    DATA: r_col_str    TYPE RANGE OF zclcxr1002_util=>gtp_es_tabla-campo,
          ti_tab_estr  TYPE zclcxr1002_util=>gtp_ti_estructura_tabla,
          ti_datos_cab TYPE zttcxr1002_2,
          o_ttdescribe TYPE REF TO cl_abap_tabledescr,
          o_stdescribe TYPE REF TO cl_abap_structdescr,
          o_eldescribe TYPE REF TO cl_abap_elemdescr,
          es_dfies     TYPE dfies.

    DATA(o_log) = zclcxr1002_log_aplicacion=>get_instancia( ).

    IF i_nom_archivo IS INITIAL.
      RAISE EXCEPTION TYPE cx_t100_msg EXPORTING textid = 'cx_t100_msg=>falta_n_fch'.
    ENDIF.

    "convertir ncolumnas en rango
    zclcxr1002_util=>crear_rango(
      EXPORTING
        i_ti       = i_ti_col_str
      CHANGING
        c_ti_rango = r_col_str ).

    TRY.
        "carga archivo a itab
        IF c_ti_datos IS INITIAL.
          "cargar encabezado
          ti_datos_cab = cargar_excel( i_filename  = i_nom_archivo
                                       i_n_hoja    = i_n_hoja
                                       i_begin_row = 2
                                       i_end_row   = i_fila_ini_campo
                                       i_end_col   = i_end_col ).
          c_ti_datos = cargar_excel( i_filename = i_nom_archivo i_n_hoja = i_n_hoja i_begin_row = i_n_fila i_end_col = i_end_col ).
        ENDIF.

        IF i_r_ti_data IS NOT INITIAL.
          o_ttdescribe ?= cl_abap_structdescr=>describe_by_data_ref( i_r_ti_data ).
          o_stdescribe ?= o_ttdescribe->get_table_line_type( ).

          DATA(ti_component) = o_stdescribe->get_components( ).
          "busca estructuras anidadas
          LOOP AT ti_component INTO DATA(es_component).
            CLEAR: es_dfies.
            " recupera descripcion del tipo
            TRY.
                o_eldescribe ?= es_component-type.
              CATCH cx_root.
                CONTINUE.
            ENDTRY.

            IF o_eldescribe->is_ddic_type( ) EQ abap_true.
              es_dfies = o_eldescribe->get_ddic_field( ).
            ELSE.
              es_dfies = VALUE #( leng = o_eldescribe->length decimals = o_eldescribe->decimals ).
              es_dfies-datatype = es_dfies-inttype = es_dfies-comptype = o_eldescribe->type_kind.
            ENDIF.

            APPEND VALUE #( campo = es_dfies-tabname nombre = es_component-name o_descripcion_elemento = o_eldescribe  ) TO ti_tab_estr.

          ENDLOOP.

          r_ti_datos = i_r_ti_data.
        ELSE.
          "si se envio el parametro c_ti_datos, y no la referencia a la cabecera
*   se debe construir la cabecera y eliminarla de c_ti_datos
          IF ti_datos_cab IS INITIAL.
            ti_datos_cab = c_ti_datos.
            DELETE ti_datos_cab WHERE row > i_n_fila.
            DELETE c_ti_datos WHERE row < i_n_fila.
          ENDIF.
          "recupera el indice en donde inicia los elementos de datos y la estructura
          DATA(inx_campo) = line_index( ti_datos_cab[ row = 2 ] ).
          "extrae campo y estructura para crear tabla interna dinamica
          ti_tab_estr = VALUE zclcxr1002_util=>gtp_ti_estructura_tabla(  LET inx_sec_d = inx_campo  IN
                                                                   FOR i =  2  WHILE i < inx_campo
                                                                   LET inx_sec = incrementar_val( CHANGING c_valor = inx_sec_d ) IN
                                                                   ( estructura = ti_datos_cab[ i ]-value
                                                                     campo  = COND #( WHEN ( line_exists( i_ti_col_str[ table_line = cte_td_str ] ) OR
                                                                                            ( ti_datos_cab[ inx_sec ]-value IN r_col_str AND r_col_str IS NOT INITIAL ) )
                                                                                      THEN cte_string
                                                                                      ELSE ti_datos_cab[ inx_sec ]-value )
                                                                     nombre = ti_datos_cab[ inx_sec ]-value )  ).
          "renombra campos con el mismo nombre
          remplazar_comp_repetido( EXPORTING i_cons_estruc = i_cons_estruc CHANGING c_ti_tab_estr = ti_tab_estr ).
          "crea tabla interna dinamica
          r_ti_datos = zclcxr1002_util=>crear_ti_dinamica( CHANGING c_ti_campos = ti_tab_estr ).
        ENDIF.
        " agrega datos de archivo de excel a itab dinamica
        llenar_tabla( EXPORTING i_ti_datos       = c_ti_datos
                                i_ti_tab_estr    = ti_tab_estr
                                i_excl_vcol      = i_excl_vcol
                                i_n_fila         = i_n_fila
                                i_n_col          = i_n_col
                                i_conv_num_dec   = i_conv_num_dec
                                i_col_id         = i_col_id
                                i_fila_ini_campo = i_fila_ini_campo
                      CHANGING  c_ti_datos       = r_ti_datos ).

      CATCH cx_sy_struct_creation INTO DATA(o_cx_st).
        o_log->set_es_log( VALUE #( type = zclcxr1002_util=>gc_e id = cte_cl_msg  number = '014' message_v1 = o_cx_st->component_name ) ).
      CATCH cx_root INTO o_cx.
        o_log->set_es_log( VALUE #( type = zclcxr1002_util=>gc_e message = COND #( WHEN o_cx->get_longtext( ) IS NOT INITIAL
                                                                                      THEN o_cx->get_longtext( )
                                                                                      ELSE o_cx->get_text( ) )  ) ).
    ENDTRY.

  ENDMETHOD.


  METHOD incrementar_val.

    ADD i_val_incr TO c_valor.
    r_valor = c_valor.

  ENDMETHOD.


  METHOD remplazar_comp_repetido.

    DATA: indx      TYPE i,
          regex_s   TYPE string VALUE '_([0-9]*)$',
          str_campo TYPE string.

    LOOP AT c_ti_tab_estr ASSIGNING FIELD-SYMBOL(<fs_es_tab_estr>).
      "remplaza nombres del tipo XXXX_##
      str_campo = <fs_es_tab_estr>-campo.
      CONDENSE str_campo NO-GAPS.

      IF i_cons_estruc EQ abap_false."conserva el nombre del campo que viene en el archivo
        REPLACE ALL OCCURRENCES OF REGEX regex_s IN str_campo WITH space.
      ENDIF.

      <fs_es_tab_estr>-campo = str_campo.
    ENDLOOP.

    LOOP AT c_ti_tab_estr ASSIGNING <fs_es_tab_estr>.

      indx = 1.

      LOOP AT c_ti_tab_estr ASSIGNING FIELD-SYMBOL(<fs_es_tab_estr_dl>) FROM ( sy-tabix + 1 )
                                                                     WHERE nombre = <fs_es_tab_estr>-nombre.
        <fs_es_tab_estr_dl>-nombre = |{ <fs_es_tab_estr_dl>-campo }{ indx }|.
        ADD 1 TO indx.

      ENDLOOP.
    ENDLOOP.

  ENDMETHOD.


  METHOD llenar_tabla.

    DATA: r_data    TYPE REF TO data,
          col_pos   TYPE i,
          col_vz    TYPE i, "columana que se enia al log
          fila_id   TYPE string,
          o_desc    TYPE REF TO cl_abap_elemdescr,
          n_fila_in TYPE i, "Linea de inicio para asigancion de valores
          n_fl_dato TYPE i,
          long_val  TYPE i.

    DATA(o_log) = zclcxr1002_log_aplicacion=>get_instancia( ).

    FIELD-SYMBOLS: <fs_ti_datos> TYPE ANY TABLE,
                   <fs_es_datos> TYPE any,
                   <fs_valor>    TYPE any.

    zclcxr1002_util=>asignar_porcentaje_br_progreso( i_porcentaje = 10 i_texto = CONV #( TEXT-005 ) ).

    "asigna valores a rango de exclusion, no verifica valores
    IF i_excl_vcol IS NOT INITIAL.
      MODIFY i_excl_vcol FROM VALUE #( sign = zclcxr1002_util=>gc_i option = zclcxr1002_util=>gc_eq )
                         TRANSPORTING sign option
                         WHERE sign = space AND option = space.
    ENDIF.
    "determina linea de inicio
    n_fl_dato = lines( i_ti_datos ).

    ASSIGN c_ti_datos->* TO <fs_ti_datos>.
    "recorrer datos del archivo para asignar a itab
    LOOP AT i_ti_datos INTO DATA(es_datos)."FROM n_fila_in.
      "solo muestra avances cada 25%, esto para optimizar el rendimiento
      IF ( ( sy-tabix * 100 ) / n_fl_dato ) MOD 25 = 0 OR sy-tabix = 1.
        zclcxr1002_util=>asignar_porcentaje_br_progreso( i_porcentaje = ( ( sy-tabix * 100 ) / n_fl_dato ) i_texto = CONV #( TEXT-005 ) ).
      ENDIF.
      col_vz = COND #( WHEN i_fila_ini_campo = 0
                       THEN line_index( i_ti_tab_estr[ nombre = i_col_id ] )
                       ELSE es_datos-col - ( i_n_col - 1 ) ).

      AT NEW row.
        " cada nuva fila crea una linea en la tabla interna.
        FREE: r_data.
        CREATE DATA r_data LIKE LINE OF <fs_ti_datos>.
        ASSIGN r_data->* TO <fs_es_datos>.
        "determina la columna id
        IF i_fila_ini_campo NE 0.
          col_pos = SWITCH #( i_col_id WHEN space THEN i_n_col
*                                       ELSE i_ti_datos[ row = i_fila_ini_campo value = i_col_id ]-col ).
                                       ELSE line_index( i_ti_tab_estr[ campo = i_col_id ] ) + ( i_n_col - 1 ) ).
        ELSE.
          col_pos = col_vz.
        ENDIF.
        "asigna identificador de la fila
        TRY.
            fila_id = i_ti_datos[ row = es_datos-row col = col_pos ]-value.
          CATCH cx_sy_itab_line_not_found.
            "en caso de que la linea indicada no tenga valor, se asigna una referencia de linea vacia
            fila_id = space.
        ENDTRY.
      ENDAT.

*      IF es_datos-col > 2.
      IF es_datos-col >= i_n_col.
        TRY.
            col_pos = es_datos-col - ( i_n_col - 1 )."determina columna en la que inician los datos
            ASSIGN COMPONENT col_pos OF STRUCTURE <fs_es_datos> TO <fs_valor>.

            IF sy-subrc IS INITIAL AND es_datos-value IS NOT INITIAL.
              "recupera información del tipo de dato
              o_desc ?= i_ti_tab_estr[ col_pos ]-o_descripcion_elemento.
              "verifica ajuste para valones numericos con decimales
              IF i_ti_tab_estr[ col_pos ]-o_descripcion_elemento->decimals > 0 AND i_conv_num_dec EQ abap_true.
                REPLACE ALL OCCURRENCES OF ',' IN es_datos-value WITH '.'.
              ENDIF.
              "valida la longitud del campo destino antes de realizar la asigancion
              long_val = i_ti_tab_estr[ col_pos ]-o_descripcion_elemento->output_length.
              IF long_val > 0 AND es_datos-value IS NOT INITIAL AND long_val < strlen( es_datos-value ).
                es_datos-value = es_datos-value(long_val).
              ENDIF.

              IF i_ti_tab_estr[ col_pos ]-o_descripcion_elemento->edit_mask IS NOT INITIAL.
                "aplica rutina de conversion
                zclcxr1002_util=>aplicar_conversion_campo(
                  EXPORTING
                    i_exit  = CONV #( i_ti_tab_estr[ col_pos ]-o_descripcion_elemento->edit_mask )
                    i_fila  = CONV i( es_datos-row )
                    i_valor = es_datos-value
                  CHANGING
                    c_valor = <fs_valor> ).
              ELSE.
                <fs_valor> = es_datos-value.
              ENDIF.
              "valida dominio si aplica
              IF o_desc->is_ddic_type( )  AND
                 ( es_datos-col NOT IN i_excl_vcol OR i_excl_vcol IS INITIAL ) AND
                 zclcxr1002_util=>validar_dominio( i_dominio = o_desc->get_ddic_field( )-domname
                                                   i_valor = CONV #( <fs_valor> ) ) IS INITIAL.
                <fs_valor> = space.
              ENDIF.

            ENDIF.
          CATCH cx_t100_msg INTO o_zcx_gen.
            o_log->set_es_log( VALUE zclcxr1002_log_aplicacion=>gtp_es_log( type = zclcxr1002_util=>gc_e row = es_datos-row
                                                                 columna = col_vz
                                                                 fila_descripcion = fila_id
                                                                 message = o_zcx_gen->get_longtext( )  ) ).
          CATCH cx_root INTO o_cx.
            o_log->set_es_log( VALUE zclcxr1002_log_aplicacion=>gtp_es_log( type = zclcxr1002_util=>gc_e row = es_datos-row
                                                                 columna = col_vz
                                                                 fila_descripcion = fila_id
                                                                 message = o_cx->get_longtext( )  ) ).
        ENDTRY.
      ENDIF.

      AT END OF row.
        INSERT <fs_es_datos> INTO TABLE <fs_ti_datos>.
      ENDAT.

    ENDLOOP.

  ENDMETHOD.


  METHOD matchcode_directorio.

    CALL METHOD cl_gui_frontend_services=>directory_browse
      EXPORTING
        window_title    = CONV #( TEXT-004 )
        initial_folder  = cte_directorio
      CHANGING
        selected_folder = r_s_directorio.

    CALL METHOD cl_gui_cfw=>flush.

  ENDMETHOD.


  METHOD descargar_tabla_excel.

    DATA lv_filename       TYPE string.
    DATA lv_return_code    TYPE i.
    DATA lv_error_code     TYPE subrc.
    DATA ti_datos TYPE REF TO data.
    FIELD-SYMBOLS <fs_table> TYPE STANDARD TABLE.

    GET REFERENCE OF i_ti_datos INTO ti_datos.

    ASSIGN ti_datos->* TO <fs_table>.
    " casteamos la ruta completa a string
    lv_filename = | { i_filename } '.' { i_ext } |.
    " Eliminamos el archivo si existe
    CALL METHOD cl_gui_frontend_services=>file_delete
      EXPORTING
        filename       = lv_filename
      CHANGING
        rc             = lv_return_code
      EXCEPTIONS
        file_not_found = 1
        OTHERS         = 2.

    lv_error_code = sy-subrc.
    " si lo elimina o no existe el archivo
    IF lv_error_code = 0 OR
       lv_error_code = 1.

      IF <fs_table> IS ASSIGNED.
        ". creamos el archivo de excel
        CALL FUNCTION 'SAP_CONVERT_TO_XLS_FORMAT'
          EXPORTING
            i_line_header  = 'X'
            i_filename     = lv_filename
          TABLES
            i_tab_sap_data = <fs_table>.

      ENDIF.
    ENDIF.
  ENDMETHOD.


  METHOD cargar_plano.

    DATA: es_datos TYPE string,
          c_ruta   TYPE string.

    IF i_c_servidor IS INITIAL.

      IF i_c_interfaz IS NOT INITIAL.

        c_ruta = i_c_filename.

        CALL FUNCTION 'GUI_UPLOAD'
          EXPORTING
            filename                = c_ruta
            filetype                = 'ASC'
            has_field_separator     = i_c_separador
            header_length           = 0
            read_by_line            = abap_true
            ignore_cerr             = abap_true
*           replacement             = '#'
          TABLES
            data_tab                = e_ti_estruc
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

*          cx_t100_msg=>prp_sy_exception( ).

          RAISE EXCEPTION TYPE cx_t100_msg
            EXPORTING
              textid     = 'err_cagr_plano'
              t100_msgv1 = c_ruta.

        ENDIF.

      ELSE.

        c_ruta = i_c_filename.

        CALL FUNCTION 'GUI_UPLOAD'
          EXPORTING
            filename                = c_ruta
            filetype                = 'ASC'
            has_field_separator     = i_c_separador
            header_length           = 0
            read_by_line            = abap_true
            ignore_cerr             = abap_true
            replacement             = '#'
            codepage                = '4110'
          TABLES
            data_tab                = e_ti_datos
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

*          cx_t100_msg=>prp_sy_exception( ).
          RAISE EXCEPTION TYPE cx_t100_msg
            EXPORTING
              textid     = 'err_cagr_plano'
              t100_msgv1 = c_ruta.

        ENDIF.

      ENDIF.

    ELSE.

*      Obtener archivo de servidor de aplicación.
      OPEN DATASET i_c_filename FOR INPUT IN TEXT MODE ENCODING NON-UNICODE .
      IF sy-subrc = 0.
        DO.
          READ DATASET i_c_filename INTO es_datos.
          IF sy-subrc EQ 0.
            APPEND es_datos TO e_ti_datos.
          ELSE.
            EXIT.
          ENDIF.
        ENDDO.
      ELSE.
        RAISE EXCEPTION TYPE cx_t100_msg
          EXPORTING
            textid     = 'err_cagr_plano'
            t100_msgv1 = CONV #( i_c_filename ).
      ENDIF.
      CLOSE DATASET i_c_filename.

    ENDIF.

  ENDMETHOD.


  METHOD mover_archivo_servidor.

    DATA: c_datos TYPE string.

    CONSTANTS: cte_clmsg TYPE arbgb VALUE 'ZTSW01'.

    DATA(o_log) = zclcxr1002_log_aplicacion=>get_instancia( ).

*    Leer datos de origen.
    OPEN DATASET i_c_origen FOR INPUT IN TEXT MODE ENCODING NON-UNICODE.
    IF sy-subrc = 0.

*      Abrir dataset de destino.
      OPEN DATASET  i_c_destino FOR OUTPUT IN TEXT MODE ENCODING NON-UNICODE.
      IF sy-subrc = 0.
        DO.
          READ DATASET i_c_origen INTO c_datos.
          IF sy-subrc = 0.
            TRANSFER c_datos TO i_c_destino.
          ELSE.
            EXIT.
          ENDIF.
        ENDDO.
      ELSE.
        o_log->set_es_log( VALUE #( type = zclcxr1002_util=>gc_e id = cte_clmsg
                                    number = '035' message_v1 = i_c_destino ) ).
      ENDIF.

      CLOSE DATASET i_c_origen.
      CLOSE DATASET i_c_destino.

*      Borrar archivo de origen.
      DELETE DATASET i_c_origen.
      IF sy-subrc EQ 0.
        o_log->set_es_log( VALUE #( type = zclcxr1002_util=>gc_s id = cte_clmsg
                                    number = '032' message_v1 = i_c_origen ) ).
      ELSE.
        o_log->set_es_log( VALUE #( type = zclcxr1002_util=>gc_e id = cte_clmsg
                                    number = '033' message_v1 = i_c_origen ) ).
      ENDIF.
    ELSE.
      o_log->set_es_log( VALUE #( type = zclcxr1002_util=>gc_e id = cte_clmsg
                                  number = '034' message_v1 = i_c_origen ) ).
    ENDIF.

  ENDMETHOD.


  METHOD cargar_excel_multiples_hojas.

    CALL FUNCTION 'ZCXR1002_ALSM_EXCEL_TO_MULTI_T'
      EXPORTING
        i_filename              = i_filename
        i_begin_col             = i_begin_col
        i_begin_row             = i_begin_row
        i_end_col               = i_end_col
        i_end_row               = i_end_row
        i_sheets                = i_n_hojas
      IMPORTING
        e_ti_intern             = r_ti_datos
      EXCEPTIONS
        inconsistent_parameters = 1
        upload_ole              = 2
        OTHERS                  = 3.

    IF sy-subrc IS NOT INITIAL AND sy-subrc NE 2.
      RAISE EXCEPTION TYPE cx_t100_msg
        EXPORTING
          t100_msgid = 'err_cagr_excel'
          t100_msgv1 = |{ i_filename }|.
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

  METHOD descomprimir_archivo_zip.
    DATA: o_zip                 TYPE REF TO cl_abap_zip,
          longitud_archivo      TYPE i,
          xtab                  TYPE TABLE OF x255,
          contenido_archivo_str TYPE string,
          contenido_archivo     TYPE xstring,
          xhead                 TYPE xstring,
          zip_content           TYPE xstring.

    CALL FUNCTION 'SCMS_BINARY_TO_XSTRING'
      EXPORTING
        input_length = longitud_archivo
      IMPORTING
        buffer       = xhead
      TABLES
        binary_tab   = i_ti_archivo_zip
      EXCEPTIONS
        failed       = 1
        OTHERS       = 2.

    IF sy-subrc <> 0 OR xhead IS INITIAL.
      IF o_zip IS NOT INITIAL.
        FREE o_zip.
      ENDIF.
    ENDIF.

    o_zip = NEW cl_abap_zip(  ).

    o_zip->load( xhead ).

    LOOP AT o_zip->files INTO DATA(es_archivo).

      CLEAR: contenido_archivo, contenido_archivo_str.

      o_zip->get( EXPORTING name    = es_archivo-name
                  IMPORTING content = contenido_archivo ).

      CALL FUNCTION 'SCMS_XSTRING_TO_BINARY'
        EXPORTING
          buffer        = contenido_archivo
        IMPORTING
          output_length = longitud_archivo
        TABLES
          binary_tab    = xtab.

      CALL FUNCTION 'SCMS_BINARY_TO_STRING'
        EXPORTING
          input_length = longitud_archivo
        IMPORTING
          text_buffer  = contenido_archivo_str
        TABLES
          binary_tab   = xtab
        EXCEPTIONS
          failed       = 1
          OTHERS       = 2.

      IF sy-subrc <> 0.
        CONTINUE.
      ENDIF.

      APPEND VALUE #( nombre = es_archivo-name contenido_str = contenido_archivo_str )
      TO e_ti_contenido_zip.

*      DO.
*        IF contenido_archivo_str CA cl_abap_char_utilities=>cr_lf.
*          SPLIT contenido_archivo_str AT cl_abap_char_utilities=>cr_lf INTO es_contenido_archivo contenido_archivo_str.
*          APPEND es_contenido_archivo TO ti_contenido_archivo.
*        ELSE.
*          EXIT.
*        ENDIF.
*      ENDDO.
*      es_contenido_archivo = contenido_archivo_str.
*      APPEND es_contenido_archivo TO ti_contenido_archivo.

    ENDLOOP.

  ENDMETHOD.

ENDCLASS.
CLASS zclcxr1006_gestion_tablas_z DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    TYPES: BEGIN OF gtp_es_registro_modificado,
             alv_indx LIKE sy-tabix,
             indx     LIKE sy-tabix,
             type     TYPE char1,
             used     TYPE char1,
             save     LIKE sy-tabix,
           END OF gtp_es_registro_modificado,

           gtp_ti_registro_modificado TYPE STANDARD TABLE OF gtp_es_registro_modificado.

    CONSTANTS: gc_nombre_campo_consecutivo_1 TYPE string VALUE 'CONSECUTIVO',
               gc_nombre_campo_ricefw        TYPE string VALUE 'RICEFW'.

    CLASS-METHODS:
      refrescar_alv_grid
        CHANGING
          c_o_custom_container TYPE REF TO cl_gui_custom_container
          c_o_alv_grid         TYPE REF TO cl_gui_alv_grid,
      "! <strong>Descripción:</strong>
      "! <p>
      "! Dada una calve primaria del tipo CONSECUTIVO en una tabla Z, este metodo permite insertar un consecutivo nuevo
      "! en cualquier posicion de la tabla, y recalcula los demas consecutivos. Teniendo en cuenta las siguientes premisas:<br/>
      "! <ol>
      "! <li>Solo aplica para transaccion ZCXR1006_1</li>
      "! <li>Debe existir una columna tipo numerica con el nombre CONSECUTIVO</li>
      "! <li>La clave primaria puede estar compuesta unicamente por el campo RICEFW</li>
      "! <li>Solo se recalculan consecutivos en bloques fijos de numeros ejemplo: insertar en posicion 3,4,5 y NO 3,7,6 </li>
      "! </ol>
      "! </p>
      "! <strong>Identificador:</strong> R1006
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 18.09.2021  \ S4DK900384 \ Sebastian Londono \ Creacion Inicial \
      "!
      "! @parameter i_nombre_tabla | Nombre tabla Z
      "! @parameter i_ricefw | Numero de RICEFW - Opcional
      "! @parameter c_ti_datos_completos | Datos completos de la tabla Z
      "! @parameter c_ti_datos_iniciales | Datos iniciales de la tabla Z
      "! @parameter c_ti_registro_modificados | Registros modificados durante el tratameinto
      "! @parameter c_es_datos_control | Datos de control de la tabla
      "! @parameter c_ti_datos_db | Datos consultados de la base de datos, antes de las modificaciones
      ajustar_consecutivo
        IMPORTING
          VALUE(i_ti_catalogo)      TYPE lvc_t_fcat
          i_nombre_tabla            TYPE string
          i_ricefw                  TYPE zzedricefw OPTIONAL
        CHANGING
          c_ti_datos_completos      TYPE table
          c_ti_datos_iniciales      TYPE table
          c_ti_registro_modificados TYPE gtp_ti_registro_modificado
          c_es_datos_control        TYPE any
          c_ti_datos_db             TYPE ANY TABLE,
      "! <strong>Descripción:</strong>
      "! <p>
      "! Dada una calve primaria del tipo CONSECUTIVO en una tabla Z, este metodo permite determinar el ultimo consecutivo
      "! de una tabla Z. Teniendo en cuenta las siguientes premisas:<br/>
      "! <ol>
      "! <li>Solo aplica para transaccion ZCXR1006_1</li>
      "! <li>Debe existir una columna tipo numerica con el nombre CONSECUTIVO</li>
      "! <li>La clave primaria puede estar compuesta unicamente por el campo RICEFW, en este caso el consecutivo se calcula agrupado</li>
      "! </ol>
      "! </p>
      "! <strong>Identificador:</strong> R1006
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 18.09.2021  \ S4DK900384 \ Sebastian Londono \ Creacion Inicial \
      "!
      "! @parameter c_ti_datos_completos | Datos completos de la tabla Z
      "! @parameter c_ti_datos_iniciales | Datos iniciales de la tabla Z
      "! @parameter c_ti_registro_modificados | Registros modificados durante el tratameinto
      "! @parameter c_es_datos_control | Datos de control de la tabla
      determinar_consecutivo
        IMPORTING
          VALUE(i_ti_catalogo)      TYPE lvc_t_fcat
        CHANGING
          c_ti_datos_completos      TYPE table
          c_ti_datos_iniciales      TYPE table
          c_ti_registro_modificados TYPE gtp_ti_registro_modificado
          c_es_datos_control        TYPE any
          c_ti_datos_eliminados     TYPE table,
      escribir_log_tabla
        IMPORTING
          i_nombre_tabla       TYPE string
          VALUE(i_ricefw)      TYPE zzedricefw OPTIONAL
        CHANGING
          c_ti_datos_completos TYPE table
          c_ti_datos_iniciales TYPE table
          c_ti_datos_db        TYPE ANY TABLE OPTIONAL.

  PROTECTED SECTION.
  PRIVATE SECTION.

    CONSTANTS: gc_codigo_transaccion_z TYPE string VALUE 'ZCXR1006_1'.
    CLASS-DATA: g_modificar_bd           TYPE flag,
                gti_registro_modificados TYPE gtp_ti_registro_modificado.

    CLASS-METHODS obtener_ricefw
      CHANGING
        c_ti_datos_completos      TYPE table
        c_ti_registro_modificados TYPE gtp_ti_registro_modificado
        c_str_where_datos_bd      TYPE string OPTIONAL
        c_ricefw                  TYPE zzedricefw.

ENDCLASS.



CLASS zclcxr1006_gestion_tablas_z IMPLEMENTATION.


  METHOD determinar_consecutivo.

    DATA: r_tabla                       TYPE REF TO data,
          str_where                     TYPE string,
          str_where_datos_bd            TYPE string,
          str_where_registro_modificado TYPE string,
          ricefw                        TYPE zzedricefw,
          consecutivo_encontrado        TYPE flag,
          consecutivo                   TYPE i.

    FIELD-SYMBOLS: <fs_ti_datos_bd>        TYPE ANY TABLE,
                   <fs_ultimo_consecutivo> TYPE any.

    ASSIGN COMPONENT 'TAB' OF STRUCTURE c_es_datos_control TO FIELD-SYMBOL(<fs_nombre_tablas>).
    CHECK <fs_nombre_tablas> IS ASSIGNED.

    CHECK sy-tcode = gc_codigo_transaccion_z AND <fs_nombre_tablas>(1) = 'Z'
          AND c_ti_datos_completos IS NOT INITIAL.

    gti_registro_modificados = c_ti_registro_modificados.

    LOOP AT c_ti_datos_eliminados ASSIGNING FIELD-SYMBOL(<fs_es_datos_eliminados>).
      ASSIGN COMPONENT 'LINE_INDEX' OF STRUCTURE <fs_es_datos_eliminados> TO FIELD-SYMBOL(<fs_indice_reg_eliminado>).

      CHECK sy-subrc IS INITIAL.

      APPEND VALUE #( indx = <fs_indice_reg_eliminado> type = 'D' )
      TO gti_registro_modificados.
    ENDLOOP.

    "Valida si la tabla tiene el campo RICEFW
    obtener_ricefw( CHANGING c_ti_datos_completos = c_ti_datos_completos
                             c_ti_registro_modificados = c_ti_registro_modificados
                             c_str_where_datos_bd = str_where_datos_bd
                             c_ricefw             = ricefw ).

    str_where = |{ gc_nombre_campo_consecutivo_1 } IS INITIAL|.

    TRY."valid si el campo consecutivo existe
        LOOP AT c_ti_datos_completos TRANSPORTING NO FIELDS WHERE (str_where).
          EXIT.
        ENDLOOP.

        consecutivo_encontrado = abap_true.

      CATCH cx_sy_itab_dyn_loop.

    ENDTRY.

    IF consecutivo_encontrado = abap_true.

      CREATE DATA r_tabla TYPE STANDARD TABLE OF (<fs_nombre_tablas>).
      CHECK r_tabla IS NOT INITIAL.
      ASSIGN r_tabla->* TO <fs_ti_datos_bd>.

      CHECK <fs_ti_datos_bd> IS ASSIGNED.
      "Consulta datos de tabla incluyendo RICEFW si la estructura de la tabla lo tiene
      SELECT *
      FROM (<fs_nombre_tablas>)
      WHERE (str_where_datos_bd)
      INTO TABLE @<fs_ti_datos_bd>.

      "selecciona el ultimo valor del consecutivo
      SORT <fs_ti_datos_bd> DESCENDING BY (gc_nombre_campo_consecutivo_1).

      LOOP AT <fs_ti_datos_bd> ASSIGNING FIELD-SYMBOL(<fs_es_datos_bd>).
        ASSIGN COMPONENT gc_nombre_campo_consecutivo_1 OF STRUCTURE <fs_es_datos_bd> TO <fs_ultimo_consecutivo>.
        EXIT.
      ENDLOOP.

      IF <fs_ultimo_consecutivo> IS NOT ASSIGNED.
        ASSIGN consecutivo TO <fs_ultimo_consecutivo>.
      ENDIF.

      ajustar_consecutivo( EXPORTING
                              i_ti_catalogo            = i_ti_catalogo
                              i_nombre_tabla           = CONV #( <fs_nombre_tablas> )
                              i_ricefw                 = ricefw
                           CHANGING
                              c_ti_datos_completos      = c_ti_datos_completos
                              c_ti_datos_iniciales      = c_ti_datos_iniciales
                              c_ti_registro_modificados = c_ti_registro_modificados
                              c_es_datos_control        = c_es_datos_control
                              c_ti_datos_db             = <fs_ti_datos_bd>  ).

      LOOP AT c_ti_datos_completos ASSIGNING FIELD-SYMBOL(<fs_es_datos_completos>) WHERE (str_where).

        ASSIGN COMPONENT 'LINE_INDEX' OF STRUCTURE <fs_es_datos_completos> TO FIELD-SYMBOL(<fs_indice_reg_mod>).
        CHECK sy-subrc IS INITIAL.

        str_where_registro_modificado = |INDX = { <fs_indice_reg_mod> }|.

        LOOP AT c_ti_registro_modificados ASSIGNING FIELD-SYMBOL(<fs_es_registro_modificados>) WHERE (str_where_registro_modificado).
          EXIT.
        ENDLOOP.

        IF sy-subrc IS INITIAL.

          ASSIGN COMPONENT gc_nombre_campo_consecutivo_1 OF STRUCTURE <fs_es_datos_completos> TO FIELD-SYMBOL(<fs_concecutivo>).

          CHECK sy-subrc IS INITIAL.

          ADD 1 TO <fs_ultimo_consecutivo>.

          <fs_concecutivo> = <fs_ultimo_consecutivo>.

        ENDIF.

      ENDLOOP.

    ENDIF.

  ENDMETHOD.

  METHOD refrescar_alv_grid.

    CHECK c_o_alv_grid IS BOUND AND sy-tcode = gc_codigo_transaccion_z.

    c_o_alv_grid->get_selected_cells_id( IMPORTING et_cells = DATA(ti_celda) ).

    TRY.
        DATA(es_celda) = ti_celda[ 1 ].
      CATCH cx_sy_itab_line_not_found.

    ENDTRY.

    c_o_alv_grid->refresh_table_display( is_stable = VALUE #( row = es_celda-row_id  col = es_celda-col_id )
                                       i_soft_refresh = abap_true ).

    IF g_modificar_bd = abap_true.
      c_o_custom_container->free( ).
      cl_gui_cfw=>flush( ).

      SET SCREEN 0.
      LEAVE SCREEN.
    ENDIF.

  ENDMETHOD.

  METHOD ajustar_consecutivo.

    DATA: str_where             TYPE string,
          str_where_eliminar_bd TYPE string,
          consecutivo_mayor     TYPE i,
          consecutivo_menor     TYPE i,
          primera_iteracion     TYPE flag,
          iniciar_incremento    TYPE flag,
          r_ti_datos_eliminar   TYPE REF TO data.

    FIELD-SYMBOLS: <fs_ti_datos_bd_eliminar> TYPE ANY TABLE,
                   <fs_consecutivo_asignar>  TYPE any,
                   <fs_consecutivo_cambiar>  TYPE any.

    g_modificar_bd = abap_false.

    DELETE i_ti_catalogo WHERE key = space.

    CREATE DATA r_ti_datos_eliminar LIKE c_ti_datos_db.
    ASSIGN r_ti_datos_eliminar->* TO <fs_ti_datos_bd_eliminar>.

    "consultar el ultimo y primer consecutivo insertado
    LOOP AT c_ti_registro_modificados ASSIGNING FIELD-SYMBOL(<fs_es_registro_modificados>) WHERE type = 'I'.

      str_where =   |LINE_INDEX = { <fs_es_registro_modificados>-indx } AND { gc_nombre_campo_consecutivo_1 } IS NOT INITIAL |.

      "Consultar consecutivo modificado
      LOOP AT c_ti_datos_completos ASSIGNING FIELD-SYMBOL(<fs_es_datos_completos>) WHERE (str_where).

        ASSIGN COMPONENT gc_nombre_campo_consecutivo_1 OF STRUCTURE <fs_es_datos_completos>
        TO <fs_consecutivo_asignar>.

        CHECK sy-subrc IS INITIAL.

        IF primera_iteracion = abap_false.
          consecutivo_menor = <fs_consecutivo_asignar>.
          primera_iteracion = abap_true.
        ENDIF.

        "Registros que se deben eliminar
*        str_where_eliminar_bd = str_where_eliminar_bd && | OR ( { gc_nombre_campo_consecutivo_1 } = '{ <fs_consecutivo_asignar> }'| &&
*                                |{ COND #( WHEN i_ricefw IS NOT INITIAL THEN | AND { gc_nombre_campo_ricefw } = '{ i_ricefw }' )| ELSE ' )' ) }|.

        IF consecutivo_menor > <fs_consecutivo_asignar>.
          consecutivo_menor = <fs_consecutivo_asignar>.
        ENDIF.

        IF consecutivo_mayor < <fs_consecutivo_asignar>.
          consecutivo_mayor = <fs_consecutivo_asignar>.
        ENDIF.

        EXIT.
      ENDLOOP.
    ENDLOOP.

    CHECK <fs_consecutivo_asignar> IS ASSIGNED.

    "Se modifican los consegutivos segun el primero que deba ajustarse
    SORT c_ti_datos_db ASCENDING BY (gc_nombre_campo_consecutivo_1).

    LOOP AT c_ti_datos_db ASSIGNING FIELD-SYMBOL(<fs_es_datos_db>).

      ASSIGN COMPONENT gc_nombre_campo_consecutivo_1 OF STRUCTURE <fs_es_datos_db>
      TO <fs_consecutivo_cambiar>.
      CHECK sy-subrc IS INITIAL.

      IF  <fs_consecutivo_cambiar> = consecutivo_menor OR iniciar_incremento = abap_true.
        INSERT <fs_es_datos_db> INTO TABLE <fs_ti_datos_bd_eliminar>.

        ADD 1 TO consecutivo_mayor.
        <fs_consecutivo_cambiar> = consecutivo_mayor.
        g_modificar_bd = iniciar_incremento = abap_true.

      ENDIF.

    ENDLOOP.

    IF g_modificar_bd = abap_true.

      REPLACE FIRST OCCURRENCE OF 'OR' IN str_where_eliminar_bd WITH space.
      DELETE  (i_nombre_tabla) FROM TABLE <fs_ti_datos_bd_eliminar>.
      COMMIT WORK AND WAIT.
      MODIFY (i_nombre_tabla) FROM TABLE c_ti_datos_db.
*      DELETE FROM (i_nombre_tabla) WHERE (str_where_eliminar_bd).
      COMMIT WORK AND WAIT.
    ENDIF.

  ENDMETHOD.

  METHOD escribir_log_tabla.

    DATA: es_datos_iniciales TYPE ztcxr1002_1,
          es_datos_finales   TYPE ztcxr1002_1,
          accion             TYPE cdchngind,
          id_objeto          TYPE cdobjectv,
          str_where          TYPE string.

    "Esta logica se debe ajustar para tener una mayor capacidad de soporte
    CASE i_nombre_tabla.
      WHEN 'ZTCXR1002_1'.

        LOOP AT gti_registro_modificados ASSIGNING FIELD-SYMBOL(<fs_es_registro_modificados>).
          CLEAR: es_datos_finales, es_datos_iniciales.

          str_where = |LINE_INDEX = { <fs_es_registro_modificados>-indx }|.

          LOOP AT c_ti_datos_completos ASSIGNING FIELD-SYMBOL(<fs_es_datos_completos>) WHERE (str_where).
            es_datos_finales = CORRESPONDING #( <fs_es_datos_completos> ).
            id_objeto = |{ es_datos_finales-ricefw }{ es_datos_finales-consecutivo }|.
          ENDLOOP.

          LOOP AT c_ti_datos_iniciales ASSIGNING FIELD-SYMBOL(<fs_es_datos_iniciales>) WHERE (str_where).
            es_datos_iniciales = CORRESPONDING #( <fs_es_datos_iniciales> ).
            id_objeto = |{ es_datos_iniciales-ricefw }{ es_datos_iniciales-consecutivo }|.
          ENDLOOP.

          accion = SWITCH #( <fs_es_registro_modificados>-type
                                WHEN 'M' THEN
                                  'U'
                                ELSE
                                  <fs_es_registro_modificados>-type
                           ).

          IF accion = 'U'.
            CLEAR: es_datos_finales-ricefw, es_datos_finales-mandt, es_datos_finales-consecutivo.
          ENDIF.

          CALL FUNCTION 'ZCX_ZTCXR1002_1_WRITE_DOCUMENT'
            EXPORTING
              objectid                = id_objeto
              tcode                   = sy-tcode
              utime                   = sy-uzeit
              udate                   = sy-datum
              username                = sy-uname
              n_ztcxr1002_1           = es_datos_finales
              o_ztcxr1002_1           = es_datos_iniciales
              object_change_indicator = COND #( WHEN accion = 'U' THEN accion ELSE space )
              upd_ztcxr1002_1         = accion.

          COMMIT WORK AND WAIT.

        ENDLOOP.
      WHEN 'ZTMMI1009_1'.

        zclfii1008_integracion_afs=>escribir_log_tbl_homologacion(
          EXPORTING
            i_ti_registro_modificados = gti_registro_modificados
          CHANGING
            c_ti_datos_completos      = c_ti_datos_completos
            c_ti_datos_iniciales      = c_ti_datos_iniciales
            c_ti_datos_db             = c_ti_datos_db ).

    ENDCASE.

  ENDMETHOD.


  METHOD obtener_ricefw.
    DATA: str_where TYPE string.

    TRY.
        ASSIGN COMPONENT gc_nombre_campo_ricefw OF STRUCTURE c_ti_datos_completos[ 1 ] TO FIELD-SYMBOL(<fs_ricefw>).

        IF <fs_ricefw> IS ASSIGNED.

          c_ricefw = <fs_ricefw>.

          IF line_exists( c_ti_registro_modificados[ type = 'I' ] ).
            ASSIGN c_ti_registro_modificados[ type = 'I' ] TO FIELD-SYMBOL(<fs_es_registro_modificados>).

            str_where = |LINE_INDEX = { <fs_es_registro_modificados>-indx }|.

            LOOP AT c_ti_datos_completos TRANSPORTING NO FIELDS WHERE (str_where).
              UNASSIGN: <fs_ricefw>.

              ASSIGN COMPONENT gc_nombre_campo_ricefw OF STRUCTURE c_ti_datos_completos[ sy-tabix ]
              TO <fs_ricefw>.

              IF <fs_ricefw> IS ASSIGNED AND <fs_ricefw> IS NOT INITIAL.
                c_str_where_datos_bd = |{ gc_nombre_campo_ricefw } = '{ <fs_ricefw> }' |.
                c_ricefw = <fs_ricefw>.
              ENDIF.

            ENDLOOP.

          ENDIF.
        ENDIF.

      CATCH cx_sy_itab_line_not_found.

    ENDTRY.
  ENDMETHOD.

ENDCLASS.
*&---------------------------------------------------------------------*
*& Include zfic1009ci1_1
*&---------------------------------------------------------------------*

CLASS lcl_carga_activo_fijo IMPLEMENTATION.

  METHOD constructor.
    CLEAR: gti_log_creacion.

    IF o_log IS BOUND. o_log->liberar( ). ENDIF.
    o_log = zclcxr1002_log_aplicacion=>get_instancia( ).

    "consultar parametro: Numero de activos creados para escribir en Log
    nr_escribelog = 10.

  ENDMETHOD.

  METHOD iniciar_proceso.

    DATA: ti_datos_temporales TYPE gtp_ti_datos_temporales,
          ti_datos_cargados   TYPE zclcxr1002_cargar_archivo=>tp_ti_con_name.

    CHECK i_arch IS NOT INITIAL AND i_fondo IS INITIAL.

    CLEAR: c_ti_dato_activofijo.

    IF i_debug = abap_true.
      BREAK-POINT.
    ENDIF.

    IF sy-batch IS INITIAL.


      TRY.
          zclcxr1002_cargar_archivo=>cargar_plano(
            EXPORTING
              i_c_filename = i_arch
            IMPORTING
              e_ti_datos   = ti_datos_cargados ).
        CATCH cx_t100_msg.
          o_log->set_es_log( i_es = VALUE #( id = sy-msgid number = sy-msgno type = sy-msgty message_v1 = sy-msgv1
                                                  message_v2 = sy-msgv2 message_v3 = sy-msgv3 message_v4 = sy-msgv4 ) ).
      ENDTRY.

    ELSE.

      ti_datos_temporales = get_datos_db( i_arch = i_arch ).
      eliminar_reg_temp_db( i_arch = i_arch ).

      ti_datos_cargados = VALUE #( FOR es_datos_temporales IN ti_datos_temporales
                                 ( es_datos_temporales-value ) ).

      IF ti_datos_cargados IS INITIAL.
        o_log->set_es_log( i_es = VALUE #( type = zclcxr1002_util=>gc_e id = zclfic1009_carga_activos_fijos=>gc_cls_msg number = '027' ) ).
      ENDIF.

    ENDIF.

    TRY.
        consolidar_datos( CHANGING c_ti_dato_activofijo = c_ti_dato_activofijo c_ti_datos_archivo = ti_datos_cargados ).
        validar_transformar_datos( CHANGING c_ti_datos = c_ti_dato_activofijo ).
        ejecutar_proceso_en_paralelo( i_ti_dat_activofijo = c_ti_dato_activofijo
                                      i_test              = pa_test
                                      i_debug             = i_debug
                                      i_arch              = i_arch ).

      CATCH cx_root INTO o_cx.
        MESSAGE s208(00) WITH o_cx->get_longtext( ) DISPLAY LIKE 'E'.
    ENDTRY.

  ENDMETHOD.


  METHOD ejecutar_proceso_en_paralelo.
    "filtra registros marcados
    DATA: l_datosmatcab     TYPE i,
          ti_dat_activofijo TYPE zttfic1009_2,
          ti_returnmessages TYPE zttfic1009_1,
          nom_proceso       TYPE string.

    zclcxr1002_util=>asignar_porcentaje_br_progreso( i_porcentaje = 5  i_texto = CONV #( TEXT-001 ) ).

    o_log = zclcxr1002_log_aplicacion=>get_instancia( ).

    CLEAR: gti_log_creacion, g_procesos_enviados, g_procesos_recibidos, g_proceso_act, g_procesos_term.

    l_datosmatcab = lines( i_ti_dat_activofijo ).
    "alade Log de validaciones
    APPEND LINES OF CORRESPONDING zttfic1009_1( o_log->get_log( ) MAPPING anln1 = fila_descripcion )
    TO gti_log_creacion.

    asignar_fecha_hora_log( CHANGING c_ti_log_creacion = gti_log_creacion ).

    LOOP AT i_ti_dat_activofijo ASSIGNING FIELD-SYMBOL(<fs_es_dtmatcab>).

      zclcxr1002_util=>asignar_porcentaje_br_progreso( i_porcentaje = ( ( sy-tabix * 100 ) / l_datosmatcab )  i_texto = CONV #( TEXT-001 ) ).

      APPEND <fs_es_dtmatcab> TO ti_dat_activofijo.

      AT END OF asset.
        "ejecuta procesos paralelos
        g_proceso_act = g_proceso_act + 1.

        nom_proceso = |PROC_CREAACTIVOFIJO_{ g_proceso_act }|.

        IF i_debug = abap_true.
          BREAK-POINT.
          CALL FUNCTION 'ZFIC1009_CARGA_ACTIVOS_FIJOS'
            EXPORTING
              i_test              = i_test
              i_ti_dat_activofijo = ti_dat_activofijo
              i_debug             = i_debug
            IMPORTING
              e_ti_returnmessages = ti_returnmessages.

          gti_log_creacion = CORRESPONDING #( BASE ( gti_log_creacion ) ti_returnmessages ) .
        ELSE.
          CALL FUNCTION 'ZFIC1009_CARGA_ACTIVOS_FIJOS'
            STARTING NEW TASK nom_proceso
            CALLING ejecucion_finalizada ON END OF TASK
            EXPORTING
              i_test                = i_test
              i_ti_dat_activofijo   = ti_dat_activofijo
            EXCEPTIONS
              communication_failure = 1
              system_failure        = 2
              OTHERS                = 3.
        ENDIF.
        IF sy-subrc IS NOT INITIAL.
          o_log->set_es_log( i_es = VALUE #( id = sy-msgid type = sy-msgty number = sy-msgno
                                                  message_v1 = sy-msgv1 message_v2 = sy-msgv2 message_v3 = sy-msgv3
                                                  message_v4 = sy-msgv4   ) ).
        ELSE.
          ADD 1 TO g_procesos_enviados.
        ENDIF.

        CLEAR: ti_dat_activofijo.

        IF gcte_max_proc = g_proceso_act.
          WAIT UNTIL g_procesos_term = gcte_max_proc.
          CLEAR: g_proceso_act, g_procesos_term.
        ENDIF.

      ENDAT.

      IF sy-batch IS NOT INITIAL AND nr_escribelog = g_procesos_recibidos.
        escribir_log_db( EXPORTING i_arch = i_arch CHANGING c_ti_log_creacion = gti_log_creacion ).
        ADD nr_escribelog TO nr_escribelog.
      ENDIF.

    ENDLOOP.
    "verifica que ya se haya recibido todas las respuestas
    WAIT UNTIL g_procesos_recibidos = g_procesos_enviados.

    MESSAGE s025(zfi01).

    IF sy-batch IS NOT INITIAL.
      escribir_log_db( EXPORTING i_arch = i_arch CHANGING c_ti_log_creacion = gti_log_creacion ).
    ELSE.
      gti_log_icon = CORRESPONDING #( gti_log_creacion ).
      zclfic1009_carga_activos_fijos=>presentar_log_crea_activofijo( EXPORTING i_o_grid_log_handler = me
                                                                     CHANGING c_ti_bapireturn = gti_log_icon ).
    ENDIF.

  ENDMETHOD.

  METHOD consolidar_datos.

    TYPES: BEGIN OF tp_es_columnas_archivo,
             nombre_columna TYPE string,
           END OF tp_es_columnas_archivo,

           BEGIN OF tp_es_descripcion_componente,
             name         TYPE string,
             o_eldescribe TYPE REF TO cl_abap_elemdescr,
           END OF tp_es_descripcion_componente.

    DATA: ti_datos_cargados         TYPE zclcxr1002_cargar_archivo=>tp_ti_con_name,
          ti_contenido_linea        TYPE string_t,
          ti_columnas               TYPE STANDARD TABLE OF tp_es_columnas_archivo,
          ti_componentes            TYPE abap_component_tab,
          ti_valores_numero         TYPE string_t,
          ti_descripcion_componente TYPE STANDARD TABLE OF tp_es_descripcion_componente,
          nombre_columna            TYPE string,
          o_ttdescribe              TYPE REF TO cl_abap_tabledescr,
          o_stdescribe              TYPE REF TO cl_abap_structdescr,
          o_eldescribe              TYPE REF TO cl_abap_elemdescr,
          fila_csv                  TYPE i.

    FIELD-SYMBOLS: <fs_es_dato_activofijo>   LIKE LINE OF c_ti_dato_activofijo,
                   <fs_valor_es_activo_fijo> TYPE any.

    DATA(o_homologacion) = NEW zclmmi1009_homologacion_campos( i_c_ricefw = 'C1009' ).

    "recuperar componentes de estructuras
    TRY.
        o_ttdescribe ?= cl_abap_structdescr=>describe_by_data( c_ti_dato_activofijo ).
        o_stdescribe ?= o_ttdescribe->get_table_line_type( ).

        ti_componentes = o_stdescribe->get_components( ).

        LOOP AT ti_componentes INTO DATA(es_component).

          o_eldescribe ?= es_component-type.
          APPEND VALUE #( name = es_component-name o_eldescribe = o_eldescribe )
          TO ti_descripcion_componente.

        ENDLOOP.
      CATCH  cx_sy_move_cast_error cx_root.

    ENDTRY.

    "convertir CSV
    LOOP AT c_ti_datos_archivo ASSIGNING FIELD-SYMBOL(<fs_es_datos_csv>).
      fila_csv = sy-tabix.
      CLEAR: ti_contenido_linea.

      SPLIT <fs_es_datos_csv> AT separador_archivo INTO TABLE ti_contenido_linea.

      IF fila_csv = 1 AND ti_contenido_linea IS NOT INITIAL.
        "Si no se tiene homologacion se debe tener archivo con el nombre exacto de la columna y su equivalencia
        ti_columnas = VALUE #( FOR _es_linea IN ti_contenido_linea
                               ( nombre_columna = _es_linea ) ).
        o_homologacion->homologar_campo( CHANGING c_ti_table = ti_columnas ).

      ELSEIF ti_contenido_linea IS NOT INITIAL.
        APPEND INITIAL LINE TO c_ti_dato_activofijo ASSIGNING <fs_es_dato_activofijo>.

        LOOP AT ti_contenido_linea ASSIGNING FIELD-SYMBOL(<fs_valor_campo>).
          TRY.

              nombre_columna = ti_columnas[ sy-tabix ]-nombre_columna.
              ASSIGN COMPONENT nombre_columna OF STRUCTURE <fs_es_dato_activofijo> TO <fs_valor_es_activo_fijo>.

              IF sy-subrc IS NOT INITIAL.
                "validar las columnas no homologadas solo para el primer conjunto de datos
                IF  fila_csv = 2.
                  o_log->set_es_log( i_es = VALUE #( type = zclcxr1002_util=>gc_w id = 'ZCX01' number = '047' message_v1 = nombre_columna ) ).
                ENDIF.
                CONTINUE.
              ENDIF.

              "Formato en campos fecha
              IF ti_descripcion_componente[ name = nombre_columna ]-o_eldescribe->type_kind = 'D'.
                REPLACE ALL OCCURRENCES OF '-' IN <fs_valor_campo> WITH space.
                REPLACE ALL OCCURRENCES OF '.' IN <fs_valor_campo> WITH space.
                REPLACE ALL OCCURRENCES OF '/' IN <fs_valor_campo> WITH space.
                "Formato en campo numerico
              ELSEIF ti_descripcion_componente[ name = nombre_columna ]-o_eldescribe->type_kind = 'P'.
                IF nombre_columna = 'QUANTITY'.
                  CLEAR: ti_valores_numero.
                  SPLIT <fs_valor_campo> AT '.' INTO TABLE ti_valores_numero.
                  <fs_valor_campo> = ti_valores_numero[ 1 ].
                ELSE.
                  REPLACE ALL OCCURRENCES OF '.' IN <fs_valor_campo> WITH space.
                ENDIF.
              ENDIF.

              IF <fs_valor_campo> CS 'N/A'.
                REPLACE ALL OCCURRENCES OF 'N/A' IN <fs_valor_campo> WITH space.
              ENDIF.

              <fs_valor_es_activo_fijo> = <fs_valor_campo>.
            CATCH cx_root INTO o_cx.
              o_log->set_es_log( i_es = VALUE #( type = zclcxr1002_util=>gc_e id = '00' number = '208' message = o_cx->get_text( ) ) ).
          ENDTRY.
        ENDLOOP.
        "Asignar valores fijos
        <fs_es_dato_activofijo>-fisc_year_postval = <fs_es_dato_activofijo>-fisc_year.
        <fs_es_dato_activofijo>-area_postval = <fs_es_dato_activofijo>-area.
***        o_homologacion->homologar_campo( CHANGING c_es = <fs_es_dato_activofijo> ).
      ENDIF.

    ENDLOOP.

    DELETE c_ti_dato_activofijo WHERE companycode IS INITIAL OR asset IS INITIAL.
    SORT c_ti_dato_activofijo BY companycode asset subnumber assetclass orig_asset ASCENDING.

  ENDMETHOD.

  METHOD  matchcode.
    DATA(lti_lst_archivo) = zclcxr1002_cargar_archivo=>matchcode_excel( ).

    CHECK lti_lst_archivo IS NOT INITIAL.

    c_arch = lti_lst_archivo[ 1 ]-filename.
  ENDMETHOD.

  METHOD verificar_autorizacion.
*    AUTHORITY-CHECK OBJECT 'ZAM001'
*        ID 'ACTVT' FIELD '03'.
*
*    IF sy-subrc IS NOT INITIAL.
*      MESSAGE e077(s#) WITH sy-tcode. " Falta autorización para la transacción &
*    ENDIF.
  ENDMETHOD.

  METHOD validar_transformar_datos.

    DATA: r_log TYPE RANGE OF zclcxr1002_log_aplicacion=>gtp_es_log-fila_descripcion.

    IF c_ti_datos IS INITIAL.
      o_log->set_es_log( i_es = VALUE #( type = zclcxr1002_util=>gc_e id = zclfic1009_carga_activos_fijos=>gc_cls_msg number = '027' ) ).
      RETURN.
    ENDIF.

    "transforma valores
    LOOP AT  c_ti_datos ASSIGNING FIELD-SYMBOL(<fs_es_datos>).
      zclcxr1002_util=>formato_fecha_ddmmyyyy( CHANGING c_fecha = <fs_es_datos>-cap_date ).
      zclcxr1002_util=>formato_fecha_ddmmyyyy( CHANGING c_fecha = <fs_es_datos>-date ).
      zclcxr1002_util=>formato_fecha_ddmmyyyy( CHANGING c_fecha = <fs_es_datos>-deact_date ).
      zclcxr1002_util=>formato_fecha_ddmmyyyy( CHANGING c_fecha = <fs_es_datos>-odep_start_date ).
    ENDLOOP.

    "agregar mensaje de exito cuando no hay errores
    IF o_log->get_errores( ) IS INITIAL AND o_log->get_advertencias( ) IS INITIAL.
      o_log->set_es_log( i_es = VALUE #( type = zclcxr1002_util=>gc_s id = zclfic1009_carga_activos_fijos=>gc_cls_msg_cx number = '043' ) ).
    ENDIF.

    CHECK o_log->get_errores( ) IS NOT INITIAL.

    "eliminar registros con error
    zclcxr1002_util=>crear_rango(
      EXPORTING
        i_ti             = o_log->get_errores( )
        i_nombre_columna = 'FILA_DESCRIPCION'
      CHANGING
        c_ti_rango       = r_log ).

    CHECK r_log IS NOT INITIAL.
    DELETE c_ti_datos WHERE asset IN r_log.

    MESSAGE s024(zfi01).

  ENDMETHOD.

  METHOD ejecucion_finalizada.

    DATA: ti_log TYPE zttfic1009_1.

    ADD 1 TO g_procesos_term.
    ADD 1 TO g_procesos_recibidos.

    RECEIVE RESULTS FROM FUNCTION 'ZFIC1009_CARGA_ACTIVOS_FIJOS'
       IMPORTING
            e_ti_returnmessages = ti_log
       EXCEPTIONS
            communication_failure = 1
            system_failure        = 2
            OTHERS                = 3.

    IF sy-subrc IS INITIAL.
      "Consolidar log
      gti_log_creacion = CORRESPONDING #( BASE ( gti_log_creacion ) ti_log ) .
    ELSE.
      ti_log = VALUE #( ( idmsg = sy-msgid type = sy-msgty numero = sy-msgno
                                               message_v1 = sy-msgv1 message_v2 = sy-msgv2 message_v3 = sy-msgv3
                                               message_v4 = sy-msgv4 ) ) .
      gti_log_creacion = CORRESPONDING #( BASE ( gti_log_creacion ) ti_log  ) .
    ENDIF.

    asignar_fecha_hora_log( CHANGING c_ti_log_creacion = gti_log_creacion ).

  ENDMETHOD.

  METHOD ejecutar_en_fondo.

    DATA: jobcount TYPE btcjobcnt,
          jobname  TYPE btcjob.

    jobname = |{ sy-repid }_{ sy-datum }{ sy-uzeit }|.

    "exportar tabla con datos a memoria
*    EXPORT ti_datos_cargados = i_ti_datos_cargados
*      TO SHARED BUFFER indx(xy)
*      ID gcte_id_sh_buffer.

    CALL FUNCTION 'JOB_OPEN'
      EXPORTING
        jobname          = jobname   " Job Name
      IMPORTING
        jobcount         = jobcount    " ID Number of Background Job          =     " Special Additional Error Code
      EXCEPTIONS
        cant_create_job  = 1
        invalid_job_data = 2
        jobname_missing  = 3
        OTHERS           = 4.

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                 WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.


    SUBMIT zfic1009_1   WITH pa_arch EQ i_narchivo
                        WITH pa_arch2 EQ i_narchivo2
                        WITH pa_arch3 EQ i_narchivo3
                        WITH pa_arch4 EQ i_narchivo4
                        WITH pa_arch5 EQ i_narchivo5
                        WITH pa_test EQ i_test
                        WITH pa_fondo EQ abap_false
                        VIA JOB jobname NUMBER jobcount AND RETURN.

    CALL FUNCTION 'JOB_CLOSE'
      EXPORTING
        jobcount             = jobcount
        jobname              = jobname
        strtimmed            = abap_true
      EXCEPTIONS
        cant_start_immediate = 1
        invalid_startdate    = 2
        jobname_missing      = 3
        job_close_failed     = 4
        job_nosteps          = 5
        job_notex            = 6
        lock_failed          = 7
        OTHERS               = 8.

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
                 WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

    MESSAGE s054(/scmb/ddd_buf_maint) WITH jobname.

  ENDMETHOD.


  METHOD escribir_log_db.

    CHECK c_ti_log_creacion IS NOT INITIAL.

    MODIFY c_ti_log_creacion FROM VALUE #( flname =  get_nombre_archivo( i_arch ) )
    TRANSPORTING flname WHERE flname IS INITIAL.

    MODIFY ztfic1009_1 FROM TABLE c_ti_log_creacion.

    IF sy-subrc IS INITIAL.
      COMMIT WORK AND WAIT.
      CLEAR c_ti_log_creacion.
    ENDIF.

  ENDMETHOD.


  METHOD asignar_fecha_hora_log.
    MODIFY c_ti_log_creacion FROM VALUE #( fecha = sy-datum hora = sy-uzeit )
    TRANSPORTING fecha hora
    WHERE fecha = '00000000'.
  ENDMETHOD.

  METHOD cargar_datos.

    DATA: ti_datos_cargados   TYPE zclcxr1002_cargar_archivo=>tp_ti_con_name,
          ti_datos_temporales TYPE gtp_ti_datos_temporales.

    CHECK i_fondo IS NOT INITIAL.

    TRY.

        zclcxr1002_cargar_archivo=>cargar_plano(
        EXPORTING
          i_c_filename = i_arch
        IMPORTING
          e_ti_datos   = ti_datos_cargados ).

        CHECK ti_datos_cargados IS NOT INITIAL.

        ti_datos_temporales = VALUE #( FOR es_datos_cargados IN ti_datos_cargados
                                       INDEX INTO indice
                                       ( value = es_datos_cargados arch = get_nombre_archivo( i_arch = i_arch )
                                         rowd = indice )
                                     ).

        "guarda datos en db
        MODIFY ztfic1009_2 FROM TABLE ti_datos_temporales.

        IF sy-subrc IS INITIAL.
          COMMIT WORK AND WAIT.
        ENDIF.

      CATCH cx_root INTO o_cx.
        MESSAGE s208(00) WITH o_cx->get_longtext( ).
    ENDTRY.

  ENDMETHOD.

  METHOD get_datos_db.
    DATA(arch) = get_nombre_archivo( i_arch =  i_arch ).

    SELECT *
    FROM ztfic1009_2
    WHERE arch = @arch
    INTO TABLE @r_ti_dat.

    SORT r_ti_dat BY rowd col.
  ENDMETHOD.

  METHOD conf_ejecucion_fondo.

    CHECK sy-batch IS INITIAL AND i_fondo IS NOT INITIAL.

    go_carga_activofijo->cargar_datos( i_arch = i_narchivo i_fondo = abap_true ).

    IF pa_arch2 IS NOT INITIAL.
      go_carga_activofijo->cargar_datos( i_arch = i_narchivo2 i_fondo = abap_true ).
    ENDIF.

    IF pa_arch3 IS NOT INITIAL.
      go_carga_activofijo->cargar_datos( i_arch = i_narchivo3 i_fondo = abap_true ).
    ENDIF.

    IF pa_arch4 IS NOT INITIAL.
      go_carga_activofijo->cargar_datos( i_arch = i_narchivo4 i_fondo = abap_true ).
    ENDIF.

    IF pa_arch5 IS NOT INITIAL.
      go_carga_activofijo->cargar_datos( i_arch = i_narchivo5 i_fondo = abap_true ).
    ENDIF.

    ejecutar_en_fondo( i_narchivo          = i_narchivo
                       i_narchivo2         = i_narchivo2
                       i_narchivo3         = i_narchivo3
                       i_narchivo4         = i_narchivo4
                       i_narchivo5         = i_narchivo5
                       i_test              = i_test ).

  ENDMETHOD.

  METHOD eliminar_reg_temp_db.

    DATA(arch) = get_nombre_archivo( i_arch =  i_arch ).

    DELETE FROM ztfic1009_2 WHERE arch = arch.

    IF sy-subrc IS INITIAL.
      COMMIT WORK AND WAIT.
    ENDIF.

  ENDMETHOD.

  METHOD get_nombre_archivo.

    SPLIT i_arch AT '\' INTO TABLE DATA(ti_str).

    r_arch = ti_str[ lines( ti_str ) ].

  ENDMETHOD.

  METHOD modificar_catalogo.
    LOOP AT c_ti_catalogo ASSIGNING FIELD-SYMBOL(<fs_es_catalogo>).
      CASE <fs_es_catalogo>-fieldname.
        WHEN 'ANLN1_C'.
          <fs_es_catalogo>-coltext =  TEXT-005.
          <fs_es_catalogo>-hotspot =  abap_true.
        WHEN 'MESSAGE'.
          <fs_es_catalogo>-outputlen =  45.
        WHEN 'TYPE'." OR 'IDMSG' OR 'NUMERO'.
          <fs_es_catalogo>-no_out  = abap_true.
      ENDCASE.
    ENDLOOP.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~registrar_evento_alv.

    SET HANDLER me->hd_hotspot_click FOR c_o_alvgrid.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai.

    CASE i_ok_code.
      WHEN 'BACK' OR 'EXIT'.
        LEAVE TO SCREEN 0.
      WHEN 'ACTUALIZAR'.
        zclfic1009_carga_activos_fijos=>mostrar_resultados_log( EXPORTING
                                                                   i_conservar_screen   = abap_true
                                                                   i_o_grid_log_handler = me
                                                                CHANGING
                                                                   c_ti_log = gti_log_icon   ).
    ENDCASE.

  ENDMETHOD.

  METHOD hd_hotspot_click.

    DATA(es_log_data) = gti_log_icon[ e_row_id ].

    CHECK es_log_data-anln1_c IS NOT INITIAL.

    SET PARAMETER ID 'AN1' FIELD es_log_data-anln1_c.
    SET PARAMETER ID 'AN2' FIELD es_log_data-anln2.
    SET PARAMETER ID 'BUK' FIELD es_log_data-bukrs.
    CALL TRANSACTION 'AS03' AND SKIP FIRST SCREEN.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai_at_exit_command.

  ENDMETHOD.

ENDCLASS.
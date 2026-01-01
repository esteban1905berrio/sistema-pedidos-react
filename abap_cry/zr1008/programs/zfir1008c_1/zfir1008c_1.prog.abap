*&---------------------------------------------------------------------*
*& Include zfir1008c_1
*&---------------------------------------------------------------------*

CLASS lcl_controlador DEFINITION INHERITING FROM zclfir1008_datacredito_cartera.

  PUBLIC SECTION.

    INTERFACES zifcxr1002_alvgrid.

    TYPES: BEGIN OF gtp_es_arc_datacredito_control,
             ind_reg_ini(18)    TYPE c,
             cod_suscriptor(6)  TYPE c,
             tipo_cuenta(2)     TYPE c,
             fecha_corte(8)     TYPE c,
             ampliac_milenio(1) TYPE c,
             ind_val_miles(1)   TYPE c,
             tipo_entrega(1)    TYPE c,
             fecha_ini_repo(8)  TYPE c,
             fecha_fin_repo(8)  TYPE c,
             ind_partir(1)      TYPE c,
             filler(746)        TYPE c,
           END OF gtp_es_arc_datacredito_control,

           BEGIN OF gtp_es_arc_datacredito_cliente,
             tipo_id(1)           TYPE c,
             num_id(11)           TYPE c,
             num_cta_obliga(18)   TYPE c,
             nom_completo(45)     TYPE c,
             situa_titular(1)     TYPE c,
             fecha_apertura(8)    TYPE c,
             fecha_vencim(8)      TYPE c,
             respon_cal_deud(2)   TYPE c,
             tipo_obliga(1)       TYPE c,
             subsi_hipoteca(1)    TYPE c,
             fecha_subsidio(8)    TYPE c,
             termino_obligac(1)   TYPE c,
             forma_pago(1)        TYPE c,
             periodicidad_pago(1) TYPE c,
             novedad(2)           TYPE c,
             estado_origen_cta(1) TYPE c,
             fecha_origen_cta(8)  TYPE c,
             estado_cuenta(2)     TYPE c,
             fecha_estado_cta(8)  TYPE c,
             estado_plastico(1)   TYPE c,
             fe_estado_plast(8)   TYPE c,
             adjetivo(1)          TYPE c,
             fe_adjetivo(8)       TYPE c,
             clase_tarjeta(1)     TYPE c,
             franquicia(1)        TYPE c,
             nom_marca_priv(30)   TYPE c,
             tipo_moneda(1)       TYPE c,
             tipo_garantia(1)     TYPE c,
             calificacion(2)      TYPE c,
             probab_incumpl(3)    TYPE c,
             edad_mora(3)         TYPE c,
             valor_inicial(11)    TYPE c,
             saldo_deuda(11)      TYPE c,
             valor_disponible(11) TYPE c,
             val_cuota_mensu(11)  TYPE c,
             val_saldo_mora(11)   TYPE c,
             total_cuotas(3)      TYPE c,
             cuotas_canceladas(3) TYPE c,
             cuotas_mora(3)       TYPE c,
             clausu_perman(3)     TYPE c,
             fe_clausu_perman(8)  TYPE c,
             fecha_lim_pago(8)    TYPE c,
             fecha_pago(8)        TYPE c,
             oficina_radicac(30)  TYPE c,
             ciudad_radicac(20)   TYPE c,
             dane_radicac(8)      TYPE c,
             ciudad_residen(20)   TYPE c,
             dane_residen(8)      TYPE c,
             depar_residen(20)    TYPE c,
             direc_residen(60)    TYPE c,
             tel_residen(12)      TYPE c,
             ciudad_laboral(20)   TYPE c,
             dane_laboral(8)      TYPE c,
             depar_laboral(20)    TYPE c,
             direc_laboral(60)    TYPE c,
             tel_laboral(12)      TYPE c,
             ciudad_corresp(20)   TYPE c,
             dane_corresp(8)      TYPE c,
             depar_corresp(20)    TYPE c,
             direc_corresp(60)    TYPE c,
             correo_electr(60)    TYPE c,
             celular(12)          TYPE c,
             suscrip_destino(6)   TYPE c,
             blanco(37)           TYPE c,
           END OF gtp_es_arc_datacredito_cliente,

           BEGIN OF gtp_es_arc_datacredito_final,
             identificador(18) TYPE c,
             fecha_proceso(8)  TYPE c,
             num_registros(8)  TYPE c,
             sum_novedades(8)  TYPE c,
             filler(758)       TYPE c,
           END OF gtp_es_arc_datacredito_final,

           gtp_char800    TYPE c LENGTH 800,
           gtp_ti_archivo TYPE STANDARD TABLE OF gtp_char800 WITH EMPTY KEY.

    DATA: gti_saldos_cartera_clientes TYPE gtp_ti_cartera_cliente,
          gti_datos_cliente           TYPE gtp_ti_datos_cliente,
          gti_catalogo                TYPE lvc_t_fcat.

    DATA: go_alv            TYPE REF TO cl_gui_alv_grid,
          go_dock_container TYPE REF TO cl_gui_docking_container.

    METHODS:
      iniciar_proceso
        IMPORTING
          i_bukrs   TYPE bseg-bukrs
          i_bldat   TYPE sy-datum
          i_dias    TYPE numc4
          i_r_kunnr LIKE so_kunnr[],
      visualizar_datos,
      cambiar_catalogo_alv
        CHANGING
          c_ti_catalogo TYPE lvc_t_fcat,
      generar_archivo_datacredito
        IMPORTING
          VALUE(i_nombre_archivo)             TYPE string
          i_suscr                             LIKE pa_suscr
          i_bldat                             LIKE pa_bldat
          i_ti_datos_cliente                  TYPE gtp_ti_datos_cliente
          i_ti_saldos_cartera_clientes        TYPE gtp_ti_cartera_cliente
        RETURNING
          VALUE(r_ti_archivo_txt_datacredito) TYPE gtp_ti_archivo,
      evento_hotspot_click
        FOR EVENT hotspot_click  OF cl_gui_alv_grid
        IMPORTING es_row_no.


  PROTECTED SECTION.

  PRIVATE SECTION.

    DATA: g_sociedad TYPE bkpf-bukrs.


ENDCLASS.

CLASS lcl_controlador IMPLEMENTATION.

  METHOD iniciar_proceso.

    g_sociedad = i_bukrs.

    TRY.
        zclfir1008_datacredito_cartera=>generar_estado_cartera(
          EXPORTING
            i_r_kunnr             = cl_shdb_seltab=>combine_seltabs( it_named_seltabs = VALUE #( ( name = 'KUNNR' dref = REF #( i_r_kunnr ) ) ) )
            i_fechacorte          = i_bldat
            i_bukrs               = i_bukrs
            i_dias                = i_dias
          IMPORTING
            e_ti_estado_cartera   = gti_saldos_cartera_clientes
            e_ti_datos_cliente    = gti_datos_cliente ).

      CATCH cx_shdb_exception.
        "handle exception
    ENDTRY.

  ENDMETHOD.

  METHOD generar_archivo_datacredito.

    CONSTANTS: c_fecha_cero TYPE char8 VALUE '00000000',
               c_valor_cero TYPE char11 VALUE '00000000000'.

    DATA: ti_arc_datacredito_final     TYPE STANDARD TABLE OF gtp_es_arc_datacredito_final,
          es_archivo_datacredito_final TYPE gtp_es_arc_datacredito_final,
          es_datos_cliente             LIKE LINE OF i_ti_datos_cliente.

    "Registro de control Inicio
    DATA(es_registro_control) = VALUE gtp_es_arc_datacredito_control(
                ind_reg_ini = 'HHHHHHHHHHHHHHHHHH'
                cod_suscriptor = i_suscr
                tipo_cuenta = '32'
                fecha_corte = i_bldat
                ampliac_milenio = 'M'
                ind_val_miles = '0'
                tipo_entrega = 'T'
                fecha_ini_repo = '00000000'
                fecha_fin_repo = '00000000'
                ind_partir = space
                filler = space ).

    APPEND es_registro_control TO r_ti_archivo_txt_datacredito.
    "Registro de detalle
    LOOP AT i_ti_saldos_cartera_clientes ASSIGNING FIELD-SYMBOL(<fs_cartera_cliente>).

      TRY.
          es_datos_cliente = i_ti_datos_cliente[ stcd1 = <fs_cartera_cliente>-stcd1 kunnr = <fs_cartera_cliente>-kunnr ].
        CATCH cx_sy_itab_line_not_found.
          CONTINUE.
      ENDTRY.

      APPEND
          VALUE gtp_es_arc_datacredito_cliente(
           tipo_id = SWITCH #( es_datos_cliente-stcdt
                               WHEN '13' THEN
                                '1'
                               WHEN '31' THEN
                                '2'
                               WHEN '22' THEN
                                '3'
                             )
           num_id = |{ es_datos_cliente-stcd1 ALPHA = IN }|
           num_cta_obliga = CONV #( es_datos_cliente-kunnr )
           nom_completo = CONV #( es_datos_cliente-name1 )
           situa_titular = '0'
           fecha_apertura = es_datos_cliente-erdat
           fecha_vencim   = COND #( WHEN <fs_cartera_cliente>-valormora IS INITIAL THEN
                                        ( i_bldat + 1095 )"agregar 3 anios a la fecha
                                    ELSE
                                     es_datos_cliente-fechavencimiento
                                   )
           respon_cal_deud = '00'
           tipo_obliga = '1'
           subsi_hipoteca = '0'
           fecha_subsidio = c_fecha_cero
           termino_obligac = '2'
           forma_pago = '0'
           periodicidad_pago = '6'
           novedad = COND #( WHEN <fs_cartera_cliente>-valortotaldeuda IS INITIAL THEN
                              '05'
                             ELSE
                              '01' )
           estado_origen_cta = '0'
           fecha_origen_cta  = es_datos_cliente-erdat
           estado_cuenta = '01'
           fecha_estado_cta = sy-datum
           estado_plastico = '0'
           fe_estado_plast = c_fecha_cero
           adjetivo = '0'
           fe_adjetivo = c_fecha_cero
           clase_tarjeta = '0'
           franquicia = '0'
           nom_marca_priv = space
           tipo_moneda = '1'
           tipo_garantia = '0'
           calificacion = space
           probab_incumpl = '000'
           edad_mora = '000'
           valor_inicial = <fs_cartera_cliente>-valortotaldeuda
           saldo_deuda = <fs_cartera_cliente>-valortotaldeuda
           valor_disponible = c_valor_cero
           val_cuota_mensu = <fs_cartera_cliente>-valortotaldeuda
           val_saldo_mora = c_valor_cero
           total_cuotas =  '001'
           cuotas_canceladas = '000'
           cuotas_mora = '000'
           clausu_perman = '000'
           fe_clausu_perman = c_fecha_cero
           fecha_lim_pago = c_fecha_cero
           fecha_pago = c_fecha_cero
           oficina_radicac = 'PRINCIPAL'
           ciudad_radicac = es_datos_cliente-ort01
           dane_radicac   = es_datos_cliente-city_code
           ciudad_residen = es_datos_cliente-ort01
           dane_residen = es_datos_cliente-city_code
           depar_residen = es_datos_cliente-departamento
           direc_residen = es_datos_cliente-stras
           tel_residen = es_datos_cliente-telf1
           ciudad_laboral = es_datos_cliente-ort01
           dane_laboral = es_datos_cliente-city_code
           depar_laboral = es_datos_cliente-departamento
           direc_laboral = es_datos_cliente-stras
           tel_laboral = es_datos_cliente-telf1
           ciudad_corresp = es_datos_cliente-ort01
           dane_corresp = es_datos_cliente-city_code
           depar_corresp = es_datos_cliente-departamento
           direc_corresp = es_datos_cliente-stras
           correo_electr = es_datos_cliente-correo_electronico
           celular = '000000000000'
           suscrip_destino = '000000'
           blanco = space )
      TO
          r_ti_archivo_txt_datacredito.
    ENDLOOP.

    "Registro de control fin
    es_archivo_datacredito_final-num_registros = lines( r_ti_archivo_txt_datacredito ) + 1.
    es_archivo_datacredito_final-sum_novedades = lines( r_ti_archivo_txt_datacredito ) - 1.

    APPEND
        VALUE gtp_es_arc_datacredito_final( identificador = 'ZZZZZZZZZZZZZZZZZZ' fecha_proceso = sy-datum
                                            num_registros =  |{ es_archivo_datacredito_final-num_registros ALPHA = IN }|
                                            sum_novedades =  |{ es_archivo_datacredito_final-sum_novedades ALPHA = IN }|
                                            filler = space
                                           )
    TO r_ti_archivo_txt_datacredito.

    CHECK r_ti_archivo_txt_datacredito IS NOT INITIAL.

    "Descargar archivo
    cl_gui_frontend_services=>gui_download(
      EXPORTING
        filename                  = i_nombre_archivo
        trunc_trailing_blanks_eol = ' '
      CHANGING
        data_tab                  = r_ti_archivo_txt_datacredito
      EXCEPTIONS
        file_write_error          = 1
        no_batch                  = 2
        gui_refuse_filetransfer   = 3
        invalid_type              = 4
        no_authority              = 5
        unknown_error             = 6
        header_not_allowed        = 7
        separator_not_allowed     = 8
        filesize_not_allowed      = 9
        header_too_long           = 10
        dp_error_create           = 11
        dp_error_send             = 12
        dp_error_write            = 13
        unknown_dp_error          = 14
        access_denied             = 15
        dp_out_of_memory          = 16
        disk_full                 = 17
        dp_timeout                = 18
        file_not_found            = 19
        dataprovider_exception    = 20
        control_flush_error       = 21
        not_supported_by_gui      = 22
        error_no_gui              = 23
        OTHERS                    = 24 ).

    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
        WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4.
    ENDIF.

  ENDMETHOD.

  METHOD visualizar_datos.

    DATA(ti_catalogo) = zclcxr1002_util=>construir_catalogo( i_ti = gti_saldos_cartera_clientes ).

    cambiar_catalogo_alv( CHANGING c_ti_catalogo = ti_catalogo ).

    CALL FUNCTION 'ZCXR1002_MOSTRARALV_01'
      EXPORTING
        i_ti_datos           = REF #( gti_saldos_cartera_clientes )
        i_ti_catalogo        = ti_catalogo
        i_titulo             = TEXT-tlp
        i_status_gui         = 'GS_0001'
        i_statusgui_programa = sy-repid
        i_o_ctr_alvgrid      = me
        i_es_layout          = VALUE lvc_s_layo( zebra = abap_true ).

  ENDMETHOD.

  METHOD cambiar_catalogo_alv.
    READ TABLE gti_saldos_cartera_clientes
      INTO DATA(es_saldos)
      INDEX 1.

    LOOP AT c_ti_catalogo ASSIGNING FIELD-SYMBOL(<fs_es_catalogo>).
      <fs_es_catalogo>-col_opt   = abap_true.
      CASE <fs_es_catalogo>-fieldname.
        WHEN 'VALORTOTALDEUDA' OR 'VALORMORA'.
          IF es_saldos-waers IS NOT INITIAL.
            <fs_es_catalogo>-currency = es_saldos-waers.
          ELSE.
            <fs_es_catalogo>-currency = 'COP'.
          ENDIF.
        WHEN 'WAERS'.
          <fs_es_catalogo>-no_out    = abap_true.
      ENDCASE.
    ENDLOOP.
  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~modificar_catalogo.

    LOOP AT c_ti_catalogo ASSIGNING FIELD-SYMBOL(<fs_es_catalogo>).

      CASE <fs_es_catalogo>-fieldname.
        WHEN 'STCD1'.
          <fs_es_catalogo>-coltext = TEXT-nit.
        WHEN 'KUNNR'.
          <fs_es_catalogo>-hotspot = abap_true.
        WHEN 'NOMBRECLIENTE'.
          <fs_es_catalogo>-coltext = TEXT-nom.
          <fs_es_catalogo>-col_opt = abap_true.
        WHEN 'VALORTOTALDEUDA'.
          <fs_es_catalogo>-coltext = TEXT-vld.
        WHEN 'VALORMORA'.
          <fs_es_catalogo>-coltext = TEXT-vlm.
        WHEN 'EDADMORA'.
          <fs_es_catalogo>-coltext = TEXT-edm.
          <fs_es_catalogo>-outputlen = 15.
      ENDCASE.
    ENDLOOP.

  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~registrar_evento_alv.

    SET HANDLER evento_hotspot_click FOR c_o_alvgrid.

  ENDMETHOD.

  METHOD evento_hotspot_click.

    DATA: num_cliente TYPE kna1-kunnr.

    TRY.
        num_cliente = gti_saldos_cartera_clientes[ es_row_no-row_id ]-kunnr.

        SET PARAMETER ID 'KUN' FIELD num_cliente.

        CALL TRANSACTION 'FBL5N' AND SKIP FIRST SCREEN.
      CATCH cx_sy_authorization_error ##NO_HANDLER.
      CATCH cx_sy_itab_line_not_found ##NO_HANDLER.
    ENDTRY.

  ENDMETHOD.

  METHOD zifcxr1002_alvgrid~pai.

    CASE i_ok_code.
      WHEN 'BACK' OR 'EXIT'.
        LEAVE TO SCREEN 0.
      WHEN 'ARCHIVO_DT'.

        generar_archivo_datacredito( i_nombre_archivo = |{ pa_file }|
                                     i_bldat = pa_bldat
                                     i_suscr = pa_suscr
                                     i_ti_datos_cliente = gti_datos_cliente
                                     i_ti_saldos_cartera_clientes = gti_saldos_cartera_clientes ).

    ENDCASE.

  ENDMETHOD.


ENDCLASS.
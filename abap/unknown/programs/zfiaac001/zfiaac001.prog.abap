REPORT zfiaac001.

TABLES: sscrfields.

*&---------------------------------------------------------------------*
*& Variables para rangos
*&---------------------------------------------------------------------*
DATA: gs_anln1 TYPE anla-anln1,
      gs_anln2 TYPE anlb-afabe,
      gs_anlkl TYPE anla-anlkl.

*&---------------------------------------------------------------------*
*& Global Data
*&---------------------------------------------------------------------*
DATA: gt_resultado TYPE zttfiaac002_2,
      gt_log       TYPE TABLE OF zetfaac001_logs,
      go_alv       TYPE REF TO cl_salv_table,
      gv_uuid      TYPE sysuuid_c32.

*&---------------------------------------------------------------------*
*& Constants
*&---------------------------------------------------------------------*
CONSTANTS: gc_max_records TYPE i VALUE 10000.

*&---------------------------------------------------------------------*
*& Selection Screen
*&---------------------------------------------------------------------*
SELECTION-SCREEN BEGIN OF BLOCK b1 WITH FRAME TITLE TEXT-001.
PARAMETERS: p_bukrs TYPE bukrs OBLIGATORY DEFAULT 'CALG',
            p_bdatu TYPE dats OBLIGATORY DEFAULT sy-datum.

SELECT-OPTIONS: s_anlkl FOR gs_anlkl,
                s_anln2 FOR gs_anln2.
"s_anln1 FOR gs_anln1.

PARAMETERS:
  p_csv TYPE flag AS CHECKBOX,
  p_alv TYPE flag AS CHECKBOX DEFAULT 'X'.
SELECTION-SCREEN END OF BLOCK b1.

SELECTION-SCREEN FUNCTION KEY 1.
SELECTION-SCREEN FUNCTION KEY 2.

*&---------------------------------------------------------------------*
*& Initialization
*&---------------------------------------------------------------------*
INITIALIZATION.
  MOVE 'Tabla de Parametros'(043) TO sscrfields-functxt_01.
  MOVE 'Tabla de Homologaciones'(043) TO sscrfields-functxt_02.


*&---------------------------------------------------------------------*
*& At Selection-Screen
*&---------------------------------------------------------------------*
AT SELECTION-SCREEN.

  CASE sscrfields-ucomm.
    WHEN 'FC01'.
      CALL TRANSACTION 'STVARV'.
    WHEN 'FC02'.
      CALL TRANSACTION 'ZFIAAC001_2'.
  ENDCASE.

*&---------------------------------------------------------------------*
*& Start-of-Selection
*&---------------------------------------------------------------------*
START-OF-SELECTION.
  " Generar UUID usando método simple y seguro
  PERFORM f_extraer_datos.

*&---------------------------------------------------------------------*
*& End-of-Selection
*&---------------------------------------------------------------------*
END-OF-SELECTION.
  IF p_csv = 'X'.
    PERFORM f_descargar_csv_local.
  ENDIF.

  IF p_alv = 'X'.
    PERFORM f_mostrar_alv.
  ENDIF.

*&---------------------------------------------------------------------*
*& Form f_extraer_datos
*&---------------------------------------------------------------------*
FORM f_extraer_datos.

  " ===================================================================
  " TIPOS LOCALES
  " ===================================================================
  TYPES: BEGIN OF ty_anla,
           companycode     TYPE bukrs,
           asset           TYPE bf_anln1,
           subnumber       TYPE bf_anln2,
           assetclass      TYPE bf_anlkl,
           descript        TYPE bf_txa50,
           descript2       TYPE bapi1022_txa50_more,
           serial_no       TYPE bf_am_sernr,
           invent_no       TYPE bf_invnr_anla,
           quantity        TYPE menge_d,
           base_uom        TYPE meins,
           date            TYPE bf_ivdat_anla,
           include_in_list TYPE bf_inken,
           note            TYPE bf_invzu_anla,
           cap_date        TYPE bf_aktivd,
           shutdown        TYPE bf_xstil,
           vendor_no       TYPE bf_am_lifnr,
           orig_asset      TYPE bf_aibn1,
           evalgroup1      TYPE bf_ord41,
           evalgroup2      TYPE bf_ord42,
           evalgroup3      TYPE bf_ord43,
           evalgroup4      TYPE bf_ord44,
           evalgroup5      TYPE bf_gdlgrp,
           super_number    TYPE anlue,
           deact_date      TYPE bf_deakt,
         END OF ty_anla,

         BEGIN OF ty_anlz,
           companycode      TYPE bukrs,
           asset            TYPE bf_anln1,
           subnumber        TYPE bf_anln2,
           costcenter       TYPE kostl,
           resp_cctr        TYPE bf_kostlv,
           plant            TYPE werks_d,
           location         TYPE stort,
           room             TYPE char10,
           plate_no         TYPE bf_am_kfzkz,
           fund             TYPE bp_geber,
           funds_center     TYPE fistl,
           wbs_element_cost TYPE bapi1022_posnr_ext2,
           profit_center    TYPE prctr,
           segment          TYPE fb_segment,
         END OF ty_anlz,

         BEGIN OF ty_anlb,
           companycode      TYPE bukrs,
           asset            TYPE bf_anln1,
           subnumber        TYPE bf_anln2,
           area             TYPE bf_afabe_d,
           dep_key          TYPE bf_afasl,
           exp_ulife_yrs    TYPE bf_ndabj,
           ulife_prds       TYPE bf_ndper,
           odep_start_date  TYPE bf_afabg,
           scrapvalue       TYPE bf_schrw,
           scrapvalue_prctg TYPE schrw_proz,
         END OF ty_anlb,

         BEGIN OF ty_anlc,
           companycode         TYPE bukrs,
           asset               TYPE bf_anln1,
           subnumber           TYPE bf_anln2,
           area                TYPE bf_afabe_d,
           fisc_year           TYPE gjahr,
           acq_value           TYPE bf_kansw,
           rev_repl            TYPE bf_kaufw,
           ord_dep             TYPE bf_knafa,
           rev_ord_dep         TYPE bf_kaufn,
           capitalization_year TYPE bf_kanza,
           zamortanio          TYPE wrbtr,
           zrevamano           TYPE wrbtr,
           zamorevano          TYPE wrbtr,
         END OF ty_anlc,

         BEGIN OF ty_anlh,
           companycode   TYPE bukrs,
           asset         TYPE bf_anln1,
           subnumber     TYPE bf_anln2,
           main_descript TYPE anlhtxt,
         END OF ty_anlh.

  " ===================================================================
  " VARIABLES LOCALES
  " ===================================================================
  DATA: lt_anla       TYPE TABLE OF ty_anla,
        lt_anlz       TYPE TABLE OF ty_anlz,
        lt_anlb       TYPE TABLE OF ty_anlb,
        lt_anlc       TYPE TABLE OF ty_anlc,
        lt_anlh       TYPE TABLE OF ty_anlh,
        ls_anla       LIKE LINE OF lt_anla,
        ls_anlz       TYPE ty_anlz,
        ls_anlb       TYPE ty_anlb,
        ls_anlc       TYPE ty_anlc,
        ls_anlh       TYPE ty_anlh,
        ls_resultado  LIKE LINE OF gt_resultado,
        ls_log        TYPE zetfaac001_logs,
        lv_count      TYPE i,
        lv_count_char TYPE string,
        lv_gjahr      TYPE gjahr.

  " Limpiar tablas internas
  CLEAR: gt_resultado, gt_log.
  BREAK-POINT.
  " Calcular ejercicio fiscal desde fecha de corte
  lv_gjahr = p_bdatu(4).  " Extraer YYYY de YYYYMMDD

  " ===================================================================
  " PASO 1: SELECT PRINCIPAL - TABLA ANLA (Maestro de Activos)
  " ===================================================================
  " Extrae solo activos válidos que cumplen todas las validaciones
  " ===================================================================
  SELECT
    bukrs       AS companycode,
    anln1       AS asset,
    anln2       AS subnumber,
    anlkl       AS assetclass,
    txt50       AS descript,
    txa50       AS descript2,
    sernr       AS serial_no,
    invnr       AS invent_no,
    menge       AS quantity,
    meins       AS base_uom,
    ivdat       AS date,
    inken       AS include_in_list,
    invzu       AS note,
    aktiv       AS cap_date,
    "xstil       AS shutdown,
    lifnr       AS vendor_no,
    aibn1       AS orig_asset,
    ord41       AS evalgroup1,
    ord42       AS evalgroup2,
    ord43       AS evalgroup3,
    ord44       AS evalgroup4,
    gdlgrp      AS evalgroup5,
    anlue       AS super_number,
    deakt       AS deact_date
  FROM anla
  WHERE bukrs = @p_bukrs
    AND anlkl IN @s_anlkl
    AND anln2 IN @s_anln2
    AND aktiv NE '00000000'
    AND deakt = '00000000'
  INTO CORRESPONDING FIELDS OF TABLE @lt_anla
  UP TO @gc_max_records ROWS.

  IF sy-subrc <> 0 OR lt_anla IS INITIAL.
    " No se encontraron activos válidos
    ls_log-uuid = gv_uuid.
    ls_log-tipo = 'W'.
    ls_log-mensaje = 'No se encontraron activos fijos válidos con los criterios especificados'.
    ls_log-fecha = sy-datum.
    ls_log-hora = sy-uzeit.
    ls_log-usuario = sy-uname.
    ls_log-programa = sy-repid.
    APPEND ls_log TO gt_log.

    MESSAGE w003(zfiaac001) WITH 'No se encontraron activos fijos válidos'.
    RETURN.
  ENDIF.

  " ===================================================================
  " PASO 2: SELECT MASIVO - TABLA ANLH (Texto Histórico)
  " ===================================================================
  SELECT
    bukrs       AS companycode,
    anln1       AS asset,
    "anln2       AS subnumber,
    anlhtxt     AS main_descript
  FROM anlh
  FOR ALL ENTRIES IN @lt_anla
  WHERE bukrs = @lt_anla-companycode
    AND anln1 = @lt_anla-asset
    "AND anln2 = @lt_anla-subnumber
  INTO CORRESPONDING FIELDS OF TABLE @lt_anlh.

  " ===================================================================
  " PASO 3: SELECT MASIVO - TABLA ANLZ (Datos Organizativos)
  " ===================================================================
  SELECT
    bukrs       AS companycode,
    anln1       AS asset,
    anln2       AS subnumber,
    kostl       AS costcenter,
    kostlv      AS resp_cctr,
    werks       AS plant,
    stort       AS location,
    raumn       AS room,
    kfzkz       AS plate_no,
    geber       AS fund,
    fistl       AS funds_center,
    ps_psp_pnr2 AS wbs_element_cost,
    prctr       AS profit_center,
    segment     AS segment
  FROM anlz
  FOR ALL ENTRIES IN @lt_anla
  WHERE bukrs = @lt_anla-companycode
    AND anln1 = @lt_anla-asset
    AND anln2 = @lt_anla-subnumber
  INTO TABLE @lt_anlz.

  " ===================================================================
  " PASO 4: SELECT MASIVO - TABLA ANLB (Valoración y Depreciación)
  " ===================================================================
  SELECT
    bukrs       AS companycode,
    anln1       AS asset,
    anln2       AS subnumber,
    afabe       AS area,
    afasl       AS dep_key,
    "ndabj       AS exp_ulife_yrs,
    ndper       AS ulife_prds,
    afabg       AS odep_start_date,
    schrw       AS scrapvalue,
    schrw_proz  AS scrapvalue_prctg
  FROM anlb
  FOR ALL ENTRIES IN @lt_anla
  WHERE bukrs = @lt_anla-companycode
    AND anln1 = @lt_anla-asset
    AND anln2 = @lt_anla-subnumber
    AND afabe = '01'
  INTO CORRESPONDING FIELDS OF TABLE @lt_anlb.

  " ===================================================================
  " PASO 5: SELECT MASIVO - TABLA ANLC (Valores Contables)
  " ===================================================================
  SELECT
    bukrs       AS companycode,
    anln1       AS asset,
    anln2       AS subnumber,
    afabe       AS area,
    gjahr       AS fisc_year,
    kansw       AS acq_value,
    kaufw       AS rev_repl,
    knafa       AS ord_dep,
    kaufn       AS rev_ord_dep,
    kanza       AS capitalization_year,
    nafal       AS zamortanio,
    aufwl       AS zrevamano,
    aufnl       AS zamorevano
  FROM anlc
  FOR ALL ENTRIES IN @lt_anla
  WHERE bukrs = @lt_anla-companycode
    AND anln1 = @lt_anla-asset
    AND anln2 = @lt_anla-subnumber
    AND afabe = '01'
    AND gjahr = @lv_gjahr
  INTO TABLE @lt_anlc.

  " ===================================================================
  " PASO 6: CONSOLIDACIÓN DE DATOS
  " ===================================================================
  LOOP AT lt_anla INTO ls_anla.
    CLEAR ls_resultado.

    " -------------------------------------------------------------------
    " 6.1 COPIAR DATOS DE ANLA (Maestro de Activos)
    " -------------------------------------------------------------------
    MOVE-CORRESPONDING ls_anla TO ls_resultado.

    " -------------------------------------------------------------------
    " 6.2 AGREGAR DATOS DE ANLH (Texto Histórico)
    " -------------------------------------------------------------------
    READ TABLE lt_anlh INTO ls_anlh
      WITH KEY companycode = ls_anla-companycode
               asset       = ls_anla-asset
               subnumber   = ls_anla-subnumber
      BINARY SEARCH.
    IF sy-subrc = 0.
      ls_resultado-main_descript = ls_anlh-main_descript.
    ENDIF.

    " -------------------------------------------------------------------
    " 6.3 AGREGAR DATOS DE ANLZ (Datos Organizativos)
    " -------------------------------------------------------------------
    READ TABLE lt_anlz INTO ls_anlz
      WITH KEY companycode = ls_anla-companycode
               asset       = ls_anla-asset
               subnumber   = ls_anla-subnumber
      BINARY SEARCH.
    IF sy-subrc = 0.
      MOVE-CORRESPONDING ls_anlz TO ls_resultado.
    ENDIF.

    " -------------------------------------------------------------------
    " 6.4 AGREGAR DATOS DE ANLB (Valoración y Depreciación)
    " -------------------------------------------------------------------
    READ TABLE lt_anlb INTO ls_anlb
      WITH KEY companycode = ls_anla-companycode
               asset       = ls_anla-asset
               subnumber   = ls_anla-subnumber
      BINARY SEARCH.
    IF sy-subrc = 0.
      MOVE-CORRESPONDING ls_anlb TO ls_resultado.
    ENDIF.

    " -------------------------------------------------------------------
    " 6.5 AGREGAR DATOS DE ANLC (Valores Contables)
    " -------------------------------------------------------------------
    READ TABLE lt_anlc INTO ls_anlc
      WITH KEY companycode = ls_anla-companycode
               asset       = ls_anla-asset
               subnumber   = ls_anla-subnumber
      BINARY SEARCH.
    IF sy-subrc = 0.
      " Lógica de negocio: Valores Acumulados vs Movimientos del Año
      IF ls_anla-cap_date < '20250101'.
        " Valores acumulados (activos capitalizados antes de 2025)
        ls_resultado-acq_value            = ls_anlc-acq_value.
        ls_resultado-rev_repl             = ls_anlc-rev_repl.
        ls_resultado-ord_dep              = ls_anlc-ord_dep.
        ls_resultado-rev_ord_dep          = ls_anlc-rev_ord_dep.
        ls_resultado-capitalization_year  = ls_anlc-capitalization_year.
      ELSE.
        " Movimientos del año (activos capitalizados en 2025)
        ls_resultado-zamortanio   = ls_anlc-zamortanio.
        ls_resultado-zrevamano    = ls_anlc-zrevamano.
        ls_resultado-zamorevano   = ls_anlc-zamorevano.
      ENDIF.
      ls_resultado-fisc_year = ls_anlc-fisc_year.
    ENDIF.

    " -------------------------------------------------------------------
    " 6.6 CAMPOS CALCULADOS Y VALORES FIJOS
    " -------------------------------------------------------------------
    ls_resultado-nassets      = '1'.      " Cantidad fija = 1
    ls_resultado-currency     = 'COP'.    " Moneda Colombia
    ls_resultado-currency_iso = 'COP'.    " ISO
    ls_resultado-area         = '01'.     " Área de valoración
    ls_resultado-fisc_year    = lv_gjahr. " Ejercicio fiscal

    " -------------------------------------------------------------------
    " 6.7 CAMPOS Z PERSONALIZADOS (Si aplica)
    " -------------------------------------------------------------------
    " NOTA: Descomentar si existe tabla Z de campos personalizados
    " SELECT SINGLE zmunicip, zmatinmob, zfichcata, zanioimp,
    "               zvalorimp, zfechaimp, zcomodat, zcontcomo,
    "               zfechini, zfechfin
    "   FROM ztabla_campos_z
    "   WHERE bukrs = @ls_anla-companycode
    "     AND anln1 = @ls_anla-asset
    "     AND anln2 = @ls_anla-subnumber
    "   INTO (@ls_resultado-zmunicip, @ls_resultado-zmatinmob,
    "         @ls_resultado-zfichcata, @ls_resultado-zanioimp,
    "         @ls_resultado-zvalorimp, @ls_resultado-zfechaimp,
    "         @ls_resultado-zcomodat, @ls_resultado-zcontcomo,
    "         @ls_resultado-zfechini, @ls_resultado-zfechfin).

    " -------------------------------------------------------------------
    " 6.8 HOMOLOGACIONES (Fase 3 - Pendiente)
    " -------------------------------------------------------------------
    " PERFORM f_homologar_campos CHANGING ls_resultado.

    " -------------------------------------------------------------------
    " 6.9 AGREGAR A TABLA DE RESULTADOS
    " -------------------------------------------------------------------
    APPEND ls_resultado TO gt_resultado.

  ENDLOOP.

  " ===================================================================
  " PASO 7: LOG Y MENSAJE FINAL
  " ===================================================================
  lv_count = lines( gt_resultado ).
  lv_count_char = lv_count.

  " Crear log de éxito
  ls_log-uuid = gv_uuid.
  ls_log-tipo = 'S'.
  CONCATENATE 'Extracción completada:' lv_count_char 'registros procesados'
    INTO ls_log-mensaje SEPARATED BY space.
  ls_log-fecha = sy-datum.
  ls_log-hora = sy-uzeit.
  ls_log-usuario = sy-uname.
  ls_log-programa = sy-repid.
  APPEND ls_log TO gt_log.

  " Mensaje al usuario
  MESSAGE s004(zfiaac001) WITH lv_count_char.

ENDFORM.

*&---------------------------------------------------------------------*
*& Form f_mostrar_alv
*&---------------------------------------------------------------------*
FORM f_mostrar_alv.


  CALL FUNCTION 'ZCX_MOSTRARALV_01'
    EXPORTING
      i_ti_datos = REF #( gt_resultado )
*     i_ti_catalogo        =
*     i_status_gui         =
*     i_statusgui_programa =
*     i_titulo   =
*     i_o_ctr_alvgrid      =
*     i_id_reporte         = SY-REPID
*     i_es_layout          =
*     i_es_variante        =
*     i_nombreusuario      = SY-UNAME
*     i_conservar_catalogo = space
*     i_persistir_dynpro   = space
*     i_ti_sort  =
*     i_o_html   =
    .


ENDFORM.

*&---------------------------------------------------------------------*
*& Form f_descargar_csv_local
*&---------------------------------------------------------------------*
FORM f_descargar_csv_local.

  DATA: lv_filename   TYPE  localfile,
        lv_filename_s TYPE string,
        lt_csv        TYPE TABLE OF string,
        lv_path       TYPE string,
        lv_line       TYPE string,
        lv_count      TYPE i.

  IF gt_resultado IS INITIAL.
    MESSAGE w008(zfiaac001) WITH 'No hay datos para exportar'.
    RETURN.
  ENDIF.

  CALL FUNCTION 'GUI_FILE_SAVE_DIALOG'
    EXPORTING
      default_extension = 'csv'
      default_file_name = 'FIAAC001_Activos.csv'
    IMPORTING
      filename          = lv_filename_s
      path              = lv_path.

  IF lv_filename_s IS INITIAL.
    MESSAGE i017(zfiaac001) WITH 'Operacion cancelada por el usuario'.
    RETURN.
  ENDIF.

  LOOP AT gt_resultado ASSIGNING FIELD-SYMBOL(<fs_resultado>).
    lv_line = zclcx_util=>es_a_string(
      EXPORTING
        i_es = <fs_resultado>
        i_separador = ','
    ).
    APPEND lv_line TO lt_csv.
  ENDLOOP.

  lv_filename = lv_filename_s.

  " Descargar usando WS_DOWNLOAD directamente (más confiable)
  CALL FUNCTION 'WS_DOWNLOAD'
    EXPORTING
      filename                = lv_filename
      filetype                = 'ASC'
      codepage                = '4110'
    TABLES
      data_tab                = lt_csv
    EXCEPTIONS
      file_write_error        = 1
      no_batch                = 2
      gui_refuse_filetransfer = 3
      invalid_type            = 4
      no_authority            = 5
      unknown_error           = 6
      header_not_allowed      = 7
      separator_not_allowed   = 8
      filesize_not_allowed    = 9
      header_too_long         = 10
      dp_error_create         = 11
      dp_error_send           = 12
      dp_error_write          = 13
      unknown_dp_error        = 14
      access_denied           = 15
      dp_out_of_memory        = 16
      disk_full               = 17
      dp_timeout              = 18
      file_not_found          = 19
      dataprovider_exception  = 20
      control_flush_error     = 21
      OTHERS                  = 22.

  IF sy-subrc = 0.
    lv_count = lines( gt_resultado ).
    MESSAGE s019(zfiaac001) WITH 'CSV descargado exitosamente:' lv_count 'registros en' lv_filename.
  ELSE.
    MESSAGE e018(zfiaac001) WITH 'Error al descargar archivo CSV' sy-subrc.
  ENDIF.

ENDFORM.
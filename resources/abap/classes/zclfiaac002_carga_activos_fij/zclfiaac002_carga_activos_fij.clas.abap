CLASS zclfiaac002_carga_activos_fij DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    CONSTANTS: gc_cls_msg           TYPE msgtyp VALUE 'ZFI01',
               gc_cls_msg_cx        TYPE msgtyp VALUE 'ZCX01',
               gc_cls_mov_anulacion TYPE bapi6037_rev_data-reason_rev VALUE '01',
               gc_mov_fi            TYPE awtyp VALUE 'AS91',
               gc_mov_af            TYPE awtyp VALUE 'AMBU'.

    TYPES tp_ti_bapi1022_dep_areas TYPE STANDARD TABLE OF bapi1022_dep_areas WITH EMPTY KEY.
    TYPES tp_ti_bapi1022_dep_areasx TYPE STANDARD TABLE OF bapi1022_dep_areasx WITH EMPTY KEY.
    TYPES tp_ti_bapi1022_cumval TYPE STANDARD TABLE OF bapi1022_cumval WITH EMPTY KEY.
    TYPES tp_ti_bapi1022_postval TYPE STANDARD TABLE OF bapi1022_postval WITH EMPTY KEY.

    TYPES: BEGIN OF tp_es_log,
             icon TYPE icon_d.
             INCLUDE TYPE ztfiaacoo2_1.
           TYPES: END OF tp_es_log.

    TYPES: tp_ti_log  TYPE STANDARD TABLE OF tp_es_log WITH EMPTY KEY,
           tp_r_anln1 TYPE RANGE OF anla-anln1,
           tp_r_anln2 TYPE RANGE OF anla-anln2,
           tp_r_flnam TYPE RANGE OF ztfiaac002_1-flname,
           tp_r_fecha TYPE RANGE OF ztfiaac002_1-fecha,
           tp_r_hora  TYPE RANGE OF ztfiaac002_1-hora,
           tp_r_tipo  TYPE RANGE OF ztfiaac002_1-type.

    TYPES: BEGIN OF tp_es_mov_af,
             bukrs TYPE bukrs,
             anln1 TYPE anla-anln1,
             anln2 TYPE anla-anln2,
             afabe TYPE anlc-afabe,
             budat TYPE bkpf-budat,
             poper TYPE string, "faa_lineitems-poper,
             awtyp TYPE bkpf-awtyp,
             awref TYPE string, "faa_lineitems-awref,
             aworg TYPE string, "faa_lineitems-aworg,
             belnr TYPE bkpf-belnr,
             gjahr TYPE bkpf-gjahr,
           END OF tp_es_mov_af,

           tp_ti_mov_af TYPE STANDARD TABLE OF tp_es_mov_af WITH NON-UNIQUE KEY bukrs anln1 anln2.

    METHODS:
      "!
      "! @parameter i_ti_dat_actf |
      "! @parameter i_test |
      "! @parameter r_ti_log |
      crear_modificar_activo_fijo
        IMPORTING
                  VALUE(i_ti_dat_actf) TYPE zttfiaac002_2
                  i_test               TYPE testrun
                  i_debug              TYPE flag DEFAULT space
        RETURNING VALUE(r_ti_log)      TYPE zttfiaac002_1.
    CLASS-METHODS:
      "!
      "! @parameter i_o_grid_log_handler |
      "! @parameter i_status_gui |
      "! @parameter i_statusgui_prog |
      "! @parameter i_cons_screen |
      "! @parameter c_ti_bapireturn |
      presentar_log_crea_activofijo
        IMPORTING
          i_o_grid_log_handler TYPE REF TO zifcx_alvgrid OPTIONAL
          i_status_gui         TYPE string OPTIONAL
          i_statusgui_prog     TYPE syrepid OPTIONAL
          i_cons_screen        TYPE flag OPTIONAL
        CHANGING
          c_ti_bapireturn      TYPE tp_ti_log,
      mostrar_resultados_log
        IMPORTING
          i_conservar_screen   TYPE flag
          i_r_asset            TYPE tp_r_anln1 OPTIONAL
          i_r_flnam            TYPE tp_r_flnam OPTIONAL
          i_r_fecha            TYPE tp_r_fecha OPTIONAL
          i_r_hora             TYPE tp_r_hora OPTIONAL
          i_r_tipo             TYPE tp_r_tipo OPTIONAL
          i_o_grid_log_handler TYPE REF TO zifcx_alvgrid OPTIONAL
        CHANGING
          c_ti_log             TYPE tp_ti_log.

    METHODS:
      consultar_ti_movimientos_af
        IMPORTING
          i_bukrs            TYPE bukrs
          i_r_anln1          TYPE tp_r_anln1
          i_r_anln2          TYPE tp_r_anln2
          i_gjahr            TYPE gjahr
        RETURNING
          VALUE(r_ti_mov_af) TYPE tp_ti_mov_af,

      "!
      "! @parameter i_testrun |
      "! @parameter i_ti_mov_af |
      anular_movimientos
        IMPORTING
                  i_testrun          TYPE    bapi1022_misc-testrun
                  VALUE(i_ti_mov_af) TYPE tp_ti_mov_af
        RETURNING VALUE(r_ti_return) TYPE bapiret2_t.
  PROTECTED SECTION.
  PRIVATE SECTION.

    METHODS:
      "!
      "! @parameter i_es_key |
      "! @parameter i_createsubnumber |
      "! @parameter i_creategroupasset |
      "! @parameter i_testrun |
      "! @parameter i_es_generaldata |
      "! @parameter i_es_generaldatax |
      "! @parameter i_es_inventory |
      "! @parameter i_es_inventoryx |
      "! @parameter i_es_postinginformation |
      "! @parameter i_es_postinginformationx |
      "! @parameter i_es_timedependentdata |
      "! @parameter i_es_timedependentdatax |
      "! @parameter i_es_allocations |
      "! @parameter i_es_allocationsx |
      "! @parameter i_es_origin |
      "! @parameter i_es_originx |
      "! @parameter i_ti_depreciationareas |
      "! @parameter i_ti_depreciationareasx |
      "! @parameter i_ti_cumulatedvalues |
      "! @parameter i_ti_postedvalues |
      "! @parameter e_companycode |
      "! @parameter e_asset |
      "! @parameter e_subnumber |
      "! @parameter e_assetcreated |
      "! @parameter e_ti_return |
      ejecutar_bapi_crear
        IMPORTING
          i_es_key                 TYPE    bapi1022_key
          i_createsubnumber        TYPE    bapi1022_misc-xsubno
          i_creategroupasset       TYPE    bapi1022_misc-xanlgr OPTIONAL
          i_testrun                TYPE    bapi1022_misc-testrun
          i_es_generaldata         TYPE    bapi1022_feglg001
          i_es_generaldatax        TYPE    bapi1022_feglg001x
          i_es_inventory           TYPE    bapi1022_feglg011
          i_es_inventoryx          TYPE    bapi1022_feglg011x
          i_es_postinginformation  TYPE    bapi1022_feglg002
          i_es_postinginformationx TYPE    bapi1022_feglg002x
          i_es_timedependentdata   TYPE    bapi1022_feglg003
          i_es_timedependentdatax  TYPE    bapi1022_feglg003x
          i_es_allocations         TYPE    bapi1022_feglg004
          i_es_allocationsx        TYPE    bapi1022_feglg004x
          i_es_origin              TYPE    bapi1022_feglg009
          i_es_originx             TYPE    bapi1022_feglg009x
          i_ti_depreciationareas   TYPE    tp_ti_bapi1022_dep_areas
          i_ti_depreciationareasx  TYPE    tp_ti_bapi1022_dep_areasx
          i_ti_cumulatedvalues     TYPE    tp_ti_bapi1022_cumval
          i_ti_postedvalues        TYPE    tp_ti_bapi1022_postval
        EXPORTING
          e_companycode            TYPE    bapi1022_1-comp_code
          e_asset                  TYPE    bapi1022_1-assetmaino
          e_subnumber              TYPE    bapi1022_1-assetsubno
          e_assetcreated           TYPE    bapi1022_reference
          e_ti_return              TYPE    bapiret2_t.

    METHODS:
      "!
      "! @parameter i_es_key |
      "! @parameter i_createsubnumber |
      "! @parameter i_creategroupasset |
      "! @parameter i_testrun |
      "! @parameter i_es_generaldata |
      "! @parameter i_es_generaldatax |
      "! @parameter i_es_inventory |
      "! @parameter i_es_inventoryx |
      "! @parameter i_es_postinginformation |
      "! @parameter i_es_postinginformationx |
      "! @parameter i_es_timedependentdata |
      "! @parameter i_es_timedependentdatax |
      "! @parameter i_es_allocations |
      "! @parameter i_es_allocationsx |
      "! @parameter i_es_origin |
      "! @parameter i_es_originx |
      "! @parameter i_ti_depreciationareas |
      "! @parameter i_ti_depreciationareasx |
      "! @parameter i_ti_cumulatedvalues |
      "! @parameter i_ti_postedvalues |
      "! @parameter e_companycode |
      "! @parameter e_asset |
      "! @parameter e_subnumber |
      "! @parameter e_assetcreated |
      "! @parameter e_ti_return |
      ejecutar_bapi_modificar
        IMPORTING
          i_es_key                 TYPE    bapi1022_key
          i_createsubnumber        TYPE    bapi1022_misc-xsubno
          i_creategroupasset       TYPE    bapi1022_misc-xanlgr OPTIONAL
          i_testrun                TYPE    bapi1022_misc-testrun
          i_es_generaldata         TYPE    bapi1022_feglg001
          i_es_generaldatax        TYPE    bapi1022_feglg001x
          i_es_inventory           TYPE    bapi1022_feglg011
          i_es_inventoryx          TYPE    bapi1022_feglg011x
          i_es_postinginformation  TYPE    bapi1022_feglg002
          i_es_postinginformationx TYPE    bapi1022_feglg002x
          i_es_timedependentdata   TYPE    bapi1022_feglg003
          i_es_timedependentdatax  TYPE    bapi1022_feglg003x
          i_es_allocations         TYPE    bapi1022_feglg004
          i_es_allocationsx        TYPE    bapi1022_feglg004x
          i_es_origin              TYPE    bapi1022_feglg009
          i_es_originx             TYPE    bapi1022_feglg009x
          i_ti_depreciationareas   TYPE    tp_ti_bapi1022_dep_areas
          i_ti_depreciationareasx  TYPE    tp_ti_bapi1022_dep_areasx
          i_ti_cumulatedvalues     TYPE    tp_ti_bapi1022_cumval
          i_ti_postedvalues        TYPE    tp_ti_bapi1022_postval
        EXPORTING
          e_companycode            TYPE    bapi1022_1-comp_code
          e_asset                  TYPE    bapi1022_1-assetmaino
          e_subnumber              TYPE    bapi1022_1-assetsubno
          e_assetcreated           TYPE    bapi1022_reference
          e_ti_return              TYPE    bapiret2_t.

    METHODS:
      "!
      "! @parameter i_bukrs |
      "! @parameter i_anln1 |
      "! @parameter i_anln2 |
      "! @parameter r_ti_mov_af |
      consultar_movimientos_af
        IMPORTING
          i_bukrs            TYPE anla-bukrs
          i_anln1            TYPE anla-anln1
          i_anln2            TYPE anla-anln2
        RETURNING
          VALUE(r_ti_mov_af) TYPE tp_ti_mov_af.

    METHODS:
      mp_key
        IMPORTING
                  i_es_activofijo TYPE zefiaac002_1
        RETURNING VALUE(r_es_key) TYPE bapi1022_key.

    METHODS:
      mp_generaldata
        IMPORTING
          i_es_activofijo   TYPE zefiaac002_1
        CHANGING
          c_es_generaldata  TYPE bapi1022_feglg001
          c_es_generaldatax TYPE bapi1022_feglg001x.

    METHODS:
      mp_inventory
        IMPORTING
          i_es_activofijo TYPE zefiaac002_1
        CHANGING
          c_es_inventory  TYPE bapi1022_feglg011
          c_es_inventoryx TYPE bapi1022_feglg011x.

    METHODS:
      mp_postinginformation
        IMPORTING
          i_es_activofijo          TYPE zefiaac002_1
        CHANGING
          c_es_postinginformation  TYPE bapi1022_feglg002
          c_es_postinginformationx TYPE bapi1022_feglg002x.

    METHODS:
      mp_timedependentdata
        IMPORTING
          i_es_activofijo         TYPE zefiaac002_1
        CHANGING
          c_es_timedependentdata  TYPE bapi1022_feglg003
          c_es_timedependentdatax TYPE bapi1022_feglg003x.

    METHODS:
      mp_allocations
        IMPORTING
          i_es_activofijo   TYPE zefiaac002_1
        CHANGING
          c_es_allocations  TYPE bapi1022_feglg004
          c_es_allocationsx TYPE bapi1022_feglg004x.

    METHODS:
      mp_origin
        IMPORTING
          i_es_activofijo TYPE zefiaac002_1
        CHANGING
          c_es_origin     TYPE bapi1022_feglg009
          c_es_originx    TYPE bapi1022_feglg009x.

    METHODS:
      mp_depreciationareas
        IMPORTING
          i_es_activofijo         TYPE zefiaac002_1
        CHANGING
          c_ti_depreciationareas  TYPE tp_ti_bapi1022_dep_areas
          c_ti_depreciationareasx TYPE tp_ti_bapi1022_dep_areasx.

    METHODS:
      mp_cumulatedvalues
        IMPORTING
          i_es_activofijo      TYPE zefiaac002_1
        CHANGING
          c_ti_cumulatedvalues TYPE tp_ti_bapi1022_cumval.

    METHODS:
      mp_postedvalues
        IMPORTING
          i_es_activofijo   TYPE zefiaac002_1
        CHANGING
          c_ti_postedvalues TYPE tp_ti_bapi1022_postval.

    CLASS-DATA: o_log TYPE REF TO zclcx_log_aplicacion.
ENDCLASS.



CLASS zclfiaac002_carga_activos_fij IMPLEMENTATION.


  METHOD anular_movimientos.

    DATA: es_origindocreference TYPE bapi6037_doc_ref,
          es_origindocumentkey  TYPE bapi6037_doc_key,
          es_reversaldata       TYPE bapi6037_rev_data,
          es_documentreference  TYPE bapi6037_doc_ref,
          es_return             TYPE bapiret2.

    DATA(o_log) = zclcx_log_aplicacion=>get_instancia( ).

    LOOP AT i_ti_mov_af ASSIGNING FIELD-SYMBOL(<fs_es_faa_lineitems>).

      CLEAR: es_origindocreference, es_origindocumentkey, es_reversaldata, es_return, es_documentreference.

      "determinar tipo de accion
      CASE <fs_es_faa_lineitems>-awtyp.
        WHEN gc_mov_fi.
          es_origindocumentkey-comp_code = <fs_es_faa_lineitems>-bukrs.
          es_origindocumentkey-ac_doc_no = <fs_es_faa_lineitems>-belnr.
          es_origindocumentkey-fisc_year = <fs_es_faa_lineitems>-gjahr.
        WHEN gc_mov_af.
          es_origindocreference-obj_type = <fs_es_faa_lineitems>-awtyp.
          es_origindocreference-ref_doc = <fs_es_faa_lineitems>-awref.
          es_origindocreference-ref_org_un = <fs_es_faa_lineitems>-aworg.
      ENDCASE.

      es_reversaldata-fisc_year = <fs_es_faa_lineitems>-gjahr.
      es_reversaldata-pstng_date = <fs_es_faa_lineitems>-budat.
      es_reversaldata-fis_period = <fs_es_faa_lineitems>-poper.
      es_reversaldata-reason_rev = gc_cls_mov_anulacion.

      CALL FUNCTION 'BAPI_ASSET_REVERSAL_POST'
        EXPORTING
          origindocreference = es_origindocreference    " Reference to Document in Accounting
          origindocumentkey  = es_origindocumentkey     " Key of a Document in Accounting
          reversaldata       = es_reversaldata    " Data on Document Reversal
        IMPORTING
          documentreference  = es_documentreference    " Reference to Generated Document in Accounting
          return             = es_return.     " Return Parameters

      IF es_return-id = 'AA' AND es_return-number = '462'.
        es_return-type = zclcx_util=>gc_w.
        es_return-id = 'ZFI01'.
        es_return-number = '026'.
        es_return-message = space.
        es_return-message_v1 = es_origindocumentkey-comp_code.
        es_return-message_v2 = es_origindocumentkey-ac_doc_no.
        es_return-message_v3 = es_origindocumentkey-fisc_year.
      ENDIF.

      o_log->set_es_log( CORRESPONDING #( es_return ) ).

      APPEND es_return TO r_ti_return.

      IF i_testrun = abap_false AND es_return-type NE zclcx_util=>gc_e.
        CALL FUNCTION 'BAPI_TRANSACTION_COMMIT'
          EXPORTING
            wait = abap_true.     " Use of Command `COMMIT AND WAIT`
      ELSE.
        CALL FUNCTION 'BAPI_TRANSACTION_ROLLBACK'.
      ENDIF.
    ENDLOOP.
  ENDMETHOD.


  METHOD consultar_movimientos_af.

    ".consultar movimientos
*    SELECT bukrs, anln1, anln2, afabe, budat, poper, awtyp, awref, aworg, belnr, gjahr
*    FROM faa_lineitems
*    WHERE bukrs = @i_bukrs AND
*          anln1 = @i_anln1 AND
*          anln2 = @i_anln2 AND
*          awtyp_rev = @space "movimientos NO anulados
*    ORDER BY awtyp ASCENDING
*    INTO CORRESPONDING FIELDS OF TABLE @r_ti_mov_af.

  ENDMETHOD.


  METHOD consultar_ti_movimientos_af.
    ".consultar movimientos
*    SELECT bukrs, anln1, anln2, afabe, budat, poper, awtyp, awref, aworg, belnr, gjahr
*    FROM faa_lineitems
*    WHERE bukrs =  @i_bukrs AND
*          anln1 IN @i_r_anln1 AND
*          anln2 IN @i_r_anln2 AND
*          gjahr =  @i_gjahr AND
*          awtyp_rev = @space "movimientos NO anulados
*    ORDER BY awtyp ASCENDING
*    INTO CORRESPONDING FIELDS OF TABLE @r_ti_mov_af.
  ENDMETHOD.


  METHOD crear_modificar_activo_fijo.
    "filtra registros marcados
    DATA:
      asset                  TYPE bapi1022_key-asset, "numero de activo fijo creado
      es_key                 TYPE bapi1022_key,
      es_generaldata         TYPE bapi1022_feglg001,
      es_generaldatax        TYPE bapi1022_feglg001x,
      es_inventory           TYPE bapi1022_feglg011,
      es_inventoryx          TYPE bapi1022_feglg011x,
      es_postinginformation  TYPE bapi1022_feglg002,
      es_postinginformationx TYPE bapi1022_feglg002x,
      es_timedependentdata   TYPE bapi1022_feglg003,
      es_timedependentdatax  TYPE bapi1022_feglg003x,
      es_allocations         TYPE bapi1022_feglg004,
      es_allocationsx        TYPE bapi1022_feglg004x,
      es_origin              TYPE bapi1022_feglg009,
      es_originx             TYPE bapi1022_feglg009x,
      ti_cumulatedvalues     TYPE tp_ti_bapi1022_cumval,
      ti_depreciationareas   TYPE tp_ti_bapi1022_dep_areas,
      ti_depreciationareasx  TYPE tp_ti_bapi1022_dep_areasx,
      ti_postedvalues        TYPE tp_ti_bapi1022_postval,
      ti_returnmessages      TYPE cfx_bi_tt_bapi_matreturn2,
      ti_tp_mov_af           TYPE tp_ti_mov_af.


    IF o_log IS BOUND. o_log->liberar( ). ENDIF.
    o_log = zclcx_log_aplicacion=>get_instancia( ).

    IF i_debug = abap_true.
      BREAK-POINT.
    ENDIF.

    "DATA(o_homologacion) = NEW zclmmi1009_homologacion_campos( i_c_ricefw = 'C1009' ).

    LOOP AT i_ti_dat_actf ASSIGNING FIELD-SYMBOL(<fs_es_activofijo>).

      CLEAR: ti_returnmessages.

      "o_homologacion->homologar_campo( CHANGING c_es = <fs_es_activofijo> ).

      "recupera datos de cabecera y asigna valores a extender
*      es_dt_mat = i_ti_dat_actf

      AT NEW subnumber.
        "realiza rollback en modo test cada que se intente crear un conjunto de datos
        IF i_test EQ abap_true.
          CALL FUNCTION 'BAPI_TRANSACTION_ROLLBACK'.
        ENDIF.

        es_key = mp_key( <fs_es_activofijo> ).

        mp_generaldata(
          EXPORTING
            i_es_activofijo   = <fs_es_activofijo>
          CHANGING
            c_es_generaldata  = es_generaldata
            c_es_generaldatax = es_generaldatax ).

        mp_postinginformation(
          EXPORTING
            i_es_activofijo          = <fs_es_activofijo>
          CHANGING
            c_es_postinginformation  = es_postinginformation
            c_es_postinginformationx = es_postinginformationx ).

        mp_timedependentdata(
          EXPORTING
            i_es_activofijo         =  <fs_es_activofijo>
          CHANGING
            c_es_timedependentdata  = es_timedependentdata
            c_es_timedependentdatax = es_timedependentdatax ).

        mp_allocations(
          EXPORTING
            i_es_activofijo   = <fs_es_activofijo>
          CHANGING
            c_es_allocations  = es_allocations
            c_es_allocationsx = es_allocationsx ).

        mp_inventory(
          EXPORTING
            i_es_activofijo = <fs_es_activofijo>
          CHANGING
            c_es_inventory  = es_inventory
            c_es_inventoryx = es_inventoryx ).

        mp_origin(
          EXPORTING
            i_es_activofijo = <fs_es_activofijo>
          CHANGING
            c_es_origin     = es_origin
            c_es_originx    = es_originx ).
      ENDAT.

      mp_depreciationareas(
        EXPORTING
          i_es_activofijo         = <fs_es_activofijo>
        CHANGING
          c_ti_depreciationareas  = ti_depreciationareas
          c_ti_depreciationareasx = ti_depreciationareasx ).

      mp_cumulatedvalues(
        EXPORTING
          i_es_activofijo      = <fs_es_activofijo>
        CHANGING
           c_ti_cumulatedvalues = ti_cumulatedvalues ).

      mp_postedvalues(
        EXPORTING
          i_es_activofijo   = <fs_es_activofijo>
        CHANGING
          c_ti_postedvalues = ti_postedvalues ).

      AT END OF subnumber.
        CLEAR asset.
        "determinar proceso a ejecutar
        ti_tp_mov_af = consultar_movimientos_af( i_bukrs = es_key-companycode i_anln1 = es_key-asset i_anln2 = es_key-subnumber ).

        IF ti_tp_mov_af IS NOT INITIAL.

          "anular movimientos
          anular_movimientos( i_testrun = i_test i_ti_mov_af = ti_tp_mov_af ).
          "modificar activo fijo
          ejecutar_bapi_modificar(
            EXPORTING
              i_es_key                 = es_key
              i_createsubnumber        = COND #( WHEN es_key-subnumber = '0000' THEN abap_false ELSE abap_true )
              i_testrun                = i_test
              i_es_generaldata         = es_generaldata
              i_es_generaldatax        = es_generaldatax
              i_es_inventory           = es_inventory
              i_es_inventoryx          = es_inventoryx
              i_es_postinginformation  = es_postinginformation
              i_es_postinginformationx = es_postinginformationx
              i_es_timedependentdata   = es_timedependentdata
              i_es_timedependentdatax  = es_timedependentdatax
              i_es_allocations         = es_allocations
              i_es_allocationsx        = es_allocationsx
              i_es_origin              = es_origin
              i_es_originx             = es_originx
              i_ti_depreciationareas   = ti_depreciationareas
              i_ti_depreciationareasx  = ti_depreciationareasx
              i_ti_cumulatedvalues     = ti_cumulatedvalues
              i_ti_postedvalues        = ti_postedvalues
            IMPORTING
              e_ti_return              = ti_returnmessages
              e_asset                  = asset ).
        ELSE.
          "ejecutar bapi
          ejecutar_bapi_crear(
            EXPORTING
              i_es_key                 = es_key
              i_createsubnumber        = COND #( WHEN es_key-subnumber = '0000' THEN abap_false ELSE abap_true )
              i_testrun                = i_test
              i_es_generaldata         = es_generaldata
              i_es_generaldatax        = es_generaldatax
              i_es_inventory           = es_inventory
              i_es_inventoryx          = es_inventoryx
              i_es_postinginformation  = es_postinginformation
              i_es_postinginformationx = es_postinginformationx
              i_es_timedependentdata   = es_timedependentdata
              i_es_timedependentdatax  = es_timedependentdatax
              i_es_allocations         = es_allocations
              i_es_allocationsx        = es_allocationsx
              i_es_origin              = es_origin
              i_es_originx             = es_originx
              i_ti_depreciationareas   = ti_depreciationareas
              i_ti_depreciationareasx  = ti_depreciationareasx
              i_ti_cumulatedvalues     = ti_cumulatedvalues
              i_ti_postedvalues        = ti_postedvalues
            IMPORTING
              e_ti_return              = ti_returnmessages
              e_asset                  = asset ).

        ENDIF.

        IF line_exists( ti_returnmessages[ type = zclcx_util=>gc_e ] ) OR
           ( i_test EQ abap_true ).
          CALL FUNCTION 'BAPI_TRANSACTION_ROLLBACK'.
        ELSEIF i_test EQ abap_false.
          CALL FUNCTION 'BAPI_TRANSACTION_COMMIT'
            EXPORTING
              wait = abap_true.     " Use of Command `COMMIT AND WAIT`
        ENDIF.

        "anadir msg de o_log
        DATA(ti_o_log) = o_log->get_log( ).

        r_ti_log = CORRESPONDING #( BASE ( r_ti_log )  ti_o_log MAPPING idmsg = id numero = number ).
        "Consolidar log
        r_ti_log = CORRESPONDING zttfiaac002_1( BASE ( r_ti_log ) ti_returnmessages MAPPING idmsg = id numero = number ).
        MODIFY r_ti_log FROM VALUE #( anln1 = <fs_es_activofijo>-asset anln1_c = asset anln2 = es_key-subnumber bukrs = es_key-companycode )
        TRANSPORTING anln1 anln1_c anln2 bukrs
        WHERE anln1 = space.

        o_log->liberar( ).
        o_log->remover_mensage(  ).

        CLEAR: es_key, es_generaldata, es_generaldatax, es_inventory, es_inventoryx, es_postinginformation, es_postinginformationx,
               es_timedependentdata, es_timedependentdatax, es_allocations, es_allocationsx, es_origin, es_originx,
               ti_depreciationareas, ti_depreciationareasx, ti_cumulatedvalues, ti_postedvalues.

      ENDAT.

    ENDLOOP.

  ENDMETHOD.


  METHOD ejecutar_bapi_crear.
    CALL FUNCTION 'BAPI_FIXEDASSET_OVRTAKE_CREATE'
      EXPORTING
        key                 = i_es_key
*       reference           =
        createsubnumber     = i_createsubnumber
        creategroupasset    = i_creategroupasset
        testrun             = i_testrun
        generaldata         = i_es_generaldata
        generaldatax        = i_es_generaldatax
        inventory           = i_es_inventory
        inventoryx          = i_es_inventoryx
        postinginformation  = i_es_postinginformation
        postinginformationx = i_es_postinginformationx
        timedependentdata   = i_es_timedependentdata
        timedependentdatax  = i_es_timedependentdatax
        allocations         = i_es_allocations
        allocationsx        = i_es_allocationsx
        origin              = i_es_origin
        originx             = i_es_originx
      IMPORTING
        companycode         = e_companycode
        asset               = e_asset
        subnumber           = e_subnumber
        assetcreated        = e_assetcreated
      TABLES
        depreciationareas   = i_ti_depreciationareas
        depreciationareasx  = i_ti_depreciationareasx
        cumulatedvalues     = i_ti_cumulatedvalues
        "postedvalues        = i_ti_postedvalues "NO APLICA PARA CARGAS INICIALES
        return              = e_ti_return.
  ENDMETHOD.


  METHOD ejecutar_bapi_modificar.
    DATA: es_return TYPE bapiret2.

    "modificar datos de activo fijo
    CALL FUNCTION 'BAPI_FIXEDASSET_CHANGE'
      EXPORTING
        companycode         = i_es_key-companycode
        asset               = i_es_key-asset
        subnumber           = i_es_key-subnumber
        generaldata         = i_es_generaldata
        generaldatax        = i_es_generaldatax
        inventory           = i_es_inventory
        inventoryx          = i_es_inventoryx
        postinginformation  = i_es_postinginformation
        postinginformationx = i_es_postinginformationx
        timedependentdata   = i_es_timedependentdata
        timedependentdatax  = i_es_timedependentdatax
        allocations         = i_es_allocations
        allocationsx        = i_es_allocationsx
        origin              = i_es_origin
        originx             = i_es_originx
      IMPORTING
        return              = es_return
      TABLES
        depreciationareas   = i_ti_depreciationareas
        depreciationareasx  = i_ti_depreciationareasx.

    IF es_return-type = zclcx_util=>gc_e OR i_testrun EQ abap_true.
      CALL FUNCTION 'BAPI_TRANSACTION_ROLLBACK'.
    ELSEIF i_testrun EQ abap_false.
      CALL FUNCTION 'BAPI_TRANSACTION_COMMIT'
        EXPORTING
          wait = abap_true.     " Use of Command `COMMIT AND WAIT`
    ENDIF.

    IF es_return-type = zclcx_util=>gc_e.
      es_return-type = zclcx_util=>gc_w.
    ENDIF.

    "modificar valores de contabilización
    CALL FUNCTION 'BAPI_FIXEDASSET_OVRTAKE_POST'
      EXPORTING
        key             = i_es_key
        testrun         = i_testrun
      TABLES
        cumulatedvalues = i_ti_cumulatedvalues
        postedvalues    = i_ti_postedvalues
        return          = e_ti_return.

    APPEND es_return TO e_ti_return.

  ENDMETHOD.


  METHOD mostrar_resultados_log.
    "valida que almenos se haya ingresado un dato en la pantalla de seleccion
    IF i_r_asset[] IS INITIAL AND i_r_fecha IS INITIAL AND
       i_r_hora IS INITIAL AND i_r_tipo IS INITIAL.

      MESSAGE s024(zcx01) DISPLAY LIKE zclcx_util=>gc_e.
      LEAVE LIST-PROCESSING.
    ELSE.

      SELECT *
      FROM ztfiaac002_1
      WHERE anln1 IN @i_r_asset[] AND
            fecha IN @i_r_fecha[] AND
            hora IN @i_r_hora[] AND
            type IN @i_r_tipo[] AND
            flname IN @i_r_flnam
      INTO CORRESPONDING FIELDS OF TABLE @c_ti_log.

      IF sy-subrc IS INITIAL.

        presentar_log_crea_activofijo( EXPORTING
                                          i_status_gui     = 'GS_0001'
                                          i_statusgui_prog = 'ZFIC1009_2'
                                          i_o_grid_log_handler = i_o_grid_log_handler
                                          i_cons_screen        = i_conservar_screen
                                        CHANGING
                                            c_ti_bapireturn = c_ti_log ).
      ELSE.
        MESSAGE s002(wusl) DISPLAY LIKE zclcx_util=>gc_e.
        LEAVE LIST-PROCESSING.
      ENDIF.
    ENDIF.
  ENDMETHOD.


  METHOD mp_allocations.
    c_es_allocations = CORRESPONDING #( i_es_activofijo ).

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = c_es_allocations
      CHANGING
        c_es_destino = c_es_allocationsx ).
  ENDMETHOD.


  METHOD mp_cumulatedvalues.
    c_ti_cumulatedvalues = VALUE #( BASE c_ti_cumulatedvalues  ( CORRESPONDING #( i_es_activofijo ) ) ).

    SORT c_ti_cumulatedvalues BY fisc_year area.
    DELETE ADJACENT DUPLICATES FROM c_ti_cumulatedvalues COMPARING fisc_year area.
  ENDMETHOD.


  METHOD mp_depreciationareas.

    DATA: es_depreciationareas  LIKE LINE OF c_ti_depreciationareas,
          es_depreciationareasx LIKE LINE OF c_ti_depreciationareasx.

    es_depreciationareas = CORRESPONDING #( i_es_activofijo EXCEPT descript ).

    APPEND es_depreciationareas TO c_ti_depreciationareas.

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = es_depreciationareas
      CHANGING
        c_es_destino = es_depreciationareasx
    ).

    IF es_depreciationareasx-scrapvalue IS INITIAL.
      CLEAR: es_depreciationareasx-currency.
    ENDIF.

    APPEND es_depreciationareasx TO c_ti_depreciationareasx.

  ENDMETHOD.


  METHOD mp_generaldata.
    c_es_generaldata = CORRESPONDING #( i_es_activofijo ).

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = c_es_generaldata
      CHANGING
        c_es_destino = c_es_generaldatax ).
  ENDMETHOD.


  METHOD mp_inventory.
    c_es_inventory = CORRESPONDING #( i_es_activofijo ).

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = c_es_inventory
      CHANGING
        c_es_destino = c_es_inventoryx ).
  ENDMETHOD.


  METHOD mp_key.
    r_es_key-companycode = i_es_activofijo-companycode.
    "Se comenta temporalmente, numeracion interna
    "r_es_key-asset = i_es_activofijo-asset.
    r_es_key-subnumber = i_es_activofijo-subnumber.
  ENDMETHOD.


  METHOD mp_origin.
    c_es_origin = CORRESPONDING #( i_es_activofijo ).

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = c_es_origin
      CHANGING
        c_es_destino = c_es_originx ).

    IF c_es_originx-orig_value IS INITIAL.
      CLEAR: c_es_originx-currency.
    ENDIF.

  ENDMETHOD.


  METHOD mp_postedvalues.
    c_ti_postedvalues = VALUE #( BASE c_ti_postedvalues ( CORRESPONDING #( i_es_activofijo
                                                                            MAPPING ord_dep   = ord_dep_postval
                                                                                    fisc_year = fisc_year_postval
                                                                                    area      = area_postval
                                                                                    rev_repl  = rev_repl_postval ) ) ).
    SORT c_ti_postedvalues BY fisc_year area ord_dep DESCENDING rev_repl.
    DELETE ADJACENT DUPLICATES FROM c_ti_postedvalues COMPARING fisc_year area.
  ENDMETHOD.


  METHOD mp_postinginformation.
    c_es_postinginformation = CORRESPONDING #( i_es_activofijo ).

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = c_es_postinginformation
      CHANGING
        c_es_destino = c_es_postinginformationx ).
  ENDMETHOD.


  METHOD mp_timedependentdata.

    c_es_timedependentdata = CORRESPONDING #( i_es_activofijo ).

    CHECK c_es_timedependentdata IS NOT INITIAL.

    zclcx_util=>asignar_marcado_estructura(
      EXPORTING
        i_es_origen  = c_es_timedependentdata
      CHANGING
        c_es_destino = c_es_timedependentdatax ).
  ENDMETHOD.


  METHOD presentar_log_crea_activofijo.

    CALL FUNCTION 'ZCX_MOSTRARALV_01'
      EXPORTING
        i_ti_datos           = REF #( c_ti_bapireturn )
        i_o_ctr_alvgrid      = i_o_grid_log_handler
        i_status_gui         = i_status_gui
        i_statusgui_programa = i_statusgui_prog
        i_id_reporte         = sy-repid
        i_conservar_catalogo = abap_true
        i_persistir_dynpro   = i_cons_screen.

  ENDMETHOD.
ENDCLASS.

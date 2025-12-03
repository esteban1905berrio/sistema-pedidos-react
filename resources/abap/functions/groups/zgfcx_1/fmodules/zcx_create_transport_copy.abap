FUNCTION ZCX_CREATE_TRANSPORT_COPY
  IMPORTING
    VALUE(IV_TRANSPORT_REQUEST) TYPE STRING
    VALUE(IV_TARGET_SYSTEM) TYPE TMSCSYS-SYSNAM OPTIONAL
    VALUE(IV_DESCRIPTION_PREFIX) TYPE STRING DEFAULT 'COPIA'
    VALUE(IV_AUTO_RELEASE) TYPE CHAR1 DEFAULT ABAP_TRUE
  EXPORTING
    VALUE(EV_NEW_TRANSPORT) TYPE TRKORR
    VALUE(EV_STATUS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_LOG) TYPE STRING
  EXCEPTIONS
    TRANSPORT_NOT_FOUND
    CREATION_FAILED
    OBJECTS_COPY_FAILED
    RELEASE_FAILED.



  DATA: lo_transport_mgr     TYPE REF TO zclcx_transport_management,
        lv_description       TYPE trexreqhd-text,
        lt_transport_request TYPE STANDARD TABLE OF trkorr,
        lt_ot_db             TYPE RANGE OF trkorr,
        lt_ot                TYPE STANDARD TABLE OF string,
        lv_original_desc     TYPE string,
        lv_trkorr            TYPE trkorr.

  CONDENSE iv_transport_request NO-GAPS.
  SPLIT iv_transport_request AT ',' INTO TABLE lt_ot.
  lt_transport_request = CORRESPONDING #( lt_ot ).

  " Validate transport exists
  SELECT trkorr AS low
    FROM e070
    FOR ALL ENTRIES IN @lt_transport_request
    WHERE trkorr = @lt_transport_request-table_line
    INTO CORRESPONDING FIELDS OF TABLE @lt_ot_db.

  IF sy-subrc <> 0.
    ev_status = 'E'.
    ev_message = |Transport request { iv_transport_request } not found|.
    RAISE transport_not_found.
  ENDIF.

  MODIFY lt_ot_db FROM VALUE #( sign = 'I' option = 'EQ' ) TRANSPORTING sign option
  WHERE low IS NOT INITIAL.

  " Get original description
  SELECT  trkorr, as4text FROM e07t
    WHERE trkorr IN @lt_ot_db
      AND langu = @sy-langu
    INTO TABLE @DATA(lt_original_desc).

  IF lines( lt_original_desc ) > 1.

    LOOP AT lt_original_desc INTO DATA(_descripcion).
      lv_original_desc = |{ lv_original_desc } { _descripcion-trkorr }|.
    ENDLOOP.

  ELSE.
    lv_original_desc = VALUE #( lt_original_desc[ 1 ]-as4text OPTIONAL ).
  ENDIF.

  " Build description with 60 char limit
  DATA(lv_max_length) = 60 - strlen( iv_description_prefix ) - 2.
  IF strlen( lv_original_desc ) > lv_max_length.
    lv_description = |{ iv_description_prefix }: { lv_original_desc(lv_max_length) }|.
  ELSE.
    lv_description = |{ iv_description_prefix }: { lv_original_desc }|.
  ENDIF.

  " Get target system if not provided
  DATA(lv_target_system) = iv_target_system.
  IF lv_target_system IS INITIAL.
    SELECT SINGLE tarsystem FROM e070
      WHERE trkorr IN @lt_ot_db
      INTO @lv_target_system.
  ENDIF.

  " Create transport management instance
  CREATE OBJECT lo_transport_mgr.

  " Execute creation
  TRY.
      lo_transport_mgr->generar_orden_copia(
      EXPORTING
       i_sistema      = lv_target_system
       i_r_orden_origen = lt_ot_db
       i_descripcion  = lv_description
       i_prefijo      = iv_description_prefix
      IMPORTING
       e_orden_copia = ev_new_transport
       e_mensaje     = ev_log
     ).

      IF ev_new_transport IS NOT INITIAL.
        ev_status = 'S'.
        ev_message = |Transport copy { ev_new_transport } created successfully|.
      ELSE.
        ev_status = 'E'.
        ev_message = 'Failed to create transport copy'.
        RAISE creation_failed.
      ENDIF.

    CATCH cx_root INTO DATA(lx_error).
      ev_status = 'E'.
      ev_message = lx_error->get_text( ).
      RAISE creation_failed.
  ENDTRY.

ENDFUNCTION.

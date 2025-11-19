FUNCTION Z_CX_GET_TRANSPORT_INFO
  IMPORTING
    VALUE(IV_TRANSPORT_NUMBER) TYPE TRKORR
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_TRANSPORT_JSON) TYPE STRING.

*"----------------------------------------------------------------------
*"* Function Module: Z_CX_GET_TRANSPORT_INFO
*"*
*"* Purpose:
*"*   Get complete metadata for a transport request from E070 table
*"*   without loading the full object list. Lightweight alternative
*"*   to Z_CX_GET_TRANSPORT_OBJECTS for metadata-only queries.
*"*
*"* Progressive Discovery Integration:
*"*   - Token cost: ~500-800 tokens (much cheaper than full object list)
*"*   - Use when you need metadata only (owner, status, dates, etc.)
*"*   - For full object details, use Z_CX_GET_TRANSPORT_OBJECTS
*"*
*"* Input:
*"*   IV_TRANSPORT_NUMBER: Transport request number (main OT or task)
*"*                        Examples: 'CADK911088', 'DEVK900123'
*"*
*"* Output:
*"*   EV_SUCCESS: 'X' if successful, '' if failed
*"*   EV_MESSAGE: Error message if failed
*"*   EV_TRANSPORT_JSON: JSON string with transport metadata
*"*
*"* JSON Structure:
*"*   {
*"*     "success": true,
*"*     "transport_number": "CADK911088",
*"*     "transport_type": "K",
*"*     "transport_type_desc": "Workbench",
*"*     "status": "D",
*"*     "status_desc": "Modifiable",
*"*     "owner": "USERNAME",
*"*     "description": "Transport description",
*"*     "created_date": "2025-01-15",
*"*     "created_time": "14:30:45",
*"*     "target_system": "S4Q",
*"*     "category": "CUST",
*"*     "parent_transport": null,
*"*     "has_objects": true,
*"*     "has_tasks": true
*"*   }
*"*
*"* Author: Crystal Development Team
*"* Date: 2025-01-18
*"* Version: 1.0
*"----------------------------------------------------------------------

  DATA: lv_json TYPE string.
  DATA: ls_e070 TYPE e070.
  DATA: lv_type_desc TYPE string.
  DATA: lv_status_desc TYPE string.
  DATA: lv_created_date TYPE string.
  DATA: lv_created_time TYPE string.
  DATA: lv_parent TYPE string.
  DATA: lv_has_objects TYPE abap_bool.
  DATA: lv_has_tasks TYPE abap_bool.
  DATA: lv_object_count TYPE i.
  DATA: lv_task_count TYPE i.

  " Initialize
  CLEAR: ev_success, ev_message, ev_transport_json.

  " Validate input
  IF iv_transport_number IS INITIAL.
    ev_success = ''.
    ev_message = 'Transport number is required'.
    RETURN.
  ENDIF.

  " Query E070 table for transport metadata
  SELECT SINGLE *
    FROM e070
    INTO ls_e070
    WHERE trkorr = iv_transport_number.

  IF sy-subrc <> 0.
    " Transport not found
    ev_success = ''.
    ev_message = |Transport { iv_transport_number } not found in E070 table|.
    RETURN.
  ENDIF.

  " Map transport type to description
  CASE ls_e070-trfunction.
    WHEN 'K'.
      lv_type_desc = 'Workbench'.
    WHEN 'S'.
      lv_type_desc = 'Task'.
    WHEN 'T'.
      lv_type_desc = 'Transport of Copies'.
    WHEN 'W'.
      lv_type_desc = 'Workbench Request'.
    WHEN 'C'.
      lv_type_desc = 'Customizing'.
    WHEN OTHERS.
      lv_type_desc = ls_e070-trfunction.
  ENDCASE.

  " Map status to description
  CASE ls_e070-trstatus.
    WHEN 'D'.
      lv_status_desc = 'Modifiable'.
    WHEN 'R'.
      lv_status_desc = 'Released'.
    WHEN 'L'.
      lv_status_desc = 'Protected'.
    WHEN 'N'.
      lv_status_desc = 'Modifiable (Protected)'.
    WHEN 'O'.
      lv_status_desc = 'Released (With Import Protection)'.
    WHEN OTHERS.
      lv_status_desc = ls_e070-trstatus.
  ENDCASE.

  " Format date (YYYYMMDD → YYYY-MM-DD)
  IF ls_e070-as4date IS NOT INITIAL.
    lv_created_date = |{ ls_e070-as4date+0(4) }-{ ls_e070-as4date+4(2) }-{ ls_e070-as4date+6(2) }|.
  ENDIF.

  " Format time (HHMMSS → HH:MM:SS)
  IF ls_e070-as4time IS NOT INITIAL.
    lv_created_time = |{ ls_e070-as4time+0(2) }:{ ls_e070-as4time+2(2) }:{ ls_e070-as4time+4(2) }|.
  ENDIF.

  " Handle parent transport (for tasks)
  IF ls_e070-strkorr IS NOT INITIAL.
    lv_parent = ls_e070-strkorr.
  ELSE.
    lv_parent = 'null'.
  ENDIF.

  " Check if transport has objects (query E071)
  SELECT COUNT(*)
    FROM e071
    INTO lv_object_count
    WHERE trkorr = iv_transport_number.

  IF lv_object_count > 0.
    lv_has_objects = abap_true.
  ELSE.
    lv_has_objects = abap_false.
  ENDIF.

  " Check if transport has tasks (only for main transports, not tasks)
  IF ls_e070-trfunction = 'K'.
    SELECT COUNT(*)
      FROM e070
      INTO lv_task_count
      WHERE strkorr = iv_transport_number
        AND trfunction = 'S'.

    IF lv_task_count > 0.
      lv_has_tasks = abap_true.
    ELSE.
      lv_has_tasks = abap_false.
    ENDIF.
  ELSE.
    lv_has_tasks = abap_false.
  ENDIF.

  " Build JSON response
  lv_json = |{ \{| &&
            |"success": true,| &&
            |"transport_number": "{ ls_e070-trkorr }",| &&
            |"transport_type": "{ ls_e070-trfunction }",| &&
            |"transport_type_desc": "{ lv_type_desc }",| &&
            |"status": "{ ls_e070-trstatus }",| &&
            |"status_desc": "{ lv_status_desc }",| &&
            |"owner": "{ ls_e070-as4user }",| &&
            |"description": "{ ls_e070-as4text }",| &&
            |"created_date": "{ lv_created_date }",| &&
            |"created_time": "{ lv_created_time }",| &&
            |"target_system": "{ ls_e070-tarsystem }",| &&
            |"category": "{ ls_e070-korrdev }",| &&
            |"parent_transport": { lv_parent },| &&
            |"has_objects": { COND #( WHEN lv_has_objects = abap_true THEN 'true' ELSE 'false' ) },| &&
            |"has_tasks": { COND #( WHEN lv_has_tasks = abap_true THEN 'true' ELSE 'false' ) }| &&
            |\}|.

  " Return success
  ev_success = 'X'.
  ev_transport_json = lv_json.

ENDFUNCTION.

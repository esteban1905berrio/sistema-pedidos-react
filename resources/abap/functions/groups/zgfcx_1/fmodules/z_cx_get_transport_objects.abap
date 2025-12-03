FUNCTION Z_CX_GET_TRANSPORT_OBJECTS
  IMPORTING
    VALUE(IV_TRANSPORT_NUMBER) TYPE TRKORR
    VALUE(IV_TASK_NUMBER) TYPE TRKORR OPTIONAL
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_TRANSPORT_JSON) TYPE STRING
  EXCEPTIONS
    TRANSPORT_NOT_FOUND
    QUERY_ERROR.



  DATA: ls_e070           TYPE e070,
        lt_e070_tasks     TYPE STANDARD TABLE OF e070,
        lt_e071           TYPE STANDARD TABLE OF e071,
        ls_e071           TYPE e071,
        lv_json           TYPE string,
        lv_metadata       TYPE string,
        lv_objects        TYPE string,
        lv_tasks          TYPE string,
        lv_object_line    TYPE string,
        lv_task_line      TYPE string,
        lv_total_objects  TYPE i,
        lv_object_count   TYPE i,
        lv_date_formatted TYPE string,
        lv_time_formatted TYPE string,
        lv_type_desc      TYPE string,
        lv_status_desc    TYPE string.

  " Step 1: Validate input
  IF iv_transport_number IS INITIAL.
    MESSAGE 'Transport number is required' TYPE 'E'.
    RAISE query_error.
  ENDIF.

  " Step 2: Get transport metadata from E070
  SELECT SINGLE *
    FROM e070
    INTO ls_e070
    WHERE trkorr = iv_transport_number.

  IF sy-subrc <> 0.
    ev_success = ''.
    ev_message = |Transport { iv_transport_number } not found in E070 table|.
    RAISE transport_not_found.
  ENDIF.

  " Step 3: Format date and time
  IF ls_e070-as4date IS NOT INITIAL.
    lv_date_formatted = |{ ls_e070-as4date+0(4) }-{ ls_e070-as4date+4(2) }-{ ls_e070-as4date+6(2) }|.
  ELSE.
    lv_date_formatted = ''.
  ENDIF.

  IF ls_e070-as4time IS NOT INITIAL.
    lv_time_formatted = |{ ls_e070-as4time+0(2) }:{ ls_e070-as4time+2(2) }:{ ls_e070-as4time+4(2) }|.
  ELSE.
    lv_time_formatted = ''.
  ENDIF.

  " Step 4: Map transport type
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
      lv_type_desc = 'Unknown'.
  ENDCASE.

  " Step 5: Map transport status
  CASE ls_e070-trstatus.
    WHEN 'D'.
      lv_status_desc = 'Modifiable'.
    WHEN 'L'.
      lv_status_desc = 'Protected'.
    WHEN 'R'.
      lv_status_desc = 'Released'.
    WHEN 'N'.
      lv_status_desc = 'Modifiable (Protected)'.
    WHEN 'O'.
      lv_status_desc = 'Released (With Import Protection)'.
    WHEN OTHERS.
      lv_status_desc = 'Unknown'.
  ENDCASE.

  " Step 6: Build metadata JSON
  lv_metadata = |"metadata":\{|
             && |"transport_number":"{ ls_e070-trkorr }",|
             && |"transport_type":"{ ls_e070-trfunction }",|
             && |"transport_type_desc":"{ lv_type_desc }",|
             && |"status":"{ ls_e070-trstatus }",|
             && |"status_desc":"{ lv_status_desc }",|
             && |"owner":"{ ls_e070-as4user }",|
             && |"created_date":"{ lv_date_formatted }",|
             && |"created_time":"{ lv_time_formatted }",|
             && |"target_system":"{ ls_e070-tarsystem }",|
             && |"category":"{ ls_e070-korrdev }",|
             && |"description":"",|
             && |"parent_transport":"{ ls_e070-strkorr }"|
             && |\}|.

  " Step 7: Get objects from E071 for main transport
  SELECT *
    FROM e071
    INTO TABLE lt_e071
    WHERE trkorr = iv_transport_number.

  " Step 8: If main transport (K), get all tasks and their objects
  IF ls_e070-trfunction = 'K'.
    SELECT *
      FROM e070
      INTO TABLE lt_e070_tasks
      WHERE strkorr = iv_transport_number
        AND trfunction = 'S'.

    " Get objects for each task
    LOOP AT lt_e070_tasks INTO DATA(ls_task).
      SELECT *
        FROM e071
        APPENDING TABLE lt_e071
        WHERE trkorr = ls_task-trkorr.
    ENDLOOP.
  ENDIF.

  " Step 9: Filter by task number if specified
  IF iv_task_number IS NOT INITIAL.
    DELETE lt_e071 WHERE trkorr <> iv_task_number.
  ENDIF.

  " Step 10: Build objects JSON array
  lv_objects = '"objects":['.
  LOOP AT lt_e071 INTO ls_e071.
    IF sy-tabix > 1.
      lv_objects = lv_objects && ','.
    ENDIF.

    lv_object_line = |\{|
                  && |"trkorr":"{ ls_e071-trkorr }",|
                  && |"pgmid":"{ ls_e071-pgmid }",|
                  && |"object_type":"{ ls_e071-object }",|
                  && |"object_name":"{ ls_e071-obj_name }",|
                  && |"lock_flag":"{ ls_e071-lockflag }",|
                  && |"gennum":"{ ls_e071-gennum }",|
                  && |"tab_key":""|
                  && |\}|.

    lv_objects = lv_objects && lv_object_line.
  ENDLOOP.
  lv_objects = lv_objects && ']'.

  lv_total_objects = lines( lt_e071 ).

  " Step 11: Build tasks JSON array (only for main transports)
  lv_tasks = '"tasks":['.
  IF ls_e070-trfunction = 'K'.
    LOOP AT lt_e070_tasks INTO ls_task.
      IF sy-tabix > 1.
        lv_tasks = lv_tasks && ','.
      ENDIF.

      " Format task date/time
      IF ls_task-as4date IS NOT INITIAL.
        lv_date_formatted = |{ ls_task-as4date+0(4) }-{ ls_task-as4date+4(2) }-{ ls_task-as4date+6(2) }|.
      ELSE.
        lv_date_formatted = ''.
      ENDIF.

      IF ls_task-as4time IS NOT INITIAL.
        lv_time_formatted = |{ ls_task-as4time+0(2) }:{ ls_task-as4time+2(2) }:{ ls_task-as4time+4(2) }|.
      ELSE.
        lv_time_formatted = ''.
      ENDIF.

      " Map task status
      CASE ls_task-trstatus.
        WHEN 'D'.
          lv_status_desc = 'Modifiable'.
        WHEN 'L'.
          lv_status_desc = 'Protected'.
        WHEN 'R'.
          lv_status_desc = 'Released'.
        WHEN OTHERS.
          lv_status_desc = 'Unknown'.
      ENDCASE.

      " Count objects for this task
      CLEAR lv_object_count.
      LOOP AT lt_e071 INTO ls_e071 WHERE trkorr = ls_task-trkorr.
        lv_object_count = lv_object_count + 1.
      ENDLOOP.

      lv_task_line = |\{|
                  && |"task_number":"{ ls_task-trkorr }",|
                  && |"owner":"{ ls_task-as4user }",|
                  && |"created_date":"{ lv_date_formatted }",|
                  && |"created_time":"{ lv_time_formatted }",|
                  && |"status":"{ ls_task-trstatus }",|
                  && |"status_desc":"{ lv_status_desc }",|
                  && |"description":"",|
                  && |"object_count":{ lv_object_count }|
                  && |\}|.

      lv_tasks = lv_tasks && lv_task_line.
    ENDLOOP.
  ENDIF.
  lv_tasks = lv_tasks && ']'.

  " Step 12: Build complete JSON
  lv_json = |\{|
         && |"success":true,|
         && |"transport_number":"{ iv_transport_number }",|
         && lv_metadata && ','
         && lv_objects && ','
         && |"total_objects":{ lv_total_objects },|
         && lv_tasks
         && |\}|.

  ev_success = 'X'.
  ev_message = |Transport objects retrieved successfully|.
  ev_transport_json = lv_json.

ENDFUNCTION.

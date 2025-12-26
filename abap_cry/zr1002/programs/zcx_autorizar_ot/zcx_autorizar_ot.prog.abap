*&---------------------------------------------------------------------*
*& Report zcx_autorizar_ot
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT zcx_autorizar_ot.
SELECTION-SCREEN BEGIN OF BLOCK b1 WITH FRAME TITLE TEXT-001.
  SELECTION-SCREEN SKIP.
  SELECTION-SCREEN: BEGIN OF LINE.
  SELECTION-SCREEN: COMMENT 1(11) TEXT-002.
  PARAMETERS: p_orden TYPE e070-trkorr OBLIGATORY.
  SELECTION-SCREEN: END OF LINE.
  SELECTION-SCREEN SKIP.
SELECTION-SCREEN END OF BLOCK b1.

START-OF-SELECTION.

  CALL FUNCTION 'ZTMS_QAI_WORKLIST_DISPLAY'
    EXPORTING
*     iv_system               = SY-SYSID
*     iv_client               = 'ALL'
*     iv_step                 = 'SAP03'
*     iv_project              =
*     iv_collect_data         = ' '
*     iv_verbose              =
*     iv_monitor              = 'X'
      iv_request              = p_orden
    EXCEPTIONS
      worklist_display_failed = 1
      no_qa_system            = 2
      OTHERS                  = 3.

  IF sy-subrc <> 0.
* MESSAGE ID SY-MSGID TYPE SY-MSGTY NUMBER SY-MSGNO
*   WITH SY-MSGV1 SY-MSGV2 SY-MSGV3 SY-MSGV4.
  ENDIF.
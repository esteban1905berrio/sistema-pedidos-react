*&---------------------------------------------------------------------*
*& Include          ZXTRKU02
*&---------------------------------------------------------------------*

"SLS 05.02.2024 I1201 : Ampliación IDOC Tipo Base: DELVRY03
zclsd_exits_delivery=>exit_saplv56k_002(
  EXPORTING
    i_control_record_out = control_record_out
    i_message_type       = message_type
    i_segment_name       = segment_name
    i_data               = data
    i_tab_idoc_reduction = tab_idoc_reduction
  CHANGING
    c_ti_idoc_data       = idoc_data[] ).
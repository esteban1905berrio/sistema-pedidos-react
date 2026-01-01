*&---------------------------------------------------------------------*
*& Include zx050u06
*&---------------------------------------------------------------------*
 DATA: ti_verificar_campo_lleno TYPE zclfii1008_integracion_afs=>gtp_ti_campo,
       ti_log_homologacion      TYPE zclmmi1009_homologacion_campos=>gtp_ti_log_homologacion.

 zclfi_exits_gestion_documentos=>validar_fecha_contab_doc_fi(
   CHANGING
     c_ti_datos_idoc = idoc_data[] ).

 zclfi_exits_gestion_documentos=>homologar_idoc_entrada_fidcc2(
   EXPORTING
      i_es_control_idoc = idoc_contrl
   CHANGING
     c_ti_verificar_campo_lleno = ti_verificar_campo_lleno
     c_ti_log_homologacion      = ti_log_homologacion
     c_ti_datos_idoc = idoc_data[] ).

 zclfi_exits_gestion_documentos=>validar_bp_homologado_en_afs(
   EXPORTING
      i_es_control_idoc = idoc_contrl
   CHANGING
     c_ti_verificar_campo_lleno = ti_verificar_campo_lleno
     c_ti_log_homologacion      = ti_log_homologacion
     c_ti_datos_idoc = idoc_data[] ).

 zclfi_exits_gestion_documentos=>asignar_documento_anulacion(
  CHANGING
    c_ti_datos_idoc = idoc_data[] ).

 zclfi_exits_gestion_documentos=>validar_cl_impuesto_proveedor(
  CHANGING
    c_ti_datos_idoc = idoc_data[] ).
*&---------------------------------------------------------------------*
*& Include          ZX050U08
*&---------------------------------------------------------------------*
 zclfi_exits_gestion_costos=>habilitar_doc_co_idoc_ficc2(
   CHANGING
     c_comp       = i_comp
     c_comp_check = i_comp_check ).

 zclfi_exits_gestion_costos=>completa_documento_anular_caja( CHANGING c_ti_accit = t_accit[] ).
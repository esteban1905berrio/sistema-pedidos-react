*&---------------------------------------------------------------------*
*& Report ZHELLO4
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT zhello4.

INCLUDE:
z_hello_p.  ".Declaracion de parametros
*z_hello4_c. ".Declaracion de Clase
*z_hello4_f. ".Declaracion de subrutinas

INITIALIZATION.
  ".Instancia de la clase local lcl_principal
*  go_principal = NEW lcl_principal( ).
  go_cl_hello4 = NEW ycl_hello4( ).


START-OF-SELECTION.
  go_cl_hello4->mt_start_of_selection(
    EXPORTING
      iss_rn_vbeln = so_vbeln[]                 " Selection range Delivery Number (Inbound or Outbound)
      iss_rn_vkorg = so_vkorg[]                 " Range Table for sales organization
      iss_rn_kunnr = so_kunnr[]                 " Ranges Table for FARR_RS_KUNNR
      iss_rn_matnr = so_matnr[]                 " Range Table for MATNR
      iss_rn_werks = so_werks[]                 " Range Table for Data Element WERKS
      iss_rn_lgort = so_lgort[]                 " Tabla range para elemento de datos LGORT_D
      iss_rn_charg = so_charg[]                 " Range Table for Batch

  ).

*   go_principal->mt_start_of_selection( ).
*
*END-OF-SELECTION.
*  go_principal->mt_end_of_selection( ).
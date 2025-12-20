*CLASS zclcxr1003_cust_imagen DEFINITION
*  PUBLIC
*  FINAL
*  CREATE PUBLIC .
*
*  PUBLIC SECTION.
*  PROTECTED SECTION.
*  PRIVATE SECTION.
*ENDCLASS.
*
*
*
*CLASS zclcxr1003_cust_imagen IMPLEMENTATION.
*ENDCLASS.


CLASS zclcxr1003_cust_imagen DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC.

  PUBLIC SECTION.
    INTERFACES if_sadl_exit_calc_element_read.
  PROTECTED SECTION.
  PRIVATE SECTION.
ENDCLASS.

CLASS zclcxr1003_cust_imagen IMPLEMENTATION.
  METHOD if_sadl_exit_calc_element_read~calculate.

    DATA l_ti_etiquetas TYPE STANDARD TABLE OF zc_rap_ztcxr1003_1 WITH DEFAULT KEY.
    l_ti_etiquetas = CORRESPONDING #( it_original_data ).

    LOOP AT l_ti_etiquetas ASSIGNING FIELD-SYMBOL(<l_fs_tiqueta>).
*      <customer>-CustImageURL = 'https://github.githubassets.com/images/modules/open_graph/github-octocat.png'.
*      <l_fs_tiqueta>-CustImageURL = 'https://sabshare03/Imagenes_WikiHelpy/Componentes.jpg'.
*      <l_fs_tiqueta>-CustImageURL = 'https://github.githubassets.com/images/spinners/octocat-spinner-128.gif'.
      <l_fs_tiqueta>-CustImageURL = 'https://vhs4dapci.crystal.com.co:44300/sap/bc/fp/YETIQUETAS/etiqueta1.png'.
    ENDLOOP.

    ct_calculated_data = CORRESPONDING #(  l_ti_etiquetas ).
  ENDMETHOD.

  METHOD if_sadl_exit_calc_element_read~get_calculation_info.
  ENDMETHOD.

ENDCLASS.
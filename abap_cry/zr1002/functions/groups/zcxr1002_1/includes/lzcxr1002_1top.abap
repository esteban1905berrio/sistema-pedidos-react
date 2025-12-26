FUNCTION-POOL zcxr1002_1.                       "MESSAGE-ID ..

* INCLUDE LZCX001D...                        " Local class definition

TYPE-POOLS: ole2.

"value of excel-cell
TYPES: gtp_d_itabvalue  TYPE zecxr1002_1-value,
       "internal table containing the excel data
       gtp_ti_itab      TYPE zttcxr1002_2,
       gtp_ti_itab_mult TYPE zttcxr1002_3,

       "line type of sender table
       BEGIN OF ty_s_senderline,
         line(15000)  TYPE c,
         "Necesaria para grandes volumenes de datos, en caso de presentarse salto de lineas,
         "aumentar la longitud del componente LINE
         extdat(1000) TYPE c,
       END OF ty_s_senderline,

       BEGIN OF ty_s_senderline_m,
         line(15000) TYPE c,
       END OF ty_s_senderline_m,
       "sender table
       gtp_ti_sender   TYPE STANDARD TABLE OF ty_s_senderline WITH EMPTY KEY,  "TYPE ty_s_senderline  OCCURS 0.
       gtp_ti_sender_m TYPE STANDARD TABLE OF ty_s_senderline_m WITH EMPTY KEY.  "TYPE ty_s_senderline  OCCURS 0.


CONSTANTS:  gc_esc              VALUE '"'.

FIELD-SYMBOLS: <gfs_ti_alv_log> TYPE ANY TABLE.

CLASS lcl_controlador DEFINITION DEFERRED.

DATA: go_alvgrid        TYPE REF TO cl_gui_alv_grid,
      go_container      TYPE REF TO cl_gui_docking_container, "Ajustar tamaño pantalla completa - FACEVEDO 15.01.2019
      go_ctr_alvgrid    TYPE REF TO zifcxr1002_alvgrid,
      go_dock_container TYPE REF TO cl_gui_docking_container,
      g_r_ti_datos_alv  TYPE REF TO data,
      gti_catalogo      TYPE lvc_t_fcat,
      g_status_gui      TYPE string,
      g_titulo          TYPE string,
      g_repid           TYPE syrepid,
      ges_layout_vari   TYPE disvariant,
      ges_layout        TYPE lvc_s_layo,
      g_username        TYPE syuname,
      g_cons_catalogo   TYPE flag,
      g_statusgui_prog  TYPE string,
      g_ok_code         TYPE syucomm,
      g_okcode_aux      TYPE syucomm,
      gti_sort          TYPE lvc_t_sort,
      go_controlador    TYPE REF TO lcl_controlador,
  "{ Inicio SGR 21.oct.2024
      go_html           TYPE REF TO cl_dd_document,
      go_split          TYPE REF TO cl_gui_easy_splitter_container,
      go_contnr_top      TYPE REF TO cl_gui_container,
      go_contnr_bot      TYPE REF TO cl_gui_container.
  "{ Fin SGR 21.oct.2024
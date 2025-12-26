*&---------------------------------------------------------------------*
*& Include zmmr1136p_1
*&---------------------------------------------------------------------*


*----------------------------------------------------------------------*
* Definición de Variables
*----------------------------------------------------------------------*

  DATA:
    ok_code                  TYPE syucomm,
    ok_code_temporal         TYPE syucomm,
    " El índice de la fila de step-loop
    g_indice_linea_actual    TYPE i,
    " Corriente de línea que se mostrará
    g_i_line                 TYPE i,
    " Las filas totales de step-loop que se mostrarán en una página
    g_filas_totales          TYPE i,
    " Los límites de final de filas de ste-loop que se puede mostrar
    g_limite_de_filas        TYPE i,
    g_posicion_cursor        TYPE i,
    " El límite inferior del índice del expediente que se mostrará
    g_limite_inferior_ndice  TYPE i,
    " El límite superior del índice de registro que se mostrará
    g_limite_superior_indice TYPE i,
    " ariable para manejar la próxima navegación de la página
    g_i_y_v_next             TYPE i,
    " Variable para manejar la navegación página anterior
    g_i_y_v_prev             TYPE i,
    " Controlador
    g_contador_de_lineas     TYPE i VALUE 1,
    " Check Box NumParte.
    gb_numparte              TYPE c,
    g_nombre_campo_cursor    TYPE char100,
    " Limite
    g_i_y_v_limit            TYPE i,
    g_posicion_cursor_pant_consumo    TYPE i.
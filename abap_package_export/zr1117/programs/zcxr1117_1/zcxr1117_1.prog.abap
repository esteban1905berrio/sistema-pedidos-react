*&---------------------------------------------------------------------*
*& Report ZCXR1117_1
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT zcxr1117_1.

INCLUDE zcxr1117c_1                             .    " Clase local
*INCLUDE ZCXR1117v_1                             .    " Global Data (Puede ser omitido por la clase)
INCLUDE zcxr1117p_1                             .    " Pantalla de selección
INCLUDE zcxr1117o_1                             .    " PBO-Modules
INCLUDE zcxr1117i_1                             .    " PAI-Modules

START-OF-SELECTION.
  cl_control=>inicio_de_seleccion( i_r_trkorr = so_orden[] ).
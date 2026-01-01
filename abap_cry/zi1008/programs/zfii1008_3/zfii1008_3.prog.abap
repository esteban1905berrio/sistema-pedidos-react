*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: I1008
* Programa     : ZFII1008_3
* Tipo Objeto  : Reporte
* Descripción  : Modificar limite de credito
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 17.09.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.03.2022   S4DK900790    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*
REPORT zfii1008_3.

INCLUDE zfii1008p_3.

START-OF-SELECTION.


  zclfii1008_integracion_afs=>modificar_limite_de_credito( i_r_partner = so_parnr[] ).
*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: I1008
* Programa     : ZFII1008_1
* Tipo Objeto  : Reporte
* Descripción  : Enviar idoc DEBMAS/CREMAS con punteros de modificacion
*                Tx DB12 y DB14
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 17.09.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.03.2021   S4DK900790    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*
REPORT zfii1008_1.

START-OF-SELECTION.

  zclfii1008_integracion_afs=>envia_idoc_cliente_modificado( ).
  zclfii1008_integracion_afs=>envia_idoc_acreedor_modificado( ).

END-OF-SELECTION.
  MESSAGE s382(3l).
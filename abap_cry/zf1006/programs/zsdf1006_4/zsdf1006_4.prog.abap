*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: F1006
* Programa : ZSDF1006_4
* Tipo Objeto : Programa / Report
* Descripción : PLista de empaque, basados en norma de embalaje
*               clases de mensaje:
*             - ZLEN  Lista Empaque LACOSTE
* Autor Prog. : Sebastian Londono
* Fecha Creac. : 02.8.2023
*----------------------------------------------------------------------*
* Órdenes de Transporte
*----------------------------------------------------------------------*
* Fecha      | CR#        | Autor          | Modificación
*----------------------------------------------------------------------*
* 03.08.2023   S4DK905274   Sebastian Londono       Versión Inicial
*----------------------------------------------------------------------*
REPORT zsdf1006_4.

"Declaración estándar
INCLUDE rle_delnote_data_declare ##INCL_OK.
"Repositorio de subrutinas
INCLUDE zsdf1006_5.
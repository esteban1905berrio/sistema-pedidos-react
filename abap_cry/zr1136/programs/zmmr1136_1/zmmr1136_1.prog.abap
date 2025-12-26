*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: R1136 - Consumo Repuestos COLHILADOS x Radio Frecuencia
* Programa     : zmmr1136_1
* Tipo Objeto  : Reporte
* Descripción  : Consumo Repuestos COLHILADOS
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 27.12.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 27.12.2022    S4DK919016   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmr1136_1.


INCLUDE zmmr1136cd_1.

INITIALIZATION.

  DATA(go_consumo_repuestos) = NEW lcl_controlador(  ).

INCLUDE zmmr1136p_1.
INCLUDE zmmr1136ci_1.
INCLUDE zmmr1136o_1.
INCLUDE zmmr1136i_1.



START-OF-SELECTION.

  go_consumo_repuestos->iniciar_proceso( ).
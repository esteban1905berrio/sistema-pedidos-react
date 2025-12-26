*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: I1082 - Update Replenishment
* Programa     : zmmi1082_2
* Tipo Objeto  : Reporte
* Descripción  : JOB Update Replenishment
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 24.11.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 25.11.2020    S4DK904990   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmi1082_2.

START-OF-SELECTION.
  NEW zclmmi1082_modifica_reposicion(  )->enviar_reposicion_job( ).
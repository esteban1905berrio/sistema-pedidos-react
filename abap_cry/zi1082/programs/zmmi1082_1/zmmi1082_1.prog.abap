*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: I1082 - Update Replenishment
* Programa     : zmmi1082_2
* Tipo Objeto  : Reporte
* Descripción  : Update Replenishment
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 24.11.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 25.11.2020    S4DK904990   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zmmi1082_1.

INCLUDE zmmi1082p_1.
INCLUDE zmmi1082o_1.
INCLUDE zmmi1082i_1.

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_rtpl-low.
  zclmmi1082_modifica_reposicion=>f4_tipo_localizacion( CHANGING c_tipo_localizacion = so_rtpl-low ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_rtpl-high.
  zclmmi1082_modifica_reposicion=>f4_tipo_localizacion( CHANGING c_tipo_localizacion = so_rtpl-high ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_tpl-low.
  zclmmi1082_modifica_reposicion=>f4_tipo_localizacion( CHANGING c_tipo_localizacion = so_tpl-low ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_tpl-high.
  zclmmi1082_modifica_reposicion=>f4_tipo_localizacion( CHANGING c_tipo_localizacion = so_tpl-high ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_locz-low.
  zclmmi1082_modifica_reposicion=>f4_localizacion( CHANGING c_localizacion = so_locz-low ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR so_locz-high.
  zclmmi1082_modifica_reposicion=>f4_localizacion( CHANGING c_localizacion = so_locz-high ).

AT SELECTION-SCREEN ON VALUE-REQUEST FOR pa_narch.

  CALL FUNCTION 'F4_FILENAME'
    IMPORTING
      file_name = pa_narch.
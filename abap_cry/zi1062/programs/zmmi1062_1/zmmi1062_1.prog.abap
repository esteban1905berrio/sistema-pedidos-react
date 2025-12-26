*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: I1062 - Instrucciones de cuidado
* Programa     : zmmi1062_1
* Tipo Objeto  : Reporte
* Descripción  : Reporte Instrucciones de cuidado
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 04.02.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 25.09.2020    S4DK902974    Sebastian Londoño         Creación SA60968
*----------------------------------------------------------------------*
REPORT zmmi1062_1.

INCLUDE zmmi1062p_1.
INCLUDE zmmi1062cd_1.
INCLUDE zmmi1062ci_1.
INCLUDE zmmi1062o_1.
INCLUDE zmmi1062i_1.

INITIALIZATION.

  CONCATENATE '@96@' text-001 INTO icono_log.

  sscrfields-functxt_01 = icono_log.

AT SELECTION-SCREEN.

  CASE sscrfields-ucomm.
    WHEN 'FC01'.
      SET PARAMETER ID 'BALOBJ' FIELD zclmmi1062_instruccion_lavado=>gc_nombre_objeto_log.
      SET PARAMETER ID 'BALSUBOBJ' FIELD zclmmi1062_instruccion_lavado=>gc_nombre_subobjeto_log.

      CALL TRANSACTION 'SLG1'.
  ENDCASE.

START-OF-SELECTION.

  lcl_controlador=>iniciar_proceso( i_r_codigo_lavado     = so_cdlav[]
                                    i_denominacion_lavado = pa_dnlv ).

END-OF-SELECTION.

  IF lcl_controlador=>gti_ins_lavado IS NOT INITIAL.
    CALL SCREEN 100.
  ELSE.
    MESSAGE i002(wusl).
  ENDIF.
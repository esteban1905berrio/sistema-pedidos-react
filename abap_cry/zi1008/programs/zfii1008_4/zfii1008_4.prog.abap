*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: I1008
* Programa     : ZFII1008_4
* Tipo Objeto  : Reporte
* Descripción  : Depuracion de IDOC FI
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 17.10.2022
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.10.2022   S4DK900790    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*
REPORT zfii1008_4.

INCLUDE zfii1008p_4.

INITIALIZATION.

  IF so_mesty[] IS INITIAL.
    APPEND LINES OF VALUE edm_mestyp_range_tt( sign = 'I' option = 'EQ'
                                               ( low = 'CODCMT'  )
                                               ( low = 'FIDCC2'  )
                                               ( low = 'PRCDOC'  )
                                             )
    TO so_mesty[].
  ENDIF.

START-OF-SELECTION.

  IF so_fecha[] IS INITIAL.

    MESSAGE s345(kt) DISPLAY LIKE 'E'.

  ELSE.
    zclfii1008_integracion_afs=>eliminar_idoc_rango_fecha( i_r_fecha  = so_fecha[]
                                                           i_r_mestyp = so_mesty[]
                                                           i_cantidad_maxima_registros = pa_maxct ).
  ENDIF.
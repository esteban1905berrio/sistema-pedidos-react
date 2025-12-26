*&---------------------------------------------------------------------*
*& Información General
*&---------------------------------------------------------------------*
* Identificador: I1008
* Programa     : ZFII1008_2
* Tipo Objeto  : Reporte
* Descripción  : Validar Homologacion IDOC
* Autor Prog.  : Sebastian Londono
* Fecha Creac. : 17.09.2021
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor           | Modificación
*----------------------------------------------------------------------*
* 17.03.2021   S4DK900790    Sebastian Londono Version Inicial
*----------------------------------------------------------------------*
REPORT zfii1008_2.

INCLUDE zfii1008p_2.

START-OF-SELECTION.

  IF so_docnm[] IS INITIAL AND so_fecha[] IS INITIAL AND so_hora[] IS INITIAL AND so_mestp IS INITIAL
    AND so_segnm[] IS INITIAL.

    MESSAGE s345(kt) DISPLAY LIKE 'E'.

  ELSE.
    zclfii1008_integracion_afs=>verificar_homologacion_idoc( i_r_docnum = so_docnm[] i_r_mestyp = so_mestp[]
                                                             i_r_segnam = so_segnm[] i_r_fecha  = so_fecha[]
                                                             i_r_hora   = so_hora[] ).
  ENDIF.
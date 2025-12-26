*----------------------------------------------------------------------*
* Información General
*----------------------------------------------------------------------*
* Identificador: E1088 - Lista de empaque Colhilados
* Programa     : zsde1088_1
* Tipo Objeto  : Reporte
* Descripción  : Lista de empaque Colhilados
* Autor Prog.  : Sebastian Londoño
* Fecha Creac. : 10.03.2023
*----------------------------------------------------------------------*
* Ordenes de Transporte
*----------------------------------------------------------------------*
* Fecha       | CR#         | Autor                   | Modificación
*----------------------------------------------------------------------*
* 10.03.2023    S4DK920581   Sebastian Londoño         Creación
*----------------------------------------------------------------------*
REPORT zsde1088_1.

FORM entry USING return_code us_screen.

  DATA: lf_retcode TYPE sy-subrc.

  ASSIGN ('NAST') TO FIELD-SYMBOL(<fs_nast>).

  DATA(o_lista_empaque) = NEW zclsde1088_envio_lista_empaque( ).

  return_code = o_lista_empaque->envio_lista_empaque( i_es_nast =  <fs_nast> ).

  "Actualizar NAST
  o_lista_empaque->actualizar_nast( ).

ENDFORM.                    "ENTRY
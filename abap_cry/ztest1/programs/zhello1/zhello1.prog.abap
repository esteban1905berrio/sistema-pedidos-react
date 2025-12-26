*&---------------------------------------------------------------------*
*& Report ZHELLO1
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT zhello1.

INCLUDE:
  zhello1_p,
  zhello1_f.

START-OF-SELECTION.
  ".Rutina de incio del proceso
  PERFORM star_of_selection.

end-of-SELECTION.
  PERFORM visualizar_informacion.

  ".tarea: crear un programa  zhello2 dentro del package ZTEST1 y la orden S4DK921693 DV-CX-R001 Reporte TEST1
  ".Crear include de parametros
  ".Crear include de subrutinas
  ".parametros tipo select-option
    ". Compañía aérea Codigo = CARRID, obligatorio
    ". Ciudad sal. = CITYFROM, sin intervalos
    ".Aerop.salida = AIRPFROM, sin intervalos

  ".Consultamos la tabla SFLIGHTS
  ".Si existe datos Visualizar la siguiente informacion:
    ". CARRNAME, CONNID, COUNTRYFR, CITYFROM, AIRPFROM, CITYTO
  ".Sino .. mostrar mensajes de no exito.
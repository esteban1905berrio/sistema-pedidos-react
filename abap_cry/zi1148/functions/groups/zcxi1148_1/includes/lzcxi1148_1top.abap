FUNCTION-POOL zcxi1148_1.                   "MESSAGE-ID ..
TYPES:
  BEGIN OF gtp_es_parametros,
    fecha_inicial TYPE sy-datum,    ".Fecha Inicial
    fecha_final   TYPE sy-datum,    ".Fecha Final
    hora_inicial  TYPE sy-uzeit,    ".Hora Inicial
    hora_final    TYPE sy-uzeit,    ".Hora Final

    user          TYPE sy-uname,    ".Usuario.
    tcode         TYPE sy-tcode,    ".Transacción
    report        TYPE sy-cprog,    ".Programa

    logon         TYPE abap_bool,   ".Entr.sist.interactiva
    rlogon        TYPE abap_bool,   ".Entrada sist.RFC/CPIC
    rfcstart      TYPE abap_bool,   ".Llamada función RFC
    tastart       TYPE abap_bool,   ".Inicio transacción
    repstart      TYPE abap_bool,   ".Inicio del report
    usermgm       TYPE abap_bool,   ".Modif.maestro usuario
    misc          TYPE abap_bool,   ".Otros eventos
    system        TYPE abap_bool,   ".Eventos de sistema
  END OF gtp_es_parametros .

* INCLUDE LZCXI1148_1D...                    " Local class definition
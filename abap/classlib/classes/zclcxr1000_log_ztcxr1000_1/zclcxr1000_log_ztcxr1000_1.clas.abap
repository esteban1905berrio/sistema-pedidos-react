CLASS zclcxr1000_log_ztcxr1000_1 DEFINITION INHERITING FROM zclcxr1000_log_auditoria
  PUBLIC
  CREATE PUBLIC .

  PUBLIC SECTION.
    METHODS:
      registrar_log REDEFINITION.
  PROTECTED SECTION.
  PRIVATE SECTION.
ENDCLASS.



CLASS zclcxr1000_log_ztcxr1000_1 IMPLEMENTATION.
  METHOD registrar_log.

*&----------------------------------------------------------------------*
*& Definicion de Estructuras
*&----------------------------------------------------------------------*
    DATA:
      es_new        TYPE ztcxr1000_1.

*&----------------------------------------------------------------------*
*& Definicion de Tablas internas
*&----------------------------------------------------------------------*
    DATA:
      ti_changes TYPE STANDARD TABLE OF cdtxt.

*&----------------------------------------------------------------------*
*& Definicion de Variables
*&----------------------------------------------------------------------*
    DATA:
      clave  TYPE cdhdr-objectid,
      accion TYPE cdchngindh.

    ".Asignamos el action
    accion = i_accion.

    DATA(es_data) = CORRESPONDING ztcxr1000_1( i_es_data ).

    "Obtenemos la informacion del parametro
    SELECT SINGLE *
      FROM ztcxr1000_1
     WHERE modulo   EQ @es_data-modulo
       AND ricefw   EQ @es_data-ricefw
       AND idcomo   EQ @es_data-idcomo
       AND idparam  EQ @es_data-idparam
      INTO @DATA(es_old).
    IF sy-subrc EQ 0.
      ".Clave del log
      clave = es_old-modulo &&  es_old-ricefw && es_old-idcomo && es_old-idparam.

      CLEAR: es_new.

      es_new-descparam = es_data-descparam.

      ".Si la accion es de Delete
      IF accion EQ gc_d.
        CLEAR es_new.
      ENDIF.
    ENDIF.

    ".Registro de la Modificacion
    CALL FUNCTION 'ZCX_ZTCXR1000_1_WRITE_DOCUMENT'
      EXPORTING
        objectid        = clave
        tcode           = sy-tcode
        utime           = sy-uzeit
        udate           = sy-datum
        username        = sy-uname
        n_ztcxr1000_1   = es_new
        o_ztcxr1000_1   = es_old
        upd_ztcxr1000_1 = accion.
  ENDMETHOD.

ENDCLASS.
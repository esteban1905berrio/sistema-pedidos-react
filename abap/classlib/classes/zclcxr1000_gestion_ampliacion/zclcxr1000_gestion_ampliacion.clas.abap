CLASS zclcxr1000_gestion_ampliacion DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    CLASS-DATA: gti_control_ampliciones TYPE SORTED TABLE OF ztcxr1000_3  WITH UNIQUE KEY primary_key COMPONENTS ricefw metclase.

    CLASS-METHODS:

      "! <strong>Descripción:</strong>
      "! <p>
      "! Valida en la tabla ZTCXR1000_3 el estado (activa/inactiva o modo debug) de una ampliacion de cliente
      "! </p>
      "! <strong>Identificador:</strong> R1000
      "! <br/>
      "! <strong>Autor:</strong>
      "! Sebastian Londono
      "! <br/>
      "! <strong>Historial de cambios:
      "! <br/><br/>
      "! \___Fecha___\____CR#____\________Autor________\_____Modificación_____\
      "! </strong><br/>
      "! \ 20.01.2021  \  S4DK900419 \ Sebastian Londono \ Creacion Inicial \
      "!
      "! @parameter i_ricefw | Numero de Ricefw
      "! @parameter i_metodo | Nombre del metodo
      "! @parameter i_codigo_transaccion | Validar Codigo de transacción que aplica en ampliacion
      "! @parameter r_ampliacion_activa | Indica si la ampliacion esta activa
      ampliacion_activa
        IMPORTING
          i_ricefw                    TYPE zzedricefw
          VALUE(i_metodo)             TYPE seocpdname OPTIONAL
          VALUE(i_codigo_transaccion) TYPE sytcode DEFAULT sy-tcode
        RETURNING
          VALUE(r_ampliacion_activa)  TYPE flag.

  PROTECTED SECTION.
  PRIVATE SECTION.


ENDCLASS.



CLASS ZCLCXR1000_GESTION_AMPLIACION IMPLEMENTATION.


  METHOD ampliacion_activa.

    DATA: ti_pila_llamados             TYPE sys_callst,
          r_codigo_transaccion_aplican TYPE RANGE OF sytcode,
          es_control_ampliciones       LIKE LINE OF gti_control_ampliciones.

    "si no se conoce el nombre del metodo se busca en la pila de llamados
    IF i_metodo IS NOT SUPPLIED.
      CALL FUNCTION 'SYSTEM_CALLSTACK'
        EXPORTING
          max_level    = 2
        IMPORTING
          et_callstack = ti_pila_llamados.

      LOOP AT ti_pila_llamados INTO DATA(es_pila_llamados) WHERE progname NE 'ZCLCXR1000_GESTION_AMPLIACION=CP'.
        EXIT.
      ENDLOOP.

      i_metodo = es_pila_llamados-eventname.

    ENDIF.

    es_control_ampliciones = VALUE #( gti_control_ampliciones[ KEY primary_key ricefw = i_ricefw metclase = i_metodo ] OPTIONAL ).

    IF es_control_ampliciones IS INITIAL.

      IF i_ricefw IS NOT INITIAL AND
         i_metodo IS NOT INITIAL.
        SELECT *
        FROM ztcxr1000_3
        WHERE
          ricefw    = @i_ricefw AND
          metclase  = @i_metodo
        INTO TABLE @gti_control_ampliciones.
      ELSE.
        SELECT *
        FROM ztcxr1000_3
        INTO TABLE @gti_control_ampliciones.
      ENDIF.

      es_control_ampliciones = VALUE #( gti_control_ampliciones[ KEY primary_key ricefw = i_ricefw metclase = i_metodo ] OPTIONAL ).

    ENDIF.

    CHECK es_control_ampliciones IS NOT INITIAL.

    IF  es_control_ampliciones-usrdebug = sy-uname.
      BREAK-POINT.
    ENDIF.

    "Valida si el codigo de transaccion aplica
    IF es_control_ampliciones-tcodes IS NOT INITIAL.
      TRY.
          zclcxr1002_util=>crear_rango_division(
            EXPORTING
              i_cadena           = es_control_ampliciones-tcodes
              i_remover_espacios = abap_true
            CHANGING
              c_ti_rango         = r_codigo_transaccion_aplican ).

          DELETE r_codigo_transaccion_aplican WHERE low = space.

          IF r_codigo_transaccion_aplican IS NOT INITIAL.

            IF i_codigo_transaccion IS INITIAL.
              i_codigo_transaccion = sy-tcode.
            ENDIF.

            IF i_codigo_transaccion NOT IN r_codigo_transaccion_aplican.
              es_control_ampliciones-activo = abap_false.
            ENDIF.

          ENDIF.

        CATCH cx_sy_assign_cast_illegal_cast cx_root.

      ENDTRY.
    ENDIF.

    r_ampliacion_activa = es_control_ampliciones-activo.

  ENDMETHOD.
ENDCLASS.
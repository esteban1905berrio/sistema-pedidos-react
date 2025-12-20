CLASS zcxr1001_excepciones_monitor DEFINITION
  PUBLIC
  INHERITING FROM cx_static_check
  CREATE PUBLIC .

  PUBLIC SECTION.

    INTERFACES if_t100_message .

    CONSTANTS:

      BEGIN OF idmsg_no_encontrado,
        msgid TYPE symsgid VALUE 'ZCX01',
        msgno TYPE symsgno VALUE '030',
        attr1 TYPE scx_attrname VALUE 'ARG_1',
        attr2 TYPE scx_attrname VALUE '',
        attr3 TYPE scx_attrname VALUE '',
        attr4 TYPE scx_attrname VALUE '',
      END OF idmsg_no_encontrado.

*      BEGIN OF cte_error_componente,
*        msgid TYPE symsgid VALUE 'ZMSG_SD',
*        msgno TYPE symsgno VALUE '001',
*        attr1 TYPE scx_attrname VALUE 'ARG_1',
*        attr2 TYPE scx_attrname VALUE 'ARG_2',
*        attr3 TYPE scx_attrname VALUE '',
*        attr4 TYPE scx_attrname VALUE '',
*      END OF   cte_error_componente,


    DATA arg_1 TYPE symsgv .
    DATA arg_2 TYPE symsgv .
    DATA arg_3 TYPE symsgv .
    DATA arg_4 TYPE symsgv .

    METHODS constructor
      IMPORTING
        !textid   LIKE if_t100_message=>t100key OPTIONAL
        !previous LIKE previous OPTIONAL
        !arg_1    TYPE symsgv OPTIONAL
        !arg_2    TYPE symsgv OPTIONAL
        !arg_3    TYPE symsgv OPTIONAL
        !arg_4    TYPE symsgv OPTIONAL .
  PROTECTED SECTION.
  PRIVATE SECTION.
ENDCLASS.



CLASS zcxr1001_excepciones_monitor IMPLEMENTATION.


  METHOD constructor ##ADT_SUPPRESS_GENERATION.
    CALL METHOD super->constructor
      EXPORTING
        previous = previous.

    me->arg_1 = arg_1 .
    me->arg_2 = arg_2 .
    me->arg_3 = arg_3 .
    me->arg_4 = arg_4 .

    CLEAR me->textid.

    IF textid IS INITIAL.
      if_t100_message~t100key = if_t100_message=>default_textid.
    ELSE.
      if_t100_message~t100key = textid.
    ENDIF.

  ENDMETHOD.
ENDCLASS.

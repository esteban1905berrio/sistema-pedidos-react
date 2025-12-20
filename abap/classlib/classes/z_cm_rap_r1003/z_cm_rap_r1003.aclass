CLASS z_cm_rap_r1003 DEFINITION
  PUBLIC
  INHERITING FROM cx_static_check
  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    INTERFACES if_t100_dyn_msg .
    INTERFACES if_t100_message .
    INTERFACES if_abap_behv_message.

    CONSTANTS:
       BEGIN OF g_tp_etiqueta_existe,
       msgid TYPE symsgid VALUE 'ZCX01',
       msgNO TYPE symsgno VALUE '024',
       ATTR1 TYPE scx_attrname VALUE 'ETIQUETAID',
*       ATTR1 TYPE scx_attrname VALUE '',
       ATTR2 TYPE scx_attrname VALUE '',
       ATTR3 TYPE scx_attrname VALUE '',
       ATTR4 TYPE scx_attrname VALUE '',
       END OF g_tp_etiqueta_existe,


      BEGIN OF g_tp_etiqueta_vacia,
       msgid TYPE symsgid VALUE 'ZCX01',
       msgNO TYPE symsgno VALUE '025',
       ATTR1 TYPE scx_attrname VALUE '',
*       ATTR1 TYPE scx_attrname VALUE '',
       ATTR2 TYPE scx_attrname VALUE '',
       ATTR3 TYPE scx_attrname VALUE '',
       ATTR4 TYPE scx_attrname VALUE '',
       END OF g_tp_etiqueta_vacia,

       BEGIN OF g_tp_campo_existe,
       msgid TYPE symsgid VALUE 'ZCX01',
       msgNO TYPE symsgno VALUE '026',
       ATTR1 TYPE scx_attrname VALUE 'CAMPOID',
*       ATTR1 TYPE scx_attrname VALUE '',
       ATTR2 TYPE scx_attrname VALUE 'ETIQUETAID',
       ATTR3 TYPE scx_attrname VALUE '',
       ATTR4 TYPE scx_attrname VALUE '',
       END OF g_tp_campo_existe,

        BEGIN OF g_tp_campo_vacia,
       msgid TYPE symsgid VALUE 'ZCX01',
       msgNO TYPE symsgno VALUE '027',
       ATTR1 TYPE scx_attrname VALUE '',
*       ATTR1 TYPE scx_attrname VALUE '',
       ATTR2 TYPE scx_attrname VALUE '',
       ATTR3 TYPE scx_attrname VALUE '',
       ATTR4 TYPE scx_attrname VALUE '',
       END OF g_tp_campo_vacia,


      BEGIN OF g_tp_campo_noexiste,
       msgid TYPE symsgid VALUE 'ZCX01',
       msgNO TYPE symsgno VALUE '027',
       ATTR1 TYPE scx_attrname VALUE 'CAMPOID',
*       ATTR1 TYPE scx_attrname VALUE '',
       ATTR2 TYPE scx_attrname VALUE '',
       ATTR3 TYPE scx_attrname VALUE '',
       ATTR4 TYPE scx_attrname VALUE '',
       END OF g_tp_campo_noexiste.

     data:
      etiquetaid TYPE string READ-ONLY,
      campoid    TYPE string READ-ONLY.

    METHODS constructor
      IMPORTING
        !SEVERITY TYPE if_abap_behv_message=>t_severity DEFAULT if_abap_behv_message=>severity-ERROR
        !textid   LIKE if_t100_message=>t100key OPTIONAL
        previous LIKE previous OPTIONAL
        etiquetaid type ztcxr1003_1-etiqueta_id OPTIONAL
        campoid    TYPE ztcxr1003_3-campo_id optional.
  PROTECTED SECTION.
  PRIVATE SECTION.
ENDCLASS.



CLASS z_cm_rap_r1003 IMPLEMENTATION.


  METHOD constructor ##ADT_SUPPRESS_GENERATION.
    CALL METHOD super->constructor
      EXPORTING
        previous = previous.
    CLEAR me->textid.
    IF textid IS INITIAL.
      if_t100_message~t100key = if_t100_message=>default_textid.
    ELSE.
      if_t100_message~t100key = textid.
    ENDIF.

    me->if_abap_behv_message~m_severity = severity.

    ME->etiquetaid = |{ etiquetaid ALPHA = OUT }|.
    ME->campoid = |{ campoid ALPHA = OUT }|.

  ENDMETHOD.
ENDCLASS.
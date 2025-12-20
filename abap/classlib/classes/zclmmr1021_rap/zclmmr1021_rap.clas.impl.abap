CLASS zclmmr1021_rap DEFINITION
  PUBLIC
  FINAL
  CREATE PUBLIC .


  PUBLIC SECTION.

    INTERFACES if_sadl_exit_calc_element_read.
  PROTECTED SECTION.
  PRIVATE SECTION.
ENDCLASS.



CLASS ZCLMMR1021_RAP IMPLEMENTATION.


  METHOD if_sadl_exit_calc_element_read~calculate.

    TYPES: BEGIN OF lty_key,
            sociedad type bukrs,
            centro TYPE werks_d,
           END OF lty_key,

           BEGIN OF lty_calc,
            Porcentajecumplido TYPE decfloat16,
           END OF lty_calc.


*    DATA: lt_ZPMMR1021_1 TYPE STANDARD TABLE OF ZVMMR1021,
    DATA: ltr_sociedad TYPE RANGE OF bukrs,
          ltr_centro   TYPE RANGE OF werks_d,
          lt_keys TYPE STANDARD TABLE OF lty_key,
          lt_calc TYPE STANDARD TABLE OF lty_calc.

    lt_keys = CORRESPONDING #( it_original_data ).

    ltr_sociedad = VALUE #( for i in lt_keys
                            ( sign = 'I'
                              option = 'EQ'
                              low = i-sociedad )
                          ).

    ltr_centro = VALUE #( for i in lt_keys
                            ( sign = 'I'
                              option = 'EQ'
                              low = i-centro )
                          ).

     SELECT sociedad, centro, pedido, posicion
      FROM zvmmr1021_2
      INTO TABLE @DATA(lt_sum)
      WHERE ( sociedad IN @ltr_sociedad
        AND centro  IN @ltr_centro ).

    SELECT sociedad, centro,
           SUM( cantconfirmada ) AS sum_conf,
           SUM( cantidad )       AS sum_cant
      FROM zvmmr1021_2
      INTO TABLE @DATA(lt_sums)
      WHERE ( sociedad IN @ltr_sociedad
        AND centro  IN @ltr_centro )
      GROUP BY sociedad, centro.



*
    LOOP AT lt_keys ASSIGNING FIELD-SYMBOL(<ls_key>).
      READ TABLE lt_sums INTO DATA(ls_sum)
           WITH KEY sociedad = <ls_key>-sociedad centro = <ls_key>-centro.
      IF sy-subrc = 0 AND ls_sum-sum_cant > 0.
        APPEND VALUE #( Porcentajecumplido = ( ls_sum-sum_conf * 100 ) / ls_sum-sum_cant ) TO lt_calc.
      ELSE.
        APPEND VALUE #( Porcentajecumplido = 0 ) TO lt_calc.
      ENDIF.
    ENDLOOP.

    ct_calculated_data = CORRESPONDING #(  lt_calc ) .
*    .
  ENDMETHOD.


  METHOD if_sadl_exit_calc_element_read~get_calculation_info.


  ENDMETHOD.
ENDCLASS.
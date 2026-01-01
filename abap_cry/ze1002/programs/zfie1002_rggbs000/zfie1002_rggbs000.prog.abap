PROGRAM zfie1002_rggbs000 .
*---------------------------------------------------------------------*
* Corrections/ repair
* wms092357 070703 Note 638886: template routines to be used for
*                  workaround to substitute bseg-bewar from bseg-xref1/2
*---------------------------------------------------------------------*
*                                                                     *
*   Substitutions: EXIT-Formpool for Uxxx-Exits                       *
*                                                                     *
*   This formpool is used by SAP for testing purposes only.           *
*                                                                     *
*   Note: If you define a new user exit, you have to enter your       *
*         user exit in the form routine GET_EXIT_TITLES.              *
*                                                                     *
*---------------------------------------------------------------------*
INCLUDE fgbbgd00.              "Standard data types


*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*
*    PLEASE INCLUDE THE FOLLOWING "TYPE-POOL"  AND "TABLES" COMMANDS  *
*        IF THE ACCOUNTING MODULE IS INSTALLED IN YOUR SYSTEM         *
TYPE-POOLS: gb002. " TO BE INCLUDED IN                       "wms092357
TABLES: bkpf,      " ANY SYSTEM THAT                         "wms092357
        bseg,      " HAS 'FI' INSTALLED                      "wms092357
        cobl,                                               "wms092357
        csks,                                               "wms092357
        anlz,                                               "wms092357
        glu1.                                               "wms092357
*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*


*----------------------------------------------------------------------*
*       FORM GET_EXIT_TITLES                                           *
*----------------------------------------------------------------------*
*       returns name and title of all available standard-exits         *
*       every exit in this formpool has to be added to this form.      *
*       You have to specify a parameter type in order to enable the    *
*       code generation program to determine correctly how to          *
*       generate the user exit call, i.e. how many and what kind of    *
*       parameter(s) are used in the user exit.                        *
*       The following parameter types exist:                           *
*                                                                      *
*       TYPE                Description              Usage             *
*    ------------------------------------------------------------      *
*       C_EXIT_PARAM_NONE   Use no parameter         Subst. and Valid. *
*                           except B_RESULT                            *
*       C_EXIT_PARAM_FIELD  Use one field as param.  Only Substitution *
*       C_EXIT_PARAM_CLASS  Use a type as parameter  Subst. and Valid  *
*                                                                      *
*----------------------------------------------------------------------*
*  -->  EXIT_TAB  table with exit-name and exit-titles                 *
*                 structure: NAME(5), PARAM(1), TITEL(60)
*----------------------------------------------------------------------*
FORM get_exit_titles TABLES etab.

  DATA: BEGIN OF exits OCCURS 50,
          name(5)   TYPE c,
          param     LIKE c_exit_param_none,
          title(60) TYPE c,
        END OF exits.

  exits-name  = 'U100'.
  exits-param = c_exit_param_none.
  exits-title = TEXT-100.             "Cost center from CSKS
  APPEND exits.

  exits-name  = 'U101'.
  exits-param = c_exit_param_field.
  exits-title = TEXT-101.             "Cost center from CSKS
  APPEND exits.

* begin of insertion                                          "wms092357
  exits-name  = 'U200'.
  exits-param = c_exit_param_field.
  exits-title = TEXT-200.             "Cons. transaction type
  APPEND exits.                       "from xref1/2
* end of insertion                                            "wms092357
* Ini EFB 28.09.2021 Migracion ZF012 Documento Soporte
  exits-name  = 'U111'.
  exits-param = c_exit_param_class.
  exits-title = TEXT-111.
  APPEND exits.
* Fin EFB 28.09.2021

  exits-name  = 'U103'.
  exits-param = c_exit_param_class.
  exits-title = TEXT-103.             "Cons. transaction type
  APPEND exits.

************************************************************************
* PLEASE DELETE THE FIRST '*' FORM THE BEGINING OF THE FOLLOWING LINES *
*        IF THE ACCOUNTING MODULE IS INSTALLED IN YOUR SYSTEM:         *
*  EXITS-NAME  = 'U102'.
*  EXITS-PARAM = C_EXIT_PARAM_CLASS.
*  EXITS-TITLE = TEXT-102.             "Sum is used for the reference.
*  APPEND EXITS.


***********************************************************************
** EXIT EXAMPLES FROM PUBLIC SECTOR INDUSTRY SOLUTION
**
** PLEASE DELETE THE FIRST '*' FORM THE BEGINING OF THE FOLLOWING LINE
** TO ENABLE PUBLIC SECTOR EXAMPLE SUBSTITUTION EXITS
***********************************************************************
  INCLUDE rggbs_ps_titles.

  REFRESH etab.
  LOOP AT exits.
    etab = exits.
    APPEND etab.
  ENDLOOP.

ENDFORM.                    "GET_EXIT_TITLES


* eject
*---------------------------------------------------------------------*
*       FORM U100                                                     *
*---------------------------------------------------------------------*
*       Reads the cost-center from the CSKS table .                   *
*---------------------------------------------------------------------*
FORM u100.

*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
* PLEASE DELETE THE FIRST '*' FORM THE BEGINING OF THE FOLLOWING LINES *
*        IF THE ACCOUNTING MODULE IS INSTALLED IN YOUR SYSTEM:         *
*  SELECT * FROM CSKS
*            WHERE KOSTL EQ COBL-KOSTL
*              AND KOKRS EQ COBL-KOKRS.
*    IF CSKS-DATBI >= SY-DATUM AND
*       CSKS-DATAB <= SY-DATUM.
*
*      MOVE CSKS-ABTEI TO COBL-KOSTL.
*
*    ENDIF.
*  ENDSELECT.
*--------------------------------------------------------------------*
*   Definición de tipos
*--------------------------------------------------------------------*
*.....Inicio Modificacion SA 56043
  TYPES:

    BEGIN OF l_tp_aufk,
      aufnr TYPE aufnr,     "Número de la orden
      auart TYPE aufart,
      autyp TYPE auftyp,    "Tipo de orden
      kostl TYPE aufkostl,  "Centro de coste para liquidación simple
    END OF l_tp_aufk,

    BEGIN OF l_tp_csks,
      kokrs TYPE kokrs,     "Sociedad CO
      kostl TYPE kostl,     "Centro de coste
      kosar TYPE kosar,     "Clase del centro de coste
    END OF l_tp_csks.
*----------------------------------------------------------------------
* Definición de constantes
*----------------------------------------------------------------------
  CONSTANTS: c_modulo     TYPE char2  VALUE 'FI',
             c_aplicacion TYPE char6  VALUE 'E1064',
             c_tip_ceco   TYPE char10 VALUE 'TIPOCECO'.
*----------------------------------------------------------------------
* Definición de variables locales
*----------------------------------------------------------------------
  DATA:   "Variable de respuesta de parametro
    result       TYPE i,
    "Tipo de CECO
    es_autyp     TYPE ztcx0001,        "ZTCXR1000_1,
    ".Check de sustición
    check        TYPE c,
    o_parametros TYPE REF TO zclcxr1000_parametros.
*--------------------------------------------------------------------*
*   Definición de estructuras
*--------------------------------------------------------------------*
  DATA: es_aufk       TYPE l_tp_aufk,
        "Estructura maestro ceco
        es_csks       TYPE l_tp_csks,
        ".Fin Modificacion SA 56043
        kosar         LIKE csks-kosar,
        ".Cuenta
        hkont         LIKE bseg-hkont,
        r_clase_orden TYPE RANGE OF aufk-auart,
        r_tipo_ceco   TYPE RANGE OF aufk-autyp.

*--------------------------------------------------------------------*
*   Definición de constantes
*--------------------------------------------------------------------*
*.Inicio CCF_01_1
  CONSTANTS:
    " Cuenta 55
    c_55     TYPE char2 VALUE '55',
    " Cuenta 56
    c_56     TYPE char2 VALUE '56',
    ". Cuenta 58
    c_58     TYPE char2 VALUE '58',
    ".Cuenta 57
    c_57     TYPE char2 VALUE '57',
    ".Cuenta 73
    c_73     TYPE char2 VALUE '73',
    " Clase centro de coste sourcing
    c_kosar  TYPE char1 VALUE 'S',
    " clase centro de coste Restaurante
    c_kosar2 TYPE char1 VALUE 'R',
    ".clase de centro de coste innovación
    c_n      TYPE char1 VALUE 'P',
    c_zsou   TYPE char4 VALUE 'ZSOU',
    c_zres   TYPE char4 VALUE 'ZRES',
    c_zpro   TYPE char4 VALUE 'ZPRO',
    c_zpac   TYPE char4 VALUE 'ZPAC'.

*.Fin CCF_01_1

*.Inicio Modificacion SA 56043

*  CALL METHOD zcl_cx_parametro_general=>recuperar_parametro
*    EXPORTING
*      i_c_modulo     = c_modulo
*      i_c_aplicacion = c_aplicacion
*      i_c_id         = c_tip_ceco
*    IMPORTING
*      e_es_parametro = es_autyp
*      e_i_result     = result.

*.Fin Modificacion SA 56043

  "+SLS 08022023 - recuperar parametro para Ordenes de Produccion
  o_parametros = NEW zclcxr1000_parametros( i_modulo = 'FI' i_ricefw = 'E1064' ).

  r_clase_orden = VALUE #( FOR es_parametro IN o_parametros->gti_parametros WHERE ( idparam = 'CLORDEN' )
                              ( CORRESPONDING #( es_parametro MAPPING option = opti ) )
                         ).

  r_tipo_ceco = VALUE #( FOR es_parametro IN o_parametros->gti_parametros WHERE ( idparam = c_tip_ceco )
                              ( CORRESPONDING #( es_parametro MAPPING option = opti ) )
                         ).

* Si la clase de cuenta es K(Acreedor) y la transacción es MIRO
* Se recupera de memoria ABAP la cuenta divergente y se reemplaza
* en el campo HKONT Cuenta mayor
  IF bseg-koart EQ 'K' AND sy-tcode EQ 'MIRO'.
    IMPORT g_c_hkont = hkont FROM MEMORY ID 'ZHKONT'.
    IF sy-subrc EQ 0 AND hkont IS NOT INITIAL.
      MOVE hkont TO bseg-hkont.
    ENDIF.
  ENDIF.

  zclfie1098_sust_cuenta_contabl=>sustituir_cuenta_contable(
*      EXPORTING
*        i_ti_bkpf       = i_ti_bkpf[]       " Standard Table Type for BKPF
    CHANGING
      c_es_bkpf       = bkpf
      c_es_bseg       = bseg
*        c_ti_bseg_subst = c_ti_bseg_subst[] " Table Type BSEG_SUBST
  ).


* se valida si la cuenta se debe sustituir
  SELECT SINGLE hkont
    INTO hkont
    FROM ztfi0007
    WHERE hkont EQ bseg-hkont.
  IF sy-subrc EQ 0.
    EXIT.
  ENDIF.

  IF bseg-hkont(2) = '50'.

* busqueda del tipo de centro de costo del doc contable
    SELECT SINGLE kosar
           INTO kosar
           FROM csks
           WHERE kokrs = bseg-kokrs AND
                 kostl = bseg-kostl AND
                 datbi >= sy-datum AND
                 datab <= sy-datum.

*.....Inicio Modificacion SA 56043
    IF kosar IS INITIAL .

      IF lines( r_tipo_ceco[] ) GT 0.
*.......Se consultan los datos de Generated Table for View CAUFV
        SELECT SINGLE aufnr auart autyp kostl
          INTO es_aufk
          FROM aufk
          WHERE aufnr EQ bseg-aufnr
            AND autyp IN r_tipo_ceco.
*            AND autyp EQ es_autyp-valtxt1.

        IF sy-subrc EQ 0.
*  ...........Se va por el Registro maestro del centro de coste
          SELECT SINGLE kokrs kostl kosar
            INTO es_csks
            FROM csks
            WHERE kokrs EQ bseg-kokrs
              AND kostl EQ es_aufk-kostl.

          IF sy-subrc EQ 0.
*  ...............Se asigna la clase de centro de costo
            kosar = es_csks-kosar.

          ENDIF.
        ENDIF.
      ENDIF.
    ENDIF.
*.....Fin Modificacion SA 56043
* sustitucion de la cuenta segun el tipo de CeCo
    CASE kosar.
      WHEN 'A'.
        CONCATENATE '51' bseg-hkont+2(8) INTO bseg-hkont.
        check = 'X'.
      WHEN 'F' OR 'D' OR 'I'.
        CONCATENATE '73' bseg-hkont+2(8) INTO bseg-hkont.
        check = 'X'.
      WHEN 'V' OR 'E'.
        CONCATENATE '52' bseg-hkont+2(8) INTO bseg-hkont.
        check = 'X'.
*.Inicio CCF_01_2
      WHEN c_kosar.    " Sourcing
        CONCATENATE c_56 bseg-hkont+2(8) INTO bseg-hkont.
        check = 'X'.
      WHEN c_kosar2.   " Restaurante
        CONCATENATE c_55 bseg-hkont+2(8) INTO bseg-hkont.
        check = 'X'.
      WHEN c_n.
        CONCATENATE c_58 bseg-hkont+2(8) INTO bseg-hkont.
        check = 'X'.
*.fin CCF_01_2
    ENDCASE.

    IF check IS INITIAL.

*.......Se consultan los datos de Generated Table for View CAUFV
      SELECT SINGLE aufnr auart autyp kostl
        INTO es_aufk
        FROM aufk
        WHERE aufnr EQ bseg-aufnr.

      IF sy-subrc EQ 0.

        ".Sustitución por Orden
        CASE es_aufk-auart.
          WHEN c_zsou.
            CONCATENATE c_56 bseg-hkont+2(8) INTO bseg-hkont.
          WHEN c_zres.
            CONCATENATE c_55 bseg-hkont+2(8) INTO bseg-hkont.
          WHEN c_zpro.
            CONCATENATE c_58 bseg-hkont+2(8) INTO bseg-hkont.
          WHEN c_zpac.
            CONCATENATE c_57 bseg-hkont+2(8) INTO bseg-hkont.
        ENDCASE.

        "+SLS Clase de orden PM{
*        IF r_clase_orden IS NOT INITIAL AND
*           es_aufk-auart IN r_clase_orden.
*
*          bseg-hkont = |{ c_73 }{ bseg-hkont+2(8) }| .
*
*        ENDIF.
        "}

      ENDIF.

    ENDIF.
    CLEAR check.

  ENDIF.


ENDFORM.                                                    "U100

* eject
*---------------------------------------------------------------------*
*       FORM U101                                                     *
*---------------------------------------------------------------------*
*       Reads the cost-center from the CSKS table for accounting      *
*       area '0001'.                                                  *
*       This exit uses a parameter for the cost_center so it can      *
*       be used irrespective of the table used in the callup point.   *
*---------------------------------------------------------------------*
FORM u101 USING cost_center.

*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
* PLEASE DELETE THE FIRST '*' FORM THE BEGINING OF THE FOLLOWING LINES *
*        IF THE ACCOUNTING MODULE IS INSTALLED IN YOUR SYSTEM:         *
*  SELECT * FROM CSKS
*            WHERE KOSTL EQ COST_CENTER
*              AND KOKRS EQ '0001'.
*    IF CSKS-DATBI >= SY-DATUM AND
*       CSKS-DATAB <= SY-DATUM.
*
*      MOVE CSKS-ABTEI TO COST_CENTER .
*
*    ENDIF.
*  ENDSELECT.

ENDFORM.                                                    "U101

* eject
*---------------------------------------------------------------------*
*       FORM U102                                                     *
*---------------------------------------------------------------------*
*       Inserts the sum of the posting into the reference field.      *
*       This exit can be used in FI for the complete document.        *
*       The complete data is passed in one parameter.                 *
*---------------------------------------------------------------------*


*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
* PLEASE DELETE THE FIRST '*' FORM THE BEGINING OF THE FOLLOWING LINES *
*        IF THE ACCOUNTING MODULE IS INSTALLED IN YOUR SYSTEM:         *
*FORM u102 USING bool_data TYPE gb002_015.
*DATA: SUM(10) TYPE C.
*
*    LOOP AT BOOL_DATA-BSEG INTO BSEG
*                    WHERE    SHKZG = 'S'.
*       BSEG-ZUONR = 'Test'.
*       MODIFY BOOL_DATA-BSEG FROM BSEG.
*       ADD BSEG-DMBTR TO SUM.
*    ENDLOOP.
*
*    BKPF-XBLNR = TEXT-001.
*    REPLACE '&' WITH SUM INTO BKPF-XBLNR.
*
*ENDFORM.


***********************************************************************
** EXIT EXAMPLES FROM PUBLIC SECTOR INDUSTRY SOLUTION
**
** PLEASE DELETE THE FIRST '*' FORM THE BEGINING OF THE FOLLOWING LINE
** TO ENABLE PUBLIC SECTOR EXAMPLE SUBSTITUTION EXITS
***********************************************************************
*INCLUDE rggbs_ps_forms.


*eject
* begin of insertion                                          "wms092357
*&---------------------------------------------------------------------*
*&      Form  u200
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
FORM u200 USING e_rmvct TYPE bseg-bewar.
  PERFORM xref_to_rmvct USING bkpf bseg 1 CHANGING e_rmvct.
ENDFORM.
*&---------------------------------------------------------------------*
*&      Form  u111.
*&---------------------------------------------------------------------*
FORM u111 USING bool_data TYPE gb002_015.
***----------------------------------------------------------------------*
** Información General
**----------------------------------------------------------------------*
** Identificador: E00233
** Programa     : ZFIE1002_RGGBS000 (En el sistema migrado esta como
**                ZFIRAFS0004
** Tipo Objeto  : Reporte
** Descripción  : Sustitución BSEG-ZUONR en posiciones vacias
**                Se creo la subrutina u110.
** Autor Prog.  : Johnny López
** Fecha Creac. : 19.01.2021
**----------------------------------------------------------------------*
** Ordenes de Transporte
**----------------------------------------------------------------------*
** Fecha       | CR#         | Autor           | Modificación
**----------------------------------------------------------------------*
** 19.01.2021    D01K981182    Johnny López     Creación
**----------------------------------------------------------------------*
** Autor Prog.  : Elizabeth Franco - Migración AFS a S4/HANA
** Fecha Creac. : 28.09.2021
*                 SE realiza esta validacion y sustitución por
**----------------------------------------------------------------------*
*  DATA:
*  l_ti_bseg  TYPE bseg_tab,
*  l_es_bseg  TYPE bseg.
*
*  FIELD-SYMBOLS:
*  <l_fs_bseg> TYPE bseg.
*
*
*  l_ti_bseg = bool_data-bseg.
*
*  DELETE l_ti_bseg WHERE bschl EQ '31'.
*  DELETE l_ti_bseg WHERE zuonr IS INITIAL.
*
*  IF l_ti_bseg IS INITIAL.
*    MESSAGE e000(gk) WITH 'El campo asignación no puede estar vacío' 'para la primera posición'.
*  ENDIF.
*
*  READ TABLE l_ti_bseg INTO l_es_bseg INDEX 1.
*
*
*  LOOP AT bool_data-bseg ASSIGNING <l_fs_bseg> WHERE bschl NE '31'.
*    IF <l_fs_bseg>-zuonr IS INITIAL.
*      <l_fs_bseg>-zuonr = l_es_bseg-zuonr.
*    ENDIF.
*  ENDLOOP.

ENDFORM.                    "u111
*&---------------------------------------------------------------------*
*&      Form  xref_to_rmvct
*&---------------------------------------------------------------------*
*       text
*----------------------------------------------------------------------*
FORM xref_to_rmvct
     USING    is_bkpf         TYPE bkpf
              is_bseg         TYPE bseg
              i_xref_field    TYPE i
     CHANGING c_rmvct         TYPE rmvct.

  DATA l_msgv TYPE symsgv.
  STATICS st_rmvct TYPE HASHED TABLE OF rmvct WITH UNIQUE DEFAULT KEY.

* either bseg-xref1 or bseg-xref2 must be used as source...
  IF i_xref_field <> 1 AND i_xref_field <> 2.
    MESSAGE x000(gk) WITH 'UNEXPECTED VALUE I_XREF_FIELD ='
      i_xref_field '(MUST BE = 1 OR = 2)' ''.
  ENDIF.
  IF st_rmvct IS INITIAL.
    SELECT trtyp FROM t856 INTO TABLE st_rmvct.
  ENDIF.
  IF i_xref_field = 1.
    c_rmvct = is_bseg-xref1.
  ELSE.
    c_rmvct = is_bseg-xref2.
  ENDIF.
  IF c_rmvct IS INITIAL.
    WRITE i_xref_field TO l_msgv LEFT-JUSTIFIED.
    CONCATENATE TEXT-m00 l_msgv INTO l_msgv SEPARATED BY space.
*   cons. transaction type is not specified => send an error message...
    MESSAGE e123(g3) WITH l_msgv.
*   Bitte geben Sie im Feld &1 eine Konsolidierungsbewegungsart an
  ENDIF.
* c_rmvct <> initial...
  READ TABLE st_rmvct TRANSPORTING NO FIELDS FROM c_rmvct.
  CHECK NOT sy-subrc IS INITIAL.
* cons. transaction type does not exist => send error message...
  WRITE i_xref_field TO l_msgv LEFT-JUSTIFIED.
  CONCATENATE TEXT-m00 l_msgv INTO l_msgv SEPARATED BY space.
  MESSAGE e124(g3) WITH c_rmvct l_msgv.
* KonsBewegungsart &1 ist ungültig (bitte Eingabe im Feld &2 korrigieren
ENDFORM.
* end of insertion                                            "wms092357
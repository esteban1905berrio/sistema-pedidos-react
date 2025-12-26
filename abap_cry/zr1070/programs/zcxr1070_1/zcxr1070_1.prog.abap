*&---------------------------------------------------------------------*
*& Report ZCXR1070_1
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
REPORT zcxr1070_1.

TABLES: tutypa, tutyppl, tutyp, tupl, tuplt, law_cont.


************************
tupl-pricelist = '05'.
tupl-deflt_utyp = 'HB'.
tupl-active = 'X'.
INSERT tupl.

tuplt-langu = 'D'.
tuplt-pricelist = '05'.
tuplt-pltext =
  'SAP S/4HANA Enterprise Mgmt.'.
INSERT tuplt.
tuplt-langu = 'E'.
INSERT tuplt.

************************

************************
tutypa-usertyp = 'HA'.
tutypa-sscr_allow = 'X'.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '250000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '05'.
tutyppl-usertyp   = 'HA'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = 'HA'.
tutyp-utyptext =
 'S/4HANA EM Developer access'.
tutyp-utyplongtext =
  'SAP S/4HANA Enterprise Management Developer access'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = 'HB'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '251000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '05'.
tutyppl-usertyp   = 'HB'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = 'HB'.
tutyp-utyptext =
 'S/4HANA EM Professional use'.
tutyp-utyplongtext =
  'SAP S/4HANA Enterprise Management for Professional use'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = 'HC'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '252000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '05'.
tutyppl-usertyp   = 'HC'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = 'HC'.
tutyp-utyptext =
 'S/4HANA EM Functional use'.
tutyp-utyplongtext =
  'SAP S/4HANA Enterprise Management for Functional use'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = 'HD'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '253000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '05'.
tutyppl-usertyp   = 'HD'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = 'HD'.
tutyp-utyptext =
 'S/4HANA EM Productivity use'.
tutyp-utyplongtext =
  'SAP S/4HANA Enterprise Management for Productivity use'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = 'HE'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '254000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '05'.
tutyppl-usertyp   = 'HE'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = 'HE'.
tutyp-utyptext =
 'S/4HANA Tech. SAP Engine User'.
tutyp-utyplongtext =
  'S/4HANA Technical SAP Engine User'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = 'HF'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '255000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '05'.
tutyppl-usertyp   = 'HF'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = 'HF'.
tutyp-utyptext =
 'S/4HANA Technical Use'.
tutyp-utyplongtext =
  'S/4HANA Technical Use'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = '68'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '610000'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '02'.
tutyppl-usertyp   = '68'.
INSERT tutyppl.
tutyppl-pricelist = '03'.
tutyppl-usertyp   = '68'.
INSERT tutyppl.
tutyppl-pricelist = '04'.
tutyppl-usertyp   = '68'.
INSERT tutyppl.
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '68'.
INSERT tutyppl.
tutyppl-pricelist = '06'.
tutyppl-usertyp   = '68'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = '68'.
tutyp-utyptext =
 'Unattended SAP IRPA'.
tutyp-utyplongtext =
  'Unattended SAP IRPA'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

************************
tutypa-usertyp = '69'.
tutypa-sscr_allow = ' '.
tutypa-active = 'X'.
tutypa-sondervers = ' '.
tutypa-country = ' '.
tutypa-sort = '610500'.
tutypa-charge_info = 'C'.
INSERT tutypa.

tutyppl-pricelist = '02'.
tutyppl-usertyp   = '69'.
INSERT tutyppl.
tutyppl-pricelist = '03'.
tutyppl-usertyp   = '69'.
INSERT tutyppl.
tutyppl-pricelist = '04'.
tutyppl-usertyp   = '69'.
INSERT tutyppl.
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '69'.
INSERT tutyppl.
tutyppl-pricelist = '06'.
tutyppl-usertyp   = '69'.
INSERT tutyppl.

tutyp-langu = 'D'.
tutyp-usertyp = '69'.
tutyp-utyptext =
 'Unattended 3rd Party RPA'.
tutyp-utyplongtext =
  'Unattended 3rd Party RPA'.
INSERT tutyp.
tutyp-langu = 'E'.
INSERT tutyp.

*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '04'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '11'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '91'.
INSERT tutyppl.
*************************

*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '71'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '72'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '73'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '74'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '75'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '76'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '77'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '78'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '79'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '80'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '81'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '82'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '83'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '84'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '85'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '86'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '87'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '88'.
INSERT tutyppl.
*************************
tutyppl-pricelist = '05'.
tutyppl-usertyp   = '89'.
INSERT tutyppl.
*************************

*************************
law_cont-usertyp = 'HA'.
law_cont-containsu = 'HE'.
INSERT law_cont.
law_cont-usertyp = 'HB'.
law_cont-containsu = 'HC'.
INSERT law_cont.
law_cont-usertyp = 'HB'.
law_cont-containsu = 'HD'.
INSERT law_cont.
law_cont-usertyp = 'HB'.
law_cont-containsu = 'HE'.
INSERT law_cont.
law_cont-usertyp = 'HC'.
law_cont-containsu = 'HD'.
INSERT law_cont.
law_cont-usertyp = 'HC'.
law_cont-containsu = 'HE'.
INSERT law_cont.
law_cont-usertyp = 'HD'.
law_cont-containsu = 'HE'.
INSERT law_cont.
law_cont-usertyp = 'HA'.
law_cont-containsu = '04'.
INSERT law_cont.
law_cont-usertyp = 'HA'.
law_cont-containsu = '11'.
INSERT law_cont.
law_cont-usertyp = 'HA'.
law_cont-containsu = '91'.
INSERT law_cont.
law_cont-usertyp = 'HB'.
law_cont-containsu = '04'.
INSERT law_cont.
law_cont-usertyp = 'HB'.
law_cont-containsu = '11'.
INSERT law_cont.
law_cont-usertyp = 'HB'.
law_cont-containsu = '91'.
INSERT law_cont.
law_cont-usertyp = 'HC'.
law_cont-containsu = '04'.
INSERT law_cont.
law_cont-usertyp = 'HC'.
law_cont-containsu = '11'.
INSERT law_cont.
law_cont-usertyp = 'HC'.
law_cont-containsu = '91'.
INSERT law_cont.
law_cont-usertyp = 'HD'.
law_cont-containsu = '04'.
INSERT law_cont.
law_cont-usertyp = 'HD'.
law_cont-containsu = '11'.
INSERT law_cont.
law_cont-usertyp = 'HD'.
law_cont-containsu = '91'.
INSERT law_cont.
*************************
UPDATE tupl SET active = ' ' WHERE pricelist = '01' OR pricelist = '02' OR pricelist = '03' OR pricelist = '04'.
UPDATE tupl SET deflt_utyp = 'HB' WHERE pricelist = '05'.
UPDATE tutyp SET utyptext = 'S/4HANA Tech. SAP Engine User' WHERE usertyp = 'HE' AND ( langu = 'D' OR langu = 'E' ).
UPDATE tutyp SET utyptext = 'S/4HANA Technical Use' WHERE usertyp = 'HF' AND ( langu = 'D' OR langu = 'E' ).
UPDATE tutyp SET utyplongtext = 'S/4HANA Technical SAP Engine User' WHERE usertyp = 'HE' AND ( langu = 'D' OR langu = 'E' ).
UPDATE tutyp SET utyplongtext = 'S/4HANA Technical Use' WHERE usertyp = 'HF' AND ( langu = 'D' OR langu = 'E' ).

MESSAGE TEXT-001 TYPE 'I'.
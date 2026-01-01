*&---------------------------------------------------------------------*
*& Include zcxc1017p_1
*&---------------------------------------------------------------------*
SELECTION-SCREEN BEGIN OF BLOCK bk1 WITH FRAME TITLE  TEXT-001 .

  PARAMETERS: pa_nach TYPE localfile.
  PARAMETERS: pa_ntbl TYPE dd02l-tabname.

SELECTION-SCREEN END OF BLOCK bk1.

SELECTION-SCREEN BEGIN OF BLOCK bk3 WITH FRAME TITLE  TEXT-003 .

  PARAMETERS : pa_visu AS CHECKBOX DEFAULT 'X',
               pa_ilc  AS CHECKBOX  DEFAULT 'X' MODIF ID m1,
               pa_iman AS CHECKBOX DEFAULT 'X'.

SELECTION-SCREEN END OF BLOCK bk3.

SELECTION-SCREEN BEGIN OF BLOCK bk2 WITH FRAME TITLE  TEXT-002 .
  PARAMETERS: pa_trtb AS CHECKBOX MODIF ID m1.
SELECTION-SCREEN END OF BLOCK bk2.
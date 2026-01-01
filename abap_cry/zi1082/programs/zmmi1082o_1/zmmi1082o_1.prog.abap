*&---------------------------------------------------------------------*
*& Include zmmi1082o_1
*&---------------------------------------------------------------------*
*&---------------------------------------------------------------------*
*& Module STATUS_0100 OUTPUT
*&---------------------------------------------------------------------*
*&
*&---------------------------------------------------------------------*
MODULE status_dynr OUTPUT.

  CLEAR: gti_ucomm.

  CASE sy-dynnr.
    WHEN 100.
      APPEND 'CONSULTAR' TO gti_ucomm.
      SET TITLEBAR 'GT_0001'.
    WHEN 200.
      APPEND 'REPMANUAL' TO gti_ucomm.
      SET TITLEBAR 'GT_0002'.
    WHEN 300.
      APPEND 'CARGAARCH' TO gti_ucomm.
      SET TITLEBAR 'GT_0003'.
    WHEN 400.
      APPEND 'CALENDARIO' TO gti_ucomm.
      SET TITLEBAR 'GT_0004'.
  ENDCASE.

  SET PF-STATUS 'GS_0001' EXCLUDING gti_ucomm.

ENDMODULE.
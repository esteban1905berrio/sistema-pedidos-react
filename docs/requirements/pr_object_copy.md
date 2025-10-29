Como desarrollador del ABAP
Quiero Ejecutar el escenario para copiar con el MCP un objeto de un sistema hacia otro.


Requerimiento:

- Copiar los FM con codigo fuente incluido

 ZFIE1017_DMEE_IDENT_RECEP_BOG (30 chars)
      → ZFIAAC002_DMEE_IDENT_RECEP_BO (30 chars)

   [2/7] ZFIE1017_DMEE_NIT (18 chars)
      → ZFIAAC002_DMEE_NIT (20 chars)

   [3/7] ZFIE1017_DMEE_NOMBRE_EMPRESA (29 chars)
      → ZFIAAC002_DMEE_NOMBRE_EMPRES (30 chars)

   [4/7] ZFIE1017_DMEE_NRO_CUENTA_BOG (29 chars)
      → ZFIAAC002_DMEE_NRO_CUENTA_BO (30 chars)

   [5/7] ZFIE1017_DMEE_TP_CUENTA_DISPER (31 chars)
      → ZFIAAC002_DMEE_TP_CUENTA_DISP (30 chars)

   [6/7] ZFIE1017_DMEE_TP_CUENT_BENEFIC (31 chars)
      → ZFIAAC002_DMEE_TP_CUENT_BENEF (30 chars)

   [7/7] ZFIE1017_DMEE_TP_IDENTI_BOG (27 chars)
      → ZFIAAC002_DMEE_TP_IDENTI_BOG (29 chars)


- Sistema fuente CRY destino GDC
- Grupo de funcion destino ZFIAAC002_1.

Criterios de aceptacion

- No activar objetos
- Copiar Firma/interfaz de los FM
- dentro del codigo fuente a copiar remplazar ztcxr1000_1-idparam por string y ztcxr1000_1-idcomo por string
- Sistema fuente CRY destino GDC
- Grupo de funcion destino ZFIAAC002_1.
- OT CADK910827
- Paquete ZFI
- Utilizar el MCP

Como desarrollador del MCP
Quiero Ejecutar el escenario para copiar con el MCP un objeto de un lado hacia otro.
Para probar la copia de objetos entre sistemas.

Requerimiento:

| #   | Módulo Original (CRY)          | Módulo Destino (GDC)           | Longitud |
  |-----|--------------------------------|--------------------------------|----------|
  | 1   | ZFIE1017_DMEE_TP_IDENTIFICACIO | ZFIAAC002_DMEE_TP_IDENTIFICACI | 30 ✓     |
  | 2   | ZFIE1017_DMEE_IDENT_RECEP_BBVA | ZFIAAC002_DMEE_IDENT_RECEP_BBV | 30 ✓     |
  | 3   | ZFIE1017_DMEE_CUENTA_BBVA      | ZFIAAC002_DMEE_CUENTA_BBVA     | 26 ✓     |
  | 4   | ZFIE1017_DMEE_TP_CUENTA_BBVA   | ZFIAAC002_DMEE_TP_CUENTA_BBVA  | 29 ✓     |
  | 5   | ZFIE1017_DMEE_NRO_CUENTA_BBVA  | ZFIAAC002_DMEE_NRO_CUENTA_BBVA | 30 ✓     |
  | 6   | ZFIE1017_DMEE_DIRECCION_BBVA   | ZFIAAC002_DMEE_DIRECCION_BBVA  | 29 ✓     |
  | 7   | ZFIE1017_DMEE_EMAIL_BBVA       | ZFIAAC002_DMEE_EMAIL_BBVA      | 25 ✓     |
  | 8   | ZFIE1017_DMEE_NIT_BBVA         | ZFIAAC002_DMEE_NIT_BBVA        | 23 ✓     |

- Sistema fuente CRY destino GDC
- Grupo de funcion destino ZFIAAC002_1.
- Copiar modulos de funcion: ZFIE1017_DMEE_VALOR_TOTAL_2DEC, ZFIE1017_DMEE_CONSECUTIVO_PAGO, ZFIE1017_DMEE_VALOR_TOTAL_2DEC, ZFIE1017_DMEE_CONSEC_OCCIDENT, ZFIE1017_DMEE_NIT_CC, ZFIE1017_DMEE_FORMA_DE_PAGO_OC, ZFIE1017_DMEE_VALOR_POSIC_2DEC, ZFIE1017_DMEE_COMPROBANTE, ZFIE1017_DMEE_TP_CUENT_DESTINO, ZFIE1017_DMEE_VALOR_TOTAL_2DEC.

- Excepto ZFIAAC002_DMEE_NRO_TRASL_DAV.
- Remplazar ZFIE1017 por ZFIAAC002, y para los nombre que queden con mas de 30 caracteres, trunca las ultimas letras del nombre hasta que cumplan los 30 caracteres.

Criterios de aceptacion

- Sistema fuente CRY destino GDC
- Grupo de funcion destino ZFIAAC002_1.
- OT CADK910827
- Paquete ZFI
- Utilizar el MCP
- Ir evalundo la respuesta de las tools para ir tratando de correguir errores y que el escenario funcione bien.
- utiliza los MCP de context7 y sequential-thinking para evaluar los errores que de el MCP, pero esto lo haces con guia mia.
# Propuesta: Function Module para Leer Log de Liberación

**Fecha**: 2025-11-18
**Estado**: Propuesta para aprobación

---

## 📋 Problema

La modificación directa de `ZCLCX_TRANSPORT_MANAGEMENT::visualizar_log()` en GDC está presentando errores de ADT API. Como alternativa, propongo crear un **Function Module independiente** que:

1. Sea más fácil de crear/modificar
2. Pueda ser llamado directamente desde el MCP tool Java
3. No dependa de modificaciones complejas a la clase existente

---

## 🎯 Propuesta: Function Module ZCX_GET_TRANSPORT_LOG

### Signature

```abap
FUNCTION ZCX_GET_TRANSPORT_LOG
  IMPORTING
    VALUE(IV_TRKORR) TYPE TRKORR
    VALUE(IV_DETAIL_LEVEL) TYPE CHAR1 DEFAULT '3'
  EXPORTING
    VALUE(EV_LOG_TEXT) TYPE STRING
    VALUE(EV_STATUS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
  EXCEPTIONS
    TRANSPORT_NOT_FOUND
    ERROR_READING_LOG.
```

### Parámetros

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `IV_TRKORR` | IMPORT | Número de orden de transporte |
| `IV_DETAIL_LEVEL` | IMPORT | Nivel de detalle ('1'=Error, '2'=Warning, '3'=Info, '4'=Todo) |
| `EV_LOG_TEXT` | EXPORT | Log formateado como texto |
| `EV_STATUS` | EXPORT | Estado: 'S'=Success, 'E'=Error, 'W'=Warning |
| `EV_MESSAGE` | EXPORT | Mensaje de resultado |

### Implementación

```abap
FUNCTION zcx_get_transport_log.
*"----------------------------------------------------------------------
*"*"Local Interface:
*"  IMPORTING
*"     VALUE(IV_TRKORR) TYPE  TRKORR
*"     VALUE(IV_DETAIL_LEVEL) TYPE  CHAR1 DEFAULT '3'
*"  EXPORTING
*"     VALUE(EV_LOG_TEXT) TYPE  STRING
*"     VALUE(EV_STATUS) TYPE  CHAR1
*"     VALUE(EV_MESSAGE) TYPE  STRING
*"  EXCEPTIONS
*"      TRANSPORT_NOT_FOUND
*"      ERROR_READING_LOG
*"----------------------------------------------------------------------

  DATA: lv_protname TYPE protname,
        lt_msg      TYPE rs_t_msg,
        lv_log_text TYPE string.

  " Validar que la OT existe
  SELECT SINGLE trkorr
    FROM e070
    WHERE trkorr = @iv_trkorr
    INTO @DATA(lv_exists).

  IF sy-subrc <> 0.
    ev_status = 'E'.
    ev_message = |Orden de transporte { iv_trkorr } no encontrada|.
    RAISE transport_not_found.
  ENDIF.

  " Construir nombre del log (formato Application Log: /CTS/<trkorr>)
  lv_protname = |/CTS/{ iv_trkorr }|.

  " Leer log de transporte usando RSDG_TR_GET_PROTOCOL
  CALL FUNCTION 'RSDG_TR_GET_PROTOCOL'
    EXPORTING
      i_protnm               = lv_protname
      i_detlevel             = iv_detail_level
    IMPORTING
      e_t_msg                = lt_msg
    EXCEPTIONS
      error_reading_protocol = 1
      OTHERS                 = 2.

  IF sy-subrc <> 0.
    ev_status = 'E'.
    ev_message = |Error al leer log de OT { iv_trkorr }|.
    RAISE error_reading_log.
  ENDIF.

  " Verificar si hay mensajes
  IF lt_msg IS INITIAL.
    ev_status = 'W'.
    ev_message = |No hay log disponible para OT { iv_trkorr }|.
    ev_log_text = |Log de liberación no disponible|.
    RETURN.
  ENDIF.

  " Convertir mensajes a texto legible
  DATA: lv_line TYPE string.

  LOOP AT lt_msg ASSIGNING FIELD-SYMBOL(<msg>).
    " Formato: [Tipo] ID/Número: Mensaje
    lv_line = |[{ <msg>-msgty }] { <msg>-msgid }/{ <msg>-msgno ALPHA = OUT }: | &&
              |{ <msg>-msgv1 } { <msg>-msgv2 } { <msg>-msgv3 } { <msg>-msgv4 }|.

    " Agregar salto de línea
    IF lv_log_text IS INITIAL.
      lv_log_text = lv_line.
    ELSE.
      lv_log_text = lv_log_text && cl_abap_char_utilities=>newline && lv_line.
    ENDIF.
  ENDLOOP.

  " Resultado exitoso
  ev_log_text = lv_log_text.
  ev_status = 'S'.
  ev_message = |Log de OT { iv_trkorr } leído exitosamente ({ lines( lt_msg ) } mensajes)|.

ENDFUNCTION.
```

---

## 🔧 Implementación en Java

### TransportCopyService Modification

Agregar método para leer log de transporte:

```java
/**
 * Reads the release log of a transport request.
 *
 * @param transportNumber Transport request number
 * @param detailLevel Detail level: '1'=Error, '2'=Warning, '3'=Info, '4'=All
 * @return Transport log as formatted text
 * @throws JCoException if RFC communication fails
 */
public String getTransportLog(String transportNumber, String detailLevel)
        throws JCoException {

    JCoFunction function = destination.getRepository()
        .getFunction("ZCX_GET_TRANSPORT_LOG");

    if (function == null) {
        throw new RuntimeException(
            "Function module ZCX_GET_TRANSPORT_LOG not found in SAP system"
        );
    }

    // Set import parameters
    function.getImportParameterList().setValue("IV_TRKORR", transportNumber);
    function.getImportParameterList().setValue("IV_DETAIL_LEVEL",
        detailLevel != null ? detailLevel : "3");

    // Execute
    function.execute(destination);

    // Get results
    String logText = function.getExportParameterList().getString("EV_LOG_TEXT");
    String status = function.getExportParameterList().getString("EV_STATUS");
    String message = function.getExportParameterList().getString("EV_MESSAGE");

    logger.info("Transport log retrieved: status={}, message={}", status, message);

    return logText != null ? logText : "No log available";
}
```

### TransportCopyResult Enhancement

Agregar campo para el log:

```java
public record TransportCopyResult(
    String newTransportNumber,
    String status,
    String message,
    boolean success,
    String releaseLog  // ← NUEVO
) {
    // ... factory methods ...
}
```

### TransportCopyService Update

Modificar `createTransportCopy()` para incluir log:

```java
public TransportCopyResult createTransportCopy(TransportCopyRequest request)
        throws JCoException {

    // ... código existente ...

    // Si liberación exitosa, obtener log
    String releaseLog = null;
    if ("S".equals(status) && newTransport != null) {
        try {
            releaseLog = getTransportLog(newTransport, "3");
        } catch (Exception e) {
            logger.warn("Could not retrieve release log for {}: {}",
                       newTransport, e.getMessage());
            releaseLog = "Log not available";
        }
    }

    return new TransportCopyResult(
        newTransport,
        status,
        message,
        "S".equals(status),
        releaseLog
    );
}
```

---

## ✅ Ventajas de Esta Propuesta

1. **Independiente**: FM separado, no requiere modificar clase existente
2. **Reutilizable**: Puede ser llamado desde cualquier código ABAP
3. **Testeable**: Fácil de probar en SE37
4. **Java-friendly**: Signature clara para JCo
5. **Evita ADT API issues**: No depende de modify_class

---

## 🧪 Testing

### Test en SE37

```abap
" Input:
IV_TRKORR = 'CADK911511'
IV_DETAIL_LEVEL = '3'

" Expected Output:
EV_STATUS = 'S'
EV_MESSAGE = 'Log de OT CADK911511 leído exitosamente (XX mensajes)'
EV_LOG_TEXT = '[I] TR/001: Orden liberada exitosamente
[I] TR/002: Export completado
[S] TR/003: 5 objetos exportados
...'
```

### Test en Java

```java
@Test
void testGetTransportLog() throws JCoException {
    String log = transportCopyService.getTransportLog("CADK911511", "3");

    assertNotNull(log);
    assertTrue(log.contains("["));  // Formato [Tipo]
    logger.info("Transport log:\n{}", log);
}
```

---

## 📝 Pasos de Implementación

1. ✅ Crear FM `ZCX_GET_TRANSPORT_LOG` en ZGFCX_1
2. ✅ Activar FM en GDC
3. ✅ Test en SE37 con OT real
4. ✅ Implementar método `getTransportLog()` en TransportCopyService.java
5. ✅ Modificar `TransportCopyResult` para incluir log
6. ✅ Actualizar `createTransportCopy()` para llamar `getTransportLog()`
7. ✅ Test Java end-to-end
8. ✅ Documentar en README_JAVA.md

---

## ❓ Aprobación Requerida

¿Apruebas esta propuesta de crear un FM independiente `ZCX_GET_TRANSPORT_LOG` en lugar de modificar `visualizar_log()` de la clase?

**Ventajas**:
- ✅ Más simple de implementar
- ✅ Evita problemas con ADT API
- ✅ Reutilizable desde otros programas
- ✅ Testeable en SE37

**Desventajas**:
- ⚠️ Requiere crear un FM adicional (ya tienes varios, así que no es problema)
- ⚠️ La clase `ZCLCX_TRANSPORT_MANAGEMENT` no se modifica (pero puede llamar al FM si lo necesita)

---

**Creado**: 2025-11-18
**Autor**: Crystal Development Team
**Estado**: Pendiente aprobación

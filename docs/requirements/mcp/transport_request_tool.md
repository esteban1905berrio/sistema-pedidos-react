# Transport Request Import Tool (STMS-like)

## User Story

Como desarrollador Java/ABAP
Quiero crear una tool que me ayude a transportar las ordenes entre ambientes.

## Criterios de Aceptacion

- [x] Sistema de trabajo GDCMCP
- [x] No buscar FM con las tools de GDCMCP
- [x] Debe recibir una o varias OT
- [x] Debe funcionar como la STMS
- [x] Buscar funcion ADT, FM o clase para ejecutar esta accion
- [ ] Crear RFC wrapper con la funcionalidad

## Decisiones de Diseno

| Decision | Valor |
|----------|-------|
| Nombre FM | `ZCX_TMS_IMPORT_REQUEST` |
| Function Group | `ZGFCX_1` |
| Funcionalidad | Solo Import (sin Forward previo) |
| Prerequisito | OTs ya liberadas y en buffer del destino |

---

## Hallazgos de Investigacion

### APIs Disponibles

| Operacion | API | RFC-Enabled | Notas |
|-----------|-----|-------------|-------|
| Release | ADT REST `/sap/bc/adt/cts/transportrequests/{trkorr}/newreleasejobs` | Si (via SADT_REST_RFC_ENDPOINT) | Ya disponible |
| Import | `CTS_API_IMPORT_CHANGE_REQUEST` | **NO** | Necesita wrapper |
| Import | `TMS_MGR_IMPORT_TR_REQUEST` | **NO** | Mas completo pero complejo |
| Forward | `TMS_MGR_FORWARD_TR_REQUEST` | **NO** | Anade al buffer destino |

### Estructura CTS_REQ

```
REQUEST: CHAR(20) - numero de transporte
RETCODE: CHAR(3)  - codigo de retorno
```

---

## Diseno RFC Wrapper

### FM: `ZCX_TMS_IMPORT_REQUEST`

**Ubicacion**: Function Group `ZGFCX_1`

#### Firma

```abap
FUNCTION ZCX_TMS_IMPORT_REQUEST
  IMPORTING
    VALUE(IV_TARGET_SYSTEM) TYPE CHAR3         " Sistema destino (ej: 'S4Q')
    VALUE(IV_TARGET_CLIENT) TYPE CHAR3         " Cliente destino (ej: '100')
    VALUE(IV_TRANSPORTS)    TYPE STRING        " CSV de OTs: "GDCK900123,GDCK900124"
    VALUE(IV_IGNORE_LOCK)   TYPE CHAR1 DEFAULT ' '  " Ignorar bloqueos
    VALUE(IV_IMPORT_AGAIN)  TYPE CHAR1 DEFAULT 'X'  " Reimportar si ya importado
  EXPORTING
    VALUE(EV_SUCCESS)       TYPE CHAR1         " X = exito, blank = error
    VALUE(EV_MESSAGE)       TYPE STRING        " Mensaje resumen
    VALUE(EV_RESULTS_JSON)  TYPE STRING        " JSON con detalle por OT
  EXCEPTIONS
    SYSTEM_INVALID
    CLIENT_INVALID
    NO_TRANSPORTS
    IMPORT_FAILED.
```

#### Estructura de Respuesta JSON

```json
{
  "targetSystem": "S4Q",
  "targetClient": "100",
  "totalRequests": 2,
  "successCount": 2,
  "errorCount": 0,
  "results": [
    {
      "transport": "GDCK900123",
      "status": "000",
      "message": "Imported successfully",
      "success": true
    },
    {
      "transport": "GDCK900124",
      "status": "000",
      "message": "Imported successfully",
      "success": true
    }
  ]
}
```

#### Logica Interna

```abap
" 1. Validar parametros de entrada
IF iv_target_system IS INITIAL.
  RAISE system_invalid.
ENDIF.

" 2. Parsear lista de transportes
SPLIT iv_transports AT ',' INTO TABLE lt_transport_list.

" 3. Construir tabla CTS_REQ
LOOP AT lt_transport_list INTO lv_transport.
  ls_cts_req-request = lv_transport.
  APPEND ls_cts_req TO lt_cts_reqs.
ENDLOOP.

" 4. Llamar FM estandar
CALL FUNCTION 'CTS_API_IMPORT_CHANGE_REQUEST'
  EXPORTING
    system   = iv_target_system
    client   = iv_target_client
  IMPORTING
    retcode  = lv_retcode
    message  = lv_message
  TABLES
    requests = lt_cts_reqs
  EXCEPTIONS
    OTHERS   = 1.

" 5. Procesar resultados y generar JSON
" 6. Establecer valores de retorno
```

---

## Implementacion MCP Java

### TransportImportService.java

**Ubicacion**: `src/main/java/com/crystal/mcp/sapserver/service/`

```java
public class TransportImportService {
    public TransportImportResult importTransports(
        String targetSystem,
        String targetClient,
        List<String> transports,
        boolean ignoreLock,
        boolean importAgain
    ) {
        // Llamar FM ZCX_TMS_IMPORT_REQUEST via JCo
    }
}
```

### TransportImportTools.java

**Ubicacion**: `src/main/java/com/crystal/mcp/sapserver/tool/`

```java
@Component
public class TransportImportTools {
    @Tool(description = "Import released transport requests to target system (STMS-like)")
    public String importTransportRequests(
        @Param("targetSystem") String targetSystem,
        @Param("targetClient") String targetClient,
        @Param("transports") String transports,
        @Param(value = "ignoreLock", required = false) Boolean ignoreLock,
        @Param(value = "importAgain", required = false) Boolean importAgain
    ) {
        // Implementacion
    }
}
```

---

## Pasos de Implementacion

### Fase 1: RFC Wrapper en SAP (GDCMCP)
- [ ] Crear FM `ZCX_TMS_IMPORT_REQUEST` en ZGFCX_1
- [ ] Implementar logica usando `CTS_API_IMPORT_CHANGE_REQUEST`
- [ ] Probar manualmente en SE37

### Fase 2: Servicio Java
- [ ] Crear `TransportImportResult.java` (DTO)
- [ ] Crear `TransportImportService.java`
- [ ] Implementar llamada JCo al FM

### Fase 3: Tool MCP
- [ ] Crear `TransportImportTools.java`
- [ ] Registrar tool con anotacion `@Tool`
- [ ] Probar con ManualTest

### Fase 4: Testing
- [ ] Test unitario del servicio
- [ ] Test de integracion con SAP real

---

## Archivos a Crear/Modificar

| Archivo | Accion | Descripcion |
|---------|--------|-------------|
| FM `ZCX_TMS_IMPORT_REQUEST` | Crear | RFC wrapper en SAP |
| `TransportImportResult.java` | Crear | DTO para resultado |
| `TransportImportService.java` | Crear | Logica de negocio |
| `TransportImportTools.java` | Crear | Tool MCP |
| `ManualTransportImportTest.java` | Crear | Test manual |

---

## Consideraciones

1. **Autorizacion**: El usuario SAP debe tener S_CTS_ADMIN o similar
2. **Sistema destino**: Debe estar configurado en STMS del sistema fuente
3. **Prerequisito**: OTs deben estar liberadas antes de importar

## Referencias

- [SAP Community - TMS_MGR_IMPORT_TR_REQUEST](https://community.sap.com/t5/crm-and-cx-q-a/function-module-tms-mgr-import-tr-request-in-s4/qaq-p/14019071)
- [abap-adt-api transports.ts](https://github.com/marcellourbani/abap-adt-api/blob/main/src/api/transports.ts)
- FM existente: `CTS_API_IMPORT_CHANGE_REQUEST` en paquete SCTS_API
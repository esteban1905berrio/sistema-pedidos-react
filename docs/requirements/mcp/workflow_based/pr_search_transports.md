# PR: Implementar Tool search_transports

## Resumen

Implementar una nueva MCP tool `search_transports` que permita buscar órdenes de transporte (OTs) en SAP usando criterios flexibles como descripción, usuario, tipo, estado y rango de fechas.

## Problema

Actualmente las tools de transporte disponibles son:

| Tool | Limitación |
|------|------------|
| `list_user_transports` | Solo lista OTs del usuario actual o filtrado por status |
| `get_transport_info` | Requiere conocer el número de OT |
| `get_transport_objects` | Requiere conocer el número de OT |
| `get_object_in_open_ot` | Busca por objeto ABAP, no por OT |

**No existe** una tool que permita buscar OTs por criterios como:
- Descripción que contenga texto (ej: `*PSR01*`, `*INVOICE*`)
- Combinación de filtros (usuario + tipo + status)

## Solución Propuesta

### 1. Crear FM RFC `Z_CX_SEARCH_TRANSPORTS` en SAP

#### Tablas Involucradas

| Tabla | Campos Relevantes | Propósito |
|-------|-------------------|-----------|
| **E070** | TRKORR, TRFUNCTION, TRSTATUS, AS4USER, AS4DATE, AS4TIME, TARSYSTEM, KORRDEV, STRKORR | Cabecera de OT |
| **E07T** | TRKORR, LANGU, AS4TEXT | Descripción de OT (JOIN) |
| **E071** | TRKORR, PGMID, OBJECT, OBJ_NAME | Objetos en OT (COUNT) |

#### Firma del Function Module

```abap
FUNCTION Z_CX_SEARCH_TRANSPORTS
  IMPORTING
    VALUE(IV_DESCRIPTION) TYPE STRING OPTIONAL      " Búsqueda LIKE '%texto%'
    VALUE(IV_USER) TYPE AS4USER OPTIONAL            " AS4USER exacto o patrón
    VALUE(IV_TRANSPORT_TYPE) TYPE TRFUNCTION OPTIONAL  " K=Workbench, W=Customizing, T=Copies, S=Task
    VALUE(IV_STATUS) TYPE TRSTATUS OPTIONAL         " D=Modifiable, R=Released, L=Protected
    VALUE(IV_TARGET_SYSTEM) TYPE TR_TARGET OPTIONAL " Sistema destino (ej: S4Q, S4P)
    VALUE(IV_DATE_FROM) TYPE SYDATUM OPTIONAL       " Fecha desde (AS4DATE)
    VALUE(IV_DATE_TO) TYPE SYDATUM OPTIONAL         " Fecha hasta (AS4DATE)
    VALUE(IV_MAX_RESULTS) TYPE I DEFAULT 100        " Límite resultados (1-1000)
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_TOTAL_FOUND) TYPE I                    " Total encontrados (antes de limit)
    VALUE(EV_RESULTS_JSON) TYPE STRING.
```

#### Lógica ABAP

```abap
FUNCTION z_cx_search_transports.
*"----------------------------------------------------------------------
*" Search transport requests with flexible criteria
*"
*" Tables: E070 (header), E07T (description), E071 (objects count)
*"----------------------------------------------------------------------

  TYPES: BEGIN OF ty_transport,
           trkorr           TYPE trkorr,
           trfunction       TYPE trfunction,
           trstatus         TYPE trstatus,
           as4user          TYPE as4user,
           as4date          TYPE as4date,
           as4time          TYPE as4time,
           tarsystem        TYPE tr_target,
           korrdev          TYPE e070-korrdev,
           strkorr          TYPE trkorr,
           as4text          TYPE as4text,
           object_count     TYPE i,
           task_count       TYPE i,
         END OF ty_transport.

  DATA: lt_transports TYPE STANDARD TABLE OF ty_transport,
        ls_transport  TYPE ty_transport,
        lv_json       TYPE string,
        lv_where      TYPE string,
        lv_first      TYPE abap_bool VALUE abap_true,
        lv_max        TYPE i.

  CLEAR: ev_success, ev_message, ev_total_found, ev_results_json.

  " Validate max results
  lv_max = COND #( WHEN iv_max_results < 1 THEN 100
                   WHEN iv_max_results > 1000 THEN 1000
                   ELSE iv_max_results ).

  " Build dynamic WHERE clause
  " Note: At least one filter should be provided for performance
  IF iv_description IS INITIAL AND
     iv_user IS INITIAL AND
     iv_transport_type IS INITIAL AND
     iv_status IS INITIAL AND
     iv_target_system IS INITIAL AND
     iv_date_from IS INITIAL AND
     iv_date_to IS INITIAL.
    ev_success = ''.
    ev_message = 'At least one search criterion is required'.
    RETURN.
  ENDIF.

  " Query with JOINs for description and counts
  " Using ABAP 7.5+ syntax for efficiency
  SELECT e070~trkorr,
         e070~trfunction,
         e070~trstatus,
         e070~as4user,
         e070~as4date,
         e070~as4time,
         e070~tarsystem,
         e070~korrdev,
         e070~strkorr,
         e07t~as4text,
         COUNT( DISTINCT e071~obj_name ) AS object_count,
         COUNT( DISTINCT tasks~trkorr ) AS task_count
    FROM e070
    INNER JOIN e07t
      ON e07t~trkorr = e070~trkorr
     AND e07t~langu = @sy-langu
    LEFT OUTER JOIN e071
      ON e071~trkorr = e070~trkorr
    LEFT OUTER JOIN e070 AS tasks
      ON tasks~strkorr = e070~trkorr
     AND tasks~trfunction = 'S'
    WHERE ( @iv_description IS INITIAL OR e07t~as4text LIKE @iv_description )
      AND ( @iv_user IS INITIAL OR e070~as4user LIKE @iv_user )
      AND ( @iv_transport_type IS INITIAL OR e070~trfunction = @iv_transport_type )
      AND ( @iv_status IS INITIAL OR e070~trstatus = @iv_status )
      AND ( @iv_target_system IS INITIAL OR e070~tarsystem = @iv_target_system )
      AND ( @iv_date_from IS INITIAL OR e070~as4date >= @iv_date_from )
      AND ( @iv_date_to IS INITIAL OR e070~as4date <= @iv_date_to )
      AND e070~trfunction <> 'S'  " Exclude tasks (only main OTs)
    GROUP BY e070~trkorr, e070~trfunction, e070~trstatus,
             e070~as4user, e070~as4date, e070~as4time,
             e070~tarsystem, e070~korrdev, e070~strkorr,
             e07t~as4text
    ORDER BY e070~as4date DESCENDING, e070~as4time DESCENDING
    INTO CORRESPONDING FIELDS OF TABLE @lt_transports
    UP TO @lv_max ROWS.

  IF sy-subrc <> 0 OR lt_transports IS INITIAL.
    ev_success = 'X'.
    ev_message = 'No transports found matching criteria'.
    ev_total_found = 0.
    ev_results_json = '{"success":true,"totalFound":0,"transports":[]}'.
    RETURN.
  ENDIF.

  ev_total_found = lines( lt_transports ).

  " Build JSON response
  lv_json = '{"success":true,' &&
            |"totalFound":{ ev_total_found },| &&
            '"transports":['.

  LOOP AT lt_transports INTO ls_transport.
    IF lv_first = abap_false.
      lv_json = lv_json && ','.
    ENDIF.

    " Map type description
    DATA(lv_type_desc) = COND string(
      WHEN ls_transport-trfunction = 'K' THEN 'Workbench'
      WHEN ls_transport-trfunction = 'T' THEN 'Transport of Copies'
      WHEN ls_transport-trfunction = 'W' THEN 'Customizing Request'
      WHEN ls_transport-trfunction = 'C' THEN 'Customizing'
      ELSE ls_transport-trfunction ).

    " Map status description
    DATA(lv_status_desc) = COND string(
      WHEN ls_transport-trstatus = 'D' THEN 'Modifiable'
      WHEN ls_transport-trstatus = 'R' THEN 'Released'
      WHEN ls_transport-trstatus = 'L' THEN 'Protected'
      WHEN ls_transport-trstatus = 'N' THEN 'Modifiable (Protected)'
      WHEN ls_transport-trstatus = 'O' THEN 'Released (With Import Protection)'
      ELSE ls_transport-trstatus ).

    " Format date/time
    DATA(lv_date) = |{ ls_transport-as4date+0(4) }-{ ls_transport-as4date+4(2) }-{ ls_transport-as4date+6(2) }|.
    DATA(lv_time) = |{ ls_transport-as4time+0(2) }:{ ls_transport-as4time+2(2) }:{ ls_transport-as4time+4(2) }|.

    " Handle parent transport
    DATA(lv_parent) = COND string(
      WHEN ls_transport-strkorr IS NOT INITIAL THEN |"{ ls_transport-strkorr }"|
      ELSE 'null' ).

    " Build JSON object
    lv_json = lv_json &&
              '{' &&
              |"transport_number":"{ ls_transport-trkorr }",| &&
              |"description":"{ escape( val = ls_transport-as4text format = cl_abap_format=>e_json_string ) }",| &&
              |"transport_type":"{ ls_transport-trfunction }",| &&
              |"transport_type_desc":"{ lv_type_desc }",| &&
              |"status":"{ ls_transport-trstatus }",| &&
              |"status_desc":"{ lv_status_desc }",| &&
              |"owner":"{ ls_transport-as4user }",| &&
              |"created_date":"{ lv_date }",| &&
              |"created_time":"{ lv_time }",| &&
              |"target_system":"{ ls_transport-tarsystem }",| &&
              |"category":"{ ls_transport-korrdev }",| &&
              |"parent_transport":{ lv_parent },| &&
              |"object_count":{ ls_transport-object_count },| &&
              |"task_count":{ ls_transport-task_count }| &&
              '}'.

    lv_first = abap_false.
  ENDLOOP.

  lv_json = lv_json && ']}'.

  ev_success = 'X'.
  ev_results_json = lv_json.

ENDFUNCTION.
```

#### Consideraciones de Performance

1. **Índices existentes en E070**:
   - Primary: TRKORR
   - Secondary: AS4USER, TRSTATUS, STRKORR

2. **Filtro obligatorio**: Al menos un criterio requerido para evitar full table scan

3. **Límite de resultados**: Máximo 1000 para evitar timeouts

4. **Ordenamiento**: Por fecha descendente (más recientes primero)

### 2. Crear Java Service `TransportSearchService.java`

```java
package com.crystal.mcp.sapserver.service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportSearchService {

    private final JCoConfiguration jCoConfiguration;

    public TransportSearchResult searchTransports(
            String description,
            String user,
            String transportType,
            String status,
            String targetSystem,
            String dateFrom,
            String dateTo,
            Integer maxResults
    ) {
        // Call Z_CX_SEARCH_TRANSPORTS FM
        // Parse JSON response
        // Return structured result
    }
}
```

### 3. Crear MCP Tool `search_transports`

```java
@McpTool(
    description = "Search transport requests by flexible criteria. " +
        "Supports searching by description pattern (LIKE), user, type, status, " +
        "target system, and date range. At least one criterion required. " +
        "Returns transports with object/task counts. " +
        "Token cost: ~1,000-3,000 tokens (depends on results). " +
        "Examples: description='%PSR01%', status='D', user='L_ABAPS_ITA'"
)
public TransportSearchResult search_transports(
    @McpToolParam(description = "Description pattern (LIKE search). " +
        "Use % for wildcards. Examples: '%PSR01%', '%INVOICE%', 'FI-%'")
    String description,

    @McpToolParam(description = "User filter (owner). " +
        "Exact match or pattern with %. Examples: 'L_ABAPS_ITA', 'L_ABAPS%'")
    String user,

    @McpToolParam(description = "Transport type: 'K' (Workbench), 'W' (Customizing), " +
        "'T' (Transport of Copies). Leave empty for all types.")
    String transportType,

    @McpToolParam(description = "Status filter: 'D' (Modifiable), 'R' (Released), " +
        "'L' (Protected). Leave empty for all statuses.")
    String status,

    @McpToolParam(description = "Target system filter. Examples: 'S4Q', 'S4P'. " +
        "Leave empty for all systems.")
    String targetSystem,

    @McpToolParam(description = "Date from (YYYY-MM-DD). Filter by creation date. " +
        "Example: '2025-01-01'")
    String dateFrom,

    @McpToolParam(description = "Date to (YYYY-MM-DD). Filter by creation date. " +
        "Example: '2025-12-31'")
    String dateTo,

    @McpToolParam(description = "Maximum results (1-1000). Default: 100")
    Integer maxResults
)
```

## Casos de Uso

### Ejemplo 1: Buscar OTs por descripción
```
User: "Busca las OTs que contengan PSR01"
Claude: search_transports(description='%PSR01%')
```

### Ejemplo 2: OTs modificables de un usuario
```
User: "Muéstrame las OTs abiertas de L_ABAPS_ITA"
Claude: search_transports(user='L_ABAPS_ITA', status='D')
```

### Ejemplo 3: OTs liberadas en un rango de fechas
```
User: "OTs liberadas en diciembre 2025"
Claude: search_transports(status='R', dateFrom='2025-12-01', dateTo='2025-12-31')
```

### Ejemplo 4: Búsqueda combinada
```
User: "Busca OTs de FI que estén modificables y sean de tipo Workbench"
Claude: search_transports(description='%FI%', status='D', transportType='K')
```

## JSON de Respuesta Esperado

```json
{
  "success": true,
  "totalFound": 3,
  "transports": [
    {
      "transport_number": "CADK911197",
      "description": "PS WB R001 R002 R006 Carga def proy. pep mga V001SL",
      "transport_type": "K",
      "transport_type_desc": "Workbench",
      "status": "D",
      "status_desc": "Modifiable",
      "owner": "L_ABAPS_ITA",
      "created_date": "2025-01-15",
      "created_time": "10:30:45",
      "target_system": "S4Q",
      "category": "SYST",
      "parent_transport": null,
      "object_count": 25,
      "task_count": 2
    }
  ]
}
```

## Archivos a Crear/Modificar

### SAP (Sistema GDC)
| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `Z_CX_SEARCH_TRANSPORTS` | Crear | FM RFC en grupo ZCXFG_1 |

### Java MCP Server
| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `TransportSearchResult.java` | Crear | DTO para resultado |
| `TransportSearchService.java` | Crear | Service para llamar FM |
| `TransportTools.java` | Modificar | Agregar tool search_transports |
| `ManualTransportSearchTest.java` | Crear | Test manual |

## Plan de Implementación

### Fase 1: FM en SAP
1. Crear FM `Z_CX_SEARCH_TRANSPORTS` en grupo ZCXFG_1
2. Configurar como RFC-enabled
3. Probar con SE37

### Fase 2: Java Service
1. Crear `TransportSearchResult.java`
2. Crear `TransportSearchService.java`
3. Implementar parsing JSON

### Fase 3: MCP Tool
1. Agregar método `search_transports` en `TransportTools.java`
2. Crear test manual
3. Probar end-to-end

## Criterios de Aceptación

- [ ] FM creado y funcional en SAP GDC
- [ ] Búsqueda por descripción con wildcards funciona
- [ ] Filtros combinados funcionan correctamente
- [ ] Conteo de objetos y tasks es correcto
- [ ] Límite de resultados respetado
- [ ] Test manual exitoso
- [ ] Tool disponible en Claude Code

## Estimación

| Fase | Esfuerzo |
|------|----------|
| FM en SAP | 1 hora |
| Java Service + DTO | 1 hora |
| MCP Tool + Tests | 30 min |
| **Total** | **2.5 horas** |

---

**Autor**: Claude Code
**Fecha**: 2025-12-04
**Estado**: Pendiente Aprobación

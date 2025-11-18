# Function Module: Z_CX_GET_TRANSPORT_OBJECTS

## Propósito
Obtener objetos de una orden de transporte (OT) incluyendo todas sus tareas asociadas.

## Ubicación
- **Function Group**: ZGFCX_1
- **Sistema**: GIRAL (GDC)

## Firma del Function Module

```abap
FUNCTION Z_CX_GET_TRANSPORT_OBJECTS
  IMPORTING
    VALUE(IV_TRANSPORT_NUMBER) TYPE TRKORR
    VALUE(IV_TASK_NUMBER) TYPE TRKORR OPTIONAL
  EXPORTING
    VALUE(EV_SUCCESS) TYPE ABAP_BOOL
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_TRANSPORT_JSON) TYPE STRING
  EXCEPTIONS
    TRANSPORT_NOT_FOUND
    QUERY_ERROR.
```

## Parámetros

### IMPORTING

#### IV_TRANSPORT_NUMBER (obligatorio)
- **Tipo**: TRKORR
- **Descripción**: Número de la orden de transporte (OT o Task)
- **Ejemplos**:
  - `'CADK911088'` - Orden principal
  - `'CADK911222'` - Tarea específica

#### IV_TASK_NUMBER (opcional)
- **Tipo**: TRKORR
- **Descripción**: Filtrar solo objetos de esta tarea (cuando se consulta OT principal)
- **Default**: Vacío (retorna todos los objetos)

### EXPORTING

#### EV_SUCCESS
- **Tipo**: ABAP_BOOL
- **Descripción**: Indica si la operación fue exitosa
- **Valores**: `ABAP_TRUE` / `ABAP_FALSE`

#### EV_MESSAGE
- **Tipo**: STRING
- **Descripción**: Mensaje descriptivo del resultado o error

#### EV_TRANSPORT_JSON
- **Tipo**: STRING
- **Descripción**: JSON con estructura completa del transporte

### EXCEPTIONS

#### TRANSPORT_NOT_FOUND
Se lanza cuando el número de transporte no existe en E070

#### QUERY_ERROR
Se lanza cuando falla alguna consulta a las tablas E070/E071

## Estructura del JSON de Salida

```json
{
  "success": true,
  "transport_number": "CADK911088",
  "metadata": {
    "transport_number": "CADK911088",
    "transport_type": "K",
    "transport_type_desc": "Workbench",
    "status": "D",
    "status_desc": "Modifiable",
    "owner": "SEBLONDO",
    "created_date": "2025-07-15",
    "created_time": "10:30:45",
    "target_system": "S4Q",
    "category": "SYST",
    "description": "DV-FI-AAC002 Reporte de conciliación",
    "parent_transport": ""
  },
  "objects": [
    {
      "trkorr": "CADK911222",
      "object_type": "CLAS",
      "object_name": "ZCLFI_AAC002_PROCESSOR",
      "pgmid": "R3TR",
      "lock_flag": "X",
      "lock_type": "M",
      "function": "K",
      "tab_key": ""
    },
    {
      "trkorr": "CADK911222",
      "object_type": "PROG",
      "object_name": "ZREP_AAC002",
      "pgmid": "R3TR",
      "lock_flag": "",
      "lock_type": "",
      "function": "K",
      "tab_key": ""
    }
  ],
  "total_objects": 19,
  "tasks": [
    {
      "task_number": "CADK911222",
      "owner": "SEBLONDO",
      "created_date": "2025-07-15",
      "created_time": "10:31:00",
      "status": "D",
      "status_desc": "Modifiable",
      "description": "Task para objetos ABAP",
      "object_count": 15
    },
    {
      "task_number": "CADK911223",
      "owner": "JMVALENC",
      "created_date": "2025-07-15",
      "created_time": "11:20:00",
      "status": "D",
      "status_desc": "Modifiable",
      "description": "Task para customizing",
      "object_count": 4
    }
  ]
}
```

## Lógica de Implementación

### 1. Validación de Entrada
```abap
IF iv_transport_number IS INITIAL.
  RAISE query_error.
ENDIF.
```

### 2. Consulta E070 - Metadata del Transporte
```abap
SELECT SINGLE trkorr, trfunction, trstatus, as4user, as4date, as4time,
              tarsystem, korrdev, strkorr, as4text
  FROM e070
  INTO @DATA(ls_e070)
  WHERE trkorr = @iv_transport_number.

IF sy-subrc <> 0.
  RAISE transport_not_found.
ENDIF.
```

### 3. Construcción de Metadata
- Mapear TRFUNCTION a descripción (K=Workbench, S=Task, T=Transport of Copies)
- Mapear TRSTATUS a descripción (D=Modifiable, R=Released, L=Protected)
- Formatear fecha: YYYYMMDD → YYYY-MM-DD
- Formatear hora: HHMMSS → HH:MM:SS

### 4. Consulta E071 - Objetos del Transporte
```abap
SELECT trkorr, pgmid, object, obj_name, lockflag, gennum, tabkey
  FROM e071
  INTO TABLE @DATA(lt_e071)
  WHERE trkorr = @iv_transport_number.
```

### 5. Detección y Procesamiento de Tareas (solo si TRFUNCTION = 'K')
```abap
IF ls_e070-trfunction = 'K'.
  " Buscar todas las tareas asociadas
  SELECT trkorr, as4user, as4date, as4time, trstatus, as4text
    FROM e070
    INTO TABLE @DATA(lt_tasks)
    WHERE strkorr = @iv_transport_number
      AND trfunction = 'S'.

  " Para cada tarea, obtener sus objetos
  LOOP AT lt_tasks INTO DATA(ls_task).
    SELECT trkorr, pgmid, object, obj_name, lockflag, gennum, tabkey
      FROM e071
      APPENDING TABLE @lt_e071
      WHERE trkorr = @ls_task-trkorr.
  ENDLOOP.
ENDIF.
```

### 6. Filtro por Task (si IV_TASK_NUMBER especificado)
```abap
IF iv_task_number IS NOT INITIAL.
  DELETE lt_e071 WHERE trkorr <> iv_task_number.
ENDIF.
```

### 7. Construcción de JSON
Usar `/UI2/CL_JSON` o serialización manual para construir el JSON de salida.

## Mapeo de Tipos

### TRFUNCTION (Transport Function)
| Código | Descripción |
|--------|-------------|
| K | Workbench |
| S | Task |
| T | Transport of Copies |
| W | Workbench Request |
| C | Customizing |

### TRSTATUS (Transport Status)
| Código | Descripción |
|--------|-------------|
| D | Modifiable |
| L | Protected |
| R | Released |
| N | Modifiable (Protected) |
| O | Released (With Import Protection) |

### PGMID (Program ID)
| Código | Descripción |
|--------|-------------|
| R3TR | Repository objects |
| LIMU | Subobjects |
| LANG | Language objects |

### OBJECT (Object Type)
| Código | Descripción |
|--------|-------------|
| CLAS | Class |
| PROG | Program |
| FUGR | Function Group |
| TABL | Table |
| DTEL | Data Element |
| DOMA | Domain |
| METH | Method |
| CLSD | Class Definition |

## Casos de Uso

### Caso 1: Consultar OT Principal con Todas las Tareas
```abap
CALL FUNCTION 'Z_CX_GET_TRANSPORT_OBJECTS'
  EXPORTING
    iv_transport_number = 'CADK911088'
  IMPORTING
    ev_success          = DATA(lv_success)
    ev_message          = DATA(lv_message)
    ev_transport_json   = DATA(lv_json)
  EXCEPTIONS
    transport_not_found = 1
    query_error         = 2
    OTHERS              = 3.
```

### Caso 2: Consultar Solo una Tarea Específica
```abap
CALL FUNCTION 'Z_CX_GET_TRANSPORT_OBJECTS'
  EXPORTING
    iv_transport_number = 'CADK911088'
    iv_task_number      = 'CADK911222'
  IMPORTING
    ev_success          = DATA(lv_success)
    ev_message          = DATA(lv_message)
    ev_transport_json   = DATA(lv_json)
  EXCEPTIONS
    transport_not_found = 1
    query_error         = 2
    OTHERS              = 3.
```

### Caso 3: Consultar Tarea Directamente
```abap
CALL FUNCTION 'Z_CX_GET_TRANSPORT_OBJECTS'
  EXPORTING
    iv_transport_number = 'CADK911222'  " Número de tarea
  IMPORTING
    ev_success          = DATA(lv_success)
    ev_message          = DATA(lv_message)
    ev_transport_json   = DATA(lv_json)
  EXCEPTIONS
    transport_not_found = 1
    query_error         = 2
    OTHERS              = 3.
```

## Ventajas de este Diseño

1. **Simplicidad**: Un solo FM para todos los casos (OT, Task, filtrado)
2. **Flexibilidad**: Parámetro `IV_TASK_NUMBER` opcional para filtrado
3. **Eficiencia**: Consultas optimizadas a E070/E071
4. **Estructura Estándar**: JSON compatible con modelo Java `TransportObjectsResult`
5. **Manejo de Errores**: Excepciones claras para casos específicos
6. **Metadata Completa**: Incluye toda la información relevante (owner, dates, status, description)
7. **Jerarquía Clara**: Diferencia entre OT principal y tareas

## Siguiente Paso
Implementar el FM en SAP usando SE37 o Eclipse ADT con esta firma exacta.

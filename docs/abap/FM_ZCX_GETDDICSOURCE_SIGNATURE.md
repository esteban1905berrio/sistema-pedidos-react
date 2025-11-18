# Function Module: ZCX_GETDDICSOURCE - Firma de Parámetros

## Información General
- **Function Module**: ZCX_GETDDICSOURCE
- **Function Group**: ZGFCX_1
- **Package**: $TMP (local object)
- **Descripción**: Get DDIC object structure from DD03L

## Estado Actual
✅ Function Group creado: ZGFCX_1
✅ Function Module creado: ZCX_GETDDICSOURCE
✅ Lógica implementada (versión inactiva)
⚠️ **PENDIENTE**: Configurar firma de parámetros en SE37

---

## Configuración de Firma en SE37

### 1. Acceder al Function Module
```
Transacción: SE37
Function Module: ZCX_GETDDICSOURCE
```

### 2. Pestaña "Import"

| Parameter Name | Typing Method | Associated Type | Optional | Pass Value | Short Text |
|----------------|---------------|-----------------|----------|------------|------------|
| OBJECT_NAME    | Type          | TABNAME         | -        | ✓          | Table/Structure/View name |

### 3. Pestaña "Export"

| Parameter Name  | Typing Method | Associated Type | Optional | Pass Value | Short Text |
|-----------------|---------------|-----------------|----------|------------|------------|
| OBJECT_TYPE     | Type          | CHAR10          | -        | ✓          | Object type (TABLE/STRUCTURE/VIEW) |
| OBJECT_STATUS   | Type          | CHAR10          | -        | ✓          | Object status (ACTIVE/INACTIVE) |
| FIELDS_JSON     | Type          | STRING          | -        | ✓          | Field metadata in JSON format |

### 4. Pestaña "Exceptions"

| Exception          | Short Text |
|--------------------|------------|
| OBJECT_NOT_FOUND   | Object does not exist in DD02L |
| INVALID_OBJECT_TYPE| No fields found in DD03L |

---

## Estructura de Salida JSON (FIELDS_JSON)

```json
[
  {
    "fieldname": "MANDT",
    "position": 1,
    "rollname": "MANDT",
    "mandatory": "X",
    "checktable": "T000",
    "adminfield": "0",
    "inttype": "C",
    "intlen": 3,
    "datatype": "CLNT",
    "keyflag": "X",
    "reffield": ""
  },
  {
    "fieldname": "MATNR",
    "position": 2,
    "rollname": "MATNR",
    "mandatory": "X",
    "checktable": "",
    "adminfield": "0",
    "inttype": "C",
    "intlen": 18,
    "datatype": "CHAR",
    "keyflag": "X",
    "reffield": ""
  }
]
```

### Campos Incluidos en JSON
- `fieldname`: Nombre del campo
- `position`: Posición del campo en la tabla
- `rollname`: Data element (tipo de dato)
- `mandatory`: 'X' si es obligatorio
- `checktable`: Tabla de verificación (foreign key)
- `adminfield`: Campo administrativo (0 = normal)
- `inttype`: Tipo interno (C=char, N=numeric, D=date, etc.)
- `intlen`: Longitud interna
- `datatype`: Tipo de dato ABAP
- `keyflag`: 'X' si es campo clave
- `reffield`: Campo de referencia

---

## Pasos para Completar la Configuración

### Paso 1: Abrir SE37
```
1. Ejecutar transacción SE37
2. Ingresar: ZCX_GETDDICSOURCE
3. Click en "Display" o "Change"
```

### Paso 2: Configurar Parámetros Import
```
1. Click en pestaña "Import"
2. Click en "New Entries" o editar línea existente
3. Ingresar:
   - Parameter name: OBJECT_NAME
   - Typing: Type
   - Associated Type: TABNAME
   - Check: Pass Value
```

### Paso 3: Configurar Parámetros Export
```
1. Click en pestaña "Export"
2. Agregar 3 parámetros:

   a) OBJECT_TYPE
      - Typing: Type
      - Associated Type: CHAR10
      - Pass Value: ✓

   b) OBJECT_STATUS
      - Typing: Type
      - Associated Type: CHAR10
      - Pass Value: ✓

   c) FIELDS_JSON
      - Typing: Type
      - Associated Type: STRING
      - Pass Value: ✓
```

### Paso 4: Configurar Excepciones
```
1. Click en pestaña "Exceptions"
2. Agregar:
   - OBJECT_NOT_FOUND
   - INVALID_OBJECT_TYPE
```

### Paso 5: Guardar y Activar
```
1. Click en "Save" (Ctrl+S)
2. Click en "Activate" (Ctrl+F3)
3. Verificar que no haya errores de sintaxis
```

---

## Verificación

### Test del Function Module (SE37)
```
1. En SE37, con ZCX_GETDDICSOURCE abierto
2. Click en F8 (Test/Execute)
3. Ingresar valores de prueba:
   - OBJECT_NAME: MARA (tabla de materiales)
4. Ejecutar
5. Verificar:
   - OBJECT_TYPE = 'TABLE'
   - OBJECT_STATUS = 'ACTIVE'
   - FIELDS_JSON contiene JSON válido con campos
```

### Ejemplo de Test con Tabla MARA
```abap
IMPORTING:
  OBJECT_NAME = 'MARA'

EXPECTED EXPORTING:
  OBJECT_TYPE = 'TABLE'
  OBJECT_STATUS = 'ACTIVE'
  FIELDS_JSON = '[{"fieldname":"MANDT","position":1,...},...]'

EXCEPTIONS:
  None (should succeed)
```

### Ejemplo de Test con Tabla Inexistente
```abap
IMPORTING:
  OBJECT_NAME = 'ZZZZZ_NO_EXISTE'

EXPECTED EXCEPTIONS:
  OBJECT_NOT_FOUND
```

---

## Integración con Java (Próximo Paso)

Una vez activado el FM, se implementará:

1. **ClassService.java**: Método `getDdicSource(String objectName)`
2. **ClassTools.java**: Tool MCP `get_ddic_source`
3. **Test**: Integración con tabla MARA, T001 (company), DD03L

---

## Referencias

- DD02L: SAP Table Definitions
- DD03L: Table Field Definitions
- TABNAME: Standard SAP data type for table names
- STRING: ABAP built-in type for long strings

---

**Status**: ⚠️ Pendiente configuración manual de firma en SE37
**Next Step**: Activar FM y proceder con integración Java

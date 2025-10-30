# PR: Mejorar herramienta get_transport_objects

## Contexto

Como desarrollador ABAP, necesito una herramienta confiable para obtener el listado completo de objetos contenidos en órdenes de transporte (OT) y tareas de desarrollo. La implementación actual de `get_transport_objects` presenta limitaciones que afectan la generación de documentación técnica y el análisis de transportes.

## Problema Actual

### Implementación existente
La función `get_transport_objects` en `app/services/transport_service.py` (línea 261) actualmente:

1. **Depende de `get_transport_request`:**
   ```python
   transport_data = self.get_transport_request(transport_number)
   objects = transport_data.get('objects', [])
   ```

2. **Problemas identificados:**
   - ❌ La respuesta de `get_transport_request` puede exceder el límite de tokens (25,000)
   - ❌ No distingue correctamente entre órdenes de transporte (TRKORR) y tareas (subtasks)
   - ❌ Devuelve resultados inconsistentes cuando se consultan tareas específicas
   - ❌ No accede directamente a las tablas SAP que contienen la información real

### Evidencia del problema

Durante la prueba con OT CADK911088:
- **Consulta a través de get_transport_objects:** Devolvió solo 1 objeto (incorrecto)
- **Consulta directa a tabla E071:** Devolvió 33 objetos correctos distribuidos en 2 tareas

```python
# Actual (incorrecto):
get_transport_objects("CADK911222")  # Devuelve 1 objeto (METH)

# Esperado (correcto):
SELECT * FROM E071 WHERE TRKORR = 'CADK911222'  # Devuelve 19 objetos
```

## Solución Propuesta

### Reemplazar implementación actual por consulta directa a tablas SAP

**Tablas SAP de transporte:**

1. **E070** - Cabecera de órdenes de transporte
   - Contiene metadatos: propietario, fecha, estado, tipo, sistema destino
   - Permite identificar si es OT principal (K) o tarea (S)

2. **E071** - Objetos de transporte
   - Contiene el listado completo de objetos por TRKORR
   - Campos clave: TRKORR, PGMID, OBJECT, OBJ_NAME, AS4POS, LOCKFLAG

3. **E070A** (opcional) - Atributos adicionales de transporte
   - Información extendida sobre la OT

### Nueva implementación

```python
def get_transport_objects(
    self,
    transport_number: str,
    task_number: Optional[str] = None,
    include_metadata: bool = True
) -> Dict[str, Any]:
    """
    Get objects from a transport request by querying table E071 directly.

    Args:
        transport_number: Transport request number (OT principal o tarea)
        task_number: Optional task number to filter (cuando transport_number es OT principal)
        include_metadata: Include E070 metadata (owner, date, status, type)

    Returns:
        {
            "transport_number": "CADK911088",
            "transport_type": "K",  # K=Workbench, S=Task
            "owner": "L_ABAPS_ITA",
            "status": "D",
            "created_date": "20251029",
            "created_time": "152248",
            "target_system": "/QASALL/",
            "parent_transport": "",  # Solo para tareas (S)
            "total_objects": 33,
            "objects": [
                {
                    "trkorr": "CADK911222",
                    "as4pos": "000001",
                    "pgmid": "R3TR",
                    "object": "CMOD",
                    "obj_name": "ZCNEX006",
                    "objfunc": "",
                    "lockflag": "X",
                    "lang": ""
                },
                ...
            ]
        }
    """
    # 1. Consultar E070 para metadata
    # 2. Consultar E071 para objetos
    # 3. Si task_number, filtrar objetos
    # 4. Retornar estructura completa
```

## Criterios de Aceptación

### Funcionales

1. ✅ **Consulta directa a E071:** La función debe consultar la tabla E071 mediante `get_table_contents`
2. ✅ **Soporte para OTs y tareas:** Debe funcionar tanto para órdenes principales (K) como tareas (S)
3. ✅ **Filtrado por tarea:** Cuando `transport_number` es OT principal, debe permitir filtrar por `task_number`
4. ✅ **Metadata completa:** Debe incluir información de E070 (propietario, fecha, estado, tipo)
5. ✅ **Listado completo:** Debe retornar TODOS los objetos de la tabla E071 (no limitado por tokens ADT)

### Técnicos

1. ✅ **Ubicación:** Modificar `app/services/transport_service.py:261` (método `get_transport_objects`)
2. ✅ **Usar QueryService:** Utilizar `QueryService` o `get_table_contents` para consultas SQL
3. ✅ **Manejo de errores:** Capturar errores de consulta y retornar mensajes claros
4. ✅ **Logging:** Registrar consultas SQL ejecutadas y número de objetos encontrados
5. ✅ **Backward compatibility:** Mantener firma del método compatible con tool MCP existente
6. ✅ **Performance:** Limitar resultados a 1000 objetos (parametrizable)

### Testing

1. ✅ **Test con OT principal vacía:** CADK911088 (0 objetos directos, solo en tareas)
2. ✅ **Test con tarea con objetos:** CADK911222 (19 objetos)
3. ✅ **Test con tarea con objetos:** CADK911089 (14 objetos)
4. ✅ **Test con filtro de tarea:** `get_transport_objects("CADK911088", task_number="CADK911222")`
5. ✅ **Test con OT inexistente:** Debe retornar error claro
6. ✅ **Validar metadata E070:** Verificar que se incluye propietario, fecha, estado

## Estructura de Respuesta Detallada

```json
{
  "success": true,
  "transport_number": "CADK911088",
  "metadata": {
    "transport_type": "K",
    "transport_type_desc": "Workbench",
    "status": "D",
    "status_desc": "Modifiable",
    "owner": "L_ABAPS_ITA",
    "created_date": "2025-10-29",
    "created_time": "15:22:48",
    "target_system": "/QASALL/",
    "category": "SYST",
    "parent_transport": null
  },
  "objects": [
    {
      "trkorr": "CADK911222",
      "as4pos": "000001",
      "pgmid": "R3TR",
      "object": "CMOD",
      "obj_name": "ZCNEX006",
      "objfunc": "",
      "lockflag": "X",
      "gennum": "",
      "lang": "",
      "activity": ""
    }
  ],
  "total_objects": 33,
  "tasks": [
    {
      "task_number": "CADK911222",
      "owner": "L_ABAPS_ITA",
      "created_date": "2025-10-28",
      "object_count": 19
    },
    {
      "task_number": "CADK911089",
      "owner": "L_ABAPS2_ITA",
      "created_date": "2025-10-21",
      "object_count": 14
    }
  ]
}
```

## Casos de Uso

### Caso 1: Obtener todos los objetos de una OT principal
```python
result = service.get_transport_objects("CADK911088")
# Retorna: 33 objetos de todas las tareas
```

### Caso 2: Obtener objetos de una tarea específica
```python
result = service.get_transport_objects("CADK911222")
# Retorna: 19 objetos de la tarea CADK911222
```

### Caso 3: Filtrar objetos de una tarea dentro de una OT
```python
result = service.get_transport_objects("CADK911088", task_number="CADK911089")
# Retorna: 14 objetos filtrados de la tarea CADK911089
```

### Caso 4: Generar documentación técnica
```python
# Escenario real: Generar reporte de OT con todas las tareas y objetos
result = service.get_transport_objects("CADK911088", include_metadata=True)

print(f"OT: {result['transport_number']}")
print(f"Propietario: {result['metadata']['owner']}")
print(f"Total objetos: {result['total_objects']}")

for task in result['tasks']:
    print(f"  Tarea: {task['task_number']} ({task['object_count']} objetos)")
```

## Beneficios

1. **Precisión:** Acceso directo a la fuente de verdad (tabla E071)
2. **Performance:** No limitado por el límite de tokens de ADT (25,000)
3. **Completitud:** Retorna TODOS los objetos sin paginación compleja
4. **Trazabilidad:** Información completa de metadata (propietario, fechas, estado)
5. **Documentación:** Base sólida para generación automática de especificaciones técnicas

## Notas Técnicas

### Queries SQL a implementar

**Query 1: Metadata de transporte (E070)**
```sql
SELECT TRKORR, TRFUNCTION, TRSTATUS, TARSYSTEM, KORRDEV,
       AS4USER, AS4DATE, AS4TIME, STRKORR
FROM E070
WHERE TRKORR = 'CADK911088'
```

**Query 2: Objetos del transporte (E071)**
```sql
SELECT TRKORR, AS4POS, PGMID, OBJECT, OBJ_NAME,
       OBJFUNC, LOCKFLAG, GENNUM, LANG, ACTIVITY
FROM E071
WHERE TRKORR = 'CADK911088'
ORDER BY AS4POS
```

**Query 3: Objetos filtrados por tarea**
```sql
SELECT * FROM E071
WHERE TRKORR IN (
  SELECT TRKORR FROM E070
  WHERE STRKORR = 'CADK911088'
    AND TRKORR = 'CADK911222'
)
ORDER BY AS4POS
```

### Mapeo de campos E070

| Campo SAP | Descripción | Valores |
|-----------|-------------|---------|
| TRFUNCTION | Tipo de transporte | K=Workbench, S=Task, T=Transport of Copies |
| TRSTATUS | Estado | D=Modifiable, R=Released, L=Protected |
| KORRDEV | Categoría desarrollo | SYST, CUST, etc. |
| STRKORR | OT padre | Vacío para OT principales, contiene OT padre para tareas |

### Mapeo de campos E071

| Campo SAP | Descripción | Ejemplo |
|-----------|-------------|---------|
| PGMID | Program ID | R3TR, LIMU |
| OBJECT | Tipo de objeto | CMOD, PROG, TABL, DTEL, CLAS, FUGR |
| OBJ_NAME | Nombre del objeto | ZCNEX006, ZXCN1U21 |
| LOCKFLAG | Estado de bloqueo | X=Bloqueado, espacio=No bloqueado |
| AS4POS | Posición secuencial | 000001, 000002, ... |

## Referencias

- Documentación SAP: Tablas CTS (Change and Transport System)
- ADT API Limitation: 25,000 tokens por respuesta
- Prueba real: OT CADK911088 con tareas CADK911222 y CADK911089

## Impacto

### Archivos a modificar:
1. `app/services/transport_service.py` - Método `get_transport_objects` (línea 261)
2. `app/mcp/tools/transport_tools.py` - Tool wrapper (línea 175) - Sin cambios necesarios
3. `app/tests/test_transport_category.py` - Agregar tests nuevos

### Compatibilidad:
- ✅ No rompe la interfaz MCP existente
- ✅ Los tools que usan `get_transport_objects` seguirán funcionando
- ✅ Mejora la calidad de datos retornados

---

**Prioridad:** Alta
**Estimación:** 4-6 horas
**Asignado a:** Por definir
**Sprint:** Por definir

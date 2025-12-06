# PR: MCP Tool - modify_transport_request

## Estado: IMPLEMENTADO

**Fecha**: 2025-12-05
**Autor**: Crystal Development Team

---

## 1. Resumen

Implementar herramientas MCP para modificar órdenes de transporte SAP, incluyendo:
- Agregar objetos a tareas de transporte
- Modificar descripciones de OT
- Liberar tareas individuales
- Liberar OT completa (con todas sus tareas)

---

## 2. Requisitos Funcionales

### 2.1 Agregar Objetos a Transporte (add_objects_to_transport)

| Aspecto | Descripción |
|---------|-------------|
| **Entrada** | Número de transporte (OT), lista de objetos (JSON) |
| **Salida** | Resultado con cantidad de objetos agregados y tarea asignada |
| **Regla de negocio** | Se debe pasar el **NÚMERO DE LA OT PRINCIPAL**, no el de una tarea. SAP (`TR_REQUEST_CHOICE`) internamente asigna los objetos a la tarea apropiada. |
| **Auto-detección** | Si no se especifica PGMID/OBJECT, se detectan desde TADIR |

### 2.2 Modificar Descripción (modify_transport_description)

| Aspecto | Descripción |
|---------|-------------|
| **Entrada** | Número de transporte, nueva descripción |
| **Salida** | Resultado de la operación |
| **Validación** | Máximo 60 caracteres |

### 2.3 Liberar Tarea (release_task)

| Aspecto | Descripción |
|---------|-------------|
| **Entrada** | Número de tarea |
| **Salida** | Resultado de la liberación |
| **Validación** | La tarea debe estar en estado modificable (D) |

### 2.4 Liberar Transporte (release_transport)

| Aspecto | Descripción |
|---------|-------------|
| **Entrada** | Número de transporte, flag de confirmación |
| **Salida** | Lista de tareas liberadas o solicitud de confirmación |
| **Flujo** | 1) Sin confirmación: devuelve lista de tareas a liberar |
|          | 2) Con confirmación: libera todas las tareas y luego la OT |
| **Regla crítica** | REQUIERE confirmación del usuario antes de liberar |

---

## 3. Diseño Técnico

### 3.1 Componentes Java

```
src/main/java/com/crystal/mcp/sapserver/
├── model/
│   └── TransportModificationResult.java    # Record con factory methods
├── service/
│   └── TransportModificationService.java   # Lógica de negocio, llama al FM
└── tool/
    └── TransportModificationTools.java     # 4 herramientas MCP
```

### 3.2 Function Module ABAP

**Nombre**: `ZCX_MODIFY_TRANSPORT_REQUEST`
**Grupo de funciones**: `ZGFCX_1`

#### Firma del FM:

```abap
FUNCTION ZCX_MODIFY_TRANSPORT_REQUEST
  IMPORTING
    VALUE(IV_OPERATION) TYPE STRING
    VALUE(IV_TRANSPORT_NUMBER) TYPE TRKORR OPTIONAL
    VALUE(IV_TASK_NUMBER) TYPE TRKORR OPTIONAL
    VALUE(IV_DESCRIPTION) TYPE AS4TEXT OPTIONAL
    VALUE(IV_OBJECTS_JSON) TYPE STRING OPTIONAL
    VALUE(IV_CONFIRMED) TYPE CHAR1 OPTIONAL
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_TRANSPORT_NUMBER) TYPE TRKORR
    VALUE(EV_TASK_NUMBER) TYPE TRKORR
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_OBJECTS_ADDED) TYPE I
    VALUE(EV_TASKS_JSON) TYPE STRING.
```

#### Operaciones soportadas (IV_OPERATION):

| Operación | SAP FM Interno | Descripción |
|-----------|----------------|-------------|
| `ADD_OBJECTS` | `TR_REQUEST_CHOICE` | Agregar objetos a tarea |
| `MODIFY_DESCRIPTION` | `TRINT_MODIFY_COMM` | Cambiar descripción |
| `RELEASE_TASK` | `TR_RELEASE_REQUEST` | Liberar tarea |
| `GET_TASKS_FOR_RELEASE` | Query E070 | Obtener tareas modificables |
| `RELEASE_TRANSPORT` | `TR_RELEASE_REQUEST` (múltiple) | Liberar OT completa |

### 3.3 Formatos JSON

**Objetos (IV_OBJECTS_JSON):**
```json
[
  {"PGMID": "R3TR", "OBJECT": "PROG", "OBJ_NAME": "ZTEST"},
  {"OBJ_NAME": "ZCL_TEST"}  // Auto-detección desde TADIR
]
```

**Tareas (EV_TASKS_JSON):**
```json
["CADK911511", "CADK911512"]
```

---

## 4. Test Manual

**Archivo**: `src/test/java/.../manual/transport/ManualTransportModificationTest.java`

**Comando de ejecución**:
```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportModificationTest
```

**Tests disponibles** (descomentarlos según necesidad):
1. `testFunctionModuleAvailability()` - Verificar disponibilidad del FM
2. `testAddObjectsToTask()` - Agregar objetos a tarea
3. `testModifyDescription()` - Modificar descripción
4. `testReleaseTask()` - Liberar tarea
5. `testGetTasksForRelease()` - Obtener tareas para confirmación
6. `testReleaseTransport()` - Liberar OT completa

---

## 5. Checklist de Implementación

### Java
- [x] Modelo `TransportModificationResult.java`
- [x] Servicio `TransportModificationService.java`
- [x] Tools `TransportModificationTools.java`
- [x] Test manual `ManualTransportModificationTest.java`
- [x] Compilación exitosa

### ABAP (GDC)
- [x] Crear FM `ZCX_MODIFY_TRANSPORT_REQUEST` en grupo `ZGFCX_1` (RFC-enabled)
- [x] Implementar operación ADD_OBJECTS
- [x] Implementar operación MODIFY_DESCRIPTION
- [x] Implementar operación RELEASE_TASK
- [x] Implementar operación GET_TASKS_FOR_RELEASE
- [x] Implementar operación RELEASE_TRANSPORT
- [x] Activar FM

### Validación
- [ ] Test manual: FM availability
- [ ] Test manual: Add objects
- [ ] Test manual: Modify description
- [ ] Test manual: Release task
- [ ] Test manual: Release transport (con confirmación)

---

## 6. Dependencias

- FM existentes: `TR_REQUEST_CHOICE`, `TRINT_MODIFY_COMM`, `TR_RELEASE_REQUEST`
- Tablas: `E070`, `E071`, `TADIR`

---

## 7. Historial

| Fecha | Versión | Cambios |
|-------|---------|---------|
| 2025-12-05 | 1.0.0 | Implementación inicial (Java) |
| 2025-12-05 | 1.1.0 | FM ABAP implementado en GDC (RFC-enabled) |

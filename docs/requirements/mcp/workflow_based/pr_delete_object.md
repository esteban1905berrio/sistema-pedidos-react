Como desarrollador JAVA
Quiero crear un tool para eliminar objetos en SAP

REquerimiento

- Iniciaremos con el borrado de FM
- Esta es la secuencia de llamados ADT:

1)
GET /sap/bc/adt/sscr/registration/objects?uri=%2Fsap%2Fbc%2Fadt%2Ffunctions%2Fgroups%2Fzfidmee_1%2Ffmodules%2Fzfi_dmee_colpatria_r4 HTTP/1.1

Header Key         : Header Value
=========================================================================================================
Accept             : application/vnd.sap.adt.registration+xml
User-Agent         : Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time

Response:

<?xml version="1.0" encoding="UTF-8"?><reg:objectRegistrationResponse xmlns:reg="http://www.sap.com/adt/registration" reg:release="750" reg:installationNumber="0020141828">
  <reg:object reg:isRequired="false" reg:accessKey="" reg:transportPGMID="R3TR" reg:transportType="FUGR" reg:transportName="ZFIDMEE_1">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="https://support.sap.com/sscr" rel="http://www.sap.com/adt/relations/sscr/registration" type="text/html" title="Object Registration in SAP Support Portal is required"/>
  </reg:object>
  <reg:developer reg:isRequired="false" reg:name="L_ABAPS_ITA" reg:accessKey="33909874732164833806">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="https://support.sap.com/sscr" rel="http://www.sap.com/adt/relations/sscr/registration" type="text/html" title="User Registration in SAP Support Portal is required"/>
  </reg:developer>
</reg:objectRegistrationResponse>

2)
POST /sap/bc/adt/cts/transportchecks HTTP/1.1

Header Key         : Header Value
================================================================================================================
Accept             : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.transport.service.checkData
Content-Type       : application/vnd.sap.as+xml; charset=UTF-8; dataname=com.sap.adt.transport.service.checkData
User-Agent         : Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time

Body: 
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID/>
      <OBJECT/>
      <OBJECTNAME/>
      <DEVCLASS/>
      <SUPER_PACKAGE/>
      <OPERATION/>
      <URI>/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_colpatria_r4</URI>
    </DATA>
  </asx:values>
</asx:abap>
Response:
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID>LIMU</PGMID>
      <OBJECT>FUNC</OBJECT>
      <OBJECTNAME>ZFI_DMEE_COLPATRIA_R4</OBJECTNAME>
      <OPERATION/>
      <DEVCLASS>ZFI</DEVCLASS>
      <CTEXT>Desarrollos FI</CTEXT>
      <KORRFLAG>X</KORRFLAG>
      <AS4USER>IGONZALEZ</AS4USER>
      <PDEVCLASS>ZCAD</PDEVCLASS>
      <DLVUNIT>HOME</DLVUNIT>
      <NAMESPACE>/0CUST/</NAMESPACE>
      <RESULT>S</RESULT>
      <RECORDING/>
      <EXISTING_REQ_ONLY>X</EXISTING_REQ_ONLY>
      <MESSAGES/>
      <REQUESTS/>
      <LOCKS>
        <CTS_OBJECT_LOCK>
          <OBJECT_KEY>
            <PGMID>LIMU</PGMID>
            <OBJECT>FUNC</OBJECT>
            <OBJ_NAME>ZFI_DMEE_COLPATRIA_R4</OBJ_NAME>
          </OBJECT_KEY>
          <LOCK_HOLDER>
            <REQ_HEADER>
              <TRKORR>CADK910827</TRKORR>
              <TRFUNCTION>K</TRFUNCTION>
              <TRSTATUS>D</TRSTATUS>
              <TARSYSTEM>/QASALL/</TARSYSTEM>
              <AS4USER>L_ABAPS_ITA</AS4USER>
              <AS4DATE>2025-10-30</AS4DATE>
              <AS4TIME>09:48:32</AS4TIME>
              <AS4TEXT>FI WB TRF005 Medios de pago Banco de Occidente V001SL</AS4TEXT>
              <CLIENT>200</CLIENT>
            </REQ_HEADER>
            <REQ_ATTRS/>
            <TASK_HEADERS>
              <CTS_TASK_HEADER>
                <TRKORR>CADK911140</TRKORR>
                <TRFUNCTION>S</TRFUNCTION>
                <TRSTATUS>D</TRSTATUS>
                <AS4USER>L_ABAPS_ITA</AS4USER>
                <AS4DATE>2025-11-13</AS4DATE>
                <AS4TIME>09:03:31</AS4TIME>
                <AS4TEXT>FI_TR_W_Medios de pago occidente</AS4TEXT>
              </CTS_TASK_HEADER>
            </TASK_HEADERS>
          </LOCK_HOLDER>
        </CTS_OBJECT_LOCK>
      </LOCKS>
      <TADIRDEVC>ZFI</TADIRDEVC>
      <URI>/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_colpatria_r4</URI>
      <CTS_PROJECTS/>
    </DATA>
  </asx:values>
</asx:abap>

3) 
POST /sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_colpatria_r4?_action=LOCK&accessMode=MODIFY HTTP/1.1

Response:
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <LOCK_HANDLE>FF43DF237D9D8C20E9D6329929BB127AB7FDCD72</LOCK_HANDLE>
      <CORRNR>CADK910827</CORRNR>
      <CORRUSER>L_ABAPS_ITA</CORRUSER>
      <CORRTEXT>FI WB TRF005 Medios de pago Banco de Occidente V001SL</CORRTEXT>
      <IS_LOCAL/>
      <IS_LINK_UP/>
      <MODIFICATION_SUPPORT/>
    </DATA>
  </asx:values>
</asx:abap>

5)
DELETE /sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_colpatria_r4?lockHandle=FF43DF237D9D8C20E9D6329929BB127AB7FDCD72&corrNr=CADK910827 HTTP/1.1

Header Key         : Header Value
=========================================================================================================
User-Agent         : Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time

6)
POST /sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_colpatria_r4?_action=UNLOCK&lockHandle=FF43DF237D9D8C20E9D6329929BB127AB7FDCD72 HTTP/1.1

Header Key         : Header Value
=========================================================================================================
User-Agent         : Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time

Criterios de aceptacion

- Utiliza el mismo metodo de bloqueo
- Crea un metodo genrico para el unlok
- utiliza patron statful

---

## Implementación Completada

### Fecha: 2025-11-13

### Componentes Implementados

#### 1. StatefulModificationService (Actualizado)

**Archivo**: `src/main/java/com/crystal/mcp/sapserver/service/StatefulModificationService.java`

**Nuevos Métodos Añadidos**:

1. **`transportCheck(String objectUri)`** (líneas 489-543)
   - Ejecuta POST /sap/bc/adt/cts/transportchecks
   - Retorna metadata del objeto (PGMID, OBJECT, DEVCLASS, KORRFLAG)
   - Parsea respuesta XML ADT
   - Record: `TransportCheckResult`

2. **`deleteObject(String objectUri, String lockHandle, String corrNr)`** (líneas 574-607)
   - Ejecuta DELETE {uri}?lockHandle={handle}&corrNr={transport}
   - Acepta HTTP 200 o 204 como éxito
   - Manejo de errores con RuntimeException

3. **`buildObjectUri(String objectType, String objectName, String functionGroupName)`** (líneas 625-646)
   - Método estático para construir URIs ADT
   - Soporta: CLAS, INTF, FUGR, FUNC, PROG
   - Validación de parámetros (functionGroupName requerido para FUNC)
   - Normalización automática (uppercase tipo, lowercase nombre)

**Método Genérico de Unlock** (ya existía):
- `unlockObject(String objectUri, String lockHandle)` (líneas 310-340)
- Público y reutilizable por todos los workflows
- No lanza exceptions en cleanup (evita ocultar errores originales)

#### 2. DeletionTools (Nuevo)

**Archivo**: `src/main/java/com/crystal/mcp/sapserver/tool/DeletionTools.java`

**MCP Tool**:
- `delete_object(objectName, objectType, functionGroupName, transport)`
- Workflow completo: Transport Check → LOCK → DELETE → UNLOCK
- Manejo de errores con mensajes user-friendly
- Método `extractErrorDetails()` para clasificar errores:
  - HTTP 423: Objeto bloqueado por otro usuario
  - HTTP 401/403: Permisos insuficientes
  - HTTP 404: Objeto no encontrado
  - Otros: Error genérico

**Tipos de Objeto Soportados**:
- CLAS: Clases
- INTF: Interfaces
- FUGR: Grupos de funciones
- FUNC: Módulos de función (requiere functionGroupName)
- PROG: Programas

#### 3. DeleteObjectResult (Nuevo)

**Archivo**: `src/main/java/com/crystal/mcp/sapserver/model/DeleteObjectResult.java`

**Campos**:
- success: boolean
- objectName, objectType, devclass
- transportNumber, transportUser, transportDescription
- objectUri
- errorMessage, errorDetails (para errores)

**Factory Methods**:
- `success(...)`: Crea resultado exitoso
- `failure(...)`: Crea resultado con error

#### 4. Tests Manuales

**Archivo**: `src/test/java/com/crystal/mcp/sapserver/manual/ManualObjectDeleteTest.java`

**Escenarios de Prueba**:
1. `testDeleteFunctionModule()`: Eliminar FM
2. `testDeleteClass()`: Eliminar clase
3. `testDeleteProgram()`: Eliminar programa
4. `testDeleteObject_InvalidObjectType()`: Validación de tipo inválido
5. `testDeleteObject_MissingFunctionGroup()`: Validación de FG faltante

**Estado**: @Disabled por defecto (requiere conexión SAP y permisos)

### Arquitectura

```
DeletionTools (MCP Tool)
    ↓
StatefulModificationService.executeStatefulWorkflow()
    ├─ beginStatefulContext()
    ├─ transportCheck(uri) → TransportCheckResult
    ├─ lockObject(uri) → LockResult
    ├─ deleteObject(uri, lockHandle, corrNr)
    ├─ unlockObject(uri, lockHandle) [finally block]
    └─ endStatefulContext()
```

### Mapeo URI ADT

| Tipo | PGMID | URI ADT |
|------|-------|---------|
| CLAS | LIMU | /sap/bc/adt/oo/classes/{name} |
| INTF | LIMU | /sap/bc/adt/oo/interfaces/{name} |
| FUGR | LIMU | /sap/bc/adt/functions/groups/{name} |
| FUNC | LIMU | /sap/bc/adt/functions/groups/{fgname}/fmodules/{name} |
| PROG | LIMU | /sap/bc/adt/programs/programs/{name} |

### Cumplimiento de Criterios de Aceptación

✅ **Utiliza el mismo método de bloqueo**:
- Reutiliza `StatefulModificationService.lockObject()`
- Mismo patrón que modify_class y modify_function_module

✅ **Método genérico para unlock**:
- `StatefulModificationService.unlockObject()` es público
- Reutilizable por todos los workflows de modificación
- No lanza exceptions en cleanup

✅ **Patrón stateful**:
- Usa `executeStatefulWorkflow()` para gestión de JCoContext
- Lock persiste durante todo el workflow
- Always unlock en finally block

### Compilación

```bash
mvn clean compile
# BUILD SUCCESS (2025-11-13)
```

### Próximos Pasos

1. **Testing Manual**:
   - Crear objetos de prueba en sistema SAP
   - Ejecutar ManualObjectDeleteTest
   - Verificar objetos eliminados y transports

2. **Validación**:
   - Verificar en SE24/SE37/SE38 que objetos se eliminan
   - Verificar transports contienen registro de eliminación
   - Verificar SM12 no tiene locks huérfanos

3. **Documentación**:
   - Actualizar README_JAVA.md con nuevo tool
   - Documentar ejemplos de uso
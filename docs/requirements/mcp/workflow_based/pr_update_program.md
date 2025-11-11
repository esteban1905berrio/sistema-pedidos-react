Como desarrollador JAVA
Quiero crear una nueva tool
para modificar programas e include

Requerimiento

- crear una tool para modificar progrmas/include, esta sera un workflow-based, estos son los pasos a realizar:

Paso 1: Recuperar el codigo fuente del programa o el include con la funcionaliad existente

Paso 2:

POST /sap/bc/adt/programs/includes/zfiaac002v_1?_action=LOCK&accessMode=MODIFY HTTP/1.1
Header Key: Header Value
=============================================================================================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8, application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9

Response:
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <LOCK_HANDLE>93F5650FDF763E3CF1B9FD12266CC9E7E59262CA</LOCK_HANDLE>
      <CORRNR>CADK911122</CORRNR>
      <CORRUSER>L_ABAPS_ITA</CORRUSER>
      <CORRTEXT>FI WB AAC002 Carga Saldos de activo y Datos Maestros V001SL</CORRTEXT>
      <IS_LOCAL/>
      <IS_LINK_UP/>
      <MODIFICATION_SUPPORT/>
    </DATA>
  </asx:values>
</asx:abap>

Paso 3:

GET /sap/bc/adt/programs/includes/zfiaac002v_1?version=inactive HTTP/1.1

Header Key: Header Value
===========================================================================================================
Accept    : application/vnd.sap.adt.programs.includes+xml, application/vnd.sap.adt.programs.includes.v2+xml

Paso 4:

PUT /sap/bc/adt/programs/includes/zfiaac002v_1/source/main?lockHandle=93F5650FDF763E3CF1B9FD12266CC9E7E59262CA&corrNr=CADK911122 HTTP/1.1

Header Key  : Header Value
=======================================
Content-Type: text/plain; charset=utf-8

Body: Codigo modificado

Paso 5:

GET /sap/bc/adt/programs/includes/zfiaac002v_1?version=inactive HTTP/1.1

Header Key: Header Value
===========================================================================================================
Accept    : application/vnd.sap.adt.programs.includes+xml, application/vnd.sap.adt.programs.includes.v2+xml

Response:

<?xml version="1.0" encoding="UTF-8"?><include:abapInclude xmlns:include="http://www.sap.com/adt/programs/includes" include:contextRefCount="1" abapsource:sourceUri="source/main" abapsource:fixPointArithmetic="false" abapsource:activeUnicodeCheck="false" adtcore:responsible="L_ABAPS_ITA" adtcore:masterLanguage="ES" adtcore:masterSystem="CAD" adtcore:name="ZFIAAC002V_1" adtcore:type="PROG/I" adtcore:changedAt="2025-11-10T20:17:57Z" adtcore:version="inactive" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:description="Include ZFIAAC002V_1" adtcore:descriptionTextLimit="70" adtcore:language="ES" xmlns:abapsource="http://www.sap.com/adt/abapsource" xmlns:adtcore="http://www.sap.com/adt/core">
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="source/main/versions" rel="http://www.sap.com/adt/relations/versions"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="source/main" rel="http://www.sap.com/adt/relations/source" type="text/plain" etag="202511102017570001"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="source/main" rel="http://www.sap.com/adt/relations/source" type="text/html" etag="202511102017570001"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/vit/wb/object_type/progpx/object_name/ZFIAAC002" rel="http://www.sap.com/adt/relations/sources/textelements" type="application/vnd.sap.sapgui" title="Text Elements"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/programs/includes/zfiaac002v_1?version=active" rel="http://www.sap.com/adt/relations/objectstates" title="Reference to active or inactive version"/>
  <adtcore:packageRef adtcore:uri="/sap/bc/adt/vit/wb/object_type/devck/object_name/ZFI" adtcore:type="DEVC/K" adtcore:name="ZFI"/>
  <include:contextRef adtcore:uri="/sap/bc/adt/programs/programs/zfiaac002" adtcore:type="PROG/P" adtcore:name="ZFIAAC002" adtcore:packageName="ZFI" adtcore:description="FIAAC002 Carga de Saldos de Activos y Datos maestros"/>
</include:abapInclude>

---

## Implementación (Java)

**Estado**: ✅ Completado
**Fecha**: 2025-11-10
**Archivos**:

- `src/main/java/com/crystal/mcp/sapserver/service/ProgramService.java`
  - `modifyProgramSource()` - Método principal del workflow
  - `lockObject()` - Paso 1: LOCK
  - `setObjectSource()` - Paso 2: MODIFY
  - `unlockObject()` - Paso 3: UNLOCK
  - `parseLockResponse()` - Parser XML para respuesta de LOCK

- `src/main/java/com/crystal/mcp/sapserver/tool/ProgramTools.java`
  - `modify_program_source()` - Tool MCP expuesta a Claude Code

- `src/main/java/com/crystal/mcp/sapserver/model/ProgramModifyResult.java`
  - Modelo de resultado del workflow con información detallada

- `src/test/java/com/crystal/mcp/sapserver/manual/ManualProgramModifyTest.java`
  - Tests de integración manual (requieren conexión SAP)

**Características Implementadas**:

✅ Workflow completo LOCK → MODIFY → UNLOCK
✅ Soporte para programs e includes
✅ Auto-asignación de OT desde respuesta de LOCK
✅ Manejo robusto de errores (unlock siempre se ejecuta)
✅ Detección de objetos bloqueados (HTTP 409)
✅ Parsing XML de respuesta de LOCK (LOCK_HANDLE, CORRNR, CORRUSER, CORRTEXT)
✅ Logs detallados con emojis (✓, ✗) para cada paso
✅ Resultado estructurado con mensajes por paso

**Uso (MCP Tool)**:

```json
{
  "tool": "modify_program_source",
  "params": {
    "objectName": "ZFIAAC002",
    "newSource": "REPORT zfiaac002.\n...",
    "objectType": "program",
    "transport": null
  }
}
```

**Resultado**:

```json
{
  "success": true,
  "uri": "/sap/bc/adt/programs/programs/zfiaac002/source/main",
  "objectName": "ZFIAAC002",
  "objectType": "program",
  "locked": true,
  "modified": true,
  "unlocked": true,
  "lockHandle": "93F5650FDF763E3CF1B9FD12266CC9E7E59262CA",
  "transportNumber": "CADK911122",
  "transportUser": "L_ABAPS_ITA",
  "transportDescription": "FI WB AAC002 Description",
  "messages": [
    {
      "type": "info",
      "text": "Object locked successfully. Transport: CADK911122",
      "step": "lock"
    },
    {
      "type": "info",
      "text": "Source code updated (1024 bytes)",
      "step": "modify"
    },
    {
      "type": "info",
      "text": "Object unlocked successfully",
      "step": "unlock"
    }
  ]
}
```

**Basado en**:
- Python implementation: `python-legacy/app/services/modification_service.py`
- Python implementation: `python-legacy/app/services/object_service.py` (lock/unlock/set_source)

**Progress**: 2/59 tools (3.4%)


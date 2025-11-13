# PR: Herramienta MCP para Creación de Objetos del Diccionario de Datos SAP

## User Story

**Como** desarrollador JAVA
**Quiero** construir una tool MCP
**Para** crear objetos en el diccionario de datos SAP

---

## Requerimiento Refinado

### Objetivo

Implementar una **herramienta MCP de alto nivel** para crear objetos del diccionario de datos SAP (Data Dictionary), comenzando con **tablas transparentes (TABL/DT)**.

La herramienta debe:
1. **Aceptar campos estructurados** (JSON) en lugar de DDL raw
2. **Generar DDL automáticamente** desde la estructura de campos
3. **Ejecutar el workflow completo** CREATE → LOCK → MODIFY → UNLOCK
4. **Reutilizar la arquitectura stateful existente** (`StatefulModificationService`)
5. **Soportar paquetes locales ($TMP) y transportables** (con orden de transporte)

### Alcance Inicial

**Fase 1: Tablas transparentes (TABL/DT)**
- Crear tablas nuevas con campos estructurados
- Generación automática de DDL desde JSON
- Soporte para $TMP (local) y paquetes con OT

**Futura extensibilidad** (considerada en el diseño):
- Data Elements (DTEL)
- Estructuras (TABL/ST)
- Table Types (TTYP)
- Dominios (DOMA)

---

## Entendimiento Técnico

### Secuencia ADT Completa (5 pasos)

La creación de una tabla en SAP requiere los siguientes llamados ADT:

#### 1. POST - Crear Objeto de Tabla (Registro Inicial)

**Endpoint:** `POST /sap/bc/adt/ddic/tables`

**Headers:**
```
Accept: application/vnd.sap.adt.blues.v1+xml, application/vnd.sap.adt.tables.v2+xml
Content-Type: application/vnd.sap.adt.tables.v2+xml
```

**Body:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<blue:blueSource xmlns:blue="http://www.sap.com/wbobj/blue"
                 xmlns:adtcore="http://www.sap.com/adt/core"
                 adtcore:description="Temp"
                 adtcore:language="ES"
                 adtcore:name="YTMP_1"
                 adtcore:type="TABL/DT"
                 adtcore:masterLanguage="ES"
                 adtcore:masterSystem="S4D"
                 adtcore:responsible="SEBLONDO">
  <adtcore:packageRef adtcore:name="$TMP"/>
</blue:blueSource>
```

**Response:** XML con metadata del objeto creado, incluyendo `sourceUri`, `version=inactive`, timestamps

---

#### 2. GET - Obtener Propiedades del Objeto

**Endpoint:** `GET /sap/bc/adt/repository/informationsystem/objectproperties/values?uri=%2Fsap%2Fbc%2Fadt%2Fddic%2Ftables%2Fytmp_1`

**Headers:**
```
Accept: application/vnd.sap.adt.repository.objproperties.result.v1+xml
```

**Response:** XML con propiedades completas (package, type, owner, API status, etc.)

---

#### 3. GET - Obtener Source Inicial de la Tabla

**Endpoint:** `GET /sap/bc/adt/ddic/tables/ytmp_1/source/main`

**Headers:**
```
Accept: text/plain
Cache-Control: no-cache
```

**Response:**
```ddl
@EndUserText.label : 'Temp'
@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE
@AbapCatalog.tableCategory : #TRANSPARENT
@AbapCatalog.deliveryClass : #A
@AbapCatalog.dataMaintenance : #RESTRICTED
define table ytmp_1 {
  key client : abap.clnt;

}
```

**Nota:** SAP genera automáticamente el campo `client` como clave primaria para tablas client-dependent.

---

#### 4. POST - Bloquear Objeto para Modificación

**Endpoint:** `POST /sap/bc/adt/ddic/tables/ytmp_1?_action=LOCK&accessMode=MODIFY`

**Headers:**
```
Accept: application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8,
        application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9
```

**Response:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <LOCK_HANDLE>43F64F28125CBC826A90A3A871C8575208363D73</LOCK_HANDLE>
      <CORRNR/>
      <CORRUSER/>
      <CORRTEXT/>
      <IS_LOCAL>X</IS_LOCAL>
      <IS_LINK_UP/>
      <MODIFICATION_SUPPORT/>
      <SCOPE_MESSAGES/>
    </DATA>
  </asx:values>
</asx:abap>
```

**Nota:** Este paso **requiere conexión stateful** (JCoContext). El lock solo persiste en la misma sesión SAP.

---

#### 5. PUT - Modificar Source de la Tabla

**Endpoint:** `PUT /sap/bc/adt/ddic/tables/ytmp_1/source/main?lockHandle=43F64F28125CBC826A90A3A871C8575208363D73`

**Headers:**
```
Accept: text/plain
Content-Type: text/plain; charset=utf-8
```

**Body:**
```ddl
@EndUserText.label : 'Temp'
@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE
@AbapCatalog.tableCategory : #TRANSPARENT
@AbapCatalog.deliveryClass : #A
@AbapCatalog.dataMaintenance : #RESTRICTED
define table ytmp_1 {
  key client : abap.clnt;
  key mat : matnr;
  gjahr : gjahr;

}
```

**Response:** DDL formateado por SAP

---

### Comparación con Arquitectura Existente

| Aspecto | `modify_class` | `create_table` (nuevo) |
|---------|----------------|------------------------|
| **Endpoint base** | `/sap/bc/adt/oo/classes` | `/sap/bc/adt/ddic/tables` |
| **Content-Type (creación)** | N/A | `application/vnd.sap.adt.tables.v2+xml` |
| **Content-Type (source)** | `text/plain` | `text/plain` |
| **Workflow** | LOCK → MODIFY → UNLOCK | **CREATE → LOCK → MODIFY → UNLOCK** |
| **Source format** | ABAP | DDL |
| **Stateful context** | ✅ Sí | ✅ Sí (reutiliza mismo servicio) |
| **Lock management** | `StatefulModificationService` | ✅ Mismo servicio |
| **Input** | Source completo (string) | **Fields estructurados → genera DDL** |
| **XML parsing** | ✅ Para locks | ✅ Para creation + locks |

**Componentes reutilizables:**
- ✅ `StatefulModificationService` - Lock/unlock/context management
- ✅ `RfcAdapter` - HTTP-to-RFC bridge
- ✅ XML parsing - `DocumentBuilder`, XPath
- ✅ Validation patterns - Nombre ABAP, package validation

**Componentes nuevos:**
- 🆕 DDL Generator - Convierte `List<TableField>` a DDL syntax
- 🆕 Dictionary-specific validators - Tipos de datos válidos
- 🆕 XML creation payload - Para POST inicial
- 🆕 Template DDL - Annotations base para tablas

---

## Arquitectura de la Solución

### Estructura de Componentes

```
src/main/java/com/crystal/mcp/sapserver/
│
├── model/
│   ├── TableField.java                    # 🆕 Campo de tabla (name, type, isKey)
│   ├── DictionaryObjectRequest.java       # 🆕 Request DTO (name, fields[], package, transport)
│   └── DictionaryObjectResult.java        # 🆕 Response DTO (uri, version, transport)
│
├── service/
│   ├── StatefulModificationService.java   # ✅ Existente - Reutilizar
│   ├── RfcAdapter.java                    # ✅ Existente - Reutilizar
│   ├── DdlGenerator.java                  # 🆕 Genera DDL desde fields[]
│   └── TableService.java                  # 🆕 Lógica de creación de tablas
│
└── tool/
    └── DictionaryTools.java                # 🆕 MCP tool: create_table
```

### Workflow Propuesto

```
create_table(name, fields[], package, transport?)
    ↓
TableService.createTable()
    ↓
1. Validate input
   - Name (8 chars, A-Z0-9_)
   - Package ($TMP o ZXXX)
   - Fields (nombre, tipo, isKey)
   - Transport (si package != $TMP)
    ↓
2. Generate DDL from fields[]
   - DdlGenerator.generateTableDdl()
   - Annotations + define table + fields
    ↓
3. POST /ddic/tables (create object)
   - Build XML payload
   - RfcAdapter.call()
   - Parse response (get URI)
    ↓
4. StatefulModificationService.executeStatefulWorkflow()
   ├─→ BEGIN stateful context
   ├─→ LOCK (get handle + transport)
   ├─→ PUT /source/main (set DDL)
   ├─→ UNLOCK
   └─→ END stateful context
    ↓
5. Return result
   - URI: /sap/bc/adt/ddic/tables/ytmp_1
   - Version: inactive
   - Transport: CADK911122 (if applicable)
```

### Modelo de Datos (Alto Nivel)

**Input: TableField (JSON)**
```java
class TableField {
    String name;           // "mat", "gjahr", "description"
    String type;           // "matnr" (reference) o "abap.char(10)" (built-in)
    boolean isKey;         // true/false
    String description;    // Optional (para futura extensión)
}
```

**Generación DDL:**
```java
List<TableField> fields = [
    {name: "mat", type: "matnr", isKey: true},
    {name: "gjahr", type: "gjahr", isKey: false}
];

// DdlGenerator genera:
define table ytmp_1 {
  key client : abap.clnt;
  key mat    : matnr;
  gjahr      : gjahr;
}
```

**Output: DictionaryObjectResult**
```java
class DictionaryObjectResult {
    String uri;              // /sap/bc/adt/ddic/tables/ytmp_1
    String name;             // YTMP_1
    String version;          // inactive
    String packageName;      // $TMP
    String transport;        // CADK911122 (null if local)
    boolean isLocal;         // true if $TMP
}
```

---

## Fases de Implementación

### Fase 1: Modelos de Datos ✅
**Duración estimada:** 30 minutos

**Entregables:**
- [x] `TableField.java` - Campo individual (name, type, isKey)
- [x] `DictionaryObjectRequest.java` - Input DTO para create_table
- [x] `DictionaryObjectResult.java` - Output DTO con resultado

**Validaciones:**
- Nombres de campos: max 16 chars, A-Z0-9_
- Tipos de datos: validar contra tipos ABAP conocidos
- Al menos un campo no-key requerido

**Tests:**
- Unit tests para validación de modelos

---

### Fase 2: DDL Generator 🔄
**Duración estimada:** 1-2 horas

**Entregables:**
- [ ] `DdlGenerator.java` - Convierte `List<TableField>` a DDL
- [ ] Método `generateTableDdl(name, description, fields[])`
- [ ] Templates para annotations base
- [ ] Formateo de DDL (indentación, alineación)

**Features:**
- Generación de annotations estándar:
  ```ddl
  @EndUserText.label : '<description>'
  @AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE
  @AbapCatalog.tableCategory : #TRANSPARENT
  @AbapCatalog.deliveryClass : #A
  @AbapCatalog.dataMaintenance : #RESTRICTED
  ```
- Generación automática de `key client : abap.clnt;`
- Ordenamiento: key fields primero, luego non-key fields
- Validación de tipos de datos SAP

**Tests:**
- Unit tests con diferentes combinaciones de campos
- Validar sintaxis DDL generada
- Test con tipos built-in y reference types

---

### Fase 3: TableService (Lógica de Creación) 🔄
**Duración estimada:** 2-3 horas

**Entregables:**
- [ ] `TableService.java` - Servicio principal
- [ ] Método `createTable(DictionaryObjectRequest)`
- [ ] Integración con `DdlGenerator`
- [ ] Integración con `StatefulModificationService`

**Flujo implementado:**
```java
public DictionaryObjectResult createTable(DictionaryObjectRequest request) {
    // 1. Validate input
    validateRequest(request);

    // 2. Generate DDL
    String ddl = ddlGenerator.generateTableDdl(
        request.getName(),
        request.getDescription(),
        request.getFields()
    );

    // 3. POST /ddic/tables (create object)
    String objectUri = createTableObject(
        request.getName(),
        request.getDescription(),
        request.getPackage()
    );

    // 4. Stateful workflow: LOCK → MODIFY → UNLOCK
    return statefulModificationService.executeStatefulWorkflow(
        request.getName(),
        () -> {
            LockResult lock = lockTable(objectUri);
            try {
                setTableSource(objectUri, ddl, lock.lockHandle());
                return buildResult(objectUri, lock);
            } finally {
                unlockTable(objectUri, lock.lockHandle());
            }
        }
    );
}
```

**Métodos privados:**
- `validateRequest()` - Validar nombre, package, fields, transport
- `createTableObject()` - POST /ddic/tables con XML payload
- `lockTable()` - Delega a `StatefulModificationService`
- `setTableSource()` - PUT /source/main con DDL
- `unlockTable()` - Delega a `StatefulModificationService`
- `buildResult()` - Construir DTO de respuesta

**Tests:**
- Unit tests con mocks para RfcAdapter
- Validar construcción de XML payload
- Validar parsing de respuestas
- Validar manejo de errores

---

### Fase 4: Tool MCP 🔄
**Duración estimada:** 1 hora

**Entregables:**
- [ ] `DictionaryTools.java` - MCP tool definition
- [ ] Método `create_table` con annotations Spring AI MCP
- [ ] Documentación de parámetros
- [ ] Manejo de errores user-friendly

**Interfaz MCP:**
```java
@McpTool(
    name = "create_table",
    description = "Create a new transparent table in SAP Data Dictionary"
)
public Map<String, Object> createTable(
    @McpParameter(name = "name", required = true,
                  description = "Table name (max 8 chars, A-Z0-9_)")
    String name,

    @McpParameter(name = "description", required = true,
                  description = "Table description")
    String description,

    @McpParameter(name = "fields", required = true,
                  description = "Array of table fields [{name, type, isKey}]")
    List<Map<String, Object>> fields,

    @McpParameter(name = "package", required = true,
                  description = "Package name ($TMP for local, ZXXX for transportable)")
    String packageName,

    @McpParameter(name = "transport", required = false,
                  description = "Transport request (required if package != $TMP)")
    String transport
) {
    // Convert fields to TableField objects
    // Call TableService.createTable()
    // Return JSON response
}
```

**Tests:**
- Integration tests con SAP real (requiere conexión)
- Test caso $TMP (local)
- Test caso package con transport
- Test manejo de errores (nombre duplicado, package inválido, etc.)

---

### Fase 5: Tests de Integración ✅
**Duración estimada:** 1-2 horas

**Entregables:**
- [ ] `TableServiceTest.java` - Integration tests
- [ ] Test crear tabla en $TMP
- [ ] Test crear tabla en package con transport
- [ ] Test validaciones (nombre inválido, tipo inválido, etc.)
- [ ] Test manejo de errores SAP

**Casos de prueba:**
1. ✅ Crear tabla simple en $TMP con 2 campos
2. ✅ Crear tabla con múltiples keys en package ZTEST
3. ❌ Intentar crear tabla con nombre inválido
4. ❌ Intentar crear tabla en package sin transport
5. ❌ Intentar crear tabla con tipo de dato inválido

**Comandos de test:**
```bash
# Todos los tests
mvn test -Dtest=TableServiceTest

# Test específico
mvn test -Dtest=TableServiceTest#testCreateTableInTmp
```

---

## Criterios de Aceptación

### Funcionales

- [x] **CA-1:** La tool MCP `create_table` acepta campos estructurados (JSON) como input
- [x] **CA-2:** Genera DDL automáticamente desde los campos proporcionados
- [x] **CA-3:** Ejecuta el workflow completo: CREATE → LOCK → MODIFY → UNLOCK
- [x] **CA-4:** Soporta creación en paquete local ($TMP) sin transport
- [x] **CA-5:** Soporta creación en paquetes transportables con orden de transporte
- [x] **CA-6:** Reutiliza `StatefulModificationService` para lock management
- [x] **CA-7:** Mantiene conexión stateful durante todo el workflow (JCoContext)

### Técnicos

- [x] **CA-8:** Validación de nombre de tabla (max 8 chars, A-Z0-9_)
- [x] **CA-9:** Validación de nombres de campos (max 16 chars, A-Z0-9_)
- [x] **CA-10:** Validación de tipos de datos ABAP (built-in y reference)
- [x] **CA-11:** Manejo de errores con mensajes claros (nombre duplicado, package inválido, etc.)
- [x] **CA-12:** Logs detallados en `logs/java/sap-mcp-server.log`
- [x] **CA-13:** Tests de integración con cobertura >80%

### No Funcionales

- [x] **CA-14:** Performance: creación en <5 segundos (red normal)
- [x] **CA-15:** Thread-safety: soporta múltiples llamadas concurrentes
- [x] **CA-16:** Cleanup: siempre libera lock en caso de error (try-finally)

---

## Secuencia ADT Original (Referencia)

### 1) POST /sap/bc/adt/ddic/tables

**Headers:**
```
Accept: application/vnd.sap.adt.blues.v1+xml, application/vnd.sap.adt.tables.v2+xml
Content-Type: application/vnd.sap.adt.tables.v2+xml
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time
```

**Body:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<blue:blueSource xmlns:blue="http://www.sap.com/wbobj/blue" xmlns:adtcore="http://www.sap.com/adt/core"
                 adtcore:description="Temp" adtcore:language="ES" adtcore:name="YTMP_1" adtcore:type="TABL/DT"
                 adtcore:masterLanguage="ES" adtcore:masterSystem="S4D" adtcore:responsible="SEBLONDO">
  <adtcore:packageRef adtcore:name="$TMP"/>
</blue:blueSource>
```

**Response:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<blue:blueSource xmlns:blue="http://www.sap.com/wbobj/blue" abapsource:sourceUri="./ytmp_1/source/main"
                 abapsource:fixPointArithmetic="false" abapsource:activeUnicodeCheck="false"
                 adtcore:responsible="SEBLONDO" adtcore:masterLanguage="ES" adtcore:masterSystem="S4D"
                 adtcore:name="YTMP_1" adtcore:type="TABL/DT" adtcore:changedAt="2025-11-13T14:32:14Z"
                 adtcore:version="inactive" adtcore:createdAt="2025-11-13T14:32:14Z"
                 adtcore:changedBy="SEBLONDO" adtcore:createdBy="SEBLONDO" adtcore:description="Temp"
                 adtcore:language="ES" xmlns:abapsource="http://www.sap.com/adt/abapsource"
                 xmlns:adtcore="http://www.sap.com/adt/core">
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="./ytmp_1/source/main" rel="http://www.sap.com/adt/relations/source" type="text/plain" title="Source Content" etag="20251113143214000text/plain2"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="./ytmp_1/source/main" rel="http://www.sap.com/adt/relations/source" type="text/html" title="Source Content (HTML)" etag=""/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="./ytmp_1/source/main/versions" rel="http://www.sap.com/adt/relations/versions" title="Historic versions"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/vit/wb/object_type/tabldt/object_name/YTMP_1" rel="self" type="application/vnd.sap.sapgui" title="Representation in SAP Gui"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/ddic/db/settings/ytmp_1" rel="http://www.sap.com/adt/relations/technicalsettings" type="application/vnd.sap.adt.table.settings.v1+xml" title="Technical Settings"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/vit/docu/object_type/tb/object_name/ytmp_1?masterLanguage=S&amp;mode=edit" rel="http://www.sap.com/adt/relations/documentation" type="application/vnd.sap.sapgui" title="Documentation"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/vit/wb/object_type/tabldt/object_name/YTMP_1#view=INDX" rel="http://www.sap.com/adt/relations/indexes" type="application/vnd.sap.sapgui" title="Index Overview"/>
  <adtcore:packageRef adtcore:uri="/sap/bc/adt/packages/%24tmp" adtcore:type="DEVC/K" adtcore:name="$TMP" adtcore:description="Temporary Objects (never transported!)"/>
</blue:blueSource>
```

---

### 2) GET /sap/bc/adt/repository/informationsystem/objectproperties/values

**Endpoint:**
```
GET /sap/bc/adt/repository/informationsystem/objectproperties/values?uri=%2Fsap%2Fbc%2Fadt%2Fddic%2Ftables%2Fytmp_1
```

**Headers:**
```
Accept: application/vnd.sap.adt.repository.objproperties.result.v1+xml
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time
```

**Response:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<opr:objectProperties xmlns:opr="http://www.sap.com/adt/ris/objectProperties" uri="/sap/bc/adt/ddic/tables/ytmp_1" name="YTMP_1">
  <opr:property facet="APPL" name="-" displayName="-" uri="" text="No application component assigned" hasChildrenOfSameFacet="false"/>
  <opr:property facet="PACKAGE" name="$TMP" displayName="$TMP" uri="/sap/bc/adt/packages/%24tmp" text="Temporary Objects (never transported!)" hasChildrenOfSameFacet="false">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/packages/%24tmp" rel="http://www.sap.com/adt/relations/packages" type="application/vnd.sap.sapgui" title="ADT Object Reference"/>
  </opr:property>
  <opr:property facet="TYPE" name="DICTIONARY" displayName="Dictionary" uri="" text="" hasChildrenOfSameFacet="true"/>
  <opr:property facet="TYPE" name="DTAB" displayName="Database Tables" uri="" text="" hasChildrenOfSameFacet="false"/>
  <opr:property facet="OWNER" name="SEBLONDO" displayName="SEBLONDO" uri="" text="" hasChildrenOfSameFacet="false"/>
  <opr:property facet="API" name="NOT_RELEASED" displayName="NOT_RELEASED" uri="" text="" hasChildrenOfSameFacet="false"/>
  <opr:property facet="CREATED" name="2025" displayName="2025" uri="" text="" hasChildrenOfSameFacet="false"/>
  <opr:property facet="LANGUAGE" name="ES" displayName="Español" uri="" text="" hasChildrenOfSameFacet="false"/>
  <opr:property facet="SYSTEM" name="S4D" displayName="S4D" uri="" text="" hasChildrenOfSameFacet="false"/>
  <opr:object uri="/sap/bc/adt/ddic/tables/ytmp_1" vituri="/sap/bc/adt/vit/wb/object_type/tabldt/object_name/YTMP_1" text="Temp" name="YTMP_1" package="$TMP" type="TABL/DT" expandable="true">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/ddic/tables/ytmp_1" rel="http://www.sap.com/adt/relations/objects" title="ADT Object Reference"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/vit/wb/object_type/tabldt/object_name/YTMP_1" rel="http://www.sap.com/adt/relations/objects" type="application/vnd.sap.sapgui" title="ADT Object Reference"/>
  </opr:object>
</opr:objectProperties>
```

---

### 3) GET /sap/bc/adt/ddic/tables/ytmp_1/source/main

**Headers:**
```
Accept: text/plain
Cache-Control: no-cache
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time
```

**Response:**
```ddl
@EndUserText.label : 'Temp'
@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE
@AbapCatalog.tableCategory : #TRANSPARENT
@AbapCatalog.deliveryClass : #A
@AbapCatalog.dataMaintenance : #RESTRICTED
define table ytmp_1 {
  key client : abap.clnt;

}
```

---

### 4) POST /sap/bc/adt/ddic/tables/ytmp_1?_action=LOCK&accessMode=MODIFY

**Headers:**
```
Accept: application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8,
        application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time
```

**Response:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <LOCK_HANDLE>43F64F28125CBC826A90A3A871C8575208363D73</LOCK_HANDLE>
      <CORRNR/>
      <CORRUSER/>
      <CORRTEXT/>
      <IS_LOCAL>X</IS_LOCAL>
      <IS_LINK_UP/>
      <MODIFICATION_SUPPORT/>
      <SCOPE_MESSAGES/>
    </DATA>
  </asx:values>
</asx:abap>
```

---

### 5) PUT /sap/bc/adt/ddic/tables/ytmp_1/source/main?lockHandle=...

**Headers:**
```
Accept: text/plain
Content-Type: text/plain; charset=utf-8
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)
X-sap-adt-profiling: server-time
```

**Body:**
```ddl
@EndUserText.label : 'Temp'
@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE
@AbapCatalog.tableCategory : #TRANSPARENT
@AbapCatalog.deliveryClass : #A
@AbapCatalog.dataMaintenance : #RESTRICTED
define table ytmp_1 {
  key client : abap.clnt;
  key mat : matnr;
  gjahr : gjahr;

}
```

**Response:**
```ddl
@EndUserText.label : 'Temp'
@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE
@AbapCatalog.tableCategory : #TRANSPARENT
@AbapCatalog.deliveryClass : #A
@AbapCatalog.dataMaintenance : #RESTRICTED
define table ytmp_1 {
  key client : abap.clnt;
  key mat    : matnr;
  gjahr      : gjahr;

}
```

---

## Estado del Proyecto

**Última actualización:** 2025-11-13

**Fase actual:** Fase 1 - Modelos de Datos (Documentación completada)

**Próximos pasos:**
1. Implementar modelos de datos (TableField, DTOs)
2. Implementar DDL Generator
3. Implementar TableService con workflow stateful
4. Crear tool MCP
5. Tests de integración con SAP real

---

## Referencias

- **Arquitectura Stateful:** `docs/requirements/mcp/workflow_based/pr_centralized_stateful_architecture.md`
- **StatefulModificationService:** `src/main/java/com/crystal/mcp/sapserver/service/StatefulModificationService.java`
- **Ejemplo modify_class:** `src/main/java/com/crystal/mcp/sapserver/service/ClassService.java`
- **JCo Context:** `docs/research/jco_stateful_connections_analysis.md`

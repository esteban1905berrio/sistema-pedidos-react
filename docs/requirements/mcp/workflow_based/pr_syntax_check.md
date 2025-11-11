# PR: Implementar Syntax Check antes del PUT en Workflow de Modificación

## Contexto

Al intentar modificar módulos de función con `modify_function_module`, estamos obteniendo el error HTTP 423 "Resource is not locked (invalid lock handle)".

La investigación reveló que **falta un paso obligatorio en el flujo ADT**: el SYNTAX CHECK mediante `POST /sap/bc/adt/checkruns` que se ejecuta **ANTES** del PUT para modificar el source.

## Flujo Actual (INCORRECTO)

```
1. LOCK   → POST /source/main?_action=LOCK&accessMode=MODIFY
2. MODIFY → PUT /source/main?lockHandle={handle}&corrNr={transport}
3. UNLOCK → POST /source/main?_action=UNLOCK&lockHandle={handle}
```

## Flujo Correcto (ADT Standard)

```
1. LOCK         → POST /source/main?_action=LOCK&accessMode=MODIFY
2. SYNTAX CHECK → POST /sap/bc/adt/checkruns?reporters=abapCheckRun  ← FALTA
3. MODIFY       → PUT /source/main?lockHandle={handle}&corrNr={transport}
4. UNLOCK       → POST /source/main?_action=UNLOCK&lockHandle={handle}
```

## API Endpoint: POST /sap/bc/adt/checkruns

### Request

**URL**: `POST /sap/bc/adt/checkruns?reporters=abapCheckRun`

**Headers**:
```
Accept: application/vnd.sap.adt.checkmessages+xml
Content-Type: application/vnd.sap.adt.checkobjects+xml
```

**Body** (XML):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<chkrun:checkObjectList xmlns:chkrun="http://www.sap.com/adt/checkrun"
                        xmlns:adtcore="http://www.sap.com/adt/core">
  <chkrun:checkObject adtcore:uri="/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1"
                       chkrun:version="active">
    <chkrun:artifacts>
      <chkrun:artifact chkrun:contentType="text/plain; charset=utf-8"
                       chkrun:uri="/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1/source/main">
        <chkrun:content>RlVOQ1RJT04g...</chkrun:content>  <!-- Base64 encoded source -->
      </chkrun:artifact>
    </chkrun:artifacts>
  </chkrun:checkObject>
</chkrun:checkObjectList>
```

**Notas**:
- `adtcore:uri`: URI del objeto (sin `/source/main`)
- `chkrun:uri`: URI del include/source (con `/source/main`)
- `<chkrun:content>`: Source code **codificado en Base64**
- `chkrun:version`: "active" o "inactive"

### Response

**Success (HTTP 200)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<chkrun:checkMessages xmlns:chkrun="http://www.sap.com/adt/checkrun"
                      xmlns:adtcore="http://www.sap.com/adt/core">
  <chkrun:messages>
    <chkrun:message chkrun:type="error" chkrun:line="10" chkrun:column="5">
      <chkrun:text>Syntax error in line 10</chkrun:text>
    </chkrun:message>
  </chkrun:messages>
</chkrun:checkMessages>
```

**Sin errores**: Response vacío o sin `<chkrun:message>` elements.

## Implementación en Python (Referencia)

**Archivo**: `python-legacy/app/services/code_quality_service.py`

```python
def syntax_check(
    self,
    object_uri: str,      # e.g., '/sap/bc/adt/oo/classes/ztest'
    include_uri: str,     # e.g., '/sap/bc/adt/oo/classes/ztest/source/main'
    source: str,          # Source code to check
    version: str = "active"
) -> List[Dict[str, Any]]:

    # Build XML with base64-encoded source
    body = self._build_syntax_check_xml(object_uri, include_uri, source, version)

    response = adapter.request(
        uri="/sap/bc/adt/checkruns",
        method="POST",
        params={"reporters": "abapCheckRun"},
        body=body,
        content_type="application/vnd.sap.adt.checkobjects+xml"
    )

    if response.status_code == 200:
        messages = self._parse_syntax_check_result(response.text)
        return messages
    else:
        raise Exception(f"Syntax check failed: {response.status_code}")
```

## Requerimientos de Implementación Java

### 1. Crear método `syntaxCheck()` en `ProgramService.java`

```java
/**
 * Perform ABAP syntax check on source code.
 *
 * This is a REQUIRED step before modifying source code via ADT API.
 * ADT performs syntax validation before allowing PUT operations.
 *
 * @param objectUri   URI of the object (without /source/main)
 * @param sourceUri   URI of the source/include (with /source/main)
 * @param sourceCode  Source code to validate
 * @param version     "active" or "inactive"
 * @return List of syntax check messages (errors, warnings, info)
 */
private List<SyntaxCheckMessage> syntaxCheck(
    String objectUri,
    String sourceUri,
    String sourceCode,
    String version
) {
    // 1. Encode source to Base64
    String base64Source = Base64.getEncoder()
        .encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));

    // 2. Build XML body
    String xmlBody = buildSyntaxCheckXml(objectUri, sourceUri, base64Source, version);

    // 3. Call ADT API
    Map<String, String> params = new HashMap<>();
    params.put("reporters", "abapCheckRun");

    RfcAdapter.RfcResponse response = rfcAdapter.request(
        "/sap/bc/adt/checkruns",
        "POST",
        null,
        params,
        xmlBody,
        "application/vnd.sap.adt.checkobjects+xml"
    );

    // 4. Parse result
    if (response.statusCode() == 200) {
        return parseSyntaxCheckResult(response.text());
    } else {
        throw new RuntimeException("Syntax check failed: HTTP " + response.statusCode());
    }
}
```

### 2. Crear clase `SyntaxCheckMessage`

```java
@Data
public class SyntaxCheckMessage {
    private String type;      // "error", "warning", "info"
    private int line;
    private int column;
    private String text;
    private String severity;
}
```

### 3. Integrar en el flujo de modificación

**Modificar**: `modifyProgramSource()` y `modifyFunctionModuleSource()`

```java
// ========================================
// Step 2: Syntax Check (NEW)
// ========================================
log.info("Step 2/4: Running syntax check...");

List<SyntaxCheckMessage> syntaxMessages = syntaxCheck(
    objectUri,      // Without /source/main
    sourceUri,      // With /source/main
    newSource,
    "inactive"      // Check against inactive version
);

// Check for errors
List<SyntaxCheckMessage> errors = syntaxMessages.stream()
    .filter(msg -> "error".equals(msg.getType()))
    .collect(Collectors.toList());

if (!errors.isEmpty()) {
    String errorMsg = String.format(
        "Syntax check failed with %d error(s)", errors.size()
    );
    log.error(errorMsg);

    for (SyntaxCheckMessage error : errors) {
        log.error("  Line {}: {}", error.getLine(), error.getText());
    }

    throw new RuntimeException(errorMsg);
}

log.info("✓ Syntax check passed");

// ========================================
// Step 3: Modify source code (was Step 2)
// ========================================
log.info("Step 3/4: Modifying source code...");
// ... existing code ...
```

## Casos de Prueba

### Test 1: Syntax Check con código válido

```java
@Test
void testSyntaxCheckValidCode() {
    String validSource = """
        FUNCTION zfi_dmee_bancolombia_r2.
        *"----------------------------------------------------------------------
        *"*"Interfase local:
        *"  IMPORTING
        *"     VALUE(I_TREE_TYPE) TYPE  DMEE_TREETYPE_ABA
        *"----------------------------------------------------------------------

        ENDFUNCTION.
        """;

    List<SyntaxCheckMessage> messages = programService.syntaxCheck(
        "/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r2",
        "/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r2/source/main",
        validSource,
        "active"
    );

    // Should have no errors
    long errors = messages.stream()
        .filter(m -> "error".equals(m.getType()))
        .count();

    assertEquals(0, errors);
}
```

### Test 2: Syntax Check con código inválido

```java
@Test
void testSyntaxCheckInvalidCode() {
    String invalidSource = """
        FUNCTION zfi_dmee_bancolombia_r2.
          INVALID SYNTAX HERE
        ENDFUNCTION.
        """;

    List<SyntaxCheckMessage> messages = programService.syntaxCheck(
        "/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r2",
        "/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r2/source/main",
        invalidSource,
        "active"
    );

    // Should have errors
    long errors = messages.stream()
        .filter(m -> "error".equals(m.getType()))
        .count();

    assertTrue(errors > 0);
}
```

## Impacto

### Archivos a Modificar

1. **`ProgramService.java`**:
   - Agregar método `syntaxCheck()`
   - Agregar método `buildSyntaxCheckXml()`
   - Agregar método `parseSyntaxCheckResult()`
   - Modificar `modifyProgramSource()` (agregar Step 2: Syntax Check)
   - Modificar `modifyFunctionModuleSource()` (agregar Step 2: Syntax Check)

2. **Nueva clase**: `SyntaxCheckMessage.java`
   - DTO para mensajes de syntax check

3. **Tests**: `ProgramServiceTest.java`
   - `testSyntaxCheckValidCode()`
   - `testSyntaxCheckInvalidCode()`
   - `testModifyFunctionModuleWithSyntaxCheck()`

### Nuevo Flujo Completo

```
1. LOCK         → Acquire exclusive lock
2. SYNTAX CHECK → Validate source code (Base64-encoded)
3. MODIFY       → Update source (only if syntax check passes)
4. UNLOCK       → Release lock (always execute)
```

## Criterios de Aceptación

- [ ] Método `syntaxCheck()` implementado en `ProgramService.java`
- [ ] XML correctamente formado con source en Base64
- [ ] Parseo correcto del response XML
- [ ] Integrado en `modifyProgramSource()`
- [ ] Integrado en `modifyFunctionModuleSource()`
- [ ] Tests unitarios implementados
- [ ] Test de integración con SAP funciona
- [ ] Documentación JavaDoc completa
- [ ] Logs informativos en cada paso

## Referencias

- **Python Implementation**: `python-legacy/app/services/code_quality_service.py`
- **Python Tests**: `python-legacy/app/tests/test_modification_workflow.py`
- **ADT API Docs**: SAP ADT REST API - Check Runs Endpoint

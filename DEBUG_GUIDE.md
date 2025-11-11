# Guía de Debug para search_objects en VS Code

## Objetivo
Debuggear `SearchService.searchObjects()` para entender por qué no está devolviendo resultados para la query `ZFIAAC002*`.

---

## Paso 1: Preparar el entorno

### 1.1 Asegurar que Maven compile el proyecto
```bash
mvn clean compile test-compile
```

### 1.2 Verificar variables de entorno SAP
Asegúrate de que tienes las variables configuradas en tu shell:
```bash
echo $SAP_ASHOST
echo $SAP_USER
# etc.
```

---

## Paso 2: Configurar Breakpoints

### 2.1 Abrir archivos clave
1. `SearchService.java` (línea 70 - método `searchObjects`)
2. `SearchService.java` (línea 100 - donde se parsea el response)
3. `SearchService.java` (línea 156 - método `parseSearchResults`)
4. `RfcAdapter.java` (para ver el request/response real)

### 2.2 Colocar breakpoints estratégicos

**En `SearchService.java`:**
- ✅ Línea 86: Antes de llamar a `rfcAdapter.request()`
- ✅ Línea 100: Después de recibir el response (HTTP 200)
- ✅ Línea 102: Para ver el XML crudo
- ✅ Línea 106: Después de parsear los resultados
- ✅ Línea 156: En `parseSearchResults` para ver si entra al parsing
- ✅ Línea 158: En el loop que procesa cada `objectReference`

**En `RfcAdapter.java` (opcional pero recomendado):**
- ✅ Donde se hace el HTTP request al SAP
- ✅ Donde se lee el HTTP response

---

## Paso 3: Ejecutar el Debug

### Opción A: Desde VS Code UI

1. **Abrir el test:**
   - Abre `SearchServiceTest.java`
   - Ve al método `testSearchObjects_SpecificProgram()`

2. **Iniciar debug:**
   - Presiona `F5` o
   - Click en "Run and Debug" (panel izquierdo)
   - Selecciona "Debug SearchServiceTest"
   - Click en el botón verde de play

3. **Alternativamente** (más fácil):
   - En `SearchServiceTest.java`, click derecho en el método `testSearchObjects_SpecificProgram`
   - Selecciona "Debug Test"

### Opción B: Desde terminal con Maven

```bash
mvn test -Dtest=SearchServiceTest#testSearchObjects_SpecificProgram -Dmaven.surefire.debug
```

Luego conecta el debugger de VS Code al puerto 5005.

---

## Paso 4: Puntos de Inspección Durante el Debug

### 4.1 En `searchObjects()` (línea 86)
**Inspeccionar:**
```
query = "ZFIAAC002*"
actualMaxResults = 50
uri = "/sap/bc/adt/repository/informationsystem/search"
params = {
    "operation": "quickSearch",
    "query": "ZFIAAC002*",
    "maxResults": "50"
}
```

**Preguntas:**
- ¿El URI está correcto?
- ¿Los parámetros están bien formados?
- ¿El query tiene el asterisco correctamente?

### 4.2 En el response (línea 100)
**Inspeccionar:**
```
response.statusCode() = 200?
response.text() = ¿Contiene XML válido?
```

**Acción clave:**
- Copia el `response.text()` completo
- Pegarlo en un editor para ver el XML
- Compara con tu ejemplo exitoso que compartiste antes

### 4.3 En parseSearchResults (línea 156)
**Inspeccionar:**
```
xmlText = ¿El XML recibido?
objectRefs = ¿Cuántos elementos encontró?
```

**Puntos críticos:**
- ¿`getElementsByTagNameNS("*", "objectReference")` encuentra elementos?
- ¿El namespace está bien manejado?

### 4.4 En el loop de parsing (línea 158)
**Inspeccionar cada iteración:**
```
objRef = elemento actual
name = ¿Qué valor tiene?
type = ¿Qué valor tiene?
uri = ¿Qué valor tiene?
```

---

## Paso 5: Escenarios Posibles y Soluciones

### Escenario 1: `response.statusCode() != 200`
**Causa:** Error en la conexión o autenticación SAP
**Solución:** Verificar credenciales SAP y conectividad

### Escenario 2: `response.text()` está vacío
**Causa:** ADT API no devuelve datos
**Solución:** Verificar que ADT esté instalado en SAP

### Escenario 3: XML válido pero `objectRefs.getLength() == 0`
**Causa:** Problema con namespaces XML
**Investigar:**
- ¿El XML usa namespaces diferentes a los esperados?
- Comparar con tu XML de ejemplo exitoso

### Escenario 4: XML contiene datos pero parsing falla
**Causa:** Atributos en namespace diferente
**Solución:** Verificar que `ADTCORE_NS = "http://www.sap.com/adt/core"` sea correcto

### Escenario 5: Parsing exitoso pero lista vacía
**Causa:** Los objetos no cumplen con el filtro
**Investigar:** ¿Qué objetos se encontraron realmente?

---

## Paso 6: Comparación con Request Exitoso

Tu request exitoso fue:
```
GET /sap/bc/adt/repository/informationsystem/search?operation=quickSearch&query=ZFIAAC002*&maxResults=51
Response: 8 objetos encontrados
```

**Verificar en el debug:**
1. ¿El URI generado es idéntico?
2. ¿Los parámetros se están enviando correctamente?
3. ¿El XML response es similar al que compartiste?

---

## Paso 7: Logging Adicional (si necesitas más info)

Si el debug no es suficiente, agrega logging temporal:

```java
// En SearchService.java línea 100
log.info("===== RAW XML RESPONSE =====");
log.info(response.text());
log.info("===== END XML =====");
```

```java
// En parseSearchResults línea 156
log.info("Parsing XML, length: {}", xmlText.length());
log.info("Found {} objectReference elements", objectRefs.getLength());
```

---

## Paso 8: Verificación Final

Después del debug, deberías saber:
1. ✅ ¿El request se envía correctamente?
2. ✅ ¿El response HTTP es 200?
3. ✅ ¿El XML contiene datos?
4. ✅ ¿El parsing extrae los objetos?
5. ✅ ¿Por qué la lista final está vacía?

---

## Comandos Útiles

### Limpiar y recompilar
```bash
mvn clean compile test-compile
```

### Ver logs de test
```bash
tail -f logs/sap-mcp-server-test.log
```

### Ejecutar test sin debug
```bash
mvn test -Dtest=SearchServiceTest#testSearchObjects_SpecificProgram
```

---

## Próximos Pasos Después del Debug

Una vez identifiques el problema:
1. Documenta el root cause
2. Implementa el fix
3. Re-ejecuta el test
4. Valida con otras queries

---

## Notas Importantes

- **No modifiques código durante el debug**, solo observa
- **Anota todos los valores** que veas en las variables
- **Compara** con el XML de respuesta exitoso que compartiste
- Si encuentras el problema, **documéntalo** antes de fix

---

## Contacto para Resultados

Cuando termines el debug, comparte:
1. ¿Qué valor tiene `response.text()`?
2. ¿Cuántos elementos encuentra `objectRefs.getLength()`?
3. ¿Cuál es el root cause identificado?

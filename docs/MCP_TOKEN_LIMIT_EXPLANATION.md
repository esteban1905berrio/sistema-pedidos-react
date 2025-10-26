# MCP Token Limit: Quién Lo Impone y Por Qué

## Respuesta Directa

**El límite de 25,000 tokens es impuesto por CLAUDE CODE**, no por el framework MCP ni por Claude AI (el modelo).

---

## Desglose de Responsabilidades

### 1. **Claude AI (el modelo)** 🤖
- **Límite de salida**: 8,192 tokens (Claude 3.5 Sonnet)
- **Límite de contexto**: 200,000 tokens (entrada)
- **NO impone** el límite de 25,000 tokens en respuestas MCP

### 2. **MCP Protocol (especificación)** 📋
- **El protocolo MCP NO especifica** un límite de tokens para respuestas de herramientas
- MCP es agnóstico al tamaño de respuesta
- Es responsabilidad del **cliente MCP** (Claude Code) decidir cómo manejar respuestas grandes

### 3. **Claude Code (cliente MCP)** ⚙️
- **SÍ impone** el límite de 25,000 tokens
- **Es configurable** mediante variable de entorno
- **Razón**: Evitar saturar el contexto de conversación con respuestas masivas de herramientas

---

## Configuración de Claude Code

### Límites por Defecto

```
Warning threshold:  10,000 tokens  (muestra advertencia)
Maximum allowed:    25,000 tokens  (rechaza la respuesta)
```

### Cómo Cambiar el Límite

```bash
export MAX_MCP_OUTPUT_TOKENS=50000
claude
```

O en tu `.mcp.json`:

```json
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": ".venv/bin/python",
      "args": ["-m", "app.main"],
      "env": {
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "MAX_MCP_OUTPUT_TOKENS": "50000"
      }
    }
  }
}
```

---

## Por Qué Claude Code Impone Este Límite

### Razones de Diseño

1. **Protección del contexto de conversación**
   - Las respuestas MCP consumen tokens del contexto total (200k)
   - Una respuesta de 100k tokens dejaría solo 100k para el resto de la conversación

2. **Performance y latencia**
   - Respuestas grandes aumentan el tiempo de procesamiento
   - El modelo debe leer y procesar toda la respuesta

3. **User Experience**
   - Respuestas enormes pueden ser abrumadoras
   - Mejor UX con respuestas paginadas o filtradas

4. **Costos de API**
   - Cada token procesado tiene un costo
   - Limitar respuestas ayuda a controlar costos

### Cita Oficial (Documentación Claude Code)

> "These limits prevent overwhelming your conversation context with excessive tool responses."
>
> "If you frequently encounter output warnings with specific MCP servers, consider increasing the limit **or configuring the server to paginate or filter its responses**."

---

## Comparación: Límite vs Capacidad del Modelo

| Componente | Límite | Razón |
|------------|--------|-------|
| **Claude AI (salida)** | 8,192 tokens | Límite del modelo |
| **Claude AI (contexto)** | 200,000 tokens | Capacidad total |
| **Claude Code MCP** | 25,000 tokens (default) | Proteger UX y contexto |
| **Respuesta ZCLCXR1002_UTIL** | 39,713 tokens | ❌ Excede límite Claude Code |

---

## Implicaciones para Nuestro MCP Server

### Situación Actual

```
Clase ZCLCXR1002_UTIL:
  - Código fuente: 98,554 chars = ~24,638 tokens
  - Metadata MCP: ~2,000 chars = ~500 tokens
  - MCP wrapper: ~60,000 chars = ~15,000 tokens
  ─────────────────────────────────────────────
  TOTAL RESPUESTA: ~160,554 chars = ~39,713 tokens ❌
```

### El Problema NO es SAP ni nuestro código

- ✅ El RFC call es rápido (0.47s)
- ✅ El código fuente es razonable (24,638 tokens)
- ❌ El **wrapper de Claude Code** agrega 15,000 tokens extras
- ❌ El límite de Claude Code (25,000) es muy estricto

### Soluciones Posibles

#### Opción 1: Aumentar el límite (temporal)
```bash
export MAX_MCP_OUTPUT_TOKENS=50000
```
**Pros**: Solución rápida
**Contras**: No resuelve el problema de fondo

#### Opción 2: Implementar paginación (recomendado)
```python
def get_class_source(
    class_name: str,
    offset: int = 0,
    limit: int = 1000  # líneas
) -> Dict[str, Any]:
    # Retornar solo un fragmento del código
    ...
```
**Pros**: Respeta el límite, mejor UX
**Contras**: Requiere múltiples llamadas

#### Opción 3: Truncar internamente (actual)
```python
CHARACTER_LIMIT = 25000  # En response_formatter.py
```
**Pros**: Ya implementado
**Contras**: Se pierde código, no es ideal

#### Opción 4: Usar get_class_structure primero
```python
# 1. Obtener estructura (pequeña)
structure = get_class_structure("ZCLCXR1002_UTIL")

# 2. Usuario elige qué método ver
source = get_method_source("ZCLCXR1002_UTIL", "ENVIAR_CORREO")
```
**Pros**: Mejor UX, respuestas pequeñas
**Contras**: Requiere nueva herramienta

---

## Conclusión

### Quién Impone el Límite

```
┌─────────────────────────────────────────────┐
│  CLAUDE CODE (cliente MCP)                  │
│  Límite: 25,000 tokens (configurable)      │
│  Variable: MAX_MCP_OUTPUT_TOKENS            │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  MCP PROTOCOL                               │
│  NO impone límites                          │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  MCP SERVER (nuestro código)                │
│  Retorna 39,713 tokens                      │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  CLAUDE AI (modelo)                         │
│  Contexto: 200,000 tokens                   │
│  Salida: 8,192 tokens                       │
└─────────────────────────────────────────────┘
```

### Recomendación

**NO aumentar el límite indefinidamente**. En su lugar:

1. ✅ Implementar paginación en el servidor MCP
2. ✅ Agregar filtros y búsqueda específica
3. ✅ Ofrecer `get_method_source` para clases grandes
4. ✅ Usar `CHARACTER_LIMIT` como respaldo

El límite de Claude Code existe por buenas razones (UX, performance, costos). Debemos **adaptarnos a él**, no evitarlo.

---

## Referencias

- [Claude Code MCP Documentation](https://docs.claude.com/en/docs/claude-code/mcp.md)
- [GitHub Issue #4002](https://github.com/anthropics/claude-code/issues/4002)
- [Stack Overflow: MCP Token Limits](https://stackoverflow.com/questions/79699282/how-to-handle-token-limit-when-processing-large-json-response-with-mcp-client-se)
- [DEV Community: Solving 25k Token Wall](https://dev.to/swapnilsurdi/solving-ais-25000-token-wall-introducing-mcp-cache-1fie)

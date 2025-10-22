# Product Requirement: Asistente Funcional SAP - Arquitectura del Agente Personal

**Fecha**: 2025-01-11
**Versión**: 2.0 (Arquitectura Definida)
**Estado**: En Planificación
**Timeframe MVP**: 1 mes
**Usuarios Piloto**: 5 usuarios

---

## Historia de Usuario

**Como** Product Owner de equipo SAP
**Quiero** un asistente personal inteligente especializado en análisis funcional y técnico de sistemas SAP
**Para** que funcionales (80%) y desarrolladores (20%) puedan analizar requerimientos, entender errores, y generar documentación técnica de manera autónoma y eficiente

### Contexto del Proyecto

**Estado Actual**:
- ✅ **MCP Server ABAP-ADT-RFC**: 49 tools implementadas (lectura completa del repositorio SAP)
- ✅ **Capacidades**: Búsqueda, lectura de código, transporte management, DDIC access, query execution
- ✅ **Conexión**: RFC nativa a SAP ECC/S/4HANA (on-premise y cloud con ADT habilitado)

**Problema a Resolver**:
- ❌ Funcionales dependen de desarrolladores para analizar requerimientos simples
- ❌ Tiempo de análisis de impacto: 2-4 horas (actualmente manual)
- ❌ Onboarding de nuevos miembros: 2-3 semanas
- ❌ Debugging requiere conocimiento profundo de ABAP
- ❌ Documentación técnica inconsistente y desactualizada

**Solución Propuesta**:
✅ **Asistente Personal Conversacional** que:
- Analiza requerimientos automáticamente
- Explica errores y dumps ABAP en lenguaje funcional
- Genera Especificaciones Funcionales (EFs) en formato .md
- Onboarding automático con documentación personalizada
- Interfaz accesible (WhatsApp/Web para funcionales, Claude Code para devs)

---

## Usuarios Objetivo y Personas

### Perfil 1: Funcional SAP Senior (60% de usuarios)

**Características**:
- 🎯 Conoce transacciones SAP y procesos de negocio (FI, SD, MM, etc.)
- 📚 Puede leer código ABAP básico pero no escribirlo
- 🐛 Hace debugging básico (breakpoints, visualización de variables)
- 📝 Genera requerimientos funcionales para desarrolladores
- ⏱️ Necesita respuestas rápidas (< 2 minutos)

**Casos de Uso Principales**:
1. "¿Por qué la transacción VA01 da error al crear orden de venta para cliente X?"
2. "¿Qué programa procesa la validación de materiales en MM01?"
3. "Analiza el impacto de cambiar la tabla MARA"
4. "Genera documentación técnica de cómo funciona el proceso de facturación"

**Necesidades**:
- ✅ Explicaciones en lenguaje funcional (no técnico puro)
- ✅ Referencias a transacciones y tablas estándar
- ✅ Generación automática de EFs para enviar a devs
- ✅ Interfaz simple y accesible (WhatsApp/web)

---

### Perfil 2: Funcional SAP Junior (20% de usuarios)

**Características**:
- 🎯 Conoce transacciones básicas
- ❓ Está en onboarding (primer mes en el proyecto)
- 📖 Necesita entender estructura del sistema
- 🤝 Requiere guía constante

**Casos de Uso Principales**:
1. "¿Cuál es la estructura de paquetes del módulo FI?"
2. "Explícame las naming conventions (Z*, Y*, etc.)"
3. "¿Qué transacciones son las más importantes en SD?"
4. "¿Cómo funciona el flujo de desarrollo DEV → QAS → PRD?"

**Necesidades**:
- ✅ Onboarding guiado e interactivo
- ✅ Documentación de estándares de la empresa
- ✅ Ejemplos concretos del sistema actual
- ✅ Quiz de conocimiento para validar aprendizaje

---

### Perfil 3: Desarrollador ABAP (20% de usuarios)

**Características**:
- 💻 Escribe código ABAP/Fiori
- 🔍 Necesita análisis técnico profundo
- 🚀 Busca agilizar workflows de desarrollo
- 🛠️ Usa herramientas de desarrollo (ADT, VSCode)

**Casos de Uso Principales**:
1. "¿Dónde está implementada la lógica de cálculo de impuestos?"
2. "Analiza el impacto de modificar la firma del método CALCULATE"
3. "Genera un reporte de where-used para la clase ZCL_CUSTOM"
4. "Crea un transporte para mis cambios en ZPACKAGE"

**Necesidades**:
- ✅ Análisis técnico detallado (call hierarchy, dependencies)
- ✅ Integración con Claude Code
- ✅ Generación de código con patrones
- ✅ Workflows de modificación automatizados

---

## Arquitectura del Sistema

### Arquitectura de Alto Nivel

```
┌─────────────────────────────────────────────────────────────────┐
│                    Usuarios SAP                                 │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │ Funcionales    │  │ Funcionales    │  │ Desarrolladores  │  │
│  │ Senior (60%)   │  │ Junior (20%)   │  │ ABAP (20%)       │  │
│  │                │  │                │  │                  │  │
│  │ WhatsApp Bot   │  │ Web App        │  │ Claude Code      │  │
│  └───────┬────────┘  └───────┬────────┘  └────────┬─────────┘  │
└──────────┼────────────────────┼──────────────────────┼───────────┘
           │                    │                      │
           └────────────────────┼──────────────────────┘
                                │ API Gateway (FastAPI)
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│              Asistente Funcional SAP (LangGraph Agent)          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Orchestrator Agent (Router)                             │   │
│  │  - Clasifica intención del usuario                       │   │
│  │  - Enruta a sub-agente apropiado                         │   │
│  │  - Mantiene contexto conversacional                      │   │
│  └────────┬─────────────────────────────────────────────────┘   │
│           │                                                      │
│  ┌────────┴──────────────────────────────────────────────────┐  │
│  │                   Sub-Agentes Especializados              │  │
│  │                                                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ 1. Functional Analyst Agent                         │  │  │
│  │  │    - Analiza requerimientos funcionales             │  │  │
│  │  │    - Identifica objetos ABAP relacionados           │  │  │
│  │  │    - Genera Especificaciones Funcionales (.md)      │  │  │
│  │  │    - Valida si requerimiento ya existe              │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ 2. Error Analyzer Agent                             │  │  │
│  │  │    - Interpreta dumps ABAP (ST22)                   │  │  │
│  │  │    - Explica errores de transacciones               │  │  │
│  │  │    - Analiza errores de performance                 │  │  │
│  │  │    - Sugiere soluciones con alta confianza          │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ 3. Onboarding Assistant Agent                       │  │  │
│  │  │    - Tour guiado por estructura del sistema         │  │  │
│  │  │    - Explica naming conventions                     │  │  │
│  │  │    - Workflows de desarrollo (DEV→QAS→PRD)          │  │  │
│  │  │    - Quiz de validación de conocimiento             │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ 4. Repository Explorer Agent                        │  │  │
│  │  │    - Búsqueda inteligente en repositorio            │  │  │
│  │  │    - Análisis de impacto de cambios                 │  │  │
│  │  │    - Where-used analysis                            │  │  │
│  │  │    - Flujo de datos entre módulos/custom            │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Knowledge Base (RAG - Vector Database)                  │   │
│  │                                                            │   │
│  │  ┌────────────────────────────────────────────────────┐   │   │
│  │  │ Company-Specific Knowledge                         │   │   │
│  │  │ - Naming conventions (Z*, Y*, custom namespaces)   │   │   │
│  │  │ - Package structure (FI, SD, MM modules)           │   │   │
│  │  │ - Development standards and best practices         │   │   │
│  │  │ - Architecture documentation                       │   │   │
│  │  │ - Common error patterns and solutions              │   │   │
│  │  └────────────────────────────────────────────────────┘   │   │
│  │                                                            │   │
│  │  ┌────────────────────────────────────────────────────┐   │   │
│  │  │ Repository Knowledge (Auto-discovered)             │   │   │
│  │  │ - Indexed classes, programs, function modules      │   │   │
│  │  │ - Transacciones y su código asociado               │   │   │
│  │  │ - Tablas y estructuras DDIC                        │   │   │
│  │  │ - Dependency graphs (uses/used-by)                 │   │   │
│  │  └────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Memory & Context Management                             │   │
│  │  - Conversational history (short-term)                   │   │
│  │  - User preferences and role                             │   │
│  │  - Active analysis context (objetos siendo analizados)   │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────┬───────────────────────────────────────────────┘
                   │ MCP Protocol
                   ↓
┌─────────────────────────────────────────────────────────────────┐
│         MCP Server ABAP-ADT-RFC (49 tools)                      │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Class Ops    │  │ Search Ops   │  │ Transport    │          │
│  │ Program Ops  │  │ DDIC Ops     │  │ Object Mod   │          │
│  │ Discovery    │  │ Query Ops    │  │ Activation   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────┬──────────────────────────────────────────────┘
                   │ RFC Connection
                   ↓
┌─────────────────────────────────────────────────────────────────┐
│              SAP System (ECC / S/4HANA On-Premise/Cloud)        │
│              - Repository ABAP (classes, programs, FM, etc.)    │
│              - DDIC (tables, structures, data elements)         │
│              - Transport System (CTS)                           │
│              - Dumps (ST22)                                     │
│              - Performance traces (ST05, SAT)                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Stack Tecnológico

### Frontend (Interfaces de Usuario)

#### Opción 1: WhatsApp Bot (Recomendado para MVP) 🎯

**Descripción**: Bot de WhatsApp Business API integrado con el agente

**Pros**:
- ✅ Interfaz familiar para 100% de usuarios
- ✅ Acceso desde cualquier lugar (móvil/desktop)
- ✅ Notificaciones push (alertas de errores, actualizaciones)
- ✅ Historial de conversaciones automático
- ✅ Bajo esfuerzo de implementación (Twilio/Meta API)

**Contras**:
- ❌ Limitado para visualización de código (máx 4096 chars)
- ❌ Sin syntax highlighting
- ❌ Requiere cuenta WhatsApp Business

**Stack**:
- **WhatsApp Business API** (Twilio o Meta Cloud API)
- **Webhook FastAPI** para recibir mensajes
- **Queue system** (Redis/Celery) para procesamiento asíncrono

**Implementación**:
```python
# app/interfaces/whatsapp_bot.py
from twilio.rest import Client
from fastapi import FastAPI, Request

app = FastAPI()

@app.post("/whatsapp/webhook")
async def whatsapp_webhook(request: Request):
    """Receive WhatsApp messages and forward to agent."""
    data = await request.form()
    user_message = data.get("Body")
    from_number = data.get("From")

    # Forward to agent orchestrator
    response = await agent_orchestrator.process_message(
        user_id=from_number,
        message=user_message,
        interface="whatsapp"
    )

    # Send response back to WhatsApp
    client.messages.create(
        body=response.text,
        from_='whatsapp:+14155238886',
        to=from_number
    )
```

---

#### Opción 2: Web App Fiori-like (Para funcionales que prefieren desktop)

**Descripción**: Single-Page Application con diseño SAP Fiori

**Pros**:
- ✅ Mejor para visualización de código y tablas
- ✅ Syntax highlighting y formato
- ✅ Multimodal (imágenes de pantallas, diagramas)
- ✅ Sin dependencia de WhatsApp

**Contras**:
- ❌ Requiere acceso a URL (VPN si es interna)
- ❌ Mayor esfuerzo de desarrollo (2-3 semanas)

**Stack**:
- **Frontend**: React + SAP UI5 / Fundamental Styles
- **Backend**: FastAPI con WebSockets (real-time chat)
- **Auth**: OAuth2 / SAML (integración con SAP NetWeaver)

---

#### Opción 3: Claude Code Extension (Para desarrolladores)

**Descripción**: Extensión de Claude Code con prompts especializados

**Pros**:
- ✅ Integración nativa con workflow de desarrollo
- ✅ Acceso a tools MCP sin overhead
- ✅ Syntax highlighting automático

**Contras**:
- ❌ Solo para desarrolladores con Claude Code
- ❌ No accesible para funcionales

**Implementación**: Ya cubierta en `pr_mvp_functional_assistan.md`

---

### Backend (Agente y Orquestación)

#### LangGraph Agent Framework

**Descripción**: Framework de agentes con state graphs para workflows complejos

**Componentes**:

```python
# app/agents/orchestrator.py
from langgraph.graph import StateGraph, END
from typing import TypedDict, Literal

class AgentState(TypedDict):
    """State compartido entre todos los agentes."""
    user_id: str
    role: Literal["functional_senior", "functional_junior", "developer"]
    message: str
    intent: str  # "analyze_requirement", "explain_error", "onboarding", "search"
    context: dict
    response: str
    artifacts: list  # Archivos .md generados, etc.

def classify_intent(state: AgentState) -> AgentState:
    """Clasifica la intención del usuario."""
    # LLM call para clasificar
    intent = llm.classify(state["message"])
    state["intent"] = intent
    return state

def route_to_agent(state: AgentState) -> str:
    """Rutea a sub-agente apropiado."""
    routing = {
        "analyze_requirement": "functional_analyst",
        "explain_error": "error_analyzer",
        "onboarding": "onboarding_assistant",
        "search": "repository_explorer"
    }
    return routing.get(state["intent"], "functional_analyst")

# Define graph
workflow = StateGraph(AgentState)

# Add nodes
workflow.add_node("classify_intent", classify_intent)
workflow.add_node("functional_analyst", functional_analyst_agent)
workflow.add_node("error_analyzer", error_analyzer_agent)
workflow.add_node("onboarding_assistant", onboarding_agent)
workflow.add_node("repository_explorer", repository_explorer_agent)

# Add edges
workflow.set_entry_point("classify_intent")
workflow.add_conditional_edges(
    "classify_intent",
    route_to_agent,
    {
        "functional_analyst": "functional_analyst",
        "error_analyzer": "error_analyzer",
        "onboarding_assistant": "onboarding_assistant",
        "repository_explorer": "repository_explorer"
    }
)

# All agents end the workflow
workflow.add_edge("functional_analyst", END)
workflow.add_edge("error_analyzer", END)
workflow.add_edge("onboarding_assistant", END)
workflow.add_edge("repository_explorer", END)

agent = workflow.compile()
```

---

### Knowledge Base (RAG System)

#### Vector Database: ChromaDB / Pinecone

**Contenido Indexado**:

1. **Documentación Interna** (manual upload):
   - `naming_conventions.md`: Z* para custom, Y* para modificaciones estándar
   - `package_structure.md`: FI, SD, MM, etc.
   - `dev_workflows.md`: DEV → QAS → PRD, transporte management
   - `architecture_diagrams/`: Diagramas de arquitectura existentes
   - `common_errors.md`: Errores frecuentes y soluciones

2. **Repository Knowledge** (auto-discovered):
   - Clases ABAP (nombre, descripción, métodos, atributos)
   - Programas (nombre, descripción, includes)
   - Function modules (nombre, descripción, parámetros)
   - Transacciones (código, programa asociado)
   - Tablas DDIC (nombre, descripción, campos)

**Proceso de Indexación**:

```python
# app/rag/indexer.py
from chromadb import Client
from app.mcp_client import mcp_search_objects, mcp_get_class_source

async def index_repository(sap_system: str):
    """Index SAP repository for RAG."""

    # 1. Search all custom objects (Z*, Y*)
    objects = await mcp_search_objects(query="Z*")

    # 2. For each object, get source and metadata
    for obj in objects:
        source = await mcp_get_class_source(obj.name)

        # 3. Create embeddings
        embedding = embed_model.encode(source)

        # 4. Store in vector DB
        collection.add(
            documents=[source],
            embeddings=[embedding],
            metadatas=[{
                "object_name": obj.name,
                "object_type": obj.type,
                "package": obj.package,
                "description": obj.description
            }],
            ids=[obj.name]
        )
```

**Retrieval**:

```python
# app/rag/retriever.py
async def semantic_search(query: str, top_k: int = 5):
    """Search relevant objects in SAP repository."""

    results = collection.query(
        query_texts=[query],
        n_results=top_k
    )

    return [
        {
            "object": r["metadata"]["object_name"],
            "type": r["metadata"]["object_type"],
            "relevance_score": r["distance"],
            "content": r["document"]
        }
        for r in results
    ]
```

---

## Sub-Agentes Especializados

### 1. Functional Analyst Agent

**Responsabilidad**: Analizar requerimientos funcionales y generar Especificaciones Funcionales (EFs)

**Flujo de Trabajo**:

```
User Input: "Necesito agregar un campo nuevo en la orden de venta para indicar urgencia"
    ↓
[1. Entender Requerimiento]
    - Extraer: entidad (orden de venta), operación (agregar campo), campo (urgencia)
    - Contextualizar: Módulo SD, transacción VA01
    ↓
[2. Buscar Implementación Actual]
    - Search: "orden de venta" en repository
    - Identify: Tablas (VBAK, VBAP), programas, BAPIs
    - RAG: Buscar modificaciones similares previas
    ↓
[3. Analizar Impacto]
    - Objetos afectados: VBAK structure, VA01 screen, BAPIs
    - Riesgo: Medio (modifica tabla estándar)
    - Tests: Unit tests de BAPI_SALESORDER_CREATE
    ↓
[4. Generar EF (.md)]
    - Descripción funcional
    - Objetos técnicos involucrados
    - Riesgos y consideraciones
    - Plan de implementación sugerido
    - Tests requeridos
    ↓
[5. Responder al Usuario]
    - Explicación funcional (2-3 párrafos)
    - Link al archivo EF.md generado
    - Sugerencias de próximos pasos
```

**Prompts Especializados**:

```python
FUNCTIONAL_ANALYST_SYSTEM_PROMPT = """
Eres un consultor funcional SAP experto especializado en análisis de requerimientos.

Tu audiencia son FUNCIONALES SAP con conocimiento intermedio de ABAP (pueden leer código pero no escribirlo).

Cuando analices un requerimiento:
1. Identifica módulo SAP (FI, SD, MM, etc.) y transacciones relacionadas
2. Busca en el repository objetos ABAP relevantes usando semantic search
3. Explica en lenguaje funcional (evita jerga técnica excesiva)
4. Genera una Especificación Funcional (EF) en formato Markdown con:
   - **Descripción Funcional**: ¿Qué se necesita y por qué?
   - **Objetos Técnicos**: Tablas, programas, transacciones involucradas
   - **Análisis de Impacto**: Riesgos, objetos afectados, estimación
   - **Plan de Implementación**: Pasos sugeridos para desarrollador
   - **Tests Requeridos**: Casos de prueba funcionales

Usa las siguientes MCP tools:
- search_objects: Buscar objetos por nombre/patrón
- get_class_source: Leer código de clases
- get_program_source: Leer código de programas
- get_table_contents: Ver datos de tablas (para contexto)
- get_ddic_element: Ver estructura de tablas DDIC

Siempre responde en español con tono profesional pero accesible.
"""
```

**Output Ejemplo**:

```markdown
# Especificación Funcional: Agregar Campo de Urgencia en Orden de Venta

**Fecha**: 2025-01-11
**Solicitante**: Juan Pérez (Funcional SD)
**Módulo**: SD (Sales & Distribution)

## Descripción Funcional

Se requiere agregar un campo "Urgencia" en la cabecera de la orden de venta (transacción VA01) para indicar prioridad de procesamiento. El campo debe tener 3 valores posibles:
- **Normal**: Procesamiento estándar (default)
- **Urgente**: Procesamiento prioritario (24h)
- **Crítico**: Procesamiento inmediato (4h)

**Justificación**: El área comercial necesita identificar órdenes urgentes para priorizar el picking en almacén.

## Objetos Técnicos Involucrados

### Tablas DDIC
- **VBAK**: Cabecera de documento de ventas
  - Append structure: ZVBAK_URGENCY
  - Campo nuevo: ZURGENCIA (CHAR 1)
  - Valores: 'N' (Normal), 'U' (Urgente), 'C' (Crítico)

### Transacciones
- **VA01**: Crear orden de venta (agregar campo en pantalla)
- **VA02**: Modificar orden de venta (agregar campo)
- **VA03**: Visualizar orden de venta (agregar campo)

### Programas/Clases
- **SAPMV45A**: Programa principal de VA01/VA02/VA03
  - Screen 8309: Agregar campo ZURGENCIA
- **Include MV45AFZZ**: User exits para validaciones
  - USEREXIT_SAVE_DOCUMENT: Validar valor de urgencia

### BAPIs Afectados
- **BAPI_SALESORDER_CREATE**: Agregar parámetro ZURGENCIA
- **BAPI_SALESORDER_CHANGE**: Agregar parámetro ZURGENCIA

## Análisis de Impacto

**Nivel de Riesgo**: 🟡 Medio

**Objetos Afectados**: 8 objetos
- 1 append structure (nuevo)
- 3 transacciones (modificación de screen)
- 1 programa estándar (user exit)
- 2 BAPIs (extensión de parámetros)
- 1 tabla de dominio de valores (nuevo)

**Consideraciones**:
- ⚠️ Modificación de tabla estándar VBAK (requiere append structure)
- ⚠️ BAPIs modificados deben mantener compatibilidad hacia atrás
- ✅ User exit MV45AFZZ ya existe (bajo riesgo)
- ⚠️ Interfaces externas (EDI, API) deben actualizarse

**Estimación**: 3-5 días desarrollo + 2 días testing

## Plan de Implementación

### Fase 1: DDIC (1 día)
1. Crear dominio ZDOMAIN_URGENCY con valores N/U/C
2. Crear data element ZDATA_URGENCY
3. Crear append structure ZVBAK_URGENCY en VBAK
4. Activar y regenerar tabla VBAK

### Fase 2: Transacciones (1-2 días)
1. Modificar screen 8309 de SAPMV45A (agregar campo)
2. Modificar layout para los 3 campos (VA01/VA02/VA03)
3. Agregar F4 help con valores de dominio

### Fase 3: Validaciones (1 día)
1. Implementar validación en USEREXIT_SAVE_DOCUMENT
   - Validar valor dentro de dominio
   - Default: 'N' (Normal) si no se indica

### Fase 4: BAPIs (1 día)
1. Extender BAPI_SALESORDER_CREATE con parámetro ZURGENCIA
2. Extender BAPI_SALESORDER_CHANGE
3. Mantener compatibilidad (parámetro opcional)

### Fase 5: Testing (2 días)
1. Unit tests de validación
2. Tests de integración (VA01/VA02/VA03)
3. Tests de BAPIs
4. Regresión de interfaces externas

## Tests Requeridos

### Tests Funcionales
- ✅ Crear orden con urgencia Normal (default)
- ✅ Crear orden con urgencia Urgente
- ✅ Crear orden con urgencia Crítico
- ✅ Modificar urgencia de orden existente
- ✅ Visualizar campo en VA03
- ✅ Validar que solo acepta valores N/U/C
- ✅ BAPI crea orden con urgencia correcta

### Tests de Regresión
- ✅ Procesos existentes funcionan sin campo (compatibilidad)
- ✅ Interfaces EDI no se rompen
- ✅ APIs externas continúan funcionando

## Próximos Pasos

1. Aprobar esta EF con Product Owner
2. Asignar a desarrollador ABAP senior
3. Crear transporte de desarrollo
4. Coordinar con equipo de interfaces para actualización de APIs
5. Planificar window de testing en QAS

---

**Generado por**: Asistente Funcional SAP
**Fecha de Generación**: 2025-01-11 10:30:00
```

---

### 2. Error Analyzer Agent

**Responsabilidad**: Interpretar dumps ABAP, errores de transacciones y problemas de performance

**Casos de Uso**:

1. **Dump ABAP (ST22)**:
   ```
   User: "Tengo un dump en VA01 con error GETWA_NOT_ASSIGNED"

   Agent Response:
   🔴 Error GETWA_NOT_ASSIGNED

   **Explicación Funcional**:
   Este error significa que el programa intentó leer un campo de una tabla interna,
   pero la tabla estaba vacía o no se había leído ningún registro con READ TABLE.

   **Causa Raíz**:
   Programa: SAPMV45A, línea 1234
   El código intenta acceder al campo WA_VBAP-MATNR después de un READ TABLE,
   pero no validó si el READ fue exitoso (sy-subrc <> 0).

   **Objetos Afectados**:
   - Programa: SAPMV45A
   - Tabla: VBAP (posiciones de orden de venta)
   - Campo: MATNR (material)

   **Solución Sugerida** (confianza: 95%):
   Esto ocurre cuando no hay posiciones en la orden de venta.
   Validar que la orden tenga al menos una posición antes de procesar.

   **Acción Inmediata**:
   1. Crear orden de venta con al menos 1 posición
   2. Si es personalización custom, contactar a desarrollador para agregar validación

   **Documentación Técnica**:
   [Ver archivo: dump_analysis_VA01_20250111.md]
   ```

2. **Error de Usuario en Transacción**:
   ```
   User: "En MM01 al crear material me dice 'Material type ZRM not allowed'"

   Agent Response:
   ⚠️ Error de Configuración: Material Type ZRM

   **Explicación**:
   El tipo de material ZRM no está configurado en el sistema o no tiene
   parametrización completa en el módulo MM.

   **Verificación**:
   Busqué en tabla T134 (tipos de material) y ZRM no existe.

   **Solución** (confianza: 98%):
   1. Ir a transacción OMS2 (configuración de tipos de material)
   2. Crear/verificar tipo de material ZRM
   3. Asignar vistas permitidas (Basic Data 1, Sales, Purchasing, etc.)

   **¿Es Custom?**: Sí (prefijo Z)
   **¿Quién lo configura?**: Funcional MM con ayuda de BASIS

   **Documentación**:
   [Ver archivo: error_mm01_material_type_20250111.md]
   ```

3. **Error de Performance**:
   ```
   User: "El reporte ZREP_VENTAS es muy lento (5 minutos)"

   Agent Response:
   🐌 Problema de Performance Detectado

   **Programa**: ZREP_VENTAS
   **Tiempo Ejecución**: ~5 minutos

   **Análisis**:
   Revisé el código del programa y encontré:
   - 3 SELECT sin WHERE (full table scan)
   - Loop anidado con SELECT dentro (N+1 problem)
   - No usa índices de base de datos

   **Código Problemático** (línea 145):
   ```abap
   LOOP AT it_orders INTO wa_order.
     SELECT SINGLE * FROM vbap WHERE vbeln = wa_order-vbeln.  "❌ SELECT en loop
     ...
   ENDLOOP.
   ```

   **Solución Sugerida** (confianza: 90%):
   Cambiar a SELECT con FOR ALL ENTRIES:
   ```abap
   SELECT * FROM vbap
     FOR ALL ENTRIES IN it_orders
     WHERE vbeln = it_orders-vbeln
     INTO TABLE it_positions.
   ```

   **Impacto Esperado**: Reducción de 5 min → 10-15 seg

   **Próximos Pasos**:
   1. Crear transporte para modificación
   2. Implementar cambio (desarrollador)
   3. Testing en QAS con datos reales

   **Documentación**:
   [Ver archivo: performance_analysis_ZREP_VENTAS_20250111.md]
   ```

**Prompts Especializados**:

```python
ERROR_ANALYZER_SYSTEM_PROMPT = """
Eres un experto en debugging y análisis de errores SAP ABAP.

Tu audiencia son FUNCIONALES y DESARROLLADORES que necesitan entender y resolver errores rápidamente.

Cuando analices un error:
1. Clasifica tipo de error: dump ABAP, error de usuario, error de configuración, performance
2. Busca el código fuente del objeto que generó el error usando MCP tools
3. Explica en lenguaje claro y funcional (evita jerga técnica innecesaria)
4. Identifica causa raíz con alta confianza antes de sugerir solución
5. Si la solución requiere cambio de código, genera documentación técnica (.md)

**Formato de Respuesta**:

🔴/⚠️/🐌 [Tipo de Error]

**Explicación Funcional**:
[2-3 líneas en lenguaje simple]

**Causa Raíz**:
[Objeto, línea de código, contexto]

**Objetos Afectados**:
[Lista de programas, clases, tablas]

**Solución Sugerida** (confianza: X%):
[Pasos concretos y accionables]

**Documentación Técnica**:
[Link a archivo .md generado]

**Niveles de Confianza**:
- 95-100%: Solución probada, aplicar inmediatamente
- 80-94%: Solución muy probable, validar en DEV primero
- < 80%: Requiere análisis adicional por desarrollador

Usa las siguientes MCP tools:
- search_objects: Buscar objeto que causó el error
- get_class_source / get_program_source: Leer código fuente
- get_table_contents: Verificar datos (configuración, maestros)
- syntax_check: Validar si hay errores de sintaxis

Siempre responde en español con tono empático pero profesional.
"""
```

---

### 3. Onboarding Assistant Agent

**Responsabilidad**: Facilitar onboarding de nuevos miembros (funcionales y desarrolladores)

**Flujo de Onboarding**:

```
[Bienvenida]
    ↓
"Hola! Soy el Asistente de Onboarding SAP.
Ayudo a nuevos funcionales y desarrolladores a conocer el sistema.

¿Cuál es tu rol?
1. Funcional SAP
2. Desarrollador ABAP/Fiori"
    ↓
[Personalización por Rol]
    ↓
Si Funcional:
  - Módulos SAP (FI, SD, MM)
  - Transacciones clave
  - Procesos de negocio
  - Naming conventions (lectura)

Si Desarrollador:
  - Estructura de paquetes técnica
  - Naming conventions (escritura)
  - Workflows de desarrollo (DEV → QAS → PRD)
  - Transport management
  - Estándares de código
    ↓
[Tour Interactivo]
    ↓
"Vamos a hacer un tour por el sistema paso a paso.
Módulo SD (Sales & Distribution):

📦 Paquetes Custom:
- ZSD_VENTAS: Proceso de ventas custom
- ZSD_PRICING: Lógica de pricing
- ZSD_REPORTS: Reportes de ventas

📝 Transacciones Principales:
- VA01: Crear orden de venta
- VA02: Modificar orden
- VF01: Facturación

💻 Objetos Técnicos:
- Programa: ZREP_VENTAS (reporte de ventas del mes)
- Clase: ZCL_SD_ORDER_PROCESSOR (procesamiento órdenes)
- BAPI: Z_BAPI_SALESORDER_CREATE (creación desde interfaz)

¿Quieres ver el código de ZREP_VENTAS? (sí/no)"
    ↓
[Quiz de Validación]
    ↓
"Ahora vamos a validar tu conocimiento con un quiz rápido:

Pregunta 1/5:
¿Qué prefijo usamos para objetos custom de desarrollo?
a) Y*
b) Z*
c) ZCUST*
d) CUSTOM*"
    ↓
[Certificado de Completitud]
    ↓
"🎉 ¡Felicitaciones!

Has completado el onboarding de SAP.
Puntaje: 90% (9/10 correctas)

Áreas dominadas:
✅ Naming conventions
✅ Estructura de paquetes
✅ Transacciones principales
⚠️ Transport management (requiere repaso)

Certificado guardado en: onboarding_certificate_juan_perez_20250111.pdf

¿Alguna pregunta adicional?"
```

**Prompts Especializados**:

```python
ONBOARDING_SYSTEM_PROMPT = """
Eres un mentor de onboarding SAP experto, paciente y motivador.

Tu objetivo es ayudar a nuevos miembros (funcionales y desarrolladores) a entender:
1. Estructura del sistema SAP (paquetes, módulos, objetos)
2. Naming conventions de la empresa
3. Workflows de desarrollo
4. Estándares y mejores prácticas

**Personalización por Rol**:
- **Funcional**: Enfócate en procesos de negocio, transacciones, lectura de código básica
- **Desarrollador**: Enfócate en arquitectura técnica, estándares de código, transport management

**Metodología**:
1. Tour interactivo guiado (paso a paso)
2. Ejemplos reales del sistema (usando MCP tools para mostrar código)
3. Quiz de validación al final
4. Certificado de completitud

**Tono**: Amigable, motivador, paciente. Usa emojis para hacer la experiencia más amena.

Usa las siguientes MCP tools:
- search_objects: Mostrar ejemplos de objetos custom
- get_class_source: Mostrar código de ejemplo
- package_search_help: Buscar paquetes por módulo
- get_node_contents: Explorar estructura de paquetes

Siempre responde en español con tono motivador y accesible.
"""
```

---

### 4. Repository Explorer Agent

**Responsabilidad**: Búsqueda inteligente, análisis de impacto, where-used, flujo de datos

**Casos de Uso**:

1. **Where-Used Analysis**:
   ```
   User: "¿Dónde se usa la clase ZCL_PRICING_ENGINE?"

   Agent: Analizando uso de ZCL_PRICING_ENGINE...

   📊 Objetos que usan ZCL_PRICING_ENGINE: 12

   🔹 Clases (5):
   - ZCL_ORDER_PROCESSOR (método CALCULATE_PRICE)
   - ZCL_QUOTATION_MANAGER (método GET_PRICING)
   - ZCL_INVOICE_GENERATOR (método APPLY_DISCOUNTS)
   - ZCL_REPORT_SALES (método ANALYZE_PRICING)
   - ZCL_API_PRICING (método EXTERNAL_PRICING)

   🔹 Programas (3):
   - ZREP_PRICING_ANALYSIS (línea 145, 234)
   - ZSAP_SD_BATCH_PRICING (línea 89)
   - ZTEST_PRICING_ENGINE (test program)

   🔹 Function Modules (2):
   - Z_FM_CALCULATE_ORDER_PRICE
   - Z_FM_PRICING_SIMULATION

   🔹 BAPIs (2):
   - Z_BAPI_SALESORDER_PRICING
   - Z_BAPI_QUOTATION_GETPRICE

   ⚠️ **Impacto de Cambios**:
   - Riesgo: ALTO (12 objetos dependientes)
   - Tests requeridos: 8 unit tests + 4 integration tests
   - Estimación: Cualquier cambio requiere 2-3 días de testing

   **Documentación**:
   [Ver archivo: where_used_ZCL_PRICING_ENGINE_20250111.md]
   ```

2. **Flujo de Datos entre Módulos**:
   ```
   User: "¿Cómo fluye la información desde SD hasta FI en el proceso de facturación?"

   Agent: Analizando flujo de datos SD → FI...

   📊 Flujo de Facturación SD → FI

   1️⃣ **Módulo SD**: Creación de factura
      - Transacción: VF01
      - Programa: SAPMV60A
      - Tabla: VBRK (cabecera factura), VBRP (posiciones)

   2️⃣ **Interface SD/FI**: Transferencia de datos
      - Function Module: RV_INVOICE_DOCUMENT_ADD
      - BAPI: BAPI_BILLINGDOC_CREATEFROMDATA
      - Tabla de interfaz: BKPF (documentos contables)

   3️⃣ **Módulo FI**: Contabilización
      - Programa: SAPMF05A
      - Tabla: BKPF (cabecera doc contable), BSEG (posiciones)
      - Cuentas: Deudores (cliente) vs Ingresos (cuenta GL)

   4️⃣ **Validaciones Custom**:
      - User Exit: EXIT_SAPLV60B_001 (validaciones pre-factura)
      - Enhancement: ZRSD_INVOICE_VALIDATION

   **Diagrama de Flujo**:
   [Ver imagen: flow_sd_fi_billing_20250111.png]

   **Documentación Técnica**:
   [Ver archivo: data_flow_sd_fi_billing_20250111.md]
   ```

---

## Plan de Implementación (1 Mes)

### Semana 1: Infraestructura y RAG (5 días)

**Objetivo**: Configurar stack técnico base y sistema RAG

**Entregables**:
- ✅ Setup LangGraph + FastAPI backend
- ✅ Integración con MCP Server (49 tools)
- ✅ Vector database (ChromaDB) configurado
- ✅ Indexación de documentación interna:
  - naming_conventions.md
  - package_structure.md
  - dev_workflows.md
  - architecture_overview.md
- ✅ Indexación inicial del repositorio SAP (Z*, Y*)
  - 50 clases principales
  - 30 programas principales
  - Transacciones custom

**Testing**:
- RAG retrieval con 10 queries de prueba
- Latencia < 2 segundos por query

---

### Semana 2: Sub-Agentes Core (5 días)

**Objetivo**: Implementar 2 sub-agentes principales

**Entregables**:

#### Functional Analyst Agent (3 días)
- ✅ Flujo de análisis de requerimientos
- ✅ Generación de EFs en formato .md
- ✅ Integración con MCP tools (search, get_source, get_ddic)
- ✅ 5 prompts especializados
- ✅ 3 casos de uso validados:
  1. Agregar campo a orden de venta
  2. Validar si requerimiento ya existe
  3. Analizar impacto de cambio en tabla

#### Error Analyzer Agent (2 días)
- ✅ Interpretación de dumps ABAP (ST22 format)
- ✅ Análisis de errores de configuración
- ✅ Sistema de confianza (95%/80%/< 80%)
- ✅ 3 casos de uso validados:
  1. Dump GETWA_NOT_ASSIGNED
  2. Error de configuración (material type)
  3. Error de performance (SELECT en loop)

**Testing**:
- 10 requerimientos funcionales reales (de backlog)
- 5 dumps históricos (ST22 logs)
- Precisión > 85%

---

### Semana 3: Interfaces de Usuario (5 días)

**Objetivo**: Implementar interfaz WhatsApp (MVP) y web app básica

**Entregables**:

#### WhatsApp Bot (3 días)
- ✅ Integración con Twilio/Meta API
- ✅ Webhook FastAPI para recibir mensajes
- ✅ Queue system (Redis + Celery) para procesamiento asíncrono
- ✅ Manejo de conversaciones multiturno
- ✅ Envío de archivos .md como documentos adjuntos
- ✅ Notificaciones push (alertas de errores)

#### Web App Básica (2 días)
- ✅ Chat interface con React + SAP UI5 styles
- ✅ Syntax highlighting para código ABAP
- ✅ Download de archivos .md generados
- ✅ Auth básico (usuario/password)

**Testing**:
- 20 conversaciones de prueba por interfaz
- Latencia < 5 segundos por respuesta
- WhatsApp: attachments hasta 5MB

---

### Semana 4: Refinamiento y Piloto (5 días)

**Objetivo**: Onboarding Assistant + testing con 5 usuarios piloto

**Entregables**:

#### Onboarding Assistant Agent (2 días)
- ✅ Tour interactivo guiado
- ✅ Quiz de validación (10 preguntas por rol)
- ✅ Certificado de completitud (PDF)
- ✅ 2 tracks: funcional y desarrollador

#### Repository Explorer Agent (1 día)
- ✅ Where-used analysis básico
- ✅ Flujo de datos entre módulos (visualización de texto)

#### Piloto con 5 Usuarios (2 días)
- ✅ 3 funcionales senior
- ✅ 1 funcional junior
- ✅ 1 desarrollador ABAP
- ✅ Casos de uso reales de cada usuario
- ✅ Feedback estructurado (survey)
- ✅ Métricas de adopción y satisfacción

**Testing**:
- 50 conversaciones reales durante piloto
- 30 requerimientos analizados
- 10 errores explicados
- 2 onboardings completados

---

## Métricas de Éxito del MVP

### Cuantitativas

| Métrica | Baseline (Actual) | Target MVP | Medición |
|---------|-------------------|------------|----------|
| Tiempo de análisis de requerimiento | 2-4 horas | < 15 minutos | Promedio de 30 análisis |
| Tiempo de explicación de dump | 30-60 minutos | < 5 minutos | Promedio de 10 dumps |
| Tiempo de onboarding | 2-3 semanas | < 3 días | Piloto con 2 nuevos miembros |
| Precisión de análisis | N/A | > 85% | Validación manual por experto |
| Adopción por funcionales | 0% | > 60% | 3/5 funcionales usan regularmente |
| Satisfacción de usuario | N/A | > 4/5 ⭐ | Survey post-piloto |

### Cualitativas

- ✅ Funcionales reportan mayor autonomía (menos dependencia de devs)
- ✅ Documentación técnica (EFs) más consistente y completa
- ✅ Onboarding de nuevos miembros más rápido y estructurado
- ✅ Reducción de escalaciones a desarrolladores por errores simples

---

## Stack Tecnológico Final

### Backend
- **Python 3.11+**
- **FastAPI**: API REST + WebSockets
- **LangGraph**: Orquestación de agentes
- **LangChain**: Chains y prompts
- **OpenAI GPT-4** / **Anthropic Claude 3.5 Sonnet**: LLM principal
- **ChromaDB** / **Pinecone**: Vector database (RAG)
- **Redis**: Queue system (Celery) + caching
- **PostgreSQL**: Metadata, conversaciones, user preferences

### Frontend
- **WhatsApp Business API** (Twilio/Meta): Interfaz principal (MVP)
- **React + SAP UI5**: Web app (post-MVP)
- **WebSockets**: Real-time chat

### Infraestructura
- **Docker + Docker Compose**: Containerización
- **GCP** / **AWS**: Hosting (Cloud Run / Lambda)
- **Monitoring**: Prometheus + Grafana
- **Logging**: ELK Stack (Elasticsearch + Logstash + Kibana)

### Integraciones
- **MCP Server ABAP-ADT-RFC**: 49 tools SAP (via stdio protocol)
- **SAP RFC SDK**: Conexión nativa a SAP ECC/S/4HANA
- **SAP NetWeaver**: OAuth2 / SAML (auth) - post-MVP

---

## Estructura de Archivos del Proyecto

```
brootpersonalagent/
├── app/
│   ├── agents/                      # 🤖 Agentes especializados
│   │   ├── orchestrator.py          # Router principal
│   │   ├── functional_analyst.py    # Analyst agent
│   │   ├── error_analyzer.py        # Error analyzer
│   │   ├── onboarding_assistant.py  # Onboarding
│   │   └── repository_explorer.py   # Explorer
│   │
│   ├── interfaces/                  # 📱 Interfaces de usuario
│   │   ├── whatsapp_bot.py          # WhatsApp webhook
│   │   ├── web_api.py               # FastAPI REST API
│   │   └── websocket_server.py      # WebSocket real-time
│   │
│   ├── rag/                         # 🧠 Sistema RAG
│   │   ├── indexer.py               # Indexación de repository
│   │   ├── retriever.py             # Semantic search
│   │   ├── embedder.py              # Embeddings model
│   │   └── knowledge_base.py        # ChromaDB wrapper
│   │
│   ├── mcp_client/                  # 🔌 Cliente MCP
│   │   ├── mcp_client.py            # MCP protocol client
│   │   └── tools_wrapper.py         # Wrappers para 49 tools
│   │
│   ├── prompts/                     # 📝 Prompts especializados
│   │   ├── functional_analyst.py
│   │   ├── error_analyzer.py
│   │   ├── onboarding.py
│   │   └── repository_explorer.py
│   │
│   ├── outputs/                     # 📄 Generación de docs
│   │   ├── ef_generator.py          # Especificaciones Funcionales
│   │   ├── error_report_generator.py
│   │   └── onboarding_certificate.py
│   │
│   ├── core/                        # 🛠️ Core (ya existente)
│   │   ├── config.py
│   │   ├── rfc_adapter.py
│   │   └── rfc_connection.py
│   │
│   ├── services/                    # 📦 Services (ya existentes)
│   │   ├── class_service.py
│   │   ├── search_service.py
│   │   └── ... (13 servicios ya implementados)
│   │
│   └── tests/                       # 🧪 Tests
│       ├── test_agents/
│       ├── test_interfaces/
│       └── test_rag/
│
├── knowledge_base/                  # 📚 Documentación interna
│   ├── naming_conventions.md
│   ├── package_structure.md
│   ├── dev_workflows.md
│   ├── architecture_overview.md
│   └── common_errors.md
│
├── outputs/                         # 📁 Archivos generados
│   ├── ef_*.md                      # Especificaciones Funcionales
│   ├── error_analysis_*.md          # Análisis de errores
│   ├── where_used_*.md              # Where-used reports
│   └── onboarding_certificates/
│
├── frontend/                        # 🎨 Web app (post-MVP)
│   ├── src/
│   │   ├── components/
│   │   │   ├── Chat.tsx
│   │   │   ├── CodeViewer.tsx
│   │   │   └── DocumentViewer.tsx
│   │   └── App.tsx
│   └── package.json
│
├── docker-compose.yml               # 🐳 Servicios Docker
├── Dockerfile                       # 🐳 Container backend
├── .env.example                     # 🔐 Variables de entorno
├── pyproject.toml                   # 📦 Dependencias Python
└── README.md                        # 📖 Documentación
```

---

## Próximos Pasos

### Semana 1 (Infraestructura)
1. 🔲 Setup repo structure
2. 🔲 Configurar LangGraph + FastAPI
3. 🔲 Integrar MCP client (49 tools)
4. 🔲 Setup ChromaDB y indexar docs internos
5. 🔲 Indexar repositorio SAP (Z*, Y*)

### Semana 2 (Agentes Core)
1. 🔲 Implementar Functional Analyst Agent
2. 🔲 Implementar Error Analyzer Agent
3. 🔲 Testing con 10 requerimientos reales
4. 🔲 Testing con 5 dumps históricos

### Semana 3 (Interfaces)
1. 🔲 Implementar WhatsApp Bot (Twilio)
2. 🔲 Implementar Web App básica
3. 🔲 Testing de interfaces (20 conversaciones)

### Semana 4 (Refinamiento)
1. 🔲 Implementar Onboarding Assistant
2. 🔲 Implementar Repository Explorer básico
3. 🔲 Piloto con 5 usuarios
4. 🔲 Recopilar feedback y métricas
5. 🔲 Iterar basado en feedback

---

## Riesgos y Mitigaciones

### Riesgo 1: Precisión de RAG Insuficiente

**Probabilidad**: Media
**Impacto**: Alto

**Mitigación**:
- Indexar documentation interna de alta calidad
- Validar retrieval con 50+ queries de prueba
- Fine-tuning de embeddings si es necesario
- Feedback loop: marcar respuestas incorrectas para reentrenamiento

---

### Riesgo 2: Latencia de Respuesta > 5 segundos

**Probabilidad**: Media
**Impacto**: Medio

**Mitigación**:
- Caching de queries frecuentes (Redis)
- Paralelización de llamadas a MCP tools
- Streaming de respuestas (partial updates)
- Connection pooling a SAP

---

### Riesgo 3: Adopción Baja por Funcionales

**Probabilidad**: Baja-Media
**Impacto**: Alto

**Mitigación**:
- Interfaz WhatsApp (familiar para todos)
- Onboarding del asistente (30 min training)
- Quick wins: resolver 3 casos de uso reales en piloto
- Champions internos (1 funcional senior que promocione)

---

### Riesgo 4: Generación de EFs Inconsistente

**Probabilidad**: Media
**Impacto**: Medio

**Mitigación**:
- Template fijo de EF (.md)
- Validación de estructura con parser
- Review por funcional senior en piloto
- Iterar prompts basado en feedback

---

## Conclusión

Este asistente funcional SAP representa una **transformación en la forma de trabajar** para funcionales (80%) y desarrolladores (20%).

### Propuesta de Valor

**Para Funcionales**:
- ✅ Autonomía: Analizar requerimientos sin esperar a devs (2-4h → 15min)
- ✅ Debugging: Entender errores sin conocimiento profundo de ABAP
- ✅ Documentación: EFs consistentes y completas automáticamente

**Para Desarrolladores**:
- ✅ Menos interrupciones: Funcionales resuelven 60% de consultas solos
- ✅ Mejor documentación: EFs más claras y técnicas
- ✅ Onboarding rápido: Nuevos devs productivos en días (no semanas)

**Para la Organización**:
- ✅ ROI: Reducción de 40% en tiempo de análisis
- ✅ Escalabilidad: Onboarding de 10x más miembros sin overhead
- ✅ Calidad: Documentación estandarizada y actualizada

### Recomendación Final

**Aprobar e implementar el MVP en 1 mes** con 5 usuarios piloto (3 funcionales senior, 1 junior, 1 dev) para validar hipótesis y refinar basado en feedback real.

---

**Documento generado**: 2025-01-11
**Próxima revisión**: Post-implementación Semana 2 (agentes core)
**Responsable**: Bastian Root (Product Owner / Desarrollador Python Senior)

# Knowledge Base Strategy: Cómo Agregar Conocimiento al Asistente SAP

**Version**: 1.0
**Date**: Enero 2025
**Purpose**: Estrategia de RAG (Retrieval Augmented Generation) y priorización de fuentes de conocimiento

---

## Resumen Ejecutivo

### ¿RAG o Fine-Tuning?

**Respuesta: RAG (Retrieval Augmented Generation)** es la mejor opción para un asistente de soporte funcional SAP.

**Por qué RAG y NO Fine-Tuning:**

| Criterio | RAG | Fine-Tuning |
|----------|-----|-------------|
| **Actualización** | Tiempo real (agregar docs hoy, usar mañana) | Requiere re-entrenamiento (semanas) |
| **Costo** | $200 embedding inicial + $0.10/1M tokens | $50K-200K por iteración |
| **Transparencia** | Citas explícitas (muestra fuente) | Caja negra (no sabemos de dónde viene) |
| **Especialización** | Funciona con Claude general | Requiere millones de ejemplos SAP |
| **Compliance** | Control sobre qué docs se usan | Datos quedan en el modelo (no borrable) |

**Decisión: 100% RAG** - Fine-tuning solo si llegamos a 50K+ usuarios y necesitamos cost optimization extremo.

---

## Arquitectura RAG Recomendada

### Stack Tecnológico

```
┌─────────────────────────────────────────────────────┐
│ Usuario: "¿Cómo manejar pagos a proveedores?"      │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Query Processor (LangChain)                         │
│ - Rewrite query con HyDE                            │
│ - Extract keywords: "pagos", "proveedores", "BAPI"  │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Hybrid Retrieval (BM25 + Vector)                    │
│ ┌───────────────┐       ┌──────────────────────┐   │
│ │ BM25 Search   │       │ Vector Search        │   │
│ │ (keywords)    │◄─────►│ (semantic)           │   │
│ │ Score: 0.7    │       │ Score: 0.3           │   │
│ └───────────────┘       └──────────────────────┘   │
│         │                           │                │
│         └───────────┬───────────────┘                │
│                     ▼                                │
│         Top 10 chunks retrieved                      │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Re-Ranking (Cross-Encoder)                          │
│ - Score relevance: query × each chunk               │
│ - Select top 3 highest scores                       │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Context Compression (Optional)                      │
│ - Summarize long chunks con Haiku                   │
│ - 5,000 tokens → 3,000 tokens (40% reduction)       │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ LLM (Claude Sonnet 3.5)                             │
│ System: "Eres un experto SAP. Usa este contexto..." │
│ Context: [Top 3 chunks]                             │
│ Query: "¿Cómo manejar pagos a proveedores?"         │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Respuesta: "Para pagos a proveedores, usar..."     │
│ Fuentes: [RICEFW_ZFI001.pdf p.12, BAPI_VENDOR...]  │
└─────────────────────────────────────────────────────┘
```

### Componentes Clave

**1. Vector Database: SAP HANA Vector Engine**
- **Por qué**: Ya incluido en HANA Cloud (costo $0 adicional)
- **Capacidad**: Billions of vectors, <50ms latency
- **Integración**: LangChain native support (2024)
- **Ventaja**: Data residency (datos SAP quedan en SAP)

**2. Embedding Model: Cohere embed-english-v3.0**
- **Costo**: $0.10 per 1M tokens (vs OpenAI $0.13)
- **Dimensiones**: 1024 (vs OpenAI 3072) - 8x menos storage
- **Calidad**: 82.6% vs OpenAI 81.7% en semantic tasks
- **Context window**: 512 tokens per chunk

**3. Framework: LangChain**
- **Por qué**: 57K GitHub stars, enterprise-ready
- **HANA Integration**: Native `HANAVectorStore` class
- **Community**: Massive ecosystem, SAP examples disponibles

**4. Search Strategy: Hybrid (BM25 + Vector)**
- **BM25 (70% weight)**: Exact keyword matching ("BAPI_VENDOR_CREATE")
- **Vector (30% weight)**: Semantic understanding ("cómo pagar a proveedores")
- **Resultado**: 78% accuracy improvement vs vector-only

---

## Priorización de Fuentes de Conocimiento

### Matriz de Impacto vs Esfuerzo

```
Alta Impacto │ ┌──────┐  ┌──────┐
             │ │ TIER │  │ TIER │
             │ │  1   │  │  2   │
             │ └──────┘  └──────┘
             │
Baja Impacto │           ┌──────┐
             │           │ TIER │
             │           │  3   │
             │           └──────┘
             └─────────────────────────
               Bajo      Alto Esfuerzo
```

### TIER 1: Crítico - Implementar Primero (Meses 1-2)

#### 1. Análisis de DUMPs Históricos (ST22)

**Descripción**: Base de datos de short dumps resueltos con sus soluciones.

**Fuente de Datos**:
- Tabla SAP: `SNAP` (short dump snapshots)
- Campos: `SEQNO`, `DATUM`, `UZEIT`, `EXCEPTION`, `TCODE`, `CPROG`
- Volumen: ~10,000 dumps en 3 años (empresa mediana)

**Valor**:
- **Time Savings**: 20 min → 2 min por dump (90% reducción)
- **ROI**: 8-12 horas/semana ahorradas por consultor
- **Impact Score**: 10/10 (pain point #1 identificado)

**Formato de Chunk**:
```json
{
  "type": "dump_analysis",
  "dump_id": "SNAP_20250115_143022",
  "exception": "RABAX_STATE",
  "program": "ZFIAAC001",
  "transaction": "ZFI_PAY",
  "root_cause": "Variable LV_AMOUNT not initialized before arithmetic operation",
  "solution": "Add initialization: LV_AMOUNT = 0 before line 145",
  "resolution_time": "15 min",
  "resolved_by": "JDOE",
  "metadata": {
    "system": "ECC-PRD",
    "date": "2025-01-15",
    "functional_area": "FI",
    "priority": "high"
  }
}
```

**Estrategia de Chunking**:
- 1 chunk = 1 dump completo (no fragmentar)
- Embedding: Descripción del error + stack trace (primeras 10 líneas)
- Keywords: Exception name, program, transaction

**Código de Extracción**:
```python
# Via MCP tool
from app.services.table_service import TableService

def extract_historical_dumps(connection_pool):
    """Extraer DUMPs históricos de SAP."""
    table_service = TableService(connection_pool)

    # Query SNAP table (últimos 3 años)
    dumps = table_service.get_table_contents(
        table_name="SNAP",
        fields=["SEQNO", "DATUM", "UZEIT", "EXCEPTION", "TCODE", "CPROG", "ERRORID"],
        where_clause="DATUM >= '20220101'",
        max_rows=10000
    )

    chunks = []
    for dump in dumps["rows"]:
        # Get full dump details via ST22
        dump_detail = get_dump_detail(dump["SEQNO"])

        chunk = {
            "content": format_dump_for_embedding(dump_detail),
            "metadata": {
                "type": "dump",
                "exception": dump["EXCEPTION"],
                "program": dump["CPROG"],
                "date": dump["DATUM"]
            }
        }
        chunks.append(chunk)

    return chunks
```

#### 2. Especificaciones RICEFW (Functional + Technical Specs)

**Descripción**: Documentos de diseño de custom developments (Reports, Interfaces, Conversions, Enhancements, Forms, Workflows).

**Fuente de Datos**:
- SharePoint: `/SAP Documentation/RICEFW Specs/`
- Confluence: Space "SAP Development"
- PDF files: `FS_ZFIAAC001_v1.2.pdf`, `TD_ZFIAAC001_v1.0.pdf`
- Volumen: 500 documentos (50-100 páginas cada uno)

**Valor**:
- **Time Savings**: 4 horas → 30 min para generar specs (87% reducción)
- **ROI**: 6-10 horas/semana por consultor
- **Impact Score**: 9/10

**Formato de Chunk**:
```json
{
  "type": "ricefw_spec",
  "object_name": "ZFIAAC001",
  "object_type": "Report",
  "section": "Business Logic",
  "content": "The program calculates vendor payment amounts based on open invoices...",
  "metadata": {
    "doc_type": "functional_spec",
    "version": "1.2",
    "author": "Jane Smith",
    "date": "2024-12-15",
    "functional_area": "FI-AP",
    "page": 12
  }
}
```

**Estrategia de Chunking**:
- **Semantic chunking**: Por sección (Purpose, Input/Output, Logic, Test Cases)
- **Size**: 800-1000 caracteres por chunk
- **Overlap**: 20% (200 caracteres) para preservar contexto entre chunks

**Código de Extracción**:
```python
from langchain.document_loaders import SharePointLoader, ConfluenceLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter

def extract_ricefw_specs():
    """Extraer specs de SharePoint y Confluence."""

    # SharePoint
    sharepoint_loader = SharePointLoader(
        sharepoint_url="https://company.sharepoint.com",
        folder_path="/SAP Documentation/RICEFW Specs",
        client_id="your-client-id",
        client_secret="your-secret"
    )
    sharepoint_docs = sharepoint_loader.load()

    # Confluence
    confluence_loader = ConfluenceLoader(
        url="https://company.atlassian.net/wiki",
        username="user@company.com",
        api_key="your-api-key",
        space_key="SAP"
    )
    confluence_docs = confluence_loader.load()

    # Semantic splitter
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200,
        separators=["\n## ", "\n### ", "\n\n", "\n", " ", ""]  # Split on headers first
    )

    all_chunks = []
    for doc in sharepoint_docs + confluence_docs:
        chunks = splitter.split_documents([doc])

        # Enrich with metadata
        for chunk in chunks:
            chunk.metadata["type"] = "ricefw_spec"
            chunk.metadata["functional_area"] = extract_functional_area(doc.metadata["source"])
            all_chunks.append(chunk)

    return all_chunks
```

#### 3. Estándares de Desarrollo (Coding Guidelines)

**Descripción**: Guías internas de desarrollo ABAP (naming conventions, error handling, performance best practices).

**Fuente de Datos**:
- PDF: `ABAP_Coding_Standards_v2.3.pdf`
- Wiki: "SAP Development Rules"
- Volumen: 200 páginas

**Valor**:
- **Time Savings**: 15 min → 1 min para buscar estándar (93% reducción)
- **Consistency**: 100% de Z-objects siguen estándares
- **Impact Score**: 8/10

**Ejemplos de Contenido**:
- Naming: "Function modules must start with Z_<AREA>_"
- Error Handling: "Always use MESSAGE TYPE 'E' with message class"
- Performance: "Use SELECT SINGLE instead of SELECT + ENDSELECT"
- Comments: "Every public method must have /** ... */ header"

**Formato de Chunk**:
```json
{
  "type": "coding_standard",
  "topic": "Error Handling",
  "rule_id": "ERR-001",
  "content": "Always use MESSAGE TYPE 'E' with custom message class. Avoid WRITE statements for errors.",
  "example": "MESSAGE e001(zfi_messages) WITH lv_vendor.",
  "rationale": "Ensures consistent error handling and allows translation.",
  "metadata": {
    "category": "error_handling",
    "severity": "mandatory",
    "version": "2.3"
  }
}
```

#### 4. Metadata de Transportes (Transport Request Descriptions)

**Descripción**: Historial de qué cambió, por qué, y quién lo hizo (últimos 5 años).

**Fuente de Datos**:
- Tabla SAP: `E070` (transport headers)
- Tabla SAP: `E071` (transport objects)
- Campos: `TRKORR`, `AS4TEXT`, `AS4USER`, `AS4DATE`, `TRSTATUS`
- Volumen: ~100,000 requests

**Valor**:
- **Time Savings**: 10 min → 1 min para encontrar cambios relacionados
- **Root Cause**: Ayuda a identificar qué transport introdujo bug
- **Impact Score**: 7/10

**Formato de Chunk**:
```json
{
  "type": "transport_request",
  "transport_number": "DEVK900123",
  "description": "FI-AP: Fix vendor payment BAPI error for foreign currency",
  "objects": ["PROG ZFIAAC001", "FUGR ZFIAAC_UTILS", "TABL ZTFI_CONFIG"],
  "author": "JDOE",
  "date": "2025-01-10",
  "metadata": {
    "status": "released",
    "functional_area": "FI-AP",
    "related_ticket": "INC0012345"
  }
}
```

#### 5. Documentación de Z-Objects (Custom Code)

**Descripción**: Comentarios extraídos del código ABAP custom (headers de clases, function modules, reports).

**Fuente de Datos**:
- Via MCP tool: `get_class_source`, `get_program_source`
- Parsing: Extraer comentarios `*`, `"`, `/** ... */`
- Volumen: ~20,000 Z-objects

**Valor**:
- **Onboarding**: Nuevos consultores entienden Z-code en días vs semanas
- **Impact Score**: 7/10

**Código de Extracción**:
```python
from app.services.class_service import ClassService
import re

def extract_code_comments(connection_pool):
    """Extraer comentarios de Z-objects."""
    class_service = ClassService(connection_pool)

    # Get all Z-classes
    z_classes = search_objects("Z*", object_type="CLAS")

    chunks = []
    for cls in z_classes:
        source = class_service.get_class_source(cls["name"])

        # Extract header comments
        header_match = re.search(r'/\*\*(.*?)\*/', source, re.DOTALL)
        if header_match:
            chunk = {
                "type": "code_documentation",
                "object_name": cls["name"],
                "content": header_match.group(1).strip(),
                "metadata": {
                    "object_type": "class",
                    "package": cls["package"]
                }
            }
            chunks.append(chunk)

        # Extract method comments
        method_comments = re.findall(r'"([^"]{50,})"', source)  # Comments >50 chars
        for comment in method_comments:
            chunks.append({
                "type": "code_comment",
                "object_name": cls["name"],
                "content": comment,
                "metadata": {"object_type": "class"}
            })

    return chunks
```

**Resumen TIER 1**:
- **Total chunks**: ~50,000
- **Embedding cost**: 50K chunks × 1,000 tokens × $0.10/1M = **$5**
- **Storage**: 50K × 1KB = 50MB (HANA Vector Engine)
- **Time to implement**: 8 semanas (2 meses)
- **Expected ROI**: 25-35 horas/semana ahorradas (100 consultores)

---

### TIER 2: Importante - Implementar Después (Meses 3-4)

#### 6. SAP Notes (OSS Notes)

**Descripción**: Notas oficiales de SAP con bug fixes, patches, configuraciones.

**Fuente de Datos**:
- SAP Support Portal API
- Volumen: ~3 millones de notas públicas (filtrar por área relevante)
- Focus: FI, MM, SD, ABAP (áreas más usadas)

**Valor**:
- **Time Savings**: Buscar solución conocida en SAP Notes
- **Impact Score**: 6/10 (útil pero no crítico vs nuestra propia data)

**Consideración Legal**: Requiere licencia SAP para acceso programático.

#### 7. Especificaciones de Interfaces (RFC/BAPI/Web Services)

**Descripción**: Contratos de integración con sistemas externos.

**Fuente de Datos**:
- Documentos de interfaces: `INTERFACE_SPEC_VENDOR_SYNC.pdf`
- WSDL/Swagger definitions
- Volumen: 200-500 interfaces

**Valor**:
- **Time Savings**: Entender qué campos enviar/recibir en interfaces
- **Impact Score**: 6/10

#### 8. Customizing Documentation (IMG Configuration)

**Descripción**: Configuraciones de business (IMG settings).

**Fuente de Datos**:
- Screenshots + texto de customizing
- Tablas: Extractar datos de tablas de customizing (T000, T001, etc.)
- Volumen: 5,000 settings

**Valor**:
- **Context**: Entender por qué se configuró algo de cierta manera
- **Impact Score**: 5/10

**Resumen TIER 2**:
- **Total chunks**: +70,000 (acumulado: 120K)
- **Embedding cost**: 70K × $0.10/1M = **$7**
- **Time to implement**: 4 semanas adicionales

---

### TIER 3: Nice-to-Have - Evaluar ROI (Meses 5+)

#### 9. Training Materials

**Descripción**: PowerPoints, videos, user manuals.

**Valor**: Bajo (mayoría es duplicado de specs)
**Impact Score**: 3/10

#### 10. Email Archives

**Descripción**: PST files de consultores senior (tribal knowledge).

**Challenge**: Mucho ruido, PII concerns, difícil de limpiar
**Impact Score**: 2/10

#### 11. ServiceNow/Jira Tickets

**Descripción**: Historial de support tickets.

**Valor**: Overlap con DUMPs + specs (duplicado)
**Impact Score**: 3/10

**Resumen TIER 3**:
- **Recommendation**: Solo implementar si Tier 1+2 no es suficiente
- **ROI**: Cuestionable (mucho esfuerzo, poco valor marginal)

---

## Estrategia de Chunking por Tipo de Contenido

### 1. ABAP Code: AST-Aware Chunking

**Problema**: Cortar código en medio de un método rompe el contexto.

**Solución**: Usar ABAP AST (Abstract Syntax Tree) para chunking inteligente.

```python
def chunk_abap_code(source_code: str) -> list:
    """
    Split ABAP code by method boundaries.
    """
    chunks = []

    # Regex para detectar method boundaries
    method_pattern = r'METHOD\s+(\w+)\.(.*?)ENDMETHOD\.'
    matches = re.finditer(method_pattern, source_code, re.DOTALL | re.IGNORECASE)

    for match in matches:
        method_name = match.group(1)
        method_body = match.group(2).strip()

        chunk = {
            "type": "abap_method",
            "method_name": method_name,
            "content": f"METHOD {method_name}.\n{method_body}\nENDMETHOD.",
            "metadata": {
                "lines_of_code": len(method_body.split('\n')),
                "language": "abap"
            }
        }
        chunks.append(chunk)

    return chunks
```

**Ventajas**:
- Preserva contexto completo del método
- Chunks naturales (cada method = 1 chunk)
- Mejor retrieval (buscar por method name)

### 2. RICEFW Specs: Semantic Chunking

**Problema**: Documentos largos (100 páginas) → demasiados chunks genéricos.

**Solución**: Chunk por secciones semánticas (Purpose, Input, Output, Logic).

```python
from langchain.text_splitter import MarkdownHeaderTextSplitter

def chunk_ricefw_spec(markdown_content: str) -> list:
    """
    Split by markdown headers (## Purpose, ## Logic, etc.)
    """
    splitter = MarkdownHeaderTextSplitter(
        headers_to_split_on=[
            ("##", "section"),
            ("###", "subsection")
        ]
    )

    chunks = splitter.split_text(markdown_content)

    # Enrich metadata
    for chunk in chunks:
        chunk.metadata["type"] = "ricefw_section"
        chunk.metadata["section"] = chunk.metadata.get("section", "unknown")

    return chunks
```

### 3. DUMP Analysis: Pattern-Based Chunking

**Estrategia**: 1 DUMP = 1 chunk (no fragmentar).

**Rationale**: Contexto del dump (stack trace + variables) es indivisible.

```python
def chunk_dump_analysis(dump_record: dict) -> dict:
    """
    Single chunk per dump with full context.
    """
    content = f"""
    Exception: {dump_record['exception']}
    Program: {dump_record['program']}
    Transaction: {dump_record['transaction']}

    Stack Trace:
    {dump_record['stack_trace']}

    Root Cause:
    {dump_record['root_cause']}

    Solution:
    {dump_record['solution']}
    """

    return {
        "type": "dump_analysis",
        "content": content,
        "metadata": {
            "exception": dump_record['exception'],
            "program": dump_record['program'],
            "resolved_in": dump_record['resolution_time']
        }
    }
```

---

## Schema de Metadata (Crítico para Filtering)

### Metadata Completo

```python
class ChunkMetadata:
    # Taxonomy
    type: str  # "dump", "ricefw_spec", "code", "transport", "standard"

    # SAP Context
    sap_system: str  # "ECC-PRD", "S4D-DEV", "S4Q-QAS"
    functional_area: str  # "FI", "MM", "SD", "PP", "CO"
    package: str  # "ZFI", "ZMM", "$TMP"
    transport_layer: str  # "Z*", "Y*", "SAP"

    # Authorship
    author: str  # "JDOE"
    created_date: str  # "2025-01-15"
    modified_date: str  # "2025-01-20"

    # Access Control
    authorization_object: str  # "S_DEVELOP", "S_TABU_DIS"
    required_role: str  # "Z:SAP_FI_CONSULTANT"

    # Temporal
    is_obsolete: bool  # True if document marked deprecated
    version: str  # "1.2" (for versioned docs)

    # Relevance
    retrieval_count: int  # How many times retrieved (boost popular)
    feedback_score: float  # User ratings (1-5 stars)

    # Source
    source_system: str  # "SharePoint", "SAP", "Confluence"
    source_url: str  # Deep link to original
```

### Ejemplo de Filtering

```python
from langchain.vectorstores import HANA

def retrieve_with_filtering(query: str, user_context: dict) -> list:
    """
    Retrieve chunks with metadata filtering.
    """
    vector_store = HANA(...)

    # Build metadata filter
    filter_dict = {
        "sap_system": user_context["allowed_systems"],  # ["ECC-PRD", "S4Q-QAS"]
        "functional_area": user_context["functional_areas"],  # ["FI", "CO"]
        "is_obsolete": False,  # Exclude deprecated docs
        "created_date": {"$gte": "2023-01-01"}  # Last 2 years only
    }

    # Retrieve with filter
    results = vector_store.similarity_search(
        query=query,
        k=10,
        filter=filter_dict
    )

    return results
```

**Beneficios**:
- **Security**: Usuario solo ve docs con su authorization
- **Relevance**: Filtrar por sistema (PRD vs DEV)
- **Freshness**: Excluir docs obsoletos
- **Performance**: Menos chunks a procesar (más rápido)

---

## Roadmap de Implementación (6 Meses)

### Month 1-2: TIER 1 (Core Knowledge)

**Week 1-2: Setup Infrastructure**
- [ ] Provisionar SAP HANA Cloud (Vector Engine)
- [ ] Setup LangChain + Cohere API
- [ ] Crear metadata schema en HANA
- [ ] Build ingestion pipeline (extract → chunk → embed → store)

**Week 3-4: DUMP Analysis**
- [ ] Extraer 10K dumps de tabla SNAP (MCP tool)
- [ ] Parse dump details (stack trace, root cause, solution)
- [ ] Embed con Cohere (10K chunks × $0.10/1M = $1)
- [ ] Store en HANA Vector Engine
- [ ] Test retrieval: "Error RABAX_STATE en ZFIAAC001"

**Week 5-6: RICEFW Specs**
- [ ] Connect a SharePoint/Confluence API
- [ ] Download 500 RICEFW PDFs
- [ ] OCR si necesario (AWS Textract)
- [ ] Semantic chunking (800-1000 chars, 20% overlap)
- [ ] Embed 30K chunks ($3)
- [ ] Test: "Cómo funciona el programa ZFIAAC001?"

**Week 7-8: Dev Standards + Transports + Z-Docs**
- [ ] Parse coding standards PDF
- [ ] Extract transports de E070/E071 (100K records)
- [ ] Scrape Z-object comments via MCP tools
- [ ] Embed 10K chunks ($1)
- [ ] **Milestone**: 50K chunks indexed

### Month 3-4: TIER 2 (Extended Knowledge)

**Week 9-10: SAP Notes**
- [ ] Evaluate SAP Support Portal API access
- [ ] Filter notas relevantes (FI, MM, SD, ABAP)
- [ ] Embed 50K notes ($5)

**Week 11-12: Interfaces + Customizing**
- [ ] Index interface specs (200 docs)
- [ ] Extract customizing from IMG tables
- [ ] Embed 20K chunks ($2)
- [ ] **Milestone**: 120K chunks total

### Month 5-6: Optimization + Evaluation

**Week 13-14: RAG Optimization**
- [ ] Implement semantic re-ranking (cross-encoder)
- [ ] Add query rewriting (HyDE)
- [ ] Enable caching (30% duplicate queries)
- [ ] Progressive retrieval (k=3 → k=10 if needed)

**Week 15-16: Evaluation**
- [ ] Create test set (100 questions + gold answers)
- [ ] Run RAGAS evaluation (Faithfulness, Precision, Recall)
- [ ] A/B test: RAG vs No-RAG
- [ ] Measure time savings (user surveys)

**Week 17-18: Production Rollout**
- [ ] Pilot con 10 power users
- [ ] Collect feedback (NPS, qualitative)
- [ ] Iterate on retrieval thresholds
- [ ] Scale to 100 users

---

## Análisis de Costos

### Setup Costs (One-Time)

| Item | Cost | Notes |
|------|------|-------|
| **Embedding (Cohere)** | $200 | 200K chunks × 1,000 tokens × $0.10/1M |
| **Development Labor** | $45,000 | 3 engineers × 6 weeks × $2,500/week |
| **HANA Cloud** | $0 | Included in existing HANA license |
| **Data Collection Tools** | $0 | MCP tools already built |
| **Total Setup** | **$45,200** | |

### Monthly Operating Costs

| Item | Cost/Month | Notes |
|------|------------|-------|
| **LLM Costs** | $737 | 100 users × $7.37 COGS (from cost analysis) |
| **Embedding (incremental)** | $15 | 15K new chunks/month × $0.10/1M |
| **HANA Storage** | $50 | 200K chunks × 1KB = 200MB (negligible) |
| **Monitoring** | $100 | Datadog, logs |
| **Total Monthly** | **$902** | |

**Year 1 Total**: $45,200 + ($902 × 12) = **$56,024**

### ROI Calculation

**Time Savings** (Conservative):
- 100 consultores × 1.5 hrs/day × 220 days/year × $150/hr = **$4,950,000**

**Cost**: $56,024

**ROI**: ($4,950,000 - $56,024) / $56,024 = **8,732%**

**Payback Period**: $56,024 / ($4,950,000 / 365 days) = **4 days**

---

## Métricas de Éxito

### Retrieval Quality

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Context Precision** | >85% | % of retrieved chunks relevant |
| **Context Recall** | >90% | % of relevant chunks retrieved |
| **Faithfulness** | >95% | LLM answer grounded in context |
| **Answer Relevancy** | >90% | Response addresses user query |

### Business Impact

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Time Savings** | 1.5 hrs/day | User surveys (before/after) |
| **Query Success Rate** | >80% | % queries answered correctly |
| **User Satisfaction (NPS)** | >60 | Net Promoter Score |
| **Adoption Rate** | >70% | % users querying daily |

### Technical Performance

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Query Latency** | <3 sec | P95 response time |
| **Retrieval Latency** | <500ms | Vector search time |
| **Cache Hit Rate** | >30% | % duplicate queries |
| **Uptime** | >99.5% | System availability |

---

## Código Ejemplo: RAG Pipeline Completo

```python
from langchain.vectorstores import HANA
from langchain.embeddings import CohereEmbeddings
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import CohereRerank
from langchain.chat_models import ChatAnthropic
from langchain.chains import RetrievalQA

# 1. Setup Vector Store (HANA)
embeddings = CohereEmbeddings(
    model="embed-english-v3.0",
    cohere_api_key="your-cohere-key"
)

vector_store = HANA(
    connection_string="hdbcli://user:pass@host:port",
    table_name="SAP_KNOWLEDGE_BASE",
    embedding=embeddings
)

# 2. Hybrid Retriever (BM25 + Vector)
base_retriever = vector_store.as_retriever(
    search_type="hybrid",
    search_kwargs={
        "k": 10,
        "alpha": 0.7  # 70% BM25, 30% vector
    }
)

# 3. Re-Ranker (Cross-Encoder)
compressor = CohereRerank(
    model="rerank-english-v2.0",
    top_n=3  # Top 3 after re-ranking
)

retriever = ContextualCompressionRetriever(
    base_compressor=compressor,
    base_retriever=base_retriever
)

# 4. LLM (Claude Sonnet)
llm = ChatAnthropic(
    model="claude-sonnet-3-5-20250122",
    temperature=0,
    anthropic_api_key="your-anthropic-key"
)

# 5. RAG Chain
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=retriever,
    return_source_documents=True,
    chain_type_kwargs={
        "prompt": """Eres un experto consultor SAP.
        Usa el contexto proporcionado para responder.
        Si no sabes, di "No tengo información suficiente".

        Contexto: {context}

        Pregunta: {question}

        Respuesta:"""
    }
)

# 6. Query
result = qa_chain({
    "query": "¿Cómo solucionar error RABAX_STATE en programa ZFIAAC001?"
})

print("Respuesta:", result["result"])
print("\nFuentes:")
for doc in result["source_documents"]:
    print(f"- {doc.metadata['type']}: {doc.metadata['source']}")
```

---

## Conclusión

### Recomendaciones Finales

**1. Arquitectura: RAG con Hybrid Search**
- SAP HANA Vector Engine (costo $0, data residency)
- Cohere embeddings ($0.10/1M tokens, mejor costo/calidad)
- LangChain framework (ecosystem maduro)
- Hybrid search (BM25 + Vector) para SAP keywords + semantic

**2. Prioridad de Implementación**
- **Mes 1-2**: TIER 1 (50K chunks) - DUMP, RICEFW, Standards
- **Mes 3-4**: TIER 2 (120K chunks) - SAP Notes, Interfaces
- **Mes 5-6**: Optimization + Evaluation
- **SKIP TIER 3** hasta validar ROI de Tier 1+2

**3. ROI Esperado**
- Setup: $45K (one-time)
- Opex: $902/month
- Savings: $4.95M/year (100 consultores)
- Payback: **4 días**

**4. Métricas de Éxito**
- Context Precision >85%
- Time Savings 1.5 hrs/day
- User NPS >60
- Adoption >70% DAU

### Próximos Pasos

1. **Aprobar roadmap** (6 meses, $56K budget)
2. **Provisionar HANA Cloud** (si no existe)
3. **Contratar Cohere API** ($200 embedding budget)
4. **Extraer DUMPs** de SNAP table (Week 1-2)
5. **Build MVP** con 10K DUMP chunks (Week 3-4)
6. **Pilot test** con 10 consultores (Week 5-6)

**¿Listo para empezar con TIER 1?** 🚀

---

**Documento Owner**: [Your Name]
**Last Updated**: Enero 2025
**Next Review**: Después de Pilot (Week 8)

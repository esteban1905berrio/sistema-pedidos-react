# Alternativas de Vector Database (Cuando el Cliente NO tiene HANA)

**Version**: 1.0
**Date**: Enero 2025
**Purpose**: Opciones de vector DB para clientes sin SAP HANA Vector Engine

---

## Resumen Ejecutivo: Top 3 Recomendaciones

### Escenario 1: Cliente Cost-Sensitive (Presupuesto < $50/mes)

**Recomendación: pgvector en Supabase**

```
✅ Costo: $0-25/mes (Free tier hasta 500MB, Pro $25/mes)
✅ Deploy: 15 minutos (signup + API key)
✅ Performance: 5-15ms query latency
✅ Capacidad: 200K-500K vectors
✅ Managed: Zero DevOps overhead
```

**Ideal para**: Startups, POCs, primeros 3-5 clientes piloto

### Escenario 2: Cliente Balanceado (Presupuesto $50-150/mes)

**Recomendación: Qdrant Cloud**

```
✅ Costo: $50-100/mes (con scalar quantization)
✅ Deploy: 30 minutos (provision cluster)
✅ Performance: <10ms query latency (P50)
✅ Capacidad: 500K-2M vectors
✅ Features: Hybrid search, metadata filtering, RBAC
```

**Ideal para**: Empresas medianas, 5-20 clientes en producción

### Escenario 3: Cliente On-Premise (Compliance estricto)

**Recomendación: Self-Hosted Qdrant (Docker)**

```
✅ Costo: $40-60/mes (infra AWS/GCP)
✅ Deploy: 2-4 horas (Docker + monitoring)
✅ Performance: Igual a cloud (Rust-based, rápido)
✅ Data Residency: 100% on-premise o private cloud
✅ Control: Full access a configuración
```

**Ideal para**: Empresas con GDPR estricto, SAP on-premise, regulados (banca, salud)

---

## Tabla Comparativa Completa

| Database | Deployment | Costo (200K vectors) | Query Latency | LangChain | Hybrid Search | On-Premise | Mejor Para |
|----------|-----------|---------------------|---------------|-----------|---------------|------------|------------|
| **Supabase pgvector** | Cloud SaaS | $0-25/mo | 10-20ms | ✅ Native | ✅ (pg_trgm) | ❌ | MVP, Cost-sensitive |
| **Qdrant Cloud** | Cloud SaaS | $50-100/mo | 5-10ms | ✅ Native | ✅ Built-in | ❌ | Production, Balanced |
| **Self-Hosted Qdrant** | Docker/K8s | $40-60/mo + DevOps | 5-10ms | ✅ Native | ✅ Built-in | ✅ | On-premise, Control |
| **Pinecone Serverless** | Cloud SaaS | $50-150/mo | 15-25ms | ✅ Native | ❌ | ❌ | Ease-of-use, Auto-scale |
| **Weaviate Cloud** | Cloud SaaS | $60-120/mo | 10-15ms | ✅ Native | ✅ Built-in | ❌ | Multi-modal, GraphQL |
| **Self-Hosted pgvector** | PostgreSQL | $30-50/mo + DevOps | 10-20ms | ✅ Native | ✅ (pg_trgm) | ✅ | Existing PostgreSQL |
| **HANA Vector Engine** | SAP HANA Cloud | $0 (incluido) | <10ms | ✅ Native | ✅ Built-in | ✅ | Cliente con HANA |

---

## Opción 1: Supabase pgvector (RECOMENDADA para MVP)

### Por Qué Supabase

**Ventajas**:
- ✅ **Free tier generoso**: 500MB storage + 2GB bandwidth/mes = ~50K-100K vectors gratis
- ✅ **PostgreSQL familiar**: La mayoría de devs conocen PostgreSQL
- ✅ **Managed**: Backups automáticos, updates, monitoring incluido
- ✅ **Fast deployment**: 15 minutos desde signup hasta primer query
- ✅ **Upgrade path**: Pro tier $25/mes para 8GB storage (200K-500K vectors)

**Desventajas**:
- ⚠️ Hybrid search requiere pg_trgm extension (más complejo que Qdrant)
- ⚠️ No tan rápido como Qdrant en >1M vectors
- ⚠️ Free tier tiene rate limits (no para high-traffic production)

### Pricing Breakdown

| Tier | Costo | Storage | Vectors | Queries/mes | Ideal Para |
|------|-------|---------|---------|-------------|------------|
| **Free** | $0 | 500MB | 50K-100K | 50K | POC, 1-2 clientes pilot |
| **Pro** | $25/mo | 8GB | 200K-500K | 500K | 5-10 clientes en producción |
| **Team** | $599/mo | 32GB | 1M+ | Unlimited | >50 clientes, enterprise |

**Cálculo para 200K vectors**:
```
Storage: 200K vectors × 1KB metadata × 1.5 overhead = ~300MB
→ Free tier: YES (si <500MB)
→ Pro tier: $25/mo (si necesitas más compute/bandwidth)
```

### Deployment Guide (15 minutos)

**Step 1: Create Supabase Project**
```bash
# 1. Sign up: https://supabase.com
# 2. Create new project: "sap-ai-assistant-prod"
# 3. Wait 2 min for provisioning
# 4. Get connection string from Settings → Database
```

**Step 2: Enable pgvector Extension**
```sql
-- En Supabase SQL Editor
CREATE EXTENSION IF NOT EXISTS vector;
```

**Step 3: Create Table for Vectors**
```sql
CREATE TABLE sap_knowledge_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    embedding VECTOR(1024),  -- Cohere embed-v3 usa 1024 dims
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index para fast similarity search
CREATE INDEX ON sap_knowledge_base
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Index para metadata filtering
CREATE INDEX ON sap_knowledge_base USING GIN (metadata);
```

**Step 4: Python Code (LangChain Integration)**
```python
from langchain.vectorstores.supabase import SupabaseVectorStore
from langchain.embeddings import CohereEmbeddings
from supabase import create_client

# Setup
supabase_url = "https://your-project.supabase.co"
supabase_key = "your-anon-key"  # From Supabase Settings
supabase = create_client(supabase_url, supabase_key)

embeddings = CohereEmbeddings(
    model="embed-english-v3.0",
    cohere_api_key="your-cohere-key"
)

# Create vector store
vector_store = SupabaseVectorStore(
    client=supabase,
    embedding=embeddings,
    table_name="sap_knowledge_base",
    query_name="match_documents"  # Function for similarity search
)

# Add documents
from langchain.schema import Document

docs = [
    Document(
        page_content="DUMP error RABAX_STATE in ZFIAAC001...",
        metadata={"type": "dump", "program": "ZFIAAC001", "date": "2025-01-15"}
    ),
    # ... más docs
]

vector_store.add_documents(docs)

# Search
results = vector_store.similarity_search(
    "Error RABAX in payment program",
    k=5,
    filter={"type": "dump"}  # Metadata filtering
)

for doc in results:
    print(f"Match: {doc.page_content[:100]}...")
    print(f"Metadata: {doc.metadata}\n")
```

**Step 5: Create Similarity Search Function**
```sql
-- En Supabase SQL Editor (requerido para LangChain)
CREATE OR REPLACE FUNCTION match_documents(
    query_embedding VECTOR(1024),
    match_count INT DEFAULT 5,
    filter JSONB DEFAULT '{}'
)
RETURNS TABLE (
    id UUID,
    content TEXT,
    metadata JSONB,
    similarity FLOAT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        sap_knowledge_base.id,
        sap_knowledge_base.content,
        sap_knowledge_base.metadata,
        1 - (sap_knowledge_base.embedding <=> query_embedding) AS similarity
    FROM sap_knowledge_base
    WHERE (filter = '{}' OR metadata @> filter)
    ORDER BY sap_knowledge_base.embedding <=> query_embedding
    LIMIT match_count;
END;
$$;
```

### Hybrid Search Setup (Optional, for BM25 + Vector)

```sql
-- Enable pg_trgm for keyword search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add GIN index for full-text search
CREATE INDEX content_trgm_idx ON sap_knowledge_base USING GIN (content gin_trgm_ops);

-- Hybrid search function
CREATE OR REPLACE FUNCTION hybrid_search(
    query_text TEXT,
    query_embedding VECTOR(1024),
    match_count INT DEFAULT 5,
    alpha FLOAT DEFAULT 0.7  -- 70% keyword, 30% semantic
)
RETURNS TABLE (
    id UUID,
    content TEXT,
    metadata JSONB,
    score FLOAT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        kb.id,
        kb.content,
        kb.metadata,
        (alpha * similarity(kb.content, query_text)) +
        ((1 - alpha) * (1 - (kb.embedding <=> query_embedding))) AS score
    FROM sap_knowledge_base kb
    ORDER BY score DESC
    LIMIT match_count;
END;
$$;
```

### Pros & Cons Summary

**✅ Pros**:
- $0-25/mo (cheapest option)
- PostgreSQL familiar (most devs know it)
- Managed (zero DevOps)
- Fast deployment (15 min)
- Good LangChain support

**❌ Cons**:
- Hybrid search más complejo que Qdrant
- Free tier rate limits (50K queries/mo)
- No tan rápido en >500K vectors
- Requiere SQL knowledge (vs REST API pura)

**Verdict**: **Best for MVP and first 5-10 customers** 🥇

---

## Opción 2: Qdrant Cloud (RECOMENDADA para Producción)

### Por Qué Qdrant

**Ventajas**:
- ✅ **Fastest performance**: Rust-based, <10ms P50 latency
- ✅ **Hybrid search out-of-the-box**: BM25 + vector en single query
- ✅ **Best LangChain integration**: Native `Qdrant` class, examples everywhere
- ✅ **Managed cloud**: Auto-scaling, backups, monitoring included
- ✅ **Scalar quantization**: Reduce storage/cost by 50% sin perder accuracy

**Desventajas**:
- ⚠️ Más caro que Supabase ($50-100/mo vs $0-25/mo)
- ⚠️ No free tier (mínimo $25/mo para cluster pequeño)

### Pricing Breakdown

**Qdrant Cloud Pricing** (2025):
```
Base cluster: $25/mo (1 node, 2 vCPU, 8GB RAM)
  → Capacity: ~500K vectors (sin compression)
  → Capacity: ~1M vectors (con scalar quantization)

Storage: $0.10/GB/mo
  → 200K vectors × 1KB × 1024 dims × 4 bytes = 800MB ≈ $0.08/mo

Compute: $0.02/hour per vCPU
  → 2 vCPU × 730 hrs × $0.02 = $29/mo

Total for 200K vectors: ~$55-60/mo (sin quantization)
Total con quantization: ~$30-35/mo (50% storage savings)
```

**Optimización con Scalar Quantization**:
```python
# Reduce storage 4x (float32 → uint8)
# Accuracy drop: <1% (casi imperceptible)
# Cost savings: 50-75%

from qdrant_client import QdrantClient
from qdrant_client.models import VectorParams, ScalarQuantization, ScalarType

client.create_collection(
    collection_name="sap_knowledge_base",
    vectors_config=VectorParams(
        size=1024,
        distance="Cosine",
        quantization_config=ScalarQuantization(
            type=ScalarType.INT8,
            quantile=0.99,
            always_ram=True
        )
    )
)
```

### Deployment Guide (30 minutos)

**Step 1: Create Qdrant Cloud Cluster**
```bash
# 1. Sign up: https://cloud.qdrant.io
# 2. Create cluster: "sap-ai-prod"
#    - Region: us-east-1 (o más cercano a tu LLM provider)
#    - Size: 1 node, 2 vCPU, 8GB RAM ($25/mo)
# 3. Wait 5 min for provisioning
# 4. Get API key + cluster URL from dashboard
```

**Step 2: Python Code (LangChain Integration)**
```python
from langchain.vectorstores import Qdrant
from langchain.embeddings import CohereEmbeddings
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams

# Setup client
qdrant_url = "https://xyz.cloud.qdrant.io"
qdrant_api_key = "your-api-key"

client = QdrantClient(
    url=qdrant_url,
    api_key=qdrant_api_key,
    timeout=30
)

# Create collection (una sola vez)
client.create_collection(
    collection_name="sap_knowledge_base",
    vectors_config=VectorParams(
        size=1024,  # Cohere embed-v3
        distance=Distance.COSINE
    )
)

# Embeddings
embeddings = CohereEmbeddings(
    model="embed-english-v3.0",
    cohere_api_key="your-cohere-key"
)

# Create vector store
vector_store = Qdrant(
    client=client,
    collection_name="sap_knowledge_base",
    embeddings=embeddings
)

# Add documents
from langchain.schema import Document

docs = [
    Document(
        page_content="DUMP error RABAX_STATE...",
        metadata={"type": "dump", "program": "ZFIAAC001"}
    ),
    # ...
]

vector_store.add_documents(docs)

# Search with metadata filter
results = vector_store.similarity_search(
    query="Error RABAX in payment",
    k=5,
    filter={
        "must": [
            {"key": "type", "match": {"value": "dump"}},
            {"key": "functional_area", "match": {"value": "FI"}}
        ]
    }
)

for doc in results:
    print(f"Match: {doc.page_content[:100]}...")
    print(f"Score: {doc.metadata.get('score', 'N/A')}")
```

**Step 3: Hybrid Search (BM25 + Vector)**
```python
from qdrant_client.models import SearchRequest, NamedVector, Query

# Qdrant's built-in hybrid search
results = client.query_points(
    collection_name="sap_knowledge_base",
    query="payment error BAPI",
    query_filter={
        "must": [{"key": "type", "match": {"value": "dump"}}]
    },
    search_params={
        "hnsw_ef": 128,  # Accuracy parameter
        "exact": False
    },
    limit=5,
    with_payload=True,
    with_vectors=False
)

for result in results.points:
    print(f"Score: {result.score}")
    print(f"Content: {result.payload['content'][:100]}...")
```

### Monitoring & Alerts

```python
# Get collection info
info = client.get_collection("sap_knowledge_base")
print(f"Vectors count: {info.points_count}")
print(f"Indexed vectors: {info.indexed_vectors_count}")
print(f"RAM usage: {info.ram_usage_bytes / 1e9:.2f} GB")

# Performance stats
import time

start = time.time()
results = vector_store.similarity_search("test query", k=10)
latency_ms = (time.time() - start) * 1000

print(f"Query latency: {latency_ms:.2f} ms")

# Alert if latency > 50ms
if latency_ms > 50:
    print("⚠️ WARNING: High latency detected!")
```

### Pros & Cons Summary

**✅ Pros**:
- Fastest performance (Rust, <10ms)
- Hybrid search built-in (BM25 + vector)
- Best LangChain integration
- Scalar quantization (50% cost savings)
- Managed cloud (auto-scaling, backups)

**❌ Cons**:
- No free tier ($25/mo minimum)
- Más caro que Supabase para small scale
- Requiere learning curve (REST API, no SQL)

**Verdict**: **Best for production with 5-20 customers** 🥈

---

## Opción 3: Self-Hosted Qdrant (Para On-Premise)

### Por Qué Self-Hosted

**Use Cases**:
- ✅ Cliente con strict data residency (GDPR, HIPAA)
- ✅ On-premise SAP (no cloud connectivity)
- ✅ Cost optimization a large scale (>500K vectors)
- ✅ Full control sobre configuración

### Deployment Guide (Docker Compose)

**Step 1: Docker Compose File**

```yaml
# docker-compose.yml
version: '3.8'

services:
  qdrant:
    image: qdrant/qdrant:v1.7.4
    container_name: qdrant
    ports:
      - "6333:6333"  # HTTP API
      - "6334:6334"  # gRPC (optional)
    volumes:
      - ./qdrant_storage:/qdrant/storage
      - ./qdrant_snapshots:/qdrant/snapshots
    environment:
      - QDRANT__SERVICE__HTTP_PORT=6333
      - QDRANT__SERVICE__GRPC_PORT=6334
      - QDRANT__STORAGE__STORAGE_PATH=/qdrant/storage
      - QDRANT__STORAGE__SNAPSHOTS_PATH=/qdrant/snapshots
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:6333/healthz"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Monitoring (optional but recommended)
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
```

**Step 2: Prometheus Config**

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'qdrant'
    static_configs:
      - targets: ['qdrant:6333']
```

**Step 3: Start Services**

```bash
# Start Qdrant + monitoring
docker-compose up -d

# Verify
curl http://localhost:6333/healthz
# Should return: {"status":"ok"}

# Check collections
curl http://localhost:6333/collections
```

**Step 4: Python Connection**

```python
from qdrant_client import QdrantClient

# Connect to self-hosted instance
client = QdrantClient(
    url="http://localhost:6333",
    timeout=30
)

# Test connection
health = client.get_health()
print(f"Qdrant health: {health}")

# Rest of code same as Qdrant Cloud...
```

### Cost Breakdown (AWS EC2)

**Infrastructure** (t3.medium: 2 vCPU, 4GB RAM):
```
EC2 instance: $30/mo (reserved) or $40/mo (on-demand)
EBS storage: 100GB SSD = $10/mo
Total: $40-50/mo
```

**+ DevOps Overhead**:
```
Monitoring setup: 4 hours one-time
Maintenance: 2 hours/month (updates, backups)
Troubleshooting: 2-4 hours/month (if issues)

Total: 4-6 hours/month × $50/hr = $200-300/month in time
```

**When Worth It**:
- >500K vectors (cost savings vs Qdrant Cloud $100+/mo)
- On-premise requirement (no choice)
- Existing DevOps team (maintenance is free)

### Backup Strategy

```bash
# Automated daily backup (cron job)
#!/bin/bash
# backup_qdrant.sh

BACKUP_DIR="/backups/qdrant"
DATE=$(date +%Y%m%d_%H%M%S)

# Create snapshot via API
curl -X POST "http://localhost:6333/collections/sap_knowledge_base/snapshots" \
  -H "Content-Type: application/json"

# Wait for snapshot
sleep 5

# Download snapshot
SNAPSHOT_NAME=$(curl "http://localhost:6333/collections/sap_knowledge_base/snapshots" | \
  jq -r '.result[0].name')

curl "http://localhost:6333/collections/sap_knowledge_base/snapshots/${SNAPSHOT_NAME}" \
  -o "${BACKUP_DIR}/qdrant_${DATE}.snapshot"

# Upload to S3 (optional)
aws s3 cp "${BACKUP_DIR}/qdrant_${DATE}.snapshot" \
  s3://my-backups/qdrant/

# Keep only last 7 days locally
find ${BACKUP_DIR} -name "qdrant_*.snapshot" -mtime +7 -delete

echo "Backup completed: ${SNAPSHOT_NAME}"
```

**Add to crontab**:
```bash
# Daily backup at 2 AM
0 2 * * * /path/to/backup_qdrant.sh >> /var/log/qdrant_backup.log 2>&1
```

### Pros & Cons Summary

**✅ Pros**:
- Full control (configuración, access, data)
- On-premise compliant (GDPR, HIPAA)
- Cost-effective at scale (>500K vectors)
- Same performance as Qdrant Cloud

**❌ Cons**:
- DevOps overhead (4-6 hrs/month)
- No auto-scaling (manual provisioning)
- Backup/disaster recovery = your responsibility
- Need monitoring setup (Prometheus + Grafana)

**Verdict**: **Best for on-premise or >500K vectors** 🥉

---

## Decision Tree: ¿Cuál Elegir?

```
┌─────────────────────────────────────────────────────┐
│ ¿Cliente tiene SAP HANA Cloud?                      │
└───────────┬─────────────────────────────────────────┘
            │
            ├── SÍ → Use HANA Vector Engine ($0 cost)
            │
            └── NO
                │
                ┌───────────────────────────────────────┐
                │ ¿Presupuesto mensual?                 │
                └──────┬────────────────────────────────┘
                       │
                       ├── $0-50/mes
                       │   └──> Supabase pgvector (Free o Pro $25)
                       │
                       ├── $50-200/mes
                       │   └──> Qdrant Cloud ($50-100)
                       │
                       └── >$200/mes O On-Premise estricto
                           └──> Self-Hosted Qdrant ($40 infra + DevOps)

┌─────────────────────────────────────────────────────┐
│ ¿Escala actual/proyectada?                          │
└──────┬──────────────────────────────────────────────┘
       │
       ├── <100K vectors → Supabase Free Tier
       ├── 100K-500K vectors → Supabase Pro o Qdrant Cloud
       └── >500K vectors → Qdrant Cloud o Self-Hosted

┌─────────────────────────────────────────────────────┐
│ ¿Compliance requirements?                           │
└──────┬──────────────────────────────────────────────┘
       │
       ├── Cloud OK → Supabase o Qdrant Cloud
       ├── EU Data Residency → Qdrant Cloud (EU region) o Self-Hosted
       └── On-Premise Only → Self-Hosted Qdrant
```

---

## Migration Path: Alternative → HANA Vector Engine

### Scenario: Cliente empieza con Supabase/Qdrant, luego compra HANA Cloud

**Zero-Downtime Migration Strategy**:

**Step 1: Dual-Write Phase (Week 1-2)**
```python
class DualVectorStore:
    """Write to both old and new vector stores simultaneously."""

    def __init__(self, old_store, new_store):
        self.old_store = old_store  # Supabase/Qdrant
        self.new_store = new_store  # HANA Vector Engine

    def add_documents(self, docs):
        # Write to both
        self.old_store.add_documents(docs)
        self.new_store.add_documents(docs)

    def similarity_search(self, query, k=5):
        # Read from old (for now)
        return self.old_store.similarity_search(query, k)

# Usage during migration
dual_store = DualVectorStore(
    old_store=supabase_vector_store,
    new_store=hana_vector_store
)
```

**Step 2: Backfill Historical Data (Week 2)**
```python
from tqdm import tqdm

def migrate_to_hana(source_store, target_store):
    """Migrate all existing vectors to HANA."""

    # Get all vectors from source (paginated)
    page_size = 1000
    offset = 0
    total_migrated = 0

    while True:
        # Fetch batch from source
        batch = source_store.get_batch(offset=offset, limit=page_size)

        if not batch:
            break  # No more data

        # Write to HANA
        target_store.add_documents(batch)

        total_migrated += len(batch)
        offset += page_size

        print(f"Migrated: {total_migrated} vectors...")

    print(f"✅ Migration complete: {total_migrated} vectors")

# Run migration (fuera de horario pico)
migrate_to_hana(supabase_vector_store, hana_vector_store)
```

**Step 3: A/B Testing (Week 3)**
```python
import random

def query_with_ab_test(query, k=5):
    """50% queries to old, 50% to new (for testing)."""

    if random.random() < 0.5:
        results = old_store.similarity_search(query, k)
        source = "old"
    else:
        results = new_store.similarity_search(query, k)
        source = "new"

    # Log for comparison
    log_query(query, results, source)

    return results
```

**Step 4: Full Cutover (Week 4)**
```python
# Switch reads to HANA
def similarity_search(self, query, k=5):
    return self.new_store.similarity_search(query, k)  # HANA only

# Stop dual-write after 1 week of stable HANA operation
def add_documents(self, docs):
    return self.new_store.add_documents(docs)  # HANA only
```

**Step 5: Decommission Old Store (Week 5)**
```bash
# Delete Supabase project (cancel subscription)
# Delete Qdrant Cloud cluster
# Save final backup for audit trail
```

**Timeline**: **4-5 weeks** for zero-downtime migration

---

## Pricing Calculator

### Scenario: 200K Vectors, 150K Queries/Month

| Option | Setup | Monthly Cost | Annual Cost | Notes |
|--------|-------|--------------|-------------|-------|
| **Supabase Free** | $0 | $0 | $0 | Max 500MB, 50K queries/mo |
| **Supabase Pro** | $0 | $25 | $300 | 8GB storage, 500K queries/mo |
| **Qdrant Cloud** | $0 | $100 | $1,200 | 1 node, 2 vCPU (sin quantization) |
| **Qdrant Cloud (optimized)** | $0 | $50 | $600 | Con scalar quantization (50% savings) |
| **Self-Hosted Qdrant (AWS)** | $500 | $45 | $1,040 | t3.medium + EBS + monitoring |
| **Pinecone Serverless** | $0 | $70 | $840 | Auto-scaling, easiest setup |
| **HANA Vector Engine** | $0 | $0 | $0 | If customer has HANA Cloud |

**Recommendation por Budget**:
- **$0-300/year**: Supabase
- **$300-1,200/year**: Qdrant Cloud (optimized)
- **On-Premise**: Self-Hosted Qdrant
- **Cliente con HANA**: HANA Vector Engine (free)

---

## Implementation Checklist

### Week 1: Evaluation & Setup
- [ ] Determine customer's constraints (budget, on-premise, GDPR)
- [ ] Choose vector DB from decision tree
- [ ] Sign up for service (Supabase/Qdrant Cloud) OR provision EC2 (self-hosted)
- [ ] Create test collection with 1,000 sample vectors
- [ ] Verify query latency (<50ms P95)

### Week 2: Data Ingestion
- [ ] Extract TIER 1 knowledge sources (DUMPs, RICEFW specs)
- [ ] Chunk documents (800-1000 chars, 20% overlap)
- [ ] Generate embeddings (Cohere embed-v3)
- [ ] Upload to vector DB (batch size 100-500)
- [ ] Verify: 50K+ vectors indexed

### Week 3: Integration & Testing
- [ ] Integrate with LangChain RAG pipeline
- [ ] Implement hybrid search (if supported)
- [ ] Add metadata filtering (SAP system, functional area)
- [ ] Test 100 sample queries
- [ ] Measure: Context Precision >85%, Latency <500ms

### Week 4: Production Deployment
- [ ] Setup monitoring (query latency, error rate)
- [ ] Configure backups (daily snapshots)
- [ ] Enable alerts (latency >100ms, errors >1%)
- [ ] Pilot with 10 power users
- [ ] Collect feedback (NPS, qualitative)

### Week 5-8: Scale & Optimize
- [ ] Add TIER 2 knowledge sources (+70K vectors)
- [ ] Enable caching (30% duplicate queries)
- [ ] Optimize embeddings (quantization if needed)
- [ ] Roll out to 100 users
- [ ] Plan migration to HANA (if customer upgrades)

---

## FAQ

### Q: ¿Puedo cambiar de vector DB después?

**A: Sí, pero requiere migración (2-4 semanas).**

Todas las opciones usan embeddings estándar (Cohere 1024 dims), así que los vectores son portables. Solo necesitas:
1. Export vectors de DB vieja
2. Import vectors a DB nueva
3. Verificar accuracy (A/B test)
4. Cutover gradual

**Tip**: Usa LangChain abstractions para minimizar vendor lock-in.

### Q: ¿Self-hosted realmente vale la pena?

**A: Solo si**:
- >500K vectors (savings vs managed)
- On-premise requirement (no opción)
- Tienes DevOps team (mantenimiento gratis)

Para <500K vectors, managed es **always cheaper** considerando time cost.

### Q: ¿Qué pasa si el cliente upgradia a HANA después?

**A: Migration path clara (4-5 semanas, zero-downtime).**

Ver sección "Migration Path" arriba. TL;DR:
1. Dual-write (new + old)
2. Backfill historical data
3. A/B test
4. Cutover gradual
5. Decommission old

### Q: ¿Supabase Free Tier es suficiente para POC?

**A: Sí, para 1-3 clientes piloto (<100K vectors).**

Limits:
- 500MB storage = ~50K-100K vectors
- 50K queries/month = ~1,600 queries/day
- 2GB bandwidth/month

Si haces POC con 3 clientes × 10 users × 50 queries/day = 1,500 queries/day → **dentro del límite**.

### Q: ¿Cómo escalar de 200K a 2M vectors?

**Supabase**: Upgrade a Team tier ($599/mo para 32GB)
**Qdrant Cloud**: Add nodes (auto-scaling) o enable compression
**Self-Hosted**: Add more EC2 instances (Qdrant supports clustering)
**HANA**: No action needed (scales automatically)

---

## Conclusión: Recomendación por Fase

### Fase 1: MVP (Meses 1-3, 1-5 clientes)
**→ Supabase Free/Pro ($0-25/mo)**
- Deploy en 15 minutos
- Costo casi $0
- Suficiente para validar product-market fit

### Fase 2: Growth (Meses 4-12, 5-20 clientes)
**→ Qdrant Cloud ($50-100/mo)**
- Best performance (<10ms queries)
- Hybrid search built-in
- Auto-scaling cuando creces

### Fase 3: Scale (Año 2+, 20-50 clientes)
**→ Migrate to HANA Vector Engine ($0)**
- Clientes ya tienen HANA Cloud (S/4HANA migration wave)
- Zero cost incremental
- Migration zero-downtime (4 semanas)

### On-Premise Path (Any Phase)
**→ Self-Hosted Qdrant ($40-60/mo)**
- GDPR compliance
- Air-gapped SAP systems
- Full data control

---

**Document Owner**: [Your Name]
**Last Updated**: Enero 2025
**Next Review**: Después del primer cliente piloto

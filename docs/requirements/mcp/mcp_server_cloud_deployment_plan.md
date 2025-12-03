# MCP Server Cloud Deployment - Investigación y Plan de Implementación

**Fecha**: 2025-01-25
**Versión**: 1.0
**Estado**: Propuesta
**Autor**: Crystal Development Team

---

## 1. Resumen Ejecutivo

Este documento presenta las opciones de arquitectura e infraestructura para desplegar el MCP Server Java (Spring Boot + SAP JCo) en la nube, permitiendo que usuarios externos lo consuman vía HTTP sin necesidad de compartir el proyecto completo.

### 1.1 Desafío Principal

El MCP Server requiere **conectividad RFC nativa a SAP** mediante SAP JCo, lo cual presenta restricciones:

- **Librerías nativas**: JCo requiere `libsapjco3.so/.dylib/.dll` específicas por plataforma
- **Conectividad SAP**: Muchos sistemas SAP solo son accesibles vía VPN corporativa
- **Seguridad**: Las credenciales SAP no deben exponerse en código ni configuraciones públicas

### 1.2 Decisión Recomendada

| Escenario | Arquitectura Recomendada |
|-----------|--------------------------|
| SAP solo accesible por VPN | AWS EC2 + Site-to-Site VPN |
| SAP BTP / Cloud Connector existente | SAP BTP Cloud Foundry |
| Multi-tenant / Máxima flexibilidad | Arquitectura Híbrida (Gateway + Agent) |
| PoC / Demo rápido | Google Cloud Run |

---

## 2. Contexto Técnico

### 2.1 Stack Actual del MCP Server

```
┌─────────────────────────────────────────┐
│         MCP Server Java                 │
├─────────────────────────────────────────┤
│  Framework    : Spring Boot 3.4.0       │
│  MCP SDK      : Spring AI MCP 1.1.0-M4  │
│  SAP Connector: SAP JCo 3.1.x           │
│  Java         : 21 LTS                  │
│  Transport    : STDIO (actual)          │
│                 HTTP (objetivo)         │
└─────────────────────────────────────────┘
```

### 2.2 MCP Transport: De STDIO a Streamable HTTP

El protocolo MCP soporta múltiples transportes:

| Transport | Uso | Estado (2025) |
|-----------|-----|---------------|
| **STDIO** | Local, Claude Desktop | Estable, producción |
| **HTTP + SSE** | Remoto, legacy | **Deprecado** (Nov 2024) |
| **Streamable HTTP** | Remoto, producción | **Recomendado** (Mar 2025) |

**Streamable HTTP** consolida envío (POST) y recepción (GET/SSE) en un único endpoint:

```
POST /mcp  → Enviar mensajes JSON-RPC
GET  /mcp  → Stream SSE para respuestas (opcional)
```

**Beneficios**:
- Servidores stateless para mejor escalabilidad
- Compatible con load balancers estándar
- Sin necesidad de sticky sessions
- 200x mejor rendimiento bajo carga vs HTTP+SSE legacy

### 2.3 Requisitos de Conectividad SAP

```
┌──────────────┐     RFC/3xx     ┌──────────────┐
│  MCP Server  │────────────────▶│  SAP System  │
│  (JCo)       │                 │  (ABAP)      │
└──────────────┘                 └──────────────┘
       │
       │ Requiere:
       ├─ Librería nativa JCo (platform-specific)
       ├─ Conectividad TCP puerto 33xx (dispatcher)
       ├─ SAP Router string (si aplica)
       └─ Credenciales SAP (user/password o SNC)
```

---

## 3. Arquitecturas Evaluadas

### 3.1 Opción A: SAP BTP Cloud Foundry + Cloud Connector

#### 3.1.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INTERNET                                      │
│    ┌─────────────┐                                                  │
│    │ MCP Client  │                                                  │
│    │ (Claude,    │                                                  │
│    │  Cursor)    │                                                  │
│    └──────┬──────┘                                                  │
│           │ HTTPS                                                   │
└───────────┼─────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    SAP BTP Cloud Foundry                             │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                     CF Application                              │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │            MCP Server (Spring Boot + JCo)                │  │ │
│  │  │  - Streamable HTTP Transport (:8080)                     │  │ │
│  │  │  - Spring AI MCP SDK                                     │  │ │
│  │  │  - JCo Destination Lookup                                │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  │                          │                                      │ │
│  │                          ▼                                      │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │              BTP Destination Service                      │  │ │
│  │  │  - Credenciales SAP encriptadas                          │  │ │
│  │  │  - Cloud Connector mapping                               │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              │                                       │
└──────────────────────────────┼───────────────────────────────────────┘
                               │ Reverse Invoke (outbound from CC)
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    ON-PREMISE / CLIENTE                              │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                  SAP Cloud Connector                            │ │
│  │  - Túnel seguro TLS                                            │ │
│  │  - Sin puertos entrantes requeridos                            │ │
│  │  - Virtual host mapping                                        │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
│                              │ RFC                                   │
│                              ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    SAP S/4HANA / ECC                            │ │
│  │  - SADT_REST_RFC_ENDPOINT                                      │ │
│  │  - ADT Services                                                │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.1.2 Análisis

| Aspecto | Evaluación |
|---------|------------|
| **JCo Nativo** | ✅ Soportado oficialmente en BTP CF |
| **VPN Requerida** | ❌ No - Cloud Connector usa reverse invoke |
| **Seguridad** | ✅ Enterprise-grade, Destination Service |
| **Auto-scaling** | ✅ CF native scaling |
| **Complejidad** | 🔶 Alta - requiere Cloud Connector on-prem |
| **Costo** | 💰 $300-500/mes (CF runtime + connectivity) |

#### 3.1.3 Prerequisitos

1. **SAP BTP Subaccount** con Cloud Foundry habilitado
2. **Cloud Connector** instalado en red on-premise
3. **Destination Service** configurado
4. **JCo buildpack** o contenedor custom con libs nativas

#### 3.1.4 Flujo de Autenticación

```
1. MCP Client → BTP App (OAuth2/API Key)
2. BTP App → Destination Service (bound service)
3. Destination Service → Cloud Connector (technical user)
4. Cloud Connector → SAP (RFC credentials from destination)
```

---

### 3.2 Opción B: AWS EC2 / GCP Compute + VPN Always-On

#### 3.2.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INTERNET                                      │
│    ┌─────────────┐                                                  │
│    │ MCP Client  │                                                  │
│    └──────┬──────┘                                                  │
│           │ HTTPS                                                   │
└───────────┼─────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      AWS VPC / GCP VPC                               │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                Application Load Balancer                        │ │
│  │  - SSL Termination                                             │ │
│  │  - Health checks                                               │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
│                              │                                       │
│                              ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    EC2 Instance (t3.medium)                     │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │            MCP Server (Spring Boot + JCo)                │  │ │
│  │  │  - Streamable HTTP Transport (:8080)                     │  │ │
│  │  │  - Native JCo libraries (linux-x64)                      │  │ │
│  │  │  - Credentials from Secrets Manager                      │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  │                          │                                      │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │         VPN Client (WireGuard/OpenVPN)                   │  │ │
│  │  │  - systemd service (always-on)                           │  │ │
│  │  │  - Auto-reconnect on failure                             │  │ │
│  │  │  - Route SAP traffic through tunnel                      │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              │                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              AWS Site-to-Site VPN / VPN Tunnel                  │ │
│  │  - IPSec / WireGuard                                           │ │
│  │  - Dual tunnels for redundancy                                 │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
└──────────────────────────────┼───────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    ON-PREMISE / CLIENTE                              │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                Customer Gateway / VPN Server                    │ │
│  │  - StrongSwan / Cisco / Fortinet                               │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
│                              │                                       │
│                              ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    SAP S/4HANA / ECC                            │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.2.2 Análisis

| Aspecto | Evaluación |
|---------|------------|
| **JCo Nativo** | ✅ Control total sobre libs nativas |
| **VPN** | ✅ Site-to-Site o client VPN always-on |
| **Seguridad** | ✅ AWS Secrets Manager, VPC isolation |
| **Auto-scaling** | 🔶 Manual o ASG con AMI custom |
| **Complejidad** | 🔶 Media - requiere gestión VPN |
| **Costo** | 💰 $50-150/mes (EC2 + VPN + ALB) |

#### 3.2.3 Opciones de VPN

| Tipo | Costo | Complejidad | Caso de Uso |
|------|-------|-------------|-------------|
| **AWS Site-to-Site VPN** | ~$36/mes + data | Baja | Corporate VPN gateway existente |
| **WireGuard en EC2** | $0 (solo EC2) | Media | VPN server propio del cliente |
| **OpenVPN Client** | $0 (solo EC2) | Media | VPN client estándar |
| **AWS Client VPN** | ~$0.05/hr/conn | Baja | Sin VPN server on-prem |

#### 3.2.4 Configuración WireGuard Always-On

```ini
# /etc/systemd/system/wg-quick@wg0.service.d/override.conf
[Service]
Restart=always
RestartSec=5

# /etc/wireguard/wg0.conf
[Interface]
PrivateKey = <EC2_PRIVATE_KEY>
Address = 10.200.200.2/32

[Peer]
PublicKey = <CUSTOMER_VPN_PUBLIC_KEY>
AllowedIPs = 10.0.0.0/8  # SAP network range
Endpoint = customer-vpn.example.com:51820
PersistentKeepalive = 25
```

---

### 3.3 Opción C: Google Cloud Run (Streamable HTTP)

#### 3.3.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INTERNET                                      │
│    ┌─────────────┐                                                  │
│    │ MCP Client  │                                                  │
│    └──────┬──────┘                                                  │
│           │ HTTPS                                                   │
└───────────┼─────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Google Cloud Run                                │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                 Cloud Run Service                               │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │      Container: MCP Server (Spring Boot + JCo)           │  │ │
│  │  │  - Streamable HTTP Transport (:8080)                     │  │ │
│  │  │  - JCo native libs in container                          │  │ │
│  │  │  - IAM authentication                                    │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  │  - Auto-scaling (0 to N instances)                             │ │
│  │  - Pay per request                                             │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              │                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              VPC Connector (Serverless VPC Access)              │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
│                              │                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              Cloud VPN / Interconnect                           │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
└──────────────────────────────┼───────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    ON-PREMISE / CLIENTE                              │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    SAP S/4HANA / ECC                            │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.3.2 Análisis

| Aspecto | Evaluación |
|---------|------------|
| **JCo Nativo** | ✅ Custom container con libs |
| **VPN** | 🔶 Requiere VPC Connector + Cloud VPN |
| **Seguridad** | ✅ IAM, Secret Manager |
| **Auto-scaling** | ✅ Nativo, scale-to-zero |
| **Complejidad** | 🔶 Media-Alta |
| **Costo** | 💰 $20-100/mes (pay-per-use) |

#### 3.3.3 Dockerfile para Cloud Run

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# Install glibc for JCo native libs
RUN apk add --no-cache gcompat libstdc++

WORKDIR /app

# Copy JCo native libraries
COPY lib/libsapjco3.so /usr/lib/
COPY lib/sapjco3.jar /app/lib/

# Copy application
COPY target/sap-mcp-server-*.jar /app/app.jar

# Cloud Run uses PORT env var
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.library.path=/usr/lib", "-jar", "/app/app.jar"]
```

---

### 3.4 Opción D: Arquitectura Híbrida (Gateway + Agent)

#### 3.4.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INTERNET                                      │
│    ┌─────────────┐                                                  │
│    │ MCP Client  │                                                  │
│    │ (Claude,    │                                                  │
│    │  Cursor)    │                                                  │
│    └──────┬──────┘                                                  │
│           │ HTTPS                                                   │
└───────────┼─────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│              CLOUD (Cloudflare Workers / Fly.io / Railway)           │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    MCP Gateway (Stateless)                      │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │  - Streamable HTTP endpoint                              │  │ │
│  │  │  - API Key / OAuth authentication                        │  │ │
│  │  │  - Rate limiting                                         │  │ │
│  │  │  - Request routing to agents                             │  │ │
│  │  │  - NO SAP credentials                                    │  │ │
│  │  │  - NO JCo (lightweight)                                  │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  │                          │                                      │ │
│  │  Agent Registry:         │                                      │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ tenant-a → wss://agent-a.tunnel.example.com             │   │ │
│  │  │ tenant-b → wss://agent-b.tunnel.example.com             │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ WebSocket / Cloudflare Tunnel
                               │
        ┌──────────────────────┴──────────────────────┐
        │                                             │
        ▼                                             ▼
┌───────────────────────────────────┐  ┌───────────────────────────────────┐
│      TENANT A (On-Premise)        │  │      TENANT B (On-Premise)        │
│  ┌─────────────────────────────┐  │  │  ┌─────────────────────────────┐  │
│  │     SAP MCP Agent           │  │  │  │     SAP MCP Agent           │  │
│  │  - Java + JCo               │  │  │  │  - Java + JCo               │  │
│  │  - Cloudflare Tunnel client │  │  │  │  - Cloudflare Tunnel client │  │
│  │  - Local SAP credentials    │  │  │  │  - Local SAP credentials    │  │
│  │  - Executes MCP tools       │  │  │  │  - Executes MCP tools       │  │
│  └──────────────┬──────────────┘  │  │  └──────────────┬──────────────┘  │
│                 │ RFC              │  │                 │ RFC              │
│                 ▼                  │  │                 ▼                  │
│  ┌─────────────────────────────┐  │  │  ┌─────────────────────────────┐  │
│  │      SAP S/4HANA (A)        │  │  │  │      SAP S/4HANA (B)        │  │
│  └─────────────────────────────┘  │  │  └─────────────────────────────┘  │
└───────────────────────────────────┘  └───────────────────────────────────┘
```

#### 3.4.2 Análisis

| Aspecto | Evaluación |
|---------|------------|
| **JCo Nativo** | ✅ En agente on-premise |
| **VPN** | ❌ No necesaria - usa tunneling |
| **Seguridad** | ✅ Credenciales nunca salen de on-prem |
| **Multi-tenant** | ✅ Diseñado para múltiples clientes |
| **Complejidad** | 🔶 Alta - dos componentes |
| **Costo** | 💰 $20-80/mes (gateway) + agente on-prem |

#### 3.4.3 Componentes

**MCP Gateway (Cloud)**:
- Lenguaje: TypeScript/Node.js o Python (ligero)
- Framework: Express + MCP SDK o Hono (Cloudflare)
- Responsabilidades:
  - Exponer endpoint MCP público
  - Autenticación de clientes
  - Routing a agentes por tenant
  - NO contiene lógica SAP

**SAP MCP Agent (On-Premise)**:
- Lenguaje: Java (Spring Boot + JCo)
- Responsabilidades:
  - Ejecutar tools MCP
  - Conectar a SAP vía RFC
  - Mantener túnel con gateway
- Deployment: Docker, systemd service, o Windows service

#### 3.4.4 Tunneling Options

| Solución | Costo | Ventajas |
|----------|-------|----------|
| **Cloudflare Tunnel** | Gratis (básico) | Zero-trust, fácil setup |
| **ngrok** | $8-25/mes | Dominios custom, dashboard |
| **Tailscale** | Gratis (personal) | Mesh VPN, fácil |
| **Teleport** | Gratis (community) | Enterprise features |

---

## 4. Comparativa Consolidada

### 4.1 Matriz de Decisión

| Criterio | Peso | BTP + CC | AWS + VPN | Cloud Run | Híbrida |
|----------|------|----------|-----------|-----------|---------|
| JCo Nativo | 25% | ✅ 10 | ✅ 10 | ✅ 10 | ✅ 10 |
| Conectividad SAP | 20% | ✅ 10 | ✅ 9 | 🔶 7 | ✅ 10 |
| Costo | 15% | ❌ 4 | 🔶 7 | ✅ 9 | ✅ 8 |
| Complejidad Setup | 15% | ❌ 4 | 🔶 6 | 🔶 6 | ❌ 5 |
| Auto-scaling | 10% | ✅ 9 | 🔶 5 | ✅ 10 | 🔶 6 |
| Multi-tenant | 10% | 🔶 6 | ❌ 4 | 🔶 6 | ✅ 10 |
| Enterprise Ready | 5% | ✅ 10 | ✅ 9 | ✅ 8 | 🔶 7 |
| **TOTAL** | 100% | **7.05** | **7.15** | **7.85** | **8.05** |

### 4.2 Resumen de Costos Mensuales

| Arquitectura | Mínimo | Típico | Enterprise |
|--------------|--------|--------|------------|
| SAP BTP + Cloud Connector | $300 | $500 | $1,000+ |
| AWS EC2 + VPN | $50 | $100 | $300 |
| Google Cloud Run | $20 | $60 | $150 |
| Híbrida (Gateway + Agent) | $20 | $50 | $150 |

---

## 5. Plan de Implementación Recomendado

### 5.1 Fase 0: Preparación (1 semana)

#### 5.1.1 Modificar MCP Server para HTTP Transport

**Archivo**: `src/main/java/com/crystal/mcp/sapserver/config/McpServerConfiguration.java`

```java
@Configuration
public class McpServerConfiguration {

    @Bean
    public McpServer mcpServer(McpToolsProvider toolsProvider) {
        return McpServer.builder()
            .serverInfo(new ServerInfo("giralmcp", "1.0.0"))
            .transport(new StreamableHttpTransport("/mcp"))  // Nuevo transport
            .tools(toolsProvider.getTools())
            .build();
    }
}
```

**Dependencia Maven** (actualizar `pom.xml`):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webmvc</artifactId>
    <version>1.1.0-M4</version>
</dependency>
```

#### 5.1.2 Externalizar Credenciales SAP

**application.yml**:

```yaml
sap:
  connection:
    ashost: ${SAP_ASHOST}
    sysnr: ${SAP_SYSNR}
    client: ${SAP_CLIENT}
    user: ${SAP_USER}
    passwd: ${SAP_PASSWD}
    lang: ${SAP_LANG:EN}
    router: ${SAP_ROUTER:}
```

### 5.2 Fase 1: PoC en Cloud Run (2 semanas)

#### 5.2.1 Objetivo

Validar que el MCP Server funciona con Streamable HTTP Transport en un entorno serverless.

#### 5.2.2 Tareas

| # | Tarea | Duración |
|---|-------|----------|
| 1.1 | Crear Dockerfile con JCo libs | 2 días |
| 1.2 | Configurar Cloud Run service | 1 día |
| 1.3 | Setup Cloud VPN (si SAP accesible) | 3 días |
| 1.4 | Testing e2e con Claude/Cursor | 2 días |
| 1.5 | Documentar y ajustar | 2 días |

#### 5.2.3 Dockerfile Completo

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY lib ./lib
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Install dependencies for JCo
RUN apt-get update && apt-get install -y \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JCo native libraries
COPY lib/libsapjco3.so /usr/lib/
COPY lib/sapjco3.jar /app/lib/

# Copy application
COPY --from=builder /build/target/sap-mcp-server-*.jar /app/app.jar

# Cloud Run requirement
ENV PORT=8080
ENV JAVA_OPTS="-Djava.library.path=/usr/lib -Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

#### 5.2.4 Cloud Run Deployment

```bash
# Build and push image
gcloud builds submit --tag gcr.io/PROJECT_ID/sap-mcp-server

# Deploy to Cloud Run
gcloud run deploy sap-mcp-server \
  --image gcr.io/PROJECT_ID/sap-mcp-server \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-secrets="SAP_USER=sap-user:latest,SAP_PASSWD=sap-password:latest" \
  --set-env-vars="SAP_ASHOST=sap.example.com,SAP_SYSNR=00,SAP_CLIENT=100" \
  --vpc-connector=sap-vpc-connector \
  --memory=1Gi \
  --cpu=1 \
  --timeout=300
```

### 5.3 Fase 2: Arquitectura Híbrida (3-4 semanas)

#### 5.3.1 Objetivo

Implementar solución multi-tenant con gateway público y agentes on-premise.

#### 5.3.2 Componentes a Desarrollar

**MCP Gateway** (nuevo proyecto):

```
mcp-gateway/
├── src/
│   ├── index.ts           # Entry point
│   ├── auth/              # API key validation
│   ├── routing/           # Tenant → Agent routing
│   └── websocket/         # Agent connection manager
├── package.json
└── wrangler.toml          # Cloudflare Workers config
```

**SAP MCP Agent** (basado en proyecto actual):

```
sap-mcp-agent/
├── src/main/java/
│   ├── agent/
│   │   ├── TunnelClient.java      # Cloudflare Tunnel connection
│   │   └── AgentRegistration.java # Register with gateway
│   ├── tool/                       # MCP tools (existentes)
│   └── service/                    # SAP services (existentes)
├── Dockerfile
└── docker-compose.yml
```

#### 5.3.3 Flujo de Comunicación

```
1. MCP Client POST /mcp → Gateway (Cloudflare)
2. Gateway extrae tenant_id del API key
3. Gateway busca WebSocket del agente registrado
4. Gateway forward request al Agent vía WS
5. Agent ejecuta tool SAP (JCo/RFC)
6. Agent retorna resultado vía WS
7. Gateway retorna response al MCP Client
```

#### 5.3.4 Tareas Detalladas

| # | Tarea | Duración | Dependencias |
|---|-------|----------|--------------|
| 2.1 | Diseñar API Gateway-Agent | 2 días | - |
| 2.2 | Implementar MCP Gateway | 5 días | 2.1 |
| 2.3 | Implementar Tunnel Client en Agent | 3 días | 2.1 |
| 2.4 | Setup Cloudflare Tunnel | 2 días | 2.2, 2.3 |
| 2.5 | Implementar tenant routing | 2 días | 2.2 |
| 2.6 | Testing multi-tenant | 3 días | 2.4, 2.5 |
| 2.7 | Documentación deployment | 2 días | 2.6 |

### 5.4 Fase 3: Productización (2 semanas)

#### 5.4.1 Seguridad

- [ ] Rate limiting por API key
- [ ] Audit logging de todas las operaciones
- [ ] Rotación automática de credenciales SAP
- [ ] Encriptación de datos en tránsito y reposo

#### 5.4.2 Observabilidad

- [ ] Métricas Prometheus/Grafana
- [ ] Distributed tracing (Jaeger)
- [ ] Alertas (PagerDuty/OpsGenie)
- [ ] Dashboard de health checks

#### 5.4.3 Alta Disponibilidad

- [ ] Multi-region gateway deployment
- [ ] Agent health monitoring
- [ ] Automatic failover

---

## 6. Configuración para Clientes

### 6.1 Claude Desktop (Streamable HTTP)

```json
{
  "mcpServers": {
    "sap-mcp": {
      "transport": {
        "type": "streamable-http",
        "url": "https://mcp-gateway.example.com/mcp",
        "headers": {
          "Authorization": "Bearer <API_KEY>"
        }
      }
    }
  }
}
```

### 6.2 Cursor IDE

```json
{
  "mcp": {
    "servers": {
      "sap-mcp": {
        "url": "https://mcp-gateway.example.com/mcp",
        "apiKey": "<API_KEY>"
      }
    }
  }
}
```

### 6.3 Programático (Python)

```python
from mcp import Client
from mcp.transports import StreamableHTTPTransport

transport = StreamableHTTPTransport(
    url="https://mcp-gateway.example.com/mcp",
    headers={"Authorization": "Bearer <API_KEY>"}
)

async with Client(transport) as client:
    result = await client.call_tool("get_class_source", {
        "className": "CL_ABAP_CHAR_UTILITIES"
    })
    print(result)
```

---

## 7. Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| JCo incompatible con container runtime | Media | Alto | Testing exhaustivo en PoC, fallback a VM |
| Latencia alta Gateway→Agent | Media | Medio | Edge deployment, connection pooling |
| Cloud Connector setup complejo | Alta | Medio | Documentación detallada, soporte SAP Basis |
| VPN corporativa inestable | Media | Alto | Retry logic, circuit breaker, alertas |
| Costos exceden presupuesto | Baja | Medio | Monitoring de costos, auto-scaling limits |

---

## 8. Métricas de Éxito

| Métrica | Target | Medición |
|---------|--------|----------|
| Latencia p95 | < 500ms | Prometheus histogram |
| Disponibilidad | > 99.5% | Uptime monitoring |
| Time to onboard tenant | < 1 día | Manual tracking |
| Errores de conexión SAP | < 0.1% | Error rate metric |
| Costo por request | < $0.001 | Cloud billing |

---

## 9. Referencias

### 9.1 Documentación Oficial

- [MCP Specification - Transports](https://modelcontextprotocol.io/docs/concepts/transports)
- [Spring AI MCP SDK](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [SAP Cloud Connector](https://help.sap.com/docs/connectivity/sap-btp-connectivity-cf/cloud-connector)
- [Google Cloud Run - Host MCP Servers](https://cloud.google.com/run/docs/host-mcp-servers)

### 9.2 Guías de Deployment

- [Koyeb - Deploy Remote MCP Servers](https://koyeb.com/tutorials/deploy-remote-mcp-servers)
- [Fly.io - Deploying Remote MCP Servers](https://fly.io/docs/blueprints/remote-mcp-servers/)
- [Railway MCP Server](https://blog.railway.com/p/railway-mcp-server)
- [Cloudflare - Streamable HTTP MCP](https://blog.cloudflare.com/streamable-http-mcp-servers)

### 9.3 Artículos Técnicos

- [Production MCP Patterns](https://nitishagar.medium.com/production-mcp-patterns)
- [StreamableHTTP for Scalable MCP Deployments](https://mcpcat.io/guides/setting-up-streamablehttp)
- [SAP JCo on BTP Cloud Foundry](https://community.sap.com/t5/technology-blog-posts/technical-user-propagation-from-jco-towards-on-premises)

---

## 10. Aprobaciones

| Rol | Nombre | Fecha | Firma |
|-----|--------|-------|-------|
| Tech Lead | | | |
| Arquitecto | | | |
| Product Owner | | | |
| Security | | | |

---

**Documento generado**: 2025-01-25
**Próxima revisión**: Después de PoC (Fase 1)

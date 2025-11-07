# PR: Evaluación Migración MCP Server Python → Java

**ID**: PR-MCP-JAVA-002
**Estado**: ✅ Investigación Completada → **APROBADA TÉCNICAMENTE**
**Fecha Creación**: 2025-11-07
**Última Actualización**: 2025-11-07
**Responsable**: Architecture Team
**Prioridad**: Alta (Post-Phase 1-4)

---

## User Story

Como desarrollador MCP
Quiero migrar mi MCP server escrito en Python hacia otro lenguaje (puede ser JAVA)
Para tener mejor performance y soporte del RFC SDK que usa JAVA, y poder distribuir el MCP server de forma más sencilla, pues ahora con Python y PyRFC no tengo buena estabilidad y cada que se configura en otro ordenador (Linux, Windows o Mac) no funciona muy bien.

---

## Criterios de Aceptación

- [x] Evaluar estrategia para empaquetar el servidor y distribuirlo de forma sencilla (Docker)
- [x] Que pueda ser fácilmente configurable en otros asistentes AI
- [x] Tener en cuenta que las conexiones se hacen por VPN (acceso a red del host)
- [x] Evaluar si crear MCP Server en Python y llamar tools escritas en JAVA
- [x] Evaluar si reescribir tools en JAVA con `sapjco3`
- [x] Evaluar si crear el MCP Server desde JAVA (nueva solución)
- [x] Buscar mejores prácticas de la industria
- [x] Validar que MCP Server con Java sea estable y robusto
- [x] Proponer tipo de proyecto de migración a JAVA
- [x] Documentación concisa (sin código fuente extenso)

---

## Hallazgo Crítico 🚨

### PyRFC Proyecto Archivado (Diciembre 2024)

> "SAP can no longer maintain PyRFC due to changing priorities. The latest version is built with an older RFC SDK which is no longer supported by SAP." — **GitHub SAP/PyRFC**

**Implicaciones:**
- ❌ **Repositorio oficialmente archivado**
- ❌ **No acepta Pull Requests ni issues**
- ❌ **RFC SDK obsoleto** (versión anterior no soportada)
- ❌ **Múltiples problemas sin resolver** (2024):
  - Compilación fallida en Raspberry Pi, Databricks
  - Errores `Module 'pyrfc' has no attribute Connection`
  - Builds locales requeridas (no wheels precompilados)

**Conclusión**: **Mantener PyRFC a largo plazo NO ES VIABLE** ⚠️

---

## Investigación Completada

### Documento de Investigación

**Ver análisis completo**: [`docs/research/abap_mcp_tools_strategy_2025.md`](/docs/research/abap_mcp_tools_strategy_2025.md)
- **Sección 13**: Evaluación Migración a Java
- **Sección 14**: Estrategia Docker & VPN
- **Sección 15**: Recomendación Final Consolidada

---

## Comparación SAP JCo vs PyRFC

| Aspecto | PyRFC | SAP JCo | Ganador |
|---------|-------|---------|---------|
| **Mantenimiento SAP** | ❌ Archivado (Dic 2024) | ✅ Soporte oficial activo | **JCo** |
| **Estabilidad Cross-Platform** | ❌ Compilación manual | ✅ Binarios precompilados | **JCo** |
| **Thread Safety** | ❌ NO thread-safe | ✅ Thread-safe nativo | **JCo** |
| **Connection Pooling** | ❌ Implementación manual | ✅ Pooling automático | **JCo** |
| **Distribución** | ❌ Compilar en cada máquina | ✅ JAR + natives | **JCo** |
| **Instalación** | ❌ Compleja (setup.sh) | ✅ Simple (JAR + classpath) | **JCo** |
| **Performance** | ⚠️ AsyncIO ayuda | ✅ JNI optimizado | **JCo** |
| **Production Ready** | ❌ Inestable (según PR) | ✅ Estable en producción | **JCo** |

**Evidencia Thread Safety:**

**PyRFC:**
> "The SAP NW RFC Library is not thread safe, neither the pyrfc is. The recommended design is to instantiate a pool of client instances." — **GitHub Issue #46**

**JCo:**
> "JCoDestination will internally create and use distinct RFC client connection objects for each session, and every thread is treated as separate session by default." — **Stack Overflow**

**JCo Connection Pooling:**
> "In JCo 3.0/3.1 the pooling is done automatically within JCo runtime, if the destination is configured accordingly." — **SAP Documentation**

---

## MCP SDK en Java

### ✅ SDK Oficial Disponible: Spring AI MCP

**Lanzamiento**: Febrero 2025 (Spring AI + Anthropic)

**Repositorio**: [modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)

**Requisitos**:
- Java 17+
- Licencia MIT

**Spring Boot Starters:**

```xml
<!-- STDIO Transport (Claude Desktop) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>

<!-- HTTP SSE (Spring MVC) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>

<!-- HTTP SSE (WebFlux Reactive) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
</dependency>
```

**Tool Definition (Declarativo):**

```java
@Tool(description = "Get ABAP class source code")
public String getClassSource(
    @ToolParam(description = "Class name") String className,
    @ToolParam(description = "Version: active|inactive") String version
) {
    return classService.getSource(className, version);
}
```

**Características:**
- ✅ Registro automático de tools (component scanning)
- ✅ Transport options: STDIO, HTTP SSE, WebSocket
- ✅ Session-based architecture (v0.8.0+)

---

## Matriz de Decisión Arquitectónica

### Opción A: Full Java Migration 🟢 **RECOMENDADA**

**Stack Propuesto:**
- **Spring Boot 3.x** + Spring AI MCP Java SDK
- **SAP JCo 3.1.x** para conectividad SAP
- **Maven/Gradle** para build management
- **Docker** para empaquetado y distribución

**Pros:**
- ✅ **Estabilidad**: JCo mantenido por SAP, thread-safe nativo
- ✅ **SDK Oficial**: Spring AI MCP con soporte activo (2025)
- ✅ **Distribución**: JAR ejecutable + Docker image simple
- ✅ **Pooling**: Connection pooling automático integrado
- ✅ **Ecosystem**: Spring Boot production-ready (Actuator, Metrics)
- ✅ **Observabilidad**: OpenTelemetry + Spring Actuator nativo
- ✅ **Cross-Platform**: Binarios JCo oficiales multi-platform

**Cons:**
- ❌ **Rewrite Completo**: 59 tools + 17 services a migrar
- ❌ **Learning Curve**: Equipo debe aprender Spring AI MCP
- ❌ **JCo Licensing**: ⚠️ Verificar licenciamiento SAP para producción
- ❌ **Native Libraries**: Distribución de `sapjco3.dll`/`.so` requerida

**Esfuerzo Estimado**: 6-8 semanas (1 desarrollador Java senior)

---

### Opción B: Arquitectura Híbrida 🟡 **NO RECOMENDADA**

**Variante B1: Python MCP + Java Tools (gRPC)**

```
Claude Desktop → Python MCP Server → gRPC → Java Service (JCo)
```

**Pros:**
- ✅ Mantiene capa MCP en Python (conocimiento existente)
- ✅ Reusa JCo para estabilidad SAP

**Cons:**
- ❌ **Complejidad Dual**: Mantener 2 runtimes (Python + Java)
- ❌ **Performance Overhead**: Serialización gRPC añade latencia
- ❌ **Deployment Complejo**: Docker multi-stage builds
- ❌ **Debugging Difícil**: Trazar errores cross-language
- ❌ **PyRFC Archivado**: Problema original no resuelto

**Performance gRPC:**
> "Python gRPC servers using AsyncIO offer significant performance boost. However, streaming RPCs create extra threads, making them much slower than unary RPCs in Python." — **gRPC Best Practices**

**Variante B2: Python MCP + Java JNI**

**Cons adicionales:**
- ❌ **JNI Complexity**: Memoria management, JVM lifecycle
- ❌ **Portability**: JNI shared libraries builds por plataforma
- ❌ **Crash Risk**: Errores JNI crashean todo el proceso

**Conclusión**: Añade complejidad sin resolver el problema raíz.

---

### Opción C: Mejorar Python Actual 🔴 **NO VIABLE**

**Estrategias Consideradas:**
1. Fork PyRFC y mantener internamente
2. Contribuir fixes a PyRFC
3. Reimplementar pooling robusto

**Por qué NO es viable:**
- ❌ **Proyecto Archivado**: SAP no acepta PRs
- ❌ **Mantenimiento a Largo Plazo**: Requiere expertise RFC SDK interno
- ❌ **Thread Safety**: Problema arquitectónico del NWRFC SDK
- ❌ **Cross-Platform**: Compilación local siempre necesaria

---

## Empaquetado y Distribución

### Estrategia Docker con VPN

**Problema**: MCP Server en container necesita acceso a SAP via VPN del host.

**✅ Solución Recomendada: Network Mode `host`**

```yaml
# docker-compose.yml
services:
  mcp-server:
    image: myorg/mcp-sap-server:latest
    network_mode: host  # Container usa red del host directamente
    environment:
      SAP_ASHOST: sap.internal.corp
      SAP_ROUTER: /H/vpn-router/S/3299
```

**Ventajas:**
- ✅ **VPN Transparente**: Container accede a recursos VPN sin configuración extra
- ✅ **Sin Routing Custom**: No requiere tablas de routing
- ✅ **Bajo Overhead**: Latencia mínima vs bridge mode

**Desventajas:**
- ⚠️ **Solo Linux**: No funciona en Mac/Windows Docker Desktop
- ⚠️ **Menos Aislamiento**: Container comparte IPs del host
- ⚠️ **Port Conflicts**: Puertos deben estar libres en host

**Alternativa (Mac/Windows):**

```yaml
services:
  vpn:
    image: openvpn-client
    cap_add: [NET_ADMIN]
    volumes: [./vpn-config:/etc/openvpn]

  mcp-server:
    network_mode: "service:vpn"  # Usa red del container VPN
    depends_on: [vpn]
```

### Docker MCP Catalog (Distribución Oficial)

**Estadísticas 2025:**
- **220+ MCP servers** en catálogo oficial
- **1M+ pulls** en primeras semanas
- Registry: [docker/mcp-registry](https://github.com/docker/mcp-registry)
- Hub: [hub.docker.com/mcp](https://hub.docker.com/mcp)

**Proceso de Publicación:**
1. Containerizar MCP server
2. Submit PR a `docker/mcp-registry`
3. Revisión y aprobación Docker
4. **Disponible en <24 horas** en Docker Desktop

**Ventajas:**
- ✅ **Discoverability**: Usuarios encuentran via Docker Hub
- ✅ **Seguridad**: Imágenes firmadas digitalmente
- ✅ **Versionado**: Tags semánticos (v1.0.0, v1.1.0)
- ✅ **Multi-Platform**: ARM64 + AMD64 builds automáticos

### Dockerfile Multi-Stage con JCo

**Problema JCo:**
> "It is not allowed to rename or repackage the original archive 'sapjco3.jar'. The default system class loader does not handle jar files inside jar files." — **SAP JCo Docs**

**Solución: External Dependencies**

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml src ./
COPY lib/sapjco3.jar ./lib/
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Native JCo libraries (platform-specific)
COPY lib/sapjco3.jar /app/lib/
COPY lib/libsapjco3.so /usr/lib/  # Linux x64

COPY --from=builder /app/target/mcp-server.jar /app/

ENV LD_LIBRARY_PATH=/usr/lib:$LD_LIBRARY_PATH

CMD ["java", "-Djava.library.path=/usr/lib", "-jar", "mcp-server.jar"]
```

**Multi-Platform Builds:**

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag myorg/mcp-sap-server:1.0.0 \
  --push .
```

**⚠️ GraalVM Native Image NO Viable:**
> "Compiling a project containing SAP JCo library fails with a fatal error in GraalVM 23.0.0 CE." — **GitHub Issue #6970**

**Conclusión**: Deploy como **JAR tradicional**, NO native image.

---

## Observabilidad y Monitoreo

### Problema MCP Observability

> "Without observability, it is difficult to trace how an agent made a decision, which tools were invoked, or why certain failures occurred. MCP servers are increasingly labelled as black boxes." — **Glama AI Blog (2025)**

### Soluciones 2025

| Solución | Fortaleza | Integración |
|----------|-----------|-------------|
| **Sentry** | MCP específico, 1-line instrumentation | JavaScript SDK |
| **OpenTelemetry** | Estándar industria | ✅ Java Agent, Spring Boot |
| **Moesif** | JSON-RPC deep visibility | API platform |
| **SigNoz** | Open-source, self-hosted | OpenTelemetry backend |

### Health Checks Pattern

```java
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/liveness")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("ALIVE");
    }

    @GetMapping("/readiness")
    public ResponseEntity<HealthStatus> readiness() {
        HealthStatus status = new HealthStatus();
        status.setSapConnection(checkSapConnection());
        return status.isHealthy()
            ? ResponseEntity.ok(status)
            : ResponseEntity.status(503).body(status);
    }
}
```

**Spring Boot Actuator:**

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Recomendación Final

### ✅ **APROBADA: Full Java Migration con Spring AI MCP**

**Justificación:**

1. **PyRFC Archivado (Crítico)**: NO VIABLE a largo plazo
2. **JCo Estabilidad**: Thread-safe, pooling automático, multi-platform
3. **MCP SDK Oficial**: Spring AI (Feb 2025) con soporte activo
4. **Distribución**: Docker MCP Catalog oficial simplifica deployment
5. **Ecosystem**: Spring Boot production-ready desde día 1

**Riesgos Mitigados:**

| Riesgo | Mitigación |
|--------|------------|
| Cross-platform builds | Docker multi-arch automático |
| Native libraries JCo | Externalizadas en Dockerfile |
| VPN corporate | `network_mode: host` transparente |
| Mantenimiento | Stack Spring Boot estándar |
| Observabilidad | OpenTelemetry + Spring Actuator |

---

## Plan de Migración (Phase 5)

### Duración: 6-8 semanas

#### 1. Setup & POC (1 semana)
- Spring Boot 3.x + Spring AI MCP starter
- Integrar SAP JCo 3.1.x
- Implementar 3-5 tools críticos (POC)
- Docker image con JCo native libraries
- Test conectividad SAP via VPN

#### 2. Core Services (2 semanas)
- Migrar `ClassService`, `ProgramService`, `SearchService`
- Implementar connection pooling JCo
- Unit tests (JUnit 5 + Mockito)
- Integration tests con SAP sandbox

#### 3. Remaining Tools (2 semanas)
- Migrar 59 MCP tools restantes
- Transport management tools
- CDS/RAP tools
- Enhancement tools

#### 4. Production Ready (1 semana)
- Health checks (liveness/readiness)
- OpenTelemetry instrumentation
- Spring Actuator metrics
- Docker multi-platform builds

#### 5. Testing & Documentation (1 semana)
- Load testing (JMeter/Gatling)
- Security scanning (Snyk, Trivy)
- Documentation técnica
- Migration guide desde Python

#### 6. Deployment & Rollout (1 semana)
- Deploy staging environment
- User acceptance testing
- Production deployment
- Monitoring setup (Prometheus/Grafana)

---

## Decisiones Pendientes

| Decisión | Responsable | Deadline |
|----------|-------------|----------|
| ⚠️ **Verificar licenciamiento SAP JCo** | Procurement / Legal | Antes de iniciar Phase 5 |
| ✅ **Aprobar presupuesto Phase 5** | Engineering Manager | Post-Phase 4 |
| ✅ **Asignar desarrollador Java senior** | Team Lead | Post-Phase 4 |
| ✅ **Preparar infraestructura Docker** | DevOps | Post-Phase 4 |

---

## Próximos Pasos Inmediatos

1. ✅ **Completar Phase 1-4** (Optimización Python): 10-14 semanas
2. ⚠️ **Validar Licensing SAP JCo**: Contactar SAP Partner/Procurement
3. ✅ **Preparar POC Java**: Spring Boot + JCo (R&D paralelo)
4. ✅ **Documentar arquitectura target**: Target state (Java)
5. ✅ **Configurar CI/CD**: Docker builds + registry

---

## Referencias

- **Investigación Completa**: [`docs/research/abap_mcp_tools_strategy_2025.md`](/docs/research/abap_mcp_tools_strategy_2025.md#13-evaluaci%C3%B3n-migraci%C3%B3n-a-java)
- **Spring AI MCP SDK**: [spring.io/blog/2025/02/14/mcp-java-sdk-released-2/](https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2/)
- **Docker MCP Catalog**: [hub.docker.com/mcp](https://hub.docker.com/mcp)
- **SAP JCo Documentation**: [SAP Java Connector](https://support.sap.com/en/product/connectors/jco.html)

---

**Última Actualización**: 2025-11-07
**Estado**: ✅ Investigación Completada → **APROBADA TÉCNICAMENTE**
**Decisión**: ✅ **Proceder con migración Java (Phase 5 post-Phase 1-4)**
**Próxima Revisión**: Post-Phase 4 implementation

# Product Requirement: MCP Server para ABAP ADT vía RFC

## Historia de Usuario

**Como** desarrollador Python
**Quiero** crear un servidor MCP que se conecte vía SAP RFC SDK hacia un sistema SAP ABAP on-premise
**Para** leer, modificar y analizar el repositorio de objetos ABAP de forma nativa y eficiente

## Contexto

Actualmente el proyecto `abap-adt-py/` utiliza conexiones HTTP para interactuar con SAP. Necesitamos migrar a una conexión basada en SAP RFC SDK (como la implementada en `app/main.py`) para obtener mejor rendimiento y acceso nativo a las funcionalidades ABAP.

## Objetivos

1. **Migración de conectividad**: Reemplazar las peticiones HTTP del proyecto `abap-adt-py/` por conexiones RFC SDK
2. **Adaptación de métodos**: Modificar los métodos existentes para utilizar la RFC `SADT_REST_RFC_ENDPOINT` con el parámetro `REQUEST`
3. **Implementación modular**: Crear estructura en `app/` con responsabilidades claras y separación de concerns
4. **Exposición MCP**: Publicar cada funcionalidad como herramienta MCP independiente
5. **Optimización para LLM**: Diseñar la interfaz pensando en consumo por Claude Code y otros LLMs

## Criterios de Aceptación

### 1. Análisis y Validación
- [ ] Revisar y documentar la arquitectura actual de `abap-adt-py/`
- [ ] Identificar todos los métodos que requieren migración
- [ ] Validar compatibilidad de funcionalidades con RFC SDK
- [ ] Crear propuesta de arquitectura consensuada

### 2. Implementación Core
- [ ] Adaptar métodos para usar `SADT_REST_RFC_ENDPOINT` en lugar de HTTP
- [ ] Implementar capa de conexión RFC reutilizable
- [ ] Migrar funcionalidades principales manteniendo compatibilidad de interfaz

### 3. Estructura Modular
- [ ] Organizar código en módulos por funcionalidad (tablas, clases, programas, etc.)
- [ ] Implementar patrón de responsabilidad única
- [ ] Crear sistema de configuración externalizado
- [ ] Documentar cada módulo con ejemplos de uso

### 4. Exposición MCP
Crear herramientas MCP para cada funcionalidad:
- [ ] `get_table`: Lectura de datos de tablas ABAP
- [ ] `get_structure`: Obtener definición de estructuras
- [ ] `get_data_element`: Consultar elementos de datos
- [ ] `get_class`: Leer definición de clases ABAP
- [ ] `get_program`: Obtener código fuente de programas
- [ ] `search_objects`: Buscar objetos ABAP por criterios
- [ ] Adicionales según análisis de `abap-adt-py/`

### 5. Optimización para LLM
- [ ] Descripciones claras y concisas en cada herramienta MCP
- [ ] Parámetros bien documentados con ejemplos
- [ ] Respuestas estructuradas en JSON
- [ ] Manejo de errores informativo
- [ ] Logging comprehensivo para debugging

## Restricciones Técnicas

- Debe mantener compatibilidad con SAP RFC SDK
- Utilizar `python3` y gestor de paquetes `uv`
- Implementar async/await para operaciones I/O
- Type hints obligatorios
- Cobertura de pruebas mínima del 70%

## Definición de Hecho

- Todas las funcionalidades de `abap-adt-py/` migradas y funcionando vía RFC
- Herramientas MCP documentadas y probadas
- Código modularizado con responsabilidades claras
- Documentación de uso para desarrolladores y LLMs
- Tests unitarios e integración pasando
- README con ejemplos de uso actualizados

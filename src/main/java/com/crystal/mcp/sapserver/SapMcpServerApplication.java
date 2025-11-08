package com.crystal.mcp.sapserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for SAP ABAP MCP Server (Java POC).
 *
 * This Spring Boot application implements an MCP (Model Context Protocol) server
 * that enables LLM tools like Claude to interact with SAP ABAP systems via
 * SAP JCo (Java Connector).
 *
 * Architecture:
 * Claude Desktop → STDIO → Spring AI MCP → RfcAdapter → SAP JCo → SADT_REST_RFC_ENDPOINT → SAP System
 *
 * @see <a href="https://modelcontextprotocol.io">Model Context Protocol</a>
 * @see <a href="https://spring.io/projects/spring-ai">Spring AI</a>
 */
@SpringBootApplication
public class SapMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SapMcpServerApplication.class, args);
    }
}

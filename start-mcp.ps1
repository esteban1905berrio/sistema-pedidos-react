#!/usr/bin/env pwsh
# ============================================================================
# SAP MCP Server - PowerShell Startup Script
# ============================================================================
# This script starts the SAP MCP Server on Windows using Maven
#
# Prerequisites:
#   1. Java 21+ installed
#   2. Maven 3.9+ installed
#   3. SAP JCo libraries installed in lib/ directory
#   4. Environment variables configured (or set in .mcp.json)
#
# Usage:
#   .\start-mcp.ps1
# ============================================================================

Write-Host "Starting SAP MCP Server (PowerShell)..." -ForegroundColor Cyan
Write-Host ""

# Detect Java Home
if (-not $env:JAVA_HOME) {
    Write-Host "WARNING: JAVA_HOME is not set" -ForegroundColor Yellow
    Write-Host "Attempting to use java from PATH..."

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Host "ERROR: Java not found. Please install Java 21+ and set JAVA_HOME" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
}

# Verify Java version
Write-Host "Checking Java version..."
$javaVersion = java -version 2>&1 | Select-String "version"
if ($javaVersion -notmatch "21") {
    Write-Host "WARNING: Java 21 recommended. Current version:" -ForegroundColor Yellow
    java -version
    Write-Host ""
}

# Verify Maven installation
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Maven not found. Please install Maven 3.9+ and add to PATH" -ForegroundColor Red
    exit 1
}

# Verify SAP JCo libraries
$jcoDll = "lib\sapjco3.dll"
$jcoJar = "lib\sapjco3.jar"

if (-not (Test-Path $jcoDll)) {
    Write-Host "ERROR: SAP JCo library not found: $jcoDll" -ForegroundColor Red
    Write-Host "Please download SAP JCo for Windows and extract to lib/ directory" -ForegroundColor Yellow
    Write-Host "See lib\README.md for installation instructions" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $jcoJar)) {
    Write-Host "ERROR: SAP JCo JAR not found: $jcoJar" -ForegroundColor Red
    Write-Host "Please download SAP JCo for Windows and extract to lib/ directory" -ForegroundColor Yellow
    Write-Host "See lib\README.md for installation instructions" -ForegroundColor Yellow
    exit 1
}

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "SAP JCo libraries found:" -ForegroundColor Green
Write-Host "  - $jcoJar" -ForegroundColor Green
Write-Host "  - $jcoDll" -ForegroundColor Green
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""

# Set library path for JCo
$libPath = Join-Path $PSScriptRoot "lib"
$env:JAVA_OPTS = "-Djava.library.path=$libPath"

Write-Host "Starting Maven Spring Boot application..." -ForegroundColor Cyan
Write-Host ""

# Start the application
mvn spring-boot:run

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Application failed to start" -ForegroundColor Red
    Write-Host "Check logs in logs\java\sap-mcp-server.log for details" -ForegroundColor Yellow
    exit 1
}

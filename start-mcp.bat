@echo off
REM ============================================================================
REM SAP MCP Server - Windows Startup Script
REM ============================================================================
REM This script starts the SAP MCP Server on Windows using Maven
REM
REM Prerequisites:
REM   1. Java 21+ installed
REM   2. Maven 3.9+ installed
REM   3. SAP JCo libraries installed in lib/ directory
REM   4. Environment variables configured (or set in .mcp.json)
REM
REM Usage:
REM   start-mcp.bat
REM ============================================================================

echo Starting SAP MCP Server (Windows)...
echo.

REM Detect Java Home if not set
if "%JAVA_HOME%"=="" (
    echo WARNING: JAVA_HOME is not set
    echo Attempting to use java from PATH...
    where java >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Java not found. Please install Java 21+ and set JAVA_HOME
        exit /b 1
    )
) else (
    echo Using JAVA_HOME: %JAVA_HOME%
)

REM Verify Java version
echo Checking Java version...
java -version 2>&1 | findstr /C:"21" >nul
if errorlevel 1 (
    echo WARNING: Java 21 recommended. Current version:
    java -version
    echo.
)

REM Verify Maven installation
where mvn >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven not found. Please install Maven 3.9+ and add to PATH
    exit /b 1
)

REM Verify SAP JCo library exists
if not exist "lib\sapjco3.dll" (
    echo ERROR: SAP JCo library not found: lib\sapjco3.dll
    echo Please download SAP JCo for Windows and extract to lib/ directory
    echo See lib\README.md for installation instructions
    exit /b 1
)

if not exist "lib\sapjco3.jar" (
    echo ERROR: SAP JCo JAR not found: lib\sapjco3.jar
    echo Please download SAP JCo for Windows and extract to lib/ directory
    echo See lib\README.md for installation instructions
    exit /b 1
)

echo ============================================================================
echo SAP JCo libraries found:
echo   - lib\sapjco3.jar
echo   - lib\sapjco3.dll
echo ============================================================================
echo.

REM Set library path for JCo
set JAVA_OPTS=-Djava.library.path=%CD%\lib

echo Starting Maven Spring Boot application...
echo.

REM Start the application
mvn spring-boot:run

if errorlevel 1 (
    echo.
    echo ERROR: Application failed to start
    echo Check logs in logs\java\sap-mcp-server.log for details
    exit /b 1
)

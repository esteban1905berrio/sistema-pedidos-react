#!/bin/bash

# Script de prueba de conexión SAP MCP Server
# Ejecuta el servidor brevemente para verificar la conexión

echo "🚀 Iniciando servidor MCP..."
echo ""

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export SAP_ASHOST=172.27.154.8
export SAP_SYSNR=00
export SAP_CLIENT=100
export SAP_USER=seblondo
export SAP_PASSWD='Broot1109*'
export SAP_LANG=ES
export SAP_ROUTER='/H/190.145.188.150/S/sapdp99'
export SAP_POOL_CAPACITY=5
export SAP_PEAK_LIMIT=10

# Ejecutar servidor
mvn spring-boot:run -Djava.library.path=./lib &
SERVER_PID=$!

echo "Servidor iniciado con PID: $SERVER_PID"
echo "Esperando 8 segundos..."
sleep 8

# Detener servidor
if ps -p $SERVER_PID > /dev/null 2>&1; then
    echo "✅ Servidor aún ejecutándose - conexión exitosa"
    kill $SERVER_PID 2>/dev/null
    wait $SERVER_PID 2>/dev/null
else
    echo "⚠️  Servidor ya terminó - verificar logs"
fi

echo ""
echo "=== Log de aplicación ==="
if [ -f logs/sap-mcp-server.log ]; then
    tail -30 logs/sap-mcp-server.log
else
    echo "No hay logs disponibles (logging desactivado para STDIO)"
fi

#!/bin/bash

# Configuration for MCP Server Manual Test
# Replicates VS Code launch configuration environment

print_header() {
    echo "================================================================"
    echo "   MCP Server - Manual Extraction Test Launcher"
    echo "================================================================"
}

print_header

# 1. Export Environment Variables (from .mcp.json)
echo "[INFO] Setting up environment variables..."

export SAP_ASHOST="172.27.154.8"
export SAP_SYSNR="00"
export SAP_CLIENT="100"
export SAP_USER="seblondo"
export SAP_PASSWD="Broot1109*"
export SAP_LANG="ES"
export SAP_ROUTER="/H/190.145.188.150/S/sapdp99"
export SAP_POOL_CAPACITY="5"
export SAP_PEAK_LIMIT="10"

# Print sanitized config for verification
echo "  SAP_ASHOST: $SAP_ASHOST"
echo "  SAP_USER:   $SAP_USER"
echo "  SAP_CLIENT: $SAP_CLIENT"
echo "----------------------------------------------------------------"

# 2. Run Manual Test via Maven
echo "[INFO] Starting Test: com.crystal.mcp.sapserver.manual.ManualExtractionTest"
echo ""

mvn spring-boot:run \
    -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualExtractionTest \
    -Dspring.output.ansi.enabled=ALWAYS

echo ""
echo "================================================================"
echo "   Test Execution Completed"
echo "================================================================"

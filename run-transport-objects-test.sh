#!/bin/bash

# Script para ejecutar ManualTransportObjectsTest de forma manual
# Ubicación: /Users/bastianroot/CursorIDEWorkspace/giralmcp/run-transport-objects-test.sh
# Uso: ./run-transport-objects-test.sh [test_name]

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Transport Objects Manual Test Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Error: pom.xml no encontrado${NC}"
    echo "   Debes ejecutar este script desde el directorio raíz del proyecto"
    exit 1
fi

# Test a ejecutar (default: todos)
TEST_NAME="${1:-ManualTransportObjectsTest}"

echo -e "${YELLOW}📋 Tests disponibles:${NC}"
echo "   1. testGetTransportObjects_MainTransport       - OT principal con tareas"
echo "   2. testGetTransportObjects_Task                - Tarea específica"
echo "   3. testGetTransportObjects_FilterByTask        - Filtrar por tarea"
echo "   4. testGetTransportObjects_NotFound            - Transporte no encontrado"
echo "   5. testGetTransportObjects_TransportOfCopies   - Transport of Copies"
echo "   6. testGetTransportObjects_Comprehensive       - Test completo detallado"
echo "   7. (todos)                                     - Ejecutar todos los tests"
echo ""

# Si se pasó un número, convertirlo al nombre del test
case "$TEST_NAME" in
    1)
        TEST_NAME="ManualTransportObjectsTest#testGetTransportObjects_MainTransport"
        ;;
    2)
        TEST_NAME="ManualTransportObjectsTest#testGetTransportObjects_Task"
        ;;
    3)
        TEST_NAME="ManualTransportObjectsTest#testGetTransportObjects_FilterByTask"
        ;;
    4)
        TEST_NAME="ManualTransportObjectsTest#testGetTransportObjects_NotFound"
        ;;
    5)
        TEST_NAME="ManualTransportObjectsTest#testGetTransportObjects_TransportOfCopies"
        ;;
    6)
        TEST_NAME="ManualTransportObjectsTest#testGetTransportObjects_Comprehensive"
        ;;
    7|todos|all)
        TEST_NAME="ManualTransportObjectsTest"
        ;;
esac

echo -e "${GREEN}🚀 Ejecutando: ${TEST_NAME}${NC}"
echo ""

# Ejecutar Maven test
mvn test -Dtest="$TEST_NAME"

# Capturar el código de salida
EXIT_CODE=$?

echo ""
echo -e "${BLUE}========================================${NC}"

if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Tests completados exitosamente${NC}"
else
    echo -e "${RED}❌ Tests fallaron (código de salida: $EXIT_CODE)${NC}"
fi

echo -e "${BLUE}========================================${NC}"
echo ""

# Mostrar ubicación de logs
echo -e "${YELLOW}📁 Logs disponibles en:${NC}"
echo "   - logs/sap-mcp-server-test.log"
echo "   - target/surefire-reports/"
echo ""

# Mostrar comando para ver logs
echo -e "${YELLOW}💡 Ver logs:${NC}"
echo "   tail -100 logs/sap-mcp-server-test.log"
echo ""

exit $EXIT_CODE

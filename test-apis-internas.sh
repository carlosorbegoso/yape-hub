#!/bin/bash

# =============================================================================
# SCRIPT DE PRUEBAS - APIs INTERNAS (Para Administración)
# =============================================================================
# Este script contiene ejemplos de uso de las APIs internas de Yape Hub
# Para administradores del sistema que gestionan pagos y facturación

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuración
BASE_URL="http://localhost:8080"
ADMIN_ID="1451"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj0xNDUxLCJpc3M9aHR0cDovL2xvY2FsaG9zdDo4MDgwLCBncm91cHM9QURNSU4sIGV4cD0xNzU4MzE2OTUwLCJpYXQ9MTc1ODMxMzM1MH0.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc"

echo -e "${CYAN}🔧 YAPE HUB - PRUEBAS DE APIs INTERNAS${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""

# Función para mostrar ayuda
show_help() {
    echo -e "${YELLOW}📋 COMANDOS DISPONIBLES:${NC}"
    echo ""
    echo -e "${GREEN}1. Dashboard de Administración${NC}"
    echo "   ./test-apis-internas.sh dashboard"
    echo ""
    echo -e "${GREEN}2. Pagos Pendientes${NC}"
    echo "   ./test-apis-internas.sh pending"
    echo ""
    echo -e "${GREEN}3. Pagos Aprobados${NC}"
    echo "   ./test-apis-internas.sh approved"
    echo ""
    echo -e "${GREEN}4. Pagos Rechazados${NC}"
    echo "   ./test-apis-internas.sh rejected"
    echo ""
    echo -e "${GREEN}5. Códigos de Pago${NC}"
    echo "   ./test-apis-internas.sh codes"
    echo ""
    echo -e "${GREEN}6. Aprobar Pago${NC}"
    echo "   ./test-apis-internas.sh approve PAYMENT_ID"
    echo ""
    echo -e "${GREEN}7. Rechazar Pago${NC}"
    echo "   ./test-apis-internas.sh reject PAYMENT_ID"
    echo ""
    echo -e "${GREEN}8. Obtener Imagen de Pago${NC}"
    echo "   ./test-apis-internas.sh image PAYMENT_ID"
    echo ""
    echo -e "${GREEN}9. Estadísticas de Administración${NC}"
    echo "   ./test-apis-internas.sh stats"
    echo ""
    echo -e "${GREEN}10. Todas las Pruebas${NC}"
    echo "   ./test-apis-internas.sh all"
    echo ""
    echo -e "${YELLOW}💡 Ejemplo: ./test-apis-internas.sh dashboard${NC}"
}

# Función para probar dashboard de administración
test_dashboard() {
    echo -e "${BLUE}📊 Probando Dashboard de Administración...${NC}"
    curl -s "${BASE_URL}/api/admin/billing?type=dashboard&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar pagos pendientes
test_pending() {
    echo -e "${BLUE}⏳ Probando Pagos Pendientes...${NC}"
    curl -s "${BASE_URL}/api/admin/billing?type=payments&adminId=${ADMIN_ID}&status=pending" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar pagos aprobados
test_approved() {
    echo -e "${BLUE}✅ Probando Pagos Aprobados...${NC}"
    curl -s "${BASE_URL}/api/admin/billing?type=payments&adminId=${ADMIN_ID}&status=approved" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar pagos rechazados
test_rejected() {
    echo -e "${BLUE}❌ Probando Pagos Rechazados...${NC}"
    curl -s "${BASE_URL}/api/admin/billing?type=payments&adminId=${ADMIN_ID}&status=rejected" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar códigos de pago
test_codes() {
    echo -e "${BLUE}🔑 Probando Códigos de Pago...${NC}"
    curl -s "${BASE_URL}/api/admin/billing?type=codes&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para aprobar pago
test_approve() {
    local payment_id=${1:-"1"}
    echo -e "${BLUE}✅ Probando Aprobación de Pago ID: ${payment_id}...${NC}"
    curl -X PUT "${BASE_URL}/api/admin/billing/payments/${payment_id}/approve?adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" | jq .
    echo ""
}

# Función para rechazar pago
test_reject() {
    local payment_id=${1:-"1"}
    echo -e "${BLUE}❌ Probando Rechazo de Pago ID: ${payment_id}...${NC}"
    curl -X PUT "${BASE_URL}/api/admin/billing/payments/${payment_id}/reject?adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "reviewNotes": "Pago rechazado por pruebas"
        }' | jq .
    echo ""
}

# Función para obtener imagen de pago
test_image() {
    local payment_id=${1:-"1"}
    echo -e "${BLUE}🖼️ Probando Obtención de Imagen de Pago ID: ${payment_id}...${NC}"
    curl -s "${BASE_URL}/api/admin/billing/payments/${payment_id}/image?adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Accept: image/png" \
        -o "payment_image_${payment_id}.png"
    
    if [ -f "payment_image_${payment_id}.png" ]; then
        echo -e "${GREEN}✅ Imagen guardada como: payment_image_${payment_id}.png${NC}"
    else
        echo -e "${RED}❌ Error al obtener imagen${NC}"
    fi
    echo ""
}

# Función para probar estadísticas
test_stats() {
    echo -e "${BLUE}📈 Probando Estadísticas de Administración...${NC}"
    curl -s "${BASE_URL}/api/admin/billing?type=stats&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para todas las pruebas
test_all() {
    echo -e "${PURPLE}🚀 Ejecutando todas las pruebas de APIs internas...${NC}"
    echo ""
    
    test_dashboard
    test_pending
    test_approved
    test_rejected
    test_codes
    test_stats
    
    echo -e "${GREEN}✅ Todas las pruebas completadas${NC}"
}

# Función para verificar salud del servidor
check_health() {
    echo -e "${BLUE}🏥 Verificando salud del servidor...${NC}"
    curl -s "${BASE_URL}/q/health" | jq .
    echo ""
}

# Función para mostrar resumen de APIs internas
show_summary() {
    echo -e "${YELLOW}📋 RESUMEN DE APIs INTERNAS:${NC}"
    echo ""
    echo -e "${GREEN}🔧 Gestión de Pagos:${NC}"
    echo "   • Dashboard de administración"
    echo "   • Pagos pendientes, aprobados y rechazados"
    echo "   • Códigos de pago generados"
    echo ""
    echo -e "${GREEN}✅ Acciones de Administración:${NC}"
    echo "   • Aprobar pagos manuales"
    echo "   • Rechazar pagos con notas"
    echo "   • Obtener imágenes de comprobantes"
    echo ""
    echo -e "${GREEN}📊 Reportes y Estadísticas:${NC}"
    echo "   • Estadísticas de administración"
    echo "   • Métricas de pagos"
    echo "   • Análisis de códigos generados"
    echo ""
    echo -e "${YELLOW}🔐 Seguridad:${NC}"
    echo "   • Todas las APIs requieren token de administrador"
    echo "   • Validación de adminId en cada request"
    echo "   • Logs detallados de todas las operaciones"
}

# Procesar argumentos
case "$1" in
    "dashboard")
        test_dashboard
        ;;
    "pending")
        test_pending
        ;;
    "approved")
        test_approved
        ;;
    "rejected")
        test_rejected
        ;;
    "codes")
        test_codes
        ;;
    "approve")
        test_approve "$2"
        ;;
    "reject")
        test_reject "$2"
        ;;
    "image")
        test_image "$2"
        ;;
    "stats")
        test_stats
        ;;
    "all")
        test_all
        ;;
    "health")
        check_health
        ;;
    "summary")
        show_summary
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo -e "${RED}❌ Comando no reconocido: $1${NC}"
        echo ""
        show_help
        ;;
esac

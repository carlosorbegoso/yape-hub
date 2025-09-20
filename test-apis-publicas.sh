#!/bin/bash

# =============================================================================
# SCRIPT DE PRUEBAS - APIs PÚBLICAS (Para Usuarios)
# =============================================================================
# Este script contiene ejemplos de uso de las APIs públicas de Yape Hub
# Para usuarios finales (admins) que quieren gestionar su facturación

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

echo -e "${CYAN}🚀 YAPE HUB - PRUEBAS DE APIs PÚBLICAS${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""
echo -e "${YELLOW}📖 DESCRIPCIÓN GENERAL:${NC}"
echo "Este script permite probar las APIs públicas de Yape Hub diseñadas para el frontend."
echo "Incluye solo las funcionalidades que necesita la interfaz de usuario final."
echo ""
echo -e "${YELLOW}🎯 CASOS DE USO PRINCIPALES:${NC}"
echo "• Consultar tokens disponibles y consumo"
echo "• Verificar estado de suscripción"
echo "• Obtener métricas del dashboard"
echo "• Consultar paquetes de tokens disponibles"
echo "• Generar códigos de pago para renovación"
echo "• Subir comprobantes de pago"
echo "• Monitorear estado de pagos"
echo ""
echo -e "${YELLOW}🔐 AUTENTICACIÓN:${NC}"
echo "Todas las APIs requieren un token JWT válido en el header Authorization."
echo "El token debe contener el adminId y permisos de ADMIN."
echo ""

# Función para mostrar ayuda
show_help() {
    echo -e "${YELLOW}📋 COMANDOS DISPONIBLES:${NC}"
    echo ""
    echo -e "${GREEN}1. Tokens${NC}"
    echo "   ./test-apis-publicas.sh tokens"
    echo "   📝 Descripción: Obtiene información sobre los tokens disponibles del admin"
    echo "   🔗 Endpoint: GET /api/billing?type=tokens&adminId={id}"
    echo ""
    echo -e "${GREEN}2. Suscripción${NC}"
    echo "   ./test-apis-publicas.sh subscription"
    echo "   📝 Descripción: Consulta el estado actual de la suscripción del admin"
    echo "   🔗 Endpoint: GET /api/billing?type=subscription&adminId={id}"
    echo ""
    echo -e "${GREEN}3. Dashboard${NC}"
    echo "   ./test-apis-publicas.sh dashboard"
    echo "   📝 Descripción: Obtiene estadísticas y métricas para el dashboard del admin"
    echo "   🔗 Endpoint: GET /api/billing?type=dashboard&adminId={id}"
    echo ""
    echo -e "${GREEN}4. Paquetes de Tokens${NC}"
    echo "   ./test-apis-publicas.sh token-packages"
    echo "   📝 Descripción: Obtiene los paquetes de tokens disponibles para compra"
    echo "   🔗 Endpoint: GET /api/billing?type=token-packages&adminId={id}"
    echo ""
    echo -e "${GREEN}5. Generar Código de Pago${NC}"
    echo "   ./test-apis-publicas.sh generate-code"
    echo "   📝 Descripción: Genera un código único para realizar pagos de suscripción"
    echo "   🔗 Endpoint: POST /api/billing/operations?adminId={id}&action=generate-code"
    echo ""
    echo -e "${GREEN}6. Subir Imagen de Pago${NC}"
    echo "   ./test-apis-publicas.sh upload"
    echo "   📝 Descripción: Sube una imagen del comprobante de pago en formato base64"
    echo "   🔗 Endpoint: POST /api/billing/payments/upload?adminId={id}&paymentCode={code}"
    echo ""
    echo -e "${GREEN}7. Estado de Pago${NC}"
    echo "   ./test-apis-publicas.sh status PAY_42FF0B5C"
    echo "   📝 Descripción: Consulta el estado actual de un pago específico"
    echo "   🔗 Endpoint: GET /api/billing/payments/status/{paymentCode}"
    echo ""
    echo -e "${GREEN}8. Todas las Pruebas${NC}"
    echo "   ./test-apis-publicas.sh all"
    echo "   📝 Descripción: Ejecuta todas las pruebas de APIs públicas en secuencia"
    echo ""
    echo -e "${GREEN}9. Salud del Servidor${NC}"
    echo "   ./test-apis-publicas.sh health"
    echo "   📝 Descripción: Verifica que el servidor esté funcionando correctamente"
    echo "   🔗 Endpoint: GET /q/health"
    echo ""
    echo -e "${YELLOW}💡 Ejemplo: ./test-apis-publicas.sh tokens${NC}"
}

# Función para probar tokens
test_tokens() {
    echo -e "${BLUE}🪙 Probando API de Tokens...${NC}"
    echo -e "${CYAN}📋 Esta API devuelve:${NC}"
    echo "   • Tokens disponibles del admin"
    echo "   • Historial de consumo de tokens"
    echo "   • Límites de tokens según el plan"
    echo "   • Fecha de renovación de tokens"
    echo ""
    curl -s "${BASE_URL}/api/billing?type=tokens&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar suscripción
test_subscription() {
    echo -e "${BLUE}📋 Probando API de Suscripción...${NC}"
    echo -e "${CYAN}📋 Esta API devuelve:${NC}"
    echo "   • Plan de suscripción actual"
    echo "   • Estado de la suscripción (activa/vencida)"
    echo "   • Fecha de inicio y vencimiento"
    echo "   • Características del plan contratado"
    echo "   • Precio y período de facturación"
    echo ""
    curl -s "${BASE_URL}/api/billing?type=subscription&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar dashboard
test_dashboard() {
    echo -e "${BLUE}📊 Probando API de Dashboard...${NC}"
    echo -e "${CYAN}📋 Esta API devuelve:${NC}"
    echo "   • Estadísticas de uso de tokens"
    echo "   • Métricas de operaciones realizadas"
    echo "   • Gráficos de consumo por período"
    echo "   • Comparación con períodos anteriores"
    echo "   • Alertas y notificaciones importantes"
    echo ""
    curl -s "${BASE_URL}/api/billing?type=dashboard&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para probar paquetes de tokens
test_token_packages() {
    echo -e "${BLUE}🪙 Probando API de Paquetes de Tokens...${NC}"
    echo -e "${CYAN}📋 Esta API devuelve:${NC}"
    echo "   • Paquetes de tokens disponibles para compra"
    echo "   • Precios y descuentos de cada paquete"
    echo "   • Características incluidas en cada paquete"
    echo "   • Recomendaciones de paquetes populares"
    echo ""
    curl -s "${BASE_URL}/api/billing?type=token-packages&adminId=${ADMIN_ID}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para generar código de pago
test_generate_code() {
    echo -e "${BLUE}💳 Probando Generación de Código de Pago...${NC}"
    echo -e "${CYAN}📋 Esta API genera:${NC}"
    echo "   • Código único de pago (ej: PAY_42FF0B5C)"
    echo "   • Información de pago con monto y método"
    echo "   • Instrucciones para realizar el pago"
    echo "   • Datos del destinatario (Yape Hub)"
    echo ""
    curl -X POST "${BASE_URL}/api/billing/operations?adminId=${ADMIN_ID}&action=generate-code" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "planId": 1,
            "paymentMethod": "yape"
        }' | jq .
    echo ""
}

# Función para subir imagen de pago
test_upload() {
    echo -e "${BLUE}📸 Probando Subida de Imagen de Pago...${NC}"
    echo -e "${CYAN}📋 Esta API permite:${NC}"
    echo "   • Subir comprobante de pago en formato base64"
    echo "   • Asociar imagen con código de pago"
    echo "   • Iniciar proceso de verificación manual"
    echo "   • Notificar al equipo de soporte"
    echo ""
    echo -e "${YELLOW}📝 Ejemplo de imagen base64 (formato requerido):${NC}"
    echo "   data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD..."
    echo ""
    curl -X POST "${BASE_URL}/api/billing/payments/upload?adminId=${ADMIN_ID}&paymentCode=PAY_42FF0B5C" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A"
        }' | jq .
    echo ""
} 

# Función para verificar estado de pago
test_status() {
    local payment_code=${1:-"PAY_42FF0B5C"}
    echo -e "${BLUE}📋 Probando Estado de Pago: ${payment_code}...${NC}"
    echo -e "${CYAN}📋 Esta API consulta:${NC}"
    echo "   • Estado actual del pago (pendiente/aprobado/rechazado)"
    echo "   • Fecha y hora de procesamiento"
    echo "   • Detalles de la transacción"
    echo "   • Comentarios del equipo de verificación"
    echo ""
    curl -s "${BASE_URL}/api/billing/payments/status/${payment_code}" \
        -H "Authorization: Bearer ${TOKEN}" | jq .
    echo ""
}

# Función para todas las pruebas
test_all() {
    echo -e "${PURPLE}🚀 Ejecutando todas las pruebas de APIs públicas...${NC}"
    echo ""
    
    test_tokens
    test_subscription
    test_dashboard
    test_token_packages
    test_generate_code
    test_upload
    test_status
    
    echo -e "${GREEN}✅ Todas las pruebas completadas${NC}"
}

# Función para verificar salud del servidor
check_health() {
    echo -e "${BLUE}🏥 Verificando salud del servidor...${NC}"
    echo -e "${CYAN}📋 Esta API verifica:${NC}"
    echo "   • Estado del servidor (UP/DOWN)"
    echo "   • Conectividad con la base de datos"
    echo "   • Recursos disponibles (memoria, CPU)"
    echo "   • Estado de los servicios internos"
    echo ""
    curl -s "${BASE_URL}/q/health" | jq .
    echo ""
}

# Procesar argumentos
case "$1" in
    "tokens")
        test_tokens
        ;;
    "subscription")
        test_subscription
        ;;
    "dashboard")
        test_dashboard
        ;;
    "token-packages")
        test_token_packages
        ;;
    "generate-code")
        test_generate_code
        ;;
    "upload")
        test_upload
        ;;
    "status")
        test_status "$2"
        ;;
    "all")
        test_all
        ;;
    "health")
        check_health
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

# Información de configuración al final
echo ""
echo -e "${PURPLE}⚙️  CONFIGURACIÓN:${NC}"
echo "• BASE_URL: ${BASE_URL}"
echo "• ADMIN_ID: ${ADMIN_ID}"
echo "• TOKEN: ${TOKEN:0:50}..."
echo ""
echo -e "${YELLOW}💡 Para personalizar la configuración, edita las variables al inicio del script:${NC}"
echo "   BASE_URL=\"http://tu-servidor:puerto\""
echo "   ADMIN_ID=\"tu-admin-id\""
echo "   TOKEN=\"tu-jwt-token\""
echo ""
echo -e "${GREEN}✅ Script ejecutado correctamente${NC}"

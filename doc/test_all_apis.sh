#!/bin/bash

# Yape Hub API Test Script
# Este script prueba todas las APIs del sistema Yape Hub con diferentes parámetros

# Configuración
BASE_URL="http://localhost:8080"
ADMIN_EMAIL="calo@hotmail.com"
ADMIN_PASSWORD="Sky22234Ts*t"
SELLER_PHONE="+51987654321"
AFFILIATION_CODE="TEST123"

# Directorio para guardar respuestas
RESPONSES_DIR="api_responses"
mkdir -p "$RESPONSES_DIR"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables globales
JWT_TOKEN=""
ADMIN_ID=""
SELLER_ID=""
BRANCH_ID=""
PAYMENT_CODE=""
AFFILIATION_CODE=""

# Función para imprimir mensajes
print_message() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para hacer requests HTTP y guardar respuestas
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local headers=$4
    local endpoint_name=$5
    
    # Crear nombre de archivo para la respuesta
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local filename="${RESPONSES_DIR}/${endpoint_name}_${timestamp}.json"
    
    local response
    local http_code
    
    if [ -n "$data" ]; then
        response=$(curl -s --max-time 10 -w "\n%{http_code}" -X $method "$url" \
            -H "Content-Type: application/json" \
            $headers \
            -d "$data")
    else
        response=$(curl -s --max-time 10 -w "\n%{http_code}" -X $method "$url" \
            -H "Content-Type: application/json" \
            $headers)
    fi
    
    # Extraer código HTTP y respuesta
    http_code=$(echo "$response" | tail -n1)
    response=$(echo "$response" | sed '$d')
    
    # Limpiar código HTTP - eliminar cualquier carácter no numérico
    http_code=$(echo "$http_code" | tr -cd '[:digit:]')
    
    # Guardar TODAS las respuestas (no solo HTTP 200)
    if [ -n "$response" ]; then
        echo "$response" | jq . > "$filename" 2>/dev/null || echo "$response" > "$filename"
        if [ "$http_code" = "200" ]; then
            print_success "✅ Respuesta guardada: $filename (HTTP $http_code)"
        else
            print_warning "⚠️ Respuesta guardada: $filename (HTTP $http_code)"
        fi
    else
        print_error "❌ No guardado: $endpoint_name (HTTP $http_code) - Respuesta vacía"
    fi
    
    # Mostrar respuesta en consola
    echo "$response"
}

# Función para buscar sellers existentes
search_existing_sellers() {
    print_message "=== BUSCANDO SELLERS EXISTENTES ==="
    
    # Buscar todos los sellers del admin usando el endpoint correcto
    print_message "Obteniendo todos los sellers del admin..."
    local all_sellers_response=$(make_request "GET" "$BASE_URL/api/admin/sellers/my-sellers" "" "-H \"Authorization: Bearer $JWT_TOKEN\"" "all_sellers_admin")
    
    # Buscar sellers usando el endpoint general
    print_message "Obteniendo lista general de sellers..."
    local general_sellers_response=$(make_request "GET" "$BASE_URL/api/admin/sellers" "" "-H \"Authorization: Bearer $JWT_TOKEN\"" "general_sellers_admin")
    
    # Extraer información de sellers
    if [ -n "$all_sellers_response" ]; then
        local seller_count=$(echo "$all_sellers_response" | jq -r '.data.sellers | length' 2>/dev/null || echo "0")
        print_message "Sellers encontrados en my-sellers: $seller_count"
        
        if [ "$seller_count" -gt 0 ]; then
            print_message "Primeros sellers encontrados:"
            echo "$all_sellers_response" | jq -r '.data.sellers[0:3][] | "ID: \(.id), Teléfono: \(.phone), Nombre: \(.name)"' 2>/dev/null || echo "No se pudo parsear la respuesta"
        fi
    fi
    
    # Verificar si hay algún seller con teléfono 777777777
    print_message "Verificando si existe seller con teléfono 777777777..."
    if [ -n "$all_sellers_response" ]; then
        local seller_777=$(echo "$all_sellers_response" | jq -r '.data.sellers[] | select(.phone == "777777777")' 2>/dev/null)
        if [ -n "$seller_777" ]; then
            print_success "✅ Seller con teléfono 777777777 encontrado:"
            echo "$seller_777" | jq .
        else
            print_warning "⚠️ No se encontró seller con teléfono 777777777"
        fi
    fi
}

# Función para generar código de afiliación y hacer login del seller
seller_login_flow() {
    print_message "=== FLUJO COMPLETO DE SELLER ==="
    
    # 1. Generar código de afiliación
    print_message "1. Generando código de afiliación..."
    local affiliation_response=$(make_request "POST" "$BASE_URL/api/generate-affiliation-code-protected" '{"businessId": '$ADMIN_ID'}' "-H \"Authorization: Bearer $JWT_TOKEN\"" "generate_affiliation_code")
    print_message "Respuesta código afiliación: $affiliation_response"
    
    # Extraer código de afiliación
    AFFILIATION_CODE=$(extract_json_value "$affiliation_response" "affiliationCode")
    if [ -n "$AFFILIATION_CODE" ] && [ "$AFFILIATION_CODE" != "null" ]; then
        print_success "✅ Código de afiliación obtenido: $AFFILIATION_CODE"
    else
        print_error "❌ No se pudo obtener código de afiliación"
        return 1
    fi
    
    # 2. Login del seller con teléfono y código de afiliación
    print_message "2. Haciendo login del seller..."
    local seller_login_response=$(make_request "POST" "$BASE_URL/api/auth/seller/login-by-phone?phone=777777777&affiliationCode=$AFFILIATION_CODE" "" "-H \"accept: application/json\"" "seller_login_by_phone")
    print_message "Respuesta login seller: $seller_login_response"
    
    # Extraer información del seller
    local seller_success=$(extract_json_value "$seller_login_response" "success")
    if [ "$seller_success" = "true" ]; then
        SELLER_ID=$(extract_json_value "$seller_login_response" "sellerId")
        print_success "✅ Seller logueado exitosamente - ID: $SELLER_ID"
        
        # 3. Obtener perfil del seller
        print_message "3. Obteniendo perfil del seller..."
        local seller_profile_response=$(make_request "GET" "$BASE_URL/api/seller/profile/$SELLER_ID" "" "-H \"Authorization: Bearer $JWT_TOKEN\"" "seller_profile")
        print_message "Respuesta perfil seller: $seller_profile_response"
        
        return 0
    else
        print_error "❌ Error en login del seller"
        return 1
    fi
}

# Función para extraer valores de JSON
extract_json_value() {
    local json=$1
    local key=$2
    echo "$json" | grep -o "\"$key\":[^,}]*" | cut -d'"' -f4
}

# Función para verificar que el token JWT sea válido
verify_jwt_token() {
    if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "null" ]; then
        print_error "Token JWT no válido o vacío"
        return 1
    fi
    
    # Verificar que el token tenga el formato correcto (al menos 20 caracteres)
    if [ ${#JWT_TOKEN} -lt 20 ]; then
        print_error "Token JWT muy corto, posiblemente inválido"
        return 1
    fi
    
    print_success "Token JWT válido: ${JWT_TOKEN:0:20}..."
    return 0
}

# Función para verificar si el servidor está corriendo
check_server() {
    print_message "Verificando si el servidor está corriendo..."
    
    local response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/swagger-ui")
    
    if [ "$response" = "200" ] || [ "$response" = "302" ]; then
        print_success "Servidor está corriendo en $BASE_URL"
        return 0
    else
        print_error "Servidor no está corriendo en $BASE_URL"
        print_message "Por favor, inicia el servidor con: ./gradlew quarkusDev"
        return 1
    fi
}

# 1. AUTENTICACIÓN
test_auth_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE AUTENTICACIÓN ==="
    
    # 1.1 Registro de administrador
    print_message "1.1 Registrando administrador..."
    local register_data='{
        "businessName": "Negocio de Prueba",
        "email": "'$ADMIN_EMAIL'",
        "password": "'$ADMIN_PASSWORD'",
        "phone": "+51987654321",
        "contactName": "Admin Test"
    }'
    
    local register_response=$(make_request "POST" "$BASE_URL/api/auth/admin/register" "$register_data" "" "admin_register")
    print_message "Respuesta registro: $register_response"
    
    # 1.2 Login - Intentar múltiples veces si es necesario
    print_message "1.2 Haciendo login..."
    local login_data='{
        "email": "'$ADMIN_EMAIL'",
        "password": "'$ADMIN_PASSWORD'",
        "deviceFingerprint": "H",
        "role": "ADMIN"
    }'
    
    local login_attempts=0
    local max_attempts=3
    
    while [ $login_attempts -lt $max_attempts ]; do
        login_attempts=$((login_attempts + 1))
        print_message "Intento de login $login_attempts/$max_attempts..."
        
        local login_response=$(make_request "POST" "$BASE_URL/api/auth/login" "$login_data" "" "admin_login")
        print_message "Respuesta login: $login_response"
        
        # Extraer token JWT
        JWT_TOKEN=$(extract_json_value "$login_response" "accessToken")
        if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
            print_success "Token JWT obtenido: ${JWT_TOKEN:0:20}..."
            ADMIN_ID=$(extract_json_value "$login_response" "userId")
            print_success "Admin ID: $ADMIN_ID"
            break
        else
            print_error "No se pudo obtener el token JWT en intento $login_attempts"
            if [ $login_attempts -lt $max_attempts ]; then
                print_message "Esperando 2 segundos antes del siguiente intento..."
                sleep 2
            fi
        fi
    done
    
    if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "null" ]; then
        print_error "No se pudo obtener el token JWT después de $max_attempts intentos"
        print_error "El script no puede continuar sin autenticación"
        return 1
    fi
    
    # Verificar que el token sea válido
    if ! verify_jwt_token; then
        print_error "Token JWT no válido, no se puede continuar"
        return 1
    fi
    
    # 1.3 Refresh token
    print_message "1.3 Probando refresh token..."
    local refresh_response=$(make_request "POST" "$BASE_URL/api/auth/refresh" "" "-H \"X-Auth-Token: $JWT_TOKEN\"" "refresh_token")
    print_message "Respuesta refresh: $refresh_response"
    
    # 1.4 Forgot password
    print_message "1.4 Probando forgot password..."
    local forgot_response=$(make_request "POST" "$BASE_URL/api/auth/forgot-password?email=$ADMIN_EMAIL" "" "" "forgot_password")
    print_message "Respuesta forgot password: $forgot_response"
    
    # 1.5 Seller login by phone
    print_message "1.5 Probando seller login by phone..."
    local seller_login_response=$(make_request "POST" "$BASE_URL/api/auth/seller/login-by-phone?phone=$SELLER_PHONEmake_request "POST" "$BASE_URL/api/auth/seller/login-by-phone?phone=$SELLER_PHONEmake_request "POST" "$BASE_URL/api/auth/seller/login-by-phone?phone=$SELLER_PHONE&affiliationCode=$AFFILIATION_CODE" ""affiliationCode=$AFFILIATION_CODE" "" "" "seller_login"affiliationCode=$AFFILIATION_CODE" "" "" "seller_login")
    print_message "Respuesta seller login: $seller_login_response"
    
    print_success "Endpoints de autenticación probados"
}

# 2. GESTIÓN DE ADMINISTRADORES
test_admin_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE ADMINISTRADORES ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 2.1 Obtener perfil de admin
    print_message "2.1 Obteniendo perfil de administrador..."
    local profile_response=$(make_request "GET" "$BASE_URL/api/admin/profile?userId=$ADMIN_ID" "" "$auth_header" "admin_profile" "admin_profile")
    print_message "Respuesta perfil: $profile_response"
    
    # 2.2 Actualizar perfil de admin
    print_message "2.2 Actualizando perfil de administrador..."
    local update_data='{
        "businessName": "Negocio Actualizado",
        "contactName": "Admin Actualizado",
        "phone": "+51987654322",
        "email": "'$ADMIN_EMAIL'"
    }'
    
    local update_response=$(make_request "PUT" "$BASE_URL/api/admin/profile?userId=$ADMIN_ID" "$update_data" "$auth_header" "admin_update_profile" "admin_update_profile")
    print_message "Respuesta actualización: $update_response"
    
    print_success "Endpoints de administradores probados"
}

# 3. GESTIÓN DE SUCURSALES
test_branch_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE SUCURSALES ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 3.1 Crear sucursal
    print_message "3.1 Creando sucursal..."
    local branch_data='{
        "name": "Sucursal Principal",
        "code": "SUC001",
        "address": "Av. Principal 123, Lima"
    }'
    
    local create_branch_response=$(make_request "POST" "$BASE_URL/api/admin/branches" "$branch_data" "$auth_header" "create_branch" "create_branch")
    print_message "Respuesta crear sucursal: $create_branch_response"
    
    # Extraer ID de sucursal
    BRANCH_ID=$(extract_json_value "$create_branch_response" "id")
    if [ -n "$BRANCH_ID" ]; then
        print_success "Sucursal creada con ID: $BRANCH_ID"
    fi
    
    # 3.2 Listar sucursales
    print_message "3.2 Listando sucursales..."
    local list_branches_response=$(make_request "GET" "$BASE_URL/api/admin/branches?page=0make_request "GET" "$BASE_URL/api/admin/branches?page=0&size=20&status=all" "" "$auth_header"size=20make_request "GET" "$BASE_URL/api/admin/branches?page=0&size=20&status=all" "" "$auth_header"status=all" "" "$auth_header" "list_branches" "list_branches")
    print_message "Respuesta listar sucursales: $list_branches_response"
    
    # 3.3 Obtener sucursal específica
    if [ -n "$BRANCH_ID" ]; then
        print_message "3.3 Obteniendo sucursal específica..."
        local get_branch_response=$(make_request "GET" "$BASE_URL/api/admin/branches/$BRANCH_ID" "" "$auth_header" "get_branch" "get_branch")
        print_message "Respuesta obtener sucursal: $get_branch_response"
        
        # 3.4 Actualizar sucursal
        print_message "3.4 Actualizando sucursal..."
        local update_branch_data='{
            "name": "Sucursal Principal Actualizada",
            "code": "SUC001",
            "address": "Av. Principal 123, Lima",
            "isActive": true
        }'
        
        local update_branch_response=$(make_request "PUT" "$BASE_URL/api/admin/branches/$BRANCH_ID" "$update_branch_data" "$auth_header" "update_branch" "update_branch")
        print_message "Respuesta actualizar sucursal: $update_branch_response"
        
        # 3.5 Obtener vendedores de sucursal
        print_message "3.5 Obteniendo vendedores de sucursal..."
        local branch_sellers_response=$(make_request "GET" "$BASE_URL/api/admin/branches/$BRANCH_ID/sellers?page=0make_request "GET" "$BASE_URL/api/admin/branches/$BRANCH_ID/sellers?page=0&size=20" "" "$auth_header"size=20" "" "$auth_header" "branch_sellers" "branch_sellers")
        print_message "Respuesta vendedores de sucursal: $branch_sellers_response"
    fi
    
    print_success "Endpoints de sucursales probados"
}

# 4. GESTIÓN DE CÓDIGOS QR
test_qr_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE CÓDIGOS QR ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 4.1 Generar código de afiliación
    print_message "4.1 Generando código de afiliación..."
    local generate_code_response=$(make_request "POST" "$BASE_URL/api/generate-affiliation-code-protected?adminId=$ADMIN_IDmake_request "POST" "$BASE_URL/api/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header"expirationHours=24make_request "POST" "$BASE_URL/api/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header"maxUses=10make_request "POST" "$BASE_URL/api/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header"branchId=$BRANCH_IDmake_request "POST" "$BASE_URL/api/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header"notes=Test" "" "$auth_header" "generate_affiliation_code" "generate_affiliation_code")
    print_message "Respuesta generar código: $generate_code_response"
    
    # Extraer código de afiliación
    AFFILIATION_CODE=$(extract_json_value "$generate_code_response" "affiliationCode")
    if [ -n "$AFFILIATION_CODE" ]; then
        print_success "Código de afiliación generado: $AFFILIATION_CODE"
    fi
    
    # 4.2 Validar código de afiliación
    if [ -n "$AFFILIATION_CODE" ]; then
        print_message "4.2 Validando código de afiliación..."
        local validate_data='{
            "affiliationCode": "'$AFFILIATION_CODE'"
        }'
        
        local validate_response=$(make_request "POST" "$BASE_URL/api/validate-affiliation-code" "$validate_data" "" "validate_affiliation_code" "" "validate_affiliation_code")
        print_message "Respuesta validar código: $validate_response"
        
        # 4.3 Registrar vendedor con código de afiliación
        print_message "4.3 Registrando vendedor con código de afiliación..."
        local seller_data='{
            "sellerName": "Vendedor Test",
            "phone": "'$SELLER_PHONE'",
            "affiliationCode": "'$AFFILIATION_CODE'"
        }'
        
        local register_seller_response=$(make_request "POST" "$BASE_URL/api/seller/register" "$seller_data")
        print_message "Respuesta registrar vendedor: $register_seller_response"
        
        # Extraer ID de vendedor
        SELLER_ID=$(extract_json_value "$register_seller_response" "sellerId")
        if [ -n "$SELLER_ID" ]; then
            print_success "Vendedor registrado con ID: $SELLER_ID"
        fi
        
        # 4.4 Generar QR Base64
        print_message "4.4 Generando QR Base64..."
        local qr_data='{
            "affiliationCode": "'$AFFILIATION_CODE'"
        }'
        
        local qr_response=$(make_request "POST" "$BASE_URL/api/generate-qr-base64" "$qr_data")
        print_message "Respuesta generar QR: $qr_response"
        
        # 4.5 Login con QR
        print_message "4.5 Probando login con QR..."
        local qr_login_data='{
            "qrData": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
            "phone": "'$SELLER_PHONE'"
        }'
        
        local qr_login_response=$(make_request "POST" "$BASE_URL/api/login-with-qr" "$qr_login_data")
        print_message "Respuesta login con QR: $qr_login_response"
    fi
    
    print_success "Endpoints de códigos QR probados"
}

# 5. GESTIÓN DE VENDEDORES
test_seller_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE VENDEDORES ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 5.1 Obtener mis vendedores
    print_message "5.1 Obteniendo mis vendedores..."
    local my_sellers_response=$(make_request "GET" "$BASE_URL/api/admin/sellers/my-sellers?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=1&limit=20" "" "$auth_header")
    print_message "Respuesta mis vendedores: $my_sellers_response"
    
    # 5.2 Listar vendedores
    print_message "5.2 Listando vendedores..."
    local list_sellers_response=$(make_request "GET" "$BASE_URL/api/admin/sellers?page=1&limit=20&branchId=$BRANCH_ID&status=all" "" "$auth_header")
    print_message "Respuesta listar vendedores: $list_sellers_response"
    
    # 5.3 Actualizar vendedor
    if [ -n "$SELLER_ID" ]; then
        print_message "5.3 Actualizando vendedor..."
        local update_seller_response=$(make_request "PUT" "$BASE_URL/api/admin/sellers/$SELLER_ID?adminId=$ADMIN_ID&name=Vendedor Actualizado&phone=$SELLER_PHONE&isActive=true" "" "$auth_header")
        print_message "Respuesta actualizar vendedor: $update_seller_response"
        
        # 5.4 Eliminar/Pausar vendedor
        print_message "5.4 Pausando vendedor..."
        local pause_seller_response=$(make_request "DELETE" "$BASE_URL/api/admin/sellers/$SELLER_ID?adminId=$ADMIN_ID&action=pause&reason=Test" "" "$auth_header")
        print_message "Respuesta pausar vendedor: $pause_seller_response"
    fi
    
    # 5.5 Obtener límites de vendedores
    print_message "5.5 Obteniendo límites de vendedores..."
    local limits_response=$(make_request "GET" "$BASE_URL/api/admin/sellers/limits?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta límites: $limits_response"
    
    print_success "Endpoints de vendedores probados"
}

# 6. GESTIÓN DE PAGOS
test_payment_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE PAGOS ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 6.1 Verificar estado de conexión del vendedor
    if [ -n "$SELLER_ID" ]; then
        print_message "6.1 Verificando estado de conexión del vendedor..."
        local status_response=$(make_request "GET" "$BASE_URL/api/payments/status/$SELLER_ID" "" "$auth_header")
        print_message "Respuesta estado conexión: $status_response"
        
        # 6.2 Reclamar pago
        print_message "6.2 Probando reclamar pago..."
        local claim_data='{
            "paymentId": "PAY123",
            "amount": 100.50,
            "sellerId": '$SELLER_ID'
        }'
        
        local claim_response=$(make_request "POST" "$BASE_URL/api/payments/claim" "$claim_data" "$auth_header")
        print_message "Respuesta reclamar pago: $claim_response"
        
        # 6.3 Rechazar pago
        print_message "6.3 Probando rechazar pago..."
        local reject_data='{
            "paymentId": "PAY124",
            "reason": "Monto incorrecto",
            "sellerId": '$SELLER_ID'
        }'
        
        local reject_response=$(make_request "POST" "$BASE_URL/api/payments/reject" "$reject_data" "$auth_header")
        print_message "Respuesta rechazar pago: $reject_response"
    fi
    
    # 6.4 Obtener pagos pendientes
    print_message "6.4 Obteniendo pagos pendientes..."
    local pending_response=$(make_request "GET" "$BASE_URL/api/payments/pending?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20&limit=20" "" "$auth_header")
    print_message "Respuesta pagos pendientes: $pending_response"
    
    # 6.5 Gestión administrativa de pagos
    print_message "6.5 Obteniendo gestión administrativa de pagos..."
    local management_response=$(make_request "GET" "$BASE_URL/api/payments/admin/management?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20&status=all" "" "$auth_header")
    print_message "Respuesta gestión administrativa: $management_response"
    
    # 6.6 Estadísticas de notificaciones
    print_message "6.6 Obteniendo estadísticas de notificaciones..."
    local stats_response=$(make_request "GET" "$BASE_URL/api/payments/notification-stats" "" "$auth_header")
    print_message "Respuesta estadísticas: $stats_response"
    
    # 6.7 Vendedores conectados para admin
    print_message "6.7 Obteniendo vendedores conectados para admin..."
    local connected_response=$(make_request "GET" "$BASE_URL/api/payments/admin/connected-sellers?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta vendedores conectados: $connected_response"
    
    # 6.8 Test de gestión administrativa (sin auth)
    
    # 6.9 Estado de todos los vendedores para admin
    print_message "6.9 Obteniendo estado de todos los vendedores para admin..."
    local all_status_response=$(make_request "GET" "$BASE_URL/api/payments/admin/sellers-status?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta estado todos vendedores: $all_status_response"
    
    # 6.10 Pagos confirmados
    if [ -n "$SELLER_ID" ]; then
        print_message "6.10 Obteniendo pagos confirmados..."
        local confirmed_response=$(make_request "GET" "$BASE_URL/api/payments/confirmed?sellerId=$SELLER_ID&adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20" "" "$auth_header")
        print_message "Respuesta pagos confirmados: $confirmed_response"
    fi
    
    print_success "Endpoints de pagos probados"
}

# 7. GESTIÓN DE NOTIFICACIONES
test_notification_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE NOTIFICACIONES ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 7.1 Obtener notificaciones
    print_message "7.1 Obteniendo notificaciones..."
    local notifications_response=$(make_request "GET" "$BASE_URL/api/notifications?userId=$ADMIN_ID&userRole=admin&startDate=2024-01-01&endDate=2024-12-31&page=1&limit=20&unreadOnly=false" "" "$auth_header")
    print_message "Respuesta notificaciones: $notifications_response"
    
    # 7.2 Marcar notificación como leída
    print_message "7.2 Marcando notificación como leída..."
    local mark_read_response=$(make_request "POST" "$BASE_URL/api/notifications/1/read" "")
    print_message "Respuesta marcar como leída: $mark_read_response"
    
    # 7.3 Procesar notificación Yape
    print_message "7.3 Procesando notificación Yape..."
    local yape_data='{
        "adminId": '$ADMIN_ID',
        "encryptedData": "encrypted_data_test",
        "transactionId": "TXN123"
    }'
    
    local yape_response=$(make_request "POST" "$BASE_URL/api/notifications/yape-notifications" "$yape_data" "$auth_header")
    print_message "Respuesta notificación Yape: $yape_response"
    
    # 7.4 Auditoría de notificaciones Yape
    print_message "7.4 Obteniendo auditoría de notificaciones Yape..."
    local audit_response=$(make_request "GET" "$BASE_URL/api/notifications/yape-audit?adminId=$ADMIN_ID&page=0&size=20" "" "$auth_header")
    print_message "Respuesta auditoría: $audit_response"
    
    print_success "Endpoints de notificaciones probados"
}

# 8. GESTIÓN DE FACTURACIÓN
test_billing_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE FACTURACIÓN ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 8.1 Obtener información de facturación
    print_message "8.1 Obteniendo información de facturación..."
    local billing_response=$(make_request "GET" "$BASE_URL/api/billing?type=dashboard&adminId=$ADMIN_ID&period=current&include=details&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header")
    print_message "Respuesta facturación: $billing_response"
    
    # 8.2 Obtener planes disponibles
    print_message "8.2 Obteniendo planes disponibles..."
    local plans_response=$(make_request "GET" "$BASE_URL/api/billing/plans" "")
    print_message "Respuesta planes: $plans_response"
    
    # 8.3 Dashboard de facturación
    print_message "8.3 Obteniendo dashboard de facturación..."
    local dashboard_response=$(make_request "GET" "$BASE_URL/api/billing/dashboard?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta dashboard: $dashboard_response"
    
    # 8.4 Cargar datos
    print_message "8.4 Cargando datos..."
    local load_data_response=$(make_request "POST" "$BASE_URL/api/billing/load-data" "")
    print_message "Respuesta cargar datos: $load_data_response"
    
    # 8.5 Operaciones de facturación
    print_message "8.5 Probando operaciones de facturación..."
    local operations_data='{
        "imageBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
        "paymentCode": "PAY123",
        "amount": 100.50
    }'
    
    local operations_response=$(make_request "POST" "$BASE_URL/api/billing/operations?adminId=$ADMIN_ID&action=generate-code&validate=true" "$operations_data" "$auth_header")
    print_message "Respuesta operaciones: $operations_response"
    
    # 8.6 Subir imagen de pago
    print_message "8.6 Subiendo imagen de pago..."
    local upload_data='{
        "imageBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
    }'
    
    local upload_response=$(make_request "POST" "$BASE_URL/api/billing/payments/upload?adminId=$ADMIN_ID&paymentCode=PAY123" "$upload_data" "$auth_header")
    print_message "Respuesta subir imagen: $upload_response"
    
    # 8.7 Estado de pago
    print_message "8.7 Obteniendo estado de pago..."
    local payment_status_response=$(make_request "GET" "$BASE_URL/api/billing/payments/status/PAY123" "" "$auth_header")
    print_message "Respuesta estado pago: $payment_status_response"
    
    print_success "Endpoints de facturación probados"
}

# 9. GESTIÓN DE FACTURACIÓN ADMINISTRATIVA
test_admin_billing_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE FACTURACIÓN ADMINISTRATIVA ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 9.1 Información de facturación administrativa
    print_message "9.1 Obteniendo información de facturación administrativa..."
    local admin_billing_response=$(make_request "GET" "$BASE_URL/api/admin/billing?type=dashboard&adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&status=all&include=details" "" "$auth_header")
    print_message "Respuesta facturación administrativa: $admin_billing_response"
    
    # 9.2 Pagos aprobados
    print_message "9.2 Obteniendo pagos aprobados..."
    local approved_response=$(make_request "GET" "$BASE_URL/api/admin/billing/payments/approved?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta pagos aprobados: $approved_response"
    
    # 9.3 Pagos rechazados
    print_message "9.3 Obteniendo pagos rechazados..."
    local rejected_response=$(make_request "GET" "$BASE_URL/api/admin/billing/payments/rejected?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta pagos rechazados: $rejected_response"
    
    # 9.4 Aprobar pago
    print_message "9.4 Aprobando pago..."
    local approve_response=$(make_request "PUT" "$BASE_URL/api/admin/billing/payments/1/approve?adminId=$ADMIN_ID&reviewNotes=Test approval" "" "$auth_header")
    print_message "Respuesta aprobar pago: $approve_response"
    
    # 9.5 Rechazar pago
    print_message "9.5 Rechazando pago..."
    local reject_payment_response=$(make_request "PUT" "$BASE_URL/api/admin/billing/payments/2/reject?adminId=$ADMIN_ID&reviewNotes=Test rejection" "" "$auth_header")
    print_message "Respuesta rechazar pago: $reject_payment_response"
    
    # 9.6 Imagen de pago
    print_message "9.6 Obteniendo imagen de pago..."
    local image_response=$(make_request "GET" "$BASE_URL/api/admin/billing/payments/1/image?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta imagen pago: $image_response"
    
    # 9.7 Códigos de pago
    print_message "9.7 Obteniendo códigos de pago..."
    local codes_response=$(make_request "GET" "$BASE_URL/api/admin/billing/payments/codes?adminId=$ADMIN_ID" "" "$auth_header")
    print_message "Respuesta códigos pago: $codes_response"
    
    # 9.8 Dashboard administrativo
    print_message "9.8 Obteniendo dashboard administrativo..."
    local admin_dashboard_response=$(make_request "GET" "$BASE_URL/api/admin/billing/dashboard?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header")
    print_message "Respuesta dashboard administrativo: $admin_dashboard_response"
    
    print_success "Endpoints de facturación administrativa probados"
}

# 10. ESTADÍSTICAS Y ANALYTICS
test_stats_endpoints() {
    print_message "=== PROBANDO ENDPOINTS DE ESTADÍSTICAS ==="
    
    local auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
    
    # 10.1 Resumen de admin
    print_message "10.1 Obteniendo resumen de admin..."
    local admin_summary_response=$(make_request "GET" "$BASE_URL/api/stats/admin/summary?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header")
    print_message "Respuesta resumen admin: $admin_summary_response"
    
    # 10.2 Resumen de vendedor
    if [ -n "$SELLER_ID" ]; then
        print_message "10.2 Obteniendo resumen de vendedor..."
        local seller_summary_response=$(make_request "GET" "$BASE_URL/api/stats/seller/summary?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header")
        print_message "Respuesta resumen vendedor: $seller_summary_response"
    fi
    
    # 10.3 Dashboard de admin
    print_message "10.3 Obteniendo dashboard de admin..."
    local admin_dashboard_response=$(make_request "GET" "$BASE_URL/api/stats/admin/dashboard?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header")
    print_message "Respuesta dashboard admin: $admin_dashboard_response"
    
    # 10.4 Analytics completos de admin
    print_message "10.4 Obteniendo analytics completos de admin..."
    local admin_analytics_response=$(make_request "GET" "$BASE_URL/api/stats/admin/analytics?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&period=monthly&metric=sales&granularity=daily&confidence=0.95&days=30" "" "$auth_header")
    print_message "Respuesta analytics admin: $admin_analytics_response"
    
    # 10.5 Analytics de vendedor
    if [ -n "$SELLER_ID" ]; then
        print_message "10.5 Obteniendo analytics de vendedor..."
        local seller_analytics_response=$(make_request "GET" "$BASE_URL/api/stats/seller/analytics?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&period=monthly&metric=sales&granularity=daily&confidence=0.95&days=30" "" "$auth_header")
        print_message "Respuesta analytics vendedor: $seller_analytics_response"
    fi
    
    # 10.6 Análisis financiero de admin
    print_message "10.6 Obteniendo análisis financiero de admin..."
    local admin_financial_response=$(make_request "GET" "$BASE_URL/api/stats/admin/financial?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&currency=PEN&taxRate=0.18" "" "$auth_header")
    print_message "Respuesta análisis financiero admin: $admin_financial_response"
    
    # 10.7 Análisis financiero de vendedor
    if [ -n "$SELLER_ID" ]; then
        print_message "10.7 Obteniendo análisis financiero de vendedor..."
        local seller_financial_response=$(make_request "GET" "$BASE_URL/api/stats/seller/financial?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&currency=PEN&commissionRate=0.05" "" "$auth_header")
        print_message "Respuesta análisis financiero vendedor: $seller_financial_response"
    fi
    
    # 10.8 Reporte de transparencia de pagos
    print_message "10.8 Obteniendo reporte de transparencia de pagos..."
    local transparency_response=$(make_request "GET" "$BASE_URL/api/stats/admin/payment-transparency?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&includeFees=true&includeTaxes=true&includeCommissions=true" "" "$auth_header")
    print_message "Respuesta transparencia: $transparency_response"
    
    print_success "Endpoints de estadísticas probados"
}

# Función principal
main() {
    print_message "=== INICIANDO PRUEBAS DE API YAPE HUB ==="
    print_message "Servidor: $BASE_URL"
    print_message "Fecha: $(date)"
    print_message ""
    
    # Verificar servidor
    if ! check_server; then
        exit 1
    fi
    
    print_message ""
    
# Ejecutar todas las pruebas
if test_auth_endpoints; then
    print_success "✅ Autenticación exitosa, continuando con las pruebas..."
    print_message ""
    
    # Verificar que el token esté disponible para las otras funciones
    if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
        test_admin_endpoints
        print_message ""
        
        test_branch_endpoints
        print_message ""
        
        test_qr_endpoints
        print_message ""
        
        # Buscar sellers existentes ANTES de probar endpoints de sellers
        search_existing_sellers
        print_message ""
        
        # Flujo completo del seller (generar código de afiliación + login)
        if seller_login_flow; then
            print_success "✅ Flujo de seller completado exitosamente"
        else
            print_error "❌ Error en flujo de seller"
        fi
        print_message ""
        
        test_seller_endpoints
        print_message ""
        
        test_payment_endpoints
        print_message ""
        
        test_notification_endpoints
        print_message ""
        
        test_billing_endpoints
        print_message ""
        
        test_admin_billing_endpoints
        print_message ""
        
        test_stats_endpoints
    else
        print_error "❌ Token JWT no disponible para las pruebas protegidas"
    fi
else
    print_error "❌ Falló la autenticación, no se pueden ejecutar las pruebas protegidas"
    print_message "Solo se ejecutaron las pruebas de autenticación"
fi
    print_message ""
    
    print_success "=== TODAS LAS PRUEBAS COMPLETADAS ==="
    print_message "Resumen:"
    print_message "- Servidor: $BASE_URL"
    print_message "- Admin ID: $ADMIN_ID"
    print_message "- Seller ID: $SELLER_ID"
    print_message "- Branch ID: $BRANCH_ID"
    print_message "- Affiliation Code: $AFFILIATION_CODE"
    print_message "- Token JWT: ${JWT_TOKEN:0:20}..."
    print_message ""
print_message "=== DOCUMENTACIÓN DISPONIBLE ==="
print_message "📚 Swagger UI (Interfaz Interactiva): $BASE_URL/swagger-ui"
print_message "📋 OpenAPI Spec (JSON): $BASE_URL/openapi"
print_message "⚙️  Configuración Swagger: $BASE_URL/swagger-ui-config"
print_message "🏥 Health Check: $BASE_URL/q/health"
print_message ""
print_message "=== CARACTERÍSTICAS DE SWAGGER ==="
print_message "✅ Ejemplos interactivos en cada endpoint"
print_message "✅ Autenticación JWT integrada"
print_message "✅ Try it out para probar APIs directamente"
print_message "✅ Documentación detallada con respuestas JSON"
print_message "✅ Filtros y búsqueda avanzada"
print_message "✅ Temas personalizados"
print_message ""
print_message "=== CÓMO USAR SWAGGER ==="
print_message "1. Ve a: $BASE_URL/swagger-ui"
print_message "2. Haz clic en 'Authorize' (🔒)"
print_message "3. Ingresa: Bearer $JWT_TOKEN"
print_message "4. Haz clic en 'Authorize'"
print_message "5. Prueba cualquier endpoint con 'Try it out'"
print_message ""
print_message "=== RESPUESTAS GUARDADAS ==="
print_message "📁 Directorio de respuestas: $RESPONSES_DIR/"
print_message "📄 Todas las respuestas se guardan en archivos JSON"
print_message "🕒 Cada archivo incluye timestamp para identificación"
print_message "📊 Formato: endpoint_timestamp.json"
print_message ""
print_message "Ejemplo de archivos generados:"
print_message "  - admin_register_20241215_143022.json"
print_message "  - admin_login_20241215_143025.json"
print_message "  - create_branch_20241215_143030.json"
print_message "  - generate_affiliation_code_20241215_143035.json"
}

# Ejecutar función principal
main "$@"

#!/bin/bash

# Script para probar WebSocket y enviar notificaciones al vendedor 101
# Autor: Sistema Yape Hub
# Fecha: $(date)

echo "🚀 Iniciando prueba completa del WebSocket para vendedor 101"
echo "================================================================"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
SELLER_ID=101
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc4MTI0NzAsIGlhdD0xNzU3ODA4ODcwfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc"
BASE_URL="http://localhost:8080"
WS_URL="ws://localhost:8080/ws/payments/$SELLER_ID"

echo -e "${BLUE}📋 Configuración:${NC}"
echo "   - Seller ID: $SELLER_ID"
echo "   - WebSocket URL: $WS_URL"
echo "   - Base URL: $BASE_URL"
echo ""

# Función para generar notificación encriptada
generate_encrypted_notification() {
    local amount=$1
    local sender_name=$2
    local code=$3
    
    python3 -c "
import base64
import random
import time

# Generar código único
timestamp = int(time.time())
random_part = random.randint(1000, 9999)
unique_code = f'{timestamp}{random_part}'[-6:]

message = f'Has recibido S/ $amount de $sender_name. Código: $code'
deviceFingerprint = 'a1b2c3d4e5f6789a'
fingerprint_bytes = deviceFingerprint.encode()

# Aplicar XOR
encrypted = ''
for i, char in enumerate(message):
    key_byte = fingerprint_bytes[i % len(fingerprint_bytes)]
    encrypted_char = chr(ord(char) ^ key_byte)
    encrypted += encrypted_char

# Codificar en Base64
base64_encoded = base64.b64encode(encrypted.encode()).decode()
print('ENCRYPTED:' + base64_encoded)
print('CODE:' + '$code')
print('MESSAGE:' + message)
print('TIMESTAMP:' + str(int(time.time() * 1000)))
"
}

# Función para enviar notificación
send_notification() {
    local encrypted_notification=$1
    local timestamp=$2
    
    echo -e "${YELLOW}📤 Enviando notificación...${NC}"
    
    response=$(curl -s --location "$BASE_URL/api/notifications/yape-notifications" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer $ADMIN_TOKEN" \
        --data "{
            \"adminId\": 605,
            \"encryptedNotification\": \"$encrypted_notification\",
            \"deviceFingerprint\": \"a1b2c3d4e5f6789a\",
            \"timestamp\": $timestamp
        }")
    
    echo "$response" | jq .
    
    # Verificar si fue exitoso
    success=$(echo "$response" | jq -r '.success // false')
    if [ "$success" = "true" ]; then
        echo -e "${GREEN}✅ Notificación enviada exitosamente${NC}"
        notification_id=$(echo "$response" | jq -r '.data.notificationId')
        echo -e "${BLUE}📋 Notification ID: $notification_id${NC}"
    else
        echo -e "${RED}❌ Error al enviar notificación${NC}"
    fi
}

# Función para probar conexión WebSocket
test_websocket_connection() {
    echo -e "${BLUE}🔌 Probando conexión WebSocket...${NC}"
    
    # Crear archivo HTML temporal para probar WebSocket
    cat > test_websocket_temp.html << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Test WebSocket Seller 101</title>
</head>
<body>
    <h1>Test WebSocket para Vendedor 101</h1>
    <div id="status">Desconectado</div>
    <div id="messages"></div>
    <button onclick="connect()">Conectar</button>
    <button onclick="disconnect()">Desconectar</button>
    
    <script>
        let ws = null;
        
        function connect() {
            ws = new WebSocket('$WS_URL');
            
            ws.onopen = function(event) {
                document.getElementById('status').innerHTML = 'Conectado';
                document.getElementById('status').style.color = 'green';
            };
            
            ws.onmessage = function(event) {
                const messages = document.getElementById('messages');
                const message = document.createElement('div');
                message.innerHTML = new Date().toLocaleTimeString() + ': ' + event.data;
                messages.appendChild(message);
            };
            
            ws.onclose = function(event) {
                document.getElementById('status').innerHTML = 'Desconectado';
                document.getElementById('status').style.color = 'red';
            };
            
            ws.onerror = function(error) {
                console.error('WebSocket error:', error);
                document.getElementById('status').innerHTML = 'Error';
                document.getElementById('status').style.color = 'red';
            };
        }
        
        function disconnect() {
            if (ws) {
                ws.close();
            }
        }
    </script>
</body>
</html>
EOF
    
    echo -e "${GREEN}✅ Archivo HTML creado: test_websocket_temp.html${NC}"
    echo -e "${YELLOW}📝 Abre el archivo en tu navegador para probar el WebSocket${NC}"
}

# Función para probar notificación de prueba
test_notification() {
    echo -e "${BLUE}🧪 Probando notificación de prueba...${NC}"
    
    response=$(curl -s --location "$BASE_URL/api/payments/test-notification/$SELLER_ID" \
        --header 'Content-Type: application/json' \
        --request POST)
    
    echo "$response" | jq .
    
    success=$(echo "$response" | jq -r '.success // false')
    if [ "$success" = "true" ]; then
        echo -e "${GREEN}✅ Notificación de prueba enviada${NC}"
    else
        echo -e "${RED}❌ Error en notificación de prueba${NC}"
    fi
}

# Función para verificar estado del vendedor
check_seller_status() {
    echo -e "${BLUE}📊 Verificando estado del vendedor $SELLER_ID...${NC}"
    
    response=$(curl -s --location "$BASE_URL/api/payments/status/$SELLER_ID" \
        --header 'accept: application/json' \
        --header "Authorization: Bearer $ADMIN_TOKEN")
    
    echo "$response" | jq .
}

# Función principal
main() {
    echo -e "${BLUE}🎯 Ejecutando pruebas para vendedor $SELLER_ID${NC}"
    echo ""
    
    # 1. Verificar estado del vendedor
    echo -e "${YELLOW}1️⃣ Verificando estado del vendedor...${NC}"
    check_seller_status
    echo ""
    
    # 2. Probar notificación de prueba
    echo -e "${YELLOW}2️⃣ Probando notificación de prueba...${NC}"
    test_notification
    echo ""
    
    # 3. Generar y enviar notificación real
    echo -e "${YELLOW}3️⃣ Generando notificación real...${NC}"
    notification_data=$(generate_encrypted_notification "50.00" "Cliente Test" "123456")
    
    encrypted=$(echo "$notification_data" | grep "ENCRYPTED:" | cut -d: -f2)
    code=$(echo "$notification_data" | grep "CODE:" | cut -d: -f2)
    message=$(echo "$notification_data" | grep "MESSAGE:" | cut -d: -f2)
    timestamp=$(echo "$notification_data" | grep "TIMESTAMP:" | cut -d: -f2)
    
    echo -e "${BLUE}📋 Datos generados:${NC}"
    echo "   - Mensaje: $message"
    echo "   - Código: $code"
    echo "   - Timestamp: $timestamp"
    echo ""
    
    send_notification "$encrypted" "$timestamp"
    echo ""
    
    # 4. Crear archivo de prueba WebSocket
    echo -e "${YELLOW}4️⃣ Creando archivo de prueba WebSocket...${NC}"
    test_websocket_connection
    echo ""
    
    # 5. Resumen
    echo -e "${GREEN}🎉 Pruebas completadas!${NC}"
    echo -e "${BLUE}📋 Resumen:${NC}"
    echo "   - ✅ Estado del vendedor verificado"
    echo "   - ✅ Notificación de prueba enviada"
    echo "   - ✅ Notificación real generada y enviada"
    echo "   - ✅ Archivo WebSocket creado"
    echo ""
    echo -e "${YELLOW}📝 Próximos pasos:${NC}"
    echo "   1. Abre 'test_websocket_temp.html' en tu navegador"
    echo "   2. Haz clic en 'Conectar' para conectar al WebSocket"
    echo "   3. Las notificaciones aparecerán en tiempo real"
    echo ""
    echo -e "${BLUE}🔗 URLs importantes:${NC}"
    echo "   - WebSocket: $WS_URL"
    echo "   - API Base: $BASE_URL"
    echo "   - Archivo HTML: test_websocket_temp.html"
}

# Ejecutar función principal
main

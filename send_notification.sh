#!/bin/bash

# Script para enviar notificación de Yape con timestamp automático
# Uso: ./send_notification.sh [monto] [mensaje]

# Configuración
ADMIN_ID=605
DEVICE_FINGERPRINT="a1b2c3d4e5f6789a"
AUTH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc4MTI0NzAsIGlhdD0xNzU3ODA4ODcwfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc"
API_URL="https://ks9ql0l7-8080.brs.devtunnels.ms/api/notifications/yape-notifications"

# Parámetros por defecto
AMOUNT=${1:-30.00}
MESSAGE=${2:-"Cliente Test"}

echo "🚀 Generando notificación de Yape..."
echo "💰 Monto: S/ $AMOUNT"
echo "👤 Cliente: $MESSAGE"

# Generar timestamp actual en milisegundos
TIMESTAMP=$(date +%s)000

# Generar código único (compatible con macOS)
RANDOM_CODE=$(python3 -c "import random; print(random.randint(100000, 999999))")

# Crear mensaje completo
FULL_MESSAGE="Has recibido S/ $AMOUNT de $MESSAGE. Código: $RANDOM_CODE"

echo "📝 Mensaje: $FULL_MESSAGE"
echo "⏰ Timestamp: $TIMESTAMP"
echo "🔑 Código: $RANDOM_CODE"

# Generar notificación encriptada usando Python
ENCRYPTED_NOTIFICATION=$(python3 -c "
import base64
import sys

message = '$FULL_MESSAGE'
deviceFingerprint = '$DEVICE_FINGERPRINT'
fingerprint_bytes = deviceFingerprint.encode()

# Aplicar XOR
encrypted = ''
for i, char in enumerate(message):
    key_byte = fingerprint_bytes[i % len(fingerprint_bytes)]
    encrypted_char = chr(ord(char) ^ key_byte)
    encrypted += encrypted_char

# Codificar en Base64
base64_encoded = base64.b64encode(encrypted.encode()).decode()
print(base64_encoded)
")

echo "🔐 Notificación encriptada generada"

# Obtener información de vendedores del admin
echo "👥 Obteniendo información de vendedores del admin $ADMIN_ID..."

SELLERS_RESPONSE=$(curl -s --location "https://ks9ql0l7-8080.brs.devtunnels.ms/api/admin/sellers/my-sellers?adminId=$ADMIN_ID&limit=50" \
--header "Authorization: Bearer $AUTH_TOKEN")

echo "📋 Respuesta de vendedores:"
echo "$SELLERS_RESPONSE" | jq .

# Extraer información de vendedores
SELLERS_COUNT=$(echo "$SELLERS_RESPONSE" | jq -r '.data.sellers | length // 0')
ACTIVE_SELLERS_COUNT=$(echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[] | select(.isActive == true) | .sellerId' | wc -l | tr -d ' ')

echo "👥 Total de vendedores encontrados: $SELLERS_COUNT"
echo "✅ Vendedores activos: $ACTIVE_SELLERS_COUNT"

if [ "$SELLERS_COUNT" -gt 0 ]; then
    echo "📝 Lista de vendedores que recibirán la notificación:"
    echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[] | select(.isActive == true) | "  ✅ ID: \(.sellerId) | Nombre: \(.name) | Teléfono: \(.phone)"'
    echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[] | select(.isActive == false) | "  ❌ ID: \(.sellerId) | Nombre: \(.name) | Teléfono: \(.phone) (INACTIVO)"'
else
    echo "⚠️  No se encontraron vendedores para este admin"
fi

# Enviar la notificación
echo "📡 Enviando notificación al servidor..."

RESPONSE=$(curl -s --location "$API_URL" \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data "{
  \"adminId\": $ADMIN_ID,
  \"encryptedNotification\": \"$ENCRYPTED_NOTIFICATION\",
  \"deviceFingerprint\": \"$DEVICE_FINGERPRINT\",
  \"timestamp\": $TIMESTAMP
}")

echo "📋 Respuesta del servidor:"
echo "$RESPONSE" | jq .

# Verificar si fue exitoso
if echo "$RESPONSE" | jq -e '.success' > /dev/null; then
    echo "✅ ¡Notificación enviada exitosamente!"
    NOTIFICATION_ID=$(echo "$RESPONSE" | jq -r '.data.notificationId')
    TRANSACTION_ID=$(echo "$RESPONSE" | jq -r '.data.transactionId')
    echo "🆔 ID de notificación: $NOTIFICATION_ID"
    echo "🔄 ID de transacción: $TRANSACTION_ID"
    
    # Consultar pagos pendientes de cada vendedor activo
    echo ""
    echo "📋 Consultando pagos pendientes de cada vendedor..."
    
    # Obtener lista de vendedores activos
    ACTIVE_SELLERS=$(echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[] | select(.isActive == true) | .sellerId')
    
    for seller_id in $ACTIVE_SELLERS; do
        echo "🔍 Consultando pagos pendientes para vendedor $seller_id..."
        
        # Crear token JWT para el vendedor (simplificado para demo)
        SELLER_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj0kKHNlbGxlcl9pZCksIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1TRUxMRVIsIGV4cD0xNzU3ODEyNDcwLCBpYXQ9MTc1NzgwODg3MH0.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc"
        
        PENDING_PAYMENTS=$(curl -s --location "https://ks9ql0l7-8080.brs.devtunnels.ms/api/payments/pending/$seller_id" \
        --header "Authorization: Bearer $SELLER_TOKEN")
        
        PENDING_COUNT=$(echo "$PENDING_PAYMENTS" | jq -r '.data | length // 0')
        
        if [ "$PENDING_COUNT" -gt 0 ]; then
            echo "  💰 Vendedor $seller_id tiene $PENDING_COUNT pagos pendientes:"
            echo "$PENDING_PAYMENTS" | jq -r '.data[] | "    • ID: \(.paymentId) | Monto: S/ \(.amount) | Cliente: \(.senderName) | Código: \(.yapeCode)"'
        else
            echo "  ✅ Vendedor $seller_id no tiene pagos pendientes"
        fi
        echo ""
    done
    
else
    echo "❌ Error al enviar la notificación"
    ERROR_MSG=$(echo "$RESPONSE" | jq -r '.message // "Error desconocido"')
    echo "💬 Mensaje de error: $ERROR_MSG"
fi

echo "🏁 Script completado"

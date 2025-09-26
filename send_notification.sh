#!/bin/bash

# Script para enviar notificación de Yape con timestamp automático
# Uso: ./send_notification.sh [monto] [mensaje]

# Configuración
ADMIN_ID=605
DEVICE_FINGERPRINT="a1b2c3d4e5f6789a"
BASE_URL="http://localhost:8080"
API_URL="$BASE_URL/api/notifications/yape-notifications"

# Credenciales para login automático
EMAIL="calo@hotmail.com"
PASSWORD="Sky22234Ts*t"

# Parámetros por defecto
AMOUNT=${1:-30.60}
MESSAGE=${2:-"Cliente Test"}

echo "🚀 Generando notificación de Yape..."
echo "💰 Monto: S/ $AMOUNT"
echo "👤 Cliente: $MESSAGE"

# Función para hacer login y obtener token
login_and_get_token() {
    echo "🔐 Iniciando sesión..." >&2
    
    LOGIN_RESPONSE=$(curl -s --location "$BASE_URL/api/auth/login" \
    --header 'accept: application/json' \
    --header 'Content-Type: application/json' \
    --data-raw "{
        \"email\": \"$EMAIL\",
        \"password\": \"$PASSWORD\",
        \"deviceFingerprint\": \"$DEVICE_FINGERPRINT\",
        \"role\": \"ADMIN\"
    }")
    
    echo "📋 Respuesta de login:" >&2
    echo "$LOGIN_RESPONSE" | jq . >&2
    
    # Extraer token de acceso
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // empty')
    
    if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
        echo "❌ Error: No se pudo obtener el token de acceso" >&2
        echo "💬 Respuesta completa: $LOGIN_RESPONSE" >&2
        exit 1
    fi
    
    echo "✅ Token obtenido exitosamente" >&2
    echo "$ACCESS_TOKEN"
}

# Obtener token de acceso
AUTH_TOKEN=$(login_and_get_token)

# Generar timestamp actual en milisegundos
TIMESTAMP=$(date +%s)000

# Generar código único (compatible con macOS)
RANDOM_CODE=$(python3 -c "import random; print(random.randint(100000, 999999))")

# Crear mensaje completo en formato que espera el servicio
FULL_MESSAGE="Has recibido S/ $AMOUNT de $MESSAGE. El cód. de seguridad es: $RANDOM_CODE. Carlos Orbegoso L. te envió"

# Generar hash de deduplicación usando el timestamp y código
DEDUPLICATION_HASH=$(echo -n "${ADMIN_ID}_${TIMESTAMP}_${RANDOM_CODE}" | shasum -a 256 | cut -d' ' -f1)

echo "📝 Mensaje: $FULL_MESSAGE"
echo "⏰ Timestamp: $TIMESTAMP"
echo "🔑 Código: $RANDOM_CODE"
echo "🔐 Hash de deduplicación: $DEDUPLICATION_HASH"

# Crear JSON completo como lo hace el frontend
DEVICE_FINGERPRINT="a1b2c3d4e5f6789a"
JSON_MESSAGE=$(cat <<EOF
{
  "packageName": "com.bcp.innovacxion.yapeapp",
  "title": "Yape",
  "text": "$FULL_MESSAGE",
  "bigText": "$FULL_MESSAGE",
  "fullText": "$FULL_MESSAGE",
  "timestamp": $TIMESTAMP,
  "notificationId": $RANDOM_CODE
}
EOF
)

echo "📋 JSON creado: $JSON_MESSAGE"

# Encriptar el JSON usando XOR con deviceFingerprint (igual que el frontend)
ENCRYPTED_MESSAGE=$(python3 -c "
import sys
import json
message = '''$JSON_MESSAGE'''
fingerprint = '$DEVICE_FINGERPRINT'
fingerprint_bytes = fingerprint.encode()
encrypted = []
for i, char in enumerate(message):
    key_byte = fingerprint_bytes[i % len(fingerprint_bytes)]
    encrypted_char = ord(char) ^ key_byte
    encrypted.append(encrypted_char)
encrypted_bytes = bytes(encrypted)
import base64
print(base64.b64encode(encrypted_bytes).decode())
")

ENCRYPTED_NOTIFICATION="$ENCRYPTED_MESSAGE"

echo "🔐 Notificación encriptada generada (Base64)"

# Obtener información de vendedores del admin
echo "👥 Obteniendo información de vendedores del admin $ADMIN_ID..."

SELLERS_RESPONSE=$(curl -s --location "$BASE_URL/api/admin/sellers/my-sellers?adminId=$ADMIN_ID&limit=50" \
--header "Authorization: Bearer $AUTH_TOKEN")

echo "📋 Respuesta de vendedores:"
echo "$SELLERS_RESPONSE" | jq .

# Extraer información de vendedores
SELLERS_COUNT=$(echo "$SELLERS_RESPONSE" | jq -r '.data.sellers | length // 0')
ACTIVE_SELLERS_COUNT=$(echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[]? | select(.isActive == true) | .sellerId' | wc -l | tr -d ' ')

# Validar que SELLERS_COUNT sea un número
if ! [[ "$SELLERS_COUNT" =~ ^[0-9]+$ ]]; then
    SELLERS_COUNT=0
fi

echo "👥 Total de vendedores encontrados: $SELLERS_COUNT"
echo "✅ Vendedores activos: $ACTIVE_SELLERS_COUNT"

if [ "$SELLERS_COUNT" -gt 0 ]; then
    echo "📝 Lista de vendedores que recibirán la notificación:"
    echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[]? | select(.isActive == true) | "  ✅ ID: \(.sellerId) | Nombre: \(.name // "N/A") | Teléfono: \(.phone)"'
    echo "$SELLERS_RESPONSE" | jq -r '.data.sellers[]? | select(.isActive == false) | "  ❌ ID: \(.sellerId) | Nombre: \(.name // "N/A") | Teléfono: \(.phone) (INACTIVO)"'
else
    echo "⚠️  No se encontraron vendedores para este admin"
fi

# Enviar la notificación
echo "📡 Enviando notificación al servidor..."
echo "🔗 URL: $API_URL"
echo "🔑 Token: ${AUTH_TOKEN:0:50}..."
echo "🔑 Token completo: $AUTH_TOKEN"

# Crear el JSON de la petición (escapar comillas correctamente)
JSON_DATA=$(cat <<EOF
{
  "adminId": $ADMIN_ID,
  "encryptedNotification": "$ENCRYPTED_NOTIFICATION",
  "deviceFingerprint": "$DEVICE_FINGERPRINT",
  "timestamp": $TIMESTAMP,
  "deduplicationHash": "$DEDUPLICATION_HASH"
}
EOF
)

echo "📋 Datos a enviar:"
echo "$JSON_DATA" | jq .

echo "📡 Ejecutando curl..."
RESPONSE=$(curl -s --location "$API_URL" \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data "$JSON_DATA")

echo "📋 Respuesta del servidor:"
echo "$RESPONSE" | jq .

# Verificar si la respuesta indica éxito
if echo "$RESPONSE" | jq -e '.success == true' > /dev/null 2>&1; then
    echo "✅ ¡Notificación enviada exitosamente!"
else
    echo "❌ Error al enviar la notificación"
    echo "💬 Mensaje de error: $(echo "$RESPONSE" | jq -r '.message // "Error desconocido"')"
fi

echo "🏁 Script completado"

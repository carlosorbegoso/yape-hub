#!/bin/bash

# Script para enviar notificación de Yape con timestamp automático
# Uso: ./send_notification.sh [monto] [mensaje]

# Configuración
ADMIN_ID=1
DEVICE_FINGERPRINT="a1b2c3d4e5f6789a"
BASE_URL="http://localhost:8080"
API_URL="$BASE_URL/api/notifications/yape-notifications"

# Credenciales para login automático
EMAIL="carlos@hotmail.com"
PASSWORD="Sky22234Ts*t"

# Parámetros por defecto
AMOUNT=${1:-30.60}
MESSAGE=${2:-"Cliente Test"}

# Asegurar que AMOUNT tenga formato con punto decimal (ej. 30.60)
AMOUNT=$(printf "%.2f" "$AMOUNT")
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

# --- Reemplazo: generar ENCRYPTED_NOTIFICATION dinámicamente en lugar de usar la cadena fija ---
# (Antes se sobrescribía DEVICE_FINGERPRINT y ENCRYPTED_NOTIFICATION estaba hardcodeada)
# Si necesitas usar un device fingerprint específico, descomenta y ajusta la siguiente línea:
# DEVICE_FINGERPRINT="033a6d1cdc2a1920bc956959e2e77a12"

# Generar payload "encriptado" a partir del mensaje. Por defecto usamos base64 para evitar dependencias.
# Si el backend espera un esquema de cifrado concreto (AES, RSA, etc.), reemplaza esta generación por el algoritmo correcto.
# El servicio espera: base64( XOR( plaintextJSON, deviceFingerprint ) )
# Construimos un JSON con la clave `text` (o `fullText`) que el backend busca.
PLAINTEXT_JSON=$(printf '%s' "{\"text\": \"$FULL_MESSAGE\"}")

# Generar ENCRYPTED_NOTIFICATION aplicando XOR con device fingerprint y luego base64 (usando Python para evitar implementaciones complejas en bash)
# Pasamos el plaintext y el fingerprint como argumentos a Python para evitar problemas de escaping en el heredoc.
ENCRYPTED_NOTIFICATION=$(python3 - "$PLAINTEXT_JSON" "$DEVICE_FINGERPRINT" <<'PY'
import sys, base64
plaintext = sys.argv[1]
finger = sys.argv[2]
# XOR each character with fingerprint
xored = ''.join(chr(ord(a) ^ ord(finger[i % len(finger)])) for i, a in enumerate(plaintext))
print(base64.b64encode(xored.encode()).decode())
PY
)

echo "📋 Notificación encriptada generada dinámicamente (base64):"
echo "$ENCRYPTED_NOTIFICATION"
# --- Fin del reemplazo ---

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

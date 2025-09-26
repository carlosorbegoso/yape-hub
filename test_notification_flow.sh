#!/bin/bash

# Script para probar el flujo completo de notificaciones WebSocket
# 1. Envía una notificación
# 2. Verifica que se procese correctamente
# 3. Simula conexión WebSocket de vendedor

echo "🧪 Probando flujo completo de notificaciones WebSocket..."

# Configuración
ADMIN_ID=605
SELLER_ID=803
BASE_URL="http://localhost:8080"

# Paso 1: Obtener token de admin
echo "🔐 Paso 1: Obteniendo token de admin..."
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "calo@hotmail.com",
    "password": "Sky22234Ts*t",
    "deviceFingerprint": "H",
    "role": "ADMIN"
  }' | jq -r '.data.accessToken // empty')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
    echo "❌ No se pudo obtener token de admin"
    exit 1
fi

echo "✅ Token de admin obtenido"

# Paso 2: Enviar notificación
echo "📡 Paso 2: Enviando notificación..."
TIMESTAMP=$(date +%s)000
DEDUPLICATION_HASH=$(echo -n "test_$(date +%s)" | shasum -a 256 | cut -d' ' -f1)

RESPONSE=$(curl -s -X POST "$BASE_URL/api/notifications/yape-notifications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"adminId\": $ADMIN_ID,
    \"encryptedNotification\": \"SxFDAFUPUAQBLVMMVBsIEgEMVBtUWkUXDFwLWEEAUkpZXF1PTwVBBgUTQkMdG0ZZFg9cFwwbdlYLVAxFWgBSW8ODXRMFU0RhAgMMEE0TTVdIFkEDF3VYR1UKQUV4RQNUVV9AXEF6ShEXAUNXD0dQw4EQFw0ZRVdeWhkVXRcXZE4RAh4CHUFzCBEAwpcHHEFVXBJDBwRMR19dVF1FVxYNF1MAAhIfEQNfA2UGHBcQWxN6U0IODEoVeUtXXAJdFlgXLR8SRFYTBFgSWMKQRBZcQUFYVV9CE1ZHFmoaGVUcVBkXJF0SU8OAV08WAFRDFwZVFENQVlEGQ1xGDBkHCFUQSRVRFF1eZFZLFRReEyAFEV4OQhl9QgAGXlpFVhV1SxIRUhcEX0RZw4ATFFhEQQIDDBIRXksSY01DCRsHFxV8CRIGw4RTTxFWVRNABFERQwoAAlZBVEoIEFBSCRcaG0FQCFcWQ1YMQRAKAgRUDlYEV1RbB1AHDB4SDAxNXFBQVlgRWwpZfgUTCABO\",
    \"deviceFingerprint\": \"033a6d1cdc2a1920bc956959e2e77a12\",
    \"timestamp\": $TIMESTAMP,
    \"deduplicationHash\": \"$DEDUPLICATION_HASH\"
  }")

echo "📋 Respuesta del servidor:"
echo "$RESPONSE" | jq '.'

# Verificar si fue exitoso
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false')
if [ "$SUCCESS" = "true" ]; then
    echo "✅ Notificación enviada exitosamente"
    
    # Extraer información de la respuesta
    NOTIFICATION_ID=$(echo "$RESPONSE" | jq -r '.data.notificationId // empty')
    TRANSACTION_ID=$(echo "$RESPONSE" | jq -r '.data.transactionId // empty')
    AMOUNT=$(echo "$RESPONSE" | jq -r '.data.amount // empty')
    
    echo "📊 Detalles de la notificación:"
    echo "   - ID: $NOTIFICATION_ID"
    echo "   - Transacción: $TRANSACTION_ID"
    echo "   - Monto: $AMOUNT"
    
else
    echo "❌ Error al enviar notificación"
    echo "💬 Mensaje: $(echo "$RESPONSE" | jq -r '.message // "Sin mensaje"')"
    exit 1
fi

# Paso 3: Verificar vendedores conectados
echo "👥 Paso 3: Verificando vendedores del admin..."
SELLERS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/admin/sellers/my-sellers?adminId=$ADMIN_ID&page=1&limit=50" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "📋 Vendedores encontrados:"
echo "$SELLERS_RESPONSE" | jq '.data.sellers[] | {id: .sellerId, name: .name, phone: .phone, isOnline: .isOnline}'

# Paso 4: Crear un endpoint de prueba para verificar WebSocket
echo "🔌 Paso 4: Creando endpoint de prueba para WebSocket..."

# Crear un script Python para simular WebSocket con token modificado
cat > /tmp/websocket_debug.py << 'EOF'
import asyncio
import websockets
import json
import sys
import base64

def create_seller_token(admin_token, seller_id):
    """Crea un token modificado que incluye sellerId"""
    try:
        # Decodificar el token admin
        parts = admin_token.split('.')
        if len(parts) != 3:
            return None
            
        # Decodificar payload
        payload = base64.urlsafe_b64decode(parts[1] + '==').decode('utf-8')
        print(f"📋 Payload original: {payload}")
        
        # Agregar sellerId al payload
        if 'sellerId' not in payload:
            # Buscar el final del JSON y agregar sellerId
            if payload.endswith('}'):
                payload = payload[:-1] + f', "sellerId": {seller_id}}}'
            else:
                payload = payload + f', "sellerId": {seller_id}'
        
        print(f"📋 Payload modificado: {payload}")
        
        # Re-codificar
        new_payload = base64.urlsafe_b64encode(payload.encode('utf-8')).decode('utf-8').rstrip('=')
        
        # Reconstruir token
        new_token = f"{parts[0]}.{new_payload}.{parts[2]}"
        return new_token
        
    except Exception as e:
        print(f"❌ Error creando token: {e}")
        return None

async def test_websocket_with_modified_token():
    seller_id = sys.argv[1]
    admin_token = sys.argv[2]
    
    # Crear token modificado
    seller_token = create_seller_token(admin_token, seller_id)
    if not seller_token:
        print("❌ No se pudo crear token modificado")
        return
    
    uri = f"ws://localhost:8080/ws/payments/{seller_id}?token={seller_token}"
    
    try:
        print(f"🔌 Conectando a: {uri}")
        print(f"🔑 Token modificado: {seller_token[:50]}...")
        
        async with websockets.connect(uri) as websocket:
            print("✅ Conexión WebSocket establecida")
            
            # Esperar mensaje de bienvenida
            try:
                welcome_message = await asyncio.wait_for(websocket.recv(), timeout=10.0)
                print(f"📨 Mensaje de bienvenida: {welcome_message}")
                
                # Mantener conexión abierta para recibir notificaciones
                print("⏳ Esperando notificaciones por 30 segundos...")
                
                try:
                    while True:
                        message = await asyncio.wait_for(websocket.recv(), timeout=30.0)
                        print(f"📨 Notificación recibida: {message}")
                        
                        # Parsear mensaje
                        try:
                            data = json.loads(message)
                            if data.get('type') == 'PAYMENT_NOTIFICATION':
                                print(f"💰 Pago recibido: {data.get('data', {})}")
                        except:
                            pass
                            
                except asyncio.TimeoutError:
                    print("⏰ Timeout - No se recibieron más notificaciones")
                
            except asyncio.TimeoutError:
                print("⏰ Timeout esperando mensaje de bienvenida")
                
    except Exception as e:
        print(f"❌ Error en WebSocket: {e}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Uso: python3 websocket_debug.py <seller_id> <admin_token>")
        sys.exit(1)
    
    asyncio.run(test_websocket_with_modified_token())
EOF

# Ejecutar prueba WebSocket con token modificado
echo "🚀 Ejecutando prueba WebSocket con token modificado..."
python3 /tmp/websocket_debug.py "$SELLER_ID" "$ADMIN_TOKEN" &
WEBSOCKET_PID=$!

# Esperar un momento para que se establezca la conexión
sleep 3

# Enviar otra notificación para probar
echo "📡 Enviando segunda notificación para probar WebSocket..."
TIMESTAMP2=$(date +%s)000
DEDUPLICATION_HASH2=$(echo -n "test2_$(date +%s)" | shasum -a 256 | cut -d' ' -f1)

curl -s -X POST "$BASE_URL/api/notifications/yape-notifications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"adminId\": $ADMIN_ID,
    \"encryptedNotification\": \"SxFDAFUPUAQBLVMMVBsIEgEMVBtUWkUXDFwLWEEAUkpZXF1PTwVBBgUTQkMdG0ZZFg9cFwwbdlYLVAxFWgBSW8ODXRMFU0RhAgMMEE0TTVdIFkEDF3VYR1UKQUV4RQNUVV9AXEF6ShEXAUNXD0dQw4EQFw0ZRVdeWhkVXRcXZE4RAh4CHUFzCBEAwpcHHEFVXBJDBwRMR19dVF1FVxYNF1MAAhIfEQNfA2UGHBcQWxN6U0IODEoVeUtXXAJdFlgXLR8SRFYTBFgSWMKQRBZcQUFYVV9CE1ZHFmoaGVUcVBkXJF0SU8OAV08WAFRDFwZVFENQVlEGQ1xGDBkHCFUQSRVRFF1eZFZLFRReEyAFEV4OQhl9QgAGXlpFVhV1SxIRUhcEX0RZw4ATFFhEQQIDDBIRXksSY01DCRsHFxV8CRIGw4RTTxFWVRNABFERQwoAAlZBVEoIEFBSCRcaG0FQCFcWQ1YMQRAKAgRUDlYEV1RbB1AHDB4SDAxNXFBQVlgRWwpZfgUTCABO\",
    \"deviceFingerprint\": \"033a6d1cdc2a1920bc956959e2e77a12\",
    \"timestamp\": $TIMESTAMP2,
    \"deduplicationHash\": \"$DEDUPLICATION_HASH2\"
  }" | jq '.'

# Esperar a que termine el proceso WebSocket
wait $WEBSOCKET_PID

# Limpiar archivo temporal
rm -f /tmp/websocket_debug.py

echo "🏁 Prueba de flujo completo completada"

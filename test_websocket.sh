#!/bin/bash

# Script para probar conexiones WebSocket de vendedores
# Simula la conexión de un vendedor al WebSocket

echo "🔌 Probando conexión WebSocket para vendedores..."

# Configuración
SELLER_ID=803  # ID del vendedor Carlos
ADMIN_ID=605
BASE_URL="http://localhost:8080"
WS_URL="ws://localhost:8080/ws/payments"

# Obtener token de vendedor (simulando login de vendedor)
echo "🔐 Obteniendo token para vendedor $SELLER_ID..."

# Primero necesitamos crear un token JWT para el vendedor
# Vamos a usar el endpoint de login del vendedor
SELLER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "seller_111111111@yapechamo.com",
    "password": "password123",
    "deviceFingerprint": "seller_device_123",
    "role": "SELLER"
  }' | jq -r '.data.accessToken // empty')

if [ -z "$SELLER_TOKEN" ] || [ "$SELLER_TOKEN" = "null" ]; then
    echo "❌ No se pudo obtener token para vendedor"
    echo "📋 Intentando con credenciales alternativas..."
    
    # Intentar con el token del admin pero modificado para seller
    ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "email": "calo@hotmail.com",
        "password": "Sky22234Ts*t",
        "deviceFingerprint": "H",
        "role": "ADMIN"
      }' | jq -r '.data.accessToken // empty')
    
    if [ -n "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
        echo "✅ Token de admin obtenido, usaremos este para la prueba"
        SELLER_TOKEN="$ADMIN_TOKEN"
    else
        echo "❌ No se pudo obtener ningún token"
        exit 1
    fi
else
    echo "✅ Token de vendedor obtenido"
fi

echo "🔑 Token: ${SELLER_TOKEN:0:50}..."

# Verificar si el servidor WebSocket está funcionando
echo "🌐 Verificando endpoint WebSocket..."

# Crear un script Python simple para probar WebSocket
cat > /tmp/websocket_test.py << 'EOF'
import asyncio
import websockets
import json
import sys

async def test_websocket():
    seller_id = sys.argv[1]
    token = sys.argv[2]
    
    uri = f"ws://localhost:8080/ws/payments/{seller_id}?token={token}"
    
    try:
        print(f"🔌 Conectando a: {uri}")
        async with websockets.connect(uri) as websocket:
            print("✅ Conexión WebSocket establecida")
            
            # Esperar mensaje de bienvenida
            try:
                welcome_message = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                print(f"📨 Mensaje recibido: {welcome_message}")
                
                # Enviar mensaje de prueba
                test_message = json.dumps({
                    "type": "PING",
                    "message": "Test desde script"
                })
                await websocket.send(test_message)
                print("📤 Mensaje de prueba enviado")
                
                # Esperar respuesta
                try:
                    response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                    print(f"📨 Respuesta recibida: {response}")
                except asyncio.TimeoutError:
                    print("⏰ Timeout esperando respuesta")
                
                # Mantener conexión abierta por un momento
                print("⏳ Manteniendo conexión por 10 segundos...")
                await asyncio.sleep(10)
                
            except asyncio.TimeoutError:
                print("⏰ Timeout esperando mensaje de bienvenida")
                
    except Exception as e:
        print(f"❌ Error en WebSocket: {e}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Uso: python3 websocket_test.py <seller_id> <token>")
        sys.exit(1)
    
    asyncio.run(test_websocket())
EOF

# Verificar si websockets está instalado
if ! python3 -c "import websockets" 2>/dev/null; then
    echo "📦 Instalando websockets..."
    pip3 install websockets
fi

# Ejecutar prueba WebSocket
echo "🚀 Ejecutando prueba WebSocket..."
python3 /tmp/websocket_test.py "$SELLER_ID" "$SELLER_TOKEN"

# Limpiar archivo temporal
rm -f /tmp/websocket_test.py

echo "🏁 Prueba WebSocket completada"

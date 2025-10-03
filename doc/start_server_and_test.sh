#!/bin/bash

# Yape Hub Server Startup Script
# Este script inicia el servidor Yape Hub y luego ejecuta las pruebas de API

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Función para verificar si el servidor está corriendo
check_server() {
    local response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/swagger-ui")
    
    if [ "$response" = "200" ]; then
        return 0
    else
        return 1
    fi
}

# Función para esperar a que el servidor esté listo
wait_for_server() {
    print_message "Esperando a que el servidor esté listo..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if check_server; then
            print_success "Servidor está listo!"
            return 0
        fi
        
        print_message "Intento $attempt/$max_attempts - Esperando servidor..."
        sleep 5
        ((attempt++))
    done
    
    print_error "El servidor no se inició en el tiempo esperado"
    return 1
}

# Función principal
main() {
    print_message "=== INICIANDO SERVIDOR YAPE HUB ==="
    
    # Verificar si el servidor ya está corriendo
    if check_server; then
        print_success "El servidor ya está corriendo!"
        print_message "Ejecutando pruebas de API..."
        ./doc/test_all_apis.sh
        return 0
    fi
    
    # Iniciar el servidor en background
    print_message "Iniciando servidor con ./gradlew quarkusDev..."
    ./gradlew quarkusDev > server.log 2>&1 &
    SERVER_PID=$!
    
    print_message "Servidor iniciado con PID: $SERVER_PID"
    print_message "Logs del servidor: server.log"
    
    # Esperar a que el servidor esté listo
    if wait_for_server; then
        print_success "Servidor iniciado exitosamente!"
        print_message ""
        print_message "=== EJECUTANDO PRUEBAS DE API ==="
        ./doc/test_all_apis.sh
        
        print_message ""
        print_message "=== INSTRUCCIONES ADICIONALES ==="
        print_message "Para detener el servidor: kill $SERVER_PID"
        print_message "Para ver logs del servidor: tail -f server.log"
        print_message "Para acceder a Swagger UI: http://localhost:8080/swagger-ui"
        print_message "Para acceder a OpenAPI spec: http://localhost:8080/openapi"
    else
        print_error "No se pudo iniciar el servidor"
        kill $SERVER_PID 2>/dev/null
        return 1
    fi
}

# Manejar interrupciones
trap 'print_message "Deteniendo servidor..."; kill $SERVER_PID 2>/dev/null; exit 0' INT TERM

# Ejecutar función principal
main "$@"

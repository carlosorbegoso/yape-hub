#!/bin/bash

echo "🔍 Debug de configuración de Quarkus"
echo "=================================="

echo ""
echo "📋 Variables de entorno actuales:"
echo "QUARKUS_PROFILE: ${QUARKUS_PROFILE:-'no definida'}"
echo "DATABASE_URL: ${DATABASE_URL:-'no definida'}"
echo "DB_USERNAME: ${DB_USERNAME:-'no definida'}"
echo "DB_PASSWORD: ${DB_PASSWORD:0:3}*** (${#DB_PASSWORD} caracteres)"

echo ""
echo "🔧 Configuración que debería usar Quarkus:"
if [ "$QUARKUS_PROFILE" = "prod" ]; then
    echo "✅ Perfil: PRODUCCIÓN"
    echo "✅ Base de datos: ${DATABASE_URL:-'ERROR: DATABASE_URL no definida'}"
else
    echo "⚠️  Perfil: ${QUARKUS_PROFILE:-'DEFAULT (desarrollo)'}"
    echo "⚠️  Base de datos: Probablemente localhost:5432"
fi

echo ""
echo "🚀 Para forzar el perfil de producción:"
echo "export QUARKUS_PROFILE=prod"
echo "export DATABASE_URL='postgresql://167.172.117.133:5432/yapechamo'"
echo "export DB_USERNAME='tu_usuario'"
echo "export DB_PASSWORD='tu_password'"

echo ""
echo "🐳 Para Docker:"
echo "docker run -e QUARKUS_PROFILE=prod -e DATABASE_URL='postgresql://167.172.117.133:5432/yapechamo' ..."
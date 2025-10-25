#!/bin/bash

echo "🔍 Probando conexión a la base de datos..."

# Valores por defecto
DB_HOST=${DB_HOST:-167.172.117.133}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-yapechamo}
DB_USER=${DB_USERNAME:-yapechamo}

echo "Host: $DB_HOST"
echo "Puerto: $DB_PORT"
echo "Base de datos: $DB_NAME"
echo "Usuario: $DB_USER"

# Probar conexión con psql si está disponible
if command -v psql &> /dev/null; then
    echo ""
    echo "🔌 Probando conexión con psql..."
    PGPASSWORD=${DB_PASSWORD} psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT version();" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ Conexión exitosa"
    else
        echo "❌ Error de conexión"
    fi
else
    echo ""
    echo "⚠️  psql no está disponible, probando con nc..."
    if command -v nc &> /dev/null; then
        nc -z $DB_HOST $DB_PORT
        if [ $? -eq 0 ]; then
            echo "✅ Puerto $DB_PORT está abierto en $DB_HOST"
        else
            echo "❌ No se puede conectar a $DB_HOST:$DB_PORT"
        fi
    else
        echo "❌ Ni psql ni nc están disponibles para probar la conexión"
    fi
fi

echo ""
echo "🔧 URL de conexión que debería usar Quarkus:"
echo "postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
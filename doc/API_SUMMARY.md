# Yape Hub - Guía de Uso de APIs

## Descripción

Este directorio contiene la documentación completa y scripts de prueba para el sistema Yape Hub, un sistema de gestión de pagos que incluye:

- **Autenticación JWT** para administradores y vendedores
- **Gestión de sucursales** y vendedores
- **Sistema de códigos QR** para afiliación
- **Procesamiento de pagos** en tiempo real
- **Notificaciones** y WebSocket
- **Facturación** y suscripciones
- **Estadísticas** y analytics avanzados

## Archivos Incluidos

### 📚 Documentación
- **`README.md`** - Documentación completa de todas las APIs con ejemplos
- **`API_SUMMARY.md`** - Resumen ejecutivo de las APIs (este archivo)

### 🧪 Scripts de Prueba
- **`test_all_apis.sh`** - Script completo para probar todas las APIs
- **`start_server_and_test.sh`** - Script que inicia el servidor y ejecuta las pruebas

## Cómo Usar

### Opción 1: Servidor ya corriendo
Si el servidor ya está corriendo en `http://localhost:8080`:

```bash
# Ejecutar solo las pruebas
./doc/test_all_apis.sh
```

### Opción 2: Iniciar servidor y probar
Si necesitas iniciar el servidor primero:

```bash
# Iniciar servidor y ejecutar pruebas automáticamente
./doc/start_server_and_test.sh
```

### Opción 3: Iniciar servidor manualmente
```bash
# Iniciar servidor
./gradlew quarkusDev

# En otra terminal, ejecutar pruebas
./doc/test_all_apis.sh
```

## APIs Disponibles

### 🔐 Autenticación (`/api/auth`)
- Registro de administradores
- Login con email/password
- Login de vendedores por teléfono + código QR
- Refresh token
- Recuperación de contraseña

### 👨‍💼 Administradores (`/api/admin`)
- Gestión de perfil
- Actualización de datos

### 🏢 Sucursales (`/api/admin/branches`)
- CRUD completo de sucursales
- Listado con paginación
- Gestión de vendedores por sucursal

### 👥 Vendedores (`/api/admin/sellers`)
- Listado de vendedores por admin
- Actualización de datos
- Pausa/eliminación
- Límites de suscripción

### 📱 Códigos QR (`/api`)
- Generación de códigos de afiliación
- Validación de códigos
- Registro de vendedores
- Generación de QR Base64
- Login con QR

### 💳 Pagos (`/api/payments`)
- Estado de conexión de vendedores
- Reclamar/rechazar pagos
- Pagos pendientes
- Gestión administrativa
- Estadísticas de notificaciones

### 🔔 Notificaciones (`/api/notifications`)
- Listado de notificaciones
- Marcar como leídas
- Procesamiento de notificaciones Yape
- Auditoría de notificaciones

### 💰 Facturación (`/api/billing`)
- Información de facturación
- Operaciones de facturación
- Subida de imágenes de pago
- Estados de pago
- Planes de suscripción

### 🏛️ Facturación Administrativa (`/api/admin/billing`)
- Gestión administrativa de pagos
- Aprobación/rechazo de pagos
- Dashboard administrativo
- Códigos de pago

### 📊 Estadísticas (`/api/stats`)
- Resúmenes de admin y vendedor
- Analytics completos
- Análisis financiero
- Reportes de transparencia

## Parámetros Comunes

### Filtros de Fecha
- Formato: `yyyy-MM-dd`
- Ejemplo: `2024-01-15`

### Paginación
- `page`: Número de página (0 o 1 según endpoint)
- `size`/`limit`: Elementos por página (default: 20)

### Estados
- `status`: all, active, inactive, pending, approved, rejected

### Autenticación
- Header: `Authorization: Bearer <token>`

## Ejemplos de Uso Rápido

### 1. Registro y Login
```bash
# Registrar admin
curl -X POST http://localhost:8080/api/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{"businessName":"Mi Negocio","email":"admin@test.com","password":"password123","phone":"+51987654321","contactName":"Admin Test"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password123"}'
```

### 2. Crear Sucursal
```bash
curl -X POST http://localhost:8080/api/admin/branches \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Sucursal Principal","code":"SUC001","address":"Av. Principal 123"}'
```

### 3. Generar Código QR
```bash
curl -X POST "http://localhost:8080/api/generate-affiliation-code-protected?adminId=1&expirationHours=24&maxUses=10" \
  -H "Authorization: Bearer <token>"
```

### 4. Obtener Estadísticas
```bash
curl -X GET "http://localhost:8080/api/stats/admin/summary?adminId=1&startDate=2024-01-01&endDate=2024-12-31" \
  -H "Authorization: Bearer <token>"
```

## URLs Importantes

- **Servidor**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui` (Interfaz interactiva mejorada)
- **OpenAPI Spec**: `http://localhost:8080/openapi` (Especificación JSON)
- **Configuración Swagger**: `http://localhost:8080/swagger-ui-config`
- **Health Check**: `http://localhost:8080/q/health`

## Códigos de Estado

- **200 OK**: Solicitud exitosa
- **201 Created**: Recurso creado
- **400 Bad Request**: Solicitud inválida
- **401 Unauthorized**: No autorizado
- **403 Forbidden**: Acceso denegado
- **404 Not Found**: Recurso no encontrado
- **500 Internal Server Error**: Error del servidor

## Formato de Respuesta

```json
{
  "success": boolean,
  "message": "string",
  "data": object,
  "timestamp": "string"
}
```

## Manejo de Errores

```json
{
  "message": "string",
  "errorCode": "string",
  "details": object,
  "timestamp": "string"
}
```

## Notas Importantes

1. **Autenticación**: La mayoría de endpoints requieren JWT
2. **Fechas**: Formato `yyyy-MM-dd`
3. **Paginación**: Soporte completo en endpoints de lista
4. **CORS**: Configurado para desarrollo local
5. **WebSocket**: Notificaciones en tiempo real
6. **QR Codes**: Sistema completo de afiliación

## Troubleshooting

### Servidor no inicia
```bash
# Verificar puerto
lsof -i :8080

# Ver logs
tail -f server.log
```

### Error de autenticación
- Verificar que el token JWT sea válido
- Comprobar formato del header: `Authorization: Bearer <token>`

### Error de base de datos
- Verificar que PostgreSQL esté corriendo
- Comprobar configuración en `application.properties`

## 🚀 Swagger UI Mejorado

### Características Avanzadas
- **Interfaz Interactiva**: Prueba APIs directamente desde el navegador
- **Autenticación JWT**: Botón "Authorize" para agregar tokens fácilmente
- **Ejemplos Detallados**: Request/response examples en cada endpoint
- **Temas Personalizados**: Colores corporativos de Yape Hub
- **Filtros Avanzados**: Búsqueda rápida de endpoints
- **Try It Out**: Funcionalidad completa para probar APIs

### Cómo Usar Swagger
1. **Acceder**: `http://localhost:8080/swagger-ui`
2. **Autenticarse**: Clic en "Authorize" (🔒) → Ingresar `Bearer <token>`
3. **Probar**: Expandir endpoint → "Try it out" → Completar parámetros → "Execute"
4. **Ver Respuesta**: Resultado en tiempo real con formato JSON

### Documentación Disponible
- **Swagger UI**: Interfaz interactiva completa
- **OpenAPI Spec**: Especificación JSON para herramientas externas
- **Configuración**: Configuración personalizada de Swagger UI
- **Esquemas**: Modelos de datos bien definidos

## Soporte

Para más información:
- **Documentación completa**: `README.md`
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **Documentación Swagger**: `SWAGGER_DOCUMENTATION.md`
- **Logs del servidor**: `server.log`

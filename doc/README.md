# Yape Hub API Documentation

## Descripción General

Yape Hub es un sistema completo de gestión de pagos que permite a los administradores gestionar vendedores, procesar pagos en tiempo real, y obtener estadísticas detalladas. El sistema incluye autenticación JWT, gestión de sucursales, códigos de afiliación QR, y notificaciones en tiempo real.

## Información del Servidor

- **URL Base**: `http://localhost:8080`
- **Puerto**: 8080
- **Documentación Swagger**: `http://localhost:8080/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/openapi`

## Autenticación

El sistema utiliza JWT (JSON Web Tokens) para la autenticación. Incluye el token en el header `Authorization` con el formato:
```
Authorization: Bearer <token>
```

## Endpoints de la API

### 1. Autenticación (`/api/auth`)

#### POST `/api/auth/admin/register`
Registra un nuevo administrador de negocio.

**Parámetros del Body:**
```json
{
  "businessName": "string",
  "email": "string",
  "password": "string",
  "phone": "string",
  "contactName": "string"
}
```

**Respuesta:** 201 Created

#### POST `/api/auth/login`
Autentica un usuario y retorna un token de acceso.

**Parámetros del Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Respuesta:** 200 OK con token JWT

#### POST `/api/auth/refresh`
Genera un nuevo token de acceso usando el refresh token.

**Headers:**
- `X-Auth-Token`: Refresh token

**Respuesta:** 200 OK con nuevo token

#### POST `/api/auth/logout`
Cierra la sesión del usuario actual.

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### POST `/api/auth/forgot-password`
Envía un email para restablecer la contraseña.

**Query Parameters:**
- `email`: Email del usuario

**Respuesta:** 200 OK

#### POST `/api/auth/seller/login-by-phone`
Login para vendedores usando número de teléfono y código de afiliación.

**Query Parameters:**
- `phone`: Número de teléfono
- `affiliationCode`: Código de afiliación

**Respuesta:** 200 OK con token JWT

### 2. Gestión de Administradores (`/api/admin`)

#### GET `/api/admin/profile`
Obtiene el perfil del administrador.

**Query Parameters:**
- `userId`: ID del usuario

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### PUT `/api/admin/profile`
Actualiza el perfil del administrador.

**Query Parameters:**
- `userId`: ID del usuario

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "businessName": "string",
  "contactName": "string",
  "phone": "string",
  "email": "string"
}
```

**Respuesta:** 200 OK

### 3. Gestión de Vendedores (`/api/admin/sellers`)

#### GET `/api/admin/sellers/my-sellers`
Obtiene todos los vendedores afiliados al administrador actual.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `page`: Número de página (default: 1)
- `limit`: Elementos por página (default: 20)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/sellers`
Lista todos los vendedores con paginación.

**Query Parameters:**
- `page`: Número de página (default: 1)
- `limit`: Elementos por página (default: 20)
- `branchId`: ID de la sucursal
- `status`: Estado del vendedor (default: all)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### PUT `/api/admin/sellers/{sellerId}`
Actualiza la información de un vendedor.

**Path Parameters:**
- `sellerId`: ID del vendedor

**Query Parameters:**
- `adminId`: ID del administrador
- `name`: Nombre del vendedor
- `phone`: Teléfono del vendedor
- `isActive`: Estado activo

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### DELETE `/api/admin/sellers/{sellerId}`
Elimina o pausa un vendedor.

**Path Parameters:**
- `sellerId`: ID del vendedor

**Query Parameters:**
- `adminId`: ID del administrador
- `action`: Acción (pause/delete, default: pause)
- `reason`: Razón de la acción

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/sellers/limits`
Obtiene los límites actuales de vendedores basados en el plan de suscripción.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

### 4. Gestión de Sucursales (`/api/admin/branches`)

#### POST `/api/admin/branches`
Crea una nueva sucursal.

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "name": "string",
  "code": "string",
  "address": "string"
}
```

**Respuesta:** 201 Created

#### GET `/api/admin/branches`
Lista todas las sucursales del administrador.

**Query Parameters:**
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)
- `status`: Estado de la sucursal (default: all)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/branches/{branchId}`
Obtiene los detalles de una sucursal específica.

**Path Parameters:**
- `branchId`: ID de la sucursal

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### PUT `/api/admin/branches/{branchId}`
Actualiza una sucursal existente.

**Path Parameters:**
- `branchId`: ID de la sucursal

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "name": "string",
  "code": "string",
  "address": "string",
  "isActive": boolean
}
```

**Respuesta:** 200 OK

#### DELETE `/api/admin/branches/{branchId}`
Elimina una sucursal (soft delete).

**Path Parameters:**
- `branchId`: ID de la sucursal

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/branches/{branchId}/sellers`
Obtiene la lista de vendedores de una sucursal específica.

**Path Parameters:**
- `branchId`: ID de la sucursal

**Query Parameters:**
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

### 5. Gestión de Códigos QR (`/api`)

#### POST `/api/generate-affiliation-code-protected`
Genera un nuevo código de afiliación para vendedores (solo administradores).

**Query Parameters:**
- `adminId`: ID del administrador
- `expirationHours`: Horas de expiración
- `maxUses`: Máximo número de usos
- `branchId`: ID de la sucursal
- `notes`: Notas adicionales

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 201 Created

#### POST `/api/validate-affiliation-code`
Valida un código de afiliación (endpoint público para vendedores).

**Parámetros del Body:**
```json
{
  "affiliationCode": "string"
}
```

**Respuesta:** 200 OK

#### POST `/api/seller/register`
Registra un nuevo vendedor usando un código de afiliación.

**Parámetros del Body:**
```json
{
  "sellerName": "string",
  "phone": "string",
  "affiliationCode": "string"
}
```

**Respuesta:** 201 Created

#### POST `/api/generate-qr-base64`
Genera un código QR en formato Base64.

**Parámetros del Body:**
```json
{
  "affiliationCode": "string"
}
```

**Respuesta:** 200 OK

#### POST `/api/login-with-qr`
Login de vendedor usando código QR.

**Parámetros del Body:**
```json
{
  "qrData": "string",
  "phone": "string"
}
```

**Respuesta:** 200 OK

### 6. Gestión de Pagos (`/api/payments`)

#### GET `/api/payments/status/{sellerId}`
Verifica el estado de conexión de un vendedor.

**Path Parameters:**
- `sellerId`: ID del vendedor

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### POST `/api/payments/claim`
Permite al vendedor reclamar un pago.

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "paymentId": "string",
  "amount": number,
  "sellerId": number
}
```

**Respuesta:** 200 OK

#### POST `/api/payments/reject`
Permite al vendedor rechazar un pago.

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "paymentId": "string",
  "reason": "string",
  "sellerId": number
}
```

**Respuesta:** 200 OK

#### GET `/api/payments/pending`
Obtiene pagos pendientes basados en el rol del usuario.

**Query Parameters:**
- `sellerId`: ID del vendedor
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)
- `limit`: Límite de elementos (default: 20)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/payments/admin/management`
Obtiene todos los pagos para gestión administrativa.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)
- `status`: Estado del pago

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/payments/notification-stats`
Obtiene estadísticas de la cola de notificaciones.

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/payments/admin/connected-sellers`
Obtiene vendedores conectados para un administrador.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/payments/test/admin/management`
Endpoint de prueba para gestión administrativa de pagos.

**Query Parameters:**
- `adminId`: ID del administrador (default: 605)
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)
- `status`: Estado del pago

**Respuesta:** 200 OK

#### GET `/api/payments/admin/sellers-status`
Obtiene el estado de conexión de todos los vendedores para un administrador.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/payments/confirmed`
Obtiene pagos confirmados para un vendedor.

**Query Parameters:**
- `sellerId`: ID del vendedor
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

### 7. Gestión de Notificaciones (`/api/notifications`)

#### GET `/api/notifications`
Obtiene notificaciones del usuario con paginación.

**Query Parameters:**
- `userId`: ID del usuario
- `userRole`: Rol del usuario
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `page`: Número de página (default: 1)
- `limit`: Elementos por página (default: 20)
- `unreadOnly`: Solo no leídas

**Respuesta:** 200 OK

#### POST `/api/notifications/{notificationId}/read`
Marca una notificación como leída.

**Path Parameters:**
- `notificationId`: ID de la notificación

**Respuesta:** 200 OK

#### POST `/api/notifications/yape-notifications`
Procesa notificación encriptada de Yape para transacciones.

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "adminId": number,
  "encryptedData": "string",
  "transactionId": "string"
}
```

**Respuesta:** 200 OK

#### GET `/api/notifications/yape-audit`
Obtiene auditoría de notificaciones Yape para un administrador.

**Query Parameters:**
- `adminId`: ID del administrador
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

### 8. Gestión de Facturación (`/api/billing`)

#### GET `/api/billing`
Obtiene información de facturación según el tipo.

**Query Parameters:**
- `type`: Tipo de información (dashboard, subscription, payments, plans)
- `adminId`: ID del administrador
- `period`: Período (default: current)
- `include`: Incluir detalles (default: details)
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### POST `/api/billing/operations`
Ejecuta operaciones de facturación.

**Query Parameters:**
- `adminId`: ID del administrador
- `action`: Acción (generate-code, upload, subscribe, upgrade, cancel, check, simulate)
- `validate`: Validar (default: true)

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "imageBase64": "string",
  "paymentCode": "string",
  "amount": number
}
```

**Respuesta:** 200 OK

#### POST `/api/billing/payments/upload`
Sube imagen del comprobante de pago en formato base64.

**Query Parameters:**
- `adminId`: ID del administrador
- `paymentCode`: Código de pago

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "imageBase64": "string"
}
```

**Respuesta:** 200 OK

#### GET `/api/billing/payments/status/{paymentCode}`
Obtiene el estado de un pago por código.

**Path Parameters:**
- `paymentCode`: Código de pago

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/billing/plans`
Obtiene los planes de suscripción disponibles.

**Respuesta:** 200 OK

#### GET `/api/billing/dashboard`
Obtiene el dashboard de facturación del administrador.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### POST `/api/billing/load-data`
Carga los planes de suscripción y paquetes de tokens en la base de datos.

**Respuesta:** 200 OK

### 9. Gestión de Facturación Administrativa (`/api/admin/billing`)

#### GET `/api/admin/billing`
Obtiene información de administración de facturación según el tipo.

**Query Parameters:**
- `type`: Tipo (dashboard, payments, codes, stats)
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `status`: Estado (default: all)
- `include`: Incluir detalles (default: details)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/billing/payments/approved`
Obtiene pagos aprobados.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/billing/payments/rejected`
Obtiene pagos rechazados.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### PUT `/api/admin/billing/payments/{paymentId}/approve`
Aprueba un pago manualmente.

**Path Parameters:**
- `paymentId`: ID del pago

**Query Parameters:**
- `adminId`: ID del administrador
- `reviewNotes`: Notas de revisión

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### PUT `/api/admin/billing/payments/{paymentId}/reject`
Rechaza un pago manualmente.

**Path Parameters:**
- `paymentId`: ID del pago

**Query Parameters:**
- `adminId`: ID del administrador
- `reviewNotes`: Notas de revisión

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/billing/payments/{paymentId}/image`
Obtiene la imagen del comprobante de pago.

**Path Parameters:**
- `paymentId`: ID del pago

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/billing/payments/codes`
Obtiene todos los códigos de pago generados.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/admin/billing/dashboard`
Obtiene el dashboard de administración de pagos.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

### 10. Estadísticas y Analytics (`/api/stats`)

#### GET `/api/stats/admin/summary`
Obtiene estadísticas básicas de ventas para un administrador.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/seller/summary`
Obtiene estadísticas básicas de ventas para un vendedor.

**Query Parameters:**
- `sellerId`: ID del vendedor
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/admin/dashboard`
Obtiene un resumen rápido de estadísticas para el dashboard del admin.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/admin/analytics`
Obtiene analytics completos con ventas diarias, top vendedores, métricas avanzadas.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `include`: Incluir datos específicos
- `period`: Período de análisis
- `metric`: Métrica específica
- `granularity`: Granularidad de datos
- `confidence`: Nivel de confianza
- `days`: Número de días

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/seller/analytics`
Obtiene resumen completo de analytics para un vendedor específico.

**Query Parameters:**
- `sellerId`: ID del vendedor
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `include`: Incluir datos específicos
- `period`: Período de análisis
- `metric`: Métrica específica
- `granularity`: Granularidad de datos
- `confidence`: Nivel de confianza
- `days`: Número de días

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/admin/financial`
Obtiene análisis financiero detallado con transparencia completa.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `include`: Incluir datos específicos
- `currency`: Moneda
- `taxRate`: Tasa de impuestos

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/seller/financial`
Obtiene análisis financiero específico para vendedores con transparencia de comisiones.

**Query Parameters:**
- `sellerId`: ID del vendedor
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `include`: Incluir datos específicos
- `currency`: Moneda
- `commissionRate`: Tasa de comisión

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

#### GET `/api/stats/admin/payment-transparency`
Obtiene reporte de transparencia de pagos con detalles de comisiones, impuestos y fees.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)
- `includeFees`: Incluir fees
- `includeTaxes`: Incluir impuestos
- `includeCommissions`: Incluir comisiones

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

## Códigos de Estado HTTP

- **200 OK**: Solicitud exitosa
- **201 Created**: Recurso creado exitosamente
- **400 Bad Request**: Solicitud inválida
- **401 Unauthorized**: No autorizado
- **403 Forbidden**: Acceso denegado
- **404 Not Found**: Recurso no encontrado
- **500 Internal Server Error**: Error interno del servidor

## Formato de Respuesta

Todas las respuestas siguen el formato estándar:

```json
{
  "success": boolean,
  "message": "string",
  "data": object,
  "timestamp": "string"
}
```

## Manejo de Errores

Los errores siguen el formato:

```json
{
  "message": "string",
  "errorCode": "string",
  "details": object,
  "timestamp": "string"
}
```

## Filtros y Parámetros Comunes

### Filtros de Fecha
- Formato: `yyyy-MM-dd`
- Ejemplo: `2024-01-15`

### Paginación
- `page`: Número de página (empezando en 0 o 1 según el endpoint)
- `size` o `limit`: Elementos por página (default: 20)

### Estados Comunes
- `status`: Estado del recurso (all, active, inactive, pending, approved, rejected)

### Parámetros de Inclusión
- `include`: Qué datos incluir (basic, details, all)

## Notas Importantes

1. **Autenticación**: La mayoría de endpoints requieren autenticación JWT
2. **Fechas**: Todas las fechas deben estar en formato `yyyy-MM-dd`
3. **Paginación**: Los endpoints de lista soportan paginación
4. **CORS**: Configurado para `http://localhost:3000` y `http://localhost:8080`
5. **WebSocket**: El sistema incluye notificaciones en tiempo real
6. **QR Codes**: Sistema completo de códigos QR para afiliación de vendedores

## Ejemplos de Uso

### 1. Registro de Administrador
```bash
curl -X POST http://localhost:8080/api/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Mi Negocio",
    "email": "admin@negocio.com",
    "password": "password123",
    "phone": "+51987654321",
    "contactName": "Juan Pérez"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@negocio.com",
    "password": "password123"
  }'
```

### 3. Obtener Vendedores
```bash
curl -X GET "http://localhost:8080/api/admin/sellers/my-sellers?adminId=1&page=1&limit=10" \
  -H "Authorization: Bearer <token>"
```

### 4. Generar Código de Afiliación
```bash
curl -X POST "http://localhost:8080/api/generate-affiliation-code-protected?adminId=1&expirationHours=24&maxUses=10" \
  -H "Authorization: Bearer <token>"
```

### 5. Obtener Estadísticas
```bash
curl -X GET "http://localhost:8080/api/stats/admin/summary?adminId=1&startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer <token>"
```

## Script de Pruebas

Para probar todas las APIs, ejecuta el script `test_all_apis.sh` incluido en este directorio:

```bash
chmod +x test_all_apis.sh
./test_all_apis.sh
```

Este script probará todos los endpoints con diferentes parámetros y mostrará los resultados.

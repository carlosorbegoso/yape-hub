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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Administrador registrado exitosamente",
  "data": {
    "adminId": 1,
    "businessName": "Mi Negocio",
    "email": "admin@negocio.com",
    "contactName": "Juan Pérez",
    "phone": "+51987654321",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/auth/login`
Autentica un usuario y retorna un token de acceso.

**Parámetros del Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Login exitoso",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "userRole": "admin",
    "businessName": "Mi Negocio",
    "expiresIn": 3600
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/auth/refresh`
Genera un nuevo token de acceso usando el refresh token.

**Headers:**
- `X-Auth-Token`: Refresh token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Token renovado exitosamente",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/auth/logout`
Cierra la sesión del usuario actual.

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Logout exitoso",
  "data": {
    "loggedOut": true,
    "timestamp": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/auth/forgot-password`
Envía un email para restablecer la contraseña.

**Query Parameters:**
- `email`: Email del usuario

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Email de recuperación enviado",
  "data": {
    "emailSent": true,
    "email": "admin@negocio.com"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/auth/seller/login-by-phone`
Login para vendedores usando número de teléfono y código de afiliación.

**Query Parameters:**
- `phone`: Número de teléfono
- `affiliationCode`: Código de afiliación

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Login de vendedor exitoso",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "sellerId": 1,
    "sellerName": "Vendedor Test",
    "phone": "+51987654321",
    "adminId": 1,
    "businessName": "Mi Negocio",
    "expiresIn": 3600
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2. Gestión de Administradores (`/api/admin`)

#### GET `/api/admin/profile`
Obtiene el perfil del administrador.

**Query Parameters:**
- `userId`: ID del usuario

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Perfil obtenido exitosamente",
  "data": {
    "adminId": 1,
    "businessName": "Mi Negocio",
    "email": "admin@negocio.com",
    "contactName": "Juan Pérez",
    "phone": "+51987654321",
    "createdAt": "2024-01-15T10:30:00Z",
    "lastLogin": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Perfil actualizado exitosamente",
  "data": {
    "adminId": 1,
    "businessName": "Negocio Actualizado",
    "contactName": "Juan Pérez Actualizado",
    "phone": "+51987654322",
    "email": "admin@negocio.com",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 3. Gestión de Sucursales (`/api/admin/branches`)

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Sucursal creada exitosamente",
  "data": {
    "branchId": 1,
    "name": "Sucursal Principal",
    "code": "SUC001",
    "address": "Av. Principal 123, Lima",
    "adminId": 1,
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### GET `/api/admin/branches`
Lista todas las sucursales del administrador.

**Query Parameters:**
- `page`: Número de página (default: 0)
- `size`: Elementos por página (default: 20)
- `status`: Estado de la sucursal (default: all)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Sucursales obtenidas exitosamente",
  "data": {
    "branches": [
      {
        "branchId": 1,
        "name": "Sucursal Principal",
        "code": "SUC001",
        "address": "Av. Principal 123, Lima",
        "isActive": true,
        "sellerCount": 5,
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### GET `/api/admin/branches/{branchId}`
Obtiene los detalles de una sucursal específica.

**Path Parameters:**
- `branchId`: ID de la sucursal

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Detalles de sucursal obtenidos exitosamente",
  "data": {
    "branchId": 1,
    "name": "Sucursal Principal",
    "code": "SUC001",
    "address": "Av. Principal 123, Lima",
    "adminId": 1,
    "isActive": true,
    "sellers": [
      {
        "sellerId": 1,
        "name": "Vendedor Test",
        "phone": "+51987654321",
        "isActive": true
      }
    ],
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 4. Gestión de Vendedores (`/api/admin/sellers`)

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Vendedores obtenidos exitosamente",
  "data": {
    "sellers": [
      {
        "sellerId": 1,
        "name": "Vendedor Test",
        "phone": "+51987654321",
        "affiliationCode": "AFF123",
        "branchId": 1,
        "branchName": "Sucursal Principal",
        "isActive": true,
        "totalSales": 1500.50,
        "lastPayment": "2024-01-15T10:30:00Z",
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "totalElements": 1,
      "totalPages": 1
    },
    "summary": {
      "totalSellers": 1,
      "activeSellers": 1,
      "totalSales": 1500.50
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### GET `/api/admin/sellers/limits`
Obtiene los límites actuales de vendedores basados en el plan de suscripción.

**Query Parameters:**
- `adminId`: ID del administrador

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Límites de vendedores obtenidos exitosamente",
  "data": {
    "adminId": 1,
    "planName": "Plan Básico",
    "maxSellers": 10,
    "currentSellers": 1,
    "remainingSlots": 9,
    "isActive": true,
    "canAddMore": true,
    "expiresAt": "2024-12-31T23:59:59Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Código de afiliación generado exitosamente",
  "data": {
    "affiliationCode": "AFF123456",
    "adminId": 1,
    "branchId": 1,
    "expiresAt": "2024-01-16T10:30:00Z",
    "maxUses": 10,
    "remainingUses": 10,
    "notes": "Código para nuevos vendedores",
    "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/validate-affiliation-code`
Valida un código de afiliación (endpoint público para vendedores).

**Parámetros del Body:**
```json
{
  "affiliationCode": "string"
}
```

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Código de afiliación válido",
  "data": {
    "affiliationCode": "AFF123456",
    "isValid": true,
    "adminId": 1,
    "businessName": "Mi Negocio",
    "branchId": 1,
    "branchName": "Sucursal Principal",
    "expiresAt": "2024-01-16T10:30:00Z",
    "remainingUses": 9,
    "notes": "Código para nuevos vendedores"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Vendedor registrado exitosamente",
  "data": {
    "sellerId": 1,
    "sellerName": "Vendedor Test",
    "phone": "+51987654321",
    "affiliationCode": "AFF123456",
    "adminId": 1,
    "businessName": "Mi Negocio",
    "branchId": 1,
    "branchName": "Sucursal Principal",
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 6. Gestión de Pagos (`/api/payments`)

#### GET `/api/payments/status/{sellerId}`
Verifica el estado de conexión de un vendedor.

**Path Parameters:**
- `sellerId`: ID del vendedor

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Estado de conexión obtenido exitosamente",
  "data": {
    "sellerId": 1,
    "sellerName": "Vendedor Test",
    "isConnected": true,
    "lastSeen": "2024-01-15T10:30:00Z",
    "connectionStatus": "online",
    "websocketId": "ws_123456"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### POST `/api/payments/claim`
Permite al vendedor reclamar un pago.

**Headers:**
- `Authorization`: Bearer token

**Parámetros del Body:**
```json
{
  "paymentId": "string",
  "amount": 100.50,
  "sellerId": 1
}
```

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Pago reclamado exitosamente",
  "data": {
    "paymentId": "PAY123",
    "sellerId": 1,
    "amount": 100.50,
    "status": "claimed",
    "claimedAt": "2024-01-15T10:30:00Z",
    "transactionId": "TXN123456"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Pagos pendientes obtenidos exitosamente",
  "data": {
    "payments": [
      {
        "paymentId": "PAY123",
        "amount": 100.50,
        "sellerId": 1,
        "sellerName": "Vendedor Test",
        "status": "pending",
        "createdAt": "2024-01-15T10:30:00Z",
        "description": "Pago por productos"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    },
    "summary": {
      "totalPending": 1,
      "totalAmount": 100.50
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Notificaciones obtenidas exitosamente",
  "data": {
    "notifications": [
      {
        "notificationId": 1,
        "title": "Nuevo pago recibido",
        "message": "Has recibido un pago de S/ 100.50",
        "type": "payment",
        "isRead": false,
        "createdAt": "2024-01-15T10:30:00Z",
        "data": {
          "paymentId": "PAY123",
          "amount": 100.50
        }
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "totalElements": 1,
      "totalPages": 1
    },
    "unreadCount": 1
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta (type=dashboard):**
```json
{
  "success": true,
  "message": "Dashboard de facturación obtenido exitosamente",
  "data": {
    "adminId": 1,
    "planName": "Plan Básico",
    "subscriptionStatus": "active",
    "expiresAt": "2024-12-31T23:59:59Z",
    "currentPeriod": {
      "startDate": "2024-01-01",
      "endDate": "2024-01-31",
      "totalSales": 5000.00,
      "totalCommissions": 250.00,
      "netAmount": 4750.00
    },
    "paymentHistory": [
      {
        "paymentId": 1,
        "amount": 100.00,
        "status": "approved",
        "paidAt": "2024-01-15T10:30:00Z"
      }
    ],
    "limits": {
      "maxSellers": 10,
      "currentSellers": 1,
      "remainingSlots": 9
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### GET `/api/billing/plans`
Obtiene los planes de suscripción disponibles.

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Planes obtenidos exitosamente",
  "data": {
    "plans": [
      {
        "planId": 1,
        "name": "Plan Básico",
        "description": "Plan básico para pequeñas empresas",
        "price": 50.00,
        "currency": "PEN",
        "maxSellers": 10,
        "features": [
          "Hasta 10 vendedores",
          "Soporte básico",
          "Reportes básicos"
        ],
        "isActive": true
      },
      {
        "planId": 2,
        "name": "Plan Premium",
        "description": "Plan premium para empresas medianas",
        "price": 150.00,
        "currency": "PEN",
        "maxSellers": 50,
        "features": [
          "Hasta 50 vendedores",
          "Soporte prioritario",
          "Reportes avanzados",
          "Analytics"
        ],
        "isActive": true
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 9. Estadísticas y Analytics (`/api/stats`)

#### GET `/api/stats/admin/summary`
Obtiene estadísticas básicas de ventas para un administrador.

**Query Parameters:**
- `adminId`: ID del administrador
- `startDate`: Fecha inicio (yyyy-MM-dd)
- `endDate`: Fecha fin (yyyy-MM-dd)

**Headers:**
- `Authorization`: Bearer token

**Respuesta:** 200 OK

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Estadísticas básicas de admin obtenidas exitosamente",
  "data": {
    "adminId": 1,
    "period": {
      "startDate": "2024-01-01",
      "endDate": "2024-01-31"
    },
    "summary": {
      "totalSales": 5000.00,
      "totalTransactions": 25,
      "averageTransaction": 200.00,
      "totalSellers": 5,
      "activeSellers": 4
    },
    "dailyStats": [
      {
        "date": "2024-01-15",
        "sales": 500.00,
        "transactions": 3
      }
    ],
    "topSellers": [
      {
        "sellerId": 1,
        "sellerName": "Vendedor Test",
        "totalSales": 1500.00,
        "transactions": 8
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de respuesta:**
```json
{
  "success": true,
  "message": "Analytics completos obtenidos exitosamente",
  "data": {
    "adminId": 1,
    "period": {
      "startDate": "2024-01-01",
      "endDate": "2024-01-31"
    },
    "metrics": {
      "totalSales": 5000.00,
      "totalTransactions": 25,
      "averageTransaction": 200.00,
      "growthRate": 15.5,
      "conversionRate": 85.2
    },
    "dailyAnalytics": [
      {
        "date": "2024-01-15",
        "sales": 500.00,
        "transactions": 3,
        "sellers": 2,
        "growth": 12.5
      }
    ],
    "topPerformers": [
      {
        "sellerId": 1,
        "sellerName": "Vendedor Test",
        "sales": 1500.00,
        "transactions": 8,
        "performance": 95.5
      }
    ],
    "insights": [
      {
        "type": "trend",
        "message": "Las ventas han aumentado 15.5% este mes",
        "confidence": 0.95
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

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

**Ejemplo de error:**
```json
{
  "message": "Credenciales inválidas",
  "errorCode": "INVALID_CREDENTIALS",
  "details": {
    "field": "email",
    "reason": "Usuario no encontrado"
  },
  "timestamp": "2024-01-15T10:30:00Z"
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

## URLs Importantes

- **Servidor**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/openapi`


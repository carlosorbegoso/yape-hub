# 📋 Endpoints por Rol - Yape Hub API

## 🔐 **Autenticación**

### **Sin Autenticación (PermitAll)**
- `POST /api/auth/admin/register` - Registro de administrador
- `POST /api/auth/login` - Login general (admin/seller)
- `POST /api/auth/refresh` - Renovar token
- `POST /api/auth/seller/login-by-phone` - Login de vendedor con código de afiliación
- `POST /api/auth/forgot-password` - Recuperar contraseña
- `POST /api/seller/register` - Registro de vendedor con código de afiliación
- `POST /api/validate-affiliation-code` - Validar código de afiliación
- `POST /api/generate-qr-base64` - Generar QR con Base64 para código de afiliación
- `POST /api/login-with-qr` - Login de vendedor usando QR con Base64

---

## 👨‍💼 **ENDPOINTS PARA ADMINISTRADORES**

### **🔑 Autenticación Requerida: Token de ADMIN**

#### **Gestión de Vendedores**
- `GET /api/admin/sellers/my-sellers?adminId={id}` - Listar mis vendedores
- `GET /api/admin/sellers?adminId={id}` - Listar vendedores con filtros
- `PUT /api/admin/sellers/{sellerId}?adminId={id}` - Actualizar vendedor
- `DELETE /api/admin/sellers/{sellerId}?adminId={id}` - Eliminar vendedor

#### **Gestión de Sucursales**
- `POST /api/admin/branches?adminId={id}` - Crear sucursal
- `GET /api/admin/branches?adminId={id}` - Listar sucursales
- `GET /api/admin/branches/{branchId}?adminId={id}` - Obtener sucursal
- `PUT /api/admin/branches/{branchId}?adminId={id}` - Actualizar sucursal
- `DELETE /api/admin/branches/{branchId}?adminId={id}` - Eliminar sucursal
- `GET /api/admin/branches/{branchId}/sellers?adminId={id}` - Vendedores de sucursal

#### **Códigos de Afiliación**
- `POST /api/generate-affiliation-code-protected?adminId={id}` - Generar código de afiliación

#### **Estadísticas y Analytics**
- `GET /api/stats/admin/summary?adminId={id}&startDate={date}&endDate={date}` - Estadísticas básicas de admin
- `GET /api/stats/admin/analytics?adminId={id}&startDate={date}&endDate={date}` - Analytics completos con insights avanzados
- `GET /api/stats/admin/dashboard?adminId={id}` - Resumen rápido para dashboard (últimos 7 días)

#### **Gestión de Pagos**
- `GET /api/payments/admin/management?adminId={id}` - Gestión de pagos
- `GET /api/payments/admin/connected-sellers?adminId={id}` - Vendedores conectados
- `GET /api/payments/admin/sellers-status?adminId={id}` - Estado de vendedores
- `GET /api/payments/notification-stats?adminId={id}` - Estadísticas de notificaciones
- `GET /api/payments/pending?sellerId={id}&adminId={adminId}&page={page}&size={size}` - Pagos pendientes de vendedor específico (para admins, también acepta `limit`)
- `GET /api/payments/confirmed?sellerId={id}&adminId={adminId}&page={page}&size={size}` - Pagos confirmados de vendedor específico (para admins)

#### **Notificaciones de Pago**
- `POST /api/notifications/yape-notifications` - Procesar notificación de pago de Yape (requiere autenticación de admin)


#### **Perfil**
- `GET /api/admin/profile?userId={id}` - Obtener perfil
- `PUT /api/admin/profile?userId={id}` - Actualizar perfil

---

## 👨‍💻 **ENDPOINTS PARA VENDEDORES**

### **🔑 Autenticación Requerida: Token de SELLER**

#### **Gestión de Pagos**
- `GET /api/payments/pending?sellerId={id}&page={page}&size={size}` - Pagos pendientes (también acepta `limit` en lugar de `size`)
- `GET /api/payments/confirmed?sellerId={id}&page={page}&size={size}` - Pagos confirmados por el vendedor
- `POST /api/payments/claim` - Reclamar pago
- `POST /api/payments/reject` - Rechazar pago
- `GET /api/payments/status/{sellerId}` - Estado de conexión

#### **Estadísticas y Analytics**
- `GET /api/stats/seller/summary?sellerId={id}&startDate={date}&endDate={date}` - Estadísticas básicas del vendedor
- `GET /api/stats/seller/analytics?sellerId={id}&startDate={date}&endDate={date}` - Analytics completos del vendedor

#### **Notificaciones**
- `GET /api/notifications` - Listar notificaciones (requiere autenticación)
- `POST /api/notifications/{notificationId}/read` - Marcar como leída (requiere autenticación)

---

## 🌐 **ENDPOINTS PÚBLICOS**

### **Sin Autenticación**

#### **Validación**
- `POST /api/validate-affiliation-code` - Validar código de afiliación

#### **QR y Códigos de Afiliación**
- `POST /api/generate-qr-base64` - Generar imagen QR en Base64 para código de afiliación
- `POST /api/login-with-qr` - Login de vendedor usando QR con Base64

---

## 🔌 **WEBSOCKET**

### **Autenticación Requerida: Token de SELLER**
- `ws://localhost:8080/ws/payments/{sellerId}?token={jwt_token}` - Conexión WebSocket para notificaciones en tiempo real

---

## 📊 **Ejemplos de Uso con CURL**

### **🔐 Autenticación**

#### **Registro de Administrador**
```bash
curl --location --request POST 'http://localhost:8080/api/auth/admin/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "businessName": "Mi Empresa",
    "email": "admin@empresa.com",
    "password": "password123",
    "phone": "987654321",
    "address": "Av. Principal 123",
    "contactName": "Juan Admin"
}'
```

#### **Login de Administrador**
```bash
# Nota: Usar credenciales reales del sistema
curl --location --request POST 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data-raw '{
    "email": "admin@example.com",
    "password": "password123",
    "role": "ADMIN",
    "deviceFingerprint": "admin-device-123"
}'

# O usar directamente el token de admin ya generado:
# Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc
```

#### **Login de Vendedor con Código de Afiliación**
```bash
curl --location --request POST 'http://localhost:8080/api/auth/seller/login-by-phone?phone=987654321&affiliationCode=AFF646424' \
--header 'accept: application/json'
```

#### **Renovar Token**
```bash
curl --location --request POST 'http://localhost:8080/api/auth/refresh' \
--header 'X-Auth-Token: {refresh_token}'
```

### **🌐 Endpoints Públicos**

#### **Validar Código de Afiliación**
```bash
curl --location --request POST 'http://localhost:8080/api/validate-affiliation-code' \
--header 'Content-Type: application/json' \
--data-raw '{
    "affiliationCode": "AFF646424"
}'
```

#### **Registro de Vendedor**
```bash
curl --location --request POST 'http://localhost:8080/api/seller/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "sellerName": "Carlos Vendedor",
    "phone": "987654321",
    "affiliationCode": "AFF646424"
}'
```

#### **Generar Imagen QR en Base64**
```bash
curl --location --request POST 'http://localhost:8080/api/generate-qr-base64' \
--header 'Content-Type: application/json' \
--data-raw '{
    "affiliationCode": "AFF646424"
}'

# Respuesta incluye imagen QR real en Base64:
# {
#   "affiliationCode": "AFF646424",
#   "qrBase64": "iVBORw0KGgoAAAANSUhEUgAAASwAAAEsAQAAAABRBrPYAAACnElEQVR4Xu2YQY7dIBBE2ysfwzcFfFMfIUuvIPUKf2XGGilZxi0jD8Ldjy9RXYA1Mf6l/Yp75Mf2Yrf2Yrf2Yrf232NHqK1jP+lbxDJGX8c4t04iaiKsEj9CT2z7YLCIWQ07mwYrEYUg2SZNVqUU3ObcdNhBxSk0gG1wlJyYTL6NT8W7X3s6TANFSrB/JcjCBscD/ae98Ggs4ir0/SERibCrNeKUngHPn1QW7NCqtfaCsX0TRSxsZ3yO8/NgkNVmHhdDqsnnqvv5tfSPx3afXUROSk+EWwkDzClpsO4923H4IbLhhDm+xMmCUXefyTASpNvtsfoitj5pMFZtoMnhfiVLjyY1ESY1euiM4uwa2HuanFlEcmH7lGXY2JhhXGp8F+ThmA4udnT51H3Qkx1sbb3mwZp37n5KFpVbHiClieGJNQ92hPyMmQ97gFlhHfZp+zwYwHJqR2v5H+bcCPJkwo6CIPR93kfiT19GjLdEGEdxxcwwjZOZ3tP9iZUH4zWcsqvFD+nQp0TfdHs6xngfV93HZWxuIk9hVhaMcUgNe0DGHqf2NVn/QmTClvkIsDht6hO0GUyD6bXob+WLAgMMf1qsjrOv82CMDcxgs9u7i75/MfnzMVoPxy9B5ITL8IqPPBjxesFKAfPi6s9UGkwV79w7WNq1ZmvvvqSmGdJgrPrkvOqO79ZEEy1IKkwLDx3LfChes0KGdx9fTP58bDZ9H8rV2sUoo/dmhxdrkgXzwim6BvI2kQUnhCKLfyINVgfb2Vmqb5NzEVef1Zmw4rWHz65qk5tHqML0fNis+GZYl5F7y5UOIyUFdguy3P8JkwGrmFx+5koaXLh8XXTPJZIIi8DSglV3iVB9alkfDu1E2N/bi93ai93ai91aBuw3/PcUxdrhnm0AAAAASUVORK5CYII=",
#   "expiresAt": "2025-09-17T00:52:10.262368",
#   "maxUses": 1,
#   "remainingUses": 1,
#   "branchName": "Sucursal Norte Actualizada",
#   "adminName": "Mi Empresa Actualizada"
# }
```

#### **Login con QR**
```bash
curl --location --request POST 'http://localhost:8080/api/login-with-qr' \
--header 'Content-Type: application/json' \
--data-raw '{
    "qrData": "eyJhZmZpbGlhdGlvbkNvZGUiOiJBRkY2NDY0MjQiLCJicmFuY2hJZCI6NjA1LCJhZG1pbklkIjo2MDUsImV4cGlyZXNBdCI6IjIwMjUtMDktMTZUMjI6MzY6MjEuMDU3ODQxIiwibWF4VXNlcyI6MX0=",
    "phone": "987654321"
}'
```

### **👨‍💼 Administradores**

#### **Gestión de Perfil**
```bash
# Obtener perfil de admin
curl --location 'http://localhost:8080/api/admin/profile?userId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Actualizar perfil de admin
curl --location --request PUT 'http://localhost:8080/api/admin/profile?userId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'Content-Type: application/json' \
--data-raw '{
    "businessName": "Mi Empresa Actualizada",
    "phone": "987654321",
    "address": "Av. Principal 123",
    "contactName": "Juan Pérez"
}'
```

#### **Gestión de Códigos de Afiliación**
```bash
# Generar código de afiliación
curl --location --request POST 'http://localhost:8080/api/generate-affiliation-code-protected?adminId=605&expirationHours=24&maxUses=10&branchId=605&notes=Código para nuevos vendedores' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
```

#### **Gestión de Vendedores**
```bash
# Listar mis vendedores
curl --location 'http://localhost:8080/api/admin/sellers/my-sellers?adminId=605&page=0&size=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Actualizar vendedor
curl --location --request PUT 'http://localhost:8080/api/admin/sellers/251?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'Content-Type: application/json' \
--data-raw '{
    "sellerName": "Carlos Vendedor Actualizado",
    "email": "carlos.actualizado@vendedor.com",
    "phone": "987654321"
}'
```

#### **Gestión de Sucursales**
```bash
# Crear sucursal
curl --location --request POST 'http://localhost:8080/api/admin/branches?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'Content-Type: application/json' \
--data-raw '{
    "name": "Sucursal Centro",
    "address": "Av. Centro 456",
    "code": "CENTRO_001"
}'

# Listar sucursales
curl --location 'http://localhost:8080/api/admin/branches?adminId=605&page=0&size=20&status=all' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Obtener sucursal específica
curl --location 'http://localhost:8080/api/admin/branches/605?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Vendedores de una sucursal
curl --location 'http://localhost:8080/api/admin/branches/605/sellers?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
```

#### **Estadísticas y Analytics**
```bash
# Estadísticas básicas de admin
curl --location 'http://localhost:8080/api/stats/admin/summary?adminId=605&startDate=2025-09-01&endDate=2025-09-16' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Analytics completos con insights avanzados
curl --location 'http://localhost:8080/api/stats/admin/analytics?adminId=605&startDate=2025-09-01&endDate=2025-09-16' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Resumen rápido para dashboard (últimos 7 días)
curl --location 'http://localhost:8080/api/stats/admin/dashboard?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
```

#### **Gestión de Pagos**
```bash
# Gestión de pagos de admin
curl --location 'http://localhost:8080/api/payments/admin/management?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Vendedores conectados
curl --location 'http://localhost:8080/api/payments/admin/connected-sellers?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Estado de vendedores
curl --location 'http://localhost:8080/api/payments/admin/sellers-status?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Estadísticas de notificaciones
curl --location 'http://localhost:8080/api/payments/notification-stats?adminId=605' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Ver pagos pendientes de un vendedor específico (para admins)
curl --location 'http://localhost:8080/api/payments/pending?sellerId=251&adminId=605&page=0&size=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Ver pagos confirmados de un vendedor específico (para admins)
curl --location 'http://localhost:8080/api/payments/confirmed?sellerId=251&adminId=605&page=0&size=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# ❌ ERROR: Intentar acceder a vendedor que no pertenece al admin
curl --location 'http://localhost:8080/api/payments/pending?sellerId=999&adminId=605&page=0&size=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
# Respuesta: {"message":"No autorizado para acceder a este vendedor","code":"SECURITY_ERROR"}
```

#### **Notificaciones de Pago**
```bash
# Procesar notificación de pago de Yape
curl --location --request POST 'http://localhost:8080/api/notifications/yape-notifications' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--data-raw '{
    "adminId": 605,
    "encryptedNotification": "KVAREhFWB10HXAJZF2sWQVIBTAJTEwBRRWEDRUMYeg4TQwdVClcLGkV2wpVSXl9WW0EAVANbBlU=",
    "deviceFingerprint": "a1b2c3d4e5f6789a",
    "timestamp": 1758073257000
}'
```

### **👨‍💻 Vendedores**

#### **Gestión de Pagos**
```bash
# Obtener pagos pendientes (usando size)
curl --location 'http://localhost:8080/api/payments/pending?sellerId=251&page=0&size=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Obtener pagos pendientes (usando limit como alternativa)
curl --location 'http://localhost:8080/api/payments/pending?sellerId=251&page=0&limit=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Reclamar pago
curl --location --request POST 'http://localhost:8080/api/payments/claim' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'Content-Type: application/json' \
--data-raw '{
    "paymentId": 2001,
    "sellerId": 251
}'

# Rechazar pago
curl --location --request POST 'http://localhost:8080/api/payments/reject' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'Content-Type: application/json' \
--data-raw '{
    "paymentId": 2001,
    "sellerId": 251,
    "reason": "Pago no corresponde a mi cliente"
}'

# Estado de conexión
curl --location 'http://localhost:8080/api/payments/status/251' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Ver pagos confirmados por el vendedor
curl --location 'http://localhost:8080/api/payments/confirmed?sellerId=251&page=0&size=20' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
```

#### **Estadísticas y Analytics**
```bash
# Estadísticas básicas del vendedor
curl --location 'http://localhost:8080/api/stats/seller/summary?sellerId=251&startDate=2025-09-01&endDate=2025-09-16' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Analytics completos del vendedor
curl --location 'http://localhost:8080/api/stats/seller/analytics?sellerId=251&startDate=2025-09-01&endDate=2025-09-16' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
```

#### **Notificaciones**
```bash
# Listar notificaciones
curl --location 'http://localhost:8080/api/notifications' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'

# Marcar notificación como leída
curl --location --request POST 'http://localhost:8080/api/notifications/123/read' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'accept: application/json'
```

### **🔌 WebSocket**

#### **Conexión WebSocket para Notificaciones**
```bash
# Conectar a WebSocket con token de vendedor
wscat -c "ws://localhost:8080/ws/payments/251?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc"

# O usando curl para probar la conexión
curl --location --request GET 'http://localhost:8080/ws/payments/251?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc' \
--header 'Upgrade: websocket' \
--header 'Connection: Upgrade'
```


---

## 🔑 **Tokens de Ejemplo**

### **Token de Admin (ID: 605)**
```bash
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj02MDUsIGlzcz1odHRwOi8vbG9jYWxob3N0OjgwODAsIGdyb3Vwcz1BRE1JTiwgZXhwPTE3NTc3MzcyOTEsIGlhdD0xNzU3NzMzNjkxfQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc
```

### **Token de Seller (ID: 251)**
```bash
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj05MDEsIHNlbGxlcklkPTI1MSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzUxMTUsIGlhdD0xNzU4MDcxNTE1fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc
```

### **Token de Seller (ID: 101) - Generado con QR**
```bash
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e3N1Yj03NTEsIHNlbGxlcklkPTEwMSwgaXNzPWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCwgZ3JvdXBzPVNFTExFUiwgZXhwPTE3NTgwNzg1ODksIGlhdD0xNzU4MDc0OTg5fQ.eWFwZWNoYW1vLXNlY3JldC1rZXktMjAyNC12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmc
```

### **Códigos de Afiliación de Ejemplo**
- `AFF646424` - Código válido para pruebas (generado recientemente)
- `AFF654598` - Código válido para pruebas (generado anteriormente)
- `AFF940450` - Código agotado (para pruebas de error)

### **QR Base64 de Ejemplo**
```bash
eyJhZmZpbGlhdGlvbkNvZGUiOiJBRkY2NDY0MjQiLCJicmFuY2hJZCI6NjA1LCJhZG1pbklkIjo2MDUsImV4cGlyZXNBdCI6IjIwMjUtMDktMTZUMjI6MzY6MjEuMDU3ODQxIiwibWF4VXNlcyI6MX0=
```

---

## ✅ **Estado de las APIs**

### **APIs Verificadas y Funcionando:**
- ✅ `POST /api/auth/admin/register` - Registro de administrador
- ✅ `POST /api/auth/login` - Login general (admin/seller)
- ✅ `POST /api/auth/seller/login-by-phone` - Login de vendedor con código de afiliación
- ✅ `POST /api/validate-affiliation-code` - Validar código de afiliación
- ✅ `POST /api/generate-qr-base64` - Generar QR con Base64
- ✅ `POST /api/login-with-qr` - Login de vendedor usando QR
- ✅ `POST /api/seller/register` - Registro de vendedor
- ✅ `GET /api/admin/profile?userId={id}` - Obtener perfil de admin
- ✅ `PUT /api/admin/profile?userId={id}` - Actualizar perfil de admin
- ✅ `POST /api/generate-affiliation-code-protected` - Generar código de afiliación
- ✅ `GET /api/admin/sellers/my-sellers?adminId={id}` - Listar vendedores
- ✅ `GET /api/admin/branches?adminId={id}` - Gestión de sucursales
- ✅ `GET /api/stats/admin?adminId={id}` - Estadísticas de admin
- ✅ `GET /api/stats/seller?sellerId={id}` - Estadísticas de vendedor
- ✅ `GET /api/stats/analytics?adminId={id}` - Analytics de admin
- ✅ `GET /api/stats/seller/analytics?sellerId={id}` - Analytics de vendedor
- ✅ `GET /api/payments/pending?sellerId={id}` - Pagos pendientes
- ✅ `POST /api/payments/claim` - Reclamar pago
- ✅ `POST /api/payments/reject` - Rechazar pago
- ✅ `POST /api/notifications/yape-notifications` - Notificación de pago de Yape
- ✅ `GET /api/notifications` - Listar notificaciones
- ✅ `ws://localhost:8080/ws/payments/{sellerId}?token={jwt_token}` - WebSocket

### **APIs con Problemas:**
- ✅ Todas las APIs principales funcionan correctamente

### **APIs Pendientes de Verificación:**
- 🔄 `POST /api/auth/refresh` - Renovar token
- 🔄 `POST /api/auth/forgot-password` - Recuperar contraseña

---

## ⚠️ **Notas Importantes**

1. **Tokens JWT**: Los tokens de SELLER incluyen tanto `userId` como `sellerId`
2. **Autorización**: Cada endpoint valida que el token corresponda al rol correcto
3. **Parámetros**: Los endpoints requieren el ID correspondiente (`adminId` o `sellerId`)
4. **WebSocket**: Requiere token JWT válido en el query parameter `token`
5. **Seguridad**: Todas las APIs requieren autenticación (excepto validación de códigos y QR)
6. **Notificaciones**: `/api/notifications/yape-notifications` para procesar pagos de Yape
7. **Códigos de Afiliación**: Solo los admins pueden generar códigos de afiliación
8. **QR con Base64**: Los QR se generan como imágenes PNG reales codificadas en Base64, listas para mostrar en el frontend
9. **Login con QR**: Permite a los vendedores hacer login usando solo el QR y su teléfono
10. **Validación de QR**: El sistema valida automáticamente el código de afiliación contenido en el QR
11. **Acceso de Admins a Vendedores**: Los admins pueden ver pagos de sus vendedores usando `adminId` + `sellerId` en `/api/payments/pending`
12. **Validación de Propiedad**: El sistema valida que el vendedor pertenezca al admin antes de permitir acceso
13. **Seguridad Estricta**: Si un admin intenta acceder a un vendedor que no le pertenece, recibirá error 401 "No autorizado para acceder a este vendedor"

---

## 🚀 **URLs Seguras**

- **Admin**: `http://localhost:8080/api/admin/*`
- **Seller**: `http://localhost:8080/api/payments/*`, `http://localhost:8080/api/stats/seller/*`
- **WebSocket**: `ws://localhost:8080/ws/payments/{sellerId}?token={jwt_token}`

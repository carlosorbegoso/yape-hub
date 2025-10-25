# 🔒 Sistema de Manejo de Errores de Autenticación

Este sistema proporciona respuestas JSON estructuradas para todos los errores de autenticación y autorización usando el DTO `ErrorResponse`.

## 📋 Exception Mappers Implementados

### 1. `UnauthorizedExceptionMapper`
- **Maneja**: `NotAuthorizedException` (JAX-RS)
- **Código HTTP**: 401 Unauthorized
- **Uso**: Cuando no se proporciona token JWT

### 2. `SecurityExceptionMapper`
- **Maneja**: `SecurityException` (Java)
- **Código HTTP**: 401 (no autenticado) o 403 (no autorizado)
- **Uso**: Errores de seguridad personalizados

### 3. `JwtAuthenticationExceptionMapper`
- **Maneja**: `RuntimeException` con mensajes relacionados con JWT
- **Código HTTP**: 401 Unauthorized
- **Uso**: Tokens JWT inválidos, expirados o malformados

### 4. `AuthenticationInterceptor` (DESHABILITADO)
- **Estado**: Comentado/Deshabilitado
- **Razón**: Causaba conflictos con endpoints públicos como `/api/auth/login`
- **Alternativa**: Quarkus maneja automáticamente la autenticación JWT

## 🎯 Ejemplos de Respuestas JSON

### Error 401 - Token Faltante
```json
{
  "message": "Authentication required. Please provide a valid JWT token.",
  "code": "UNAUTHORIZED",
  "details": {
    "error_type": "missing_authentication",
    "hint": "Include 'Authorization: Bearer <your-jwt-token>' header",
    "endpoint": "api/payments/claim",
    "method": "POST"
  },
  "timestamp": "2024-10-24T20:45:30.123Z"
}
```

### Error 401 - Token Expirado
```json
{
  "message": "JWT authentication failed: Token expired",
  "code": "UNAUTHORIZED",
  "details": {
    "error_type": "jwt_expired",
    "hint": "Your JWT token has expired. Please login again to get a new token",
    "token_status": "invalid"
  },
  "timestamp": "2024-10-24T20:45:30.123Z"
}
```

### Error 403 - Sin Permisos
```json
{
  "message": "Access forbidden. Insufficient permissions",
  "code": "FORBIDDEN",
  "details": {
    "error_type": "insufficient_permissions",
    "hint": "You don't have permission to access this resource"
  },
  "timestamp": "2024-10-24T20:45:30.123Z"
}
```

## 🔧 Uso en Controladores

### Opción 1: Automático (Recomendado)
Los Exception Mappers manejan automáticamente los errores:

```java
@GET
@Path("/protected")
public Uni<Response> protectedEndpoint(@HeaderParam("Authorization") String auth) {
    return securityService.validateJwtToken(auth)
        .chain(userId -> businessLogic(userId))
        .map(result -> Response.ok(result).build());
    // Los errores se manejan automáticamente por los Exception Mappers
}
```

### Opción 2: Manual con ControllerErrorHandler
```java
@GET
@Path("/protected")
public Uni<Response> protectedEndpoint(@HeaderParam("Authorization") String auth) {
    return securityService.validateJwtToken(auth)
        .chain(userId -> businessLogic(userId))
        .map(result -> Response.ok(result).build())
        .onFailure().recoverWithItem(ControllerErrorHandler::handleControllerError);
}
```

### Opción 3: Crear Respuesta Manual
```java
@GET
@Path("/protected")
public Uni<Response> protectedEndpoint(@HeaderParam("Authorization") String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) {
        return Uni.createFrom().item(
            ControllerErrorHandler.createUnauthorizedResponse("Missing JWT token")
        );
    }
    
    return businessLogic()
        .map(result -> Response.ok(result).build());
}
```

## 🚀 Configuración Automática

Los Exception Mappers están marcados con `@Provider` y se registran automáticamente en Quarkus. No necesitas configuración adicional.

## 🔍 Testing

### Endpoints de Prueba
Usa endpoints existentes para probar diferentes escenarios:

```bash
# 1. Login (funciona sin token - endpoint público)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password","role":"ADMIN"}'

# 2. Endpoint protegido sin token (Error 401)
curl -X GET http://localhost:8080/api/payments/status/1

# 3. Endpoint protegido con token inválido (Error 401)
curl -X GET http://localhost:8080/api/payments/status/1 \
  -H "Authorization: Bearer invalid-token"

# 4. Endpoint protegido con token válido (funciona)
curl -X GET http://localhost:8080/api/payments/status/1 \
  -H "Authorization: Bearer your-valid-jwt-token"

# 5. Registro de admin (funciona sin token - endpoint público)
curl -X POST http://localhost:8080/api/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password","businessName":"Test Business"}'
```

### Respuestas Esperadas

**Sin token (401):**
```json
{
  "message": "Authentication required. Please provide a valid JWT token.",
  "code": "UNAUTHORIZED",
  "details": {
    "error_type": "missing_authentication",
    "hint": "Include 'Authorization: Bearer <your-jwt-token>' header",
    "endpoint": "api/test-auth/protected",
    "method": "GET"
  },
  "timestamp": "2024-10-24T21:15:30.123Z"
}
```

**Token inválido (401):**
```json
{
  "message": "JWT authentication failed: Invalid token format",
  "code": "UNAUTHORIZED",
  "details": {
    "error_type": "jwt_invalid",
    "hint": "Your JWT token is invalid. Please login again",
    "token_status": "invalid"
  },
  "timestamp": "2024-10-24T21:15:30.123Z"
}
```

**Sin permisos (403):**
```json
{
  "message": "Access forbidden. Insufficient permissions: Admin role required",
  "code": "FORBIDDEN",
  "details": {
    "error_type": "insufficient_permissions",
    "hint": "You don't have permission to access this resource"
  },
  "timestamp": "2024-10-24T21:15:30.123Z"
}
```

Todas las respuestas seguirán el formato del `ErrorResponse` DTO.
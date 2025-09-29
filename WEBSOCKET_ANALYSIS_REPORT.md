# 🔍 Análisis WebSocket - Complejidad Cognitiva y Problemas de Seguridad

## ❌ **Problemas Identificados:**

### 🔥 **1. Complejidad Cognitiva Alta (23 > 15 permitido):**
**Método:** `WebSocketSessionManager.handleConnection()` línea 99

**Factores que incrementan complejidad:**
- Multiple try-catch blocks anidados
- Nested conditional statements 
- Multiple chain operations con validaciones
- Exception handling dentro de Uni chains
- Parallel validation flows

### 🔥 **2. Inyecciones No Utilizadas:**

#### ❌ **PaymentWebSocketController.java:**
```java
@Inject
SecurityService securityService;  // ← NO SE USA! Solo se usa en SessionManager

@Inject  
WebSocketTokenExtractor tokenExtractor; // ← NO SE USA! Solo se usa en SessionManager
```

#### ❌ **WebSocketSessionManager.java:**
```java
@Inject
SecurityService securityService; // ← PROBLEMA: Service haciendo validaciones de seguridad
```

### 🔥 **3. Violación de Arquitectura de Seguridad:**
**Problema:** WebSocketSessionManager está haciendo validaciones de seguridad directamente

```java
// INCORRECTO - Service manejando seguridad
return securityService.validateSellerAuthorization(authorization, sellerId)
        .chain(userId -> registerSessionAndSendWelcome(sellerId, session))
```

**Debería ser:** Controller maneja seguridad → Service recibe userId limpio

### 🔥 **4. Flujo de Seguridad WebSocket Duplicado:**

**ACTUAL (MALO):**
```
WebSocketController.onOpen()
  ↓ 
WebSocketSessionManager.handleConnection()  ← Violación arquitectural
  ├── tokenExtractor.extractTokenFromSession()
  ├── securityService.validateSellerAuthorization()  ← Service haciendo security!
  └── registerSessionAndSendWelcome()
```

**CORRECTO DEBERÍA SER:**
```
WebSocketController.onOpen()
  ├── validateSecurity() ← En controller
  ├── extractUserId() ← En controller  
  └── sessionManager.handleConnection(userId, session) ← Solo business logic
```

---

## 🔧 **Refactoring Needed:**

### 🎯 **1. Reducir Complejidad Cognitiva:**

**Problema:** `handleConnection()` método monolítico con múltiples responsabilidades

**Solución:** Dividir en métodos más pequeños:

```java
public Uni<Void> handleConnection(Session session, String sellerIdParam) {
    return validateSellerId(sellerIdParam)
            .chain(sellerId -> configureSession(session, sellerId))
            .chain(sellerId -> authenticateUser(session, sellerId))
            .chain(sellerId -> registerSessionAndSendWelcome(sellerId, session))
            .onFailure().recoverWithUni(throwable -> sendErrorAndClose(session, throwable.getMessage()));
}
```

### 🎯 **2. Mover Seguridad al Controller:**

**Problema:** WebSocketSessionManager violando principios arquitecturales

**Solución:** 
- Controller valida seguridad antes de llamar al SessionManager
- SessionManager solo maneja business logic de conexiones
- Eliminar SecurityService injection del SessionManager

### 🎯 **3. Eliminar Inyecciones Innecesarias:**

**Problema:** PaymentWebSocketController inyecta servicios que no usa

**Solución:**
- Eliminar `SecurityService` injection (duplicado)
- Eliminar `WebSocketTokenExtractor` injection (no usado en controller)
- Solo mantener `WebSocketSessionManager` y `WebSocketMessageHandler`

---

## 📋 **Plan de Refactoring:**

### 🔥 **Fase 1: Reducir Complejidad Cognitiva**
1. **Extraer métodos:** `validateSellerId()`, `configureSession()`, `authenticateUser()`
2. **Simplificar:** Eliminar nested try-catch blocks
3. **Separar:** Validación de parámetros vs business logic

### 🔥 **Fase 2: Limpiar Arquitectura**
1. **Mover seguridad:** A PaymentWebSocketController
2. **Eliminar:** SecurityService injection de SessionManager  
3. **Signatura limpia:** `handleConnection(Long userId, Session session)`

### 🔥 **Fase 3: Eliminar Duplicaciones**
1. **Remover:** SecurityService duplicado en PaymentWebSocketController
2. **Remover:** WebSocketTokenExtractor no usado
3. **Consolidar:** Security validation en un solo lugar

---

## 🎯 **Estado Actual vs Esperado:**

| Componente | Actual | Problema | Esperado |
|------------|--------|----------|----------|
| **Complexity** | 23 | >15 limit | <15 ✅ |
| **Architecture** | Service→Security | Violation | Controller→Security ✅ |
| **Dependencies** | Multiple unused | Waste | Only used services ✅ |
| **Separation** | Mixed concerns | Bad | Clean separation ✅ |

---

## ⚡ **Métricas de Complejidad Actual:**

### 🔥 **handleConnection() Analysis:**
- **Conditionals:** 4 (`if` statements)
- **Try-catch:** 2 blocks anidados  
- **Chain operations:** 3 nested chains
- **Decision points:** 7 total
- **Calculated complexity:** 23 (8 over limit!)

### ✅ **Target After Refactoring:**
- **Conditionals:** 2 max
- **Try-catch:** 1 block  
- **Chain operations:** Linear, no nesting
- **Decision points:** 4 max
- **Target complexity:** ≤15 ✅

---

**¡Necesita refactoring urgente para cumplir con estándares de código!** 🚨

# 🔄 Flujo Completo: API Yape Notifications + WebSocket (Mobile)

## 📱 **Arquitectura del Flujo**

```
Sistema Yape → Backend → WebSocket → App Mobile (Vendedor)
```

---

## **1. 🚀 PUNTO DE ENTRADA: API Yape Notifications**

### **Endpoint:** `POST /api/notifications/yape-notifications`

**¿Qué hace?**
- **Admin** recibe pago encriptado desde sistema externo de Yape
- **Backend** desencripta la información del pago
- **Backend** guarda el pago en base de datos con status `PENDING`
- **Backend** identifica al admin y sus vendedores afiliados
- **Backend** envía notificación WebSocket a **TODOS** los vendedores del admin

**Datos que recibe:**
```json
{
  "adminId": 605,
  "encryptedNotification": "KVAREhFWB10HXAJZF2sWQVIBTAJTEwBRRWEDRUMYeg4TQwdVClcLGkV2wpVSXl9WW0EAVANbBlU=",
  "deviceFingerprint": "a1b2c3d4e5f6789a",
  "timestamp": 1757995044
}
```

**Datos que devuelve:**
```json
{
  "success": true,
  "data": {
    "paymentId": 1234,
    "amount": 80.0,
    "status": "PENDING_CONFIRMATION",
    "message": "Pago enviado a vendedores para confirmación"
  }
}
```

---

## **2. 📡 NOTIFICACIÓN WEBSOCKET AUTOMÁTICA**

### **¿Qué pasa después del paso 1?**
1. **Backend identifica** al admin (605) y busca todos sus vendedores afiliados
2. **Backend envía** mensaje WebSocket a **TODOS** los vendedores del admin
3. **Solo los vendedores** del admin reciben la notificación

**WebSocket URL:** `ws://localhost:8080/ws/payments/{sellerId}`

**Mensaje enviado:**
```json
{
  "type": "PAYMENT_NOTIFICATION",
  "data": {
    "paymentId": 1234,
    "amount": 80.0,
    "senderName": "987654321",
    "yapeCode": "YAPE_1757840358050_907349_68",
    "status": "PENDING",
    "timestamp": "2025-09-15T22:26:52.09808",
    "message": "Pago pendiente de confirmación"
  }
}
```

---

## **3. 📱 IMPLEMENTACIÓN EN MOBILE**

### **A. Conexión WebSocket**
- **Cuándo:** Al iniciar la app del vendedor
- **URL:** `ws://localhost:8080/ws/payments/{sellerId}`
- **Autenticación:** Token JWT del vendedor
- **Reconexión:** Automática si se pierde la conexión

### **B. Manejo de Mensajes**
Cuando llega un mensaje WebSocket:

1. **Verificar tipo:** `PAYMENT_NOTIFICATION`
2. **Extraer datos:** paymentId, amount, senderName, etc.
3. **Mostrar notificación:** Push notification + UI update
4. **Actualizar lista:** Refrescar lista de pagos pendientes
5. **Reproducir sonido:** Audio de notificación

### **C. Acciones del Vendedor**
Cuando el vendedor ve el pago, puede:

**Reclamar Pago:**
- **API:** `POST /api/payments/claim`
- **Body:** `{"sellerId": 251, "paymentId": 1234}`
- **Resultado:** Pago cambia a status `CONFIRMED`

**Rechazar Pago:**
- **API:** `POST /api/payments/reject`
- **Body:** `{"sellerId": 251, "paymentId": 1234, "reason": "No es mi cliente"}`
- **Resultado:** Pago cambia a status `REJECTED_BY_SELLER`

---

## **4. 🔄 NOTIFICACIÓN DE RESULTADO**

### **¿Qué pasa después de reclamar/rechazar?**
1. **Vendedor** reclama/rechaza el pago
2. **Backend** actualiza el status del pago en BD
3. **Backend** identifica al admin del vendedor
4. **Backend** envía notificación al **admin** y **todos los vendedores** del admin

**Mensaje WebSocket enviado:**
```json
{
  "type": "PAYMENT_RESULT",
  "data": {
    "paymentId": 1234,
    "status": "CONFIRMED",
    "message": "Pago confirmado por vendedor 251",
    "sellerId": 251,
    "sellerName": "Juan Pérez"
  }
}
```

**En la app mobile:**
- Actualizar el estado del pago en la UI
- Remover de la lista de pendientes
- Mostrar mensaje de confirmación

---

## **5. 📋 APIS ADICIONALES PARA MOBILE**

### **Ver Pagos Pendientes:**
- **API:** `GET /api/payments/pending?sellerId={id}&page=0&size=20`
- **Uso:** Cargar lista inicial y refrescar después de acciones

### **Estadísticas del Vendedor:**
- **API:** `GET /api/stats/seller/summary?sellerId={id}`
- **Uso:** Dashboard con métricas del vendedor

---

## **6. 🎯 FLUJO COMPLETO PASO A PASO**

### **Escenario: Cliente hace un pago**

1. **Sistema Yape** → Envía pago encriptado a `/api/notifications/yape-notifications`
2. **Backend** → Desencripta, identifica admin (605), guarda en BD, status `PENDING`
3. **Backend** → Busca todos los vendedores afiliados al admin (605)
4. **Backend** → Envía WebSocket `PAYMENT_NOTIFICATION` a todos los vendedores del admin
5. **App Mobile** → Recibe notificación, muestra notificación local
6. **Vendedor** → Ve el pago en la app, decide si reclamar o rechazar
7. **App Mobile** → Envía `POST /api/payments/claim` o `/reject`
8. **Backend** → Actualiza status del pago en BD, identifica admin del vendedor
9. **Backend** → Envía WebSocket `PAYMENT_RESULT` al admin y todos sus vendedores
10. **App Mobile** → Actualiza UI, remueve de lista pendiente

---

## **7. 🔐 FLUJO DE ENCRIPTACIÓN Y ADMIN**

### **A. Proceso de Encriptación (Sistema Externo → Admin)**
1. **Sistema Yape** genera pago con datos del cliente
2. **Sistema Yape** encripta la información usando clave del admin
3. **Sistema Yape** envía pago encriptado al admin
4. **Admin** recibe pago encriptado y lo reenvía al backend

### **B. Proceso de Desencriptación (Backend)**
1. **Backend** recibe pago encriptado del admin
2. **Backend** desencripta usando clave del admin
3. **Backend** extrae información del pago (monto, cliente, etc.)
4. **Backend** identifica al admin y sus vendedores afiliados

### **C. Notificación a Vendedores**
1. **Backend** busca todos los vendedores del admin
2. **Backend** envía WebSocket a cada vendedor del admin
3. **Solo vendedores** del admin reciben la notificación
4. **Vendedores** pueden reclamar o rechazar el pago

### **D. Notificación de Resultado**
1. **Vendedor** reclama/rechaza el pago
2. **Backend** actualiza status del pago
3. **Backend** notifica al admin y todos sus vendedores
4. **Admin** recibe notificación del resultado
5. **Otros vendedores** ven que el pago fue procesado

---

## **8. ⚙️ CONSIDERACIONES TÉCNICAS PARA MOBILE**

### **WebSocket:**
- **Reconexión automática** si se pierde conexión
- **Manejo de estados** de conexión (conectado/desconectado/conectando)
- **Autenticación** con token JWT
- **Heartbeat** para mantener conexión activa

### **Notificaciones Locales:**
- **Notificaciones en UI** cuando la app está abierta
- **Sonido** y vibración para alertar al vendedor
- **Actualización automática** de la lista de pagos

### **UI/UX:**
- **Lista en tiempo real** de pagos pendientes
- **Botones de acción** claros (Reclamar/Rechazar)
- **Estados visuales** diferentes (PENDING/CONFIRMED/REJECTED)
- **Indicadores** de conexión WebSocket

### **Manejo de Errores:**
- **Conexión perdida:** Reconectar automáticamente
- **Token expirado:** Renovar token y reconectar
- **Errores de API:** Mostrar mensajes de error al usuario
- **Sin conexión:** Modo offline con sincronización posterior

---

## **9. 📊 DATOS IMPORTANTES**

### **Información del Pago:**
- `paymentId`: ID único del pago
- `amount`: Monto del pago
- `senderName`: Nombre/teléfono del cliente
- `yapeCode`: Código de referencia de Yape
- `status`: PENDING → CONFIRMED/REJECTED_BY_SELLER
- `timestamp`: Fecha y hora del pago

### **Información del Vendedor:**
- `sellerId`: ID del vendedor (del token JWT)
- `token`: JWT para autenticación
- `adminId`: ID del admin (para filtros)

---

## **10. 🎯 RESUMEN PARA EL EQUIPO MOBILE**

**Lo que necesitan implementar:**

1. **WebSocket Client** que se conecte a `ws://localhost:8080/ws/payments/{sellerId}`
2. **Manejo de mensajes** `PAYMENT_NOTIFICATION` y `PAYMENT_RESULT`
3. **APIs REST** para claim/reject y ver pagos pendientes
4. **Notificaciones locales** cuando llegan pagos nuevos
5. **UI en tiempo real** que se actualice automáticamente
6. **Reconexión automática** del WebSocket
7. **Manejo de errores** y estados de conexión

**El backend ya está listo** - solo necesitan implementar el cliente mobile que consuma estas APIs y WebSocket.

---

## **11. 🔧 IMPLEMENTACIÓN TÉCNICA**

### **WebSocket Client (Android/iOS):**
```kotlin
// Android - Kotlin
class PaymentWebSocketClient {
    private var webSocket: WebSocket? = null
    
    fun connect(sellerId: Long, token: String) {
        val url = "ws://localhost:8080/ws/payments/$sellerId"
        // Implementar conexión WebSocket con autenticación
    }
    
    fun handleMessage(message: String) {
        val notification = Gson().fromJson(message, PaymentNotification::class.java)
        when (notification.type) {
            "PAYMENT_NOTIFICATION" -> showPaymentNotification(notification.data)
            "PAYMENT_RESULT" -> updatePaymentStatus(notification.data)
        }
    }
}
```

### **APIs REST (Android/iOS):**
```kotlin
// Android - Retrofit
interface PaymentApi {
    @GET("api/payments/pending")
    suspend fun getPendingPayments(
        @Query("sellerId") sellerId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PendingPaymentsResponse>
    
    @POST("api/payments/claim")
    suspend fun claimPayment(@Body request: PaymentClaimRequest): Response<ApiResponse>
    
    @POST("api/payments/reject")
    suspend fun rejectPayment(@Body request: PaymentRejectRequest): Response<ApiResponse>
}
```

### **Notificaciones Locales:**
```kotlin
// Android - Notificaciones locales cuando la app está abierta
class PaymentNotificationManager {
    fun showLocalNotification(paymentData: PaymentData) {
        // Mostrar notificación en la UI de la app
        // Reproducir sonido
        // Vibrar
        // Actualizar lista de pagos
    }
    
    fun playNotificationSound() {
        // Reproducir sonido de notificación
    }
    
    fun vibrate() {
        // Vibrar el dispositivo
    }
}
```

---

## **12. 📱 FLUJO DE UI/UX**

### **Pantalla Principal:**
1. **Lista de pagos pendientes** (actualizada en tiempo real)
2. **Indicador de conexión WebSocket** (verde/rojo)
3. **Botón de refrescar** (para sincronizar manualmente)
4. **Contador de pagos pendientes**

### **Pantalla de Pago:**
1. **Información del pago** (monto, cliente, código Yape)
2. **Botón "Reclamar"** (verde)
3. **Botón "Rechazar"** (rojo)
4. **Campo de razón** (opcional para rechazo)
5. **Confirmación** antes de ejecutar acción

### **Notificaciones:**
1. **Notificación local** cuando llega pago nuevo (app abierta)
2. **Sonido** y vibración
3. **Toast/Snackbar** para confirmaciones
4. **Actualización automática** de la lista

---

## **13. 🚨 MANEJO DE ERRORES**

### **Errores de Conexión:**
- **WebSocket desconectado:** Mostrar indicador rojo, intentar reconectar
- **Sin internet:** Modo offline, sincronizar cuando vuelva conexión
- **Token expirado:** Renovar token automáticamente

### **Errores de API:**
- **401 Unauthorized:** Token inválido, redirigir a login
- **400 Bad Request:** Datos inválidos, mostrar error específico
- **500 Server Error:** Error del servidor, mostrar mensaje genérico

### **Errores de WebSocket:**
- **Conexión perdida:** Reconectar automáticamente cada 5 segundos
- **Mensaje inválido:** Ignorar mensaje, log para debugging
- **Timeout:** Reiniciar conexión

---

## **14. 📋 CHECKLIST DE IMPLEMENTACIÓN**

### **WebSocket:**
- [ ] Conexión inicial al iniciar app
- [ ] Autenticación con token JWT
- [ ] Manejo de mensajes `PAYMENT_NOTIFICATION`
- [ ] Manejo de mensajes `PAYMENT_RESULT`
- [ ] Reconexión automática
- [ ] Indicador visual de conexión
- [ ] Manejo de errores de conexión

### **APIs REST:**
- [ ] GET `/api/payments/pending` (con paginación)
- [ ] POST `/api/payments/claim`
- [ ] POST `/api/payments/reject`
- [ ] GET `/api/stats/seller/summary`
- [ ] Manejo de respuestas y errores
- [ ] Autenticación con token JWT

### **UI/UX:**
- [ ] Lista de pagos pendientes
- [ ] Pantalla de detalle de pago
- [ ] Botones de acción (Reclamar/Rechazar)
- [ ] Indicadores de estado
- [ ] Notificaciones locales
- [ ] Sonidos y vibración
- [ ] Modo offline

### **Notificaciones:**
- [ ] Notificaciones locales en UI
- [ ] Sonidos de notificación
- [ ] Vibración
- [ ] Toast/Snackbar para confirmaciones
- [ ] Actualización automática de lista

---

## **15. 🎯 CONCLUSIÓN**

**El backend está completamente implementado y listo.** Tu equipo de mobile solo necesita:

1. **Implementar cliente WebSocket** para recibir notificaciones en tiempo real
2. **Consumir APIs REST** para las acciones de reclamar/rechazar
3. **Crear UI/UX** que se actualice automáticamente
4. **Implementar notificaciones locales** para alertar al vendedor
5. **Manejar reconexión** y errores de conexión

**El flujo es automático:** cuando llega un pago → WebSocket → notificación → vendedor actúa → WebSocket → actualización en tiempo real.

¿Necesitas que profundice en algún aspecto específico para tu equipo de mobile?
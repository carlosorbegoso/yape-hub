# Swagger UI - Yape Hub API Documentation

## 🚀 Acceso a la Documentación

### URLs Principales
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/openapi`
- **Configuración Personalizada**: `http://localhost:8080/swagger-ui-config`

## 🎨 Características Mejoradas

### 1. Interfaz Mejorada
- **Tema Personalizado**: Colores corporativos de Yape Hub
- **Navegación Mejorada**: Deep linking y filtros avanzados
- **Ejemplos Interactivos**: Ejemplos de request/response en cada endpoint
- **Try It Out**: Funcionalidad completa para probar APIs directamente

### 2. Documentación Detallada
- **Descripciones Completas**: Cada endpoint tiene descripción detallada
- **Ejemplos de Respuesta**: JSON de ejemplo para cada respuesta
- **Códigos de Error**: Documentación completa de errores posibles
- **Esquemas Personalizados**: Modelos de datos bien definidos

### 3. Autenticación Integrada
- **JWT Bearer Token**: Configuración automática de autenticación
- **Botón Authorize**: Interfaz fácil para agregar tokens
- **Instrucciones Claras**: Guía paso a paso para obtener tokens

## 🔧 Configuración Avanzada

### Temas y Personalización
```json
{
  "theme": {
    "colors": {
      "primary": { "main": "#1976d2" },
      "secondary": { "main": "#dc004e" }
    }
  },
  "deepLinking": true,
  "displayRequestDuration": true,
  "filter": true,
  "showExtensions": true
}
```

### Características Habilitadas
- ✅ **Deep Linking**: Enlaces directos a endpoints
- ✅ **Request Duration**: Muestra tiempo de respuesta
- ✅ **Filtering**: Filtro de búsqueda avanzado
- ✅ **Extensions**: Muestra extensiones OpenAPI
- ✅ **Try It Out**: Pruebas interactivas
- ✅ **Examples**: Ejemplos en cada endpoint

## 📚 Categorías de APIs

### 🔐 Authentication
- Registro de administradores
- Login con email/password
- Refresh token
- Recuperación de contraseña
- Login de vendedores por teléfono

### 👨‍💼 Admin Management
- Perfil de administrador
- Actualización de datos

### 🏢 Branch Management
- CRUD completo de sucursales
- Listado con paginación
- Gestión de vendedores por sucursal

### 👥 Seller Management
- Listado de vendedores
- Actualización de datos
- Pausa/eliminación
- Límites de suscripción

### 📱 Affiliation Management
- Generación de códigos QR
- Validación de códigos
- Registro de vendedores
- Login con QR

### 💳 Payments
- Estados de conexión
- Reclamación de pagos
- Rechazo de pagos
- Pagos pendientes
- Gestión administrativa

### 🔔 Notifications
- Listado de notificaciones
- Marcado como leídas
- Procesamiento Yape
- Auditoría

### 💰 Billing Management
- Información de facturación
- Operaciones de facturación
- Planes de suscripción
- Estados de pago

### 🏛️ Admin Billing Management
- Gestión administrativa
- Aprobación/rechazo
- Dashboard administrativo

### 📊 Statistics
- Resúmenes básicos
- Analytics avanzados
- Análisis financiero
- Reportes de transparencia

## 🎯 Cómo Usar Swagger UI

### 1. Acceder a la Documentación
```
http://localhost:8080/swagger-ui
```

### 2. Autenticarse
1. Haz clic en el botón **"Authorize"** (🔒)
2. Ingresa tu token JWT: `Bearer <your-token>`
3. Haz clic en **"Authorize"**

### 3. Probar Endpoints
1. Expande cualquier endpoint
2. Haz clic en **"Try it out"**
3. Completa los parámetros
4. Haz clic en **"Execute"**
5. Ve la respuesta en tiempo real

### 4. Ejemplos de Uso

#### Registro de Administrador
```bash
POST /api/auth/admin/register
{
  "businessName": "Mi Negocio",
  "email": "admin@negocio.com",
  "password": "password123",
  "phone": "+51987654321",
  "contactName": "Juan Pérez"
}
```

#### Login
```bash
POST /api/auth/login
{
  "email": "admin@negocio.com",
  "password": "password123"
}
```

#### Crear Sucursal
```bash
POST /api/admin/branches
Authorization: Bearer <token>
{
  "name": "Sucursal Principal",
  "code": "SUC001",
  "address": "Av. Principal 123"
}
```

## 🔍 Características Especiales

### 1. Ejemplos Interactivos
Cada endpoint incluye ejemplos de:
- Request body
- Response success
- Response error
- Headers requeridos

### 2. Validación en Tiempo Real
- Validación de esquemas
- Autocompletado de campos
- Verificación de tipos de datos

### 3. Documentación Contextual
- Descripciones detalladas
- Parámetros explicados
- Códigos de estado documentados

### 4. Navegación Intuitiva
- Agrupación por categorías
- Búsqueda rápida
- Filtros avanzados

## 🛠️ Configuración Técnica

### OpenAPI 3.0
- Especificación completa OpenAPI 3.0
- Esquemas personalizados
- Ejemplos integrados
- Seguridad JWT

### SmallRye OpenAPI
- Integración nativa con Quarkus
- Generación automática
- Configuración flexible
- Soporte completo

### Swagger UI
- Interfaz moderna
- Funcionalidades avanzadas
- Personalización completa
- Experiencia de usuario optimizada

## 📱 Responsive Design

La documentación está optimizada para:
- 💻 **Desktop**: Experiencia completa
- 📱 **Mobile**: Navegación adaptada
- 📟 **Tablet**: Interfaz optimizada

## 🚀 Mejoras Futuras

### Próximas Características
- [ ] Temas personalizables
- [ ] Exportación de colecciones
- [ ] Integración con Postman
- [ ] Métricas de uso
- [ ] Documentación offline

### Optimizaciones
- [ ] Carga más rápida
- [ ] Caché inteligente
- [ ] Compresión de respuestas
- [ ] CDN integration

## 📞 Soporte

Para problemas con la documentación:
- **Email**: support@yapechamo.com
- **GitHub**: [Yape Hub Repository]
- **Documentación**: [Link to docs]

## 🔗 Enlaces Útiles

- **API Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/openapi`
- **Health Check**: `http://localhost:8080/q/health`

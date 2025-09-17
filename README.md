# Yape Hub API

Un sistema completo de gestión de pagos construido con Quarkus que permite a las empresas gestionar pagos Yape, vendedores y transacciones a través de una API REST robusta.

## 🚀 Características

- **Gestión de Usuarios**: Registro de administradores y vendedores con autenticación JWT
- **Gestión de Negocios**: Soporte multi-sucursal con controles administrativos
- **Gestión de Vendedores**: Afiliación, gestión y seguimiento de rendimiento de vendedores
- **Procesamiento de Transacciones**: Procesamiento de pagos Yape con flujos de confirmación
- **Dashboard y Analytics**: Dashboards en tiempo real para administradores y vendedores
- **Sistema de Notificaciones**: Notificaciones push para transacciones y eventos del sistema
- **Generación de Códigos QR**: Códigos QR dinámicos para pagos y afiliaciones
- **Sistema de Afiliación**: Onboarding seguro de vendedores con códigos únicos
- **Reportes**: Reportes completos de transacciones y analytics
- **Documentación API**: Documentación completa OpenAPI/Swagger
- **WebSocket**: Notificaciones en tiempo real para vendedores
- **Gestión de Sucursales**: Administración completa de sucursales por administrador

## 🛠 Technology Stack

- **Framework**: Quarkus (Java 21)
- **Database**: PostgreSQL with Hibernate ORM
- **Authentication**: JWT (JSON Web Tokens)
- **API Documentation**: OpenAPI/Swagger
- **Validation**: Bean Validation
- **Security**: SmallRye JWT
- **QR Code Generation**: ZXing library
- **Email**: Quarkus Mailer
- **Build Tool**: Gradle

## 📋 Prerequisites

- Java 21
- PostgreSQL database
- Maven/Gradle

## 🚀 Quick Start

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE yapechamo;
CREATE USER yapechamo WITH PASSWORD 'yapechamo123';
GRANT ALL PRIVILEGES ON DATABASE yapechamo TO yapechamo;
```

### 2. Configuration

Update database credentials in `src/main/resources/application.properties`:

```properties
quarkus.datasource.username=yapechamo
quarkus.datasource.password=yapechamo123
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/yapechamo
```

### 3. Run the Application

```bash
./gradlew quarkusDev
```

The API will be available at `http://localhost:8080`

### 4. Acceder a la Documentación de la API

- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI JSON**: `http://localhost:8080/openapi`
- **Documentación Completa**: [ENDPOINTS_BY_ROLE.md](./ENDPOINTS_BY_ROLE.md)

## 📚 Documentación de la API

La documentación completa de la API está disponible en [ENDPOINTS_BY_ROLE.md](./ENDPOINTS_BY_ROLE.md)

### Endpoints Principales

#### **Autenticación**
- `POST /api/auth/admin/register` - Registro de administrador
- `POST /api/auth/login` - Login general (admin/seller)
- `POST /api/auth/seller/login-by-phone` - Login de vendedor con código de afiliación
- `POST /api/login-with-qr` - Login de vendedor usando QR

#### **Administradores**
- `GET /api/admin/profile` - Gestión de perfil
- `GET /api/admin/sellers/my-sellers` - Listar vendedores
- `GET /api/admin/branches` - Gestión de sucursales
- `POST /api/generate-affiliation-code-protected` - Generar códigos de afiliación
- `GET /api/stats/admin` - Estadísticas y analytics

#### **Vendedores**
- `GET /api/payments/pending` - Pagos pendientes
- `POST /api/payments/claim` - Reclamar pago
- `POST /api/payments/reject` - Rechazar pago
- `GET /api/stats/seller` - Estadísticas del vendedor

#### **Públicos**
- `POST /api/validate-affiliation-code` - Validar código de afiliación
- `POST /api/generate-qr-base64` - Generar QR con Base64
- `POST /api/seller/register` - Registro de vendedor

#### **WebSocket**
- `ws://localhost:8080/ws/payments/{sellerId}?token={jwt_token}` - Notificaciones en tiempo real

## 🏗 Project Structure

```
src/main/java/org/sky/
├── controller/          # REST API controllers
├── service/            # Business logic services
├── model/              # JPA entities
├── dto/                # Data transfer objects
└── util/               # Utility classes
```

## 🔧 Development

### Running in Development Mode

```bash
./gradlew quarkusDev
```

This enables live coding with automatic reloading.

### Building the Application

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Building Native Executable

```bash
./gradlew build -Dquarkus.native.enabled=true
```

## 🐳 Docker Support

Build Docker image:

```bash
./gradlew build -Dquarkus.native.enabled=true
docker build -f src/main/docker/Dockerfile.native -t yapechamo-api .
```

## 🔐 Security Features

- **Password Hashing**: BCrypt with salt
- **JWT Authentication**: Secure token-based authentication
- **Input Validation**: Bean Validation annotations
- **SQL Injection Protection**: Hibernate ORM with parameterized queries
- **CORS Configuration**: Configurable cross-origin resource sharing
- **Rate Limiting**: Protection against abuse

## 📊 Monitoring

### Health Checks

- **Health Check**: `GET /q/health`
- **Metrics**: `GET /q/metrics`
- **Info**: `GET /q/info`

## 🚀 Deployment

### Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_USERNAME=yapechamo
QUARKUS_DATASOURCE_PASSWORD=yapechamo123
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/yapechamo

# Email
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# JWT
JWT_SECRET=your-jwt-secret-key
```

### Production Checklist

1. Set up PostgreSQL database
2. Configure environment variables
3. Set up SSL certificates
4. Configure reverse proxy (nginx)
5. Set up monitoring and logging

## 📝 Ejemplos de la API

### Registro de Administrador

```bash
curl -X POST http://localhost:8080/api/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Mi Negocio SRL",
    "email": "admin@minegocio.com",
    "password": "SecurePass123!",
    "phone": "+51987654321",
    "address": "Av. Principal 123, Lima",
    "contactName": "Juan Pérez"
  }'
```

### Login de Administrador

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@minegocio.com",
    "password": "SecurePass123!",
    "deviceFingerprint": "device_unique_id_123",
    "role": "ADMIN"
  }'
```

### Login de Vendedor con Código de Afiliación

```bash
curl -X POST "http://localhost:8080/api/auth/seller/login-by-phone?phone=987654321&affiliationCode=AFF646424" \
  -H "accept: application/json"
```

### Generar Código QR

```bash
curl -X POST http://localhost:8080/api/generate-qr-base64 \
  -H "Content-Type: application/json" \
  -d '{
    "affiliationCode": "AFF646424"
  }'
```

### Login con QR

```bash
curl -X POST http://localhost:8080/api/login-with-qr \
  -H "Content-Type: application/json" \
  -d '{
    "qrData": "eyJhZmZpbGlhdGlvbkNvZGUiOiJBRkY2NDY0MjQiLCJicmFuY2hJZCI6NjA1LCJhZG1pbklkIjo2MDUsImV4cGlyZXNBdCI6IjIwMjUtMDktMTZUMjI6MzY6MjEuMDU3ODQxIiwibWF4VXNlcyI6MX0=",
    "phone": "987654321"
  }'
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Soporte

Para soporte y preguntas:

- **Documentación Completa**: [ENDPOINTS_BY_ROLE.md](./ENDPOINTS_BY_ROLE.md)
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI JSON**: `http://localhost:8080/openapi`
- **Issues**: Crear issues en GitHub
- **Email**: support@yapechamo.com

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2025  
**Autor**: Yape Hub Development Team
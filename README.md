# YapeChamo API Hub

A comprehensive payment management system built with Quarkus that allows businesses to manage Yape payments, sellers, and transactions through a robust REST API.

## 🚀 Features

- **User Management**: Admin and seller registration with JWT authentication
- **Business Management**: Multi-branch business support with admin controls
- **Seller Management**: Seller affiliation, management, and performance tracking
- **Transaction Processing**: Yape payment processing with confirmation workflows
- **Dashboard & Analytics**: Real-time dashboards for admins and sellers
- **Notification System**: Push notifications for transactions and system events
- **QR Code Generation**: Dynamic QR codes for payments and affiliations
- **Affiliation System**: Secure seller onboarding with unique codes
- **Reporting**: Comprehensive transaction reports and analytics
- **API Documentation**: Complete OpenAPI/Swagger documentation

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

### 4. Access API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui`

## 📚 API Documentation

Complete API documentation is available in [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

### Key Endpoints

- **Authentication**: `/api/auth/*`
- **Admin Management**: `/api/admin/*`
- **Seller Management**: `/api/admin/sellers/*`
- **Transactions**: `/api/transactions/*`
- **Dashboard**: `/api/admin/dashboard`, `/api/sellers/dashboard`
- **Notifications**: `/api/notifications/*`
- **QR Codes**: `/api/admin/qr/*`

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

## 📝 API Examples

### Register Admin

```bash
curl -X POST http://localhost:8080/api/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Mi Negocio SRL",
    "businessType": "RESTAURANT",
    "ruc": "20123456789",
    "email": "admin@minegocio.com",
    "password": "SecurePass123!",
    "phone": "+51987654321",
    "address": "Av. Principal 123, Lima",
    "contactName": "Juan Pérez"
  }'
```

### Login

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

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For support and questions:

- **Documentation**: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **Issues**: Create GitHub issues
- **Email**: support@yapechamo.com

---

**Version**: 1.0.0  
**Last Updated**: January 2024  
**Author**: YapeChamo Development Team
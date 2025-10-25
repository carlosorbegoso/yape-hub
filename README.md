# 🐳 Docker Native Build - Guía Completa

Esta guía te ayudará a construir y ejecutar Yape Hub como una **imagen nativa de Docker** usando Quarkus y GraalVM.

## 📋 Requisitos Previos

- **Docker** instalado
- **Gradle** (incluido en el proyecto)
- **PostgreSQL** corriendo
- Al menos **4GB de RAM libre** para el build nativo

## 🏗️ Construcción de la Imagen Nativa


### Paso 1: Construir el Binario Nativo

Ejecuta el siguiente comando (toma 3-5 minutos):

```bash
./gradlew clean build -x test \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.additional-build-args="--gc=serial"
```

**Explicación de los parámetros:**
- `clean` - Limpia builds anteriores
- `build` - Construye el proyecto
- `-x test` - Omite tests (opcional, quítalo si quieres ejecutar tests)
- `-Dquarkus.package.jar.enabled=false` - No genera JAR
- `-Dquarkus.native.enabled=true` - Habilita compilación nativa
- `-Dquarkus.native.container-build=true` - Compila dentro de un contenedor (no necesitas GraalVM instalado)
- `--gc=serial` - Usa Serial GC (único soportado en GraalVM)

### Paso 2: Construir la Imagen Docker

```bash
docker build -f src/main/docker/Dockerfile.native-micro -t quarkus/yape-hub:latest .
```

## 🚀 Ejecución del Contenedor

### Ejecutar con Variables de Entorno

```bash
docker run -d \
  --name yape-hub \
  -p 8080:8080 \
  -e QUARKUS_PROFILE=prod \
  -e QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://192.168.3.47:5432/yapechamo \
  -e QUARKUS_DATASOURCE_USERNAME=yapechamo \
  -e QUARKUS_DATASOURCE_PASSWORD=yapechamo123 \
  quarkus/yape-hub:latest
```

**Nota**: Cambia `192.168.3.47` por la IP de tu base de datos. Si PostgreSQL está en tu máquina local (macOS/Windows), usa `host.docker.internal` en lugar de la IP.

## 📊 Verificación

### Ver logs del contenedor

```bash
docker logs -f yape-hub
```

### Verificar Health Check

```bash
curl http://localhost:8080/q/health
```

### Detener el contenedor

```bash
docker stop yape-hub
```

## 🔧 Troubleshooting

### No se puede conectar a la base de datos

Si PostgreSQL está en tu máquina local, usa:
```bash
-e QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://host.docker.internal:5432/yapechamo
```

### El puerto 8080 está en uso

Usa otro puerto:
```bash
docker run -d --name yape-hub -p 8081:8080 -e QUARKUS_PROFILE=prod quarkus/yape-hub:latest
```

## 📈 Ventajas del Build Nativo

- ✅ **Startup ultra-rápido**: ~50ms vs 2-3 segundos en JVM
- ✅ **Bajo consumo de memoria**: ~50-100MB vs 200-300MB en JVM
- ✅ **Imagen más pequeña**: ~80-120MB vs 200-300MB
- ✅ **Ideal para microservicios y contenedores**

## 🎯 Resumen de Comandos Rápidos

```bash
# 1. Build nativo (una sola vez, toma 3-5 minutos)
./gradlew clean build -x test \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.additional-build-args="--gc=serial"

# 2. Construir imagen Docker
docker build -f src/main/docker/Dockerfile.native-micro -t quarkus/yape-hub:latest .

# 3. Ejecutar contenedor
docker run -d --name yape-hub -p 8080:8080 \
  -e QUARKUS_PROFILE=prod \
  -e QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://192.168.3.47:5432/yapechamo \
  -e QUARKUS_DATASOURCE_USERNAME=yapechamo \
  -e QUARKUS_DATASOURCE_PASSWORD=yapechamo123 \
  quarkus/yape-hub:latest

# 4. Ver logs
docker logs -f yape-hub

# 5. Verificar salud
curl http://localhost:8080/q/health
```

---

**Nota**: Cambia `192.168.3.47` por la IP de tu base de datos PostgreSQL, o usa `host.docker.internal` en macOS/Windows si PostgreSQL está en tu máquina local.
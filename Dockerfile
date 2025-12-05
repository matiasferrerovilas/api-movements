# === STAGE 1: Build (Utiliza el JDK para construir) ===
FROM eclipse-temurin:25-jdk-alpine AS build

# Instalar 'unzip' para el proceso de extracción de capas del JAR
RUN apk add --no-cache unzip

WORKDIR /app

# 1. Copiar y descargar dependencias de Gradle (para aprovechar el cache de Docker)
# Si estos archivos no cambian, Docker no ejecutará la siguiente capa de RUN.
COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/

# Descargar dependencias para popular el cache de Gradle (acelera el build)
RUN ./gradlew dependencies --no-daemon

# 2. Copiar el código fuente completo
COPY src src

# 3. Construir el JAR
RUN ./gradlew clean bootJar --no-daemon

# 4. Usar la herramienta de capas de Spring Boot para extraer el JAR en directorios
RUN java -Djarmode=layertools -jar build/libs/expenses-*.jar extract --destination extracted


# === STAGE 2: Runtime (Utiliza el JRE más pequeño para ejecutar) ===
FROM eclipse-temurin:25-jre-alpine AS runtime

# Mejorar seguridad y rendimiento creando un usuario no root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# 1. Copiar las capas optimizadas desde el stage 'build'
# Orden de copia es crucial: Dependencias (raras veces cambia) -> Aplicación (cambia siempre)

# Capa 1: Dependencias estáticas
COPY --from=build --chown=appuser:appgroup /app/extracted/dependencies/ ./
# Capa 2: Spring Boot Loader (cargador del JAR, cambia poco)
COPY --from=build --chown=appuser:appgroup /app/extracted/spring-boot-loader/ ./
# Capa 3: Dependencias de Snapshot (si usas versiones SNAPSHOT)
COPY --from=build --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
# Capa 4: Código de la aplicación (¡Lo que cambia más!)
COPY --from=build --chown=appuser:appgroup /app/extracted/application/ ./

EXPOSE 8080

# Usar el cargador de Spring Boot para iniciar la aplicación en capas
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
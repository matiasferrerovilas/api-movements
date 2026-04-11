FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Las layers son extraídas en CI antes del build Docker (job build → Extract Spring Boot Layers)
# Orden: dependencias primero (cambian poco) → app al final (cambia siempre)
# Esto maximiza el cache de Docker layers en builds sucesivos
COPY --chown=appuser:appgroup extracted/dependencies/ ./
COPY --chown=appuser:appgroup extracted/spring-boot-loader/ ./
COPY --chown=appuser:appgroup extracted/snapshot-dependencies/ ./
COPY --chown=appuser:appgroup extracted/application/ ./

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

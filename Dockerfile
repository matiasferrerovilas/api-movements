FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# Convertir CRLF → LF
RUN apk add --no-cache dos2unix && dos2unix gradlew

RUN chmod +x gradlew
RUN ./gradlew clean assemble -x test --no-daemon


FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/expenses-api.jar

EXPOSE 8080

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/expenses-api.jar"]

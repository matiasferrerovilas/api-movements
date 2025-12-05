FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

COPY . .

RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

COPY build/libs/expenses-[0-9]*.jar expenses-api.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/expenses-api.jar"]



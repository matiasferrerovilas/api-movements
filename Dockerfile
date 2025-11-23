FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY expenses-api.jar expenses-api.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/expenses-api.jar"]


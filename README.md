# Expenses API

A robust and scalable RESTful API for managing personal or business expenses, built with Spring Boot and designed for containerized deployment.

## Features

- **Expense Management**: Track expenses with categories, amounts, and dates
- **Multi-currency Support**: Handle expenses in different currencies (ARS, USD, EUR, CHF)
- **Credit/Debit Tracking**: Support for both credit and debit transactions
- **User Authentication**: Secure API with OAuth2 and JWT
- **API Documentation**: Interactive documentation with Swagger/OpenAPI
- **Metrics & Monitoring**: Built-in support for Prometheus and Grafana
- **Container Ready**: Docker support for easy deployment
- **Database Migrations**: Liquibase for database versioning

## Tech Stack

- **Java 21** with **Spring Boot 3.5.5**
- **MySQL 8.0** Database
- **Liquibase** for database migrations
- **MapStruct** for object mapping
- **Spring Security** with OAuth2
- **Spring Web** for REST endpoints
- **Spring Data JPA** for data access
- **Spring AOP** for cross-cutting concerns
- **Micrometer** for application metrics
- **TestContainers** for integration testing
- **Spock** for testing

## Prerequisites

- Java 21 JDK
- Docker and Docker Compose (for containerized deployment)
- MySQL 8.0+ (or use the provided Docker Compose setup)
- Gradle 8.0+

## Getting Started

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/api-expenses.git
   cd api-expenses
   ```

2. **Set up the database**
    - Create a MySQL database named `expenses`
    - Or use the provided docker-compose file:
      ```bash
      docker-compose up -d mysql
      ```

3. **Configure application properties**
   Create `src/main/resources/application-dev.yml` with your database credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/expenses
       username: your_username
       password: your_password
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

### Docker Build

Build a native image using GraalVM:

```bash
docker build -t expenses-api .

# Run the container
docker run -p 8081:8081 \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  expenses-api
```

## API Documentation

Once the application is running, access the API documentation at:
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

## Authentication

The API uses OAuth2 with JWT. To authenticate:

1. Obtain a token from the `/oauth2/token` endpoint
2. Include the token in the `Authorization` header as `Bearer <token>`

## Database Schema

The database schema is managed using Liquibase. All migrations are located in the `build.migrations/db` directory.

Key tables:
- `gastos`: Main expenses table
- `category`: Expense categories
- `currency`: Supported currencies

## Testing

Run the test suite:

```bash
./gradlew test
```

## Monitoring

Metrics are available at `/actuator/prometheus` and can be scraped by Prometheus.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

# Movement API

A robust and scalable RESTful API for managing personal finances, built with Spring Boot and designed for containerized deployment. Supports movements, subscriptions, incomes, and shared workspaces with real-time updates via WebSocket.

## Features

- **Expense Management**: Track expenses with categories, amounts, and dates
- **Multi-currency Support**: Handle expenses in different currencies (ARS, USD, EUR, CHF)
- **Credit/Debit Tracking**: Support for both credit and debit transactions
- **Shared Workspaces**: Invite members to shared workspaces with role-based access
- **Real-time Updates**: WebSocket (STOMP/SockJS) push for movements, subscriptions, and workspace events
- **User Authentication**: Secure API with Keycloak OAuth2 and JWT (RS256)
- **API Documentation**: Interactive documentation with Swagger/OpenAPI at `/docs`
- **Metrics & Monitoring**: Built-in support for Prometheus and Grafana
- **Container Ready**: Docker support for easy deployment
- **Database Migrations**: Liquibase for database versioning (`ddl-auto: none`)

## Tech Stack

- **Java 25** with **Spring Boot 4.0.2**
- **MySQL 8.0** Database
- **Liquibase** for database migrations
- **MapStruct** for object mapping
- **Spring Security** with OAuth2 / Keycloak JWT
- **Spring Web** for REST endpoints
- **Spring Data JPA** for data access
- **Spring AOP** for cross-cutting concerns (membership guard)
- **RabbitMQ** for async messaging
- **Caffeine** in-memory cache (currency exchange rates)
- **Micrometer** for application metrics
- **TestContainers** for integration testing
- **Spock** for testing

## Prerequisites

- Java 25 JDK
- Docker and Docker Compose (for containerized deployment)
- MySQL 8.0+ (or use the provided Docker Compose setup)
- Gradle 9+

## Getting Started

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/api-movements.git
   cd api-movements
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

The API uses Keycloak OAuth2 with JWT (RS256). To authenticate:

1. Obtain a token from your Keycloak realm
2. Include the token in the `Authorization` header as `Bearer <token>`

## Database Schema

The database schema is managed using Liquibase (`ddl-auto: none`). All migrations are located in `src/main/resources/db/changelog/`.

Key tables:
- `movements`: Main expenses/income table
- `workspaces`: Shared workspaces
- `workspace_members`: Workspace membership with roles
- `workspace_invitations`: Pending invitations
- `category`: Expense categories
- `currency`: Supported currencies
- `subscriptions`: Recurring subscriptions
- `income`: Income records

## Testing

Run the test suite:

```bash
./gradlew test
```

Run tests and checkstyle together:

```bash
./gradlew test checkstyleMain checkstyleTest
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

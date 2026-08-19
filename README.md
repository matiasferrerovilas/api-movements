# Movement API

A RESTful API for managing personal finances, built with Spring Boot and designed for containerized deployment. Supports movements, recurring income, subscriptions, budgets, savings goals, and shared workspaces with real-time updates via WebSocket. User identity and workspace membership are delegated to [api-identity](https://github.com/matiasferrerovilas/api-identity) — this service owns financial domain data only.

## Features

- **Movement tracking**: expenses/income/credit with categories, installments, multi-bank and multi-currency support
- **Bank statement import**: PDF parsing for BBVA and Galicia (Argentina), via a pluggable Strategy per bank
- **Recurring income & subscriptions**: fixed monthly income and recurring bills with payment tracking
- **Budgets**: per-category, per-currency budgets (monthly, annual, or one-time) with threshold-crossing alerts
- **Shared workspaces**: invite members with role-based access (`OWNER`/`COLLABORATOR`/`READ_ONLY`), delegated to api-identity
- **Real-time updates**: WebSocket (STOMP/SockJS) push for movements, subscriptions, budgets, invitations, and workspace events
- **User authentication**: Keycloak OAuth2 / JWT (RS256) resource server
- **API documentation**: interactive Swagger/OpenAPI UI
- **Metrics & monitoring**: built-in Prometheus support
- **Database migrations**: Liquibase (`ddl-auto: none`)

## Tech Stack

- **Java 25** with **Spring Boot 4.0.2**
- **MySQL 8.0** Database
- **Liquibase** for database migrations
- **MapStruct** for object mapping
- **Spring Security** with OAuth2 / Keycloak JWT
- **Spring Web** for REST endpoints
- **Spring Data JPA** for data access
- **Spring AOP** for cross-cutting concerns (membership guard)
- **RabbitMQ** for async messaging (consumes workspace-invitation events published by api-identity)
- **Caffeine** in-memory cache (currency exchange rates via [Frankfurter](https://frankfurter.dev))
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
   git clone https://github.com/matiasferrerovilas/api-movements.git
   cd api-movements
   ```

2. **Run api-identity** — this service calls out to api-identity for users/workspaces/membership on every relevant request (`identity.base-url`, defaults to `http://localhost:8082`), so it needs to be running alongside this one.

3. **Set up the database**
    - Create a MySQL database named `expenses`
    - Or use the provided docker-compose file:
      ```bash
      docker-compose up -d mysql
      ```

4. **Configure application properties**
   Create `src/main/resources/application-dev.yml` with your database credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/expenses
       username: your_username
       password: your_password
   ```

5. **Run the application**
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

### Demo Mode

A `demo` Spring profile seeds a few months of realistic demo data (categorized movements, a couple
of budgets, a subscription, recurring income entries, and savings goals) against a fixed,
suite-wide shared demo workspace id (`1`):

```bash
./gradlew bootRun --args='--spring.profiles.active=demo'
```

Notes:
- The seeder (`DemoDataSeeder`) only runs when the `demo` profile is active — it never runs in
  `dev`, `prod`, or the default profile.
- It's idempotent: restarting in `demo` profile does not duplicate data, it detects existing demo
  movements and skips seeding.
- It assumes workspace id `1` exists (api-identity's own `demo` profile creates that workspace
  record independently) — this service only inserts its own domain rows referencing that id, since
  api-movements' tables have no local foreign key to a workspaces table (workspace membership is
  delegated to api-identity).
- Config: `src/main/resources/application-demo.yaml` (same datasource/env-var shape as `dev`).

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

This service only owns financial domain data — workspaces, membership, and invitations live in api-identity's own database and are reached here through `IdentityClient`, not local tables.

Key tables:
- `movements`: expense/income/credit records, including installments
- `ingreso`: recurring fixed income
- `services`: recurring subscriptions/bills
- `budget`: per-category, per-currency budgets
- `banks`, `user_banks`: bank catalog and per-user bank associations
- `category`, `workspace_categories`: category catalog and per-workspace associations
- `currency`, `workspace_currencies`: currency catalog and per-workspace associations
- `monthly_summary_snapshot`: precomputed monthly aggregates used for reporting

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

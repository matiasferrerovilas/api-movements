# api-movements

Backend de gestión de finanzas personales para el mercado latinoamericano (Argentina). Permite registrar movimientos, suscripciones, ingresos y cuentas compartidas con actualizaciones en tiempo real vía WebSocket. Producción: `https://movement.eva-core.com`

## Tech Stack

| Area | Tecnología |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.2 (Gradle 9) |
| Database | MySQL 8 + Liquibase (`ddl-auto: none`) |
| ORM | Spring Data JPA / Hibernate |
| Auth | Keycloak — OAuth2 Resource Server, JWT RS256 |
| Messaging | RabbitMQ (AMQP) + WebSocket STOMP/SockJS |
| Mapping | MapStruct 1.6.3 (`componentModel = "spring"`) |
| Boilerplate | Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`) |
| Cache | Caffeine (in-memory, 5h TTL para currency) |
| PDF parsing | Apache PDFBox 3.0.6 |
| API docs | SpringDoc OpenAPI 3 (`/docs`) |
| Testing | Spock 2.4 + Testcontainers (MySQL) + Mockito |
| CI/CD | GitHub Actions → Docker Hub `mferrerovilas/api-movements` (ARM64) |

## Package Structure

```
api.m2.movements
├── controller/         REST controllers, uno por dominio, todos bajo /v1/*
├── entities/           JPA entities (Lombok @Data + @Builder)
├── enums/              Enums de dominio: AccountRole, CategoryEnum, EventType, etc.
├── exceptions/         BusinessException, EntityNotFoundException, PermissionDeniedException
├── helpers/            PDF parsers: PdfExtractprHelper (interface) + BBVA/Galicia impls + ParserRegistry
├── mappers/            MapStruct interfaces, 13 mappers, se componen entre sí
├── projections/        JPA interface projections (read-only, para queries livianas)
├── records/            DTOs como Java records, organizados por dominio
│   ├── accounts/
│   ├── balance/
│   ├── invite/
│   ├── movements/
│   ├── users/          UserBaseRecord, UserMeRecord
│   └── ...
├── repositories/       Spring Data JPA, todos extienden JpaRepository
├── security/           JwtAuthenticationConverter + SecurityConfiguration
└── services/           Lógica de negocio, organizada por dominio
    ├── balance/
    ├── groups/         AccountQueryService (reads) + GroupAddService (writes) + MembershipService
    ├── movements/      MovementAddService + MovementGetService + MovementFactory + file import strategies
    ├── publishing/
    │   ├── rabbit/     RabbitSocketMessageService (base) + MovementPublishServiceRabbit
    │   └── websockets/ WebSocketMessageService (base) + Movement/Account/ServicePublishServiceWebSocket
    └── user/           UserService + UserAddService
```

## Entities y Relaciones Clave

| Entity | Campos relevantes | Relaciones |
|---|---|---|
| `User` | `id: Long` (PK, auto-increment), `email`, `isFirstLogin`, `userType` | base de todo |
| `Account` | `id`, `name` | `owner → User`, `members → AccountMember[]` |
| `AccountMember` | `role: AccountRole` | `user → User`, `account → Account` |
| `AccountInvitation` | `status: InvitationStatus` | `user → User` (invitado), `invitedBy → User`, `account → Account` |
| `Movement` | `amount`, `date`, `type`, `description`, `cuotaActual/Total` | `owner → User`, `account → Account`, `category`, `currency`, `bank` |
| `Income` | `amount` | `user → User`, `bank → Bank`, `currency`, `account → Account` |
| `Subscription` | `description`, `amount`, `lastPayment`, `@Transient isPaid()` | `owner → User`, `account → Account`, `currency` |
| `UserSetting` | `settingKey: UserSettingKey`, `settingValue: Long` | `user → User` |

> **IMPORTANTE:** `User.id` es un `Long` auto-incremental de DB. El Keycloak subject (`sub` claim del JWT) es un UUID `String` separado. No son intercambiables.

## API Endpoints

### `GET /v1/users/me`
Retorna `UserMeRecord { id: Long, email, isFirstLogin, userType }`. Si el usuario no existe en DB (nuevo): `{ id: null, email: null, isFirstLogin: true, userType: null }`.

### `POST /v1/onboarding` — crea usuario, cuentas e ingreso inicial
### `GET /v1/expenses` — movimientos paginados con filtros
### `POST /v1/expenses` — crear movimiento
### `POST /v1/expenses/import-file` — importar movimientos desde PDF bancario
### `PATCH /v1/expenses/{id}` — actualización parcial (MapStruct `IGNORE` null)
### `DELETE /v1/expenses/{id}`
### `GET /v1/balance` — balance total (INGRESO/GASTO)
### `GET /v1/balance/category` — balance por categoría
### `GET /v1/balance/group` — balance por cuenta
### `GET /v1/balance/monthly-evolution` — evolución mensual por moneda
### `POST /v1/account` — crear cuenta/grupo
### `GET /v1/account/membership` — membresías del usuario
### `GET /v1/account/count` — cuentas con cantidad de miembros
### `DELETE /v1/account/{accountId}` — salir de una cuenta
### `POST /v1/account/{id}/invitations` — invitar usuarios por email
### `GET /v1/account/invitations` — invitaciones pendientes del usuario
### `PATCH /v1/account/invitations/{invitationId}` — aceptar/rechazar invitación
### `PATCH /v1/account/{id}/default` — setear cuenta por defecto
### `GET /v1/income` / `POST` / `DELETE /{id}` / `POST /{id}/reload`
### `GET /v1/subscriptions` / `POST` / `PATCH /{id}/payment` / `PATCH /{id}` / `DELETE /{id}`
### `GET /v1/settings/defaults` / `GET /{key}` / `PUT /{key}` / `GET /last-ingreso`
### `GET /v1/categories` / `GET /v1/currency` / `GET /v1/banks`

## WebSocket Topics (STOMP)

Endpoint SockJS: `/ws`. Todos los mensajes van envueltos en `EventWrapper<T> { eventType: EventType, message: T }`.

**CRÍTICO — hay dos tipos de ID usados en los topics:**
- Topics de **movimientos/servicios/cuentas**: usan `accountId` (Long, PK de `Account`)
- Topics de **invitaciones**: usan `userId` (Long, PK de `User` en DB — NO el Keycloak subject)
- Topic de **default account**: usa el Keycloak `sub` UUID (String) — el único que usa el subject

| Topic | EventType | Payload | Cuándo |
|---|---|---|---|
| `/topic/movimientos/{accountId}/new` | `MOVEMENT_ADDED` | `MovementRecord` | Movimiento creado o importado por PDF |
| `/topic/movimientos/{accountId}/delete` | `MOVEMENT_DELETED` | `Long` (movementId) | Movimiento eliminado |
| `/topic/servicios/{accountId}/new` | `SERVICE_PAID` | `SubscriptionRecord` | Suscripción creada |
| `/topic/servicios/{accountId}/update` | `SERVICE_PAID` / `SERVICE_UPDATED` | `SubscriptionRecord` | Suscripción pagada o actualizada |
| `/topic/servicios/{accountId}/remove` | `SERVICE_DELETED` | `SubscriptionRecord` | Suscripción eliminada |
| `/topic/invitation/{userId}/new` | `INVITATION_ADDED` | `InvitationToGroupRecord` | Invitación enviada — `userId` = `User.id` (Long DB) |
| `/topic/invitation/{userId}/update` | `INVITATION_CONFIRMED_REJECTED` | `InvitationToGroupRecord` | Invitación aceptada/rechazada — `userId` = `User.id` (Long DB) |
| `/topic/account/{ownerId}/new` | `ACCOUNT_CREATED` | `GroupRecord` | Cuenta creada — `ownerId` = `User.id` (Long DB) |
| `/topic/account/default/{keycloakSubject}` | `MEMBERSHIP_UPDATED` | `GroupRecord` | Default account cambiada — usa Keycloak `sub` UUID |

Todos los publishers usan `@TransactionalEventListener(phase = AFTER_COMMIT)` — el WS push solo ocurre si el commit fue exitoso.

## Seguridad

- **Principal:** el claim `preferred_username` del JWT (generalmente el email) se mapea a `Authentication.getName()`. Con ese email se busca el `User` en DB.
- **Keycloak subject:** disponible vía `userService.getCurrentKeycloakId()` → `JwtAuthenticationToken.getToken().getSubject()`. Se usa **solo** para el topic `/topic/account/default/{id}`.
- **Roles:** se leen de `realm_access.roles[]` del JWT. Prefijo `ROLE_` requerido.
- **Rutas públicas:** `/swagger-ui/**`, `/v3/api-docs/**`, `/ws/**`
- **Rutas con rol:** `/v1/onboarding/**` requiere `ADMIN`, `FAMILY` o `GUEST`
- **Resto:** cualquier JWT válido
- **CORS:** `https://movement.eva-core.com`, `http://localhost:5173`, `http://localhost:8081`

## Patrones de Diseño

### Service Splitting
Cada dominio tiene servicios separados por responsabilidad: `*AddService` (escritura), `*GetService` / `*QueryService` (lectura), `*Factory` (construcción de entidades). No existe un `MovementService` monolítico.

### Factory + Resolver
`MovementFactory` construye la entidad `Movement` resolviendo todas las FKs (category, currency, bank, user, account) a través de `CategoryResolver` y `CurrencyResolver`. `CurrencyResolver` usa cache Caffeine.

### Strategy (File Import)
`ExpenseFileStrategy` es clase abstracta. `BBVACreditImportService` y `GaliciaCreditImportService` se registran como beans. `MovementImportFileService` despacha por `match(bank)`.

### Event-Driven WebSocket
Los servicios de escritura publican Spring Application Events dentro de `@Transactional`. Los publishers WS escuchan con `@TransactionalEventListener(phase = AFTER_COMMIT)`.

### MapStruct Conventions
- Todos los mappers usan `componentModel = "spring"`
- Se componen entre sí (ej: `MovementMapper` usa `CategoryMapper`, `CurrencyMapper`, `UserMapper`)
- Actualizaciones parciales: `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` en métodos `update*()`

## Convenciones del Proyecto

- **DTOs como Java records** — nunca clases para request/response. Organizados en `records/` por subpaquete de dominio.
- **Dominio en español** — enums, campos y comentarios en español (mercado argentino): `INGRESO`, `GASTO`, `HOGAR`, etc.
- **Liquibase es dueño del schema** — Hibernate tiene `ddl-auto: none`. Nunca modificar el schema sin un nuevo changeset en `db/changelog/`.
- **Sin prefix global en controllers** — cada controller declara su propio `/v1/*` en `@RequestMapping`.
- **Audit fields vía Hibernate** — `@CreationTimestamp` / `@UpdateTimestamp`, no Spring Data `@CreatedDate`.
- **Enums como seeds de categorías y grupos** — `CategoryEnum` y `GroupsEnum` definen los valores iniciales. `CategoryResolver` usa `SIN_CATEGORIA` como fallback.
- **`@EqualsAndHashCode` + `@ToString` en entidades bidireccionales** — para evitar recursión infinita (Account, AccountMember).
- **RabbitMQ** — exchange `movement.topic`, routing key `n8n.import.file` → integración con n8n para importación de archivos. El listener de respuesta está en el código pero comentado.
- **Testing** — Spock con Groovy para specs, Testcontainers para tests de integración con MySQL real.

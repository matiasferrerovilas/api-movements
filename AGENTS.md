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
| `UserBank` | `createdAt` | `user → User`, `bank → Bank` |
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
### `GET /v1/banks`
Retorna los bancos asociados al usuario autenticado (tabla `user_banks`). Lista vacía si no tiene ninguno.

### `POST /v1/banks` — agrega banco al usuario
Body: `{ "description": "galicia" }`. Sanitiza (trim + uppercase) antes de buscar en `banks`. Si el banco no existe lo crea. Si el usuario ya lo tiene, es idempotente. Retorna `BankRecord`.

### `DELETE /v1/banks/{id}` — quita banco de la lista del usuario
`{id}` es el `bank_id`. Lanza 404 si el banco no está en la lista del usuario. El banco sigue existiendo en el catálogo global.

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

### AOP — Membership Guard (`@RequiresMembership`)
Todo método de mutación (update, delete, pay, reload) que opere sobre un recurso perteneciente a una cuenta compartida **debe** anotarse con `@RequiresMembership`. El aspecto `MembershipCheckAspect` intercepta la llamada, resuelve el `accountId` del recurso y verifica que el usuario autenticado sea miembro antes de ejecutar el método.

```java
// El parámetro id está en el índice 0 por defecto
@RequiresMembership(domain = MembershipDomain.INCOME)
public void deleteIncome(Long id) { ... }

// Cuando el id NO está en el primer parámetro, indicar el índice
@RequiresMembership(domain = MembershipDomain.MOVEMENT, idParamIndex = 1)
public void updateMovement(@Valid ExpenseToUpdate dto, Long id) { ... }
```

**Dominios disponibles:** `MOVEMENT`, `INCOME`, `SUBSCRIPTION`. Si se agrega un nuevo dominio, extender el enum `MembershipDomain` y el switch en `MembershipCheckAspect.resolveAccountId()`.

### MapStruct Conventions
- Todos los mappers usan `componentModel = "spring"`
- Se componen entre sí (ej: `MovementMapper` usa `CategoryMapper`, `CurrencyMapper`, `UserMapper`)
- Actualizaciones parciales: `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` en métodos `update*()`

### Mappers en tests — nunca mockear
Los mappers **nunca** se mockean con `Mock()` ni `Stub()` en tests unitarios. Siempre se instancian reales:

- **Mapper sin dependencias** (`uses = {}` vacío o ausente): `Mappers.getMapper(XxxMapper.class)`
- **Mapper con dependencias** (`uses = {OtroMapper.class, ...}`): `new XxxMapperImpl()` + `ReflectionTestUtils.setField()` para cada campo inyectado

MapStruct con `componentModel = "spring"` genera field injection (`@Autowired`), no constructor injection. Por eso `Mappers.getMapper()` solo funciona para mappers sin dependencias.

```groovy
// Mapper self-contained
BankMapper bankMapper = Mappers.getMapper(BankMapper)

// Mapper con dependencias
MovementMapper movementMapper

def setup() {
    movementMapper = new MovementMapperImpl()
    ReflectionTestUtils.setField(movementMapper, "categoryMapper", Mappers.getMapper(CategoryMapper))
    ReflectionTestUtils.setField(movementMapper, "currencyMapper", Mappers.getMapper(CurrencyMapper))
    ReflectionTestUtils.setField(movementMapper, "userMapper", Mappers.getMapper(UserMapper))
}
```

Como consecuencia, las entidades pasadas al mapper deben tener todos los campos que este accede construidos con `Entity.builder()...build()` — no con `Stub()`. Los stubs de retorno tipo `mapperX.toRecord(...) >> someRecord` deben eliminarse.

## Entorno de Desarrollo

- **IDE:** IntelliJ IDEA con Gradle como build tool.
- **MapStruct + Lombok:** el procesamiento de anotaciones lo maneja Gradle. No hace falta crear ni configurar `.factorypath` manualmente — IntelliJ lo resuelve solo al sincronizar el proyecto con Gradle.
- **Archivos de IDE:** nunca crear ni modificar archivos de proyecto del IDE (`.project`, `.classpath`, `.settings/`, `.idea/`, `*.iml`, etc.). Estos son gestionados por el IDE y no deben tocarse.

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

## Testing

### TDD obligatorio
Todo componente nuevo (service, resolver, factory, helper) debe tener su test unitario escrito junto con el código de producción. No se acepta código nuevo sin cobertura de casos principales.

### Ubicación y naming
- Servicios: `src/test/groovy/api/m2/movements/unit/services/`
- Clases utilitarias / resolvers: `src/test/groovy/api/m2/movements/unit/unit/`
- Nombre del archivo: `{NombreClase}Test.groovy` — mismo nombre que la clase Java bajo test

### Reglas generales
- **Sin `@SpringBootTest`** — los tests unitarios son puramente en memoria, sin contexto Spring
- **Instanciación manual** — el servicio bajo test se crea via constructor en el bloque `setup()`, inyectando los mocks/stubs como argumentos
- **`Mock()` vs `Stub()`**:
  - `Mock()` — cuando hay que verificar que una interacción ocurrió (`1 * service.method(...)`)
  - `Stub()` — cuando solo se necesita un valor de retorno y no importa si/cuánto se llama
- **Estructura `given/when/then`** — obligatoria en todos los tests
- **Nombres de test en inglés** — formato: `"metodo - should comportamiento"` (ej: `"addIngreso - should save movement with correct parameters"`)
- **Wildcards tipados en mocks** — al usar `_` como matcher de argumento en interacciones o stubs, siempre especificar el tipo: `_ as String`, `_ as MovementToAdd`, etc. Nunca dejar `_` sin tipo cuando el tipo es conocido.

### Ejemplo canónico
`src/test/groovy/api/m2/movements/unit/services/SettingServiceTest.groovy`

```groovy
class SettingServiceTest extends Specification {

    MovementAddService movementAddService = Mock(MovementAddService)
    AccountQueryService accountQueryService = Mock(AccountQueryService)

    SettingService service

    def setup() {
        service = new SettingService(movementAddService, accountQueryService)
    }

    def "addIngreso - should save movement with correct parameters"() {
        given:
        def incomeToAdd = new IncomeToAdd("GALICIA", "EUR", new BigDecimal("1000.00"), "Mi grupo")
        accountQueryService.findAccountByName("Mi grupo") >> Stub(Account) { getId() >> 1L }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.amount() == new BigDecimal("1000.00")
            assert m.type()   == MovementType.INGRESO.name()
        }
    }
}
```

# Design: Exception Standardization, Subscription Decoupling & Income Deduplication

**Date:** 2026-06-02

---

## Topic 1 — Exception Standardization

### Goal

Eliminate all uses of `IllegalArgumentException`, `IllegalStateException`, and other non-domain exceptions from business code. Every thrown exception must map to a class in the `DomainException` sealed hierarchy.

### New class: `ServiceException`

```java
package api.m2.movements.exceptions;

public final class ServiceException extends DomainException {
    public ServiceException(String message) {
        super(message);
    }
}
```

- Added to `DomainException`'s `permits` clause
- Represents internal/programming errors: misconfiguration, missing registered implementations
- HTTP response: **500 Internal Server Error**
- Handler added to `ErrorHandler` with `log.error` (not `log.warn`)

### Replacement map

| File | Old exception | New exception | Reason |
|---|---|---|---|
| `ParserRegistry.init()` — duplicate parser | `IllegalStateException` | `ServiceException` | Startup config error |
| `WorkspaceIdResolverRegistry` — no resolver | `IllegalArgumentException` | `ServiceException` | Missing dev implementation |
| `ParserRegistry.getParser()` — bank not found | `IllegalArgumentException` | `BusinessException` | User sent invalid bank |
| `MovementImportFileService` — 0 or multiple strategies | `IllegalArgumentException` | `BusinessException` | Invalid user input |
| `UserService.getCurrentKeycloakId()` — no JWT | `IllegalStateException` | `PermissionDeniedException` | Authentication concern |
| `WorkspaceQueryService` — empty workspace name | `IllegalArgumentException` | `BusinessException` | Input validation |

### ErrorHandler

- Add `@ExceptionHandler(ServiceException.class)` → HTTP 500, `log.error`
- Keep `@ExceptionHandler(IllegalArgumentException.class)` as safety net for Spring/framework-thrown exceptions

### Tests

- Existing tests that expect `IllegalArgumentException` or `IllegalStateException` from the above locations must be updated to expect the new exception type.

---

## Topic 2 — Subscription Decoupling via Events

### Goal

Remove direct dependencies on `MovementAddService` and `MovementRepository` from `SubscriptionAddService`. Use Spring `ApplicationEventPublisher` + synchronous `@EventListener` to keep the same transactional guarantees.

### New event records

```
records/subscriptions/SubscriptionPaidEvent.java
records/subscriptions/SubscriptionMovementSyncEvent.java
```

**`SubscriptionPaidEvent`** — carries all data needed to create the movement:
```java
public record SubscriptionPaidEvent(
    BigDecimal amount,
    LocalDate paymentDate,
    String description,
    String currencySymbol,
    User owner,
    Workspace workspace
) {}
```

**`SubscriptionMovementSyncEvent`** — carries all data needed to find and update the associated movement:
```java
public record SubscriptionMovementSyncEvent(
    String oldDescription,
    Long workspaceId,
    int year,
    int month,
    BigDecimal newAmount,
    String newDescription,
    LocalDate newDate
) {}
```

### New class: `SubscriptionMovementHandler`

```
services/subscriptions/SubscriptionMovementHandler.java
```

- `@Component`, `@RequiredArgsConstructor`
- Dependencies: `MovementAddService`, `CategoryAddService`, `UserSettingService`, `SyncMovementsService`
- Handles `SubscriptionPaidEvent` with `@EventListener` → constructs `MovementToAdd`, calls `movementAddService.saveMovement(dto, workspace, owner)`
- Handles `SubscriptionMovementSyncEvent` with `@EventListener` → delegates to `syncMovementsService.syncSubscriptionMovement(...)`

### New class: `SyncMovementsService`

```
services/movements/SyncMovementsService.java
```

- `@Service`, `@RequiredArgsConstructor`
- Dependencies: `MovementRepository`
- Single method: `syncSubscriptionMovement(String oldDesc, Long workspaceId, int year, int month, BigDecimal newAmount, String newDesc, LocalDate newDate)`
- Encapsulates: `movementRepository.findByDescriptionAndAccountAndMonth(...)` + field updates + `movementRepository.save(movement)`

### Updated `SubscriptionAddService`

Remove:
- `MovementRepository` dependency
- `MovementAddService` dependency
- `CategoryAddService` dependency
- `UserSettingService` dependency
- `addMovementForSubscription(Subscription)` method (moves to handler)
- `syncAssociatedMovement(...)` method (moves to SyncMovementsService via handler)

Add:
- `ApplicationEventPublisher` dependency
- Publish `SubscriptionPaidEvent` where `addMovementForSubscription` was called
- Publish `SubscriptionMovementSyncEvent` where `syncAssociatedMovement` was called

### Transaction guarantee

Both listeners use `@EventListener` (not `@TransactionalEventListener`), so they run synchronously within the same transaction as the publisher. If the listener fails, the full transaction rolls back — same behavior as before.

### Tests

- `SubscriptionAddServiceTest`: remove movement-related mock verifications, replace with `1 * applicationEventPublisher.publishEvent(_ as SubscriptionPaidEvent)`
- New `SubscriptionMovementHandlerTest`: verify `movementAddService.saveMovement(...)` is called with correct data
- New `SyncMovementsServiceTest`: verify find + update + save behavior on the movement

---

## Topic 3 — IncomeAddService Deduplication

### Goal

Extract the repeated `MovementToAdd` construction into a single private method.

### Change

Add private method to `IncomeAddService`:

```java
private MovementToAdd buildIncomeMovement(BigDecimal amount, String currencySymbol,
                                          String bankDescription, String description) {
    return new MovementToAdd(
        amount,
        LocalDate.now(ZoneOffset.UTC),
        description,
        DefaultCategory.HOGAR.getDescription(),
        MovementType.INGRESO.name(),
        currencySymbol,
        null,
        null,
        bankDescription
    );
}
```

Replace the inline construction in:
- `addIngreso()` → `buildIncomeMovement(dto.amount(), currency.getSymbol(), dto.bank(), "Sueldo Recibido")`
- `reloadIncome()` → `buildIncomeMovement(income.getAmount(), income.getCurrency().getSymbol(), income.getBank().getDescription(), "Ingreso")`
- `generateRecurringIncomeForUser()` → `buildIncomeMovement(income.getAmount(), income.getCurrency().getSymbol(), income.getBank().getDescription(), "Ingreso recurrente")`

The `saveMovement(dto, workspace, user)` overload in `generateRecurringIncomeForUser` remains as-is since it needs explicit workspace/user.

### Tests

No changes needed — existing tests cover behavior, not construction details.

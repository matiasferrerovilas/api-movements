package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.MovementType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.ExchangeRateNotFoundException;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.services.currencies.ExchangeRateResolver;
import api.m2.movements.services.category.CategoryResolver;
import api.m2.movements.services.currencies.CurrencyResolver;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementFactory {
    private final CategoryResolver categoryResolver;
    private final CurrencyResolver currencyResolver;
    private final UserService userService;
    private final MovementMapper movementMapper;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceQueryService workspaceQueryService;
    private final BankRepository bankRepository;
    private final ExchangeRateResolver exchangeRateResolver;

    /** Único de los 3 overloads alcanzable directamente desde un request de usuario (ver
     * MovementAddService.saveMovement(dto)) — por eso es el único que valida rol de escritura.
     * Los otros dos reciben el workspaceId ya resuelto por un caller interno de confianza
     * (onboarding, jobs programados), nunca por un READ_ONLY intentando crear algo. */
    public Movement create(MovementToAdd dto) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        workspaceQueryService.verifyCanWrite(workspaceId);
        return this.create(dto, workspaceId);
    }

    public Movement create(MovementToAdd dto, Long workspaceId) {
        return this.create(dto, workspaceId, userService.getMe().id());
    }

    public Movement create(MovementToAdd dto, Long workspaceId, Long ownerId) {
        var movement = movementMapper.toEntity(dto);

        movement.setWorkspaceId(workspaceId);
        movement.setCategories(new HashSet<>(categoryResolver.resolveAll(dto.categories(), workspaceId)));
        var currency = currencyResolver.resolve(dto.currency(), workspaceId);
        movement.setCurrency(currency);
        movement.setOwnerId(ownerId);

        if (dto.bank() != null) {
            var bank = bankRepository.findByDescription(dto.bank())
                    .orElseThrow(() -> new EntityNotFoundException("Banco no encontrado: " + dto.bank()));
            movement.setBank(bank);
        }

        movement.setExchangeRate(this.resolveExchangeRateOrNull(currency.getSymbol(), dto.date()));
        movement.setLastCreditPayment(this.resolveLastCreditPayment(dto));

        return movement;
    }

    /** Solo aplica a CREDITO. Si el caller ya trae el valor (el cron de cuotas, copiando el de la
     * cuota anterior) se usa tal cual, sin recalcular — así el valor queda estable durante todo
     * el plan de cuotas en vez de recomputarse cada mes desde una fecha distinta. Si no viene, se
     * calcula desde esta cuota: fecha + (cuotasTotales - cuotaActual) meses, la fecha de la
     * última cuota del plan — funciona tanto para la primera cuota (cuotaActual=1) como para un
     * import de PDF que arranca a mitad de plan (cuotaActual=N), porque el resultado es el mismo
     * sin importar desde qué cuota se calcule. */
    private LocalDate resolveLastCreditPayment(MovementToAdd dto) {
        if (dto.lastCreditPayment() != null) {
            return dto.lastCreditPayment();
        }
        if (!MovementType.CREDITO.name().equals(dto.type()) || dto.cuotaActual() == null || dto.cuotasTotales() == null) {
            return null;
        }
        return dto.date().plusMonths(dto.cuotasTotales() - dto.cuotaActual());
    }

    private BigDecimal resolveExchangeRateOrNull(String symbol, LocalDate date) {
        try {
            return exchangeRateResolver.resolveRate(symbol, date);
        } catch (ExchangeRateNotFoundException e) {
            log.warn("No se pudo resolver tasa de cambio para {} en {}, se guarda sin tasa: {}",
                    symbol, date, e.getMessage());
            return null;
        }
    }

    public void applyUpdates(ExpenseToUpdate dto, Movement movement) {
        if (dto.currency() != null) {
            movement.setCurrency(currencyResolver.resolve(dto.currency(), movement.getWorkspaceId()));
        }
        if (dto.categories() != null) {
            movement.setCategories(new HashSet<>(categoryResolver.resolveAll(dto.categories())));
        }
    }
}


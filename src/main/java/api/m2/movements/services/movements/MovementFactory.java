package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementFactory {
    private final CategoryResolver categoryResolver;
    private final CurrencyResolver currencyResolver;
    private final UserService userService;
    private final MovementMapper movementMapper;
    private final WorkspaceContextService workspaceContextService;
    private final BankRepository bankRepository;
    private final ExchangeRateResolver exchangeRateResolver;

    public Movement create(MovementToAdd dto) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return this.create(dto, workspaceId);
    }

    public Movement create(MovementToAdd dto, Long workspaceId) {
        return this.create(dto, workspaceId, userService.getMe().id());
    }

    public Movement create(MovementToAdd dto, Long workspaceId, Long ownerId) {
        var movement = movementMapper.toEntity(dto);

        movement.setWorkspaceId(workspaceId);
        movement.setCategory(categoryResolver.resolve(dto.category(), workspaceId));
        var currency = currencyResolver.resolve(dto.currency(), workspaceId);
        movement.setCurrency(currency);
        movement.setOwnerId(ownerId);

        if (dto.bank() != null) {
            var bank = bankRepository.findByDescription(dto.bank())
                    .orElseThrow(() -> new EntityNotFoundException("Banco no encontrado: " + dto.bank()));
            movement.setBank(bank);
        }

        movement.setExchangeRate(this.resolveExchangeRateOrNull(currency.getSymbol(), dto.date()));

        return movement;
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
        if (dto.category() != null) {
            movement.setCategory(categoryResolver.resolve(dto.category()));
        }
    }
}


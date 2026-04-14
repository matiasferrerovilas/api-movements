package api.m2.movements.services.movements;

import api.m2.movements.entities.Movement;
import api.m2.movements.entities.Workspace;
import api.m2.movements.exceptions.EntityNotFoundException;
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
        var workspace = workspaceContextService.getActiveWorkspace();
        return this.create(dto, workspace);
    }

    public Movement create(MovementToAdd dto, Workspace workspace) {
        var movement = movementMapper.toEntity(dto);

        movement.setWorkspace(workspace);

        movement.setCategory(categoryResolver.resolve(dto.category(), workspace));
        var currency = currencyResolver.resolve(dto.currency());
        movement.setCurrency(currency);

        movement.setOwner(userService.getAuthenticatedUser());

        if (dto.bank() != null) {
            var bank = bankRepository.findByDescription(dto.bank())
                    .orElseThrow(() -> new EntityNotFoundException("Banco no encontrado: " + dto.bank()));
            movement.setBank(bank);
        }

        movement.setExchangeRate(
                exchangeRateResolver.resolveRate(currency.getSymbol(), dto.date())
        );

        return movement;
    }

    public void applyUpdates(ExpenseToUpdate dto, Movement movement) {
        if (dto.currency() != null) {
            movement.setCurrency(currencyResolver.resolve(dto.currency()));
        }
        if (dto.category() != null) {
            movement.setCategory(categoryResolver.resolve(dto.category()));
        }
    }
}


package api.m2.movements.services.movements;

import api.m2.movements.entities.Movement;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.services.currencies.ExchangeRateResolver;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import api.m2.movements.services.category.CategoryResolver;
import api.m2.movements.services.currencies.CurrencyResolver;
import api.m2.movements.services.user.UserService;
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
    private final WorkspaceQueryService workspaceQueryService;
    private final BankRepository bankRepository;
    private final ExchangeRateResolver exchangeRateResolver;

    public Movement create(MovementToAdd dto) {

        var movement = movementMapper.toEntity(dto);

        movement.setCategory(categoryResolver.resolve(dto.category()));
        var currency = currencyResolver.resolve(dto.currency());
        movement.setCurrency(currency);

        movement.setOwner(userService.getAuthenticatedUser());
        var account = workspaceQueryService.findWorkspaceById(dto.workspaceId());
        movement.setWorkspace(account);

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
        if (dto.currency() != null) movement.setCurrency(currencyResolver.resolve(dto.currency()));
        if (dto.category() != null) movement.setCategory(categoryResolver.resolve(dto.category().description()));
    }

}


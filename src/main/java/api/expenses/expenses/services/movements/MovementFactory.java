package api.expenses.expenses.services.movements;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.services.accounts.AccountQueryService;
import api.expenses.expenses.services.category.CategoryResolver;
import api.expenses.expenses.services.currencies.CurrencyResolver;
import api.expenses.expenses.services.user.UserService;
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
    private final AccountQueryService accountQueryService;
    public Movement create(MovementToAdd dto) {

        var movement = movementMapper.toEntity(dto);

        movement.setCategory(categoryResolver.resolve(dto.category()));
        movement.setCurrency(currencyResolver.resolve(dto.currency()));

        movement.setYear(movement.getDate().getYear());
        movement.setMonth(movement.getDate().getMonthValue());

        movement.setOwner(userService.getAuthenticatedUser());
        var account = accountQueryService.findAccountByName(dto.group());
        movement.setAccount(account);

        return movement;
    }

    public void applyUpdates(ExpenseToUpdate dto, Movement movement) {
        if (dto.currency() != null) movement.setCurrency(currencyResolver.resolve(dto.currency()));
        if (dto.category() != null) movement.setCategory(categoryResolver.resolve(dto.category().description()));
    }

}

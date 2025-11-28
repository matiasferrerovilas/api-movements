package api.expenses.expenses.services.movements;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.exceptions.BusinessException;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.services.category.CategoryResolver;
import api.expenses.expenses.services.currencies.CurrencyResolver;
import api.expenses.expenses.services.groups.GroupResolver;
import api.expenses.expenses.services.movements.types.ExpenseStrategy;
import api.expenses.expenses.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementFactory {
    private final Set<ExpenseStrategy> strategies;
    private final CategoryResolver categoryResolver;
    private final CurrencyResolver currencyResolver;
    private final GroupResolver groupResolver;
    private final UserService userService;

    public Movement create(MovementToAdd dto) {

        var strategy = this.resolveStrategy(dto.type());
        var movement = strategy.process(dto);

        movement.setCategory(categoryResolver.resolve(dto.category()));
        movement.setCurrency(currencyResolver.resolve(dto.currency()));

        movement.setYear(movement.getDate().getYear());
        movement.setMonth(movement.getDate().getMonthValue());

        movement.setUsers(userService.getAuthenticatedUser());
        movement.setUserGroups(groupResolver.resolve(dto.group()));

        return movement;
    }

    public void applyUpdates(ExpenseToUpdate dto, Movement movement) {
        if (dto.currency() != null)
            movement.setCurrency(currencyResolver.resolve(dto.currency()));
        if (dto.category() != null)
            movement.setCategory(categoryResolver.resolve(dto.category().description()));
    }

    private ExpenseStrategy resolveStrategy(String type) {
        var mt = MovementType.valueOf(type.toUpperCase());

        return strategies.stream()
                .filter(st -> st.match(mt))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Invalid payment method: " + type));
    }
}

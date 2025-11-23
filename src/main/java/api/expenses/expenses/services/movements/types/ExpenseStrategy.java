package api.expenses.expenses.services.movements.types;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.movements.MovementToAdd;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ExpenseStrategy {
    protected final MovementMapper movementMapper;

    public abstract boolean match(MovementType paymentMethod);
    @Transactional
    public abstract Movement process(MovementToAdd movementToAdd);
}

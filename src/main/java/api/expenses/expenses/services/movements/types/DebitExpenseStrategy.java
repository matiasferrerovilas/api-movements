package api.expenses.expenses.services.movements.types;

import api.expenses.expenses.entities.Debito;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.movements.MovementToAdd;
import org.springframework.stereotype.Service;

@Service
public class DebitExpenseStrategy extends ExpenseStrategy {

    public DebitExpenseStrategy(MovementMapper movementMapper) {
        super(movementMapper);
    }

    @Override
    public boolean match(MovementType paymentMethod) {
        return MovementType.DEBITO.equals(paymentMethod);
    }

    @Override
    public Debito process(MovementToAdd movementToAdd) {
        return movementMapper.toDebito(movementToAdd);
    }
}

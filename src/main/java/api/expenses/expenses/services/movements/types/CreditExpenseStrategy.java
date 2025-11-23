package api.expenses.expenses.services.movements.types;

import api.expenses.expenses.entities.Credito;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.movements.MovementToAdd;
import org.springframework.stereotype.Service;

@Service
public class CreditExpenseStrategy extends ExpenseStrategy {


    public CreditExpenseStrategy(MovementMapper movementMapper) {
        super(movementMapper);
    }

    @Override
    public boolean match(MovementType paymentMethod) {
        return MovementType.CREDITO.equals(paymentMethod);
    }

    @Override
    public Credito process(MovementToAdd movementToAdd) {
        return movementMapper.toCredito(movementToAdd);
    }
}

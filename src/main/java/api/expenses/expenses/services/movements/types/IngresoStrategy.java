package api.expenses.expenses.services.movements.types;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.movements.MovementToAdd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngresoStrategy extends ExpenseStrategy {
    public IngresoStrategy(MovementMapper movementMapper) {
        super(movementMapper);
    }

    @Override
    public boolean match(MovementType paymentMethod) {
        return MovementType.INGRESO.equals(paymentMethod);
    }

    @Override
    public Movement process(MovementToAdd movementToAdd) {
        return movementMapper.toIngreso(movementToAdd);
    }
}

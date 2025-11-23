package api.expenses.expenses.entities;

import api.expenses.expenses.enums.MovementType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("INGRESO")
@Data
@SuperBuilder
@NoArgsConstructor
public class Ingreso extends Movement {
    @Override
    public MovementType getType() {
        return MovementType.INGRESO;
    }
}

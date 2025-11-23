package api.expenses.expenses.entities;

import api.expenses.expenses.enums.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("CREDITO")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Credito extends Movement {
    @Column(nullable = false, length = 30)
    private Integer cuotaActual;
    @Column(nullable = false, length = 30)
    private Integer cuotasTotales;

    @Override
    public MovementType getType() {
        return MovementType.CREDITO;
    }
}

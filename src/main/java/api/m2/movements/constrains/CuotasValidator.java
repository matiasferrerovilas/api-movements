package api.m2.movements.constrains;

import api.m2.movements.enums.MovementType;
import api.m2.movements.records.movements.MovementToAdd;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CuotasValidator implements ConstraintValidator<ValidCuotas, MovementToAdd> {

    @Override
    public boolean isValid(MovementToAdd value, ConstraintValidatorContext context) {
        if (value == null || value.type() == null) {
            return true;
        }

        boolean isCredito = MovementType.CREDITO.name().equals(value.type());

        if (!isCredito) {
            return true;
        }

        Integer cuotaActual = value.cuotaActual();
        Integer cuotasTotales = value.cuotasTotales();

        if (cuotaActual == null || cuotasTotales == null) {
            return false;
        }

        return cuotaActual > 0 && cuotasTotales > 0 && cuotaActual <= cuotasTotales;
    }
}

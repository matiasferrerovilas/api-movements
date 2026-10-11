package api.m2.movements.constraints;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import api.m2.movements.enums.MovementType;
import api.m2.movements.records.movements.MovementToAdd;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
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

package api.m2.movements.configuration;

import api.m2.movements.constraints.CuotasValidator;
import api.m2.movements.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.movements.records.movements.MovementSearchFilterRecord;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Bajo native-image, los records bindeados desde query params vía
 * {@code @ParameterObject} (DataBinder.construct) necesitan sus accessors
 * de record component registrados explícitamente para reflection, y los
 * {@code ConstraintValidator} custom necesitan su constructor registrado
 * para que {@code SpringConstraintValidatorFactory} pueda instanciarlos.
 */
public class WebBindingRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(BalanceFilterRecord.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS)
                .registerType(MovementSearchFilterRecord.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS)
                .registerType(CuotasValidator.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}

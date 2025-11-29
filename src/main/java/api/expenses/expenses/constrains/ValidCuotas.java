package api.expenses.expenses.constrains;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CuotasValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCuotas {
    String message() default "La cuota actual no puede ser mayor que las cuotas totales";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
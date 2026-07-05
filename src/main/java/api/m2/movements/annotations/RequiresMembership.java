package api.m2.movements.annotations;

import api.m2.movements.movements.enums.MembershipDomain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Verifica, antes de ejecutar el método, que el usuario autenticado sea miembro
 * del workspace al que pertenece el recurso identificado por el parámetro en
 * {@code idParamIndex}. La verificación real la hace {@code MembershipCheckAspect}
 * vía un proxy estándar de Spring AOP (no hay weaving de AspectJ configurado).
 *
 * <p><b>Trampa de self-invocation:</b> como es un proxy, el guard solo se aplica
 * cuando el método se invoca a través del bean gestionado por Spring (por ejemplo,
 * desde otro servicio inyectado). Una llamada interna tipo {@code this.metodo(...)}
 * dentro de la misma clase evita el proxy y por lo tanto salta la verificación
 * de membresía silenciosamente.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresMembership {
    MembershipDomain domain();
    int idParamIndex() default 0;
}

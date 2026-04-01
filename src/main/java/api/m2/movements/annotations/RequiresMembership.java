package api.m2.movements.annotations;

import api.m2.movements.enums.MembershipDomain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresMembership {
    MembershipDomain domain();
    int idParamIndex() default 0;
}

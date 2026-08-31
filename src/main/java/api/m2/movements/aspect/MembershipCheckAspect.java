package api.m2.movements.aspect;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.aspect.membership.WorkspaceIdResolverRegistry;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspecto que verifica, antes de ejecutar métodos anotados con @RequiresMembership, que el
 * usuario autenticado tenga acceso de escritura al workspace dueño del recurso — no solo
 * membership. Las 11 usos actuales de @RequiresMembership son, sin excepción, update/delete/
 * contribute/pay (nunca una lectura), así que exigir rol >= COLLABORATOR acá es seguro: no hay
 * ningún método de solo-lectura que dependa de este aspecto y que un miembro READ_ONLY deba
 * poder seguir usando.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MembershipCheckAspect {

    private final UserService userService;
    private final WorkspaceQueryService workspaceQueryService;
    private final WorkspaceIdResolverRegistry resolverRegistry;

    @Before("@annotation(requiresMembership)")
    public void checkMembership(JoinPoint joinPoint, RequiresMembership requiresMembership) {
        Object[] args = joinPoint.getArgs();
        Long entityId = (Long) args[requiresMembership.idParamIndex()];

        Long workspaceId = resolverRegistry.resolve(requiresMembership.domain(), entityId);

        log.debug("Verificando acceso de escritura: domain={}, entityId={}, workspaceId={}, userId={}",
                requiresMembership.domain(), entityId, workspaceId, userService.getMe().id());

        workspaceQueryService.verifyCanWrite(workspaceId);
    }
}

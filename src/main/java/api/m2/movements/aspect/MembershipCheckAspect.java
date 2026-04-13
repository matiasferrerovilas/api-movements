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
 * Aspecto que verifica la membresia del usuario antes de ejecutar
 * metodos anotados con @RequiresMembership.
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
        Long userId = userService.getAuthenticatedUser().getId();

        log.debug("Verificando membresía: domain={}, entityId={}, workspaceId={}, userId={}",
                requiresMembership.domain(), entityId, workspaceId, userId);

        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);
    }
}

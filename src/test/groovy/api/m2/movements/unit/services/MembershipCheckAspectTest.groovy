package api.m2.movements.unit.services

import api.m2.movements.annotations.RequiresMembership
import api.m2.movements.aspect.MembershipCheckAspect
import api.m2.movements.aspect.membership.WorkspaceIdResolverRegistry
import api.m2.movements.entities.User
import api.m2.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.services.workspaces.WorkspaceQueryService
import api.m2.movements.services.user.UserService
import org.aspectj.lang.JoinPoint
import spock.lang.Specification

import java.lang.reflect.Proxy

class MembershipCheckAspectTest extends Specification {

    UserService userService = Mock(UserService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    WorkspaceIdResolverRegistry resolverRegistry = Mock(WorkspaceIdResolverRegistry)

    MembershipCheckAspect aspect

    def setup() {
        aspect = new MembershipCheckAspect(
                userService,
                workspaceQueryService,
                resolverRegistry
        )
    }

    /**
     * Creates a real RequiresMembership annotation proxy since Spock cannot stub annotation interfaces.
     */
    RequiresMembership annotation(MembershipDomain domain, int idParamIndex = 0) {
        return (RequiresMembership) Proxy.newProxyInstance(
                RequiresMembership.classLoader,
                [RequiresMembership] as Class[],
                { proxy, method, args ->
                    switch (method.name) {
                        case "domain": return domain
                        case "idParamIndex": return idParamIndex
                        case "annotationType": return RequiresMembership
                        default: return null
                    }
                } as java.lang.reflect.InvocationHandler
        )
    }

    def buildJoinPoint(Object... args) {
        return Stub(JoinPoint) { getArgs() >> args }
    }

    // --- MOVEMENT domain ---

    def "checkMembership - should verify membership for MOVEMENT domain"() {
        given:
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.MOVEMENT)
        def joinPoint = buildJoinPoint(10L)

        resolverRegistry.resolve(MembershipDomain.MOVEMENT, 10L) >> 1L
        userService.getAuthenticatedUser() >> user

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 42L)
    }

    def "checkMembership - should use idParamIndex=1 to extract entity id"() {
        given:
        def user = Stub(User) { getId() >> 7L }
        def ann = annotation(MembershipDomain.MOVEMENT, 1)
        def joinPoint = buildJoinPoint(new Object(), 99L)

        resolverRegistry.resolve(MembershipDomain.MOVEMENT, 99L) >> 5L
        userService.getAuthenticatedUser() >> user

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(5L, 7L)
    }

    def "checkMembership - should throw EntityNotFoundException when MOVEMENT does not exist"() {
        given:
        def ann = annotation(MembershipDomain.MOVEMENT)
        def joinPoint = buildJoinPoint(999L)

        resolverRegistry.resolve(MembershipDomain.MOVEMENT, 999L) >> {
            throw new EntityNotFoundException("Movimiento no encontrado: 999")
        }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }

    def "checkMembership - should throw PermissionDeniedException when user is not member (MOVEMENT)"() {
        given:
        def user = Stub(User) { getId() >> 99L }
        def ann = annotation(MembershipDomain.MOVEMENT)
        def joinPoint = buildJoinPoint(10L)

        resolverRegistry.resolve(MembershipDomain.MOVEMENT, 10L) >> 1L
        userService.getAuthenticatedUser() >> user
        workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 99L) >> {
            throw new PermissionDeniedException("No tienes permiso")
        }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(PermissionDeniedException)
    }

    // --- INCOME domain ---

    def "checkMembership - should verify membership for INCOME domain"() {
        given:
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.INCOME)
        def joinPoint = buildJoinPoint(20L)

        resolverRegistry.resolve(MembershipDomain.INCOME, 20L) >> 2L
        userService.getAuthenticatedUser() >> user

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(2L, 42L)
    }

    def "checkMembership - should throw EntityNotFoundException when INCOME does not exist"() {
        given:
        def ann = annotation(MembershipDomain.INCOME)
        def joinPoint = buildJoinPoint(999L)

        resolverRegistry.resolve(MembershipDomain.INCOME, 999L) >> {
            throw new EntityNotFoundException("Ingreso no encontrado: 999")
        }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }

    // --- SUBSCRIPTION domain ---

    def "checkMembership - should verify membership for SUBSCRIPTION domain"() {
        given:
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.SUBSCRIPTION)
        def joinPoint = buildJoinPoint(30L)

        resolverRegistry.resolve(MembershipDomain.SUBSCRIPTION, 30L) >> 3L
        userService.getAuthenticatedUser() >> user

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(3L, 42L)
    }

    def "checkMembership - should throw EntityNotFoundException when SUBSCRIPTION does not exist"() {
        given:
        def ann = annotation(MembershipDomain.SUBSCRIPTION)
        def joinPoint = buildJoinPoint(999L)

        resolverRegistry.resolve(MembershipDomain.SUBSCRIPTION, 999L) >> {
            throw new EntityNotFoundException("Servicio no encontrado: 999")
        }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }

    def "checkMembership - should throw PermissionDeniedException when user is not member (SUBSCRIPTION)"() {
        given:
        def user = Stub(User) { getId() >> 99L }
        def ann = annotation(MembershipDomain.SUBSCRIPTION)
        def joinPoint = buildJoinPoint(30L)

        resolverRegistry.resolve(MembershipDomain.SUBSCRIPTION, 30L) >> 3L
        userService.getAuthenticatedUser() >> user
        workspaceQueryService.verifyUserIsMemberOfWorkspace(3L, 99L) >> {
            throw new PermissionDeniedException("No tienes permiso")
        }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(PermissionDeniedException)
    }

    // --- BUDGET domain ---

    def "checkMembership - should verify membership for BUDGET domain"() {
        given:
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.BUDGET)
        def joinPoint = buildJoinPoint(40L)

        resolverRegistry.resolve(MembershipDomain.BUDGET, 40L) >> 4L
        userService.getAuthenticatedUser() >> user

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(4L, 42L)
    }

    def "checkMembership - should throw EntityNotFoundException when BUDGET does not exist"() {
        given:
        def ann = annotation(MembershipDomain.BUDGET)
        def joinPoint = buildJoinPoint(999L)

        resolverRegistry.resolve(MembershipDomain.BUDGET, 999L) >> {
            throw new EntityNotFoundException("Presupuesto no encontrado: 999")
        }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }
}

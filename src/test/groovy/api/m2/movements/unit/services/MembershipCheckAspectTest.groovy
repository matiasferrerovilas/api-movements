package api.m2.movements.unit.services

import api.m2.movements.annotations.RequiresMembership
import api.m2.movements.aspect.MembershipCheckAspect
import api.m2.movements.entities.Budget
import api.m2.movements.entities.Income
import api.m2.movements.entities.Movement
import api.m2.movements.entities.Subscription
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.workspaces.WorkspaceQueryService
import api.m2.movements.services.user.UserService
import org.aspectj.lang.JoinPoint
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.lang.reflect.Proxy

class MembershipCheckAspectTest extends Specification {

    UserService userService = Mock(UserService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    MovementRepository movementRepository = Mock(MovementRepository)
    IncomeRepository incomeRepository = Mock(IncomeRepository)
    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    BudgetRepository budgetRepository = Mock(BudgetRepository)

    MembershipCheckAspect aspect

    def setup() {
        aspect = new MembershipCheckAspect(
                userService,
                workspaceQueryService,
                movementRepository,
                incomeRepository,
                subscriptionRepository,
                budgetRepository
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

    def buildWorkspace(Long workspaceId) {
        return Stub(Workspace) { getId() >> workspaceId }
    }

    // --- MOVEMENT domain ---

    def "checkMembership - should verify membership for MOVEMENT domain"() {
        given:
        def workspace = buildWorkspace(1L)
        def movement = Stub(Movement) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.MOVEMENT)
        def joinPoint = buildJoinPoint(10L)

        movementRepository.findById(10L) >> Optional.of(movement)
        userService.getAuthenticatedUser() >> user

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 42L)
    }

    def "checkMembership - should use idParamIndex=1 to extract entity id"() {
        given:
        def workspace = buildWorkspace(5L)
        def movement = Stub(Movement) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 7L }
        def ann = annotation(MembershipDomain.MOVEMENT, 1)
        def joinPoint = buildJoinPoint(new Object(), 99L)

        movementRepository.findById(99L) >> Optional.of(movement)
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

        movementRepository.findById(999L) >> Optional.empty()

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }

    def "checkMembership - should throw PermissionDeniedException when user is not member (MOVEMENT)"() {
        given:
        def workspace = buildWorkspace(1L)
        def movement = Stub(Movement) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 99L }
        def ann = annotation(MembershipDomain.MOVEMENT)
        def joinPoint = buildJoinPoint(10L)

        movementRepository.findById(10L) >> Optional.of(movement)
        userService.getAuthenticatedUser() >> user
        workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 99L) >> { throw new PermissionDeniedException("No tienes permiso") }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(PermissionDeniedException)
    }

    // --- INCOME domain ---

    def "checkMembership - should verify membership for INCOME domain"() {
        given:
        def workspace = buildWorkspace(2L)
        def income = Stub(Income) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.INCOME)
        def joinPoint = buildJoinPoint(20L)

        incomeRepository.findById(20L) >> Optional.of(income)
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

        incomeRepository.findById(999L) >> Optional.empty()

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }

    // --- SUBSCRIPTION domain ---

    def "checkMembership - should verify membership for SUBSCRIPTION domain"() {
        given:
        def workspace = buildWorkspace(3L)
        def subscription = Stub(Subscription) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.SUBSCRIPTION)
        def joinPoint = buildJoinPoint(30L)

        subscriptionRepository.findById(30L) >> Optional.of(subscription)
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

        subscriptionRepository.findById(999L) >> Optional.empty()

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }

    def "checkMembership - should throw PermissionDeniedException when user is not member (SUBSCRIPTION)"() {
        given:
        def workspace = buildWorkspace(3L)
        def subscription = Stub(Subscription) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 99L }
        def ann = annotation(MembershipDomain.SUBSCRIPTION)
        def joinPoint = buildJoinPoint(30L)

        subscriptionRepository.findById(30L) >> Optional.of(subscription)
        userService.getAuthenticatedUser() >> user
        workspaceQueryService.verifyUserIsMemberOfWorkspace(3L, 99L) >> { throw new PermissionDeniedException("No tienes permiso") }

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(PermissionDeniedException)
    }

    // --- BUDGET domain ---

    def "checkMembership - should verify membership for BUDGET domain"() {
        given:
        def workspace = buildWorkspace(4L)
        def budget = Stub(Budget) { getWorkspace() >> workspace }
        def user = Stub(User) { getId() >> 42L }
        def ann = annotation(MembershipDomain.BUDGET)
        def joinPoint = buildJoinPoint(40L)

        budgetRepository.findById(40L) >> Optional.of(budget)
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

        budgetRepository.findById(999L) >> Optional.empty()

        when:
        aspect.checkMembership(joinPoint, ann)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
    }
}

package api.m2.movements.unit.services

import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.projections.MembershipSummaryProjection
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.services.groups.MembershipService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class MembershipServiceTest extends Specification {

    MembershipRepository membershipRepository = Mock(MembershipRepository)
    UserService userService = Mock(UserService)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)

    MembershipService service

    def setup() {
        service = new MembershipService(
                membershipRepository,
                userService,
                workspaceContextService
        )
    }

    def "getAllMemberships - should return membership projections for authenticated user"() {
        given:
        def userId = 1L
        def userRecord = new UserBaseRecord("John", userId)

        def projection1 = Stub(MembershipSummaryProjection) {
            getWorkspaceId() >> 10L
            getWorkspaceName() >> "Personal"
            getRole() >> WorkspaceRole.OWNER
        }
        def projection2 = Stub(MembershipSummaryProjection) {
            getWorkspaceId() >> 20L
            getWorkspaceName() >> "Shared"
            getRole() >> WorkspaceRole.COLLABORATOR
        }

        userService.getAuthenticatedUserRecord() >> userRecord
        membershipRepository.findAllByUserId(userId) >> [projection1, projection2]

        when:
        def result = service.getAllMemberships()

        then:
        result.size() == 2
        result[0].getWorkspaceName() == "Personal"
        result[0].getRole() == WorkspaceRole.OWNER
        result[1].getWorkspaceName() == "Shared"
        result[1].getRole() == WorkspaceRole.COLLABORATOR
    }

    def "getAllMemberships - should return empty list when user has no memberships"() {
        given:
        def userId = 1L
        def userRecord = new UserBaseRecord("John", userId)

        userService.getAuthenticatedUserRecord() >> userRecord
        membershipRepository.findAllByUserId(userId) >> []

        when:
        def result = service.getAllMemberships()

        then:
        result.isEmpty()
    }

    def "getAllMemberships - should delegate to repository with correct user id"() {
        given:
        def userId = 42L
        def userRecord = new UserBaseRecord("Jane", userId)

        userService.getAuthenticatedUserRecord() >> userRecord
        membershipRepository.findAllByUserId(userId) >> []

        when:
        service.getAllMemberships()

        then:
        1 * membershipRepository.findAllByUserId(42L)
    }
}

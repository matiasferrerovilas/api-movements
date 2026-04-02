package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.AccountMember
import api.m2.movements.entities.User
import api.m2.movements.enums.AccountRole
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.AccountMapper
import api.m2.movements.records.accounts.GroupDetail
import api.m2.movements.records.accounts.GroupRecord
import api.m2.movements.records.groups.AddGroupRecord
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.repositories.AccountRepository
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.groups.GroupAddService
import api.m2.movements.services.publishing.websockets.AccountPublishServiceWebSocket
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class GroupAddServiceTest extends Specification {

    AccountQueryService accountQueryService = Mock(AccountQueryService)
    UserService userService = Mock(UserService)
    AccountRepository accountRepository = Mock(AccountRepository)
    MembershipRepository membershipRepository = Mock(MembershipRepository)
    AccountPublishServiceWebSocket accountPublishServiceWebSocket = Mock(AccountPublishServiceWebSocket)
    AccountMapper accountMapper = Mock(AccountMapper)
    UserSettingService userSettingService = Mock(UserSettingService)

    GroupAddService service

    def setup() {
        service = new GroupAddService(
                accountQueryService,
                userService,
                accountRepository,
                membershipRepository,
                accountPublishServiceWebSocket,
                accountMapper,
                userSettingService
        )
    }

    def "createAccount - should save account and publish GroupDetail via publishGroupMembershipUpdated"() {
        given:
        def record = new AddGroupRecord("Viajes")
        def owner = User.builder().id(1L).email("user@test.com").build()
        def savedAccount = Account.builder().id(10L).name("Viajes").owner(owner).build()

        userService.getAuthenticatedUser() >> owner
        userService.getCurrentKeycloakId() >> "keycloak-uuid-123"
        accountQueryService.verifyAccountExist("Viajes", 1L) >> false
        accountRepository.save(_ as Account) >> savedAccount

        when:
        service.createAccount(record)

        then:
        1 * accountRepository.save(_ as Account) >> savedAccount
        1 * accountPublishServiceWebSocket.publishGroupMembershipUpdated(
                new GroupDetail(10L, "Viajes", 1, false),
                "keycloak-uuid-123"
        )
    }

    def "createAccount - should skip when description is blank"() {
        given:
        def record = new AddGroupRecord("   ")

        when:
        service.createAccount(record)

        then:
        0 * accountRepository.save(_ as Account)
        0 * accountPublishServiceWebSocket.publishGroupMembershipUpdated(_ as GroupDetail, _ as String)
    }

    def "createAccount - should skip when account already exists"() {
        given:
        def record = new AddGroupRecord("Hogar")
        def owner = User.builder().id(2L).email("user@test.com").build()

        userService.getAuthenticatedUser() >> owner
        accountQueryService.verifyAccountExist("Hogar", 2L) >> true

        when:
        service.createAccount(record)

        then:
        0 * accountRepository.save(_ as Account)
        0 * accountPublishServiceWebSocket.publishGroupMembershipUpdated(_ as GroupDetail, _ as String)
    }

    def "leaveAccount - should throw PermissionDeniedException when user is not a member"() {
        given:
        def user = new UserBaseRecord("user@test.com", 5L)

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(99L, 5L) >> Optional.empty()

        when:
        service.leaveAccount(99L)

        then:
        thrown(PermissionDeniedException)
    }

    def "leaveAccount - should throw PermissionDeniedException when owner tries to leave with other members"() {
        given:
        def user = new UserBaseRecord("owner@test.com", 1L)
        def account = Account.builder().id(10L).name("Grupo").build()
        def membership = Stub(AccountMember) {
            getRole() >> AccountRole.OWNER
            getAccount() >> account
        }

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(10L, 1L) >> Optional.of(membership)
        membershipRepository.countByAccountId(10L) >> 3L

        when:
        service.leaveAccount(10L)

        then:
        thrown(PermissionDeniedException)
    }

    def "leaveAccount - should deactivate account and publish event when owner leaves as sole member"() {
        given:
        def user = new UserBaseRecord("owner@test.com", 1L)
        def account = Account.builder().id(10L).name("Solo").build()
        def membership = Stub(AccountMember) {
            getRole() >> AccountRole.OWNER
            getAccount() >> account
        }
        def groupRecord = new GroupRecord(10L, "Solo", new UserBaseRecord("owner@test.com", 1L), [])

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(10L, 1L) >> Optional.of(membership)
        membershipRepository.countByAccountId(10L) >> 1L
        accountMapper.toRecord(account) >> groupRecord

        when:
        service.leaveAccount(10L)

        then:
        account.isActive() == false
        1 * accountRepository.save(account)
        1 * membershipRepository.delete(membership)
        1 * accountPublishServiceWebSocket.publishAccountLeft(groupRecord)
    }

    def "leaveAccount - should delete membership and publish event for collaborator"() {
        given:
        def user = new UserBaseRecord("collab@test.com", 2L)
        def account = Account.builder().id(10L).name("Grupo").build()
        def membership = Stub(AccountMember) {
            getRole() >> AccountRole.COLLABORATOR
            getAccount() >> account
        }
        def groupRecord = new GroupRecord(10L, "Grupo", new UserBaseRecord("owner@test.com", 1L), [])

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(10L, 2L) >> Optional.of(membership)
        accountMapper.toRecord(account) >> groupRecord

        when:
        service.leaveAccount(10L)

        then:
        0 * accountRepository.save(_ as Account)
        1 * membershipRepository.delete(membership)
        1 * accountPublishServiceWebSocket.publishAccountLeft(groupRecord)
    }

    def "addMemberToAccount - should publish MEMBERSHIP_UPDATED after member is saved"() {
        given:
        def owner = User.builder().id(1L).email("owner@test.com").build()
        def account = Account.builder().id(20L).name("Familia").owner(owner).build()
        def joiningUser = User.builder().id(2L).email("new@test.com").build()

        userService.getAuthenticatedUser() >> joiningUser
        membershipRepository.countByAccountId(20L) >> 3L

        when:
        service.addMemberToAccount(account)

        then:
        1 * membershipRepository.save(_ as AccountMember)
        1 * accountPublishServiceWebSocket.publishMemberAdded(
                new GroupDetail(20L, "Familia", 3, false),
                20L
        )
    }

    def "updateDefaultGroup - should upsert setting and publish GroupDetail with isDefault true"() {
        given:
        def user = new UserBaseRecord("user@test.com", 1L)
        def account = Account.builder().id(30L).name("Principal").build()
        def membership = Stub(AccountMember) {
            getAccount() >> account
        }

        userService.getAuthenticatedUserRecord() >> user
        userService.getCurrentKeycloakId() >> "keycloak-uuid-456"
        membershipRepository.findMember(30L, 1L) >> Optional.of(membership)
        membershipRepository.countByAccountId(30L) >> 2L

        when:
        service.updateDefaultGroup(30L)

        then:
        1 * userSettingService.upsert(_ as api.m2.movements.enums.UserSettingKey, 30L)
        1 * accountPublishServiceWebSocket.publishGroupMembershipUpdated(
                new GroupDetail(30L, "Principal", 2, true),
                "keycloak-uuid-456"
        )
    }

    def "updateDefaultGroup - should throw PermissionDeniedException when user is not a member"() {
        given:
        def user = new UserBaseRecord("user@test.com", 1L)

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(99L, 1L) >> Optional.empty()

        when:
        service.updateDefaultGroup(99L)

        then:
        thrown(PermissionDeniedException)
    }
}

package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.AccountMember
import api.m2.movements.entities.User
import api.m2.movements.enums.AccountRole
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.AccountMapper
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

    def "createAccount - should save account and publish event when description is valid"() {
        given:
        def record = new AddGroupRecord("Viajes")
        def owner = User.builder().id(1L).email("user@test.com").build()
        def savedAccount = Account.builder().id(10L).name("Viajes").owner(owner).build()
        def groupRecord = new GroupRecord(10L, "Viajes", new UserBaseRecord("user@test.com", 1L), [])

        userService.getAuthenticatedUser() >> owner
        accountQueryService.verifyAccountExist("Viajes", 1L) >> false
        accountRepository.save(_ as Account) >> savedAccount
        accountMapper.toRecord(_ as Account) >> groupRecord

        when:
        service.createAccount(record)

        then:
        1 * accountRepository.save(_ as Account) >> savedAccount
        1 * accountPublishServiceWebSocket.publishAccountCreated(groupRecord)
    }

    def "createAccount - should skip when description is blank"() {
        given:
        def record = new AddGroupRecord("   ")

        when:
        service.createAccount(record)

        then:
        0 * accountRepository.save(_ as Account)
        0 * accountPublishServiceWebSocket.publishAccountCreated(_ as GroupRecord)
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
        0 * accountPublishServiceWebSocket.publishAccountCreated(_ as GroupRecord)
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
}

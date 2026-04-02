package api.m2.movements.unit.services

import api.m2.movements.entities.AccountMember
import api.m2.movements.entities.User
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.AccountMapper
import api.m2.movements.mappers.AccountMapperImpl
import api.m2.movements.mappers.UserMapper
import api.m2.movements.projections.AccountSummaryProjection
import api.m2.movements.records.accounts.GroupDetail
import api.m2.movements.repositories.AccountRepository
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

class AccountQueryServiceTest extends Specification {

    AccountRepository accountRepository = Mock(AccountRepository)
    UserService userService = Mock(UserService)
    AccountMapper accountMapper
    MembershipRepository membershipRepository = Mock(MembershipRepository)
    UserSettingService userSettingService = Mock(UserSettingService)

    AccountQueryService service

    def setup() {
        UserMapper userMapper = Mappers.getMapper(UserMapper)
        accountMapper = new AccountMapperImpl()
        ReflectionTestUtils.setField(accountMapper, "userMapper", userMapper)

        service = new AccountQueryService(
                accountRepository,
                userService,
                accountMapper,
                membershipRepository,
                userSettingService
        )
    }

    def "verifyUserIsMemberOfAccount - should not throw when user is member of account"() {
        given:
        membershipRepository.findMember(1L, 42L) >> Optional.of(Stub(AccountMember))

        when:
        service.verifyUserIsMemberOfAccount(1L, 42L)

        then:
        noExceptionThrown()
    }

    def "verifyUserIsMemberOfAccount - should throw PermissionDeniedException when user is not member"() {
        given:
        membershipRepository.findMember(1L, 99L) >> Optional.empty()

        when:
        service.verifyUserIsMemberOfAccount(1L, 99L)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyUserIsMemberOfAccount - should query with the exact accountId and userId provided"() {
        given:
        def accountId = 5L
        def userId = 10L
        membershipRepository.findMember(accountId, userId) >> Optional.of(Stub(AccountMember))

        when:
        service.verifyUserIsMemberOfAccount(accountId, userId)

        then:
        1 * membershipRepository.findMember(5L, 10L) >> Optional.of(Stub(AccountMember))
    }

    def "getAllGroupDetails - should mark account as default when it matches DEFAULT_ACCOUNT setting"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        def proj1 = Stub(AccountSummaryProjection) {
            getAccountId() >> 10L
            getAccountName() >> "Hogar"
            getMembersCount() >> 2L
        }
        def proj2 = Stub(AccountSummaryProjection) {
            getAccountId() >> 20L
            getAccountName() >> "Viajes"
            getMembersCount() >> 1L
        }

        userService.getAuthenticatedUser() >> owner
        userSettingService.getDefaultAccountId(owner) >> Optional.of(10L)
        accountRepository.findAccountSummariesByMemberUserId(1L) >> [proj1, proj2]

        when:
        List<GroupDetail> result = service.getAllGroupDetails()

        then:
        result.size() == 2
        result[0].id() == 10L
        result[0].name() == "Hogar"
        result[0].membersCount() == 2
        result[0].isDefault() == true
        result[1].id() == 20L
        result[1].isDefault() == false
    }

    def "getAllGroupDetails - should mark isDefault false when no DEFAULT_ACCOUNT setting exists"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        def proj = Stub(AccountSummaryProjection) {
            getAccountId() >> 10L
            getAccountName() >> "Hogar"
            getMembersCount() >> 1L
        }

        userService.getAuthenticatedUser() >> owner
        userSettingService.getDefaultAccountId(owner) >> Optional.empty()
        accountRepository.findAccountSummariesByMemberUserId(1L) >> [proj]

        when:
        List<GroupDetail> result = service.getAllGroupDetails()

        then:
        result.size() == 1
        result[0].isDefault() == false
    }
}

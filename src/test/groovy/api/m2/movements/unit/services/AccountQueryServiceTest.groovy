package api.m2.movements.unit.services

import api.m2.movements.entities.AccountMember
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.AccountMapper
import api.m2.movements.repositories.AccountRepository
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class AccountQueryServiceTest extends Specification {

    AccountRepository accountRepository = Mock(AccountRepository)
    UserService userService = Mock(UserService)
    AccountMapper accountMapper = Mock(AccountMapper)
    MembershipRepository membershipRepository = Mock(MembershipRepository)

    AccountQueryService service

    def setup() {
        service = new AccountQueryService(
                accountRepository,
                userService,
                accountMapper,
                membershipRepository
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
}

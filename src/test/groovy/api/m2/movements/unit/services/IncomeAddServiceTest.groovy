package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Income

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.IncomeMapper
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.services.currencies.CurrencyResolver
import api.m2.movements.services.workspaces.WorkspaceContextService
import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class IncomeAddServiceTest extends Specification {

    IncomeRepository incomeRepository = Mock(IncomeRepository)
    UserService userService = Mock(UserService)
    IncomeMapper incomeMapper
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    CurrencyResolver currencyResolver = Mock(CurrencyResolver)
    BankRepository bankRepository = Mock(BankRepository)

    IncomeAddService service

    def setup() {
        incomeMapper = Mappers.getMapper(IncomeMapper)

        service = new IncomeAddService(
                incomeRepository,
                userService,
                incomeMapper,
                workspaceContextService,
                currencyResolver,
                bankRepository
        )
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, [], null))
    }

    def "loadIncome - should set bank on income before saving"() {
        given:
        def incomeToAdd = new IncomeToAdd("galicia", new CurrencyRecord("ARS", null), new BigDecimal("150000.00"))
        def currency = Stub(Currency)
        def bank = Bank.builder().id(1L).description("GALICIA").build()

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWritableWorkspaceId() >> 1L
        currencyResolver.resolve("ARS", 1L) >> currency
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved ->
            saved.bank == bank
        })
    }

    def "loadIncome - should sanitize bank name (trim + uppercase) before lookup"() {
        given:
        def incomeToAdd = new IncomeToAdd("  bbva  ", new CurrencyRecord("USD", null), new BigDecimal("500.00"))
        def bank = Bank.builder().id(2L).description("BBVA").build()

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWritableWorkspaceId() >> 1L
        currencyResolver.resolve("USD", 1L) >> Stub(Currency)
        bankRepository.findByDescription("BBVA") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved -> saved.bank == bank })
    }

    def "loadIncome - should throw EntityNotFoundException when bank does not exist"() {
        given:
        def incomeToAdd = new IncomeToAdd("BANCO_INEXISTENTE", new CurrencyRecord("ARS", null), new BigDecimal("100.00"))

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWritableWorkspaceId() >> 1L
        currencyResolver.resolve("ARS", 1L) >> Stub(Currency)
        bankRepository.findByDescription("BANCO_INEXISTENTE") >> Optional.empty()

        when:
        service.loadIncome(incomeToAdd)

        then:
        thrown(EntityNotFoundException)
        0 * incomeRepository.save(_)
    }

    def "loadIncome - should set user, workspace and currency on income"() {
        given:
        def incomeToAdd = new IncomeToAdd("SANTANDER", new CurrencyRecord("ARS", null), new BigDecimal("200000.00"))
        def currency = Stub(Currency)
        def bank = Bank.builder().id(3L).description("SANTANDER").build()

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWritableWorkspaceId() >> 1L
        currencyResolver.resolve("ARS", 1L) >> currency
        bankRepository.findByDescription("SANTANDER") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved ->
            saved.userId == 1L &&
            saved.workspaceId == 1L &&
            saved.currency == currency &&
            saved.bank == bank
        })
    }

    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "deleteIncome - should delete income when called"() {
        given:
        def income = new Income(id: 10L, workspaceId: 1L)
        incomeRepository.findById(10L) >> Optional.of(income)

        when:
        service.deleteIncome(10L)

        then:
        1 * incomeRepository.delete(income)
    }

    def "deleteIncome - should throw EntityNotFoundException when income does not exist"() {
        given:
        incomeRepository.findById(999L) >> Optional.empty()

        when:
        service.deleteIncome(999L)

        then:
        thrown(EntityNotFoundException)
        0 * incomeRepository.delete(_ as Income)
    }
}

package api.m2.movements.unit.services

import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.entities.Account
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Income
import api.m2.movements.enums.BanksEnum
import api.m2.movements.enums.CategoryEnum
import api.m2.movements.enums.MovementType
import api.m2.movements.mappers.IncomeMapper
import api.m2.movements.records.income.IncomeRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.user.UserService
import api.m2.movements.exceptions.EntityNotFoundException
import org.mapstruct.factory.Mappers
import spock.lang.Specification
import spock.lang.Subject
import api.m2.movements.entities.User
import java.time.LocalDate

class IncomeAddServiceTest extends Specification {

    IncomeRepository incomeRepository = Mock(IncomeRepository)
    UserService userService = Mock(UserService)
    IncomeMapper incomeMapper = Mappers.getMapper(IncomeMapper)
    AccountQueryService accountQueryService = Mock(AccountQueryService)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    MovementAddService movementAddService = Mock(MovementAddService)

    @Subject
    IncomeAddService service

    def setup() {
        service = new IncomeAddService(
                incomeRepository,
                userService,
                incomeMapper,
                accountQueryService,
                currencyAddService,
                movementAddService
        )
        userService.getAuthenticatedUser() >> Stub(User) { getId() >> 1L }
    }

    def "loadIncome - should save income with correct parameters"() {
        given:
        def incomeToAdd = new IncomeToAdd("GALICIA", "ARS", new BigDecimal("1000.00"), "Mi grupo")
        def account = Stub(Account) { getId() >> 1L }
        def currency = Stub(Currency) { getSymbol() >> "ARS" }

        accountQueryService.findAccountByName("Mi grupo") >> account
        currencyAddService.findBySymbol("ARS") >> currency

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save(_ as Income) >> { Income income ->
            assert income.getUser().getId() == 1L
            assert income.getAccount().getId() == 1L
            assert income.getCurrency().getSymbol() == "ARS"
            assert income.getBank() == "GALICIA"
            assert income.getAmount() == new BigDecimal("1000.00")
        }
    }

    def "getAllIncomes - should return all incomes for the authenticated user"() {
        given:
        def income1 = Stub(Income) { getId() >> 1L; getAmount() >> new BigDecimal("1000.00"); getCurrency() >> Stub(Currency) { getSymbol() >> "ARS" }; getBank() >> "GALICIA"; getAccount() >> Stub(Account) { getName() >> "Mi grupo" } }
        def income2 = Stub(Income) { getId() >> 2L; getAmount() >> new BigDecimal("2000.00"); getCurrency() >> Stub(Currency) { getSymbol() >> "USD" }; getBank() >> "BBVA"; getAccount() >> Stub(Account) { getName() >> "Otro grupo" } }

        incomeRepository.findAllByUserOrGroupsIn(1L) >> [income1, income2]

        when:
        def result = service.getAllIncomes()

        then:
        result.size() == 2
        result.find { it.id() == 1L }.amount() == new BigDecimal("1000.00")
        result.find { it.id() == 2L }.amount() == new BigDecimal("2000.00")
    }

    def "deleteIncome - should delete income by id"() {
        given:
        def incomeToDelete = Stub(Income) { getId() >> 1L }

        incomeRepository.findById(1L) >> Optional.of(incomeToDelete)

        when:
        service.deleteIncome(1L)

        then:
        1 * incomeRepository.delete(incomeToDelete)
    }

    def "deleteIncome - should throw EntityNotFoundException if income is not found"() {
        given:
        incomeRepository.findById(1L) >> Optional.empty()

        when:
        service.deleteIncome(1L)

        then:
        thrown(EntityNotFoundException)
    }

    def "reloadIncome - should save movement with correct parameters"() {
        given:
        def incomeToReload = Stub(Income) {
            getAmount() >> new BigDecimal("1000.00")
            getCurrency() >> Stub(Currency) { getSymbol() >> "ARS" }
            getBank() >> "GALICIA"
            getAccount() >> Stub(Account) { getId() >> 1L }
        }

        incomeRepository.findById(1L) >> Optional.of(incomeToReload)

        when:
        service.reloadIncome(1L)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m ->
            assert m.amount() == new BigDecimal("1000.00")
            assert m.date() == LocalDate.now()
            assert m.description() == "Ingreso"
            assert m.category() == CategoryEnum.HOGAR.name()
            assert m.type() == MovementType.INGRESO.name()
            assert m.currency() == "ARS"
            assert m.bank() == BanksEnum.findByDescription("GALICIA")
            assert m.groupId() == 1L
        })
    }

    def "reloadIncome - should throw EntityNotFoundException if income is not found"() {
        given:
        incomeRepository.findById(1L) >> Optional.empty()

        when:
        service.reloadIncome(1L)

        then:
        thrown(EntityNotFoundException)
    }
}
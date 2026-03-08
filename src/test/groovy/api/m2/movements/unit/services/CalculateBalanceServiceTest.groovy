package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.BalanceEnum
import api.m2.movements.enums.MovementType
import api.m2.movements.mappers.BalanceEvolutionMapper
import api.m2.movements.records.balance.*
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.balance.CalculateBalanceService
import api.m2.movements.services.user.UserService
import spock.lang.Specification
import org.mapstruct.factory.Mappers

import java.time.LocalDate

class CalculateBalanceServiceTest extends Specification {

    MovementRepository movementRepository = Mock()
    UserService userService = Mock()
    CurrencyRepository currencyRepository = Mock()

    // mapper REAL
    BalanceEvolutionMapper balanceEvolutionMapper = Mappers.getMapper(BalanceEvolutionMapper)

    CalculateBalanceService service

    def setup() {
        service = new CalculateBalanceService(
                movementRepository,
                userService,
                currencyRepository,
                balanceEvolutionMapper
        )
    }

    def "should calculate ingreso and gasto balance"() {

        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026,1,1),
                LocalDate.of(2026,12,31),
                [1,2],
                ["EUR"]
        )

        def user = Stub(User) {
            getEmail() >> "user@test.com"
        }

        userService.getAuthenticatedUser() >> user
        currencyRepository.findAllBySymbol(["EUR"]) >> []

        movementRepository.getBalanceByFilters(
                _ as LocalDate,
                _ as LocalDate,
                _ as String,
                [MovementType.INGRESO.toString()],
                _ as List<Integer>,
                _ as List
        ) >> new BigDecimal("1000")

        movementRepository.getBalanceByFilters(
                _ as LocalDate,
                _ as LocalDate,
                _ as String,
                [MovementType.DEBITO.toString()],
                _ as List<Integer>,
                _ as List
        ) >> new BigDecimal("400")

        when:
        def result = service.getBalance(filter)

        then:
        result[BalanceEnum.INGRESO] == new BigDecimal("1000")
        result[BalanceEnum.GASTO] == new BigDecimal("400")
    }
}
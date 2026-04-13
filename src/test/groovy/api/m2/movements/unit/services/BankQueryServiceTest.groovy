package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.User
import api.m2.movements.entities.UserBank
import api.m2.movements.mappers.BankMapper
import api.m2.movements.repositories.UserBankRepository
import api.m2.movements.services.banks.BankQueryService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class BankQueryServiceTest extends Specification {

    UserBankRepository userBankRepository = Mock(UserBankRepository)
    UserService userService = Mock(UserService)
    BankMapper bankMapper = Mappers.getMapper(BankMapper)

    BankQueryService service

    def setup() {
        service = new BankQueryService(userBankRepository, userService, bankMapper)
    }

    def "getAllBanks - should return only banks associated with the authenticated user"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Bank.builder().id(10L).description("GALICIA").build()
        def userBank = Stub(UserBank) { getBank() >> bank }

        userService.getAuthenticatedUser() >> user
        userBankRepository.findByUserId(1L) >> [userBank]

        when:
        def result = service.getAllBanks()

        then:
        result.size() == 1
        result[0].description() == "GALICIA"
    }

    def "getAllBanks - should return empty list when user has no banks"() {
        given:
        def user = Stub(User) { getId() >> 1L }

        userService.getAuthenticatedUser() >> user
        userBankRepository.findByUserId(1L) >> []

        when:
        def result = service.getAllBanks()

        then:
        result.isEmpty()
    }
}

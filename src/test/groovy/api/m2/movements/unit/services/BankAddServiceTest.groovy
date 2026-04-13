package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.User
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.BankMapper
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.UserBankRepository
import api.m2.movements.services.banks.BankAddService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class BankAddServiceTest extends Specification {

    BankRepository bankRepository = Mock(BankRepository)
    UserBankRepository userBankRepository = Mock(UserBankRepository)
    UserService userService = Mock(UserService)
    BankMapper bankMapper = Mappers.getMapper(BankMapper)

    BankAddService service

    def setup() {
        service = new BankAddService(bankRepository, userBankRepository, userService, bankMapper)
    }

    def "addBankToUser - should find existing bank and associate it to user"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Bank.builder().id(10L).description("GALICIA").build()

        userService.getAuthenticatedUser() >> user
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> false

        when:
        def result = service.addBankToUser("galicia")

        then:
        1 * bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        0 * bankRepository.save(_)
        1 * userBankRepository.save(_)
        result.description() == "GALICIA"
    }

    def "addBankToUser - should create bank if it does not exist and associate it to user"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def savedBank = Bank.builder().id(99L).description("BANCO NACION").build()

        userService.getAuthenticatedUser() >> user
        bankRepository.findByDescription("BANCO NACION") >> Optional.empty()
        userBankRepository.existsByUserIdAndBankId(1L, 99L) >> false

        when:
        def result = service.addBankToUser("  banco nacion  ")

        then:
        1 * bankRepository.save({ Bank b -> b.description == "BANCO NACION" }) >> savedBank
        1 * userBankRepository.save(_)
        result.description() == "BANCO NACION"
    }

    def "addBankToUser - should sanitize description (trim and uppercase) before lookup"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Bank.builder().id(2L).description("BBVA").build()

        userService.getAuthenticatedUser() >> user
        userBankRepository.existsByUserIdAndBankId(1L, 2L) >> false

        when:
        service.addBankToUser("  bbva  ")

        then:
        1 * bankRepository.findByDescription("BBVA") >> Optional.of(bank)
        0 * bankRepository.findByDescription("  bbva  ")
    }

    def "addBankToUser - should not duplicate association if user already has the bank"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Bank.builder().id(10L).description("GALICIA").build()

        userService.getAuthenticatedUser() >> user
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true

        when:
        service.addBankToUser("GALICIA")

        then:
        0 * userBankRepository.save(_)
    }

    def "removeBankFromUser - should delete association when bank exists for user"() {
        given:
        def user = Stub(User) { getId() >> 1L }

        userService.getAuthenticatedUser() >> user
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true

        when:
        service.removeBankFromUser(10L)

        then:
        1 * userBankRepository.deleteByUserIdAndBankId(1L, 10L)
    }

    def "removeBankFromUser - should throw EntityNotFoundException when bank is not in user list"() {
        given:
        def user = Stub(User) { getId() >> 1L }

        userService.getAuthenticatedUser() >> user
        userBankRepository.existsByUserIdAndBankId(1L, 99L) >> false

        when:
        service.removeBankFromUser(99L)

        then:
        thrown(EntityNotFoundException)
        0 * userBankRepository.deleteByUserIdAndBankId(_, _)
    }
}

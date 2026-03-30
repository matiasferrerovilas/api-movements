package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.User
import api.m2.movements.entities.UserBank
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.BankMapper
import api.m2.movements.records.banks.BankRecord
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.UserBankRepository
import api.m2.movements.services.banks.BankService
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class BankServiceTest extends Specification {

    BankRepository bankRepository = Mock(BankRepository)
    UserBankRepository userBankRepository = Mock(UserBankRepository)
    UserService userService = Mock(UserService)
    BankMapper bankMapper = Mock(BankMapper)

    BankService service

    def setup() {
        service = new BankService(bankRepository, userBankRepository, userService, bankMapper)
    }

    // ─── getAllBanks ────────────────────────────────────────────────────────────

    def "getAllBanks - should return only banks associated with the authenticated user"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Stub(Bank) { getId() >> 10L; getDescription() >> "GALICIA" }
        def userBank = Stub(UserBank) { getBank() >> bank }
        def record = new BankRecord(10L, "GALICIA")

        userService.getAuthenticatedUser() >> user
        userBankRepository.findByUserId(1L) >> [userBank]
        bankMapper.toRecord(bank) >> record

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

    // ─── addBankToUser ──────────────────────────────────────────────────────────

    def "addBankToUser - should find existing bank and associate it to user"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Stub(Bank) { getId() >> 10L; getDescription() >> "GALICIA" }
        def record = new BankRecord(10L, "GALICIA")

        userService.getAuthenticatedUser() >> user
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> false
        bankMapper.toRecord(bank) >> record

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
        def savedBank = Stub(Bank) { getId() >> 99L; getDescription() >> "BANCO NACION" }
        def record = new BankRecord(99L, "BANCO NACION")

        userService.getAuthenticatedUser() >> user
        bankRepository.findByDescription("BANCO NACION") >> Optional.empty()
        userBankRepository.existsByUserIdAndBankId(1L, 99L) >> false
        bankMapper.toRecord(savedBank) >> record

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
        def bank = Stub(Bank) { getId() >> 2L; getDescription() >> "BBVA" }

        userService.getAuthenticatedUser() >> user
        userBankRepository.existsByUserIdAndBankId(1L, 2L) >> false
        bankMapper.toRecord(bank) >> new BankRecord(2L, "BBVA")

        when:
        service.addBankToUser("  bbva  ")

        then:
        1 * bankRepository.findByDescription("BBVA") >> Optional.of(bank)
        0 * bankRepository.findByDescription("  bbva  ")
    }

    def "addBankToUser - should not duplicate association if user already has the bank"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def bank = Stub(Bank) { getId() >> 10L; getDescription() >> "GALICIA" }

        userService.getAuthenticatedUser() >> user
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        bankMapper.toRecord(bank) >> new BankRecord(10L, "GALICIA")

        when:
        service.addBankToUser("GALICIA")

        then:
        0 * userBankRepository.save(_)
    }

    // ─── removeBankFromUser ─────────────────────────────────────────────────────

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

package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank

import api.m2.movements.entities.integrity.UserBank
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.BankMapper
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.UserBankRepository
import api.m2.movements.services.banks.BankAddService
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class BankAddServiceTest extends Specification {

    BankRepository bankRepository = Mock(BankRepository)
    UserBankRepository userBankRepository = Mock(UserBankRepository)
    UserService userService = Mock(UserService)
    UserSettingService userSettingService = Mock(UserSettingService)
    BankMapper bankMapper = Mappers.getMapper(BankMapper)

    BankAddService service

    def setup() {
        service = new BankAddService(bankRepository, userBankRepository, userService, bankMapper, userSettingService)
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, []))
    }

    def "addBankToUser - should find existing bank and associate it to user"() {
        given:
        def bank = Bank.builder().id(10L).description("GALICIA").build()

        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> false
        userBankRepository.findByUserId(1L) >> [Stub(UserBank), Stub(UserBank)]

        when:
        def result = service.addBankToUser("galicia")

        then:
        1 * bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        0 * bankRepository.save(_)
        1 * userBankRepository.save(_ as UserBank)
        result.description() == "GALICIA"
    }

    def "addBankToUser - should create bank if it does not exist and associate it to user"() {
        given:
        def savedBank = Bank.builder().id(99L).description("BANCO NACION").build()

        userService.getMe() >> userMe(1L)
        bankRepository.findByDescription("BANCO NACION") >> Optional.empty()
        userBankRepository.existsByUserIdAndBankId(1L, 99L) >> false
        userBankRepository.findByUserId(1L) >> [Stub(UserBank), Stub(UserBank)]

        when:
        def result = service.addBankToUser("  banco nacion  ")

        then:
        1 * bankRepository.save({ Bank b -> b.description == "BANCO NACION" }) >> savedBank
        1 * userBankRepository.save(_ as UserBank)
        result.description() == "BANCO NACION"
    }

    def "addBankToUser - should sanitize description (trim and uppercase) before lookup"() {
        given:
        def bank = Bank.builder().id(2L).description("BBVA").build()

        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 2L) >> false
        userBankRepository.findByUserId(1L) >> [Stub(UserBank)]

        when:
        service.addBankToUser("  bbva  ")

        then:
        1 * bankRepository.findByDescription("BBVA") >> Optional.of(bank)
        0 * bankRepository.findByDescription("  bbva  ")
    }

    def "addBankToUser - should not duplicate association if user already has the bank"() {
        given:
        def bank = Bank.builder().id(10L).description("GALICIA").build()

        userService.getMe() >> userMe(1L)
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        userBankRepository.findByUserId(1L) >> [Stub(UserBank)]

        when:
        service.addBankToUser("GALICIA")

        then:
        0 * userBankRepository.save(_)
        0 * userSettingService.upsertForUser(_, _, _)
    }

    def "addBankToUser - should set bank as default when it is the only bank for user"() {
        given:
        def bank = Bank.builder().id(10L).description("GALICIA").build()
        def userBank = Stub(UserBank) { getBank() >> bank }

        userService.getMe() >> userMe(1L)
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> false
        userBankRepository.findByUserId(1L) >> [userBank]

        when:
        service.addBankToUser("galicia")

        then:
        1 * userBankRepository.save(_ as UserBank)
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 10L)
    }

    def "addBankToUser - should NOT set as default when user already has other banks"() {
        given:
        def bank = Bank.builder().id(10L).description("GALICIA").build()

        userService.getMe() >> userMe(1L)
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> false
        userBankRepository.findByUserId(1L) >> [Stub(UserBank), Stub(UserBank)]

        when:
        service.addBankToUser("galicia")

        then:
        1 * userBankRepository.save(_ as UserBank)
        0 * userSettingService.upsertForUser(_, _, _)
    }

    def "addBankToUser - should not query banks when bank already associated"() {
        given:
        def bank = Bank.builder().id(10L).description("GALICIA").build()

        userService.getMe() >> userMe(1L)
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        userBankRepository.findByUserId(1L) >> [Stub(UserBank)]

        when:
        service.addBankToUser("GALICIA")

        then:
        0 * userBankRepository.save(_)
        0 * userSettingService.upsertForUser(_, _, _)
    }

    def "removeBankFromUser - should delete association when bank exists for user"() {
        given:
        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        userBankRepository.findByUserId(1L) >> [Stub(UserBank), Stub(UserBank), Stub(UserBank)]

        when:
        service.removeBankFromUser(10L)

        then:
        1 * userBankRepository.deleteByUserIdAndBankId(1L, 10L)
    }

    def "removeBankFromUser - should throw EntityNotFoundException when bank is not in user list"() {
        given:
        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 99L) >> false

        when:
        service.removeBankFromUser(99L)

        then:
        thrown(EntityNotFoundException)
        0 * userBankRepository.deleteByUserIdAndBankId(_, _)
    }

    def "removeBankFromUser - should set remaining bank as default when only one left"() {
        given:
        def remainingBank = Bank.builder().id(99L).description("BBVA").build()
        def userBank = Stub(UserBank) { getBank() >> remainingBank }

        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        userBankRepository.findByUserId(1L) >> [userBank]

        when:
        service.removeBankFromUser(10L)

        then:
        1 * userBankRepository.deleteByUserIdAndBankId(1L, 10L)
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 99L)
        0 * userSettingService.deleteByKey(_)
    }

    def "removeBankFromUser - should NOT change default when multiple banks remain"() {
        given:
        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        userBankRepository.findByUserId(1L) >> [Stub(UserBank), Stub(UserBank), Stub(UserBank)]

        when:
        service.removeBankFromUser(10L)

        then:
        1 * userBankRepository.deleteByUserIdAndBankId(1L, 10L)
        0 * userSettingService.upsertForUser(_, _, _)
        0 * userSettingService.deleteByKey(_)
    }

    def "removeBankFromUser - should clear DEFAULT_BANK setting when no banks remain"() {
        given:
        userService.getMe() >> userMe(1L)
        userBankRepository.existsByUserIdAndBankId(1L, 10L) >> true
        userBankRepository.findByUserId(1L) >> []

        when:
        service.removeBankFromUser(10L)

        then:
        1 * userBankRepository.deleteByUserIdAndBankId(1L, 10L)
        1 * userSettingService.deleteByKey(UserSettingKey.DEFAULT_BANK)
        0 * userSettingService.upsertForUser(_, _, _)
    }
}

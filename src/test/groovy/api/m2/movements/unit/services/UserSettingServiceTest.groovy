package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank

import api.m2.movements.entities.integrity.UserSetting
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.UserSettingRepository
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class UserSettingServiceTest extends Specification {

    UserSettingRepository userSettingRepository = Mock(UserSettingRepository)
    UserService userService = Mock(UserService)
    BankRepository bankRepository = Mock(BankRepository)

    UserSettingService service

    def setup() {
        service = new UserSettingService(userSettingRepository, userService, bankRepository)
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, []))
    }

    def "getAll - should return all settings for authenticated user"() {
        given:
        def setting1 = Stub(UserSetting) {
            getSettingKey() >> UserSettingKey.DEFAULT_BANK
            getSettingValue() >> 10L
        }
        def setting2 = Stub(UserSetting) {
            getSettingKey() >> UserSettingKey.DEFAULT_WORKSPACE
            getSettingValue() >> 20L
        }

        userService.getMe() >> userMe(1L)
        userSettingRepository.findAllByUserId(1L) >> [setting1, setting2]

        when:
        def result = service.getAll()

        then:
        result.size() == 2
        result[0].key() == UserSettingKey.DEFAULT_BANK
        result[0].value() == 10L
        result[1].key() == UserSettingKey.DEFAULT_WORKSPACE
        result[1].value() == 20L
    }

    def "getAll - should return empty list when user has no settings"() {
        given:
        userService.getMe() >> userMe(1L)
        userSettingRepository.findAllByUserId(1L) >> []

        when:
        def result = service.getAll()

        then:
        result.isEmpty()
    }

    def "getByKey - should return setting when found"() {
        given:
        def setting = Stub(UserSetting) {
            getSettingKey() >> UserSettingKey.DEFAULT_BANK
            getSettingValue() >> 42L
        }

        userService.getMe() >> userMe(1L)
        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK) >> Optional.of(setting)

        when:
        def result = service.getByKey(UserSettingKey.DEFAULT_BANK)

        then:
        result.key() == UserSettingKey.DEFAULT_BANK
        result.value() == 42L
    }

    def "getByKey - should throw EntityNotFoundException when setting not found"() {
        given:
        userService.getMe() >> userMe(1L)
        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_CURRENCY) >> Optional.empty()

        when:
        service.getByKey(UserSettingKey.DEFAULT_CURRENCY)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("DEFAULT_CURRENCY")
    }

    def "upsert - should create new setting when not exists"() {
        given:
        userService.getMe() >> userMe(1L)
        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK) >> Optional.empty()

        def savedSetting = Stub(UserSetting) {
            getSettingKey() >> UserSettingKey.DEFAULT_BANK
            getSettingValue() >> 99L
        }
        userSettingRepository.saveAndFlush(_ as UserSetting) >> savedSetting

        when:
        def result = service.upsert(UserSettingKey.DEFAULT_BANK, 99L)

        then:
        result.key() == UserSettingKey.DEFAULT_BANK
        result.value() == 99L
    }

    def "upsert - should update existing setting"() {
        given:
        def existingSetting = new UserSetting()
        existingSetting.setSettingKey(UserSettingKey.DEFAULT_BANK)
        existingSetting.setSettingValue(10L)

        userService.getMe() >> userMe(1L)
        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK) >> Optional.of(existingSetting)

        def savedSetting = Stub(UserSetting) {
            getSettingKey() >> UserSettingKey.DEFAULT_BANK
            getSettingValue() >> 50L
        }
        userSettingRepository.saveAndFlush(_ as UserSetting) >> savedSetting

        when:
        def result = service.upsert(UserSettingKey.DEFAULT_BANK, 50L)

        then:
        result.value() == 50L
    }

    def "upsertForUser - should upsert setting for given user"() {
        when:
        service.upsertForUser(5L, UserSettingKey.DEFAULT_WORKSPACE, 100L)

        then:
        1 * userSettingRepository.upsertSetting(5L, UserSettingKey.DEFAULT_WORKSPACE.name(), 100L)
    }

    def "getDefaultBank - should return bank when setting exists"() {
        given:
        def setting = Stub(UserSetting) { getSettingValue() >> 7L }
        def bank = Stub(Bank) { getId() >> 7L }

        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK) >> Optional.of(setting)
        bankRepository.findById(7L) >> Optional.of(bank)

        when:
        def result = service.getDefaultBank(1L)

        then:
        result.isPresent()
        result.get().id == 7L
    }

    def "getDefaultBank - should return empty when setting not found"() {
        given:
        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK) >> Optional.empty()

        when:
        def result = service.getDefaultBank(1L)

        then:
        result.isEmpty()
    }

    def "getDefaultBank - should return empty when bank not found"() {
        given:
        def setting = Stub(UserSetting) { getSettingValue() >> 999L }

        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK) >> Optional.of(setting)
        bankRepository.findById(999L) >> Optional.empty()

        when:
        def result = service.getDefaultBank(1L)

        then:
        result.isEmpty()
    }

    def "getDefaultWorkspaceId - should return workspace id when setting exists"() {
        given:
        def setting = Stub(UserSetting) { getSettingValue() >> 123L }

        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_WORKSPACE) >> Optional.of(setting)

        when:
        def result = service.getDefaultWorkspaceId(1L)

        then:
        result.isPresent()
        result.get() == 123L
    }

    def "getDefaultWorkspaceId - should return empty when setting not found"() {
        given:
        userSettingRepository.findByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_WORKSPACE) >> Optional.empty()

        when:
        def result = service.getDefaultWorkspaceId(1L)

        then:
        result.isEmpty()
    }

    def "deleteByKey - should delete setting for authenticated user"() {
        given:
        userService.getMe() >> userMe(1L)

        when:
        service.deleteByKey(UserSettingKey.DEFAULT_BANK)

        then:
        1 * userSettingRepository.deleteByUserIdAndSettingKey(1L, UserSettingKey.DEFAULT_BANK)
    }

    def "getUsersWithMonthlySnapshotEnabled - should delegate to repository"() {
        given:
        userSettingRepository.findUserIdsWithSettingEnabled(UserSettingKey.MONTHLY_SUMMARY_ENABLED) >> [1L, 2L]

        when:
        def result = service.getUsersWithMonthlySnapshotEnabled()

        then:
        result.size() == 2
    }

    def "getUsersWithAutoIncomeEnabled - should delegate to repository"() {
        given:
        userSettingRepository.findUserIdsWithSettingEnabled(UserSettingKey.AUTO_INCOME_ENABLED) >> [1L]

        when:
        def result = service.getUsersWithAutoIncomeEnabled()

        then:
        result.size() == 1
    }
}

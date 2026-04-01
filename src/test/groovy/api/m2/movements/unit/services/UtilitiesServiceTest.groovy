package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Subscription
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.SubscriptionMapper
import api.m2.movements.records.services.UpdateSubscriptionRecord
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket
import api.m2.movements.services.services.UtilitiesService
import api.m2.movements.services.services.UtilityAddService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class UtilitiesServiceTest extends Specification {

    SubscriptionMapper serviceMapper = Mappers.getMapper(SubscriptionMapper)
    SubscriptionRepository serviceRepository = Mock(SubscriptionRepository)
    UtilityAddService utilityAddService = Mock(UtilityAddService)
    UserService userService = Mock(UserService)
    ServicePublishServiceWebSocket servicePublishService = Mock(ServicePublishServiceWebSocket)
    AccountQueryService accountQueryService = Mock(AccountQueryService)

    UtilitiesService service

    def setup() {
        service = new UtilitiesService(
                serviceMapper,
                serviceRepository,
                utilityAddService,
                userService,
                servicePublishService,
                accountQueryService
        )
    }

    def buildSubscription(Long accountId) {
        def account = Stub(Account) { getId() >> accountId; getName() >> "Mi cuenta" }
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        return new Subscription(id: 1L, description: "Netflix", amount: new BigDecimal("10.00"),
                account: account, currency: currency)
    }

    // --- payServiceById ---
    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "payServiceById - should pay service when called"() {
        given:
        def sub = buildSubscription(1L)
        serviceRepository.findById(10L) >> Optional.of(sub)

        when:
        service.payServiceById(10L)

        then:
        1 * utilityAddService.addMovementService(sub)
        1 * serviceRepository.save(sub)
    }

    def "payServiceById - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        serviceRepository.findById(999L) >> Optional.empty()

        when:
        service.payServiceById(999L)

        then:
        thrown(EntityNotFoundException)
        0 * utilityAddService.addMovementService(_ as Subscription)
    }

    // --- updateService ---
    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "updateService - should update service when called"() {
        given:
        def sub = buildSubscription(2L)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("15.00"), null, null, "Netflix HD")
        serviceRepository.findById(20L) >> Optional.of(sub)

        when:
        service.updateService(20L, dto)

        then:
        1 * serviceRepository.save(sub)
    }

    def "updateService - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        serviceRepository.findById(999L) >> Optional.empty()

        when:
        service.updateService(999L, new UpdateSubscriptionRecord(null, null, null, null))

        then:
        thrown(EntityNotFoundException)
        0 * serviceRepository.save(_ as Subscription)
    }

    // --- deleteService ---
    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "deleteService - should delete and publish event when called"() {
        given:
        def sub = buildSubscription(3L)
        serviceRepository.findByIdWithCurrency(30L) >> Optional.of(sub)

        when:
        service.deleteService(30L)

        then:
        1 * serviceRepository.delete(sub)
        1 * servicePublishService.publishDeleteService(_ as api.m2.movements.records.services.SubscriptionRecord)
    }

    def "deleteService - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        serviceRepository.findByIdWithCurrency(999L) >> Optional.empty()

        when:
        service.deleteService(999L)

        then:
        thrown(EntityNotFoundException)
        0 * serviceRepository.delete(_ as Subscription)
    }
}

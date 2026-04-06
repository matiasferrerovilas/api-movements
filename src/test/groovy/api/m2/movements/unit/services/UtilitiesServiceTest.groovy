package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
import api.m2.movements.entities.Subscription
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.SubscriptionMapper
import api.m2.movements.records.services.UpdateSubscriptionRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket
import api.m2.movements.services.subscriptions.UtilitiesService
import api.m2.movements.services.subscriptions.UtilityAddService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class UtilitiesServiceTest extends Specification {

    SubscriptionMapper serviceMapper = Mappers.getMapper(SubscriptionMapper)
    SubscriptionRepository serviceRepository = Mock(SubscriptionRepository)
    UtilityAddService utilityAddService = Mock(UtilityAddService)
    UserService userService = Mock(UserService)
    ServicePublishServiceWebSocket servicePublishService = Mock(ServicePublishServiceWebSocket)
    AccountQueryService accountQueryService = Mock(AccountQueryService)
    MovementRepository movementRepository = Mock(MovementRepository)

    UtilitiesService service

    def setup() {
        service = new UtilitiesService(
                serviceMapper,
                serviceRepository,
                utilityAddService,
                userService,
                servicePublishService,
                accountQueryService,
                movementRepository
        )
    }

    def buildSubscription(Long accountId, LocalDate lastPayment = null) {
        def account = Stub(Account) { getId() >> accountId; getName() >> "Mi cuenta" }
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        return new Subscription(id: 1L, description: "Netflix", amount: new BigDecimal("10.00"),
                lastPayment: lastPayment, account: account, currency: currency)
    }

    // --- payServiceById ---

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

    def "updateService - should update service when called"() {
        given:
        def sub = buildSubscription(2L)  // lastPayment null → isPaid false
        def dto = new UpdateSubscriptionRecord(new BigDecimal("15.00"), null, null, "Netflix HD")
        serviceRepository.findById(20L) >> Optional.of(sub)

        when:
        service.updateService(20L, dto)

        then:
        1 * serviceRepository.save(sub)
        0 * movementRepository.findByDescriptionAndAccountAndMonth(_, _, _, _)
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

    def "updateService - should update associated movement when subscription isPaid true"() {
        given:
        def now = LocalDate.now(ZoneOffset.UTC)
        def sub = buildSubscription(5L, now)  // isPaid = true
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, "Netflix HD")
        def movement = new Movement(description: "Servicio Pagado Netflix", amount: new BigDecimal("10.00"))

        serviceRepository.findById(50L) >> Optional.of(sub)
        serviceRepository.save(sub) >> sub
        movementRepository.findByDescriptionAndAccountAndMonth(
                "Servicio Pagado Netflix", 5L, now.year, now.monthValue) >> Optional.of(movement)

        when:
        service.updateService(50L, dto)

        then:
        1 * movementRepository.save(movement)
        movement.amount == new BigDecimal("20.00")
        movement.description == "Servicio Pagado Netflix HD"
    }

    def "updateService - should throw EntityNotFoundException when movement not found and isPaid true"() {
        given:
        def now = LocalDate.now(ZoneOffset.UTC)
        def sub = buildSubscription(5L, now)  // isPaid = true
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, null)

        serviceRepository.findById(50L) >> Optional.of(sub)
        serviceRepository.save(sub) >> sub
        movementRepository.findByDescriptionAndAccountAndMonth(
                "Servicio Pagado Netflix", 5L, now.year, now.monthValue) >> Optional.empty()

        when:
        service.updateService(50L, dto)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.save(_ as Movement)
    }

    def "updateService - should not touch movements when isPaid false"() {
        given:
        // lastPayment en mes anterior → isPaid false
        def lastMonth = LocalDate.now(ZoneOffset.UTC).minusMonths(1)
        def sub = buildSubscription(5L, lastMonth)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, null)

        serviceRepository.findById(50L) >> Optional.of(sub)
        serviceRepository.save(sub) >> sub

        when:
        service.updateService(50L, dto)

        then:
        0 * movementRepository.findByDescriptionAndAccountAndMonth(_, _, _, _)
        0 * movementRepository.save(_ as Movement)
    }

    // --- deleteService ---

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

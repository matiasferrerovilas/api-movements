package api.m2.movements.unit.services

import api.m2.movements.movements.entities.commons.Currency
import api.m2.movements.movements.entities.movements.Subscription

import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.movements.mappers.SubscriptionMapper
import api.m2.movements.movements.records.services.ServiceDeletedEvent
import api.m2.movements.movements.records.services.UpdateSubscriptionRecord
import api.m2.movements.movements.records.subscriptions.SubscriptionMovementSyncEvent
import api.m2.movements.movements.records.subscriptions.SubscriptionPaidEvent
import api.m2.movements.movements.repositories.CurrencyRepository
import api.m2.movements.movements.repositories.SubscriptionRepository
import api.m2.movements.movements.services.subscriptions.SubscriptionAddService
import api.m2.movements.identity.services.user.UserService
import api.m2.movements.identity.services.workspaces.WorkspaceContextService
import api.m2.movements.identity.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class SubscriptionAddServiceTest extends Specification {

    SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper)
    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    CurrencyRepository currencyRepository = Mock(CurrencyRepository)
    UserService userService = Mock(UserService)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)

    SubscriptionAddService service

    def setup() {
        service = new SubscriptionAddService(
                subscriptionMapper,
                subscriptionRepository,
                currencyRepository,
                userService,
                workspaceContextService,
                workspaceQueryService,
                eventPublisher
        )
    }

    def buildSubscription(Long workspaceId, LocalDate lastPayment = null) {
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        return new Subscription(id: 1L, description: "Netflix", amount: new BigDecimal("10.00"),
                lastPayment: lastPayment, workspaceId: workspaceId, currency: currency, ownerId: 1L)
    }

    // --- paySubscriptionById ---

    def "paySubscriptionById - should publish SubscriptionPaidEvent and save"() {
        given:
        def subscription = buildSubscription(1L)
        subscriptionRepository.findById(10L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.paySubscriptionById(10L)

        then:
        1 * eventPublisher.publishEvent(_ as SubscriptionPaidEvent) >> { List args ->
            def event = args[0] as SubscriptionPaidEvent
            assert event.amount() == new BigDecimal("10.00")
            assert event.currencySymbol() == "ARS"
            assert event.description() == "Servicio Pagado Netflix"
        }
        1 * subscriptionRepository.save(subscription)
    }

    def "paySubscriptionById - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        subscriptionRepository.findById(999L) >> Optional.empty()

        when:
        service.paySubscriptionById(999L)

        then:
        thrown(EntityNotFoundException)
        0 * eventPublisher.publishEvent(_ as SubscriptionPaidEvent)
    }

    def "paySubscriptionById - should set payment date to today UTC"() {
        given:
        def subscription = buildSubscription(1L, null)
        subscriptionRepository.findById(1L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.paySubscriptionById(1L)

        then:
        1 * eventPublisher.publishEvent(_ as SubscriptionPaidEvent) >> { List args ->
            def event = args[0] as SubscriptionPaidEvent
            assert event.paymentDate() == LocalDate.now(ZoneOffset.UTC)
        }
    }

    // --- updateSubscription ---

    def "updateSubscription - should save subscription when called"() {
        given:
        def subscription = buildSubscription(2L)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("15.00"), null, null, "Netflix HD")
        subscriptionRepository.findById(20L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.updateSubscription(20L, dto)

        then:
        1 * subscriptionRepository.save(subscription)
    }

    def "updateSubscription - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        subscriptionRepository.findById(999L) >> Optional.empty()

        when:
        service.updateSubscription(999L, new UpdateSubscriptionRecord(null, null, null, null))

        then:
        thrown(EntityNotFoundException)
        0 * subscriptionRepository.save(_ as Subscription)
    }

    def "updateSubscription - should publish SubscriptionMovementSyncEvent when subscription isPaid"() {
        given:
        def now = LocalDate.now(ZoneOffset.UTC)
        def subscription = buildSubscription(5L, now)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, "Netflix HD")

        subscriptionRepository.findById(50L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.updateSubscription(50L, dto)

        then:
        1 * eventPublisher.publishEvent(_ as SubscriptionMovementSyncEvent) >> { List args ->
            def event = args[0] as SubscriptionMovementSyncEvent
            assert event.oldDescription() == "Netflix"
            assert event.workspaceId() == 5L
            assert event.year() == now.year
            assert event.month() == now.monthValue
            assert event.newAmount() == new BigDecimal("20.00")
            assert event.newDescription() == "Netflix HD"
        }
    }

    def "updateSubscription - should not publish sync event when subscription is not paid"() {
        given:
        def subscription = buildSubscription(5L, null)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, null)

        subscriptionRepository.findById(50L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.updateSubscription(50L, dto)

        then:
        0 * eventPublisher.publishEvent(_ as SubscriptionMovementSyncEvent)
    }

    // --- deleteSubscription ---

    def "deleteSubscription - should delete and publish websocket event"() {
        given:
        def subscription = buildSubscription(3L)
        subscriptionRepository.findByIdWithCurrency(30L) >> Optional.of(subscription)

        when:
        service.deleteSubscription(30L)

        then:
        1 * subscriptionRepository.delete(subscription)
        1 * eventPublisher.publishEvent(_ as ServiceDeletedEvent)
    }

    def "deleteSubscription - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        subscriptionRepository.findByIdWithCurrency(999L) >> Optional.empty()

        when:
        service.deleteSubscription(999L)

        then:
        thrown(EntityNotFoundException)
        0 * subscriptionRepository.delete(_ as Subscription)
    }
}

package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
import api.m2.movements.entities.Subscription
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.SubscriptionMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.services.UpdateSubscriptionRecord
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.subscriptions.SubscriptionAddService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class SubscriptionAddServiceTest extends Specification {

    SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper)
    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    CurrencyRepository currencyRepository = Mock(CurrencyRepository)
    MovementRepository movementRepository = Mock(MovementRepository)
    MovementAddService movementAddService = Mock(MovementAddService)
    CategoryAddService categoryAddService = Mock(CategoryAddService)
    UserService userService = Mock(UserService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    ServicePublishServiceWebSocket servicePublishService = Mock(ServicePublishServiceWebSocket)
    UserSettingService userSettingService = Mock(UserSettingService)

    SubscriptionAddService service

    def setup() {
        service = new SubscriptionAddService(
                subscriptionMapper,
                subscriptionRepository,
                currencyRepository,
                movementRepository,
                movementAddService,
                categoryAddService,
                userService,
                workspaceQueryService,
                servicePublishService,
                userSettingService
        )
    }

    def buildSubscription(Long workspaceId, LocalDate lastPayment = null) {
        def workspace = Stub(Workspace) { getId() >> workspaceId; getName() >> "Mi cuenta" }
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def owner = Stub(User)
        return new Subscription(id: 1L, description: "Netflix", amount: new BigDecimal("10.00"),
                lastPayment: lastPayment, workspace: workspace, currency: currency, owner: owner)
    }

    // --- addMovementForSubscription ---

    def "addMovementForSubscription - should use default bank when user has DEFAULT_BANK configured"() {
        given:
        def subscription = buildSubscription(10L, LocalDate.of(2026, 3, 1))
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }

        categoryAddService.findCategoryByDescription("SERVICIOS") >>
                Stub(CategoryRecord) { description() >> "SERVICIOS" }
        userSettingService.getDefaultBank(subscription.owner) >> Optional.of(bank)

        when:
        service.addMovementForSubscription(subscription)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.bank() == "GALICIA"
            assert m.amount() == new BigDecimal("10.00")
            assert m.type() == MovementType.DEBITO.name()
            assert m.currency() == "ARS"
            assert m.workspaceId() == 10L
        }
    }

    def "addMovementForSubscription - should use null bank when user has no DEFAULT_BANK configured"() {
        given:
        def subscription = buildSubscription(10L, LocalDate.of(2026, 3, 1))

        categoryAddService.findCategoryByDescription("SERVICIOS") >>
                Stub(CategoryRecord) { description() >> "SERVICIOS" }
        userSettingService.getDefaultBank(subscription.owner) >> Optional.empty()

        when:
        service.addMovementForSubscription(subscription)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.bank() == null
        }
    }

    def "addMovementForSubscription - should use lastPayment date when present"() {
        given:
        def subscription = buildSubscription(10L, LocalDate.of(2026, 3, 1))

        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "Servicios" }
        userSettingService.getDefaultBank(subscription.owner) >> Optional.empty()

        when:
        service.addMovementForSubscription(subscription)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.date() == LocalDate.of(2026, 3, 1)
        }
    }

    def "addMovementForSubscription - should fallback to today UTC when lastPayment is null"() {
        given:
        def subscription = buildSubscription(10L, null)

        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "Servicios" }
        userSettingService.getDefaultBank(subscription.owner) >> Optional.empty()

        when:
        service.addMovementForSubscription(subscription)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.date() == LocalDate.now(ZoneOffset.UTC)
        }
    }

    def "addMovementForSubscription - should build description with subscription name"() {
        given:
        def subscription = buildSubscription(10L, LocalDate.of(2026, 3, 1))

        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "Servicios" }
        userSettingService.getDefaultBank(subscription.owner) >> Optional.empty()

        when:
        service.addMovementForSubscription(subscription)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.description() == "Servicio Pagado Netflix"
        }
    }

    // --- paySubscriptionById ---

    def "paySubscriptionById - should pay subscription when called"() {
        given:
        def subscription = buildSubscription(1L)
        subscriptionRepository.findById(10L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription
        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "SERVICIOS" }
        userSettingService.getDefaultBank(_) >> Optional.empty()

        when:
        service.paySubscriptionById(10L)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd)
        1 * subscriptionRepository.save(subscription)
    }

    def "paySubscriptionById - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        subscriptionRepository.findById(999L) >> Optional.empty()

        when:
        service.paySubscriptionById(999L)

        then:
        thrown(EntityNotFoundException)
        0 * movementAddService.saveMovement(_ as MovementToAdd)
    }

    // --- updateSubscription ---

    def "updateSubscription - should update subscription when called"() {
        given:
        def subscription = buildSubscription(2L)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("15.00"), null, null, "Netflix HD")
        subscriptionRepository.findById(20L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.updateSubscription(20L, dto)

        then:
        1 * subscriptionRepository.save(subscription)
        0 * movementRepository.findByDescriptionAndAccountAndMonth(_, _, _, _)
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

    def "updateSubscription - should update associated movement when subscription isPaid true"() {
        given:
        def now = LocalDate.now(ZoneOffset.UTC)
        def subscription = buildSubscription(5L, now)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, "Netflix HD")
        def movement = new Movement(description: "Servicio Pagado Netflix", amount: new BigDecimal("10.00"))

        subscriptionRepository.findById(50L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription
        movementRepository.findByDescriptionAndAccountAndMonth(
                "Servicio Pagado Netflix", 5L, now.year, now.monthValue) >> Optional.of(movement)

        when:
        service.updateSubscription(50L, dto)

        then:
        1 * movementRepository.save(movement)
        movement.amount == new BigDecimal("20.00")
        movement.description == "Servicio Pagado Netflix HD"
    }

    def "updateSubscription - should throw EntityNotFoundException when movement not found and isPaid true"() {
        given:
        def now = LocalDate.now(ZoneOffset.UTC)
        def subscription = buildSubscription(5L, now)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, null)

        subscriptionRepository.findById(50L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription
        movementRepository.findByDescriptionAndAccountAndMonth(
                "Servicio Pagado Netflix", 5L, now.year, now.monthValue) >> Optional.empty()

        when:
        service.updateSubscription(50L, dto)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.save(_ as Movement)
    }

    def "updateSubscription - should not touch movements when isPaid false"() {
        given:
        def lastMonth = LocalDate.now(ZoneOffset.UTC).minusMonths(1)
        def subscription = buildSubscription(5L, lastMonth)
        def dto = new UpdateSubscriptionRecord(new BigDecimal("20.00"), null, null, null)

        subscriptionRepository.findById(50L) >> Optional.of(subscription)
        subscriptionRepository.save(subscription) >> subscription

        when:
        service.updateSubscription(50L, dto)

        then:
        0 * movementRepository.findByDescriptionAndAccountAndMonth(_, _, _, _)
        0 * movementRepository.save(_ as Movement)
    }

    // --- deleteSubscription ---

    def "deleteSubscription - should delete and publish event when called"() {
        given:
        def subscription = buildSubscription(3L)
        subscriptionRepository.findByIdWithCurrency(30L) >> Optional.of(subscription)

        when:
        service.deleteSubscription(30L)

        then:
        1 * subscriptionRepository.delete(subscription)
        1 * servicePublishService.publishDeleteService(_ as api.m2.movements.records.services.SubscriptionRecord)
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

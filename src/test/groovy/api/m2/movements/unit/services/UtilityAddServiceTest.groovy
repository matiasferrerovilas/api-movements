package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Subscription
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MovementType
import api.m2.movements.mappers.SubscriptionMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.subscriptions.UtilityAddService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class UtilityAddServiceTest extends Specification {

    CategoryAddService categoryAddService = Mock()
    MovementAddService movementAddService = Mock()
    UserSettingService userSettingService = Mock()

    UtilityAddService service

    def setup() {
        service = new UtilityAddService(
                Mappers.getMapper(SubscriptionMapper),
                Mock(SubscriptionRepository),
                Mock(CurrencyRepository),
                movementAddService,
                categoryAddService,
                Mock(UserService),
                Mock(WorkspaceQueryService),
                Mock(ServicePublishServiceWebSocket),
                userSettingService
        )
    }

    def buildSubscription() {
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def workspace  = Stub(Workspace)  { getId()     >> 10L }
        def owner    = Stub(User)

        def sub = Stub(Subscription) {
            getAmount()      >> new BigDecimal("500.00")
            getLastPayment() >> LocalDate.of(2026, 3, 1)
            getDescription() >> "Netflix"
            getCurrency()    >> currency
            getWorkspace()   >> workspace
            getOwner()       >> owner
        }
        return [sub: sub, owner: owner]
    }

    def "addMovementService - should use default bank when user has DEFAULT_BANK configured"() {
        given:
        def fixtures = buildSubscription()
        def sub   = fixtures.sub as Subscription
        def owner = fixtures.owner as User
        def bank  = Stub(Bank) { getDescription() >> "GALICIA" }

        categoryAddService.findCategoryByDescription("SERVICIOS") >>
                Stub(CategoryRecord) { description() >> "SERVICIOS" }
        userSettingService.getDefaultBank(owner) >> Optional.of(bank)

        when:
        service.addMovementService(sub)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.bank()     == "GALICIA"
            assert m.amount()   == new BigDecimal("500.00")
            assert m.type()     == MovementType.DEBITO.name()
            assert m.currency() == "ARS"
            assert m.workspaceId()  == 10L
        }
    }

    def "addMovementService - should use null bank when user has no DEFAULT_BANK configured"() {
        given:
        def fixtures = buildSubscription()
        def sub   = fixtures.sub as Subscription
        def owner = fixtures.owner as User

        categoryAddService.findCategoryByDescription("SERVICIOS") >>
                Stub(CategoryRecord) { description() >> "SERVICIOS" }
        userSettingService.getDefaultBank(owner) >> Optional.empty()

        when:
        service.addMovementService(sub)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.bank() == null
        }
    }

    def "addMovementService - should use lastPayment date when present"() {
        given:
        def fixtures = buildSubscription()
        def sub   = fixtures.sub as Subscription
        def owner = fixtures.owner as User

        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "Servicios" }
        userSettingService.getDefaultBank(owner) >> Optional.empty()

        when:
        service.addMovementService(sub)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.date() == LocalDate.of(2026, 3, 1)
        }
    }

    def "addMovementService - should fallback to today UTC when lastPayment is null"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def workspace  = Stub(Workspace)  { getId()     >> 10L }
        def owner    = Stub(User)
        def sub = Stub(Subscription) {
            getAmount()      >> new BigDecimal("100.00")
            getLastPayment() >> null
            getDescription() >> "Spotify"
            getCurrency()    >> currency
            getWorkspace()   >> workspace
            getOwner()       >> owner
        }

        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "Servicios" }
        userSettingService.getDefaultBank(owner) >> Optional.empty()

        when:
        service.addMovementService(sub)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.date() == LocalDate.now(ZoneOffset.UTC)
        }
    }

    def "addMovementService - should build description with subscription name"() {
        given:
        def fixtures = buildSubscription()
        def sub   = fixtures.sub as Subscription
        def owner = fixtures.owner as User

        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "Servicios" }
        userSettingService.getDefaultBank(owner) >> Optional.empty()

        when:
        service.addMovementService(sub)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.description() == "Servicio Pagado Netflix"
        }
    }
}

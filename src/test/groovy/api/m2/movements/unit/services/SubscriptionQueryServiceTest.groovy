package api.m2.movements.unit.services

import api.m2.movements.entities.Currency
import api.m2.movements.entities.Subscription
import api.m2.movements.entities.Workspace
import api.m2.movements.mappers.SubscriptionMapper
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.subscriptions.SubscriptionQueryService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

import java.time.LocalDate

class SubscriptionQueryServiceTest extends Specification {

    SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper)
    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    UserService userService = Mock(UserService)

    SubscriptionQueryService service

    def setup() {
        service = new SubscriptionQueryService(
                subscriptionMapper,
                subscriptionRepository,
                userService
        )
    }

    def "getSubscriptionsBy - should return subscriptions filtered by currency and lastPayment"() {
        given:
        def userId = 1L
        def currencySymbols = ["ARS", "USD"]
        def lastPayment = LocalDate.of(2026, 3, 1)

        def workspace = Stub(Workspace) { getId() >> 10L; getName() >> "Mi cuenta" }
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def subscription = new Subscription(
                id: 1L,
                description: "Netflix",
                amount: new BigDecimal("10.00"),
                lastPayment: lastPayment,
                workspace: workspace,
                currency: currency
        )

        userService.getAuthenticatedUserRecord() >> new UserBaseRecord("test@test.com", userId)
        subscriptionRepository.findByCurrencyAndLastPayment(userId, currencySymbols, lastPayment) >> [subscription]

        when:
        def result = service.getSubscriptionsBy(currencySymbols, lastPayment)

        then:
        result.size() == 1
        result[0].description() == "Netflix"
    }

    def "getSubscriptionsBy - should return empty list when no subscriptions match"() {
        given:
        def userId = 1L
        def currencySymbols = ["EUR"]
        def lastPayment = LocalDate.of(2026, 3, 1)

        userService.getAuthenticatedUserRecord() >> new UserBaseRecord("test@test.com", userId)
        subscriptionRepository.findByCurrencyAndLastPayment(userId, currencySymbols, lastPayment) >> []

        when:
        def result = service.getSubscriptionsBy(currencySymbols, lastPayment)

        then:
        result.isEmpty()
    }
}

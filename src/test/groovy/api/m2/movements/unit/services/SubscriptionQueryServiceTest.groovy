package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Subscription

import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.mappers.SubscriptionMapper
import api.m2.movements.records.workspaces.WorkspaceMemberDTO
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.subscriptions.SubscriptionQueryService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

import java.time.LocalDate

class SubscriptionQueryServiceTest extends Specification {

    SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper)
    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    UserService userService = Mock(UserService)

    SubscriptionQueryService service

    def setup() {
        service = new SubscriptionQueryService(
                subscriptionMapper,
                subscriptionRepository,
                workspaceContextService,
                userService
        )
    }

    def "getSubscriptionsBy - should return subscriptions filtered by currency and lastPayment, enriched with workspace and owner names"() {
        given:
        def workspaceId = 10L
        def currencySymbols = ["ARS", "USD"]
        def lastPayment = LocalDate.of(2026, 3, 1)

        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def subscription = new Subscription(
                id: 1L,
                description: "Netflix",
                amount: new BigDecimal("10.00"),
                lastPayment: lastPayment,
                workspaceId: workspaceId,
                currency: currency,
                ownerId: 1L
        )

        workspaceContextService.getActiveWorkspace() >> new WorkspaceMemberDTO(
                1L, workspaceId, "Familia", new WorkspaceMemberDTO.Metadata([], WorkspaceRole.OWNER, null, false))
        subscriptionRepository.findByWorkspaceAndCurrencyAndLastPayment(workspaceId, currencySymbols, lastPayment) >> [subscription]
        userService.getUserNamesByIds([1L] as List<Long>) >> [1L: "Matias"]

        when:
        def result = service.getSubscriptionsBy(currencySymbols, lastPayment)

        then:
        result.size() == 1
        result[0].description() == "Netflix"
        result[0].workspaceName() == "Familia"
        result[0].user() == "Matias"
    }

    def "getSubscriptionsBy - should return empty list when no subscriptions match without calling identity for owner names"() {
        given:
        def workspaceId = 10L
        def currencySymbols = ["EUR"]
        def lastPayment = LocalDate.of(2026, 3, 1)

        workspaceContextService.getActiveWorkspace() >> new WorkspaceMemberDTO(
                1L, workspaceId, "Familia", new WorkspaceMemberDTO.Metadata([], WorkspaceRole.OWNER, null, false))
        subscriptionRepository.findByWorkspaceAndCurrencyAndLastPayment(workspaceId, currencySymbols, lastPayment) >> []

        when:
        def result = service.getSubscriptionsBy(currencySymbols, lastPayment)

        then:
        result.isEmpty()
        0 * userService.getUserNamesByIds(_ as List<Long>)
    }
}

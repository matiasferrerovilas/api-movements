package api.m2.movements.unit.services

import api.m2.movements.entities.WorkspaceCurrency
import api.m2.movements.entities.commons.Currency
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.records.currencies.CurrencyToAdd
import api.m2.movements.repositories.WorkspaceCurrencyRepository
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.currencies.WorkspaceCurrencyService
import api.m2.movements.services.workspaces.WorkspaceContextService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

class WorkspaceCurrencyServiceTest extends Specification {

    WorkspaceCurrencyRepository workspaceCurrencyRepository = Mock(WorkspaceCurrencyRepository)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)

    WorkspaceCurrencyService service

    def setup() {
        service = new WorkspaceCurrencyService(
                workspaceCurrencyRepository,
                currencyAddService,
                workspaceContextService,
                workspaceQueryService
        )
    }

    def "getWorkspaceCurrencies - should return currencies for active workspace, always deletable"() {
        given:
        def workspaceId = 1L
        def currency = Stub(Currency) { getId() >> 5L; getSymbol() >> "BTC"; getDescription() >> "Bitcoin" }
        def workspaceCurrency = Stub(WorkspaceCurrency) { getId() >> 20L; getCurrency() >> currency }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCurrencyRepository.findByWorkspaceId(workspaceId) >> [workspaceCurrency]

        when:
        def result = service.getWorkspaceCurrencies()

        then:
        result.size() == 1
        result[0].id() == 20L
        result[0].symbol() == "BTC"
        result[0].description() == "Bitcoin"
        result[0].isDeletable()
    }

    def "addCurrency - should create currency and associate to workspace"() {
        given:
        def workspaceId = 1L
        def dto = new CurrencyToAdd("BTC", "Bitcoin")
        def currency = Stub(Currency) { getId() >> 5L; getSymbol() >> "BTC"; getDescription() >> "Bitcoin" }
        def workspaceCurrency = Stub(WorkspaceCurrency) { getId() >> 20L; getCurrency() >> currency }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        currencyAddService.addCurrency("BTC", "Bitcoin") >> currency
        workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(workspaceId, 5L) >> Optional.empty()
        workspaceCurrencyRepository.save(_ as WorkspaceCurrency) >> workspaceCurrency

        when:
        def result = service.addCurrency(dto)

        then:
        result.symbol() == "BTC"
        result.isDeletable()
    }

    def "addCurrency - should be idempotent when currency already associated"() {
        given:
        def workspaceId = 1L
        def dto = new CurrencyToAdd("BTC", "Bitcoin")
        def currency = Stub(Currency) { getId() >> 5L; getSymbol() >> "BTC"; getDescription() >> "Bitcoin" }
        def existing = Stub(WorkspaceCurrency) { getId() >> 20L; getCurrency() >> currency }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        currencyAddService.addCurrency("BTC", "Bitcoin") >> currency
        workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(workspaceId, 5L) >> Optional.of(existing)

        when:
        service.addCurrency(dto)

        then:
        0 * workspaceCurrencyRepository.save(_ as WorkspaceCurrency)
    }

    def "addDefaultCurrencies - should associate the default currency to the workspace"() {
        given:
        def usd = Stub(Currency) { getId() >> 2L }

        currencyAddService.getDefaultCurrency() >> usd
        workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(1L, 2L) >> Optional.empty()

        when:
        service.addDefaultCurrencies(1L)

        then:
        1 * workspaceCurrencyRepository.save(_ as WorkspaceCurrency)
    }

    def "ensureCurrencyInWorkspace - should create association if not exists"() {
        given:
        def currency = Stub(Currency) { getId() >> 5L }
        workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(1L, 5L) >> Optional.empty()

        when:
        service.ensureCurrencyInWorkspace(1L, currency)

        then:
        1 * workspaceCurrencyRepository.save(_ as WorkspaceCurrency)
    }

    def "ensureCurrencyInWorkspace - should not duplicate if already exists"() {
        given:
        def currency = Stub(Currency) { getId() >> 5L }
        def existing = Stub(WorkspaceCurrency)
        workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(1L, 5L) >> Optional.of(existing)

        when:
        service.ensureCurrencyInWorkspace(1L, currency)

        then:
        0 * workspaceCurrencyRepository.save(_ as WorkspaceCurrency)
    }

    def "deleteCurrency - should delete currency from workspace regardless of catalog or usage"() {
        given:
        def workspaceId = 1L
        def workspaceCurrencyId = 10L
        def currency = Stub(Currency) { getId() >> 5L }
        def workspaceCurrency = Stub(WorkspaceCurrency) {
            getWorkspaceId() >> workspaceId
            getCurrency() >> currency
        }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCurrencyRepository.findById(workspaceCurrencyId) >> Optional.of(workspaceCurrency)

        when:
        service.deleteCurrency(workspaceCurrencyId)

        then:
        1 * workspaceCurrencyRepository.delete(workspaceCurrency)
    }

    def "deleteCurrency - should throw EntityNotFoundException when not found"() {
        given:
        workspaceContextService.getActiveWorkspaceId() >> 1L
        workspaceCurrencyRepository.findById(999L) >> Optional.empty()

        when:
        service.deleteCurrency(999L)

        then:
        thrown(EntityNotFoundException)
    }

    def "deleteCurrency - should throw PermissionDeniedException when currency belongs to different workspace"() {
        given:
        def workspaceCurrency = Stub(WorkspaceCurrency) { getWorkspaceId() >> 999L }

        workspaceContextService.getActiveWorkspaceId() >> 1L
        workspaceCurrencyRepository.findById(10L) >> Optional.of(workspaceCurrency)

        when:
        service.deleteCurrency(10L)

        then:
        thrown(PermissionDeniedException)
        0 * workspaceCurrencyRepository.delete(_)
    }
}

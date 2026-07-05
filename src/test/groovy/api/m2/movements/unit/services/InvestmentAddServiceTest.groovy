package api.m2.movements.unit.services

import api.m2.movements.movements.entities.commons.Currency
import api.m2.movements.movements.entities.integrity.User
import api.m2.movements.movements.entities.integrity.Workspace
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.investment.entities.Investment
import api.m2.movements.investment.entities.InvestmentType
import api.m2.movements.investment.mappers.InvestmentMapper
import api.m2.movements.investment.repositories.InvestmentRepository
import api.m2.movements.investment.repositories.InvestmentTypeRepository
import api.m2.movements.investment.services.InvestmentAddService
import api.m2.movements.investment.records.InvestmentAddedEvent
import api.m2.movements.investment.records.InvestmentDeletedEvent
import api.m2.movements.investment.records.InvestmentRecord
import api.m2.movements.investment.records.InvestmentToAdd
import api.m2.movements.investment.records.InvestmentToUpdate
import api.m2.movements.investment.records.InvestmentUpdatedEvent
import api.m2.movements.movements.repositories.CurrencyRepository
import api.m2.movements.movements.services.user.UserService
import api.m2.movements.movements.services.workspaces.WorkspaceContextService
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

import java.time.LocalDate

class InvestmentAddServiceTest extends Specification {

    InvestmentRepository investmentRepository = Mock(InvestmentRepository)
    InvestmentMapper investmentMapper = Mock(InvestmentMapper)
    InvestmentTypeRepository investmentTypeRepository = Mock(InvestmentTypeRepository)
    CurrencyRepository currencyRepository = Mock(CurrencyRepository)
    UserService userService = Mock(UserService)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)

    InvestmentAddService service

    def setup() {
        service = new InvestmentAddService(
                investmentRepository,
                investmentMapper,
                investmentTypeRepository,
                currencyRepository,
                userService,
                workspaceContextService,
                eventPublisher
        )
    }

    def buildInvestment(Long workspaceId = 1L) {
        def workspace = Stub(Workspace) { getId() >> workspaceId }
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def type = Stub(InvestmentType) { getId() >> 1L }
        def owner = Stub(User)
        return new Investment(id: 10L, amount: new BigDecimal("50000.00"),
                startDate: LocalDate.of(2026, 1, 1), workspace: workspace,
                currency: currency, investmentType: type, owner: owner)
    }

    def buildRecord(Long workspaceId = 1L) {
        return Stub(InvestmentRecord) { workspaceId() >> workspaceId }
    }

    // --- add ---

    def "add - should save investment and publish event"() {
        given:
        def dto = new InvestmentToAdd(new BigDecimal("50000.00"), LocalDate.of(2026, 1, 1),
                null, "Plazo Fijo Galicia", null, null, 1L, "ARS")
        def workspace = Stub(Workspace) { getId() >> 1L }
        def user = Stub(User)
        def currency = Stub(Currency)
        def investmentType = Stub(InvestmentType)
        def investment = buildInvestment()
        def record = buildRecord()

        workspaceContextService.getActiveWorkspace() >> workspace
        userService.getAuthenticatedUser() >> user
        currencyRepository.findBySymbol("ARS") >> Optional.of(currency)
        investmentTypeRepository.findById(1L) >> Optional.of(investmentType)
        investmentRepository.save(_ as Investment) >> investment
        investmentMapper.toRecord(investment) >> record

        when:
        service.add(dto)

        then:
        1 * investmentRepository.save(_ as Investment) >> investment
        1 * eventPublisher.publishEvent(_ as InvestmentAddedEvent)
    }

    def "add - should throw EntityNotFoundException when currency not found"() {
        given:
        def dto = new InvestmentToAdd(new BigDecimal("1000.00"), LocalDate.now(),
                null, null, null, null, 1L, "USD_INEXISTENTE")

        workspaceContextService.getActiveWorkspace() >> Stub(Workspace)
        userService.getAuthenticatedUser() >> Stub(User)
        currencyRepository.findBySymbol("USD_INEXISTENTE") >> Optional.empty()

        when:
        service.add(dto)

        then:
        thrown(EntityNotFoundException)
        0 * investmentRepository.save(_ as Investment)
    }

    def "add - should throw EntityNotFoundException when investment type not found"() {
        given:
        def dto = new InvestmentToAdd(new BigDecimal("1000.00"), LocalDate.now(),
                null, null, null, null, 999L, "ARS")

        workspaceContextService.getActiveWorkspace() >> Stub(Workspace)
        userService.getAuthenticatedUser() >> Stub(User)
        currencyRepository.findBySymbol("ARS") >> Optional.of(Stub(Currency))
        investmentTypeRepository.findById(999L) >> Optional.empty()

        when:
        service.add(dto)

        then:
        thrown(EntityNotFoundException)
        0 * investmentRepository.save(_ as Investment)
    }

    // --- update ---

    def "update - should update investment and publish event"() {
        given:
        def investment = buildInvestment()
        def dto = new InvestmentToUpdate(new BigDecimal("60000.00"), null, null, null, null, null, null, null)
        def record = buildRecord()

        investmentRepository.findById(10L) >> Optional.of(investment)
        investmentRepository.save(investment) >> investment
        investmentMapper.toRecord(investment) >> record

        when:
        service.update(dto, 10L)

        then:
        1 * investmentRepository.save(investment) >> investment
        1 * eventPublisher.publishEvent(_ as InvestmentUpdatedEvent)
    }

    def "update - should throw EntityNotFoundException when investment not found"() {
        given:
        investmentRepository.findById(999L) >> Optional.empty()

        when:
        service.update(new InvestmentToUpdate(null, null, null, null, null, null, null, null), 999L)

        then:
        thrown(EntityNotFoundException)
        0 * investmentRepository.save(_ as Investment)
    }

    // --- delete ---

    def "delete - should delete investment and publish event"() {
        given:
        def investment = buildInvestment()
        def record = buildRecord()

        investmentRepository.findById(10L) >> Optional.of(investment)
        investmentMapper.toRecord(investment) >> record

        when:
        service.delete(10L)

        then:
        1 * investmentRepository.delete(investment)
        1 * eventPublisher.publishEvent(_ as InvestmentDeletedEvent)
    }

    def "delete - should throw EntityNotFoundException when investment not found"() {
        given:
        investmentRepository.findById(999L) >> Optional.empty()

        when:
        service.delete(999L)

        then:
        thrown(EntityNotFoundException)
        0 * investmentRepository.delete(_ as Investment)
    }
}

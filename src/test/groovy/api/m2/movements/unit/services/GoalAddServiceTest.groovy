package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.entities.Goal
import api.m2.movements.entities.commons.Currency
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.GoalMapper
import api.m2.movements.records.goals.GoalContribution
import api.m2.movements.records.goals.GoalRecord
import api.m2.movements.records.goals.GoalToAdd
import api.m2.movements.records.goals.GoalToUpdate
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.GoalRepository
import api.m2.movements.services.currencies.WorkspaceCurrencyService
import api.m2.movements.services.goals.GoalAddService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

import java.time.LocalDate

class GoalAddServiceTest extends Specification {

    GoalRepository goalRepository = Mock()
    GoalMapper goalMapper = Mock()
    CurrencyRepository currencyRepository = Mock()
    WorkspaceCurrencyService workspaceCurrencyService = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()
    UserService userService = Mock()

    GoalAddService service

    def setup() {
        service = new GoalAddService(
                goalRepository,
                goalMapper,
                currencyRepository,
                workspaceCurrencyService,
                workspaceQueryService,
                userService
        )
    }

    // --- save ---

    def "save - should verify membership, zero out currentAmount and persist the goal"() {
        given:
        def dto = new GoalToAdd(5L, "Viaje a Bariloche", new BigDecimal("500000.00"), "ARS", null)
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def goal = new Goal(currency: currency)
        def savedGoal = new Goal(id: 1L, workspaceId: 5L, currency: currency)
        def record = new GoalRecord(1L, 5L, "Viaje a Bariloche", new BigDecimal("500000.00"),
                BigDecimal.ZERO, null, null, BigDecimal.ZERO, null)

        userService.getMe() >> new UserMe(9L, "a@b.com", "A", "B", "PERSONAL", null)
        goalMapper.toEntity(dto, currencyRepository) >> goal
        goalRepository.save(goal) >> savedGoal
        goalMapper.toRecord(savedGoal) >> record

        when:
        def result = service.save(dto)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(5L, 9L)
        1 * workspaceCurrencyService.ensureCurrencyInWorkspace(5L, currency)
        goal.workspaceId == 5L
        goal.currentAmount == BigDecimal.ZERO
        result == record
    }

    // --- update ---

    def "update - should update only the provided fields"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def goal = new Goal(id: 10L, workspaceId: 5L, name: "Old", currency: currency,
                targetAmount: new BigDecimal("1000.00"))
        def dto = new GoalToUpdate("New name", new BigDecimal("2000.00"), LocalDate.of(2027, 1, 1))

        goalRepository.findById(10L) >> Optional.of(goal)
        goalMapper.toRecord(goal) >> new GoalRecord(10L, 5L, "New name", new BigDecimal("2000.00"),
                BigDecimal.ZERO, null, LocalDate.of(2027, 1, 1), BigDecimal.ZERO, null)

        when:
        service.update(dto, 10L)

        then:
        1 * goalRepository.save(goal)
        goal.name == "New name"
        goal.targetAmount == new BigDecimal("2000.00")
        goal.targetDate == LocalDate.of(2027, 1, 1)
    }

    def "update - should throw EntityNotFoundException when goal does not exist"() {
        given:
        goalRepository.findById(999L) >> Optional.empty()

        when:
        service.update(new GoalToUpdate("x", null, null), 999L)

        then:
        thrown(EntityNotFoundException)
        0 * goalRepository.save(_ as Goal)
    }

    // --- contribute ---

    def "contribute - should increment currentAmount by the given amount"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def goal = new Goal(id: 10L, workspaceId: 5L, currency: currency,
                currentAmount: new BigDecimal("100.00"))
        def dto = new GoalContribution(new BigDecimal("50.00"))

        goalRepository.findById(10L) >> Optional.of(goal)
        goalMapper.toRecord(goal) >> new GoalRecord(10L, 5L, "Goal", new BigDecimal("500.00"),
                new BigDecimal("150.00"), null, null, new BigDecimal("30.00"), null)

        when:
        service.contribute(dto, 10L)

        then:
        1 * goalRepository.save(goal)
        goal.currentAmount == new BigDecimal("150.00")
    }

    def "contribute - should throw EntityNotFoundException when goal does not exist"() {
        given:
        goalRepository.findById(999L) >> Optional.empty()

        when:
        service.contribute(new GoalContribution(new BigDecimal("10.00")), 999L)

        then:
        thrown(EntityNotFoundException)
        0 * goalRepository.save(_ as Goal)
    }

    // --- delete ---

    def "delete - should remove goal when it exists"() {
        given:
        goalRepository.existsById(5L) >> true

        when:
        service.delete(5L)

        then:
        1 * goalRepository.deleteById(5L)
    }

    def "delete - should throw EntityNotFoundException when goal does not exist"() {
        given:
        goalRepository.existsById(999L) >> false

        when:
        service.delete(999L)

        then:
        thrown(EntityNotFoundException)
        0 * goalRepository.deleteById(_ as Long)
    }
}

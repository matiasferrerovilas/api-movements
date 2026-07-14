package api.m2.movements.unit.services


import api.m2.movements.movements.services.income.IncomeAddService
import api.m2.movements.movements.services.income.RecurringIncomeJob
import api.m2.movements.movements.services.user.UserService
import spock.lang.Specification

class RecurringIncomeJobTest extends Specification {

    UserService userService = Mock(UserService)
    IncomeAddService incomeAddService = Mock(IncomeAddService)

    RecurringIncomeJob job

    def setup() {
        job = new RecurringIncomeJob(userService, incomeAddService)
    }

    def "generateRecurringIncomes - should not call incomeAddService when user list is empty"() {
        given:
        userService.getUsersWithAutoIncomeEnabled() >> []

        when:
        job.generateRecurringIncomes()

        then:
        0 * incomeAddService.generateRecurringIncomeForUser(_ as Long)
    }

    def "generateRecurringIncomes - should call generateRecurringIncomeForUser once for a single user"() {
        given:
        def userId = 1L
        userService.getUsersWithAutoIncomeEnabled() >> [userId]
        incomeAddService.generateRecurringIncomeForUser(userId) >> 2

        when:
        job.generateRecurringIncomes()

        then:
        1 * incomeAddService.generateRecurringIncomeForUser(userId) >> 2
    }

    def "generateRecurringIncomes - should call generateRecurringIncomeForUser once per user"() {
        given:
        def userId1 = 1L
        def userId2 = 2L
        def userId3 = 3L
        userService.getUsersWithAutoIncomeEnabled() >> [userId1, userId2, userId3]

        when:
        job.generateRecurringIncomes()

        then:
        1 * incomeAddService.generateRecurringIncomeForUser(userId1) >> 1
        1 * incomeAddService.generateRecurringIncomeForUser(userId2) >> 3
        1 * incomeAddService.generateRecurringIncomeForUser(userId3) >> 2
    }

    def "generateRecurringIncomes - should call getUsersWithAutoIncomeEnabled exactly once"() {
        given:
        userService.getUsersWithAutoIncomeEnabled() >> []

        when:
        job.generateRecurringIncomes()

        then:
        1 * userService.getUsersWithAutoIncomeEnabled() >> []
    }

    def "generateRecurringIncomes - should continue processing other users when one fails"() {
        given:
        def userId1 = 1L
        def userId2 = 2L
        def userId3 = 3L
        userService.getUsersWithAutoIncomeEnabled() >> [userId1, userId2, userId3]
        incomeAddService.generateRecurringIncomeForUser(userId1) >> 1
        incomeAddService.generateRecurringIncomeForUser(userId2) >> { throw new RuntimeException("DB error") }
        incomeAddService.generateRecurringIncomeForUser(userId3) >> 2

        when:
        job.generateRecurringIncomes()

        then:
        noExceptionThrown()
        1 * incomeAddService.generateRecurringIncomeForUser(userId1)
        1 * incomeAddService.generateRecurringIncomeForUser(userId2)
        1 * incomeAddService.generateRecurringIncomeForUser(userId3)
    }
}

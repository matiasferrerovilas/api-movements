package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.services.income.RecurringIncomeJob
import api.m2.movements.services.user.UserService
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
        0 * incomeAddService.generateRecurringIncomeForUser(_ as User)
    }

    def "generateRecurringIncomes - should call generateRecurringIncomeForUser once for a single user"() {
        given:
        def user = Stub(User) { getEmail() >> "user@test.com" }
        userService.getUsersWithAutoIncomeEnabled() >> [user]
        incomeAddService.generateRecurringIncomeForUser(user) >> 2

        when:
        job.generateRecurringIncomes()

        then:
        1 * incomeAddService.generateRecurringIncomeForUser(user) >> 2
    }

    def "generateRecurringIncomes - should call generateRecurringIncomeForUser once per user"() {
        given:
        def user1 = Stub(User) { getEmail() >> "user1@test.com" }
        def user2 = Stub(User) { getEmail() >> "user2@test.com" }
        def user3 = Stub(User) { getEmail() >> "user3@test.com" }
        userService.getUsersWithAutoIncomeEnabled() >> [user1, user2, user3]

        when:
        job.generateRecurringIncomes()

        then:
        1 * incomeAddService.generateRecurringIncomeForUser(user1) >> 1
        1 * incomeAddService.generateRecurringIncomeForUser(user2) >> 3
        1 * incomeAddService.generateRecurringIncomeForUser(user3) >> 2
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
        def user1 = Stub(User) { getEmail() >> "user1@test.com" }
        def user2 = Stub(User) { getEmail() >> "user2@test.com" }
        def user3 = Stub(User) { getEmail() >> "user3@test.com" }
        userService.getUsersWithAutoIncomeEnabled() >> [user1, user2, user3]
        incomeAddService.generateRecurringIncomeForUser(user1) >> 1
        incomeAddService.generateRecurringIncomeForUser(user2) >> { throw new RuntimeException("DB error") }
        incomeAddService.generateRecurringIncomeForUser(user3) >> 2

        when:
        job.generateRecurringIncomes()

        then:
        noExceptionThrown()
        1 * incomeAddService.generateRecurringIncomeForUser(user1)
        1 * incomeAddService.generateRecurringIncomeForUser(user2)
        1 * incomeAddService.generateRecurringIncomeForUser(user3)
    }
}

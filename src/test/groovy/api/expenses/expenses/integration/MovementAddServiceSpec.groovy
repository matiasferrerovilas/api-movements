package api.expenses.expenses.integration

import api.expenses.expenses.repositories.CategoryRepository
import api.expenses.expenses.repositories.CurrencyRepository
import api.expenses.expenses.repositories.GroupRepository
import api.expenses.expenses.repositories.MovementRepository
import api.expenses.expenses.repositories.UserRepository
import api.expenses.expenses.services.movements.MovementAddService
import api.expenses.expenses.services.movements.MovementGetService
import org.springframework.beans.factory.annotation.Autowired


class MovementAddServiceSpec extends CommonSpec {

    @Autowired
    MovementAddService movementAddService

    @Autowired
    MovementGetService movementGetService

    @Autowired
    CurrencyRepository currencyRepository

    @Autowired
    MovementRepository movementRepository

    @Autowired
    UserRepository userRepository

    @Autowired
    GroupRepository groupRepository

    @Autowired
    CategoryRepository categoryRepository

    def setup() {
        movementRepository.deleteAll()
    }
}
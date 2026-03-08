package api.m2.movements.unit.unit

import api.m2.movements.services.ingreso.CronIngreso
import api.m2.movements.enums.CategoryEnum
import api.m2.movements.enums.MovementType
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.movements.MovementToGet
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.MovementGetService
import spock.lang.Specification
import spock.lang.Subject
import java.math.BigDecimal
import java.time.LocalDate

class CronIngresoTest extends Specification {

    MovementGetService movementGetService = Mock(MovementGetService)
    MovementAddService movementAddService = Mock(MovementAddService)
    CategoryAddService categoryAddService = Mock(CategoryAddService)

    @Subject
    CronIngreso service

    def setup() {
        service = new CronIngreso(movementGetService, movementAddService, categoryAddService)
    }

    def "createIngresoMovement - should call movementAddService with correct parameters"() {
        given:
        def lastIngreso = Stub(MovementToGet) {
            getAmount()      >> new BigDecimal("1000.00")
            getDescription() >> "Sueldo Recibido"
            getCurrency()     >> Stub() {
                getSymbol() >> "EUR"
            }
            getBank()         >> "GALICIA"
        }
        def category = Stub() {
            getDescription() >> CategoryEnum.HOGAR.getDescripcion()
        }
        movementGetService.getLastIngreso() >> lastIngreso
        categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion()) >> category

        when:
        service.createIngresoMovement()

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m ->
            assert m.amount()      == new BigDecimal("1000.00")
            assert m.date()        == LocalDate.now()
            assert m.description() == "Sueldo Recibido"
            assert m.category()     == CategoryEnum.HOGAR.getDescripcion()
            assert m.type()        == MovementType.INGRESO.name()
            assert m.currency()    == "EUR"
            assert m.bank()        == "GALICIA"
            assert m.accountId()  == 0
        })
    }

    def "createIngresoMovement - should call movementGetService to get last ingreso"() {
        when:
        service.createIngresoMovement()

        then:
        1 * movementGetService.getLastIngreso() 
    }

    def "createIngresoMovement - should call categoryAddService to find category by description"() {
        given:
        movementGetService.getLastIngreso() >> Stub(MovementToGet)

        when:
        service.createIngresoMovement()

        then:
        1 * categoryAddService.findCategoryByDescription(_ as String) 
    }
}
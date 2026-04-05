package api.m2.movements.unit.unit

import api.m2.movements.entities.Category
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.category.CategoryResolver
import spock.lang.Specification
import spock.lang.Subject

class CategoryResolverTest extends Specification {

    CategoryAddService categoryAddService = Mock(CategoryAddService)

    @Subject
    CategoryResolver service

    def setup() {
        service = new CategoryResolver(categoryAddService)
    }

    def "resolve - should add category when record is null"() {
        given:
        def defaultCategory = "Default Category"
        categoryAddService.getDefaultCategory() >> defaultCategory
        def category = Stub(Category) { getId() >> 1L }

        when:
        def result = service.resolve(null as CategoryRecord)

        then:
        1 * categoryAddService.addCategory(defaultCategory) >> category
        result == category
    }

    def "resolve - should add category when record is not null"() {
        given:
        def description = "Test Category"
        def record = new CategoryRecord(1L, description, true, true)
        def category = Stub(Category) { getId() >> 1L }

        when:
        def result = service.resolve(record)

        then:
        1 * categoryAddService.addCategory(description) >> category
        result == category
    }

    def "resolve - should add category when description is provided"() {
        given:
        def description = "Test Category"
        def category = Stub(Category) { getId() >> 1L }

        when:
        def result = service.resolve(description)

        then:
        1 * categoryAddService.addCategory(description) >> category
        result == category
    }
}
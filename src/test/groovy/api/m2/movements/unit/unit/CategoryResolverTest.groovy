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
    CategoryResolver resolver

    def setup() {
        resolver = new CategoryResolver(categoryAddService)
    }

    def "resolve - should add category with default description when record is null"() {
        given:
        def defaultCategory = "Default Category"
        categoryAddService.getDefaultCategory() >> defaultCategory
        def category = Stub(Category) { getDescription() >> defaultCategory }

        when:
        def result = resolver.resolve(null as CategoryRecord)

        then:
        1 * categoryAddService.addCategory(defaultCategory) >> category
        result == category
    }

    def "resolve - should add category with record description when record is not null"() {
        given:
        def description = "Test Category"
        def record = new CategoryRecord(description)
        def category = Stub(Category) { getDescription() >> description }

        when:
        def result = resolver.resolve(record)

        then:
        1 * categoryAddService.addCategory(description) >> category
        result == category
    }

    def "resolve - should add category with given description"() {
        given:
        def description = "Test Category"
        def category = Stub(Category) { getDescription() >> description }

        when:
        def result = resolver.resolve(description)

        then:
        1 * categoryAddService.addCategory(description) >> category
        result == category
    }
}
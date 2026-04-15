package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.services.category.CategoryAddService
import spock.lang.Specification

class CategoryAddServiceTest extends Specification {

    CategoryRepository categoryRepository = Mock(CategoryRepository)
    CategoryMapper categoryMapper = Mock(CategoryMapper)

    CategoryAddService service

    def setup() {
        service = new CategoryAddService(categoryRepository, categoryMapper)
    }

    def "addCategory - should return existing category when found"() {
        given:
        def existingCategory = Stub(Category) {
            getDescription() >> "HOGAR"
            getId() >> 1L
        }
        categoryRepository.findByDescription("HOGAR") >> Optional.of(existingCategory)

        when:
        def result = service.addCategory("hogar")

        then:
        result == existingCategory
        0 * categoryRepository.save(_ as Category)
    }

    def "addCategory - should create new category when not found"() {
        given:
        categoryRepository.findByDescription("NUEVA_CATEGORIA") >> Optional.empty()
        def savedCategory = Stub(Category) {
            getDescription() >> "NUEVA_CATEGORIA"
            getId() >> 5L
        }
        categoryRepository.save(_ as Category) >> savedCategory

        when:
        def result = service.addCategory("nueva_categoria")

        then:
        result == savedCategory
    }

    def "addCategory - should normalize description to uppercase and trim"() {
        given:
        def category = Stub(Category)
        categoryRepository.findByDescription("TRIMMED") >> Optional.of(category)

        when:
        def result = service.addCategory("  trimmed  ")

        then:
        result == category
    }

    def "findCategoryByDescription - should return category record when found"() {
        given:
        def category = Stub(Category) { getDescription() >> "HOGAR" }
        def categoryRecord = new CategoryRecord(1L, "HOGAR", true, true, null, null)

        categoryRepository.findByDescription("HOGAR") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.findCategoryByDescription("hogar")

        then:
        result.description() == "HOGAR"
    }

    def "findCategoryByDescription - should throw EntityNotFoundException when not found"() {
        given:
        categoryRepository.findByDescription("UNKNOWN") >> Optional.empty()

        when:
        service.findCategoryByDescription("unknown")

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("unknown")
    }

    def "getDefaultCategory - should return SIN_CATEGORIA"() {
        when:
        def result = service.getDefaultCategory()

        then:
        result == "SIN_CATEGORIA"
    }

    def "resolveDefaultCategory - should return SIN_CATEGORIA when description is blank"() {
        given:
        def category = Stub(Category) { getDescription() >> "SIN_CATEGORIA" }
        def categoryRecord = new CategoryRecord(1L, "SIN_CATEGORIA", true, false, null, null)

        categoryRepository.findByDescription("SIN_CATEGORIA") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.resolveDefaultCategory("  ")

        then:
        result.description() == "SIN_CATEGORIA"
    }

    def "resolveDefaultCategory - should return STREAMING for Netflix"() {
        given:
        def category = Stub(Category) { getDescription() >> "STREAMING" }
        def categoryRecord = new CategoryRecord(2L, "STREAMING", true, true, null, null)

        categoryRepository.findByDescription("STREAMING") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.resolveDefaultCategory("Netflix Premium")

        then:
        result.description() == "STREAMING"
    }

    def "resolveDefaultCategory - should return STREAMING for HBO"() {
        given:
        def category = Stub(Category) { getDescription() >> "STREAMING" }
        def categoryRecord = new CategoryRecord(2L, "STREAMING", true, true, null, null)

        categoryRepository.findByDescription("STREAMING") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.resolveDefaultCategory("hbo max subscription")

        then:
        result.description() == "STREAMING"
    }

    def "resolveDefaultCategory - should return STREAMING for Disney+"() {
        given:
        def category = Stub(Category) { getDescription() >> "STREAMING" }
        def categoryRecord = new CategoryRecord(2L, "STREAMING", true, true, null, null)

        categoryRepository.findByDescription("STREAMING") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.resolveDefaultCategory("Disney+ Annual")

        then:
        result.description() == "STREAMING"
    }

    def "resolveDefaultCategory - should return SERVICIOS for Spotify"() {
        given:
        def category = Stub(Category) { getDescription() >> "SERVICIOS" }
        def categoryRecord = new CategoryRecord(3L, "SERVICIOS", true, true, null, null)

        categoryRepository.findByDescription("SERVICIOS") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.resolveDefaultCategory("Spotify Premium")

        then:
        result.description() == "SERVICIOS"
    }

    def "resolveDefaultCategory - should return SIN_CATEGORIA for unrecognized description"() {
        given:
        def category = Stub(Category) { getDescription() >> "SIN_CATEGORIA" }
        def categoryRecord = new CategoryRecord(1L, "SIN_CATEGORIA", true, false, null, null)

        categoryRepository.findByDescription("SIN_CATEGORIA") >> Optional.of(category)
        categoryMapper.toRecord(category) >> categoryRecord

        when:
        def result = service.resolveDefaultCategory("Random purchase")

        then:
        result.description() == "SIN_CATEGORIA"
    }
}

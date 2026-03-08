package api.m2.movements.unit.services

import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.entities.Category
import api.m2.movements.enums.CategoryEnum
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.repositories.CategoryRepository
import jakarta.persistence.EntityNotFoundException
import org.mapstruct.factory.Mappers
import spock.lang.Specification
import spock.lang.Subject

class CategoryAddServiceTest extends Specification {

    CategoryRepository categoryRepository = Mock(CategoryRepository)
    CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper)

    @Subject
    CategoryAddService service

    def setup() {
        service = new CategoryAddService(categoryRepository, categoryMapper)
    }

    def "addCategory - should save new category when not found"() {
        given:
        def description = "New Category"
        categoryRepository.findByDescription(_ as String) >> Optional.empty()

        when:
        def result = service.addCategory(description)

        then:
        1 * categoryRepository.save(_ as Category) >> { args ->
            def category = args[0] as Category
            assert category.description == description.toUpperCase()
            category
        }
        result.description == description.toUpperCase()
    }

    def "addCategory - should return existing category when found"() {
        given:
        def description = "Existing Category"
        def existingCategory = Stub(Category) {
            getDescription() >> description.toUpperCase()
        }
        categoryRepository.findByDescription(_ as String) >> Optional.of(existingCategory)

        when:
        def result = service.addCategory(description)

        then:
        0 * categoryRepository.save(_ as Category)
        result == existingCategory
    }

    def "findCategoryByDescription - should return category record when found"() {
        given:
        def description = CategoryEnum.HOGAR.getDescripcion()
        def category = Stub(Category) {
            getDescription() >> description
        }
        categoryRepository.findByDescription(_ as String) >> Optional.of(category)

        when:
        def result = service.findCategoryByDescription(description)

        then:
        result.description == description
    }

    def "findCategoryByDescription - should throw EntityNotFoundException when not found"() {
        given:
        def description = "Non Existing Category"
        categoryRepository.findByDescription(_ as String) >> Optional.empty()

        when:
        service.findCategoryByDescription(description)

        then:
        thrown(EntityNotFoundException)
    }

    def "getAllCategories - should return list of category records"() {
        given:
        def categories = [
                Stub(Category) { getDescription() >> CategoryEnum.HOGAR.getDescripcion() },
                Stub(Category) { getDescription() >> CategoryEnum.SIN_CATEGORIA.getDescripcion() }
        ]
        categoryRepository.findAll() >> categories

        when:
        def result = service.getAllCategories()

        then:
        result.size() == 2
        result.find { it.description == CategoryEnum.HOGAR.getDescripcion() } != null
        result.find { it.description == CategoryEnum.SIN_CATEGORIA.getDescripcion() } != null
    }

    def "getDefaultCategory - should return default category description"() {
        when:
        def result = service.getDefaultCategory()

        then:
        result == CategoryEnum.SIN_CATEGORIA.getDescripcion()
    }

    def "getCategoryAtLoadDefaultByStringHelper - should return default category when description is blank"() {
        given:
        def description = ""
        def defaultCategory = Stub(CategoryRecord) {
            getDescription() >> CategoryEnum.SIN_CATEGORIA.getDescripcion()
        }
        categoryRepository.findByDescription(_ as String) >> Optional.of(Stub(Category) {
            getDescription() >> CategoryEnum.SIN_CATEGORIA.getDescripcion()
        })

        when:
        def result = service.getCategoryAtLoadDefaultByStringHelper(description)

        then:
        result.description == CategoryEnum.SIN_CATEGORIA.getDescripcion()
    }

    def "getCategoryAtLoadDefaultByStringHelper - should return streaming category when description contains streaming keywords"() {
        given:
        def description = "netflix"
        def streamingCategory = Stub(CategoryRecord) {
            getDescription() >> CategoryEnum.STREAMING.getDescripcion()
        }
        categoryRepository.findByDescription(_ as String) >> Optional.of(Stub(Category) {
            getDescription() >> CategoryEnum.STREAMING.getDescripcion()
        })

        when:
        def result = service.getCategoryAtLoadDefaultByStringHelper(description)

        then:
        result.description == CategoryEnum.STREAMING.getDescripcion()
    }

    def "getCategoryAtLoadDefaultByStringHelper - should return servicios category when description contains spotify"() {
        given:
        def description = "spotify"
        def serviciosCategory = Stub(CategoryRecord) {
            getDescription() >> CategoryEnum.SERVICIOS.getDescripcion()
        }
        categoryRepository.findByDescription(_ as String) >> Optional.of(Stub(Category) {
            getDescription() >> CategoryEnum.SERVICIOS.getDescripcion()
        })

        when:
        def result = service.getCategoryAtLoadDefaultByStringHelper(description)

        then:
        result.description == CategoryEnum.SERVICIOS.getDescripcion()
    }

    def "getCategoryAtLoadDefaultByStringHelper - should return default category when description does not match any keywords"() {
        given:
        def description = "other"
        def defaultCategory = Stub(CategoryRecord) {
            getDescription() >> CategoryEnum.SIN_CATEGORIA.getDescripcion()
        }
        categoryRepository.findByDescription(_ as String) >> Optional.of(Stub(Category) {
            getDescription() >> CategoryEnum.SIN_CATEGORIA.getDescripcion()
        })

        when:
        def result = service.getCategoryAtLoadDefaultByStringHelper(description)

        then:
        result.description == CategoryEnum.SIN_CATEGORIA.getDescripcion()
    }
}
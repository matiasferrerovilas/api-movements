package api.m2.movements.unit.services

import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.entities.Category
import api.m2.movements.enums.CategoryEnum
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.repositories.CategoryRepository
import org.springframework.dao.EmptyResultDataAccessException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static api.m2.movements.Mappers.getMapper

class CategoryAddServiceTest extends Specification {

    CategoryRepository categoryRepository = Mock(CategoryRepository)
    CategoryMapper categoryMapper = getMapper(CategoryMapper)

    @Subject
    CategoryAddService service

    def setup() {
        service = new CategoryAddService(categoryRepository, categoryMapper)
    }

    def "addCategory - should save new category when not found"() {
        given:
        def description = "New Category"
        categoryRepository.findByDescription(description.toUpperCase()) >> Optional.empty()

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
        def existingCategory = new Category(1L, description.toUpperCase())
        categoryRepository.findByDescription(description.toUpperCase()) >> Optional.of(existingCategory)

        when:
        def result = service.addCategory(description)

        then:
        0 * categoryRepository.save(_ as Category)
        result == existingCategory
    }

    def "findCategoryByDescription - should return category when found"() {
        given:
        def description = "Existing Category"
        def existingCategory = new Category(1L, description.toUpperCase())
        categoryRepository.findByDescription(description.toUpperCase()) >> Optional.of(existingCategory)

        when:
        def result = service.findCategoryByDescription(description)

        then:
        result.description == description.toUpperCase()
    }

    def "findCategoryByDescription - should throw EntityNotFoundException when not found"() {
        given:
        def description = "Non Existing Category"
        categoryRepository.findByDescription(description.toUpperCase()) >> Optional.empty()

        when:
        service.findCategoryByDescription(description)

        then:
        thrown(EntityNotFoundException)
    }

    def "getAllCategories - should return all categories"() {
        given:
        def categories = [new Category(1L, "Category 1"), new Category(2L, "Category 2")]
        categoryRepository.findAll() >> categories

        when:
        def result = service.getAllCategories()

        then:
        result.size() == 2
        result[0].description == "Category 1"
        result[1].description == "Category 2"
    }

    def "getDefaultCategory - should return default category"() {
        when:
        def result = service.getDefaultCategory()

        then:
        result == CategoryEnum.SIN_CATEGORIA.getDescripcion()
    }

    @Unroll
    def "getCategoryAtLoadDefaultByStringHelper - should return category based on description"() {
        given:
        def description = desc
        def category = new Category(1L, catDesc)
        categoryRepository.findByDescription(catDesc.toUpperCase()) >> Optional.of(category)

        when:
        def result = service.getCategoryAtLoadDefaultByStringHelper(description)

        then:
        result.description == catDesc

        where:
        desc                        | catDesc
        ""                          | CategoryEnum.SIN_CATEGORIA.getDescripcion()
        "Netflix"                   | CategoryEnum.STREAMING.getDescripcion()
        "HBO"                        | CategoryEnum.STREAMING.getDescripcion()
        "Disney+"                    | CategoryEnum.STREAMING.getDescripcion()
        "Spotify"                    | CategoryEnum.SERVICIOS.getDescripcion()
        "Other"                       | CategoryEnum.SIN_CATEGORIA.getDescripcion()
    }
}
package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Category
import api.m2.movements.records.categories.CategoryCorrectedEvent
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.services.category.rules.CategoryRuleLearningHandler
import api.m2.movements.services.category.rules.CategoryRuleService
import spock.lang.Specification

class CategoryRuleLearningHandlerTest extends Specification {

    CategoryRuleService categoryRuleService = Mock(CategoryRuleService)
    CategoryRepository categoryRepository = Stub(CategoryRepository)
    CategoryRuleLearningHandler handler

    def setup() {
        handler = new CategoryRuleLearningHandler(categoryRuleService, categoryRepository)
    }

    def "onCategoryCorrected - should learn the rule and apply it to uncategorized movements"() {
        given:
        def supermercado = Category.builder().id(5L).description("SUPERMERCADO").build()
        categoryRepository.findById(5L) >> Optional.of(supermercado)

        when:
        handler.onCategoryCorrected(new CategoryCorrectedEvent(1L, "Mercadona", 5L))

        then:
        1 * categoryRuleService.learn(1L, "Mercadona", supermercado)
        1 * categoryRuleService.applyToUncategorized(1L, "Mercadona", supermercado)
    }

    def "onCategoryCorrected - should do nothing when the category no longer exists"() {
        given:
        categoryRepository.findById(9L) >> Optional.empty()

        when:
        handler.onCategoryCorrected(new CategoryCorrectedEvent(1L, "Mercadona", 9L))

        then:
        0 * categoryRuleService.learn(_ as Long, _ as String, _ as Category)
        0 * categoryRuleService.applyToUncategorized(_ as Long, _ as String, _ as Category)
    }
}

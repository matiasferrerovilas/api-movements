package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.CategoryRule
import api.m2.movements.entities.movements.Movement
import api.m2.movements.repositories.CategoryRuleRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.category.rules.CategoryRuleService
import api.m2.movements.services.category.rules.MerchantKeyNormalizer
import spock.lang.Specification

class CategoryRuleServiceTest extends Specification {

    CategoryRuleRepository categoryRuleRepository = Mock(CategoryRuleRepository)
    MovementRepository movementRepository = Mock(MovementRepository)
    CategoryRuleService service

    Category uncategorized = Category.builder().id(1L).description("SIN CATEGORIA").build()
    Category supermercado = Category.builder().id(5L).description("SUPERMERCADO").build()
    Category hogar = Category.builder().id(2L).description("HOGAR").build()

    def setup() {
        service = new CategoryRuleService(categoryRuleRepository, movementRepository, new MerchantKeyNormalizer())
    }

    def movement(String description, Category... categories) {
        Movement.builder().description(description).workspaceId(1L).categories(categories as Set).build()
    }

    def rule(String key, Category category, int hits = 1) {
        CategoryRule.builder().workspaceId(1L).merchantKey(key).category(category).hits(hits).build()
    }

    def "learn - should create a rule for a new merchant"() {
        given:
        categoryRuleRepository.findByWorkspaceIdAndMerchantKey(1L, "MERCADONA VALENCIA") >> Optional.empty()

        when:
        service.learn(1L, "Mercadona Valencia 1234", supermercado)

        then:
        1 * categoryRuleRepository.save(_ as CategoryRule) >> { List args ->
            def saved = args[0] as CategoryRule
            assert saved.merchantKey == "MERCADONA VALENCIA"
            assert saved.category.is(supermercado)
            assert saved.hits == 1
        }
    }

    def "learn - should reinforce an existing rule with the same category"() {
        given:
        def existing = rule("MERCADONA VALENCIA", supermercado, 3)
        categoryRuleRepository.findByWorkspaceIdAndMerchantKey(1L, "MERCADONA VALENCIA") >> Optional.of(existing)

        when:
        service.learn(1L, "Mercadona Valencia", supermercado)

        then:
        1 * categoryRuleRepository.save(_ as CategoryRule) >> { List args ->
            assert (args[0] as CategoryRule).hits == 4
        }
    }

    def "learn - should let the last correction win when the category is different"() {
        given:
        def existing = rule("MERCADONA VALENCIA", hogar, 3)
        categoryRuleRepository.findByWorkspaceIdAndMerchantKey(1L, "MERCADONA VALENCIA") >> Optional.of(existing)

        when:
        service.learn(1L, "Mercadona Valencia", supermercado)

        then:
        1 * categoryRuleRepository.save(_ as CategoryRule) >> { List args ->
            def saved = args[0] as CategoryRule
            assert saved.category.is(supermercado)
            assert saved.hits == 1
        }
    }

    def "learn - should ignore corrections to 'Sin categoría' and descriptions without a merchant"() {
        when:
        service.learn(1L, "Mercadona", uncategorized)
        service.learn(1L, "1234", supermercado)

        then:
        0 * categoryRuleRepository.save(_ as CategoryRule)
    }

    def "matcherFor - should build rules from history the first time and match exact key or first word"() {
        given:
        categoryRuleRepository.existsByWorkspaceId(1L) >> false
        movementRepository.findByWorkspaceIdWithCategories(1L) >> [
                movement("Lidl Valencia", supermercado),
                movement("Lidl Valencia", supermercado),
                movement("Lidl Valencia", hogar),
                movement("Farmacia Centro", uncategorized),
                movement("Ikea Alfafar", hogar, supermercado)
        ]
        List<CategoryRule> saved = []
        categoryRuleRepository.findByWorkspaceIdWithCategory(1L) >> { saved }

        when:
        def matcher = service.matcherFor(1L)

        then:
        1 * categoryRuleRepository.saveAll(_ as List<CategoryRule>) >> { List args ->
            saved.addAll(args[0] as List<CategoryRule>)
            assert saved*.merchantKey == ["LIDL VALENCIA"]
            assert saved[0].category.is(supermercado)
            assert saved[0].hits == 2
            saved
        }
        matcher.match("LIDL VALENCIA 0001").get() == "SUPERMERCADO"
        matcher.match("Lidl Madrid").get() == "SUPERMERCADO"
        matcher.match("Farmacia Centro").isEmpty()
    }

    def "matcherFor - should not rebuild from history when the workspace already has rules"() {
        given:
        categoryRuleRepository.existsByWorkspaceId(1L) >> true
        categoryRuleRepository.findByWorkspaceIdWithCategory(1L) >> [rule("BIZUM ANA", hogar)]

        when:
        def matcher = service.matcherFor(1L)

        then:
        0 * movementRepository.findByWorkspaceIdWithCategories(_ as Long)
        matcher.match("Bizum De Ana").get() == "HOGAR"
        matcher.match("Bizum De Juan").isEmpty()
    }

    def "applyToUncategorized - should recategorize only uncategorized movements of the same merchant"() {
        given:
        def same = movement("MERCADONA VALENCIA 99", uncategorized)
        def other = movement("Lidl Valencia", uncategorized)
        movementRepository.findByWorkspaceIdAndCategoryDescription(1L, "SIN CATEGORIA") >> [same, other]

        when:
        def updated = service.applyToUncategorized(1L, "Mercadona Valencia", supermercado)

        then:
        updated == 1
        same.categories == [supermercado] as Set
        other.categories == [uncategorized] as Set
        1 * movementRepository.saveAll({ it.size() == 1 && it[0].is(same) })
    }
}

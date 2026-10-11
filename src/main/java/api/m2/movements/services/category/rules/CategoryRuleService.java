package api.m2.movements.services.category.rules;

import api.m2.movements.entities.commons.Category;
import api.m2.movements.entities.commons.CategoryRule;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.repositories.CategoryRuleRepository;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Aprende a categorizar a partir de las correcciones del usuario: cada vez que le cambia la
 * categoría a un movimiento se guarda "comercio → categoría" y los próximos imports del mismo
 * comercio entran ya categorizados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryRuleService {

    private static final String UNCATEGORIZED = DefaultCategory.SIN_CATEGORIA.getDescription();

    private final CategoryRuleRepository categoryRuleRepository;
    private final MovementRepository movementRepository;
    private final MerchantKeyNormalizer normalizer;

    /**
     * Reglas del workspace listas para usar en un import. La primera vez (workspace sin reglas) las
     * arma desde los movimientos que ya tienen categoría, para no arrancar de cero.
     */
    @Transactional
    public CategoryMatcher matcherFor(Long workspaceId) {
        if (!categoryRuleRepository.existsByWorkspaceId(workspaceId)) {
            this.bootstrapFromHistory(workspaceId);
        }
        Map<String, String> byKey = new HashMap<>();
        Map<String, CategoryRule> bestByHead = new HashMap<>();
        for (CategoryRule rule : categoryRuleRepository.findByWorkspaceIdWithCategory(workspaceId)) {
            byKey.put(rule.getMerchantKey(), rule.getCategory().getDescription());
            var head = normalizer.headOf(rule.getMerchantKey());
            if (!head.isEmpty()) {
                bestByHead.merge(head, rule, (a, b) -> a.getHits() >= b.getHits() ? a : b);
            }
        }
        Map<String, String> byHead = new HashMap<>();
        bestByHead.forEach((head, rule) -> byHead.put(head, rule.getCategory().getDescription()));
        return new CategoryMatcher(normalizer, byKey, byHead);
    }

    /**
     * Guarda o actualiza la regla del comercio de {@code description}. Si ya existía con otra
     * categoría gana la última corrección.
     */
    @Transactional
    public void learn(Long workspaceId, String description, Category category) {
        var key = normalizer.keyOf(description);
        if (key.isEmpty() || this.isUncategorized(category)) {
            return;
        }
        var rule = categoryRuleRepository.findByWorkspaceIdAndMerchantKey(workspaceId, key)
                .map(existing -> this.reinforce(existing, category))
                .orElseGet(() -> CategoryRule.builder()
                        .workspaceId(workspaceId)
                        .merchantKey(key)
                        .category(category)
                        .build());
        categoryRuleRepository.save(rule);
        log.info("Regla de categoría aprendida: workspace={}, comercio='{}', categoría='{}'",
                workspaceId, key, category.getDescription());
    }

    /**
     * Pasa a {@code category} los movimientos del workspace que siguen "Sin categoría" y son del
     * mismo comercio que {@code description}. Devuelve cuántos cambió.
     */
    @Transactional
    public int applyToUncategorized(Long workspaceId, String description, Category category) {
        var key = normalizer.keyOf(description);
        if (key.isEmpty() || this.isUncategorized(category)) {
            return 0;
        }
        var updated = movementRepository.findByWorkspaceIdAndCategoryDescription(workspaceId, UNCATEGORIZED).stream()
                .filter(movement -> key.equals(normalizer.keyOf(movement.getDescription())))
                .toList();
        updated.forEach(movement -> movement.setCategories(new HashSet<>(Set.of(category))));
        movementRepository.saveAll(updated);
        if (!updated.isEmpty()) {
            log.info("Recategorizados {} movimientos sin categoría de '{}' a '{}'", updated.size(), key, category.getDescription());
        }
        return updated.size();
    }

    private void bootstrapFromHistory(Long workspaceId) {
        Map<String, Map<Category, Integer>> votes = new HashMap<>();
        for (Movement movement : movementRepository.findByWorkspaceIdWithCategories(workspaceId)) {
            this.singleRealCategory(movement).ifPresent(category -> {
                var key = normalizer.keyOf(movement.getDescription());
                if (!key.isEmpty()) {
                    votes.computeIfAbsent(key, k -> new HashMap<>()).merge(category, 1, Integer::sum);
                }
            });
        }
        var rules = votes.entrySet().stream()
                .map(entry -> {
                    var winner = entry.getValue().entrySet().stream()
                            .max(Comparator.comparingInt(Map.Entry::getValue))
                            .orElseThrow();
                    return CategoryRule.builder()
                            .workspaceId(workspaceId)
                            .merchantKey(entry.getKey())
                            .category(winner.getKey())
                            .hits(winner.getValue())
                            .build();
                })
                .toList();
        categoryRuleRepository.saveAll(rules);
        log.info("Reglas de categoría iniciales para workspace={}: {}", workspaceId, rules.size());
    }

    private Optional<Category> singleRealCategory(Movement movement) {
        var real = movement.getCategories().stream().filter(category -> !this.isUncategorized(category)).toList();
        return real.size() == 1 ? Optional.of(real.getFirst()) : Optional.empty();
    }

    private CategoryRule reinforce(CategoryRule rule, Category category) {
        if (rule.getCategory().getId().equals(category.getId())) {
            rule.setHits(rule.getHits() + 1);
        } else {
            rule.setCategory(category);
            rule.setHits(1);
        }
        return rule;
    }

    private boolean isUncategorized(Category category) {
        return UNCATEGORIZED.equals(category.getDescription());
    }
}

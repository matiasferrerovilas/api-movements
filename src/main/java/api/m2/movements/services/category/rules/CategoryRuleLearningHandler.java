package api.m2.movements.services.category.rules;

import api.m2.movements.records.categories.CategoryCorrectedEvent;
import api.m2.movements.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuando el usuario corrige la categoría de un movimiento, guarda la regla del comercio y
 * recategoriza los demás movimientos del mismo comercio que seguían "Sin categoría".
 */
@Component
@RequiredArgsConstructor
public class CategoryRuleLearningHandler {

    private final CategoryRuleService categoryRuleService;
    private final CategoryRepository categoryRepository;

    @EventListener
    @Transactional
    public void onCategoryCorrected(CategoryCorrectedEvent event) {
        categoryRepository.findById(event.categoryId()).ifPresent(category -> {
            categoryRuleService.learn(event.workspaceId(), event.movementDescription(), category);
            categoryRuleService.applyToUncategorized(event.workspaceId(), event.movementDescription(), category);
        });
    }
}

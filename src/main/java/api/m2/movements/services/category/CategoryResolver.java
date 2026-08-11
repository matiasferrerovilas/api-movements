package api.m2.movements.services.category;

import api.m2.movements.entities.commons.Category;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryResolver {
    private final CategoryAddService categoryAddService;
    private final WorkspaceCategoryService workspaceCategoryService;

    public List<Category> resolveAll(List<CategoryUpdateRecord> categories, Long workspaceId) {
        if (categories == null || categories.isEmpty()) {
            return List.of(this.resolveAndEnsureInWorkspace(categoryAddService.getDefaultCategory(), workspaceId));
        }
        return categories.stream()
                .map(category -> this.resolveAndEnsureInWorkspace(category.description(), workspaceId))
                .distinct()
                .toList();
    }

    public List<Category> resolveAll(List<CategoryUpdateRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of(categoryAddService.addCategory(categoryAddService.getDefaultCategory()));
        }
        return records.stream()
                .map(record -> categoryAddService.addCategory(record.description()))
                .distinct()
                .toList();
    }

    private Category resolveAndEnsureInWorkspace(String description, Long workspaceId) {
        var category = categoryAddService.addCategory(description);
        workspaceCategoryService.ensureCategoryInWorkspace(workspaceId, category);
        return category;
    }
}

package api.m2.movements.services.category;

import api.m2.movements.entities.commons.Category;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryResolver {
    private final CategoryAddService categoryAddService;
    private final WorkspaceCategoryService workspaceCategoryService;

    public Category resolve(CategoryRecord record, Long workspaceId) {
        if (record == null) {
            return this.resolveAndEnsureInWorkspace(categoryAddService.getDefaultCategory(), workspaceId);
        }
        return this.resolveAndEnsureInWorkspace(record.description(), workspaceId);
    }

    public Category resolve(CategoryUpdateRecord record) {
        if (record == null) return categoryAddService.addCategory(categoryAddService.getDefaultCategory());

        return categoryAddService.addCategory(record.description());
    }

    public Category resolve(String description, Long workspaceId) {
        return this.resolveAndEnsureInWorkspace(description, workspaceId);
    }

    private Category resolveAndEnsureInWorkspace(String description, Long workspaceId) {
        var category = categoryAddService.addCategory(description);
        workspaceCategoryService.ensureCategoryInWorkspace(workspaceId, category);
        return category;
    }
}

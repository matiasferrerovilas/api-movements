package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.entities.Workspace;
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

    public Category resolve(CategoryRecord record, Workspace workspace) {
        if (record == null) {
            return this.resolveAndEnsureInWorkspace(categoryAddService.getDefaultCategory(), workspace);
        }
        return this.resolveAndEnsureInWorkspace(record.description(), workspace);
    }

    public Category resolve(CategoryUpdateRecord record) {
        if (record == null) return categoryAddService.addCategory(categoryAddService.getDefaultCategory());

        return categoryAddService.addCategory(record.description());
    }

    public Category resolve(String description, Workspace workspace) {
        return this.resolveAndEnsureInWorkspace(description, workspace);
    }

    private Category resolveAndEnsureInWorkspace(String description, Workspace workspace) {
        var category = categoryAddService.addCategory(description);
        workspaceCategoryService.ensureCategoryInWorkspace(workspace, category);
        return category;
    }
}
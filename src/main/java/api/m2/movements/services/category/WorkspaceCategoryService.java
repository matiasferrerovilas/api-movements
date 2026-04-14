package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.entities.Workspace;
import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.WorkspaceCategoryMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceCategoryService {

    private final WorkspaceCategoryRepository workspaceCategoryRepository;
    private final CategoryAddService categoryAddService;
    private final WorkspaceCategoryMapper workspaceCategoryMapper;
    private final WorkspaceContextService workspaceContextService;

    public List<CategoryRecord> getActiveCategories() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return workspaceCategoryMapper.toRecordList(
                workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId));
    }

    @Transactional
    public CategoryRecord addCategory(String description) {
        var workspace = workspaceContextService.getActiveWorkspace();
        var category = categoryAddService.addCategory(description);
        return workspaceCategoryMapper.toRecord(this.resolveWorkspaceCategory(workspace, category));
    }

    @Transactional
    public void addDefaultCategories(Workspace workspace) {
        var category = categoryAddService.addCategory(DefaultCategory.SERVICIOS.getDescription());
        this.resolveWorkspaceCategory(workspace, category);
    }

    @Transactional
    public void addCategories(Workspace workspace, List<String> descriptions) {
        descriptions.forEach(description -> {
            var category = categoryAddService.addCategory(description);
            this.resolveWorkspaceCategory(workspace, category);
        });
    }

    @Transactional
    public void deleteCategory(Long workspaceCategoryId) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var workspaceCategory = workspaceCategoryRepository.findById(workspaceCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        if (!workspaceCategory.getWorkspace().getId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tenés permiso para eliminar esta categoría");
        }
        if (!workspaceCategory.getCategory().isDeletable()) {
            throw new BusinessException("No se puede eliminar esta categoría");
        }

        workspaceCategoryRepository.delete(workspaceCategory);
    }

    /**
     * Asegura que una categoría esté asociada al workspace.
     * Si no existe la asociación, la crea automáticamente.
     * Usado por CategoryResolver para auto-agregar categorías.
     */
    @Transactional
    public void ensureCategoryInWorkspace(Workspace workspace, Category category) {
        this.resolveWorkspaceCategory(workspace, category);
    }

    private WorkspaceCategory resolveWorkspaceCategory(Workspace workspace, Category category) {
        return workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(workspace.getId(), category.getId())
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        return workspaceCategoryRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> workspaceCategoryRepository.save(
                        WorkspaceCategory.builder()
                                .workspace(workspace)
                                .category(category)
                                .build()));
    }
}

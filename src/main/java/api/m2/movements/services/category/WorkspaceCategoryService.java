package api.m2.movements.services.category;

import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.WorkspaceCategoryMapper;
import api.m2.movements.records.categories.CategoryPatchRequest;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.events.CategoryUpdatedEvent;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Categorías del workspace activo del request. La asociación de categorías a un workspace dado
 * por parámetro (onboarding, resolver de movimientos) vive en
 * {@link WorkspaceCategoryAssociationService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceCategoryService {

    private final WorkspaceCategoryRepository workspaceCategoryRepository;
    private final WorkspaceCategoryAssociationService associationService;
    private final WorkspaceCategoryMapper workspaceCategoryMapper;
    private final WorkspaceContextService workspaceContextService;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<CategoryRecord> getActiveCategories() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return workspaceCategoryMapper.toRecordList(
                workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId));
    }

    @Transactional
    public CategoryRecord addCategory(String description) {
        var workspaceId = workspaceContextService.getActiveWritableWorkspaceId();
        return workspaceCategoryMapper.toRecord(associationService.addCategory(workspaceId, description));
    }

    @Transactional
    public void deleteCategory(Long workspaceCategoryId) {
        var workspaceId = workspaceContextService.getActiveWritableWorkspaceId();
        var workspaceCategory = this.findInWorkspace(
                workspaceCategoryId, workspaceId, "No tenés permiso para eliminar esta categoría");

        if (!workspaceCategory.getCategory().isDeletable()) {
            throw new BusinessException("No se puede eliminar esta categoría");
        }

        workspaceCategoryRepository.delete(workspaceCategory);
    }

    @Transactional
    public CategoryRecord updateCategory(Long workspaceCategoryId, CategoryPatchRequest request) {
        var workspaceId = workspaceContextService.getActiveWritableWorkspaceId();
        var workspaceCategory = this.findInWorkspace(
                workspaceCategoryId, workspaceId, "No tenés acceso a esta categoría");

        if (request.description() != null && !request.description().isBlank()) {
            // La descripción modifica la Category global
            var category = workspaceCategory.getCategory();
            category.setDescription(request.description());
            categoryRepository.save(category);
        }
        if (request.iconName() != null) {
            workspaceCategory.setIconName(request.iconName());
        }
        if (request.iconColor() != null) {
            workspaceCategory.setIconColor(request.iconColor());
        }

        var savedWorkspaceCategory = workspaceCategoryRepository.save(workspaceCategory);
        var categoryRecord = workspaceCategoryMapper.toRecord(savedWorkspaceCategory);

        // Publicar evento para WebSocket (solo si el commit es exitoso)
        eventPublisher.publishEvent(new CategoryUpdatedEvent(categoryRecord, workspaceId));

        return categoryRecord;
    }

    private WorkspaceCategory findInWorkspace(Long workspaceCategoryId, Long workspaceId, String forbiddenMessage) {
        var workspaceCategory = workspaceCategoryRepository.findById(workspaceCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        if (!workspaceCategory.getWorkspaceId().equals(workspaceId)) {
            throw new PermissionDeniedException(forbiddenMessage);
        }
        return workspaceCategory;
    }
}

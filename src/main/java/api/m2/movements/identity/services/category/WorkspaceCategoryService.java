package api.m2.movements.identity.services.category;

import api.m2.movements.movements.entities.commons.Category;
import api.m2.movements.identity.entities.WorkspaceCategory;
import api.m2.movements.movements.enums.DefaultCategory;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.identity.mappers.WorkspaceCategoryMapper;
import api.m2.movements.movements.records.categories.CategoryPatchRequest;
import api.m2.movements.movements.records.categories.CategoryRecord;
import api.m2.movements.movements.records.categories.CategoryUpdatedEvent;
import api.m2.movements.movements.repositories.CategoryRepository;
import api.m2.movements.movements.services.category.CategoryAddService;
import api.m2.movements.identity.repositories.WorkspaceCategoryRepository;
import api.m2.movements.identity.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<CategoryRecord> getActiveCategories() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return workspaceCategoryMapper.toRecordList(
                workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId));
    }

    @Transactional
    public CategoryRecord addCategory(String description) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var category = categoryAddService.addCategory(description);
        return workspaceCategoryMapper.toRecord(this.resolveWorkspaceCategory(workspaceId, category));
    }

    @Transactional
    public void addDefaultCategories(Long workspaceId) {
        var category = categoryAddService.addCategory(DefaultCategory.SERVICIOS.getDescription());
        this.resolveWorkspaceCategory(workspaceId, category);
    }

    @Transactional
    public void addCategories(Long workspaceId, List<String> descriptions) {
        descriptions.forEach(description -> {
            var category = categoryAddService.addCategory(description);
            this.resolveWorkspaceCategory(workspaceId, category);
        });
    }

    @Transactional
    public void deleteCategory(Long workspaceCategoryId) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var workspaceCategory = workspaceCategoryRepository.findById(workspaceCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        if (!workspaceCategory.getWorkspaceId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tenés permiso para eliminar esta categoría");
        }
        if (!workspaceCategory.getCategory().isDeletable()) {
            throw new BusinessException("No se puede eliminar esta categoría");
        }

        workspaceCategoryRepository.delete(workspaceCategory);
    }

    @Transactional
    public CategoryRecord updateCategory(Long workspaceCategoryId, CategoryPatchRequest request) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();

        // Buscar la WorkspaceCategory por su ID directo
        var workspaceCategory = workspaceCategoryRepository
                .findById(workspaceCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        // Verificar que pertenece al workspace activo
        if (!workspaceCategory.getWorkspaceId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tenés acceso a esta categoría");
        }

        // Actualizar solo los campos proporcionados (null-safe)
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

    /**
     * Asegura que una categoría esté asociada al workspace.
     * Si no existe la asociación, la crea automáticamente.
     * Usado por CategoryResolver para auto-agregar categorías.
     */
    @Transactional
    public void ensureCategoryInWorkspace(Long workspaceId, Category category) {
        this.resolveWorkspaceCategory(workspaceId, category);
    }

    private WorkspaceCategory resolveWorkspaceCategory(Long workspaceId, Category category) {
        return workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(workspaceId, category.getId())
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        return workspaceCategoryRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> workspaceCategoryRepository.save(
                        WorkspaceCategory.builder()
                                .workspaceId(workspaceId)
                                .category(category)
                                .build()));
    }
}

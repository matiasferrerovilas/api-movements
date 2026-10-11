package api.m2.movements.services.category;

import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.entities.commons.Category;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Asocia categorías a un workspace dado por parámetro, sin leer el workspace activo del request.
 * Lo usan el onboarding (el usuario todavía no tiene DEFAULT_WORKSPACE), el resolver de categorías
 * de los movimientos y {@link WorkspaceCategoryService} para el alta desde el request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceCategoryAssociationService {

    private final WorkspaceCategoryRepository workspaceCategoryRepository;
    private final CategoryAddService categoryAddService;

    @Transactional
    public WorkspaceCategory addCategory(Long workspaceId, String description) {
        var category = categoryAddService.addCategory(description);
        return this.ensureCategoryInWorkspace(workspaceId, category);
    }

    @Transactional
    public void addDefaultCategories(Long workspaceId) {
        // SIN_CATEGORIA queda afuera a propósito: se autocrea en el workspace recién cuando
        // aparece el primer movimiento sin categoría (CategoryResolver), no antes.
        Arrays.stream(DefaultCategory.values())
                .filter(defaultCategory -> defaultCategory != DefaultCategory.SIN_CATEGORIA)
                .forEach(defaultCategory -> this.addCategory(workspaceId, defaultCategory.getDescription()));
    }

    @Transactional
    public void addCategories(Long workspaceId, List<String> descriptions) {
        descriptions.forEach(description -> this.addCategory(workspaceId, description));
    }

    /**
     * Asegura que una categoría esté asociada al workspace: reactiva la asociación si estaba
     * inactiva y la crea si no existe. Usado por CategoryResolver para auto-agregar categorías.
     */
    @Transactional
    public WorkspaceCategory ensureCategoryInWorkspace(Long workspaceId, Category category) {
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

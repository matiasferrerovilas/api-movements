package api.m2.movements.services.category;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.categories.CategoryMigrateRequest;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryMigrateService {

    private final MovementRepository movementRepository;
    private final CategoryRepository categoryRepository;
    private final WorkspaceContextService workspaceContextService;

    @Transactional
    public void migrateCategory(CategoryMigrateRequest request) {
        if (request.fromCategoryId().equals(request.toCategoryId())) {
            throw new BusinessException("fromCategoryId y toCategoryId no pueden ser iguales");
        }

        var workspaceId = workspaceContextService.getActiveWorkspaceId();

        var toCategory = categoryRepository.findById(request.toCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoría destino no encontrada con id: " + request.toCategoryId()));

        var movements = movementRepository.findByWorkspaceIdAndCategoryId(
                workspaceId, request.fromCategoryId());

        movements.forEach(movement -> movement.setCategory(toCategory));
        movementRepository.saveAll(movements);
    }
}

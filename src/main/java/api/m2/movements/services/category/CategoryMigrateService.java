package api.m2.movements.services.category;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.categories.CategoryMigrateRequest;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryMigrateService {

    private final MovementRepository movementRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Transactional
    public void migrateCategory(CategoryMigrateRequest request) {
        if (request.fromCategoryId().equals(request.toCategoryId())) {
            throw new BusinessException("fromCategoryId y toCategoryId no pueden ser iguales");
        }

        var toCategory = categoryRepository.findById(request.toCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoría destino no encontrada con id: " + request.toCategoryId()));

        var user = userService.getAuthenticatedUser();
        var movements = movementRepository.findByOwnerIdAndCategoryId(user.getId(), request.fromCategoryId());

        movements.forEach(movement -> movement.setCategory(toCategory));
        movementRepository.saveAll(movements);
    }
}

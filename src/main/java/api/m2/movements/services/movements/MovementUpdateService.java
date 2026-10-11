package api.m2.movements.services.movements;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.commons.Category;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.events.CategoryCorrectedEvent;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.repositories.MovementRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementUpdateService {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementFactory movementFactory;
    private final MovementItemService movementItemService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT, idParamIndex = 1)
    public void updateMovement(@Valid ExpenseToUpdate dto, Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));

        var previousCategoryIds = this.categoryIds(movement);
        movementMapper.updateMovement(dto, movement);
        movementFactory.applyUpdates(dto, movement);
        movementRepository.save(movement);
        this.publishCategoryCorrection(dto, movement, previousCategoryIds);
        if (dto.items() != null) {
            movementItemService.replaceItems(id, dto.items());
        }

        log.info("Movimiento actualizado: id={}", id);
    }

    // El front manda las categorías en cada edición aunque no cambien: solo es corrección si el
    // conjunto cambió y quedó una única categoría real (con varias no hay a cuál asociar el comercio).
    private void publishCategoryCorrection(ExpenseToUpdate dto, Movement movement, Set<Long> previousCategoryIds) {
        if (dto.categories() == null || previousCategoryIds.equals(this.categoryIds(movement))) {
            return;
        }
        var real = movement.getCategories().stream()
                .filter(category -> !DefaultCategory.SIN_CATEGORIA.getDescription().equals(category.getDescription()))
                .toList();
        if (real.size() == 1) {
            eventPublisher.publishEvent(new CategoryCorrectedEvent(
                    movement.getWorkspaceId(), movement.getDescription(), real.getFirst().getId()));
        }
    }

    private Set<Long> categoryIds(Movement movement) {
        return movement.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
    }
}

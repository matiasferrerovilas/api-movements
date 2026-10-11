package api.m2.movements.services.movements;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.movements.MovementDeletedEvent;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementDeleteService {

    private final MovementRepository movementRepository;
    private final MovementItemService movementItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT)
    public void deleteMovement(Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento con Id" + id + " no existe"));

        Long workspaceId = movement.getWorkspaceId();
        movementItemService.replaceItems(id, null);
        movementRepository.deleteById(id);
        eventPublisher.publishEvent(new MovementDeletedEvent(id, workspaceId));

        log.info("Movimiento eliminado correctamente: id={}", id);
    }

    /**
     * Borra todos los movimientos cuyo owner es el usuario autenticado, en cualquier workspace.
     * Los movimientos que otros miembros cargaron en un workspace compartido no se tocan.
     */
    @Transactional
    public int deleteAllMovementsOfCurrentUser() {
        var ownerId = userService.getMe().id();
        var movements = movementRepository.findAllByOwnerId(ownerId);
        if (movements.isEmpty()) {
            return 0;
        }

        movementItemService.deleteItemsOf(movements.stream().map(Movement::getId).toList());
        movementRepository.deleteAll(movements);
        movements.forEach(movement ->
                eventPublisher.publishEvent(new MovementDeletedEvent(movement.getId(), movement.getWorkspaceId())));

        log.info("Movimientos eliminados del usuario: ownerId={}, total={}", ownerId, movements.size());
        return movements.size();
    }
}

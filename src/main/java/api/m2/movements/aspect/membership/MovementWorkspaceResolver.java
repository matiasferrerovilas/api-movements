package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolver para obtener el workspaceId de un Movement.
 */
@Component
@RequiredArgsConstructor
public class MovementWorkspaceResolver implements WorkspaceIdResolver {

    private final MovementRepository movementRepository;

    @Override
    public boolean supports(MembershipDomain domain) {
        return domain == MembershipDomain.MOVEMENT;
    }

    @Override
    public Long resolveWorkspaceId(Long entityId) {
        return movementRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Movimiento no encontrado: " + entityId))
                .getWorkspace().getId();
    }
}

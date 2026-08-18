package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolver para obtener el workspaceId de una Goal.
 */
@Component
@RequiredArgsConstructor
public class GoalWorkspaceResolver implements WorkspaceIdResolver {

    private final GoalRepository goalRepository;

    @Override
    public boolean supports(MembershipDomain domain) {
        return domain == MembershipDomain.GOAL;
    }

    @Override
    public Long resolveWorkspaceId(Long entityId) {
        return goalRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Meta de ahorro no encontrada: " + entityId))
                .getWorkspaceId();
    }
}

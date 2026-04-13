package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolver para obtener el workspaceId de un Subscription.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionWorkspaceResolver implements WorkspaceIdResolver {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public boolean supports(MembershipDomain domain) {
        return domain == MembershipDomain.SUBSCRIPTION;
    }

    @Override
    public Long resolveWorkspaceId(Long entityId) {
        return subscriptionRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Servicio no encontrado: " + entityId))
                .getWorkspace().getId();
    }
}

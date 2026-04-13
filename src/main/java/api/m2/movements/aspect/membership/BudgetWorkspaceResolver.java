package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolver para obtener el workspaceId de un Budget.
 */
@Component
@RequiredArgsConstructor
public class BudgetWorkspaceResolver implements WorkspaceIdResolver {

    private final BudgetRepository budgetRepository;

    @Override
    public boolean supports(MembershipDomain domain) {
        return domain == MembershipDomain.BUDGET;
    }

    @Override
    public Long resolveWorkspaceId(Long entityId) {
        return budgetRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Presupuesto no encontrado: " + entityId))
                .getWorkspace().getId();
    }
}

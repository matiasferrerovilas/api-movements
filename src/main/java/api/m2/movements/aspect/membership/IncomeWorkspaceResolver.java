package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolver para obtener el workspaceId de un Income.
 */
@Component
@RequiredArgsConstructor
public class IncomeWorkspaceResolver implements WorkspaceIdResolver {

    private final IncomeRepository incomeRepository;

    @Override
    public boolean supports(MembershipDomain domain) {
        return domain == MembershipDomain.INCOME;
    }

    @Override
    public Long resolveWorkspaceId(Long entityId) {
        return incomeRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ingreso no encontrado: " + entityId))
                .getWorkspace().getId();
    }
}

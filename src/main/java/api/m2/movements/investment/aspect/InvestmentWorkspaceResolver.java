package api.m2.movements.investment.aspect;

import api.m2.movements.aspect.membership.WorkspaceIdResolver;
import api.m2.movements.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.investment.repositories.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvestmentWorkspaceResolver implements WorkspaceIdResolver {

    private final InvestmentRepository investmentRepository;

    @Override
    public boolean supports(MembershipDomain domain) {
        return domain == MembershipDomain.INVESTMENT;
    }

    @Override
    public Long resolveWorkspaceId(Long entityId) {
        return investmentRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inversión no encontrada: " + entityId))
                .getWorkspace().getId();
    }
}

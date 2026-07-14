package api.m2.movements.movements.services.budgets;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.movements.mappers.BudgetMapper;
import api.m2.movements.movements.records.BudgetToAdd;
import api.m2.movements.movements.records.budgets.BudgetToUpdate;
import api.m2.movements.movements.repositories.BudgetRepository;
import api.m2.movements.movements.repositories.CategoryRepository;
import api.m2.movements.movements.repositories.CurrencyRepository;
import api.m2.movements.identity.services.workspaces.WorkspaceContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetAddService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final CategoryRepository categoryRepository;
    private final CurrencyRepository currencyRepository;
    private final WorkspaceContextService workspaceContextService;

    @Transactional
    public void save(@Valid BudgetToAdd dto) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var budget = budgetMapper.toEntity(dto, categoryRepository, currencyRepository);
        budget.setWorkspaceId(workspaceId);
        budgetRepository.save(budget);
        log.debug("Presupuesto creado: workspaceId={}, category={}, currency={}",
                workspaceId, dto.category(), dto.currency());
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.BUDGET)
    public void update(@Valid BudgetToUpdate dto, Long id) {
        var budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));
        budget.setAmount(dto.amount());
        budgetRepository.save(budget);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.BUDGET)
    public void delete(Long id) {
        if (!budgetRepository.existsById(id)) {
            throw new EntityNotFoundException("Presupuesto no encontrado: " + id);
        }
        budgetRepository.deleteById(id);
    }
}

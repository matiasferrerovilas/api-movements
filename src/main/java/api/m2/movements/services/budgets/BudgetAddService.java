package api.m2.movements.services.budgets;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.BudgetMapper;
import api.m2.movements.records.BudgetToAdd;
import api.m2.movements.records.budgets.BudgetToUpdate;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.services.groups.AccountQueryService;
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
    private final AccountQueryService accountQueryService;

    @Transactional
    public void save(@Valid BudgetToAdd dto) {
        var account = accountQueryService.findAccountById(dto.accountId());
        var budget = budgetMapper.toEntity(dto, categoryRepository, currencyRepository);
        budget.setAccount(account);
        budgetRepository.save(budget);
        log.debug("Presupuesto creado: accountId={}, category={}, currency={}",
                dto.accountId(), dto.category(), dto.currency());
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

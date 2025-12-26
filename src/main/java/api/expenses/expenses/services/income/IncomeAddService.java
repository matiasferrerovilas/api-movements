package api.expenses.expenses.services.income;

import api.expenses.expenses.mappers.IncomeMapper;
import api.expenses.expenses.records.income.IncomeRecord;
import api.expenses.expenses.records.income.IncomeToAdd;
import api.expenses.expenses.repositories.IncomeRepository;
import api.expenses.expenses.services.accounts.AccountQueryService;
import api.expenses.expenses.services.currencies.CurrencyAddService;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeAddService {
    private final IncomeRepository incomeRepository;
    private final UserService userService;
    private final IncomeMapper incomeMapper;
    private final AccountQueryService accountQueryService;
    private final CurrencyAddService currencyAddService;

    @Transactional
    public void loadIncome(IncomeToAdd incomeToAdd) {
        var income = incomeMapper.toEntity(incomeToAdd);
        var user = userService.getAuthenticatedUser();
        income.setUser(user);
        var account = accountQueryService.findAccountByName(incomeToAdd.group());
        income.setAccount(account);
        var currency = currencyAddService.findBySymbol(incomeToAdd.currency());
        income.setCurrency(currency);

        incomeRepository.save(income);
    }

    public List<IncomeRecord> getAllIncomes() {
        var user = userService.getAuthenticatedUser();
        return incomeMapper.toRecord(incomeRepository.findAllByUserOrGroupsIn(user.getId()));
    }

    public void deleteIncome(Long id) {
        log.debug("Eliminando Income {}", id);
        var incomeToDelete = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        incomeRepository.delete(incomeToDelete);
    }
}
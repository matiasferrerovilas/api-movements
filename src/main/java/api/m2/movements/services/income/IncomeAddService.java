package api.m2.movements.services.income;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.enums.CategoryEnum;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.enums.MovementType;
import api.m2.movements.mappers.IncomeMapper;
import api.m2.movements.records.income.IncomeRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.services.groups.AccountQueryService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.user.UserService;
import api.m2.movements.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private final MovementAddService movementAddService;
    private final BankRepository bankRepository;

    @Transactional
    public void loadIncome(IncomeToAdd incomeToAdd) {
        var income = incomeMapper.toEntity(incomeToAdd);
        var user = userService.getAuthenticatedUser();
        income.setUser(user);
        var account = accountQueryService.findAccountByName(incomeToAdd.group());
        income.setAccount(account);
        var currency = currencyAddService.findBySymbol(incomeToAdd.currency());
        income.setCurrency(currency);
        var bank = bankRepository.findByDescription(incomeToAdd.bank().trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Banco no encontrado: " + incomeToAdd.bank()));
        income.setBank(bank);

        incomeRepository.save(income);
    }

    public List<IncomeRecord> getAllIncomes() {
        var user = userService.getAuthenticatedUser();
        return incomeMapper.toRecord(incomeRepository.findAllByUserOrGroupsIn(user.getId()));
    }

    @RequiresMembership(domain = MembershipDomain.INCOME)
    public void deleteIncome(Long id) {
        log.debug("Eliminando Income {}", id);
        var incomeToDelete = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        incomeRepository.delete(incomeToDelete);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.INCOME)
    public void reloadIncome(Long id) {
        log.debug("Reloading Income {}", id);
        var incomeToReload = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingreso no encontrada"));

        var movementToAdd = new MovementToAdd(
                incomeToReload.getAmount(),
                LocalDate.now(ZoneOffset.UTC),
                "Ingreso",
                CategoryEnum.HOGAR.name(),
                MovementType.INGRESO.name(),
                incomeToReload.getCurrency().getSymbol(),
                null,
                null,
                incomeToReload.getBank().getDescription(),
                incomeToReload.getAccount().getId()
        );
        movementAddService.saveMovement(movementToAdd);

        log.debug("Income {} recargado", id);
    }
}
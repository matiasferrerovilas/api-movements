package api.m2.movements.movements.services.income;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.identity.entities.User;
import api.m2.movements.identity.entities.Workspace;
import api.m2.movements.movements.enums.DefaultCategory;
import api.m2.movements.movements.enums.MembershipDomain;
import api.m2.movements.movements.enums.MovementType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.movements.mappers.IncomeMapper;
import api.m2.movements.movements.records.income.IncomeToAdd;
import api.m2.movements.movements.records.movements.MovementToAdd;
import api.m2.movements.movements.repositories.BankRepository;
import api.m2.movements.movements.repositories.IncomeRepository;
import api.m2.movements.movements.services.currencies.CurrencyAddService;
import api.m2.movements.movements.services.movements.MovementAddService;
import api.m2.movements.identity.services.user.UserService;
import api.m2.movements.identity.services.workspaces.WorkspaceContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeAddService {
    private final IncomeRepository incomeRepository;
    private final UserService userService;
    private final IncomeMapper incomeMapper;
    private final WorkspaceContextService workspaceContextService;
    private final CurrencyAddService currencyAddService;
    private final MovementAddService movementAddService;
    private final BankRepository bankRepository;

    @Transactional
    public void loadIncome(IncomeToAdd incomeToAdd) {
        var workspace = workspaceContextService.getActiveWorkspace();
        this.loadIncome(incomeToAdd, workspace);
    }

    /**
     * Carga un ingreso asociándolo a un workspace específico.
     * Usado por el onboarding donde el usuario aún no tiene DEFAULT_WORKSPACE configurado.
     */
    @Transactional
    public void loadIncome(IncomeToAdd incomeToAdd, Workspace workspace) {
        var income = incomeMapper.toEntity(incomeToAdd);
        var user = userService.getAuthenticatedUser();
        income.setUser(user);
        income.setWorkspace(workspace);
        var currency = currencyAddService.findBySymbol(incomeToAdd.currency().symbol());
        income.setCurrency(currency);
        var bank = bankRepository.findByDescription(incomeToAdd.bank().trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Banco no encontrado: " + incomeToAdd.bank()));
        income.setBank(bank);

        incomeRepository.save(income);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.INCOME)
    public void deleteIncome(Long id) {
        log.debug("Eliminando Income {}", id);
        var incomeToDelete = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        incomeRepository.delete(incomeToDelete);
    }

    public void addIngreso(@Valid IncomeToAdd incomeToAdd) {
        var currency = currencyAddService.findBySymbol(incomeToAdd.currency().symbol());
        movementAddService.saveMovement(
                this.buildIncomeMovement(incomeToAdd.amount(), currency.getSymbol(), incomeToAdd.bank(), "Sueldo Recibido"));
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.INCOME)
    public void reloadIncome(Long id) {
        log.debug("Reloading Income {}", id);
        var income = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingreso no encontrada"));

        movementAddService.saveMovement(
                this.buildIncomeMovement(income.getAmount(), income.getCurrency().getSymbol(),
                        income.getBank().getDescription(), "Ingreso"));

        log.debug("Income {} recargado", id);
    }

    @Transactional
    public int generateRecurringIncomeForUser(User user) {
        var incomes = incomeRepository.findAllByUserId(user.getId());
        log.info("Generando {} movimientos de ingreso para usuario {}", incomes.size(), user.getEmail());

        for (var income : incomes) {
            movementAddService.saveMovement(
                    this.buildIncomeMovement(income.getAmount(), income.getCurrency().getSymbol(),
                            income.getBank().getDescription(), "Ingreso recurrente"),
                    income.getWorkspace(),
                    income.getUser());
        }

        return incomes.size();
    }

    private MovementToAdd buildIncomeMovement(BigDecimal amount, String currencySymbol,
                                              String bankDescription, String description) {
        return new MovementToAdd(
                amount,
                LocalDate.now(ZoneOffset.UTC),
                description,
                DefaultCategory.HOGAR.getDescription(),
                MovementType.INGRESO.name(),
                currencySymbol,
                null,
                null,
                bankDescription);
    }
}
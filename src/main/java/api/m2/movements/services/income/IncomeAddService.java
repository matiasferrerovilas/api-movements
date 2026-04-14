package api.m2.movements.services.income;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.User;
import api.m2.movements.entities.Workspace;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.enums.MovementType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.IncomeMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CategoryAddService categoryAddService;

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
        CategoryRecord category = categoryAddService
                .findCategoryByDescription(DefaultCategory.HOGAR.getDescription());
        var currency = currencyAddService.findBySymbol(incomeToAdd.currency().symbol());

        movementAddService.saveMovement(new MovementToAdd(incomeToAdd.amount(),
                LocalDate.now(ZoneOffset.UTC),
                "Sueldo Recibido",
                category.description(),
                MovementType.INGRESO.name(),
                currency.getSymbol(),
                0,
                0,
                incomeToAdd.bank()));
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
                DefaultCategory.HOGAR.getDescription(),
                MovementType.INGRESO.name(),
                incomeToReload.getCurrency().getSymbol(),
                null,
                null,
                incomeToReload.getBank().getDescription()
        );
        movementAddService.saveMovement(movementToAdd);

        log.debug("Income {} recargado", id);
    }

    /**
     * Genera movimientos de ingreso para todos los Income configurados de un usuario.
     * Este método es llamado por el cron de ingresos recurrentes.
     *
     * @param user el usuario para el cual generar los movimientos
     * @return cantidad de movimientos generados
     */
    @Transactional
    public int generateRecurringIncomeForUser(User user) {
        var incomes = incomeRepository.findAllByUserId(user.getId());
        log.info("Generando {} movimientos de ingreso para usuario {}", incomes.size(), user.getEmail());

        for (var income : incomes) {
            var movementToAdd = new MovementToAdd(
                    income.getAmount(),
                    LocalDate.now(ZoneOffset.UTC),
                    "Ingreso recurrente",
                    DefaultCategory.HOGAR.getDescription(),
                    MovementType.INGRESO.name(),
                    income.getCurrency().getSymbol(),
                    null,
                    null,
                    income.getBank().getDescription()
            );
            movementAddService.saveMovement(movementToAdd);
        }

        return incomes.size();
    }
}
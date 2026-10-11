package api.m2.movements.services.income;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.IncomeMapper;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.services.currencies.CurrencyResolver;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta y baja de la plantilla de ingreso ({@code Income}). Generar movimientos a partir de ella
 * es responsabilidad de {@link IncomeMovementService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeAddService {
    private final IncomeRepository incomeRepository;
    private final UserService userService;
    private final IncomeMapper incomeMapper;
    private final WorkspaceContextService workspaceContextService;
    private final CurrencyResolver currencyResolver;
    private final BankRepository bankRepository;

    @Transactional
    public void loadIncome(IncomeToAdd incomeToAdd) {
        var workspaceId = workspaceContextService.getActiveWritableWorkspaceId();
        this.loadIncome(incomeToAdd, workspaceId);
    }

    /**
     * Carga un ingreso asociándolo a un workspace específico.
     * Usado por el onboarding donde el usuario aún no tiene DEFAULT_WORKSPACE configurado.
     */
    @Transactional
    public void loadIncome(IncomeToAdd incomeToAdd, Long workspaceId) {
        var income = incomeMapper.toEntity(incomeToAdd);
        var userId = userService.getMe().id();
        income.setUserId(userId);
        income.setWorkspaceId(workspaceId);
        var currency = currencyResolver.resolve(incomeToAdd.currency().symbol(), workspaceId);
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
}

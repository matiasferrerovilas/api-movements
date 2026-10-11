package api.m2.movements.services.income;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.enums.MovementType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.helpers.GeneratedLedger;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.SystemMovementAddService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Convierte ingresos en movimientos: a pedido del usuario ({@link MovementAddService}) o desde el
 * job de ingresos recurrentes, que no tiene JWT ({@link SystemMovementAddService}). La plantilla
 * de ingreso en sí (la entidad {@code Income}) la gestiona {@link IncomeAddService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeMovementService {
    private static final String RECURRING_INCOME_DESCRIPTION = "Ingreso recurrente";

    private final IncomeRepository incomeRepository;
    private final MovementRepository movementRepository;
    private final CurrencyAddService currencyAddService;
    private final MovementAddService movementAddService;
    private final SystemMovementAddService systemMovementAddService;

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

    /**
     * Genera el movimiento del mes para cada ingreso recurrente del usuario. Es idempotente:
     * descuenta los ingresos recurrentes que ya existen este mes (de una corrida anterior), así
     * que volver a correrlo no duplica. Devuelve cuántos generó en esta corrida.
     */
    @Transactional
    public int generateRecurringIncomeForUser(Long userId) {
        var incomes = incomeRepository.findAllByUserId(userId);
        var alreadyGenerated = this.alreadyGenerated(userId, YearMonth.now(ZoneOffset.UTC));

        int generated = 0;
        for (var income : incomes) {
            var key = new RecurringIncomeKey(income.getWorkspaceId(), income.getAmount().stripTrailingZeros(),
                    income.getCurrency().getSymbol(), income.getBank().getDescription());
            if (alreadyGenerated.consume(key)) {
                continue;
            }
            systemMovementAddService.saveMovement(
                    this.buildIncomeMovement(income.getAmount(), income.getCurrency().getSymbol(),
                            income.getBank().getDescription(), RECURRING_INCOME_DESCRIPTION),
                    income.getWorkspaceId(),
                    income.getUserId());
            generated++;
        }

        log.info("Ingresos recurrentes del usuario {}: {} generados, {} ya existían",
                userId, generated, incomes.size() - generated);
        return generated;
    }

    private GeneratedLedger<RecurringIncomeKey> alreadyGenerated(Long userId, YearMonth month) {
        var existing = movementRepository.findIncomesByOwnerAndDescriptionBetween(
                userId, RECURRING_INCOME_DESCRIPTION, month.atDay(1), month.plusMonths(1).atDay(1));
        return new GeneratedLedger<>(existing.stream()
                .map(movement -> new RecurringIncomeKey(movement.getWorkspaceId(),
                        movement.getAmount().stripTrailingZeros(), movement.getCurrency().getSymbol(),
                        movement.getBank() != null ? movement.getBank().getDescription() : null))
                .toList());
    }

    private record RecurringIncomeKey(Long workspaceId, BigDecimal amount, String currency, String bank) {
    }

    private MovementToAdd buildIncomeMovement(BigDecimal amount, String currencySymbol,
                                              String bankDescription, String description) {
        return new MovementToAdd(
                amount,
                LocalDate.now(ZoneOffset.UTC),
                description,
                List.of(new CategoryUpdateRecord(null, DefaultCategory.HOGAR.getDescription())),
                MovementType.INGRESO.name(),
                currencySymbol,
                null,
                null,
                bankDescription,
                null,
                null);
    }
}

package api.m2.movements.demo;

import api.m2.movements.entities.Budget;
import api.m2.movements.entities.Goal;
import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.entities.WorkspaceCurrency;
import api.m2.movements.entities.commons.Bank;
import api.m2.movements.entities.commons.Category;
import api.m2.movements.entities.commons.Currency;
import api.m2.movements.entities.movements.Income;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.entities.movements.Subscription;
import api.m2.movements.enums.MovementType;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.GoalRepository;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import api.m2.movements.repositories.WorkspaceCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;

/**
 * Seeds a few months of realistic demo data (movements, budgets, a subscription, recurring
 * income, and savings goals) when the {@code demo} Spring profile is active.
 *
 * <p>This is a suite-wide convention: the fixed id {@value #DEMO_WORKSPACE_ID} identifies the
 * shared demo workspace across the M2 suite. api-identity's own {@code demo} profile independently
 * creates that workspace record; this seeder only inserts domain rows referencing it and does not
 * create the workspace itself. api-movements' own tables have no foreign key to a local
 * "workspaces" table (workspace management is fully delegated to api-identity), so this can run
 * safely even if api-identity's demo seeder hasn't run yet.
 *
 * <p>Guarded by {@link Profile @Profile("demo")}: this bean is never registered outside the
 * {@code demo} profile, so it cannot run in {@code dev}, {@code prod}, or the default profile.
 *
 * <p>Idempotent: re-running the app in {@code demo} profile (e.g. a restart) does not duplicate
 * the seed data — it bails out early if any movement already exists for the demo workspace.
 */
@Component
@Profile("demo")
@Slf4j
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    /**
     * Fixed id for the shared demo workspace, relied upon across the M2 suite's demo profiles.
     */
    static final long DEMO_WORKSPACE_ID = 1L;

    private static final long DEMO_USER_ID = 1L;
    private static final String DEMO_CURRENCY_SYMBOL = "ARS";
    private static final String DEMO_BANK = "GALICIA";

    private static final List<String> EXPENSE_CATEGORIES =
            List.of("Supermercado", "Transporte", "Servicios", "Streaming", "Restaurante", "Hogar");
    private static final int SEED_MONTHS = 3;
    private static final long AMOUNT_VARIATION_STEP = 137L;
    private static final int FIRST_MOVEMENT_DAY = 3;
    private static final int DAY_STEP_PER_CATEGORY = 4;
    private static final int GOAL_TARGET_MONTHS_AHEAD = 6;

    private final MovementRepository movementRepository;
    private final IncomeRepository incomeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final CurrencyRepository currencyRepository;
    private final BankRepository bankRepository;
    private final WorkspaceCategoryRepository workspaceCategoryRepository;
    private final WorkspaceCurrencyRepository workspaceCurrencyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (movementRepository.existsByWorkspaceId(DEMO_WORKSPACE_ID)) {
            log.info("Demo profile: demo data already present for workspace id={}, skipping seed",
                    DEMO_WORKSPACE_ID);
            return;
        }

        Currency currency = findOrCreateCurrency();
        Bank bank = findOrCreateBank();
        List<Category> categories = EXPENSE_CATEGORIES.stream().map(this::findOrCreateCategory).toList();

        ensureWorkspaceCurrency(currency);
        categories.forEach(this::ensureWorkspaceCategory);

        int movementsCreated = seedMovements(currency, bank, categories);
        int incomesCreated = seedRecurringIncome(currency, bank);
        seedBudgets(currency, categories);
        seedSubscription(currency);
        seedGoals(currency);

        log.info("Demo profile seed ready — workspace id={}, movements={}, incomes={}",
                DEMO_WORKSPACE_ID, movementsCreated, incomesCreated);
    }

    // ── Reference data (find-or-create) ────────────────────────────────────────

    private Currency findOrCreateCurrency() {
        return currencyRepository.findBySymbol(DEMO_CURRENCY_SYMBOL)
                .orElseGet(() -> currencyRepository.save(Currency.builder()
                        .symbol(DEMO_CURRENCY_SYMBOL)
                        .description("Peso Argentino")
                        .enabled(true)
                        .build()));
    }

    private Bank findOrCreateBank() {
        return bankRepository.findByDescription(DEMO_BANK)
                .orElseGet(() -> bankRepository.save(Bank.builder().description(DEMO_BANK).build()));
    }

    private Category findOrCreateCategory(String description) {
        return categoryRepository.findByDescription(description)
                .orElseGet(() -> categoryRepository.save(Category.builder().description(description).build()));
    }

    private void ensureWorkspaceCurrency(Currency currency) {
        workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(DEMO_WORKSPACE_ID, currency.getId())
                .orElseGet(() -> workspaceCurrencyRepository.save(WorkspaceCurrency.builder()
                        .workspaceId(DEMO_WORKSPACE_ID)
                        .currency(currency)
                        .build()));
    }

    private void ensureWorkspaceCategory(Category category) {
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(DEMO_WORKSPACE_ID, category.getId())
                .orElseGet(() -> workspaceCategoryRepository.save(WorkspaceCategory.builder()
                        .workspaceId(DEMO_WORKSPACE_ID)
                        .category(category)
                        .isActive(true)
                        .build()));
    }

    // ── Domain data ─────────────────────────────────────────────────────────────

    /**
     * Seeds ~8 categorized expense movements per month for the trailing 3 months (including the
     * current one), cycling through the demo categories with realistic-looking amounts.
     */
    private int seedMovements(Currency currency, Bank bank, List<Category> categories) {
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        BigDecimal[] baseAmounts = {
                new BigDecimal("45000.00"), new BigDecimal("18000.00"), new BigDecimal("12000.00"),
                new BigDecimal("4500.00"), new BigDecimal("22000.00"), new BigDecimal("30000.00"),
        };

        int created = 0;
        for (int monthsAgo = SEED_MONTHS - 1; monthsAgo >= 0; monthsAgo--) {
            YearMonth yearMonth = currentMonth.minusMonths(monthsAgo);
            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);
                // Ligera variación mes a mes para que no se vea como datos repetidos calcados.
                BigDecimal amount = baseAmounts[i % baseAmounts.length]
                        .add(BigDecimal.valueOf((i + monthsAgo) * AMOUNT_VARIATION_STEP));
                int day = Math.min(FIRST_MOVEMENT_DAY + i * DAY_STEP_PER_CATEGORY, yearMonth.lengthOfMonth());

                Movement movement = Movement.builder()
                        .amount(amount)
                        .description(category.getDescription() + " - compra habitual")
                        .categories(new HashSet<>(List.of(category)))
                        .currency(currency)
                        .ownerId(DEMO_USER_ID)
                        .workspaceId(DEMO_WORKSPACE_ID)
                        .bank(bank)
                        .type(MovementType.DEBITO)
                        .date(yearMonth.atDay(day))
                        .build();
                movementRepository.save(movement);
                created++;
            }
        }
        return created;
    }

    /** Seeds a monthly "salary" income entry for the trailing 3 months (including the current one). */
    private int seedRecurringIncome(Currency currency, Bank bank) {
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        int created = 0;
        for (int monthsAgo = SEED_MONTHS - 1; monthsAgo >= 0; monthsAgo--) {
            Income income = Income.builder()
                    .amount(new BigDecimal("650000.00"))
                    .currency(currency)
                    .userId(DEMO_USER_ID)
                    .bank(bank)
                    .workspaceId(DEMO_WORKSPACE_ID)
                    .build();
            incomeRepository.save(income);
            created++;
        }
        return created;
    }

    private void seedBudgets(Currency currency, List<Category> categories) {
        Category supermercado = categories.get(0);
        Category transporte = categories.get(1);

        budgetRepository.save(Budget.builder()
                .workspaceId(DEMO_WORKSPACE_ID)
                .category(supermercado)
                .currency(currency)
                .amount(new BigDecimal("150000.00"))
                .build());

        budgetRepository.save(Budget.builder()
                .workspaceId(DEMO_WORKSPACE_ID)
                .category(transporte)
                .currency(currency)
                .amount(new BigDecimal("40000.00"))
                .build());
    }

    private void seedSubscription(Currency currency) {
        subscriptionRepository.save(Subscription.builder()
                .description("Netflix")
                .amount(new BigDecimal("4990.00"))
                .currency(currency)
                .workspaceId(DEMO_WORKSPACE_ID)
                .ownerId(DEMO_USER_ID)
                .lastPayment(LocalDate.now().minusMonths(1))
                .build());
    }

    private void seedGoals(Currency currency) {
        goalRepository.save(Goal.builder()
                .workspaceId(DEMO_WORKSPACE_ID)
                .name("Viaje a Bariloche")
                .targetAmount(new BigDecimal("500000.00"))
                .currentAmount(new BigDecimal("120000.00"))
                .currency(currency)
                .targetDate(LocalDate.now().plusMonths(GOAL_TARGET_MONTHS_AHEAD))
                .build());

        goalRepository.save(Goal.builder()
                .workspaceId(DEMO_WORKSPACE_ID)
                .name("Fondo de emergencia")
                .targetAmount(new BigDecimal("1000000.00"))
                .currentAmount(new BigDecimal("400000.00"))
                .currency(currency)
                .build());
    }
}

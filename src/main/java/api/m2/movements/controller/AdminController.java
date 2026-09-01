package api.m2.movements.controller;

import api.m2.movements.services.balance.MonthlySummaryJob;
import api.m2.movements.services.gamification.BudgetBadgeJob;
import api.m2.movements.services.income.RecurringIncomeJob;
import api.m2.movements.services.movements.CreditInstallmentJob;
import api.m2.movements.services.subscriptions.SubscriptionOverdueJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos — todo bajo /v1/admin requiere ROLE_ADMIN, forzado a nivel de
 * SecurityConfiguration (no solo acá), así que un nuevo método en este controller queda
 * protegido aunque alguien se olvide de anotarlo.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin")
@Tag(name = "Admin", description = "Endpoints administrativos — requieren ROLE_ADMIN")
public class AdminController {

    private final RecurringIncomeJob recurringIncomeJob;
    private final CreditInstallmentJob creditInstallmentJob;
    private final SubscriptionOverdueJob subscriptionOverdueJob;
    private final MonthlySummaryJob monthlySummaryJob;
    private final BudgetBadgeJob budgetBadgeJob;

    @Operation(
            summary = "Ejecutar job de ingresos recurrentes",
            description = "Corre manualmente RecurringIncomeJob (normalmente el 1° de cada mes a las 6:00).",
            responses = @ApiResponse(responseCode = "200", description = "Job ejecutado")
    )
    @PostMapping("/crons/recurring-income")
    @ResponseStatus(HttpStatus.OK)
    public void runRecurringIncomeJob() {
        recurringIncomeJob.generateRecurringIncomes();
    }

    @Operation(
            summary = "Ejecutar job de cuotas de crédito",
            description = "Corre manualmente CreditInstallmentJob (normalmente el 1° de cada mes a las 6:30).",
            responses = @ApiResponse(responseCode = "200", description = "Job ejecutado")
    )
    @PostMapping("/crons/credit-installments")
    @ResponseStatus(HttpStatus.OK)
    public void runCreditInstallmentJob() {
        creditInstallmentJob.generateNextInstallments();
    }

    @Operation(
            summary = "Ejecutar job de servicios vencidos",
            description = "Corre manualmente SubscriptionOverdueJob (normalmente todos los días a las 9:00).",
            responses = @ApiResponse(responseCode = "200", description = "Job ejecutado")
    )
    @PostMapping("/crons/subscription-overdue")
    @ResponseStatus(HttpStatus.OK)
    public void runSubscriptionOverdueJob() {
        subscriptionOverdueJob.notifyOverdueSubscriptions();
    }

    @Operation(
            summary = "Ejecutar job de resumen mensual",
            description = "Corre manualmente MonthlySummaryJob (normalmente el último día del mes a las 23:00).",
            responses = @ApiResponse(responseCode = "200", description = "Job ejecutado")
    )
    @PostMapping("/crons/monthly-summary")
    @ResponseStatus(HttpStatus.OK)
    public void runMonthlySummaryJob() {
        monthlySummaryJob.generateMonthlySnapshots();
    }

    @Operation(
            summary = "Ejecutar job de badges de presupuesto",
            description = "Corre manualmente BudgetBadgeJob (normalmente el último día del mes a las 23:00).",
            responses = @ApiResponse(responseCode = "200", description = "Job ejecutado")
    )
    @PostMapping("/crons/budget-badges")
    @ResponseStatus(HttpStatus.OK)
    public void runBudgetBadgeJob() {
        budgetBadgeJob.evaluateClosedBudgets();
    }
}

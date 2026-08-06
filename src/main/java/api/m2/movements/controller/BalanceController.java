package api.m2.movements.controller;

import api.m2.movements.enums.BalanceEnum;
import api.m2.movements.records.balance.BalanceByCategoryRecord;
import api.m2.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.records.balance.BalanceMonthlyEvolutionRecord;
import api.m2.movements.records.balance.RecoveryTimeRecord;
import api.m2.movements.services.balance.CalculateBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/balance")
@Validated
@Tag(name = "Balance", description = "Manejo de balance del usuario.")
public class BalanceController {
    private final CalculateBalanceService calculateBalanceService;

    @Operation(
            summary = "Obtener balance",
            description = "Devuelve el balance total opcionalmente filtrado por año y mes"
    )
    @ApiResponse(responseCode = "200", description = "Balance obtenido correctamente")
    @GetMapping
    public Map<BalanceEnum, BigDecimal> getBalance(@ParameterObject BalanceFilterRecord balanceRecord) {
        return calculateBalanceService.getBalance(balanceRecord);
    }

    @Operation(
            summary = "Balance por categoría",
            description = "Devuelve el balance total agrupado por categoría en un año"
    )
    @ApiResponse(responseCode = "200", description = "Balance por categoría calculado correctamente")
    @GetMapping("/category")
    public Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(@ParameterObject BalanceFilterRecord balanceFilterRecord) {
        return calculateBalanceService.getBalanceWithCategoryByYear(balanceFilterRecord);
    }

    @Operation(
            summary = "Evolución mensual de gastos por moneda",
            description = "Devuelve los totales de gastos agrupados por mes y moneda para un año dado."
    )
    @ApiResponse(responseCode = "200", description = "Evolución calculada correctamente")
    @GetMapping("/monthly-evolution")
    public List<BalanceMonthlyEvolutionRecord> getMonthlyEvolution(
            @RequestParam @Min(2000) @Max(2100) Integer year) {
        return calculateBalanceService.getMonthlyEvolution(year);
    }

    @Operation(
            summary = "Tiempo de recuperación de un gasto",
            description = "Calcula cuántos meses tomaría recuperar un gasto de 'amount' dado el ahorro "
                    + "promedio (ingresos - gastos) de los últimos 'months' meses cerrados, en la moneda indicada."
    )
    @ApiResponse(responseCode = "200", description = "Cálculo realizado correctamente")
    @GetMapping("/recovery-time")
    public RecoveryTimeRecord getRecoveryTime(
            @RequestParam @Positive BigDecimal amount,
            @RequestParam @NotBlank String currency,
            @RequestParam(defaultValue = "3") @Min(1) @Max(24) Integer months) {
        return calculateBalanceService.calculateRecoveryTime(amount, currency, months);
    }
}
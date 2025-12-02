package api.expenses.expenses.controller;

import api.expenses.expenses.enums.BalanceEnum;
import api.expenses.expenses.records.BalanceByCategoryRecord;
import api.expenses.expenses.records.balance.BalanceFilterRecord;
import api.expenses.expenses.services.balance.CalculateBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/balance")
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
    public Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(@RequestParam Integer year) {
        return calculateBalanceService.getBalanceWithCategoryByYear(year);
    }
}
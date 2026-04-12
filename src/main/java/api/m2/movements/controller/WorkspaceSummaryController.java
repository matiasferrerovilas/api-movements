package api.m2.movements.controller;

import api.m2.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.services.balance.MonthlySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces")
@Validated
@Tag(name = "Workspaces", description = "Resúmenes y estadísticas por workspace.")
public class WorkspaceSummaryController {

    private final MonthlySummaryService monthlySummaryService;

    @Operation(
            summary = "Resumen mensual",
            description = "Devuelve el resumen mensual del workspace: total ingresado, total gastado, "
                    + "diferencia, categoría con mayor gasto y comparación vs mes anterior."
    )
    @ApiResponse(responseCode = "200", description = "Resumen calculado correctamente")
    @GetMapping("/{id}/summary/monthly")
    public MonthlySummaryResponse getMonthlySummary(
            @Parameter(description = "ID del workspace") @PathVariable Long id,
            @RequestParam @Min(2000) @Max(2100) Integer year,
            @RequestParam @Min(1) @Max(12) Integer month) {
        return monthlySummaryService.getSummary(id, year, month);
    }
}

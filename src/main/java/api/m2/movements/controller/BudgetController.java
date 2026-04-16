package api.m2.movements.controller;

import api.m2.movements.records.BudgetToAdd;
import api.m2.movements.records.budgets.BudgetRecord;
import api.m2.movements.records.budgets.BudgetToUpdate;
import api.m2.movements.services.budgets.BudgetAddService;
import api.m2.movements.services.budgets.BudgetQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/budgets")
@Tag(name = "Budgets", description = "API para la gestión de presupuestos por cuenta")
public class BudgetController {

    private final BudgetAddService budgetAddService;
    private final BudgetQueryService budgetQueryService;

    @Operation(
            summary = "Listar presupuestos",
            description = "Recupera los presupuestos del workspace activo para un período dado, "
                    + "con el monto consumido calculado. Si no se especifica currency, "
                    + "retorna presupuestos de todas las monedas.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de presupuestos con consumo",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = BudgetRecord.class))
                            )
                    )
            }
    )
    @GetMapping
    public List<BudgetRecord> getBudgets(
            @Parameter(description = "Símbolo de moneda (ej: ARS, USD). Opcional.")
            @RequestParam(required = false) String currency,
            @Parameter(description = "Año del período") @RequestParam int year,
            @Parameter(description = "Mes del período (1-12)") @RequestParam int month) {
        return budgetQueryService.getByAccount(currency, year, month);
    }

    @Operation(
            summary = "Crear presupuesto",
            description = "Crea un nuevo presupuesto para una cuenta. Único por account + category + currency.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Presupuesto creado")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@RequestBody @Valid BudgetToAdd budgetToAdd) {
        budgetAddService.save(budgetToAdd);
    }

    @Operation(
            summary = "Actualizar monto del presupuesto",
            description = "Actualiza el monto de un presupuesto existente",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Presupuesto actualizado")
            }
    )
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void update(@PathVariable Long id, @RequestBody @Valid BudgetToUpdate dto) {
        budgetAddService.update(dto, id);
    }

    @Operation(
            summary = "Eliminar presupuesto",
            description = "Elimina un presupuesto por ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Presupuesto eliminado")
            }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        budgetAddService.delete(id);
    }
}

package api.m2.movements.controller;

import api.m2.movements.records.currencies.CurrencyToAdd;
import api.m2.movements.records.currencies.WorkspaceCurrencyRecord;
import api.m2.movements.services.currencies.WorkspaceCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspace")
@Tag(name = "Workspace Currencies", description = "API para la gestión de monedas de workspaces")
public class WorkspaceCurrencyController {

    private final WorkspaceCurrencyService workspaceCurrencyService;

    @Operation(
            summary = "Obtener monedas del workspace activo",
            description = "Recupera la lista de monedas asociadas al workspace activo del usuario",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de monedas del workspace",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = WorkspaceCurrencyRecord.class)
                                    )
                            )
                    )
            }
    )
    @GetMapping("/currencies")
    public List<WorkspaceCurrencyRecord> getCurrencies() {
        return workspaceCurrencyService.getWorkspaceCurrencies();
    }

    @Operation(
            summary = "Agregar moneda al workspace activo",
            description = "Crea la moneda si no existe (reutilizando el catálogo global por símbolo) "
                    + "y la asocia al workspace activo. Es idempotente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Moneda creada o asociada")
            }
    )
    @PostMapping("/currencies")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceCurrencyRecord addCurrency(@Valid @RequestBody CurrencyToAdd request) {
        return workspaceCurrencyService.addCurrency(request);
    }

    @Operation(
            summary = "Eliminar moneda del workspace activo",
            description = "Elimina la asociación entre el workspace activo y la moneda. "
                    + "No elimina la moneda global ni afecta movimientos/ingresos/suscripciones/presupuestos "
                    + "ya cargados, que referencian la moneda directamente.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Moneda eliminada"),
                    @ApiResponse(responseCode = "404", description = "Moneda no encontrada")
            }
    )
    @DeleteMapping("/currencies/{currencyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrency(@PathVariable Long currencyId) {
        workspaceCurrencyService.deleteCurrency(currencyId);
    }
}

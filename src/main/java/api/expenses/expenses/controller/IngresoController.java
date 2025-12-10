package api.expenses.expenses.controller;

import api.expenses.expenses.records.income.IngresoToAdd;
import api.expenses.expenses.services.income.IncomeAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/income")
@Slf4j
@Tag(name = "Income", description = "API para la gestión de los ingresos personales del usuario")
public class IngresoController {

    private final IncomeAddService incomeAddService;

    @Operation(
            summary = "Cargar ingresos",
            description = "Carga una lista de ingresos para el usuario autenticado.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Lista de ingresos a cargar",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IngresoToAdd.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Ingresos cargados correctamente"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Datos inválidos"
                    )
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void loadIncome(@RequestBody List<IngresoToAdd> ingresoToAdds) {
        incomeAddService.loadIncome(ingresoToAdds);
    }
}

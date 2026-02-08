package api.m2.movements.controller;

import api.m2.movements.records.income.IncomeRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.services.income.IncomeAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
                            schema = @Schema(implementation = IncomeToAdd.class)
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
    public void loadIncome(@RequestBody IncomeToAdd incomeToAdds) {
        incomeAddService.loadIncome(incomeToAdds);
    }

    @Operation(
            summary = "Obtener ingresos",
            description = "Devuelve el listado completo de ingresos configurados para el usuario autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Listado de ingresos obtenido correctamente",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = IncomeRecord.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuario no autenticado"
                    )
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<IncomeRecord> getAllIncomes() {
        return incomeAddService.getAllIncomes();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteIncome(@PathVariable Long id) {
        incomeAddService.deleteIncome(id);
    }


    @Operation(
            summary = "Recargar ingreso",
            description = "Genera un nuevo movimiento basado en un ingreso existente. Se utiliza el ID del ingreso a recargar.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Movimiento generado correctamente"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Ingreso no encontrado"
                    )
            }
    )
    @PostMapping("{id}/reload")
    @ResponseStatus(HttpStatus.CREATED)
    public void reloadIncome(@PathVariable Long id) {
        incomeAddService.reloadIncome(id);
    }
}

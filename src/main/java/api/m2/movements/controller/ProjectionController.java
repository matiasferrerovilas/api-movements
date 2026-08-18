package api.m2.movements.controller;

import api.m2.movements.records.projections.ProjectionResponse;
import api.m2.movements.services.projections.ProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/projection")
@Validated
@Tag(name = "Projection", description = "Proyección de balance futuro en base a la tendencia de flujo de caja")
public class ProjectionController {

    private final ProjectionService projectionService;

    @Operation(
            summary = "Proyectar balance futuro",
            description = "Calcula el ahorro neto promedio (ingresos - gastos) de los últimos 'months' meses "
                    + "cerrados y extrapola linealmente el balance actual hacia adelante (0, 3, 6 y 12 meses). "
                    + "Los montos se devuelven en la moneda por defecto del usuario (o USD si no configuró una, "
                    + "o si la tasa de cambio no estuvo disponible) — ver el campo `currency` de la respuesta. "
                    + "Es una estimación conservadora de tendencia de flujo de caja: "
                    + "no incorpora rendimientos de inversión ni supuestos especulativos. "
                    + "Verifica que el usuario autenticado sea miembro del workspace.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Proyección calculada correctamente",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectionResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @GetMapping
    public ProjectionResponse getProjection(
            @Parameter(description = "ID del workspace", required = true)
            @RequestParam Long workspaceId,
            @Parameter(description = "Cantidad de meses cerrados usados para calcular el promedio de ahorro neto")
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) Integer months) {
        return projectionService.getProjection(workspaceId, months);
    }
}

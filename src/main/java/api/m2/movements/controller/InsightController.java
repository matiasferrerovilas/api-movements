package api.m2.movements.controller;

import api.m2.movements.records.insights.CategoryInsightRecord;
import api.m2.movements.services.insights.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/insights")
@Tag(name = "Insights", description = "Detección de anomalías de gasto por categoría")
public class InsightController {

    private final InsightService insightService;

    @Operation(
            summary = "Obtener insights de gasto del mes actual",
            description = "Compara el gasto del mes en curso, categoría por categoría, contra el promedio "
                    + "de los últimos 6 meses (calculado sobre los snapshots mensuales). Devuelve únicamente "
                    + "las categorías cuya desviación supera el ±25%. Verifica que el usuario autenticado "
                    + "sea miembro del workspace.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de insights detectados, ordenada por magnitud de desviación descendente",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = CategoryInsightRecord.class))
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @GetMapping
    public List<CategoryInsightRecord> getInsights(
            @Parameter(description = "ID del workspace", required = true)
            @RequestParam Long workspaceId) {
        return insightService.getInsights(workspaceId);
    }
}

package api.m2.movements.controller;

import api.m2.movements.records.goals.GoalContribution;
import api.m2.movements.records.goals.GoalRecord;
import api.m2.movements.records.goals.GoalToAdd;
import api.m2.movements.records.goals.GoalToUpdate;
import api.m2.movements.services.goals.GoalAddService;
import api.m2.movements.services.goals.GoalQueryService;
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
@RequestMapping("/v1/goals")
@Tag(name = "Goals", description = "API para la gestión de metas de ahorro por workspace")
public class GoalController {

    private final GoalAddService goalAddService;
    private final GoalQueryService goalQueryService;

    @Operation(
            summary = "Listar metas de ahorro",
            description = "Recupera todas las metas de ahorro del workspace indicado, con el porcentaje "
                    + "de avance ya calculado (capado en 100%). Verifica que el usuario autenticado "
                    + "sea miembro del workspace.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de metas de ahorro",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = GoalRecord.class))
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @GetMapping
    public List<GoalRecord> getGoals(
            @Parameter(description = "ID del workspace", required = true)
            @RequestParam Long workspaceId) {
        return goalQueryService.getByWorkspace(workspaceId);
    }

    @Operation(
            summary = "Crear meta de ahorro",
            description = "Crea una nueva meta de ahorro para un workspace, con monto acumulado inicial en cero.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Meta de ahorro creada",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GoalRecord.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalRecord save(@RequestBody @Valid GoalToAdd goalToAdd) {
        return goalAddService.save(goalToAdd);
    }

    @Operation(
            summary = "Actualizar meta de ahorro",
            description = "Actualiza nombre, monto objetivo y/o fecha objetivo de una meta existente. "
                    + "Todos los campos son opcionales; solo se actualizan los enviados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Meta de ahorro actualizada"),
                    @ApiResponse(responseCode = "404", description = "Meta de ahorro no encontrada"),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GoalRecord update(@PathVariable Long id, @RequestBody @Valid GoalToUpdate dto) {
        return goalAddService.update(dto, id);
    }

    @Operation(
            summary = "Registrar contribución a una meta de ahorro",
            description = "Incrementa el monto acumulado (currentAmount) de la meta en el importe indicado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contribución registrada"),
                    @ApiResponse(responseCode = "404", description = "Meta de ahorro no encontrada"),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @PatchMapping("/{id}/contribute")
    @ResponseStatus(HttpStatus.OK)
    public GoalRecord contribute(@PathVariable Long id, @RequestBody @Valid GoalContribution dto) {
        return goalAddService.contribute(dto, id);
    }

    @Operation(
            summary = "Eliminar meta de ahorro",
            description = "Elimina una meta de ahorro por ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Meta de ahorro eliminada"),
                    @ApiResponse(responseCode = "404", description = "Meta de ahorro no encontrada"),
                    @ApiResponse(responseCode = "403", description = "El usuario no pertenece al workspace")
            }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        goalAddService.delete(id);
    }
}

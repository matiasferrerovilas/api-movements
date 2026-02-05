package api.m2.movements.controller;

import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.MovementGetService;
import api.m2.movements.services.movements.files.MovementImportFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/expenses")
@Slf4j
@Tag(name = "Movements", description = "API para la gestión de gastos personales")
public class MovementController {
    private final MovementAddService movementAddService;
    private final MovementImportFileService movementImportFileService;
    private final MovementGetService movementGetService;

    @Operation(
            summary = "Listar gastos",
            description = "Obtiene una lista paginada de movimientos filtrados.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Listado obtenido correctamente",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = MovementRecord.class))
                            )
                    )
            }
    )
    @GetMapping
    public Page<@NonNull MovementRecord> getExpensesBy(
            @ParameterObject MovementSearchFilterRecord filter,
            Pageable page) {
        return movementGetService.getExpensesBy(filter, page);
    }

    @Operation(
            summary = "Crear un movimiento",
            description = "Crea un nuevo registro de gasto.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Creado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementRecord saveExpense(
            @Parameter(description = "Datos del gasto a crear", required = true)
            @Valid @RequestBody MovementToAdd movementToAdd) {
        return movementAddService.saveMovement(movementToAdd);
    }

    @Operation(
            summary = "Importar movimientos desde archivo",
            description = "Importa múltiples movimientos desde un archivo bancario.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Archivo procesado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Formato de archivo inválido")
            }
    )
    @PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void saveExpenseByFile(
            @Parameter(description = "Archivo bancario", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Banco del cual proviene el archivo", required = true)
            @RequestParam("bank") String bank,

            @Parameter(description = "Grupo destino (opcional)")
            @RequestParam(value = "accountId", required = false) Long accountId) {
        movementImportFileService.importMovementsByFile(file, bank, accountId);
    }

    @Operation(
            summary = "Actualizar un movimiento",
            description = "Actualiza datos del gasto por ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Actualizado correctamente"),
                    @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
            }
    )
    @PatchMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMovement(
            @Parameter(description = "Archivo con los gastos a importar", required = true)
            @Valid @RequestBody ExpenseToUpdate expenseToUpdate,
            @PathVariable Long id) {
        movementAddService.updateMovement(expenseToUpdate, id);
    }

    @Operation(
            summary = "Eliminar un movimiento",
            description = "Elimina un gasto existente por ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Eliminado correctamente"),
                    @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
            }
    )
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovement(@PathVariable Long id) {
        movementAddService.deleteMovement(id);
    }
}
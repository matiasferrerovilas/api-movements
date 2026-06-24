package api.m2.movements.movements.controller;

import api.m2.movements.movements.records.categories.CategoryMigrateRequest;
import api.m2.movements.movements.records.categories.CategoryPatchRequest;
import api.m2.movements.movements.records.categories.CategoryRecord;
import api.m2.movements.movements.services.category.CategoryMigrateService;
import api.m2.movements.movements.services.category.WorkspaceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/v1/workspace")
@Tag(name = "Workspace Categories", description = "API para la gestión de categorías de workspaces")
public class WorkspaceCategoryController {

    private final WorkspaceCategoryService workspaceCategoryService;
    private final CategoryMigrateService categoryMigrateService;

    @Operation(
            summary = "Obtener categorias activas del workspace activo",
            description = "Recupera la lista de categorias activas asociadas al workspace activo del usuario",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de categorias del workspace",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = CategoryRecord.class)
                                    )
                            )
                    )
            }
    )
    @GetMapping("/categories")
    public List<CategoryRecord> getCategories() {
        return workspaceCategoryService.getActiveCategories();
    }

    @Operation(
            summary = "Agregar categoria al workspace activo",
            description = "Crea la categoria si no existe y la asocia al workspace activo. Es idempotente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Categoria creada o reactivada")
            }
    )
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRecord addCategory(@RequestParam String description) {
        return workspaceCategoryService.addCategory(description);
    }

    @Operation(
            summary = "Actualizar categoría",
            description = "Actualiza la descripción, ícono y/o color de una categoría. "
                    + "Todos los campos son opcionales.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
                    @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
            }
    )
    @PatchMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryRecord updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPatchRequest request
    ) {
        return workspaceCategoryService.updateCategory(categoryId, request);
    }

    @Operation(
            summary = "Eliminar categoria del workspace activo",
            description = "Elimina la asociacion entre el workspace activo y la categoria. "
                    + "No elimina la categoria global.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
                    @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
            }
    )
    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        workspaceCategoryService.deleteCategory(categoryId);
    }

    @Operation(
            summary = "Migrar movimientos de una categoria a otra",
            description = "Reasigna los movimientos del workspace activo de fromCategoryId a toCategoryId. "
                    + "Solo ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Migracion completada"),
                    @ApiResponse(responseCode = "400",
                            description = "fromCategoryId igual a toCategoryId"),
                    @ApiResponse(responseCode = "404", description = "Categoria destino no encontrada")
            }
    )
    @PatchMapping("/categories/migrate")
    @PreAuthorize("hasRole('ADMIN')")
    public void migrateCategory(@Valid @RequestBody CategoryMigrateRequest request) {
        categoryMigrateService.migrateCategory(request);
    }
}
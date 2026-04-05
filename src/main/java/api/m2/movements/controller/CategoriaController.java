package api.m2.movements.controller;

import api.m2.movements.records.categories.CategoryMigrateRequest;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.services.category.CategoryMigrateService;
import api.m2.movements.services.category.UserCategoryService;
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
@RequestMapping("/v1/categories")
@Tag(name = "Categorias", description = "API para la gestión de categorias personales")
public class CategoriaController {

    private final UserCategoryService userCategoryService;
    private final CategoryMigrateService categoryMigrateService;

    @Operation(
            summary = "Obtener categorias activas del usuario",
            description = "Recupera la lista de categorias activas asociadas al usuario autenticado",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de categorias del usuario",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = CategoryRecord.class))
                            )
                    )
            }
    )
    @GetMapping
    public List<CategoryRecord> getCategories() {
        return userCategoryService.getActiveCategories();
    }

    @Operation(
            summary = "Agregar categoria al usuario",
            description = "Crea la categoria si no existe y la asocia al usuario autenticado. Es idempotente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Categoria creada o reactivada")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRecord addCategory(@RequestParam String description) {
        return userCategoryService.addCategory(description);
    }

    @Operation(
            summary = "Eliminar categoria del usuario",
            description = "Elimina la asociacion entre el usuario y la categoria. No elimina la categoria global.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
                    @ApiResponse(responseCode = "403", description = "La categoria no puede eliminarse")
            }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        userCategoryService.deleteCategory(id);
    }

    @Operation(
            summary = "Migrar movimientos de una categoria a otra",
            description = "Reasigna los movimientos del usuario de fromCategoryId a toCategoryId. Solo ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Migracion completada"),
                    @ApiResponse(responseCode = "400", description = "fromCategoryId igual a toCategoryId"),
                    @ApiResponse(responseCode = "404", description = "Categoria destino no encontrada")
            }
    )
    @PatchMapping("/migrate")
    @PreAuthorize("hasRole('ADMIN')")
    public void migrateCategory(@Valid @RequestBody CategoryMigrateRequest request) {
        categoryMigrateService.migrateCategory(request);
    }
}

package api.expenses.expenses.controller;

import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.services.category.CategoryAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/categories")
@Tag(name = "Categorias", description = "API para la gestión de categorias personales")
public class CategoriaController {

    private final CategoryAddService categoryAddService;

    @Operation(
            summary = "Obtener todas las categorias",
            description = "Recupera una lista de categorias",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de categorias encontradas",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = CategoryRecord.class))
                            )
                    )
            }
    )
    @GetMapping
    public List<CategoryRecord> getAllCategories() {
        return categoryAddService.getAllCategories();
    }
}

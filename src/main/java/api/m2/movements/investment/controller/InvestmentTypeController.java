package api.m2.movements.investment.controller;

import api.m2.movements.investment.records.InvestmentTypeRecord;
import api.m2.movements.investment.records.InvestmentTypeToAdd;
import api.m2.movements.investment.records.InvestmentTypeToUpdate;
import api.m2.movements.investment.services.InvestmentTypeService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspace/investment-types")
@Tag(name = "Investment Types", description = "API para la gestión de tipos de inversión del workspace")
public class InvestmentTypeController {

    private final InvestmentTypeService investmentTypeService;

    @Operation(summary = "Listar tipos de inversión del workspace activo")
    @GetMapping
    public List<InvestmentTypeRecord> getAll() {
        return investmentTypeService.getByWorkspace();
    }

    @Operation(summary = "Crear un tipo de inversión en el workspace activo")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentTypeRecord create(@Valid @RequestBody InvestmentTypeToAdd dto) {
        return investmentTypeService.add(dto);
    }

    @Operation(summary = "Actualizar un tipo de inversión del workspace activo")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InvestmentTypeRecord update(@PathVariable Long id, @RequestBody InvestmentTypeToUpdate dto) {
        return investmentTypeService.update(id, dto);
    }

    @Operation(summary = "Eliminar un tipo de inversión del workspace activo")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        investmentTypeService.delete(id);
    }
}

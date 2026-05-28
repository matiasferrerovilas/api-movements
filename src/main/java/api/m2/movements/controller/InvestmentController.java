package api.m2.movements.controller;

import api.m2.movements.records.investments.InvestmentRecord;
import api.m2.movements.records.investments.InvestmentToAdd;
import api.m2.movements.records.investments.InvestmentToUpdate;
import api.m2.movements.services.investments.InvestmentAddService;
import api.m2.movements.services.investments.InvestmentQueryService;
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
@RequestMapping("/v1/investments")
@Tag(name = "Investments", description = "API para la gestión de inversiones")
public class InvestmentController {

    private final InvestmentAddService investmentAddService;
    private final InvestmentQueryService investmentQueryService;

    @Operation(summary = "Listar inversiones del workspace activo")
    @GetMapping
    public List<InvestmentRecord> getAll() {
        return investmentQueryService.getByWorkspace();
    }

    @Operation(summary = "Crear una inversión")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentRecord create(@Valid @RequestBody InvestmentToAdd dto) {
        return investmentAddService.add(dto);
    }

    @Operation(summary = "Actualizar una inversión")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InvestmentRecord update(@PathVariable Long id, @RequestBody InvestmentToUpdate dto) {
        return investmentAddService.update(dto, id);
    }

    @Operation(summary = "Eliminar una inversión")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        investmentAddService.delete(id);
    }
}
